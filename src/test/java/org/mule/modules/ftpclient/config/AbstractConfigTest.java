package org.mule.modules.ftpclient.config;

import static org.junit.Assert.*;

import org.junit.Test;

public class AbstractConfigTest {

    @Test(expected = IllegalArgumentException.class)
    public void testCreateAbsolutePathBothEmpty() {
        AbstractConfig.createCompletePath("", "");
    }

    @Test(expected = IllegalArgumentException.class)
    public void testCreateAbsolutePathFilenameEmpty() {
        AbstractConfig.createCompletePath("dir", "");
    }

    @Test
    public void testCreateAbsolutePathFileOnly() {
        assertEquals("file", AbstractConfig.createCompletePath("", "file"));
    }

    @Test
    public void testCreateAbsolutePathFileOnlyWithSlash() {
        assertEquals("file", AbstractConfig.createCompletePath("", "/file"));
    }

    @Test
    public void testCreateAbsolutePathDirWithSlashFileWithSlash() {
        assertEquals("/dir/file", AbstractConfig.createCompletePath("/dir", "/file"));
    }

    @Test
    public void testCreateAbsolutePathDirWithoutSlashFileWithSlash() {
        assertEquals("dir/file", AbstractConfig.createCompletePath("dir", "/file"));
    }

    @Test
    public void testCreateAbsolutePathDirWithSlashFileWithoutSlash() {
        assertEquals("/dir/file", AbstractConfig.createCompletePath("/dir", "file"));
    }

    @Test
    public void testCreateAbsolutePathDirWithSlashesFileWithSlash() {
        assertEquals("/dir/file", AbstractConfig.createCompletePath("/dir/", "/file"));
    }

}
