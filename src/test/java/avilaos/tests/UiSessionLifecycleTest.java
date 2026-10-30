package avilaos.tests;
import avilaos.ui.session.*;
import avilaos.simulation.*;
import static avilaos.tests.TestSupport.*;
public final class UiSessionLifecycleTest {
    public static void run(){
        for(int i=0;i<8;i++){
            UiSession preparing=new UiSession(SimulationConfigTest.empty());
            if(i%2==0)preparing.restart();
            SessionTestSupport.close(preparing);equal(SessionPhase.CLOSED,preparing.view().phase());
        }
        UiSession s=new UiSession(SimulationConfigTest.empty());
        try{
            SessionView v=SessionTestSupport.await(s,x->x.phase()==SessionPhase.ACTIVE);
            for(int i=0;i<5;i++){
                long g=v.generation();s.resume();SessionTestSupport.await(s,x->x.started());s.pause();
                SessionTestSupport.await(s,x->x.simulation().state()==SimulationState.PAUSED&&x.pendingActions()==0);
                s.restart();v=SessionTestSupport.await(s,x->x.generation()>g&&x.phase()==SessionPhase.ACTIVE);
                equal(0L,v.simulation().nextCycle());equal(SimulationState.PAUSED,v.simulation().state());
            }
            long a=s.addProcess(SimulationTest.cpu(1,1)),b=s.addProcess(SimulationTest.cpu(2,1));isTrue(a<b);
            SessionTestSupport.await(s,x->x.pendingActions()==0);s.step();v=SessionTestSupport.await(s,x->x.simulation().nextCycle()==1);
            equal(1L,v.simulation().computerAt(0).terminatedAt(0).id());equal(2L,v.simulation().computerAt(1).terminatedAt(0).id());
        }finally{SessionTestSupport.close(s);}
        Thread[] threads=new Thread[Thread.activeCount()+32];int count=Thread.enumerate(threads);int remaining=0;
        for(int i=0;i<count;i++)if(threads[i]!=null&&threads[i].isAlive()&&threads[i].getName().startsWith("avilaos-"))remaining++;
        equal(0,remaining);
    }
}
