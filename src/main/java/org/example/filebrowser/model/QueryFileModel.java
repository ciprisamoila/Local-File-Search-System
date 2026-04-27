package org.example.filebrowser.model;

public record QueryFileModel(
        String fullName, // name and extension
        String path,
        String creation_time,
        String last_modified_time,
        String last_accessed_time,
        long size,
        boolean readAccess,
        String headline
) {
}
