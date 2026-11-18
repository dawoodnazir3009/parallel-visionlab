package com.parallelvisionlab.ui;

import com.parallelvisionlab.service.RuntimeSnapshot;
import java.util.Locale;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.scene.chart.*;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.util.Duration;

final class MonitorView extends VBox implements AutoCloseable {
    private final Label cpu = UiCards.label("Waiting…", "metric-value");
    private final Label heap = UiCards.label("Waiting…", "metric-value");
    private final ProgressBar cpuBar = new ProgressBar(0);
    private final ProgressBar heapBar = new ProgressBar(0);
    private final XYChart.Series<Number, Number> cpuSamples = new XYChart.Series<>();
    private final XYChart.Series<Number, Number> heapSamples = new XYChart.Series<>();
    private final Timeline timer;
    private int seconds;
    private boolean paused;

    MonitorView() {
        super(14);
        timer = new Timeline(new KeyFrame(Duration.seconds(1), event -> sample()));
        timer.setCycleCount(Timeline.INDEFINITE);
        Button pause = new Button("Pause");
        pause.setOnAction(event -> {
            paused = !paused;
            pause.setText(paused ? "Resume" : "Pause");
            if (paused) { timer.pause(); } else { timer.play(); }
        });
        cpuBar.setMaxWidth(Double.MAX_VALUE);
        heapBar.setMaxWidth(Double.MAX_VALUE);
        heapBar.getStyleClass().add("backend-2");
        getChildren().addAll(
                UiCards.label("LIVE OS/JVM DATA · GPU, thermals, power and per-core telemetry are not connected", "sample-notice"), pause,
                UiCards.columns(UiCards.panel("SYSTEM CPU LOAD", cpu, cpuBar),
                        UiCards.panel("JVM HEAP USED", heap, heapBar),
                        UiCards.metric("GPU LOAD / POWER", "Unavailable", "Native provider required"),
                        UiCards.metric("LOGICAL PROCESSORS", String.valueOf(Runtime.getRuntime().availableProcessors()), "Detected by the JVM")),
                UiCards.columns(UiCards.panel("CPU utilization history · %", chart(cpuSamples, "CPU %")),
                        UiCards.panel("JVM heap history · % of maximum", chart(heapSamples, "Heap %"))),
                UiCards.panel("Logical processor inventory", cores(),
                        UiCards.label("Processor count is detected. Individual core loads are unavailable; no per-core values are simulated.", "muted")),
                UiCards.panel("Workload inspector", UiCards.label("Image loader: on demand · Sequential + ExecutorService: available · CUDA: not connected", "muted")));
        sceneProperty().addListener((observable, before, after) -> {
            if (after == null) { timer.stop(); } else if (!paused) { timer.play(); }
        });
        sample();
    }

    private LineChart<Number, Number> chart(XYChart.Series<Number, Number> series, String name) {
        NumberAxis time = new NumberAxis();
        time.setLabel("Session seconds");
        time.setForceZeroInRange(false);
        NumberAxis percent = new NumberAxis(0, 100, 25);
        percent.setLabel(name);
        LineChart<Number, Number> chart = new LineChart<>(time, percent);
        chart.getData().add(series);
        chart.setLegendVisible(false);
        chart.setCreateSymbols(false);
        chart.setAnimated(false);
        chart.setMinWidth(0);
        chart.setPrefHeight(230);
        return chart;
    }

    private FlowPane cores() {
        FlowPane cores = new FlowPane(8, 8);
        int count = Runtime.getRuntime().availableProcessors();
        for (int i = 0; i < Math.min(count, 64); i++) {
            Label label = UiCards.label("C" + i + "\nN/A", "core");
            cores.getChildren().add(label);
        }
        if (count > 64) { cores.getChildren().add(UiCards.label("Showing first 64 of " + count, "muted")); }
        return cores;
    }

    private void sample() {
        RuntimeSnapshot snapshot = RuntimeSnapshot.capture();
        double heapFraction = (double) snapshot.usedHeapBytes() / snapshot.maxHeapBytes();
        heap.setText(String.format(Locale.ROOT, "%.0f MB", snapshot.usedHeapBytes() / 1048576.0));
        heapBar.setProgress(heapFraction);
        cpu.setText(snapshot.cpuLoad() < 0 ? "Unavailable" : String.format(Locale.ROOT, "%.0f%%", snapshot.cpuLoad() * 100));
        cpuBar.setProgress(Math.max(0, snapshot.cpuLoad()));
        if (snapshot.cpuLoad() >= 0) { append(cpuSamples, snapshot.cpuLoad() * 100); }
        append(heapSamples, heapFraction * 100);
        seconds++;
    }

    private void append(XYChart.Series<Number, Number> series, double value) {
        series.getData().add(new XYChart.Data<>(seconds, value));
        if (series.getData().size() > 60) { series.getData().removeFirst(); }
    }

    @Override public void close() { timer.stop(); }
}
