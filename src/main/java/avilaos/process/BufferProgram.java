package avilaos.process;
/** Programa puro: la fase se deriva de instrucciones ejecutadas, no del tiempo bloqueado. */
public final class BufferProgram {
    private BufferProgram() { }
    public static BufferPhase phaseAt(ProcessDefinition d, int index) {
        if (d == null || (d.type() != ProcessType.PRODUCER && d.type() != ProcessType.CONSUMER)
                || index < 0 || index > d.instructionCount()) { throw new IllegalArgumentException("Programa/índice inválido"); }
        if (index == d.instructionCount()) { return BufferPhase.COMPLETE; }
        long period = d.operationInterval() + 5L;
        if (index >= period * d.targetItems()) { return BufferPhase.COMPUTE; }
        long position = index % period;
        if (d.type() == ProcessType.PRODUCER) {
            if (position < d.operationInterval()) { return BufferPhase.COMPUTE; }
            return switch ((int) (position - d.operationInterval())) {
                case 0 -> BufferPhase.WAIT_EMPTY;
                case 1 -> BufferPhase.WAIT_MUTEX;
                case 2 -> BufferPhase.INSERT;
                case 3 -> BufferPhase.SIGNAL_MUTEX;
                default -> BufferPhase.SIGNAL_FULL;
            };
        }
        if (position >= 5) { return BufferPhase.COMPUTE; }
        return switch ((int) position) {
            case 0 -> BufferPhase.WAIT_FULL;
            case 1 -> BufferPhase.WAIT_MUTEX;
            case 2 -> BufferPhase.REMOVE;
            case 3 -> BufferPhase.SIGNAL_MUTEX;
            default -> BufferPhase.SIGNAL_EMPTY;
        };
    }
    public static InstructionKind nextInstruction(ProcessControlBlock p) {
        if (p == null || p.remainingInstructions() == 0) { throw new IllegalArgumentException("Instrucciones pendientes requeridas"); }
        return switch (phaseAt(p.definition(), p.pc())) {
            case COMPUTE -> InstructionKind.COMPUTE;
            case WAIT_EMPTY, WAIT_FULL, WAIT_MUTEX -> InstructionKind.SEM_WAIT;
            case INSERT -> InstructionKind.BUFFER_INSERT;
            case REMOVE -> InstructionKind.BUFFER_REMOVE;
            case SIGNAL_MUTEX, SIGNAL_FULL, SIGNAL_EMPTY -> InstructionKind.SEM_SIGNAL;
            default -> throw new IllegalStateException("Programa agotado");
        };
    }
}
