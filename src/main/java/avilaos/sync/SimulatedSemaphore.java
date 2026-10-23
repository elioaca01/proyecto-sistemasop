package avilaos.sync;
import avilaos.process.ProcessControlBlock;
import avilaos.structures.LinkedList;

/** Espera de PCB, sin bloquear Threads Java. El servicio coordina el acceso real. */
public final class SimulatedSemaphore {
    private final int maximum;
    private int value;
    private final LinkedList<ProcessControlBlock> waiting = new LinkedList<>();
    public SimulatedSemaphore(int initial, int maximum) {
        if (maximum <= 0 || initial < 0 || initial > maximum) { throw new IllegalArgumentException("Contador inválido"); }
        this.maximum = maximum; value = initial;
    }
    public boolean waitFor(ProcessControlBlock p) {
        if (p == null) { throw new IllegalArgumentException("PCB requerido"); }
        for (int i = 0; i < waiting.size(); i++) {
            if (waiting.get(i).id() == p.id()) { throw new IllegalArgumentException("PCB ya en espera"); }
        }
        if (value > 0) { value--; return true; }
        waiting.addLast(p); return false;
    }
    public ProcessControlBlock signal() {
        if (!waiting.isEmpty()) { return waiting.removeFirst(); }
        if (value == maximum) { throw new IllegalStateException("Señal sin permiso adquirido"); }
        value++; return null;
    }
    public boolean cancelWaiting(ProcessControlBlock p) {
        if (p == null) { throw new IllegalArgumentException("PCB requerido"); }
        for (int i = 0; i < waiting.size(); i++) { if (waiting.get(i) == p) { waiting.removeAt(i); return true; } }
        return false;
    }
    public int value() { return value; }
    public int waitingCount() { return waiting.size(); }
    public ProcessControlBlock waitingAt(int index) { return waiting.get(index); }
}
