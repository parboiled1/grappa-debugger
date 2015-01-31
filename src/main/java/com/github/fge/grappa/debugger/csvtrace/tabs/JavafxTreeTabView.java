package com.github.fge.grappa.debugger.csvtrace.tabs;

import com.github.fge.grappa.buffers.InputBuffer;
import com.github.fge.grappa.debugger.common.BackgroundTaskRunner;
import com.github.fge.grappa.debugger.common.JavafxView;
import com.github.fge.grappa.debugger.javafx.JavafxUtils;
import com.github.fge.grappa.debugger.stats.ParseNode;
import com.github.fge.grappa.debugger.stats.TracingCharEscaper;
import com.github.fge.grappa.internal.NonFinalForTesting;
import com.github.fge.grappa.trace.ParseRunInfo;
import com.google.common.annotations.VisibleForTesting;
import com.google.common.escape.CharEscaper;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.TreeItem;
import javafx.scene.control.TreeView;
import javafx.scene.paint.Color;
import javafx.scene.text.Text;
import org.parboiled.support.Position;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.function.Function;

@NonFinalForTesting
public class JavafxTreeTabView
    extends JavafxView<TreeTabPresenter, TreeTabDisplay>
    implements TreeTabView
{
    @SuppressWarnings("AutoBoxing")
    private static final Function<Position, String> POS_TO_STRING = pos ->
        String.format("line %d, column %d", pos.getLine(), pos.getColumn());
    private static final CharEscaper ESCAPER = new TracingCharEscaper();

    private final BackgroundTaskRunner taskRunner;
    private final InputBuffer buffer;

    public JavafxTreeTabView(final BackgroundTaskRunner taskRunner,
        final InputBuffer buffer)
        throws IOException
    {
        super("/tabs/treeTab.fxml");
        this.taskRunner = Objects.requireNonNull(taskRunner);
        this.buffer = Objects.requireNonNull(buffer);
    }

    @Override
    public void loadTree(final ParseNode rootNode)
    {
        final Button button = display.treeExpand;

        taskRunner.compute(
            () -> buildTree(rootNode),
            value -> {
                display.parseTree.setRoot(value);
                display.treeToolbar.getItems().setAll(button);
                button.setDisable(false);
            }
        );
    }

    @Override
    public void loadText()
    {
        final ObservableList<Node> children = display.inputText.getChildren();
        taskRunner.compute(() -> buffer.extract(0, buffer.length()),
            text -> children.setAll(new Text(text)));
    }

    @Override
    public void showParseNode(final ParseNode node)
    {
        final int realStart = Math.min(buffer.length(), node.getStart());
        final Position start = buffer.getPosition(realStart);
        final int realEnd = Math.min(buffer.length(), node.getEnd());
        final Position end = buffer.getPosition(realEnd);

        // Pure text, or nearly so
        display.nodeDepth.setText(Integer.toString(node.getLevel()));
        display.nodeRuleName.setText(node.getRuleName());
        display.nodeMatcherType.setText(node.getMatcherType().name());
        display.nodeMatcherClass.setText(node.getMatcherClass());

        // Time
        display.nodeTime.setText(JavafxUtils.nanosToString(node.getNanos()));

        // Status
        if (node.isSuccess()) {
            display.nodeStatus.setText("SUCCESS");
            display.nodeStatus.setTextFill(Color.GREEN);
        } else {
            display.nodeStatus.setText("FAILURE");
            display.nodeStatus.setTextFill(Color.RED);
        }

        // Positions
        display.nodeStartPos.setText(POS_TO_STRING.apply(start));
        display.nodeEndPos.setText(POS_TO_STRING.apply(end));
    }

    @Override
    public void highlightSuccess(final int start, final int end)
    {
        final int length = buffer.length();
        final int realStart = Math.min(start, length);
        final int realEnd = Math.min(end, length);
        //final List<Text> list = getSuccessfulMatchFragments(length, realStart,
        //    realEnd);

        final ObservableList<Node> nodes = display.inputText.getChildren();

        taskRunner.compute(
            () -> getSuccessfulMatchFragments(length, realStart, realEnd),
            fragments -> {
                nodes.setAll(fragments);
                setScroll(realStart);
            }
        );
    }

    @Override
    public void highlightFailure(final int end)
    {
        final int length = buffer.length();
        final int realEnd = Math.min(end, length);
        final ObservableList<Node> nodes = display.inputText.getChildren();

        taskRunner.compute(
            () -> getFailedMatchFragments(length, realEnd),
            fragments -> {
                nodes.setAll(fragments);
                setScroll(realEnd);
            });
    }

    @SuppressWarnings("AutoBoxing")
    @Override
    public void loadParseRunInfo(final ParseRunInfo info)
    {
        final int nrLines = info.getNrLines();
        final int nrChars = info.getNrChars();
        final int nrCodePoints = info.getNrCodePoints();
        final String value = String.format("%d lines, %d characters, "
                + "%d code points", nrLines, nrChars, nrCodePoints);
        display.textInfo.setText(value);
    }

    @Override
    public void expandParseTree()
    {
        final TreeView<ParseNode> parseTree = display.parseTree;
        final Button expand = display.treeExpand;
        final ParseNode root = parseTree.getRoot().getValue();
        taskRunner.compute(
            () -> expand.setDisable(true),
            () -> buildTree(root, true),
            item -> { parseTree.setRoot(item); expand.setDisable(false); }
        );
    }

    private List<Text> getFailedMatchFragments(final int length,
        final int realEnd)
    {
        final List<Text> list = new ArrayList<>(3);

        String fragment;
        Text text;

        fragment = buffer.extract(0, realEnd);
        if (!fragment.isEmpty()) {
            text = new Text(fragment);
            text.setFill(Color.GRAY);
            list.add(text);
        }

        text = new Text("\u2612");
        text.setFill(Color.RED);
        text.setUnderline(true);
        list.add(text);

        fragment = buffer.extract(realEnd, length);
        if (!fragment.isEmpty())
            list.add(new Text(fragment));
        return list;
    }

    private List<Text> getSuccessfulMatchFragments(final int length,
        final int realStart, final int realEnd)
    {
        final List<Text> list = new ArrayList<>(3);

        String fragment;
        Text text;

        // Before match
        fragment = buffer.extract(0, realStart);
        if (!fragment.isEmpty()) {
            text = new Text(fragment);
            text.setFill(Color.GRAY);
            list.add(text);
        }

        // Match
        fragment = buffer.extract(realStart, realEnd);
        text = new Text(fragment.isEmpty() ? "\u2205"
            : '\u21fe' + ESCAPER.escape(fragment) + '\u21fd');
        text.setFill(Color.GREEN);
        text.setUnderline(true);
        list.add(text);

        // After match
        fragment = buffer.extract(realEnd, length);
        if (!fragment.isEmpty()) {
            text = new Text(fragment);
            list.add(text);
        }
        return list;
    }

    @VisibleForTesting
    TreeItem<ParseNode> buildTree(final ParseNode root)
    {
        return buildTree(root, false);
    }

    private TreeItem<ParseNode> buildTree(final ParseNode root,
        final boolean expanded)
    {
        final TreeItem<ParseNode> ret = new TreeItem<>(root);

        addChildren(ret, root, expanded);

        return ret;
    }

    private void addChildren(final TreeItem<ParseNode> item,
        final ParseNode parent, final boolean expanded)
    {
        TreeItem<ParseNode> childItem;
        final List<TreeItem<ParseNode>> childrenItems
            = FXCollections.observableArrayList();

        for (final ParseNode node: parent.getChildren()) {
            childItem = new TreeItem<>(node);
            addChildren(childItem, node, expanded);
            childrenItems.add(childItem);
        }

        item.getChildren().setAll(childrenItems);
        item.setExpanded(expanded);
    }

    private void setScroll(final int index)
    {
        final Position position = buffer.getPosition(index);
        double line = position.getLine();
        final double nrLines = buffer.getLineCount();
        if (line != nrLines)
            line--;
        display.inputTextScroll.setVvalue(line / nrLines);
    }
}
