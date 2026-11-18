package com.parallelvisionlab;

import javafx.application.Application;
import javafx.scene.Scene;
import javafx.stage.Stage;
import com.parallelvisionlab.ui.LabShell;

/** JavaFX entry point; processing engines will be added in later phases. */
public final class VisionLabApplication extends Application {
    @Override
    public void start(Stage stage) {
        stage.setTitle("Parallel VisionLab");
        Scene scene = new Scene(new LabShell(stage), 1280, 840);
        scene.getStylesheets().add(getClass().getResource("/com/parallelvisionlab/workspace.css").toExternalForm());
        stage.setScene(scene);
        stage.setMinWidth(1000);
        stage.setMinHeight(650);
        stage.show();
    }

    public static void main(String[] args) {
        launch(args);
    }
}
