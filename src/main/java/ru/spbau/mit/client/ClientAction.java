package ru.spbau.mit.client;

import java.io.IOException;
import java.net.UnknownHostException;

/**
 * Created by belaevstanislav on 19.03.16.
 * SPBAU Java practice.
 */

public interface ClientAction {
    void connect() throws UnknownHostException, IOException;

    void disconnect() throws IOException;

    Client.ListAnswer executeList(String path) throws IOException;

    Client.GetAnswer executeGet(String path) throws IOException;
}
