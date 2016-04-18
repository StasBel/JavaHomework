package ru.spbau.mit;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.Socket;

/**
 * Created by belaevstanislav on 14.03.16.
 * SPBAU Java practice.
 */

public class Connection implements AutoCloseable {
    private final Socket socket;
    private final DataInputStream dataInputStream;
    private final DataOutputStream dataOutputStream;

    public Connection(Socket socket) throws IOException {
        this.socket = socket;
        dataInputStream = new DataInputStream(socket.getInputStream());
        dataOutputStream = new DataOutputStream(socket.getOutputStream());
    }

    public byte readQueryId() throws IOException {
        return dataInputStream.readByte();
    }

    public void close() throws IOException {
        socket.close();
    }

    public boolean isConnected() {
        return socket.isConnected();
    }

    public Socket getSocket() {
        return socket;
    }

    public int getPort() {
        return socket.getPort();
    }

    public DataInputStream getDataInputStream() {
        return dataInputStream;
    }

    public DataOutputStream getDataOutputStream() {
        return dataOutputStream;
    }
}
