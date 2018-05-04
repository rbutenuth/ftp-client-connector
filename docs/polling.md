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

## "Done-File" Handling

Writing files to an ftp server (remote or even local) does not happen atomically. Therefore, you may read a file which is not complete.
This can be avoided by a simple pattern:
1. Write a file with the content (e.g. my-file.xml)
1. Then write an empty marker file (e.g. my-file.xml.done) 

On the "reading side", you have to wait for the "done-file" and then read the original file. This can be achieved with `translatedNameExpression`.
In `filename`, specify a regular expression matching the file with "done" suffix. In `translatedNameExpression` specify a MEL expression which
removes the suffix, in this case: `#[message.inboundProperties.originalFilename.toString().replace(".done", "")]` . The ftp connector
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

