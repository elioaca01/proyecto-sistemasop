package avilaos.tests;

public final class AllTests {
    private AllTests() { }

    public static void main(String[] args) {
        LinkedListTest.run();
        LinkedQueueTest.run();
        RingBufferTest.run();
        ProcessDefinitionTest.run();
        System.out.println("OK: " + TestSupport.count() + " comprobaciones");
    }
}
