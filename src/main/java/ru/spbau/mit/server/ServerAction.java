package ru.spbau.mit.server;

import java.io.IOException;

/**
 * Created by belaevstanislav on 14.03.16.
 */

public interface ServerAction {
    void start();

    void stop() throws IOException;
}
