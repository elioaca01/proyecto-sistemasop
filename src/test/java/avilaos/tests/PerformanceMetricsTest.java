package avilaos.tests;

import avilaos.metrics.*;
import avilaos.hardware.CpuMode;
import avilaos.process.*;
import avilaos.simulation.*;
import static avilaos.tests.TestSupport.*;

public final class PerformanceMetricsTest {
    static void near(double expected,double actual){isTrue(Double.isFinite(actual)&&Math.abs(expected-actual)<1e-10);}
    public static void run(){admission();smallTrace();denominatorsAndAggregation();}
    private static void admission(){
        ProcessControlBlock p=new ProcessControlBlock(1,SimulationTest.cpu(1,1),0);equal(false,p.everAdmitted());p.admit();equal(true,p.everAdmitted());p.dispatch(0);p.executeInstruction(CpuMode.USER);p.terminate(TerminationReason.COMPLETED,0);equal(0,p.assignedMemory());equal(true,ProcessSnapshot.capture(p).everAdmitted());
        ProcessControlBlock waiting=new ProcessControlBlock(2,SimulationTest.cpu(1,1),0);waiting.terminate(TerminationReason.DEADLINE_EXPIRED,19);equal(false,ProcessSnapshot.capture(waiting).everAdmitted());
    }
    private static void smallTrace(){
        Computer c=new Computer(1,10);c.submit(new ProcessControlBlock(1,SimulationTest.cpu(1,2),0));c.submit(new ProcessControlBlock(2,SimulationTest.cpu(1,1),0));c.onTick(0);
        PerformanceMetrics one=MetricsCalculator.computer(ComputerSnapshot.capture(c),1);equal(2L,one.admitted());near(.5,one.fairness());near(0,one.averageResponse());equal(1L,one.responded());equal(2L,one.pending());
        c.onTick(1);c.onTick(2);PerformanceMetrics done=MetricsCalculator.computer(ComputerSnapshot.capture(c),3);
        equal(2L,done.completed());equal(0L,done.expired());equal(0L,done.pending());equal(3L,done.userCycles());equal(0L,done.kernelCycles());near(2.0/3,done.throughput());near(1,done.utilization());near(1,done.averageResponse());near(.9,done.fairness());near(1,done.deadlineCompliance());
        Computer constrained=new Computer(1,4);constrained.submit(new ProcessControlBlock(1,SimulationTest.cpu(1,4),0));constrained.submit(new ProcessControlBlock(2,new ProcessDefinition("venceNEW",1,ProcessType.CPU_BOUND,1,4,0,1,0,0,0,0,0),0));constrained.onTick(0);
        PerformanceMetrics limited=MetricsCalculator.computer(ComputerSnapshot.capture(constrained),1);equal(1L,limited.admitted());equal(1L,limited.expired());equal(1L,limited.pending());near(1,limited.fairness());near(0,limited.deadlineCompliance());
    }
    private static void denominatorsAndAggregation(){
        PerformanceMetrics zero=new PerformanceMetrics(0,1,0,0,0,0,0,0,0,0,0);
        for(double v:new double[]{zero.throughput(),zero.utilization(),zero.averageResponse(),zero.deadlineCompliance(),zero.fairness()})isTrue(Double.isNaN(v));
        PerformanceMetrics empty=new PerformanceMetrics(8,1,0,0,0,0,0,0,0,0,0);near(0,empty.throughput());near(0,empty.utilization());isTrue(Double.isNaN(empty.fairness()));
        PerformanceMetrics a=new PerformanceMetrics(4,1,3,1,1,1,2,1,0,2,10);
        PerformanceMetrics b=new PerformanceMetrics(4,1,1,0,1,0,1,3,12,3,1);
        PerformanceMetrics total=MetricsCalculator.aggregate(new PerformanceMetrics[]{a,b},4);
        equal(2,total.processors());equal(4L,total.userCycles());equal(1L,total.kernelCycles());equal(2L,total.completed());equal(1L,total.expired());equal(3L,total.pending());near(.5,total.throughput());near(5.0/8,total.utilization());near(.5,total.userUtilization());near(1.0/8,total.kernelUtilization());near(3,total.averageResponse());near(2.0/3,total.deadlineCompliance());near(25.0/55,total.fairness());
        PerformanceMetrics big=new PerformanceMetrics(Long.MAX_VALUE,2,4_000_000_000L,0,0,0,1,0,0,1,16e18);near(1,big.fairness());isTrue(Double.isFinite(big.utilization())&&big.utilization()>0);
        throwsType(IllegalArgumentException.class,()->new PerformanceMetrics(-1,1,0,0,0,0,0,0,0,0,0));
    }
}
