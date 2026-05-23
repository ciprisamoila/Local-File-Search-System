package org.example.filebrowser.ui.widgets;

import javafx.geometry.Insets;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.Alert;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import org.example.filebrowser.model.QueryFileModel;
import org.example.filebrowser.model.QueryInducedType;
import org.example.filebrowser.model.QueryResponse;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static java.lang.Math.min;

public class AnalyzeLogsWidget implements IWidget {

    private static final Pattern LOG_LINE_PATTERN = Pattern.compile(
            "^(\\d{4}-\\d{2}-\\d{2})\\s+(\\d{2}:\\d{2}:\\d{2})\\s+\\[(DEBUG|INFO|WARN|ERROR)]"
    );

    @Override
    public Button getWidget(QueryResponse response) {
        Button button = new Button("Analyze Logs");
        button.setOnAction(_ -> showLogSummary(response, button.getScene() == null ? null : button.getScene().getWindow()));
        return button;
    }

    @Override
    public float widgetScore(QueryResponse response) {
        if (response == null || response.queryFileModels() == null || response.queryFileModels().isEmpty()) {
            return 0;
        }

        float score = 0;

        if (response.queryInducedType() == QueryInducedType.LOGS) {
            score += 0.3f;
        }

        long nrOfLogFiles = response.queryFileModels().stream().filter(this::containLogs).count();
        if (nrOfLogFiles != 0) {
            score += 0.5f;
        }

        return min(score, 1.0f);
    }

    private void showLogSummary(QueryResponse response, javafx.stage.Window ownerWindow) {
        Map<LogType, Integer> counts = new EnumMap<>(LogType.class);
        for (LogType type : LogType.values()) {
            counts.put(type, 0);
        }

        List<QueryFileModel> files = response == null || response.queryFileModels() == null
                ? List.of()
                : response.queryFileModels();

        int scannedFiles = 0;
        int readableLogFiles = 0;

        for (QueryFileModel file : files) {
            if (!containLogs(file)) {
                continue;
            }

            scannedFiles++;
            Path logPath = Path.of(file.path());
            if (!Files.isReadable(logPath)) {
                showAlert(ownerWindow, Alert.AlertType.WARNING, "Skipping unreadable log file: " + describeFile(file));
                continue;
            }

            readableLogFiles++;
            try {
                countLogTypesInFile(logPath, counts);
            } catch (IOException e) {
                showAlert(ownerWindow, Alert.AlertType.WARNING, "Skipping unreadable log file: " + logPath + "\n" + e.getMessage());
            }
        }

        showSummaryWindow(scannedFiles, readableLogFiles, counts);
    }

    private void countLogTypesInFile(Path logPath, Map<LogType, Integer> counts) throws IOException {
        for (String line : Files.readAllLines(logPath, StandardCharsets.UTF_8)) {
            Matcher matcher = LOG_LINE_PATTERN.matcher(line);
            if (!matcher.find()) {
                continue;
            }

            LogType type = LogType.valueOf(matcher.group(3));
            counts.put(type, counts.get(type) + 1);
        }
    }

    private void showSummaryWindow(int scannedFiles, int readableLogFiles, Map<LogType, Integer> counts) {
        Stage stage = new Stage();
        stage.setTitle("Log Analysis");

        VBox root = new VBox(8);
        root.setPadding(new Insets(12));

        root.getChildren().add(new Label("Scanned files containing logs: " + scannedFiles));
        root.getChildren().add(new Label("Readable log files: " + readableLogFiles));
        root.getChildren().add(new Label("DEBUG: " + counts.get(LogType.DEBUG)));
        root.getChildren().add(new Label("INFO: " + counts.get(LogType.INFO)));
        root.getChildren().add(new Label("WARN: " + counts.get(LogType.WARN)));
        root.getChildren().add(new Label("ERROR: " + counts.get(LogType.ERROR)));

        stage.setScene(new Scene(root, 280, 180));
        stage.show();
    }

    private static String describeFile(QueryFileModel file) {
        return file.fullName() + " (" + file.path() + ")";
    }

    private static void showAlert(javafx.stage.Window ownerWindow, Alert.AlertType type, String message) {
        Alert alert = new Alert(type);
        if (ownerWindow != null) {
            alert.initOwner(ownerWindow);
        }
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }

    private enum LogType {
        DEBUG,
        INFO,
        WARN,
        ERROR
    }

    private boolean containLogs(QueryFileModel file) {
        if (file.headline() == null) {
            return false;
        }
        Matcher matcher = LOG_LINE_PATTERN.matcher(file.headline());

        return matcher.find();
    }
}
