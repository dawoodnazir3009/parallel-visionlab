package com.parallelvisionlab.ui;

import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.scene.layout.*;

/** Shared dashboard building blocks with shrinkable, equal-width columns. */
final class UiCards {
    private UiCards() { }

    static Label label(String text, String style) {
        Label label = new Label(text);
        label.getStyleClass().add(style);
        label.setWrapText(true);
        return label;
    }

    static VBox panel(String title, Node... children) {
        VBox box = new VBox(10);
        box.getStyleClass().add("panel");
        box.setMinWidth(0);
        box.getChildren().add(label(title, "panel-title"));
        box.getChildren().addAll(children);
        return box;
    }

    static GridPane columns(Node... children) {
        GridPane row = new GridPane();
        row.setHgap(14);
        // Percentage columns give wrapping labels their actual available width.
        // A preferred width of one pixel makes JavaFX measure entire cards as
        // extremely tall columns of wrapped text, even in a wide window.
        for (int index = 0; index < children.length; index++) {
            ColumnConstraints column = new ColumnConstraints();
            column.setPercentWidth(100.0 / children.length);
            column.setFillWidth(true);
            row.getColumnConstraints().add(column);
            Node node = children[index];
            row.add(node, index, 0);
            GridPane.setValignment(node, javafx.geometry.VPos.TOP);
            if (node instanceof Region region) {
                region.setMinWidth(0);
                region.setMaxWidth(Double.MAX_VALUE);
            }
        }
        return row;
    }

    static VBox metric(String title, String value, String note) {
        return panel(title, label(value, "metric-value"), label(note, "muted"));
    }

    static VBox empty(String title, String description) {
        VBox box = new VBox(10, label(title, "empty-title"), label(description, "muted"));
        box.getStyleClass().add("empty-state");
        box.setAlignment(javafx.geometry.Pos.CENTER);
        box.setMinHeight(120);
        return box;
    }
}
