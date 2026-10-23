package avilaos.ui.session;

import avilaos.config.SimulationConfig;
import avilaos.process.ProcessDefinition;
import avilaos.simulation.*;
import avilaos.scheduling.SchedulingPolicyType;
import avilaos.structures.LinkedList;
import avilaos.structures.LinkedQueue;
import avilaos.sync.BufferDefinition;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;

/** Dueño del motor. Las solicitudes no esperan ciclos, creación ni cierre en el llamador. */
public final class UiSession {
    private enum Kind { REPLACE, ADD_PROCESS, ADD_BUFFER, SCHEDULING, DURATION, RESUME, PAUSE, STEP, RESTART }
    private record Action(long id,Kind kind,Object data) { }
    private record Schedule(int computerId,SchedulingPolicyType policy,int quantum) { }
    private final Semaphore mutex=new Semaphore(1),wake=new Semaphore(0),finished=new Semaphore(0);
    private final LinkedQueue<Action> actions=new LinkedQueue<>();
    private final LinkedList<SessionNotice> notices=new LinkedList<>();
    private final Thread worker;
    private Simulation engine;
    private SimulationSnapshot lastSnapshot;
    private SimulationConfig initial;
    private SessionPhase phase=SessionPhase.PREPARING;
    private long nextActionId=1,generation;
    private int pendingActions=1;
    private boolean started,closing;

    public UiSession(SimulationConfig initial){
        if(initial==null)throw new IllegalArgumentException("Escenario inicial requerido");
        this.initial=initial;actions.enqueue(new Action(0,Kind.REPLACE,initial));
        worker=new Thread(this::run,"avilaos-ui-session");worker.start();wake.release();
    }
    private long submit(Kind kind,Object data){
        mutex.acquireUninterruptibly();
        try{
            if(closing||phase==SessionPhase.CLOSED)throw new IllegalStateException("Sesión cerrada");
            long id=nextActionId;nextActionId=Math.incrementExact(id);actions.enqueue(new Action(id,kind,data));pendingActions++;wake.release();return id;
        }finally{mutex.release();}
    }
    public long replaceScenario(SimulationConfig c){return submit(Kind.REPLACE,c);}
    public long addProcess(ProcessDefinition p){return submit(Kind.ADD_PROCESS,p);}
    public long addBuffer(BufferDefinition b){return submit(Kind.ADD_BUFFER,b);}
    public long changeScheduling(int id,SchedulingPolicyType policy,int quantum){return submit(Kind.SCHEDULING,new Schedule(id,policy,quantum));}
    public long changeCycleDuration(long millis){return submit(Kind.DURATION,millis);}
    public long resume(){return submit(Kind.RESUME,null);} public long pause(){return submit(Kind.PAUSE,null);}
    public long step(){return submit(Kind.STEP,null);} public long restart(){return submit(Kind.RESTART,null);}
    private void notice(long id,long request,boolean error,String text){
        mutex.acquireUninterruptibly();try{notices.addLast(new SessionNotice(id,request,error,text));}finally{mutex.release();}
    }
    private boolean isClosing(){mutex.acquireUninterruptibly();try{return closing;}finally{mutex.release();}}
    private void run(){
        boolean interrupted=false;
        try{
            while(true){
                wake.acquire();Action action;
                mutex.acquireUninterruptibly();try{if(closing)break;action=actions.dequeue();}finally{mutex.release();}
                if(action==null)continue;
                try{execute(action);}
                catch(IllegalArgumentException|IllegalStateException rejected){notice(action.id(),-1,true,rejected.getMessage());}
                catch(Throwable failure){mutex.acquireUninterruptibly();try{phase=SessionPhase.FAILED;}finally{mutex.release();}notice(action.id(),-1,true,"Fallo de sesión: "+failure);}
                finally{mutex.acquireUninterruptibly();try{pendingActions--;}finally{mutex.release();}}
            }
        }catch(InterruptedException e){interrupted=true;notice(-1,-1,true,"Sesión interrumpida");}
        finally{
            mutex.acquireUninterruptibly();
            try{closing=true;phase=SessionPhase.CLOSING;Action a;while((a=actions.dequeue())!=null){pendingActions--;notices.addLast(new SessionNotice(a.id(),-1,true,"Cancelada por cierre"));}}
            finally{mutex.release();}
            boolean clean=true;Simulation old=engine;
            try{if(old!=null)old.close();}catch(Throwable e){clean=false;notice(-1,-1,true,"No se confirmó el cierre: "+e);}
            SimulationSnapshot end=old==null?lastSnapshot:old.snapshot();
            mutex.acquireUninterruptibly();try{lastSnapshot=end;engine=null;phase=clean?SessionPhase.CLOSED:SessionPhase.FAILED;}finally{mutex.release();}
            finished.release();if(interrupted)Thread.currentThread().interrupt();
        }
    }
    private void execute(Action a){
        if(a.kind()==Kind.REPLACE){
            if(a.id()!=0&&started)throw new IllegalStateException("La configuración inicial se edita antes de Iniciar/Paso; usa Reiniciar");
            replace((SimulationConfig)a.data());notice(a.id(),-1,false,"Escenario inicial preparado; ciclo 0");return;
        }
        if(a.kind()==Kind.RESTART){replace(initial);notice(a.id(),-1,false,"Reinicio del escenario inicial; ciclo 0");return;}
        if(engine==null||phase!=SessionPhase.ACTIVE)throw new IllegalStateException("La sesión no está disponible");
        switch(a.kind()){
            case ADD_PROCESS->{ProcessDefinition p=(ProcessDefinition)a.data();SimulationConfig candidate=started?initial:initial.withAddedProcess(p);long req=engine.enqueue(new AddProcessCommand(p));publishInitial(candidate);notice(a.id(),req,false,"Encolada: alta de proceso; se aplica en el próximo ciclo");}
            case ADD_BUFFER->{BufferDefinition b=(BufferDefinition)a.data();SimulationConfig candidate=started?initial:initial.withAddedBuffer(b);long req=engine.enqueue(new AddBufferCommand(b));publishInitial(candidate);notice(a.id(),req,false,"Encolada: alta de buffer; se aplica en el próximo ciclo");}
            case SCHEDULING->{
                Schedule change=(Schedule)a.data();ChangeSchedulingCommand command=new ChangeSchedulingCommand(change.computerId(),change.policy(),change.quantum());
                SimulationConfig candidate=initial;
                if(!started){ComputerConfig[] cs=new ComputerConfig[initial.computerCount()];boolean found=false;for(int i=0;i<cs.length;i++){ComputerConfig c=initial.computerAt(i);cs[i]=c;if(c.id()==change.computerId()){cs[i]=new ComputerConfig(c.id(),c.ramCapacity(),change.policy(),change.quantum());found=true;}}if(!found)throw new IllegalArgumentException("Computador inexistente");candidate=initial.withComputers(cs);}
                long req=engine.enqueue(command);publishInitial(candidate);notice(a.id(),req,false,"Encolada: política y quantum; se aplica en el próximo ciclo");
            }
            case DURATION->{long ms=(Long)a.data();ChangeCycleDurationCommand command=new ChangeCycleDurationCommand(ms);SimulationConfig candidate=started?initial:initial.withTiming(ms,initial.networkLatency());long req=engine.enqueue(command);publishInitial(candidate);notice(a.id(),req,false,"Encolada: duración del ciclo; se aplica en el próximo ciclo");}
            case RESUME->{engine.resume();markStarted();notice(a.id(),-1,false,"Inicio/continuación solicitado");}
            case STEP->{engine.step();markStarted();notice(a.id(),-1,false,"Un paso solicitado");}
            case PAUSE->{engine.pause();notice(a.id(),-1,false,"Pausa solicitada; se confirma al terminar el ciclo");}
            default->throw new IllegalStateException("Acción no reconocida");
        }
    }
    private void publishInitial(SimulationConfig candidate){mutex.acquireUninterruptibly();try{initial=candidate;}finally{mutex.release();}}
    private void markStarted(){mutex.acquireUninterruptibly();try{started=true;}finally{mutex.release();}}
    private void replace(SimulationConfig config){
        if(config==null)throw new IllegalArgumentException("Escenario inicial requerido");
        mutex.acquireUninterruptibly();try{phase=generation==0?SessionPhase.PREPARING:SessionPhase.REPLACING;}finally{mutex.release();}
        Simulation old=engine;
        if(old!=null){old.close();mutex.acquireUninterruptibly();try{lastSnapshot=old.snapshot();engine=null;}finally{mutex.release();}}
        if(isClosing())return;
        ComputerConfig[] cs=new ComputerConfig[config.computerCount()];for(int i=0;i<cs.length;i++)cs[i]=config.computerAt(i);
        Simulation created=new Simulation(cs,config.cycleMillis(),config.networkLatency());boolean installed=false;
        try{
            for(int i=0;i<config.bufferCount();i++)created.enqueue(new AddBufferCommand(config.bufferAt(i)));
            for(int i=0;i<config.processCount();i++)created.enqueue(new AddProcessCommand(config.processAt(i)));
            mutex.acquireUninterruptibly();
            try{if(!closing){engine=created;initial=config;generation++;started=false;lastSnapshot=created.snapshot();phase=SessionPhase.ACTIVE;installed=true;}}
            finally{mutex.release();}
        }finally{if(!installed)created.close();}
    }
    public SessionView view(){
        Simulation captured;SimulationSnapshot last;SimulationConfig config;SessionPhase p;long g;boolean began;int pending;SessionNotice[] copies;
        mutex.acquireUninterruptibly();
        try{captured=engine;last=lastSnapshot;config=initial;p=phase;g=generation;began=started;pending=pendingActions;copies=new SessionNotice[notices.size()];for(int i=0;i<copies.length;i++)copies[i]=notices.get(i);}
        finally{mutex.release();}
        return new SessionView(g,p,began,pending,config,captured==null?last:captured.snapshot(),copies);
    }
    public void requestClose(){mutex.acquireUninterruptibly();try{closing=true;if(phase!=SessionPhase.CLOSED)phase=SessionPhase.CLOSING;wake.release();}finally{mutex.release();}}
    /** Espera acotada para llamadores fuera del hilo de eventos. */
    public boolean awaitClosed(long timeoutMillis)throws InterruptedException{
        if(timeoutMillis<=0)throw new IllegalArgumentException("Espera positiva requerida");if(Thread.currentThread()==worker)throw new IllegalStateException("La sesión no puede esperarse a sí misma");
        long start=System.nanoTime();if(!finished.tryAcquire(timeoutMillis,TimeUnit.MILLISECONDS))return false;finished.release();
        long remaining=TimeUnit.MILLISECONDS.toNanos(timeoutMillis)-(System.nanoTime()-start);if(remaining>0)worker.join(Math.max(1,TimeUnit.NANOSECONDS.toMillis(remaining)));return !worker.isAlive();
    }
}
