package org.example.filebrowser.crawler;

import org.example.filebrowser.indexupdater.IUpdater;
import org.example.filebrowser.indexupdater.PgUpdater;
import org.example.filebrowser.model.FileAttributes;
import org.example.filebrowser.model.FileModel;
import org.example.filebrowser.model.UpdateValidationData;
import org.example.filebrowser.utils.CrawlConfig;
import org.example.filebrowser.utils.exceptions.CrawlerException;
import org.example.filebrowser.utils.exceptions.IndexUpdaterException;

import java.io.File;
import java.util.logging.Level;
import java.util.logging.Logger;

public class FileCrawler implements Runnable {
    private final CrawlConfig config;
    private final Logger logger = Logger.getLogger("crawler");
    private final FileInspector fileInspector;
    private final FileChecker fileChecker;
    private final IUpdater filePersistor;
    private final long scanId;

    private void recursiveTraversal(File root) throws CrawlerException, IndexUpdaterException {
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
                        UpdateValidationData validationData;
                        if ((validationData = filePersistor.searchByPath(file.getAbsolutePath())) == null) {
                            FileModel fileModel = fileInspector.getFileModel(file, fileAttributes, scanId);
                            System.out.println("Insert file");
                            filePersistor.insert(fileModel);
                        } else if (fileChecker.modifiedTimeHasBeenModified(validationData.lastModifiedTime(), fileAttributes.lastModifiedTime())) {
                            FileModel fileModel = fileInspector.getFileModel(file, fileAttributes, scanId);
                            if (fileChecker.checksumHasBeenModified(validationData.checksumValue(), fileModel.checksumValue())) {
                                System.out.println("Update file");
                                filePersistor.updateFile(validationData.id(), fileModel);
                            } else {
                                System.out.println("Update just date and scanId");
                                filePersistor.updateLastModifiedTime(validationData.id(), fileAttributes.lastModifiedTime());
                                filePersistor.updateLastScanId(validationData.id(), scanId);
                            }
                        } else if (fileChecker.readingRightsHaveBeenModified(validationData.readAccess(), fileInspector.canRead(file))) {
                            FileModel fileModel = fileInspector.getFileModel(file, fileAttributes, scanId);
                            System.out.println("reading rights changed!!!");
                            filePersistor.updateFile(validationData.id(), fileModel);
                        } else {
                            System.out.println("Update just scanId");
                            filePersistor.updateLastScanId(validationData.id(), scanId);
                        }
                    }
                }
            }
        }
    }

    private void initTraversal() throws CrawlerException, IndexUpdaterException {
        File root = new File(config.root());

        recursiveTraversal(root);

        filePersistor.removeUnscanned(scanId);
    }

    private void makeReport(){

    }
    public FileCrawler(CrawlConfig config) throws IndexUpdaterException {
        this.config = config;
        this.scanId = System.currentTimeMillis();
        this.fileInspector = new FileInspector();
        this.fileChecker = new FileChecker();
        this.filePersistor = new PgUpdater();
    }
    @Override
    public void run() {
        try {
            initTraversal();
        } catch (CrawlerException | IndexUpdaterException e) {
            throw new RuntimeException(e); //? e bine ce fac aici?
            //TODO: sa scriu in report despre exceptii, in rest le ignor
        }

        makeReport();
    }
}
