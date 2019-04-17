# Polling

## Introduction 

Polling can be used at the start of a flow only. It will poll a remote `directory`, listing all files, filtering for regular files (no subdirectories, symbolic links etc.),
matching against the regular expression in `filename` and process them sequentially. After processing has finished, it will wait `pollingPeriod` milliseconds.

The message contains the file content as byte array or InputStream (when `streamin` is switched to true). 
In case of streaming, make sure the stream is read to its end and/or closed, otherwise the connection to the remote system will
not be closed (resource leak) and the action (remove, delete, rename) will not be executed.

The following inboundProperties will be set:

* originalFilename: Name of file
* filename: Result of `filenameTranslator` when specified, otherwise the same as `originalFilename`.
* timestamp: A `java.util.Date` with a timestamp of the file.

## "Ready-File" Handling

Writing files to an ftp server (remote or even local) does not happen atomically. Therefore, you may read a file which is not complete.
This can be avoided by a simple pattern:
1. Write a file with the content (e.g. my-file.xml)
1. Then write an empty marker file (e.g. my-file.xml.ready) 

On the "reading side", you have to wait for the "done-file" and then read the original file. This can be achieved with `translatedNameExpression`.
In `filename`, specify a regular expression matching the file with "done" suffix. In `translatedNameExpression` specify a MEL expression which
removes the suffix, in this case: `#[message.inboundProperties.originalFilename.toString().replace('.ready', '')]` . The ftp connector
will start reading when both files are there. (When there is only a "done file" but the "real file" is missing, a warning is written to the log.)

## File Completion Handling

### Delete

The most simple way of polling usually deletes the file after processing it. This is done when you set `deleteAfterGet`
to true. If false, the file will be considered again in the next polling period. This makes only sense when it is overwritten 
by the producer.


### Move to Directory

This polling variant archives processed files by moving them to another directory. Setting `deleteAfterGet` to true makes only sense
when you are working with "done files", in this case the file with the data will be moved, the "done file" deleted.

### Rename

After processing, the processed file and/or the "ok-file" are renamed within the input directory. 
Use MEL expressions named originalFilenameExpression and filenameExpression for renaming. 
In case the expressions evaluate to null or an empty String, the files will be deleted.

## Examples

### Read .txt Files and Rename to .txt.done

The following example reads all files ending with ".txt", therefore the regular expression ".*\.txt"  (note the backspace for escaping the dot in the filename). The filenameExpression contains a MEL expression to add ".done" to the originalFilename for renaming. Note you have to do the string concatanetion within the MEL expression, you can't add string content after the closing bracket.

Even when you are not using "ok files", you have to give an expression for renaming the original filename. Here it is a MEL expression returning an empty string. (In the future this attribute will be made optional.)

```XML
<ftp-client:poll-with-archiving-by-renaming config-ref="Ftp_Configuration" doc:name="FTP" 
	directory="/"  
	pollingPeriod="10000" 
	filename=".*\.txt" 
	filenameExpression="#[message.inboundProperties.originalFilename + '.done']" 
	originalFilenameExpression="#['']"/>
```

### Deleay Reading a File Until a File with Suffix .ready appears

The following example reads files ending with .txt, but does not start until a file with the same basename but with suffix .txt.ready exists. The content of thie "ready-file" is ignored, just the existence is checked. Here the XML:

```XML
<ftp-client:poll-with-archiving-by-renaming config-ref="Ftp_Configuration" doc:name="FTP"
    directory="/"  
    pollingPeriod="10000" 
    filename=".*\.txt\.ready"
    filenameExpression="#[message.inboundProperties.filename + '.done']" 
    originalFilenameExpression="#[message.inboundProperties.originalFilename + '.done']"
    translatedNameExpression="#[message.inboundProperties.originalFilename.toString().replace('.ready', '')]"/>
```

The regular expression in "filename" checks for the "ready-file". In case it is found, the suffix is removed by the MEL expression in attribute "translatedNameExpression". The two attributes "filenameExpression" and "originalFilenameExpression" contain MEL expressions responsible for the renaming. They just add ".done" to their filename.

In case you just want to delete the "ready-file" (as it contains no data), change "originalFilenameExpression" to #['']. An emtpy result of the MEL expressions causes a deletion of the file.
