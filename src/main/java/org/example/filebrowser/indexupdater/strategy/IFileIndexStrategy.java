package org.example.filebrowser.indexupdater.strategy;

import org.example.filebrowser.model.index.FileModel;
import org.example.filebrowser.utils.exceptions.IndexUpdaterException;

import java.sql.Connection;

public interface IFileIndexStrategy {
    void insertSpecificData(Connection conn, long fileId, FileModel fileModel) throws IndexUpdaterException;
    void updateSpecificData(Connection conn, long fileId, FileModel fileModel) throws IndexUpdaterException;
}
