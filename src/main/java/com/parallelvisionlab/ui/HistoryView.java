package com.parallelvisionlab.ui;

import com.parallelvisionlab.benchmark.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.List;
import java.util.Locale;
import java.util.function.Function;
import javafx.beans.property.ReadOnlyStringWrapper;
import javafx.concurrent.Task;
import javafx.scene.control.*;
import javafx.scene.layout.VBox;
import javafx.stage.FileChooser;
import javafx.stage.Stage;

/** Persistent real experiment history and measured CSV export. */
final class HistoryView extends VBox implements AutoCloseable {
    private record Row(BenchmarkRun run, BenchmarkRun.Measurement measurement) { }
    private final TableView<Row> table=new TableView<>();
    private Task<Void> active;
    HistoryView(Stage owner,LabSession session) {
        super(14);
        Button export=new Button("Export Measured CSV");
        export.disableProperty().bind(javafx.beans.binding.Bindings.isEmpty(session.history).or(session.busy));
        Label status=UiCards.label("History is saved under .tools/experiments in this checkout.","muted");
        Label warning=UiCards.label(session.archiveStatus.get(),"sample-notice");
        export.setOnAction(event->{
            FileChooser chooser=new FileChooser();chooser.setTitle("Export measured experiments");chooser.setInitialFileName("visionlab-measured-benchmarks.csv");
            chooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("CSV files","*.csv"));
            var file=chooser.showSaveDialog(owner);if(file==null)return;
            var snapshot=List.copyOf(session.history);session.busy.set(true);
            active=new Task<>() {
                @Override protected Void call()throws Exception {
                    Files.writeString(file.toPath(),BenchmarkCsv.encode(snapshot),StandardCharsets.UTF_8);return null;
                }
            };
            active.setOnSucceeded(done->{session.busy.set(false);status.setText("Measured CSV exported to "+file.getAbsolutePath());active=null;});
            active.setOnFailed(done->{session.busy.set(false);status.setText("Export failed: "+active.getException().getMessage());active=null;});
            active.setOnCancelled(done->session.busy.set(false));
            Thread worker=new Thread(active,"visionlab-csv-export");worker.setDaemon(true);worker.start();
        });
        column("Date",row->row.run().timestamp().toString());
        column("Source",row->row.run().source());
        column("Operation",row->row.run().request().operation().toString());
        column("Backend",row->row.measurement().backend().toString());
        column("Median ms",row->String.format(Locale.ROOT,"%.3f",row.measurement().medianMs()));
        column("Validation",row->"Exact pass");
        table.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_FLEX_LAST_COLUMN);
        table.setPrefHeight(420);table.setPlaceholder(UiCards.label("No measured experiments yet. Run a comparison in Benchmark Lab.","muted"));
        Runnable refresh=()->{table.getItems().clear();for(var run:session.history)for(var measurement:run.measurements())table.getItems().add(new Row(run,measurement));};
        session.history.addListener((javafx.collections.ListChangeListener<BenchmarkRun>)change->refresh.run());refresh.run();
        getChildren().add(UiCards.panel("Recorded experiments",new javafx.scene.layout.HBox(10,export),warning,table,status));
    }
    private void column(String name,Function<Row,String> value) {
        TableColumn<Row,String> column=new TableColumn<>(name);
        column.setCellValueFactory(cell->new ReadOnlyStringWrapper(value.apply(cell.getValue())));table.getColumns().add(column);
    }
    @Override public void close(){if(active!=null)active.cancel(true);}
}
