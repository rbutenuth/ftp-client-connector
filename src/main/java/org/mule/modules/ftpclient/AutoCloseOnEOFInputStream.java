package org.mule.modules.ftpclient;

import java.io.IOException;
import java.io.InputStream;

import org.apache.commons.io.IOUtils;

/**
 * Delegates to a wrapped {@link InputStream} and closes the delegate
 * automatically when end of stream is reached.
 */
public class AutoCloseOnEOFInputStream extends InputStream {

    @FunctionalInterface
    public interface ConsumerWithIOException {
        /**
         * Applies this method to the given argument.
         * @throws IOException When delegate throws.
         */
        void apply() throws IOException;
    }

    private InputStream delegate;
    private ConsumerWithIOException onClose;
    private boolean closed;

    public AutoCloseOnEOFInputStream(InputStream delegate, ConsumerWithIOException onClose) {
        this.delegate = delegate;
        this.onClose = onClose;
        closed = false;
    }

    @Override
    public int available() throws IOException {
        return delegate.available();
    }

    @Override
    public synchronized void mark(int readlimit) {
        delegate.mark(readlimit);
    }

    @Override
    public boolean markSupported() {
        return delegate.markSupported();
    }

    @Override
    public synchronized void reset() throws IOException {
        delegate.reset();
    }

    @Override
    public long skip(long n) throws IOException {
        return delegate.skip(n);
    }

    @Override
    public int read() throws IOException {
        return closeIfEndReached(delegate.read());
    }

    @Override
    public int read(byte[] b) throws IOException {
        return closeIfEndReached(delegate.read(b));
    }

    @Override
    public int read(byte[] b, int off, int len) throws IOException {
        return closeIfEndReached(delegate.read(b, off, len));
    }

    @Override
    public synchronized void close() throws IOException {
        if (!closed) {
            closed = true;
            try {
                delegate.close();
            } finally {
                onClose.apply();
            }
        }
    }

    @Override
    public void finalize() throws Throwable {
        // Better close it here than leaving it open forever.
        IOUtils.closeQuietly(this);
        super.finalize();
    }

    public boolean isClosed() {
        return closed;
    }

    private int closeIfEndReached(int readCount) throws IOException {
        if (readCount == -1) {
            close();
        }
        return readCount;
    }
}
