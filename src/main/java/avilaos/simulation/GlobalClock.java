package avilaos.simulation;

import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;

/** Reloj con controles cortos y ciclos concedidos de forma atómica. */
public final class GlobalClock implements AutoCloseable {
    private final ClockDriver driver;
    private final Thread thread;
    private final Semaphore control = new Semaphore(1), wake = new Semaphore(0), stopped = new Semaphore(0);
    private boolean started, continuous, stopping, inFlight, manualInFlight;
    private int steps;
    private long cycleMillis, nextCycle;
    private volatile SimulationState state = SimulationState.PAUSED;

    public GlobalClock(ClockDriver driver, long cycleMillis) {
        if (driver == null || cycleMillis <= 0) { throw new IllegalArgumentException("Driver y duración positiva requeridos"); }
        this.driver = driver; this.cycleMillis = cycleMillis;
        thread = new Thread(this::run, "avilaos-global-clock");
    }
    public void start() {
        control.acquireUninterruptibly();
        try {
            if (started || stopping) { throw new IllegalStateException("Reloj ya iniciado/cerrado"); }
            started = true; thread.start();
        } finally { control.release(); }
    }
    private void requireActive() {
        if (!started || stopping || state == SimulationState.STOPPED || state == SimulationState.FAILED) {
            throw new IllegalStateException("Reloj no disponible");
        }
    }
    public void resume() {
        control.acquireUninterruptibly();
        try { requireActive(); continuous = true; steps = 0; wake.release(); }
        finally { control.release(); }
    }
    public void pause() {
        control.acquireUninterruptibly();
        try { requireActive(); continuous = false; steps = 0; wake.release(); }
        finally { control.release(); }
    }
    public void step() {
        control.acquireUninterruptibly();
        try {
            requireActive();
            if (continuous || (inFlight && !manualInFlight)) { throw new IllegalStateException("Se requiere modo pausado"); }
            steps = Math.incrementExact(steps); wake.release();
        } finally { control.release(); }
    }
    public void stop() {
        control.acquireUninterruptibly();
        try {
            stopping = true; continuous = false; steps = 0; wake.release();
            if (!started) { started = true; thread.start(); }
        } finally { control.release(); }
    }
    private void run() {
        Throwable failure = null;
        boolean restoreInterrupt = false;
        try {
            driver.publishState(0, SimulationState.PAUSED, cycleMillis, null);
            while (true) {
                boolean grant;
                control.acquire();
                try {
                    if (stopping) { break; }
                    grant = continuous || steps > 0;
                    if (grant) {
                        inFlight = true; manualInFlight = !continuous;
                        if (manualInFlight) { steps--; }
                        state = SimulationState.RUNNING; wake.drainPermits();
                    } else { state = SimulationState.PAUSED; }
                } finally { control.release(); }
                if (!grant) {
                    driver.publishState(nextCycle, SimulationState.PAUSED, cycleMillis, null);
                    wake.acquire(); continue;
                }
                long duration = driver.advanceCycle(nextCycle);
                if (duration <= 0) { throw new IllegalStateException("Duración de ciclo inválida"); }
                cycleMillis = duration; nextCycle = Math.incrementExact(nextCycle);
                boolean pace;
                SimulationState publication;
                control.acquire();
                try {
                    inFlight = false; manualInFlight = false;
                    pace = continuous && !stopping;
                    publication = stopping ? SimulationState.STOPPING : pace ? SimulationState.RUNNING : SimulationState.PAUSED;
                    state = publication;
                } finally { control.release(); }
                driver.publishState(nextCycle, publication, cycleMillis, null);
                if (pace) { waitForPace(); }
            }
        } catch (Throwable problem) {
            failure = problem; restoreInterrupt = problem instanceof InterruptedException;
        } finally {
            restoreInterrupt |= Thread.interrupted();
            try { driver.shutdown(); }
            catch (Throwable cleanup) { if (failure == null) { failure = cleanup; } else { failure.addSuppressed(cleanup); } }
            control.acquireUninterruptibly();
            try { stopping = true; inFlight = false; state = failure == null ? SimulationState.STOPPED : SimulationState.FAILED; }
            finally { control.release(); }
            try { driver.publishState(nextCycle, state, cycleMillis, describe(failure)); }
            catch (Throwable publicationFailure) {
                state = SimulationState.FAILED;
                try { driver.publishState(nextCycle, state, cycleMillis, describe(publicationFailure)); }
                catch (Throwable ignored) { /* state() sigue anunciando el fallo si el publicador también falla. */ }
            } finally {
                stopped.release();
                if (restoreInterrupt) { Thread.currentThread().interrupt(); }
            }
        }
    }
    private void waitForPace() throws InterruptedException {
        long start = System.nanoTime(), duration = TimeUnit.MILLISECONDS.toNanos(cycleMillis);
        while (true) {
            control.acquire();
            try { if (stopping || !continuous) { return; } }
            finally { control.release(); }
            long remaining = duration - (System.nanoTime() - start);
            if (remaining <= 0) { return; }
            wake.tryAcquire(remaining, TimeUnit.NANOSECONDS);
        }
    }
    private static String describe(Throwable problem) {
        return problem == null ? null : problem.getClass().getSimpleName() + ": " + problem.getMessage();
    }
    boolean ownsCurrentThread() { return Thread.currentThread() == thread; }
    public SimulationState state() { return state; }
    public boolean isAlive() { return thread.isAlive(); }
    public boolean awaitStopped(long timeoutMillis) throws InterruptedException {
        if (timeoutMillis <= 0) { throw new IllegalArgumentException("Espera positiva requerida"); }
        if (ownsCurrentThread()) { throw new IllegalStateException("El reloj no puede esperarse a sí mismo"); }
        long start = System.nanoTime();
        if (!stopped.tryAcquire(timeoutMillis, TimeUnit.MILLISECONDS)) { return false; }
        stopped.release();
        long remaining = TimeUnit.MILLISECONDS.toNanos(timeoutMillis) - (System.nanoTime() - start);
        if (remaining > 0) { thread.join(Math.max(1, TimeUnit.NANOSECONDS.toMillis(remaining))); }
        return !thread.isAlive();
    }
    @Override public void close() {
        stop();
        try { if (!awaitStopped(2000)) { throw new IllegalStateException("El reloj no confirmó su cierre"); } }
        catch (InterruptedException e) { Thread.currentThread().interrupt(); throw new IllegalStateException("Cierre interrumpido", e); }
    }
}
