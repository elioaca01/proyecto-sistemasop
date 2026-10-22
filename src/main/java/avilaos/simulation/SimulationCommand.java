package avilaos.simulation;
/** Órdenes inmutables; su aplicación pertenece exclusivamente al coordinador. */
public sealed interface SimulationCommand permits AddProcessCommand, ChangeSchedulingCommand, ChangeCycleDurationCommand {
    long apply(Simulation simulation);
}
