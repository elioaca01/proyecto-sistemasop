package avilaos.simulation;
/** Publicación inmutable y versionada; los arreglos nunca salen del objeto. */
public final class SimulationSnapshot {
    private final long version, nextCycle, cycleMillis;
    private final SimulationState state;
    private final String error;
    private final ComputerSnapshot[] computers;
    private final CommandResult[] results;
    public SimulationSnapshot(long version, long nextCycle, SimulationState state, long cycleMillis,
            String error, ComputerSnapshot[] computers, CommandResult[] results) {
        if (version < 0 || nextCycle < 0 || state == null || cycleMillis <= 0 || computers == null || results == null) {
            throw new IllegalArgumentException("Instantánea inválida");
        }
        this.computers = computers.clone(); this.results = results.clone();
        for (ComputerSnapshot c : this.computers) { if (c == null) { throw new IllegalArgumentException("Computador nulo"); } }
        for (CommandResult r : this.results) { if (r == null) { throw new IllegalArgumentException("Resultado nulo"); } }
        this.version = version; this.nextCycle = nextCycle; this.state = state;
        this.cycleMillis = cycleMillis; this.error = error;
    }
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
