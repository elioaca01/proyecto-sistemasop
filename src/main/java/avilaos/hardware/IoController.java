package avilaos.hardware;

import avilaos.process.ProcessControlBlock;
import avilaos.simulation.Tickable;
import avilaos.structures.LinkedList;

/** Temporizadores independientes; entrega eventos sin modificar el PCB. */
public final class IoController implements Tickable {
    private final LinkedList<IoRequest> pending = new LinkedList<>();
    private final LinkedList<ProcessControlBlock> completed = new LinkedList<>();
    private long nextCycle;

    public void request(ProcessControlBlock process, long issuedCycle, int duration) {
        if (nextCycle == 0 || issuedCycle != nextCycle - 1) {
            throw new IllegalArgumentException("La solicitud requiere el último ciclo procesado");
        }
        IoRequest request = new IoRequest(process, issuedCycle, duration);
        for (int i = 0; i < pending.size(); i++) {
            if (pending.get(i).process() == process) { throw new IllegalArgumentException("E/S duplicada"); }
        }
        for (int i = 0; i < completed.size(); i++) {
            if (completed.get(i) == process) { throw new IllegalArgumentException("E/S completada sin retirar"); }
        }
        pending.addLast(request);
    }
    @Override public void onTick(long cycle) {
        if (cycle < 0 || cycle != nextCycle || cycle == Long.MAX_VALUE) {
            throw new IllegalArgumentException("Ciclo E/S fuera de secuencia");
        }
        for (int i = 0; i < pending.size();) {
            if (pending.get(i).completesAt() <= cycle) {
                completed.addLast(pending.removeAt(i).process());
            } else { i++; }
        }
        nextCycle++;
    }
    public ProcessControlBlock pollCompleted() { return completed.isEmpty() ? null : completed.removeFirst(); }
    public boolean cancel(ProcessControlBlock process) {
        if (process == null) { throw new IllegalArgumentException("Proceso requerido"); }
        boolean removed = false;
        for (int i = 0; i < pending.size();) {
            if (pending.get(i).process() == process) { pending.removeAt(i); removed = true; }
            else { i++; }
        }
        for (int i = 0; i < completed.size();) {
            if (completed.get(i) == process) { completed.removeAt(i); removed = true; }
            else { i++; }
        }
        return removed;
    }
    public int pendingCount() { return pending.size(); }
}
