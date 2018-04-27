package org.mule.modules.ftpclient.sftpserver;

import org.apache.sshd.common.Factory;
import org.apache.sshd.server.Command;

public class SecureShellFactory implements Factory<Command> {
    @Override
    public Command get() {
        return create();
    }

    @Override
    public Command create() {
        return new SecureShellCommand();
    }
}