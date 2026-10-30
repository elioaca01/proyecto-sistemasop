package avilaos.tests;
import avilaos.simulation.*;

public final class ComputerWorkerTest {
    public static void run() {
        long[] seen = {-1}; Thread[] executedBy = new Thread[1];
        ComputerWorker worker = new ComputerWorker(7, c -> { seen[0] = c; executedBy[0] = Thread.currentThread(); });
        TestSupport.throwsType(IllegalStateException.class, () -> execute(worker, 0));
        try {
            worker.start(); TestSupport.throwsType(IllegalStateException.class, worker::start);
            execute(worker, 0); TestSupport.equal(0L, seen[0]);
            TestSupport.isTrue(executedBy[0] != Thread.currentThread()); TestSupport.equal(7, worker.computerId());
            for (long bad : new long[]{-1, 0, 2, Long.MAX_VALUE}) {
                TestSupport.throwsType(IllegalArgumentException.class, () -> execute(worker, bad));
                TestSupport.equal(0L, seen[0]);
            }
            execute(worker, 1); TestSupport.equal(1L, seen[0]);
        } finally { worker.close(); }
        worker.close(); TestSupport.isTrue(!worker.isAlive());
        TestSupport.throwsType(IllegalStateException.class, () -> execute(worker, 2));
        RuntimeException cause = new IllegalStateException("fallo controlado");
        ComputerWorker failed = new ComputerWorker(1, c -> { throw cause; });
        try {
            failed.start();
            try { execute(failed, 0); throw new AssertionError("Debía fallar"); }
            catch (IllegalStateException e) { TestSupport.equal(cause, e.getCause()); }
            TestSupport.throwsType(IllegalStateException.class, () -> execute(failed, 1));
        } finally { failed.close(); }
        TestSupport.isTrue(!failed.isAlive());
        ComputerWorker idle = new ComputerWorker(2, c -> { }); idle.start(); idle.close();
        TestSupport.isTrue(!idle.isAlive());
        TestSupport.throwsType(IllegalArgumentException.class, () -> new ComputerWorker(0, c -> { }));
        TestSupport.throwsType(IllegalArgumentException.class, () -> new ComputerWorker(1, null));
    }
    static void execute(ComputerWorker worker, long cycle) {
        try { worker.executeCycle(cycle); } catch (InterruptedException e) { Thread.currentThread().interrupt(); throw new AssertionError(e); }
    }
}
