package avilaos.tests;

import avilaos.structures.RingBuffer;

public final class RingBufferTest {
    private RingBufferTest() { }

    public static void run() {
        // Capacidad lógica independiente del almacenamiento ocupado, incluso tras crecer envuelto.
        RingBuffer<Integer> growing = new RingBuffer<>(Integer.MAX_VALUE);
        TestSupport.equal(Integer.MAX_VALUE, growing.capacity());
        for (int i = 0; i < 16; i++) { growing.put(i); }
        TestSupport.isTrue(!growing.isFull());
        for (int i = 0; i < 10; i++) { TestSupport.equal(i, growing.take()); }
        for (int i = 16; i < 40; i++) { growing.put(i); }
        for (int i = 10; i < 40; i++) { TestSupport.equal(i, growing.take()); }
        TestSupport.isTrue(growing.isEmpty());
        RingBuffer<Integer> bounded = new RingBuffer<>(17);
        for (int i = 0; i < 17; i++) { bounded.put(i); }
        TestSupport.isTrue(bounded.isFull());
        TestSupport.throwsType(IllegalStateException.class, () -> bounded.put(17));
        TestSupport.equal(0, bounded.get(0)); TestSupport.equal(16, bounded.get(16));
        rejectsInvalidCapacityAndPreservesFullContents();
        wrapsAroundInFifoOrder();
    }

    private static void rejectsInvalidCapacityAndPreservesFullContents() {
        TestSupport.throwsType(IllegalArgumentException.class, () -> new RingBuffer<>(0));
        TestSupport.throwsType(IllegalArgumentException.class, () -> new RingBuffer<>(-1));
        RingBuffer<Integer> buffer = new RingBuffer<>(1);
        TestSupport.equal(1, buffer.capacity());
        buffer.put(7);
        TestSupport.isTrue(buffer.isFull());
        TestSupport.throwsType(IllegalStateException.class, () -> buffer.put(8));
        TestSupport.equal(1, buffer.size());
        TestSupport.equal(7, buffer.take());
        TestSupport.throwsType(IllegalStateException.class, buffer::take);
        TestSupport.throwsType(IllegalArgumentException.class, () -> buffer.put(null));
        TestSupport.equal(0, buffer.size());
        TestSupport.isTrue(buffer.isEmpty());
    }

    private static void wrapsAroundInFifoOrder() {
        RingBuffer<Integer> buffer = new RingBuffer<>(3);
        buffer.put(1);
        buffer.put(2);
        buffer.put(3);
        TestSupport.equal(1, buffer.take());
        TestSupport.equal(2, buffer.take());
        buffer.put(4);
        buffer.put(5);
        TestSupport.equal(3, buffer.get(0));
        TestSupport.equal(4, buffer.get(1));
        TestSupport.equal(5, buffer.get(2));
        TestSupport.throwsType(IndexOutOfBoundsException.class, () -> buffer.get(-1));
        TestSupport.throwsType(IndexOutOfBoundsException.class, () -> buffer.get(3));
        TestSupport.equal(3, buffer.size());
        TestSupport.equal(3, buffer.take());
        TestSupport.equal(4, buffer.take());
        TestSupport.equal(5, buffer.take());
        buffer.put(6);
        TestSupport.equal(6, buffer.take());
        TestSupport.isTrue(buffer.isEmpty());
    }
}
