package avilaos.tests;

public final class AllTests {
    private AllTests() { }

    public static void main(String[] args) {
        // Los componentes se prueban sin ventanas; App corre en otra JVM con su interfaz normal.
        System.setProperty("java.awt.headless", "true");
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
        PreemptiveKernelTest.run();
        SchedulingChangeTest.run();
        SnapshotTest.run();
        SnapshotStoreTest.run();
        ComputerWorkerTest.run();
        GlobalClockTest.run();
        SimulationTest.run();
        ClusterIntegrationTest.run();
        SimulatedSemaphoreTest.run();
        BoundedBufferTest.run();
        BufferProgramTest.run();
        BufferOperationTest.run();
        BufferNetworkTest.run();
        BufferKernelTest.run();
        BufferSimulationTest.run();
        ProducerConsumerIntegrationTest.run();
        SimulationConfigTest.run();
        EventLogTest.run();
        DecisionLogTest.run();
        UiSessionTest.run();
        UiSessionLifecycleTest.run();
        UiTableModelTest.run();
        UiFormTest.run();
        ComputerPanelTest.run();
        BufferPanelTest.run();
        EventPanelTest.run();
        UiControllerTest.run();
        UiIntegrationTest.run();
        ScenarioJsonTest.run();
        ConfigurationStoreTest.run();
        UiSessionPersistenceTest.run();
        ScenarioControlsTest.run();
        ScenarioIntegrationTest.run();
        PerformanceMetricsTest.run();
        SemaphoreWaitMetricsTest.run();
        MetricsHistoryTest.run();
        MetricsPanelTest.run();
        MetricsIntegrationTest.run();
        System.out.println("OK: " + TestSupport.count() + " comprobaciones");
    }
}
