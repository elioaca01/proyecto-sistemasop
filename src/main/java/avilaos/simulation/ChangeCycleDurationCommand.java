package avilaos.simulation;
public record ChangeCycleDurationCommand(long cycleMillis) implements SimulationCommand {
    public ChangeCycleDurationCommand { if (cycleMillis <= 0) { throw new IllegalArgumentException("Duración positiva requerida"); } }
    @Override public long apply(Simulation simulation) { return simulation.applyDuration(cycleMillis); }
}
