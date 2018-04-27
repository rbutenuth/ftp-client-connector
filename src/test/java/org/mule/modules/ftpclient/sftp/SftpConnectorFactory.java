package org.mule.modules.ftpclient.sftp;

import java.io.Closeable;
import java.util.ArrayList;
import java.util.List;

import org.mule.api.ConnectionException;
import org.mule.modules.ftpclient.FtpClientConnector;
import org.mule.modules.ftpclient.config.SftpConfig;

public class SftpConnectorFactory implements Closeable {
    private String host;
    private String knownHostsFile;
    private int port;
    private List<FtpClientConnector> created;

    public SftpConnectorFactory(String host, int port, String knownHostsFile) {
        this.host = host;
        this.port = port;
        this.knownHostsFile = knownHostsFile;
        created = new ArrayList<>();
    }

    @Override
    public void close() {
        for (FtpClientConnector fcc : created) {
            SftpConfig config = fcc.getConfig();
            config.disconnect();
            fcc.setConfig(null);
        }
        created = null;
    }

    public FtpClientConnector createPwdUserConnector(String user, String pwd) throws ConnectionException {
        SftpConfig config = new SftpConfig();
        config.setHost(host);
        config.setPort(port);
        config.setKnownHostsFile(knownHostsFile);
        config.setPassword(pwd);
        config.connect(user);

        return createConnector(config);
    }

    public FtpClientConnector createPrivateKeyFromFileConnector(String user, String identityFile, String passphrase)
            throws ConnectionException {
        SftpConfig config = new SftpConfig();
        config.setHost(host);
        config.setPort(port);
        config.setKnownHostsFile(knownHostsFile);
        config.setIdentityFile(identityFile);
        config.setPassphrase(passphrase);
        config.connect(user);

        return createConnector(config);
    }

    public FtpClientConnector createPrivateKeyFromResourceConnector(String user, String identityResource,
            String passphrase) throws ConnectionException {
        SftpConfig config = new SftpConfig();
        config.setHost(host);
        config.setPort(port);
        config.setKnownHostsFile(knownHostsFile);
        config.setIdentityResource(identityResource);
        config.setPassphrase(passphrase);
        config.connect(user);

        return createConnector(config);
    }

    private FtpClientConnector createConnector(SftpConfig config) {
        FtpClientConnector connector = new FtpClientConnector();
        connector.setConfig(config);
        created.add(connector);

        return connector;
    }
}
