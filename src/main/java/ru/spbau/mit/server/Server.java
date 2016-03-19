package ru.spbau.mit.server;

import ru.spbau.mit.Connection;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.stream.Collectors;

/**
 * Created by belaevstanislav on 14.03.16.
 */

public class Server implements ServerAction {
    private final ServerSocket serverSocket;
    private final ExecutorService threadPool;

    public Server(int portNumber) throws IOException {
        this.serverSocket = new ServerSocket(portNumber);
        this.threadPool = Executors.newCachedThreadPool();
    }

    private void handleConnetion(Socket socket) throws IOException {
        ServerConnection connection = new ServerConnection(socket);

        switch (connection.readQueryType()) {
            case LIST:
                connection.doList();
                break;
            case GET:
                connection.doGet();
                break;
            default:
                System.err.println("Wrong type of query!");
                break;
        }

        connection.close();
    }

    private void run() {
        while (true) {
            try {
                handleConnetion(serverSocket.accept());
            } catch (IOException e) {
                break;
            }
        }
    }

    @Override
    public void start() {
        threadPool.submit(this::run);
    }

    @Override
    public void stop() throws IOException {
        threadPool.shutdown();
        serverSocket.close();
    }

    private enum QueryType {
        LIST,
        GET;
    }

    private class ServerConnection extends Connection {
        public ServerConnection(Socket socket) throws IOException {
            super(socket);
        }

        public QueryType readQueryType() throws IOException {
            switch (dataInputStream.readInt()) {
                case 1:
                    return QueryType.LIST;
                case 2:
                    return QueryType.GET;
                default:
                    throw new IOException();
            }
        }

        public void doList() throws IOException {
            String path = dataInputStream.readUTF();

            List<Path> files = Files.list(Paths.get(path)).collect(Collectors.toList());
            dataOutputStream.writeInt(files.size());
            for (Path file : files) {
                dataOutputStream.writeUTF(file.getFileName().toString());
                dataOutputStream.writeBoolean(Files.isDirectory(file));
            }

            dataOutputStream.flush();
        }

        public void doGet() throws IOException {
            String path = dataInputStream.readUTF();

            Path file = Paths.get(path);
            dataOutputStream.writeLong(Files.size(file));
            dataOutputStream.write(Files.readAllBytes(file));

            dataOutputStream.flush();
        }
    }
}
