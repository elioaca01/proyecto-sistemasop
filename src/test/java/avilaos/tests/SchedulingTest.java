package avilaos.tests;

import avilaos.process.*;
import avilaos.structures.OrderedProcessQueue;
import avilaos.scheduling.*;
import avilaos.kernel.Scheduler;

public final class SchedulingTest {
    static ProcessControlBlock process(long id, int priority) {
        return new ProcessControlBlock(id, new ProcessDefinition("P" + id, 1,
                ProcessType.CPU_BOUND, 3, 1, priority, 10, 0, 0, 0, 0, 0), 0);
    }
    static ProcessControlBlock ready(long id, int priority) {
        ProcessControlBlock p = process(id, priority);
        p.admit();
        return p;
    }
    public static void run() {
        OrderedProcessQueue q = new OrderedProcessQueue();
        FcfsPolicy policy = new FcfsPolicy();
        Scheduler scheduler = new Scheduler(q, policy);
        ProcessControlBlock a = ready(1, 2), b = ready(2, 1), c = ready(3, 2);
        TestSupport.isTrue(q.isEmpty());
        TestSupport.equal(null, q.peek());
        TestSupport.equal(null, q.remove(9));
        q.addLast(a); q.addLast(b); q.addLast(c);
        TestSupport.equal(a, q.peek());
        TestSupport.equal(a, scheduler.select());
        TestSupport.equal(ProcessState.READY, a.state());
        TestSupport.equal(b, scheduler.select());
        TestSupport.equal(c, scheduler.select());
        TestSupport.equal(null, scheduler.select());
        TestSupport.isTrue(!policy.shouldPreempt(a, q, 100, 1));
        q.addLast(a); q.addLast(b); q.addLast(c);
        TestSupport.equal(b, q.remove(2));
        TestSupport.equal(a, q.get(0));
        TestSupport.equal(c, q.get(1));
        b.dispatch(0); b.block(BlockReason.IO); b.unblock(); q.addLast(b);
        TestSupport.equal(b, q.get(2));
        TestSupport.throwsType(IllegalArgumentException.class, () -> q.addLast(null));
        TestSupport.throwsType(IllegalArgumentException.class, () -> q.addLast(process(4, 0)));
        TestSupport.throwsType(IllegalArgumentException.class, () -> q.addLast(ready(1, 0)));
        TestSupport.equal(3, q.size());
        q.sort((left, right) -> Integer.compare(left.definition().priority(), right.definition().priority()));
        TestSupport.equal(b, q.poll());
        TestSupport.equal(a, q.poll());
        TestSupport.equal(c, q.poll());
        final int[] reordered = {0};
        SchedulingPolicy last = new SchedulingPolicy() {
            public SchedulingPolicyType type() { return SchedulingPolicyType.FCFS; }
            public ProcessControlBlock select(OrderedProcessQueue queue) {
                return queue.isEmpty() ? null : queue.remove(queue.get(queue.size() - 1).id());
            }
            public boolean shouldPreempt(ProcessControlBlock running, OrderedProcessQueue queue, int used, int quantum) { return false; }
            public void reorder(OrderedProcessQueue queue) { reordered[0]++; }
        };
        q.addLast(a); q.addLast(b);
        Scheduler injected = new Scheduler(q, last);
        TestSupport.equal(b, injected.select());
        TestSupport.equal(1, reordered[0]);
        TestSupport.equal(a, injected.select());
        TestSupport.equal(null, injected.select());
        TestSupport.equal(3, reordered[0]);
        TestSupport.throwsType(IllegalArgumentException.class, () -> new Scheduler(null, policy));
        TestSupport.throwsType(IllegalArgumentException.class, () -> new Scheduler(q, null));
    }
}
