package org.mule.modules.ftpclient.ftp;

import static org.junit.Assert.*;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

import org.apache.commons.io.IOUtils;
import org.junit.Test;
import org.mule.api.ConnectionException;
import org.mule.modules.ftpclient.FtpClientConnector;
import org.mule.modules.ftpclient.config.TransferMode;

public class FtpGetTest extends AbstractFtpClientTest {

    @Test
    public void simpleGetStream() throws Exception {
        final String HELLO = "Hello, world!";
        FtpClientConnector connector = connectorFactory.createConnector(TransferMode.Ascii, true);

        fileManager.createTextFile(new File(fileManager.getDirectory(), "test.txt"), HELLO);
        try (InputStream is = (InputStream) connector.getFile("", "test.txt", true)) {
            assertNotNull("input stream", is);
            assertEquals(HELLO, IOUtils.toString(is, StandardCharsets.UTF_8));
        }
        assertEquals(0, connector.getConfig().getActiveConnections());
    }

    @Test(expected = ConnectionException.class)
    public void getFailsWhenServerStopped() throws Exception {
        FtpClientConnector connector = connectorFactory.createConnector(TransferMode.Ascii, true);
        stopFtpServer();
        connector.getFile("", "does-not-matter", true);
    }

    @Test
    public void getNonexistingFile() throws Exception {
        FtpClientConnector connector = connectorFactory.createConnector(TransferMode.Ascii, true);

        try {
            connector.getFile("", "test.txt", true);
            fail("Exception missing");
        } catch (IOException e) {
            // expected
        }
        assertEquals(0, connector.getConfig().getActiveConnections());
    }

    @Test
    public void simpleGetStreamNullDirectory() throws Exception {
        final String HELLO = "Hello, world!";
        FtpClientConnector connector = connectorFactory.createConnector(TransferMode.Ascii, true);

        fileManager.createTextFile(new File(fileManager.getDirectory(), "test.txt"), HELLO);
        try (InputStream is = (InputStream) connector.getFile(null, "test.txt", true)) {
            assertNotNull("input stream", is);
            assertEquals(HELLO, IOUtils.toString(is, StandardCharsets.UTF_8));
        }
        assertEquals(0, connector.getConfig().getActiveConnections());
    }

    @Test
    public void simpleGetStreamRelativeDirectory() throws Exception {
        final String HELLO = "Hello, world!";
        FtpClientConnector connector = connectorFactory.createConnector(TransferMode.Ascii, true);

        fileManager.createTextFile(new File(new File(fileManager.getDirectory(), "sub-dir"), "test.txt"), HELLO);
        try (InputStream is = (InputStream) connector.getFile("sub-dir", "test.txt", true)) {
            assertNotNull("input stream", is);
            assertEquals(HELLO, IOUtils.toString(is, StandardCharsets.UTF_8));
        }
        assertEquals(0, connector.getConfig().getActiveConnections());
    }

    @Test
    public void simpleGetStreamAbsoluteDirectory() throws Exception {
        final String HELLO = "Hello, world!";
        FtpClientConnector connector = connectorFactory.createConnector(TransferMode.Ascii, true);

        fileManager.createTextFile(new File(new File(fileManager.getDirectory(), "sub-dir"), "test.txt"), HELLO);
        try (InputStream is = (InputStream) connector.getFile("/sub-dir", "test.txt", true)) {
            assertNotNull("input stream", is);
            assertEquals(HELLO, IOUtils.toString(is, StandardCharsets.UTF_8));
        }
        assertEquals(0, connector.getConfig().getActiveConnections());
    }

    @Test
    public void asciiOnGetShouldChangeLineEndings() throws Exception {
        // Use Unix and Windows, one should be changed on one of the systems...
        final String HELLO = "Hallöle!\nNext line\r\nAnother One";
        FtpClientConnector connector = connectorFactory.createConnector(TransferMode.Ascii, true);

        fileManager.createTextFile(new File(fileManager.getDirectory(), "test.txt"), HELLO);
        try (InputStream is = (InputStream) connector.getFile("", "test.txt", true)) {
            assertNotNull("input stream", is);
            assertFalse(HELLO.equals(IOUtils.toString(is, StandardCharsets.UTF_8)));
        }
        assertEquals(0, connector.getConfig().getActiveConnections());
    }

    @Test
    public void binaryOnGetShouldKeepLineEndings() throws Exception {
        // Use Unix and Windows, one should be changed on one of the systems...
        final String HELLO = "Hallöle!\nNext line\r\nAnother One";
        FtpClientConnector connector = connectorFactory.createConnector(TransferMode.Binary, true);

        File testFile = new File(fileManager.getDirectory(), "test.txt");
        fileManager.createTextFile(testFile, HELLO);
        try (InputStream is = (InputStream) connector.getFile("", "test.txt", true)) {
            assertNotNull("input stream", is);
            assertEquals(HELLO, IOUtils.toString(is, StandardCharsets.UTF_8));
        }
        assertTrue("file still exists", testFile.isFile());
        assertEquals(0, connector.getConfig().getActiveConnections());
    }

    @Test
    public void binaryShoulWorkWithAllBytesStreaming() throws Exception {
        byte[] written = createContent(2024);
        FtpClientConnector connector = connectorFactory.createConnector(TransferMode.Binary, true);

        fileManager.createBinaryFile(new File(fileManager.getDirectory(), "test.bin"), written);
        try (InputStream is = (InputStream) connector.getFile("", "test.bin", true)) {
            assertNotNull("input stream", is);
            byte[] read = IOUtils.toByteArray(is);
            assertArrayEquals(written, read);
        }
        assertEquals(0, connector.getConfig().getActiveConnections());
    }

    @Test
    public void binaryShoulWorkWithAllBytesInMemory() throws Exception {
        byte[] written = createContent(2024);
        FtpClientConnector connector = connectorFactory.createConnector(TransferMode.Binary, true);

        fileManager.createBinaryFile(new File(fileManager.getDirectory(), "test.bin"), written);
        byte[] read = (byte[]) connector.getFile("", "test.bin", false);
        assertArrayEquals(written, read);
        assertEquals(0, connector.getConfig().getActiveConnections());
    }

    @Test
    public void binaryShouldWorkWithAllBytesActiveMode() throws Exception {
        byte[] written = createContent(2024);
        FtpClientConnector connector = connectorFactory.createConnector(TransferMode.Binary, false);

        fileManager.createBinaryFile(new File(fileManager.getDirectory(), "test.bin"), written);
        try (InputStream is = (InputStream) connector.getFile("", "test.bin", true)) {
            assertNotNull("input stream", is);
            byte[] read = IOUtils.toByteArray(is);
            assertArrayEquals(written, read);
        }
        assertEquals(0, connector.getConfig().getActiveConnections());
    }

    @Test
    public void directoryTraversalTests() throws Exception {
        // These tests have to be in ONE method because we want to reuse the
        // connector with its state: The current directory
        FtpClientConnector connector = connectorFactory.createConnector(TransferMode.Binary, true);

        checkGetFromSubDir(connector, "");
        checkGetFromSubDir(connector, "sub-dir");
        checkGetFromSubDir(connector, "sub-dir/sub-sub-dir");
        checkGetFromSubDir(connector, "sub-dir/sub-dir-sibling");
        checkGetFromSubDir(connector, "other-dir");
        checkGetFromSubDir(connector, "");
    }

    private void checkGetFromSubDir(FtpClientConnector connector, String directory) throws Exception {
        final String CONTENT_BASE = "content-";
        long sequence = System.currentTimeMillis();
        String content = CONTENT_BASE + sequence;

        File baseDir = fileManager.getDirectory();
        fileManager.createTextFile(new File(new File(baseDir, directory), "test.txt"), content);
        byte[] result = (byte[]) connector.getFile(directory, "test.txt", false);
        assertArrayEquals(content.getBytes(StandardCharsets.UTF_8), result);
        assertEquals(0, connector.getConfig().getActiveConnections());
    }
}
