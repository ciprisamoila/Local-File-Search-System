package org.example.filebrowser.model.index;

public class TextFileModel extends FileModel {
    private final String content;

    public TextFileModel(FileAttributes fileAttributes, String checksumValue, long lastScanId, boolean readAccess, double score, FileType fileType, String content) {
        super(fileAttributes, checksumValue, lastScanId, readAccess, score, fileType);
        this.content = content;
    }

    public TextFileModel(FileModel fileModel, String content) {
        super(fileModel);
        this.content = content;
    }

    public String getContent() {
        return content;
    }
}
