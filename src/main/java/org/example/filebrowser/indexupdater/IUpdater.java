package org.example.filebrowser.indexupdater;

import org.example.filebrowser.model.FileModel;
import org.example.filebrowser.model.UpdateValidationData;

import java.nio.file.attribute.FileTime;

public interface IUpdater {
    UpdateValidationData searchByPath(String[] path);
    void insert(FileModel fileModel);
    void updateLastModifiedTime(FileTime lastModifiedTime);
    void updateFile(FileModel fileModel);
    void updateLastScanId(long scanId);
    void removeUnscanned(long scanId);
}
