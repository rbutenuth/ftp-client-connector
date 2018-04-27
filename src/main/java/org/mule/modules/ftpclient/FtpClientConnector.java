package org.mule.modules.ftpclient;

import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
import java.util.regex.Pattern;

import javax.inject.Inject;

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.mule.DefaultMuleMessage;
import org.mule.api.MessagingException;
import org.mule.api.MuleContext;
import org.mule.api.MuleEvent;
import org.mule.api.MuleMessage;
import org.mule.api.annotations.Config;
import org.mule.api.annotations.Connector;
import org.mule.api.annotations.Processor;
import org.mule.api.annotations.Source;
import org.mule.api.annotations.SourceStrategy;
import org.mule.api.annotations.display.FriendlyName;
import org.mule.api.annotations.param.Default;
import org.mule.api.annotations.param.Literal;
import org.mule.api.callback.SourceCallback;
import org.mule.api.expression.ExpressionManager;
import org.mule.api.transport.OutputHandler;
import org.mule.api.transport.PropertyScope;
import org.mule.modules.ftpclient.config.AbstractConfig;
import org.mule.transport.NullPayload;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Connector(name = "ftp-client", friendlyName = "FTP/SFTP")
public class FtpClientConnector {
    private static Logger logger = LoggerFactory.getLogger(FtpClientConnector.class);

    @Inject
    private MuleContext muleContext;

    @Config
    private AbstractConfig<?> config;

    /**
     * Poll a remote directory. The following inboundProperties will be set:
     * <ul>
     * <li>originalFilename: Filename on the remote system (without path)</li>
     * <li>fileSize: Size in bytes, -1 if unknown.</li>
     * <li>timestamp: The time stamp (java.util.Date) of a file (usually last
     * modified data)</li>
     * <li>filename: Same as originalFilename or modified when a
     * <code>filenameTrnanslator</code> is configured</li>
     * </ul>
     *
     * @param directory
     *            Directory to poll
     * @param filename
     *            Reqular expression to match against found files.
     * @param translatedNameExpression
     *            A MEL expression generating a second filename from the
     *            inboundProperty "filename". When the result is not empty, the
     *            connector will read the second file, not the original one.
     *            When the delete flag is set, both files will be deleted after
     *            processing.
     * @param deleteAfterGet
     *            Delete the file after it has been read (done when InputStream
     *            has been closed).
     * @param streaming
     *            Return an InputStream instead of an byte[]
     * @param callback
     *            Interface for message generation (set by Mule)
     */
    @Source(friendlyName = "Poll a remote directory", primaryNodeOnly = true, sourceStrategy = SourceStrategy.POLLING, pollingPeriod = 6000)
    public void poll(//
            @FriendlyName("Directory") @Default("") String directory, //
            @FriendlyName("Regex for filename") @Default(".*") String filename, //
            @FriendlyName("File to read instead of matched") @Default("") @Literal String translatedNameExpression, //
            @FriendlyName("Delete after get") @Default("false") boolean deleteAfterGet, //
            @FriendlyName("Streaming") @Default("true") boolean streaming, //
            SourceCallback callback) {

        CompletionStrategy cs = new DeleteOrNothingStrategy(deleteAfterGet);
        handlePoll(cs, directory, filename, translatedNameExpression, streaming, callback);
    }

    /**
     * Poll a remote directory. Move files to an archive directory after
     * successful processing. The following inboundProperties will be set:
     * <ul>
     * <li>originalFilename: Filename on the remote system (without path)</li>
     * <li>fileSize: Size in bytes, -1 if unknown.</li>
     * <li>timestamp: The time stamp (java.util.Date) of a file (usually last
     * modified data)</li>
     * <li>filename: Same as originalFilename or modified when a
     * <code>filenameTrnanslator</code> is configured</li>
     * </ul>
     *
     * @param directory
     *            Directory to poll
     * @param filename
     *            Reqular expression to match against found files.
     * @param translatedNameExpression
     *            A MEL expression generating a second filename from the
     *            inboundProperty "filename". When the result is not empty, the
     *            connector will read the second file, not the original one.
     *            When the delete flag is set, both files will be deleted after
     *            processing.
     * @param deleteAfterGet
     *            Delete the original file after it has been read (done when
     *            InputStream has been closed).
     * @param moveToDirectory
     *            Move the original file and the translated file to this
     *            directory after processing.
     * @param streaming
     *            Return an InputStream instead of an byte[]
     * @param callback
     *            Interface for message generation (set by Mule)
     */
    @Source(friendlyName = "Poll with archiving to another directory", primaryNodeOnly = true, sourceStrategy = SourceStrategy.POLLING, pollingPeriod = 6000)
    public void pollWithArchivingByMovingToDirectory(//
            @FriendlyName("Directory") @Default("") String directory, //
            @FriendlyName("Regex for filename") @Default(".*") String filename, //
            @FriendlyName("File to read instead of matched") @Default("") @Literal String translatedNameExpression, //
            @FriendlyName("Delete original file after get") @Default("false") boolean deleteAfterGet, //
            @FriendlyName("Move to Directory") String moveToDirectory, //
            @FriendlyName("Streaming") @Default("true") boolean streaming, //
            SourceCallback callback) {

        String dir = moveToDirectory.trim();
        if (dir.endsWith("/")) {
            dir = dir.substring(0, dir.length() - 1);
        }
        CompletionStrategy cs = new ArchiveDirectoryStrategy(deleteAfterGet, dir);
        handlePoll(cs, directory, filename, translatedNameExpression, streaming, callback);
    }

    /**
     * Poll a remote directory. Archive files by renaming after successful
     * processing. The following inboundProperties will be set:
     * <ul>
     * <li>originalFilename: Filename on the remote system (without path)</li>
     * <li>fileSize: Size in bytes, -1 if unknown.</li>
     * <li>timestamp: The time stamp (java.util.Date) of a file (usually last
     * modified data)</li>
     * <li>filename: Same as originalFilename or modified when a
     * <code>filenameTrnanslator</code> is configured</li>
     * </ul>
     *
     * @param directory
     *            Directory to poll
     * @param filename
     *            Reqular expression to match against found files.
     * @param translatedNameExpression
     *            A MEL expression generating a second filename from the
     *            inboundProperty "filename". When the result is not empty, the
     *            connector will read the second file, not the original one.
     *            When the delete flag is set, both files will be deleted after
     *            processing.
     * @param translatedNameExpression
     *            Expression for renaming the file read.
     * @param filenameExpression
     *            Expression for renaming filename
     * @param originalFilenameExpression
     *            Expression for renaming tho original file name (e.g. with
     *            ".ok" at the end)
     * @param streaming
     *            Return an InputStream instead of an byte[]
     * @param callback
     *            Interface for message generation (set by Mule)
     */
    @Source(friendlyName = "Poll with archiving by renaming", primaryNodeOnly = true, sourceStrategy = SourceStrategy.POLLING, pollingPeriod = 6000)
    public void pollWithArchivingByRenaming(//
            @FriendlyName("Directory") @Default("") String directory, //
            @FriendlyName("Regex for filename") @Default(".*") String filename, //
            @FriendlyName("File to read instead of matched") @Default("") @Literal String translatedNameExpression, //
            @FriendlyName("Expression for renaming filename") @Literal String filenameExpression, //
            @FriendlyName("Expression for renaming originalFilename") @Literal String originalFilenameExpression, //
            @FriendlyName("Streaming") @Default("true") boolean streaming, //
            SourceCallback callback) {

        CompletionStrategy cs = new RenameStrategy(muleContext, filenameExpression, originalFilenameExpression);
        handlePoll(cs, directory, filename, translatedNameExpression, streaming, callback);
    }

    private void handlePoll(CompletionStrategy cs, //
            String directory, //
            String filename, //
            String translatedNameExpression, //
            boolean streaming, //
            SourceCallback callback) {
        Pattern pattern = Pattern.compile(filename);

        Map<String, Long> sizeMap = new HashMap<>();
        List<RemoteFile> filesToHandle = new ArrayList<>();
        List<RemoteFile> allFiles;
        try {
            allFiles = config.list(directory);
        } catch (Exception e) {
            logger.error("failure in polling: list files in  failed", e);
            return;
        }
        if (logger.isDebugEnabled()) {
            logger.debug("Found " + allFiles.size() + " files");
        }
        for (RemoteFile file : allFiles) {
            if (FtpFileType.FILE.equals(file.getType())) {
                sizeMap.put(file.getName(), Long.valueOf(file.getSize()));
                if (pattern.matcher(file.getName()).matches()) {
                    filesToHandle.add(file);
                } else {
                    logger.debug("Skip {} as not matching pattern {}", file, filename);
                }
            } else {
                logger.debug("Skip {} as not filetype {}", file, FtpFileType.FILE);
            }
        }
        for (RemoteFile file : filesToHandle) {
            try {
                handleFile(sizeMap, directory, file, translatedNameExpression, cs, streaming, callback);
            } catch (Exception e) {
                logger.error("failure in polling" + file, e);
                return;
            }
        }
    }

    private void handleFile(Map<String, Long> sizeMap, final String directory, RemoteFile file,
            String translatedNameExpression, CompletionStrategy cs, boolean streaming, SourceCallback callback)
            throws Exception {
        final String filename = file.getName();
        final String translatedName;

        Map<String, Object> inbound = new HashMap<>();
        inbound.put("originalFilename", filename);
        inbound.put("fileSize", file.getSize());
        inbound.put("timestamp", file.getTimestamp());

        MuleMessage muleMessage = new DefaultMuleMessage(NullPayload.getInstance(), muleContext);
        muleMessage.addProperties(inbound, PropertyScope.INBOUND);

        if (StringUtils.isNotBlank(translatedNameExpression)) {
            ExpressionManager expressionManager = muleContext.getExpressionManager();
            String result = (String) expressionManager.evaluate(translatedNameExpression, null, muleMessage, true);
            if (StringUtils.isNotEmpty(result)) {
                translatedName = result;
                Long size = sizeMap.get(translatedName);
                if (size == null) {
                    logger.warn("for file {} the translated file {} does not exist.", filename, translatedName);
                    return;
                }
                muleMessage.setProperty("fileSize", size, PropertyScope.INBOUND);
                inbound.put("fileSize", size);
            } else {
                translatedName = filename;
            }
        } else {
            translatedName = filename;
        }
        muleMessage.setProperty("filename", translatedName, PropertyScope.INBOUND);
        inbound.put("filename", translatedName);

        Consumer<ClientWrapper> onClose = cs.createCompletionHandler(muleMessage, filename, translatedName);

        InputStream is = config.getInputStream(directory, translatedName, onClose);
        if (streaming) {
            muleMessage.setPayload(is);
        } else {
            muleMessage.setPayload(IOUtils.toByteArray(is));
            is.close();
        }
        try {
            callback.process(muleMessage, inbound);
        } catch (MessagingException e) {
            // Ensure stream is closed when exception happens
            close(streaming, is);
            throw e;
        } catch (Exception e) {
            // Ensure stream is closed when exception happens
            close(streaming, is);
            throw new MessagingException(getEvent(), e);
        }
    }

    private void close(boolean streaming, InputStream is) {
        if (streaming) {
            IOUtils.closeQuietly(is);
        }
    }

    @SuppressWarnings("deprecation")
    private MuleEvent getEvent() {
        // It's deprecated, but there is no other way...
        return org.mule.RequestContext.getEvent();
    }

    /**
     * Write a file to FTP server.
     *
     * @param directory
     *            The name of the directory on the remote system. When the
     *            directory does not exist, it will be created.
     *
     * @param filename
     *            The name of the file on the remote system.
     *
     * @param content
     *            Content to be written. If {@link InputStream}, content of that
     *            stream. if {@link byte[]}, content of that array. Otherwise
     *            the String representation of the object.
     *
     * @param event
     *            Mule event being processed.
     *            
     * @return The value of the parameter <code>content</code> (probably not
     *         usable if Stream).
     *         
     * @throws Exception 
     *         Anything wrong in low level ftp/sftp libraries.
     */
    @Processor
    public Object putFile(@FriendlyName("Directory") @Default("") String directory, //
            @FriendlyName("File name") String filename, //
            @Default("#[payload]") @FriendlyName("File content") Object content, //
            MuleEvent event) throws Exception {

        try (OutputStream out = config.getOutputStream(directory, filename)) {
            if (content instanceof InputStream) {
                InputStream is = ((InputStream) content);
                IOUtils.copy(is, out);
                is.close();
            } else if (content instanceof OutputHandler) {
                ((OutputHandler) content).write(event, out);
            } else {
                byte[] dataBytes;
                if (content instanceof byte[]) {
                    dataBytes = (byte[]) content;
                } else {
                    dataBytes = content.toString().getBytes(event.getEncoding());
                }
                IOUtils.write(dataBytes, out);
            }
        }
        return content;
    }

    /**
     * Read a file from FTP server.
     *
     * @param directory
     *            The name of the directory on the remote system.
     *
     * @param filename
     *            The name of the file.
     *
     * @param streaming
     *            Return an {@link InputStream} instead of an
     *            <code>byte[]</code>
     *
     * @return The file content as {@link byte[]} or {@link InputStream}
     * 
     * @throws Exception 
     *         Anything wrong in low level ftp/sftp libraries.
     */
    @Processor
    public Object getFile(@FriendlyName("Directory") @Default("") final String directory, //
            @FriendlyName("File name") final String filename, //
            @FriendlyName("Streaming") @Default("true") boolean streaming) throws Exception {

        InputStream is = config.getInputStream(directory, filename, null);
        if (streaming) {
            return is;
        } else {
            byte[] result = IOUtils.toByteArray(is);
            is.close();
            return result;
        }
    }

    /**
     * @param directory
     *            Directory of file to delete
     * @param filename
     *            Filename of file to delete
     * @throws Exception
     *             Anything wrong in low level ftp/sftp libraries.
     */
    @Processor
    public void delete(@FriendlyName("Directory") @Default("") String directory, //
            @FriendlyName("File name") String filename) throws Exception {
        config.delete(directory, filename);
    }

    /**
     * @param directory
     *            Directory
     * @return Collection of files in this directory.
     * @throws Exception
     *             Anything wrong in low level ftp/sftp libraries.
     */
    @Processor
    public Collection<RemoteFile> list(@FriendlyName("Directory") @Default("") String directory) throws Exception {
        return config.list(directory);
    }

    /**
     * Rename/move a file.
     * 
     * @param originalDirectory
     *            Original directory
     * @param originalFilename
     *            Original file name
     * @param newDirectory
     *            New directory, relative to originalDirectory
     * @param newFilename
     *            New file name
     * @throws Exception 
     *         Anything wrong in low level ftp/sftp libraries.
     */
    @Processor
    public void rename(@FriendlyName("Original directory") @Default("") String originalDirectory, //
            @FriendlyName("Original file name") String originalFilename,
            @FriendlyName("New dir. (rel. to original)") @Default("") String newDirectory, //
            @FriendlyName("New file name") String newFilename) throws Exception {
        config.rename(originalDirectory, originalFilename, newDirectory, newFilename);
    }

    @SuppressWarnings("unchecked")
    public <T extends AbstractConfig<?>> T getConfig() {
        return (T) config;
    }

    public void setConfig(AbstractConfig<?> config) {
        this.config = config;
    }

    public void setMuleContext(MuleContext muleContext) {
        this.muleContext = muleContext;
    }
}
