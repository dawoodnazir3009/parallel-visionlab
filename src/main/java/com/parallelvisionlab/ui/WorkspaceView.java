package com.parallelvisionlab.ui;

import com.parallelvisionlab.service.ImageLoader;
import com.parallelvisionlab.service.PixelImages;
import com.parallelvisionlab.service.PngExporter;
import com.parallelvisionlab.model.PixelImage;
import com.parallelvisionlab.model.FilterOperation;
import com.parallelvisionlab.model.ProcessingRequest;
import com.parallelvisionlab.model.ExecutionBackend;
import com.parallelvisionlab.service.ProcessingEngines;
import java.io.File;
import java.util.Locale;
import java.util.function.Consumer;
import javafx.concurrent.Task;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.*;
import javafx.stage.FileChooser;
import javafx.stage.Stage;

/** Background sequential/multithreaded filters and full-resolution PNG export. */
public final class WorkspaceView extends BorderPane implements AutoCloseable {
    private final ImageLoader loader = new ImageLoader();
    private final LabSession session;
    private final ImageView preview = new ImageView();
    private final Label details = new Label("No image selected");
    private final Label status = new Label("Ready. Open an image to get started.");
    private final Button open = new Button("Choose Image");
    private final Button clear = new Button("Clear");
    private final Label empty = new Label("Your image will appear here\nPNG, JPEG, BMP or GIF");
    private final Stage owner;
    private final Button process = new Button("Apply Filter");
    private final Button cancel = new Button("Cancel");
    private final ComboBox<FilterOperation> operation = new ComboBox<>();
    private final ComboBox<ExecutionBackend> backend = new ComboBox<>();
    private final ComboBox<String> threads = choice("Automatic", "1", "2", "4", "8", "16");
    private final ProcessingEngines engines = new ProcessingEngines();
    private Task<?> activeTask;
    private boolean closed;
    private final Spinner<Integer> radius = new Spinner<>(1, 7, 2);
    private final Spinner<Integer> threshold = new Spinner<>(1, 2048, 32);
    private final Spinner<Double> sigma = new Spinner<>(0.1, 10.0, 1.2, 0.1);
    private final ProgressIndicator activity = new ProgressIndicator();
    private final Button save = new Button("Save PNG");
    private final ImageView outputPreview = new ImageView();
    private final Label outputEmpty = UiCards.label("Processed image appears here", "muted");
    private PixelImage source;
    private PixelImage result;
    private String sourceName = "image";
    private FilterOperation resultOperation;
    private boolean busy;
    private boolean cancellable;

    public WorkspaceView(Stage owner) {
        this(owner, new LabSession());
    }

    WorkspaceView(Stage owner, LabSession session) {
        this.owner = owner;
        this.session = session;
        session.busy.addListener((observable, before, after) -> updateBusy(after));
        getStyleClass().add("workspace");
        clear.setDisable(true);
        open.getStyleClass().add("primary");
        preview.setPreserveRatio(true);
        preview.setSmooth(true);
        preview.setAccessibleText("Selected input image preview");
        empty.setTextAlignment(javafx.scene.text.TextAlignment.CENTER);
        empty.getStyleClass().add("muted");
        ImagePreviewPane canvas = new ImagePreviewPane(preview,empty);
        details.setWrapText(true);
        status.getStyleClass().add("muted");
        status.setWrapText(true);
        HBox actions = new HBox(10, open, clear);
        VBox input = UiCards.panel("Input image", canvas, details, actions, status);
        operation.getItems().setAll(FilterOperation.values());
        operation.setValue(FilterOperation.GRAYSCALE);
        operation.setMaxWidth(Double.MAX_VALUE);
        operation.setId("filter-operation");
        radius.setId("gaussian-radius");
        threshold.setId("forkjoin-threshold");
        threshold.setMaxWidth(Double.MAX_VALUE);
        sigma.setId("gaussian-sigma");
        radius.setMaxWidth(Double.MAX_VALUE);
        sigma.setMaxWidth(Double.MAX_VALUE);
        backend.getItems().setAll(ProcessingEngines.availableBackends());
        backend.setValue(ExecutionBackend.SEQUENTIAL);
        backend.setMaxWidth(Double.MAX_VALUE);
        backend.setId("execution-backend");
        threads.setId("worker-threads");
        threads.setDisable(true);
        process.getStyleClass().add("primary");
        process.setDisable(true);
        process.setTooltip(new Tooltip("Apply the selected filter and backend to the original image."));
        activity.setPrefSize(22, 22);
        activity.setMaxSize(22, 22);
        activity.setAccessibleText("Image operation in progress");
        activity.setVisible(false);
        activity.setManaged(false);
        cancel.setDisable(true);
        cancel.setOnAction(event->{if(activeTask!=null){cancel.setDisable(true);status.setText("Cancelling processing...");activeTask.cancel(true);}});
        VBox configuration = UiCards.panel("Processing configuration",
                UiCards.columns(field("Image operation", operation), field("Execution backend", backend), field("CPU workers", threads)),
                UiCards.columns(field("Fork/Join leaf rows", threshold),
                field("Gaussian radius", radius), field("Gaussian sigma", sigma)),
                new HBox(12, process, cancel, activity), UiCards.label("All three Java CPU backends support every filter. CUDA backends appear when the native executable is built and require an NVIDIA driver.", "sample-notice"));
        configuration.setId("workspace-configuration");
        save.setDisable(true);
        outputPreview.setPreserveRatio(true);
        outputPreview.setAccessibleText("Processed filter output preview");
        ImagePreviewPane outputCanvas = new ImagePreviewPane(outputPreview,outputEmpty);
        VBox output = UiCards.panel("Processed output", outputCanvas, save);
        setCenter(new VBox(12, configuration, UiCards.columns(input, output)));
        open.setOnAction(event -> chooseImage());
        process.setOnAction(event -> processImage());
        save.setOnAction(event -> saveResult());
        operation.valueProperty().addListener((observable, before, after) -> configurationChanged());
        radius.valueProperty().addListener((observable, before, after) -> configurationChanged());
        sigma.valueProperty().addListener((observable, before, after) -> configurationChanged());
        backend.valueProperty().addListener((observable, before, after) -> configurationChanged());
        threads.valueProperty().addListener((observable, before, after) -> configurationChanged());
        threshold.valueProperty().addListener((observable, before, after) -> configurationChanged());
        updateParameterControls();
        clear.setOnAction(event -> {
            preview.setImage(null);
            source = null;
            session.input.set(null);
            resetOutput();
            empty.setVisible(true);
            details.setText("No image selected");
            status.setText("Ready. Open an image to get started.");
            clear.setDisable(true);
            process.setDisable(true);
        });
    }

    private ComboBox<String> choice(String... values) {
        ComboBox<String> choice = new ComboBox<>();
        choice.getItems().addAll(values);
        choice.getSelectionModel().selectFirst();
        choice.setMaxWidth(Double.MAX_VALUE);
        return choice;
    }

    private VBox field(String name, Control control) {
        Label label = UiCards.label(name, "muted");
        label.setLabelFor(control);
        return new VBox(6, label, control);
    }

    private void chooseImage() {
        FileChooser chooser = new FileChooser();
        chooser.setTitle("Open image");
        chooser.getExtensionFilters().addAll(
                new FileChooser.ExtensionFilter("Images", "*.png", "*.jpg", "*.jpeg", "*.bmp", "*.gif"),
                new FileChooser.ExtensionFilter("All files", "*.*"));
        File file = chooser.showOpenDialog(owner);
        if (file == null) {
            return;
        }
        status.setText("Loading " + file.getName() + "...");
        Task<LoadedImage> task = new Task<>() {
            @Override
            protected LoadedImage call() throws Exception {
                Image image = loader.load(file.toPath());
                return new LoadedImage(image, PixelImages.fromFx(image));
            }
        };
        runTask(task, loaded -> {
            Image image = loaded.preview();
            source = loaded.pixels();
            sourceName = file.getName();
            session.input.set(source);
            session.sourceName.set(sourceName);
            resetOutput();
            preview.setImage(image);
            empty.setVisible(false);
            details.setText(file.getName() + "  |  " + (int) image.getWidth() + " × "
                    + (int) image.getHeight() + " pixels");
            status.setText("Image loaded. Preview fits the window; original resolution is preserved.");
        });
    }

    private void processImage() {
        PixelImage input = source;
        if (input == null) { return; }
        ProcessingRequest request = new ProcessingRequest(operation.getValue(), radius.getValue(), sigma.getValue());
        ExecutionBackend selectedBackend = backend.getValue();
        int workers = selectedBackend == ExecutionBackend.SEQUENTIAL || selectedBackend.gpu() ? 1 :
                (threads.getValue().equals("Automatic") ? ProcessingEngines.automaticWorkers() : Integer.parseInt(threads.getValue()));
        String execution = selectedBackend.gpu()? selectedBackend+" (process bridge)" : selectedBackend + " (" + workers + " configured worker" + (workers == 1 ? "" : "s") + ")";
        int leafRows = threshold.getValue();
        status.setText("Applying " + request.operation() + " using " + execution + "...");
        Task<ProcessedImage> task = new Task<>() {
            @Override protected ProcessedImage call() {
                var engine = engines.select(selectedBackend, workers, leafRows);
                long start = System.nanoTime();
                PixelImage pixels = engine.process(input, request);
                long elapsed = System.nanoTime() - start;
                var expected=selectedBackend==ExecutionBackend.SEQUENTIAL?pixels:new com.parallelvisionlab.engine.SequentialProcessor().process(input,request);
                if(!com.parallelvisionlab.service.ImageValidation.compare(expected,pixels,selectedBackend.gpu()?1:0).passed())throw new IllegalStateException("Output validation failed; result was not accepted.");
                return new ProcessedImage(pixels, PixelImages.toFx(pixels), elapsed);
            }
        };
        runTask(task, processed -> {
            result = processed.pixels();
            resultOperation = request.operation();
            outputPreview.setImage(processed.preview());
            outputEmpty.setVisible(false);
            status.setText(String.format(Locale.ROOT, "%s · %s · %.2f ms single execution (not a benchmark). Original preserved.", request.operation(), execution, processed.nanoseconds() / 1_000_000.0));
        }, true);
    }

    private void saveResult() {
        if (result == null) { return; }
        FileChooser chooser = new FileChooser();
        chooser.setTitle("Save " + resultOperation + " PNG");
        int dot = sourceName.lastIndexOf('.');
        chooser.setInitialFileName((dot > 0 ? sourceName.substring(0, dot) : sourceName) + "-" + resultOperation.fileSuffix() + ".png");
        chooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("PNG image", "*.png"));
        File file = chooser.showSaveDialog(owner);
        if (file == null) { return; }
        PixelImage output = result;
        status.setText("Saving PNG...");
        Task<Void> task = new Task<>() {
            @Override protected Void call() throws Exception {
                new PngExporter().save(output, file.toPath());
                return null;
            }
        };
        runTask(task, ignored -> status.setText("Full-resolution PNG saved to " + file.getAbsolutePath()));
    }

    private void resetOutput() {
        result = null;
        resultOperation = null;
        outputPreview.setImage(null);
        outputEmpty.setVisible(true);
        save.setDisable(true);
    }

    private void setBusy(boolean busy) {
        session.busy.set(busy);
        updateBusy(busy);
    }

    private void updateBusy(boolean busy) {
        this.busy = busy;
        cancel.setDisable(!busy||activeTask==null||!cancellable);
        activity.setVisible(busy);
        activity.setManaged(busy);
        operation.setDisable(busy);
        backend.setDisable(busy);
        updateParameterControls();
        open.setDisable(busy);
        clear.setDisable(busy || source == null);
        process.setDisable(busy || source == null);
        save.setDisable(busy || result == null);
    }

    private void updateParameterControls() {
        threads.setDisable(busy || backend.getValue() == ExecutionBackend.SEQUENTIAL || backend.getValue().gpu());
        threshold.setDisable(busy || backend.getValue() != ExecutionBackend.FORK_JOIN);
        boolean unavailable = busy || operation.getValue() != FilterOperation.GAUSSIAN;
        radius.setDisable(unavailable);
        sigma.setDisable(unavailable);
    }

    private void configurationChanged() {
        session.request.set(new ProcessingRequest(operation.getValue(), radius.getValue(), sigma.getValue()));
        resetOutput();
        updateParameterControls();
        if (source != null) { status.setText("Configuration changed. Apply Filter to generate a new result from the original."); }
    }

    private <T> void runTask(Task<T> task, Consumer<T> success) {
        runTask(task,success,false);
    }

    private <T> void runTask(Task<T> task, Consumer<T> success, boolean cancellable) {
        if (closed) { return; }
        this.cancellable=cancellable;
        activeTask = task;
        setBusy(true);
        task.setOnSucceeded(event -> {
            activeTask = null;
            if (closed) { return; }
            try { success.accept(task.getValue()); } finally { setBusy(false); }
        });
        task.setOnFailed(event -> {
            activeTask = null;
            if (closed) { return; }
            setBusy(false);
            status.setText("Operation failed. Your previous image and result are preserved.");
            Alert alert = new Alert(Alert.AlertType.ERROR, task.getException().getMessage());
            alert.initOwner(owner);
            alert.setTitle("Image operation failed");
            alert.setHeaderText("The operation could not be completed.");
            alert.showAndWait();
        });
        Thread worker = new Thread(()->{
            try{task.run();}finally{
                if(task.isCancelled())javafx.application.Platform.runLater(()->{
                    if(!closed&&activeTask==task){activeTask=null;setBusy(false);status.setText("Processing cancelled. Previous input/result preserved.");}
                });
            }
        }, "visionlab-image-task");
        worker.setDaemon(true);
        worker.start();
    }

    @Override public void close() {
        closed = true;
        if (activeTask != null) { activeTask.cancel(true); }
        engines.close();
    }

    private record LoadedImage(Image preview, PixelImage pixels) { }
    private record ProcessedImage(PixelImage pixels, Image preview, long nanoseconds) { }
}
