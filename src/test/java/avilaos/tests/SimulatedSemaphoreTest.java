package avilaos.tests;
import avilaos.sync.SimulatedSemaphore;
import avilaos.process.*;

public final class SimulatedSemaphoreTest {
    public static void run() {
        SimulatedSemaphore sem = new SimulatedSemaphore(1, 1);
        ProcessControlBlock a = SchedulingTest.ready(1, 0), b = SchedulingTest.ready(2, 0), c = SchedulingTest.ready(3, 0);
        TestSupport.isTrue(sem.waitFor(a)); TestSupport.isTrue(!sem.waitFor(b)); TestSupport.isTrue(!sem.waitFor(c));
        TestSupport.equal(0, sem.value()); TestSupport.equal(2, sem.waitingCount()); TestSupport.equal(b, sem.waitingAt(0));
        TestSupport.throwsType(IllegalArgumentException.class, () -> sem.waitFor(b));
        TestSupport.throwsType(IllegalArgumentException.class, () -> sem.waitFor(SchedulingTest.ready(2, 0)));
        TestSupport.isTrue(!sem.cancelWaiting(SchedulingTest.ready(2, 0)));
        TestSupport.equal(b, sem.signal()); TestSupport.equal(c, sem.signal());
        TestSupport.equal(0, sem.value()); TestSupport.equal(null, sem.signal()); TestSupport.equal(1, sem.value());
        TestSupport.throwsType(IllegalStateException.class, sem::signal); TestSupport.equal(1, sem.value());
        TestSupport.equal(ProcessState.READY, b.state()); TestSupport.equal(0, b.pc());
        sem.waitFor(a); sem.waitFor(b); sem.waitFor(c);
        TestSupport.isTrue(sem.cancelWaiting(b)); TestSupport.isTrue(!sem.cancelWaiting(b));
        TestSupport.equal(c, sem.signal()); TestSupport.equal(0, sem.waitingCount());
        TestSupport.throwsType(IllegalArgumentException.class, () -> sem.waitFor(null));
        TestSupport.throwsType(IllegalArgumentException.class, () -> sem.cancelWaiting(null));
        TestSupport.throwsType(IndexOutOfBoundsException.class, () -> sem.waitingAt(0));
        TestSupport.throwsType(IllegalArgumentException.class, () -> new SimulatedSemaphore(-1, 1));
        TestSupport.throwsType(IllegalArgumentException.class, () -> new SimulatedSemaphore(2, 1));
        TestSupport.throwsType(IllegalArgumentException.class, () -> new SimulatedSemaphore(0, 0));
    }
}
