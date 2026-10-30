package avilaos.simulation;
public record SimulationEvent(long cycle, int computerId, long processId, String message) { }
