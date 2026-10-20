package avilaos.kernel;

import avilaos.hardware.Ram;
import avilaos.process.ProcessControlBlock;
import avilaos.process.ProcessState;
import avilaos.structures.LinkedList;

/** Relaciona cada proceso admitido con su reserva real de RAM. */
public final class MemoryAdmission {
    private record Allocation(ProcessControlBlock process, Ram.Reservation reservation) { }
    private final Ram ram;
    private final LinkedList<Allocation> allocations = new LinkedList<>();

    public MemoryAdmission(Ram ram) {
        if (ram == null) { throw new IllegalArgumentException("RAM requerida"); }
        this.ram = ram;
    }
    public boolean admit(ProcessControlBlock process) {
        if (process == null) { throw new IllegalArgumentException("Proceso requerido"); }
        if (process.state() != ProcessState.NEW) { throw new IllegalStateException("Solo se admite NEW"); }
        for (int i = 0; i < allocations.size(); i++) {
            if (allocations.get(i).process().id() == process.id()) {
                throw new IllegalArgumentException("ID con reserva activa");
            }
        }
        Ram.Reservation reservation = ram.reserve(process.definition().memoryRequired());
        if (reservation == null) { return false; }
        process.admit();
        allocations.addLast(new Allocation(process, reservation));
        return true;
    }
    public boolean releaseIfAllocated(ProcessControlBlock process) {
        if (process == null) { throw new IllegalArgumentException("Proceso requerido"); }
        for (int i = 0; i < allocations.size(); i++) {
            Allocation allocation = allocations.get(i);
            if (allocation.process() == process) {
                if (process.state() != ProcessState.TERMINATED) {
                    throw new IllegalStateException("La reserva se libera después de terminar");
                }
                ram.release(allocation.reservation());
                allocations.removeAt(i);
                return true;
            }
        }
        return false;
    }
}
