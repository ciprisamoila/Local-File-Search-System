package org.example.filebrowser.crawler.strategy;

import org.example.filebrowser.model.index.FileModel;
import org.example.filebrowser.model.index.TextFileModel;
import org.example.filebrowser.utils.exceptions.CrawlerException;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;

public class TextFileStrategy implements ContentInspectStrategy {

    @Override
    public TextFileModel getSpecificFileModel(FileModel fileModel) throws CrawlerException {
        if (!fileModel.isReadAccess()) {
            return new TextFileModel(fileModel, null);
        }
        try {
            BufferedReader br = new BufferedReader(new FileReader(fileModel.getFileAttributes().path()));
            String line;
            StringBuilder content = new StringBuilder();
            while ((line = br.readLine()) != null) {
                content.append(line);
                content.append('\n');
            }

            return new TextFileModel(fileModel, content.toString());
        } catch (IOException e) {
            throw new CrawlerException(e.getMessage());
        }
    }
}
