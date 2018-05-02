[![Build Status](https://travis-ci.org/rbutenuth/ftp-client-connector.svg?branch=master)](https://travis-ci.org/rbutenuth/ftp-client-connector)

# FtpClient Anypoint Connector

The ftp-client-connector is an alternative implementation to the ftp and sftp connectors within Mule 3.x. It supports several features missing from the connector provided by MuleSoft:

1. Additional to polling, you can read files from the middle of a flow (given directory/name)
1. Delete files
1. List files in a directory
1. Poll with "ok file handling": Wait for file x and y, then read y

## Mule supported versions
Mule 3.8/3.9 (tested just this, probably also works with 3.6/3.7)

## Installation
For beta connectors you can download the source code and build it with devkit to find it available on your local repository. Then you can add it to Studio

For direct installation within AnypointStudio, go to Help -> Install new Software... and add the 
[Update site](https://raw.githubusercontent.com/rbutenuth/ftp-client-connector/master/update-site/)
to the list of available sites.

When you use Maven: Released versions of the connector are on Maven Central, so you don't need to add a repository.

## Usage Instructions

TODO...

* [Polling](https://github.com/rbutenuth/ftp-client-connector/blob/master/docs/polling.md)
* [Put, Get](https://github.com/rbutenuth/ftp-client-connector/blob/master/docs/put-and-get.md)
* [List, Delete](https://github.com/rbutenuth/ftp-client-connector/blob/master/docs/list-and-delete.md)

## CI

https://travis-ci.org/rbutenuth/ftp-client-connector

## Maven Site

[Site](https://rbutenuth.github.io/ftp-client-connector/site/index.html)

## Reporting Issues

We use GitHub:Issues for tracking issues with this connector. You can report new issues at this link https://github.com/rbutenuth/ftp-client-connector/issues.
