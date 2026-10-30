package avilaos.tests;

import avilaos.simulation.*;
import avilaos.process.*;
import avilaos.scheduling.SchedulingPolicyType;

public final class SnapshotTest {
    public static void run() {
        Computer c = new Computer(1, 10);
        c.submit(PreemptiveKernelTest.process(1, 1, 2, 6, 0, 10, 0, false));
        c.submit(PreemptiveKernelTest.process(2, 1, 1, 4, 0, 10, 0, false));
        c.onTick(0); ComputerSnapshot old = ComputerSnapshot.capture(c);
        TestSupport.equal(1, old.running().pc()); TestSupport.equal(ProcessState.READY, old.readyAt(0).state());
        c.onTick(1); c.onTick(2);
        TestSupport.equal(1, old.running().pc()); TestSupport.equal(10, old.ramUsed());
        TestSupport.equal(0, old.readyAt(0).pc()); TestSupport.equal(0, old.terminatedCount());
        ComputerSnapshot now = ComputerSnapshot.capture(c);
        TestSupport.equal(null, now.running()); TestSupport.equal(10, now.ramFree());
        TestSupport.equal(2, now.terminatedCount()); TestSupport.equal(1L, now.terminatedAt(0).terminationCycle());
        TestSupport.equal(0L, old.running().firstRunCycle()); TestSupport.equal(10L, old.running().deadlineAt());
        TestSupport.equal(6, old.running().assignedMemory()); TestSupport.equal(1, old.running().mar());
        Computer io = new Computer(2, 10);
        io.submit(PreemptiveKernelTest.process(3, 2, 2, 6, 0, 4, 0, true));
        io.onTick(0); io.onTick(1);
        ProcessSnapshot blocked = ComputerSnapshot.capture(io).blockedAt(0);
        TestSupport.equal(BlockReason.IO, blocked.blockReason()); TestSupport.equal(0, blocked.remainingInstructions());
        TestSupport.equal(1L, blocked.userCycles()); TestSupport.equal(1L, blocked.kernelCycles());
        io.onTick(2); io.onTick(3);
        TestSupport.equal(TerminationReason.DEADLINE_EXPIRED, ComputerSnapshot.capture(io).terminatedAt(0).terminationReason());
        TestSupport.equal(ProcessState.BLOCKED, blocked.state());
        ComputerSnapshot[] computers = {old, now};
        CommandResult[] results = {new CommandResult(1, 0, true, 1, "creado")};
        SimulationSnapshot s = new SimulationSnapshot(0, 1, SimulationState.PAUSED, 10, null, computers, results);
        computers[0] = now; results[0] = null;
        TestSupport.equal(1, s.computerAt(0).running().pc()); TestSupport.isTrue(s.resultAt(0).accepted());
        TestSupport.equal(2, s.computerCount()); TestSupport.equal(1, s.resultCount());
        TestSupport.throwsType(IndexOutOfBoundsException.class, () -> s.computerAt(2));
        TestSupport.throwsType(IndexOutOfBoundsException.class, () -> old.newAt(0));
        for (ComputerConfig cfg : new ComputerConfig[]{new ComputerConfig(1, 10, SchedulingPolicyType.FCFS, 2)}) {
            TestSupport.equal(1, cfg.id());
        }
        TestSupport.throwsType(IllegalArgumentException.class, () -> new ComputerConfig(0, 10, SchedulingPolicyType.FCFS, 2));
        TestSupport.throwsType(IllegalArgumentException.class, () -> new ComputerConfig(1, 0, SchedulingPolicyType.FCFS, 2));
        TestSupport.throwsType(IllegalArgumentException.class, () -> new ComputerConfig(1, 10, null, 2));
        TestSupport.throwsType(IllegalArgumentException.class, () -> new ComputerConfig(1, 10, SchedulingPolicyType.FCFS, 0));
        TestSupport.throwsType(IllegalArgumentException.class, () -> ComputerSnapshot.capture(null));
        TestSupport.throwsType(IllegalArgumentException.class, () -> ProcessSnapshot.capture(null));
        TestSupport.throwsType(IllegalArgumentException.class, () -> new SimulationSnapshot(-1, 0, SimulationState.PAUSED, 10, null, computers, new CommandResult[0]));
    }
}
