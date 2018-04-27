/*
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 *
 */
package org.mule.modules.ftpclient.sftpserver;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.PublicKey;
import java.util.Arrays;

import org.apache.sshd.common.NamedFactory;
import org.apache.sshd.common.PropertyResolverUtils;
import org.apache.sshd.common.channel.Channel;
import org.apache.sshd.common.compression.BuiltinCompressions;
import org.apache.sshd.common.compression.Compression;
import org.apache.sshd.common.mac.BuiltinMacs;
import org.apache.sshd.common.mac.Mac;
import org.apache.sshd.common.util.security.SecurityUtils;
import org.apache.sshd.server.Command;
import org.apache.sshd.server.ServerFactoryManager;
import org.apache.sshd.server.SshServer;
import org.apache.sshd.server.auth.password.PasswordAuthenticator;
import org.apache.sshd.server.auth.pubkey.PublickeyAuthenticator;
import org.apache.sshd.server.channel.ChannelSessionFactory;
import org.apache.sshd.server.keyprovider.AbstractGeneratorHostKeyProvider;
import org.apache.sshd.server.keyprovider.SimpleGeneratorHostKeyProvider;
import org.apache.sshd.server.scp.ScpCommandFactory;
import org.apache.sshd.server.session.ServerSession;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * SFTP Server
 *
 * @author Guillermo Grandes / guillermo.grandes[at]gmail.com
 * @see https://github.com/ggrandes/sftpserver
 */
public class SftpServer implements PasswordAuthenticator, PublickeyAuthenticator {
    public static final String HOSTKEY_FILE_PEM = "keys/hostkey.pem";
    public static final String HOSTKEY_FILE_SER = "keys/hostkey.ser";

    static final Logger LOG = LoggerFactory.getLogger(SftpServer.class);
    private SftpConfig config;
    private SshServer sshd;

    public static void main(final String[] args) throws Exception {
        final String SSH_KEY = "ssh-rsa AAAAB3NzaC1yc2EAAAABJQAAAQEAuYeVa/27CdNXWqTErrv8rREKMf+CkQ4gffaQcXFaA+8" //
                + "QOfsDLsPEJXOUQd9Tkn1gB6YX+IRUpPYh7AkSYTR6FOKZVy9dnLClk9MWGIsezS/ThnvErz8TCYZVo3" //
                + "XO2fYlhKddAX7NYP4IyB9e9eoIKnZx6+7XUxsr+/RFRCAk4VAs46EwjSCZkeJsh1vLeef9NIqIXpWMo" //
                + "QjmViCCD9R+Y1+rfn+fQGMohDoCYkLB6duQ8y1wwq4x7Z28k1+68O6rN90zLzg/TlNQ61948xKDffqAU" //
                + "9HISompA2AxYaDyhgU/H3WN55hLMkGadp+C6DRJ0xrVJWDsahIWRLYbDrHXkw==";
        SftpConfig c = new SftpConfig();
        Path keyPath = Paths.get(SftpServer.class.getResource("/server-key.ppk").toURI());
        c.setHostKeyPath(keyPath);
        c.addUser(new SftpUser("pwd-user", new File("C:/ftp").toPath(), true).setPassword("secret"));
        c.addUser(new SftpUser("key-user", new File("C:/ftp").toPath(), true).addKey(SSH_KEY));
        c.setPort(2222);
        SftpServer server = new SftpServer(c);
        server.start();
        System.out.println("Sftp server started");
        Thread.sleep(10 * 3600 * 1000L);
        System.out.println("10 hours are enough...");
        server.stop();
    }

    public SftpServer(SftpConfig config) {
        this.config = config;
    }

    public void start() {
        LOG.info("Starting");
        sshd = SshServer.setUpDefaultServer();
        LOG.info("SSHD " + sshd.getVersion());
        hackVersion();
        setupFactories();
        setupKeyPair();
        setupScp();
        setupAuth();
        setupDummyShell();

        try {
            final int port = config.getPort();
            if (config.isCompress()) {
                setupCompress();
            }
            sshd.setPort(port);
            LOG.info("Listen on port=" + port);
            final SftpServer thisServer = this;
            Runtime.getRuntime().addShutdownHook(new Thread() {
                @Override
                public void run() {
                    thisServer.stop();
                }
            });
            sshd.start();
        } catch (Exception e) {
            LOG.error("Exception " + e.toString(), e);
        }
    }

    public void stop() {
        LOG.info("Stopping");
        try {
            sshd.stop();
        } catch (IOException e) {
            try {
                sshd.stop(true);
            } catch (IOException ee) {
                LOG.error("Failed to stop", ee);
            }
        }
    }

    protected void setupFactories() {
        sshd.setSubsystemFactories(Arrays.<NamedFactory<Command>> asList(new CustomSftpSubsystemFactory()));
        sshd.setMacFactories(Arrays.<NamedFactory<Mac>> asList( //
                BuiltinMacs.hmacsha512, //
                BuiltinMacs.hmacsha256, //
                BuiltinMacs.hmacsha1));
        sshd.setChannelFactories(Arrays.<NamedFactory<Channel>> asList(ChannelSessionFactory.INSTANCE));
    }

    protected void setupDummyShell() {
        sshd.setShellFactory(new SecureShellFactory());
    }

    protected void setupKeyPair() {
        Path path = config.getHostKeyPath();
        if (path != null && Files.isReadable(path)) {
            AbstractGeneratorHostKeyProvider hostKeyProvider = SecurityUtils.createGeneratorHostKeyProvider(path);
            sshd.setKeyPairProvider(hostKeyProvider);
        } else {
            if (SecurityUtils.isBouncyCastleRegistered()) {
                sshd.setKeyPairProvider(
                        SecurityUtils.createGeneratorHostKeyProvider(new File(HOSTKEY_FILE_PEM).toPath()));
            } else {
                sshd.setKeyPairProvider(new SimpleGeneratorHostKeyProvider(new File(HOSTKEY_FILE_SER)));
            }
        }
    }

    protected void setupScp() {
        sshd.setCommandFactory(new ScpCommandFactory());
        sshd.setFileSystemFactory(new SecureFileSystemFactory(config));
        sshd.setTcpipForwardingFilter(null);
        sshd.setAgentFactory(null);
    }

    protected void setupAuth() {
        sshd.setPasswordAuthenticator(this);
        sshd.setPublickeyAuthenticator(this);
        sshd.setGSSAuthenticator(null);
    }

    protected void setupCompress() {
        // Compression is not enabled by default
        // You need download and compile:
        // http://www.jcraft.com/jzlib/
        sshd.setCompressionFactories(Arrays.<NamedFactory<Compression>> asList( //
                BuiltinCompressions.none, //
                BuiltinCompressions.zlib, //
                BuiltinCompressions.delayedZlib));
    }

    private void hackVersion() {
        PropertyResolverUtils.updateProperty(sshd, ServerFactoryManager.SERVER_IDENTIFICATION, "SSHD");
    }

    @Override
    public boolean authenticate(final String username, final String password,
            @SuppressWarnings("unused") final ServerSession session) {
        LOG.info("Request auth (Password) for username=" + username);
        if ((username != null) && (password != null)) {
            return config.checkUserPassword(username, password);
        }
        return false;
    }

    @Override
    public boolean authenticate(final String username, final PublicKey key,
            @SuppressWarnings("unused") final ServerSession session) {
        LOG.info("Request auth (PublicKey) for username=" + username);
        if ((username != null) && (key != null)) {
            return config.checkUserPublicKey(username, key);
        }
        return false;
    }
}
