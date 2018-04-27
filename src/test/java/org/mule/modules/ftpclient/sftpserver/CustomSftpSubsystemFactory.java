package org.mule.modules.ftpclient.sftpserver;

import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.util.Collection;

import org.apache.sshd.common.util.GenericUtils;
import org.apache.sshd.server.Command;
import org.apache.sshd.server.subsystem.sftp.SftpEventListener;
import org.apache.sshd.server.subsystem.sftp.SftpSubsystem;
import org.apache.sshd.server.subsystem.sftp.SftpSubsystemFactory;

public class CustomSftpSubsystemFactory extends SftpSubsystemFactory {
    @Override
    public Command create() {
        final SftpSubsystem subsystem = new SftpSubsystem(getExecutorService(), isShutdownOnExit(),
                getUnsupportedAttributePolicy(), getFileSystemAccessor()) {
            @SuppressWarnings("unused")
            @Override
            protected void setFileAttribute(final Path file, final String view, final String attribute,
                    final Object value, final LinkOption... options) {
                throw new UnsupportedOperationException("setFileAttribute Disabled");
            }

            @SuppressWarnings("unused")
            @Override
            protected void createLink(final int id, final String targetPath, final String linkPath,
                    final boolean symLink) {
                throw new UnsupportedOperationException("createLink Disabled");
            }
        };
        final Collection<? extends SftpEventListener> listeners = getRegisteredListeners();
        if (GenericUtils.size(listeners) > 0) {
            for (final SftpEventListener l : listeners) {
                subsystem.addSftpEventListener(l);
            }
        }
        return subsystem;
    }
}