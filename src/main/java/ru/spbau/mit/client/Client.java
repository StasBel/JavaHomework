package ru.spbau.mit.client;

import ru.spbau.mit.Connection;
import ru.spbau.mit.File;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.InetAddress;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by belaevstanislav on 14.03.16.
 * SPBAU Java practice.
 */

public class Client implements ClientAction {
    private static final int LIST_NUMBER = 1;
    private static final int GET_NUMBER = 2;

    private final int portNumber;
    private final String ipAddress;
    private ClientConnection connection;

    public Client(int portNumber, String ipAddress) {
        this.portNumber = portNumber;
        this.ipAddress = ipAddress;
    }

    @Override
    public void connect() throws UnknownHostException, IOException {
        connection = new ClientConnection(
                new Socket(InetAddress.getByName(ipAddress), portNumber)
        );
    }

    @Override
    public void disconnect() throws IOException {
        connection.close();
    }

    @Override
    public ListAnswer executeList(String path) throws IOException {
        connection.executeList(path);
        return connection.fetchAnswerList();
    }

    @Override
    public GetAnswer executeGet(String path) throws IOException {
        connection.executeGet(path);
        return connection.fetchAnswerGet();
    }

    private class ClientConnection extends Connection {
        private final DataInputStream dataInputStream;
        private final DataOutputStream dataOutputStream;

        public ClientConnection(Socket socket) throws IOException {
            super(socket);
            this.dataInputStream = getDataInputStream();
            this.dataOutputStream = getDataOutputStream();
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

        public GetAnswer fetchAnswerGet() throws IOException {
            long size = dataInputStream.readLong();
            byte[] content = new byte[(int) size];
            int position = 0;
            while (position < content.length) {
                position += dataInputStream.read(content, position, content.length - position);
            }
            return new GetAnswer(size, content);
        }
    }

    public class ListAnswer {
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

    public class GetAnswer {
        private final long size;
        private final byte[] content;

        public GetAnswer(long size, byte[] content) {
            this.size = size;
            this.content = content;
        }

        public long getSize() {
            return size;
        }

        public byte[] getContent() {
            return content;
        }
    }
}
