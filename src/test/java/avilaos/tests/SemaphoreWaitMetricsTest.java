package avilaos.tests;

import avilaos.config.*;
import avilaos.metrics.SemaphoreWaitMetrics;
import avilaos.process.*;
import avilaos.simulation.*;
import avilaos.sync.BufferDefinition;
import java.io.IOException;
import java.nio.file.Path;
import static avilaos.tests.TestSupport.*;

public final class SemaphoreWaitMetricsTest {
    static SimulationConfig example(String name){try{return new JsonConfigurationStore().load(Path.of("examples",name+".json"));}catch(IOException e){throw new AssertionError(e);}}
    static Simulation create(SimulationConfig c){
        ComputerConfig[] machines=new ComputerConfig[c.computerCount()];for(int i=0;i<machines.length;i++)machines[i]=c.computerAt(i);
        Simulation sim=new Simulation(machines,c.cycleMillis(),c.networkLatency());
        for(int i=0;i<c.bufferCount();i++)sim.enqueue(new AddBufferCommand(c.bufferAt(i)));
        for(int i=0;i<c.processCount();i++)sim.enqueue(new AddProcessCommand(c.processAt(i)));
        return sim;
    }
    static SimulationSnapshot step(Simulation sim,int cycle){sim.step();return SimulationTest.await(sim,cycle,SimulationState.PAUSED);}
    static ProcessDefinition participant(String name,int computer,ProcessType type,long deadline){return new ProcessDefinition(name,computer,type,6,2,0,deadline,0,0,1,1,1);}
    static SimulationConfig config(int capacity,ProcessDefinition... processes){return new SimulationConfig(500,0,SimulationTest.configs(),new BufferDefinition[]{new BufferDefinition(1,1,capacity)},processes);}
    public static void run(){local();deadlineAndGrantedWake();remoteAndMutex();emptyAndCombine();}
    private static void local(){
        try(Simulation s=create(example("local-producer-consumer"))){
            SemaphoreWaitMetrics first=step(s,1).bufferAt(0).waits();equal(0L,first.completedEpisodes());equal(1L,first.openEpisodes());equal(0L,first.openCycles());isTrue(Double.isNaN(first.averageWait()));
            SemaphoreWaitMetrics second=step(s,2).bufferAt(0).waits();equal(1L,second.openCycles());
            for(int n=3;n<=12;n++)step(s,n);SemaphoreWaitMetrics end=s.snapshot().bufferAt(0).waits();equal(1L,end.completedEpisodes());equal(6L,end.completedCycles());equal(0L,end.openEpisodes());PerformanceMetricsTest.near(6,end.averageWait());equal(1L,first.openEpisodes());equal(0L,first.openCycles());
        }
    }
    private static void deadlineAndGrantedWake(){
        try(Simulation s=create(config(1,participant("C",1,ProcessType.CONSUMER,2)))){
            step(s,1);SemaphoreWaitMetrics m=step(s,2).bufferAt(0).waits();equal(1L,m.completedEpisodes());equal(1L,m.completedCycles());equal(0L,m.openEpisodes());equal(TerminationReason.DEADLINE_EXPIRED,s.snapshot().computerAt(0).terminatedAt(0).terminationReason());
        }
        try(Simulation s=create(config(1,participant("C",1,ProcessType.CONSUMER,7),participant("P",1,ProcessType.PRODUCER,100)))){
            for(int n=1;n<=8;n++)step(s,n);SemaphoreWaitMetrics m=s.snapshot().bufferAt(0).waits();equal(1L,m.completedEpisodes());equal(6L,m.completedCycles());equal(0L,m.openEpisodes());equal(1,s.snapshot().bufferAt(0).size());
        }
        try(Simulation s=create(config(1,participant("C",1,ProcessType.CONSUMER,1)))){
            SemaphoreWaitMetrics m=step(s,1).bufferAt(0).waits();equal(1L,m.completedEpisodes());equal(0L,m.completedCycles());PerformanceMetricsTest.near(0,m.averageWait());
        }
    }
    private static void remoteAndMutex(){
        try(Simulation s=create(example("remote-producer"))){for(int n=1;n<=8;n++)step(s,n);SemaphoreWaitMetrics m=s.snapshot().bufferAt(0).waits();equal(0L,m.completedEpisodes());equal(0L,m.openEpisodes());isTrue(Double.isNaN(m.averageWait()));}
        try(Simulation s=create(config(2,participant("P1",1,ProcessType.PRODUCER,100),participant("P2",2,ProcessType.PRODUCER,100)))){
            for(int n=1;n<=3;n++)step(s,n);equal(1L,s.snapshot().bufferAt(0).waits().openEpisodes());equal(0L,s.snapshot().bufferAt(0).waits().openCycles());
            equal(1L,step(s,4).bufferAt(0).waits().openCycles());for(int n=5;n<=8;n++)step(s,n);SemaphoreWaitMetrics m=s.snapshot().bufferAt(0).waits();equal(1L,m.completedEpisodes());equal(2L,m.completedCycles());equal(0L,m.openEpisodes());equal(2L,s.snapshot().bufferAt(0).produced());
        }
        try(Simulation s=create(config(1,participant("P1",1,ProcessType.PRODUCER,20),participant("P2",2,ProcessType.PRODUCER,20)))){
            for(int n=1;n<=20;n++)step(s,n);SemaphoreWaitMetrics m=s.snapshot().bufferAt(0).waits();equal(1L,m.completedEpisodes());equal(18L,m.completedCycles());equal(0L,m.openEpisodes());
        }
    }
    private static void emptyAndCombine(){
        SemaphoreWaitMetrics sum=SemaphoreWaitMetrics.combine(new SemaphoreWaitMetrics[]{new SemaphoreWaitMetrics(2,6,1,8),new SemaphoreWaitMetrics(1,0,2,3)});equal(3L,sum.completedEpisodes());equal(6L,sum.completedCycles());equal(3L,sum.openEpisodes());equal(11L,sum.openCycles());PerformanceMetricsTest.near(2,sum.averageWait());
        isTrue(Double.isNaN(SemaphoreWaitMetrics.combine(new SemaphoreWaitMetrics[0]).averageWait()));throwsType(IllegalArgumentException.class,()->new SemaphoreWaitMetrics(-1,0,0,0));
    }
}
