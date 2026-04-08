package org.example.filebrowser.querymanager;

import org.example.filebrowser.model.QueryFileModel;
import org.example.filebrowser.utils.PgUtils;
import org.example.filebrowser.utils.exceptions.QueryManagerException;
import org.json.JSONException;

import java.io.FileNotFoundException;
import java.sql.*;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Properties;
import java.util.logging.Level;
import java.util.logging.Logger;

public class PgQuerier implements IQuerier {
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
                throw new SQLException("The table does not exist");
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
    public List<QueryFileModel> getNextFilesMatching(int nrFiles, int offset, String query) throws QueryManagerException {
        try {
            // 4 divides the rank by the mean harmonic distance between extents (this is implemented only by ts_rank_cd)
            PreparedStatement st = conn.prepareStatement(
                    """
                            with query as (
                                select to_tsquery('english', ?) as q
                            )
                            select f.name || '.' || f.extension as full_name,
                                   f.path,
                                   f.read_access,
                                   ts_headline('english', coalesce(f.content, ''), q.q) as headline
                            from file f, query q
                            where f.ts @@ q.q
                            order by ts_rank_cd(f.ts, q.q, 4) desc
                            limit ? offset ?;
                    """);

            st.setString(1, tokenizeQuery(query));
            st.setInt(2, nrFiles);
            st.setInt(3, offset);

            ResultSet rs = st.executeQuery();

            List<QueryFileModel> fileList = new ArrayList<>();
            while (rs.next()) {
                fileList.add(new QueryFileModel(
                        rs.getString("full_name"),
                        rs.getString("path"),
                        rs.getString("headline"),
                        rs.getBoolean("read_access")
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
