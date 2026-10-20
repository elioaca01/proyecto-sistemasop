package avilaos.kernel;

import avilaos.process.ProcessControlBlock;
import avilaos.scheduling.SchedulingPolicy;
import avilaos.structures.OrderedProcessQueue;

/** Selecciona por política; el Kernel conserva la propiedad de las transiciones. */
public final class Scheduler {
    private final OrderedProcessQueue ready;
    private final SchedulingPolicy policy;

    public Scheduler(OrderedProcessQueue ready, SchedulingPolicy policy) {
        if (ready == null || policy == null) { throw new IllegalArgumentException("Cola y política requeridas"); }
        this.ready = ready;
        this.policy = policy;
    }
    public ProcessControlBlock select() {
        ProcessControlBlock selected = policy.select(ready);
        policy.reorder(ready);
        return selected;
    }
}
