package ru.job4j;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedDeque;

public class Queue implements QueuesStorage<String> {

    private final ConcurrentHashMap<String, ConcurrentLinkedDeque<String>> queue;

    public Queue() {
        this.queue = new ConcurrentHashMap<>();
    }

    private Queue(ConcurrentHashMap<String, ConcurrentLinkedDeque<String>> queue) {
        this.queue = queue;
    }

    @Override
    public String poll(String queueName) {
        return queue.get(queueName).poll();
    }

    @Override
    public boolean offer(String name, String value) {
        boolean rsl;
        if (rsl = queue.get(name).offer(value)) {
            notifyAll();
        }
        return rsl;
    }

    /**
     *
     * @return copy of current queue (for 'topic' use in the project)
     */
    @Override
    public QueuesStorage<String> copy() {
        ConcurrentHashMap<String, ConcurrentLinkedDeque<String>> copy = new ConcurrentHashMap<>();
        for (String name : queue.keySet()) {
            copy.put(name, new ConcurrentLinkedDeque<>(queue.get(name)));
        }
        return new Queue(copy);
    }

    @Override
    public boolean contains(String name) {
        return queue.containsKey(name);
    }
}
