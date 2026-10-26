package avilaos.sync;

import avilaos.metrics.SemaphoreWaitMetrics;

/** Copia por valor; se construye bajo el mutex del servicio, sin retener PCB ni buffer. */
public final class BufferSnapshot {
    private final BufferDefinition definition;
    private final int size, emptyValue, fullValue, mutexValue;
    private final long produced, consumed, mutexOwnerId;
    private final BufferItem[] items;
    private final long[] emptyWaiting, fullWaiting, mutexWaiting;
    private final BufferOperationSnapshot[] operations;
    private final SemaphoreWaitMetrics waits;
    BufferSnapshot(BoundedBuffer b, BufferOperationSnapshot[] operations, SemaphoreWaitMetrics waits) {
        this.waits = waits;
        definition = b.definition(); size = b.size(); produced = b.produced(); consumed = b.consumed();
        emptyValue = b.empty().value(); fullValue = b.full().value(); mutexValue = b.mutex().value();
        mutexOwnerId = b.mutexOwner() == null ? -1 : b.mutexOwner().id();
        items = new BufferItem[size];
        for (int i = 0; i < items.length; i++) { items[i] = b.itemAt(i); }
        emptyWaiting = waiting(b.empty()); fullWaiting = waiting(b.full()); mutexWaiting = waiting(b.mutex());
        this.operations = operations.clone();
    }
    private static long[] waiting(SimulatedSemaphore s) {
        long[] ids = new long[s.waitingCount()];
        for (int i = 0; i < ids.length; i++) { ids[i] = s.waitingAt(i).id(); }
        return ids;
    }
    public SemaphoreWaitMetrics waits() { return waits; }
    public BufferDefinition definition() { return definition; }
    public int size() { return size; }
    public long produced() { return produced; }
    public long consumed() { return consumed; }
    public long mutexOwnerId() { return mutexOwnerId; }
    public int emptyValue() { return emptyValue; }
    public int fullValue() { return fullValue; }
    public int mutexValue() { return mutexValue; }
    public BufferItem itemAt(int i) { return items[i]; }
    public int emptyWaitingCount() { return emptyWaiting.length; }
    public long emptyWaitingAt(int i) { return emptyWaiting[i]; }
    public int fullWaitingCount() { return fullWaiting.length; }
    public long fullWaitingAt(int i) { return fullWaiting[i]; }
    public int mutexWaitingCount() { return mutexWaiting.length; }
    public long mutexWaitingAt(int i) { return mutexWaiting[i]; }
    public int operationCount() { return operations.length; }
    public BufferOperationSnapshot operationAt(int i) { return operations[i]; }
}
