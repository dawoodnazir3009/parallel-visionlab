package com.parallelvisionlab.ui;

import com.parallelvisionlab.benchmark.BenchmarkConfig;
import javafx.scene.control.*;
import javafx.scene.layout.VBox;

/** Persistent preferences consumed by the measured benchmark runner. */
final class SettingsView extends VBox {
    SettingsView(LabSession session) {
        super(14);
        var preferences=session.preferences.get();
        Spinner<Integer> warmups=new Spinner<>(0,20,preferences.warmups());
        Spinner<Integer> iterations=new Spinner<>(1,100,preferences.iterations());
        Spinner<Integer> workers=new Spinner<>(1,com.parallelvisionlab.service.CpuLimits.MAX_WORKERS,preferences.workers());
        Spinner<Integer> leafRows=new Spinner<>(1,2048,preferences.leafRows());
        Label feedback=UiCards.label("Preferences are saved locally under .tools/preferences.properties.","muted");
        Button apply=new Button("Save Benchmark Preferences");apply.getStyleClass().add("primary");
        apply.disableProperty().bind(session.busy);
        for(Spinner<?> spinner:new Spinner<?>[]{warmups,iterations,workers,leafRows})spinner.disableProperty().bind(session.busy);
        apply.setOnAction(event->{
            var config=new BenchmarkConfig(warmups.getValue(),iterations.getValue(),workers.getValue(),leafRows.getValue());
            session.preferences.set(config);
            try {session.preferenceStore.save(config);feedback.setText("Saved. Benchmark Lab now uses these defaults.");}
            catch(java.io.IOException exception){feedback.setText("Applied this session; saving failed: "+exception.getMessage());}
        });
        getChildren().add(UiCards.columns(
                UiCards.panel("Detected runtime",field("Java runtime",System.getProperty("java.version")),
                        field("Operating system",System.getProperty("os.name")+" · "+System.getProperty("os.arch")),
                        field("Logical processors",""+Runtime.getRuntime().availableProcessors()),
                        field("Application CPU limit",com.parallelvisionlab.service.CpuLimits.affinityStatus()),
                        field("GPU processing",com.parallelvisionlab.engine.CudaProcessor.available()?"Basic + tiled CUDA executable available":"Run install-cuda.ps1 and build-cuda.ps1 to enable"),
                        UiCards.label("Runtime values are detected. CPU benchmarks use engine wall-clock timing, not simulated telemetry.","muted")),
                UiCards.panel("Benchmark preferences",control("Warm-up runs",warmups),control("Measured iterations",iterations),
                        control("CPU workers",workers),control("Fork/Join leaf rows",leafRows),apply,feedback)));
    }
    private VBox field(String name,String value){TextField field=new TextField(value);field.setEditable(false);return control(name,field);}
    private VBox control(String name,Control field){Label label=UiCards.label(name,"muted");label.setLabelFor(field);return new VBox(6,label,field);}
}
