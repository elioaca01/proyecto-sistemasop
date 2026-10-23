package avilaos.ui;
import avilaos.config.SimulationConfig;
import avilaos.process.ProcessDefinition;
import avilaos.scheduling.SchedulingPolicyType;
import avilaos.sync.BufferDefinition;
import avilaos.simulation.*;
import avilaos.ui.session.*;
import java.util.function.LongSupplier;
import javax.swing.*;
/** Adaptador EDT: entrega solicitudes y presenta valores, nunca espera el cierre del motor. */
public final class UiController {
    private final UiSession session;private final UiView view;private final Timer timer;private SessionView last;
    private long generation=-1,version=-1;private int noticeCount,resultCount;private boolean closing,closed;
    public UiController(UiSession session,UiView view){if(session==null||view==null)throw new IllegalArgumentException("Sesión y vista requeridas");this.session=session;this.view=view;timer=new Timer(100,e->refresh());}
    private static void edt(){if(!SwingUtilities.isEventDispatchThread())throw new IllegalStateException("La interfaz se actualiza en EDT");}
    public void start(){edt();timer.start();refresh();}
    public void refresh(){edt();if(!closed)accept(session.view());}
    /** Entrada de una publicación: también filtra entregas demoradas de otra generación. */
    public void accept(SessionView value){
        edt();if(closed||value.generation()<generation)return;
        if(closing&&value.phase()!=SessionPhase.CLOSING&&value.phase()!=SessionPhase.CLOSED&&value.phase()!=SessionPhase.FAILED)return;
        SimulationSnapshot snapshot=value.simulation();long nextVersion=snapshot==null?-1:snapshot.version();
        if(value.generation()==generation&&nextVersion<version)return;
        if(value.generation()!=generation){noticeCount=0;resultCount=0;version=-1;last=null;generation=value.generation();}
        boolean changed=last==null||version!=nextVersion||last.phase()!=value.phase()||last.pendingActions()!=value.pendingActions()||last.started()!=value.started()||last.initialConfig()!=value.initialConfig()||last.noticeCount()!=value.noticeCount();
        if(changed)view.render(value);
        for(int i=noticeCount;i<value.noticeCount();i++)view.showNotice(value.noticeAt(i));noticeCount=value.noticeCount();
        if(snapshot!=null){for(int i=resultCount;i<snapshot.resultCount();i++){CommandResult r=snapshot.resultAt(i);view.showNotice(new SessionNotice(-1,r.requestId(),!r.accepted(),"Orden "+r.requestId()+" "+(r.accepted()?"aplicada":"rechazada")+": "+r.message()));}resultCount=snapshot.resultCount();}
        last=value;version=nextVersion;
        if(value.phase()==SessionPhase.CLOSED){closed=true;timer.stop();view.closed();}
    }
    private void send(LongSupplier action){edt();if(closing||closed)return;try{action.getAsLong();refresh();}catch(IllegalArgumentException|IllegalStateException failure){view.showNotice(new SessionNotice(-1,-1,true,failure.getMessage()));}}
    public void resume(){send(session::resume);}public void pause(){send(session::pause);}public void step(){send(session::step);}public void restart(){send(session::restart);}
    public void replaceScenario(SimulationConfig c){send(()->session.replaceScenario(c));}public void addProcess(ProcessDefinition p){send(()->session.addProcess(p));}public void addBuffer(BufferDefinition b){send(()->session.addBuffer(b));}
    public void changeScheduling(int id,SchedulingPolicyType p,int q){send(()->session.changeScheduling(id,p,q));}public void changeCycleDuration(long ms){send(()->session.changeCycleDuration(ms));}
    public void requestClose(){edt();if(closed)return;closing=true;session.requestClose();refresh();}
}
