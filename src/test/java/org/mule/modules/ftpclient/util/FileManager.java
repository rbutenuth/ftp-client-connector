package org.mule.modules.ftpclient.util;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.Reader;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.util.Random;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;

public class FileManager {
    private File directory;

    /**
     * Create a new empty directory in the system temp directory.
     */
    public FileManager(String prefix) throws IOException {
        File tempDir = FileUtils.getTempDirectory();
        Random rand = new Random();
        do {
            directory = new File(tempDir, prefix + rand.nextInt(1000));
        } while (directory.exists());
        if (!directory.mkdir()) {
            throw new IOException("Could not create " + directory);
        }
    }

    public File getDirectory() {
        return directory;
    }

    public void createTextFile(File file, String content) throws IOException {
        file.getParentFile().mkdirs();
        try (Writer wr = new OutputStreamWriter(new FileOutputStream(file), StandardCharsets.UTF_8)) {
            wr.write(content);
        }
    }

    public void createBinaryFile(File file, byte[] content) throws IOException {
        file.getParentFile().mkdirs();
        try (OutputStream os = new FileOutputStream(file)) {
            os.write(content);
        }
    }

    public String readTextFile(File file) throws IOException {
        try (InputStream is = new FileInputStream(file);
                Reader rd = new InputStreamReader(is, StandardCharsets.UTF_8)) {
            return IOUtils.toString(rd);
        }

    }

    public byte[] readBinaryFile(File file) throws IOException {
        try (InputStream is = new FileInputStream(file)) {
            return IOUtils.toByteArray(is);
        }
    }

    /**
     * Delete the created directory and its content.
     */
    public void destroy() throws IOException {
        FileUtils.deleteDirectory(directory);
    }
}
