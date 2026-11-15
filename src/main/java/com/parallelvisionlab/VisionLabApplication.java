package com.parallelvisionlab;

import javafx.application.Application;
import javafx.scene.Scene;
import javafx.stage.Stage;
import com.parallelvisionlab.ui.WorkspaceView;

/** JavaFX entry point; processing engines will be added in later phases. */
public final class VisionLabApplication extends Application {
    @Override
    public void start(Stage stage) {
        stage.setTitle("Parallel VisionLab");
        Scene scene = new Scene(new WorkspaceView(stage), 1160, 760);
        scene.getStylesheets().add(getClass().getResource("/com/parallelvisionlab/workspace.css").toExternalForm());
        stage.setScene(scene);
        stage.setMinWidth(800);
        stage.setMinHeight(560);
        stage.show();
    }

    public static void main(String[] args) {
        launch(args);
    }
}
