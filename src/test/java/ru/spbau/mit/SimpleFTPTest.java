package ru.spbau.mit;

/**
 * Created by belaevstanislav on 19.03.16.
 * SPBAU Java practice.
 */

public class SimpleFTPTest {
    /*private static final int PORT_NUMBER = 6666;
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
    private static final long FILE_1_SIZE = 136L;
    private static final long FILE_2_SIZE = 134L;
    private static final long FILE_3_SIZE = 83L;

    @Test
    public void test() throws UnknownHostException, IOException {
        TorrentServer torrentServer = new TorrentServer(PORT_NUMBER);
        torrentServer.start();

        TorrentClient client = new TorrentClient(PORT_NUMBER, IP_ADDRESS);

        // LIST
        client.connect();
        TorrentClient.ListAnswer listAnswer = client.executeList(PATH_TO_TEST_RESOURCES);
        assertEquals("Size", (long) TEST_RESOURCES_DIR_SIZE, (long) listAnswer.getSize());
        assertEquals("Files", TEST_RESOURCES_FILES, listAnswer.getFiles());
        client.disconnect();

        // GET1
        client.connect();
        assertGet(client, PATH_TO_TEST_RESOURCES + FILE_1, FILE_1_SIZE);
        client.disconnect();

        // GET2
        client.connect();
        assertGet(client, PATH_TO_TEST_RESOURCES + FILE_2, FILE_2_SIZE);
        client.disconnect();

        // GET3
        client.connect();
        assertGet(client, PATH_TO_TEST_RESOURCES + FILE_3, FILE_3_SIZE);
        client.disconnect();

        torrentServer.stop();
    }

    private void assertGet(TorrentClient client, String path, long size) throws IOException {
        TorrentClient.GetAnswer getAnswer1 = client.executeGet(path);

        assertEquals("Size", size, getAnswer1.getSize());

        Path filePath = getAnswer1.getPath();
        assertTrue("Content",
                IOUtils.contentEquals(
                        Files.newInputStream(Paths.get(path)),
                        Files.newInputStream(filePath)
                )
        );

        Files.delete(filePath);
    }*/
}
