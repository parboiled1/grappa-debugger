package com.github.fge.grappa.debugger.mainwindow;

import com.github.fge.grappa.debugger.GrappaDebuggerException;
import com.github.fge.grappa.debugger.MainWindowFactory;
import com.github.fge.grappa.debugger.common.GuiTaskRunner;
import com.github.fge.grappa.debugger.csvtrace.CsvTraceModel;
import com.github.fge.grappa.debugger.csvtrace.CsvTracePresenter;
import com.github.fge.grappa.debugger.javafx.BasePresenter;
import com.github.fge.grappa.debugger.model.DbCsvTraceModel;
import com.github.fge.grappa.debugger.model.ParseInfo;
import com.github.fge.grappa.debugger.model.db.DbLoadStatus;
import com.github.fge.grappa.debugger.model.db.DbLoader;
import com.google.common.annotations.VisibleForTesting;

import javax.annotation.ParametersAreNonnullByDefault;
import java.io.IOException;
import java.net.URI;
import java.nio.file.FileSystem;
import java.nio.file.FileSystems;
import java.nio.file.Path;
import java.sql.SQLException;
import java.util.Collections;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.TimeUnit;

@ParametersAreNonnullByDefault
public class MainWindowPresenter
    extends BasePresenter<MainWindowView>
{
    private static final Map<String, ?> ZIPFS_ENV
        = Collections.singletonMap("readonly", "true");

    private final GuiTaskRunner taskRunner;
    private final MainWindowFactory windowFactory;

    @VisibleForTesting
    CsvTracePresenter tracePresenter;

    public MainWindowPresenter(final MainWindowFactory windowFactory,
        final GuiTaskRunner taskRunner)
    {
        this.windowFactory = Objects.requireNonNull(windowFactory);
        this.taskRunner = Objects.requireNonNull(taskRunner);
    }

    @Override
    public void load()
    {
    }

    public void handleCloseWindow()
    {
        if (tracePresenter != null)
            tracePresenter.dispose();
        windowFactory.close(this);
    }

    public void handleNewWindow()
    {
        windowFactory.createWindow();
    }

    public void handleLoadFile()
    {
        final Path path = view.chooseFile();

        if (path == null)
            return;

        MainWindowPresenter window = this;

        if (tracePresenter != null) {
            window = windowFactory.createWindow();
            if (window == null)
                return;
        }

        // TODO: need to close the zipfs if this is not a legal trace file!
        window.loadTab(path);
    }

    @VisibleForTesting
    void loadTab(final Path path)
    {
        taskRunner.computeOrFail(
            () -> getModel(path),
            model -> {
                tracePresenter = createTabPresenter(model);
                view.attachTrace(tracePresenter);
                tracePresenter.load();
                view.setWindowTitle("Grappa debugger: "
                    + path.toAbsolutePath());
            },
            this::handleLoadFileError
        );
    }

    @VisibleForTesting
    CsvTracePresenter createTabPresenter(final CsvTraceModel model)
    {
        return new CsvTracePresenter(view, taskRunner, model);
    }

    // TODO: split; too long
    @VisibleForTesting
    CsvTraceModel getModel(final Path path)
        throws IOException, SQLException
    {
        final URI uri = URI.create("jar:" + path.toUri());

        final FileSystem zipfs = FileSystems.newFileSystem(uri, ZIPFS_ENV);

        final DbLoader loader = new DbLoader(zipfs);

        final CsvTraceModel model = new DbCsvTraceModel(zipfs, loader);

        final ParseInfo info = model.getParseInfo();

        final DbLoadStatus status = loader.getStatus();

        taskRunner.executeBackground(() -> checkLoadStatus(status, info, zipfs));

        return model;
    }

    @VisibleForTesting
    void handleLoadFileError(final Throwable throwable)
    {
        view.showError("Trace file error", "Unable to load trace file",
            throwable);
        view.setLabelText("Please load a trace file (File -> Load file)");
    }

    private void checkLoadStatus(final DbLoadStatus status,
        final ParseInfo info, FileSystem zipfs)
    {
        taskRunner.executeFront(view::initLoad);
        try {
            while (!status.waitReady(1L, TimeUnit.SECONDS))
                taskRunner.executeFront(
                    () -> view.reportProgress(status, info)
                );
            taskRunner.executeFront(view::loadComplete);
        } catch (InterruptedException ignored) {
            taskRunner.executeFront(view::loadAborted);
        }
            taskRunner.executeFront(() -> {
                try {
                    zipfs.close();
                } catch (IOException e) {
                    throw new RuntimeException(new GrappaDebuggerException(e));
                }
            });
    }
}
