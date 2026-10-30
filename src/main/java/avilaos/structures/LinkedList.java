package avilaos.structures;

import java.util.Comparator;

/** Lista propia; el orden de elementos equivalentes se conserva al ordenar. */
public final class LinkedList<T> {
    private static final class Node<T> {
        private final T value;
        private Node<T> next;

        private Node(T value) { this.value = value; }
    }

    private Node<T> head;
    private Node<T> tail;
    private int size;

    public void addLast(T value) {
        if (value == null) {
            throw new IllegalArgumentException("El valor no puede ser nulo");
        }
        Node<T> node = new Node<>(value);
        if (tail == null) {
            head = node;
        } else {
            tail.next = node;
        }
        tail = node;
        size++;
    }

    public T get(int index) { return nodeAt(index).value; }

    /** Copia secuencial O(n), sin volver a recorrer la cabeza por cada índice. */
    public void copyInto(T[] destination) {
        if (destination == null || destination.length < size) {
            throw new IllegalArgumentException("El arreglo debe admitir todos los elementos");
        }
        int index = 0;
        for (Node<T> node = head; node != null; node = node.next) {
            destination[index++] = node.value;
        }
    }

    public T removeFirst() {
        if (head == null) {
            throw new IllegalStateException("La lista está vacía");
        }
        T value = head.value;
        head = head.next;
        size--;
        if (head == null) {
            tail = null;
        }
        return value;
    }

    public T removeAt(int index) {
        checkIndex(index);
        if (index == 0) {
            return removeFirst();
        }
        Node<T> previous = nodeAt(index - 1);
        Node<T> removed = previous.next;
        previous.next = removed.next;
        if (removed == tail) {
            tail = previous;
        }
        size--;
        return removed.value;
    }

    public int size() { return size; }

    public boolean isEmpty() { return size == 0; }

    /** Inserción estable: un elemento nuevo pasa después de los equivalentes. */
    public void sort(Comparator<? super T> order) {
        if (order == null) {
            throw new IllegalArgumentException("Se requiere un comparador");
        }
        Node<T> sorted = null;
        Node<T> current = head;
        while (current != null) {
            Node<T> next = current.next;
            if (sorted == null || order.compare(current.value, sorted.value) < 0) {
                current.next = sorted;
                sorted = current;
            } else {
                Node<T> position = sorted;
                while (position.next != null
                        && order.compare(position.next.value, current.value) <= 0) {
                    position = position.next;
                }
                current.next = position.next;
                position.next = current;
            }
            current = next;
        }
        head = sorted;
        tail = head;
        while (tail != null && tail.next != null) {
            tail = tail.next;
        }
    }

    private Node<T> nodeAt(int index) {
        checkIndex(index);
        Node<T> node = head;
        for (int i = 0; i < index; i++) {
            node = node.next;
        }
        return node;
    }

    private void checkIndex(int index) {
        if (index < 0 || index >= size) {
            throw new IndexOutOfBoundsException("Índice " + index + "; tamaño " + size);
        }
    }
}
