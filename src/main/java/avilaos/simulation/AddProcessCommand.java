package avilaos.simulation;
import avilaos.process.ProcessDefinition;
public record AddProcessCommand(ProcessDefinition definition) implements SimulationCommand {
    public AddProcessCommand { if (definition == null) { throw new IllegalArgumentException("Definición requerida"); } }
    @Override public long apply(Simulation simulation) { return simulation.applyProcess(definition); }
}
