package org.mule.modules.ftpclient;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.function.Consumer;

import org.apache.commons.lang3.StringUtils;

import com.jcraft.jsch.JSchException;
import com.jcraft.jsch.SftpException;

public abstract class ClientWrapper {
    private boolean currentDirectoryAbsolute;
    private List<String> currentDirectory;

    protected ClientWrapper() {
        currentDirectoryAbsolute = false;
        currentDirectory = new ArrayList<>();
    }

    public abstract void destroy() throws JSchException, IOException;

    public abstract boolean validate();

    public abstract OutputStream getOutputStream(String directory, String filename) throws IOException, SftpException;

    public abstract InputStream getInputStream(final String directory, final String filename,
            final Consumer<ClientWrapper> onClose) throws IOException, SftpException;

    public abstract void delete(String directory, String filename) throws IOException;

    public abstract void move(String fromFile, String toCompletePath) throws IOException, SftpException;

    public abstract List<RemoteFile> list(String directory) throws IOException;

    public boolean isAbsolute(String directory) {
        return StringUtils.isNotBlank(directory) && directory.trim().charAt(0) == '/';
    }

    public String getCurrentDirectory() {
        StringBuilder sb = new StringBuilder();
        if (currentDirectoryAbsolute) {
            sb.append('/');
        }
        Iterator<String> iter = currentDirectory.iterator();
        while (iter.hasNext()) {
            sb.append(iter.next());
            if (iter.hasNext()) {
                sb.append('/');
            }
        }
        return sb.toString();
    }

    public void changeWorkingDirectory(String newDirectory, boolean create) throws IOException {
        List<String> normalizedDirectory = split(newDirectory);

        if (isAbsolute(newDirectory)) {
            currentDirectoryAbsolute = true;
            currentDirectory = normalizedDirectory;
            changeToAbsoluteDirectory(newDirectory, create);
        } else {
            int commonPrefixLength = 0;
            while (currentDirectory.size() > commonPrefixLength && normalizedDirectory.size() > commonPrefixLength
                    && currentDirectory.get(commonPrefixLength).equals(normalizedDirectory.get(commonPrefixLength))) {
                commonPrefixLength++;
            }
            while (currentDirectory.size() > commonPrefixLength) {
                changeToParentDirectory();
                currentDirectory.remove(currentDirectory.size() - 1);
            }
            while (currentDirectory.size() < normalizedDirectory.size()) {
                String name = normalizedDirectory.get(currentDirectory.size());
                changeToChildDirectory(name, create);
                currentDirectory.add(name);
            }
        }
    }

    protected abstract void changeToAbsoluteDirectory(String directory, boolean create) throws IOException;

    protected abstract void changeToParentDirectory() throws IOException;

    protected abstract void changeToChildDirectory(String name, boolean create) throws IOException;

    protected abstract void createDirectory(String name) throws IOException;

    public static String normalize(String name) {
        String d = StringUtils.isEmpty(name) ? "" : name.trim();
        if (d.startsWith("/")) {
            d = d.substring(1);
        }
        if (d.endsWith("/")) {
            d = d.substring(0, d.length() - 1);
        }
        return d;
    }

    protected List<String> split(String path) {
        List<String> parts = new ArrayList<>();
        String normalized = normalize(path);
        int length = normalized.length();
        StringBuilder part = new StringBuilder();
        for (int i = 0; i < length; i++) {
            char ch = normalized.charAt(i);
            if (ch == '/') {
                addPart(parts, part);
                part = new StringBuilder();
            } else {
                part.append(ch);
            }
        }
        if (part.length() > 0) {
            addPart(parts, part);
        }
        return parts;
    }

    private void addPart(List<String> parts, CharSequence part) {
        String p = part.toString();
        if (!".".equals(p)) {
            parts.add(p);
        }
    }
}
