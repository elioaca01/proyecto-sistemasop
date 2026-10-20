package avilaos.tests;

import avilaos.hardware.*;
import avilaos.process.*;

public final class IoControllerTest {
    public static void run() {
        IoController io = new IoController();
        ProcessControlBlock a = CpuTest.io(1, 3, 1, 2);
        ProcessControlBlock b = CpuTest.io(2, 3, 1, 2);
        ProcessControlBlock c = CpuTest.io(3, 3, 1, 2);
        TestSupport.equal(null, io.pollCompleted());
        TestSupport.throwsType(IllegalArgumentException.class, () -> io.request(a, 0, 2));
        io.onTick(0); io.onTick(1);
        io.request(a, 1, 2); io.request(b, 1, 2); io.request(c, 1, 1);
        TestSupport.equal(3, io.pendingCount());
        TestSupport.equal(ProcessState.NEW, a.state());
        TestSupport.throwsType(IllegalArgumentException.class, () -> io.request(a, 1, 1));
        TestSupport.throwsType(IllegalArgumentException.class, () -> io.request(null, 1, 1));
        TestSupport.throwsType(IllegalArgumentException.class, () -> io.request(CpuTest.io(4, 3, 1, 1), 1, 0));
        TestSupport.throwsType(IllegalArgumentException.class, () -> io.request(CpuTest.io(5, 3, 1, 1), 0, 1));
        for (long bad : new long[]{-1, 0, 1, 3, Long.MAX_VALUE}) {
            TestSupport.throwsType(IllegalArgumentException.class, () -> io.onTick(bad));
            TestSupport.equal(3, io.pendingCount());
        }
        io.onTick(2); TestSupport.equal(null, io.pollCompleted());
        io.onTick(3); TestSupport.equal(c, io.pollCompleted());
        TestSupport.equal(null, io.pollCompleted());
        TestSupport.equal(2, io.pendingCount());
        io.onTick(4); TestSupport.equal(0, io.pendingCount());
        TestSupport.throwsType(IllegalArgumentException.class, () -> io.request(a, 4, 1));
        TestSupport.equal(a, io.pollCompleted()); TestSupport.equal(b, io.pollCompleted());
        TestSupport.equal(null, io.pollCompleted());
        io.request(a, 4, 1);
        TestSupport.isTrue(io.cancel(a)); TestSupport.isTrue(!io.cancel(a));
        io.onTick(5); io.onTick(6); TestSupport.equal(null, io.pollCompleted());
        io.request(a, 6, 1); io.onTick(7); io.onTick(8);
        TestSupport.isTrue(io.cancel(a)); TestSupport.equal(null, io.pollCompleted());
        TestSupport.throwsType(IllegalArgumentException.class, () -> io.cancel(null));
        TestSupport.equal(4L, new IoRequest(a, 1, 2).completesAt());
        TestSupport.equal(a, new IoRequest(a, 1, 2).process());
        TestSupport.throwsType(IllegalArgumentException.class, () -> new IoRequest(a, Long.MAX_VALUE, 1));
        TestSupport.throwsType(IllegalArgumentException.class, () -> new IoRequest(a, Long.MAX_VALUE - 1, 1));
        TestSupport.throwsType(IllegalArgumentException.class, () -> new IoRequest(a, -1, 1));
        TestSupport.throwsType(IllegalArgumentException.class, () -> new IoRequest(a, 0, 0));
        TestSupport.throwsType(IllegalArgumentException.class, () -> new IoRequest(null, 0, 1));
    }
}
