package org.example.filebrowser.crawler;

import org.example.filebrowser.crawler.report.ConsoleReport;
import org.example.filebrowser.crawler.report.IReport;
import org.example.filebrowser.crawler.report.JsonReport;
import org.example.filebrowser.crawler.report.TextReport;
import org.example.filebrowser.indexupdater.IUpdater;
import org.example.filebrowser.indexupdater.IndexEntryPoint;
import org.example.filebrowser.model.queue.ConcurrentQueue;
import org.example.filebrowser.model.queue.QueueMessage;
import org.example.filebrowser.utils.CrawlConfig;
import org.example.filebrowser.utils.exceptions.IndexUpdaterException;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class FileCrawlerManager implements Crawling {
    static int QUEUE_CAPACITY = 100;
    static int NO_PRODUCERS = 10;

    private void startConsumer(ConcurrentQueue<QueueMessage> queue) throws IndexUpdaterException {

        IndexEntryPoint fileConsumer = new IndexEntryPoint(queue);

        Executors.newSingleThreadExecutor().submit(fileConsumer);

        //TODO : catch exception, stop
    }

    private ExecutorService startProducers(ConcurrentQueue<QueueMessage> queue) {
        // TODO: sa schimb poate putin, sa blochez crawler-ul la un anumit nr de threaduri
        return Executors.newFixedThreadPool(NO_PRODUCERS);
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

        ExecutorService threadPool = startProducers(queue);

        FileCrawler fileCrawler = new FileCrawler(config, reporter, queue, threadPool);
        fileCrawler.run();
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
