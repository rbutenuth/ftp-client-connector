package org.mule.modules.ftpclient;

import java.io.IOException;
import java.util.function.Consumer;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.jcraft.jsch.SftpException;

class FileArchiver implements Consumer<ClientWrapper> {

    private static final Logger LOGGER = LoggerFactory.getLogger(FileArchiver.class);

    private final String from1;
    private final String to1;
    private final String from2;
    private final String to2;

    public FileArchiver(String from1, String to1, String from2, String to2) {
        this.from1 = from1;
        this.to1 = to1;
        this.from2 = from2;
        this.to2 = to2;
    }

    @Override
    public void accept(ClientWrapper wrapper) {
        try {
            move(wrapper, from1, to1);
        } catch (IOException | SftpException e) {
            LOGGER.warn("Archiving " + from1 + " to " + to1 + " failed", e);
        }
        if (!from1.equals(from2)) {
            try {
                move(wrapper, from2, to2);
            } catch (IOException | SftpException e) {
                LOGGER.warn("Archiving " + from2 + " to " + to2 + " failed", e);
            }
        }
    }

    private void move(ClientWrapper wrapper, String from, String to) throws IOException, SftpException {
        if (StringUtils.isBlank(to)) {
            wrapper.delete(wrapper.getCurrentDirectory(), from);
        } else {
            wrapper.move(from, to);
        }
    }
}