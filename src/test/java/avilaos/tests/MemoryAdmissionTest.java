package avilaos.tests;

import avilaos.hardware.Ram;
import avilaos.kernel.MemoryAdmission;
import avilaos.process.*;

public final class MemoryAdmissionTest {
    static ProcessControlBlock process(long id, int memory) {
        return new ProcessControlBlock(id, new ProcessDefinition("P" + id, 1,
                ProcessType.CPU_BOUND, 3, memory, 0, 10, 0, 0, 0, 0, 0), 0);
    }
    public static void run() {
        Ram ram = new Ram(10);
        MemoryAdmission admission = new MemoryAdmission(ram);
        ProcessControlBlock a = process(1, 8), b = process(2, 4);
        TestSupport.isTrue(admission.admit(a));
        TestSupport.equal(ProcessState.READY, a.state());
        TestSupport.equal(8, a.assignedMemory());
        TestSupport.isTrue(!admission.admit(b));
        TestSupport.equal(ProcessState.NEW, b.state());
        TestSupport.equal(0, b.assignedMemory());
        TestSupport.equal(8, ram.used());
        TestSupport.throwsType(IllegalStateException.class, () -> admission.admit(a));
        TestSupport.throwsType(IllegalArgumentException.class, () -> admission.admit(process(1, 1)));
        TestSupport.throwsType(IllegalStateException.class, () -> admission.releaseIfAllocated(a));
        TestSupport.isTrue(!admission.releaseIfAllocated(process(1, 8)));
        TestSupport.isTrue(!admission.releaseIfAllocated(b));
        TestSupport.equal(8, ram.used());
        a.terminate(TerminationReason.DEADLINE_EXPIRED, 9);
        TestSupport.isTrue(admission.releaseIfAllocated(a));
        TestSupport.equal(0, ram.used());
        TestSupport.isTrue(!admission.releaseIfAllocated(a));
        TestSupport.isTrue(admission.admit(b));
        TestSupport.equal(4, ram.used());
        TestSupport.throwsType(IllegalArgumentException.class, () -> admission.admit(process(3, 11)));
        TestSupport.throwsType(IllegalArgumentException.class, () -> new MemoryAdmission(null));
        TestSupport.throwsType(IllegalArgumentException.class, () -> admission.admit(null));
        TestSupport.throwsType(IllegalArgumentException.class, () -> admission.releaseIfAllocated(null));
        TestSupport.equal(4, ram.used());
    }
}
