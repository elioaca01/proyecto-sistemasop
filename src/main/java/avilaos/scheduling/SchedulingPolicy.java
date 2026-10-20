package avilaos.scheduling;

import avilaos.process.ProcessControlBlock;
import avilaos.structures.OrderedProcessQueue;

public interface SchedulingPolicy {
    ProcessControlBlock select(OrderedProcessQueue ready);
    boolean shouldPreempt(ProcessControlBlock running, OrderedProcessQueue ready, int sliceUsed, int quantum);
    void reorder(OrderedProcessQueue ready);
}
