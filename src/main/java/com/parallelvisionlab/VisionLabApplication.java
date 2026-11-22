package com.parallelvisionlab;

import javafx.application.Application;
import javafx.scene.Scene;
import javafx.stage.Stage;
import com.parallelvisionlab.ui.LabShell;

/** JavaFX entry point and owner of application resource cleanup. */
public final class VisionLabApplication extends Application {
    private LabShell shell;
    @Override
    public void start(Stage stage) {
        com.parallelvisionlab.service.CpuLimits.applyProcessLimit();
        stage.setTitle("Parallel VisionLab");
        shell = new LabShell(stage);
        Scene scene = new Scene(shell, 1280, 840);
        scene.getStylesheets().add(getClass().getResource("/com/parallelvisionlab/workspace.css").toExternalForm());
        stage.setScene(scene);
        stage.setMinWidth(1000);
        stage.setMinHeight(650);
        stage.show();
    }

    public static void main(String[] args) {
        launch(args);
    }

    @Override public void stop() {
        if (shell != null) { shell.close(); }
    }
}
