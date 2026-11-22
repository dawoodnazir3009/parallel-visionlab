package com.parallelvisionlab.ui;

import com.parallelvisionlab.benchmark.*;
import javafx.beans.property.ReadOnlyStringWrapper;
import javafx.concurrent.Task;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import java.util.Locale;
import java.util.function.Function;

/** Real measurements over the image and filter selected in Image Workspace. */
final class MeasuredBenchmarkView extends VBox implements AutoCloseable {
    private final LabSession session;
    private final Label status = UiCards.label("Load an image and choose a filter in Image Workspace first.", "muted");
    private final Label summary = UiCards.label("No benchmark has been run", "metric-value");
    private final TableView<BenchmarkRun.Measurement> table = new TableView<>();
    private final VBox bars = new VBox(12);
    private final ProgressBar progress = new ProgressBar(0);
    private final BenchmarkUsageView usage = new BenchmarkUsageView();
    private Task<BenchmarkRun> active;

    MeasuredBenchmarkView(LabSession session) {
        super(14);
        this.session = session;
        var preferences=session.preferences.get();
        Spinner<Integer> warmups = new Spinner<>(0,20,preferences.warmups());
        Spinner<Integer> repeats = new Spinner<>(1,100,preferences.iterations());
        Spinner<Integer> workers = new Spinner<>(1,com.parallelvisionlab.service.CpuLimits.MAX_WORKERS,preferences.workers());
        Spinner<Integer> threshold = new Spinner<>(1,2048,preferences.leafRows());
        session.preferences.addListener((observable,before,after)->{
            warmups.getValueFactory().setValue(after.warmups());repeats.getValueFactory().setValue(after.iterations());
            workers.getValueFactory().setValue(after.workers());threshold.getValueFactory().setValue(after.leafRows());
        });
        CheckBox includeCuda=new CheckBox("Include CUDA (process-bridge timings)");
        includeCuda.setId("include-cuda");
        includeCuda.setSelected(com.parallelvisionlab.engine.CudaProcessor.available());
        includeCuda.disableProperty().bind(session.busy.or(new javafx.beans.property.SimpleBooleanProperty(!com.parallelvisionlab.engine.CudaProcessor.available())));
        Button run = new Button("Run Backend Comparison");
        run.setId("run-benchmark");
        run.getStyleClass().add("primary");
        run.disableProperty().bind(session.input.isNull().or(session.busy));
        Button cancel = new Button("Cancel");
        cancel.setDisable(true);
        cancel.setOnAction(event -> { if(active!=null)active.cancel(true); });
        for (Spinner<?> spinner : new Spinner<?>[]{warmups,repeats,workers,threshold}) spinner.disableProperty().bind(session.busy);
        progress.setMaxWidth(Double.MAX_VALUE);
        run.setOnAction(event -> {
            var input = session.input.get();
            var request = session.request.get();
            String name = session.sourceName.get();
            var config = new BenchmarkConfig(warmups.getValue(),repeats.getValue(),workers.getValue(),threshold.getValue());
            boolean useCuda=includeCuda.isSelected();
            session.busy.set(true); cancel.setDisable(false);
            status.setText("Measuring " + request.operation() + " · " + name + ". Every output is validated against sequential.");
            active = new Task<>() {
                @Override protected BenchmarkRun call() {
                    var result=new BenchmarkRunner(useCuda).run(input,name,request,config,value -> updateProgress(value,1));
                    try {session.archive.save(result);updateMessage("Results saved. CPU validation exact; GPU RGB tolerance 1, alpha exact.");}
                    catch(java.io.IOException exception){updateMessage("Measured results available this session; saving history failed: "+exception.getMessage());}
                    return result;
                }
            };
            progress.progressProperty().bind(active.progressProperty());
            active.setOnSucceeded(done -> {
                String message=active.getMessage();
                session.history.add(0,active.getValue());
                finish(cancel);
                status.setText(message);
            });
            active.setOnFailed(done -> {
                String error = active.getException().getMessage(); finish(cancel);
                status.setText("Benchmark failed; no partial results recorded: " + error);
            });
            active.setOnCancelled(done -> { finish(cancel); status.setText("Cancelled; no partial experiment recorded."); });
            Task<BenchmarkRun> task = active;
            Thread thread = new Thread(() -> {
                try { task.run(); }
                finally {
                    // Outside all timed measurements and after the runner stack has unwound.
                    // The JVM decides how much unused heap to return to the operating system.
                    System.gc();
                }
            },"visionlab-benchmark"); thread.setDaemon(true); thread.start();
        });
        addColumn("Backend",m -> m.backend().toString());
        addColumn("Median ms",m -> format(m.medianMs()));
        addColumn("Min / Max ms",m -> format(m.minimumMs()) + " / " + format(m.maximumMs()));
        addColumn("Speedup",m -> format(m.speedup()) + "x");
        addColumn("Efficiency",m -> m.backend().gpu()?"N/A":format(m.efficiency()*100) + "%");
        addColumn("MP/s",m -> format(m.megapixelsPerSecond()));
        addColumn("GPU kernel ms",m -> m.backend().gpu()?format(m.kernelMs()):"—");
        addColumn("GPU H2D / D2H ms",m -> m.backend().gpu()?format(m.uploadMs())+" / "+format(m.downloadMs()):"—");
        table.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_FLEX_LAST_COLUMN);
        table.setMinHeight(200);
        table.setPrefHeight(210);
        table.setPlaceholder(UiCards.label("Run a comparison to record measured CPU results.","muted"));
        TabPane results = new TabPane(new Tab("Measurements", table), new Tab("Runtime comparison", bars));
        results.setTabClosingPolicy(TabPane.TabClosingPolicy.UNAVAILABLE);
        results.setPrefHeight(240);
        summary.getStyleClass().remove("metric-value");
        summary.getStyleClass().add("panel-title");
        Label timingNotes = UiCards.label("Live graphs: latest 60 samples, about 2 seconds apart; vertical scale 0–100%. RAM shows physical system memory used / total; JVM heap shows Java memory only. CPU/GPU are system-wide. Brief workloads may fall between samples; telemetry adds background activity.", "muted");
        TitledPane methodology = new TitledPane("Timing and validation details", UiCards.label(
                "CPU: engine wall-clock. GPU: total process bridge, including startup, files and transfers. GPU event kernel/H2D/D2H times are separate. Validation, decoding and rendering are excluded. Warmups excluded; fixed backend order and other workloads affect results. GPU efficiency is N/A; lower runtime is not guaranteed.", "muted"));
        methodology.setExpanded(false);
        getChildren().addAll(usage, UiCards.panel("Benchmark configuration",
                UiCards.columns(field("Warm-up runs",warmups),field("Measured iterations",repeats),field("CPU workers",workers),field("Fork/Join leaf rows",threshold)),
                includeCuda,new HBox(10,run,cancel),progress,status),
                UiCards.panel("Latest measured experiment",summary,results), timingNotes, methodology);
        session.history.addListener((javafx.collections.ListChangeListener<BenchmarkRun>) change -> refresh());
        refresh();
    }
    private VBox field(String name, Control control) { return new VBox(6,UiCards.label(name,"muted"),control); }
    private void addColumn(String name, Function<BenchmarkRun.Measurement,String> value) {
        TableColumn<BenchmarkRun.Measurement,String> column = new TableColumn<>(name);
        column.setCellValueFactory(cell -> new ReadOnlyStringWrapper(value.apply(cell.getValue())));
        table.getColumns().add(column);
    }
    private void finish(Button cancel) {
        progress.progressProperty().unbind(); progress.setProgress(0);
        session.busy.set(false); cancel.setDisable(true); active=null;
    }
    private void refresh() {
        if(session.history.isEmpty())return;
        var run=session.history.getFirst();
        summary.setText(run.request().operation() + " · " + run.width() + " × " + run.height());
        table.getItems().setAll(run.measurements()); bars.getChildren().clear();
        double largest=run.measurements().stream().mapToDouble(BenchmarkRun.Measurement::medianMs).max().orElse(1);
        int index=0;
        for(var m:run.measurements()) {
            ProgressBar bar=new ProgressBar(m.medianMs()/largest);bar.setMaxWidth(Double.MAX_VALUE);bar.getStyleClass().add("backend-"+index++);
            bars.getChildren().add(new VBox(6,UiCards.label(m.backend()+" · "+format(m.medianMs())+" ms · validated","chart-label"),bar));
        }
    }
    private static String format(double value) {return String.format(Locale.ROOT,"%.3f",value);}
    @Override public void close(){usage.close();if(active!=null)active.cancel(true);}
}
