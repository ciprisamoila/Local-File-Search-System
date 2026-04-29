package org.example.filebrowser.querylogic;

import org.example.filebrowser.model.QueryFileModel;
import org.example.filebrowser.model.QuerySpecs;
import org.example.filebrowser.model.RankingStrategy;
import org.example.filebrowser.utils.exceptions.QueryManagerException;

import java.util.List;

public interface IQuerier {
    List<QueryFileModel> getNextFilesMatching(QuerySpecs querySpecs, String query, boolean isUnderTest) throws QueryManagerException;
    List<String> getQueryHistory(int nrQueries, String query);
}
