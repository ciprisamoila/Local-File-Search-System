module org.example.filebrowser {
    requires javafx.controls;
    requires javafx.fxml;
    requires java.logging;
    requires org.json;
    requires java.sql;
    requires org.apache.tika.core;


    opens org.example.filebrowser to javafx.fxml;
    exports org.example.filebrowser;
}