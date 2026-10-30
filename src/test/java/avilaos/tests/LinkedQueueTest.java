package avilaos.tests;

import avilaos.structures.LinkedQueue;

public final class LinkedQueueTest {
    private LinkedQueueTest() { }

    public static void run() {
        LinkedQueue<String> queue = new LinkedQueue<>();
        TestSupport.isTrue(queue.isEmpty());
        TestSupport.equal(null, queue.dequeue());
        TestSupport.equal(null, queue.peek());
        queue.enqueue("A");
        queue.enqueue("B");
        queue.enqueue("C");
        TestSupport.equal("A", queue.peek());
        TestSupport.equal(3, queue.size());
        TestSupport.equal("B", queue.get(1));
        TestSupport.throwsType(IllegalArgumentException.class, () -> queue.enqueue(null));
        TestSupport.throwsType(IndexOutOfBoundsException.class, () -> queue.get(-1));
        TestSupport.throwsType(IndexOutOfBoundsException.class, () -> queue.get(3));
        TestSupport.equal(3, queue.size());
        TestSupport.equal("A", queue.dequeue());
        TestSupport.equal("B", queue.dequeue());
        TestSupport.equal("C", queue.dequeue());
        TestSupport.equal(null, queue.dequeue());
        queue.enqueue("D");
        TestSupport.equal("D", queue.dequeue());
        TestSupport.isTrue(queue.isEmpty());
    }
}
