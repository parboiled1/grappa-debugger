package com.github.fge.grappa.debugger.javafx.csvtrace.tabs.tree;

import com.github.fge.grappa.debugger.csvtrace.tabs.tree.TreeTabPresenter;
import com.github.fge.grappa.debugger.javafx.common.JavafxDisplay;
import com.github.fge.grappa.debugger.javafx.custom.ParseTreeItem;
import com.github.fge.grappa.debugger.javafx.custom.ParseTreeNodeCell;
import com.github.fge.grappa.debugger.model.ParseTreeNode;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.control.TreeView;
import org.fxmisc.richtext.InlineCssTextArea;

public class TreeTabDisplay
    extends JavafxDisplay<TreeTabPresenter>
{
    /*
     * Tree
     */
    @FXML
    protected Label treeInfo;

    @FXML
    protected TreeView<ParseTreeNode> parseTree;

    /*
     * Node detail
     */
    @FXML
    protected Label nodeDepth;

    @FXML
    protected Label nodeRuleName;

    @FXML
    protected Label nodeMatcherType;

    @FXML
    protected Label nodeMatcherClass;

    @FXML
    protected Label nodeStatus;

    @FXML
    protected Label nodeStartPos;

    @FXML
    protected Label nodeEndPos;

    @FXML
    protected Label nodeTime;

    /*
     * Text
     */
    @FXML
    protected Label textInfo;

    @FXML
    protected InlineCssTextArea inputText;

    protected ParseTreeItem currentItem;

    @Override
    public void init()
    {
        parseTree.setCellFactory(param -> new ParseTreeNodeCell(this));
    }

    public void parseTreeNodeShowEvent(final ParseTreeNode node)
    {
        presenter.handleParseTreeNodeShow(node);
    }

    public void needChildrenEvent(final ParseTreeItem parseTreeItem)
    {
        currentItem = parseTreeItem;
        presenter.handleNeedChildren(currentItem.getValue());
    }
}
