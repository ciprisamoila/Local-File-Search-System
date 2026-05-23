package org.example.filebrowser.ui.widgets;

import javafx.scene.control.Button;
import org.example.filebrowser.model.QueryFileModel;
import org.example.filebrowser.model.QueryInducedType;
import org.example.filebrowser.model.QueryResponse;

import java.io.File;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static java.lang.Math.min;

public class AnalyzeLogsWidget implements IWidget {

    @Override
    public Button getWidget() {
        System.out.println("logs");
        return null;
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

    private boolean containLogs(QueryFileModel file) {
        if (file.headline() == null) {
            return false;
        }
        String regex = "^(\\d{4}-\\d{2}-\\d{2})\\s+(\\d{2}:\\d{2}:\\d{2})\\s+\\[(\\w+)]";

        Pattern pattern = Pattern.compile(regex);
        Matcher matcher = pattern.matcher(file.headline());

        return matcher.find();
    }
}
