package org.example.filebrowser.ui.widgets;

import javafx.scene.control.Button;
import org.example.filebrowser.model.QueryResponse;

public interface IWidget {
    Button getWidget();
    // each widget scores (0 to 1) its applicability with the query response
    float widgetScore(QueryResponse response);
}
