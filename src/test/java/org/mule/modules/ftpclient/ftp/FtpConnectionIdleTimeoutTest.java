package org.mule.modules.ftpclient.ftp;

import static org.junit.Assert.*;

import java.io.File;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

import org.apache.commons.io.IOUtils;
import org.junit.Test;
import org.mule.modules.ftpclient.FtpClientConnector;
import org.mule.modules.ftpclient.config.TransferMode;

public class FtpConnectionIdleTimeoutTest extends AbstractFtpClientTest {

    public FtpConnectionIdleTimeoutTest() {
        maxIdleTimeSeconds = 5;
    }

    @Test
    public void simpleGetStream() throws Exception {
        final String HELLO = "Hello, world!";
        FtpClientConnector connector = connectorFactory.createConnector(TransferMode.Ascii, true);

        fileManager.createTextFile(new File(fileManager.getDirectory(), "test.txt"), HELLO);
        try (InputStream is = (InputStream) connector.getFile("", "test.txt", true)) {
            assertNotNull("input stream", is);
            assertEquals(HELLO, IOUtils.toString(is, StandardCharsets.UTF_8));
        }
        Thread.sleep(10_000);
        try (InputStream is = (InputStream) connector.getFile("", "test.txt", true)) {
            assertNotNull("input stream", is);
            assertEquals(HELLO, IOUtils.toString(is, StandardCharsets.UTF_8));
        }

        assertEquals(0, connector.getConfig().getActiveConnections());
    }

}
