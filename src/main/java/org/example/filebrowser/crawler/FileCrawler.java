package org.example.filebrowser.crawler;

import org.example.filebrowser.crawler.report.IReport;
import org.example.filebrowser.crawler.report.ReportData;
import org.example.filebrowser.indexupdater.IUpdater;
import org.example.filebrowser.indexupdater.PgUpdater;
import org.example.filebrowser.model.FileAttributes;
import org.example.filebrowser.model.FileModel;
import org.example.filebrowser.model.UpdateValidationData;
import org.example.filebrowser.utils.CrawlConfig;
import org.example.filebrowser.utils.exceptions.CrawlerException;
import org.example.filebrowser.utils.exceptions.IndexUpdaterException;

import java.io.File;

public class FileCrawler {
    private final CrawlConfig config;
    private final FileInspector fileInspector;
    private final FileChecker fileChecker;
    private final IUpdater filePersistor;
    private final long scanId;

    private final IReport reporter;
    private int nrSymlinks = 0;
    private int nrDirectoriesEntered = 0;
    private int nrFilesToInsert = 0;
    private int nrFilesToUpdate = 0;
    private int nrFilesInserted = 0;
    private int nrFilesUpdated = 0;
    private final StringBuilder errorMessage = new StringBuilder();

    private void updateFile(long fileId, FileModel fileModel) {
        try {
            filePersistor.updateFile(fileId, fileModel);
        } catch (IndexUpdaterException e) {
            return;
        }
        nrFilesUpdated++;
    }

    private void insert(FileModel fileModel) {
        try {
            filePersistor.insert(fileModel);
        } catch (IndexUpdaterException e) {
            return;
        }
        nrFilesInserted++;
    }

    private void recursiveTraversal(File root) throws CrawlerException, IndexUpdaterException {
        File[] files = root.listFiles();

        if (files == null) {
            // we suppose we get in the config file an existing directory
            // if it is not traversable, we index nothing
            return;
        }

        nrDirectoriesEntered++;

        for (File file : files) {
            // we are ignoring symbolic links
            if (fileInspector.isSymbolicLink(file)) {
                nrSymlinks++;
                continue;
            }
            if (file.isDirectory()) {
                recursiveTraversal(file);
            } else if (!file.isDirectory()) {
                if (fileInspector.isTextFile(file)) {
                    FileAttributes fileAttributes = fileInspector.getFileAttributes(file);

                    if (fileInspector.verifiesConfig(fileAttributes, config)) {
                        UpdateValidationData validationData = filePersistor.searchByPath(file.getAbsolutePath());
                        FileModel fileModel = fileInspector.getFileModel(file, fileAttributes, scanId);

                        if (validationData == null) {
                            System.out.println("Insert file");
                            nrFilesToInsert++;
                            insert(fileModel);
                        } else {
                            if (fileChecker.checksumHasBeenModified(validationData.checksumValue(), fileModel.checksumValue())) {
                                System.out.println("Update file");
                                nrFilesToUpdate++;
                                updateFile(validationData.id(), fileModel);
                            } else {
                                System.out.println("Update just scanId");
                                filePersistor.updateLastScanId(validationData.id(), scanId);
                            }
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

    public FileCrawler(CrawlConfig config, IReport reporter) throws IndexUpdaterException {
        this.config = config;
        this.scanId = System.currentTimeMillis();
        this.fileInspector = new FileInspector();
        this.fileChecker = new FileChecker();
        this.filePersistor = new PgUpdater();

        this.reporter = reporter;
    }

    public void run() {
        try {
            initTraversal();
        } catch (CrawlerException e) {
            errorMessage.append("Errors at crawling. See logs for details!");
        } catch (IndexUpdaterException e) {
            errorMessage.append("\n");
            errorMessage.append("Errors at index updates. See logs for details!");
        }

        reporter.makeReport(new ReportData(
                nrSymlinks,
                nrDirectoriesEntered,
                nrFilesToInsert,
                nrFilesToUpdate,
                nrFilesInserted,
                nrFilesUpdated,
                errorMessage.toString()
        ));
    }
}
