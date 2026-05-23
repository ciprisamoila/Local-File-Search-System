package org.example.filebrowser.ui.widgets;

import javafx.geometry.Insets;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.VBox;
import javafx.scene.Scene;
import javafx.stage.Stage;
import org.example.filebrowser.model.QueryFileModel;
import org.example.filebrowser.model.QueryInducedType;
import org.example.filebrowser.model.QueryResponse;
import org.example.filebrowser.model.index.FileType;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static java.lang.Math.min;

public class ShowGalleryWidget implements IWidget {

    @Override
    public Button getWidget(QueryResponse response) {
        Button button = new Button("Show Gallery");
        button.setOnAction(_ -> openGallery(response));
        return button;
    }

    @Override
    public float widgetScore(QueryResponse response) {
        if (response == null || response.queryFileModels() == null || response.queryFileModels().isEmpty()) {
            return 0;
        }

        float score = 0;

        if (response.queryInducedType() == QueryInducedType.IMAGE) {
            score += 0.5f;
        }

        long nrOfImages = response.queryFileModels().stream().filter(file -> file.fileType() == FileType.IMAGE).count();
        score += nrOfImages >= 5 ? 0.5f : nrOfImages / 10.0f;

        return min(score, 1.0f);
    }

    private void openGallery(QueryResponse response) {
        List<QueryFileModel> imageFiles = response == null || response.queryFileModels() == null
                ? List.of()
                : response.queryFileModels().stream()
                .filter(file -> file.fileType() == FileType.IMAGE)
                .toList();

        Stage stage = new Stage();
        stage.setTitle("Image Gallery");

        VBox root = new VBox(10);
        root.setPadding(new Insets(12));

        if (imageFiles.isEmpty()) {
            root.getChildren().add(new Label("No image files found in current results."));
        } else {
            FlowPane gallery = new FlowPane();
            gallery.setHgap(10);
            gallery.setVgap(10);

            for (QueryFileModel file : imageFiles) {
                gallery.getChildren().add(createImageCard(file));
            }

            ScrollPane scrollPane = new ScrollPane(gallery);
            scrollPane.setFitToWidth(true);
            root.getChildren().add(scrollPane);
        }

        stage.setScene(new Scene(root, 900, 600));
        stage.show();
    }

    private VBox createImageCard(QueryFileModel file) {
        VBox card = new VBox(6);
        card.setPadding(new Insets(8));
        card.setPrefWidth(190);
        card.setStyle("-fx-background-color: white; -fx-border-color: #d1d5db; -fx-border-radius: 6; -fx-background-radius: 6;");

        Label nameLabel = new Label(file.fullName());
        nameLabel.setWrapText(true);

        Path imagePath;
        try {
            imagePath = Path.of(file.path());
        } catch (Exception e) {
            card.getChildren().addAll(nameLabel, new Label("Invalid image path."));
            return card;
        }

        if (!Files.exists(imagePath) || !Files.isReadable(imagePath)) {
            card.getChildren().addAll(nameLabel, new Label("Image file not accessible."));
            return card;
        }

        Image image = new Image(imagePath.toUri().toString(), 170, 130, true, true);
        if (image.isError()) {
            card.getChildren().addAll(nameLabel, new Label("Unable to load preview."));
            return card;
        }

        ImageView imageView = new ImageView(image);
        imageView.setPreserveRatio(true);
        imageView.setFitWidth(170);
        imageView.setFitHeight(130);

        Label pathLabel = new Label(file.path());
        pathLabel.setWrapText(true);
        pathLabel.setStyle("-fx-text-fill: #475569; -fx-font-size: 11px;");

        card.getChildren().addAll(nameLabel, imageView, pathLabel);
        return card;
    }
}
