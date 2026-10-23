package avilaos.tests;
import avilaos.sync.*;
import avilaos.process.*;
import avilaos.hardware.*;

public final class BufferOperationTest {
    static final class Rig {
        final BufferRegistry registry = new BufferRegistry(); final BufferService service;
        final BoundedBuffer buffer; final Cpu cpu = new Cpu(); long cycle;
        Rig(int capacity) {
            registry.registerRam(1, new Ram(100)); buffer = registry.create(new BufferDefinition(1, 1, capacity));
            service = new BufferService(registry);
        }
        ProcessControlBlock add(long id, ProcessType type, long deadline) {
            ProcessControlBlock p = BufferProgramTest.process(id, type, 1, 6, deadline);
            service.registerProcess(p); p.admit(); return p;
        }
        void begin() {
            var wakes = service.beginCycle(cycle); BufferWake wake;
            while ((wake = wakes.dequeue()) != null) {
                if (wake.process().state() == ProcessState.BLOCKED) { wake.process().unblock(); }
            }
        }
        BlockReason tick(ProcessControlBlock p) {
            begin();
            if (p.state() == ProcessState.READY) { p.dispatch(cycle); }
            cpu.load(p); cpu.onTick(cycle);
            BlockReason reason = service.afterInstruction(p, cycle);
            if (reason != BlockReason.NONE) { p.block(reason); } else { p.preempt(); }
            cpu.clear(); cycle++; return reason;
        }
        void expire(ProcessControlBlock p) {
            p.terminate(TerminationReason.DEADLINE_EXPIRED, cycle - 1);
            service.cleanup(p, cycle - 1, TerminationReason.DEADLINE_EXPIRED);
        }
        void seed() {
            ProcessControlBlock p = add(99, ProcessType.PRODUCER, 100);
            for (int i = 0; i < 6; i++) { tick(p); }
            p.terminate(TerminationReason.COMPLETED, 5); service.cleanup(p, 5, TerminationReason.COMPLETED);
        }
    }
    public static void run() {
        localWake(); cleanupProducer(); cleanupConsumer(); handoffBeforeWake(); keepNewMutexOwner();
    }
    private static void localWake() {
        Rig r = new Rig(1); ProcessControlBlock c = r.add(1, ProcessType.CONSUMER, 100), p = r.add(2, ProcessType.PRODUCER, 100);
        TestSupport.equal(BlockReason.SEMAPHORE_FULL, r.tick(c)); TestSupport.equal(1, c.pc());
        for (int i = 0; i < 6; i++) { r.tick(p); }
        TestSupport.equal(1, r.buffer.size()); TestSupport.equal(0, r.buffer.full().value());
        TestSupport.isTrue(r.service.operationSnapshot(c).countingPermit());
        TestSupport.equal(ProcessState.BLOCKED, c.state());
        TestSupport.equal(BlockReason.NONE, r.tick(c)); TestSupport.equal(2, c.pc());
        for (int i = 0; i < 4; i++) { r.tick(c); }
        TestSupport.equal(0, r.buffer.size()); TestSupport.equal(1L, r.buffer.produced()); TestSupport.equal(1L, r.buffer.consumed());
        TestSupport.equal(1, r.buffer.empty().value()); TestSupport.equal(1, r.buffer.mutex().value());
        TestSupport.equal(1, r.service.operationSnapshot(c).completedItems());
        TestSupport.equal(6, c.pc());
    }
    private static void cleanupProducer() {
        for (int last : new int[]{1, 2, 3, 4}) {
            Rig r = new Rig(1); ProcessControlBlock p = r.add(1, ProcessType.PRODUCER, last + 1);
            for (int i = 0; i <= last; i++) { r.tick(p); }
            int pc = p.pc(); r.expire(p); r.service.cleanup(p, last, TerminationReason.DEADLINE_EXPIRED);
            TestSupport.equal(pc, p.pc()); TestSupport.equal(1, r.buffer.mutex().value());
            TestSupport.equal(last >= 3 ? 1 : 0, r.buffer.size());
            TestSupport.equal(last >= 3 ? 1 : 0, r.buffer.full().value());
            TestSupport.equal(last >= 3 ? 0 : 1, r.buffer.empty().value());
            TestSupport.equal(null, r.service.operationSnapshot(p)); TestSupport.isTrue(r.service.eventCount() > 0);
        }
        Rig r = new Rig(1); ProcessControlBlock p = BufferProgramTest.process(1, ProcessType.PRODUCER, 1, 6, 1);
        r.service.registerProcess(p); r.service.beginCycle(0); p.terminate(TerminationReason.DEADLINE_EXPIRED, 0);
        r.service.cleanup(p, 0, TerminationReason.DEADLINE_EXPIRED); TestSupport.equal(1, r.buffer.empty().value());
    }
    private static void cleanupConsumer() {
        for (int last : new int[]{7, 8, 9}) {
            Rig r = new Rig(1); r.seed(); ProcessControlBlock c = r.add(1, ProcessType.CONSUMER, last + 1);
            while (r.cycle <= last) { r.tick(c); }
            r.expire(c);
            TestSupport.equal(last >= 8 ? 0 : 1, r.buffer.size());
            TestSupport.equal(last >= 8 ? 1 : 0, r.buffer.empty().value());
            TestSupport.equal(last >= 8 ? 0 : 1, r.buffer.full().value());
            TestSupport.equal(1, r.buffer.mutex().value()); TestSupport.equal(last >= 8 ? 1L : 0L, r.buffer.consumed());
        }
    }
    private static void handoffBeforeWake() {
        Rig r = new Rig(1);
        ProcessControlBlock a = r.add(1, ProcessType.CONSUMER, 8), b = r.add(2, ProcessType.CONSUMER, 100), p = r.add(3, ProcessType.PRODUCER, 100);
        r.tick(a); r.tick(b); for (int i = 0; i < 6; i++) { r.tick(p); }
        TestSupport.isTrue(r.service.operationSnapshot(a).countingPermit()); r.expire(a);
        TestSupport.isTrue(r.service.operationSnapshot(b).countingPermit());
        r.tick(b); TestSupport.equal(2, b.pc()); TestSupport.equal(ProcessState.TERMINATED, a.state());
        TestSupport.equal(0, r.buffer.full().waitingCount());
    }
    private static void keepNewMutexOwner() {
        Rig r = new Rig(2); ProcessControlBlock a = r.add(1, ProcessType.PRODUCER, 8), b = r.add(2, ProcessType.PRODUCER, 100);
        r.tick(a); r.tick(b); r.tick(a); r.tick(b); r.tick(a);
        TestSupport.equal(BlockReason.SEMAPHORE_MUTEX, r.tick(b)); r.tick(a); r.tick(a);
        TestSupport.equal(b, r.buffer.mutexOwner()); r.expire(a);
        TestSupport.equal(b, r.buffer.mutexOwner()); TestSupport.isTrue(r.service.operationSnapshot(b).mutexHeld());
        r.tick(b); TestSupport.equal(2, r.buffer.size());
        TestSupport.throwsType(IllegalStateException.class, () -> r.service.afterInstruction(b, r.cycle - 1));
    }
}
