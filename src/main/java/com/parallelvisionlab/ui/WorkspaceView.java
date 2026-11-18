package com.parallelvisionlab.ui;

import com.parallelvisionlab.service.ImageLoader;
import com.parallelvisionlab.service.PixelImages;
import com.parallelvisionlab.service.PngExporter;
import com.parallelvisionlab.model.PixelImage;
import com.parallelvisionlab.model.FilterOperation;
import com.parallelvisionlab.model.ProcessingRequest;
import com.parallelvisionlab.engine.SequentialProcessor;
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

/** Background sequential filters and full-resolution PNG export in the dashboard workspace. */
public final class WorkspaceView extends BorderPane {
    private final ImageLoader loader = new ImageLoader();
    private final ImageView preview = new ImageView();
    private final Label details = new Label("No image selected");
    private final Label status = new Label("Ready. Open an image to get started.");
    private final Button open = new Button("Choose Image");
    private final Button clear = new Button("Clear");
    private final Label empty = new Label("Your image will appear here\nPNG, JPEG, BMP or GIF");
    private final Stage owner;
    private final Button process = new Button("Apply Filter");
    private final ComboBox<FilterOperation> operation = new ComboBox<>();
    private final Spinner<Integer> radius = new Spinner<>(1, 7, 2);
    private final Spinner<Double> sigma = new Spinner<>(0.1, 10.0, 1.2, 0.1);
    private final Button save = new Button("Save PNG");
    private final ImageView outputPreview = new ImageView();
    private final Label outputEmpty = UiCards.label("Processed image appears here", "muted");
    private PixelImage source;
    private PixelImage result;
    private String sourceName = "image";
    private FilterOperation resultOperation;
    private boolean busy;

    public WorkspaceView(Stage owner) {
        this.owner = owner;
        getStyleClass().add("workspace");
        clear.setDisable(true);
        open.getStyleClass().add("primary");
        preview.setPreserveRatio(true);
        preview.setSmooth(true);
        preview.setAccessibleText("Selected input image preview");
        empty.setTextAlignment(javafx.scene.text.TextAlignment.CENTER);
        empty.getStyleClass().add("muted");
        StackPane canvas = new StackPane(empty, preview);
        canvas.getStyleClass().add("canvas");
        canvas.setMinSize(0, 225);
        canvas.setPrefHeight(270);
        preview.fitWidthProperty().bind(canvas.widthProperty().subtract(40));
        preview.fitHeightProperty().bind(canvas.heightProperty().subtract(40));
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
        sigma.setId("gaussian-sigma");
        radius.setMaxWidth(Double.MAX_VALUE);
        sigma.setMaxWidth(Double.MAX_VALUE);
        ComboBox<String> backend = choice("Sequential Java");
        ComboBox<String> threads = choice("Automatic", "2", "4", "8", "16");
        backend.setDisable(true);
        threads.setDisable(true);
        process.getStyleClass().add("primary");
        process.setDisable(true);
        process.setTooltip(new Tooltip("Apply the selected filter to the original image using sequential Java."));
        VBox configuration = UiCards.panel("Processing configuration",
                field("Image operation", operation), field("Execution backend", backend), field("CPU worker threads", threads),
                field("Gaussian radius (kernel = 2r + 1)", radius), field("Gaussian sigma", sigma),
                process, UiCards.label("All four sequential filters are available. Borders replicate the nearest pixel; alpha is preserved. Parallel/CUDA engines are not connected.", "sample-notice"));
        save.setDisable(true);
        outputPreview.setPreserveRatio(true);
        outputPreview.setAccessibleText("Processed filter output preview");
        StackPane outputCanvas = new StackPane(outputEmpty, outputPreview);
        outputCanvas.getStyleClass().add("canvas");
        outputCanvas.setMinSize(0, 225);
        outputCanvas.setPrefHeight(270);
        outputPreview.fitWidthProperty().bind(outputCanvas.widthProperty().subtract(40));
        outputPreview.fitHeightProperty().bind(outputCanvas.heightProperty().subtract(40));
        VBox output = UiCards.panel("Processed output", outputCanvas, save);
        setCenter(new VBox(14, UiCards.columns(input, configuration), output));
        open.setOnAction(event -> chooseImage());
        process.setOnAction(event -> processImage());
        save.setOnAction(event -> saveResult());
        operation.valueProperty().addListener((observable, before, after) -> configurationChanged());
        radius.valueProperty().addListener((observable, before, after) -> configurationChanged());
        sigma.valueProperty().addListener((observable, before, after) -> configurationChanged());
        updateParameterControls();
        clear.setOnAction(event -> {
            preview.setImage(null);
            source = null;
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
        status.setText("Applying " + request.operation() + " using sequential Java...");
        Task<ProcessedImage> task = new Task<>() {
            @Override protected ProcessedImage call() {
                long start = System.nanoTime();
                PixelImage pixels = new SequentialProcessor().process(input, request);
                long elapsed = System.nanoTime() - start;
                return new ProcessedImage(pixels, PixelImages.toFx(pixels), elapsed);
            }
        };
        runTask(task, processed -> {
            result = processed.pixels();
            resultOperation = request.operation();
            outputPreview.setImage(processed.preview());
            outputEmpty.setVisible(false);
            status.setText(String.format(Locale.ROOT, "%s complete · %.2f ms single execution (not a benchmark). Original preserved.", request.operation(), processed.nanoseconds() / 1_000_000.0));
        });
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
        this.busy = busy;
        operation.setDisable(busy);
        updateParameterControls();
        open.setDisable(busy);
        clear.setDisable(busy || source == null);
        process.setDisable(busy || source == null);
        save.setDisable(busy || result == null);
    }

    private void updateParameterControls() {
        boolean unavailable = busy || operation.getValue() != FilterOperation.GAUSSIAN;
        radius.setDisable(unavailable);
        sigma.setDisable(unavailable);
    }

    private void configurationChanged() {
        resetOutput();
        updateParameterControls();
        if (source != null) { status.setText("Configuration changed. Apply Filter to generate a new result from the original."); }
    }

    private <T> void runTask(Task<T> task, Consumer<T> success) {
        setBusy(true);
        task.setOnSucceeded(event -> {
            try { success.accept(task.getValue()); } finally { setBusy(false); }
        });
        task.setOnFailed(event -> {
            setBusy(false);
            status.setText("Operation failed. Your previous image and result are preserved.");
            Alert alert = new Alert(Alert.AlertType.ERROR, task.getException().getMessage());
            alert.initOwner(owner);
            alert.setTitle("Image operation failed");
            alert.setHeaderText("The operation could not be completed.");
            alert.showAndWait();
        });
        Thread worker = new Thread(task, "visionlab-image-task");
        worker.setDaemon(true);
        worker.start();
    }

    private record LoadedImage(Image preview, PixelImage pixels) { }
    private record ProcessedImage(PixelImage pixels, Image preview, long nanoseconds) { }
}
