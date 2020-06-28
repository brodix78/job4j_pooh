package ru.job4j;

import com.sun.net.httpserver.HttpServer;
import com.sun.org.slf4j.internal.Logger;
import com.sun.org.slf4j.internal.LoggerFactory;

import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.LinkedList;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedDeque;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class RabbitQueue {

    private final ServerSocket socket;
    private final Object monitor = this;
    protected final Logger Log = LoggerFactory.getLogger(RabbitQueue.class);

    ConcurrentHashMap<String, ConcurrentLinkedDeque<String>> queue = new ConcurrentHashMap<>();

    public RabbitQueue(ServerSocket socket, HttpServer server) {
        this.socket = socket;
    }

    ExecutorService executors = Executors.newFixedThreadPool(
            Runtime.getRuntime().availableProcessors()
    );

    /**
     * Implementing action by executors
     */
    public void service() {
        executors.submit(this::action);
    }

    /**
     * Scenario for a single thread of executors
     */
    private void action() {
        try (Socket connection = socket.accept();
             BufferedReader input = new BufferedReader(new InputStreamReader(connection.getInputStream()));
             BufferedWriter output = new BufferedWriter(new OutputStreamWriter(connection.getOutputStream()))) {
            String clientIP = connection.getRemoteSocketAddress().toString();
            Log.debug(String.format("Client IP%s is connected", clientIP));
            String in;
            while ((in = input.readLine()) != null) {
                if ("POST /queue".equals(in) || "POST /topic".equals(in)) {
                    Log.debug(String.format("Client IP%s - publisher", clientIP));
                    String[] json = new String[4];
                    int i = 0;
                    while (i < 4 && (in = input.readLine()) != null) {
                        json[i++] = in;
                    }
                    if (in == null) {
                        break;
                    }
                    if ("{".equals(json[0].replaceAll(" ", ""))
                            && "}".equals(json[0].replaceAll(" ", ""))) {
                        queuePost(new String[]{json[1], json[2]});
                        Log.debug(String.format("Client IP%s added new object", clientIP));
                    }
                } else if (in.startsWith("GET /queue/")) {
                    Log.debug(String.format("Client IP%s - queues subscriber", clientIP));
                    output.write(queueGet(in.substring(11)));
                    output.flush();
                    Log.debug(String.format("Client IP%s polled object", clientIP));
                } else if (in.startsWith("GET /topic/")) {
                    Log.debug(String.format("Client IP%s - topics subscriber", clientIP));
                    String topicName = in.substring(11);
                    LinkedList<String> topics = new LinkedList<>(queue.get(topicName));
                    Log.debug(String.format("Client IP%s received queue copy", clientIP));
                    do {
                        topicName = in.substring(11);
                        String text;
                        if ((text = topics.poll()) != null) {
                            output.write(toJson("topic", topicName, text));
                            output.flush();
                        } else {
                            break;
                        }
                        in = input.readLine();
                    } while (in.startsWith("GET /topic/")
                            && topicName.equals(input.readLine().substring(11)));
                }
            }
            Log.debug("Client IP%s is disconnected", clientIP);
        } catch (IOException e) {
            Log.error(e.getMessage(), e);
            Thread.currentThread().interrupt();
        }
    }

    /**
     * Polling element from queue
     * @param queueName - name of queue
     * @return element from queue with queueName in JSON format
     */
    private String queueGet(String queueName) {
        String text;
        while ((text = queue.get(queueName).poll()) == null) {
            try {
                monitor.wait();
            } catch (InterruptedException e) {
                Log.error(e.getMessage(), e);
                Thread.currentThread().interrupt();
            }
        }
        return toJson("queue", queueName, text);
    }

    /**
     * Adding element to queue
     * @param lines - 0 - queue name, 1 - value
     */

    private void queuePost(String[] lines) {
        String[] keyAndValue = pair(lines);
        queue.get(keyAndValue[0]).offer(keyAndValue[1]);
        monitor.notifyAll();
    }

    /**
     * Convert JSON into two elements array
     * @param lines without { } from JSON
     * @return 0 - queue name, 1 - value
     */
    private String[] pair (String[] lines) {
        for (int i = 0; i < 2; i++) {
            lines[i] = lines[i].substring(lines[i].indexOf(':'));
            lines[i] = lines[i].substring(lines[i].indexOf('"') + 1, lines[i].lastIndexOf('"'));
        }
        return lines;
    }

    /**
     * Create JSON
     * @param type - queue or topic value
     * @param queueName - queue name
     * @param text - value
     * @return String in JSON format
     */
    private String toJson(String type, String queueName, String text) {
        return String.format("{%n  \"%s\" : \"%s\",%n  \"text\" : \"%s\"%n}%n", type, queueName, text);
    }
}
