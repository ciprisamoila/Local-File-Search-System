package org.example.filebrowser.indexupdater;

import org.example.filebrowser.indexupdater.strategy.IFileIndexStrategy;
import org.example.filebrowser.indexupdater.strategy.ImageFileIndexStrategy;
import org.example.filebrowser.indexupdater.strategy.TextFileIndexStrategy;
import org.example.filebrowser.model.index.FileModel;
import org.example.filebrowser.model.index.ImageFileModel;
import org.example.filebrowser.model.index.TextFileModel;
import org.example.filebrowser.model.index.UpdateValidationData;
import org.example.filebrowser.model.queue.*;
import org.example.filebrowser.model.queue.payloads.*;
import org.example.filebrowser.utils.exceptions.IndexUpdaterException;

import java.util.concurrent.Callable;

public class IndexEntryPoint implements Callable<Void> {

    private final IUpdater pgUpdater;
    private final ConcurrentQueue<QueueMessage> queue;

    public IndexEntryPoint(ConcurrentQueue<QueueMessage> queue) throws IndexUpdaterException {
        this.pgUpdater = new PgUpdater();
        this.queue = queue;
    }

    private IFileIndexStrategy getIndexStrategy(FileModel fileModel) throws IndexUpdaterException {
        return switch (fileModel) {
            case TextFileModel ignored -> new TextFileIndexStrategy();
            case ImageFileModel ignored -> new ImageFileIndexStrategy();
            default -> throw new IndexUpdaterException("Unexpected value: " + fileModel);
        };
    }

    private void processMessage(QueueMessage queueMessage) throws IndexUpdaterException {
        switch (queueMessage.type()) {
            case SEARCH -> processSearch(queueMessage);
            case INSERT -> processInsert(queueMessage);
            case UPDATE_FILE -> processUpdateFile(queueMessage);
            case UPDATE_ID -> processUpdateLastScanId(queueMessage);
            case REMOVE_UNSCANNED -> processRemoveUnscanned(queueMessage);
        }
    }

    @Override
    public Void call() throws IndexUpdaterException {
        while (true) {
            try {
                QueueMessage queueMessage = queue.poll();

                processMessage(queueMessage);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                return null;
            }
        }
    }

    private void processSearch(QueueMessage queueMessage) throws IndexUpdaterException {
        if (!(queueMessage.payload() instanceof SearchPayload(String path))) {
            throw new IndexUpdaterException("Wrong payload type");
        }

        System.out.println("[CONSUMER] Search for path " + path);

        UpdateValidationData validationData = pgUpdater.searchByPath(path);

        queueMessage.future().complete(validationData);
    }

    private void processInsert(QueueMessage queueMessage) throws IndexUpdaterException {
        if (!(queueMessage.payload() instanceof InsertPayload(FileModel fileModel))) {
            throw new IndexUpdaterException("Wrong payload type");
        }

        System.out.println("[CONSUMER] Insert file " + fileModel.getFileAttributes().path());

        pgUpdater.setFileIndexStrategy(getIndexStrategy(fileModel));
        pgUpdater.insert(fileModel);
    }

    private void processUpdateFile(QueueMessage queueMessage) throws IndexUpdaterException {
        if (!(queueMessage.payload() instanceof UpdateFilePayload(long fileId, FileModel fileModel))) {
            throw new IndexUpdaterException("Wrong payload type");
        }

        System.out.println("[PRODUCER] Update file " + fileModel.getFileAttributes().path());

        pgUpdater.setFileIndexStrategy(getIndexStrategy(fileModel));
        pgUpdater.updateFile(fileId, fileModel);
    }

    private void processUpdateLastScanId(QueueMessage queueMessage) throws IndexUpdaterException {
        if (!(queueMessage.payload() instanceof UpdateIdPayload(long fileId, long scanId))) {
            throw new IndexUpdaterException("Wrong payload type");
        }

        System.out.println("[CONSUMER] Update just scanId for file " + fileId);

        pgUpdater.updateLastScanId(fileId, scanId);
    }

    private void processRemoveUnscanned(QueueMessage queueMessage) throws IndexUpdaterException {
        if (!(queueMessage.payload() instanceof RemoveUnscannedPayload(long scanId))) {
            throw new IndexUpdaterException("Wrong payload type");
        }

        System.out.println("[CONSUMER] Remove files with different scan id " + scanId);

        pgUpdater.removeUnscanned(scanId);
    }
}
