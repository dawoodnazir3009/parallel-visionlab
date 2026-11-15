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
    private final Button open = new Button("Open image");
    private final Button clear = new Button("Clear");
    private final Label empty = new Label("Your image will appear here\nPNG, JPEG, BMP or GIF");
    private final Stage owner;

    public WorkspaceView(Stage owner) {
        this.owner = owner;
        getStyleClass().add("workspace");
        Label brand = new Label("Parallel VisionLab");
        brand.getStyleClass().add("brand");
        Label subtitle = new Label("IMAGE PROCESSING WORKSPACE");
        subtitle.getStyleClass().add("muted");
        VBox heading = new VBox(6, subtitle, brand);
        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);
        clear.setDisable(true);
        open.getStyleClass().add("primary");
        HBox header = new HBox(12, heading, spacer, clear, open);
        header.getStyleClass().add("workspace-header");
        header.setAlignment(Pos.CENTER_LEFT);
        header.setPadding(new Insets(24));
        setTop(header);

        preview.setPreserveRatio(true);
        preview.setSmooth(true);
        empty.setTextAlignment(javafx.scene.text.TextAlignment.CENTER);
        empty.getStyleClass().add("muted");
        StackPane canvas = new StackPane(empty, preview);
        canvas.getStyleClass().add("canvas");
        canvas.setMinSize(0, 0);
        preview.fitWidthProperty().bind(canvas.widthProperty().subtract(40));
        preview.fitHeightProperty().bind(canvas.heightProperty().subtract(40));
        BorderPane.setMargin(canvas, new Insets(0, 24, 20, 24));
        setCenter(canvas);

        details.setWrapText(true);
        VBox footer = new VBox(8, details, status);
        footer.getStyleClass().add("workspace-footer");
        footer.setPadding(new Insets(16, 20, 16, 20));
        BorderPane.setMargin(footer, new Insets(0, 24, 24, 24));
        status.getStyleClass().add("muted");
        setBottom(footer);
        open.setOnAction(event -> chooseImage());
        clear.setOnAction(event -> {
            preview.setImage(null);
            empty.setVisible(true);
            details.setText("No image selected");
            status.setText("Ready. Open an image to get started.");
            clear.setDisable(true);
        });
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
