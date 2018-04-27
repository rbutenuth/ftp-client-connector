package org.mule.modules.ftpclient.sftp;

import org.apache.commons.lang3.StringUtils;
import org.mule.modules.ftpclient.ClientFactory;

import com.jcraft.jsch.ChannelSftp;
import com.jcraft.jsch.JSch;

public class SftpClientFactory extends ClientFactory<SftpClientWrapper> {
    private JSch jsch;
    private String host;
    private int port;
    private String knownHostsFile;
    private String user;
    private String password;
    private String identityFile;
    private String identityResource;
    private String passphrase;

    public SftpClientFactory(JSch jsch, String host, int port, String knownHostsFile, String user, String password,
            String identityFile, String identityResource, String passphrase) {
        this.jsch = jsch;
        this.host = host;
        this.port = port;
        this.knownHostsFile = knownHostsFile;
        this.user = user;
        this.password = password;
        this.identityFile = identityFile;
        this.identityResource = identityResource;
        this.passphrase = passphrase;
    }

    @Override
    public SftpClientWrapper makeObject() throws Exception {
        ChannelSftp channel;
        if (StringUtils.isNotBlank(identityFile) || StringUtils.isNotBlank(identityResource)) {
            channel = SftpClientWrapper.createChannel(jsch, host, port, knownHostsFile, user, identityFile,
                    identityResource, passphrase);
        } else {
            channel = SftpClientWrapper.createChannel(jsch, host, port, knownHostsFile, user, password);
        }
        channel.connect();
        return new SftpClientWrapper(getPool(), channel);
    }

}
