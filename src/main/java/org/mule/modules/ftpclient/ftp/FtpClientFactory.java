package org.mule.modules.ftpclient.ftp;

import java.io.IOException;
import java.net.UnknownHostException;

import org.apache.commons.net.ftp.FTP;
import org.apache.commons.net.ftp.FTPClient;
import org.apache.commons.net.ftp.FTPReply;
import org.mule.api.ConnectionException;
import org.mule.api.ConnectionExceptionCode;
import org.mule.modules.ftpclient.ClientFactory;
import org.mule.modules.ftpclient.config.TransferMode;

/**
 * A factory for Apache {@link FTPClient}s. A new factory is only usable when
 * the pool is set.
 */
public class FtpClientFactory extends ClientFactory<FtpClientWrapper> {
    private String user;
    private String password;
    private boolean passiveMode;
    private TransferMode transferMode;
    private int port;
    private String host;

    public FtpClientFactory(String host, int port, TransferMode transferMode, boolean passiveMode, String user,
            String password) {
        this.host = host;
        this.port = port;
        this.transferMode = transferMode;
        this.passiveMode = passiveMode;
        this.user = user;
        this.password = password;
    }

    @Override
    public FtpClientWrapper makeObject() throws Exception {
        FTPClient client = new FTPClient();
        boolean ready = false;
        try {
            try {
                client.connect(host, port);
            } catch (UnknownHostException e) {
                throw new ConnectionException(ConnectionExceptionCode.UNKNOWN_HOST, "", "Unknown host: " + host);
            } catch (IOException e) {
                throw new ConnectionException(ConnectionExceptionCode.CANNOT_REACH, "",
                        "Could not connect to ftp server: " + port + "@" + host);
            }
            int reply = client.getReplyCode();
            if (!FTPReply.isPositiveCompletion(reply)) {
                throw new ConnectionException(ConnectionExceptionCode.CANNOT_REACH, "",
                        "Could not connect to ftp server: " + port + "@" + host);
            }
            boolean success;
            try {
                success = client.login(user, password);
            } catch (IOException e) {
                throw new ConnectionException(ConnectionExceptionCode.INCORRECT_CREDENTIALS, "",
                        "Could not login to ftp server with user " + user);
            }
            if (!success) {
                throw new ConnectionException(ConnectionExceptionCode.INCORRECT_CREDENTIALS, "",
                        "Could not login to ftp server with user " + user);
            }
            switch (transferMode) {
            case Ascii:
                client.setFileType(FTP.ASCII_FILE_TYPE);
                break;
            case Binary:
            default:
                client.setFileType(FTP.BINARY_FILE_TYPE);
                break;
            }
            if (passiveMode) {
                client.enterLocalPassiveMode();
            } else {
                client.enterLocalActiveMode();
            }
            ready = true;
        } catch (IOException e) {
            throw new ConnectionException(ConnectionExceptionCode.UNKNOWN, e.getMessage(),
                    "Could not login to ftp server", e);
        } finally {
            if (!ready && client.isConnected()) {
                client.disconnect();
            }
        }

        return new FtpClientWrapper(getPool(), client);
    }
}
