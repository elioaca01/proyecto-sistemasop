package avilaos.scheduling;

import avilaos.process.ProcessControlBlock;
import avilaos.structures.OrderedProcessQueue;

/** Número menor significa mayor prioridad; un empate conserva al actual. */
public final class PreemptivePriorityPolicy implements SchedulingPolicy {
    @Override public SchedulingPolicyType type() { return SchedulingPolicyType.PREEMPTIVE_PRIORITY; }
    @Override public ProcessControlBlock select(OrderedProcessQueue ready) { reorder(ready); return ready.poll(); }
    @Override public boolean shouldPreempt(ProcessControlBlock running, OrderedProcessQueue ready, int sliceUsed, int quantum) {
        if (running == null) { return false; }
        reorder(ready);
        return !ready.isEmpty() && ready.peek().definition().priority() < running.definition().priority();
    }
    @Override public void reorder(OrderedProcessQueue ready) {
        ready.restoreFifo();
        ready.sort((a, b) -> Integer.compare(a.definition().priority(), b.definition().priority()));
    }
}
