package org.mule.modules.ftpclient;

import java.io.IOException;
import java.util.Collection;
import java.util.HashSet;
import java.util.function.Consumer;

import org.mule.api.MuleMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Strategy for deleting processed files (or to do nothing).
 */
public class DeleteOrNothingStrategy implements CompletionStrategy {
    private static Logger logger = LoggerFactory.getLogger(DeleteOrNothingStrategy.class);

    private boolean deleteAfterGet;

    public DeleteOrNothingStrategy(boolean deleteAfterGet) {
        this.deleteAfterGet = deleteAfterGet;
    }

    @Override
    public Consumer<ClientWrapper> createCompletionHandler(@SuppressWarnings("unused") MuleMessage message,
            String filename, String translatedName) {
        Consumer<ClientWrapper> onClose;

        if (deleteAfterGet) {
            onClose = new FileDeleter(filename, translatedName);
        } else {
            onClose = new FileDeleter();
        }

        return onClose;
    }

    private static class FileDeleter implements Consumer<ClientWrapper> {
        private Collection<String> fileSet;

        public FileDeleter(String... files) {
            fileSet = new HashSet<>();
            for (String file : files) {
                fileSet.add(file);
            }
        }

        @Override
        public void accept(ClientWrapper wrapper) {
            String directory = wrapper.getCurrentDirectory();
            for (String file : fileSet) {
                try {
                    wrapper.delete(directory, file);
                } catch (IOException e) {
                    logger.warn("Could not delete file " + directory + "/" + file, e);
                }
            }
        }
    }
}
