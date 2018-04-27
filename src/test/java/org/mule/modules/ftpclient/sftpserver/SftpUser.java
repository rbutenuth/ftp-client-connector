package org.mule.modules.ftpclient.sftpserver;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collection;

/**
 * sftpserver.user.test.userpassword=secret
 * 
 * # PublicKeys for user (OpenSSH format) sftpserver.user.test.userkey.1=ssh-rsa
 * AAAAB3NzaC1yc2EAAAADA...E7uQ== sftpserver.user.test.userkey.2=ssh-dss
 * AAAAB3NzaC1kc3MAAACBA...IwtA==
 * 
 * # Set user home directory (chrooted)
 * sftpserver.user.test.homedirectory=C:/ftp
 * 
 * # Enable write (default: false) sftpserver.user.test.writepermission=true
 *
 */
public class SftpUser {
    private String name;
    private Path homedirectory;
    private boolean writepermission;

    private String password;
    private Collection<String> keys;

    public SftpUser(String name, Path homedirectory, boolean writepermission) {
        this.name = name;
        this.homedirectory = homedirectory;
        this.writepermission = writepermission;
        keys = new ArrayList<>();
    }

    public String getName() {
        return name;
    }

    public Path getHomedirectory() {
        return homedirectory;
    }

    public boolean isWritepermission() {
        return writepermission;
    }

    public SftpUser setPassword(String password) {
        this.password = password;
        return this;
    }

    public String getPassword() {
        return password;
    }

    public SftpUser addKey(String key) {
        keys.add(key);
        return this;
    }

    public Collection<String> getKeys() {
        return new ArrayList<>(keys);
    }
}
