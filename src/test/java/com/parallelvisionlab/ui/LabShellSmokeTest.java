package com.parallelvisionlab.ui;

import java.util.concurrent.*;
import javafx.application.Platform;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Spinner;
import javafx.scene.control.CheckBox;
import javafx.scene.control.Slider;
import com.parallelvisionlab.model.FilterOperation;
import com.parallelvisionlab.model.ExecutionBackend;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;
import static org.junit.jupiter.api.Assertions.*;

/** Opt-in desktop smoke check; ordinary CI remains independent of a display server. */
@EnabledIfSystemProperty(named = "visionlab.uiTests", matches = "true")
class LabShellSmokeTest {
    @TempDir java.nio.file.Path directory;
    @BeforeAll static void startToolkit()throws Exception {
        CompletableFuture<Void> toolkit = new CompletableFuture<>();
        Platform.startup(() -> { Platform.setImplicitExit(false); toolkit.complete(null); });
        toolkit.get(15, TimeUnit.SECONDS);
    }
    @AfterAll static void stopToolkit(){Platform.exit();}
    @Test void navigatesEveryPageAndRetainsWorkspaceInstance() throws Exception {
        FutureTask<Void> check = new FutureTask<>(() -> {
            Stage stage = new Stage();
            LabShell shell = new LabShell(stage);
            try {
                Scene scene = new Scene(shell, 1280, 840);
                scene.getStylesheets().add(getClass().getResource("/com/parallelvisionlab/workspace.css").toExternalForm());
                stage.setScene(scene);
                shell.applyCss();
                shell.layout();
                WorkspaceView workspace = null;
                VBox sidebar = (VBox) shell.getLeft();
                for (LabPage page : LabPage.values()) {
                    Button link = sidebar.getChildren().stream().filter(Button.class::isInstance)
                            .map(Button.class::cast).filter(button -> button.getText().equals(page.title)).findFirst().orElseThrow();
                    link.fire();
                    shell.applyCss();
                    shell.layout();
                    assertEquals(page.title, ((Label) shell.lookup(".page-title")).getText());
                    assertEquals(1, sidebar.getChildren().stream().filter(node -> node.getStyleClass().contains("active")).count());
                    if (page == LabPage.WORKSPACE) { workspace = (WorkspaceView) shell.lookup(".workspace"); }
                }
                sidebar.getChildren().stream().filter(Button.class::isInstance).map(Button.class::cast)
                        .filter(button -> button.getText().equals(LabPage.WORKSPACE.title)).findFirst().orElseThrow().fire();
                assertNotNull(workspace);
                assertSame(workspace, shell.lookup(".workspace"));
                CheckBox fit=(CheckBox)workspace.lookup("#preview-fit");
                Slider zoom=(Slider)workspace.lookup("#preview-zoom");
                assertTrue(zoom.isDisabled());fit.setSelected(false);assertFalse(zoom.isDisabled());
                zoom.setValue(150);assertEquals(150,zoom.getValue());fit.setSelected(true);
                @SuppressWarnings("unchecked")
                ComboBox<FilterOperation> filters = (ComboBox<FilterOperation>) workspace.lookup("#filter-operation");
                Spinner<?> radius = (Spinner<?>) workspace.lookup("#gaussian-radius");
                Spinner<?> sigma = (Spinner<?>) workspace.lookup("#gaussian-sigma");
                assertEquals(4, filters.getItems().size());
                assertTrue(radius.isDisabled());
                for (FilterOperation filter : FilterOperation.values()) {
                    filters.setValue(filter);
                    assertEquals(filter != FilterOperation.GAUSSIAN, radius.isDisabled());
                    assertEquals(filter != FilterOperation.GAUSSIAN, sigma.isDisabled());
                }
                @SuppressWarnings("unchecked")
                ComboBox<ExecutionBackend> backend = (ComboBox<ExecutionBackend>) workspace.lookup("#execution-backend");
                ComboBox<?> threads = (ComboBox<?>) workspace.lookup("#worker-threads");
                assertEquals(com.parallelvisionlab.service.ProcessingEngines.availableBackends().size(), backend.getItems().size());
                assertTrue(threads.isDisabled());
                backend.setValue(ExecutionBackend.EXECUTOR);
                assertFalse(threads.isDisabled());
                backend.setValue(ExecutionBackend.SEQUENTIAL);
                assertTrue(threads.isDisabled());
                Spinner<?> threshold = (Spinner<?>) workspace.lookup("#forkjoin-threshold");
                assertTrue(threshold.isDisabled());
                backend.setValue(ExecutionBackend.FORK_JOIN);
                assertFalse(threshold.isDisabled());
                assertFalse(threads.isDisabled());
                return null;
            } finally {
                shell.close();
                stage.setScene(null);
                stage.close();
            }
        });
        Platform.runLater(check);
        check.get(30, TimeUnit.SECONDS);
    }
    @Test void benchmarkButtonRecordsAndRestoresARealExperiment()throws Exception {
        CompletableFuture<Void> recorded=new CompletableFuture<>();
        var shellRef=new java.util.concurrent.atomic.AtomicReference<LabShell>();
        var stageRef=new java.util.concurrent.atomic.AtomicReference<Stage>();
        FutureTask<Void> start=new FutureTask<>(()->{
            var session=new LabSession(directory);session.preferences.set(new com.parallelvisionlab.benchmark.BenchmarkConfig(0,1,2,1));
            Stage stage=new Stage();stageRef.set(stage);var shell=new LabShell(stage,session);shellRef.set(shell);stage.setScene(new Scene(shell,1280,840));
            session.input.set(new com.parallelvisionlab.model.PixelImage(3,1,new int[]{0xffff0000,0xff00ff00,0xff0000ff}));session.sourceName.set("ui-fixture.png");
            session.history.addListener((javafx.collections.ListChangeListener<com.parallelvisionlab.benchmark.BenchmarkRun>)change->{
                try {
                    assertEquals(1,session.history.size());assertEquals(3,session.history.getFirst().measurements().size());
                    assertEquals("ui-fixture.png",session.history.getFirst().source());assertEquals(session.history.getFirst(),session.archive.load().runs().getFirst());
                    recorded.complete(null);
                }catch(Throwable error){recorded.completeExceptionally(error);}
            });
            ((VBox)shell.getLeft()).getChildren().stream().filter(Button.class::isInstance).map(Button.class::cast).filter(button->button.getText().equals("Benchmark Lab")).findFirst().orElseThrow().fire();
            ((CheckBox)shell.lookup("#include-cuda")).setSelected(false);
            Button run=(Button)shell.lookup("#run-benchmark");assertFalse(run.isDisabled());run.fire();
            return null;
        });
        try{Platform.runLater(start);start.get(15,TimeUnit.SECONDS);recorded.get(30,TimeUnit.SECONDS);}
        finally{
            FutureTask<Void> cleanup=new FutureTask<>(()->{if(shellRef.get()!=null)shellRef.get().close();if(stageRef.get()!=null)stageRef.get().close();return null;});
            Platform.runLater(cleanup);cleanup.get(10,TimeUnit.SECONDS);
        }
    }
}
