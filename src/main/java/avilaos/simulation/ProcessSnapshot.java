package avilaos.simulation;
import avilaos.process.*;
/** Copia por valor del contexto; no retiene el PCB mutable. */
public record ProcessSnapshot(long id, ProcessDefinition definition, ProcessState state, BlockReason blockReason, int pc, int mar, int remainingInstructions, int assignedMemory, long arrivalCycle, long deadlineAt, long firstRunCycle, long terminationCycle, TerminationReason terminationReason, long userCycles, long kernelCycles) {
    public static ProcessSnapshot capture(ProcessControlBlock p) {
        if (p == null) { throw new IllegalArgumentException("PCB requerido"); }
        return new ProcessSnapshot(p.id(), p.definition(), p.state(), p.blockReason(), p.pc(), p.mar(), p.remainingInstructions(), p.assignedMemory(), p.arrivalCycle(), p.deadlineAt(), p.firstRunCycle(), p.terminationCycle(), p.terminationReason(), p.userCycles(), p.kernelCycles());
    }
}
