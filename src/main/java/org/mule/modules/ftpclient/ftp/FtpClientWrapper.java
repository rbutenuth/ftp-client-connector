package org.mule.modules.ftpclient.ftp;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.function.Consumer;

import org.apache.commons.net.ftp.FTPClient;
import org.apache.commons.net.ftp.FTPFile;
import org.apache.commons.net.ftp.FTPReply;
import org.apache.log4j.Logger;
import org.mule.model.streaming.CallbackOutputStream;
import org.mule.modules.ftpclient.AutoCloseOnEOFInputStream;
import org.mule.modules.ftpclient.AutoCloseOnEOFInputStream.ConsumerWithIOException;
import org.mule.modules.ftpclient.ClientWrapper;
import org.mule.modules.ftpclient.FtpFileType;
import org.mule.modules.ftpclient.RemoteFile;

public class FtpClientWrapper extends ClientWrapper {
    private static Logger logger = Logger.getLogger(FtpClientWrapper.class);

    private FTPClient client;

    public FtpClientWrapper(FTPClient client) {
        this.client = client;
    }

    @Override
    public void destroy() throws IOException {
        if (client.isConnected()) {
            client.logout();
            client.disconnect();
        }
    }

    @Override
    public boolean validate() {
        try {
            int reply = client.noop();
            if (!FTPReply.isPositiveCompletion(reply)) {
                return false;
            }
        } catch (IOException e) {
            return false;
        }
        return true;
    }

    @Override
    public OutputStream getOutputStream(String directory, String filename) throws IOException {
        changeWorkingDirectory(directory, true);
        try {
            OutputStream out;
            out = openUnwrappedOutputStream(filename);

            return new CallbackOutputStream(out, new CallbackOutputStream.Callback() {
                boolean closed = false;

                @Override
                public void onClose() {
                    if (!closed) {
                        completeCommandAndReturnClient();
                        closed = true;
                    }
                }
            });
        } catch (IOException | RuntimeException e) {
            throw invalidate(e);
        }
    }

    private OutputStream openUnwrappedOutputStream(String filename) throws IOException {
        OutputStream out = client.storeFileStream(filename);
        if (out == null) {
            throw new IOException("FTP operation failed: " + client.getReplyString());
        }
        return out;
    }

    @Override
    public InputStream getInputStream(String directory, String filename, final Consumer<ClientWrapper> onClose)
            throws IOException {
        changeWorkingDirectory(directory, false);
        try {
            final InputStream is = openUnwrappedInputStream(filename);

            return new AutoCloseOnEOFInputStream(is, new ConsumerWithIOException() {
                @Override
                public void apply() throws IOException {
                    if (!client.completePendingCommand()) {
                        logger.info("Could not complete command after get");
                        onClose.accept(FtpClientWrapper.this);
                        invalidate();
                    } else {
                        onClose.accept(FtpClientWrapper.this);
                    }
                }
            });
        } catch (IOException | RuntimeException e) {
            throw invalidate(e);
        }
    }

    private InputStream openUnwrappedInputStream(String filename) throws IOException {
        InputStream is = client.retrieveFileStream(filename);
        if (is == null) {
            throw new IOException("FTP operation failed: " + client.getReplyString());
        }
        return is;
    }

    @Override
    public void delete(String directory, String filename) throws IOException {
        changeWorkingDirectory(directory, false);
        try {
            if (!client.deleteFile(filename)) {
                throw new IOException("Could not delete " + directory + "/" + filename);
            }
        } catch (IOException | RuntimeException e) {
            throw invalidate(e);
        }
    }

    @Override
    public void move(String fromCompletePath, String toCompletePath) throws IOException {
        if (!client.rename(fromCompletePath, toCompletePath)) {
            throw new IOException("Could not move from " + fromCompletePath + " to " + toCompletePath);
        }
    }

    @Override
    public List<RemoteFile> list(String directory) throws IOException {
        List<RemoteFile> fileList = new ArrayList<>();
        changeWorkingDirectory(directory, false);
        try {
            FTPFile[] files = client.listFiles();
            for (FTPFile file : files) {
                if (file != null) {
                    FtpFileType type;
                    switch (file.getType()) {
                    case FTPFile.FILE_TYPE:
                        type = FtpFileType.FILE;
                        break;
                    case FTPFile.DIRECTORY_TYPE:
                        type = FtpFileType.DIRECTORY;
                        break;
                    case FTPFile.SYMBOLIC_LINK_TYPE:
                        type = FtpFileType.SYMBOLIC_LINK;
                        break;
                    case FTPFile.UNKNOWN_TYPE:
                    default:
                        type = FtpFileType.UNKNOWN;
                    }
                    Calendar cal = file.getTimestamp();
                    Date timestamp = cal == null ? null : cal.getTime();
                    fileList.add(new RemoteFile(type, file.getName(), file.getSize(), timestamp));
                }
            }
        } catch (IOException | RuntimeException e) {
            throw invalidate(e);
        }
        return fileList;
    }

    @Override
    protected void changeToAbsoluteDirectory(String directory, boolean create) throws IOException {
        try {
            if (!client.changeWorkingDirectory(directory)) {
                if (create) {
                    if (!client.changeWorkingDirectory("/")) {
                        throw new IOException("Could not change to directory " + directory);
                    }
                    for (String d : split(directory)) {
                        changeToChildDirectory(d, true);
                    }
                } else {
                    throw invalidate(new IOException("Could not change to directory " + directory));
                }
            }
        } catch (IOException e) {
            throw invalidate(e);
        }
    }

    @Override
    protected void changeToParentDirectory() throws IOException {
        try {
            if (!client.changeToParentDirectory()) {
                throw invalidate(new IOException("Could not change to parent directory"));
            }
        } catch (IOException e) {
            throw invalidate(e);
        }
    }

    @Override
    protected void changeToChildDirectory(String name, boolean create) throws IOException {
        try {
            if (!client.changeWorkingDirectory(name)) {
                if (create) {
                    createDirectory(name);
                    changeToChildDirectory(name, false);
                } else {
                    throw invalidate(new IOException("Could not change to directory " + name));
                }
            }
        } catch (IOException e) {
            throw invalidate(e);
        }
    }

    @Override
    protected void createDirectory(String name) throws IOException {
        if (!client.makeDirectory(name)) {
            throw new IOException("Could not create directory " + name);
        }
    }

    private void completeCommandAndReturnClient() {
        try {
            if (!client.completePendingCommand()) {
                invalidate();
            }
        } catch (IOException e) {
            logger.error("ignore exception on cleanup", e);
        }
    }

    private void invalidate() {
        try {
            destroy();
        } catch (Exception e) {
            logger.error("ignore exception on cleanup", e);
        }
    }

    private IOException invalidate(Exception ex) {
        invalidate();
        if (ex instanceof IOException) {
            return (IOException) ex;
        } else {
            return new IOException(ex);
        }
    }
}
