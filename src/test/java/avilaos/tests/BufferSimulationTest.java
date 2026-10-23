package avilaos.tests;
import avilaos.simulation.*;
import avilaos.process.*;
import avilaos.sync.*;
import static avilaos.tests.TestSupport.*;

public final class BufferSimulationTest {
    private BufferSimulationTest() { }
    static ProcessDefinition definition(int computer, ProcessType type) {
        return new ProcessDefinition("buffer", computer, type, 6, 2, 0, 100, 0,0,1,1,1);
    }
    static SimulationSnapshot step(Simulation sim) {
        long cycle = sim.snapshot().nextCycle(); sim.step();
        return SimulationTest.await(sim, cycle + 1, SimulationState.PAUSED);
    }
    public static void run() {
        throwsType(IllegalArgumentException.class, () -> new Simulation(SimulationTest.configs(), 1, -1));
        throwsType(IllegalArgumentException.class, () -> new AddBufferCommand(null));
        try (Simulation sim = new Simulation(SimulationTest.configs(), 100, 2)) {
            throwsType(IllegalStateException.class, () -> new AddBufferCommand(new BufferDefinition(1,1,1)).apply(sim));
            sim.enqueue(new AddProcessCommand(definition(1, ProcessType.PRODUCER)));
            sim.enqueue(new AddBufferCommand(new BufferDefinition(1,1,3)));
            sim.enqueue(new AddProcessCommand(definition(1, ProcessType.PRODUCER)));
            sim.enqueue(new AddBufferCommand(new BufferDefinition(2,2,4)));
            sim.enqueue(new AddBufferCommand(new BufferDefinition(1,1,1)));
            // El lote se aplica antes de admitir procesos: quedan 7 unidades disponibles.
            sim.enqueue(new AddBufferCommand(new BufferDefinition(3,1,8)));
            sim.enqueue(new AddBufferCommand(new BufferDefinition(4,9,1)));
            SimulationSnapshot first = step(sim);
            equal(2, first.networkLatency()); equal(2, first.bufferCount()); equal(7, first.resultCount());
            isTrue(!first.resultAt(0).accepted()); isTrue(first.resultAt(1).accepted()); equal(-1L, first.resultAt(1).processId());
            equal(1L, first.resultAt(2).processId());
            for (int i = 4; i < 7; i++) { isTrue(!first.resultAt(i).accepted()); }
            equal(5, first.computerAt(0).ramUsed()); equal(4, first.computerAt(1).ramUsed());
            BufferSnapshot empty = first.bufferAt(0); equal(3, empty.emptyValue()); equal(0, empty.size()); equal(1, empty.operationCount());
            equal(BufferPhase.WAIT_EMPTY, first.computerAt(0).running().operation().phase());
            SimulationSnapshot inserted = first;
            for (int i = 1; i <= 3; i++) { inserted = step(sim); }
            BufferSnapshot saved = inserted.bufferAt(0); equal(1, saved.size()); equal(1L, saved.produced()); equal(1L, saved.mutexOwnerId());
            equal(1L, saved.itemAt(0).producerId()); equal(1, saved.itemAt(0).producerComputerId()); equal(1L, saved.itemAt(0).id());
            equal(true, saved.operationAt(0).modified());
            step(sim); SimulationSnapshot end = step(sim);
            equal(3, end.computerAt(0).ramUsed()); equal(0, end.bufferAt(0).operationCount()); equal(1, end.eventCount());
            equal(1L, end.eventAt(0).processId()); equal(TerminationReason.COMPLETED, end.computerAt(0).terminatedAt(0).terminationReason());
            equal(0, empty.size()); equal(0L, empty.produced()); equal(1L, saved.mutexOwnerId()); equal(true, saved.operationAt(0).modified());
            ComputerSnapshot[] computers = {end.computerAt(0)}; CommandResult[] results = {end.resultAt(1)};
            BufferSnapshot[] buffers = {saved}; SimulationEvent[] events = {end.eventAt(0)};
            SimulationSnapshot copy = new SimulationSnapshot(1, 6, SimulationState.PAUSED, 100, null, computers, results, 2, buffers, events);
            computers[0] = null; results[0] = null; buffers[0] = null; events[0] = null;
            equal(1, copy.computerAt(0).id()); equal(1, copy.bufferAt(0).size()); equal(1L, copy.eventAt(0).processId()); isTrue(copy.resultAt(0).accepted());
            SimulationSnapshot legacy = new SimulationSnapshot(0,0,SimulationState.PAUSED,1,null,new ComputerSnapshot[0],new CommandResult[0]);
            equal(0, legacy.networkLatency()); equal(0, legacy.bufferCount()); equal(0, legacy.eventCount());
        }
        try (Simulation sim = new Simulation(SimulationTest.configs(), 100, 2)) {
            sim.enqueue(new AddBufferCommand(new BufferDefinition(1,1,1)));
            sim.enqueue(new AddProcessCommand(definition(2, ProcessType.CONSUMER)));
            SimulationSnapshot blocked = step(sim);
            equal(BlockReason.NETWORK, blocked.computerAt(1).blockedAt(0).blockReason());
            equal(2L, blocked.computerAt(1).blockedAt(0).operation().networkAvailableAt());
            step(sim); SimulationSnapshot sem = step(sim);
            equal(BlockReason.SEMAPHORE_FULL, sem.computerAt(1).blockedAt(0).blockReason());
            equal(1, sem.bufferAt(0).fullWaitingCount()); equal(1L, sem.bufferAt(0).fullWaitingAt(0));
            equal(0, blocked.bufferAt(0).fullWaitingCount()); equal(0, blocked.computerAt(1).blockedAt(0).pc());
        }
    }
}
