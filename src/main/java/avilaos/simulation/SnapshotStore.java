package avilaos.simulation;

import avilaos.structures.LinkedList;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;

/** Publicación por referencia inmutable y aviso independiente a cada lector. */
public final class SnapshotStore {
    private record Waiter(long version, Semaphore signal) { }
    private final Semaphore mutex = new Semaphore(1);
    private final LinkedList<Waiter> readers = new LinkedList<>();
    private SimulationSnapshot current;

    public SnapshotStore(SimulationSnapshot initial) {
        if (initial == null) { throw new IllegalArgumentException("Instantánea inicial requerida"); }
        current = initial;
    }
    public SimulationSnapshot get() {
        mutex.acquireUninterruptibly();
        try { return current; } finally { mutex.release(); }
    }
    public void publish(SimulationSnapshot next) {
        if (next == null) { throw new IllegalArgumentException("Instantánea requerida"); }
        mutex.acquireUninterruptibly();
        try {
            if (next.version() <= current.version()) { throw new IllegalArgumentException("Versión no creciente"); }
            current = next;
            for (int i = 0; i < readers.size();) {
                Waiter reader = readers.get(i);
                if (next.version() > reader.version()) { readers.removeAt(i); reader.signal().release(); }
                else { i++; }
            }
        } finally { mutex.release(); }
    }
    public SimulationSnapshot awaitNewer(long version, long timeoutMillis) throws InterruptedException {
        if (version < 0 || timeoutMillis <= 0) { throw new IllegalArgumentException("Versión/espera inválida"); }
        Waiter reader = new Waiter(version, new Semaphore(0));
        mutex.acquire();
        try {
            if (current.version() > version) { return current; }
            readers.addLast(reader);
        } finally { mutex.release(); }
        try {
            reader.signal().tryAcquire(timeoutMillis, TimeUnit.MILLISECONDS);
            mutex.acquire();
            try { return current.version() > version ? current : null; }
            finally { mutex.release(); }
        } finally {
            mutex.acquireUninterruptibly();
            try {
                for (int i = 0; i < readers.size(); i++) {
                    if (readers.get(i) == reader) { readers.removeAt(i); break; }
                }
            } finally { mutex.release(); }
        }
    }
}
