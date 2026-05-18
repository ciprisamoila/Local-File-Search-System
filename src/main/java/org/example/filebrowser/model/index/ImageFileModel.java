package org.example.filebrowser.model.index;

public class ImageFileModel extends FileModel {
    private final String color;

    public ImageFileModel(FileAttributes fileAttributes, String checksumValue, long lastScanId, boolean readAccess, double score, FileType fileType, String color) {
        super(fileAttributes, checksumValue, lastScanId, readAccess, score, fileType);
        this.color = color;
    }

    public ImageFileModel(FileModel fileModel, String color) {
        super(fileModel);
        this.color = color;
    }

    public String getColor() {
        return color;
    }

}
