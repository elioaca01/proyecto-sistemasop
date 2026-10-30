package avilaos.simulation;
import avilaos.scheduling.SchedulingPolicyType;
public record ChangeSchedulingCommand(int computerId, SchedulingPolicyType policy, int quantum) implements SimulationCommand {
    public ChangeSchedulingCommand {
        if (computerId <= 0 || policy == null || quantum <= 0) { throw new IllegalArgumentException("Planificación inválida"); }
    }
    @Override public long apply(Simulation simulation) { return simulation.applyScheduling(computerId, policy, quantum); }
}
