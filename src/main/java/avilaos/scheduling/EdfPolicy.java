package avilaos.scheduling;

import avilaos.process.ProcessControlBlock;
import avilaos.structures.OrderedProcessQueue;

/** Earliest Deadline First, con apropiación solo por deadline absoluto menor. */
public final class EdfPolicy implements SchedulingPolicy {
    @Override public SchedulingPolicyType type() { return SchedulingPolicyType.EDF; }
    @Override public ProcessControlBlock select(OrderedProcessQueue ready) { reorder(ready); return ready.poll(); }
    @Override public boolean shouldPreempt(ProcessControlBlock running, OrderedProcessQueue ready, int sliceUsed, int quantum) {
        if (running == null) { return false; }
        reorder(ready);
        return !ready.isEmpty() && ready.peek().deadlineAt() < running.deadlineAt();
    }
    @Override public void reorder(OrderedProcessQueue ready) {
        ready.restoreFifo();
        ready.sort((a, b) -> Long.compare(a.deadlineAt(), b.deadlineAt()));
    }
}
