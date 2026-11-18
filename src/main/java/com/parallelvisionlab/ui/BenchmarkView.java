package com.parallelvisionlab.ui;

import javafx.beans.property.ReadOnlyStringWrapper;
import javafx.scene.control.*;
import javafx.scene.layout.*;

final class BenchmarkView extends VBox {
    BenchmarkView() {
        super(14);
        getChildren().addAll(
                UiCards.label("ILLUSTRATIVE DATA — not measured on this computer", "sample-notice"),
                UiCards.columns(
                        UiCards.metric("SEQUENTIAL SAMPLE", "120 ms", "Illustrative baseline"),
                        UiCards.metric("FASTEST SAMPLE", "18 ms", "CUDA tiled illustration"),
                        UiCards.metric("SAMPLE SPEEDUP", "6.67x", "Relative to sample baseline"),
                        UiCards.metric("BACKEND SAMPLES", "5", "No experiments recorded")),
                UiCards.panel("Execution time by backend · sample milliseconds", bars()),
                UiCards.panel("Execution comparison · sample data", comparisonTable()),
                UiCards.label("Real benchmarking will include warmups, repeated measurements, output validation and separate GPU transfer/launch overhead.", "muted"));
    }

    static VBox bars() {
        VBox bars = new VBox(13);
        int index = 0;
        for (var sample : DemoBenchmarks.SAMPLES) {
            Label name = UiCards.label(sample.backend(), "chart-label");
            Region spacer = new Region();
            HBox.setHgrow(spacer, Priority.ALWAYS);
            HBox legend = new HBox(8, name, spacer, UiCards.label(sample.timeLabel(), "chart-label"));
            ProgressBar bar = new ProgressBar(sample.milliseconds() / 120.0);
            bar.setMaxWidth(Double.MAX_VALUE);
            bar.getStyleClass().add("backend-" + index++);
            bars.getChildren().add(new VBox(6, legend, bar));
        }
        return bars;
    }

    private TableView<DemoBenchmarks.Sample> comparisonTable() {
        TableView<DemoBenchmarks.Sample> table = new TableView<>();
        TableColumn<DemoBenchmarks.Sample, String> backend = new TableColumn<>("Backend");
        backend.setCellValueFactory(cell -> new ReadOnlyStringWrapper(cell.getValue().backend()));
        TableColumn<DemoBenchmarks.Sample, String> time = new TableColumn<>("Time (sample)");
        time.setCellValueFactory(cell -> new ReadOnlyStringWrapper(cell.getValue().timeLabel()));
        TableColumn<DemoBenchmarks.Sample, String> speedup = new TableColumn<>("Speedup (sample)");
        speedup.setCellValueFactory(cell -> new ReadOnlyStringWrapper(cell.getValue().speedupLabel()));
        table.getColumns().add(backend);
        table.getColumns().add(time);
        table.getColumns().add(speedup);
        table.getItems().setAll(DemoBenchmarks.SAMPLES);
        table.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_FLEX_LAST_COLUMN);
        table.setPrefHeight(230);
        return table;
    }
}
