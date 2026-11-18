package com.parallelvisionlab.ui;

import java.util.function.Consumer;
import javafx.scene.control.Button;
import javafx.scene.layout.VBox;

final class OverviewView extends VBox {
    OverviewView(Consumer<LabPage> navigate) {
        super(14);
        Button workspace = new Button("Open Image Workspace →");
        workspace.getStyleClass().add("primary");
        workspace.setOnAction(event -> navigate.accept(LabPage.WORKSPACE));
        Button benchmark = new Button("View Benchmark Lab →");
        benchmark.setOnAction(event -> navigate.accept(LabPage.BENCHMARK));
        Button monitor = new Button("Open Live Monitor →");
        monitor.setOnAction(event -> navigate.accept(LabPage.MONITOR));
        getChildren().addAll(
                UiCards.columns(
                        UiCards.metric("LOGICAL PROCESSORS", String.valueOf(Runtime.getRuntime().availableProcessors()), "Available to this JVM"),
                        UiCards.metric("GPU TELEMETRY", "Unavailable", "Native monitor not connected"),
                        UiCards.metric("JVM HEAP LIMIT", String.format(java.util.Locale.ROOT, "%.1f GB", Runtime.getRuntime().maxMemory() / 1073741824.0), "Not total system RAM"),
                        UiCards.metric("PROCESSING ENGINES", "Not connected", "Image preview is available")),
                UiCards.columns(
                        UiCards.panel("Processing workspace", UiCards.empty("Start a new image experiment", "Load an image and inspect it at its original resolution."), workspace),
                        UiCards.panel("Illustrative performance comparison", BenchmarkView.bars(),
                                UiCards.label("Sample timings only — not measured on your hardware.", "sample-notice"), benchmark)),
                UiCards.panel("Hardware telemetry", UiCards.label("Inspect real JVM resources and backend availability. GPU measurements are not connected yet.", "muted"), monitor));
    }
}
