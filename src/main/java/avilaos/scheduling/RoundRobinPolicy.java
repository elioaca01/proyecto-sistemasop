package avilaos.scheduling;

import avilaos.process.ProcessControlBlock;
import avilaos.structures.OrderedProcessQueue;

/** Turnos FIFO, contados por instrucciones ejecutadas de usuario y núcleo. */
public final class RoundRobinPolicy implements SchedulingPolicy {
    @Override public SchedulingPolicyType type() { return SchedulingPolicyType.ROUND_ROBIN; }
    @Override public ProcessControlBlock select(OrderedProcessQueue ready) { reorder(ready); return ready.poll(); }
    @Override public boolean shouldPreempt(ProcessControlBlock running, OrderedProcessQueue ready, int sliceUsed, int quantum) {
        if (sliceUsed < 0 || quantum <= 0) { throw new IllegalArgumentException("Turno o quantum inválidos"); }
        return running != null && sliceUsed >= quantum;
    }
    @Override public void reorder(OrderedProcessQueue ready) { ready.restoreFifo(); }
}
