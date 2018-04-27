package org.mule.modules.ftpclient;

import java.util.Date;

public class RemoteFile {
    private FtpFileType type;
    private final String name;
    private final long size;
    private final Date timestamp;

    public RemoteFile(FtpFileType type, String name, long size, Date timestamp) {
        this.type = type;
        this.name = name;
        this.size = size;
        this.timestamp = timestamp == null ? null : (Date) timestamp.clone();
    }

    /**
     * @return file type.
     */
    public FtpFileType getType() {
        return type;
    }

    /**
     * @return file name.
     */
    public String getName() {
        return name;
    }

    /**
     * @return file size in bytes
     */
    public long getSize() {
        return size;
    }

    /**
     * @return timestamp, may be <code>null</code> if not known
     */
    public Date getTimestamp() {
        return timestamp == null ? null : (Date) timestamp.clone();
    }

    /**
     *
     */
    @Override
    public String toString() {
        return "RemoteFile [name=" + name + ", type=" + type + ", size=" + size + ", timestamp=" + timestamp + "]";
    }
}
