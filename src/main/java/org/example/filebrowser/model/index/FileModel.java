package org.example.filebrowser.model.index;

public class FileModel {

    private final FileAttributes fileAttributes;
    private final String checksumValue;
    private final long lastScanId;
    private final boolean readAccess;
    private final double score;
    private final FileType fileType;

    public FileModel(FileAttributes fileAttributes, String checksumValue, long lastScanId, boolean readAccess, double score, FileType fileType) {
        this.fileAttributes = fileAttributes;
        this.checksumValue = checksumValue;
        this.lastScanId = lastScanId;
        this.readAccess = readAccess;
        this.score = score;
        this.fileType = fileType;
    }

    public FileModel(FileModel fileModel) {
        this.fileAttributes = fileModel.fileAttributes;
        this.checksumValue = fileModel.checksumValue;
        this.lastScanId = fileModel.lastScanId;
        this.readAccess = fileModel.readAccess;
        this.score = fileModel.score;
        this.fileType = fileModel.fileType;
    }

    public FileAttributes getFileAttributes() {
        return fileAttributes;
    }

    public String getChecksumValue() {
        return checksumValue;
    }

    public long getLastScanId() {
        return lastScanId;
    }

    public boolean isReadAccess() {
        return readAccess;
    }

    public double getScore() {
        return score;
    }

    public FileType getFileType() {
        return fileType;
    }
}
