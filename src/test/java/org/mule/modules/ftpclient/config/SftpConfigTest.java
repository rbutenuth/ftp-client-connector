package org.mule.modules.ftpclient.config;

import org.junit.Test;
import org.mule.api.ConnectionException;

public class SftpConfigTest {

    @Test(expected = ConnectionException.class)
    public void testConnectWithFileAndResourceFails() throws Exception {
        SftpConfig config = new SftpConfig();
        config.setIdentityFile("file.ppk");
        config.setIdentityResource("some-resource.ppk");
        config.connect("Scott");
    }

    @Test(expected = ConnectionException.class)
    public void testTestConnectWithFileAndResourceFails() throws Exception {
        SftpConfig config = new SftpConfig();
        config.setIdentityFile("file.ppk");
        config.setIdentityResource("some-resource.ppk");
        config.testConnect("Scott");
    }

}
