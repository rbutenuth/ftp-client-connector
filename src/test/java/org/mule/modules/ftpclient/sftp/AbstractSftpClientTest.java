package org.mule.modules.ftpclient.sftp;

import java.io.File;
import java.io.IOException;
import java.net.ServerSocket;
import java.nio.file.Paths;

import org.junit.After;
import org.junit.Before;
import org.mule.modules.ftpclient.sftpserver.SftpConfig;
import org.mule.modules.ftpclient.sftpserver.SftpServer;
import org.mule.modules.ftpclient.sftpserver.SftpUser;
import org.mule.modules.ftpclient.util.AbstractClientTest;

public abstract class AbstractSftpClientTest extends AbstractClientTest {
    protected static final String PWD_USER_NAME = "pwd-user";
    protected static final String PWD_USER_PWD = "secret";
    protected static final String KEY_USER_NAME = "key-user";
    protected static final String KEY_USER_KEY = "ssh-rsa AAAAB3NzaC1yc2EAAAABJQAAAQEAuYeVa/27CdNXWqTErrv8rREKMf+CkQ4gffaQcXFaA+8" //
            + "QOfsDLsPEJXOUQd9Tkn1gB6YX+IRUpPYh7AkSYTR6FOKZVy9dnLClk9MWGIsezS/ThnvErz8TCYZVo3" //
            + "XO2fYlhKddAX7NYP4IyB9e9eoIKnZx6+7XUxsr+/RFRCAk4VAs46EwjSCZkeJsh1vLeef9NIqIXpWMo" //
            + "QjmViCCD9R+Y1+rfn+fQGMohDoCYkLB6duQ8y1wwq4x7Z28k1+68O6rN90zLzg/TlNQ61948xKDffqAU" //
            + "9HISompA2AxYaDyhgU/H3WN55hLMkGadp+C6DRJ0xrVJWDsahIWRLYbDrHXkw==";
    protected static final String KEY_USER_PASSPRHASE = "client";

    protected SftpServer ftpServer;
    protected SftpConnectorFactory connectorFactory;
    protected File pwdUserDir;
    protected File keyUserDir;
    protected String identityFile;
    protected String identityResource;
    protected String knownHostsFile;
    protected int port;

    @Override
    @Before
    public void prepare() throws Exception {
        super.prepare();
        File baseDir = fileManager.getDirectory();
        pwdUserDir = new File(baseDir, "pwd-user-dir");
        pwdUserDir.mkdirs();
        keyUserDir = new File(baseDir, "key-user-dir");
        keyUserDir.mkdirs();
        identityFile = Paths.get(getClass().getResource("/client-key.ppk").toURI()).toString();
        identityResource = "client-key.ppk";
        knownHostsFile = Paths.get(getClass().getResource("/known_hosts").toURI()).toString();
        port = getFreePort();
        SftpConfig c = new SftpConfig();
        c.setHostKeyPath(Paths.get(getClass().getResource("/server-key.ppk").toURI()));
        c.addUser(new SftpUser(PWD_USER_NAME, pwdUserDir.toPath(), true).setPassword(PWD_USER_PWD));
        c.addUser(new SftpUser("key-user", keyUserDir.toPath(), true).addKey(KEY_USER_KEY));
        c.setPort(port);
        ftpServer = new SftpServer(c);
        ftpServer.start();

        connectorFactory = new SftpConnectorFactory("localhost", port, knownHostsFile);
    }

    @Override
    @After
    public void cleanup() throws Exception {
        connectorFactory.close();
        connectorFactory = null;
        ftpServer.stop();
        ftpServer = null;
        pwdUserDir = null;
        keyUserDir = null;
        super.cleanup();
    }

    private int getFreePort() throws IOException {
        try (ServerSocket socket = new ServerSocket(0)) {
            return socket.getLocalPort();
        }
    }
}
