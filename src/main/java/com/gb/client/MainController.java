package com.gb.client;

import com.sun.javafx.application.PlatformImpl;
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
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.ResourceBundle;

import static java.lang.String.valueOf;

public class MainController implements Initializable {
    private static final int BUFFER_SIZE = 256;
    private File currentDirectory;
   //private File serverUserDirectory;
    public TextField clientPath;
    public TextField serverPath;
    public ComboBox <String> disks;
    public ListView<String> clientView;

    // данных нет на клиенте
    public ListView<String> serverView;
    //private File[] rootDrive = File.listRoots();

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
            //disks.getItems()
                    //.add(String.valueOf(rootDrive));
        });
    }

    private void updateServerView(List<String> names ) {
        Platform.runLater(() -> {
            //serverPath.setText(serverUserDirectory.getAbsolutePath());
            serverView.getItems().clear();
            serverView.getItems().add("...");
            serverView.getItems()
                    .addAll(names);
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


    //Download file from server
    public void download(ActionEvent actionEvent) throws IOException {
        os.writeUTF("#get_file#");
        os.writeUTF(serverView.getSelectionModel().getSelectedItem());
        os.flush();
    }

    private void readFromInputStream() {
        try {
            while (true) {
                String command = is.readUTF();
                //#list# size name1 name2 .... name_size
                //#path# pathName
                //#path_action# dirName
                if ("#list#".equals(command)) {
                    List<String> names = new ArrayList<>();
                    int count = is.readInt();
                    for (int i = 1; i < count; i++) {
                        String name = is.readUTF();
                        names.add(name);
                    }
                    updateServerView(names);
                }
                if ("#path#".equals(command)) {
                    String serverDir = is.readUTF();
                    Platform.runLater(() -> serverPath.setText(serverDir));
                }
                if ("#file_message".equals(command)) {
                    String name = is.readUTF();
                    long size = is.readLong();
                    File newFile = currentDirectory.toPath()
                            .resolve(name)
                            .toFile();
                    try (OutputStream fos = new FileOutputStream(newFile)) {
                        for (int i = 0; i < (size + BUFFER_SIZE - 1) / BUFFER_SIZE; i++) {
                            int readCount = is.read(buf);
                            fos.write(buf, 0, readCount);
                        }
                    }
                    updateClientView();
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void initNetwork() {
        try {
            buf = new byte[BUFFER_SIZE];
            Socket socket = new Socket ("localhost", 8189);
            is = new DataInputStream(socket.getInputStream());
            os = new DataOutputStream(socket.getOutputStream());
            Thread readThread = new Thread(this::readFromInputStream);
            readThread.setDaemon(true);
            readThread.start();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {
        currentDirectory = new File (System.getProperty("user.home"));
        //serverUserDirectory = new File("C:\\Users\\nmanov\\IdeaProjects\\gb\\feb-2022\\cloud-storage\\server");
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
