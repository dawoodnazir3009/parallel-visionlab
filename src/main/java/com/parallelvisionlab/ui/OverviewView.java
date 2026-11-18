package com.parallelvisionlab.ui;

import java.util.function.Consumer;
import javafx.scene.control.Button;
import javafx.scene.layout.VBox;

final class OverviewView extends VBox {
    OverviewView(Consumer<LabPage> navigate, LabSession session) {
        super(14);
        Button workspace = new Button("Open Image Workspace →");
        workspace.getStyleClass().add("primary");
        workspace.setOnAction(event -> navigate.accept(LabPage.WORKSPACE));
        Button benchmark = new Button("View Benchmark Lab →");
        benchmark.setOnAction(event -> navigate.accept(LabPage.BENCHMARK));
        Button monitor = new Button("Open Live Monitor →");
        monitor.setOnAction(event -> navigate.accept(LabPage.MONITOR));
        VBox measured=new VBox(12);
        Runnable refresh=()->{
            measured.getChildren().clear();
            if(session.history.isEmpty()){measured.getChildren().add(UiCards.empty("No measured experiments yet","Run a CPU comparison in Benchmark Lab."));return;}
            var run=session.history.getFirst();
            double largest=run.measurements().stream().mapToDouble(com.parallelvisionlab.benchmark.BenchmarkRun.Measurement::medianMs).max().orElse(1);
            for(var result:run.measurements()){
                var bar=new javafx.scene.control.ProgressBar(result.medianMs()/largest);bar.setMaxWidth(Double.MAX_VALUE);
                measured.getChildren().add(new VBox(6,UiCards.label(result.backend()+" · "+String.format(java.util.Locale.ROOT,"%.3f ms",result.medianMs()),"chart-label"),bar));
            }
        };
        session.history.addListener((javafx.collections.ListChangeListener<com.parallelvisionlab.benchmark.BenchmarkRun>)change->refresh.run());refresh.run();
        getChildren().addAll(
                UiCards.columns(
                        UiCards.metric("LOGICAL PROCESSORS", String.valueOf(Runtime.getRuntime().availableProcessors()), "Available to this JVM"),
                        UiCards.metric("GPU PROCESSING", com.parallelvisionlab.engine.CudaProcessor.available()?"CUDA ready":"Not built", "See Live Monitor for driver telemetry"),
                        UiCards.metric("JVM HEAP LIMIT", String.format(java.util.Locale.ROOT, "%.1f GB", Runtime.getRuntime().maxMemory() / 1073741824.0), "Not total system RAM"),
                        UiCards.metric("PROCESSING ENGINES", com.parallelvisionlab.engine.CudaProcessor.available()?"5 available":"3 available", "3 Java engines + optional basic/tiled CUDA")),
                UiCards.columns(
                        UiCards.panel("Processing workspace", UiCards.empty("Start a new image experiment", "Load an image and inspect it at its original resolution."), workspace),
                        UiCards.panel("Latest measured CPU comparison", measured,
                                UiCards.label("Measured medians from validated experiments. Lower is faster.", "muted"), benchmark)),
                UiCards.panel("Hardware telemetry", UiCards.label("Inspect actual CPU/JVM resources and optional NVIDIA load, VRAM, power and temperature.", "muted"), monitor));
    }
}
