package org.example.filebrowser.model;

import java.nio.file.attribute.FileTime;

public record UpdateValidationData(
        long id,
        FileTime lastModifiedTime,
        String checksumValue,
        boolean readAccess
) {
}
