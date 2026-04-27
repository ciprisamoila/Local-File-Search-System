package org.example.filebrowser.querylogic;

import org.example.filebrowser.model.QueryFileModel;
import org.example.filebrowser.utils.exceptions.QueryManagerException;

import java.util.List;

public interface IQuerier {
    List<QueryFileModel> getNextFilesMatching(int nrFiles, int offset, String query) throws QueryManagerException;
}
