package avilaos.metrics;

import avilaos.simulation.ComputerSnapshot;
import avilaos.sync.BufferSnapshot;

/** Bases por máquina y agregación ponderada de una frontera global confirmada. */
public final class MetricsSnapshot {
    private final int[] ids;
    private final PerformanceMetrics[] computers;
    private final PerformanceMetrics global;
    public record BufferMetrics(int id,long produced,long consumed,int occupancy,SemaphoreWaitMetrics waits) { }
    private final SemaphoreWaitMetrics waits;
    private final BufferMetrics[] bufferMetrics;
    private MetricsSnapshot(long cycles, ComputerSnapshot[] machines, BufferSnapshot[] buffers) {
        ids = new int[machines.length]; computers = new PerformanceMetrics[machines.length];
        for (int i = 0; i < machines.length; i++) {
            if (machines[i] == null) { throw new IllegalArgumentException("Computador nulo"); }
            ids[i] = machines[i].id(); computers[i] = MetricsCalculator.computer(machines[i], cycles);
        }
        global = MetricsCalculator.aggregate(computers, cycles);
        bufferMetrics = new BufferMetrics[buffers.length];
        SemaphoreWaitMetrics[] values = new SemaphoreWaitMetrics[buffers.length];
        for (int i = 0; i < buffers.length; i++) {
            if (buffers[i] == null) { throw new IllegalArgumentException("Buffer nulo"); }
            values[i] = buffers[i].waits();
            BufferSnapshot b = buffers[i]; bufferMetrics[i] = new BufferMetrics(b.definition().id(),b.produced(),b.consumed(),b.size(),b.waits());
        }
        waits = SemaphoreWaitMetrics.combine(values);
    }
    public static MetricsSnapshot calculate(long cycles, ComputerSnapshot[] computers, BufferSnapshot[] buffers) {
        if (cycles < 0 || computers == null || buffers == null) { throw new IllegalArgumentException("Datos de métricas inválidos"); }
        return new MetricsSnapshot(cycles, computers, buffers);
    }
    public PerformanceMetrics global() { return global; }
    public int bufferCount() { return bufferMetrics.length; }
    public BufferMetrics bufferAt(int index) { return bufferMetrics[index]; }
    public SemaphoreWaitMetrics waits() { return waits; }
    public int computerCount() { return computers.length; }
    public int computerIdAt(int index) { return ids[index]; }
    public PerformanceMetrics computerAt(int index) { return computers[index]; }
}
