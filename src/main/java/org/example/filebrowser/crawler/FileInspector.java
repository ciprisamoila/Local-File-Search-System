package org.example.filebrowser.crawler;

import org.example.filebrowser.model.FileAttributes;
import org.example.filebrowser.utils.CrawlConfig;
import org.example.filebrowser.utils.exceptions.CrawlerException;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.logging.Level;
import java.util.logging.Logger;

public class FileInspector {
    Logger logger = Logger.getLogger("crawler");

    private BasicFileAttributes getBasicAttributes(File file) throws CrawlerException {
        Path path = file.toPath();
        BasicFileAttributes attr;

        try {
            // we follow file symbolic links
            attr = Files.readAttributes(path, BasicFileAttributes.class);

            return attr;
        } catch (IOException e) {
            logger.log(Level.WARNING, e.getMessage());

            throw new CrawlerException(e.getMessage());
        }
    }


    public boolean isSymbolicLink(File file){

        return Files.isSymbolicLink(file.toPath());
    }

    public FileAttributes getFileAttributes(File file) throws CrawlerException {
        BasicFileAttributes attr = getBasicAttributes(file);

        // decompose file name into name without extension and extension
        String fileName = file.getName();
        String extension = "";
        int index = fileName.length() - 1;
        while (index >= 0 && fileName.charAt(index) != '.') {
            index--;
        }
        if (index >= 0) {
            extension = fileName.substring(index + 1);
            fileName = fileName.substring(0, index);
        }

        return new FileAttributes(
                fileName,
                extension,
                file.getAbsolutePath(),
                attr.creationTime(),
                attr.lastAccessTime(),
                attr.lastModifiedTime(),
                attr.size()
        );
    }

    public boolean isTextFile(File file) throws CrawlerException {
        Path path = file.toPath();

        String mimeType;
        try {
            mimeType = Files.probeContentType(path);
        } catch (IOException e) {
            throw new CrawlerException(e.getMessage());
        }

        return mimeType != null && mimeType.startsWith("text");
    }

    public boolean verifiesConfig(FileAttributes attr, CrawlConfig config) {
        if (attr.size() > config.maxFileSize())
            return false;

        for (String ext : config.fileTypes())
            if (ext.equals(attr.extension()))
                return true;

        return false;

    }
}
