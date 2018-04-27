package org.mule.modules.ftpclient;

import java.util.function.Consumer;

import org.mule.api.MuleMessage;

/**
 * Produce implementations for the "on close" handling for the ftp/sftp streams.
 */
public interface CompletionStrategy {

    public Consumer<ClientWrapper> createCompletionHandler(MuleMessage message, String filename, String translatedName);

}
