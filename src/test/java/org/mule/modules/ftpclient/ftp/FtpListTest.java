package org.mule.modules.ftpclient.ftp;

import static org.junit.Assert.*;

import java.io.File;
import java.io.IOException;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import org.junit.Test;
import org.mule.modules.ftpclient.FtpClientConnector;
import org.mule.modules.ftpclient.FtpFileType;
import org.mule.modules.ftpclient.RemoteFile;
import org.mule.modules.ftpclient.config.TransferMode;

public class FtpListTest extends AbstractFtpClientTest {

    @Test
    public void listTest() throws Exception {
        final int count = 10;
        for (int i = 0; i < count; i++) {
            fileManager.createBinaryFile(new File(fileManager.getDirectory(), filename(i)), new byte[filesize(i)]);
        }
        FtpClientConnector connector = connectorFactory.createConnector(TransferMode.Binary, true);
        Collection<RemoteFile> files = connector.list("");
        Map<String, RemoteFile> fileMap = new HashMap<>();
        for (RemoteFile rf : files) {
            fileMap.put(rf.getName(), rf);
        }
        assertEquals(count, fileMap.size());
        for (int i = 0; i < count; i++) {
            String name = filename(i);
            RemoteFile rf = fileMap.get(name);
            assertNotNull(rf);
            assertEquals(name, rf.getName());
            assertEquals(filesize(i), rf.getSize());
            assertEquals(FtpFileType.FILE, rf.getType());
        }
    }

    @Test(expected = IOException.class)
    public void listNonExistingDirectory() throws Exception {
        FtpClientConnector connector = connectorFactory.createConnector(TransferMode.Binary, true);
        connector.list("this-directory-does-not-exist");
    }

    private String filename(int index) {
        return String.format("file_%02d.txt", index);
    }

    private int filesize(int index) {
        return 10 * index;
    }
}
