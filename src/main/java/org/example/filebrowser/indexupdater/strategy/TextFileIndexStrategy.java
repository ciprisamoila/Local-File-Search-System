package org.example.filebrowser.indexupdater.strategy;

import org.example.filebrowser.model.index.FileModel;
import org.example.filebrowser.model.index.TextFileModel;
import org.example.filebrowser.utils.exceptions.IndexUpdaterException;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;

public class TextFileIndexStrategy implements IFileIndexStrategy {

    @Override
    public void insertSpecificData(Connection conn, long fileId, FileModel fileModel) throws IndexUpdaterException {
        if (!(fileModel instanceof TextFileModel textFileModel)) {
            throw new IndexUpdaterException("FileModel is not of type TextFileModel");
        }

        try {
            PreparedStatement st = conn.prepareStatement(
                    """
                        INSERT INTO text_file(
                              file_id, content
                        ) VALUES (?, ?)
                        """
            );

            st.setLong(1, fileId);
            st.setString(2, textFileModel.getContent());

            st.executeUpdate();

            st.close();

        } catch (SQLException e) {
            throw new IndexUpdaterException(e.getMessage());
        }
    }

    @Override
    public void updateSpecificData(Connection conn, long fileId, FileModel fileModel) throws IndexUpdaterException {
        if (!(fileModel instanceof TextFileModel textFileModel)) {
            throw new IndexUpdaterException("FileModel is not of type TextFileModel");
        }

        try {
            PreparedStatement st = conn.prepareStatement(
                    """
                        UPDATE text_file SET
                             content = ?
                        WHERE file_id = ?
                        """
            );

            st.setString(1, textFileModel.getContent());
            st.setLong(2, fileId);

            st.executeUpdate();

            st.close();

        } catch (SQLException e) {
            throw new IndexUpdaterException(e.getMessage());
        }
    }
}
