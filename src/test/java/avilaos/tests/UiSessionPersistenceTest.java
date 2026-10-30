package avilaos.tests;

import avilaos.config.*;
import avilaos.process.ProcessType;
import avilaos.scheduling.SchedulingPolicyType;
import avilaos.simulation.*;
import avilaos.sync.BufferDefinition;
import avilaos.ui.session.*;
import java.io.IOException;
import java.nio.file.Path;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;
import javax.swing.SwingUtilities;
import static avilaos.tests.TestSupport.*;

public final class UiSessionPersistenceTest {
    static final Path FILE=Path.of("escenario de prueba.json");
    static final class Store implements ConfigurationStore {
        volatile SimulationConfig loaded=ScenarioJsonTest.read(ScenarioJsonTest.REMOTE),saved;
        volatile boolean failLoad,failSave,blockLoad,blockSave;
        volatile int loads,saves;final Semaphore entered=new Semaphore(0),release=new Semaphore(0);
        private void waitForRelease(){entered.release();release.acquireUninterruptibly();}
        public SimulationConfig load(Path file)throws IOException{loads++;if(blockLoad)waitForRelease();if(failLoad)throw new ConfigurationException(file+": $.formatVersion inválido");return loaded;}
        public void save(Path file,SimulationConfig c)throws IOException{saves++;if(blockSave)waitForRelease();if(failSave)throw new IOException(file+": escritura rechazada");saved=c;}
    }
    private static SessionView settled(UiSession session){return SessionTestSupport.await(session,v->v.phase()==SessionPhase.ACTIVE&&v.pendingActions()==0);}
    private static SessionView step(UiSession session){long cycle=session.view().simulation().nextCycle();session.step();return SessionTestSupport.await(session,v->v.pendingActions()==0&&v.simulation().nextCycle()==cycle+1);}
    public static void run(){initialVersusLive();loadsPreserveOrReplace();closeDuringFileOperation(false);closeDuringFileOperation(true);}
    private static void initialVersusLive(){
        Store store=new Store();UiSession s=new UiSession(ScenarioJsonTest.mixed().withTiming(500,2),store);
        try{
            settled(s);s.addProcess(SimulationTest.cpu(1,5));s.addBuffer(new BufferDefinition(2,2,1));s.changeCycleDuration(25);s.changeScheduling(1,SchedulingPolicyType.ROUND_ROBIN,1);s.saveScenario(FILE);
            SessionView initial=settled(s);equal(5,store.saved.processCount());equal(2,store.saved.bufferCount());equal(25L,store.saved.cycleMillis());equal(SchedulingPolicyType.ROUND_ROBIN,store.saved.computerAt(0).policy());
            ScenarioJsonTest.equivalent(initial.initialConfig(),store.saved);equal(0L,initial.simulation().nextCycle());
            step(s);s.addProcess(SimulationTest.cpu(1,3));s.addBuffer(new BufferDefinition(3,1,1));s.changeCycleDuration(7);s.changeScheduling(1,SchedulingPolicyType.EDF,2);settled(s);step(s);
            equal(7L,s.view().simulation().cycleMillis());equal(SchedulingPolicyType.EDF,s.view().simulation().computerAt(0).policyType());
            s.resume();SessionTestSupport.await(s,v->v.simulation().state()==SimulationState.RUNNING);s.saveScenario(FILE);settled(s);ScenarioJsonTest.equivalent(initial.initialConfig(),store.saved);
            s.pause();SessionTestSupport.await(s,v->v.pendingActions()==0&&v.simulation().state()==SimulationState.PAUSED);
            store.failSave=true;long generation=s.view().generation(),cycle=s.view().simulation().nextCycle();s.saveScenario(FILE);SessionView rejected=settled(s);equal(generation,rejected.generation());equal(cycle,rejected.simulation().nextCycle());isTrue(rejected.noticeAt(rejected.noticeCount()-1).error());equal(SessionPhase.ACTIVE,rejected.phase());
        }finally{SessionTestSupport.close(s);}
    }
    private static void loadsPreserveOrReplace(){
        Store store=new Store();UiSession s=new UiSession(SimulationConfigTest.empty(),store);
        try{
            long before=settled(s).generation();s.loadScenario(FILE);SessionView first=settled(s);isTrue(first.generation()>before);equal(0L,first.simulation().nextCycle());equal(0,first.simulation().computerAt(0).newCount());equal(3,first.noticeCount());isTrue(first.noticeAt(0).engineRequestId()>0);isTrue(first.noticeAt(1).engineRequestId()>0);equal(-1L,first.noticeAt(2).engineRequestId());
            step(s);step(s);long generation=s.view().generation();s.loadScenario(FILE);SessionView loaded=settled(s);
            isTrue(loaded.generation()>generation);equal(0L,loaded.simulation().nextCycle());equal(SimulationState.PAUSED,loaded.simulation().state());isTrue(!loaded.started());equal(1,loaded.initialConfig().processCount());
            SessionView one=step(s);equal(1L,one.simulation().computerAt(1).running().id());equal(1,one.simulation().computerAt(1).running().pc());
            generation=one.generation();store.failLoad=true;s.loadScenario(FILE);SessionView invalid=settled(s);equal(generation,invalid.generation());equal(1L,invalid.simulation().nextCycle());ScenarioJsonTest.equivalent(one.initialConfig(),invalid.initialConfig());isTrue(invalid.noticeAt(invalid.noticeCount()-1).message().contains("formatVersion"));isTrue(invalid.noticeAt(invalid.noticeCount()-1).error());equal(SessionPhase.ACTIVE,invalid.phase());
            step(s);equal(2L,s.view().simulation().nextCycle());store.failLoad=false;s.resume();SessionTestSupport.await(s,v->v.simulation().state()==SimulationState.RUNNING);int loads=store.loads;s.loadScenario(FILE);SessionView running=settled(s);equal(loads,store.loads);equal(generation,running.generation());isTrue(running.noticeAt(running.noticeCount()-1).error());
            s.pause();SessionTestSupport.await(s,v->v.pendingActions()==0&&v.simulation().state()==SimulationState.PAUSED);s.loadScenario(FILE);s.restart();SessionView reset=settled(s);equal(0L,reset.simulation().nextCycle());isTrue(!reset.started());ScenarioJsonTest.equivalent(store.loaded,reset.initialConfig());
            s.step();SessionView again=SessionTestSupport.await(s,v->v.pendingActions()==0&&v.simulation().nextCycle()==1);equal(1L,again.simulation().computerAt(1).running().id());
        }finally{SessionTestSupport.close(s);}
    }
    private static void closeDuringFileOperation(boolean save){
        Store store=new Store();store.blockSave=save;store.blockLoad=!save;UiSession s=new UiSession(SimulationConfigTest.empty(),store);
        try{
            long generation=settled(s).generation();Semaphore heartbeat=new Semaphore(0);
            SwingTestSupport.edt(()->{if(save)s.saveScenario(FILE);else s.loadScenario(FILE);SwingUtilities.invokeLater(heartbeat::release);});
            isTrue(heartbeat.tryAcquire(2,TimeUnit.SECONDS));isTrue(store.entered.tryAcquire(2,TimeUnit.SECONDS));isTrue(s.view().pendingActions()>0);
            s.restart();SwingTestSupport.edt(()->{s.requestClose();SwingUtilities.invokeLater(heartbeat::release);});isTrue(heartbeat.tryAcquire(2,TimeUnit.SECONDS));isTrue(!s.awaitClosed(20));
            store.release.release();isTrue(s.awaitClosed(3000));equal(SessionPhase.CLOSED,s.view().phase());equal(generation,s.view().generation());equal(0,s.view().initialConfig().processCount());
            throwsType(IllegalStateException.class,()->s.loadScenario(FILE));
        }catch(InterruptedException e){throw new AssertionError(e);}
        finally{store.release.release();SessionTestSupport.close(s);}
    }
}
