package avilaos.scheduling;

/** Construye una política sin incorporar estado del núcleo ni del hardware. */
public final class SchedulingPolicies {
    private SchedulingPolicies() { }
    public static SchedulingPolicy create(SchedulingPolicyType type) {
        if (type == null) { throw new IllegalArgumentException("Tipo de política requerido"); }
        return switch (type) {
            case FCFS -> new FcfsPolicy();
            case EDF -> new EdfPolicy();
            case ROUND_ROBIN -> new RoundRobinPolicy();
            case PREEMPTIVE_PRIORITY -> new PreemptivePriorityPolicy();
        };
    }
}
