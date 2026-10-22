package avilaos.tests;
import avilaos.simulation.*;
import java.util.concurrent.Semaphore;

public final class SnapshotStoreTest {
    static SimulationSnapshot snapshot(long version) {
        return new SimulationSnapshot(version, version, SimulationState.PAUSED, 1, null,
                new ComputerSnapshot[0], new CommandResult[0]);
    }
    public static void run() {
        SnapshotStore store = new SnapshotStore(snapshot(0));
        TestSupport.equal(0L, store.get().version()); store.publish(snapshot(1));
        TestSupport.throwsType(IllegalArgumentException.class, () -> store.publish(snapshot(1)));
        TestSupport.throwsType(IllegalArgumentException.class, () -> store.publish(null));
        TestSupport.throwsType(IllegalArgumentException.class, () -> new SnapshotStore(null));
        try {
            TestSupport.equal(1L, store.awaitNewer(0, 100).version());
            TestSupport.equal(null, store.awaitNewer(1, 10));
            TestSupport.throwsType(IllegalArgumentException.class, () -> awaitInvalid(store, -1, 1));
            TestSupport.throwsType(IllegalArgumentException.class, () -> awaitInvalid(store, 0, 0));
        } catch (InterruptedException e) { throw new AssertionError(e); }
        Semaphore started = new Semaphore(0);
        SimulationSnapshot[] result = new SimulationSnapshot[2]; Throwable[] failures = new Throwable[2];
        Thread[] readers = new Thread[2];
        for (int i = 0; i < 2; i++) {
            final int index = i;
            readers[i] = new Thread(() -> {
                started.release();
                try { result[index] = store.awaitNewer(1, 1500); } catch (Throwable e) { failures[index] = e; }
            }, "snapshot-reader-" + i); readers[i].start();
        }
        try {
            ConcurrencyTestSupport.acquire(started); ConcurrencyTestSupport.acquire(started); store.publish(snapshot(2));
            for (Thread reader : readers) { ConcurrencyTestSupport.join(reader); }
            for (int i = 0; i < 2; i++) { TestSupport.equal(null, failures[i]); TestSupport.equal(2L, result[i].version()); }
        } finally { for (Thread reader : readers) { reader.interrupt(); ConcurrencyTestSupport.join(reader); } }
        Throwable[] interrupted = new Throwable[1];
        Thread cancelled = new Thread(() -> {
            started.release();
            try { store.awaitNewer(2, 1500); } catch (Throwable e) { interrupted[0] = e; }
        }); cancelled.start();
        try {
            ConcurrencyTestSupport.acquire(started); cancelled.interrupt(); ConcurrencyTestSupport.join(cancelled);
            TestSupport.isTrue(interrupted[0] instanceof InterruptedException);
            store.publish(snapshot(3)); TestSupport.equal(3L, store.get().version());
        } finally { cancelled.interrupt(); ConcurrencyTestSupport.join(cancelled); }
    }
    private static void awaitInvalid(SnapshotStore store, long v, long t) {
        try { store.awaitNewer(v, t); } catch (InterruptedException e) { throw new AssertionError(e); }
    }
}
