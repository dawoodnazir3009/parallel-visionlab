package com.parallelvisionlab.ui;

import com.parallelvisionlab.service.NvidiaTelemetry;
import com.parallelvisionlab.service.RuntimeSnapshot;
import java.util.Locale;
import java.util.concurrent.*;
import javafx.application.Platform;
import javafx.scene.control.Label;
import javafx.scene.chart.*;
import javafx.scene.layout.VBox;

/** Live host measurements, independent of the benchmark's engine timing. */
final class BenchmarkUsageView extends VBox implements AutoCloseable {
    private final Label cpu = value("benchmark-live-cpu");
    private final Label heap = value("benchmark-live-heap");
    private final Label ram = value("benchmark-live-ram");
    private final Label gpu = value("benchmark-live-gpu");
    private final Label vram = value("benchmark-live-vram");
    private final XYChart.Series<Number, Number> cpuHistory = new XYChart.Series<>();
    private final XYChart.Series<Number, Number> heapHistory = new XYChart.Series<>();
    private final XYChart.Series<Number, Number> ramHistory = new XYChart.Series<>();
    private final XYChart.Series<Number, Number> gpuHistory = new XYChart.Series<>();
    private final XYChart.Series<Number, Number> vramHistory = new XYChart.Series<>();
    private final long started = System.nanoTime();
    private final ScheduledExecutorService sampler = Executors.newSingleThreadScheduledExecutor(
            Thread.ofPlatform().daemon(true).name("visionlab-benchmark-telemetry").factory());
    private ScheduledFuture<?> poll;
    private boolean closed;

    BenchmarkUsageView() {
        super(12);
        setId("benchmark-live-usage");
        getChildren().add(UiCards.columns(UiCards.panel("LIVE SYSTEM CPU", cpu, chart(cpuHistory, "cpu")),
                UiCards.panel("LIVE SYSTEM RAM", ram, chart(ramHistory, "ram")),
                UiCards.panel("LIVE JVM HEAP", heap, chart(heapHistory, "heap")),
                UiCards.panel("LIVE GPU LOAD", gpu, chart(gpuHistory, "gpu")),
                UiCards.panel("LIVE GPU VRAM", vram, chart(vramHistory, "vram"))));
        sceneProperty().addListener((observable, before, after) -> {
            if (after == null) stop(); else start();
        });
    }

    private LineChart<Number, Number> chart(XYChart.Series<Number, Number> history, String name) {
        NumberAxis time = new NumberAxis();
        time.setForceZeroInRange(false);
        NumberAxis percent = new NumberAxis(0, 100, 25);
        for (NumberAxis axis : new NumberAxis[]{time, percent}) {
            axis.setTickLabelsVisible(false); axis.setTickMarkVisible(false);
            axis.setMinorTickVisible(false); axis.setOpacity(0);
        }
        LineChart<Number, Number> chart = new LineChart<>(time, percent);
        chart.setId("benchmark-history-" + name);
        chart.getData().add(history);
        chart.setAnimated(false); chart.setCreateSymbols(false); chart.setLegendVisible(false);
        chart.setHorizontalGridLinesVisible(false); chart.setVerticalGridLinesVisible(false);
        chart.setHorizontalZeroLineVisible(false); chart.setVerticalZeroLineVisible(false);
        chart.setMinSize(0, 60); chart.setPrefHeight(70); chart.setMaxHeight(70);
        chart.setAccessibleText(name + " live history, percent, latest 60 samples");
        return chart;
    }

    private void append(XYChart.Series<Number, Number> history, double percent) {
        // Unavailable readings are never plotted as zero.
        if (!Double.isFinite(percent)) return;
        double seconds = (System.nanoTime() - started) / 1_000_000_000.0;
        history.getData().add(new XYChart.Data<>(seconds, percent));
        if (history.getData().size() > 60) history.getData().removeFirst();
    }

    private static Label value(String id) {
        Label label = UiCards.label("Sampling…", "live-value");
        label.setId(id);
        return label;
    }

    private void start() {
        if (closed || poll != null) return;
        poll = sampler.scheduleWithFixedDelay(() -> {
            RuntimeSnapshot runtime = RuntimeSnapshot.capture();
            var nvidia = NvidiaTelemetry.sample();
            Platform.runLater(() -> {
                if (closed || getScene() == null) return;
                cpu.setText(runtime.cpuLoad() < 0 ? "Unavailable" : number(runtime.cpuLoad() * 100, "%"));
                heap.setText(number(runtime.usedHeapBytes() / 1048576.0, " MB"));
                ram.setText(runtime.memorySummary());
                gpu.setText(nvidia.map(reading -> number(reading.utilization(), "%")).orElse("Unavailable"));
                vram.setText(nvidia.map(reading -> number(reading.usedMiB(), "") + " / "
                        + number(reading.totalMiB(), " MiB")).orElse("Unavailable"));
                append(cpuHistory, runtime.cpuLoad() >= 0 ? runtime.cpuLoad() * 100 : Double.NaN);
                append(heapHistory, 100.0 * runtime.usedHeapBytes() / runtime.maxHeapBytes());
                append(ramHistory, runtime.memoryFraction() * 100);
                append(gpuHistory, nvidia.map(NvidiaTelemetry.Snapshot::utilization).orElse(Double.NaN));
                append(vramHistory, nvidia.map(reading -> 100.0 * reading.usedMiB() / reading.totalMiB()).orElse(Double.NaN));
            });
        }, 0, 2, TimeUnit.SECONDS);
    }

    private static String number(double value, String unit) {
        return Double.isFinite(value) ? String.format(Locale.ROOT, "%.0f%s", value, unit) : "Unavailable";
    }

    private void stop() {
        if (poll != null) { poll.cancel(true); poll = null; }
    }

    @Override public void close() { closed = true; stop(); sampler.shutdownNow(); }
}
