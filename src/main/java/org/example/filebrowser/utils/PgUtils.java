package org.example.filebrowser.utils;

import org.example.filebrowser.utils.exceptions.IndexUpdaterException;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.io.FileNotFoundException;
import java.util.Properties;
import java.util.Scanner;
import java.util.logging.Level;

public class PgUtils {
    static public Properties getCredentialsFromFile(String path) throws FileNotFoundException, JSONException {
        // read credentials in JSON format from credentials.json
        Scanner fileReader = null;
        fileReader = new Scanner(new File(path));

        StringBuilder jsonString = new StringBuilder();
        while (fileReader.hasNextLine()) {
            String data = fileReader.nextLine();
            jsonString.append(data);
        }

        // parsing json
        JSONObject jo = new JSONObject(jsonString.toString());

        Properties props = new Properties();
        props.setProperty("user", jo.getString("user"));
        props.setProperty("password", jo.getString("password"));

        return props;
    }
}
