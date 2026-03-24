module org.example.filebrowser {
    requires javafx.controls;
    requires javafx.fxml;


    opens org.example.filebrowser to javafx.fxml;
    exports org.example.filebrowser;
}