package avilaos.structures;

/** Cola FIFO sobre nuestra lista; el núcleo coordinará el acceso concurrente. */
public final class LinkedQueue<T> {
    private final LinkedList<T> elements = new LinkedList<>();

    public void enqueue(T value) { elements.addLast(value); }

    public T dequeue() { return isEmpty() ? null : elements.removeFirst(); }

    public T peek() { return isEmpty() ? null : elements.get(0); }

    public T get(int index) { return elements.get(index); }

    public int size() { return elements.size(); }

    public boolean isEmpty() { return elements.isEmpty(); }
}
