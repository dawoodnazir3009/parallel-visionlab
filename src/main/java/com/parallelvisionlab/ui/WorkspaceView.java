package com.parallelvisionlab.ui;

import com.parallelvisionlab.service.ImageLoader;
import java.io.File;
import javafx.concurrent.Task;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.*;
import javafx.stage.FileChooser;
import javafx.stage.Stage;

/** Image workspace. Processing controls arrive when processing engines are available. */
public final class WorkspaceView extends BorderPane {
    private final ImageLoader loader = new ImageLoader();
    private final ImageView preview = new ImageView();
    private final Label details = new Label("No image selected");
    private final Label status = new Label("Ready. Open an image to get started.");
    private final Button open = new Button("Choose Image");
    private final Button clear = new Button("Clear");
    private final Label empty = new Label("Your image will appear here\nPNG, JPEG, BMP or GIF");
    private final Stage owner;

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
        ComboBox<String> operation = choice("Grayscale", "Gaussian Blur", "Sobel Edge Detection", "Sharpen Convolution");
        ComboBox<String> backend = choice("Sequential Java", "Java Multithreading", "Java Fork/Join", "CUDA GPU — Basic Kernel", "CUDA GPU — Tiled Kernel");
        ComboBox<String> threads = choice("Automatic", "2", "4", "8", "16");
        operation.setDisable(true);
        backend.setDisable(true);
        threads.setDisable(true);
        Button process = new Button("Preview Filter");
        process.getStyleClass().add("primary");
        process.setDisable(true);
        process.setTooltip(new Tooltip("Processing engines are planned in the next project phases."));
        VBox configuration = UiCards.panel("Processing configuration",
                field("Image operation", operation), field("Execution backend", backend), field("CPU worker threads", threads),
                process, UiCards.label("Image loading works. Filter controls become available when the Java/CUDA engines are implemented.", "sample-notice"));
        Button save = new Button("Save Output");
        save.setDisable(true);
        VBox output = UiCards.panel("Processed output", UiCards.empty("Processed image appears here", "Run an implemented filter to create output. No simulated image is generated."), save);
        setCenter(new VBox(14, UiCards.columns(input, configuration), output));
        open.setOnAction(event -> chooseImage());
        clear.setOnAction(event -> {
            preview.setImage(null);
            empty.setVisible(true);
            details.setText("No image selected");
            status.setText("Ready. Open an image to get started.");
            clear.setDisable(true);
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
        open.setDisable(true);
        clear.setDisable(true);
        status.setText("Loading " + file.getName() + "...");
        Task<Image> task = new Task<>() {
            @Override
            protected Image call() throws Exception {
                return loader.load(file.toPath());
            }
        };
        task.setOnSucceeded(event -> {
            Image image = task.getValue();
            preview.setImage(image);
            empty.setVisible(false);
            details.setText(file.getName() + "  |  " + (int) image.getWidth() + " × "
                    + (int) image.getHeight() + " pixels");
            status.setText("Image loaded. Preview fits the window; original resolution is preserved.");
            open.setDisable(false);
            clear.setDisable(false);
        });
        task.setOnFailed(event -> {
            open.setDisable(false);
            clear.setDisable(preview.getImage() == null);
            status.setText("Could not load " + file.getName() + ". Choose another image.");
            Alert alert = new Alert(Alert.AlertType.ERROR);
            alert.initOwner(owner);
            alert.setTitle("Image could not be opened");
            alert.setHeaderText("Choose a readable, supported image file.");
            alert.setContentText(task.getException().getMessage());
            alert.showAndWait();
        });
        Thread worker = new Thread(task, "image-loader");
        worker.setDaemon(true);
        worker.start();
    }
}
