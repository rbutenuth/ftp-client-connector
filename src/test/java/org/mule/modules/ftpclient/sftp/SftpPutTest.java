package org.mule.modules.ftpclient.sftp;

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

public class SftpPutTest extends AbstractSftpClientTest {

    @Test
    public void simplePutByteArrayUserPassword() throws Exception {
        final String HELLO = "Hello, world!";
        FtpClientConnector connector = connectorFactory.createPwdUserConnector(PWD_USER_NAME, PWD_USER_PWD);
        connector.putFile("", "test.txt", HELLO.getBytes(StandardCharsets.UTF_8), null);
        String content = fileManager.readTextFile(new File(pwdUserDir, "test.txt"));
        assertEquals(HELLO, content);
        assertEquals(0, connector.getConfig().getActiveConnections());
    }

    @Test
    public void simplePutByteArrayKeyFile() throws Exception {
        final String HELLO = "Hello, world!";
        FtpClientConnector connector = connectorFactory.createPrivateKeyFromFileConnector(KEY_USER_NAME, identityFile,
                KEY_USER_PASSPRHASE);
        connector.putFile("", "test.txt", HELLO.getBytes(StandardCharsets.UTF_8), null);
        String content = fileManager.readTextFile(new File(keyUserDir, "test.txt"));
        assertEquals(HELLO, content);
        assertEquals(0, connector.getConfig().getActiveConnections());
    }

    @Test
    public void simplePutStream() throws Exception {
        final String HELLO = "Hello, world!";
        FtpClientConnector connector = connectorFactory.createPwdUserConnector(PWD_USER_NAME, PWD_USER_PWD);
        connector.putFile("", "test.txt", new ByteArrayInputStream(HELLO.getBytes(StandardCharsets.UTF_8)), null);
        String content = fileManager.readTextFile(new File(pwdUserDir, "test.txt"));
        assertEquals(HELLO, content);
        assertEquals(0, connector.getConfig().getActiveConnections());
    }

    @Test
    public void simplePutOutputHandler() throws Exception {
        final String HELLO = "Hello, world!";
        FtpClientConnector connector = connectorFactory.createPwdUserConnector(PWD_USER_NAME, PWD_USER_PWD);
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
    public void binaryOnPutShouldKeepLineEndings() throws Exception {
        final String HELLO = "Hallöle!\nNext line\r\nAnother One";
        FtpClientConnector connector = connectorFactory.createPwdUserConnector(PWD_USER_NAME, PWD_USER_PWD);
        connector.putFile("", "test.txt", HELLO.getBytes(StandardCharsets.UTF_8), null);
        String content = fileManager.readTextFile(new File(pwdUserDir, "test.txt"));
        assertEquals(HELLO, content);
        assertEquals(0, connector.getConfig().getActiveConnections());
    }

    @Test
    public void binaryShoulWorkWithAllBytesInMemory() throws Exception {
        byte[] written = createContent(2024);
        FtpClientConnector connector = connectorFactory.createPwdUserConnector(PWD_USER_NAME, PWD_USER_PWD);
        connector.putFile("", "test.bin", written, null);
        byte[] read = fileManager.readBinaryFile(new File(pwdUserDir, "test.bin"));
        assertArrayEquals(written, read);
        assertEquals(0, connector.getConfig().getActiveConnections());
    }

    @Test
    public void binaryShouldWorkWithAllBytesActiveMode() throws Exception {
        byte[] written = createContent(2024);
        FtpClientConnector connector = connectorFactory.createPwdUserConnector(PWD_USER_NAME, PWD_USER_PWD);
        connector.putFile("", "test.bin", new ByteArrayInputStream(written), null);
        byte[] read = fileManager.readBinaryFile(new File(pwdUserDir, "test.bin"));
        assertArrayEquals(written, read);
        assertEquals(0, connector.getConfig().getActiveConnections());
    }

    @Test
    public void directoryTraversalTests() throws Exception {
        // These tests have to be in ONE method because we want to reuse the
        // connector with its state: The current directory
        FtpClientConnector connector = connectorFactory.createPwdUserConnector(PWD_USER_NAME, PWD_USER_PWD);

        checkPutToSubDir(connector, "");
        checkPutToSubDir(connector, "sub-dir");
        checkPutToSubDir(connector, "sub-dir/sub-sub-dir");
        checkPutToSubDir(connector, "sub-dir/sub-dir-sibling");
        checkPutToSubDir(connector, "other-dir");
        checkPutToSubDir(connector, "/absolute-dir");
        checkPutToSubDir(connector, "");
    }

    private void checkPutToSubDir(FtpClientConnector connector, String directory) throws Exception {
        final String CONTENT_BASE = "content-";
        long sequence = System.currentTimeMillis();
        String content = CONTENT_BASE + sequence;

        File baseDir = pwdUserDir;
        connector.putFile(directory, "test.txt", content.getBytes(StandardCharsets.UTF_8), null);
        String result = fileManager.readTextFile(new File(new File(baseDir, directory), "test.txt"));

        assertEquals(content, result);
        assertEquals(0, connector.getConfig().getActiveConnections());
    }

}
