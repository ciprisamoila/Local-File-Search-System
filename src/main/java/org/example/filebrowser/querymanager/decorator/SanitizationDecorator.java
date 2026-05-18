package org.example.filebrowser.querymanager.decorator;

public class SanitizationDecorator extends QueryDecorator {
    public SanitizationDecorator(IQueryBuilder wrapped) {
        super(wrapped);
    }

    @Override
    public String buildQuery(String input) {
        // first we call the wrapper method
        String query = wrapped.buildQuery(input);

        // then we add our behavior
        // we erase anything but letters, digits and spaces
        query = query.replaceAll("[^a-zA-Z0-9\\s]", "");

        // we erase all redundant spaces
        query = query.replaceAll("\\s+", " ").trim();

        return query;
    }
}
