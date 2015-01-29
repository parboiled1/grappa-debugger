package com.github.fge.grappa.debugger.csvtrace;

import com.github.fge.grappa.debugger.stats.ParseNode;
import com.github.fge.grappa.internal.NonFinalForTesting;
import com.google.common.annotations.VisibleForTesting;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.event.Event;
import javafx.fxml.FXML;
import javafx.scene.control.TreeCell;
import javafx.scene.control.TreeView;

import javax.annotation.ParametersAreNonnullByDefault;
import java.util.Objects;

@ParametersAreNonnullByDefault
@NonFinalForTesting
public class CsvTraceDisplay
{
    private CsvTracePresenter presenter;

    @FXML
    TreeView<ParseNode> parseTree;

    void setPresenter(final CsvTracePresenter presenter)
    {
        this.presenter = Objects.requireNonNull(presenter);
        init();
    }

    @VisibleForTesting
    void init()
    {
        /*
         * Parse tree
         */
        parseTree.setCellFactory(param -> new ParseNodeCell(presenter));
    }

    @FXML
    void expandParseTreeEvent(final Event event)
    {
        presenter.handleExpandParseTree();
    }

    private static final class ParseNodeCell
        extends TreeCell<ParseNode>
    {
        private ParseNodeCell(final CsvTracePresenter presenter)
        {
            setEditable(false);
            selectedProperty().addListener(new ChangeListener<Boolean>()
            {
                @Override
                public void changed(
                    final ObservableValue<? extends Boolean> observable,
                    final Boolean oldValue, final Boolean newValue)
                {
                    if (!newValue)
                        return;
                    final ParseNode node = getItem();
                    if (node != null)
                        presenter.parseNodeShowEvent(node);
                }
            });
        }

        @Override
        protected void updateItem(final ParseNode item, final boolean empty)
        {
            super.updateItem(item, empty);
            setText(empty ? null : String.format("%s (%s)", item.getRuleName(),
                item.isSuccess() ? "SUCCESS" : "FAILURE"));
        }
    }
}