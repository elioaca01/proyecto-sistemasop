package avilaos.metrics;

import avilaos.process.ProcessState;
import avilaos.process.TerminationReason;
import avilaos.simulation.ComputerSnapshot;
import avilaos.simulation.ProcessSnapshot;

/** Cada PCB pertenece a una sola cola o CPU; los terminados conservan su servicio. */
public final class MetricsCalculator {
    private MetricsCalculator() { }
    public static PerformanceMetrics computer(ComputerSnapshot computer, long cycles) {
        if (computer == null) { throw new IllegalArgumentException("Computador requerido"); }
        Totals t = new Totals();
        if (computer.running() != null) { t.add(computer.running()); }
        for (int i = 0; i < computer.newCount(); i++) { t.add(computer.newAt(i)); }
        for (int i = 0; i < computer.readyCount(); i++) { t.add(computer.readyAt(i)); }
        for (int i = 0; i < computer.blockedCount(); i++) { t.add(computer.blockedAt(i)); }
        for (int i = 0; i < computer.terminatedCount(); i++) { t.add(computer.terminatedAt(i)); }
        return t.snapshot(cycles, 1);
    }
    public static PerformanceMetrics aggregate(PerformanceMetrics[] machines, long cycles) {
        if (machines == null) { throw new IllegalArgumentException("Métricas requeridas"); }
        Totals t = new Totals(); int processors = 0;
        for (PerformanceMetrics m : machines) {
            if (m == null) { throw new IllegalArgumentException("Métrica nula"); }
            processors = Math.addExact(processors, m.processors());
            t.user = Math.addExact(t.user, m.userCycles()); t.kernel = Math.addExact(t.kernel, m.kernelCycles());
            t.completed = Math.addExact(t.completed, m.completed()); t.expired = Math.addExact(t.expired, m.expired());
            t.pending = Math.addExact(t.pending, m.pending()); t.responded = Math.addExact(t.responded, m.responded());
            t.admitted = Math.addExact(t.admitted, m.admitted()); t.responses += m.responseCycles(); t.squared += m.serviceSquared();
        }
        return t.snapshot(cycles, processors);
    }
    private static final class Totals {
        long user, kernel, completed, expired, pending, responded, admitted;
        double responses, squared;
        void add(ProcessSnapshot p) {
            user = Math.addExact(user, p.userCycles()); kernel = Math.addExact(kernel, p.kernelCycles());
            if (p.state() != ProcessState.TERMINATED) { pending++; }
            else if (p.terminationReason() == TerminationReason.COMPLETED) { completed++; }
            else if (p.terminationReason() == TerminationReason.DEADLINE_EXPIRED) { expired++; }
            if (p.firstRunCycle() >= 0) { responded++; responses += p.firstRunCycle() - p.arrivalCycle(); }
            if (p.everAdmitted()) {
                admitted++; double service = (double) p.userCycles() + p.kernelCycles(); squared += service * service;
            }
        }
        PerformanceMetrics snapshot(long cycles, int processors) {
            return new PerformanceMetrics(cycles, processors, user, kernel, completed, expired, pending, responded, responses, admitted, squared);
        }
    }
}
