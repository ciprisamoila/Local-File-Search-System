package org.example.filebrowser.model;

public record FileModel(
        FileAttributes fileAttributes,
        long checksumValue,
        String content,
        long lastScanId
) {
}
