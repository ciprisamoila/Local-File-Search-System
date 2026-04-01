package org.example.filebrowser.utils.exceptions;

public class IndexUpdaterException extends Exception {
    public IndexUpdaterException(String message) {
        super("Index updater: " + message);
    }
}
