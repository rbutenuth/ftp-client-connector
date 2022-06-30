package org.mule.modules.ftpclient.sftp;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.ConnectException;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.List;
import java.util.Properties;
import java.util.function.Consumer;

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.mule.api.ConnectionException;
import org.mule.api.ConnectionExceptionCode;
import org.mule.modules.ftpclient.AutoCloseOnEOFInputStream;
import org.mule.modules.ftpclient.AutoCloseOnEOFInputStream.ConsumerWithIOException;
import org.mule.modules.ftpclient.ClientWrapper;
import org.mule.modules.ftpclient.FtpFileType;
import org.mule.modules.ftpclient.RemoteFile;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.jcraft.jsch.ChannelSftp;
import com.jcraft.jsch.ChannelSftp.LsEntry;
import com.jcraft.jsch.JSch;
import com.jcraft.jsch.JSchException;
import com.jcraft.jsch.Session;
import com.jcraft.jsch.SftpATTRS;
import com.jcraft.jsch.SftpException;

public class SftpClientWrapper extends ClientWrapper {
	public static final String CHANNEL_SFTP = "sftp";
	public static final String STRICT_HOST_KEY_CHECKING = "StrictHostKeyChecking";

	private static final Logger LOGGER = LoggerFactory.getLogger(SftpClientWrapper.class);

	private ChannelSftp channel;

	public SftpClientWrapper(ChannelSftp channel) {
		this.channel = channel;
	}

	@Override
	public void destroy() throws JSchException {
		if (channel != null) {
			Session session = channel.getSession();
			channel.disconnect();
			if (session != null && session.isConnected()) {
				session.disconnect();
			}
		}
	}

	@Override
	public boolean validate() {
		return channel.isConnected();
	}

	@Override
	public OutputStream getOutputStream(String directory, String filename) throws IOException, SftpException {
		changeWorkingDirectory(directory, true);
		OutputStream out = channel.put(filename);
		return out;
	}

	@Override
	public InputStream getInputStream(final String directory, final String filename, final Consumer<ClientWrapper> onClose) throws SftpException, IOException {
		changeWorkingDirectory(directory, false);
		final InputStream is = channel.get(filename);
		return new AutoCloseOnEOFInputStream(is, new ConsumerWithIOException() {
			@Override
			public void apply() {
				try {
					onClose.accept(SftpClientWrapper.this);
				} catch (Exception e) {// signature is from commons.pool NOSONAR
					LOGGER.debug("ignore exception in cleanup", e);
				}
			}
		});
	}

	@Override
	public void delete(String directory, String filename) throws IOException {
		changeWorkingDirectory(directory, false);
		try {
			channel.rm(filename);
		} catch (SftpException e) {
			throw new IOException("Can't delete " + directory + "/" + filename, e);
		}
	}

	@Override
	public void move(String fromCompletePath, String toCompletePath) throws SftpException {
		channel.rename(fromCompletePath, toCompletePath);
	}

	@Override
	public List<RemoteFile> list(String directory) throws IOException {
		changeWorkingDirectory(directory, false);
		List<RemoteFile> fileList = new ArrayList<>();
		try {
			@SuppressWarnings("unchecked")
			Collection<LsEntry> files = channel.ls(".");
			for (LsEntry e : files) {
				String name = e.getFilename();
				if (".".equals(name) || "..".equals(name)) {
					continue;
				}
				SftpATTRS attrs = e.getAttrs();
				FtpFileType type;
				if (attrs.isDir()) {
					type = FtpFileType.DIRECTORY;
				} else if (attrs.isLink()) {
					type = FtpFileType.SYMBOLIC_LINK;
				} else if (attrs.isReg()) {
					type = FtpFileType.FILE;
				} else {
					type = FtpFileType.UNKNOWN;
				}
				fileList.add(new RemoteFile(type, name, attrs.getSize(), new Date(attrs.getMTime() * 1000L)));
			}
		} catch (SftpException e) {
			throw invalidate(new IOException("Can't list " + directory, e));
		}
		return fileList;
	}

	@Override
	protected void changeToAbsoluteDirectory(String directory, boolean create) throws IOException {
		try {
			channel.cd(directory);
		} catch (SftpException e) {
			// We may fail because the directory does not exist: Just create and
			// try again
			if (create) {
				try {
					channel.cd("/");
				} catch (SftpException e1) {
					throw invalidate(new IOException(e));
				}
				for (String d : split(directory)) {
					changeToChildDirectory(d, true);
				}
			} else {
				throw invalidate(new IOException(e));
			}
		}
	}

	@Override
	protected void changeToParentDirectory() throws IOException {
		try {
			channel.cd("..");
		} catch (SftpException e) {
			throw invalidate(new IOException(e));
		}
	}

	@Override
	protected void changeToChildDirectory(String name, boolean create) throws IOException {
		try {
			channel.cd(name);
		} catch (SftpException e) {
			// We may fail because the directory does not exist: Just create and
			// try again
			if (create) {
				createDirectory(name);
				changeToChildDirectory(name, false);
			} else {
				throw invalidate(new IOException(e));
			}
		}
	}

	@Override
	protected void createDirectory(String name) throws IOException {
		try {
			channel.mkdir(name);
		} catch (SftpException e) {
			throw invalidate(new IOException(e));
		}
	}

	public static ChannelSftp createChannel(JSch jsch, String host, int port, int timeout, String knownHostsFile, String user, String password)
			throws ConnectionException {
		try {
			Properties props = new Properties();
			configureHostChecking(jsch, knownHostsFile, props);

			Session session = jsch.getSession(user, host);
			session.setConfig(props);
			session.setPort(port);
			session.setPassword(password);
			session.setTimeout(timeout);
			session.connect(timeout);
			return (ChannelSftp) session.openChannel(CHANNEL_SFTP);
		} catch (JSchException e) {
			translateException(e, host, port, user);
			return null; // not reached, but compiler doesn't know
		}
	}

	public static ChannelSftp createChannel(JSch jsch, String host, int port, int timeout, String knownHostsFile, String user, String identityFile,
			String identityResource, String passphrase) throws ConnectionException {
		try {
			if (StringUtils.isNotBlank(identityResource)) {
				addIdentityFromResource(jsch, identityResource, passphrase);
			} else {
				addIdentity(jsch, new File(identityFile), passphrase);
			}
			Properties props = new Properties();
			configureHostChecking(jsch, knownHostsFile, props);

			Session session = jsch.getSession(user, host);
			session.setConfig(props);
			session.setPort(port);
			session.setTimeout(timeout);
			session.connect(timeout);
			return (ChannelSftp) session.openChannel(CHANNEL_SFTP);
		} catch (JSchException e) {
			translateException(e, host, port, user);
			return null; // not reached, but compiler doesn't know
		}
	}

	private static void addIdentityFromResource(JSch jsch, String identityResource, String passphrase) throws JSchException, ConnectionException {
		File file = null;
		try {
			file = File.createTempFile(identityResource, "tmp");
			try (InputStream is = Thread.currentThread().getContextClassLoader().getResourceAsStream(identityResource.trim());
					OutputStream os = new FileOutputStream(file)) {
				if (is == null) {
					throw new ConnectionException(ConnectionExceptionCode.UNKNOWN, "", "Can't load classpath resource " + identityResource);
				}
				IOUtils.copy(is, os);
				addIdentity(jsch, file, passphrase);
			} finally {
				if (file != null) {
					file.delete();
				}
			}
		} catch (IOException e) {
			throw new ConnectionException(ConnectionExceptionCode.UNKNOWN, e.getMessage(), "Can't copy " + identityResource + " to temp file");
		} finally {
			if (file != null) {
				file.delete();
			}
		}
	}

	private static void addIdentity(JSch jsch, File file, String passphrase) throws JSchException {
		if (StringUtils.isEmpty(passphrase)) {
			jsch.addIdentity(file.getAbsolutePath());
		} else {
			jsch.addIdentity(file.getAbsolutePath(), passphrase);
		}
	}

	private static void configureHostChecking(JSch jsch, String knownHostsFile, Properties props) throws JSchException {
		if (StringUtils.isNotBlank(knownHostsFile)) {
			props.put(STRICT_HOST_KEY_CHECKING, "ask");
			jsch.setKnownHosts(knownHostsFile);
		} else {
			props.put(STRICT_HOST_KEY_CHECKING, "no");
		}
	}

	private IOException invalidate(IOException ioe) {
		try {
			destroy();
		} catch (Exception e) {
			LOGGER.error("ignore on cleanup", e);
		}
		return ioe;
	}

	public static void translateException(JSchException e, String host, int port, String user) throws ConnectionException {
		if (e.getCause() instanceof ConnectException) {
			throw new ConnectionException(ConnectionExceptionCode.CANNOT_REACH, "", "Could not connect to ftp server: " + port + "@" + host, e);
		} else if (e.getCause() instanceof UnknownHostException) {
			throw new ConnectionException(ConnectionExceptionCode.UNKNOWN_HOST, "", "Unknown host: " + host, e);
		} else {
			throw new ConnectionException(ConnectionExceptionCode.INCORRECT_CREDENTIALS, "", "Could not login to ftp server with user " + user, e);
		}
	}
}
