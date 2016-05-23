package ru.spbau.mit;

import junitx.framework.Assert;
import junitx.framework.FileAssert;
import org.junit.Test;

import java.io.File;
import java.io.IOException;
import java.net.UnknownHostException;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

import static org.junit.Assert.*;

/**
 * Created by belaevstanislav on 19.03.16.
 * SPBAU Java practice.
 */

public class TorrentTest {
    private static final int PORT_NUMBER_1 = 6666;
    private static final int PORT_NUMBER_2 = 6667;
    private static final int PORT_NUMBER_3 = 6668;
    private static final String IP_ADDRESS_1 = "127.0.0.1";
    private static final String DEFAULT_DIRECTORY_STR = "./src/test/resources/";
    private static final String SEED_CLIENT_DIRECTORY_STR = DEFAULT_DIRECTORY_STR + "seed/";
    private static final String LEECH_CLIENT_DIRECTORY_STR = DEFAULT_DIRECTORY_STR + "leech/";
    private static final String FILE_1_NAME = "Text1.txt";
    private static final String FILE_2_NAME = "Text2.txt";
    private static final String FILE_3_NAME = "Text3.txt";
    private static final String FILE_4_NAME = "Picture.bmp";
    private static final String FILE_5_NAME = "50MB.zip";
    private static final long FILE_1_SIZE = 136L;
    private static final long FILE_2_SIZE = 134L;
    private static final long FILE_3_SIZE = 83L;
    private static final int TEST_LIST_AND_COMMIT_NUMBER_OF_FILES = 3;
    private static final int TEST_DOWNLOAD_NUMBER_OF_FILES = 3;
    private static final int TEST_SERIALIZATION_NUMBER_OF_FILES = 1;

    public static void main(String[] args) {
        clearSaves();

        Server.main(new String[]{DEFAULT_DIRECTORY_STR});

        ClientUI.main(new String[]{String.valueOf(PORT_NUMBER_1), IP_ADDRESS_1, SEED_CLIENT_DIRECTORY_STR});
    }

    @Test
    public void testListAndCommit() throws UnknownHostException, IOException {
        clearSaves(DEFAULT_DIRECTORY_STR);

        final Server server = new Server(DEFAULT_DIRECTORY_STR);
        server.start();

        final Client client = new Client(PORT_NUMBER_1, IP_ADDRESS_1, SEED_CLIENT_DIRECTORY_STR);
        client.start();

        client.connectToServer();

        client.commitFile(FILE_1_NAME);
        client.commitFile(FILE_2_NAME);
        client.commitFile(FILE_3_NAME);

        final Client.ListAnswer listAnswer = client.executeList();

        assertNotNull("Not null answer", listAnswer);
        assertEquals("Count", TEST_LIST_AND_COMMIT_NUMBER_OF_FILES, listAnswer.getCount());
        assertNotNull("Not null files", listAnswer.getFiles());
        assertEquals("Files size", TEST_LIST_AND_COMMIT_NUMBER_OF_FILES, listAnswer.getFiles().size());
        final boolean[] isVis = new boolean[TEST_LIST_AND_COMMIT_NUMBER_OF_FILES];
        for (Client.ListFile file : listAnswer.getFiles().values()) {
            switch (file.getName()) {
                case FILE_1_NAME:
                    isVis[0] = true;
                    assertEquals("Text1.txt size", FILE_1_SIZE, file.getSize());
                    break;
                case FILE_2_NAME:
                    isVis[1] = true;
                    assertEquals("Text2.txt size", FILE_2_SIZE, file.getSize());
                    break;
                case FILE_3_NAME:
                    isVis[2] = true;
                    assertEquals("Text3.txt size", FILE_3_SIZE, file.getSize());
                    break;
                default:
                    fail("No such file");
            }
        }
        final boolean[] isVisExpected = {true, true, true};
        assertArrayEquals("Check duplicate", isVisExpected, isVis);

        client.disconnectFromServer();

        client.stop();

        server.stop();
    }

    @Test
    public void testDownload() throws UnknownHostException, IOException {
        clearSaves();
        deleteFileIfExists(LEECH_CLIENT_DIRECTORY_STR, FILE_1_NAME);
        deleteFileIfExists(LEECH_CLIENT_DIRECTORY_STR, FILE_4_NAME);
        deleteFileIfExists(LEECH_CLIENT_DIRECTORY_STR, FILE_5_NAME);

        final Server server = new Server(DEFAULT_DIRECTORY_STR);
        server.start();

        final Client client1 = new Client(PORT_NUMBER_1, IP_ADDRESS_1, SEED_CLIENT_DIRECTORY_STR);
        client1.start();

        client1.connectToServer();
        client1.commitFile(FILE_1_NAME);
        client1.commitFile(FILE_4_NAME);
        client1.commitFile(FILE_5_NAME);
        client1.disconnectFromServer();

        final Client client2 = new Client(PORT_NUMBER_2, IP_ADDRESS_1, LEECH_CLIENT_DIRECTORY_STR);
        client2.start();

        client2.connectToServer();
        final Client.ListAnswer listAnswer = client2.executeList();
        assertEquals("Count", TEST_DOWNLOAD_NUMBER_OF_FILES, listAnswer.getCount());
        for (int id : listAnswer.getFiles().keySet()) {
            client2.downloadFile(id);
        }
        FileAssert.assertEquals("Text1.txt equality",
                new File(SEED_CLIENT_DIRECTORY_STR, FILE_1_NAME),
                new File(LEECH_CLIENT_DIRECTORY_STR, FILE_1_NAME)
        );
        FileAssert.assertEquals("Picture.bmp equality",
                new File(SEED_CLIENT_DIRECTORY_STR, FILE_4_NAME),
                new File(LEECH_CLIENT_DIRECTORY_STR, FILE_4_NAME)
        );
        FileAssert.assertEquals("50MB.zip equality",
                new File(SEED_CLIENT_DIRECTORY_STR, FILE_5_NAME),
                new File(LEECH_CLIENT_DIRECTORY_STR, FILE_5_NAME)
        );
        client2.disconnectFromServer();

        client2.stop();

        client1.stop();

        server.stop();
    }

    @Test
    public void testPartsDownload() throws IOException {
        clearSaves();
        deleteFileIfExists(LEECH_CLIENT_DIRECTORY_STR, FILE_5_NAME);

        final Server server = new Server(DEFAULT_DIRECTORY_STR);
        server.start();

        final Client client1 = new Client(PORT_NUMBER_1, IP_ADDRESS_1, SEED_CLIENT_DIRECTORY_STR);
        client1.start();

        client1.connectToServer();
        client1.commitFile(FILE_5_NAME);
        client1.disconnectFromServer();

        final Client client2 = new Client(PORT_NUMBER_2, IP_ADDRESS_1, LEECH_CLIENT_DIRECTORY_STR);
        client2.start();
        client2.connectToServer();
        final Client.ListAnswer listAnswer = client2.executeList();
        final int id = listAnswer.getFiles().keySet().iterator().next();

        final Set<Integer> parts1 = new HashSet<>(Arrays.asList(0, 2, 4));
        client2.downloadFile(id, parts1);

        Assert.assertNotEquals("Files size",
                (new File(SEED_CLIENT_DIRECTORY_STR, FILE_5_NAME)).length(),
                (new File(LEECH_CLIENT_DIRECTORY_STR, FILE_5_NAME)).length()
        );

        final Set<Integer> parts2 = new HashSet<>(Arrays.asList(1, 3, 5));
        client2.downloadFile(id, parts2);

        FileAssert.assertEquals("50MB.zip equality",
                new File(SEED_CLIENT_DIRECTORY_STR, FILE_5_NAME),
                new File(LEECH_CLIENT_DIRECTORY_STR, FILE_5_NAME)
        );

        client2.disconnectFromServer();
        client2.stop();

        client1.stop();

        server.stop();
    }

    @Test
    public void testSerialization() throws IOException {
        clearSaves();
        deleteFileIfExists(LEECH_CLIENT_DIRECTORY_STR, FILE_1_NAME);

        final Server server = new Server(DEFAULT_DIRECTORY_STR);
        server.start();

        final Client client1 = new Client(PORT_NUMBER_1, IP_ADDRESS_1, SEED_CLIENT_DIRECTORY_STR);
        client1.start();
        client1.connectToServer();
        client1.commitFile(FILE_1_NAME);
        client1.disconnectFromServer();
        client1.stop();

        // using same .ser file
        final Client client2 = new Client(PORT_NUMBER_1, IP_ADDRESS_1, SEED_CLIENT_DIRECTORY_STR);
        client2.start();

        final Client client3 = new Client(PORT_NUMBER_2, IP_ADDRESS_1, LEECH_CLIENT_DIRECTORY_STR);
        client3.start();
        client3.connectToServer();

        final Client.ListAnswer listAnswer = client3.executeList();
        assertEquals("Same as old one", TEST_SERIALIZATION_NUMBER_OF_FILES, listAnswer.getCount());

        final int id = listAnswer.getFiles().keySet().iterator().next();
        client3.downloadFile(id);

        FileAssert.assertEquals("Text1.txt equality",
                new File(SEED_CLIENT_DIRECTORY_STR, FILE_1_NAME),
                new File(LEECH_CLIENT_DIRECTORY_STR, FILE_1_NAME)
        );

        client3.disconnectFromServer();
        client3.stop();

        client2.stop();

        server.stop();
    }

    @Test
    public void testUi() throws IOException {
        clearSaves();

        final Server server = new Server(DEFAULT_DIRECTORY_STR);
        server.start();

        final ClientUI clientUI = new ClientUI(PORT_NUMBER_1, IP_ADDRESS_1, SEED_CLIENT_DIRECTORY_STR);
        clientUI.start();

        clientUI.connectToServer();

        clientUI.commitFile(FILE_1_NAME);
        clientUI.commitFile(FILE_2_NAME);
        clientUI.commitFile(FILE_3_NAME);

        clientUI.updateTable();

        watchForAWhile();

        clientUI.disconnectFromServer();

        clientUI.stop();

        server.stop();
    }

    private static void clearSaves() {
        clearSaves(DEFAULT_DIRECTORY_STR);
        clearSaves(SEED_CLIENT_DIRECTORY_STR);
        clearSaves(LEECH_CLIENT_DIRECTORY_STR);
    }

    private static void clearSaves(String directoryString) {
        File folder = new File(directoryString);
        File[] fileList = folder.listFiles();

        for (File file : fileList) {
            if (file.getName().endsWith(".ser")) {
                file.delete();
            }
        }
    }

    private void watchForAWhile() {
        try {
            Thread.sleep(5000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    private void deleteFileIfExists(String directoryString, String fileName) {
        final File file = new File(directoryString, fileName);
        if (file.exists()) {
            file.delete();
        }
    }
}
