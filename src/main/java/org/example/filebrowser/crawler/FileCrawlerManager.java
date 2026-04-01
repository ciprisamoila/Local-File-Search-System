package org.example.filebrowser.crawler;

import org.example.filebrowser.utils.CrawlConfig;
import org.example.filebrowser.utils.exceptions.IndexUpdaterException;

public class FileCrawlerManager implements Crawling {
    public void crawl() throws IndexUpdaterException {
        CrawlConfig config = CrawlConfig.readConfigFromFileNoCreation();

        FileCrawler fileCrawler = new FileCrawler(config);
        Thread t = new Thread(fileCrawler);
        t.start();
    }

    public static void main(String[] args) {
        FileCrawlerManager fileCrawlerManager = new FileCrawlerManager();
        try {
            fileCrawlerManager.crawl();
        } catch (IndexUpdaterException e) {
            //TODO handle exception
            throw new RuntimeException(e);
        }
    }
}
