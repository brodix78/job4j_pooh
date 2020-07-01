package ru.job4j;

import java.net.ServerSocket;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class RabbitQueue {

    ExecutorService executors = Executors.newFixedThreadPool(
            Runtime.getRuntime().availableProcessors()
    );

    /**
     * Implementing action by executors
     */
    public void service(Runnable action) {
        executors.submit(action);
    }

    public void serverStart(ServerSocket socket, QueuesStorage<String> queues) {
        SimpleTextServer thread = new SimpleTextServer(socket, new HttpFormatOperator(queues, new SimpleJSON()));
        service(thread);
    }
}
