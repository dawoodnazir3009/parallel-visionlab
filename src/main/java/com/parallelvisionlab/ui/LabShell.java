package com.parallelvisionlab.ui;

import java.util.EnumMap;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.stage.Stage;

/** Persistent page instances retain loaded images and local selections during navigation. */
public final class LabShell extends BorderPane {
    private final EnumMap<LabPage, Node> pages = new EnumMap<>(LabPage.class);
    private final EnumMap<LabPage, Button> links = new EnumMap<>(LabPage.class);
    private final Label title = UiCards.label("", "page-title");
    private final Label subtitle = UiCards.label("", "muted");
    private final VBox content = new VBox(22);
    private final ScrollPane scroll = new ScrollPane(content);

    public LabShell(Stage owner) {
        VBox sidebar = new VBox(7);
        sidebar.getStyleClass().add("sidebar");
        sidebar.setPrefWidth(210);
        Label mark = UiCards.label("◈", "brand-mark");
        HBox brand = new HBox(9, mark, UiCards.label("Parallel VisionLab", "brand"));
        brand.setAlignment(Pos.CENTER_LEFT);
        brand.setPadding(new Insets(0, 0, 18, 0));
        sidebar.getChildren().add(brand);
        String previousGroup = "";
        for (LabPage page : LabPage.values()) {
            if (!previousGroup.equals(page.group)) {
                Label group = UiCards.label(page.group.toUpperCase(java.util.Locale.ROOT), "nav-section");
                sidebar.getChildren().add(group);
                previousGroup = page.group;
            }
            Button link = new Button(page.title);
            link.getStyleClass().add("nav-button");
            link.setMaxWidth(Double.MAX_VALUE);
            link.setOnAction(event -> navigate(page));
            links.put(page, link);
            sidebar.getChildren().add(link);
        }
        Region spacer = new Region();
        VBox.setVgrow(spacer, Priority.ALWAYS);
        sidebar.getChildren().addAll(spacer, UiCards.label("Image preview ready\nv0.2 · JavaFX desktop\nCharts use labeled sample data", "sidebar-footer"));
        setLeft(sidebar);
        content.setPadding(new Insets(24));
        scroll.setFitToWidth(true);
        scroll.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        scroll.getStyleClass().add("page-scroll");
        setCenter(scroll);
        pages.put(LabPage.WORKSPACE, new WorkspaceView(owner));
        pages.put(LabPage.BENCHMARK, new BenchmarkView());
        pages.put(LabPage.MONITOR, new MonitorView());
        pages.put(LabPage.HISTORY, new HistoryView(owner));
        pages.put(LabPage.SETTINGS, new SettingsView());
        pages.put(LabPage.OVERVIEW, new OverviewView(this::navigate));
        navigate(LabPage.OVERVIEW);
    }

    private void navigate(LabPage page) {
        title.setText(page.title);
        subtitle.setText(page.subtitle);
        Label badge = UiCards.label("UI PREVIEW", "status-pill");
        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);
        HBox header = new HBox(12, new VBox(6, title, subtitle), spacer, badge);
        header.setAlignment(Pos.CENTER_LEFT);
        content.getChildren().setAll(header, pages.get(page));
        links.forEach((key, button) -> {
            button.getStyleClass().remove("active");
            if (key == page) { button.getStyleClass().add("active"); }
        });
        scroll.setVvalue(0);
    }
}
