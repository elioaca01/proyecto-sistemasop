package avilaos.process;

/** Calcula la instrucción siguiente sin alterar el contexto del proceso. */
public final class LinearProgram {
    private LinearProgram() { }

    public static InstructionKind nextInstruction(ProcessControlBlock process) {
        if (process == null || process.remainingInstructions() == 0) {
            throw new IllegalArgumentException("Proceso con instrucciones pendientes requerido");
        }
        return switch (process.definition().type()) {
            case CPU_BOUND -> InstructionKind.COMPUTE;
            case IO_BOUND -> (process.pc() + 1L) % (process.definition().cpuBurst() + 1L) == 0
                    ? InstructionKind.IO_REQUEST : InstructionKind.COMPUTE;
            default -> throw new IllegalArgumentException("Programa no soportado por CPU lineal");
        };
    }
}
