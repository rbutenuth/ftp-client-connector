package org.mule.modules.ftpclient.ftp;

import static org.junit.Assert.*;

import java.io.File;
import java.io.IOException;

import org.junit.Test;
import org.mule.modules.ftpclient.FtpClientConnector;
import org.mule.modules.ftpclient.config.TransferMode;

public class FtpRenameTest extends AbstractFtpClientTest {

    @Test
    public void renameInRoot() throws Exception {
        final String HELLO = "Hello, world!";
        FtpClientConnector connector = connectorFactory.createConnector(TransferMode.Ascii, true);

        fileManager.createTextFile(new File(fileManager.getDirectory(), "foo.txt"), HELLO);
        connector.rename("", "foo.txt", "bar.txt");

        String content = fileManager.readTextFile(new File(fileManager.getDirectory(), "bar.txt"));
        assertEquals(HELLO, content);
    }

    @Test(expected = IOException.class)
    public void renameShouldFailIntoNonexistingDir() throws Exception {
        final String HELLO = "Hello, world!";
        FtpClientConnector connector = connectorFactory.createConnector(TransferMode.Ascii, true);

        fileManager.createTextFile(new File(fileManager.getDirectory(), "foo.txt"), HELLO);
        connector.rename("", "foo.txt", "/nonexisting/bar.txt");
    }

    @Test
    public void renameIntoSubdir() throws Exception {
        final String HELLO = "Hello, world!";
        FtpClientConnector connector = connectorFactory.createConnector(TransferMode.Ascii, true);

        fileManager.createTextFile(new File(fileManager.getDirectory(), "foo.txt"), HELLO);
        File subdir = new File(fileManager.getDirectory(), "subdir");
        assertTrue(subdir.mkdirs());
        connector.rename("", "foo.txt", "subdir/bar.txt");

        String content = fileManager.readTextFile(new File(subdir, "bar.txt"));
        assertEquals(HELLO, content);
    }
}
