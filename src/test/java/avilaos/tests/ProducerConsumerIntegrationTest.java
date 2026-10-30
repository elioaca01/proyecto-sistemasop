package avilaos.tests;

import avilaos.process.*;
import avilaos.simulation.*;
import avilaos.scheduling.SchedulingPolicyType;
import avilaos.sync.*;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;
import static avilaos.tests.TestSupport.*;

/** Aceptación a través del reloj y los workers reales, observando solo valores publicados. */
public final class ProducerConsumerIntegrationTest {
    private ProducerConsumerIntegrationTest() { }
    public static void run() {
        localTrace(); remoteTraces(); crossComputerWakes(); quotasAndPolicies();
        producerDeadlines(); consumerDeadlines(); waitingDeadlines(); grantedBeforeWake();
        memoryAdmission(); criticalPreemption(); concurrentReader();
    }
    private static Simulation simulation(SchedulingPolicyType policy, int latency, int ram, int capacity) {
        Simulation s = new Simulation(new ComputerConfig[]{new ComputerConfig(1,ram,policy,1), new ComputerConfig(2,ram,policy,1)},100,latency);
        s.enqueue(new AddBufferCommand(new BufferDefinition(1,1,capacity))); return s;
    }
    private static ProcessDefinition definition(int computer, ProcessType type, int quota, int extra, long deadline) {
        return new ProcessDefinition(type.name(),computer,type,6*quota+extra,2,5,deadline,0,0,1,1,quota);
    }
    private static void add(Simulation s, int computer, ProcessType type, int quota, int extra, long deadline) {
        s.enqueue(new AddProcessCommand(definition(computer,type,quota,extra,deadline)));
    }
    private static SimulationSnapshot tick(Simulation s) {
        SimulationSnapshot value = BufferSimulationTest.step(s); invariants(value); return value;
    }
    private static ProcessSnapshot process(SimulationSnapshot s, long id) {
        for (int i=0;i<s.computerCount();i++) {
            ComputerSnapshot c=s.computerAt(i);
            if(c.running()!=null && c.running().id()==id) return c.running();
            for(int j=0;j<c.newCount();j++) if(c.newAt(j).id()==id) return c.newAt(j);
            for(int j=0;j<c.readyCount();j++) if(c.readyAt(j).id()==id) return c.readyAt(j);
            for(int j=0;j<c.blockedCount();j++) if(c.blockedAt(j).id()==id) return c.blockedAt(j);
            for(int j=0;j<c.terminatedCount();j++) if(c.terminatedAt(j).id()==id) return c.terminatedAt(j);
        }
        throw new AssertionError("Proceso ausente: "+id);
    }
    private static void localTrace() {
        try(Simulation s=simulation(SchedulingPolicyType.FCFS,0,20,1)) {
            add(s,1,ProcessType.CONSUMER,1,0,100); add(s,1,ProcessType.PRODUCER,1,0,100);
            long[] ids={1,2,2,2,2,2,2,1,1,1,1,1}; int[] pc={1,1,2,3,4,5,6,2,3,4,5,6};
            for(int t=0;t<12;t++) {
                SimulationSnapshot v=tick(s); equal(ids[t],v.computerAt(0).lastProcessId()); equal(pc[t],process(v,ids[t]).pc());
                if(t==6) { equal(ProcessState.BLOCKED,process(v,1).state()); equal(1,process(v,1).pc()); }
            }
            equal(0,s.snapshot().bufferAt(0).size()); equal(1L,s.snapshot().bufferAt(0).consumed());
        }
    }
    private static void remoteTraces() {
        for(int latency:new int[]{0,1,2,3}) {
            try(Simulation s=simulation(SchedulingPolicyType.FCFS,latency,20,1)) {
                add(s,2,ProcessType.PRODUCER,1,0,100);
                for(int t=0;t<6+latency;t++) {
                    SimulationSnapshot v=tick(s); ProcessSnapshot p=process(v,1);
                    int expected=t==0?1:(t<=latency?1:t-latency+1); equal(expected,p.pc());
                    if(t>=1&&t<=latency) {
                        equal(BlockReason.NETWORK,p.blockReason()); equal(1,v.bufferAt(0).emptyValue());
                        equal(-1L,v.bufferAt(0).mutexOwnerId()); equal(0L,p.kernelCycles());
                    }
                }
                equal(TerminationReason.COMPLETED,process(s.snapshot(),1).terminationReason()); equal(5L,process(s.snapshot(),1).kernelCycles());
                equal(1L,s.snapshot().bufferAt(0).produced());
            }
        }
        try(Simulation s=simulation(SchedulingPolicyType.FCFS,3,20,1)) {
            add(s,1,ProcessType.PRODUCER,1,0,100);
            for(int i=0;i<6;i++) tick(s);
            equal(5L,process(s.snapshot(),1).terminationCycle());
        }
        try(Simulation s=simulation(SchedulingPolicyType.FCFS,2,20,1)) {
            add(s,2,ProcessType.CONSUMER,1,0,100);
            tick(s); tick(s); SimulationSnapshot blocked=tick(s); equal(BlockReason.SEMAPHORE_FULL,process(blocked,1).blockReason());
            add(s,1,ProcessType.PRODUCER,1,0,100);
            int networkAgain=0;
            for(int i=0;i<13;i++) { SimulationSnapshot v=tick(s); if(process(v,1).blockReason()==BlockReason.NETWORK) networkAgain++; }
            equal(0,networkAgain); equal(TerminationReason.COMPLETED,process(s.snapshot(),1).terminationReason());
            equal(5L,process(s.snapshot(),1).kernelCycles());
        }
    }
    private static void crossComputerWakes() {
        for(int producer:new int[]{1,2}) {
            int consumer=3-producer;
            try(Simulation s=simulation(SchedulingPolicyType.FCFS,0,20,1)) {
                add(s,consumer,ProcessType.CONSUMER,1,0,100); add(s,producer,ProcessType.PRODUCER,1,0,100);
                for(int t=0;t<=5;t++) {
                    SimulationSnapshot v=tick(s);
                    if(t==5) { equal(ProcessState.BLOCKED,process(v,1).state()); equal(1,process(v,1).pc()); isTrue(process(v,1).operation().countingPermit()); }
                }
                SimulationSnapshot next=tick(s); equal(2,process(next,1).pc()); equal(6L,next.nextCycle()-1);
            }
        }
    }
    private static void quotasAndPolicies() {
        for(SchedulingPolicyType policy:SchedulingPolicyType.values()) {
            for(int latency:new int[]{0,2}) {
                try(Simulation s=simulation(policy,latency,40,1)) {
                    add(s,1,ProcessType.CONSUMER,2,3,300); add(s,1,ProcessType.PRODUCER,2,3,300);
                    add(s,2,ProcessType.PRODUCER,2,3,300); add(s,2,ProcessType.CONSUMER,2,3,300);
                    SimulationSnapshot v=s.snapshot();
                    for(int t=0;t<160;t++) {
                        v=tick(s);
                        if(v.computerAt(0).terminatedCount()+v.computerAt(1).terminatedCount()==4) break;
                    }
                    equal(4L,v.bufferAt(0).produced()); equal(4L,v.bufferAt(0).consumed()); equal(0,v.bufferAt(0).size());
                    for(long id=1;id<=4;id++) { ProcessSnapshot p=process(v,id); equal(TerminationReason.COMPLETED,p.terminationReason()); equal(10L,p.kernelCycles()); equal(5L,p.userCycles()); }
                }
            }
        }
        // Dos productores sin consumidores: los tokens preservan el orden de inserción.
        try(Simulation s=simulation(SchedulingPolicyType.FCFS,0,20,2)) {
            add(s,1,ProcessType.PRODUCER,1,0,100); add(s,1,ProcessType.PRODUCER,1,0,100);
            for(int i=0;i<12;i++) tick(s);
            equal(1L,s.snapshot().bufferAt(0).itemAt(0).producerId()); equal(2L,s.snapshot().bufferAt(0).itemAt(1).producerId());
        }
    }
    private static void producerDeadlines() {
        for(int deadline=1;deadline<=6;deadline++) {
            try(Simulation s=simulation(SchedulingPolicyType.FCFS,0,20,1)) {
                add(s,1,ProcessType.PRODUCER,1,1,deadline);
                for(int t=0;t<deadline+3;t++) tick(s);
                SimulationSnapshot v=s.snapshot(); ProcessSnapshot p=process(v,1); int committed=deadline>=4?1:0;
                equal(TerminationReason.DEADLINE_EXPIRED,p.terminationReason()); equal(deadline,p.pc()); equal(null,p.operation());
                equal(committed,v.bufferAt(0).size()); equal(committed,v.bufferAt(0).fullValue()); equal(1,v.bufferAt(0).mutexValue());
                equal(0,v.bufferAt(0).operationCount()); equal(1,v.computerAt(0).ramUsed());
            }
        }
    }
    private static void consumerDeadlines() {
        for(int deadline=1;deadline<=5;deadline++) {
            try(Simulation s=simulation(SchedulingPolicyType.FCFS,0,20,1)) {
                add(s,1,ProcessType.PRODUCER,1,0,100); for(int i=0;i<6;i++) tick(s);
                add(s,1,ProcessType.CONSUMER,1,0,deadline);
                for(int i=0;i<deadline+3;i++) tick(s);
                SimulationSnapshot v=s.snapshot(); ProcessSnapshot p=process(v,2); int remaining=deadline>=3?0:1;
                equal(TerminationReason.DEADLINE_EXPIRED,p.terminationReason()); equal(deadline,p.pc()); equal(null,p.operation());
                equal(remaining,v.bufferAt(0).size()); equal(remaining,v.bufferAt(0).fullValue()); equal(1,v.bufferAt(0).mutexValue());
            }
        }
    }
    private static void waitingDeadlines() {
        for(int computer:new int[]{1,2}) {
            try(Simulation s=simulation(SchedulingPolicyType.FCFS,3,20,1)) {
                add(s,computer,ProcessType.CONSUMER,1,0,2); for(int i=0;i<7;i++) tick(s);
                ProcessSnapshot p=process(s.snapshot(),1); equal(TerminationReason.DEADLINE_EXPIRED,p.terminationReason());
                equal(computer==1?1:0,p.pc()); equal(0,s.snapshot().bufferAt(0).fullWaitingCount());
            }
        }
        try(Simulation s=simulation(SchedulingPolicyType.FCFS,0,20,1)) {
            add(s,1,ProcessType.PRODUCER,1,0,100); add(s,1,ProcessType.PRODUCER,1,0,8);
            for(int i=0;i<12;i++) tick(s);
            equal(TerminationReason.DEADLINE_EXPIRED,process(s.snapshot(),2).terminationReason()); equal(2,process(s.snapshot(),2).pc());
            equal(0,s.snapshot().bufferAt(0).emptyWaitingCount()); equal(1,s.snapshot().bufferAt(0).fullValue());
        }
        // RR fuerza espera por mutex; el vencido devuelve su reserva de vacíos.
        for(int deadline:new int[]{6,8}) {
            try(Simulation s=simulation(SchedulingPolicyType.ROUND_ROBIN,0,20,2)) {
                add(s,1,ProcessType.PRODUCER,1,0,100); add(s,1,ProcessType.PRODUCER,1,0,deadline);
                SimulationSnapshot waiting=null;
                for(int t=0;t<12;t++) { SimulationSnapshot v=tick(s); if(t==5) waiting=v; }
                equal(3,process(waiting,2).pc());
                if(deadline==8) { equal(BlockReason.SEMAPHORE_MUTEX,process(waiting,2).blockReason()); }
                equal(TerminationReason.DEADLINE_EXPIRED,process(s.snapshot(),2).terminationReason());
                equal(1,s.snapshot().bufferAt(0).size()); equal(1,s.snapshot().bufferAt(0).emptyValue()); equal(1,s.snapshot().bufferAt(0).mutexValue());
            }
        }
    }
    private static void grantedBeforeWake() {
        try(Simulation s=simulation(SchedulingPolicyType.FCFS,0,20,1)) {
            add(s,2,ProcessType.CONSUMER,1,0,6); add(s,1,ProcessType.PRODUCER,1,0,100);
            for(int i=0;i<10;i++) tick(s);
            equal(TerminationReason.DEADLINE_EXPIRED,process(s.snapshot(),1).terminationReason()); equal(1,process(s.snapshot(),1).pc());
            equal(1,s.snapshot().bufferAt(0).fullValue()); equal(1,s.snapshot().bufferAt(0).size()); equal(0,s.snapshot().bufferAt(0).operationCount());
        }
    }
    private static ProcessDefinition cpu(int instructions,int memory,long deadline,int priority) {
        return new ProcessDefinition("cpu",1,ProcessType.CPU_BOUND,instructions,memory,priority,deadline,0,0,0,0,0);
    }
    private static void memoryAdmission() {
        try(Simulation s=simulation(SchedulingPolicyType.FCFS,0,5,3)) {
            s.enqueue(new AddProcessCommand(cpu(2,2,100,0)));
            add(s,1,ProcessType.PRODUCER,1,0,1); add(s,1,ProcessType.PRODUCER,1,0,100);
            SimulationSnapshot a=tick(s); equal(TerminationReason.DEADLINE_EXPIRED,process(a,2).terminationReason()); equal(0,process(a,2).pc());
            equal(ProcessState.NEW,process(a,3).state()); equal(5,a.computerAt(0).ramUsed());
            SimulationSnapshot b=tick(s); equal(ProcessState.READY,process(b,3).state()); equal(0,process(b,3).pc());
            for(int i=0;i<6;i++) tick(s);
            equal(TerminationReason.COMPLETED,process(s.snapshot(),3).terminationReason()); equal(3,s.snapshot().computerAt(0).ramUsed());
        }
    }
    private static void criticalPreemption() {
        for(SchedulingPolicyType policy:new SchedulingPolicyType[]{SchedulingPolicyType.EDF,SchedulingPolicyType.PREEMPTIVE_PRIORITY}) {
            for(ProcessType type:new ProcessType[]{ProcessType.PRODUCER,ProcessType.CONSUMER}) {
                for(int phase=1;phase<=5;phase++) {
                    try(Simulation s=simulation(policy,0,20,1)) {
                        long id=1;
                        if(type==ProcessType.CONSUMER) {
                            add(s,1,ProcessType.PRODUCER,1,0,100); for(int i=0;i<6;i++) tick(s); id=2;
                        }
                        add(s,1,type,1,0,100); for(int i=0;i<phase;i++) tick(s);
                        BufferOperationSnapshot before=process(s.snapshot(),id).operation();
                        s.enqueue(new AddProcessCommand(cpu(1,2,10,0)));
                        SimulationSnapshot preempted=tick(s);
                        equal(id+1,preempted.computerAt(0).lastProcessId()); equal(phase,process(preempted,id).pc());
                        equal(ProcessState.READY,process(preempted,id).state()); equal(before,process(preempted,id).operation());
                        for(int i=phase;i<6;i++) tick(s);
                        equal(TerminationReason.COMPLETED,process(s.snapshot(),id).terminationReason()); equal(1L,s.snapshot().bufferAt(0).produced());
                        equal(type==ProcessType.PRODUCER?0L:1L,s.snapshot().bufferAt(0).consumed());
                    }
                }
            }
        }
    }
    private static void concurrentReader() {
        Simulation s=simulation(SchedulingPolicyType.ROUND_ROBIN,2,40,1);
        Semaphore ready=new Semaphore(0), observed=new Semaphore(0); Throwable[] failure={null};
        Thread reader=new Thread(() -> {
            try {
                SimulationSnapshot v=s.snapshot(); ready.release();
                while(v.state()!=SimulationState.STOPPED) {
                    BufferSnapshot retained=v.bufferCount()==0?null:v.bufferAt(0);
                    int oldSize=retained==null?0:retained.size();
                    long oldProduced=retained==null?0:retained.produced();
                    SimulationSnapshot next=s.awaitNextSnapshot(v.version(),2000);
                    if(next==null) throw new AssertionError("Lector sin publicación");
                    if(next.version()<=v.version()) throw new AssertionError("Versión no creciente");
                    if(retained!=null && (retained.size()!=oldSize || retained.produced()!=oldProduced)) throw new AssertionError("Captura mutable");
                    v=next; observed.release();
                }
            } catch(Throwable e) { failure[0]=e; }
        },"buffer-snapshot-reader");
        reader.start();
        try {
            isTrue(ready.tryAcquire(2,TimeUnit.SECONDS));
            add(s,1,ProcessType.PRODUCER,2,0,100); add(s,2,ProcessType.CONSUMER,2,0,100);
            for(int i=0;i<30;i++) tick(s);
            isTrue(observed.tryAcquire(2,TimeUnit.SECONDS)); s.stop(); isTrue(s.awaitStopped(2000));
            reader.join(2000); isTrue(!reader.isAlive()); equal(null,failure[0]);
        } catch(InterruptedException e) { Thread.currentThread().interrupt(); throw new AssertionError(e); }
        finally { s.close(); reader.interrupt(); try { reader.join(2000); } catch(InterruptedException e) { Thread.currentThread().interrupt(); } }
    }
    private static void invariants(SimulationSnapshot s) {
        for(int b=0;b<s.bufferCount();b++) {
            BufferSnapshot v=s.bufferAt(b); int empty=v.emptyValue(),full=v.fullValue(),owners=0;
            isTrue(v.size()>=0 && v.size()<=v.definition().capacity()); equal(v.produced()-v.consumed(),(long)v.size());
            for(int i=0;i<v.operationCount();i++) {
                BufferOperationSnapshot op=v.operationAt(i); ProcessSnapshot p=process(s,op.processId());
                isTrue(p.state()!=ProcessState.TERMINATED); equal(op,p.operation());
                if(p.definition().type()==ProcessType.PRODUCER) { if(op.countingPermit()) empty++; if(op.modified()) full++; }
                else { if(op.countingPermit()) full++; if(op.modified()) empty++; }
                if(op.mutexHeld()) { owners++; equal(op.processId(),v.mutexOwnerId()); }
                if(op.waitingReason()==BlockReason.NETWORK) { isTrue(!op.countingPermit()&&!op.mutexHeld()&&!op.modified()); }
            }
            equal(v.definition().capacity()-v.size(),empty); equal(v.size(),full); equal(1,v.mutexValue()+owners); equal(v.mutexOwnerId()==-1?0:1,owners);
            for(int kind=0;kind<3;kind++) {
                int count=kind==0?v.emptyWaitingCount():kind==1?v.fullWaitingCount():v.mutexWaitingCount();
                for(int i=0;i<count;i++) {
                    long id=kind==0?v.emptyWaitingAt(i):kind==1?v.fullWaitingAt(i):v.mutexWaitingAt(i); ProcessSnapshot p=process(s,id);
                    equal(ProcessState.BLOCKED,p.state()); equal(kind==0?BlockReason.SEMAPHORE_EMPTY:kind==1?BlockReason.SEMAPHORE_FULL:BlockReason.SEMAPHORE_MUTEX,p.blockReason());
                    for(int j=i+1;j<count;j++) { long other=kind==0?v.emptyWaitingAt(j):kind==1?v.fullWaitingAt(j):v.mutexWaitingAt(j); isTrue(id!=other); }
                }
            }
        }
        for(int i=0;i<s.computerCount();i++) {
            ComputerSnapshot c=s.computerAt(i); ProcessSnapshot[] all=new ProcessSnapshot[c.newCount()+c.readyCount()+c.blockedCount()+c.terminatedCount()+(c.running()==null?0:1)]; int n=0;
            if(c.running()!=null) { equal(ProcessState.RUNNING,c.running().state()); all[n++]=c.running(); }
            for(int j=0;j<c.newCount();j++) { equal(ProcessState.NEW,c.newAt(j).state()); all[n++]=c.newAt(j); }
            for(int j=0;j<c.readyCount();j++) { equal(ProcessState.READY,c.readyAt(j).state()); all[n++]=c.readyAt(j); }
            for(int j=0;j<c.blockedCount();j++) { equal(ProcessState.BLOCKED,c.blockedAt(j).state()); all[n++]=c.blockedAt(j); }
            for(int j=0;j<c.terminatedCount();j++) { equal(ProcessState.TERMINATED,c.terminatedAt(j).state()); equal(null,c.terminatedAt(j).operation()); all[n++]=c.terminatedAt(j); }
            for(int j=0;j<all.length;j++) { ProcessSnapshot p=all[j]; equal(p.pc(),p.mar()); equal((long)p.pc(),p.userCycles()+p.kernelCycles()); for(int k=j+1;k<all.length;k++) isTrue(p.id()!=all[k].id()); }
            equal(c.ramCapacity(),c.ramUsed()+c.ramFree()); equal(s.nextCycle(),c.nextCycle());
        }
    }
}
