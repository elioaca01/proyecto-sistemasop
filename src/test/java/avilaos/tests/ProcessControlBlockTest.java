package avilaos.tests;

import avilaos.hardware.CpuMode;
import avilaos.process.BlockReason;
import avilaos.process.ProcessControlBlock;
import avilaos.process.ProcessDefinition;
import avilaos.process.ProcessState;
import avilaos.process.ProcessType;
import avilaos.process.TerminationReason;

public final class ProcessControlBlockTest {
    private ProcessControlBlockTest() { }

    public static void run() {
        keepsContextThroughPreemptionAndReleasesMemoryOnCompletion();
        blocksWithoutLosingMemoryAndResumesWithContext();
        expiresFromEveryLiveState();
        rejectsInvalidTransitionsWithoutMutation();
        givesDeadlineOneExactlyOneCycle();
        rejectsInvalidConstructionAndTimes();
    }

    private static ProcessDefinition definition(int instructions, long deadline) {
        return new ProcessDefinition("P1", 1, ProcessType.CPU_BOUND,
                instructions, 8, 0, deadline, 0, 0, 0, 0, 0);
    }

    private static void keepsContextThroughPreemptionAndReleasesMemoryOnCompletion() {
        ProcessControlBlock pcb = new ProcessControlBlock(1, definition(2, 10), 0);
        TestSupport.equal(ProcessState.NEW, pcb.state());
        TestSupport.equal(0, pcb.assignedMemory());
        TestSupport.equal(2, pcb.remainingInstructions());
        TestSupport.equal(-1L, pcb.firstRunCycle());
        TestSupport.equal(-1L, pcb.terminationCycle());
        TestSupport.equal(null, pcb.terminationReason());
        pcb.admit();
        TestSupport.equal(ProcessState.READY, pcb.state());
        TestSupport.equal(8, pcb.assignedMemory());
        pcb.dispatch(0);
        pcb.executeInstruction(CpuMode.USER);
        pcb.preempt();
        TestSupport.equal(ProcessState.READY, pcb.state());
        TestSupport.equal(1, pcb.pc());
        TestSupport.equal(1, pcb.mar());
        TestSupport.equal(1, pcb.remainingInstructions());
        pcb.dispatch(1);
        pcb.executeInstruction(CpuMode.KERNEL);
        TestSupport.equal(2, pcb.pc());
        TestSupport.equal(2, pcb.mar());
        TestSupport.equal(0, pcb.remainingInstructions());
        TestSupport.equal(0L, pcb.firstRunCycle());
        TestSupport.equal(1L, pcb.userCycles());
        TestSupport.equal(1L, pcb.kernelCycles());
        TestSupport.throwsType(IllegalStateException.class, () -> pcb.executeInstruction(CpuMode.USER));
        pcb.terminate(TerminationReason.COMPLETED, 1);
        TestSupport.equal(ProcessState.TERMINATED, pcb.state());
        TestSupport.equal(TerminationReason.COMPLETED, pcb.terminationReason());
        TestSupport.equal(1L, pcb.terminationCycle());
        TestSupport.equal(0, pcb.assignedMemory());
        TestSupport.equal(BlockReason.NONE, pcb.blockReason());
        TestSupport.throwsType(IllegalStateException.class, () -> pcb.executeInstruction(CpuMode.USER));
        TestSupport.throwsType(IllegalStateException.class, () -> pcb.terminate(TerminationReason.COMPLETED, 1));
        TestSupport.throwsType(IllegalStateException.class, pcb::admit);
        TestSupport.equal(2, pcb.pc());
        TestSupport.equal(0, pcb.assignedMemory());
    }

    private static void blocksWithoutLosingMemoryAndResumesWithContext() {
        ProcessControlBlock pcb = new ProcessControlBlock(2, definition(3, 10), 0);
        pcb.admit();
        pcb.dispatch(0);
        pcb.executeInstruction(CpuMode.USER);
        pcb.block(BlockReason.IO);
        TestSupport.equal(ProcessState.BLOCKED, pcb.state());
        TestSupport.equal(BlockReason.IO, pcb.blockReason());
        TestSupport.equal(8, pcb.assignedMemory());
        TestSupport.equal(1, pcb.pc());
        TestSupport.equal(2, pcb.remainingInstructions());
        TestSupport.equal(2L, pcb.deadlineRemaining(8));
        pcb.unblock();
        TestSupport.equal(ProcessState.READY, pcb.state());
        TestSupport.equal(BlockReason.NONE, pcb.blockReason());
        pcb.dispatch(8);
        pcb.executeInstruction(CpuMode.USER);
        TestSupport.equal(2, pcb.pc());
        TestSupport.equal(0L, pcb.firstRunCycle());
    }

    private static void expiresFromEveryLiveState() {
        for (ProcessState state : new ProcessState[]{ProcessState.NEW, ProcessState.READY,
                ProcessState.RUNNING, ProcessState.BLOCKED}) {
            ProcessControlBlock pcb = new ProcessControlBlock(3, definition(2, 10), 0);
            if (state != ProcessState.NEW) { pcb.admit(); }
            if (state == ProcessState.RUNNING || state == ProcessState.BLOCKED) { pcb.dispatch(0); }
            if (state == ProcessState.BLOCKED) { pcb.block(BlockReason.SEMAPHORE_EMPTY); }
            pcb.terminate(TerminationReason.DEADLINE_EXPIRED, 9);
            TestSupport.equal(ProcessState.TERMINATED, pcb.state());
            TestSupport.equal(0, pcb.assignedMemory());
            TestSupport.equal(BlockReason.NONE, pcb.blockReason());
            TestSupport.equal(TerminationReason.DEADLINE_EXPIRED, pcb.terminationReason());
            TestSupport.equal(2, pcb.remainingInstructions());
            TestSupport.equal(9L, pcb.terminationCycle());
        }
    }

    private static void rejectsInvalidTransitionsWithoutMutation() {
        ProcessControlBlock pcb = new ProcessControlBlock(4, definition(2, 10), 0);
        TestSupport.throwsType(IllegalStateException.class, () -> pcb.dispatch(0));
        TestSupport.throwsType(IllegalStateException.class, () -> pcb.executeInstruction(CpuMode.USER));
        TestSupport.throwsType(IllegalStateException.class, () -> pcb.block(BlockReason.IO));
        TestSupport.throwsType(IllegalStateException.class, pcb::unblock);
        TestSupport.throwsType(IllegalStateException.class, pcb::preempt);
        TestSupport.equal(ProcessState.NEW, pcb.state());
        TestSupport.equal(0, pcb.assignedMemory());
        pcb.admit();
        TestSupport.throwsType(IllegalStateException.class, pcb::admit);
        TestSupport.throwsType(IllegalStateException.class, () -> pcb.executeInstruction(CpuMode.USER));
        TestSupport.throwsType(IllegalStateException.class, pcb::unblock);
        pcb.dispatch(0);
        TestSupport.throwsType(IllegalArgumentException.class, () -> pcb.block(BlockReason.NONE));
        TestSupport.throwsType(IllegalArgumentException.class, () -> pcb.block(null));
        TestSupport.throwsType(IllegalArgumentException.class, () -> pcb.executeInstruction(CpuMode.IDLE));
        TestSupport.throwsType(IllegalArgumentException.class, () -> pcb.executeInstruction(null));
        TestSupport.throwsType(IllegalArgumentException.class, () -> pcb.terminate(null, 0));
        TestSupport.throwsType(IllegalArgumentException.class, () -> pcb.terminate(TerminationReason.DEADLINE_EXPIRED, 0));
        TestSupport.throwsType(IllegalStateException.class, () -> pcb.terminate(TerminationReason.COMPLETED, 0));
        TestSupport.equal(ProcessState.RUNNING, pcb.state());
        TestSupport.equal(BlockReason.NONE, pcb.blockReason());
        TestSupport.equal(8, pcb.assignedMemory());
        TestSupport.equal(0, pcb.pc());
        TestSupport.equal(0, pcb.mar());
        TestSupport.equal(2, pcb.remainingInstructions());
        TestSupport.equal(0L, pcb.userCycles());
        TestSupport.equal(0L, pcb.kernelCycles());
    }

    private static void givesDeadlineOneExactlyOneCycle() {
        ProcessControlBlock completed = new ProcessControlBlock(5, definition(1, 1), 5);
        TestSupport.equal(6L, completed.deadlineAt());
        TestSupport.equal(1L, completed.deadlineRemaining(5));
        completed.admit();
        completed.dispatch(5);
        completed.executeInstruction(CpuMode.USER);
        completed.terminate(TerminationReason.COMPLETED, 5);
        TestSupport.equal(ProcessState.TERMINATED, completed.state());
        TestSupport.equal(0, completed.assignedMemory());
        TestSupport.equal(0L, completed.deadlineRemaining(6));
        TestSupport.equal(0L, completed.deadlineRemaining(Long.MAX_VALUE));

        ProcessControlBlock expired = new ProcessControlBlock(6, definition(2, 1), 5);
        expired.admit();
        TestSupport.throwsType(IllegalArgumentException.class, () -> expired.dispatch(6));
        TestSupport.equal(ProcessState.READY, expired.state());
        expired.terminate(TerminationReason.DEADLINE_EXPIRED, 5);
        TestSupport.equal(TerminationReason.DEADLINE_EXPIRED, expired.terminationReason());
        TestSupport.equal(0, expired.assignedMemory());
    }

    private static void rejectsInvalidConstructionAndTimes() {
        TestSupport.throwsType(IllegalArgumentException.class, () -> new ProcessControlBlock(0, definition(2, 10), 0));
        TestSupport.throwsType(IllegalArgumentException.class, () -> new ProcessControlBlock(1, null, 0));
        TestSupport.throwsType(IllegalArgumentException.class, () -> new ProcessControlBlock(1, definition(2, 10), -1));
        TestSupport.throwsType(IllegalArgumentException.class, () -> new ProcessControlBlock(1, definition(2, 10), Long.MAX_VALUE));
        ProcessControlBlock pcb = new ProcessControlBlock(7, definition(2, 10), 5);
        pcb.admit();
        TestSupport.throwsType(IllegalArgumentException.class, () -> pcb.dispatch(-1));
        TestSupport.throwsType(IllegalArgumentException.class, () -> pcb.dispatch(4));
        TestSupport.throwsType(IllegalArgumentException.class, () -> pcb.deadlineRemaining(-1));
        TestSupport.throwsType(IllegalArgumentException.class, () -> pcb.terminate(TerminationReason.DEADLINE_EXPIRED, -1));
        TestSupport.equal(ProcessState.READY, pcb.state());
        TestSupport.equal(-1L, pcb.firstRunCycle());
        TestSupport.equal(8, pcb.assignedMemory());
    }
}
