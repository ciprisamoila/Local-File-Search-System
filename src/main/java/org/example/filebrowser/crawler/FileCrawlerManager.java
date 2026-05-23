package org.example.filebrowser.crawler;

import org.example.filebrowser.crawler.report.ConsoleReport;
import org.example.filebrowser.crawler.report.IReport;
import org.example.filebrowser.crawler.report.JsonReport;
import org.example.filebrowser.crawler.report.TextReport;
import org.example.filebrowser.indexupdater.IndexEntryPoint;
import org.example.filebrowser.model.queue.ConcurrentQueue;
import org.example.filebrowser.model.queue.QueueMessage;
import org.example.filebrowser.utils.CrawlConfig;
import org.example.filebrowser.utils.exceptions.IndexUpdaterException;

import java.util.concurrent.*;

public class FileCrawlerManager implements Crawling {
    static int QUEUE_CAPACITY = 100;
    static int NO_PRODUCERS = 10;
    static int WAITING_QUEUE_CAPACITY = 100;

    private ExecutorService consumerExecutor;
    private ExecutorService producerExecutor;

    private Throwable consumerFailure;

    private void startConsumer(ConcurrentQueue<QueueMessage> queue) throws IndexUpdaterException {

        consumerExecutor = Executors.newSingleThreadExecutor();

        CompletableFuture<Void> future =
            CompletableFuture.supplyAsync(() -> {

                try {
                    return new IndexEntryPoint(queue).call();

                } catch (IndexUpdaterException e) {
                    throw new CompletionException(e);
                }

            }, consumerExecutor);

        future.whenComplete((_, throwable) -> {

            if (throwable != null) {

                consumerFailure = throwable.getCause();

                shutdownAll();

                throw new CompletionException(consumerFailure);
            }
        });
    }

    private ExecutorService startProducers(ConcurrentQueue<QueueMessage> queue) {
        int poolSize = NO_PRODUCERS;
        int queueSize = WAITING_QUEUE_CAPACITY;

        BlockingQueue<Runnable> workQueue =
                new ArrayBlockingQueue<>(queueSize);

        // when the queue with waiting tasks is full, the execution will block
        return new ThreadPoolExecutor(
                poolSize,
                poolSize,
                0L,
                TimeUnit.MILLISECONDS,
                workQueue,
                new ThreadPoolExecutor.CallerRunsPolicy()
        );
    }

    private void shutdownAll() {
        consumerExecutor.shutdownNow();
        producerExecutor.shutdownNow();
    }

    public void crawl() throws IndexUpdaterException {
        CrawlConfig config = CrawlConfig.readConfigFromFileNoCreation();

        IReport reporter = switch (config.reportType()) {
            case TEXT -> new TextReport();
            case CONSOLE -> new ConsoleReport();
            case JSON -> new JsonReport();
        };

        ConcurrentQueue<QueueMessage> queue = new ConcurrentQueue<>(FileCrawlerManager.QUEUE_CAPACITY);

        startConsumer(queue);

        producerExecutor = startProducers(queue);

        FileCrawler fileCrawler = new FileCrawler(config, reporter, queue, producerExecutor);
        fileCrawler.run();

        if (consumerFailure != null) {
            throw new IndexUpdaterException(consumerFailure.getMessage());
        }
    }

    public static void main(String[] args) {
        FileCrawlerManager fileCrawlerManager = new FileCrawlerManager();
        try {
            fileCrawlerManager.crawl();
        } catch (IndexUpdaterException e) {
            throw new RuntimeException(e);
        }
    }
}
