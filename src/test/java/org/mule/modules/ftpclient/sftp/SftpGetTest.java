package org.mule.modules.ftpclient.sftp;

import static org.junit.Assert.*;

import java.io.File;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

import org.apache.commons.io.IOUtils;
import org.junit.Test;
import org.mule.modules.ftpclient.FtpClientConnector;

public class SftpGetTest extends AbstractSftpClientTest {

    @Test
    public void simpleGetStreamUserPassword() throws Exception {
        final String HELLO = "Hello, world!";
        FtpClientConnector connector = connectorFactory.createPwdUserConnector(PWD_USER_NAME, PWD_USER_PWD);

        fileManager.createTextFile(new File(pwdUserDir, "test.txt"), HELLO);
        try (InputStream is = (InputStream) connector.getFile("", "test.txt", true)) {
            assertNotNull("input stream", is);
            assertEquals(HELLO, IOUtils.toString(is, StandardCharsets.UTF_8));
        }
    }

    @Test
    public void simpleGetStreamKeyFile() throws Exception {
        final String HELLO = "Hallo Welt!";
        FtpClientConnector connector = connectorFactory.createPrivateKeyFromFileConnector(KEY_USER_NAME, identityFile,
                KEY_USER_PASSPRHASE);

        fileManager.createTextFile(new File(keyUserDir, "test.txt"), HELLO);
        try (InputStream is = (InputStream) connector.getFile("", "test.txt", true)) {
            assertNotNull("input stream", is);
            assertEquals(HELLO, IOUtils.toString(is, StandardCharsets.UTF_8));
        }
    }

    @Test
    public void keepLineEndings() throws Exception {
        // Unix and Windows should be unchanged
        final String HELLO = "Hallöle!\nNext line\r\nAnother One";
        FtpClientConnector connector = connectorFactory.createPwdUserConnector(PWD_USER_NAME, PWD_USER_PWD);

        File testFile = new File(pwdUserDir, "test.txt");
        fileManager.createTextFile(testFile, HELLO);
        try (InputStream is = (InputStream) connector.getFile("", "test.txt", true)) {
            assertNotNull("input stream", is);
            assertEquals(HELLO, IOUtils.toString(is, StandardCharsets.UTF_8));
        }
        assertTrue("file still exists", testFile.isFile());
    }

    @Test
    public void binaryShoulWorkWithAllBytesStreaming() throws Exception {
        byte[] written = createContent(2024);
        FtpClientConnector connector = connectorFactory.createPwdUserConnector(PWD_USER_NAME, PWD_USER_PWD);

        fileManager.createBinaryFile(new File(pwdUserDir, "test.bin"), written);
        try (InputStream is = (InputStream) connector.getFile("", "test.bin", true)) {
            assertNotNull("input stream", is);
            byte[] read = IOUtils.toByteArray(is);
            assertArrayEquals(written, read);
        }
    }

    @Test
    public void binaryShoulWorkWithAllBytesInMemory() throws Exception {
        byte[] written = createContent(2024);
        FtpClientConnector connector = connectorFactory.createPwdUserConnector(PWD_USER_NAME, PWD_USER_PWD);

        fileManager.createBinaryFile(new File(pwdUserDir, "test.bin"), written);
        byte[] read = (byte[]) connector.getFile("", "test.bin", false);
        assertArrayEquals(written, read);
    }

    @Test
    public void directoryTraversalTests() throws Exception {
        // These tests have to be in ONE method because we want to reuse the
        // connector with its state: The current directory
        FtpClientConnector connector = connectorFactory.createPwdUserConnector(PWD_USER_NAME, PWD_USER_PWD);

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

        File baseDir = pwdUserDir;
        fileManager.createTextFile(new File(new File(baseDir, directory), "test.txt"), content);
        byte[] result = (byte[]) connector.getFile(directory, "test.txt", false);
        assertArrayEquals(content.getBytes(StandardCharsets.UTF_8), result);
    }
}
