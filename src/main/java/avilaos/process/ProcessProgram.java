package avilaos.process;
public final class ProcessProgram {
    private ProcessProgram() { }
    public static InstructionKind nextInstruction(ProcessControlBlock p) {
        if (p == null) { throw new IllegalArgumentException("PCB requerido"); }
        return switch (p.definition().type()) {
            case CPU_BOUND, IO_BOUND -> LinearProgram.nextInstruction(p);
            case PRODUCER, CONSUMER -> BufferProgram.nextInstruction(p);
        };
    }
}
