package avilaos.tests;

public final class AllTests {
    private AllTests() { }

    public static void main(String[] args) {
        LinkedListTest.run();
        LinkedQueueTest.run();
        RingBufferTest.run();
        ProcessDefinitionTest.run();
        ProcessControlBlockTest.run();
        RamTest.run();
        SchedulingTest.run();
        MemoryAdmissionTest.run();
        CpuTest.run();
        IoControllerTest.run();
        ComputerTest.run();
        ReadyOrderTest.run();
        PolicyTest.run();
        System.out.println("OK: " + TestSupport.count() + " comprobaciones");
    }
}
