package avilaos.kernel;

import avilaos.process.ProcessControlBlock;
import avilaos.scheduling.SchedulingPolicy;
import avilaos.scheduling.SchedulingPolicyType;
import avilaos.structures.OrderedProcessQueue;

/** Selecciona por política; el Kernel conserva la propiedad de las transiciones. */
public final class Scheduler {
    private final OrderedProcessQueue ready;
    private SchedulingPolicy policy;

    public Scheduler(OrderedProcessQueue ready, SchedulingPolicy policy) {
        if (ready == null || policy == null) { throw new IllegalArgumentException("Cola y política requeridas"); }
        this.ready = ready;
        this.policy = policy;
    }
    public void setPolicy(SchedulingPolicy policy) {
        if (policy == null) { throw new IllegalArgumentException("Política requerida"); }
        this.policy = policy;
        reorder();
    }
    public SchedulingPolicyType policyType() { return policy.type(); }
    public void reorder() { policy.reorder(ready); }
    public boolean shouldPreempt(ProcessControlBlock running, int sliceUsed, int quantum) {
        return policy.shouldPreempt(running, ready, sliceUsed, quantum);
    }
    public ProcessControlBlock select() {
        ProcessControlBlock selected = policy.select(ready);
        policy.reorder(ready);
        return selected;
    }
}
