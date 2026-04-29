package org.example.filebrowser.querylogic;

import org.example.filebrowser.model.QueryFileModel;
import org.example.filebrowser.model.QuerySpecs;
import org.example.filebrowser.model.RankingStrategy;
import org.example.filebrowser.querymanager.IDatabaseQuerier;
import org.example.filebrowser.querymanager.PgQuerier;
import org.example.filebrowser.utils.exceptions.ParserException;
import org.example.filebrowser.utils.exceptions.QueryManagerException;

import java.util.List;

public class QueryParser implements IQuerier {
    IDatabaseQuerier databaseQuerier;

    public QueryParser() throws QueryManagerException {
        this.databaseQuerier = new PgQuerier();
    }

    @Override
    public List<QueryFileModel> getNextFilesMatching(QuerySpecs querySpecs, String query, boolean isUnderTest) throws QueryManagerException {
        try {
            // sanitize query by eliminating "'"
            Parser parser = new Parser(new Lexer(query.replace("'", "")));
            return databaseQuerier.getNextFilesMatching(querySpecs, query, parser.parseExpression(), isUnderTest);
        } catch (ParserException e) {
            throw new QueryManagerException(e.getMessage());
        }
    }

    @Override
    public List<String> getQueryHistory(int nrQueries, String query) {
        return databaseQuerier.getQueryHistory(nrQueries, query);
    }
}
