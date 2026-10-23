package avilaos.sync;
import avilaos.structures.RingBuffer;
import avilaos.process.ProcessControlBlock;

/** Contenido y dueño; BufferService contabiliza las reservas de cada operación. */
public final class BoundedBuffer {
    private final BufferDefinition definition;
    private final RingBuffer<BufferItem> items;
    private final SimulatedSemaphore empty, full, mutex = new SimulatedSemaphore(1, 1);
    private ProcessControlBlock owner;
    private long produced, consumed;
    public BoundedBuffer(BufferDefinition definition) {
        if (definition == null) { throw new IllegalArgumentException("Definición requerida"); }
        this.definition = definition; items = new RingBuffer<>(definition.capacity());
        empty = new SimulatedSemaphore(definition.capacity(), definition.capacity());
        full = new SimulatedSemaphore(0, definition.capacity());
    }
    public void claimMutex(ProcessControlBlock p) {
        if (p == null) { throw new IllegalArgumentException("PCB requerido"); }
        if (owner != null || mutex.value() != 0) { throw new IllegalStateException("Mutex no adquirido"); }
        owner = p;
    }
    private void requireOwner(ProcessControlBlock p) {
        if (p == null || owner != p) { throw new IllegalStateException("El proceso no posee el mutex"); }
    }
    public ProcessControlBlock releaseMutex(ProcessControlBlock p) {
        requireOwner(p); ProcessControlBlock granted = mutex.signal(); owner = granted; return granted;
    }
    public void insert(ProcessControlBlock p, BufferItem item) { requireOwner(p); items.put(item); produced++; }
    public BufferItem remove(ProcessControlBlock p) { requireOwner(p); BufferItem item = items.take(); consumed++; return item; }
    public BufferDefinition definition() { return definition; }
    public int size() { return items.size(); }
    public BufferItem itemAt(int index) { return items.get(index); }
    public long produced() { return produced; }
    public long consumed() { return consumed; }
    public SimulatedSemaphore empty() { return empty; }
    public SimulatedSemaphore full() { return full; }
    public SimulatedSemaphore mutex() { return mutex; }
    public ProcessControlBlock mutexOwner() { return owner; }
}
