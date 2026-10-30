package avilaos.tests;
import avilaos.simulation.*;
import avilaos.process.*;
import avilaos.scheduling.SchedulingPolicyType;

public final class SimulationTest {
    static ComputerConfig[] configs() {
        return new ComputerConfig[]{new ComputerConfig(2, 10, SchedulingPolicyType.FCFS, 2),
                new ComputerConfig(1, 10, SchedulingPolicyType.FCFS, 2)};
    }
    static ProcessDefinition cpu(int computer, int instructions) {
        return new ProcessDefinition("P", computer, ProcessType.CPU_BOUND, instructions, 4, 0, 20, 0, 0, 0, 0, 0);
    }
    public static void run() {
        TestSupport.throwsType(IllegalArgumentException.class, () -> new Simulation(null, 1));
        TestSupport.throwsType(IllegalArgumentException.class, () -> new Simulation(new ComputerConfig[0], 1));
        TestSupport.throwsType(IllegalArgumentException.class, () -> new Simulation(new ComputerConfig[]{configs()[0]}, 1));
        TestSupport.throwsType(IllegalArgumentException.class, () -> new Simulation(new ComputerConfig[]{configs()[0], configs()[0]}, 1));
        TestSupport.throwsType(IllegalArgumentException.class, () -> new Simulation(new ComputerConfig[]{null, configs()[0]}, 1));
        TestSupport.throwsType(IllegalArgumentException.class, () -> new Simulation(configs(), 0));
        ComputerConfig[] config = configs();
        Simulation sim = new Simulation(config, 1000);
        try {
            config[0] = new ComputerConfig(9, 1, SchedulingPolicyType.EDF, 1);
            SimulationSnapshot initial = sim.snapshot();
            TestSupport.equal(1, initial.computerAt(0).id()); TestSupport.equal(2, initial.computerAt(1).id());
            TestSupport.equal(0L, initial.nextCycle()); TestSupport.equal(SimulationState.PAUSED, initial.state());
            TestSupport.throwsType(IllegalArgumentException.class, () -> sim.enqueue(null));
            long a = sim.enqueue(new AddProcessCommand(cpu(2, 2)));
            long b = sim.enqueue(new AddProcessCommand(cpu(1, 1)));
            long rejected = sim.enqueue(new AddProcessCommand(cpu(3, 1)));
            TestSupport.equal(1L, a); TestSupport.equal(2L, b); TestSupport.equal(3L, rejected);
            TestSupport.equal(0, sim.snapshot().resultCount());
            TestSupport.throwsType(IllegalStateException.class, () -> new AddProcessCommand(cpu(1, 1)).apply(sim));
            sim.step(); SimulationSnapshot first = await(sim, 1, SimulationState.PAUSED);
            TestSupport.equal(3, first.resultCount()); TestSupport.equal(1L, first.resultAt(0).processId());
            TestSupport.equal(2L, first.resultAt(1).processId()); TestSupport.isTrue(!first.resultAt(2).accepted());
            TestSupport.equal(2L, first.computerAt(0).terminatedAt(0).id());
            TestSupport.equal(1L, first.computerAt(1).running().id()); TestSupport.equal(1, first.computerAt(1).running().pc());
            sim.enqueue(new AddProcessCommand(cpu(1, 1)));
            sim.enqueue(new ChangeSchedulingCommand(1, SchedulingPolicyType.EDF, 3));
            sim.enqueue(new ChangeCycleDurationCommand(25)); sim.step();
            SimulationSnapshot second = await(sim, 2, SimulationState.PAUSED);
            TestSupport.equal(3L, second.resultAt(3).processId());
            TestSupport.equal(1L, second.computerAt(0).terminatedAt(1).arrivalCycle());
            TestSupport.equal(SchedulingPolicyType.EDF, second.computerAt(0).policyType());
            TestSupport.equal(3, second.computerAt(0).quantum()); TestSupport.equal(25L, second.cycleMillis());
            TestSupport.equal(SchedulingPolicyType.FCFS, second.computerAt(1).policyType());
            TestSupport.equal(1, first.computerAt(1).running().pc()); TestSupport.equal(3, first.resultCount());
            long cancelled = sim.enqueue(new AddProcessCommand(cpu(1, 1))); sim.stop();
            TestSupport.isTrue(stopped(sim)); SimulationSnapshot end = sim.snapshot();
            TestSupport.equal(SimulationState.STOPPED, end.state()); TestSupport.equal(7, end.resultCount());
            TestSupport.equal(cancelled, end.resultAt(6).requestId()); TestSupport.isTrue(!end.resultAt(6).accepted());
            TestSupport.equal(-1L, end.resultAt(6).appliedCycle());
            TestSupport.throwsType(IllegalStateException.class, () -> sim.enqueue(new AddProcessCommand(cpu(1, 1))));
            TestSupport.throwsType(IllegalStateException.class, sim::resume);
        } finally { sim.close(); }
        sim.close();
        TestSupport.throwsType(IllegalArgumentException.class, () -> new AddProcessCommand(null));
        TestSupport.throwsType(IllegalArgumentException.class, () -> new ChangeSchedulingCommand(1, null, 1));
        TestSupport.throwsType(IllegalArgumentException.class, () -> new ChangeSchedulingCommand(0, SchedulingPolicyType.FCFS, 1));
        TestSupport.throwsType(IllegalArgumentException.class, () -> new ChangeSchedulingCommand(1, SchedulingPolicyType.FCFS, 0));
        TestSupport.throwsType(IllegalArgumentException.class, () -> new ChangeCycleDurationCommand(0));
    }
    static SimulationSnapshot await(Simulation sim, long cycle, SimulationState state) {
        long start = System.nanoTime(); SimulationSnapshot current = sim.snapshot();
        while (current.nextCycle() < cycle || current.state() != state) {
            if (current.state() == SimulationState.FAILED) { throw new AssertionError(current.error()); }
            long remaining = 2000 - (System.nanoTime() - start) / 1_000_000;
            if (remaining <= 0) { throw new AssertionError("Ciclo/estado no alcanzado"); }
            try { current = sim.awaitNextSnapshot(current.version(), remaining); }
            catch (InterruptedException e) { throw new AssertionError(e); }
            if (current == null) { throw new AssertionError("No se publicó el estado esperado"); }
        }
        return current;
    }
    static boolean stopped(Simulation sim) {
        try { return sim.awaitStopped(2000); } catch (InterruptedException e) { throw new AssertionError(e); }
    }
}
