package com.github.fge.grappa.debugger.javafx;

import com.github.fge.grappa.debugger.MainWindowFactory;
import com.github.fge.grappa.debugger.common.GuiTaskRunner;
import com.github.fge.grappa.debugger.javafx.common.AlertFactory;
import com.github.fge.grappa.debugger.javafx.mainwindow.JavafxMainWindowView;
import com.github.fge.grappa.debugger.mainwindow.MainWindowPresenter;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.scene.Scene;
import javafx.scene.layout.Pane;
import javafx.stage.Stage;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.annotation.ParametersAreNonnullByDefault;
import java.io.IOException;
import java.net.URL;
import java.util.*;

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
        createWindow(primaryStage);
        Thread.setDefaultUncaughtExceptionHandler((t, e) -> {
            if (Platform.isFxApplicationThread())
                alertFactory.unhandledError(e);
            else
                e.printStackTrace(System.err);
        });
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
        final Scene scene = new Scene(pane, 1024, 768);
        stage.setScene(scene);
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
