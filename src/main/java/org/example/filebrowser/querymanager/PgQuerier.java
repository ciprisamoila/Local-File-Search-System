package org.example.filebrowser.querymanager;

import org.example.filebrowser.model.QueryFileModel;
import org.example.filebrowser.model.QuerySpecs;
import org.example.filebrowser.model.RankingStrategy;
import org.example.filebrowser.model.index.FileType;
import org.example.filebrowser.querylogic.parser.Lexer;
import org.example.filebrowser.querylogic.parser.Parser;
import org.example.filebrowser.querylogic.parser.expression.Expr;
import org.example.filebrowser.querymanager.decorator.*;
import org.example.filebrowser.querymanager.decorator.dictionary.ISynonymDictionary;
import org.example.filebrowser.querymanager.decorator.dictionary.MapDictionary;
import org.example.filebrowser.querymanager.tracking.Observation;
import org.example.filebrowser.querymanager.tracking.ObservedSubject;
import org.example.filebrowser.querymanager.tracking.Observer;
import org.example.filebrowser.querymanager.tracking.SearchTracker;
import org.example.filebrowser.utils.PgUtils;
import org.example.filebrowser.utils.exceptions.ParserException;
import org.example.filebrowser.utils.exceptions.QueryManagerException;
import org.example.filebrowser.utils.exceptions.TableDoesNotExist;
import org.json.JSONException;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.sql.*;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Properties;
import java.util.logging.Level;
import java.util.logging.Logger;

public class PgQuerier implements IDatabaseQuerier, ObservedSubject {
    Logger logger = Logger.getLogger("querymanager");
    Connection conn;
    private final List<Observer> observers = new ArrayList<>();

    @Override
    public void addObserver(Observer o) {
        observers.add(o);
    }

    @Override
    public void removeObserver(Observer o) {
        observers.remove(o);
    }

    @Override
    public void notifyObservers(Observation observation) {
        for (Observer o : observers) {
            o.update(observation);
        }
    }

    private void executeFromFile(String filename) throws SQLException {
        try {
            Statement st = conn.createStatement();
            BufferedReader br = new BufferedReader(new FileReader("src/main/java/org/example/filebrowser/querymanager/sql/" + filename));

            StringBuilder query = new StringBuilder();
            String line;
            while ((line = br.readLine()) != null) {
                // skip comments
                if (line.trim().startsWith("--")) {
                    continue;
                }

                query.append(line.trim()).append(" ");

                if (line.trim().endsWith(";")) {
                    st.execute(query.toString());
                    query = new StringBuilder();
                }
            }
        } catch (IOException e) {
            throw new SQLException(e);
        }
    }

    public PgQuerier() throws QueryManagerException {
        String url = "jdbc:postgresql://localhost:5432/filebrowser";

        try {
            Properties props = PgUtils.getCredentialsFromFile("./credentials.json");

            conn = DriverManager.getConnection(url, props);

            // checking if table FILE exists
            DatabaseMetaData metaData = conn.getMetaData();
            ResultSet rs = metaData.getTables("filebrowser", "public", "file", null);

            if (!rs.first()) {
                // the table does not exist
                throw new TableDoesNotExist("The table does not exist");
            }

            // create if not exist table QUERY and SEARCHED_FILE
            rs = metaData.getTables("filebrowser", "public", "query", null);
            if (!rs.first()) {
                executeFromFile("create_table_query.sql");
            }
            rs = metaData.getTables("filebrowser", "public", "searched_file", null);
            if (!rs.first()) {
                executeFromFile("create_table_searched_file.sql");
            }

            addObserver(new SearchTracker(url, props));
        } catch (SQLException | FileNotFoundException | JSONException e) {
            logger.log(Level.SEVERE, e.getMessage());
            throw new QueryManagerException(e.getMessage());
        }
    }

    public String tokenizeQuery(String query) {
        String[] tokens = query.split(" ");
        return Arrays.stream(tokens)
                .reduce((t1, t2) -> t1 + " & " + t2)
                .map(s -> s + ":*")
                .orElse("");
    }

    private QueryBuilder queryBuilderFactory() {
        ISynonymDictionary synonymDictionary = new MapDictionary();
        IQueryBuilder contentQueryBuilder = new LogicDecorator(
                new SynonymDecorator(
                    new SanitizationDecorator(
                            new BaseQueryBuilder()
                    ),
                    synonymDictionary
                )
        );

        return new QueryBuilder(contentQueryBuilder);
    }

    private String buildFullQuery(Expr ast, QuerySpecs querySpecs) throws QueryManagerException {
        StringBuilder query = new StringBuilder();

        // SELECT clause
        query.append("""
SELECT name COLLATE "C" || '.' || extension AS full_name,
       path,
       file_creation_time,
       file_last_modified_time,
       file_last_accessed_time,
       size,
       read_access,
       SUBSTRING(content FROM 0 FOR 32) AS headline,
       type,
       id
FROM   file
        """);

        // JOIN clause
        query.append("LEFT JOIN text_file ON file.id = text_file.file_id").append("\n");
        query.append("LEFT JOIN image_file ON file.id = image_file.file_id").append("\n");
        boolean frequencyOrder = querySpecs.rankingStrategy() == RankingStrategy.FREQUENCY;
        if (frequencyOrder) {
            query.append("JOIN searched_file ON file.id = searched_file.file_id").append('\n');
        }

        // WHERE clause
        String whereClause;
        QueryBuilder queryBuilder = queryBuilderFactory();
        try {
            whereClause = queryBuilder.exprToSQL(ast);
        } catch (ParserException e) {
            throw new QueryManagerException(e.getMessage());
        }
        if (!whereClause.isEmpty()) {
            query.append("WHERE ").append(whereClause).append("\n");
        }

        // ORDER BY clause
        String rankColumn = switch(querySpecs.rankingStrategy()) {
            case RELEVANCE -> "score";
            case ALPHABETICAL -> "full_name";
            case DATE_ACCESSED -> "file_last_accessed_time";
            case FREQUENCY -> "searched_file.nr_searches";
        };
        query.append("ORDER BY ").append(rankColumn);
        if (!querySpecs.increasing()) {
            query.append(" DESC");
        }
        query.append("\n");

        // LIMIT clause
        query.append("LIMIT ").append(querySpecs.nrFiles()).append('\n');
        query.append("OFFSET ").append(querySpecs.offset()).append('\n');

        return query.toString();
    }

    @Override
    public List<QueryFileModel> getNextFilesMatching(QuerySpecs querySpecs, String originalQuery, Expr ast, boolean isUnderTest) throws QueryManagerException {
        try {
            PreparedStatement preparedStatement = conn.prepareStatement(buildFullQuery(ast, querySpecs));

            if (!isUnderTest) {
                System.out.println("Query: \n" + preparedStatement);
            }

            ResultSet resultSet = preparedStatement.executeQuery();

            List<QueryFileModel> fileList = new ArrayList<>();
            List<Long> searchFileIds = new ArrayList<>();
            while (resultSet.next()) {
                fileList.add(new QueryFileModel(
                        resultSet.getString("full_name"),
                        resultSet.getString("path"),
                        resultSet.getString("file_creation_time"),
                        resultSet.getString("file_last_modified_time"),
                        resultSet.getString("file_last_accessed_time"),
                        resultSet.getLong("size"),
                        resultSet.getBoolean("read_access"),
                        resultSet.getString("headline"),
                        FileType.valueOf(resultSet.getString("type"))
                ));
                searchFileIds.add(resultSet.getLong("id"));
            }

            if (!isUnderTest) {
                notifyObservers(new Observation(
                        searchFileIds,
                        originalQuery
                ));
            }

            return fileList;
        } catch (SQLException e) {
            logger.log(Level.WARNING, "Query failed!\n" + e.getMessage());
            throw new QueryManagerException(e.getMessage());
        }
    }

    @Override
    public List<String> getQueryHistory(int nrQueries, String query) {
        try {
            PreparedStatement preparedStatement = conn.prepareStatement(
                    "SELECT query FROM query " +
                            "WHERE query LIKE CONCAT(?, '%')" +
                            "ORDER BY last_used_at DESC " +
                            "LIMIT ?"
            );

            preparedStatement.setString(1, query);
            preparedStatement.setInt(2, nrQueries);
            ResultSet rs = preparedStatement.executeQuery();
            List<String> queryHistory = new ArrayList<>();
            while (rs.next()) {
                queryHistory.add(rs.getString("query"));
            }
            return queryHistory;

        } catch (SQLException e) {
            logger.log(Level.WARNING, "Query history failed!\n" + e.getMessage());
        }
        return null;
    }

    public static void main(String[] args) throws QueryManagerException {
        PgQuerier q = new PgQuerier();

        String input = "created:<=2007-10-3T13:00:00 OR modified:2007-10-3T13:00:00..2007-10-13 OR accessed:2007-10-3 AND color:white";
        Lexer lexer = new Lexer(input);
        Parser parser = new Parser(lexer);
        Expr ast = parser.parseExpression();

        System.out.println(q.buildFullQuery(ast, new QuerySpecs(100, 0, RankingStrategy.ALPHABETICAL, true)));
    }
}
