package org.example.filebrowser.querymanager;

import org.example.filebrowser.model.QueryFileModel;
import org.example.filebrowser.querylogic.parser.Expr;
import org.example.filebrowser.utils.PgUtils;
import org.example.filebrowser.utils.exceptions.ParserException;
import org.example.filebrowser.utils.exceptions.QueryManagerException;
import org.example.filebrowser.utils.exceptions.TableDoesNotExist;
import org.json.JSONException;

import java.io.FileNotFoundException;
import java.sql.*;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Properties;
import java.util.logging.Level;
import java.util.logging.Logger;

public class PgQuerier implements IDatabaseQuerier {
    Logger logger = Logger.getLogger("querymanager");
    Connection conn;

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

    @Override
    public List<QueryFileModel> getNextFilesMatching(int nrFiles, int offset, Expr ast) throws QueryManagerException {
        try {
            String query;
            try {
                query = QueryBuilder.exprToSQL(ast);
            } catch (ParserException e) {
                throw new QueryManagerException(e.getMessage());
            }
            System.out.println(query);
            // 4 divides the rank by the mean harmonic distance between extents (this is implemented only by ts_rank_cd)
            PreparedStatement st = conn.prepareStatement(String.format(
                    """
                            select name || '.' || extension as full_name,
                                   path,
                                   file_creation_time,
                                   file_last_modified_time,
                                   file_last_accessed_time,
                                   size,
                                   read_access,
                                   substring(content from 0 for 32) as headline
                            from file
                            where %s
                            limit ? offset ?;
                    """, query));

            st.setInt(1, nrFiles);
            st.setInt(2, offset);

            ResultSet rs = st.executeQuery();

            List<QueryFileModel> fileList = new ArrayList<>();
            while (rs.next()) {
                fileList.add(new QueryFileModel(
                        rs.getString("full_name"),
                        rs.getString("path"),
                        rs.getString("file_creation_time"),
                        rs.getString("file_last_modified_time"),
                        rs.getString("file_last_accessed_time"),
                        rs.getLong("size"),
                        rs.getBoolean("read_access"),
                        rs.getString("headline")
                ));
            }

            return fileList;
        } catch (SQLException e) {
            logger.log(Level.WARNING, "Query failed!\n" + e.getMessage());
            throw new QueryManagerException(e.getMessage());
        }
    }

    public static void main(String[] args) throws QueryManagerException {
        PgQuerier q = new PgQuerier();

        System.out.println(q.tokenizeQuery("free of ch"));
    }
}
