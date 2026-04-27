package org.example.filebrowser.querylogic;

import org.example.filebrowser.model.QueryFileModel;
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
    public List<QueryFileModel> getNextFilesMatching(int nrFiles, int offset, String query) throws QueryManagerException {
        try {
            // sanitize query by eliminating "'"
            Parser parser = new Parser(new Lexer(query.replace("'", "")));
            return databaseQuerier.getNextFilesMatching(nrFiles, offset, parser.parseExpression());
        } catch (ParserException e) {
            throw new QueryManagerException(e.getMessage());
        }
    }
}
