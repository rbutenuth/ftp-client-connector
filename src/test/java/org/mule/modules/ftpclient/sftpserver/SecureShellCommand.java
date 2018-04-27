package org.mule.modules.ftpclient.sftpserver;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import org.apache.sshd.server.Command;
import org.apache.sshd.server.Environment;
import org.apache.sshd.server.ExitCallback;

public class SecureShellCommand implements Command {
    private OutputStream err = null;
    private ExitCallback callback = null;

    @Override
    public void setInputStream(@SuppressWarnings("unused") final InputStream in) {
        // nothing to do
    }

    @Override
    public void setOutputStream(@SuppressWarnings("unused") final OutputStream out) {
        // nothing to do
    }

    @Override
    public void setErrorStream(final OutputStream err) {
        this.err = err;
    }

    @Override
    public void setExitCallback(final ExitCallback callback) {
        this.callback = callback;
    }

    @Override
    public void start(@SuppressWarnings("unused") final Environment env) throws IOException {
        if (err != null) {
            err.write("shell not allowed\r\n".getBytes("ISO-8859-1"));
            err.flush();
        }
        if (callback != null) {
            callback.onExit(-1, "shell not allowed");
        }
    }

    @Override
    public void destroy() {
        // nothing to do
    }
}