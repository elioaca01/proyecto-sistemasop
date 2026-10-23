package avilaos.tests;

import avilaos.process.*;
import avilaos.simulation.Computer;
import avilaos.scheduling.SchedulingPolicyType;
import avilaos.sync.*;
import avilaos.structures.LinkedQueue;
import static avilaos.tests.TestSupport.*;

public final class BufferKernelTest {
    private BufferKernelTest() { }
    public static void run() {
        localTrace(); remotePreparation(); quantumAndCleanup();
    }
    static ProcessControlBlock process(long id, int computer, ProcessType type, long deadline) {
        return new ProcessControlBlock(id, new ProcessDefinition("p" + id, computer, type, 6, 2, 1, deadline, 0, 0, 1, 1, 1), 0);
    }
    static final class Rig {
        final BufferRegistry registry = new BufferRegistry();
        final BufferService service;
        final Computer computer;
        final BoundedBuffer buffer;
        long cycle;
        Rig(int computerId, int latency, SchedulingPolicyType policy) {
            service = new BufferService(registry, latency);
            computer = new Computer(computerId, 20, policy, 1, service);
            registry.registerRam(1, computer.ram());
            buffer = registry.create(new BufferDefinition(1, 1, 1));
        }
        void tick() {
            LinkedQueue<BufferWake> due = service.beginCycle(cycle);
            while (!due.isEmpty()) { BufferWake w = due.dequeue(); isTrue(computer.unblockExternal(w.process(), w.reason())); }
            computer.onTick(cycle++);
        }
    }
    private static void localTrace() {
        Rig r = new Rig(1, 0, SchedulingPolicyType.FCFS);
        ProcessControlBlock c = process(1, 1, ProcessType.CONSUMER, 100), p = process(2, 1, ProcessType.PRODUCER, 100);
        r.computer.submit(c); r.computer.submit(p);
        long[] who = {1,2,2,2,2,2,2,1,1,1,1,1};
        int[] pc = {1,1,2,3,4,5,6,2,3,4,5,6};
        for (int i = 0; i < who.length; i++) {
            r.tick(); equal(who[i], r.computer.cpu().lastProcessId()); equal(pc[i], who[i] == 1 ? c.pc() : p.pc());
            if (i == 0) { equal(BlockReason.SEMAPHORE_FULL, c.blockReason()); }
            if (i == 6) { equal(ProcessState.BLOCKED, c.state()); equal(1, c.pc()); }
        }
        equal(ProcessState.TERMINATED, c.state()); equal(ProcessState.TERMINATED, p.state());
        equal(0, r.buffer.size()); equal(1, r.buffer.empty().value()); equal(0, r.buffer.full().value()); equal(1, r.buffer.mutex().value());
        equal(19, r.computer.ram().free()); equal(0, r.computer.kernel().blockedCount());
        equal(null, r.service.operationSnapshot(c)); isTrue(!r.computer.unblockExternal(c, BlockReason.SEMAPHORE_FULL));
    }
    private static void remotePreparation() {
        Rig r = new Rig(2, 2, SchedulingPolicyType.FCFS);
        ProcessControlBlock c = process(1, 2, ProcessType.CONSUMER, 100);
        ProcessControlBlock cpu = new ProcessControlBlock(2, new ProcessDefinition("cpu", 2, ProcessType.CPU_BOUND, 1, 2, 1, 100, 0,0,0,0,0), 0);
        r.computer.submit(c); r.computer.submit(cpu); r.tick();
        equal(-1L, c.firstRunCycle()); equal(0, c.pc()); equal(BlockReason.NETWORK, c.blockReason()); equal(2L, r.computer.cpu().lastProcessId());
        isTrue(!r.computer.unblockExternal(c, BlockReason.SEMAPHORE_EMPTY));
        r.tick(); equal(0, c.pc()); r.tick(); equal(1, c.pc()); equal(2L, c.firstRunCycle()); equal(BlockReason.SEMAPHORE_FULL, c.blockReason());
        equal(3L, r.computer.kernel().nextCycle());
        ProcessControlBlock fresh = process(10, 2, ProcessType.CONSUMER, 100); fresh.admit();
        fresh.blockBeforeDispatch(BlockReason.NETWORK); equal(-1L, fresh.firstRunCycle()); equal(2, fresh.assignedMemory()); equal(0, fresh.pc());
        isTrue(!r.computer.unblockExternal(fresh, BlockReason.NETWORK));
        ProcessControlBlock invalid = process(11, 2, ProcessType.CONSUMER, 100); invalid.admit();
        throwsType(IllegalArgumentException.class, () -> invalid.blockBeforeDispatch(BlockReason.SEMAPHORE_FULL)); equal(ProcessState.READY, invalid.state());
    }
    private static void quantumAndCleanup() {
        Rig r = new Rig(1, 0, SchedulingPolicyType.ROUND_ROBIN);
        ProcessControlBlock p = process(1, 1, ProcessType.PRODUCER, 4), other = process(2, 1, ProcessType.PRODUCER, 100);
        r.computer.submit(p); r.computer.submit(other);
        r.tick(); r.tick(); r.tick(); equal(1, r.computer.sliceUsed()); equal(0, r.buffer.empty().value());
        r.tick(); equal(TerminationReason.DEADLINE_EXPIRED, p.terminationReason()); equal(2, p.pc()); equal(null, r.service.operationSnapshot(p));
        for (int i = 0; i < 8; i++) { r.tick(); }
        equal(ProcessState.TERMINATED, other.state()); equal(1, r.buffer.size()); equal(1L, r.buffer.produced()); equal(1, r.buffer.full().value()); equal(1, r.buffer.mutex().value());
    }
}
