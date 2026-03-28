package org.example.filebrowser.crawler;

import org.example.filebrowser.utils.CrawlConfig;

public class FileCrawlerManager implements Crawling {
    public void crawl() {
        CrawlConfig config = CrawlConfig.readConfigFromFileNoCreation();

        FileCrawler fileCrawler = new FileCrawler(config);
        fileCrawler.run();
    }

    public static void main(String[] args) {
        FileCrawlerManager fileCrawlerManager = new FileCrawlerManager();
        fileCrawlerManager.crawl();
    }
}
