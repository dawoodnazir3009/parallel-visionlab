package com.parallelvisionlab.ui;

import javafx.scene.layout.StackPane;
import javafx.scene.shape.SVGPath;
import javafx.scene.shape.StrokeLineCap;
import javafx.scene.shape.StrokeLineJoin;

/** Small vector graphics stay crisp at Windows display scaling settings. */
final class NavigationIcon {
    private NavigationIcon() { }

    static StackPane create(LabPage page) {
        SVGPath icon = new SVGPath();
        icon.setContent(switch (page) {
            case OVERVIEW -> "M1 1 H6 V6 H1 Z M10 1 H15 V6 H10 Z M1 10 H6 V15 H1 Z M10 10 H15 V15 H10 Z";
            case WORKSPACE -> "M2 1 H14 Q15 1 15 2 V14 Q15 15 14 15 H2 Q1 15 1 14 V2 Q1 1 2 1 Z M1 12 L5 8 L8 11 L11 7 L15 12 M5 4 H5.1";
            case BENCHMARK -> "M1 1 V15 H15 M4 12 V8 H6 V12 M8 12 V5 H10 V12 M12 12 V2 H14 V12";
            case MONITOR -> "M1 4 H15 V14 H1 Z M1 9 H4 L6 6 L9 12 L11 9 H15 M6 1 H10";
            case HISTORY -> "M2 5 A7 7 0 1 1 1 10 M1 1 V5 H5 M8 4 V8 L11 10";
            case SETTINGS -> "M6 1 H10 L11 4 L14 4 L16 8 L14 12 H11 L10 15 H6 L5 12 H2 L0 8 L2 4 H5 Z M8 5 A3 3 0 1 0 8 11 A3 3 0 1 0 8 5";
        });
        icon.getStyleClass().add("nav-icon");
        icon.setStrokeWidth(1.4);
        icon.setStrokeLineCap(StrokeLineCap.ROUND);
        icon.setStrokeLineJoin(StrokeLineJoin.ROUND);
        StackPane graphic = new StackPane(icon);
        graphic.setMinSize(20, 20);
        graphic.setPrefSize(20, 20);
        graphic.setMaxSize(20, 20);
        graphic.setMouseTransparent(true);
        return graphic;
    }
}
