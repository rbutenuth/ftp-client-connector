[![Build Status](https://travis-ci.org/rbutenuth/ftp-client-connector.svg?branch=master)](https://travis-ci.org/rbutenuth/ftp-client-connector)

# FtpClient Anypoint Connector

The ftp-client-connector is an implementation of the ftp/sftp protocol within Mule 3.x. 
It has been developed together by [C&A](https://www.c-and-a.com/) and [codecentric](https://www.codecentric.de/).

It supports several features missing from the connectors for ftp/sftp provided by MuleSoft:

1. Additional to polling, you can read files from the middle of a flow (given directory/name)
1. Delete with known name within a flow
1. List files in a directory (with file size and modification timestamp)
1. Poll with "ok file handling": Wait for file x and y, then read y (transformation from name x to y with a MEL expression). This is a standard pattern to avoid reading files while they are still beeing written.

## Mule supported versions
Mule 3.8/3.9 (tested just this, probably also works with 3.6/3.7)

## Installation
For beta connectors you can download the source code and build it with DevKit to find it available on your local repository. Then you can add it to AnypointStudio.

For direct installation within AnypointStudio, go to Help -> Install new Software... and add the 
[Update site](https://raw.githubusercontent.com/rbutenuth/ftp-client-connector/master/update-site/)
to the list of available sites.

When you use Maven: Released versions of the connector are on Maven Central, so you don't need to add a repository.

## Usage Instructions

* [Connection configuration](https://github.com/rbutenuth/ftp-client-connector/blob/master/docs/connection.md)
* [Polling](https://github.com/rbutenuth/ftp-client-connector/blob/master/docs/polling.md)
* [Put, Get](https://github.com/rbutenuth/ftp-client-connector/blob/master/docs/put-and-get.md)
* [List, Delete, Rename](https://github.com/rbutenuth/ftp-client-connector/blob/master/docs/list-delete-rename.md)

## CI

https://travis-ci.org/rbutenuth/ftp-client-connector

## Maven Site

[Site](https://rbutenuth.github.io/ftp-client-connector/site/index.html)

## Reporting Issues

We use GitHub:Issues for tracking issues with this connector. You can report new issues at this link https://github.com/rbutenuth/ftp-client-connector/issues.
