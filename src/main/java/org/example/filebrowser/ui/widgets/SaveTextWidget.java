package org.example.filebrowser.ui.widgets;

import javafx.scene.control.Button;
import org.example.filebrowser.model.QueryInducedType;
import org.example.filebrowser.model.QueryResponse;
import org.example.filebrowser.model.index.FileType;

import static java.lang.Math.min;

public class SaveTextWidget implements IWidget {

    @Override
    public Button getWidget() {
        System.out.println("text");
        return null;
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
}
