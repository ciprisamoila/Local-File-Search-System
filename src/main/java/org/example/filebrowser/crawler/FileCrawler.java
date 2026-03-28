package org.example.filebrowser.crawler;

import org.example.filebrowser.model.FileAttributes;
import org.example.filebrowser.utils.CrawlConfig;
import org.example.filebrowser.utils.exceptions.CrawlerException;

import java.io.File;
import java.util.logging.Level;
import java.util.logging.Logger;

public class FileCrawler implements Runnable {
    private final CrawlConfig config;
    private final Logger logger = Logger.getLogger("crawler");
    private final FileInspector fileInspector;

    private void recursiveTraversal(File root) throws CrawlerException {
        File[] files = root.listFiles();

        if (files == null) {
            // we suppose we get in the config file an existing directory
            // if it is not traversable, we index nothing
            return;
        }

        for (File file : files) {
            // we are ignoring symbolic links
            if (fileInspector.isSymbolicLink(file)) {
                continue;
            }
            if (file.isDirectory()) {
                recursiveTraversal(file);
            } else if (!file.isDirectory()) {
                if (fileInspector.isTextFile(file)) {
                    FileAttributes fileAttributes = fileInspector.getFileAttributes(file);

                    if (fileInspector.verifiesConfig(fileAttributes, config)) {
                        System.out.println(fileAttributes);
                    }

                }
            }
        }
    }

    private void initTraversal() throws CrawlerException {

        File root = new File(config.root());

        recursiveTraversal(root);
    }

    private void makeReport(){

    }
    public FileCrawler(CrawlConfig config) {
        this.config = config;
        this.fileInspector = new FileInspector();
    }
    @Override
    public void run() {
        try {
            initTraversal();
        } catch (CrawlerException e) {
            throw new RuntimeException(e); //? e bine ce fac aici?
        }

        makeReport();
    }
}
