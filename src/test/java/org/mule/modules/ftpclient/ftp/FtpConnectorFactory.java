package org.mule.modules.ftpclient.ftp;

import java.io.Closeable;
import java.util.ArrayList;
import java.util.List;

import org.mule.api.ConnectionException;
import org.mule.modules.ftpclient.FtpClientConnector;
import org.mule.modules.ftpclient.config.FtpConfig;
import org.mule.modules.ftpclient.config.TransferMode;

public class FtpConnectorFactory implements Closeable {
    private String host;
    private int port;
    private String user;
    private String password;
    private List<FtpClientConnector> created;

    public FtpConnectorFactory(String host, int port, String user, String password) {
        this.host = host;
        this.port = port;
        this.user = user;
        this.password = password;
        created = new ArrayList<>();
    }

    @Override
    public void close() {
        for (FtpClientConnector fcc : created) {
            FtpConfig config = fcc.getConfig();
            config.disconnect();
            fcc.setConfig(null);
        }
        created = null;
    }

    public FtpClientConnector createConnector() throws ConnectionException {
        return createConnector(TransferMode.Binary, true);
    }

    public FtpClientConnector createConnector(TransferMode mode, boolean passiveMode) throws ConnectionException {
        FtpConfig config = new FtpConfig();
        config.setHost(host);
        config.setPort(port);
        config.setPassword(password);
        config.setPassiveMode(passiveMode);
        config.setTransferMode(mode);
        config.connect(user);
        FtpClientConnector connector = new FtpClientConnector();
        connector.setConfig(config);
        created.add(connector);

        return connector;
    }
}
