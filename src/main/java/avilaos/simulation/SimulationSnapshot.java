package avilaos.simulation;
import avilaos.sync.BufferSnapshot;
import avilaos.metrics.*;
/** Publicación inmutable y versionada; los arreglos nunca salen del objeto. */
public final class SimulationSnapshot {
    private final long version, nextCycle, cycleMillis;
    private final SimulationState state;
    private final String error;
    private final ComputerSnapshot[] computers;
    private final CommandResult[] results;
    private final int networkLatency;
    private final BufferSnapshot[] buffers;
    private final SimulationEvent[] events;
    private final MetricsSnapshot metrics;
    private final UtilizationSample[] samples;
    public SimulationSnapshot(long version, long nextCycle, SimulationState state, long cycleMillis,
            String error, ComputerSnapshot[] computers, CommandResult[] results) {
        this(version, nextCycle, state, cycleMillis, error, computers, results, 0, new BufferSnapshot[0], new SimulationEvent[0]);
    }
    public SimulationSnapshot(long version, long nextCycle, SimulationState state, long cycleMillis,
            String error, ComputerSnapshot[] computers, CommandResult[] results, int networkLatency, BufferSnapshot[] buffers, SimulationEvent[] events) {
        this(version, nextCycle, state, cycleMillis, error, computers, results, networkLatency, buffers, events,
                MetricsSnapshot.calculate(nextCycle, computers, buffers), new UtilizationSample[0]);
    }
    public SimulationSnapshot(long version, long nextCycle, SimulationState state, long cycleMillis,
            String error, ComputerSnapshot[] computers, CommandResult[] results, int networkLatency, BufferSnapshot[] buffers,
            SimulationEvent[] events, MetricsSnapshot metrics, UtilizationSample[] samples) {
        if (version < 0 || nextCycle < 0 || state == null || cycleMillis <= 0 || computers == null || results == null
                || networkLatency < 0 || buffers == null || events == null || metrics == null || samples == null) {
            throw new IllegalArgumentException("Instantánea inválida");
        }
        this.metrics = metrics; this.samples = samples.clone();
        for (UtilizationSample sample : this.samples) { if (sample == null) { throw new IllegalArgumentException("Muestra nula"); } }
        this.networkLatency = networkLatency; this.buffers = buffers.clone(); this.events = events.clone();
        for (BufferSnapshot b : this.buffers) { if (b == null) { throw new IllegalArgumentException("Buffer nulo"); } }
        for (SimulationEvent e : this.events) { if (e == null) { throw new IllegalArgumentException("Evento nulo"); } }
        this.computers = computers.clone(); this.results = results.clone();
        for (ComputerSnapshot c : this.computers) { if (c == null) { throw new IllegalArgumentException("Computador nulo"); } }
        for (CommandResult r : this.results) { if (r == null) { throw new IllegalArgumentException("Resultado nulo"); } }
        this.version = version; this.nextCycle = nextCycle; this.state = state;
        this.cycleMillis = cycleMillis; this.error = error;
    }
    public MetricsSnapshot metrics() { return metrics; }
    public int sampleCount() { return samples.length; }
    public UtilizationSample sampleAt(int index) { return samples[index]; }
    public int networkLatency() { return networkLatency; }
    public int bufferCount() { return buffers.length; }
    public BufferSnapshot bufferAt(int index) { return buffers[index]; }
    public int eventCount() { return events.length; }
    public SimulationEvent eventAt(int index) { return events[index]; }
    public long version() { return version; }
    public long nextCycle() { return nextCycle; }
    public SimulationState state() { return state; }
    public long cycleMillis() { return cycleMillis; }
    public String error() { return error; }
    public int computerCount() { return computers.length; }
    public ComputerSnapshot computerAt(int index) { return computers[index]; }
    public int resultCount() { return results.length; }
    public CommandResult resultAt(int index) { return results[index]; }
}
