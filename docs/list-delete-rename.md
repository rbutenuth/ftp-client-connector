# List and Delete Remote Files

## Delete

Specify `directory` and `filename`, that's it.
Returns the original payload.

## List

Just provide a `directory`, result is a `Collection<RemoteFile>` in the payload.

## Rename

Rename a file with name `originalFilename` in `originalDirectory` to `newFilename`.
The new filename can contain a path, which is interpreted relativ to the original directory.
