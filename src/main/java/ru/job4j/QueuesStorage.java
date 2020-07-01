package ru.job4j;

public interface QueuesStorage<T> {

    T poll(String queueName);
    boolean offer(String name, String value);
    QueuesStorage<T> copy();
    boolean contains(String name);
}
