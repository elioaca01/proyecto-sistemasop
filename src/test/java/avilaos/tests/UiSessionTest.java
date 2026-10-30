package avilaos.tests;
import avilaos.ui.session.*;
import avilaos.simulation.*;
import avilaos.process.*;
import avilaos.sync.*;
import avilaos.scheduling.SchedulingPolicyType;
import static avilaos.tests.TestSupport.*;
public final class UiSessionTest {
    public static void run(){
        UiSession s=new UiSession(SimulationConfigTest.empty());
        try{
            SessionView first=SessionTestSupport.await(s,v->v.phase()==SessionPhase.ACTIVE);
            equal(0L,first.simulation().nextCycle());equal(SimulationState.PAUSED,first.simulation().state());
            s.addBuffer(new BufferDefinition(1,1,1));s.addProcess(BufferSimulationTest.definition(1,ProcessType.PRODUCER));
            SessionView staged=SessionTestSupport.await(s,v->v.pendingActions()==0&&v.initialConfig().processCount()==1);
            equal(0L,staged.simulation().nextCycle());equal(0,staged.simulation().bufferCount());equal(1,staged.initialConfig().bufferCount());
            isTrue(staged.noticeAt(staged.noticeCount()-1).engineRequestId()>0);isTrue(staged.noticeAt(staged.noticeCount()-1).message().contains("Encolada"));
            s.step();SessionView one=SessionTestSupport.await(s,v->v.simulation()!=null&&v.simulation().nextCycle()==1);
            equal(1,one.simulation().computerAt(0).running().pc());isTrue(one.started());
            s.addProcess(SimulationTest.cpu(2,2));s.changeScheduling(1,SchedulingPolicyType.ROUND_ROBIN,1);s.changeCycleDuration(5);
            SessionTestSupport.await(s,v->v.pendingActions()==0);s.step();
            SessionView live=SessionTestSupport.await(s,v->v.simulation().nextCycle()==2);
            equal(1,live.initialConfig().processCount());equal(500L,live.initialConfig().cycleMillis());equal(5L,live.simulation().cycleMillis());
            equal(SchedulingPolicyType.ROUND_ROBIN,live.simulation().computerAt(0).policyType());
            long generation=live.generation();s.replaceScenario(SimulationConfigTest.empty());
            SessionView rejected=SessionTestSupport.await(s,v->v.pendingActions()==0&&v.noticeAt(v.noticeCount()-1).error());
            equal(generation,rejected.generation());equal(1,rejected.initialConfig().processCount());
            s.restart();SessionView reset=SessionTestSupport.await(s,v->v.generation()>generation&&v.phase()==SessionPhase.ACTIVE);
            equal(0L,reset.simulation().nextCycle());equal(500L,reset.simulation().cycleMillis());equal(SchedulingPolicyType.FCFS,reset.simulation().computerAt(0).policyType());isTrue(!reset.started());
            equal(2L,live.simulation().nextCycle());s.step();SessionView again=SessionTestSupport.await(s,v->v.simulation().nextCycle()==1);
            equal(1L,again.simulation().resultAt(1).processId());
        }finally{SessionTestSupport.close(s);}
        equal(SessionPhase.CLOSED,s.view().phase());throwsType(IllegalStateException.class,s::step);
        throwsType(IllegalArgumentException.class,()->new UiSession(null));
        UiSession editable=new UiSession(SimulationConfigTest.empty());
        try{
            SessionTestSupport.await(editable,v->v.phase()==SessionPhase.ACTIVE);editable.addBuffer(new BufferDefinition(1,1,6));editable.addBuffer(new BufferDefinition(2,1,5));
            SessionView invalid=SessionTestSupport.await(editable,v->v.pendingActions()==0);equal(1,invalid.initialConfig().bufferCount());isTrue(invalid.noticeAt(invalid.noticeCount()-1).error());
            editable.changeScheduling(1,SchedulingPolicyType.EDF,3);editable.changeCycleDuration(25);SessionTestSupport.await(editable,v->v.pendingActions()==0);
            editable.restart();SessionView updated=SessionTestSupport.await(editable,v->v.generation()>1&&v.phase()==SessionPhase.ACTIVE);
            equal(25L,updated.initialConfig().cycleMillis());equal(SchedulingPolicyType.EDF,updated.simulation().computerAt(0).policyType());
        }finally{SessionTestSupport.close(editable);}
    }
}
