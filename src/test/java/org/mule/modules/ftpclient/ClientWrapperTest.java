package org.mule.modules.ftpclient;

import static org.junit.Assert.*;

import java.io.InputStream;
import java.io.OutputStream;
import java.util.List;
import java.util.function.Consumer;

import org.junit.Test;

public class ClientWrapperTest {

    @Test
    public void testNormalizeNull() {
        assertEquals("", ClientWrapper.normalize(null));
    }

    @Test
    public void testNormalizeEmpty() {
        assertEquals("", ClientWrapper.normalize(""));
    }

    @Test
    public void testNormalizeAlreadyNormalized() {
        assertEquals("foo", ClientWrapper.normalize("foo"));
    }

    @Test
    public void testNormalizeFrontSlash() {
        assertEquals("foo", ClientWrapper.normalize("/foo"));
    }

    @Test
    public void testNormalizeEndSlash() {
        assertEquals("foo", ClientWrapper.normalize("foo/"));
    }

    /**
     * This does not really implement any of the abstract methods, it just helps
     * to test non abstract methods.
     */
    @SuppressWarnings("unused")
    private static class ClientWrapperImpl extends ClientWrapper {

        @Override
        public void destroy() {
            throw new UnsupportedOperationException("destroy");
        }

        @Override
        public boolean validate() {
            throw new UnsupportedOperationException("validate");
        }

        @Override
        public OutputStream getOutputStream(String directory, String filename) {
            throw new UnsupportedOperationException("getOutputStream");
        }

        @Override
        public InputStream getInputStream(String directory, String filename, Consumer<ClientWrapper> onClose) {
            throw new UnsupportedOperationException("getInputStream");
        }

        @Override
        public void delete(String directory, String filename) {
            throw new UnsupportedOperationException("delete");
        }

        @Override
        public void move(String fromFile, String toCompletePath) {
            throw new UnsupportedOperationException("move");
        }

        @Override
        public List<RemoteFile> list(String directory) {
            throw new UnsupportedOperationException("");
        }

        @Override
        protected void changeToAbsoluteDirectory(String aDirectory, boolean aCreate) {
            throw new UnsupportedOperationException("");
        }

        @Override
        protected void changeToParentDirectory() {
            throw new UnsupportedOperationException("list");
        }

        @Override
        protected void changeToChildDirectory(String name, boolean create) {
            throw new UnsupportedOperationException("changeToChildDirectory");
        }

        @Override
        protected void createDirectory(String name) {
            throw new UnsupportedOperationException("createDirectory");
        }
    }
}
