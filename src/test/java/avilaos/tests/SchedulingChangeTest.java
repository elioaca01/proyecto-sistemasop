package avilaos.tests;

import avilaos.hardware.CpuMode;
import avilaos.process.*;
import avilaos.scheduling.SchedulingPolicyType;
import avilaos.simulation.Computer;

public final class SchedulingChangeTest {
    private static ProcessControlBlock cpu(long id, int instructions, int priority, long deadline) {
        return PreemptiveKernelTest.process(id, 1, instructions, 4, priority, deadline, 0, false);
    }
    public static void run() {
        changesAtBoundary(); restoresFifo(); restartsSlice(); rejectsWithoutLosingPending();
        ioAtSliceBoundary(false); ioAtSliceBoundary(true); independentAndIdle();
    }
    private static void changesAtBoundary() {
        Computer c = new Computer(1, 20);
        ProcessControlBlock a = cpu(1, 4, 5, 20), b = cpu(2, 1, 1, 5);
        c.submit(a); c.submit(b); c.onTick(0);
        TestSupport.equal(SchedulingPolicyType.FCFS, c.policyType()); TestSupport.equal(2, c.quantum());
        TestSupport.equal(1, c.sliceUsed());
        c.configureScheduling(SchedulingPolicyType.EDF, 2);
        TestSupport.equal(SchedulingPolicyType.FCFS, c.policyType()); TestSupport.equal(1, a.pc());
        c.onTick(1);
        TestSupport.equal(SchedulingPolicyType.EDF, c.policyType()); TestSupport.equal(2L, c.cpu().lastProcessId());
        TestSupport.equal(ProcessState.READY, a.state()); TestSupport.equal(1, a.pc());
        c.onTick(2); TestSupport.equal(1L, c.cpu().lastProcessId());
        ProcessControlBlock urgent = PreemptiveKernelTest.process(3, 1, 1, 4, 0, 1, 3, false);
        c.submit(urgent); c.configureScheduling(SchedulingPolicyType.FCFS, 1);
        c.onTick(3);
        TestSupport.equal(SchedulingPolicyType.FCFS, c.policyType()); TestSupport.equal(1L, c.cpu().lastProcessId());
        TestSupport.equal(TerminationReason.DEADLINE_EXPIRED, urgent.terminationReason());
        TestSupport.equal(0, urgent.pc()); TestSupport.equal(0L, a.firstRunCycle());
        c.onTick(4); TestSupport.equal(TerminationReason.COMPLETED, a.terminationReason());
        TestSupport.equal(20, c.ram().free());
    }
    private static void restoresFifo() {
        for (SchedulingPolicyType target : new SchedulingPolicyType[]{SchedulingPolicyType.FCFS, SchedulingPolicyType.ROUND_ROBIN}) {
            Computer c = new Computer(1, 20, SchedulingPolicyType.PREEMPTIVE_PRIORITY, 2);
            ProcessControlBlock a = cpu(30, 1, 0, 20), b = cpu(10, 3, 5, 20), small = cpu(20, 1, 1, 20);
            c.submit(a); c.submit(b); c.submit(small); c.onTick(0);
            TestSupport.equal(small, c.kernel().readyAt(0)); TestSupport.equal(b, c.kernel().readyAt(1));
            c.configureScheduling(target, 1); c.onTick(1);
            TestSupport.equal(10L, c.cpu().lastProcessId()); TestSupport.equal(1, b.pc());
            TestSupport.equal(0, small.pc()); TestSupport.equal(target, c.policyType());
        }
    }
    private static void restartsSlice() {
        Computer c = new Computer(1, 20, SchedulingPolicyType.ROUND_ROBIN, 2);
        ProcessControlBlock a = cpu(1, 4, 0, 20), b = cpu(2, 4, 0, 20);
        c.submit(a); c.submit(b); c.onTick(0);
        c.configureScheduling(SchedulingPolicyType.ROUND_ROBIN, 1);
        TestSupport.equal(2, c.quantum()); c.onTick(1);
        TestSupport.equal(1, c.quantum()); TestSupport.equal(1L, c.cpu().lastProcessId());
        TestSupport.equal(1, c.sliceUsed()); c.onTick(2);
        TestSupport.equal(2L, c.cpu().lastProcessId()); TestSupport.equal(1, c.sliceUsed());
        c.configureScheduling(SchedulingPolicyType.ROUND_ROBIN, 2); c.onTick(3);
        TestSupport.equal(2L, c.cpu().lastProcessId()); TestSupport.equal(1, c.sliceUsed());
        c.onTick(4); TestSupport.equal(2L, c.cpu().lastProcessId()); TestSupport.equal(2, c.sliceUsed());
        c.onTick(5); TestSupport.equal(1L, c.cpu().lastProcessId());
        TestSupport.equal(0L, a.firstRunCycle()); TestSupport.equal(2L, b.firstRunCycle());
        c.onTick(6); c.onTick(7); TestSupport.equal(20, c.ram().free());
    }
    private static void rejectsWithoutLosingPending() {
        Computer c = new Computer(1, 20);
        ProcessControlBlock a = cpu(1, 4, 5, 20), b = cpu(2, 1, 1, 5);
        c.submit(a); c.submit(b); c.onTick(0); c.configureScheduling(SchedulingPolicyType.EDF, 3);
        TestSupport.throwsType(IllegalArgumentException.class, () -> c.configureScheduling(null, 2));
        TestSupport.throwsType(IllegalArgumentException.class, () -> c.configureScheduling(SchedulingPolicyType.FCFS, 0));
        TestSupport.throwsType(IllegalArgumentException.class, () -> c.configureScheduling(SchedulingPolicyType.FCFS, -1));
        for (long bad : new long[]{-1, 0, 2, Long.MAX_VALUE}) {
            TestSupport.throwsType(IllegalArgumentException.class, () -> c.onTick(bad));
            TestSupport.equal(1, a.pc()); TestSupport.equal(0, b.pc());
            TestSupport.equal(8, c.ram().used()); TestSupport.equal(1, c.sliceUsed());
            TestSupport.equal(SchedulingPolicyType.FCFS, c.policyType()); TestSupport.equal(2, c.quantum());
            TestSupport.equal(1L, c.kernel().nextCycle()); TestSupport.equal(b, c.kernel().readyAt(0));
        }
        c.onTick(1); TestSupport.equal(SchedulingPolicyType.EDF, c.policyType());
        TestSupport.equal(3, c.quantum()); TestSupport.equal(2L, c.cpu().lastProcessId());
        c.configureScheduling(SchedulingPolicyType.ROUND_ROBIN, 1);
        c.configureScheduling(SchedulingPolicyType.PREEMPTIVE_PRIORITY, 4); c.onTick(2);
        TestSupport.equal(SchedulingPolicyType.PREEMPTIVE_PRIORITY, c.policyType()); TestSupport.equal(4, c.quantum());
    }
    private static void ioAtSliceBoundary(boolean expires) {
        Computer c = new Computer(1, 10, SchedulingPolicyType.ROUND_ROBIN, 2);
        ProcessControlBlock a = PreemptiveKernelTest.process(1, 1, 2, 6, 0, expires ? 4 : 10, 0, true);
        ProcessControlBlock b = PreemptiveKernelTest.process(2, 1, 2, 4, 1, 10, 0, false);
        c.submit(a); c.submit(b); c.onTick(0);
        TestSupport.equal(1, c.sliceUsed()); c.onTick(1);
        TestSupport.equal(CpuMode.KERNEL, c.cpu().mode()); TestSupport.equal(1L, a.kernelCycles());
        TestSupport.equal(ProcessState.BLOCKED, a.state()); TestSupport.equal(0, a.remainingInstructions());
        TestSupport.equal(0, c.sliceUsed()); TestSupport.equal(1, c.kernel().blockedCount());
        TestSupport.equal(1, c.kernel().readyCount()); TestSupport.equal(1, c.io().pendingCount());
        TestSupport.equal(10, c.ram().used());
        c.configureScheduling(SchedulingPolicyType.PREEMPTIVE_PRIORITY, 1);
        c.onTick(2); TestSupport.equal(2L, c.cpu().lastProcessId());
        TestSupport.equal(2, a.pc()); TestSupport.equal(1, c.io().pendingCount());
        c.onTick(3); TestSupport.equal(2L, c.cpu().lastProcessId());
        TestSupport.equal(expires ? 0 : 6, c.ram().used()); c.onTick(4); c.onTick(5);
        TestSupport.equal(expires ? TerminationReason.DEADLINE_EXPIRED : TerminationReason.COMPLETED, a.terminationReason());
        TestSupport.equal(expires ? 3L : 4L, a.terminationCycle());
        TestSupport.equal(2, c.kernel().terminatedCount()); TestSupport.equal(0, c.kernel().blockedCount());
        TestSupport.equal(0, c.kernel().readyCount()); TestSupport.equal(0, c.io().pendingCount());
        TestSupport.equal(10, c.ram().free()); TestSupport.equal(2, a.pc());
    }
    private static void independentAndIdle() {
        TestSupport.throwsType(IllegalArgumentException.class, () -> new Computer(1, 10, null, 2));
        TestSupport.throwsType(IllegalArgumentException.class, () -> new Computer(1, 10, SchedulingPolicyType.EDF, 0));
        TestSupport.throwsType(IllegalArgumentException.class, () -> new Computer(1, 10, SchedulingPolicyType.FCFS, -1));
        Computer a = new Computer(1, 10, SchedulingPolicyType.ROUND_ROBIN, 1);
        Computer b = new Computer(2, 10, SchedulingPolicyType.EDF, 3);
        a.configureScheduling(SchedulingPolicyType.PREEMPTIVE_PRIORITY, 2); a.onTick(0);
        TestSupport.equal(SchedulingPolicyType.PREEMPTIVE_PRIORITY, a.policyType());
        TestSupport.equal(2, a.quantum()); TestSupport.equal(CpuMode.IDLE, a.cpu().mode());
        TestSupport.equal(SchedulingPolicyType.EDF, b.policyType()); TestSupport.equal(3, b.quantum());
        TestSupport.equal(0L, b.kernel().nextCycle());
        Computer blocked = new Computer(1, 10);
        ProcessControlBlock p = PreemptiveKernelTest.process(1, 1, 2, 6, 0, 10, 0, true);
        blocked.submit(p); blocked.onTick(0); blocked.onTick(1);
        blocked.configureScheduling(SchedulingPolicyType.EDF, 4); blocked.onTick(2);
        TestSupport.equal(SchedulingPolicyType.EDF, blocked.policyType());
        TestSupport.equal(CpuMode.IDLE, blocked.cpu().mode()); TestSupport.equal(ProcessState.BLOCKED, p.state());
        TestSupport.equal(0, blocked.sliceUsed()); blocked.onTick(3); blocked.onTick(4);
        TestSupport.equal(4L, p.terminationCycle()); TestSupport.equal(10, blocked.ram().free());
    }
}
