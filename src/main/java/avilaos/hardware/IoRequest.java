package avilaos.hardware;

import avilaos.process.ProcessControlBlock;

/** Ticket inmutable: solicita en t, espera d ciclos y completa al inicio de t+d+1. */
public final class IoRequest {
    private final ProcessControlBlock process;
    private final long completesAt;

    public IoRequest(ProcessControlBlock process, long issuedCycle, int duration) {
        if (process == null || issuedCycle < 0 || duration <= 0) {
            throw new IllegalArgumentException("Solicitud de E/S inválida");
        }
        try {
            completesAt = Math.addExact(Math.addExact(issuedCycle, 1L), duration);
        } catch (ArithmeticException overflow) {
            throw new IllegalArgumentException("Fin de E/S fuera de rango", overflow);
        }
        this.process = process;
    }
    public ProcessControlBlock process() { return process; }
    public long completesAt() { return completesAt; }
}
