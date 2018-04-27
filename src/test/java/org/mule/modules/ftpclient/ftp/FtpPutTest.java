package org.mule.modules.ftpclient.ftp;

import static org.junit.Assert.*;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;

import org.junit.Test;
import org.mule.api.MuleEvent;
import org.mule.api.transport.OutputHandler;
import org.mule.modules.ftpclient.FtpClientConnector;
import org.mule.modules.ftpclient.config.TransferMode;
import org.mule.modules.ftpclient.util.SimpleMock;

public class FtpPutTest extends AbstractFtpClientTest {

    @Test
    public void simplePutByteArray() throws Exception {
        final String HELLO = "Hello, world!";
        FtpClientConnector connector = connectorFactory.createConnector(TransferMode.Ascii, true);
        connector.putFile("", "test.txt", HELLO.getBytes(StandardCharsets.UTF_8), null);
        String content = fileManager.readTextFile(new File(fileManager.getDirectory(), "test.txt"));
        assertEquals(HELLO, content);
        assertEquals(0, connector.getConfig().getActiveConnections());
    }

    @Test
    public void simplePutStream() throws Exception {
        final String HELLO = "Hello, world!";
        FtpClientConnector connector = connectorFactory.createConnector(TransferMode.Ascii, true);
        connector.putFile("", "test.txt", new ByteArrayInputStream(HELLO.getBytes(StandardCharsets.UTF_8)), null);
        String content = fileManager.readTextFile(new File(fileManager.getDirectory(), "test.txt"));
        assertEquals(HELLO, content);
        assertEquals(0, connector.getConfig().getActiveConnections());
    }

    @Test
    public void simplePutStringBuilder() throws Exception {
        final String HELLO = "Hello, world!";
        FtpClientConnector connector = connectorFactory.createConnector(TransferMode.Ascii, true);
        connector.putFile("", "test.txt", new StringBuilder(HELLO), createEvent());
        String content = fileManager.readTextFile(new File(fileManager.getDirectory(), "test.txt"));
        assertEquals(HELLO, content);
        assertEquals(0, connector.getConfig().getActiveConnections());
    }

    private MuleEvent createEvent() {
        SimpleMock<MuleEvent> mock = new SimpleMock<>(MuleEvent.class);
        mock.storeResult("UTF-8", "getEncoding");
        return mock.getMockObject();
    }

    @Test
    public void simplePutOutputHandler() throws Exception {
        final String HELLO = "Hello, world!";
        FtpClientConnector connector = connectorFactory.createConnector(TransferMode.Ascii, true);
        OutputHandler outputHandler = new OutputHandler() {
            @Override
            public void write(MuleEvent event, OutputStream out) throws IOException {
                assertNull(event);
                byte[] helloBytes = HELLO.getBytes(StandardCharsets.UTF_8);
                out.write(helloBytes);
            }
        };
        connector.putFile("", "test.txt", outputHandler, null);
        assertEquals(0, connector.getConfig().getActiveConnections());
    }

    @Test
    public void asciiOnPutShouldChangeLineEndings() throws Exception {
        // Use Unix and Windows, one should be changed on one of the systems...
        final String HELLO = "Hallöle!\nNext line\r\nAnother One";
        FtpClientConnector connector = connectorFactory.createConnector(TransferMode.Ascii, true);
        connector.putFile("", "test.txt", HELLO.getBytes(StandardCharsets.UTF_8), null);
        String content = fileManager.readTextFile(new File(fileManager.getDirectory(), "test.txt"));
        assertFalse(HELLO.equals(content));
        assertEquals(0, connector.getConfig().getActiveConnections());
    }

    @Test
    public void binaryOnGetShouldKeepLineEndings() throws Exception {
        // Use Unix and Windows, one should be changed on one of the systems...
        final String HELLO = "Hallöle!\nNext line\r\nAnother One";
        FtpClientConnector connector = connectorFactory.createConnector(TransferMode.Binary, true);
        connector.putFile("", "test.txt", HELLO.getBytes(StandardCharsets.UTF_8), null);
        String content = fileManager.readTextFile(new File(fileManager.getDirectory(), "test.txt"));
        assertEquals(HELLO, content);
        assertEquals(0, connector.getConfig().getActiveConnections());
    }

    @Test
    public void binaryShoulWorkWithAllBytesInMemory() throws Exception {
        byte[] written = createContent(2024);
        FtpClientConnector connector = connectorFactory.createConnector(TransferMode.Binary, true);
        connector.putFile("", "test.bin", written, null);
        byte[] read = fileManager.readBinaryFile(new File(fileManager.getDirectory(), "test.bin"));
        assertArrayEquals(written, read);
        assertEquals(0, connector.getConfig().getActiveConnections());
    }

    @Test
    public void binaryShouldWorkWithAllBytesActiveMode() throws Exception {
        byte[] written = createContent(2024);
        FtpClientConnector connector = connectorFactory.createConnector(TransferMode.Binary, true);
        connector.putFile("", "test.bin", new ByteArrayInputStream(written), null);
        byte[] read = fileManager.readBinaryFile(new File(fileManager.getDirectory(), "test.bin"));
        assertArrayEquals(written, read);
        assertEquals(0, connector.getConfig().getActiveConnections());
    }

    @Test
    public void directoryTraversalTests() throws Exception {
        // These tests have to be in ONE method because we want to reuse the
        // connector with its state: The current directory
        FtpClientConnector connector = connectorFactory.createConnector(TransferMode.Binary, true);

        checkPutToSubDir(connector, "");
        checkPutToSubDir(connector, "sub-dir");
        checkPutToSubDir(connector, "sub-dir/sub-sub-dir");
        checkPutToSubDir(connector, "sub-dir/sub-dir-sibling");
        checkPutToSubDir(connector, "other-dir");
        checkPutToSubDir(connector, "");
        checkPutToSubDir(connector, "/absolute/sub-dir");
    }

    private void checkPutToSubDir(FtpClientConnector connector, String directory) throws Exception {
        final String CONTENT_BASE = "content-";
        long sequence = System.currentTimeMillis();
        String content = CONTENT_BASE + sequence;

        File baseDir = fileManager.getDirectory();
        connector.putFile(directory, "test.txt", content.getBytes(StandardCharsets.UTF_8), null);
        String dirWithoutSlash = directory.startsWith("/") ? directory.substring(1) : directory;
        String result = fileManager.readTextFile(new File(new File(baseDir, dirWithoutSlash), "test.txt"));

        assertEquals(content, result);
        assertEquals(0, connector.getConfig().getActiveConnections());
    }

}
