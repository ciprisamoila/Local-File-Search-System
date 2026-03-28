package org.example.filebrowser.model;

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
