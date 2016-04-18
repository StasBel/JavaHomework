package ru.spbau.mit;

import org.junit.Test;

import java.io.IOException;
import java.net.UnknownHostException;

//import static org.junit.Assert.assertEquals;

/**
 * Created by belaevstanislav on 19.03.16.
 * SPBAU Java practice.
 */

public class SimpleTest {
    private static final int PORT_NUMBER_1 = 6666;
    private static final String IP_ADDRESS = "127.0.0.1";
    private static final String DEFAULT_DIRECTORY_STR = "./";

    @Test
    public void simpleTest() throws UnknownHostException, IOException {
        Server server = new Server(DEFAULT_DIRECTORY_STR);
        server.start();

        Client client = new Client(PORT_NUMBER_1, IP_ADDRESS, DEFAULT_DIRECTORY_STR);
        client.start();

        client.connectToServer();

        //Client.ListAnswer listAnswer = client.executeList();
        //assertEquals("Files size", 0, listAnswer.getFiles().size());

        client.disconnectFromServer();

        client.stop();

        server.stop();
    }
}
