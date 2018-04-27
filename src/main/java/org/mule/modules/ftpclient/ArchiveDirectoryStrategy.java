package org.mule.modules.ftpclient;

import java.util.function.Consumer;

import org.mule.api.MuleMessage;

/**
 * Archiver which works by moving to a different directory.
 */
public class ArchiveDirectoryStrategy implements CompletionStrategy {

    private boolean deleteAfterGet;
    private String moveToDirectory;

    public ArchiveDirectoryStrategy(boolean deleteAfterGet, String moveToDirectory) {
        this.deleteAfterGet = deleteAfterGet;
        this.moveToDirectory = moveToDirectory;
    }

    @Override
    public Consumer<ClientWrapper> createCompletionHandler(@SuppressWarnings("unused") MuleMessage message,
            String filename, String translatedName) {
        String to1 = combine(moveToDirectory, translatedName);
        String to2 = deleteAfterGet ? null : combine(moveToDirectory, filename);
        return new FileArchiver(translatedName, to1, filename, to2);

    }

    private String combine(String directory, String file) {
        return directory + "/" + file;
    }
}
