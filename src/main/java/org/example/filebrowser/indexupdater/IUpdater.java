package org.example.filebrowser.indexupdater;

import org.example.filebrowser.indexupdater.strategy.IFileIndexStrategy;
import org.example.filebrowser.model.index.FileModel;
import org.example.filebrowser.model.index.UpdateValidationData;
import org.example.filebrowser.utils.exceptions.IndexUpdaterException;

public interface IUpdater {
    UpdateValidationData searchByPath(String path) throws IndexUpdaterException;
    void insert(FileModel fileModel) throws IndexUpdaterException;
    void updateFile(long fileId, FileModel fileModel) throws IndexUpdaterException;
    void updateLastScanId(long fileId, long scanId) throws IndexUpdaterException;
    void removeUnscanned(long scanId) throws IndexUpdaterException;

    void setFileIndexStrategy(IFileIndexStrategy fileIndexStrategy);
}
