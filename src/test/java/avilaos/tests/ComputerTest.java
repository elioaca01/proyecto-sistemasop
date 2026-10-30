package avilaos.tests;

import avilaos.hardware.*;
import avilaos.kernel.Kernel;
import avilaos.process.*;
import avilaos.scheduling.FcfsPolicy;
import avilaos.simulation.Computer;

public final class ComputerTest {
    private static ProcessControlBlock process(long id, int computer, ProcessType type,
            int instructions, int memory, long deadline, long arrival) {
        return new ProcessControlBlock(id, new ProcessDefinition("P" + id, computer, type,
                instructions, memory, 0, deadline, type == ProcessType.IO_BOUND ? 1 : 0,
                type == ProcessType.IO_BOUND ? 2 : 0, 0, 0, 0), arrival);
    }
    private static ProcessControlBlock cpu(long id, int instructions, int memory, long deadline) {
        return process(id, 1, ProcessType.CPU_BOUND, instructions, memory, deadline, 0);
    }
    private static ProcessControlBlock io(long id, int instructions, long deadline) {
        return process(id, 1, ProcessType.IO_BOUND, instructions, 8, deadline, 0);
    }
    public static void run() {
        executesFcfs(); admitsSmallerAndReadmitsAtEnd(); overlapsIoWithCpu();
        waitsForFinalIo(); expiresEveryState(); rejectsInvalidInputAtomically(); isolatesComputers();
    }
    private static void executesFcfs() {
        Computer c = new Computer(1, 10);
        ProcessControlBlock a = cpu(1, 2, 4, 10), b = cpu(2, 1, 4, 10);
        c.submit(a); c.submit(b);
        TestSupport.equal(a, c.kernel().newAt(0));
        c.onTick(0);
        TestSupport.equal(1L, c.cpu().lastProcessId()); TestSupport.equal(1, a.pc());
        TestSupport.equal(0, b.pc()); TestSupport.equal(0L, a.firstRunCycle());
        TestSupport.equal(b, c.kernel().readyAt(0));
        c.onTick(1);
        TestSupport.equal(1L, c.cpu().lastProcessId()); TestSupport.equal(2, a.pc());
        TestSupport.equal(TerminationReason.COMPLETED, a.terminationReason());
        TestSupport.equal(1L, a.terminationCycle()); TestSupport.equal(null, c.cpu().current());
        c.onTick(2);
        TestSupport.equal(2L, c.cpu().lastProcessId()); TestSupport.equal(2L, b.firstRunCycle());
        TestSupport.equal(2L, b.terminationCycle()); TestSupport.equal(2, c.kernel().terminatedCount());
        TestSupport.equal(a, c.kernel().terminatedAt(0)); TestSupport.equal(b, c.kernel().terminatedAt(1));
        TestSupport.equal(10, c.ram().free()); TestSupport.equal(3L, c.kernel().nextCycle());
        c.onTick(3); TestSupport.equal(CpuMode.IDLE, c.cpu().mode());
    }
    private static void admitsSmallerAndReadmitsAtEnd() {
        Computer c = new Computer(1, 10);
        ProcessControlBlock a = cpu(1, 2, 8, 10), b = cpu(2, 1, 4, 10), small = cpu(3, 1, 2, 10);
        c.submit(a); c.submit(b); c.submit(small); c.onTick(0);
        TestSupport.equal(ProcessState.NEW, b.state()); TestSupport.equal(1, c.kernel().newCount());
        TestSupport.equal(b, c.kernel().newAt(0)); TestSupport.equal(small, c.kernel().readyAt(0));
        TestSupport.equal(10, c.ram().used());
        c.onTick(1);
        TestSupport.equal(0, c.kernel().newCount()); TestSupport.equal(2, c.kernel().readyCount());
        TestSupport.equal(small, c.kernel().readyAt(0)); TestSupport.equal(b, c.kernel().readyAt(1));
        TestSupport.equal(0, b.pc()); TestSupport.equal(0, small.pc()); TestSupport.equal(6, c.ram().used());
        c.onTick(2); TestSupport.equal(3L, c.cpu().lastProcessId());
        c.onTick(3); TestSupport.equal(2L, c.cpu().lastProcessId());
        TestSupport.equal(10, c.ram().free());
    }
    private static void overlapsIoWithCpu() {
        Computer c = new Computer(1, 10);
        ProcessControlBlock a = io(1, 3, 10), b = cpu(2, 2, 2, 10);
        c.submit(a); c.submit(b); c.onTick(0); c.onTick(1);
        TestSupport.equal(ProcessState.BLOCKED, a.state()); TestSupport.equal(BlockReason.IO, a.blockReason());
        TestSupport.equal(a, c.kernel().blockedAt(0)); TestSupport.equal(1, c.io().pendingCount());
        TestSupport.equal(CpuMode.KERNEL, c.cpu().mode());
        c.onTick(2); TestSupport.equal(2L, c.cpu().lastProcessId());
        TestSupport.equal(2, a.pc()); TestSupport.equal(10, c.ram().used());
        c.onTick(3); TestSupport.equal(2L, c.cpu().lastProcessId());
        TestSupport.equal(2, a.pc()); TestSupport.equal(8, c.ram().used());
        c.onTick(4);
        TestSupport.equal(1L, c.cpu().lastProcessId()); TestSupport.equal(3, a.pc());
        TestSupport.equal(4L, a.terminationCycle()); TestSupport.equal(0, c.kernel().blockedCount());
        TestSupport.equal(0, c.io().pendingCount()); TestSupport.equal(10, c.ram().free());
        TestSupport.equal(2L, a.userCycles()); TestSupport.equal(1L, a.kernelCycles());
    }
    private static void waitsForFinalIo() {
        Computer c = new Computer(1, 10);
        ProcessControlBlock a = io(1, 2, 10); c.submit(a);
        c.onTick(0); c.onTick(1);
        TestSupport.equal(0, a.remainingInstructions()); TestSupport.equal(ProcessState.BLOCKED, a.state());
        c.onTick(2); c.onTick(3);
        TestSupport.equal(0, c.kernel().terminatedCount()); TestSupport.equal(8, c.ram().used());
        // Fin de E/S libera memoria antes de admitir y ejecutar al inicio del ciclo 4.
        ProcessControlBlock b = process(2, 1, ProcessType.CPU_BOUND, 1, 10, 10, 4); c.submit(b);
        c.onTick(4);
        TestSupport.equal(4L, a.terminationCycle()); TestSupport.equal(TerminationReason.COMPLETED, a.terminationReason());
        TestSupport.equal(2, a.pc()); TestSupport.equal(2L, c.cpu().lastProcessId());
        TestSupport.equal(4L, b.firstRunCycle()); TestSupport.equal(2, c.kernel().terminatedCount());
        TestSupport.equal(10, c.ram().free());
    }
    private static void expiresEveryState() {
        Computer one = new Computer(1, 10); ProcessControlBlock a = cpu(1, 1, 1, 1);
        one.submit(a); one.onTick(0); TestSupport.equal(TerminationReason.COMPLETED, a.terminationReason());
        Computer two = new Computer(1, 10); ProcessControlBlock b = cpu(2, 2, 1, 1);
        two.submit(b); two.onTick(0); TestSupport.equal(TerminationReason.DEADLINE_EXPIRED, b.terminationReason());
        TestSupport.equal(1, b.pc()); TestSupport.equal(10, two.ram().free());
        Computer waiting = new Computer(1, 10);
        ProcessControlBlock owner = cpu(1, 3, 8, 10), noRam = cpu(2, 1, 4, 1), ready = cpu(3, 1, 2, 1);
        waiting.submit(owner); waiting.submit(noRam); waiting.submit(ready); waiting.onTick(0);
        TestSupport.equal(TerminationReason.DEADLINE_EXPIRED, noRam.terminationReason());
        TestSupport.equal(-1L, noRam.firstRunCycle()); TestSupport.equal(0, noRam.pc());
        TestSupport.equal(TerminationReason.DEADLINE_EXPIRED, ready.terminationReason());
        TestSupport.equal(8, waiting.ram().used()); TestSupport.equal(0, waiting.kernel().newCount());
        TestSupport.equal(0, waiting.kernel().readyCount()); TestSupport.equal(owner, waiting.cpu().current());
        Computer blocked = new Computer(1, 10); ProcessControlBlock last = io(1, 2, 4); blocked.submit(last);
        for (int t = 0; t < 4; t++) { blocked.onTick(t); }
        TestSupport.equal(TerminationReason.DEADLINE_EXPIRED, last.terminationReason());
        TestSupport.equal(3L, last.terminationCycle()); TestSupport.equal(0, blocked.io().pendingCount());
        TestSupport.equal(0, blocked.kernel().blockedCount()); TestSupport.equal(10, blocked.ram().free());
        blocked.onTick(4); blocked.onTick(5);
        TestSupport.equal(1, blocked.kernel().terminatedCount()); TestSupport.equal(0, blocked.kernel().readyCount());
        TestSupport.equal(2, last.pc()); TestSupport.equal(CpuMode.IDLE, blocked.cpu().mode());
    }
    private static void rejectsInvalidInputAtomically() {
        Computer c = new Computer(1, 10);
        TestSupport.throwsType(IllegalArgumentException.class, () -> new Computer(0, 10));
        TestSupport.throwsType(IllegalArgumentException.class, () -> new Kernel(1, null, new Ram(10), new IoController(), new FcfsPolicy()));
        TestSupport.throwsType(IllegalArgumentException.class, () -> c.submit(null));
        TestSupport.throwsType(IllegalArgumentException.class, () -> c.submit(process(1, 2, ProcessType.CPU_BOUND, 1, 1, 10, 0)));
        TestSupport.throwsType(IllegalArgumentException.class, () -> c.submit(process(1, 1, ProcessType.CPU_BOUND, 1, 1, 10, 1)));
        TestSupport.throwsType(IllegalArgumentException.class, () -> c.submit(cpu(1, 1, 11, 10)));
        TestSupport.throwsType(IllegalArgumentException.class, () -> c.submit(SchedulingTest.ready(7, 0)));
        ProcessControlBlock producer = new ProcessControlBlock(8, new ProcessDefinition("Prod", 1,
                ProcessType.PRODUCER, 6, 1, 0, 10, 0, 0, 1, 1, 1), 0);
        TestSupport.throwsType(IllegalArgumentException.class, () -> c.submit(producer));
        TestSupport.equal(0, c.kernel().newCount()); TestSupport.equal(0, c.ram().used());
        ProcessControlBlock a = cpu(1, 2, 8, 10); c.submit(a);
        TestSupport.throwsType(IllegalArgumentException.class, () -> c.submit(cpu(1, 1, 1, 10)));
        for (long bad : new long[]{-1, 1, Long.MAX_VALUE}) {
            TestSupport.throwsType(IllegalArgumentException.class, () -> c.onTick(bad));
            TestSupport.equal(0L, c.kernel().nextCycle()); TestSupport.equal(0, a.pc());
            TestSupport.equal(1, c.kernel().newCount()); TestSupport.equal(0, c.ram().used());
        }
        c.onTick(0);
        for (long bad : new long[]{0, 2, Long.MAX_VALUE}) {
            TestSupport.throwsType(IllegalArgumentException.class, () -> c.onTick(bad));
            TestSupport.equal(1, a.pc()); TestSupport.equal(8, c.ram().used()); TestSupport.equal(1L, c.kernel().nextCycle());
        }
        c.onTick(1);
        ProcessControlBlock reused = process(1, 1, ProcessType.CPU_BOUND, 1, 1, 10, 2);
        TestSupport.throwsType(IllegalArgumentException.class, () -> c.submit(reused));
        TestSupport.equal(1, c.kernel().terminatedCount()); TestSupport.equal(0, c.kernel().newCount());
        TestSupport.equal(ProcessState.NEW, reused.state());
    }
    private static void isolatesComputers() {
        Computer first = new Computer(1, 10), second = new Computer(2, 20);
        ProcessControlBlock a = cpu(1, 2, 8, 10);
        ProcessControlBlock b = process(1, 2, ProcessType.CPU_BOUND, 1, 15, 10, 0);
        first.submit(a); second.submit(b); first.onTick(0);
        TestSupport.equal(8, first.ram().used()); TestSupport.equal(0, second.ram().used());
        TestSupport.equal(0, b.pc()); TestSupport.equal(1, second.kernel().newCount());
        second.onTick(0);
        TestSupport.equal(20, second.ram().free()); TestSupport.equal(8, first.ram().used());
        TestSupport.equal(ProcessState.RUNNING, a.state()); TestSupport.equal(ProcessState.TERMINATED, b.state());
        TestSupport.isTrue(first.cpu() != second.cpu()); TestSupport.isTrue(first.io() != second.io());
    }
}
