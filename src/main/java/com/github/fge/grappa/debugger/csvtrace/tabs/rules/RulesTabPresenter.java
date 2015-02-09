package com.github.fge.grappa.debugger.csvtrace.tabs.rules;

import com.github.fge.grappa.debugger.common.GuiTaskRunner;
import com.github.fge.grappa.debugger.csvtrace.CsvTraceModel;
import com.github.fge.grappa.debugger.javafx.BasePresenter;
import com.github.fge.grappa.debugger.mainwindow.MainWindowView;
import com.github.fge.grappa.debugger.model.ParseInfo;
import com.github.fge.grappa.debugger.model.db.PerClassStatistics;
import com.google.common.annotations.VisibleForTesting;

import java.util.List;

public class RulesTabPresenter
    extends BasePresenter<RulesTabView>
{
    private final GuiTaskRunner taskRunner;
    private final MainWindowView mainView;
    private final CsvTraceModel model;

    public RulesTabPresenter(final GuiTaskRunner taskRunner,
        final MainWindowView mainView, final CsvTraceModel model)
    {
        this.taskRunner = taskRunner;
        this.mainView = mainView;
        this.model = model;
    }

    @Override
    public void load()
    {
        loadParseInfo();
        loadTotalParseTime();
        loadMatchersByType();
        handleRefreshRules();
    }

    @VisibleForTesting
    void loadParseInfo()
    {
        final ParseInfo info = model.getParseInfo();
        view.displayParseInfo(info);
    }

    @VisibleForTesting
    void loadTotalParseTime()
    {
        taskRunner.computeOrFail(
            () -> model.getNodeById(0).getNanos(),
            view::displayTotalParseTime,
            this::handleLoadTotalParseTimeError
        );
    }

    @VisibleForTesting
    void handleLoadTotalParseTimeError(final Throwable throwable)
    {
       mainView.showError("Load error", "Unable to load parse tree", throwable);
    }

    @VisibleForTesting
    void loadMatchersByType()
    {
        taskRunner.computeOrFail(
            model::getMatchersByType,
            view::displayMatchersByType,
            this::handleLoadMatchersByTypeError
        );
    }

    @VisibleForTesting
    void handleLoadMatchersByTypeError(final Throwable throwable)
    {
       mainView.showError("Load error", "Unable to load matcher statistics",
           throwable);
    }

    public void handleRefreshRules()
    {
        taskRunner.computeOrFail(
            view::disableRefreshRules,
            model::getRulesByClass,
            this::doHandleRefreshRules,
            this::handleRefreshRulesError
        );
    }

    @VisibleForTesting
    void doHandleRefreshRules(final List<PerClassStatistics> stats)
    {
        view.displayRules(stats);

        final Runnable runnable = model.isLoadComplete()
            ? view::hideRefreshRules : view::enableRefreshRules;
        runnable.run();
    }

    @VisibleForTesting
    void handleRefreshRulesError(final Throwable throwable)
    {
        mainView.showError("Database error", "Unable to refresh rules",
            throwable);
    }
}
