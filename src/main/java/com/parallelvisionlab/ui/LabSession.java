package com.parallelvisionlab.ui;

import com.parallelvisionlab.model.*;
import com.parallelvisionlab.benchmark.BenchmarkRun;
import com.parallelvisionlab.benchmark.BenchmarkArchive;
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
    final BenchmarkArchive archive = new BenchmarkArchive(Path.of(".tools","experiments"));
    final StringProperty archiveStatus = new SimpleStringProperty("");
    LabSession() {
        try {
            var loaded=archive.load();history.setAll(loaded.runs());
            if(loaded.skipped()>0)archiveStatus.set(loaded.skipped()+" invalid experiment file(s) skipped.");
        } catch(java.io.IOException exception){archiveStatus.set("Could not restore history: "+exception.getMessage());}
    }
}
