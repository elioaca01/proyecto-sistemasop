package avilaos.hardware;

import avilaos.process.*;
import avilaos.simulation.Tickable;

/** Ejecuta una instrucción; bloqueos y terminaciones pertenecen al Kernel. */
public final class Cpu implements Tickable {
    private ProcessControlBlock current;
    private CpuMode mode = CpuMode.IDLE;
    private InstructionKind lastInstruction;
    private long lastProcessId = -1;
    private long nextCycle;

    public void load(ProcessControlBlock process) {
        if (current != null) { throw new IllegalStateException("CPU ocupada"); }
        if (process == null || process.state() != ProcessState.RUNNING) {
            throw new IllegalArgumentException("La carga requiere un proceso RUNNING");
        }
        LinearProgram.nextInstruction(process);
        current = process;
    }
    @Override public void onTick(long cycle) {
        if (cycle < 0 || cycle != nextCycle || cycle == Long.MAX_VALUE) {
            throw new IllegalArgumentException("Ciclo CPU fuera de secuencia");
        }
        if (current == null) {
            mode = CpuMode.IDLE;
            lastInstruction = null;
            lastProcessId = -1;
        } else {
            InstructionKind instruction = LinearProgram.nextInstruction(current);
            CpuMode executionMode = instruction == InstructionKind.IO_REQUEST ? CpuMode.KERNEL : CpuMode.USER;
            current.executeInstruction(executionMode);
            lastInstruction = instruction;
            mode = executionMode;
            lastProcessId = current.id();
        }
        nextCycle++;
    }
    public ProcessControlBlock current() { return current; }
    public void clear() { current = null; }
    public CpuMode mode() { return mode; }
    public InstructionKind lastInstruction() { return lastInstruction; }
    public long lastProcessId() { return lastProcessId; }
}
