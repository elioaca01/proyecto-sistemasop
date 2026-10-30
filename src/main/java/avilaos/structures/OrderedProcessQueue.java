package avilaos.structures;

import avilaos.process.ProcessControlBlock;
import avilaos.process.ProcessState;
import java.util.Comparator;

/** Cola de listos: orden visible y orden de entrada se conservan por separado. */
public final class OrderedProcessQueue {
    private record Entry(ProcessControlBlock process, long ordinal) { }
    private final LinkedList<Entry> entries = new LinkedList<>();
    private long nextOrdinal;

    public void addLast(ProcessControlBlock process) {
        if (process == null || process.state() != ProcessState.READY) {
            throw new IllegalArgumentException("Solo se encolan procesos READY");
        }
        for (int i = 0; i < entries.size(); i++) {
            if (entries.get(i).process().id() == process.id()) {
                throw new IllegalArgumentException("ID duplicado en listos");
            }
        }
        long ordinal = entries.isEmpty() ? 0 : nextOrdinal;
        long following = Math.incrementExact(ordinal);
        entries.addLast(new Entry(process, ordinal));
        nextOrdinal = following;
    }
    public ProcessControlBlock poll() { return isEmpty() ? null : entries.removeFirst().process(); }
    public ProcessControlBlock peek() { return isEmpty() ? null : entries.get(0).process(); }
    public ProcessControlBlock get(int index) { return entries.get(index).process(); }
    public ProcessControlBlock remove(long id) {
        for (int i = 0; i < entries.size(); i++) {
            if (entries.get(i).process().id() == id) { return entries.removeAt(i).process(); }
        }
        return null;
    }
    public int size() { return entries.size(); }
    public boolean isEmpty() { return entries.isEmpty(); }
    public void sort(Comparator<? super ProcessControlBlock> order) {
        if (order == null) { throw new IllegalArgumentException("Se requiere un comparador"); }
        entries.sort((left, right) -> order.compare(left.process(), right.process()));
    }
    public void restoreFifo() { entries.sort((left, right) -> Long.compare(left.ordinal(), right.ordinal())); }
}
