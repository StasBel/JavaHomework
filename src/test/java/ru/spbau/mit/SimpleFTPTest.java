package ru.spbau.mit;

import org.junit.Test;
import ru.spbau.mit.client.Client;
import ru.spbau.mit.server.Server;

import java.io.IOException;
import java.net.UnknownHostException;
import java.util.Arrays;
import java.util.List;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;

/**
 * Created by belaevstanislav on 19.03.16.
 */

public class SimpleFTPTest {
    private static final int PORT_NUMBER = 6666;
    private static final String IP_ADDRESS = "127.0.0.1";
    private static final String PATH_TO_TEST_RESOURCES = "./src/test/resources/";
    private static final int TEST_RESOURCES_DIR_SIZE = 3;
    private static final String FILE_1 = "File1.txt";
    private static final String FILE_2 = "File2.txt";
    private static final String FILE_3 = "File3.txt";
    private static final List<File> TEST_RESOURCES_FILES = Arrays.asList(
            new File(FILE_1, false),
            new File(FILE_2, false),
            new File(FILE_3, false)
    );
    private static final long FILE_1_SIZE = 136l;
    private static final long FILE_2_SIZE = 134l;
    private static final long FILE_3_SIZE = 83l;
    private static final byte[] FILE_1_CONTENT = new byte[]{73, 32, 100, 105, 100, 32, 110, 111, 116, 32, 106, 111, 105, 110, 32, 116, 104, 101, 32, 114, 101, 115, 105, 115, 116, 97, 110, 99, 101, 32, 109, 111, 118, 101, 109, 101, 110, 116, 32, 116, 111, 32, 107, 105, 108, 108, 32, 112, 101, 111, 112, 108, 101, 44, 32, 116, 111, 32, 107, 105, 108, 108, 32, 116, 104, 101, 32, 110, 97, 116, 105, 111, 110, 46, 32, 76, 111, 111, 107, 32, 97, 116, 32, 109, 101, 32, 110, 111, 119, 46, 32, 65, 109, 32, 73, 32, 97, 32, 115, 97, 118, 97, 103, 101, 32, 112, 101, 114, 115, 111, 110, 63, 32, 77, 121, 32, 99, 111, 110, 115, 99, 105, 101, 110, 99, 101, 32, 105, 115, 32, 99, 108, 101, 97, 114, 46};
    private static final byte[] FILE_2_CONTENT = new byte[]{87, 104, 101, 110, 32, 73, 32, 100, 105, 101, 44, 32, 109, 121, 32, 111, 110, 108, 121, 32, 119, 105, 115, 104, 32, 105, 115, 32, 116, 104, 97, 116, 32, 67, 97, 109, 98, 111, 100, 105, 97, 32, 114, 101, 109, 97, 105, 110, 32, 67, 97, 109, 98, 111, 100, 105, 97, 32, 97, 110, 100, 32, 98, 101, 108, 111, 110, 103, 32, 116, 111, 32, 116, 104, 101, 32, 87, 101, 115, 116, 46, 32, 73, 116, 32, 105, 115, 32, 111, 118, 101, 114, 32, 102, 111, 114, 32, 99, 111, 109, 109, 117, 110, 105, 115, 109, 44, 32, 97, 110, 100, 32, 73, 32, 119, 97, 110, 116, 32, 116, 111, 32, 115, 116, 114, 101, 115, 115, 32, 116, 104, 97, 116, 46};
    private static final byte[] FILE_3_CONTENT = new byte[]{83, 105, 110, 99, 101, 32, 104, 101, 32, 105, 115, 32, 111, 102, 32, 110, 111, 32, 117, 115, 101, 32, 97, 110, 121, 109, 111, 114, 101, 44, 32, 116, 104, 101, 114, 101, 32, 105, 115, 32, 110, 111, 32, 103, 97, 105, 110, 32, 105, 102, 32, 104, 101, 32, 108, 105, 118, 101, 115, 32, 97, 110, 100, 32, 110, 111, 32, 108, 111, 115, 115, 32, 105, 102, 32, 104, 101, 32, 100, 105, 101, 115, 46};

    private void assertGet(Client client, String path, long size, byte[] content) throws IOException {
        Client.GetAnswer getAnswer1 = client.executeGet(path);

        assertEquals("Size", getAnswer1.getSize(), size);
        assertArrayEquals("Content", getAnswer1.getContent(), content);
    }

    @Test
    public void test() {
        try {
            Server server = new Server(PORT_NUMBER);
            server.start();

            Client client = new Client(PORT_NUMBER, IP_ADDRESS);

            // LIST
            client.conect();
            Client.ListAnswer listAnswer = client.executeList(PATH_TO_TEST_RESOURCES);
            assertEquals("Size", (long) listAnswer.getSize(), (long) TEST_RESOURCES_DIR_SIZE);
            assertEquals("Files", listAnswer.getFiles(), TEST_RESOURCES_FILES);
            client.disconnect();

            // GET1
            client.conect();
            assertGet(client, PATH_TO_TEST_RESOURCES + FILE_1, FILE_1_SIZE, FILE_1_CONTENT);
            client.disconnect();

            // GET2
            client.conect();
            assertGet(client, PATH_TO_TEST_RESOURCES + FILE_2, FILE_2_SIZE, FILE_2_CONTENT);
            client.disconnect();

            // GET3
            client.conect();
            assertGet(client, PATH_TO_TEST_RESOURCES + FILE_3, FILE_3_SIZE, FILE_3_CONTENT);
            client.disconnect();

            server.stop();
        } catch (UnknownHostException e) {
            System.err.println("Error! Unknown host!");
            e.printStackTrace();
        } catch (IOException e) {
            System.err.println("Error! Bad IO");
            e.printStackTrace();
        }
    }
}
