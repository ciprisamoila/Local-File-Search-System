package org.example.filebrowser.querymanager.decorator;

public abstract class QueryDecorator implements IQueryBuilder {
    protected final IQueryBuilder wrapped;

    public QueryDecorator(IQueryBuilder wrapped) {
        this.wrapped = wrapped;
    }
}
