package avilaos.tests;

import avilaos.hardware.Ram;

public final class RamTest {
    private RamTest() { }

    public static void run() {
        reservesOnlyWhatFitsAndReleasesOnlyOnce();
        rejectsInvalidAndForeignReservations();
        handlesMaximumCapacityWithoutOverflow();
    }

    private static void reservesOnlyWhatFitsAndReleasesOnlyOnce() {
        Ram ram = new Ram(10);
        Ram.Reservation first = ram.reserve(7);
        TestSupport.equal(7, first.units());
        TestSupport.equal(7, ram.used());
        TestSupport.equal(3, ram.free());
        TestSupport.equal(null, ram.reserve(4));
        TestSupport.equal(7, ram.used());
        Ram.Reservation second = ram.reserve(3);
        TestSupport.equal(0, ram.free());
        ram.release(first);
        TestSupport.equal(3, ram.used());
        TestSupport.throwsType(IllegalStateException.class, () -> ram.release(first));
        TestSupport.equal(3, ram.used());
        ram.release(second);
        TestSupport.equal(0, ram.used());
        TestSupport.equal(10, ram.free());
    }

    private static void rejectsInvalidAndForeignReservations() {
        TestSupport.throwsType(IllegalArgumentException.class, () -> new Ram(0));
        TestSupport.throwsType(IllegalArgumentException.class, () -> new Ram(-1));
        Ram ram = new Ram(10);
        TestSupport.throwsType(IllegalArgumentException.class, () -> ram.reserve(0));
        TestSupport.throwsType(IllegalArgumentException.class, () -> ram.reserve(-1));
        TestSupport.throwsType(IllegalArgumentException.class, () -> ram.reserve(11));
        TestSupport.throwsType(IllegalArgumentException.class, () -> ram.release(null));
        Ram other = new Ram(10);
        Ram.Reservation foreign = other.reserve(5);
        TestSupport.throwsType(IllegalArgumentException.class, () -> ram.release(foreign));
        TestSupport.equal(0, ram.used());
        TestSupport.equal(5, other.used());
        other.release(foreign);
        TestSupport.equal(10, other.free());
    }

    private static void handlesMaximumCapacityWithoutOverflow() {
        Ram ram = new Ram(Integer.MAX_VALUE);
        Ram.Reservation reservation = ram.reserve(Integer.MAX_VALUE);
        TestSupport.equal(0, ram.free());
        TestSupport.equal(null, ram.reserve(1));
        TestSupport.equal(Integer.MAX_VALUE, ram.used());
        ram.release(reservation);
        TestSupport.equal(Integer.MAX_VALUE, ram.free());
    }
}
