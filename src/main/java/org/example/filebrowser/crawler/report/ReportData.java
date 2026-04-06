package org.example.filebrowser.crawler.report;

public record ReportData(
        int nrSymlinks,
        int nrDirectoriesEntered,
        int nrFilesToInsert,
        int nrFilesToUpdate,
        int nrFilesInserted,
        int nrFilesUpdated,
        String errorMessage
) {
}
