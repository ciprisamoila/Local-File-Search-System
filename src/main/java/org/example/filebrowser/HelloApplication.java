package org.example.filebrowser;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.stage.Stage;
import org.example.filebrowser.crawler.Crawling;
import org.example.filebrowser.crawler.FileCrawlerManager;
import org.example.filebrowser.ui.SearchController;
import org.example.filebrowser.querymanager.PgQuerier;
import org.example.filebrowser.utils.exceptions.QueryManagerException;

import java.io.IOException;

public class HelloApplication extends Application {
    private SearchController controller;

    @Override
    public void start(Stage stage) throws IOException {
        FXMLLoader fxmlLoader = new FXMLLoader(HelloApplication.class.getResource("hello-view.fxml"));
        Scene scene = new Scene(fxmlLoader.load(), 980, 640);

        controller = fxmlLoader.getController();
        Crawling crawler = new FileCrawlerManager();
        controller.setCrawler(crawler);

        try {
            controller.setQuerier(new PgQuerier());
        } catch (QueryManagerException e) {
            controller.setInitializationError("Database connection failed: " + e.getMessage());
        }

        stage.setTitle("File Browser Search");
        stage.setScene(scene);
        stage.setOnCloseRequest(event -> controller.shutdown());
        stage.show();
    }

    @Override
    public void stop() {
        if (controller != null) {
            controller.shutdown();
        }
    }

    public static void main(String[] args) {
        launch();
    }
}