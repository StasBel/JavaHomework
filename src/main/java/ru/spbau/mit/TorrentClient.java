package ru.spbau.mit;

import org.apache.commons.io.IOUtils;

import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.channels.Channels;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Created by belaevstanislav on 14.03.16.
 * SPBAU Java practice.
 */

public class TorrentClient {
    // TODO public?
    public static final byte STAT_QUERY_ID = 1;
    public static final byte GET_QUERY_ID = 2;
    public static final long BLOCK_SIZE = 10485760;
    private static final String WRONG_TYPE_OF_QUERY_MESSAGE = "Wrong type of query!";

    private final ServerSocket serverSocket;
    private final ExecutorService threadPool;
    private final Map<Integer, File> files;

    public TorrentClient(int portNumber) throws IOException {
        serverSocket = new ServerSocket();
        threadPool = Executors.newCachedThreadPool();
        files = new HashMap<Integer, File>();
    }

    public void start() {
        threadPool.submit(this::run);
    }

    public void stop() throws IOException {
        threadPool.shutdown();
        serverSocket.close();
    }

    private void handleConnection(Socket socket) {
        try {
            final TorrentConnection connection = new TorrentConnection(socket);

            switch (connection.readQueryId()) {
                case STAT_QUERY_ID:
                    doStat(connection);
                    break;
                case GET_QUERY_ID:
                    doGet(connection);
                    break;
                default:
                    System.err.println(WRONG_TYPE_OF_QUERY_MESSAGE);
                    break;
            }

            connection.close();
        } catch (IOException e) {
            // TODO ???
        }
    }

    private void run() {
        while (true) {
            try {
                final Socket socket = serverSocket.accept();
                threadPool.submit(() -> handleConnection(socket));
            } catch (IOException e) {
                break;
            }
        }
    }


    // TODO !!! эти методы вызывают много потоков, но connection для каждого уникален

    private void doStat(TorrentConnection connection) throws IOException {
        final DataInputStream dataInputStream = connection.getDataInputStream();
        final DataOutputStream dataOutputStream = connection.getDataOutputStream();

        final int id = dataInputStream.readInt();

        final Set<Integer> parts;
        synchronized (files) {
            parts = files.get(id).parts;
        }

        synchronized (parts) {
            dataOutputStream.writeInt(parts.size());
            for (Integer part : parts) {
                dataOutputStream.writeInt(part);
            }
            dataOutputStream.flush();
        }
    }

    private void doGet(TorrentConnection connection) throws IOException {
        final DataInputStream dataInputStream = connection.getDataInputStream();
        final DataOutputStream dataOutputStream = connection.getDataOutputStream();

        final int id = dataInputStream.readInt();
        final int part = dataInputStream.readInt();

        final RandomAccessFile file;
        synchronized (files) {
            file = files.get(id).file;
        }

        synchronized (file) {
            file.seek(part * BLOCK_SIZE);
            // TODO copyLarge?
            IOUtils.copy(Channels.newInputStream(file.getChannel()), dataOutputStream);
            dataOutputStream.flush();
        }
    }

    /*public static void main(String[] args) {
        try {
            File file;
            file = new File("./src/main/java/ru/spbau/mit/test.txt");
            file.parts.add(5);

            try {
                FileOutputStream fs = new FileOutputStream("test.ser");
                ObjectOutputStream os = new ObjectOutputStream(fs);
                os.writeObject(file);
                os.flush();
                os.close();

                FileInputStream fis = new FileInputStream("test.ser");
                ObjectInputStream ois = new ObjectInputStream(fis);
                file = (File) ois.readObject();
                ois.close();

                System.out.println(file.pathString);
                System.out.println(file.parts.contains(5));
            } catch (Exception e) {
                e.printStackTrace();
            }
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }
    }*/

    private static class File implements Serializable {
        private static final String OPEN_MODE = "rw";

        private final Set<Integer> parts;
        private final String pathString;
        private transient RandomAccessFile file;

        public File(String pathString) throws FileNotFoundException {
            parts = new HashSet<>();
            this.pathString = pathString;
            file = new RandomAccessFile(new java.io.File(pathString), OPEN_MODE);
        }

        private void readObject(ObjectInputStream ois) throws IOException, ClassNotFoundException {
            ois.defaultReadObject();
            file = new RandomAccessFile(new java.io.File(pathString), OPEN_MODE);
        }
    }
}
