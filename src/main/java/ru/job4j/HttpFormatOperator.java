package ru.job4j;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;

public class HttpFormatOperator implements Operator {

    private StringBuilder data;
    private final QueuesStorage<String> storage;
    private QueuesStorage<String> workStorage;
    private QueuesStorage<String> topicStorage;
    private final Parser parser;
    private Map<String, Function<String, Map<String, String>>> dispatch =
            Map.of("POST /Queue", postQueue(),
                    "POST /topic", postTopic(),
                    "GET /queue/", getQueue(),
                    "GET /topic/", getTopic());


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
    public Map<String, String> communicate(String input) {
        Map<String, String> response = null;
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
        } else {
            for (String command : dispatch.keySet()) {
                if (input.length() > 1 && command.startsWith(input)) {
                    response = dispatch.get(command).apply(input);
                    break;
                }
            }
        }
        return response;
    }

    @Override
    public Operator getInstance() {
        return new HttpFormatOperator(this.storage, this.parser);
    }

    private Function<String, Map<String, String>> postQueue() {
        return input -> {
            workStorage = storage;
            data = new StringBuilder();
            return Map.of("log", "waiting for object input");
        };
    }

    private Function<String, Map<String, String>> postTopic() {
        return input -> {
            workStorage = topicStorage == null ? storage.copy() : topicStorage;
            data = new StringBuilder();
            return Map.of("log", "waiting for object input");
        };
    }

    private Function<String, Map<String, String>> getQueue() {
        return input -> {
            Map<String, String> response;
            data = null;
            String polled = null;
            String queueName = input.substring(input.lastIndexOf('/') + 1);
            if (storage.contains(queueName)) {
                int tries = 0;
                while (tries++ < 10
                        && (polled = storage.poll(queueName)) == null) {
                    try {
                        storage.wait(1000);
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                    }
                }
                if (polled != null) {
                    try {
                        response = Map.of("log", "object polled from queue",
                                "output",
                                parser.mapToFormat(new HashMap<>(Map.of("queue", queueName,
                                        "text", polled))));
                    } catch (Exception e) {
                        response = Map.of("log", "format convert issue");
                    }
                } else {
                    response = Map.of("log", "queue is empty");
                }
            } else {
                response = Map.of("log", "no such queue in storage");
            }
            return response;
        };
    }

    private Function<String, Map<String, String>> getTopic() {
        return input -> {
            Map<String, String> response;
            data = null;
            if (topicStorage == null) {
                topicStorage = storage.copy();
            }
            String queueName = input.substring(input.lastIndexOf('/') + 1);
            String polled = topicStorage.poll(queueName);
            if (polled != null) {
                try {
                    response = Map.of("log", "object polled from queue",
                            "output",
                            parser.mapToFormat(new HashMap<>(Map.of("queue", queueName,
                                    "text", polled))));
                } catch (Exception e) {
                    response = Map.of("log", "format convert issue");
                }
            } else {
                response = Map.of("log", "topics in queue is over");
            }
            return response;
        };
    }
}