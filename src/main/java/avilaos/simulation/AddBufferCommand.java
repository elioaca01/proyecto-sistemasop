package avilaos.simulation;
import avilaos.sync.BufferDefinition;
public record AddBufferCommand(BufferDefinition definition) implements SimulationCommand {
    public AddBufferCommand { if (definition == null) { throw new IllegalArgumentException("Definición de buffer requerida"); } }
    @Override public long apply(Simulation simulation) { return simulation.applyBuffer(definition); }
}
