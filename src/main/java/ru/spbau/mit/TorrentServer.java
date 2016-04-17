package ru.spbau.mit;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.Serializable;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.TreeSet;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.logging.Logger;

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
    private static final long ACTIVE_SEED_TIME_MILLIS = 300000;
    private static final Logger LOG = Logger.getLogger(TorrentServer.class.getName());
    private static final String WRONG_TYPE_OF_QUERY_MESSAGE = "Wrong type of query!";
    private static final String CONNECTION_OVER_MESSAGE = "Connection is over!";
    private static final String BAD_CONNECTION_MESSAGE = "Something went wrong with connection!";
    private static final String BAD_IO_NEW_CONNECTIONS_MESSAGE = "Bad I/O while waiting for a connection!";

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
        try (TorrentConnection connection = new TorrentConnection(socket)) {
            while (true) {
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
                        LOG.warning(WRONG_TYPE_OF_QUERY_MESSAGE);
                        break;
                }
            }
        } catch (IOException e) {
            LOG.warning(BAD_CONNECTION_MESSAGE);
        }

        LOG.info(CONNECTION_OVER_MESSAGE);
    }

    private void run() {
        while (true) {
            try {
                final Socket socket = serverSocket.accept();
                threadPool.submit(() -> handleConnection(socket));
            } catch (IOException e) {
                LOG.warning(BAD_IO_NEW_CONNECTIONS_MESSAGE);
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

        final Seeds seeds;
        synchronized (files) {
            seeds = files.get(id).seeds;
        }

        synchronized (seeds) {
            seeds.removeInactive();

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

    private void doUpdate(TorrentConnection connection) throws IOException {
        final DataInputStream dataInputStream = connection.getDataInputStream();
        final DataOutputStream dataOutputStream = connection.getDataOutputStream();

        try {
            final byte[] ip = connection.getSocket().getInetAddress().getAddress();
            final short port = dataInputStream.readShort();
            final int count = dataInputStream.readInt();

            for (int i = 0; i < count; i++) {
                final int id = dataInputStream.readInt();

                final Seeds seeds;
                synchronized (files) {
                    seeds = files.get(id).seeds;
                }

                synchronized (seeds) {
                    final Seed freshSeed = new Seed(ip, port);
                    final Seed oldSeed = seeds.ceiling(freshSeed);

                    if (oldSeed == null || freshSeed.compareTo(oldSeed) != 0) {
                        seeds.add(freshSeed);
                        freshSeed.resetTime();
                    } else {
                        oldSeed.resetTime();
                    }
                }
            }
        } catch (IOException e) {
            dataOutputStream.writeBoolean(false);
            dataOutputStream.flush();
            throw e;
        }

        dataOutputStream.writeBoolean(true);
        dataOutputStream.flush();
    }

    private static class File implements Serializable {
        private final String name;
        private final long size;
        private final Seeds seeds;

        public File(String name, long size) {
            this.name = name;
            this.size = size;
            seeds = new Seeds();
        }
    }

    private static class Seeds extends TreeSet<Seed> implements Serializable {
        public void removeInactive() {
            final Iterator<Seed> iterator = iterator();
            while (iterator.hasNext()) {
                final Seed seed = iterator.next();
                if (seed.isInactive()) {
                    iterator.remove();
                }
            }
        }
    }

    private static class Seed implements Serializable, Comparable<Seed> {
        private final byte[] ip;
        private final short port;
        private Long lastTime;

        public Seed(byte[] ip, short port) {
            this.ip = ip;
            this.port = port;
        }

        @Override
        public int compareTo(Seed o) {
            final int compareIpResult = compareIp(o.ip);
            if (compareIpResult == 0) {
                return ((Short) port).compareTo(o.port);
            } else {
                return compareIpResult;
            }
        }

        public void resetTime() {
            synchronized (lastTime) {
                lastTime = System.currentTimeMillis();
            }
        }

        public boolean isInactive() {
            final long curTime = System.currentTimeMillis();

            synchronized (lastTime) {
                return curTime - lastTime > ACTIVE_SEED_TIME_MILLIS;
            }
        }

        private int compareIp(byte[] ip) {
            for (int index = 0; index < IP_ADDRESS_BYTE_COUNT; index++) {
                final int result = ((Byte) this.ip[index]).compareTo(ip[index]);
                if (result != 0) {
                    return result;
                }
            }
            return 0;
        }
    }
}
