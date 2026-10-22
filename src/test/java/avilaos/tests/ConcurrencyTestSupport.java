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
    static void join(Thread thread) {
        try { thread.join(2000); }
        catch (InterruptedException e) { Thread.currentThread().interrupt(); throw new AssertionError(e); }
        if (thread.isAlive()) { throw new AssertionError("Hilo no terminó: " + thread.getName()); }
    }
}
