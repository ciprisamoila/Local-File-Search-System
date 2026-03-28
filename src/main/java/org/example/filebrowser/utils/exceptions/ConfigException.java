package org.example.filebrowser.utils.exceptions;

public class ConfigException extends Exception {
    public ConfigException(String message) {
        super("Config Exception:\n" + message);
    }
}
