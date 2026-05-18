package org.example.filebrowser.crawler.strategy;

import org.example.filebrowser.model.index.FileModel;
import org.example.filebrowser.model.index.ImageFileModel;
import org.example.filebrowser.utils.exceptions.CrawlerException;

public class ImageFileStrategy implements ContentInspectStrategy {

    @Override
    public ImageFileModel getSpecificFileModel(FileModel fileModel) throws CrawlerException {
        if (!fileModel.isReadAccess()) {
            return new ImageFileModel(fileModel, null);
        }

        return new ImageFileModel(fileModel, "dummy");
    }
}
