package avilaos.tests;
import avilaos.sync.*;
import avilaos.process.*;
import avilaos.hardware.*;

public final class BufferNetworkTest {
    private static BufferRegistry registry() {
        BufferRegistry r = new BufferRegistry(); r.registerRam(1, new Ram(100)); r.create(new BufferDefinition(1, 1, 2)); return r;
    }
    private static void execute(BufferService service, ProcessControlBlock p, long cycle) {
        if (p.state() == ProcessState.READY) { p.dispatch(cycle); }
        p.executeInstruction(ProcessProgram.nextInstruction(p) == InstructionKind.COMPUTE ? CpuMode.USER : CpuMode.KERNEL);
        BlockReason reason = service.afterInstruction(p, cycle);
        if (reason != BlockReason.NONE) { p.block(reason); } else { p.preempt(); }
    }
    public static void run() {
        BufferRegistry registry = registry(); BufferService service = new BufferService(registry, 2);
        ProcessControlBlock p = new ProcessControlBlock(1, BufferProgramTest.definition(ProcessType.PRODUCER, 2, 12, 1, 2, 100), 0);
        service.registerProcess(p); p.admit(); service.beginCycle(0); execute(service, p, 0);
        service.beginCycle(1); TestSupport.equal(BlockReason.NETWORK, service.prepare(p, 1));
        p.dispatch(1); p.block(BlockReason.NETWORK);
        TestSupport.equal(3L, service.operationSnapshot(p).networkAvailableAt());
        TestSupport.equal(1, p.pc()); TestSupport.equal(2, registry.get(1).empty().value());
        TestSupport.equal(null, registry.get(1).mutexOwner()); TestSupport.isTrue(!service.operationSnapshot(p).countingPermit());
        TestSupport.isTrue(service.beginCycle(2).isEmpty());
        var due = service.beginCycle(3); TestSupport.equal(p, due.dequeue().process()); TestSupport.isTrue(due.isEmpty());
        p.unblock(); TestSupport.equal(BlockReason.NONE, service.prepare(p, 3)); execute(service, p, 3);
        for (int t = 4; t <= 7; t++) {
            service.beginCycle(t); TestSupport.equal(BlockReason.NONE, service.prepare(p, t)); execute(service, p, t);
        }
        TestSupport.equal(6, p.pc()); TestSupport.equal(1, registry.get(1).size());
        service.beginCycle(8); execute(service, p, 8); service.beginCycle(9);
        TestSupport.equal(BlockReason.NETWORK, service.prepare(p, 9)); TestSupport.equal(11L, service.operationSnapshot(p).networkAvailableAt());
        TestSupport.isTrue(service.beginCycle(10).isEmpty()); TestSupport.equal(p, service.beginCycle(11).dequeue().process());
        for (int computer : new int[]{1, 2}) {
            BufferService s = new BufferService(registry(), computer == 1 ? 2 : 0);
            ProcessControlBlock q = BufferProgramTest.process(2, ProcessType.PRODUCER, computer, 6, 10); s.registerProcess(q); q.admit();
            s.beginCycle(0); execute(s, q, 0); s.beginCycle(1);
            TestSupport.equal(BlockReason.NONE, s.prepare(q, 1)); TestSupport.equal(-1L, s.operationSnapshot(q).networkAvailableAt());
        }
        BufferRegistry cancelledRegistry = registry(); BufferService cancelled = new BufferService(cancelledRegistry, 2);
        ProcessControlBlock q = BufferProgramTest.process(3, ProcessType.PRODUCER, 2, 6, 2); cancelled.registerProcess(q); q.admit();
        cancelled.beginCycle(0); execute(cancelled, q, 0); cancelled.beginCycle(1); cancelled.prepare(q, 1);
        q.terminate(TerminationReason.DEADLINE_EXPIRED, 1); cancelled.cleanup(q, 1, TerminationReason.DEADLINE_EXPIRED);
        TestSupport.isTrue(cancelled.beginCycle(2).isEmpty()); TestSupport.isTrue(cancelled.beginCycle(3).isEmpty());
        TestSupport.equal(2, cancelledRegistry.get(1).empty().value()); TestSupport.equal(null, cancelled.operationSnapshot(q));
        TestSupport.equal(3L, new NetworkTransfer(p, 1, 1, 2).availableCycle());
        TestSupport.throwsType(IllegalArgumentException.class, () -> new NetworkTransfer(p, 1, Long.MAX_VALUE, 1));
        TestSupport.throwsType(IllegalArgumentException.class, () -> new NetworkTransfer(p, 1, 0, 0));
        TestSupport.throwsType(IllegalArgumentException.class, () -> new BufferService(registry, -1));
    }
}
