package org.example.filebrowser.crawler;

import org.example.filebrowser.crawler.report.ConsoleReport;
import org.example.filebrowser.crawler.report.IReport;
import org.example.filebrowser.crawler.report.JsonReport;
import org.example.filebrowser.crawler.report.TextReport;
import org.example.filebrowser.indexupdater.IUpdater;
import org.example.filebrowser.indexupdater.PgUpdater;
import org.example.filebrowser.utils.CrawlConfig;
import org.example.filebrowser.utils.exceptions.IndexUpdaterException;

public class FileCrawlerManager implements Crawling {
    public void crawl() throws IndexUpdaterException {
        CrawlConfig config = CrawlConfig.readConfigFromFileNoCreation();

        IReport reporter = switch (config.reportType()) {
            case TEXT -> new TextReport();
            case CONSOLE -> new ConsoleReport();
            case JSON -> new JsonReport();
        };

        IUpdater filePersistor = new PgUpdater();

        FileCrawler fileCrawler = new FileCrawler(config, reporter, filePersistor);
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
