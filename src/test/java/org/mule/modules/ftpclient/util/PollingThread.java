package org.mule.modules.ftpclient.util;

import org.mule.api.callback.SourceCallback;
import org.mule.modules.ftpclient.FtpClientConnector;

public class PollingThread extends Thread {
    private enum ArchiveMode {
        NONE {
            @Override
            public void runPoll(PollingThread pt) throws Exception {
                pt.connector.poll(pt.directory, pt.filename, pt.filenameTranslatorExpression, pt.deleteAfterGet,
                        pt.streaming, pt.callback);
            }
        },
        DIRECTORY {
            @Override
            public void runPoll(PollingThread pt) throws Exception {
                pt.connector.pollWithArchivingByMovingToDirectory(pt.directory, pt.filename,
                        pt.filenameTranslatorExpression, pt.deleteAfterGet, pt.moveToDirectory, pt.streaming,
                        pt.callback);
            }
        },
        RENAME {
            @Override
            public void runPoll(PollingThread pt) throws Exception {
                pt.connector.pollWithArchivingByRenaming(pt.directory, pt.filename, pt.filenameTranslatorExpression,
                        pt.filenameExpression, pt.originalFilenameExpression, pt.streaming, pt.callback);
            }
        };

        public abstract void runPoll(PollingThread pt) throws Exception;
    }

    private ArchiveMode archiveMode;
    private FtpClientConnector connector;
    private long pollingPeriod;
    private String directory;
    private String filename;
    private String filenameTranslatorExpression;
    private boolean deleteAfterGet;
    private String moveToDirectory;
    private boolean streaming;
    private SourceCallback callback;
    private String filenameExpression;
    private String originalFilenameExpression;

    private PollingThread(FtpClientConnector connector, long pollingPeriod, String directory, String filename,
            String filenameTranslatorExpression) {
        this.connector = connector;
        this.pollingPeriod = pollingPeriod;
        this.directory = directory;
        this.filename = filename;
        this.filenameTranslatorExpression = filenameTranslatorExpression;
    }

    public PollingThread(FtpClientConnector connector, long pollingPeriod, String directory, String filename,
            String filenameTranslatorExpression, boolean deleteAfterGet, boolean streaming, SourceCallback callback) {
        this(connector, pollingPeriod, directory, filename, filenameTranslatorExpression);
        archiveMode = ArchiveMode.NONE;

        this.deleteAfterGet = deleteAfterGet;
        this.streaming = streaming;
        this.callback = callback;
        setDaemon(true);
    }

    public PollingThread(FtpClientConnector connector, long pollingPeriod, String directory, String filename,
            String filenameTranslatorExpression, boolean deleteAfterGet, String moveToDirectory, boolean streaming,
            SourceCallback callback) {
        this(connector, pollingPeriod, directory, filename, filenameTranslatorExpression);
        archiveMode = ArchiveMode.DIRECTORY;

        this.deleteAfterGet = deleteAfterGet;
        this.moveToDirectory = moveToDirectory;
        this.streaming = streaming;
        this.callback = callback;
        setDaemon(true);
    }

    public PollingThread(FtpClientConnector connector, long pollingPeriod, String directory, String filename,
            String filenameTranslatorExpression, String filenameExpression, String originalFilenameExpression,
            boolean streaming, SourceCallback callback) {
        this(connector, pollingPeriod, directory, filename, filenameTranslatorExpression);
        archiveMode = ArchiveMode.RENAME;

        this.filenameExpression = filenameExpression;
        this.originalFilenameExpression = originalFilenameExpression;
        this.streaming = streaming;
        this.callback = callback;
    }

    @Override
    public void run() {
        try {
            while (true) {
                archiveMode.runPoll(this);
                Thread.sleep(pollingPeriod);
            }
        } catch (Exception e) {
            throw new RuntimeException("Error", e);
        }
    }
}
