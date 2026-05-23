package org.example.filebrowser.ui.widgets;

import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.stage.FileChooser;
import javafx.stage.Window;
import org.example.filebrowser.model.QueryInducedType;
import org.example.filebrowser.model.QueryFileModel;
import org.example.filebrowser.model.QueryResponse;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static java.lang.Math.min;

public class SaveTextWidget implements IWidget {

    @Override
    public Button getWidget(QueryResponse response) {
        Button button = new Button("Save Text");
        button.setOnAction(_ -> saveText(response, button.getScene() == null ? null : button.getScene().getWindow()));
        return button;
    }

    @Override
    public float widgetScore(QueryResponse response) {
        if (response == null || response.queryFileModels() == null || response.queryFileModels().isEmpty()) {
            return 0;
        }

        float score = 0;

        if (response.queryInducedType() == QueryInducedType.TXT) {
            score += 0.5f;
        }

        long nrOfTxtFiles = response.queryFileModels().stream().filter(file -> file.fullName().endsWith("txt")).count();
        score += nrOfTxtFiles >= 3 ? 0.5f : nrOfTxtFiles / 10.0f;

        return min(score, 1.0f);
    }

    private void saveText(QueryResponse response, Window ownerWindow) {
        List<QueryFileModel> textFiles = response == null || response.queryFileModels() == null
                ? List.of()
                : response.queryFileModels().stream()
                .filter(file -> file.fullName() != null && file.fullName().toLowerCase().endsWith(".txt"))
                .toList();

        if (textFiles.isEmpty()) {
            showAlert(ownerWindow, Alert.AlertType.INFORMATION, "No text files found in the current response.");
            return;
        }

        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Save combined text");
        fileChooser.setInitialFileName("combined-text");
        fileChooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("Text Files", "*.txt"));

        Path downloadsDirectory = getDownloadsDirectory();
        if (downloadsDirectory != null && Files.isDirectory(downloadsDirectory)) {
            fileChooser.setInitialDirectory(downloadsDirectory.toFile());
        }

        File destination = fileChooser.showSaveDialog(ownerWindow);
        if (destination == null) {
            return;
        }

        StringBuilder combinedText = new StringBuilder();
        for (QueryFileModel file : textFiles) {
            Path sourcePath = Path.of(file.path());
            try {
                combinedText.append(Files.readString(sourcePath, StandardCharsets.UTF_8));
                if (!combinedText.isEmpty() && combinedText.charAt(combinedText.length() - 1) != '\n') {
                    combinedText.append(System.lineSeparator());
                }
            } catch (IOException | RuntimeException e) {
                showAlert(ownerWindow, Alert.AlertType.ERROR, "Failed to read: " + sourcePath + "\n" + e.getMessage());
            }
        }

        try {
            Files.writeString(destination.toPath(), combinedText.toString(), StandardCharsets.UTF_8);
            showAlert(ownerWindow, Alert.AlertType.INFORMATION, "Saved combined text to:\n" + destination.getAbsolutePath());
        } catch (IOException e) {
            showAlert(ownerWindow, Alert.AlertType.ERROR, "Failed to save file:\n" + e.getMessage());
        }
    }

    private static Path getDownloadsDirectory() {
        Path downloads = Path.of(System.getProperty("user.home"), "Downloads");
        if (Files.isDirectory(downloads)) {
            return downloads;
        }

        Path home = Path.of(System.getProperty("user.home"));
        return Files.isDirectory(home) ? home : null;
    }

    private static void showAlert(Window ownerWindow, Alert.AlertType type, String message) {
        Alert alert = new Alert(type);
        alert.initOwner(ownerWindow);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }
}
