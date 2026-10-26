package avilaos.process;

import avilaos.hardware.CpuMode;

/** Contexto mutable del único hilo simulado de un proceso. Lo administra el núcleo. */
public final class ProcessControlBlock {
    private final long id;
    private final ProcessDefinition definition;
    private final long arrivalCycle;
    private final long deadlineAt;
    private ProcessState state = ProcessState.NEW;
    private BlockReason blockReason = BlockReason.NONE;
    private int pc;
    private int mar;
    private int assignedMemory;
    private boolean everAdmitted;
    private long firstRunCycle = -1;
    private long terminationCycle = -1;
    private TerminationReason terminationReason;
    private long userCycles;
    private long kernelCycles;

    public ProcessControlBlock(long id, ProcessDefinition definition, long arrivalCycle) {
        if (id <= 0 || definition == null || arrivalCycle < 0) {
            throw new IllegalArgumentException("ID, definición o llegada inválidos");
        }
        try {
            this.deadlineAt = Math.addExact(arrivalCycle, definition.deadlineCycles());
        } catch (ArithmeticException overflow) {
            throw new IllegalArgumentException("El deadline absoluto excede el rango permitido", overflow);
        }
        this.id = id;
        this.definition = definition;
        this.arrivalCycle = arrivalCycle;
    }

    /** El administrador de RAM debe reservar el espacio real antes de admitir. */
    public void admit() {
        requireState(ProcessState.NEW);
        assignedMemory = definition.memoryRequired();
        everAdmitted = true;
        state = ProcessState.READY;
    }

    public void dispatch(long cycle) {
        requireExecutionCycle(cycle);
        requireState(ProcessState.READY);
        if (firstRunCycle == -1) {
            firstRunCycle = cycle;
        }
        state = ProcessState.RUNNING;
    }

    public void executeInstruction(CpuMode mode) {
        if (mode == null || mode == CpuMode.IDLE) {
            throw new IllegalArgumentException("Una instrucción requiere modo usuario o núcleo");
        }
        requireState(ProcessState.RUNNING);
        if (remainingInstructions() == 0) {
            throw new IllegalStateException("El proceso ya ejecutó todas sus instrucciones");
        }
        pc++;
        mar++;
        if (mode == CpuMode.USER) {
            userCycles++;
        } else {
            kernelCycles++;
        }
    }

    public void preempt() {
        requireState(ProcessState.RUNNING);
        state = ProcessState.READY;
    }

    public void block(BlockReason reason) {
        if (reason == null || reason == BlockReason.NONE) {
            throw new IllegalArgumentException("Se requiere un motivo de bloqueo");
        }
        requireState(ProcessState.RUNNING);
        blockReason = reason;
        state = ProcessState.BLOCKED;
    }

    public void unblock() {
        requireState(ProcessState.BLOCKED);
        blockReason = BlockReason.NONE;
        state = ProcessState.READY;
    }

    /** Preparar red no ejecuta una instrucción ni registra primera respuesta. */
    public void blockBeforeDispatch(BlockReason reason) {
        if (reason != BlockReason.NETWORK) { throw new IllegalArgumentException("Solo red antes del despacho"); }
        requireState(ProcessState.READY);
        blockReason = reason;
        state = ProcessState.BLOCKED;
    }

    /** cycle identifica la instrucción o cierre de ciclo en que termina el proceso. */
    public void terminate(TerminationReason reason, long cycle) {
        if (reason == null || cycle < 0) {
            throw new IllegalArgumentException("Motivo o ciclo de terminación inválido");
        }
        if (state == ProcessState.TERMINATED) {
            throw new IllegalStateException("El proceso ya terminó");
        }
        if (reason == TerminationReason.COMPLETED) {
            requireExecutionCycle(cycle);
            if (remainingInstructions() != 0) {
                throw new IllegalStateException("Quedan instrucciones por ejecutar");
            }
        } else if (cycle < deadlineAt - 1) {
            throw new IllegalArgumentException("El deadline todavía no venció");
        }
        terminationReason = reason;
        terminationCycle = cycle;
        assignedMemory = 0;
        blockReason = BlockReason.NONE;
        state = ProcessState.TERMINATED;
    }

    public long deadlineRemaining(long boundaryCycle) {
        if (boundaryCycle < 0) {
            throw new IllegalArgumentException("El ciclo no puede ser negativo");
        }
        return Math.max(0L, deadlineAt - boundaryCycle);
    }

    public long id() { return id; }
    public ProcessDefinition definition() { return definition; }
    public ProcessState state() { return state; }
    public BlockReason blockReason() { return blockReason; }
    public int pc() { return pc; }
    public int mar() { return mar; }
    public int remainingInstructions() { return definition.instructionCount() - pc; }
    public int assignedMemory() { return assignedMemory; }
    public boolean everAdmitted() { return everAdmitted; }
    public long arrivalCycle() { return arrivalCycle; }
    public long deadlineAt() { return deadlineAt; }
    public long firstRunCycle() { return firstRunCycle; }
    public long terminationCycle() { return terminationCycle; }
    public TerminationReason terminationReason() { return terminationReason; }
    public long userCycles() { return userCycles; }
    public long kernelCycles() { return kernelCycles; }

    private void requireState(ProcessState expected) {
        if (state != expected) {
            throw new IllegalStateException("Se esperaba " + expected + "; estado actual " + state);
        }
    }

    private void requireExecutionCycle(long cycle) {
        if (cycle < arrivalCycle || cycle >= deadlineAt) {
            throw new IllegalArgumentException("Ciclo fuera del tiempo de vida del proceso");
        }
    }
}
