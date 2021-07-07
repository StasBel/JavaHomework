package ru.spbau.mit;

import java.io.*;
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

public class Server extends Consts {
    private static final Logger LOG = Logger.getLogger(Server.class.getName());

    private final File savingFile;
    private final ServerSocket serverSocket;
    private final ExecutorService threadPool;
    private final Map<Integer, FileMeta> files;
    private int idCounter;

    public Server(String directoryStr) throws IOException {
        savingFile = new File(directoryStr, SAVE_SERVER_DATA_FILE_NAME);
        if (savingFile.exists()) {
            files = loadData();
        } else {
            files = new HashMap<Integer, FileMeta>();
        }
        serverSocket = new ServerSocket(SERVER_PORT_NUMBER);
        threadPool = Executors.newCachedThreadPool();
        idCounter = -1;
    }

    public static void main(String[] args) {
        final Server server;

        if (args.length > 1) {
            LOG.warning(TOO_MANY_ARGS_MESSAGE);
            System.exit(1);
        }

        try {
            if (args.length == 0) {
                server = new Server(DEFAULT_DIRECTORY_STR);
            } else {
                server = new Server(args[0]);
            }

            server.start();

            Runtime.getRuntime().addShutdownHook(new Thread(() -> {
                try {
                    server.stop();
                } catch (IOException e) {
                    LOG.warning(STOP_SERVER_ERROR_MESSAGE);
                }
            }));
        } catch (IOException e) {
            LOG.severe(NEW_SERVER_ERROR_MESSAGE);
        }
    }

    public void start() {
        threadPool.submit(this::run);
    }

    public void stop() throws IOException {
        threadPool.shutdown();
        serverSocket.close();
        saveData();
    }

    @SuppressWarnings("unchecked")
    private HashMap<Integer, FileMeta> loadData() {
        HashMap<Integer, FileMeta> result;
        try {
            final FileInputStream fileInputStream = new FileInputStream(savingFile);
            final ObjectInputStream objectInputStream = new ObjectInputStream(fileInputStream);
            result = (HashMap<Integer, FileMeta>) objectInputStream.readObject();
            objectInputStream.close();
            fileInputStream.close();
        } catch (IOException | ClassNotFoundException e) {
            LOG.warning(FAIL_TO_LOAD_FILES_MESSAGE);
            result = new HashMap<Integer, FileMeta>();
        }
        return result;
    }

    private void saveData() {
        try {
            final FileOutputStream fileOutputStream = new FileOutputStream(savingFile);
            final ObjectOutputStream objectOutputStream = new ObjectOutputStream(fileOutputStream);
            objectOutputStream.writeObject(files);
            objectOutputStream.flush();
            objectOutputStream.close();
            fileOutputStream.close();
        } catch (IOException e) {
            LOG.warning(FAIL_TO_SAVE_FILES_MESSAGE);
        }
    }

    private void handleConnection(Socket socket) {
        try (Connection connection = new Connection(socket)) {
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
                        LOG.severe(WRONG_TYPE_OF_QUERY_MESSAGE);
                        break;
                }
            }
        } catch (IOException e) {
            if (!socket.isClosed()) {
                LOG.warning(BAD_CONNECTION_MESSAGE);
            }
        }
    }

    private void run() {
        while (true) {
            try {
                final Socket socket = serverSocket.accept();
                threadPool.submit(() -> handleConnection(socket));
            } catch (IOException e) {
                if (!serverSocket.isClosed()) {
                    LOG.warning(BAD_IO_NEW_CONNECTIONS_MESSAGE);
                }
                break;
            }
        }
    }

    private void doList(Connection connection) throws IOException {
        final DataInputStream dataInputStream = connection.getDataInputStream();
        final DataOutputStream dataOutputStream = connection.getDataOutputStream();

        synchronized (files) {
            dataOutputStream.writeInt(files.size());

            for (Map.Entry<Integer, FileMeta> entry : files.entrySet()) {
                dataOutputStream.writeInt(entry.getKey());

                final FileMeta fileMeta = entry.getValue();
                dataOutputStream.writeUTF(fileMeta.name);
                dataOutputStream.writeLong(fileMeta.size);
            }

            dataOutputStream.flush();
        }
    }

    private void doUpload(Connection connection) throws IOException {
        final DataInputStream dataInputStream = connection.getDataInputStream();
        final DataOutputStream dataOutputStream = connection.getDataOutputStream();

        final String name = dataInputStream.readUTF();
        final long size = dataInputStream.readLong();

        synchronized (files) {
            idCounter++;
            if (idCounter == MAX_FILES) {
                idCounter = 0;
            }

            files.put(idCounter, new FileMeta(name, size));

            dataOutputStream.writeInt(idCounter);
            dataOutputStream.flush();
        }
    }

    private void doSources(Connection connection) throws IOException {
        final DataInputStream dataInputStream = connection.getDataInputStream();
        final DataOutputStream dataOutputStream = connection.getDataOutputStream();

        final int id = dataInputStream.readInt();

        final Seeds seeds;
        synchronized (files) {
            final FileMeta fileMeta = files.get(id);
            if (fileMeta != null) {
                seeds = fileMeta.seeds;
            } else {
                seeds = null;
            }
        }

        if (seeds != null) {
            synchronized (seeds) {
                seeds.removeInactive();

                dataOutputStream.writeInt(seeds.size());

                for (Seed seed : seeds) {
                    for (int index = 0; index < IP_ADDRESS_BYTE_COUNT; index++) {
                        dataOutputStream.writeByte(seed.ip[index]);
                    }
                    dataOutputStream.writeShort(seed.port);
                }

                dataOutputStream.flush();
            }
        } else {
            dataOutputStream.writeInt(0);
            dataOutputStream.flush();
        }
    }

    private void doUpdate(Connection connection) throws IOException {
        final DataInputStream dataInputStream = connection.getDataInputStream();
        final DataOutputStream dataOutputStream = connection.getDataOutputStream();

        try {
            final byte[] ip = connection.getSocket().getInetAddress().getAddress();
            final short port = dataInputStream.readShort();

            final int count = dataInputStream.readInt();

            for (int index = 0; index < count; index++) {
                final int id = dataInputStream.readInt();

                final Seeds seeds;
                synchronized (files) {
                    final FileMeta fileMeta = files.get(id);
                    if (fileMeta != null) {
                        seeds = fileMeta.seeds;
                    } else {
                        seeds = null;
                    }
                }

                if (seeds != null) {

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
            }
        } catch (IOException e) {
            dataOutputStream.writeBoolean(false);
            dataOutputStream.flush();
            throw e;
        }

        dataOutputStream.writeBoolean(true);
        dataOutputStream.flush();
    }

    private static class FileMeta implements Serializable {
        private final String name;
        private final long size;
        private final Seeds seeds;

        public FileMeta(String name, long size) {
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
            lastTime = new Long(0);
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
