package avilaos.simulation;
import avilaos.scheduling.SchedulingPolicyType;
/** Configuración inmutable previa al arranque de hilos. */
public record ComputerConfig(int id, int ramCapacity, SchedulingPolicyType policy, int quantum) {
    public ComputerConfig {
        if (id <= 0 || ramCapacity <= 0 || policy == null || quantum <= 0) {
            throw new IllegalArgumentException("Configuración de computador inválida");
        }
    }
}
