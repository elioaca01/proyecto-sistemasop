package avilaos.structures;

import avilaos.process.ProcessControlBlock;
import avilaos.process.ProcessState;
import java.util.Comparator;

/** Cola de listos con ordenamiento propio y unicidad de ID. */
public final class OrderedProcessQueue {
    private final LinkedList<ProcessControlBlock> entries = new LinkedList<>();

    public void addLast(ProcessControlBlock process) {
        if (process == null || process.state() != ProcessState.READY) {
            throw new IllegalArgumentException("Solo se encolan procesos READY");
        }
        for (int i = 0; i < entries.size(); i++) {
            if (entries.get(i).id() == process.id()) {
                throw new IllegalArgumentException("ID duplicado en listos");
            }
        }
        entries.addLast(process);
    }
    public ProcessControlBlock poll() { return isEmpty() ? null : entries.removeFirst(); }
    public ProcessControlBlock peek() { return isEmpty() ? null : entries.get(0); }
    public ProcessControlBlock get(int index) { return entries.get(index); }
    public ProcessControlBlock remove(long id) {
        for (int i = 0; i < entries.size(); i++) {
            if (entries.get(i).id() == id) { return entries.removeAt(i); }
        }
        return null;
    }
    public int size() { return entries.size(); }
    public boolean isEmpty() { return entries.isEmpty(); }
    public void sort(Comparator<? super ProcessControlBlock> order) { entries.sort(order); }
}
