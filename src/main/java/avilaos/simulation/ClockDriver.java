package avilaos.simulation;
/** Trabajo y publicación del propietario del modelo; invocados solo por el reloj. */
public interface ClockDriver {
    long advanceCycle(long cycle);
    void publishState(long nextCycle, SimulationState state, long cycleMillis, String error);
    void shutdown();
}
