package org.example.filebrowser.crawler;

import org.example.filebrowser.model.index.FileAttributes;
import org.example.filebrowser.model.index.FileModel;
import org.example.filebrowser.model.index.FileType;
import org.example.filebrowser.model.index.UpdateValidationData;
import org.example.filebrowser.model.queue.ConcurrentQueue;
import org.example.filebrowser.model.queue.MessageType;
import org.example.filebrowser.model.queue.QueueMessage;
import org.example.filebrowser.model.queue.payloads.InsertPayload;
import org.example.filebrowser.model.queue.payloads.SearchPayload;
import org.example.filebrowser.model.queue.payloads.UpdateFilePayload;
import org.example.filebrowser.model.queue.payloads.UpdateIdPayload;
import org.example.filebrowser.utils.CrawlConfig;
import org.example.filebrowser.utils.exceptions.CrawlerException;

import java.io.File;
import java.util.concurrent.Callable;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.atomic.AtomicInteger;

public class FileProcessor implements Callable<Void> {

    private final ConcurrentQueue<QueueMessage> queue;

    private final FileInspector fileInspector;
    private final FileChecker fileChecker;
    private final CrawlConfig config;
    private final File file;
    private final long scanId;

    private final AtomicInteger nrFilesToInsert;
    private final AtomicInteger nrFilesInserted;
    private final AtomicInteger nrFilesToUpdate;
    private final AtomicInteger nrFilesUpdated;

    private void updateFile(long fileId, FileModel fileModel) {
        System.out.println("[PRODUCER] Update file " + fileModel.getFileAttributes().path());
        try {
            queue.add(new QueueMessage(
                    new UpdateFilePayload(
                            fileId,
                            fileModel
                    ),
                    MessageType.UPDATE_FILE,
                    null
            ));
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return;
        }
        nrFilesUpdated.incrementAndGet();
    }

    private void updateLastScanId(long fileId, long scanId) {
        System.out.println("[PRODUCER] Update just scanId for file " + fileId);
        try {
            queue.add(new QueueMessage(
                    new UpdateIdPayload(
                            fileId,
                            scanId
                    ),
                    MessageType.UPDATE_ID,
                    null
            ));
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    private void insert(FileModel fileModel) {
        System.out.println("[PRODUCER] Insert file " + fileModel.getFileAttributes().path());
        try {
            queue.add(new QueueMessage(
                    new InsertPayload(
                            fileModel
                    ),
                    MessageType.INSERT,
                    null
            ));
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return;
        }
        nrFilesInserted.incrementAndGet();
    }

    private UpdateValidationData searchByPath(String path) throws CrawlerException {
        System.out.println("[PRODUCER] Search for path " + path);
        CompletableFuture<UpdateValidationData> future = new CompletableFuture<>();
        try {
            queue.add(new QueueMessage(
                    new SearchPayload(
                            path
                    ),
                    MessageType.SEARCH,
                    future
            ));

            return future.get();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            future.completeExceptionally(e);
            throw new CrawlerException("Thread interrupted");
        } catch (ExecutionException e) {
            throw new CrawlerException(e.getMessage());
        }
    }

    public FileProcessor(ConcurrentQueue<QueueMessage> queue, FileChecker fileChecker, CrawlConfig config, File file, long scanId,
                         AtomicInteger nrFilesToInsert, AtomicInteger nrFilesToUpdate, AtomicInteger nrFilesInserted, AtomicInteger nrFilesUpdated) {
        this.queue = queue;

        this.fileInspector = new FileInspector();
        this.fileChecker = fileChecker;
        this.file = file;
        this.config = config;
        this.scanId = scanId;

        this.nrFilesToInsert = nrFilesToInsert;
        this.nrFilesInserted = nrFilesInserted;
        this.nrFilesToUpdate = nrFilesToUpdate;
        this.nrFilesUpdated = nrFilesUpdated;
    }

    @Override
    public Void call() throws CrawlerException {
        // last accessed time must remain unchanged by content detection
        FileAttributes fileAttributes = fileInspector.getFileAttributes(file);
        FileType fileType = fileInspector.getFileType(file);
        if (fileType != null) {
            fileInspector.setStrategy(fileType);
            if (fileInspector.verifiesConfig(fileAttributes, config)) {
                UpdateValidationData validationData = searchByPath(file.getAbsolutePath().replace('\\', '/'));
                FileModel fileModel = fileInspector.getFileModel(file, fileAttributes, scanId, fileType);

                if (validationData == null) {
                    nrFilesToInsert.incrementAndGet();
                    insert(fileModel);
                } else {
                    if (fileChecker.checksumHasBeenModified(validationData.checksumValue(), fileModel.getChecksumValue())) {
                        nrFilesToUpdate.incrementAndGet();
                        updateFile(validationData.id(), fileModel);
                    } else {
                        updateLastScanId(validationData.id(), scanId);
                    }
                }
            }
        }

        return null;
    }
}
