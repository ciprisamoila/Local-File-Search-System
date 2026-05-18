package org.example.filebrowser.model.index;

import java.nio.file.attribute.FileTime;

public record FileAttributes(
        String name,
        String extension,
        String path,
        FileTime creationTime,
        FileTime lastAccessedTime,
        FileTime lastModifiedTime,
        long size
) {
}
