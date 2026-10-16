module com.example.sellercabinetapp {
    requires javafx.controls;
    requires javafx.fxml;

    requires org.controlsfx.controls;
    requires com.dlsc.formsfx;
    requires org.kordamp.bootstrapfx.core;

    requires java.net.http;
    requires com.fasterxml.jackson.databind;
    requires com.fasterxml.jackson.datatype.jsr310;

    opens com.example.sellercabinetapp.dto to com.fasterxml.jackson.databind;

    opens com.example.sellercabinetapp to javafx.fxml;
    exports com.example.sellercabinetapp;
}