package org.mule.modules.ftpclient.ftp;

import static org.junit.Assert.*;

import java.io.File;
import java.io.IOException;

import org.junit.Test;
import org.mule.api.ConnectionException;
import org.mule.api.ConnectionExceptionCode;
import org.mule.modules.ftpclient.FtpClientConnector;
import org.mule.modules.ftpclient.config.FtpConfig;
import org.mule.modules.ftpclient.config.TransferMode;

public class FtpConnectionTest extends AbstractFtpClientTest {

    @Test
    public void configTestConnect() throws Exception {
        FtpClientConnector connector = connectorFactory.createConnector(TransferMode.Binary, true);
        FtpConfig config = connector.getConfig();
        assertTrue(config.isPassiveMode());
        assertEquals(TransferMode.Binary, config.getTransferMode());
        config.setPassword(ADMIN_PASSWORD);
        config.testConnect(ADMIN_USER);
        assertEquals("localhost", config.getHost());
        assertEquals(port, config.getPort());
        assertEquals(ADMIN_USER, config.getUser());
        assertEquals(ADMIN_PASSWORD, config.getPassword());
        assertEquals(ADMIN_USER, config.connectionId());
        config.disconnect();
        assertEquals(0, config.getActiveConnections());
    }

    @Test
    public void configTestConnectBadHost() throws Exception {
        try (FtpConnectorFactory f = new FtpConnectorFactory("foo.baz", ftpServer.getPort(), ADMIN_USER,
                ADMIN_PASSWORD)) {
            FtpClientConnector connector = f.createConnector(TransferMode.Binary, true);
            FtpConfig config = connector.getConfig();
            config.setPassword(ADMIN_PASSWORD);
            try {
                config.testConnect(ADMIN_USER);
                fail("ConnectionException missing");
            } catch (ConnectionException e) {
                assertEquals(ConnectionExceptionCode.UNKNOWN_HOST, e.getCode());
            }
        }
    }

    @Test
    public void configConnectBadHost() throws Exception {
        try (FtpConnectorFactory f = new FtpConnectorFactory("foo.baz", ftpServer.getPort(), ADMIN_USER,
                ADMIN_PASSWORD)) {
            FtpClientConnector connector = f.createConnector(TransferMode.Binary, true);
            try {
                connector.getFile("", "test.txt", false);
                fail("ConnectionException missing");
            } catch (ConnectionException e) {
                assertEquals(ConnectionExceptionCode.UNKNOWN_HOST, e.getCode());
            }
        }
    }

    @Test
    public void configTestConnectBadCredentials() throws Exception {
        FtpClientConnector connector = connectorFactory.createConnector(TransferMode.Binary, true);
        FtpConfig config = connector.getConfig();
        config.setPassword("bar");
        try {
            config.testConnect("foo");
            fail("ConnectionException missing");
        } catch (ConnectionException e) {
            assertEquals(ConnectionExceptionCode.INCORRECT_CREDENTIALS, e.getCode());
        }
    }

    @Test
    public void configConnectBadCredentials() throws Exception {
        try (FtpConnectorFactory f = new FtpConnectorFactory("localhost", ftpServer.getPort(), "foo", "baz")) {
            FtpClientConnector connector = f.createConnector(TransferMode.Binary, true);
            try {
                connector.getFile("", "test.txt", false);
                fail("ConnectionException missing");
            } catch (ConnectionException e) {
                assertEquals(ConnectionExceptionCode.INCORRECT_CREDENTIALS, e.getCode());
            }
        }
    }

    @Test
    public void configDisconnect() throws Exception {
        FtpClientConnector connector = connectorFactory.createConnector(TransferMode.Binary, true);
        FtpConfig config = connector.getConfig();
        assertTrue(config.isConnected());
        config.disconnect();
        assertFalse(config.isConnected());
    }

    @Test
    public void deleteTest() throws Exception {
        FtpClientConnector connector = connectorFactory.createConnector(TransferMode.Binary, true);
        File file = new File(fileManager.getDirectory(), "test.bin");
        fileManager.createBinaryFile(file, new byte[10]);
        assertTrue(file.isFile());
        connector.delete("", "test.bin");
        assertFalse(file.isFile());
    }

    @Test(expected = IOException.class)
    public void deleteNonExistingDirectory() throws Exception {
        FtpClientConnector connector = connectorFactory.createConnector(TransferMode.Binary, true);
        connector.delete("", "this-file-does-not-exist");
    }

}
