package org.mule.modules.ftpclient;

import static org.junit.Assert.*;

import java.util.Date;

import org.junit.Test;

public class RemoteFileTest {

    @Test
    public void testRemoteFileWithoutTimestamp() {
        RemoteFile f = new RemoteFile(FtpFileType.FILE, "name", 42L, null);
        assertEquals(FtpFileType.FILE, f.getType());
        assertEquals("name", f.getName());
        assertEquals(42L, f.getSize());
        assertNull(f.getTimestamp());
    }

    @Test
    public void testRemoteFileWithTimestamp() {
        Date date = new Date();
        RemoteFile f = new RemoteFile(FtpFileType.FILE, "name", 42L, date);
        assertEquals(FtpFileType.FILE, f.getType());
        assertEquals("name", f.getName());
        assertEquals(42L, f.getSize());
        assertNotSame(date, f.getTimestamp());
        assertEquals(date, f.getTimestamp());
    }

}
