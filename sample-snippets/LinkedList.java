package ch.demo;

/**
 * A generic singly linked list implementation.
 * Demonstrates generics, inner classes, and data structures.
 */
public class LinkedList<T> {

    private Node<T> head;
    private int size;

    private static class Node<T> {
        T data;
        Node<T> next;

        Node(T data) {
            this.data = data;
            this.next = null;
        }
    }

    public LinkedList() {
        this.head = null;
        this.size = 0;
    }

    public void addFirst(T element) {
        Node<T> newNode = new Node<>(element);
        newNode.next = head;
        head = newNode;
        size++;
    }

    public void addLast(T element) {
        Node<T> newNode = new Node<>(element);
        if (head == null) {
            head = newNode;
        } else {
            Node<T> current = head;
            while (current.next != null) {
                current = current.next;
            }
            current.next = newNode;
        }
        size++;
    }

    public T removeFirst() {
        if (head == null) {
            throw new RuntimeException("List is empty!");
        }
        T data = head.data;
        head = head.next;
        size--;
        return data;
    }

    public int size() {
        return size;
    }

    public boolean isEmpty() {
        return size == 0;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder("[");
        Node<T> current = head;
        while (current != null) {
            sb.append(current.data);
            if (current.next != null) {
                sb.append(" -> ");
            }
            current = current.next;
        }
        sb.append("]");
        return sb.toString();
    }

    public static void main(String[] args) {
        LinkedList<String> list = new LinkedList<>();

        list.addLast("Hello");
        list.addLast("World");
        list.addFirst("Start");

        System.out.println(list);
        System.out.println("Size: " + list.size());

        String removed = list.removeFirst();
        System.out.println("Removed: " + removed);
        System.out.println(list);
    }
}
