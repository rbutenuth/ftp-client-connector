# Connection Configuration

The configuration differs for ftp and sftp. Common for both are:

* host: Host name (e.g. ftp.some-company.com) or IP address of remote host.
* port: Port number used by the remote host. The default is correct for ftp, for sftp you have to configure it (usually it's 22 for sftp)

## ftp

ftp has just on login method, so it's simple: Provide username and password. 

The ftp protocol allows automatic encoding conversions when transferMode is set to `Ascii`. Usually you don't want that, so the default `Binary` should fit most cases.

The passiveMode let's you control the direction in which the (second) TCP/IP connection for data transfer is created. See [Active FTP vs. Passive FTP, a Definitive Explanation](http://slacksite.com/other/ftp.html)" for a details. You probably need to tweak this when a firewall is involved.

## sftp

The sftp has a similar name to ftp, but uses only one encrypted TCP/IP connection for control and data traffic. As it's encryption with private/public key pairs, it's a little bit more complicated.

Public certificates of remote servers can be stored in the `knownHostsFile`. Unknown certificates are automatically added. A changed certificate triggers an error. It avoids man in the middle attacks.

A sample file looks like this:

```
localhost ssh-rsa AAAAB3NzaC1yc2EAAAABJQAAAQEAvf...QtESI7bxpWCPmQ==
```

sftp supports two authentication methods:

### Username / Password

Same as for ftp, just provide username and password.

### Key Based

Authentication is based on a public/private key pair. You have to provide an `identityFile`. This can be copied to `src/main/resources/keypair.ssh`, 
it will be copied to classes by the Maven build. In this example, reference it like this: `${app.home}/classes/keypair.ssh`. 

When the file is protected by a passphrase, you have to provide it. 

The server recognizes you by your public key, which you have to give to the server provider.
