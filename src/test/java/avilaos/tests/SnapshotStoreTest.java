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
                try { result[index] = store.awaitNewer(1, 10000); } catch (Throwable e) { failures[index] = e; }
            }, "snapshot-reader-" + i); readers[i].start();
        }
        try {
            ConcurrencyTestSupport.acquire(started); ConcurrencyTestSupport.acquire(started);
            for (Thread reader : readers) { ConcurrencyTestSupport.awaitTimedWaiting(reader); }
            store.publish(snapshot(2));
            for (Thread reader : readers) { ConcurrencyTestSupport.join(reader); }
            for (int i = 0; i < 2; i++) { TestSupport.equal(null, failures[i]); TestSupport.equal(2L, result[i].version()); }
        } finally { for (Thread reader : readers) { reader.interrupt(); ConcurrencyTestSupport.join(reader); } }
        cancellationKeepsOtherReader(true);
        cancellationKeepsOtherReader(false);
    }
    private static void cancellationKeepsOtherReader(boolean interrupt) {
        SnapshotStore store = new SnapshotStore(snapshot(2));
        SimulationSnapshot[] result = new SimulationSnapshot[2]; Throwable[] failures = new Throwable[2];
        Thread[] readers = new Thread[2]; Semaphore started = new Semaphore(0);
        for (int i = 0; i < 2; i++) {
            final int index = i;
            readers[i] = new Thread(() -> {
                started.release();
                try { result[index] = store.awaitNewer(2, index == 0 && !interrupt ? 250 : 10000); }
                catch (Throwable e) { failures[index] = e; }
            }, "cancellation-reader-" + i);
            readers[i].start();
        }
        try {
            ConcurrencyTestSupport.acquire(started); ConcurrencyTestSupport.acquire(started);
            for (Thread reader : readers) { ConcurrencyTestSupport.awaitTimedWaiting(reader); }
            if (interrupt) { readers[0].interrupt(); }
            ConcurrencyTestSupport.join(readers[0]);
            if (interrupt) { TestSupport.isTrue(failures[0] instanceof InterruptedException); }
            else { TestSupport.equal(null, failures[0]); TestSupport.equal(null, result[0]); }
            store.publish(snapshot(3));
            // Debe acabar antes de sus 10 segundos: no basta recuperar current al vencer timeout.
            ConcurrencyTestSupport.join(readers[1]);
            TestSupport.equal(null, failures[1]); TestSupport.equal(3L, result[1].version());
        } finally { for (Thread reader : readers) { reader.interrupt(); ConcurrencyTestSupport.join(reader); } }
    }
    private static void awaitInvalid(SnapshotStore store, long v, long t) {
        try { store.awaitNewer(v, t); } catch (InterruptedException e) { throw new AssertionError(e); }
    }
}
