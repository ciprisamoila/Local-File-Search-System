package org.example.filebrowser.querymanager;

import org.example.filebrowser.model.QueryResponse;
import org.example.filebrowser.model.QuerySpecs;
import org.example.filebrowser.querylogic.parser.expression.Expr;
import org.example.filebrowser.utils.exceptions.QueryManagerException;

import java.util.List;

public interface IDatabaseQuerier {
    QueryResponse getNextFilesMatching(QuerySpecs querySpecs, String initialQuery, Expr ast, boolean isUnderTest) throws QueryManagerException;
    List<String> getQueryHistory(int nrQueries, String query);
}
