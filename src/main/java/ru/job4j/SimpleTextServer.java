package ru.job4j;

import com.sun.org.slf4j.internal.Logger;
import com.sun.org.slf4j.internal.LoggerFactory;

import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.HashMap;

public class SimpleTextServer implements Runnable{

    private final ServerSocket socket;
    private final Logger Log = LoggerFactory.getLogger(SimpleTextServer.class);
    private final Operator operator;

    public SimpleTextServer(ServerSocket socket, Operator operator) {
        this.socket = socket;
        this.operator = operator;
    }

    @Override
    public void run() {
        try (Socket connection = socket.accept();
             BufferedReader input = new BufferedReader(new InputStreamReader(connection.getInputStream()));
             BufferedWriter output = new BufferedWriter(new OutputStreamWriter(connection.getOutputStream()))) {
            String clientIP = connection.getRemoteSocketAddress().toString();
            Log.debug(String.format("Client IP%s is connected", clientIP));
            String in;
            while ((in = input.readLine()) != null) {
                HashMap<String, String> out = operator.communicate(in);
                if (out != null) {
                    if (out.containsKey("log")) {
                        Log.debug(String.format("Client IP%s : %s", clientIP, out.get("log")));
                    }
                    if (out.containsKey("output")) {
                        output.write(out.get("output"));
                        output.flush();
                    }
                }
            }
            Log.debug("Client IP%s is disconnected", clientIP);
        } catch (IOException e) {
            Log.error(e.getMessage(), e);
            Thread.currentThread().interrupt();
        }
    }
}