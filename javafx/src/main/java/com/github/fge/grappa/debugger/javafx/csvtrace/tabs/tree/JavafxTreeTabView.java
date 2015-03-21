package com.github.fge.grappa.debugger.javafx.csvtrace.tabs.tree;

import com.github.fge.grappa.buffers.InputBuffer;
import com.github.fge.grappa.debugger.common.GuiTaskRunner;
import com.github.fge.grappa.debugger.common.TracingCharEscaper;
import com.github.fge.grappa.debugger.csvtrace.tabs.tree.TreeTabPresenter;
import com.github.fge.grappa.debugger.csvtrace.tabs.tree.TreeTabView;
import com.github.fge.grappa.debugger.javafx.common.JavafxUtils;
import com.github.fge.grappa.debugger.javafx.common.JavafxView;
import com.github.fge.grappa.debugger.javafx.custom.ParseTreeItem;
import com.github.fge.grappa.debugger.javafx.model.MatchFragments;
import com.github.fge.grappa.debugger.model.InputText;
import com.github.fge.grappa.debugger.model.ParseTree;
import com.github.fge.grappa.debugger.model.ParseTreeNode;
import com.github.fge.grappa.debugger.model.RuleInfo;
import com.github.fge.grappa.internal.NonFinalForTesting;
import com.google.common.escape.CharEscaper;
import javafx.scene.paint.Color;
import org.fxmisc.richtext.InlineCssTextArea;
import org.parboiled.support.Position;

import javax.annotation.Nullable;
import javax.annotation.ParametersAreNonnullByDefault;
import java.io.IOException;
import java.util.List;
import java.util.Objects;
import java.util.function.Function;
import java.util.stream.Collectors;

@NonFinalForTesting
@ParametersAreNonnullByDefault
public class JavafxTreeTabView
    extends JavafxView<TreeTabPresenter, TreeTabDisplay>
    implements TreeTabView
{
    @SuppressWarnings("AutoBoxing")
    private static final Function<Position, String> POS_TO_STRING = pos ->
        String.format("line %d, column %d", pos.getLine(), pos.getColumn());
    private static final CharEscaper ESCAPER = new TracingCharEscaper();

    private final GuiTaskRunner taskRunner;

    private InputBuffer buffer;

    public JavafxTreeTabView(final GuiTaskRunner taskRunner)
        throws IOException
    {
        super("/tabs/treeTab.fxml");
        this.taskRunner = Objects.requireNonNull(taskRunner);
    }

    @SuppressWarnings("AutoBoxing")
    @Override
    public void loadInputText(final InputText inputText)
    {
        buffer = inputText.getInputBuffer();
        display.textInfo.setText(String.format("Input text: %d lines, %d "
            + "characters, %d code points", inputText.getNrLines(),
            inputText.getNrChars(), inputText.getNrCodePoints()));

        taskRunner.compute(() -> buffer.extract(0, buffer.length()),
            text -> display.inputText.appendText(text));
    }

    @Override
    public void highlightSuccess(final int start, final int end)
    {
        final int length = buffer.length();
        final int realStart = Math.min(start, length);
        final int realEnd = Math.min(end, length);

        taskRunner.compute(
            () -> getSuccessfulMatchFragments(length, realStart, realEnd),
            fragments -> {
                appendSuccessfulMatchFragments(fragments);
                setScroll(realStart);
            }
        );
    }

    @Override
    public void highlightFailure(final int end)
    {
        final int length = buffer.length();
        final int realEnd = Math.min(end, length);

        taskRunner.compute(() -> getFailedMatchFragments(length, realEnd),
            fragments -> {
                appendFailedMatchFragments(fragments);
                setScroll(realEnd);
            });
    }

    @SuppressWarnings("AutoBoxing")
    @Override
    public void loadParseTree(@Nullable final ParseTree parseTree)
    {
        if (parseTree == null) {
            display.treeInfo.setText("Load error");
            return;
        }

        final ParseTreeNode rootNode = parseTree.getRootNode();
        display.parseTree.setRoot(new ParseTreeItem(display, rootNode));
        display.treeInfo.setText(String.format("Tree: %d nodes; depth %d",
            parseTree.getNrInvocations(), parseTree.getTreeDepth()));
    }

    @Override
    public void showParseTreeNode(final ParseTreeNode node)
    {
        final RuleInfo info = node.getRuleInfo();

        // Pure text, or nearly so
        display.nodeDepth.setText(Integer.toString(node.getLevel()));
        display.nodeRuleName.setText(info.getName());
        display.nodeMatcherType.setText(info.getType().name());
        display.nodeMatcherClass.setText(info.getClassName());

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
        final Position start = buffer.getPosition(node.getStartIndex());
        final Position end = buffer.getPosition(node.getEndIndex());

        display.nodeStartPos.setText(POS_TO_STRING.apply(start));
        display.nodeEndPos.setText(POS_TO_STRING.apply(end));

        setScroll(node.getStartIndex());
    }

    @Override
    public void waitForChildren()
    {
        display.currentItem.loadingProperty().setValue(true);
    }

    @Override
    public void setTreeChildren(final List<ParseTreeNode> children)
    {
        final List<ParseTreeItem> items = children.stream()
            .map(node -> new ParseTreeItem(display, node))
            .collect(Collectors.toList());
        display.currentItem.getChildren().setAll(items);
        display.currentItem.loadingProperty().setValue(false);
    }

    private MatchFragments getFailedMatchFragments(final int length,
        final int realEnd)
    {
        String fragment, match;
        String afterMatch = "";
        String beforeMatch = "";

        // Before match
        fragment = buffer.extract(0, realEnd);
        if (!fragment.isEmpty())
            beforeMatch = fragment;

        // Match
        match = "\u2612";

        // After match
        fragment = buffer.extract(realEnd, length);
        if (!fragment.isEmpty())
            afterMatch = fragment;

        return new MatchFragments(beforeMatch, match, afterMatch);
    }

    private MatchFragments getSuccessfulMatchFragments(final int length,
        final int realStart, final int realEnd)
    {
        String fragment, match;
        String afterMatch = "";
        String beforeMatch = "";

        // Before match
        fragment = buffer.extract(0, realStart);
        if (!fragment.isEmpty())
            beforeMatch = fragment;

        // Match
        fragment = buffer.extract(realStart, realEnd);
        match = fragment.isEmpty() ? "\u2205"
                : '\u21fe' + ESCAPER.escape(fragment) + '\u21fd';

        // After match
        fragment = buffer.extract(realEnd, length);
        if (!fragment.isEmpty())
            afterMatch = fragment;

        return new MatchFragments(beforeMatch, match, afterMatch);
    }

    private void setScroll(final int index)
    {
        final Position position = buffer.getPosition(index);
        double line = position.getLine();
        final double nrLines = buffer.getLineCount();
        if (line != nrLines)
            line--;

        //TO-DO check with FGE the need for this and edit it to serve the need
        //display.inputText.setVvalue(line / nrLines);
    }

    private void appendSuccessfulMatchFragments(MatchFragments fragments)
    {
        int length =0;
        final InlineCssTextArea textArea = display.inputText;

        if(!fragments.getBeforeMatch().isEmpty())
        {
            length = textArea.getText().length();
            textArea.appendText(fragments.getBeforeMatch());
            textArea.setStyle(length, length + fragments.getBeforeMatch().length(), JavafxUtils.GRAY);
        }

        length = textArea.getText().length();
        textArea.appendText(fragments.getMatch());
        textArea.setStyle(length, length + fragments.getMatch().length(), JavafxUtils.GREEN_UNDERLINED);

        if(!fragments.getAfterMatch().isEmpty())
            textArea.appendText(fragments.getAfterMatch());

    }

    private void appendFailedMatchFragments(MatchFragments fragments){
        int length =0;
        final InlineCssTextArea textArea = display.inputText;

        if(!fragments.getBeforeMatch().isEmpty())
        {
            length = textArea.getText().length();
            textArea.appendText(fragments.getBeforeMatch());
            textArea.setStyle(length, length + fragments.getBeforeMatch().length(), JavafxUtils.GRAY);
        }

        length = textArea.getText().length();
        textArea.appendText(fragments.getMatch());
        textArea.setStyle(length, length + fragments.getMatch().length(), JavafxUtils.RED_UNDERLINED);

        if(!fragments.getAfterMatch().isEmpty())
            textArea.appendText(fragments.getAfterMatch());
    }
}
