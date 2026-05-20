package org.example.filebrowser.indexupdater;

import org.example.filebrowser.indexupdater.strategy.IFileIndexStrategy;
import org.example.filebrowser.indexupdater.strategy.ImageFileIndexStrategy;
import org.example.filebrowser.indexupdater.strategy.TextFileIndexStrategy;
import org.example.filebrowser.model.index.FileModel;
import org.example.filebrowser.model.index.ImageFileModel;
import org.example.filebrowser.model.index.TextFileModel;
import org.example.filebrowser.model.index.UpdateValidationData;
import org.example.filebrowser.utils.exceptions.IndexUpdaterException;

public class IndexEntryPoint implements IUpdater {

    private final PgUpdater pgUpdater;

    public IndexEntryPoint() throws IndexUpdaterException {
        this.pgUpdater = new PgUpdater();
    }

    private IFileIndexStrategy getIndexStrategy(FileModel fileModel) throws IndexUpdaterException {
        return switch (fileModel) {
            case TextFileModel ignored -> new TextFileIndexStrategy();
            case ImageFileModel ignored -> new ImageFileIndexStrategy();
            default -> throw new IndexUpdaterException("Unexpected value: " + fileModel);
        };
    }

    @Override
    public UpdateValidationData searchByPath(String path) throws IndexUpdaterException {
        return pgUpdater.searchByPath(path);
    }

    @Override
    public void insert(FileModel fileModel) throws IndexUpdaterException {
        pgUpdater.setFileIndexStrategy(getIndexStrategy(fileModel));
        pgUpdater.insert(fileModel);
    }

    @Override
    public void updateFile(long fileId, FileModel fileModel) throws IndexUpdaterException {
        pgUpdater.setFileIndexStrategy(getIndexStrategy(fileModel));
        pgUpdater.updateFile(fileId, fileModel);
    }

    @Override
    public void updateLastScanId(long fileId, long scanId) throws IndexUpdaterException {
        pgUpdater.updateLastScanId(fileId, scanId);
    }

    @Override
    public void removeUnscanned(long scanId) throws IndexUpdaterException {
        pgUpdater.removeUnscanned(scanId);
    }
}
