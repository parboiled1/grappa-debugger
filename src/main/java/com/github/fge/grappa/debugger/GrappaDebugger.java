package com.github.fge.grappa.debugger;

import com.github.fge.grappa.debugger.common.GuiTaskRunner;
import com.github.fge.grappa.debugger.javafx.AlertFactory;
import com.github.fge.grappa.debugger.mainwindow.JavafxMainWindowView;
import com.github.fge.grappa.debugger.mainwindow.MainWindowPresenter;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.scene.Scene;
import javafx.scene.layout.Pane;
import javafx.stage.Stage;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.annotation.ParametersAreNonnullByDefault;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.URL;
import java.nio.file.AccessDeniedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

@ParametersAreNonnullByDefault
public final class GrappaDebugger
    extends Application
    implements MainWindowFactory
{
    private static final URL BASE_WINDOW_FXML;

    static {
        BASE_WINDOW_FXML = GrappaDebugger.class.getResource("/mainWindow.fxml");
        if (BASE_WINDOW_FXML == null)
            throw new ExceptionInInitializerError("unable to load base window"
                + " fxml");
    }

    private final AlertFactory alertFactory = new AlertFactory();
    private final GuiTaskRunner taskRunner
        = new GuiTaskRunner("grappa-debugger-%d", Platform::runLater);

    private final Map<MainWindowPresenter, Stage> windows = new HashMap<>();

    @Override
    public void start(final Stage primaryStage)
    {
        final MainWindowPresenter presenter = createWindow(primaryStage);
        Thread.setDefaultUncaughtExceptionHandler((t, e) -> {
            if (Platform.isFxApplicationThread())
                alertFactory.unhandledError(e);
            else
                e.printStackTrace(System.err);
        });
        final List<String> params = getParameters().getRaw();
        if(presenter != null && !params.isEmpty()) {
            final Path traceFile = Paths.get(params.get(0));
            if(!Files.exists(traceFile)) {
                alertFactory.showError("Trace file does not exist", "Trace file does not exist", new FileNotFoundException(params.get(0)));
            } else if(!Files.isReadable(traceFile)) {
                alertFactory.showError("Trace file is not readable", "Trace file is not readable", new AccessDeniedException(params.get(0)));
            } else {
                presenter.loadTab(traceFile);
            }
        }
    }

    public static void main(final String... args)
    {
        launch(args);
    }

    @Override
    @Nullable
    public MainWindowPresenter createWindow()
    {
        return createWindow(new Stage());
    }

    @Nullable
    private MainWindowPresenter createWindow(final Stage stage)
    {
        final MainWindowPresenter presenter;

        final JavafxMainWindowView view;
        final Pane pane;

        try {
            view = new JavafxMainWindowView(stage, taskRunner, alertFactory);
        } catch (IOException e) {
            alertFactory.showError("Window creation error",
                "Unable to create window", e);
            return null;
        }

        pane = view.getNode();
        stage.setScene(new Scene(pane, 1024, 768));
        stage.setTitle("Grappa debugger");

        presenter = new MainWindowPresenter(this, taskRunner);
        presenter.setView(view);
        view.getDisplay().setPresenter(presenter);

        windows.put(presenter, stage);

        stage.show();

        return presenter;
    }

    @Override
    public void stop()
    {
        final Set<MainWindowPresenter> set = new HashSet<>(windows.keySet());
        set.forEach(MainWindowPresenter::handleCloseWindow);
        taskRunner.dispose();
    }

    @Override
    public void close(@Nonnull final MainWindowPresenter presenter)
    {
        Objects.requireNonNull(presenter);
        windows.remove(presenter).close();
    }
}
