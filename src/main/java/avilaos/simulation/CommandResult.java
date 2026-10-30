package avilaos.simulation;
/** Resultado estable de una solicitud; -1 identifica ausencia de proceso/ciclo aplicado. */
public record CommandResult(long requestId, long appliedCycle, boolean accepted, long processId, String message) { }
