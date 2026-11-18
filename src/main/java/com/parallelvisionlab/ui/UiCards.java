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
        VBox box = new VBox(14);
        box.getStyleClass().add("panel");
        box.setMinWidth(0);
        box.getChildren().add(label(title, "panel-title"));
        box.getChildren().addAll(children);
        return box;
    }

    static HBox columns(Node... children) {
        HBox row = new HBox(14, children);
        for (Node node : children) {
            HBox.setHgrow(node, Priority.ALWAYS);
            if (node instanceof Region region) {
                region.setMinWidth(0);
                region.setMaxWidth(Double.MAX_VALUE);
                if (region instanceof VBox box) { box.setPrefWidth(1); }
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
        box.setMinHeight(170);
        return box;
    }
}
