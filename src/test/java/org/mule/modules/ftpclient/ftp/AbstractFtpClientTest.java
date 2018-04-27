package org.mule.modules.ftpclient.ftp;

import org.junit.After;
import org.junit.Before;
import org.mule.modules.ftpclient.ftpserver.SimpleFtpServer;
import org.mule.modules.ftpclient.util.AbstractClientTest;

public class AbstractFtpClientTest extends AbstractClientTest {
    protected static final String ADMIN_USER = "admin";
    protected static final String ADMIN_PASSWORD = "secret";
    protected SimpleFtpServer ftpServer;
    protected FtpConnectorFactory connectorFactory;
    protected int maxIdleTimeSeconds = 300;
    protected int port;

    @Override
    @Before
    public void prepare() throws Exception {
        super.prepare();
        startFtpServer();
        port = ftpServer.getPort();
        connectorFactory = new FtpConnectorFactory("localhost", ftpServer.getPort(), ADMIN_USER, ADMIN_PASSWORD);
    }

    @Override
    @After
    public void cleanup() throws Exception {
        connectorFactory.close();
        connectorFactory = null;
        stopFtpServer();
        super.cleanup();
    }

    public void startFtpServer() throws Exception {
        ftpServer = new SimpleFtpServer(ADMIN_USER, ADMIN_PASSWORD, fileManager.getDirectory(), port,
                maxIdleTimeSeconds);
        ftpServer.startServer();
    }

    public void stopFtpServer() {
        if (ftpServer != null) {
            ftpServer.stopServer();
            ftpServer = null;
        }
    }
}
