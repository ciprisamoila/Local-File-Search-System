package org.example.filebrowser.querymanager;

import org.example.filebrowser.model.QueryFileModel;
import org.example.filebrowser.model.QuerySpecs;
import org.example.filebrowser.querylogic.parser.Expr;
import org.example.filebrowser.utils.exceptions.QueryManagerException;

import java.util.List;

public interface IDatabaseQuerier {
    List<QueryFileModel> getNextFilesMatching(QuerySpecs querySpecs, Expr ast) throws QueryManagerException;
}
