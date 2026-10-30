package avilaos.tests;
import avilaos.simulation.*;
import java.util.concurrent.Semaphore;

public final class GlobalClockTest {
    static final class Driver implements ClockDriver {
        final Semaphore published = new Semaphore(0), entered = new Semaphore(0), proceed = new Semaphore(0);
        volatile SimulationState state; volatile long next; volatile String error;
        volatile int calls, shutdowns; volatile boolean hold, fail, failShutdown, failPublish;
        volatile Thread executionThread;
        public long advanceCycle(long c) {
            executionThread = Thread.currentThread();
            if (c != calls) { throw new AssertionError("Salto de ciclo"); }
            calls++; entered.release();
            if (hold) { ConcurrencyTestSupport.acquire(proceed); }
            if (fail) { throw new IllegalStateException("fallo de ciclo"); }
            return 60000;
        }
        public void publishState(long n, SimulationState s, long millis, String e) {
            if (failPublish && n == 1) { failPublish = false; throw new IllegalStateException("publicación fallida"); }
            next = n; error = e; state = s; published.release();
        }
        public void shutdown() { shutdowns++; if (failShutdown) { throw new IllegalStateException("cierre fallido"); } }
    }
    public static void run() {
        controlsAndPacing(); failureStopsAndConfirms(); pauseInFlight();
    }
    private static void controlsAndPacing() {
        Driver d = new Driver(); GlobalClock clock = new GlobalClock(d, 60000);
        TestSupport.throwsType(IllegalStateException.class, clock::resume);
        try {
            clock.start(); await(d, 0, SimulationState.PAUSED); TestSupport.equal(0, d.calls);
            clock.step(); await(d, 1, SimulationState.PAUSED); TestSupport.equal(1, d.calls);
            clock.step(); clock.step(); await(d, 3, SimulationState.PAUSED); TestSupport.equal(3, d.calls);
            clock.resume(); await(d, 4, SimulationState.RUNNING);
            TestSupport.throwsType(IllegalStateException.class, clock::step);
            clock.pause(); await(d, 4, SimulationState.PAUSED); TestSupport.equal(4, d.calls);
            TestSupport.isTrue(d.executionThread != Thread.currentThread());
            clock.resume(); await(d, 5, SimulationState.RUNNING); clock.stop();
            TestSupport.isTrue(stopped(clock)); TestSupport.equal(SimulationState.STOPPED, d.state);
            TestSupport.equal(5, d.calls); TestSupport.equal(1, d.shutdowns);
            TestSupport.throwsType(IllegalStateException.class, clock::resume);
        } finally { clock.close(); }
        TestSupport.isTrue(!clock.isAlive()); clock.close();
        TestSupport.throwsType(IllegalArgumentException.class, () -> new GlobalClock(null, 1));
        TestSupport.throwsType(IllegalArgumentException.class, () -> new GlobalClock(d, 0));
    }
    private static void failureStopsAndConfirms() {
        for (int kind = 0; kind < 3; kind++) {
            Driver d = new Driver(); d.fail = kind == 0; d.failShutdown = kind == 1; d.failPublish = kind == 2;
            GlobalClock clock = new GlobalClock(d, 60000);
            try {
                clock.start(); await(d, 0, SimulationState.PAUSED);
                if (kind == 1) { clock.stop(); } else { clock.step(); }
                TestSupport.isTrue(stopped(clock)); TestSupport.equal(SimulationState.FAILED, clock.state());
                TestSupport.equal(SimulationState.FAILED, d.state); TestSupport.isTrue(d.error != null && !d.error.isBlank());
                TestSupport.equal(kind == 2 ? 1L : 0L, d.next); TestSupport.equal(1, d.shutdowns);
                TestSupport.throwsType(IllegalStateException.class, clock::resume);
            } finally { clock.close(); }
        }
    }
    private static void pauseInFlight() {
        Driver d = new Driver(); d.hold = true; GlobalClock clock = new GlobalClock(d, 60000);
        try {
            clock.start(); await(d, 0, SimulationState.PAUSED); clock.resume();
            ConcurrencyTestSupport.acquire(d.entered); clock.pause(); d.proceed.release();
            await(d, 1, SimulationState.PAUSED); TestSupport.equal(1, d.calls);
        } finally { d.proceed.release(); clock.close(); }
        TestSupport.equal(1, d.shutdowns); TestSupport.isTrue(!clock.isAlive());
    }
    static boolean stopped(GlobalClock clock) {
        try { return clock.awaitStopped(2000); } catch (InterruptedException e) { throw new AssertionError(e); }
    }
    static void await(Driver d, long next, SimulationState state) {
        long start = System.nanoTime();
        while (d.next < next || d.state != state) {
            if (System.nanoTime() - start > 2_000_000_000L) { throw new AssertionError("Estado no alcanzado: " + state); }
            ConcurrencyTestSupport.acquire(d.published);
        }
    }
}
