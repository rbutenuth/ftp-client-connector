package org.mule.modules.ftpclient.ftp;

import static org.junit.Assert.*;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;

import org.apache.commons.io.IOUtils;
import org.junit.After;
import org.junit.Test;
import org.mule.api.MuleMessage;
import org.mule.api.transport.PropertyScope;
import org.mule.modules.ftpclient.AutoCloseOnEOFInputStream;
import org.mule.modules.ftpclient.FtpClientConnector;
import org.mule.modules.ftpclient.config.TransferMode;
import org.mule.modules.ftpclient.util.PollingThread;
import org.mule.modules.ftpclient.util.SourceCallbackRecorder;

public class FtpPollTest extends AbstractFtpClientTest {
    private PollingThread pollingThread;

    @Override
    @After
    public void cleanup() throws Exception {
        if (pollingThread != null) {
            pollingThread.interrupt();
            pollingThread.join(1_000);
        }
        super.cleanup();
    }

    @Test
    public void pollForExistingFile() throws Exception {
        pollForExistingFile("");
    }

    @Test
    public void pollForExistingFileExpressionReturnsEmptyString() throws Exception {
        pollForExistingFile("#['']");
    }

    private void pollForExistingFile(String translateExpression) throws Exception {
        final String HELLO = "Hello, world!";
        byte[] helloBytes = HELLO.getBytes(StandardCharsets.UTF_8);

        fileManager.createBinaryFile(new File(fileManager.getDirectory(), "test.txt"), helloBytes);
        assertTrue(new File(fileManager.getDirectory(), "test.txt").isFile());

        FtpClientConnector connector = connectorFactory.createConnector(TransferMode.Binary, true);
        connector.setMuleContext(createMuleContext());

        SourceCallbackRecorder callback = new SourceCallbackRecorder();
        pollingThread = new PollingThread(connector, 100, "", ".*", translateExpression, true, false, callback);
        pollingThread.start();
        List<MuleMessage> messages = callback.waitForMessages(1, 10_000);
        assertEquals(1, messages.size());
        checkFile(messages.get(0), helloBytes, "test.txt", "test.txt", helloBytes.length);
        assertDeleted(new File(fileManager.getDirectory(), "test.txt"), 5_000);
    }

    @Test
    public void pollForExistingFileWithException() throws Exception {
        final String HELLO = "Hello, world!";
        byte[] helloBytes = HELLO.getBytes(StandardCharsets.UTF_8);

        fileManager.createBinaryFile(new File(fileManager.getDirectory(), "test.txt"), helloBytes);
        assertTrue(new File(fileManager.getDirectory(), "test.txt").isFile());

        FtpClientConnector connector = connectorFactory.createConnector(TransferMode.Binary, true);
        connector.setMuleContext(createMuleContext());

        SourceCallbackRecorder callback = new SourceCallbackRecorder(new Exception("Bum!"));
        pollingThread = new PollingThread(connector, 100, "", ".*", "", true, false, callback);
        pollingThread.start();
        List<MuleMessage> messages = callback.waitForMessages(1, 10_000);
        assertEquals(1, messages.size());
        assertTrue(messages.get(0).getPayload() instanceof byte[]);
        assertDeleted(new File(fileManager.getDirectory(), "test.txt"), 5_000);
    }

    @Test
    public void pollForExistingFileDoNotDelete() throws Exception {
        final String HELLO = "Hello, world!";
        byte[] helloBytes = HELLO.getBytes(StandardCharsets.UTF_8);

        fileManager.createBinaryFile(new File(fileManager.getDirectory(), "test.txt"), helloBytes);
        assertTrue(new File(fileManager.getDirectory(), "test.txt").isFile());

        FtpClientConnector connector = connectorFactory.createConnector(TransferMode.Binary, true);
        connector.setMuleContext(createMuleContext());

        SourceCallbackRecorder callback = new SourceCallbackRecorder();
        pollingThread = new PollingThread(connector, 100, "", ".*", "", false, false, callback);
        pollingThread.start();
        List<MuleMessage> messages = callback.waitForMessages(1, 10_000);
        assertEquals(1, messages.size());
        checkFile(messages.get(0), helloBytes, "test.txt", "test.txt", helloBytes.length);
        assertTrue(new File(fileManager.getDirectory(), "test.txt").isFile());
    }

    @Test
    public void pollForExistingFileStreaming() throws Exception {
        final String HELLO = "Hello, world!";
        byte[] helloBytes = HELLO.getBytes(StandardCharsets.UTF_8);

        fileManager.createBinaryFile(new File(fileManager.getDirectory(), "test.txt"), helloBytes);
        assertTrue(new File(fileManager.getDirectory(), "test.txt").isFile());

        FtpClientConnector connector = connectorFactory.createConnector(TransferMode.Binary, true);
        connector.setMuleContext(createMuleContext());

        SourceCallbackRecorder callback = new SourceCallbackRecorder();
        pollingThread = new PollingThread(connector, 100, "", ".*", "", true, true, callback);
        pollingThread.start();
        List<MuleMessage> messages = callback.waitForMessages(1, 10_000);
        assertEquals(1, messages.size());
        checkFile(messages.get(0), helloBytes, "test.txt", "test.txt", helloBytes.length);
        assertDeleted(new File(fileManager.getDirectory(), "test.txt"), 5_000);
    }

    @Test
    public void pollForExistingFileStreamingWithException() throws Exception {
        final String HELLO = "Hello, world!";
        byte[] helloBytes = HELLO.getBytes(StandardCharsets.UTF_8);

        fileManager.createBinaryFile(new File(fileManager.getDirectory(), "test.txt"), helloBytes);
        assertTrue(new File(fileManager.getDirectory(), "test.txt").isFile());

        FtpClientConnector connector = connectorFactory.createConnector(TransferMode.Binary, true);
        connector.setMuleContext(createMuleContext());

        SourceCallbackRecorder callback = new SourceCallbackRecorder(new Exception("Bum!"));
        pollingThread = new PollingThread(connector, 100, "", ".*", "", true, true, callback);
        pollingThread.start();
        List<MuleMessage> messages = callback.waitForMessages(1, 10_000);
        assertEquals(1, messages.size());
        AutoCloseOnEOFInputStream is = (AutoCloseOnEOFInputStream) messages.get(0).getPayload();
        assertTrue(is.isClosed());
        assertDeleted(new File(fileManager.getDirectory(), "test.txt"), 5_000);
    }

    @Test
    public void pollForExistingOkFile() throws Exception {
        final String HELLO = "Hello, world!";
        byte[] helloBytes = HELLO.getBytes(StandardCharsets.UTF_8);

        fileManager.createBinaryFile(new File(fileManager.getDirectory(), "test.txt"), helloBytes);
        fileManager.createBinaryFile(new File(fileManager.getDirectory(), "test.txt.ok"), new byte[0]);

        FtpClientConnector connector = connectorFactory.createConnector(TransferMode.Binary, true);
        connector.setMuleContext(createMuleContext());

        SourceCallbackRecorder callback = new SourceCallbackRecorder();
        pollingThread = new PollingThread(connector, 100, "", //
                ".*\\.ok", "#[message.inboundProperties.originalFilename.toString().replace(\".ok\", \"\")]", //
                true, false, callback);
        pollingThread.start();
        List<MuleMessage> messages = callback.waitForMessages(1, 10_000);
        assertEquals(1, messages.size());
        checkFile(messages.get(0), helloBytes, "test.txt.ok", "test.txt", helloBytes.length);
    }

    @Test
    public void pollOkFileExistsButFileIsMissing() throws Exception {
        fileManager.createBinaryFile(new File(fileManager.getDirectory(), "test.txt.ok"), new byte[0]);

        FtpClientConnector connector = connectorFactory.createConnector(TransferMode.Binary, true);
        connector.setMuleContext(createMuleContext());

        SourceCallbackRecorder callback = new SourceCallbackRecorder();
        pollingThread = new PollingThread(connector, 1000, "", //
                ".*\\.ok", "#[message.inboundProperties.originalFilename.toString().replace(\".ok\", \"\")]", //
                true, false, callback);
        pollingThread.start();
        List<MuleMessage> messages = callback.waitForMessages(1, 10_000);
        assertEquals(0, messages.size());
    }

    @Test
    public void pollDirectoryShouldBeIgnored() throws Exception {
        File base = fileManager.getDirectory();
        fileManager.createBinaryFile(new File(base, "test.txt"), new byte[0]);
        fileManager.createBinaryFile(new File(new File(base, "sub-dir"), "another-file.txt"), new byte[0]);

        FtpClientConnector connector = connectorFactory.createConnector(TransferMode.Binary, true);
        connector.setMuleContext(createMuleContext());

        SourceCallbackRecorder callback = new SourceCallbackRecorder();
        pollingThread = new PollingThread(connector, 1000, "", ".*", "", true, false, callback);
        pollingThread.start();
        List<MuleMessage> messages = callback.waitForMessages(1, 5_000);
        assertEquals(1, messages.size());
    }

    private void checkFile(MuleMessage message, byte[] content, String originalFilename, String filename, long length)
            throws IOException {
        assertEquals(4, message.getPropertyNames(PropertyScope.INBOUND).size());
        assertEquals(length, (long)message.getInboundProperty("fileSize"));
        assertEquals(originalFilename, message.getInboundProperty("originalFilename"));
        assertEquals(filename, message.getInboundProperty("filename"));
        assertNotNull(message.getInboundProperty("timestamp"));
        Object payload = message.getPayload();
        if (payload instanceof InputStream) {
            InputStream is = (InputStream) payload;
            byte[] bytes = IOUtils.toByteArray(is);
            is.close();
            assertArrayEquals(content, bytes);
        } else if (payload instanceof byte[]) {
            assertArrayEquals(content, (byte[]) message.getPayload());
        } else {
            fail("unexpected payload: " + payload);
        }
    }
}
