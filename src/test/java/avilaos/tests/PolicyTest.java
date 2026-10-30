package avilaos.tests;

import avilaos.kernel.Scheduler;
import avilaos.process.*;
import avilaos.scheduling.*;
import avilaos.structures.OrderedProcessQueue;

public final class PolicyTest {
    static ProcessControlBlock ready(long id, int priority, long deadline, long arrival) {
        ProcessControlBlock p = new ProcessControlBlock(id, new ProcessDefinition("P" + id, 1,
                ProcessType.CPU_BOUND, 3, 1, priority, deadline, 0, 0, 0, 0, 0), arrival);
        p.admit(); return p;
    }
    public static void run() {
        selectsByKeyAndRestoresFifo(); preemptsStrictlyBetter(); roundRobinAndFactory();
    }
    private static void selectsByKeyAndRestoresFifo() {
        for (SchedulingPolicy policy : new SchedulingPolicy[]{new EdfPolicy(), new PreemptivePriorityPolicy()}) {
            OrderedProcessQueue q = new OrderedProcessQueue();
            ProcessControlBlock a = ready(30, 2, 30, 0), b = ready(10, 0, 10, 0), c = ready(20, 0, 10, 0);
            q.addLast(a); q.addLast(b); q.addLast(c);
            policy.reorder(q);
            TestSupport.equal(3, q.size()); TestSupport.equal(b, q.get(0)); TestSupport.equal(c, q.get(1));
            TestSupport.equal(0, a.pc()); TestSupport.equal(1, a.assignedMemory());
            new FcfsPolicy().reorder(q); TestSupport.equal(a, q.peek());
            policy.reorder(q); new RoundRobinPolicy().reorder(q); TestSupport.equal(a, q.peek());
            TestSupport.equal(b, policy.select(q)); TestSupport.equal(ProcessState.READY, b.state());
            TestSupport.equal(c, policy.select(q)); TestSupport.equal(a, policy.select(q));
            TestSupport.equal(null, policy.select(q));
        }
        OrderedProcessQueue q = new OrderedProcessQueue();
        ProcessControlBlock x = ready(1, 1, 30, 0), y = ready(2, 1, 10, 0);
        q.addLast(x); q.addLast(y); new EdfPolicy().reorder(q);
        TestSupport.equal(y, q.peek());
        Scheduler scheduler = new Scheduler(q, new EdfPolicy());
        scheduler.setPolicy(new PreemptivePriorityPolicy());
        TestSupport.equal(SchedulingPolicyType.PREEMPTIVE_PRIORITY, scheduler.policyType());
        TestSupport.equal(x, q.peek()); // El empate recupera el orden de entrada.
        scheduler.setPolicy(new EdfPolicy()); TestSupport.equal(y, q.peek());
        TestSupport.throwsType(IllegalArgumentException.class, () -> scheduler.setPolicy(null));
        TestSupport.equal(SchedulingPolicyType.EDF, scheduler.policyType());
        scheduler.setPolicy(new FcfsPolicy()); scheduler.reorder(); TestSupport.equal(x, scheduler.select());
        TestSupport.equal(y, scheduler.select());
    }
    private static void preemptsStrictlyBetter() {
        for (SchedulingPolicy policy : new SchedulingPolicy[]{new EdfPolicy(), new PreemptivePriorityPolicy()}) {
            OrderedProcessQueue q = new OrderedProcessQueue();
            ProcessControlBlock running = ready(1, 2, 20, 0); running.dispatch(0);
            TestSupport.isTrue(!policy.shouldPreempt(running, q, 1, 2));
            ProcessControlBlock equal = ready(2, 2, 20, 0); q.addLast(equal);
            TestSupport.isTrue(!policy.shouldPreempt(running, q, 1, 2));
            ProcessControlBlock worse = ready(3, 3, 30, 0); q.addLast(worse);
            TestSupport.isTrue(!policy.shouldPreempt(running, q, 1, 2));
            ProcessControlBlock better = ready(4, 0, 10, 0); q.addLast(better);
            Scheduler scheduler = new Scheduler(q, policy);
            TestSupport.isTrue(scheduler.shouldPreempt(running, 1, 2));
            TestSupport.equal(ProcessState.RUNNING, running.state());
            TestSupport.equal(0, running.pc()); TestSupport.equal(3, q.size());
        }
        EdfPolicy edf = new EdfPolicy(); OrderedProcessQueue q = new OrderedProcessQueue();
        ProcessControlBlock early = ready(1, 0, 10, 0); early.dispatch(0);
        ProcessControlBlock late = ready(2, 0, 1, 100); q.addLast(late);
        TestSupport.isTrue(!edf.shouldPreempt(early, q, 0, 2));
        early.preempt(); q.addLast(early); TestSupport.equal(early, edf.select(q));
        OrderedProcessQueue extremes = new OrderedProcessQueue();
        ProcessControlBlock max = ready(3, Integer.MAX_VALUE, Long.MAX_VALUE, 0);
        ProcessControlBlock min = ready(4, 0, 1, 0); extremes.addLast(max); extremes.addLast(min);
        TestSupport.equal(min, edf.select(extremes));
        extremes.addLast(min); TestSupport.equal(min, new PreemptivePriorityPolicy().select(extremes));
    }
    private static void roundRobinAndFactory() {
        OrderedProcessQueue q = new OrderedProcessQueue();
        ProcessControlBlock running = ready(1, 0, 10, 0); running.dispatch(0);
        RoundRobinPolicy rr = new RoundRobinPolicy();
        TestSupport.isTrue(!rr.shouldPreempt(running, q, 1, 2));
        TestSupport.isTrue(rr.shouldPreempt(running, q, 2, 2));
        q.addLast(ready(2, 0, 10, 0));
        TestSupport.isTrue(rr.shouldPreempt(running, q, 2, 2));
        TestSupport.throwsType(IllegalArgumentException.class, () -> rr.shouldPreempt(running, q, 0, 0));
        TestSupport.throwsType(IllegalArgumentException.class, () -> rr.shouldPreempt(running, q, 0, -1));
        TestSupport.throwsType(IllegalArgumentException.class, () -> rr.shouldPreempt(running, q, -1, 2));
        TestSupport.isTrue(!new FcfsPolicy().shouldPreempt(running, q, Integer.MAX_VALUE, 1));
        for (SchedulingPolicyType type : SchedulingPolicyType.values()) {
            SchedulingPolicy policy = SchedulingPolicies.create(type);
            TestSupport.equal(type, policy.type());
            TestSupport.isTrue(!policy.shouldPreempt(null, q, 0, 2));
        }
        TestSupport.throwsType(IllegalArgumentException.class, () -> SchedulingPolicies.create(null));
    }
}
