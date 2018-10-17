package org.mule.modules.ftpclient.config;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.List;
import java.util.function.Consumer;

import org.mule.api.annotations.Configurable;
import org.mule.api.annotations.ConnectionIdentifier;
import org.mule.api.annotations.Disconnect;
import org.mule.api.annotations.ValidateConnection;
import org.mule.api.annotations.display.Placement;
import org.mule.api.annotations.param.Default;
import org.mule.modules.ftpclient.ClientWrapper;
import org.mule.modules.ftpclient.RemoteFile;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public abstract class AbstractConfig {

    // Not static, should reflect the non abstract class inheriting from this.
    protected final Logger LOGGER = LoggerFactory.getLogger(getClass());

    @Configurable
    @Placement(order = 1, group = "Server")
    protected String host;

    @Configurable
    @Placement(order = 2, group = "Server")
    @Default("21")
    protected int port;

    protected String user;

    protected ClientWrapper clientWrapper;

    private static final Consumer<ClientWrapper> DO_NOTHING_CONSUMER = new Consumer<ClientWrapper>() {
        @Override
        public void accept(@SuppressWarnings("unused") ClientWrapper cw) {
            // do nothing
        }
    };

    @ConnectionIdentifier
    public String connectionId() {
        return user;
    }

    @Disconnect
    public void disconnect() {
        LOGGER.debug("disconnect, host={}, port={}, user={}", host, port, user);
        if (clientWrapper != null) {
            try {
                clientWrapper.destroy();
            } catch (Exception e) {
                LOGGER.debug("ignore exception on cleanup", e);
            }
        }
        clientWrapper = null;
        user = null;
    }

    @ValidateConnection
    public boolean isConnected() {
        boolean result = clientWrapper != null && clientWrapper.validate();
        LOGGER.debug("isConnected(host={}, port={}, user={}) -> {}", host, port, user, result);
        return result;
    }

    /**
     * @param directory
     *            Directory on remote system, will be created when does not
     *            exist.
     * @param filename
     *            File name on remote system.
     * 
     * @return Stream to which content for the remote file has to be written.
     * 
     * @throws Exception
     *             Anything wrong in low level ftp/sftp libraries.
     */
    public OutputStream getOutputStream(String directory, String filename) throws Exception {
        return clientWrapper.getOutputStream(directory, filename);
    }

    /**
     * @param directory
     *            Directory on remote system.
     * @param filename
     *            File name on remote system.
     * @param onClose
     *            Callback with close action.
     * @return Stream with content of file.
     * 
     * @throws Exception
     *             Anything wrong in low level ftp/sftp libraries.
     */
    public InputStream getInputStream(String directory, String filename, Consumer<ClientWrapper> onClose)
            throws Exception {
        return clientWrapper.getInputStream(directory, filename, onClose == null ? DO_NOTHING_CONSUMER : onClose);
    }

    /**
     * Delete a remote file.
     * 
     * @param directory
     *            Directory on remote system.
     * @param filename
     *            File name on remote system.
     * @throws IOException
     *            When communication with server fails. 
     */
    public void delete(String directory, String filename) throws IOException {
        clientWrapper.delete(directory, filename);
    }

    public List<RemoteFile> list(String directory) throws Exception {
        return clientWrapper.list(directory);
    }

    public void rename(String originalDirectory, String originalFilename, String newFilename) throws Exception {
        clientWrapper.changeWorkingDirectory(originalDirectory, false);
        String toCompletePath = ClientWrapper.normalize(newFilename);
        clientWrapper.move(originalFilename, toCompletePath);
    }

    public static String createCompletePath(String directory, String filename) {
        String normalizedDirectory = ClientWrapper.normalize(directory);
        String normalizedFilename = ClientWrapper.normalize(filename);
        if (normalizedFilename.isEmpty()) {
            throw new IllegalArgumentException("filename is empty");
        }

        StringBuilder sb = new StringBuilder(normalizedDirectory.length() + normalizedFilename.length() + 3);
        if (directory.trim().startsWith("/")) {
            sb.append('/');
        }
        sb.append(normalizedDirectory);
        if (sb.length() > 0 && sb.charAt(sb.length() - 1) != '/') {
            sb.append("/");
        }
        sb.append(normalizedFilename);

        return sb.toString();
    }

    public String getHost() {
        return host;
    }

    public void setHost(String host) {
        this.host = host;
    }

    public int getPort() {
        return port;
    }

    public void setPort(int port) {
        this.port = port;
    }

    public String getUser() {
        return user;
    }
}
