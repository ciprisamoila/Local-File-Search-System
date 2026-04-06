package org.example.filebrowser.indexupdater;

import org.example.filebrowser.model.FileModel;
import org.example.filebrowser.model.UpdateValidationData;
import org.example.filebrowser.utils.PgUtils;
import org.example.filebrowser.utils.exceptions.IndexUpdaterException;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.*;
import java.nio.file.attribute.FileTime;
import java.sql.*;
import java.util.Properties;
import java.util.Scanner;
import java.util.logging.Level;
import java.util.logging.Logger;

public class PgUpdater implements IUpdater{
    private final Logger logger = Logger.getLogger("indexupdater");
    private final Connection conn;

    private void executeFromFile() throws SQLException {
        try {
            Statement st = conn.createStatement();
            BufferedReader br = new BufferedReader(new FileReader("src/main/java/org/example/filebrowser/indexupdater/sql/create_table_file.sql"));

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

    public PgUpdater() throws IndexUpdaterException {
        String url = "jdbc:postgresql://localhost:5432/filebrowser";

        try {
            Properties props = PgUtils.getCredentialsFromFile("./credentials.json");

            conn = DriverManager.getConnection(url, props);

            // checking if table FILE exists
            DatabaseMetaData metaData = conn.getMetaData();
            ResultSet rs = metaData.getTables("filebrowser", "public", "file", null);

            if (!rs.first()) {
                // the table does not exist, we create it
                executeFromFile();
            }
        } catch (SQLException | FileNotFoundException | JSONException e) {
            logger.log(Level.SEVERE, e.getMessage());
            throw new IndexUpdaterException(e.getMessage());
        }
    }

    /**
     * Searches for a file in the database by its path
     * @param path The path of the file
     * @return an {@code UpdateValidationData} object containing the data needed
     *          for update validation, or {@code null} if the file was not found
     */
    @Override
    public UpdateValidationData searchByPath(String path) throws IndexUpdaterException {
        try {
            PreparedStatement st = conn.prepareStatement(
                    "SELECT id, file_last_modified_time, checksum, read_access FROM file WHERE path = ?"
            );

            st.setString(1, path);

            ResultSet rs = st.executeQuery();
            if (rs.next()) {
                return new UpdateValidationData(
                        rs.getLong("id"),
                        FileTime.fromMillis(rs.getTimestamp("file_last_modified_time").getTime()),
                        rs.getString("checksum"),
                        rs.getBoolean("read_access")
                );
            } else {
                return null;
            }
        } catch (SQLException e) {
            logger.log(Level.WARNING, "Search failed!\n" + e.getMessage());
            throw new IndexUpdaterException(e.getMessage());
        }
    }

    @Override
    public void insert(FileModel fileModel) throws IndexUpdaterException {
        try {
            PreparedStatement st = conn.prepareStatement(
                    "INSERT INTO file(" +
                            "name, " +
                            "extension, " +
                            "path, " +
                            "file_creation_time, " +
                            "file_last_modified_time, " +
                            "size, " +
                            "read_access," +
                            "checksum, " +
                            "content, " +
                            "last_scan_id" +
                            ") VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)"
            );

            st.setString(1, fileModel.fileAttributes().name());
            st.setString(2, fileModel.fileAttributes().extension());
            st.setString(3, fileModel.fileAttributes().path());
            st.setTimestamp(4, new Timestamp(fileModel.fileAttributes().creationTime().toMillis()));
            st.setTimestamp(5, new Timestamp(fileModel.fileAttributes().lastModifiedTime().toMillis()));
            st.setLong(6, fileModel.fileAttributes().size());
            st.setBoolean(7, fileModel.readAccess());
            st.setString(8, fileModel.checksumValue());
            st.setString(9, fileModel.content());
            st.setLong(10, fileModel.lastScanId());

            st.executeUpdate();

            st.close();

        } catch (SQLException e) {
            logger.log(Level.WARNING, "Insertion failed!\n" + e.getMessage());
            throw new IndexUpdaterException(e.getMessage());
        }
    }

    @Override
    public void updateLastModifiedTime(long fileId, FileTime lastModifiedTime) throws IndexUpdaterException {
        try {
            PreparedStatement st = conn.prepareStatement(
                    "UPDATE file " +
                            "SET file_last_modified_time = ?, updated_at = DEFAULT " +
                            "WHERE id = ?"
            );

            st.setTimestamp(1, new Timestamp(lastModifiedTime.toMillis()));
            st.setLong(2, fileId);

            st.executeUpdate();

            st.close();
        } catch (SQLException e) {
            logger.log(Level.WARNING, "Update time failed!\n" + e.getMessage());
            throw new IndexUpdaterException(e.getMessage());
        }
    }

    @Override
    public void updateFile(long fileId, FileModel fileModel) throws IndexUpdaterException {
        try {
            PreparedStatement st = conn.prepareStatement(
                    "UPDATE file " +
                            "SET file_creation_time = ?, " +
                                "file_last_modified_time = ?, " +
                                "size = ?, " +
                                "read_access = ?, " +
                                "checksum = ?, " +
                                "content = ?, " +
                                "last_scan_id = ?, " +
                                "updated_at = DEFAULT " +
                            "WHERE id = ?"
            );

            st.setTimestamp(1, new Timestamp(fileModel.fileAttributes().lastModifiedTime().toMillis()));
            st.setTimestamp(2, new Timestamp(fileModel.fileAttributes().lastModifiedTime().toMillis()));
            st.setLong(3, fileModel.fileAttributes().size());
            st.setBoolean(4, fileModel.readAccess());
            st.setString(5, fileModel.checksumValue());
            st.setString(6, fileModel.content());
            st.setLong(7, fileModel.lastScanId());
            st.setLong(8, fileId);

            st.executeUpdate();

            st.close();
        } catch (SQLException e) {
            logger.log(Level.WARNING, "Update file failed!\n" + e.getMessage());
            throw new IndexUpdaterException(e.getMessage());
        }
    }

    @Override
    public void updateLastScanId(long fileId, long scanId) throws IndexUpdaterException {
        try {
            PreparedStatement st = conn.prepareStatement(
                    "UPDATE file " +
                            "SET last_scan_id = ?, updated_at = DEFAULT " +
                            "WHERE id = ?"
            );

            st.setLong(1, scanId);
            st.setLong(2, fileId);

            st.executeUpdate();

            st.close();
        } catch (SQLException e) {
            logger.log(Level.WARNING, "Update scanId failed!\n" + e.getMessage());
            throw new IndexUpdaterException(e.getMessage());
        }
    }

    @Override
    public void removeUnscanned(long scanId) throws IndexUpdaterException {
        try {
            PreparedStatement st = conn.prepareStatement(
                    "DELETE FROM file WHERE last_scan_id <> ?"
            );

            st.setLong(1, scanId);

            st.executeUpdate();
        } catch(SQLException e) {
            logger.log(Level.WARNING, "Remove unscanned failed!\n" + e.getMessage());
            throw new IndexUpdaterException(e.getMessage());
        }
    }
}
