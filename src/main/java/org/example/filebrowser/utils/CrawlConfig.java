package org.example.filebrowser.utils;

import org.example.filebrowser.utils.exceptions.ConfigException;
import org.json.JSONArray;
import org.json.JSONObject;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Scanner;
import java.util.logging.Level;
import java.util.logging.Logger;

public record CrawlConfig(
        String root,
        ReportType reportType,
        String[] fileTypes,
        long maxFileSize
) {
    private static final String configFilePath = "./crawlconfig.json";

    private static CrawlConfig computeDefaultConfig() {
        String root = System.getProperty("user.dir");
        return new CrawlConfig(root, ReportType.TEXT, new String[]{"txt", "cpp", "c"}, 100_000);
    }
    private static final CrawlConfig defaultConfig = computeDefaultConfig();

    private static CrawlConfig readConfigFromScanner(Scanner fileReader) {
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

        long maxFileSize = jo.getLong("maxFileSize");

        return new CrawlConfig(root, reportType, fileTypes, maxFileSize);
    }

    public static CrawlConfig readConfigFromFileNoCreation() {
        Logger logger = Logger.getLogger("utils");

        File configFile = new File(configFilePath);

        try (Scanner fileReader = new Scanner(configFile)) {
            return readConfigFromScanner(fileReader);

        } catch (FileNotFoundException e) {
            // if no file found, return default configs
            logger.log(Level.WARNING, e.getMessage());

            return defaultConfig;
        }
    }

    public static CrawlConfig readConfigFromFileWithCreation() throws ConfigException {

        File configFile = new File(configFilePath);

        try (Scanner fileReader = new Scanner(configFile)) {
            return readConfigFromScanner(fileReader);

        } catch (FileNotFoundException e) {
            // if no file found, return default configs
            writeConfigToFile(defaultConfig);

            return defaultConfig;
        }
    }

    public static void writeConfigToFile(CrawlConfig crawlConfig) throws ConfigException {
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

            throw new ConfigException(e.getMessage());
        }
    }



    public static void main(String[] args) {
        CrawlConfig cf = readConfigFromFileNoCreation();

        System.out.println(cf);
    }
}
