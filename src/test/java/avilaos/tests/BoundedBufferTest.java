package avilaos.tests;
import avilaos.sync.*;
import avilaos.process.ProcessControlBlock;
import avilaos.hardware.Ram;

public final class BoundedBufferTest {
    public static void run() {
        BoundedBuffer b = new BoundedBuffer(new BufferDefinition(1, 1, 2));
        ProcessControlBlock owner = SchedulingTest.ready(1, 0), other = SchedulingTest.ready(2, 0);
        TestSupport.equal(2, b.empty().value()); TestSupport.equal(0, b.full().value()); TestSupport.equal(1, b.mutex().value());
        TestSupport.throwsType(IllegalStateException.class, () -> b.claimMutex(owner));
        b.mutex().waitFor(owner); b.claimMutex(owner);
        BufferItem first = new BufferItem(1, 1, 1), second = new BufferItem(2, 1, 1);
        TestSupport.throwsType(IllegalStateException.class, () -> b.insert(other, first));
        TestSupport.throwsType(IllegalStateException.class, () -> b.remove(other));
        TestSupport.throwsType(IllegalStateException.class, () -> b.releaseMutex(other));
        b.insert(owner, first); b.insert(owner, second);
        TestSupport.equal(first, b.itemAt(0)); TestSupport.equal(2L, b.produced());
        TestSupport.throwsType(IllegalStateException.class, () -> b.insert(owner, new BufferItem(3, 1, 1)));
        TestSupport.equal(first, b.remove(owner)); TestSupport.equal(second, b.remove(owner));
        TestSupport.equal(2L, b.consumed()); TestSupport.equal(0, b.size());
        TestSupport.throwsType(IllegalStateException.class, () -> b.remove(owner));
        b.mutex().waitFor(other); TestSupport.equal(other, b.releaseMutex(owner));
        TestSupport.equal(other, b.mutexOwner()); TestSupport.equal(0, b.mutex().value());
        b.releaseMutex(other); TestSupport.equal(null, b.mutexOwner()); TestSupport.equal(1, b.mutex().value());
        Ram ram = new Ram(10), otherRam = new Ram(10); BufferRegistry registry = new BufferRegistry();
        registry.registerRam(1, ram); registry.registerRam(2, otherRam);
        registry.create(new BufferDefinition(1, 1, 4)); TestSupport.equal(6, ram.free());
        TestSupport.throwsType(IllegalArgumentException.class, () -> registry.create(new BufferDefinition(2, 1, 7)));
        TestSupport.throwsType(IllegalArgumentException.class, () -> registry.create(new BufferDefinition(1, 2, 1)));
        TestSupport.throwsType(IllegalArgumentException.class, () -> registry.create(new BufferDefinition(2, 9, 1)));
        TestSupport.equal(1, registry.size()); TestSupport.equal(6, ram.free());
        registry.create(new BufferDefinition(2, 2, 3)); TestSupport.equal(7, otherRam.free());
        TestSupport.equal(1, registry.getAt(0).definition().id()); TestSupport.equal(2, registry.get(2).definition().hostId());
        TestSupport.throwsType(IllegalArgumentException.class, () -> registry.registerRam(1, otherRam));
        TestSupport.throwsType(IllegalArgumentException.class, () -> registry.get(99));
        TestSupport.throwsType(IllegalArgumentException.class, () -> new BufferDefinition(0, 1, 1));
        TestSupport.throwsType(IllegalArgumentException.class, () -> new BufferDefinition(1, 0, 1));
        TestSupport.throwsType(IllegalArgumentException.class, () -> new BufferDefinition(1, 1, 0));
        TestSupport.throwsType(IllegalArgumentException.class, () -> new BufferItem(0, 1, 1));
    }
}
