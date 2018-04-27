package org.mule.modules.ftpclient.sftp;

import static org.junit.Assert.*;

import java.io.File;

import org.junit.Test;
import org.mule.api.ConnectionException;
import org.mule.api.ConnectionExceptionCode;
import org.mule.modules.ftpclient.FtpClientConnector;
import org.mule.modules.ftpclient.config.SftpConfig;

public class SftpConnectionTest extends AbstractSftpClientTest {

    @Test
    public void configTestConnectUserPassword() throws Exception {
        FtpClientConnector connector = connectorFactory.createPwdUserConnector(PWD_USER_NAME, PWD_USER_PWD);
        SftpConfig config = connector.getConfig();
        assertEquals(PWD_USER_NAME, config.getUser());
        assertEquals(PWD_USER_PWD, config.getPassword());
        assertEquals(knownHostsFile, config.getKnownHostsFile());

        config.testConnect(PWD_USER_NAME);
    }

    @Test
    public void configTestConnectPrivateKeyFromFile() throws Exception {
        FtpClientConnector connector = connectorFactory.createPrivateKeyFromFileConnector(KEY_USER_NAME, identityFile,
                KEY_USER_PASSPRHASE);
        SftpConfig config = connector.getConfig();
        assertEquals(identityFile, config.getIdentityFile());
        assertEquals(KEY_USER_PASSPRHASE, config.getPassphrase());
        assertEquals(knownHostsFile, config.getKnownHostsFile());

        config.testConnect(KEY_USER_NAME);
    }

    @Test(expected = ConnectionException.class)
    public void configTestConnectPrivateKeyFromFileMissingPassphrase() throws Exception {
        FtpClientConnector connector = connectorFactory.createPrivateKeyFromFileConnector(KEY_USER_NAME, identityFile,
                "");
        SftpConfig config = connector.getConfig();
        assertEquals(identityFile, config.getIdentityFile());
        assertEquals("", config.getPassphrase());
        assertEquals(knownHostsFile, config.getKnownHostsFile());

        config.testConnect(KEY_USER_NAME);
    }

    @Test
    public void configTestConnectPrivateKeyFromResource() throws Exception {
        FtpClientConnector connector = connectorFactory.createPrivateKeyFromResourceConnector(KEY_USER_NAME,
                identityResource, KEY_USER_PASSPRHASE);
        SftpConfig config = connector.getConfig();
        assertEquals(identityResource, config.getIdentityResource());
        assertEquals(KEY_USER_PASSPRHASE, config.getPassphrase());
        assertEquals(knownHostsFile, config.getKnownHostsFile());

        config.testConnect(KEY_USER_NAME);
    }

    @Test(expected = ConnectionException.class)
    public void configTestConnectPrivateKeyFromBadResource() throws Exception {
        FtpClientConnector connector = connectorFactory.createPrivateKeyFromResourceConnector(KEY_USER_NAME, "foo.ppk",
                KEY_USER_PASSPRHASE);
        SftpConfig config = connector.getConfig();
        config.testConnect(KEY_USER_NAME);
    }

    @Test
    public void configTestConnectBadHost() throws Exception {
        try (SftpConnectorFactory f = new SftpConnectorFactory("foo.baz", port, knownHostsFile)) {
            FtpClientConnector connector = f.createPwdUserConnector(PWD_USER_NAME, PWD_USER_PWD);
            SftpConfig config = connector.getConfig();
            try {
                config.testConnect(PWD_USER_NAME);
                fail("ConnectionException missing");
            } catch (ConnectionException e) {
                assertEquals(ConnectionExceptionCode.UNKNOWN_HOST, e.getCode());
            }
        }
    }

    @Test
    public void configTestConnectBadCredentials() throws Exception {
        FtpClientConnector connector = connectorFactory.createPwdUserConnector(PWD_USER_NAME, PWD_USER_PWD);
        SftpConfig config = connector.getConfig();
        try {
            config.testConnect("foo");
            fail("ConnectionException missing");
        } catch (ConnectionException e) {
            assertEquals(ConnectionExceptionCode.INCORRECT_CREDENTIALS, e.getCode());
        }
    }

    @Test
    public void configConnectBadCredentials() throws Exception {
        try (SftpConnectorFactory f = new SftpConnectorFactory("localhost", port, knownHostsFile)) {
            FtpClientConnector connector = f.createPwdUserConnector("foo", "baz");
            try {
                connector.getFile("", "test.txt", false);
                fail("ConnectionException missing");
            } catch (ConnectionException e) {
                assertEquals(ConnectionExceptionCode.INCORRECT_CREDENTIALS, e.getCode());
            }
        }
    }

    @Test
    public void configTestConnectPrivateKeyBadPassphrase() throws Exception {
        FtpClientConnector connector = connectorFactory.createPrivateKeyFromFileConnector(KEY_USER_NAME, identityFile,
                KEY_USER_PASSPRHASE + "Hugo");
        SftpConfig config = connector.getConfig();
        try {
            config.testConnect(KEY_USER_NAME);
            fail("ConnectionException missing");
        } catch (ConnectionException e) {
            assertEquals(ConnectionExceptionCode.INCORRECT_CREDENTIALS, e.getCode());
        }
    }

    @Test
    public void configDisconnect() throws Exception {
        FtpClientConnector connector = connectorFactory.createPwdUserConnector(PWD_USER_NAME, PWD_USER_PWD);
        SftpConfig config = connector.getConfig();
        assertTrue(config.isConnected());
        config.disconnect();
        assertFalse(config.isConnected());
    }

    @Test
    public void deleteTest() throws Exception {
        FtpClientConnector connector = connectorFactory.createPwdUserConnector(PWD_USER_NAME, PWD_USER_PWD);
        File file = new File(pwdUserDir, "test.bin");
        fileManager.createBinaryFile(file, new byte[10]);
        assertTrue(file.isFile());
        connector.delete("", "test.bin");
        assertFalse(file.isFile());
    }

}
