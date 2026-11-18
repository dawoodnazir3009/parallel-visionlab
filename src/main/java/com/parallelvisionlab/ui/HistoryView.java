package com.parallelvisionlab.ui;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import javafx.scene.control.*;
import javafx.scene.layout.VBox;
import javafx.stage.FileChooser;
import javafx.stage.Stage;

final class HistoryView extends VBox {
    HistoryView(Stage owner) {
        super(14);
        Button export = new Button("Export Sample CSV");
        Label result = UiCards.label("No experiment has been recorded. Export contains sample data only.", "muted");
        export.setOnAction(event -> {
            FileChooser chooser = new FileChooser();
            chooser.setTitle("Export illustrative benchmark data");
            chooser.setInitialFileName("visionlab-sample-benchmarks.csv");
            chooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("CSV files", "*.csv"));
            File file = chooser.showSaveDialog(owner);
            if (file == null) { return; }
            try {
                Files.writeString(file.toPath(), DemoBenchmarks.csv(), StandardCharsets.UTF_8);
                result.setText("Sample data exported to " + file.getAbsolutePath());
            } catch (IOException exception) {
                Alert alert = new Alert(Alert.AlertType.ERROR, "Could not export sample data: " + exception.getMessage());
                alert.initOwner(owner);
                alert.showAndWait();
            }
        });
        getChildren().addAll(UiCards.panel("Experiment history", UiCards.empty("No experiments yet", "Real processing and persistent experiment records are planned in later phases."), export, result),
                UiCards.panel("Sample export preview", UiCards.label("ILLUSTRATIVE DATA · not experiment history", "sample-notice"),
                        UiCards.label("Sequential Java: 120 ms\nMultithreading: 54 ms\nFork/Join: 48 ms\nCUDA Basic: 27 ms\nCUDA Tiled: 18 ms", "muted")));
    }
}
