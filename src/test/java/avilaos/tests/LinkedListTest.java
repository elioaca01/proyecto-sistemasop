package avilaos.tests;

import avilaos.structures.LinkedList;

public final class LinkedListTest {
    private LinkedListTest() { }

    public static void run() {
        removesFromEachPositionAndCanBeReused();
        rejectsInvalidOperationsWithoutChangingContents();
        sortsStablyAndKeepsTailValid();
    }

    private static void removesFromEachPositionAndCanBeReused() {
        LinkedList<Integer> list = new LinkedList<>();
        TestSupport.isTrue(list.isEmpty());
        TestSupport.equal(0, list.size());
        TestSupport.throwsType(IllegalStateException.class, list::removeFirst);
        list.addLast(10);
        list.addLast(20);
        list.addLast(30);
        TestSupport.equal(3, list.size());
        TestSupport.equal(20, list.get(1));
        TestSupport.equal(20, list.removeAt(1));
        TestSupport.equal(10, list.removeFirst());
        TestSupport.equal(30, list.removeAt(0));
        TestSupport.isTrue(list.isEmpty());
        list.addLast(40);
        list.addLast(50);
        TestSupport.equal(50, list.removeAt(1));
        list.addLast(60);
        TestSupport.equal(40, list.removeFirst());
        TestSupport.equal(60, list.removeFirst());
        list.addLast(70);
        TestSupport.equal(70, list.get(0));
    }

    private static void rejectsInvalidOperationsWithoutChangingContents() {
        LinkedList<Integer> list = new LinkedList<>();
        list.addLast(10);
        list.addLast(20);
        TestSupport.throwsType(IndexOutOfBoundsException.class, () -> list.get(-1));
        TestSupport.throwsType(IndexOutOfBoundsException.class, () -> list.get(2));
        TestSupport.throwsType(IndexOutOfBoundsException.class, () -> list.removeAt(-1));
        TestSupport.throwsType(IndexOutOfBoundsException.class, () -> list.removeAt(2));
        TestSupport.throwsType(IllegalArgumentException.class, () -> list.addLast(null));
        TestSupport.equal(2, list.size());
        TestSupport.equal(10, list.get(0));
        TestSupport.equal(20, list.get(1));
    }

    private record Item(int key, String label) { }

    private static void sortsStablyAndKeepsTailValid() {
        LinkedList<Item> list = new LinkedList<>();
        list.sort((left, right) -> Integer.compare(left.key(), right.key()));
        list.addLast(new Item(2, "A"));
        list.sort((left, right) -> Integer.compare(left.key(), right.key()));
        list.addLast(new Item(1, "B"));
        list.addLast(new Item(2, "C"));
        list.sort((left, right) -> Integer.compare(left.key(), right.key()));
        TestSupport.equal("B", list.get(0).label());
        TestSupport.equal("A", list.get(1).label());
        TestSupport.equal("C", list.get(2).label());
        TestSupport.equal(3, list.size());
        list.addLast(new Item(3, "D"));
        TestSupport.equal("D", list.get(3).label());
        TestSupport.equal("B", list.removeFirst().label());
        TestSupport.equal("A", list.removeFirst().label());
        TestSupport.equal("C", list.removeFirst().label());
        TestSupport.equal("D", list.removeFirst().label());
        TestSupport.isTrue(list.isEmpty());
    }
}
