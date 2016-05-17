package ru.spbau.mit;

import javax.swing.*;
import java.awt.*;
import java.io.IOException;

/**
 * Created by belaevstanislav on 16.05.16.
 * SPBAU Java practice.
 */

public class ClientUI extends Client {
    private static final String NAME = "Torrentino v1.0";

    public ClientUI(int portNumber, String ipAddress, String directoryStr) throws IOException {
        super(portNumber, ipAddress, directoryStr);

        SwingUtilities.invokeLater(() -> {
            TorrentUI torrentUi = new TorrentUI();
        });
    }

    public static void main(String[] args) {
        try {
            final ClientUI clientUI = new ClientUI(6666, "host", "./");
            clientUI.start();
            clientUI.stop();
        } catch (IOException exception) {
            exception.printStackTrace();
        }
    }

    private class TorrentUI extends JFrame {
        public TorrentUI() throws HeadlessException {
            super(NAME);
            setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

            JProgressBar progressBar = new JProgressBar(0, 100);
            progressBar.setValue(50);
            progressBar.setStringPainted(true);
            add(progressBar, BorderLayout.SOUTH);

            JButton b4 = new JButton("Панель управления");
            add(b4, BorderLayout.WEST);

            JTable table = new JTable(new Object[][]{{"ХУЙ", "ХУЙ"}, {"ХУЙ", "ХУЙ"}},
                    new String[]{"?", "?"});
            add(table, BorderLayout.CENTER);


            setSize(400, 400);
            //setResizable(false);
            setVisible(true);
        }
    }
}
