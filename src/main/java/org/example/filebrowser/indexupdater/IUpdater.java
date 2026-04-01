package org.example.filebrowser.indexupdater;

import org.example.filebrowser.model.FileModel;
import org.example.filebrowser.model.UpdateValidationData;
import org.example.filebrowser.utils.exceptions.IndexUpdaterException;

import java.nio.file.attribute.FileTime;

public interface IUpdater {
    UpdateValidationData searchByPath(String path) throws IndexUpdaterException;
    void insert(FileModel fileModel) throws IndexUpdaterException;
    void updateLastModifiedTime(FileTime lastModifiedTime);
    void updateFile(FileModel fileModel);
    void updateLastScanId(long scanId);
    void removeUnscanned(long scanId);
}
