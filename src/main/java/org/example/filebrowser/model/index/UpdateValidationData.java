package org.example.filebrowser.model.index;

import java.nio.file.attribute.FileTime;

public record UpdateValidationData(
        long id,
        FileTime lastModifiedTime,
        String checksumValue,
        boolean readAccess
) {
}
