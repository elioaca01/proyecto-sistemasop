package avilaos.structures;

/** FIFO con capacidad lógica fija y almacenamiento que crece según el contenido real. */
public final class RingBuffer<T> {
    private Object[] elements;
    private final int capacity;
    private int head;
    private int size;

    public RingBuffer(int capacity) {
        if (capacity <= 0) {
            throw new IllegalArgumentException("La capacidad debe ser positiva");
        }
        this.capacity = capacity;
        elements = new Object[Math.min(capacity, 16)];
    }

    public void put(T value) {
        if (value == null) {
            throw new IllegalArgumentException("El valor no puede ser nulo");
        }
        if (isFull()) {
            throw new IllegalStateException("El buffer está lleno");
        }
        if (size == elements.length) { grow(); }
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

    public int capacity() { return capacity; }

    public boolean isEmpty() { return size == 0; }

    public boolean isFull() { return size == capacity; }

    private void grow() {
        int length = (int) Math.min((long) capacity, (long) elements.length * 2);
        Object[] expanded = new Object[length];
        for (int i = 0; i < size; i++) { expanded[i] = elements[physicalIndex(i)]; }
        elements = expanded;
        head = 0;
    }

    private int physicalIndex(int offset) {
        return (int) (((long) head + offset) % elements.length);
    }
}
