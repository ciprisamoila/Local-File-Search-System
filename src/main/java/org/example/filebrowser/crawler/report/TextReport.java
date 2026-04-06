package org.example.filebrowser.crawler.report;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;

public class TextReport implements IReport {
    Logger logger = Logger.getLogger("report");
    @Override
    public void makeReport(ReportData reportData) {
        try {
            BufferedWriter writer = new BufferedWriter(new FileWriter("./reports/report.txt"));

            writer.write("Number of symlinks: ");
            writer.write(Integer.toString(reportData.nrSymlinks()));
            writer.newLine();

            writer.write("Number of directories entered: ");
            writer.write(Integer.toString(reportData.nrDirectoriesEntered()));
            writer.newLine();

            writer.write("Number of files to insert: ");
            writer.write(Integer.toString(reportData.nrFilesToInsert()));
            writer.newLine();

            writer.write("Number of inserted files: ");
            writer.write(Integer.toString(reportData.nrFilesInserted()));
            writer.newLine();

            writer.write("Number of files to update: ");
            writer.write(Integer.toString(reportData.nrFilesToUpdate()));
            writer.newLine();

            writer.write("Number of updated files: ");
            writer.write(Integer.toString(reportData.nrFilesUpdated()));
            writer.newLine();

            if (!reportData.errorMessage().isEmpty()) {
                writer.write("Error message: ");
                writer.write(reportData.errorMessage());
            }

            writer.close();

        } catch (IOException e) {
            logger.log(Level.WARNING, e.getMessage());
        }
    }
}
