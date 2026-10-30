package avilaos.hardware;

/** Contabiliza RAM sin direcciones ni particiones; cada reserva tiene un dueño. */
public final class Ram {
    public static final class Reservation {
        private final Ram owner;
        private final int units;
        private boolean active = true;

        private Reservation(Ram owner, int units) {
            this.owner = owner;
            this.units = units;
        }

        public int units() { return units; }
    }

    private final int capacity;
    private int used;

    public Ram(int capacity) {
        if (capacity <= 0) {
            throw new IllegalArgumentException("La capacidad de RAM debe ser positiva");
        }
        this.capacity = capacity;
    }

    public Reservation reserve(int units) {
        if (units <= 0 || units > capacity) {
            throw new IllegalArgumentException("La reserva debe ser positiva y caber en la RAM total");
        }
        if (units > free()) {
            return null;
        }
        Reservation reservation = new Reservation(this, units);
        used += units;
        return reservation;
    }

    public void release(Reservation reservation) {
        if (reservation == null || reservation.owner != this) {
            throw new IllegalArgumentException("La reserva no pertenece a esta RAM");
        }
        if (!reservation.active) {
            throw new IllegalStateException("La reserva ya fue liberada");
        }
        used -= reservation.units;
        reservation.active = false;
    }

    public int capacity() { return capacity; }
    public int used() { return used; }
    public int free() { return capacity - used; }
}
