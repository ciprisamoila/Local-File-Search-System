package org.example.filebrowser.querymanager.decorator;

public class BaseQueryBuilder implements IQueryBuilder {
    @Override
    public String buildQuery(String input) {
        return input;
    }
}
