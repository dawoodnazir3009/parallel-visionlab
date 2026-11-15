package com.parallelvisionlab;

import javafx.application.Application;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

/** JavaFX entry point; processing engines will be added in later phases. */
public final class VisionLabApplication extends Application {
    @Override
    public void start(Stage stage) {
        Label title = new Label("Parallel VisionLab");
        title.setStyle("-fx-font-size: 30px; -fx-font-weight: bold;");
        Label subtitle = new Label("Parallel and Distributed Image Processing");
        Label status = new Label("Phase 1 • JavaFX environment ready");
        VBox root = new VBox(16, title, subtitle, status);
        root.setAlignment(Pos.CENTER);
        root.setPadding(new Insets(32));
        stage.setTitle("Parallel VisionLab");
        stage.setScene(new Scene(root, 1000, 680));
        stage.setMinWidth(640);
        stage.setMinHeight(420);
        stage.show();
    }

    public static void main(String[] args) {
        launch(args);
    }
}
