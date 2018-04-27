package org.mule.modules.ftpclient.ftpserver;

import java.io.File;
import java.io.IOException;
import java.net.ServerSocket;

import org.apache.ftpserver.FtpServer;
import org.apache.ftpserver.FtpServerFactory;
import org.apache.ftpserver.filesystem.nativefs.NativeFileSystemFactory;
import org.apache.ftpserver.ftplet.FtpException;
import org.apache.ftpserver.ftplet.UserManager;
import org.apache.ftpserver.listener.ListenerFactory;

/**
 * Wrapper around Apaches ftp server with a simple in memory user management.
 */
public class SimpleFtpServer {
    private String adminUser;
    private String adminPassword;
    private File adminHome;
    private int port;
    private int maxIdleTimeSeconds;
    private FtpServer server;

    public static void main(String[] args) throws Exception {

        SimpleFtpServer server = new SimpleFtpServer("admin", "secret", new File("C:/ftp"), 2221, 300);

        server.startServer();
        System.out.println("server started");
        Thread.sleep(3_600_000 * 10); // 10 hours
        System.out.println("stopping after 10 hours...");
        server.stopServer();
    }

    /**
     * @param adminUser
     *            Name of admin user (per default the only user)
     * @param adminPassword
     *            Password of admin user
     * @param adminHome
     *            Home directory for admin user
     * @param port
     *            TCP port, if 0, server chooses own port.
     * @param maxIdleTimeSeconds
     *            Time after which the connection is close when idle (in
     *            seconds)
     */
    public SimpleFtpServer(String adminUser, String adminPassword, File adminHome, int port, int maxIdleTimeSeconds) {
        this.adminUser = adminUser;
        this.adminPassword = adminPassword;
        this.adminHome = adminHome;
        this.port = port;
        this.maxIdleTimeSeconds = maxIdleTimeSeconds;
    }

    /**
     * When you choose 0 in constructor, number will be changed on
     * {@link #startServer()}.
     */
    public int getPort() {
        return port;
    }

    public void startServer() throws IOException, FtpException {
        if (port == 0) {
            try (ServerSocket socket = new ServerSocket(0)) {
                port = socket.getLocalPort();
            }
        }
        FtpServerFactory serverFactory = new FtpServerFactory();
        ListenerFactory factory = new ListenerFactory();
        factory.setPort(port);

        serverFactory.addListener("default", factory.createListener());

        UserManager userManager = new InMemoryUserManager(adminUser, adminPassword, adminHome, maxIdleTimeSeconds);
        serverFactory.setUserManager(userManager);

        NativeFileSystemFactory fsf = new NativeFileSystemFactory();
        serverFactory.setFileSystem(fsf);

        server = serverFactory.createServer();
        server.start();
    }

    public void stopServer() {
        server.stop();
        server = null;
        port = 0;
    }
}
