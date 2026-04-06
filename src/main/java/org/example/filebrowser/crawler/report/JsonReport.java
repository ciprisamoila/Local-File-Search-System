package org.example.filebrowser.crawler.report;

import org.json.JSONObject;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;

public class JsonReport implements IReport {
    Logger logger = Logger.getLogger("report");

    @Override
    public void makeReport(ReportData reportData) {
        try (BufferedWriter writer = new BufferedWriter(new FileWriter("./reports/report.json"))) {

            JSONObject json = new JSONObject();

            json.put("nrSymlinks", reportData.nrSymlinks());
            json.put("nrDirectoriesEntered", reportData.nrDirectoriesEntered());
            json.put("nrFilesToInsert", reportData.nrFilesToInsert());
            json.put("nrFilesInserted", reportData.nrFilesInserted());
            json.put("nrFilesToUpdate", reportData.nrFilesToUpdate());
            json.put("nrFilesUpdated", reportData.nrFilesUpdated());

            if (!reportData.errorMessage().isEmpty()) {
                json.put("errorMessage", reportData.errorMessage());
            }

            writer.write(json.toString(4)); // pretty print with indent = 4

        } catch (IOException e) {
            logger.log(Level.WARNING, e.getMessage());
        }
    }
}