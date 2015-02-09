package com.github.fge.grappa.debugger.csvtrace.tabs.treedepth;

import com.github.fge.grappa.debugger.javafx.JavafxView;
import javafx.collections.ObservableList;
import javafx.scene.chart.XYChart.Data;

import java.io.IOException;
import java.util.Map;
import java.util.Map.Entry;
import java.util.function.Function;
import java.util.stream.Stream;

public final class JavafxTreeDepthTabView
    extends JavafxView<TreeDepthTabPresenter, TreeDepthTabDisplay>
    implements TreeDepthTabView
{
    public JavafxTreeDepthTabView()
        throws IOException
    {
        super("/tabs/treeDepthTab.fxml");
    }

    @Override
    public void disableToolbar()
    {
        Stream.of(display.requiredLine, display.linesDisplayed,
            display.prevLines, display.nextLines, display.refreshButton
        ).forEach(node -> node.setDisable(true));
        display.loadInProgress.setVisible(true);
    }

    @Override
    public void setMaxLines(final int nrLines)
    {
        display.totalLines.setText(String.format("/ %d", nrLines));
    }

    @Override
    public void displayChart(final Map<Integer, Integer> depthMap)
    {
        final ObservableList<Data<Number, Number>> list
            = display.series.getData();

        list.clear();

        final Function<Entry<Integer, Integer>, Data<Number, Number>> toData
            = entry -> new Data<>(entry.getKey(), entry.getValue());

        final Integer startLine = depthMap.keySet().stream()
            .min(Integer::compare).get();
        final Integer endLine = depthMap.keySet().stream()
            .max(Integer::compare).get();
        final Integer maxDepth = depthMap.values().stream()
            .max(Integer::compare).get();

        display.xAxis.setLowerBound(startLine.doubleValue());
        display.xAxis.setUpperBound(endLine.doubleValue());
        display.yAxis.setUpperBound(maxDepth.doubleValue());
        //noinspection AutoUnboxing
        display.yAxis.setTickUnit(Math.max(1, maxDepth / 15));

        depthMap.entrySet().stream().map(toData).forEach(list::add);

        display.currentLines.setText(String.format("Lines %d-%d", startLine,
            endLine));
    }

    @Override
    public void updateStartLine(final int startLine)
    {
        display.requiredLine.setText(String.valueOf(startLine));
    }

    @Override
    public void updateToolbar(final boolean disablePrev,
        final boolean disableNext, final boolean disableRefresh)
    {
        display.requiredLine.setDisable(false);
        display.linesDisplayed.setDisable(false);
        display.prevLines.setDisable(disablePrev);
        display.nextLines.setDisable(disableNext);
        if (disableRefresh)
            display.hbox.getChildren().remove(display.refreshBox);
        else {
            display.refreshButton.setDisable(false);
            display.loadInProgress.setVisible(false);
        }
    }
}
