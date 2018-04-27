package org.mule.modules.ftpclient.config;

import java.io.IOException;
import java.net.UnknownHostException;

import org.apache.commons.net.ftp.FTPClient;
import org.apache.commons.net.ftp.FTPReply;
import org.apache.commons.pool.impl.GenericObjectPool;
import org.mule.api.ConnectionException;
import org.mule.api.ConnectionExceptionCode;
import org.mule.api.annotations.Configurable;
import org.mule.api.annotations.Connect;
import org.mule.api.annotations.TestConnectivity;
import org.mule.api.annotations.components.ConnectionManagement;
import org.mule.api.annotations.display.Password;
import org.mule.api.annotations.display.Placement;
import org.mule.api.annotations.param.ConnectionKey;
import org.mule.api.annotations.param.Default;
import org.mule.modules.ftpclient.ftp.FtpClientFactory;
import org.mule.modules.ftpclient.ftp.FtpClientWrapper;

@ConnectionManagement(configElementName = "ftp-config", friendlyName = "Ftp Configuration")
public class FtpConfig extends AbstractConfig<FtpClientWrapper> {
    @Configurable
    @Password
    @Placement(order = 2, group = "Connection")
    private String password;

    @Configurable
    @Default("Binary")
    @Placement(order = 1, group = "Parameters")
    private TransferMode transferMode = TransferMode.Binary;

    @Configurable
    @Placement(order = 2, group = "Parameters")
    @Default("true")
    private boolean passiveMode = true;

    @SuppressWarnings("unused") // ConnectionException
    @Connect
    public void connect(
            @SuppressWarnings("hiding") @Placement(order = 1, group = "Connection") @ConnectionKey String user)
            throws ConnectionException {
        this.user = user;
        FtpClientFactory factory = new FtpClientFactory(host, port, transferMode, passiveMode, user, password);
        clientPool = new GenericObjectPool<>(factory);
        clientPool.setTestOnBorrow(true);
        factory.setPool(clientPool);
    }

    @TestConnectivity
    public void testConnect(
            @SuppressWarnings("hiding") @Placement(order = 1, group = "Connection") @ConnectionKey String user)
            throws ConnectionException {
        this.user = user;
        FTPClient ftp = new FTPClient();
        try {
            try {
                ftp.connect(host, port);
            } catch (UnknownHostException e) {
                throw new ConnectionException(ConnectionExceptionCode.UNKNOWN_HOST, "", "Unknown host: " + host);
            } catch (IOException e) {
                throw new ConnectionException(ConnectionExceptionCode.CANNOT_REACH, "",
                        "Could not connect to ftp server: " + port + "@" + host);
            }
            int reply = ftp.getReplyCode();
            if (!FTPReply.isPositiveCompletion(reply)) {
                throw new ConnectionException(ConnectionExceptionCode.CANNOT_REACH, "",
                        "Could not connect to ftp server: " + port + "@" + host);
            }
            boolean success;
            try {
                success = ftp.login(user, password);
            } catch (IOException e) {
                throw new ConnectionException(ConnectionExceptionCode.INCORRECT_CREDENTIALS, "",
                        "Could not login to ftp server with user " + user);
            }
            if (!success) {
                throw new ConnectionException(ConnectionExceptionCode.INCORRECT_CREDENTIALS, "",
                        "Could not login to ftp server with user " + user);
            }
            ftp.logout();
        } catch (IOException e) {
            throw new ConnectionException(ConnectionExceptionCode.UNKNOWN, e.getMessage(), "Unknown error");
        } finally {
            if (ftp.isConnected()) {
                try {
                    ftp.disconnect();
                } catch (IOException ioe) {
                    // ignore excpentions on cleanup
                }
            }
        }
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public TransferMode getTransferMode() {
        return transferMode;
    }

    public void setTransferMode(TransferMode transferMode) {
        this.transferMode = transferMode;
    }

    public boolean isPassiveMode() {
        return passiveMode;
    }

    public void setPassiveMode(boolean passiveMode) {
        this.passiveMode = passiveMode;
    }
}