package avilaos.tests;

import avilaos.metrics.*;
import avilaos.simulation.*;
import avilaos.scheduling.SchedulingPolicyType;
import avilaos.sync.BufferSnapshot;
import static avilaos.tests.TestSupport.*;

public final class MetricsHistoryTest {
    public static void run(){confirmedCycles();failure();defensiveHistory();}
    private static void confirmedCycles(){
        Simulation s=new Simulation(new ComputerConfig[]{new ComputerConfig(9,10,SchedulingPolicyType.FCFS,2),new ComputerConfig(2,10,SchedulingPolicyType.FCFS,2)},500);
        try{
            equal(0,s.snapshot().sampleCount());isTrue(Double.isNaN(s.snapshot().metrics().global().utilization()));
            s.enqueue(new AddProcessCommand(SimulationTest.cpu(2,2)));SimulationSnapshot first=SemaphoreWaitMetricsTest.step(s,1);equal(1,first.sampleCount());equal(1L,first.metrics().global().cycles());PerformanceMetricsTest.near(.5,first.metrics().global().utilization());
            long version=first.version();s.pause();SimulationSnapshot pause=s.awaitNextSnapshot(version,2000);isTrue(pause!=null);equal(1,pause.sampleCount());
            SemaphoreWaitMetricsTest.step(s,2);SimulationSnapshot third=SemaphoreWaitMetricsTest.step(s,3);equal(3,third.sampleCount());
            double[] expected={1,1,2.0/3};for(int i=0;i<3;i++){UtilizationSample sample=third.sampleAt(i);equal(i+1L,sample.cycle());equal(2,sample.computerIdAt(0));equal(9,sample.computerIdAt(1));PerformanceMetricsTest.near(expected[i],sample.utilizationAt(0));PerformanceMetricsTest.near(0,sample.utilizationAt(1));}
            equal(1,first.sampleCount());PerformanceMetricsTest.near(1,first.sampleAt(0).utilizationAt(0));equal(2,third.metrics().computerIdAt(0));equal(9,third.metrics().computerIdAt(1));
            s.stop();isTrue(SimulationTest.stopped(s));equal(3,s.snapshot().sampleCount());equal(3L,s.snapshot().metrics().global().cycles());
        }catch(InterruptedException e){throw new AssertionError(e);}finally{s.close();}
    }
    private static void failure(){
        try(Simulation s=new Simulation(SimulationTest.configs(),500)){
            s.enqueue(new AddProcessCommand(SimulationTest.cpu(1,3)));SimulationSnapshot before=SemaphoreWaitMetricsTest.step(s,1);var field=Simulation.class.getDeclaredField("workers");field.setAccessible(true);((ComputerWorker[])field.get(s))[1].close();s.step();SimulationSnapshot failed=SimulationTest.await(s,1,SimulationState.FAILED);
            equal(1L,failed.nextCycle());equal(1,failed.sampleCount());equal(before.metrics().global(),failed.metrics().global());equal(1L,failed.metrics().global().userCycles());equal(2L,failed.computerAt(0).nextCycle());
        }catch(ReflectiveOperationException e){throw new AssertionError(e);}
    }
    private static void defensiveHistory(){
        int[] ids={2,9};double[] values={.25,.5};UtilizationSample sample=new UtilizationSample(1,ids,values);ids[0]=7;values[0]=1;equal(2,sample.computerIdAt(0));PerformanceMetricsTest.near(.25,sample.utilizationAt(0));
        throwsType(IllegalArgumentException.class,()->new UtilizationSample(0,new int[]{2},new double[]{0}));throwsType(IllegalArgumentException.class,()->new UtilizationSample(1,new int[]{2},new double[]{Double.NaN}));
        ComputerSnapshot[] cs={ComputerSnapshot.capture(new Computer(2,10))};MetricsSnapshot one=MetricsSnapshot.calculate(1,cs,new BufferSnapshot[0]),two=MetricsSnapshot.calculate(2,cs,new BufferSnapshot[0]);
        UtilizationHistory h=new UtilizationHistory();throwsType(IllegalArgumentException.class,()->h.record(two));h.record(one);throwsType(IllegalArgumentException.class,()->h.record(one));UtilizationSample[] copied=h.copy();
        SimulationSnapshot snapshot=new SimulationSnapshot(1,1,SimulationState.PAUSED,500,null,cs,new CommandResult[0],0,new BufferSnapshot[0],new SimulationEvent[0],one,copied);
        copied[0]=sample;cs[0]=ComputerSnapshot.capture(new Computer(8,10));equal(2,h.copy()[0].computerIdAt(0));equal(2,snapshot.sampleAt(0).computerIdAt(0));PerformanceMetricsTest.near(0,snapshot.sampleAt(0).utilizationAt(0));equal(2,one.computerIdAt(0));h.record(two);equal(1,snapshot.sampleCount());equal(2,h.copy().length);
    }
}
