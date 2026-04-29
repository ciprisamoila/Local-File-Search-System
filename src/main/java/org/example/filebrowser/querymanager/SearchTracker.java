package org.example.filebrowser.querymanager;

import javafx.concurrent.Task;

import java.sql.*;
import java.util.Properties;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.logging.Level;
import java.util.logging.Logger;

public class SearchTracker implements Observer {
    private final Logger logger = Logger.getLogger("querymanager");
    Connection connection;
    public SearchTracker(String url, Properties properties) throws SQLException {
        // it has his own db connection
        connection = DriverManager.getConnection(url, properties);
    }
    @Override
    public void update(Observation observation) {

        Task<Void> trackerTask = new Task<>() {
            @Override
            protected Void call() throws Exception {
                PreparedStatement preparedStatement = connection.prepareStatement(
                        "INSERT INTO query (query) VALUES (?)" +
                        "ON CONFLICT (query) DO UPDATE SET last_used_at = current_timestamp"
                );
                preparedStatement.setString(1, observation.query());
                preparedStatement.executeUpdate();
                preparedStatement.close();
                return null;
            }
        };

        trackerTask.setOnFailed(_ -> {
            logger.log(Level.WARNING, "An error occurred while trying to track search results. " +
                    "Continuing without saving");
        });

        ExecutorService executor = Executors.newSingleThreadExecutor();
        executor.submit(trackerTask);
    }
}
