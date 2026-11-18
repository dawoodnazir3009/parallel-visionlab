package com.parallelvisionlab.ui;

import com.parallelvisionlab.model.*;
import com.parallelvisionlab.benchmark.BenchmarkRun;
import com.parallelvisionlab.benchmark.BenchmarkArchive;
import com.parallelvisionlab.benchmark.BenchmarkConfig;
import com.parallelvisionlab.benchmark.BenchmarkPreferences;
import java.nio.file.Path;
import javafx.beans.property.*;
import javafx.collections.*;

/** Shared UI state; properties are updated only on the JavaFX thread. */
final class LabSession {
    final ObjectProperty<PixelImage> input = new SimpleObjectProperty<>();
    final StringProperty sourceName = new SimpleStringProperty("image");
    final ObjectProperty<ProcessingRequest> request = new SimpleObjectProperty<>(new ProcessingRequest(FilterOperation.GRAYSCALE,2,1.2));
    final BooleanProperty busy = new SimpleBooleanProperty();
    final ObservableList<BenchmarkRun> history = FXCollections.observableArrayList();
    final BenchmarkArchive archive;
    final StringProperty archiveStatus = new SimpleStringProperty("");
    final BenchmarkPreferences preferenceStore;
    final ObjectProperty<BenchmarkConfig> preferences=new SimpleObjectProperty<>(new BenchmarkConfig(3,10,com.parallelvisionlab.service.ProcessingEngines.automaticWorkers(),32));
    LabSession() {
        this(Path.of(".tools"));
    }
    LabSession(Path dataDirectory) {
        archive=new BenchmarkArchive(dataDirectory.resolve("experiments"));
        preferenceStore=new BenchmarkPreferences(dataDirectory.resolve("preferences.properties"));
        try {preferences.set(preferenceStore.load(preferences.get()));}catch(java.io.IOException exception){archiveStatus.set(exception.getMessage());}
        try {
            var loaded=archive.load();history.setAll(loaded.runs());
            if(loaded.skipped()>0)archiveStatus.set(loaded.skipped()+" invalid experiment file(s) skipped.");
        } catch(java.io.IOException exception){archiveStatus.set("Could not restore history: "+exception.getMessage());}
    }
}
