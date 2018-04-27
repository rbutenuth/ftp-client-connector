package org.mule.modules.ftpclient.sftpserver;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Path;
import java.security.PublicKey;
import java.util.Properties;

public class SftpConfig {
    // Global config
    private static final String BASE = "sftpserver";
    private static final String PROP_GLOBAL = BASE + "." + "global";
    private static final String PROP_PORT = "port";
    private static final String PROP_COMPRESS = "compress";
    private static final String PROP_HOST_KEY_PATH = "hostkeypath";

    // User config
    private static final String PROP_BASE_USERS = BASE + "." + "user";
    private static final String PROP_PWD = "userpassword";
    private static final String PROP_KEY = "userkey" + ".";
    private static final String PROP_HOME = "homedirectory";
    private static final String PROP_ENABLE_WRITE = "writepermission"; // true /
                                                                       // false

    private final Properties db;

    public SftpConfig() {
        db = new Properties();
    }

    public SftpConfig(final Properties db) {
        this.db = db;
    }

    public SftpConfig(String resourceName) throws IOException {
        this(loadFromClasspath(resourceName));
    }

    private static Properties loadFromClasspath(String resourceName) throws IOException {
        final Properties props = new Properties();

        try (InputStream is = SftpConfig.class.getResourceAsStream(resourceName)) {
            if (is == null) {
                throw new IOException("Config file " + resourceName + " not found in classpath");
            } else {
                props.load(is);
            }
        }
        return props;
    }

    // Global config

    public boolean isCompress() {
        return Boolean.parseBoolean(getValue(PROP_COMPRESS));
    }

    public void setCompress(boolean compress) {
        setValue(PROP_COMPRESS, Boolean.toString(compress));
    }

    public int getPort() {
        return Integer.parseInt(getValue(PROP_PORT));
    }

    public void setPort(int port) {
        setValue(PROP_PORT, Integer.toString(port));
    }

    public Path getHostKeyPath() {
        String pathname = getValue(PROP_HOST_KEY_PATH);
        if (pathname != null && pathname.trim().length() > 0) {
            return new File(pathname).toPath();
        } else {
            return null;
        }
    }

    public void setHostKeyPath(Path hostKeyPath) {
        setValue(PROP_HOST_KEY_PATH, hostKeyPath.toAbsolutePath().toString());
    }

    private String getValue(final String key) {
        if (key == null) {
            return null;
        }
        return db.getProperty(PROP_GLOBAL + "." + key);
    }

    private void setValue(String key, String value) {
        if (key == null) {
            throw new NullPointerException("key is null");
        }
        db.setProperty(PROP_GLOBAL + "." + key, value);
    }

    // User config

    public SftpConfig addUser(SftpUser user) {
        setValue(user.getName(), PROP_HOME, user.getHomedirectory().toAbsolutePath().toString());
        if (user.getPassword() != null) {
            setValue(user.getName(), PROP_PWD, user.getPassword());
        }
        setValue(user.getName(), PROP_ENABLE_WRITE, user.isWritepermission());

        int counter = 1;
        for (String key : user.getKeys()) {
            setValue(user.getName(), PROP_KEY + Integer.toString(counter++), key);
        }
        return this;
    }

    private final String getValue(final String user, final String key) {
        if ((user == null) || (key == null)) {
            return null;
        }
        final String value = db.getProperty(PROP_BASE_USERS + "." + user + "." + key);
        return ((value == null) ? null : value.trim());
    }

    final void setValue(final String user, final String key, final Object value) {
        if ((user == null) || (key == null) || (value == null)) {
            return;
        }
        db.setProperty(PROP_BASE_USERS + "." + user + "." + key, String.valueOf(value));
    }

    public boolean isEnabledUser(final String user) {
        final String value = getValue(user, PROP_HOME);
        return value != null && value.trim().length() > 0;
    }

    public boolean checkUserPassword(final String user, final String pwd) {
        final StringBuilder sb = new StringBuilder(96);
        boolean traceInfo = false;
        boolean authOk = false;
        sb.append("Request auth (Password) for username=").append(user).append(" ");
        try {
            if (!isEnabledUser(user)) {
                sb.append("(user disabled)");
                return authOk;
            }
            final String value = getValue(user, PROP_PWD);
            if (value == null) {
                sb.append("(no password)");
                return authOk;
            }
            final boolean isCrypted = PasswordEncrypt.isCrypted(value);
            authOk = isCrypted ? PasswordEncrypt.checkPassword(value, pwd) : value.equals(pwd);
            sb.append(isCrypted ? "(encrypted)" : "(unencrypted)");
            traceInfo = isCrypted;
        } finally {
            sb.append(": ").append(authOk ? "OK" : "FAIL");
            if (authOk) {
                if (traceInfo) {
                    SftpServer.LOG.info(sb.toString());
                } else {
                    SftpServer.LOG.warn(sb.toString());
                }
            } else {
                SftpServer.LOG.error(sb.toString());
            }
        }
        return authOk;
    }

    public boolean checkUserPublicKey(final String user, final PublicKey key) {
        final String encodedKey = PublicKeyHelper.getEncodedPublicKey(key);
        final StringBuilder sb = new StringBuilder(96);
        boolean authOk = false;
        sb.append("Request auth (PublicKey) for username=").append(user);
        sb.append(" (").append(key.getAlgorithm()).append(")");
        try {
            if (!isEnabledUser(user)) {
                sb.append(" (user disabled)");
                return authOk;
            }
            for (int i = 1; i < 1024; i++) {
                final String value = getValue(user, PROP_KEY + i);
                if (value == null) {
                    if (i == 1) {
                        sb.append(" (no publickey)");
                    }
                    break;
                } else if (value.equals(encodedKey)) {
                    authOk = true;
                    break;
                }
            }
        } finally {
            sb.append(": ").append(authOk ? "OK" : "FAIL");
            if (authOk) {
                SftpServer.LOG.info(sb.toString());
            } else {
                SftpServer.LOG.error(sb.toString());
            }
        }
        return authOk;
    }

    public String getHome(final String user) {
        try {
            final File home = new File(getValue(user, PROP_HOME));
            if (home.isDirectory() && home.canRead()) {
                return home.getCanonicalPath();
            }
            return null;
        } catch (IOException e) {
            return null;
        }
    }

    public boolean hasWritePerm(final String user) {
        final String value = getValue(user, PROP_ENABLE_WRITE);
        return Boolean.parseBoolean(value);
    }
}