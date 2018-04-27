package org.mule.modules.ftpclient.ftp;

import static org.junit.Assert.*;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;

import org.apache.commons.io.IOUtils;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.mule.api.MuleMessage;
import org.mule.api.transport.PropertyScope;
import org.mule.modules.ftpclient.FtpClientConnector;
import org.mule.modules.ftpclient.config.TransferMode;
import org.mule.modules.ftpclient.util.PollingThread;
import org.mule.modules.ftpclient.util.SourceCallbackRecorder;

public class FtpPollWithArchivingTest extends AbstractFtpClientTest {
    private static final String REMOVE_OK_EXPRESSION = "#[message.inboundProperties.originalFilename.toString().replace(\".ok\", \"\")]";
    private static final String RENAME_FILENAME_EXPRESSION = "#[message.inboundProperties.filename + \".archive\"]";
    private static final String RENAME_ORIGINAL_FILENAME_EXPRESSION = "#[message.inboundProperties.originalFilename + \".archive\"]";
    private static final String TEST_TXT = "test.txt";
    private static final String TEST_TXT_OK = "test.txt.ok";
    private static final String INPUT = "input";
    private static final String ARCHIVE = "archive";
    private static final String HELLO = "Hello, world!";
    private static final byte[] HELLO_BYTES = HELLO.getBytes(StandardCharsets.UTF_8);
    private PollingThread pollingThread;
    private File input;
    private File archive;

    @Override
    @Before
    public void prepare() throws Exception {
        super.prepare();
        input = new File(fileManager.getDirectory(), INPUT);
        archive = new File(fileManager.getDirectory(), ARCHIVE);
        assertTrue(archive.mkdirs());

    }

    @Override
    @After
    public void cleanup() throws Exception {
        if (pollingThread != null) {
            pollingThread.interrupt();
            pollingThread.join(1_000);
        }
        input = null;
        archive = null;
        super.cleanup();
    }

    @Test
    public void pollForExistingFile() throws Exception {
        pollForExistingFile(true, false);
    }

    @Test
    public void pollForExistingFileStreaming() throws Exception {
        pollForExistingFile(true, true);
    }

    @Test
    public void pollForExistingFileDoNotDelete() throws Exception {
        pollForExistingFile(false, false);
    }

    // deleteAfterGet does only change the handling of "ok files"
    private void pollForExistingFile(boolean deleteAfterGet, boolean streaming) throws Exception {
        fileManager.createBinaryFile(new File(input, TEST_TXT), HELLO_BYTES);
        assertTrue(new File(input, TEST_TXT).isFile());

        FtpClientConnector connector = connectorFactory.createConnector(TransferMode.Binary, true);
        connector.setMuleContext(createMuleContext());

        SourceCallbackRecorder callback = new SourceCallbackRecorder();

        pollingThread = new PollingThread(connector, 100, INPUT, ".*", "", deleteAfterGet, "../" + ARCHIVE, streaming,
                callback);
        pollingThread.start();
        List<MuleMessage> messages = callback.waitForMessages(1, 10_000);
        assertEquals(1, messages.size());
        MuleMessage message = messages.get(0);
        checkFile(message, HELLO_BYTES, TEST_TXT, TEST_TXT, HELLO_BYTES.length, streaming);
        assertDeleted(new File(input, TEST_TXT), 5_000);
        checkFile(new File(archive, TEST_TXT), HELLO_BYTES);
    }

    @Test
    public void pollForExistingOkFileDeleteOkFile() throws Exception {
        pollForExistingFileWithOkFile(true);
    }

    @Test
    public void pollForExistingOkFileArchiveOkFile() throws Exception {
        pollForExistingFileWithOkFile(false);
    }

    private void pollForExistingFileWithOkFile(boolean deleteOkFile) throws Exception {
        fileManager.createBinaryFile(new File(input, TEST_TXT), HELLO_BYTES);
        fileManager.createBinaryFile(new File(input, TEST_TXT_OK), new byte[0]);

        FtpClientConnector connector = connectorFactory.createConnector(TransferMode.Binary, true);
        connector.setMuleContext(createMuleContext());

        SourceCallbackRecorder callback = new SourceCallbackRecorder();
        pollingThread = new PollingThread(connector, 100, INPUT, //
                ".*\\.ok", REMOVE_OK_EXPRESSION, //
                deleteOkFile, "../" + ARCHIVE, false, callback);
        pollingThread.start();
        List<MuleMessage> messages = callback.waitForMessages(1, 10_000);
        assertEquals(1, messages.size());
        checkFile(messages.get(0), HELLO_BYTES, TEST_TXT_OK, TEST_TXT, HELLO_BYTES.length, false);
        assertDeleted(new File(input, TEST_TXT), 5_000);
        assertDeleted(new File(input, TEST_TXT_OK), 5_000);
        checkFile(new File(archive, TEST_TXT), HELLO_BYTES);
        if (deleteOkFile) {
            assertDeleted(new File(input, TEST_TXT_OK), 5_000);
            assertDeleted(new File(archive, TEST_TXT_OK), 5_000);
        } else {
            assertDeleted(new File(input, TEST_TXT_OK), 5_000);
            checkFile(new File(archive, TEST_TXT_OK), new byte[0]);
        }
    }

    @Test
    public void pollForExistingOkFileArchiveByRename() throws Exception {
        fileManager.createBinaryFile(new File(input, TEST_TXT), HELLO_BYTES);
        fileManager.createBinaryFile(new File(input, TEST_TXT_OK), new byte[0]);

        FtpClientConnector connector = connectorFactory.createConnector(TransferMode.Binary, true);
        connector.setMuleContext(createMuleContext());

        SourceCallbackRecorder callback = new SourceCallbackRecorder();
        pollingThread = new PollingThread(connector, 100, INPUT, ".*\\.ok", //
                REMOVE_OK_EXPRESSION, RENAME_FILENAME_EXPRESSION, RENAME_ORIGINAL_FILENAME_EXPRESSION, //
                false, callback);
        pollingThread.start();
        List<MuleMessage> messages = callback.waitForMessages(1, 10_000_000);
        assertEquals(1, messages.size());
        checkFile(messages.get(0), HELLO_BYTES, TEST_TXT_OK, TEST_TXT, HELLO_BYTES.length, false);
        assertDeleted(new File(input, TEST_TXT), 5_000);
        assertDeleted(new File(input, TEST_TXT_OK), 5_000);
        checkFile(new File(input, TEST_TXT + ".archive"), HELLO_BYTES);
        checkFile(new File(input, TEST_TXT_OK + ".archive"), new byte[0]);
    }

    private void checkFile(MuleMessage message, byte[] content, String originalFilename, String filename, long length,
            boolean streaming) throws IOException {
        assertEquals(4, message.getPropertyNames(PropertyScope.INBOUND).size());
        System.out.println(message.getPropertyNames(PropertyScope.INBOUND));
        assertEquals(length, message.getInboundProperty("fileSize"));
        assertEquals(originalFilename, message.getInboundProperty("originalFilename"));
        assertEquals(filename, message.getInboundProperty("filename"));
        assertNotNull(message.getInboundProperty("timestamp"));
        Object payload = message.getPayload();
        if (payload instanceof InputStream) {
            assertTrue(streaming);
            InputStream is = (InputStream) payload;
            byte[] bytes = IOUtils.toByteArray(is);
            is.close();
            assertArrayEquals(content, bytes);
        } else if (payload instanceof byte[]) {
            assertFalse(streaming);
            assertArrayEquals(content, (byte[]) message.getPayload());
        } else {
            fail("unexpected payload: " + payload);
        }
    }
}
