package avilaos.tests;

import avilaos.simulation.*;
import avilaos.process.*;
import java.util.concurrent.Semaphore;

public final class ClusterIntegrationTest {
    public static void run() {
        TestSupport.equal(trace(1), trace(1000));
        partialFailure(); concurrentCommands(); concurrentReaders(); stopRace(); interruptedDelivery();
    }
    private static String trace(long duration) {
        try (Simulation sim = new Simulation(SimulationTest.configs(), duration)) {
            sim.enqueue(new AddProcessCommand(new ProcessDefinition("IO", 1, ProcessType.IO_BOUND, 3, 4, 0, 10, 1, 2, 0, 0, 0)));
            sim.enqueue(new AddProcessCommand(new ProcessDefinition("CPU", 2, ProcessType.CPU_BOUND, 5, 4, 0, 3, 0, 0, 0, 0, 0)));
            int[] pcA = {1, 2, 2, 2, 3}, pcB = {1, 2, 3, 3, 3};
            int[] ramA = {4, 4, 4, 4, 0}, ramB = {4, 4, 0, 0, 0};
            String trace = "";
            for (int i = 0; i < 5; i++) {
                sim.step(); SimulationSnapshot s = SimulationTest.await(sim, i + 1, SimulationState.PAUSED);
                ComputerSnapshot a = s.computerAt(0), b = s.computerAt(1);
                TestSupport.equal(i + 1L, a.nextCycle()); TestSupport.equal(i + 1L, b.nextCycle());
                ProcessSnapshot pa = a.running() != null ? a.running() : a.blockedCount() > 0 ? a.blockedAt(0) : a.terminatedAt(0);
                ProcessSnapshot pb = b.running() != null ? b.running() : b.terminatedAt(0);
                TestSupport.equal(pcA[i], pa.pc()); TestSupport.equal(pcB[i], pb.pc());
                TestSupport.equal(ramA[i], a.ramUsed()); TestSupport.equal(ramB[i], b.ramUsed());
                if (i == 2) { TestSupport.equal(TerminationReason.DEADLINE_EXPIRED, pb.terminationReason()); }
                trace += pa.pc() + ":" + pb.pc() + ":" + a.ramUsed() + ":" + b.ramUsed() + ";";
            }
            // Después de publicar ciclo 4, todo comando nuevo pertenece al lote de ciclo 5.
            sim.enqueue(new AddProcessCommand(SimulationTest.cpu(2, 1))); sim.step();
            SimulationSnapshot late = SimulationTest.await(sim, 6, SimulationState.PAUSED);
            TestSupport.equal(5L, late.resultAt(2).appliedCycle());
            TestSupport.equal(5L, late.computerAt(1).terminatedAt(1).arrivalCycle());
            return trace;
        }
    }
    private static void partialFailure() {
        Computer a = new Computer(1, 10), b = new Computer(2, 10);
        Thread[] threads = new Thread[3]; int[] order = new int[2]; int[] count = {0};
        ComputerWorker first = new ComputerWorker(1, c -> { order[count[0]++] = 1; threads[0] = Thread.currentThread(); a.onTick(c); });
        ComputerWorker second = new ComputerWorker(2, c -> { order[count[0]++] = 2; threads[1] = Thread.currentThread(); throw new IllegalStateException("segundo falla"); });
        SnapshotStore store = new SnapshotStore(new SimulationSnapshot(0, 0, SimulationState.PAUSED, 1, null,
                new ComputerSnapshot[]{ComputerSnapshot.capture(a), ComputerSnapshot.capture(b)}, new CommandResult[0]));
        ClockDriver driver = new ClockDriver() {
            public long advanceCycle(long cycle) {
                threads[2] = Thread.currentThread();
                ComputerWorkerTest.execute(first, cycle); ComputerWorkerTest.execute(second, cycle); return 1;
            }
            public void publishState(long n, SimulationState state, long millis, String error) {
                store.publish(new SimulationSnapshot(store.get().version() + 1, n, state, millis, error,
                        new ComputerSnapshot[]{ComputerSnapshot.capture(a), ComputerSnapshot.capture(b)}, new CommandResult[0]));
            }
            public void shutdown() { first.close(); second.close(); }
        };
        GlobalClock clock = new GlobalClock(driver, 1);
        try {
            first.start(); second.start(); clock.start(); clock.step();
            TestSupport.isTrue(GlobalClockTest.stopped(clock)); SimulationSnapshot end = store.get();
            TestSupport.equal(SimulationState.FAILED, end.state()); TestSupport.equal(0L, end.nextCycle());
            TestSupport.equal(1L, end.computerAt(0).nextCycle()); TestSupport.equal(0L, end.computerAt(1).nextCycle());
            TestSupport.isTrue(end.error() != null); TestSupport.equal(2, count[0]);
            TestSupport.equal(1, order[0]); TestSupport.equal(2, order[1]);
            TestSupport.isTrue(threads[0] != threads[1] && threads[0] != threads[2] && threads[1] != threads[2]);
        } finally { clock.close(); first.close(); second.close(); }
        TestSupport.isTrue(!first.isAlive() && !second.isAlive() && !clock.isAlive());
    }
    private static void concurrentCommands() {
        try (Simulation sim = new Simulation(SimulationTest.configs(), 1)) {
            Semaphore start = new Semaphore(0); long[][] ids = new long[2][10]; int[][] targets = new int[2][10];
            Throwable[] failures = new Throwable[2]; Thread[] producers = new Thread[2];
            for (int p = 0; p < 2; p++) {
                final int index = p;
                producers[p] = new Thread(() -> {
                    try {
                        start.acquire();
                        for (int j = 0; j < 10; j++) {
                            int target = j % 3 == 0 ? 99 : index + 1;
                            targets[index][j] = target; ids[index][j] = sim.enqueue(new AddProcessCommand(SimulationTest.cpu(target, 1)));
                        }
                    } catch (Throwable t) { failures[index] = t; }
                }); producers[p].start();
            }
            try { start.release(2); for (Thread producer : producers) { ConcurrencyTestSupport.join(producer); } }
            finally { for (Thread producer : producers) { producer.interrupt(); ConcurrencyTestSupport.join(producer); } }
            boolean[] accepted = new boolean[21], seen = new boolean[21];
            for (int p = 0; p < 2; p++) {
                TestSupport.equal(null, failures[p]);
                for (int j = 0; j < 10; j++) {
                    int id = (int) ids[p][j]; TestSupport.isTrue(id > 0 && id <= 20 && !seen[id]);
                    seen[id] = true; accepted[id] = targets[p][j] != 99;
                }
            }
            sim.step(); SimulationSnapshot s = SimulationTest.await(sim, 1, SimulationState.PAUSED);
            TestSupport.equal(20, s.resultCount()); long nextProcessId = 1;
            for (int i = 0; i < 20; i++) {
                CommandResult result = s.resultAt(i);
                TestSupport.equal(i + 1L, result.requestId()); TestSupport.equal(accepted[i + 1], result.accepted());
                TestSupport.equal(accepted[i + 1] ? nextProcessId++ : -1L, result.processId());
            }
        }
    }
    private static void concurrentReaders() {
        Simulation sim = new Simulation(SimulationTest.configs(), 1);
        Throwable[] failures = new Throwable[2]; SimulationSnapshot[] retained = new SimulationSnapshot[2];
        Semaphore ready = new Semaphore(0); Thread[] readers = new Thread[2];
        for (int i = 0; i < 2; i++) {
            final int index = i;
            readers[i] = new Thread(() -> {
                try {
                    SimulationSnapshot current = sim.snapshot(); retained[index] = current; ready.release();
                    while (current.state() != SimulationState.STOPPED) {
                        for (int j = 0; j < current.computerCount(); j++) {
                            if (current.computerAt(j).nextCycle() != current.nextCycle()) { throw new IllegalStateException("Publicación parcial"); }
                        }
                        current = sim.awaitNextSnapshot(current.version(), 1500);
                        if (current == null) { throw new IllegalStateException("Sin publicación terminal"); }
                    }
                } catch (Throwable failure) { failures[index] = failure; }
            }); readers[i].start();
        }
        try {
            ConcurrencyTestSupport.acquire(ready); ConcurrencyTestSupport.acquire(ready);
            sim.enqueue(new AddProcessCommand(SimulationTest.cpu(1, 3)));
            for (int i = 0; i < 5; i++) { sim.step(); SimulationTest.await(sim, i + 1, SimulationState.PAUSED); }
            sim.stop(); TestSupport.isTrue(SimulationTest.stopped(sim));
            for (Thread reader : readers) { ConcurrencyTestSupport.join(reader); }
            for (int i = 0; i < 2; i++) { TestSupport.equal(null, failures[i]); TestSupport.equal(0L, retained[i].nextCycle()); }
        } finally { sim.close(); for (Thread reader : readers) { reader.interrupt(); ConcurrencyTestSupport.join(reader); } }
    }
    private static void stopRace() {
        try (Simulation sim = new Simulation(SimulationTest.configs(), 1)) {
            Semaphore first = new Semaphore(0), race = new Semaphore(0);
            int[] accepted = {0}; Throwable[] failure = new Throwable[1];
            Thread producer = new Thread(() -> {
                try {
                    sim.enqueue(new AddProcessCommand(SimulationTest.cpu(1, 1))); accepted[0]++; first.release(); race.acquire();
                    for (int i = 0; i < 20; i++) {
                        try { sim.enqueue(new AddProcessCommand(SimulationTest.cpu(2, 1))); accepted[0]++; }
                        catch (IllegalStateException closed) { break; }
                    }
                } catch (Throwable t) { failure[0] = t; }
            }); producer.start();
            try { ConcurrencyTestSupport.acquire(first); race.release(); sim.stop(); ConcurrencyTestSupport.join(producer); }
            finally { producer.interrupt(); ConcurrencyTestSupport.join(producer); }
            TestSupport.isTrue(SimulationTest.stopped(sim)); TestSupport.equal(null, failure[0]);
            SimulationSnapshot end = sim.snapshot(); TestSupport.equal(accepted[0], end.resultCount());
            boolean allRejected = true;
            for (int i = 0; i < end.resultCount(); i++) { allRejected &= !end.resultAt(i).accepted(); }
            TestSupport.isTrue(allRejected);
        }
    }
    private static void interruptedDelivery() {
        Semaphore entered = new Semaphore(0), proceed = new Semaphore(0);
        int[] calls = {0}; Throwable[] delivered = new Throwable[1];
        ComputerWorker worker = new ComputerWorker(1, c -> {
            if (++calls[0] == 1) { entered.release(); ConcurrencyTestSupport.acquire(proceed); }
        });
        Thread caller = new Thread(() -> {
            try { worker.executeCycle(0); } catch (Throwable t) { delivered[0] = t; }
        });
        try {
            worker.start(); caller.start(); ConcurrencyTestSupport.acquire(entered);
            caller.interrupt(); ConcurrencyTestSupport.join(caller); proceed.release();
            TestSupport.isTrue(delivered[0] instanceof InterruptedException);
            TestSupport.throwsType(IllegalStateException.class, () -> ComputerWorkerTest.execute(worker, 0));
        } finally { proceed.release(); caller.interrupt(); ConcurrencyTestSupport.join(caller); worker.close(); }
    }
}
