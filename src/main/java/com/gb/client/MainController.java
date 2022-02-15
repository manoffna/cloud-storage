package com.gb.client;

import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.fxml.Initializable;
import javafx.scene.control.ComboBox;
import javafx.scene.control.ListView;
import javafx.scene.control.TextField;

import java.io.*;
import java.net.Socket;
import java.net.URL;
import java.nio.file.FileSystem;
import java.nio.file.FileSystems;
import java.util.ResourceBundle;

public class MainController implements Initializable {
    private static final int BUFFER_SIZE = 256;

    private File currentDirectory;
    public TextField clientPath;
    public TextField serverPath;
    public ComboBox<String> disks;
    public ListView<String> clientView;
    public ListView<String> serverView;

    private DataInputStream is;
    private DataOutputStream os;
    private byte[] buf;

    // Platform.runlater(() -> {})
    private void updateClientView() {
        Platform.runLater(() -> {
            clientPath.setText(currentDirectory.getAbsolutePath());
            clientView.getItems().clear();
            clientView.getItems().add("...");
            clientView.getItems()
                    .addAll(currentDirectory.list());
            //disks.getItems().add(String e);
        });
    }

    //Upload file to server
    public void upload(ActionEvent actionEvent) throws IOException {
        String item = clientView.getSelectionModel().getSelectedItem();
        File selected = currentDirectory.toPath().resolve(item).toFile();
        if (selected.isFile()) {
            os.writeUTF("#file_message");
            os.writeUTF(selected.getName());
            os.writeLong(selected.length());
            try (InputStream fis = new FileInputStream(selected)) {
                while (fis.available() > 0) {
                    int readBytes = fis.read(buf);
                    os.write(buf, 0,readBytes);
                }
            }
            os.flush();

        }
    }

    public void download(ActionEvent actionEvent) {

    }

    private void initNetwork() {
        try {
            buf = new byte[BUFFER_SIZE];
            Socket socket = new Socket ("localhost", 8189);
            is = new DataInputStream(socket.getInputStream());
            os = new DataOutputStream(socket.getOutputStream());
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {
        currentDirectory = new File (System.getProperty("user.home"));
        FileSystems.getDefault().getFileStores().forEach(
                f -> System.out.println(f.name())
        );
        updateClientView();
        initNetwork();
        clientView.setOnMouseClicked(e -> {
            String item = clientView.getSelectionModel().getSelectedItem();
            if (item.equals("...")) {
                currentDirectory = currentDirectory.getParentFile();
                updateClientView();
            } else {
                File selected = currentDirectory.toPath().resolve(item).toFile();
                if (selected.isDirectory()) {
                    currentDirectory = selected;
                    updateClientView();
                }
            }
        });
    }
}
