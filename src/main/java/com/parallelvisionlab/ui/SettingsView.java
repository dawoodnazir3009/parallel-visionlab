package com.parallelvisionlab.ui;

import javafx.scene.control.*;
import javafx.scene.layout.VBox;

/** Session-only preferences until a real benchmark runner consumes them. */
final class SettingsView extends VBox {
    SettingsView() {
        super(14);
        ComboBox<Integer> warmups = new ComboBox<>();
        warmups.getItems().addAll(3, 5, 10);
        warmups.setValue(3);
        ComboBox<Integer> iterations = new ComboBox<>();
        iterations.getItems().addAll(10, 20, 50);
        iterations.setValue(10);
        ComboBox<String> timing = new ComboBox<>();
        timing.getItems().addAll("End-to-end processing", "Kernel only", "Transfer + kernel");
        timing.setValue("End-to-end processing");
        for (ComboBox<?> control : new ComboBox<?>[] { warmups, iterations, timing }) {
            control.setMaxWidth(Double.MAX_VALUE);
        }
        Label feedback = UiCards.label("Session preview only. No benchmark engine is connected.", "muted");
        Button apply = new Button("Apply Preview Preferences");
        apply.getStyleClass().add("primary");
        apply.setOnAction(event -> feedback.setText("Preview selected: " + warmups.getValue() + " warmups, "
                + iterations.getValue() + " iterations, " + timing.getValue() + ". Not persisted or executed."));
        getChildren().add(UiCards.columns(
                UiCards.panel("Detected runtime", field("Java runtime", System.getProperty("java.version")),
                        field("Operating system", System.getProperty("os.name") + " · " + System.getProperty("os.arch")),
                        field("Logical processors", String.valueOf(Runtime.getRuntime().availableProcessors())),
                        field("GPU telemetry", "Not connected"),
                        UiCards.label("Runtime information is detected. An NVIDIA GPU is not assumed on other computers.", "muted")),
                UiCards.panel("Benchmark preferences", UiCards.label("Warm-up runs", "muted"), warmups,
                        UiCards.label("Measured iterations", "muted"), iterations,
                        UiCards.label("Default timing mode", "muted"), timing, apply, feedback)));
    }

    private VBox field(String name, String value) {
        TextField field = new TextField(value);
        field.setEditable(false);
        return new VBox(6, UiCards.label(name, "muted"), field);
    }
}
