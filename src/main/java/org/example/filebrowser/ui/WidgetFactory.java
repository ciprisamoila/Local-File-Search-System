package org.example.filebrowser.ui;

import javafx.scene.control.Button;
import org.example.filebrowser.model.QueryResponse;
import org.example.filebrowser.ui.widgets.IWidget;

import java.util.ArrayList;
import java.util.List;

public class WidgetFactory {

    private final List<IWidget> possibleWidgets;

    public WidgetFactory(List<IWidget> possibleWidgets) {
        this.possibleWidgets = possibleWidgets;
    }

    public void addWidget(IWidget widget) {
        possibleWidgets.add(widget);
    }

    public void removeWidget(IWidget widget) {
        possibleWidgets.remove(widget);
    }

    List<Button> getWidgets(QueryResponse queryResponse) {

        List<Button> buttons = new ArrayList<>();
        float SCORE_THRESHOLD = 0.5f;
        for (IWidget widget : possibleWidgets) {
            if (widget.widgetScore(queryResponse) >= SCORE_THRESHOLD) {
                buttons.add(widget.getWidget());
            }
        }

        return buttons;
    }
}
