package avilaos.simulation;

import java.util.concurrent.Semaphore;

/** Un solo trabajo en vuelo; la confirmación se entrega incluso si falla el target. */
public final class ComputerWorker implements AutoCloseable {
    private final int computerId;
    private final Tickable target;
    private final Thread thread;
    private final Semaphore lifecycle = new Semaphore(1);
    private final Semaphore requests = new Semaphore(1);
    private final Semaphore begin = new Semaphore(0), finished = new Semaphore(0);
    private volatile boolean closing;
    private boolean started, pending;
    private long requestedCycle, nextCycle;
    private volatile Throwable failure;

    public ComputerWorker(int computerId, Tickable target) {
        if (computerId <= 0 || target == null) { throw new IllegalArgumentException("Computador y target requeridos"); }
        this.computerId = computerId; this.target = target;
        thread = new Thread(this::run, "avilaos-computer-" + computerId);
    }
    public void start() {
        lifecycle.acquireUninterruptibly();
        try {
            if (started || closing) { throw new IllegalStateException("Worker ya iniciado o cerrado"); }
            started = true;
            thread.start();
        } finally { lifecycle.release(); }
    }
    public void executeCycle(long cycle) throws InterruptedException {
        requests.acquire();
        try {
            lifecycle.acquire();
            try {
                if (!started || closing || failure != null) { throw new IllegalStateException("Worker no disponible", failure); }
                if (cycle < 0 || cycle != nextCycle || cycle == Long.MAX_VALUE) { throw new IllegalArgumentException("Ciclo fuera de secuencia"); }
                requestedCycle = cycle; pending = true; begin.release();
            } finally { lifecycle.release(); }
            try { finished.acquire(); }
            catch (InterruptedException cancelled) {
                // La petición entregada puede seguir en vuelo: impedir reutilizar su confirmación.
                lifecycle.acquireUninterruptibly();
                try { closing = true; failure = cancelled; thread.interrupt(); }
                finally { lifecycle.release(); }
                throw cancelled;
            }
            if (failure != null || closing) { throw new IllegalStateException("Worker falló o fue cerrado", failure); }
            nextCycle++;
        } finally { requests.release(); }
    }
    private void run() {
        try {
            while (!closing) {
                begin.acquire();
                if (closing) { break; }
                try { target.onTick(requestedCycle); }
                catch (Throwable problem) { failure = problem; closing = true; }
                finally { completeRequest(); }
            }
        } catch (InterruptedException interrupted) {
            if (!closing) { failure = interrupted; }
            Thread.currentThread().interrupt();
        } finally { closing = true; completeRequest(); }
    }
    private void completeRequest() {
        lifecycle.acquireUninterruptibly();
        try { if (pending) { pending = false; finished.release(); } }
        finally { lifecycle.release(); }
    }
    public int computerId() { return computerId; }
    public boolean isAlive() { return thread.isAlive(); }
    @Override public void close() {
        lifecycle.acquireUninterruptibly();
        try { closing = true; thread.interrupt(); }
        finally { lifecycle.release(); }
        if (Thread.currentThread() == thread) { throw new IllegalStateException("El worker no puede esperarse a sí mismo"); }
        try { thread.join(2000); }
        catch (InterruptedException interrupted) { Thread.currentThread().interrupt(); throw new IllegalStateException("Cierre interrumpido", interrupted); }
        if (thread.isAlive()) { throw new IllegalStateException("El worker no confirmó su cierre"); }
    }
}
