package org.example.filebrowser.crawler;

import org.example.filebrowser.crawler.report.IReport;
import org.example.filebrowser.crawler.report.ReportData;
import org.example.filebrowser.indexupdater.IUpdater;
import org.example.filebrowser.model.index.*;
import org.example.filebrowser.model.queue.ConcurrentQueue;
import org.example.filebrowser.model.queue.MessageType;
import org.example.filebrowser.model.queue.QueueMessage;
import org.example.filebrowser.model.queue.payloads.RemoveUnscannedPayload;
import org.example.filebrowser.model.queue.payloads.UpdateIdPayload;
import org.example.filebrowser.utils.CrawlConfig;
import org.example.filebrowser.utils.exceptions.CrawlerException;
import org.example.filebrowser.utils.exceptions.IndexUpdaterException;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicInteger;

public class FileCrawler {
    private final CrawlConfig config;
    private final FileInspector fileInspector;
    private final FileChecker fileChecker;
    private final ConcurrentQueue<QueueMessage> queue;
    private final long scanId;

    private final ExecutorService threadPool;

    private final IReport reporter;
    private int nrSymlinks = 0;
    private int nrDirectoriesEntered = 0;
    private final AtomicInteger nrFilesToInsert = new AtomicInteger(0);
    private final AtomicInteger nrFilesToUpdate = new AtomicInteger(0);
    private final AtomicInteger nrFilesInserted = new AtomicInteger(0);
    private final AtomicInteger nrFilesUpdated = new AtomicInteger(0);
    private final StringBuilder errorMessage = new StringBuilder();

    private final List<Future<?>> futures = new ArrayList<>();

    private void startProducer(File file) {
        Future<?> future = threadPool.submit(new FileProcessor(
                queue,
                fileInspector,
                fileChecker,
                config,
                file,
                scanId,
                nrFilesToInsert,
                nrFilesToUpdate,
                nrFilesInserted,
                nrFilesUpdated
        ));

        futures.add(future);
    }

    private void recursiveTraversal(File root) throws CrawlerException, IndexUpdaterException {
        File[] files = root.listFiles();

        if (files == null) {
            // we suppose we get in the config file an existing directory
            // if it is not traversable, we index nothing
            return;
        }

        nrDirectoriesEntered++;

        for (File file : files) {
            // we are ignoring symbolic links
            if (fileInspector.isSymbolicLink(file)) {
                nrSymlinks++;
                continue;
            }
            if (file.isDirectory()) {
                recursiveTraversal(file);
            } else {
                startProducer(file);
            }
        }
    }

    private void removeUnscanned(long scanId) {

        System.out.println("[PRODUCER] Remove files with different scan id " + scanId);

        try {
            queue.add(new QueueMessage(
                    new RemoveUnscannedPayload(
                            scanId
                    ),
                    MessageType.REMOVE_UNSCANNED,
                    null
            ));
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    private void initTraversal() throws CrawlerException, IndexUpdaterException {
        File root = new File(config.root());

        recursiveTraversal(root);

        for (Future<?> future : futures) {
            try {
                future.get();
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            } catch (ExecutionException e) {
                throw new CrawlerException(e.getMessage());
            }
        }

        removeUnscanned(scanId);
    }

    public FileCrawler(CrawlConfig config, IReport reporter, ConcurrentQueue<QueueMessage> queue, ExecutorService threadPool) throws IndexUpdaterException {
        this.config = config;
        this.scanId = System.currentTimeMillis();
        this.fileInspector = new FileInspector();
        this.fileChecker = new FileChecker();
        this.queue = queue;

        this.threadPool = threadPool;

        this.reporter = reporter;
    }

    public void run() {
        try {
            initTraversal();
        } catch (CrawlerException e) {
            errorMessage.append("Errors at crawling. See logs for details!");
        } catch (IndexUpdaterException e) {
            errorMessage.append("\n");
            errorMessage.append("Errors at index updates. See logs for details!");
        }

        reporter.makeReport(new ReportData(
                nrSymlinks,
                nrDirectoriesEntered,
                nrFilesToInsert.get(),
                nrFilesToUpdate.get(),
                nrFilesInserted.get(),
                nrFilesUpdated.get(),
                errorMessage.toString()
        ));
    }
}
