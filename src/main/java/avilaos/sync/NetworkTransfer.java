package avilaos.sync;
import avilaos.process.ProcessControlBlock;
/** Latencia previa a una operación, sin reservar permisos del buffer. */
public final class NetworkTransfer {
    private final ProcessControlBlock process; private final int bufferId; private final long availableCycle;
    public NetworkTransfer(ProcessControlBlock process, int bufferId, long issuedCycle, int latency) {
        if (process == null || bufferId <= 0 || issuedCycle < 0 || latency <= 0) { throw new IllegalArgumentException("Transferencia inválida"); }
        try { availableCycle = Math.addExact(issuedCycle, latency); }
        catch (ArithmeticException e) { throw new IllegalArgumentException("Transferencia fuera de rango", e); }
        this.process = process; this.bufferId = bufferId;
    }
    public ProcessControlBlock process() { return process; }
    public int bufferId() { return bufferId; }
    public long availableCycle() { return availableCycle; }
}
