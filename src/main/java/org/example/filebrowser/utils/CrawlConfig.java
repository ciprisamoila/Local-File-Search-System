package org.example.filebrowser.utils;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Scanner;
import java.util.logging.Level;
import java.util.logging.Logger;

public record CrawlConfig(String root, ReportType reportType, String[] fileTypes, double maxFileSize) {
    private static final String configFilePath = "./crawlconfig.json";

    private static CrawlConfig computeDefaultConfig() {
        String root = System.getProperty("user.dir");
        return new CrawlConfig(root, ReportType.TEXT, new String[]{".txt", ".cpp", ".c"}, 1);
    }
    private static final CrawlConfig defaultConfig = computeDefaultConfig();


    public static CrawlConfig readConfigFromFile() {
        Logger logger = Logger.getLogger("utils");

        File configFile = new File(configFilePath);

        try (Scanner fileReader = new Scanner(configFile)) {
            StringBuilder jsonString = new StringBuilder();
            while (fileReader.hasNextLine()) {
                String data = fileReader.nextLine();
                jsonString.append(data);
            }

            // parsing json
            JSONObject jo = new JSONObject(jsonString.toString());

            String root = jo.getString("root");

            ReportType reportType =
                    ReportType.valueOf(jo.getString("reportType"));

            JSONArray arr = jo.getJSONArray("fileTypes");
            String[] fileTypes = new String[arr.length()];
            for (int i = 0; i < arr.length(); i++) {
                fileTypes[i] = arr.getString(i);
            }

            double maxFileSize = jo.getDouble("maxFileSize");

            return new CrawlConfig(root, reportType, fileTypes, maxFileSize);

        } catch (FileNotFoundException e) {
            // if no file found, create one with default configs
            writeConfigToFile(defaultConfig);

            return defaultConfig;
        } catch (Exception e) {
            logger.log(Level.WARNING, e.getMessage());
            throw new RuntimeException(e);
        }
    }

    public static void writeConfigToFile(CrawlConfig crawlConfig) {
        Logger logger = Logger.getLogger("utils");

        JSONObject jo = new JSONObject();

        jo.put("root", crawlConfig.root);
        jo.put("reportType", crawlConfig.reportType);
        jo.put("fileTypes", new JSONArray(crawlConfig.fileTypes));
        jo.put("maxFileSize", crawlConfig.maxFileSize);

        // the file will be closed automatically
        try (FileWriter configFile = new FileWriter(configFilePath)){
            configFile.write(jo.toString());
        } catch (IOException e) {
            logger.log(Level.WARNING, e.getMessage());
        }
    }



    public static void main(String[] args) {
        CrawlConfig cf = readConfigFromFile();

        writeConfigToFile(cf);
    }
}
