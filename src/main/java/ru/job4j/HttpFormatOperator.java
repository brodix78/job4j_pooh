package ru.job4j;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

public class HttpFormatOperator implements Operator {

    private StringBuilder data;
    private final QueuesStorage<String> storage;
    private QueuesStorage<String> workStorage;
    private QueuesStorage<String> topicStorage;
    private final Parser parser;

    public HttpFormatOperator(QueuesStorage<String> storage, Parser parser) {
        this.storage = storage;
        this.parser = parser;
        workStorage = this.storage;
    }

    /**
     * Control link between server and storage
     * @param input line from client side
     * @return output for client, log information, null if input not (fully) recognised
     */
    @Override
    public HashMap<String, String> communicate(String input) {
        HashMap<String, String> response = null;
        if (data != null) {
            HashMap<String, String> json;
            try {
                json = parser.formatToMap(data.toString());
                response = null;
                if (json != null && json.keySet().size() == 2 && json.containsKey("text")) {
                    if (workStorage == storage && json.containsKey("queue")) {
                        storage.offer(json.get("queue"), json.get("text"));
                        response = new HashMap<>(Map.of("log", "object added to queue"));
                    } else if (workStorage != storage && json.containsKey("topic")) {
                        workStorage.offer(json.get("topic"), json.get("text"));
                        response = new HashMap<>(Map.of("log", "object added to topic"));
                    }
                }
                if (response == null) {
                    response = new HashMap<>(Map.of("log", "wrong JSON data"));
                }
                data = null;
            } catch (IOException e) {
                response = new HashMap<>(Map.of("log", "JSON not recognized"));
            }
        } else if ("POST /queue".equals(input)) {
            workStorage = storage;
            data = new StringBuilder();
        } else if ("POST /topic".equals(input)) {
            workStorage = topicStorage == null ? storage.copy() : topicStorage;
            data = new StringBuilder();
        } else if (input.startsWith("GET /queue/")) {
            data = null;
            String polled;
            String queueName = input.substring(input.lastIndexOf('/') + 1);
            if (storage.contains(queueName)) {
                while ((polled = storage.poll(queueName)) == null) {
                    try {
                        storage.wait();
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                    }
                }
                response = new HashMap<>(Map.of("log", "object polled from queue",
                        "output", polled));
            } else {
                response = new HashMap<>(Map.of("log", "no such queue in storage"));
            }
        } else if (input.startsWith("GET /topic/")) {
            data = null;
            if (topicStorage == null) {
                topicStorage = storage.copy();
            }
            String polled = topicStorage.poll(input.substring(input.lastIndexOf('/') + 1));
            if (polled != null) {
                response = new HashMap<>(Map.of("log", "object polled from queue copy",
                        "output", polled));
            } else {
                response = new HashMap<>(Map.of("log", "topics in queue is over"));
            }

        }
        return response;
    }
}