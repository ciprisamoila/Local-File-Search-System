package org.example.filebrowser.utils.exceptions;

public class QueryManagerException extends Exception {
    public QueryManagerException(String message) {
        super("Query manager: " + message);
    }
}
