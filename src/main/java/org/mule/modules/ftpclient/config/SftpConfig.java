package org.mule.modules.ftpclient.config;

import org.apache.commons.lang3.StringUtils;
import org.mule.api.ConnectionException;
import org.mule.api.ConnectionExceptionCode;
import org.mule.api.annotations.Configurable;
import org.mule.api.annotations.Connect;
import org.mule.api.annotations.TestConnectivity;
import org.mule.api.annotations.components.ConnectionManagement;
import org.mule.api.annotations.display.FriendlyName;
import org.mule.api.annotations.display.Password;
import org.mule.api.annotations.display.Placement;
import org.mule.api.annotations.param.ConnectionKey;
import org.mule.api.annotations.param.Optional;
import org.mule.modules.ftpclient.sftp.SftpClientWrapper;
import org.mule.modules.ftpclient.sftp.UnrestrictedCryptographyEnabler;

import com.jcraft.jsch.Channel;
import com.jcraft.jsch.ChannelSftp;
import com.jcraft.jsch.JSch;
import com.jcraft.jsch.JSchException;

@ConnectionManagement(configElementName = "sftp-config", friendlyName = "Sftp Configuration")
public class SftpConfig extends AbstractConfig {

	@Configurable
	@Placement(order = 3, group = "Server")
	@Optional
	@FriendlyName("Known Hosts File")
	protected String knownHostsFile;

	@Configurable
	@Password
	@Optional
	@Placement(order = 1, group = "Password")
	private String password;

	@Configurable
	@Optional
	@Placement(order = 1, group = "Public/Private Key")
	@FriendlyName("Identity File")
	private String identityFile;

	@Configurable
	@Optional
	@Placement(order = 2, group = "Public/Private Key")
	@FriendlyName("Identity Classpath Resource")
	private String identityResource;

	@Configurable
	@Password
	@Optional
	@Placement(order = 3, group = "Public/Private Key")
	@FriendlyName("Passphrase")
	private String passphrase;

	private JSch jsch = new JSch();

	public SftpConfig() {
		UnrestrictedCryptographyEnabler.enable();
	}

	@Connect
	public void connect(@SuppressWarnings("hiding") @Placement(order = 1, group = "Connection") @ConnectionKey String user) throws ConnectionException {
		LOGGER.debug("connect, host={}, port={}, user={}", host, port, user);
		this.user = user;
		if (StringUtils.isNotEmpty(identityFile) && StringUtils.isNotEmpty(identityResource)) {
			throw new ConnectionException(ConnectionExceptionCode.UNKNOWN, "", "Don't specifiy Identity File and Identity Classpath Resource");
		}
		ChannelSftp channel;
		if (StringUtils.isNotBlank(identityFile) || StringUtils.isNotBlank(identityResource)) {
			channel = SftpClientWrapper.createChannel(jsch, host, port, timeout, knownHostsFile, user, identityFile, identityResource, passphrase);
		} else {
			channel = SftpClientWrapper.createChannel(jsch, host, port, timeout, knownHostsFile, user, password);
		}
		try {
			channel.connect();
		} catch (JSchException e) {
			throw new ConnectionException(ConnectionExceptionCode.UNKNOWN, e.getMessage(), e.getMessage(), e);
		}
		clientWrapper = new SftpClientWrapper(channel);
	}

	@TestConnectivity
	public void testConnect(@SuppressWarnings("hiding") @Placement(order = 1, group = "Connection") @ConnectionKey String user) throws ConnectionException {
		this.user = user;
		if (StringUtils.isNotEmpty(identityFile) && StringUtils.isNotEmpty(identityResource)) {
			throw new ConnectionException(ConnectionExceptionCode.UNKNOWN, "", "Don't specifiy Identity File and Identity Classpath Resource");
		}
		try {
			Channel channel;
			if (StringUtils.isNotBlank(identityFile) || StringUtils.isNotBlank(identityResource)) {
				channel = SftpClientWrapper.createChannel(jsch, host, port, timeout, knownHostsFile, user, identityFile, identityResource, passphrase);
			} else {
				channel = SftpClientWrapper.createChannel(jsch, host, port, timeout, knownHostsFile, user, password);
			}
			channel.connect();
			channel.disconnect();
		} catch (JSchException e) {
			SftpClientWrapper.translateException(e, host, port, user);
		}
	}

	public String getKnownHostsFile() {
		return knownHostsFile;
	}

	public void setKnownHostsFile(String knownHostsFile) {
		this.knownHostsFile = knownHostsFile;
	}

	public String getPassword() {
		return password;
	}

	public void setPassword(String password) {
		this.password = password;
	}

	public String getIdentityFile() {
		return identityFile;
	}

	public void setIdentityFile(String identityFile) {
		this.identityFile = identityFile;
	}

	public String getIdentityResource() {
		return identityResource;
	}

	public void setIdentityResource(String identityResource) {
		this.identityResource = identityResource;
	}

	public String getPassphrase() {
		return passphrase;
	}

	public void setPassphrase(String passphrase) {
		this.passphrase = passphrase;
	}
}
