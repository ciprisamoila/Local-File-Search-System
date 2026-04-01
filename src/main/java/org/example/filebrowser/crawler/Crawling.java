package org.example.filebrowser.crawler;

import org.example.filebrowser.utils.exceptions.IndexUpdaterException;

public interface Crawling {
    void crawl() throws IndexUpdaterException;
}
