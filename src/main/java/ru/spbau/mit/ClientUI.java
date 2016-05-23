package ru.spbau.mit;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Logger;

/**
 * Created by belaevstanislav on 16.05.16.
 * SPBAU Java practice.
 */

public class ClientUI extends Client {
    private static final Logger LOG = Logger.getLogger(Server.class.getName());

    private static final String NAME = "Torrentino v1.0";

    private TorrentUI ui;

    public ClientUI(int portNumber, String ipAddress, String directoryStr) throws IOException {
        super(portNumber, ipAddress, directoryStr);

        ui = new TorrentUI();

        /*SwingUtilities.invokeLater(() -> {
            ui = new TorrentUI();
        });*/
    }

    public static void main(String[] args) {
        if (args.length > 3) {
            LOG.warning(TOO_MANY_ARGS_MESSAGE);
            System.exit(1);
        }

        try {
            final ClientUI clientUI = new ClientUI(Integer.parseInt(args[0]), args[1], args[2]);
            clientUI.start();
            clientUI.connectToServer();

            // pre-action
            clientUI.updateTable();

            Runtime.getRuntime().addShutdownHook(new Thread(() -> {
                try {
                    clientUI.disconnectFromServer();
                    clientUI.stop();
                } catch (IOException e) {
                    LOG.warning(STOP_SERVER_ERROR_MESSAGE);
                }
            }));
        } catch (IOException exception) {
            exception.printStackTrace();
        }
    }

    public void updateTable() throws IOException {
        final ListAnswer listAnswer = executeList();

        Map<Integer, TableFile> newFiles = new HashMap<>();

        for (Map.Entry<Integer, ListFile> entry : listAnswer.getFiles().entrySet()) {
            final ListFile file = entry.getValue();
            newFiles.put(entry.getKey(), new TableFile(file.getName(), file.getSize(), ""));
        }

        for (Map.Entry<Integer, FileMeta> entry : files.entrySet()) {
            final FileMeta file = entry.getValue();
            newFiles.put(entry.getKey(), new TableFile(file.getName(), file.getSize(),
                    file.isDownloading() ? "downloading" : "local"));
        }

        final Object[][] data = newFiles.entrySet().stream()
                .sorted((e1, e2) -> e1.getKey().compareTo(e2.getKey()))
                .map(e -> new Object[]{e.getKey(), e.getValue().name, e.getValue().size, e.getValue().status})
                .toArray(Object[][]::new);

        JTable table = ui.makeTable(data);
        ui.scrollPane.setViewportView(table);
    }

    private static class TableFile {
        private final String name;
        private final long size;
        private final String status;

        public TableFile(String name, long size, String status) {
            this.name = name;
            this.size = size;
            this.status = status;
        }
    }

    private class TorrentUI extends JFrame implements ActionListener {
        private final JButton downloadButton;
        private final JButton newFileButton;
        private final JScrollPane scrollPane;
        private final JProgressBar progressBar;
        private final JFileChooser fileChooser;

        public TorrentUI() throws HeadlessException {
            super(NAME);
            // EXIT
            setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

            // PROGRESS BAR
            progressBar = new JProgressBar(0, 100);
            progressBar.setValue(50);
            progressBar.setStringPainted(true);
            add(progressBar, BorderLayout.SOUTH);
            progressBar.setVisible(false);

            // BUTTONS
            JPanel menuPanel = new JPanel();
            menuPanel.setLayout(new BoxLayout(menuPanel, BoxLayout.Y_AXIS));
            Dimension buttonsDimension = new Dimension(150, 30);

            downloadButton = new JButton("Загрузить");
            downloadButton.setPreferredSize(buttonsDimension);
            downloadButton.setMinimumSize(buttonsDimension);
            downloadButton.setMaximumSize(buttonsDimension);
            downloadButton.setAlignmentX(Component.CENTER_ALIGNMENT);

            newFileButton = new JButton("Новый файл");
            newFileButton.setPreferredSize(buttonsDimension);
            newFileButton.setMinimumSize(buttonsDimension);
            newFileButton.setMaximumSize(buttonsDimension);
            newFileButton.setAlignmentX(Component.CENTER_ALIGNMENT);
            newFileButton.addActionListener(this);
            fileChooser = new JFileChooser();
            File workingDirectory = new File(System.getProperty("user.dir"), directoryStr);
            fileChooser.setCurrentDirectory(workingDirectory);

            menuPanel.add(downloadButton);
            menuPanel.add(newFileButton);
            add(menuPanel, BorderLayout.WEST);

            // TABLE
            scrollPane = new JScrollPane();
            JTable table = makeTable(new Object[][]{});
            scrollPane.setViewportView(table);
            add(scrollPane, BorderLayout.CENTER);

            // SIZE
            setSize(400, 400);
            //setResizable(false);
            setVisible(true);
        }

        public void actionPerformed(ActionEvent e) {
            if (e.getSource() == newFileButton) {
                if (fileChooser.showOpenDialog(TorrentUI.this) == JFileChooser.APPROVE_OPTION) {
                    try {
                        //System.out.print(fileChooser.getSelectedFile().getAbsolutePath());
                        commitFile(fileChooser.getSelectedFile().getName());
                        updateTable();
                    } catch (IOException exception) {
                        exception.printStackTrace();
                    }
                }
            }
        }

        private JTable makeTable(Object[][] data) {
            return new JTable(data, new String[]{"id", "name", "size", "status"});
        }
    }
}
