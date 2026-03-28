package org.example.filebrowser.utils.exceptions;

public class CrawlerException extends Exception {
    public CrawlerException(String message) {
        super("Crawler Exception:\n" + message);
    }
}
