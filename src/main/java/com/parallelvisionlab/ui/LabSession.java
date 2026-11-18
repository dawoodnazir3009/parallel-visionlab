package com.parallelvisionlab.ui;

import com.parallelvisionlab.model.*;
import com.parallelvisionlab.benchmark.BenchmarkRun;
import javafx.beans.property.*;
import javafx.collections.*;

/** Shared UI state; properties are updated only on the JavaFX thread. */
final class LabSession {
    final ObjectProperty<PixelImage> input = new SimpleObjectProperty<>();
    final StringProperty sourceName = new SimpleStringProperty("image");
    final ObjectProperty<ProcessingRequest> request = new SimpleObjectProperty<>(new ProcessingRequest(FilterOperation.GRAYSCALE,2,1.2));
    final BooleanProperty busy = new SimpleBooleanProperty();
    final ObservableList<BenchmarkRun> history = FXCollections.observableArrayList();
}
