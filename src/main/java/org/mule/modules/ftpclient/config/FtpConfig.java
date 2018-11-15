package org.mule.modules.ftpclient.config;

import java.io.IOException;
import java.net.UnknownHostException;

import org.apache.commons.net.ftp.FTP;
import org.apache.commons.net.ftp.FTPClient;
import org.apache.commons.net.ftp.FTPReply;
import org.mule.api.ConnectionException;
import org.mule.api.ConnectionExceptionCode;
import org.mule.api.annotations.Configurable;
import org.mule.api.annotations.Connect;
import org.mule.api.annotations.TestConnectivity;
import org.mule.api.annotations.components.ConnectionManagement;
import org.mule.api.annotations.display.Password;
import org.mule.api.annotations.display.Placement;
import org.mule.api.annotations.param.ConnectionKey;
import org.mule.api.annotations.param.Default;
import org.mule.modules.ftpclient.ftp.FtpClientWrapper;

@ConnectionManagement(configElementName = "ftp-config", friendlyName = "Ftp Configuration")
public class FtpConfig extends AbstractConfig {
	@Configurable
	@Password
	@Placement(order = 2, group = "Connection")
	private String password;

	@Configurable
	@Default("Binary")
	@Placement(order = 1, group = "Parameters")
	private TransferMode transferMode = TransferMode.Binary;

	@Configurable
	@Placement(order = 2, group = "Parameters")
	@Default("true")
	private boolean passiveMode = true;

	@Connect
	public void connect(@SuppressWarnings("hiding") @Placement(order = 1, group = "Connection") @ConnectionKey String user) throws ConnectionException {
		LOGGER.debug("connect, host={}, port={}, user={}", host, port, user);
		this.user = user;
		FTPClient client = null;
		boolean ready = false;
		try {
			client = createConnectedClient();
			switch (transferMode) {
			case Ascii:
				client.setFileType(FTP.ASCII_FILE_TYPE);
				break;
			case Binary:
			default:
				client.setFileType(FTP.BINARY_FILE_TYPE);
				break;
			}
			if (passiveMode) {
				client.enterLocalPassiveMode();
			} else {
				client.enterLocalActiveMode();
			}
			ready = true;
		} catch (IOException e) {
			throw new ConnectionException(ConnectionExceptionCode.UNKNOWN, e.getMessage(), "Could not login to ftp server", e);
		} finally {
			if (!ready && client != null && client.isConnected()) {
				try {
					client.disconnect();
				} catch (IOException e) {
					// ignore
				}
			}
		}

		clientWrapper = new FtpClientWrapper(client);
	}

	@TestConnectivity
	public void testConnect(@SuppressWarnings("hiding") @Placement(order = 1, group = "Connection") @ConnectionKey String user) throws ConnectionException {
		this.user = user;
		FTPClient client = null;
		try {
			client = createConnectedClient();
			client.logout();
		} catch (IOException e) {
			throw new ConnectionException(ConnectionExceptionCode.UNKNOWN, e.getMessage(), "Unknown error");
		} finally {
			if (client != null && client.isConnected()) {
				try {
					client.disconnect();
				} catch (IOException ioe) {
					// ignore exceptions on cleanup
				}
			}
		}
	}

	private FTPClient createConnectedClient() throws ConnectionException {
		FTPClient client = new FTPClient();
		client.setConnectTimeout(timeout);
		try {
			client.connect(host, port);
			client.setDataTimeout(timeout);
		} catch (UnknownHostException e) {
			throw new ConnectionException(ConnectionExceptionCode.UNKNOWN_HOST, "", "Unknown host: " + host);
		} catch (IOException e) {
			throw new ConnectionException(ConnectionExceptionCode.CANNOT_REACH, "", "Could not connect to ftp server: " + port + "@" + host);
		}
		int reply = client.getReplyCode();
		if (!FTPReply.isPositiveCompletion(reply)) {
			throw new ConnectionException(ConnectionExceptionCode.CANNOT_REACH, "", "Could not connect to ftp server: " + port + "@" + host);
		}
		boolean success;
		try {
			success = client.login(user, password);
		} catch (IOException e) {
			throw new ConnectionException(ConnectionExceptionCode.INCORRECT_CREDENTIALS, "", "Could not login to ftp server with user " + user);
		}
		if (!success) {
			throw new ConnectionException(ConnectionExceptionCode.INCORRECT_CREDENTIALS, "", "Could not login to ftp server with user " + user);
		}
		return client;
	}

	public String getPassword() {
		return password;
	}

	public void setPassword(String password) {
		this.password = password;
	}

	public TransferMode getTransferMode() {
		return transferMode;
	}

	public void setTransferMode(TransferMode transferMode) {
		this.transferMode = transferMode;
	}

	public boolean isPassiveMode() {
		return passiveMode;
	}

	public void setPassiveMode(boolean passiveMode) {
		this.passiveMode = passiveMode;
	}
}