package org.example.filebrowser.crawler.strategy;

import org.example.filebrowser.model.index.FileModel;
import org.example.filebrowser.utils.exceptions.CrawlerException;


public interface ContentInspectStrategy {
    FileModel getSpecificFileModel(FileModel fileModel) throws CrawlerException;
}
