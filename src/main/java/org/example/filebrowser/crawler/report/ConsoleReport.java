package org.example.filebrowser.crawler.report;

public class ConsoleReport implements IReport {

    @Override
    public void makeReport(ReportData reportData) {
        System.out.println("Number of symlinks: " + reportData.nrSymlinks());
        System.out.println("Number of directories entered: " + reportData.nrDirectoriesEntered());
        System.out.println("Number of files to insert: " + reportData.nrFilesToInsert());
        System.out.println("Number of inserted files: " + reportData.nrFilesInserted());
        System.out.println("Number of files to update: " + reportData.nrFilesToUpdate());
        System.out.println("Number of updated files: " + reportData.nrFilesUpdated());

        if (!reportData.errorMessage().isEmpty()) {
            System.out.println("Error message: " + reportData.errorMessage());
        }
    }
}