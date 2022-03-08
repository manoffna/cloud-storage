package com.gb.server;

import java.io.*;
import java.net.Socket;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class CloudFileHandler implements Runnable {
    private static final int BUFFER_SIZE = 8192;
    private final Socket socket;
    private final DataInputStream is;
    private final DataOutputStream os;
    private final byte[] buf;
    private File serverDirectory;

    public CloudFileHandler(Socket socket) throws IOException {
        this.socket = socket;
        System.out.println("Client connected");
        is = new DataInputStream(socket.getInputStream());
        os = new DataOutputStream(socket.getOutputStream());
        buf = new byte[BUFFER_SIZE];
        serverDirectory = new File("server");
        sendServerFiles();
        sendCurrentDirName();
    }

    private List<String> getServerFiles() {
        String[] names = serverDirectory.list();
        if (names != null) {
            return Arrays.asList(serverDirectory.list());
        }
        return new ArrayList<>();
    }

    private void sendCurrentDirName() throws IOException {
        os.writeUTF("#path#");
        os.writeUTF(serverDirectory.getName());
    }

    private void sendServerFiles() throws IOException {
        //#list# size name1 name2 .... name_size
        os.writeUTF("#list#");
        List<String> files = getServerFiles();
        os.writeInt(files.size());
        for (String file : files) {
            os.writeUTF(file);
        }
    }

    @Override
        public void run() {
            try {
                while(true) {
                    String command = is.readUTF();
                    if ("#file_message".equals(command)) {
                        String name = is.readUTF();
                        long size = is.readLong();
                        File newFile = serverDirectory.toPath()
                                .resolve(name)
                                .toFile();
                        try (OutputStream fos = new FileOutputStream(newFile)) {
                            for (int i = 0; i < (size + BUFFER_SIZE - 1) / BUFFER_SIZE; i++) {
                                int readCount = is.read(buf);
                                fos.write(buf, 0, readCount);
                            }
                        }
                        System.out.println("File: " + name + " was uploaded");
                        sendServerFiles();
                    } else if ("#get_file#".equals(command)) {
                        String fileName = is.readUTF();
                        File file = serverDirectory.toPath().resolve(fileName).toFile();
                        os.writeUTF("#file_message#");
                        os.writeUTF(fileName);
                        os.writeLong(file.length());
                        try (InputStream fis = new FileInputStream(file)) {
                            while (fis.available() > 0) {
                                int count = fis.read(buf);
                                os.write(buf, 0, count);
                            }
                        }
                        os.flush();
                    } else {
                        System.err.println("Unknown command" + command);
                    }
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
}
