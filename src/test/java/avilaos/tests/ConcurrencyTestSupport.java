package avilaos.tests;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;
/** Esperas acotadas para pruebas; ninguna aserción se ejecuta en los auxiliares. */
final class ConcurrencyTestSupport {
    private ConcurrencyTestSupport() { }
    static void acquire(Semaphore signal) {
        try { if (!signal.tryAcquire(2, TimeUnit.SECONDS)) { throw new AssertionError("Aviso no recibido"); } }
        catch (InterruptedException e) { Thread.currentThread().interrupt(); throw new AssertionError(e); }
    }
    // Estos lectores solo usan espera temporizada dentro de awaitNewer: verla prueba el registro.
    static void awaitTimedWaiting(Thread thread) {
        long start = System.nanoTime();
        while (thread.getState() != Thread.State.TIMED_WAITING) {
            if (!thread.isAlive() || System.nanoTime() - start > 2_000_000_000L) {
                throw new AssertionError("El lector no registró su espera: " + thread.getName());
            }
            Thread.yield();
        }
    }
    static void join(Thread thread) {
        try { thread.join(2000); }
        catch (InterruptedException e) { Thread.currentThread().interrupt(); throw new AssertionError(e); }
        if (thread.isAlive()) { throw new AssertionError("Hilo no terminó: " + thread.getName()); }
    }
}
