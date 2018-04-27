package org.mule.modules.ftpclient.sftp;

import static org.junit.Assert.*;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.util.List;

import org.junit.After;
import org.junit.Test;
import org.mule.api.MuleMessage;
import org.mule.api.transport.PropertyScope;
import org.mule.modules.ftpclient.FtpClientConnector;
import org.mule.modules.ftpclient.util.PollingThread;
import org.mule.modules.ftpclient.util.SourceCallbackRecorder;

public class SftpPollTest extends AbstractSftpClientTest {
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
        final String HELLO = "Hello, world!";
        byte[] helloBytes = HELLO.getBytes(StandardCharsets.UTF_8);

        fileManager.createBinaryFile(new File(pwdUserDir, "test.txt"), helloBytes);
        assertTrue(new File(pwdUserDir, "test.txt").isFile());

        FtpClientConnector connector = connectorFactory.createPwdUserConnector(PWD_USER_NAME, PWD_USER_PWD);
        connector.setMuleContext(createMuleContext());

        SourceCallbackRecorder callback = new SourceCallbackRecorder();
        pollingThread = new PollingThread(connector, 100, "", ".*", "", true, false, callback);
        pollingThread.start();
        List<MuleMessage> messages = callback.waitForMessages(1, 10_000);
        assertEquals(1, messages.size());
        checkFile(messages.get(0), helloBytes, "test.txt", "test.txt", helloBytes.length);
        assertDeleted(new File(pwdUserDir, "test.txt"), 5_000);
    }

    @Test
    public void pollForExistingOkFileDelete() throws Exception {
        final String HELLO = "Hello, world!";
        byte[] helloBytes = HELLO.getBytes(StandardCharsets.UTF_8);

        fileManager.createBinaryFile(new File(pwdUserDir, "test.txt"), helloBytes);
        fileManager.createBinaryFile(new File(pwdUserDir, "test.txt.ok"), new byte[0]);

        FtpClientConnector connector = connectorFactory.createPwdUserConnector(PWD_USER_NAME, PWD_USER_PWD);
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
    public void pollForExistingOkFileRename() throws Exception {
        final String HELLO = "Hello, world!";
        byte[] helloBytes = HELLO.getBytes(StandardCharsets.UTF_8);

        File input = new File(pwdUserDir, "test.txt");
        File inputOk = new File(pwdUserDir, "test.txt.ok");
        fileManager.createBinaryFile(input, helloBytes);
        fileManager.createBinaryFile(inputOk, new byte[0]);

        FtpClientConnector connector = connectorFactory.createPwdUserConnector(PWD_USER_NAME, PWD_USER_PWD);
        connector.setMuleContext(createMuleContext());

        SourceCallbackRecorder callback = new SourceCallbackRecorder();
        pollingThread = new PollingThread(connector, 100, "", //
                ".*\\.ok", "#[message.inboundProperties.originalFilename.toString().replace(\".ok\", \"\")]", //
                "#[message.inboundProperties.filename + \".archive\"]", //
                "#[message.inboundProperties.originalFilename + \".archive\"]", false, callback);
        pollingThread.start();
        List<MuleMessage> messages = callback.waitForMessages(1, 10_000);
        assertEquals(1, messages.size());
        checkFile(messages.get(0), helloBytes, "test.txt.ok", "test.txt", helloBytes.length);
        assertDeleted(input, 5_000);
        assertDeleted(inputOk, 5_000);
        checkFile(new File(input.getAbsolutePath() + ".archive"), helloBytes);
        checkFile(new File(inputOk.getAbsolutePath() + ".archive"), new byte[0]);
    }

    @Test
    public void pollForExistingOkFileMoveToDirectory() throws Exception {
        final String HELLO = "Hello, world!";
        byte[] helloBytes = HELLO.getBytes(StandardCharsets.UTF_8);

        File inputDir = new File(pwdUserDir, "input");
        File archiveDir = new File(pwdUserDir, "archive");
        archiveDir.mkdirs();
        fileManager.createBinaryFile(new File(inputDir, "test.txt"), helloBytes);
        fileManager.createBinaryFile(new File(inputDir, "test.txt.ok"), new byte[0]);

        FtpClientConnector connector = connectorFactory.createPwdUserConnector(PWD_USER_NAME, PWD_USER_PWD);
        connector.setMuleContext(createMuleContext());

        SourceCallbackRecorder callback = new SourceCallbackRecorder();
        pollingThread = new PollingThread(connector, 100, "input", //
                ".*\\.ok", "#[message.inboundProperties.originalFilename.toString().replace(\".ok\", \"\")]", //
                false, "../archive", false, callback);
        pollingThread.start();
        List<MuleMessage> messages = callback.waitForMessages(1, 10_000);
        assertEquals(1, messages.size());
        checkFile(messages.get(0), helloBytes, "test.txt.ok", "test.txt", helloBytes.length);
        checkFile(new File(archiveDir, "test.txt"), helloBytes);
        checkFile(new File(archiveDir, "test.txt.ok"), new byte[0]);
    }

    private void checkFile(MuleMessage message, byte[] content, String originalFilename, String filename, long length) {
        assertEquals(4, message.getPropertyNames(PropertyScope.INBOUND).size());
        assertEquals(length, message.getInboundProperty("fileSize"));
        assertEquals(originalFilename, message.getInboundProperty("originalFilename"));
        assertEquals(filename, message.getInboundProperty("filename"));
        assertNotNull(message.getInboundProperty("timestamp"));
        assertArrayEquals(content, (byte[]) message.getPayload());
    }
}
