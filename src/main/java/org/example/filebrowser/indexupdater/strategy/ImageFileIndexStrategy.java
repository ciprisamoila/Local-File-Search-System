package org.example.filebrowser.indexupdater.strategy;

import org.example.filebrowser.model.index.FileModel;
import org.example.filebrowser.model.index.ImageFileModel;
import org.example.filebrowser.utils.exceptions.IndexUpdaterException;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;

public class ImageFileIndexStrategy implements IFileIndexStrategy {

    @Override
    public void insertSpecificData(Connection conn, long fileId, FileModel fileModel) throws IndexUpdaterException {
        if (!(fileModel instanceof ImageFileModel imageFileModel)) {
            throw new IndexUpdaterException("FileModel is not of type ImageFileModel");
        }

        try {
            PreparedStatement st = conn.prepareStatement(
                    """
                        INSERT INTO image_file(
                              file_id, color
                        ) VALUES (?, ?)
                        """
            );

            st.setLong(1, fileId);
            st.setString(2, imageFileModel.getColor());

            st.executeUpdate();

            st.close();

        } catch (SQLException e) {
            throw new IndexUpdaterException(e.getMessage());
        }
    }

    @Override
    public void updateSpecificData(Connection conn, long fileId, FileModel fileModel) throws IndexUpdaterException {
        if (!(fileModel instanceof ImageFileModel imageFileModel)) {
            throw new IndexUpdaterException("FileModel is not of type ImageFileModel");
        }

        try {
            PreparedStatement st = conn.prepareStatement(
                    """
                        UPDATE image_file SET
                             color = ?
                        WHERE file_id = ?
                        """
            );

            st.setString(1, imageFileModel.getColor());
            st.setLong(2, fileId);

            st.executeUpdate();

            st.close();

        } catch (SQLException e) {
            throw new IndexUpdaterException(e.getMessage());
        }
    }
}
