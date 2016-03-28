package ru.spbau.mit;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.Socket;

/**
 * Created by belaevstanislav on 14.03.16.
 * SPBAU Java practice.
 */

public abstract class Connection {
    private final DataInputStream dataInputStream;
    private final DataOutputStream dataOutputStream;
    private final Socket socket;

    public Connection(Socket socket) throws IOException {
        this.socket = socket;
        dataInputStream = new DataInputStream(socket.getInputStream());
        dataOutputStream = new DataOutputStream(socket.getOutputStream());
    }

    public void close() throws IOException {
        socket.close();
    }

    public boolean isConnected() {
        return socket.isConnected();
    }

    protected DataInputStream getDataInputStream() {
        return dataInputStream;
    }

    protected DataOutputStream getDataOutputStream() {
        return dataOutputStream;
    }
}
