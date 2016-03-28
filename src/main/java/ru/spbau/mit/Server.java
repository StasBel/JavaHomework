package ru.spbau.mit;

import org.apache.commons.io.IOUtils;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Created by belaevstanislav on 14.03.16.
 * SPBAU Java practice.
 */

public class Server {
    private final ServerSocket serverSocket;
    private final Thread mainThread;

    public Server(int portNumber) throws IOException {
        serverSocket = new ServerSocket(portNumber);
        mainThread = new Thread(this::run);
    }

    public void start() {
        mainThread.start();
    }

    public void stop() throws IOException {
        serverSocket.close();
    }

    private void handleConnection(Socket socket) throws IOException {
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
                handleConnection(serverSocket.accept());
            } catch (IOException e) {
                break;
            }
        }
    }

    private static enum QueryType {
        LIST,
        GET;
    }

    private static class ServerConnection extends Connection {
        private final DataInputStream dataInputStream;
        private final DataOutputStream dataOutputStream;

        public ServerConnection(Socket socket) throws IOException {
            super(socket);
            dataInputStream = getDataInputStream();
            dataOutputStream = getDataOutputStream();
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
            String pathStr = dataInputStream.readUTF();
            Path path = Paths.get(pathStr);

            if (Files.isDirectory(path)) {
                List<Path> files = Files.list(path).collect(Collectors.toList());
                dataOutputStream.writeInt(files.size());
                for (Path file : files) {
                    dataOutputStream.writeUTF(file.getFileName().toString());
                    dataOutputStream.writeBoolean(Files.isDirectory(file));
                }
            } else {
                dataOutputStream.writeInt(0);
            }

            dataOutputStream.flush();
        }

        public void doGet() throws IOException {
            String pathStr = dataInputStream.readUTF();
            Path path = Paths.get(pathStr);

            if (Files.exists(path)) {
                dataOutputStream.writeLong(Files.size(path));
                IOUtils.copyLarge(Files.newInputStream(path), dataOutputStream);
            } else {
                dataOutputStream.writeLong(0L);
            }

            dataOutputStream.flush();
        }
    }
}
