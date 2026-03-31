package org.example.filebrowser.model;

import java.nio.file.attribute.FileTime;

public record UpdateValidationData(
        FileTime lastModifiedTime,
        Long checksumValue
) {
}
