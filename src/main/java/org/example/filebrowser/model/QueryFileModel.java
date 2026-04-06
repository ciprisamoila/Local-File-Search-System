package org.example.filebrowser.model;

public record QueryFileModel(
        String fullName, // name and extension
        String path,
        String headline,
        boolean readAccess
) {
}
