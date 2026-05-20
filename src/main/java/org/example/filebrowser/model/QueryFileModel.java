package org.example.filebrowser.model;

import org.example.filebrowser.model.index.FileType;

public record QueryFileModel(
        String fullName, // name and extension
        String path,
        String creation_time,
        String last_modified_time,
        String last_accessed_time,
        long size,
        boolean readAccess,
        String headline,
        FileType fileType
) {
}
