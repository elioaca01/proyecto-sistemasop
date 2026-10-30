package avilaos.tests;

import avilaos.process.ProcessControlBlock;
import avilaos.process.ProcessState;
import avilaos.structures.OrderedProcessQueue;

public final class ReadyOrderTest {
    public static void run() {
        OrderedProcessQueue q = new OrderedProcessQueue();
        q.restoreFifo(); q.sort((a, b) -> 0);
        TestSupport.equal(null, q.poll());
        ProcessControlBlock a = SchedulingTest.ready(30, 1);
        ProcessControlBlock b = SchedulingTest.ready(10, 2);
        ProcessControlBlock c = SchedulingTest.ready(20, 0);
        q.addLast(a); q.restoreFifo();
        TestSupport.equal(a, q.peek());
        q.addLast(b); q.addLast(c);
        q.sort((left, right) -> Integer.compare(left.definition().priority(), right.definition().priority()));
        order(q, c, a, b);
        q.sort((left, right) -> 0);
        order(q, c, a, b); // sort sigue siendo estable sobre el orden actual.
        q.restoreFifo(); order(q, a, b, c);
        TestSupport.equal(b, q.remove(b.id())); q.addLast(b);
        q.sort((left, right) -> Long.compare(left.id(), right.id()));
        q.restoreFifo(); order(q, a, c, b);
        TestSupport.throwsType(IllegalArgumentException.class, () -> q.addLast(null));
        TestSupport.throwsType(IllegalArgumentException.class, () -> q.addLast(SchedulingTest.process(40, 0)));
        TestSupport.throwsType(IllegalArgumentException.class, () -> q.addLast(SchedulingTest.ready(30, 0)));
        TestSupport.throwsType(IllegalArgumentException.class, () -> q.sort(null));
        order(q, a, c, b);
        TestSupport.equal(ProcessState.READY, a.state());
        TestSupport.equal(a, q.poll()); TestSupport.equal(c, q.poll()); TestSupport.equal(b, q.poll());
        q.restoreFifo(); TestSupport.isTrue(q.isEmpty());
        q.addLast(b); q.addLast(a); q.restoreFifo();
        TestSupport.equal(b, q.poll()); TestSupport.equal(a, q.poll());
    }
    private static void order(OrderedProcessQueue q, ProcessControlBlock a, ProcessControlBlock b, ProcessControlBlock c) {
        TestSupport.equal(3, q.size());
        TestSupport.equal(a, q.get(0)); TestSupport.equal(b, q.get(1)); TestSupport.equal(c, q.get(2));
    }
}
