package org.example.filebrowser.model;

public record FileModel(
        FileAttributes fileAttributes,
        String checksumValue,
        String content,
        long lastScanId,
        boolean readAccess,
        double score
) {
}
