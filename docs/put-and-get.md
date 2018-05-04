# Put and Get Remote Files


## Put

Write the payload to a remote file (null payload will result in an empty file).

* directory: Directory on the remote system. When the directory does not exist, it will be created.
* filename: The name of the file on the remote system.
* content:  Content to be written. If `InputStream`, content of that stream. If `byte[]`, content of that array. Otherwise the String representation of the object. If `OutputHandler`: Whatever the method `write`produces.

Return value: The value of the parameter `content`(probably not usable if Stream).

## Get

Read a file from ftp server. Parameters:
* directory: The name of the directory on the remote system.
* filename: The name of the file on the remote system.
* streaming Return an `InputStream` instead of an `byte[]`
* deleteAfterGet: Delete the file after it has been read.

Returns a stream (`streaming` true) or the content as byte array.
