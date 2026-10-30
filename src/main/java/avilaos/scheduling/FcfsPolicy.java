package avilaos.scheduling;

import avilaos.process.ProcessControlBlock;
import avilaos.structures.OrderedProcessQueue;

/** First Come, First Served: FIFO sin apropiación. */
public final class FcfsPolicy implements SchedulingPolicy {
    @Override public SchedulingPolicyType type() { return SchedulingPolicyType.FCFS; }
    @Override public ProcessControlBlock select(OrderedProcessQueue ready) { reorder(ready); return ready.poll(); }
    @Override public boolean shouldPreempt(ProcessControlBlock running, OrderedProcessQueue ready, int sliceUsed, int quantum) { return false; }
    @Override public void reorder(OrderedProcessQueue ready) { ready.restoreFifo(); }
}
