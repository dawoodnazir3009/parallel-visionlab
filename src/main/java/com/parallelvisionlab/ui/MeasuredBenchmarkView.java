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
    private Task<BenchmarkRun> active;

    MeasuredBenchmarkView(LabSession session) {
        super(14);
        this.session = session;
        Spinner<Integer> warmups = new Spinner<>(0,20,3);
        Spinner<Integer> repeats = new Spinner<>(1,100,10);
        Spinner<Integer> workers = new Spinner<>(1,256,com.parallelvisionlab.service.ProcessingEngines.automaticWorkers());
        Spinner<Integer> threshold = new Spinner<>(1,2048,32);
        Button run = new Button("Run CPU Comparison");
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
            session.busy.set(true); cancel.setDisable(false);
            status.setText("Measuring " + request.operation() + " · " + name + ". Each output is validated exactly.");
            active = new Task<>() {
                @Override protected BenchmarkRun call() {
                    var result=new BenchmarkRunner().run(input,name,request,config,value -> updateProgress(value,1));
                    try {session.archive.save(result);updateMessage("Measured results saved locally. All outputs passed exact validation.");}
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
            Thread thread = new Thread(active,"visionlab-benchmark"); thread.setDaemon(true); thread.start();
        });
        addColumn("Backend",m -> m.backend().toString());
        addColumn("Median ms",m -> format(m.medianMs()));
        addColumn("Min / Max ms",m -> format(m.minimumMs()) + " / " + format(m.maximumMs()));
        addColumn("Speedup",m -> format(m.speedup()) + "x");
        addColumn("Efficiency",m -> format(m.efficiency()*100) + "%");
        addColumn("MP/s",m -> format(m.megapixelsPerSecond()));
        table.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_FLEX_LAST_COLUMN);
        table.setPrefHeight(180);
        table.setPlaceholder(UiCards.label("Run a comparison to record measured CPU results.","muted"));
        getChildren().addAll(UiCards.panel("Benchmark configuration",
                UiCards.columns(field("Warm-up runs",warmups),field("Measured iterations",repeats),field("CPU workers",workers),field("Fork/Join leaf rows",threshold)),
                new HBox(10,run,cancel),progress,status),
                UiCards.panel("Latest measured experiment",summary,bars,table),
                UiCards.label("Engine wall-clock scope: includes allocations, scheduling and synchronization; excludes image decoding, validation, rendering and export. Sequential baseline → ExecutorService → Fork/Join. Median/min/max are measured; warmups are excluded. Background workloads and fixed backend order can affect results.","muted"));
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
            bars.getChildren().add(new VBox(6,UiCards.label(m.backend()+" · "+format(m.medianMs())+" ms · exact pass","chart-label"),bar));
        }
    }
    private static String format(double value) {return String.format(Locale.ROOT,"%.3f",value);}
    @Override public void close(){if(active!=null)active.cancel(true);}
}
