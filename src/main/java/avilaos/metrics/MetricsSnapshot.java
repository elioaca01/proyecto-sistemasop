package avilaos.metrics;

import avilaos.simulation.ComputerSnapshot;
import avilaos.sync.BufferSnapshot;

/** Bases por máquina y agregación ponderada de una frontera global confirmada. */
public final class MetricsSnapshot {
    private final int[] ids;
    private final PerformanceMetrics[] computers;
    private final PerformanceMetrics global;
    private final SemaphoreWaitMetrics waits;
    private MetricsSnapshot(long cycles, ComputerSnapshot[] machines, BufferSnapshot[] buffers) {
        ids = new int[machines.length]; computers = new PerformanceMetrics[machines.length];
        for (int i = 0; i < machines.length; i++) {
            if (machines[i] == null) { throw new IllegalArgumentException("Computador nulo"); }
            ids[i] = machines[i].id(); computers[i] = MetricsCalculator.computer(machines[i], cycles);
        }
        global = MetricsCalculator.aggregate(computers, cycles);
        SemaphoreWaitMetrics[] values = new SemaphoreWaitMetrics[buffers.length];
        for (int i = 0; i < buffers.length; i++) {
            if (buffers[i] == null) { throw new IllegalArgumentException("Buffer nulo"); }
            values[i] = buffers[i].waits();
        }
        waits = SemaphoreWaitMetrics.combine(values);
    }
    public static MetricsSnapshot calculate(long cycles, ComputerSnapshot[] computers, BufferSnapshot[] buffers) {
        if (cycles < 0 || computers == null || buffers == null) { throw new IllegalArgumentException("Datos de métricas inválidos"); }
        return new MetricsSnapshot(cycles, computers, buffers);
    }
    public PerformanceMetrics global() { return global; }
    public SemaphoreWaitMetrics waits() { return waits; }
    public int computerCount() { return computers.length; }
    public int computerIdAt(int index) { return ids[index]; }
    public PerformanceMetrics computerAt(int index) { return computers[index]; }
}
