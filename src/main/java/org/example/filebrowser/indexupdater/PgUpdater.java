package org.example.filebrowser.indexupdater;

import org.example.filebrowser.model.FileModel;
import org.example.filebrowser.model.UpdateValidationData;
import org.example.filebrowser.utils.exceptions.IndexUpdaterException;

import java.io.*;
import java.nio.file.attribute.FileTime;
import java.sql.*;
import java.util.Properties;
import java.util.logging.Level;
import java.util.logging.Logger;

public class PgUpdater implements IUpdater{
    private final Logger logger = Logger.getLogger("indexupdater");
    private final Connection conn;

    private void executeFromFile(String filename) throws SQLException {
        try {
            Statement st = conn.createStatement();
            BufferedReader br = new BufferedReader(new FileReader(filename));

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

        // trebuie sa citesc datele sensibile dintr-un fisier
        Properties props = new Properties();
        props.setProperty("user", "postgres");
        props.setProperty("password", "postgres");

        try {
            conn = DriverManager.getConnection(url, props);

            // checking if table FILE exists
            DatabaseMetaData metaData = conn.getMetaData();
            ResultSet rs = metaData.getTables("filebrowser", "public", "file", null);

            if (!rs.first()) {
                // the table does not exist, we create it
                executeFromFile("src/main/java/org/example/filebrowser/indexupdater/sql/create_table_file.sql");
            }
        } catch (SQLException e) {
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
                    "SELECT file_last_modified_time, checksum FROM file WHERE path = ?"
            );

            st.setString(1, path);

            ResultSet rs = st.executeQuery();
            if (rs.next()) {
                return new UpdateValidationData(
                        FileTime.fromMillis(rs.getTimestamp("file_last_modified_time").getTime()),
                        rs.getLong("checksum")
                );
            } else {
                return null;
            }
        } catch (SQLException e) {
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
                            "checksum, " +
                            "content, " +
                            "last_scan_id" +
                            ") VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)"
            );

            st.setString(1, fileModel.fileAttributes().name());
            st.setString(2, fileModel.fileAttributes().extension());
            st.setString(3, fileModel.fileAttributes().path());
            st.setTimestamp(4, new Timestamp(fileModel.fileAttributes().creationTime().toMillis()));
            st.setTimestamp(5, new Timestamp(fileModel.fileAttributes().lastModifiedTime().toMillis()));
            st.setLong(6, fileModel.fileAttributes().size());
            st.setLong(7, fileModel.checksumValue());
            st.setString(8, fileModel.content());
            st.setLong(9, fileModel.lastScanId());

            st.executeUpdate();

            st.close();

        } catch (SQLException e) {
            logger.log(Level.WARNING, "Insertion failed!\n" + e.getMessage());
            throw new IndexUpdaterException(e.getMessage());
        }
    }

    @Override
    public void updateLastModifiedTime(FileTime lastModifiedTime) {

    }

    @Override
    public void updateFile(FileModel fileModel) {

    }

    @Override
    public void updateLastScanId(long scanId) {

    }

    @Override
    public void removeUnscanned(long scanId) {

    }

    public static void main(String[] args) throws IndexUpdaterException {
        PgUpdater u = new PgUpdater();

        System.out.println(u.searchByPath("aaa"));
    }
}
