package org.example.filebrowser.model.queue;

import java.util.ArrayDeque;
import java.util.Queue;

public class ConcurrentQueue<T> {
    private final Queue<T> queue = new ArrayDeque<>();
    private final int capacity;

    private final Object lock = new Object();

    public ConcurrentQueue(int capacity) {
        this.capacity = capacity;
    }

    public void add(T item) throws InterruptedException {
        synchronized (lock) {
            while (queue.size() >= capacity) {
                lock.wait();
            }

            queue.add(item);

            lock.notifyAll();
        }
    }

    public T poll() throws InterruptedException {
        synchronized (lock) {
            while (queue.isEmpty()) {
                lock.wait();
            }

            T item = queue.poll();

            lock.notifyAll();

            return item;
        }
    }
}
