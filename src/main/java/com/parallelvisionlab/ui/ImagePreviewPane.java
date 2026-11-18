package com.parallelvisionlab.ui;

import javafx.beans.binding.Bindings;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.image.ImageView;
import javafx.scene.layout.*;

/** Fit or inspect actual image pixels with scrollable 25-200% zoom. */
final class ImagePreviewPane extends VBox {
    ImagePreviewPane(ImageView image,Label empty) {
        super(10);
        CheckBox fit=new CheckBox("Fit to window");fit.setSelected(true);fit.setId("preview-fit");
        Slider zoom=new Slider(25,200,100);zoom.setPrefWidth(130);zoom.setId("preview-zoom");
        zoom.disableProperty().bind(fit.selectedProperty());zoom.setAccessibleText("Preview zoom percentage");
        Label percent=UiCards.label("100%","muted");percent.textProperty().bind(zoom.valueProperty().asString("%.0f%%"));
        StackPane canvas=new StackPane(empty,image);canvas.getStyleClass().add("canvas");
        canvas.setMinSize(0,0);canvas.setAlignment(Pos.CENTER);
        ScrollPane scroll=new ScrollPane(canvas);scroll.getStyleClass().add("page-scroll");
        scroll.setPrefHeight(270);scroll.setMinHeight(225);scroll.setPannable(true);
        scroll.fitToWidthProperty().bind(fit.selectedProperty());scroll.fitToHeightProperty().bind(fit.selectedProperty());
        image.fitWidthProperty().bind(Bindings.createDoubleBinding(()->image.getImage()==null?0:
                (fit.isSelected()?Math.max(1,scroll.getViewportBounds().getWidth()-40):image.getImage().getWidth()*zoom.getValue()/100),
                fit.selectedProperty(),scroll.viewportBoundsProperty(),image.imageProperty(),zoom.valueProperty()));
        image.fitHeightProperty().bind(Bindings.createDoubleBinding(()->image.getImage()==null?0:
                (fit.isSelected()?Math.max(1,scroll.getViewportBounds().getHeight()-40):image.getImage().getHeight()*zoom.getValue()/100),
                fit.selectedProperty(),scroll.viewportBoundsProperty(),image.imageProperty(),zoom.valueProperty()));
        getChildren().addAll(new HBox(10,fit,zoom,percent),scroll);
    }
}
