package avilaos.tests;

import avilaos.hardware.*;
import avilaos.kernel.Kernel;
import avilaos.process.*;
import avilaos.scheduling.*;

public final class PreemptiveKernelTest {
    static ProcessControlBlock process(long id, int computer, int instructions, int memory,
            int priority, long deadline, long arrival, boolean io) {
        return new ProcessControlBlock(id, new ProcessDefinition("P" + id, computer,
                io ? ProcessType.IO_BOUND : ProcessType.CPU_BOUND, instructions, memory, priority,
                deadline, io ? 1 : 0, io ? 2 : 0, 0, 0, 0), arrival);
    }
    private static final class Rig {
        final Cpu cpu = new Cpu(); final Ram ram = new Ram(10); final IoController io = new IoController();
        final Kernel kernel;
        Rig(SchedulingPolicyType type, int quantum) {
            kernel = new Kernel(1, cpu, ram, io, SchedulingPolicies.create(type), quantum);
        }
        void tick(long cycle, long expectedId) {
            kernel.onTick(cycle); TestSupport.equal(expectedId, cpu.lastProcessId());
        }
    }
    public static void run() {
        betterArrivalPreempts(); tiesKeepCurrent(); roundRobinTurns(); ioCompletionPreempts();
        TestSupport.throwsType(IllegalArgumentException.class,
                () -> new Kernel(1, new Cpu(), new Ram(10), new IoController(), new FcfsPolicy(), 0));
    }
    private static void betterArrivalPreempts() {
        for (SchedulingPolicyType type : new SchedulingPolicyType[]{SchedulingPolicyType.EDF, SchedulingPolicyType.PREEMPTIVE_PRIORITY}) {
            Rig r = new Rig(type, 2);
            ProcessControlBlock a = process(1, 1, 4, 6, 5, 20, 0, false);
            ProcessControlBlock b = process(2, 1, 1, 4, 1, type == SchedulingPolicyType.EDF ? 3 : 20, 1, false);
            r.kernel.submit(a); r.tick(0, 1); r.kernel.submit(b); r.tick(1, 2);
            TestSupport.equal(ProcessState.READY, a.state());
            TestSupport.equal(1, a.pc()); TestSupport.equal(1, a.mar());
            TestSupport.equal(6, a.assignedMemory()); TestSupport.equal(6, r.ram.used());
            TestSupport.equal(0L, a.firstRunCycle()); TestSupport.equal(1L, b.firstRunCycle());
            TestSupport.equal(type, r.kernel.policyType()); TestSupport.equal(2, r.kernel.quantum());
            r.tick(2, 1); r.tick(3, 1); r.tick(4, 1);
            TestSupport.equal(4, a.pc()); TestSupport.equal(0L, a.firstRunCycle());
            TestSupport.equal(10, r.ram.free()); TestSupport.equal(2, r.kernel.terminatedCount());
        }
    }
    private static void tiesKeepCurrent() {
        for (SchedulingPolicyType type : new SchedulingPolicyType[]{SchedulingPolicyType.EDF, SchedulingPolicyType.PREEMPTIVE_PRIORITY}) {
            Rig r = new Rig(type, 2);
            ProcessControlBlock a = process(1, 1, 3, 4, 1, 10, 0, false);
            ProcessControlBlock b = process(2, 1, 1, 4, 1, 9, 1, false);
            r.kernel.submit(a); r.tick(0, 1); r.kernel.submit(b); r.tick(1, 1);
            TestSupport.equal(ProcessState.RUNNING, a.state()); TestSupport.equal(0, b.pc());
            r.tick(2, 1); r.tick(3, 2); TestSupport.equal(10, r.ram.free());
        }
    }
    private static void roundRobinTurns() {
        for (int quantum : new int[]{1, 2}) {
            Rig r = new Rig(SchedulingPolicyType.ROUND_ROBIN, quantum);
            ProcessControlBlock a = process(1, 1, 3, 6, 0, 20, 0, false);
            ProcessControlBlock b = process(2, 1, 2, 4, 0, 20, 0, false);
            r.kernel.submit(a); r.kernel.submit(b);
            long[] order = quantum == 1 ? new long[]{1, 2, 1, 2, 1} : new long[]{1, 1, 2, 2, 1};
            for (int i = 0; i < order.length; i++) {
                r.tick(i, order[i]);
                TestSupport.equal(i + 1L, a.userCycles() + b.userCycles());
                TestSupport.equal(a.pc(), a.mar()); TestSupport.equal(b.pc(), b.mar());
                if (i < 3) { TestSupport.equal(10, r.ram.used()); }
            }
            TestSupport.equal(0L, a.firstRunCycle()); TestSupport.equal((long) quantum, b.firstRunCycle());
            TestSupport.equal(10, r.ram.free()); TestSupport.equal(0, r.kernel.sliceUsed());
        }
        Rig alone = new Rig(SchedulingPolicyType.ROUND_ROBIN, 2);
        ProcessControlBlock p = process(1, 1, 5, 6, 0, 5, 0, false); alone.kernel.submit(p);
        for (int i = 0; i < 5; i++) { alone.tick(i, 1); TestSupport.equal(i + 1, p.pc()); }
        TestSupport.equal(TerminationReason.COMPLETED, p.terminationReason());
        TestSupport.equal(0L, p.firstRunCycle()); TestSupport.equal(10, alone.ram.free());
        Rig fcfs = new Rig(SchedulingPolicyType.FCFS, 1);
        fcfs.kernel.submit(process(1, 1, 2, 4, 0, 10, 0, false));
        fcfs.kernel.submit(process(2, 1, 1, 4, 0, 10, 0, false));
        fcfs.tick(0, 1); fcfs.tick(1, 1); fcfs.tick(2, 2);
    }
    private static void ioCompletionPreempts() {
        for (SchedulingPolicyType type : new SchedulingPolicyType[]{SchedulingPolicyType.EDF, SchedulingPolicyType.PREEMPTIVE_PRIORITY}) {
            Rig r = new Rig(type, 2);
            ProcessControlBlock a = process(1, 1, 3, 6, 0, 10, 0, true);
            ProcessControlBlock b = process(2, 1, 5, 4, 5, 20, 0, false);
            r.kernel.submit(a); r.kernel.submit(b);
            r.tick(0, 1); r.tick(1, 1);
            TestSupport.equal(ProcessState.BLOCKED, a.state()); TestSupport.equal(0, r.kernel.sliceUsed());
            r.tick(2, 2); r.tick(3, 2); r.tick(4, 1);
            TestSupport.equal(ProcessState.READY, b.state()); TestSupport.equal(2, b.pc());
            TestSupport.equal(2, b.mar()); TestSupport.equal(4, b.assignedMemory());
            TestSupport.equal(4, r.ram.used()); TestSupport.equal(0, r.io.pendingCount());
            TestSupport.equal(4L, a.terminationCycle()); TestSupport.equal(2L, b.firstRunCycle());
            r.tick(5, 2); r.tick(6, 2); r.tick(7, 2); TestSupport.equal(10, r.ram.free());
        }
    }
}
