package ru.spbau.mit;

import org.apache.commons.io.IOUtils;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.InetAddress;
import java.net.Socket;
import java.net.UnknownHostException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by belaevstanislav on 14.03.16.
 * SPBAU Java practice.
 */

public class Client {
    private static final int LIST_NUMBER = 1;
    private static final int GET_NUMBER = 2;

    private final int portNumber;
    private final String ipAddress;
    private ClientConnection connection;

    public Client(int portNumber, String ipAddress) {
        this.portNumber = portNumber;
        this.ipAddress = ipAddress;
    }

    public void connect() throws UnknownHostException, IOException {
        connection = new ClientConnection(
                new Socket(InetAddress.getByName(ipAddress), portNumber)
        );
    }

    public void disconnect() throws IOException {
        if (connection != null && connection.isConnected()) {
            connection.close();
        }
    }

    public ListAnswer executeList(String path) throws IOException {
        if (connection != null && connection.isConnected()) {
            connection.executeList(path);
            return connection.fetchAnswerList();
        } else {
            return null;
        }
    }

    public GetAnswer executeGet(String path) throws IOException {
        if (connection != null && connection.isConnected()) {
            connection.executeGet(path);
            return connection.fetchAnswerGet(path);
        } else {
            return null;
        }
    }

    private static class ClientConnection extends Connection {
        private final DataInputStream dataInputStream;
        private final DataOutputStream dataOutputStream;

        public ClientConnection(Socket socket) throws IOException {
            super(socket);
            dataInputStream = getDataInputStream();
            dataOutputStream = getDataOutputStream();
        }

        public void executeList(String path) throws IOException {
            dataOutputStream.writeInt(LIST_NUMBER);
            dataOutputStream.writeUTF(path);
            dataOutputStream.flush();
        }

        public ListAnswer fetchAnswerList() throws IOException {
            int size = dataInputStream.readInt();
            List<File> files = new ArrayList<>();
            for (int index = 0; index != size; index++) {
                String name = dataInputStream.readUTF();
                boolean isDir = dataInputStream.readBoolean();
                files.add(new File(name, isDir));
            }
            return new ListAnswer(size, files);
        }

        public void executeGet(String path) throws IOException {
            dataOutputStream.writeInt(GET_NUMBER);
            dataOutputStream.writeUTF(path);
            dataOutputStream.flush();
        }

        public GetAnswer fetchAnswerGet(String pathStr) throws IOException {
            long size = dataInputStream.readLong();
            Path path = null;
            if (size > 0) {
                path = Files.createFile(Paths.get(pathStr).getFileName());
                IOUtils.copyLarge(dataInputStream, Files.newOutputStream(path));
            }
            return new GetAnswer(size, path);
        }
    }

    public static class ListAnswer {
        private final int size;
        private final List<File> files;

        public ListAnswer(int size, List<File> files) {
            this.size = size;
            this.files = files;
        }

        public int getSize() {
            return size;
        }

        public List<File> getFiles() {
            return files;
        }
    }

    public static class GetAnswer {
        private final long size;
        private final Path path;

        public GetAnswer(long size, Path path) {
            this.size = size;
            this.path = path;
        }

        public long getSize() {
            return size;
        }

        public Path getPath() {
            return path;
        }
    }
}
