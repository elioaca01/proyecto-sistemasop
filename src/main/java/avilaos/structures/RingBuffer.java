package avilaos.structures;

/** Almacenamiento acotado FIFO; todavía no incluye semáforos simulados. */
public final class RingBuffer<T> {
    private final Object[] elements;
    private int head;
    private int size;

    public RingBuffer(int capacity) {
        if (capacity <= 0) {
            throw new IllegalArgumentException("La capacidad debe ser positiva");
        }
        elements = new Object[capacity];
    }

    public void put(T value) {
        if (value == null) {
            throw new IllegalArgumentException("El valor no puede ser nulo");
        }
        if (isFull()) {
            throw new IllegalStateException("El buffer está lleno");
        }
        elements[physicalIndex(size)] = value;
        size++;
    }

    public T take() {
        if (isEmpty()) {
            throw new IllegalStateException("El buffer está vacío");
        }
        T value = get(0);
        elements[head] = null;
        head = physicalIndex(1);
        size--;
        return value;
    }

    @SuppressWarnings("unchecked")
    public T get(int index) {
        if (index < 0 || index >= size) {
            throw new IndexOutOfBoundsException("Índice " + index + "; tamaño " + size);
        }
        return (T) elements[physicalIndex(index)];
    }

    public int size() { return size; }

    public int capacity() { return elements.length; }

    public boolean isEmpty() { return size == 0; }

    public boolean isFull() { return size == elements.length; }

    private int physicalIndex(int offset) {
        return (int) (((long) head + offset) % elements.length);
    }
}
