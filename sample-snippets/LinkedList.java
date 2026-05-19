// @step 1
package ch.demo;

// @step 1
public class LinkedList<T> {

// @step 2
    private Node<T> head;
    private int size;

// @step 3
    private static class Node<T> {
        T data;
        Node<T> next;

        Node(T data) {
            this.data = data;
            this.next = null;
        }
    }

// @step 4
    public LinkedList() {
        this.head = null;
        this.size = 0;
    }

// @step 5
    public void addFirst(T element) {
        Node<T> newNode = new Node<>(element);
        newNode.next = head;
        head = newNode;
        size++;
    }

// @step 5
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

// @step 6
    public T removeFirst() {
        if (head == null) {
            throw new RuntimeException("List is empty!");
        }
        T data = head.data;
        head = head.next;
        size--;
        return data;
    }

// @step 6
    public int size() {
        return size;
    }

// @step 6
    public boolean isEmpty() {
        return size == 0;
    }

// @step 7
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

// @step 8
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

// @step 1
}
