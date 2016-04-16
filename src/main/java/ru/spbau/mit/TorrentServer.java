package ru.spbau.mit;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.Serializable;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Created by belaevstanislav on 14.03.16.
 * SPBAU Java practice.
 */

public class TorrentServer {
    // TODO public?
    public static final byte LIST_QUERY_ID = 1;
    public static final byte UPLOAD_QUERY_ID = 2;
    public static final byte SOURCES_QUERY_ID = 3;
    public static final byte UPDATE_QUERY_ID = 4;
    private static final int PORT_NUMBER = 8081;
    private static final int MAX_FILES = 1073741824;
    private static final int IP_ADDRESS_BYTE_COUNT = 4;
    private static final long ACTIVE_SEED_TIME_MILLIS = 300001;
    private static final String WRONG_TYPE_OF_QUERY_MESSAGE = "Wrong type of query!";

    private final ServerSocket serverSocket;
    private final ExecutorService threadPool;
    private final Map<Integer, File> files;
    private int idCounter;

    public TorrentServer() throws IOException {
        serverSocket = new ServerSocket(PORT_NUMBER);
        threadPool = Executors.newCachedThreadPool();
        files = new HashMap<Integer, File>();
        idCounter = -1;
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
                case LIST_QUERY_ID:
                    doList(connection);
                    break;
                case UPLOAD_QUERY_ID:
                    doUpload(connection);
                    break;
                case SOURCES_QUERY_ID:
                    doSources(connection);
                    break;
                case UPDATE_QUERY_ID:
                    doUpdate(connection);
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


    // TODO !!! эти методы вызывают много потоков, но connection для каждого уникален

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

    private void doList(TorrentConnection connection) throws IOException {
        final DataInputStream dataInputStream = connection.getDataInputStream();
        final DataOutputStream dataOutputStream = connection.getDataOutputStream();

        synchronized (files) {
            dataOutputStream.writeInt(files.size());

            for (Map.Entry<Integer, File> entry : files.entrySet()) {
                dataOutputStream.writeInt(entry.getKey());

                final File file = entry.getValue();
                dataOutputStream.writeUTF(file.name);
                dataOutputStream.writeLong(file.size);
            }

            dataOutputStream.flush();
        }
    }

    private void doUpload(TorrentConnection connection) throws IOException {
        final DataInputStream dataInputStream = connection.getDataInputStream();
        final DataOutputStream dataOutputStream = connection.getDataOutputStream();

        final String name = dataInputStream.readUTF();
        final long size = dataInputStream.readLong();

        synchronized (files) {
            idCounter++;
            if (idCounter == MAX_FILES) {
                idCounter = 0;
            }

            files.put(idCounter, new File(name, size));

            dataOutputStream.writeInt(idCounter);
            dataOutputStream.flush();
        }
    }

    private void doSources(TorrentConnection connection) throws IOException {
        final DataInputStream dataInputStream = connection.getDataInputStream();
        final DataOutputStream dataOutputStream = connection.getDataOutputStream();

        final int id = dataInputStream.readInt();

        final Set<Seed> seeds;
        synchronized (files) {
            seeds = files.get(id).seeds;
        }

        synchronized (seeds) {
            for (Seed seed : seeds) {
                seed.checkLastTime(seeds);
            }

            dataOutputStream.writeInt(seeds.size());

            for (Seed seed : seeds) {
                for (int i = 0; i < IP_ADDRESS_BYTE_COUNT; i++) {
                    dataOutputStream.writeByte(seed.ip[i]);
                }
                dataOutputStream.writeShort(seed.port);
            }

            dataOutputStream.flush();
        }
    }

    // TODO check every 5 min?
    private void doUpdate(TorrentConnection connection) throws IOException {
        final DataInputStream dataInputStream = connection.getDataInputStream();
        final DataOutputStream dataOutputStream = connection.getDataOutputStream();

        final byte[] ip = connection.getSocket().getInetAddress().getAddress();
        final short port = dataInputStream.readShort();

        final int count = dataInputStream.readInt();
        for (int i = 0; i < count; i++) {
            final int id = dataInputStream.readInt();

            final Set<Seed> seeds;
            synchronized (files) {
                seeds = files.get(id).seeds;
            }

            synchronized (seeds) {
                final Seed seed = new Seed(ip, port);
                if (!seeds.contains(seed)) {
                    seeds.add(seed);
                }
                seed.resetTime();
            }
        }

        // TODO when false?
        dataOutputStream.writeBoolean(true);
        dataOutputStream.flush();
    }

    private static class File implements Serializable {
        private final String name;
        private final long size;
        private final Set<Seed> seeds;

        public File(String name, long size) {
            this.name = name;
            this.size = size;
            seeds = new HashSet<>();
        }
    }

    private static class Seed implements Serializable {
        private final byte[] ip;
        private final short port;
        private Long lastTime;

        public Seed(byte[] ip, short port) {
            this.ip = ip;
            this.port = port;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) {
                return true;
            }
            if (o == null || getClass() != o.getClass()) {
                return false;
            }
            final Seed seed = (Seed) o;
            return port == seed.port && Arrays.equals(ip, seed.ip);
        }

        @Override
        public int hashCode() {
            int ipHash = 0;
            for (int i = 0; i < IP_ADDRESS_BYTE_COUNT; i++) {
                ipHash = Objects.hash(ipHash, ip[i]);
            }
            return Objects.hash(ipHash, port);
        }

        public void resetTime() {
            synchronized (lastTime) {
                lastTime = System.currentTimeMillis();
            }
        }

        public void checkLastTime(Set<Seed> seeds) {
            final long curTime = System.currentTimeMillis();

            synchronized (lastTime) {
                if (curTime - lastTime > ACTIVE_SEED_TIME_MILLIS) {
                    seeds.remove(this);
                }
            }
        }
    }
}
