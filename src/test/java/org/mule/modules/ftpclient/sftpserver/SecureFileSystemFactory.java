package org.mule.modules.ftpclient.sftpserver;

import java.io.IOException;
import java.nio.file.FileSystem;
import java.nio.file.Paths;
import java.util.Collections;

import org.apache.sshd.common.file.FileSystemFactory;
import org.apache.sshd.common.file.root.RootedFileSystemProvider;
import org.apache.sshd.common.session.Session;
import org.mule.modules.ftpclient.sftpserver.readonly.ReadOnlyRootedFileSystemProvider;

public class SecureFileSystemFactory implements FileSystemFactory {
    private final SftpConfig db;

    public SecureFileSystemFactory(final SftpConfig db) {
        this.db = db;
    }

    @Override
    public FileSystem createFileSystem(final Session session) throws IOException {
        final String userName = session.getUsername();
        final String home = db.getHome(userName);
        if (home == null) {
            throw new IOException("user home error");
        }
        final RootedFileSystemProvider rfsp = db.hasWritePerm(userName) ? new RootedFileSystemProvider()
                : new ReadOnlyRootedFileSystemProvider();
        return rfsp.newFileSystem(Paths.get(home), Collections.<String, Object> emptyMap());
    }
}