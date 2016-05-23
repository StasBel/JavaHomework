package ru.spbau.mit;

import java.io.*;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.logging.Logger;

/**
 * Created by belaevstanislav on 14.03.16.
 * SPBAU Java practice.
 */

public class Client extends Consts {
    private static final Logger LOG = Logger.getLogger(Client.class.getName());

    protected final Map<Integer, FileMeta> files;
    protected final String directoryStr;
    private final int portNumber;
    private final String ipAddress;
    private final File savingFile;
    private final ServerSocket serverSocket;
    private final ExecutorService threadPool;
    private Connection serverConnection;

    public Client(int portNumber, String ipAddress, String directoryStr) throws IOException {
        this.portNumber = portNumber;
        this.ipAddress = ipAddress;
        this.directoryStr = directoryStr;
        savingFile = new File(directoryStr,
                SAVE_CLIENT_DATA_FILE_NAME + portNumber + SAVE_CLIENT_DATA_FILE_EX
        );
        if (savingFile.exists()) {
            files = loadData();
        } else {
            files = new HashMap<Integer, FileMeta>();
        }
        serverSocket = new ServerSocket(portNumber);
        threadPool = Executors.newCachedThreadPool();
        serverConnection = null;
    }

    // args pattern: (ip:String) (port:int) actions;
    // actions: (list / commit (path:String) / download (id:int) (part:int)^*)
    public static void main(String[] args) {
        if (args.length < MIN_ARGS_NUMBER_CLIENT_MAIN) {
            LOG.warning(NOT_ENOUGH_ARGS_MESSAGE);
            System.exit(1);
        }

        class InvalidArgumentsException extends Exception {
        }

        int index = 0;
        try {
            final Client client = new Client(Integer.valueOf(args[0]), args[1], DEFAULT_DIRECTORY_STR);
            client.start();
            client.connectToServer();
            Runtime.getRuntime().addShutdownHook(new Thread(() -> {
                try {
                    client.disconnectFromServer();
                    client.stop();
                } catch (IOException e) {
                    LOG.warning(STOP_CLIENT_ERROR_MESSAGE);
                }
            }));
            index = 2;

            while (index < args.length) {
                switch (args[index]) {
                    case "list":
                        final ListAnswer listAnswer = client.executeList();
                        System.out.println(listAnswer.count);
                        for (ListFile listFile : listAnswer.files.values()) {
                            System.out.println(listFile.name + " " + listFile.size);
                        }
                        index++;
                        break;
                    case "commit":
                        index++;
                        if (index >= args.length) {
                            throw new InvalidArgumentsException();
                        }
                        final String pathString = args[index];
                        client.commitFile(pathString);
                        index++;
                        break;
                    case "download":
                        index++;
                        if (index >= args.length) {
                            throw new InvalidArgumentsException();
                        }
                        final int id = Integer.valueOf(args[index]);
                        index++;
                        if (index >= args.length) {
                            throw new InvalidArgumentsException();
                        }
                        try {
                            final Set<Integer> parts = new HashSet<>();
                            while (index < args.length) {
                                parts.add(Integer.valueOf(args[index]));
                                index++;
                            }
                            client.downloadFile(id, parts);
                        } catch (NumberFormatException e) {
                            break;
                        }
                        index++;
                        break;
                    default:
                        throw new InvalidArgumentsException();
                }
            }
        } catch (IOException e) {
            LOG.warning(NEW_CLIENT_ERROR_MESSAGE);
        } catch (InvalidArgumentsException e) {
            LOG.warning(INVALID_ARGS_MESSAGE);
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

    public void connectToServer() throws UnknownHostException, IOException {
        serverConnection = new Connection(
                new Socket(InetAddress.getByName(ipAddress), SERVER_PORT_NUMBER)
        );
        executeUpdate();
    }

    public void disconnectFromServer() throws IOException {
        if (serverConnection != null && serverConnection.isConnected()) {
            serverConnection.close();
        }
    }

    public void commitFile(String pathString) throws IOException {
        final File file = new File(directoryStr, pathString);

        if (file.exists()) {
            final String name = file.getName();
            final long size = file.length();
            final UploadAnswer uploadAnswer = executeUpload(name, size);

            final Set<Integer> parts = new HashSet<>();
            final int numberOfParts = getNumberOfParts(size);
            for (int index = 0; index < numberOfParts; index++) {
                parts.add(index);
            }

            synchronized (files) {
                files.put(uploadAnswer.id, new FileMeta(directoryStr, pathString, name, size, parts));
            }

            executeUpdate();
        }
    }

    public void downloadFile(int id) throws IOException {
        final FileMeta fileMeta = getFileMeta(id);
        if (fileMeta == null) {
            LOG.info(NO_SUCH_FILE_MESSAGE);
            return;
        }
        final String name = fileMeta.name;
        final long size = fileMeta.size;

        final Set<Integer> parts = new HashSet<>();
        final int numberOfParts = getNumberOfParts(size);
        for (int index = 0; index < numberOfParts; index++) {
            parts.add(index);
        }

        downloading(id, parts, fileMeta);
    }

    public void downloadFile(int id, Set<Integer> parts) throws IOException {
        final FileMeta fileMeta = getFileMeta(id);
        if (fileMeta == null) {
            LOG.info(NO_SUCH_FILE_MESSAGE);
            return;
        }
        final String name = fileMeta.name;
        final long size = fileMeta.size;

        final int numberOfParts = getNumberOfParts(size);
        for (int part : parts) {
            if (part >= numberOfParts) {
                parts.remove(part);
                LOG.info(NO_SUCH_PART_IN_THIS_FILE);
            }
        }

        downloading(id, parts, fileMeta);
    }

    public ListAnswer executeList() throws IOException {
        if (serverConnection != null && serverConnection.isConnected()) {
            final DataInputStream dataInputStream = serverConnection.getDataInputStream();
            final DataOutputStream dataOutputStream = serverConnection.getDataOutputStream();

            dataOutputStream.writeByte(LIST_QUERY_ID);
            dataOutputStream.flush();

            final int count = dataInputStream.readInt();
            final Map<Integer, ListFile> files = new HashMap<>();
            for (int index = 0; index < count; index++) {
                final int id = dataInputStream.readInt();
                final String name = dataInputStream.readUTF();
                final long size = dataInputStream.readLong();

                files.put(id, new ListFile(name, size));
            }

            return new ListAnswer(count, files);
        } else {
            return null;
        }
    }

    private int getNumberOfParts(long size) {
        int numberOfParts = (int) (size / BLOCK_SIZE);
        if (size % BLOCK_SIZE > 0) {
            numberOfParts++;
        }
        return numberOfParts;
    }

    private FileMeta getFileMeta(int id) throws IOException {
        FileMeta fileMeta;

        synchronized (files) {
            fileMeta = files.get(id);
        }

        if (fileMeta == null) {
            final ListAnswer listAnswer = executeList();
            final ListFile listFile = listAnswer.files.get(id);
            if (listFile == null) {
                return null;
            }
            fileMeta = new FileMeta(directoryStr, listFile.name, listFile.name, listFile.size, new HashSet<>());
        }

        return fileMeta;
    }

    private void downloading(int id, Set<Integer> parts, FileMeta fileMeta) throws IOException {
        synchronized (fileMeta) {
            fileMeta.isDownloading = true;
        }

        final SourcesAnswer sourcesAnswer = executeSources(id);
        for (SourcesSeed seed : sourcesAnswer.seeds) {
            final Connection connection = connectToSeed(seed.ip, seed.port);

            final StatAnswer statAnswer = executeStat(connection, id);
            for (int part : statAnswer.parts) {
                if (parts.contains(part)) {
                    executeGet(connection, id, part, fileMeta);
                    parts.remove(part);
                }
            }

            disconnectFromSeed(connection);
        }

        synchronized (fileMeta) {
            fileMeta.isDownloading = false;
        }

        if (parts.size() != 0) {
            LOG.info(NOT_ALL_PARTS_DOWNLOADED_MESSAGE);
        }

        executeUpdate();
    }

    private Connection connectToSeed(byte[] ip, short port) throws UnknownHostException, IOException {
        return new Connection(new Socket(InetAddress.getByAddress(ip), port));
    }

    private void disconnectFromSeed(Connection connection) throws IOException {
        connection.close();
    }

    private StatAnswer executeStat(Connection connection, int id) throws IOException {
        if (connection != null && connection.isConnected()) {
            final DataInputStream dataInputStream = connection.getDataInputStream();
            final DataOutputStream dataOutputStream = connection.getDataOutputStream();

            dataOutputStream.writeByte(STAT_QUERY_ID);
            dataOutputStream.writeInt(id);
            dataOutputStream.flush();

            final int count = dataInputStream.readInt();
            final Set<Integer> parts = new HashSet<>();
            for (int index = 0; index < count; index++) {
                final int part = dataInputStream.readInt();
                parts.add(part);
            }

            return new StatAnswer(count, parts);
        } else {
            return null;
        }
    }

    private void executeGet(Connection connection, int id, int part, FileMeta fileMeta) throws IOException {
        if (connection != null && connection.isConnected()) {
            final DataInputStream dataInputStream = connection.getDataInputStream();
            final DataOutputStream dataOutputStream = connection.getDataOutputStream();

            dataOutputStream.writeByte(GET_QUERY_ID);
            dataOutputStream.writeInt(id);
            dataOutputStream.writeInt(part);
            dataOutputStream.flush();

            final RandomAccessFile file = fileMeta.file;
            synchronized (file) {
                file.seek(part * BLOCK_SIZE);
                int length = dataInputStream.readInt();
                while (length > 0) {
                    final int thisTime = Math.min(length, BUFF_SIZE);
                    final byte[] bytes = new byte[thisTime];
                    dataInputStream.readFully(bytes);
                    file.write(bytes);
                    length -= thisTime;
                }
            }

            synchronized (fileMeta.parts) {
                fileMeta.parts.add(part);
            }
        }
    }

    private UploadAnswer executeUpload(String name, long size) throws IOException {
        if (serverConnection != null && serverConnection.isConnected()) {
            final DataInputStream dataInputStream = serverConnection.getDataInputStream();
            final DataOutputStream dataOutputStream = serverConnection.getDataOutputStream();

            dataOutputStream.writeByte(UPLOAD_QUERY_ID);
            dataOutputStream.writeUTF(name);
            dataOutputStream.writeLong(size);
            dataOutputStream.flush();

            final int id = dataInputStream.readInt();

            return new UploadAnswer(id);
        } else {
            return null;
        }
    }

    private SourcesAnswer executeSources(int id) throws IOException {
        if (serverConnection != null && serverConnection.isConnected()) {
            final DataInputStream dataInputStream = serverConnection.getDataInputStream();
            final DataOutputStream dataOutputStream = serverConnection.getDataOutputStream();

            dataOutputStream.writeByte(SOURCES_QUERY_ID);
            dataOutputStream.writeInt(id);
            dataOutputStream.flush();

            final int size = dataInputStream.readInt();
            final Set<SourcesSeed> seeds = new HashSet<>();
            for (int index = 0; index < size; index++) {
                final byte[] ip = new byte[IP_ADDRESS_BYTE_COUNT];
                for (int jndex = 0; jndex < IP_ADDRESS_BYTE_COUNT; jndex++) {
                    ip[jndex] = dataInputStream.readByte();
                }
                final short port = dataInputStream.readShort();
                seeds.add(new SourcesSeed(ip, port));
            }

            return new SourcesAnswer(size, seeds);
        } else {
            return null;
        }
    }

    private UpdateAnswer executeUpdate() throws IOException {
        if (serverConnection != null && serverConnection.isConnected()) {
            final DataInputStream dataInputStream = serverConnection.getDataInputStream();
            final DataOutputStream dataOutputStream = serverConnection.getDataOutputStream();

            dataOutputStream.writeByte(UPDATE_QUERY_ID);
            dataOutputStream.writeShort(portNumber);
            synchronized (files) {
                dataOutputStream.writeInt(files.size());
                for (int id : files.keySet()) {
                    dataOutputStream.writeInt(id);
                }
                dataOutputStream.flush();
            }

            final boolean status = dataInputStream.readBoolean();

            return new UpdateAnswer(status);
        } else {
            return null;
        }
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
                    case STAT_QUERY_ID:
                        doStat(connection);
                        break;
                    case GET_QUERY_ID:
                        doGet(connection);
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

    private void doStat(Connection connection) throws IOException {
        final DataInputStream dataInputStream = connection.getDataInputStream();
        final DataOutputStream dataOutputStream = connection.getDataOutputStream();

        final int id = dataInputStream.readInt();

        final Set<Integer> parts;
        synchronized (files) {
            final FileMeta fileMeta = files.get(id);
            if (fileMeta != null) {
                parts = fileMeta.parts;
            } else {
                parts = null;
            }
        }

        if (parts != null) {
            synchronized (parts) {
                dataOutputStream.writeInt(parts.size());
                for (Integer part : parts) {
                    dataOutputStream.writeInt(part);
                }
                dataOutputStream.flush();
            }
        } else {
            dataOutputStream.writeInt(0);
            dataOutputStream.flush();
        }
    }

    private void doGet(Connection connection) throws IOException {
        final DataInputStream dataInputStream = connection.getDataInputStream();
        final DataOutputStream dataOutputStream = connection.getDataOutputStream();

        final int id = dataInputStream.readInt();
        final int part = dataInputStream.readInt();

        final RandomAccessFile file;
        final long size;
        synchronized (files) {
            final FileMeta outerFileMeta = files.get(id);
            if (outerFileMeta != null) {
                file = outerFileMeta.file;
                size = outerFileMeta.size;
            } else {
                file = null;
                size = 0;
            }
        }

        if (file != null) {
            synchronized (file) {
                file.seek(part * BLOCK_SIZE);
                int length = (int) Math.min(size - file.getFilePointer(), BLOCK_SIZE);
                dataOutputStream.writeInt(length);
                while (length > 0) {
                    final int thisTime = Math.min(length, BUFF_SIZE);
                    final byte[] bytes = new byte[thisTime];
                    file.readFully(bytes);
                    dataOutputStream.write(bytes);
                    length -= thisTime;
                }

                dataOutputStream.flush();
            }
        } else {
            dataOutputStream.write(0);
            dataOutputStream.flush();
        }
    }

    private static class StatAnswer {
        private final int count;
        private final Set<Integer> parts;

        public StatAnswer(int count, Set<Integer> parts) {
            this.count = count;
            this.parts = parts;
        }
    }

    public static class ListFile {
        private final String name;
        private final long size;

        public ListFile(String name, long size) {
            this.name = name;
            this.size = size;
        }

        public String getName() {
            return name;
        }

        public long getSize() {
            return size;
        }
    }

    public static class ListAnswer {
        private final int count;
        private final Map<Integer, ListFile> files;

        public ListAnswer(int count, Map<Integer, ListFile> files) {
            this.count = count;
            this.files = files;
        }

        public int getCount() {
            return count;
        }

        public Map<Integer, ListFile> getFiles() {
            return files;
        }
    }

    private static class UploadAnswer {
        private final int id;

        public UploadAnswer(int id) {
            this.id = id;
        }
    }

    private static class SourcesSeed {
        private final byte[] ip;
        private final short port;

        public SourcesSeed(byte[] ip, short port) {
            this.ip = ip;
            this.port = port;
        }
    }

    private static class SourcesAnswer {
        private final int size;
        private final Set<SourcesSeed> seeds;

        public SourcesAnswer(int size, Set<SourcesSeed> seeds) {
            this.size = size;
            this.seeds = seeds;
        }
    }

    private static class UpdateAnswer {
        private final boolean status;

        public UpdateAnswer(boolean status) {
            this.status = status;
        }
    }

    protected static class FileMeta implements Serializable {
        private static final String OPEN_MODE = "rw";

        private final String directoryStr;
        private final String pathString;
        private final String name;
        private final long size;
        private final Set<Integer> parts;
        private boolean isDownloading;
        private transient RandomAccessFile file;

        public FileMeta(String directoryStr, String pathString, String name,
                        long size, Set<Integer> parts) throws FileNotFoundException {
            this.directoryStr = directoryStr;
            this.pathString = pathString;
            this.name = name;
            this.size = size;
            this.parts = parts;
            file = new RandomAccessFile(new File(directoryStr, pathString), OPEN_MODE);
            isDownloading = false;
        }

        public String getName() {
            return name;
        }

        public long getSize() {
            return size;
        }

        public boolean isDownloading() {
            return isDownloading;
        }

        private void readObject(ObjectInputStream objectInputStream) throws IOException, ClassNotFoundException {
            objectInputStream.defaultReadObject();
            file = new RandomAccessFile(new File(directoryStr, pathString), OPEN_MODE);
        }
    }
}
