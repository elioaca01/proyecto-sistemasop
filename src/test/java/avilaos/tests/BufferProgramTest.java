package avilaos.tests;
import avilaos.process.*;
import avilaos.hardware.*;

public final class BufferProgramTest {
    static ProcessDefinition definition(ProcessType type, int computer, int instructions, int interval, int target, long deadline) {
        return new ProcessDefinition(type.name(), computer, type, instructions, 2, 0, deadline, 0, 0, 1, interval, target);
    }
    static ProcessControlBlock process(long id, ProcessType type, int computer, int instructions, long deadline) {
        return new ProcessControlBlock(id, definition(type, computer, instructions, 1, 1, deadline), 0);
    }
    public static void run() {
        BufferPhase[][] phases = {
            {BufferPhase.COMPUTE, BufferPhase.WAIT_EMPTY, BufferPhase.WAIT_MUTEX, BufferPhase.INSERT, BufferPhase.SIGNAL_MUTEX, BufferPhase.SIGNAL_FULL},
            {BufferPhase.WAIT_FULL, BufferPhase.WAIT_MUTEX, BufferPhase.REMOVE, BufferPhase.SIGNAL_MUTEX, BufferPhase.SIGNAL_EMPTY, BufferPhase.COMPUTE}
        };
        InstructionKind[][] kinds = {
            {InstructionKind.COMPUTE, InstructionKind.SEM_WAIT, InstructionKind.SEM_WAIT, InstructionKind.BUFFER_INSERT, InstructionKind.SEM_SIGNAL, InstructionKind.SEM_SIGNAL},
            {InstructionKind.SEM_WAIT, InstructionKind.SEM_WAIT, InstructionKind.BUFFER_REMOVE, InstructionKind.SEM_SIGNAL, InstructionKind.SEM_SIGNAL, InstructionKind.COMPUTE}
        };
        ProcessType[] types = {ProcessType.PRODUCER, ProcessType.CONSUMER};
        for (int k = 0; k < 2; k++) {
            ProcessControlBlock p = new ProcessControlBlock(k + 1, definition(types[k], 1, 14, 1, 2, 30), 0);
            p.admit(); p.dispatch(0); Cpu cpu = new Cpu(); cpu.load(p);
            for (int i = 0; i < 14; i++) {
                TestSupport.equal(i < 12 ? phases[k][i % 6] : BufferPhase.COMPUTE, BufferProgram.phaseAt(p.definition(), i));
                cpu.onTick(i); TestSupport.equal(i < 12 ? kinds[k][i % 6] : InstructionKind.COMPUTE, cpu.lastInstruction());
                TestSupport.equal(i + 1, p.pc());
            }
            TestSupport.equal(4L, p.userCycles()); TestSupport.equal(10L, p.kernelCycles());
            TestSupport.equal(ProcessState.RUNNING, p.state());
            TestSupport.equal(BufferPhase.COMPLETE, BufferProgram.phaseAt(p.definition(), 14));
            TestSupport.throwsType(IllegalArgumentException.class, () -> ProcessProgram.nextInstruction(p));
            TestSupport.throwsType(IllegalArgumentException.class, () -> BufferProgram.phaseAt(p.definition(), -1));
            TestSupport.throwsType(IllegalArgumentException.class, () -> BufferProgram.phaseAt(p.definition(), 15));
        }
        ProcessDefinition maximum = definition(ProcessType.PRODUCER, 1, Integer.MAX_VALUE, Integer.MAX_VALUE - 5, 1, 10);
        TestSupport.equal(BufferPhase.WAIT_EMPTY, BufferProgram.phaseAt(maximum, Integer.MAX_VALUE - 5));
        TestSupport.equal(BufferPhase.SIGNAL_FULL, BufferProgram.phaseAt(maximum, Integer.MAX_VALUE - 1));
        TestSupport.equal(BufferPhase.COMPLETE, BufferProgram.phaseAt(maximum, Integer.MAX_VALUE));
        TestSupport.throwsType(IllegalArgumentException.class, () -> definition(ProcessType.PRODUCER, 1, Integer.MAX_VALUE, Integer.MAX_VALUE, 1, 10));
        TestSupport.throwsType(IllegalArgumentException.class, () -> BufferProgram.nextInstruction(null));
    }
}
