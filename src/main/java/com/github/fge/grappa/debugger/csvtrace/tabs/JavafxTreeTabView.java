package com.github.fge.grappa.debugger.csvtrace.tabs;

import com.github.fge.grappa.debugger.common.BackgroundTaskRunner;
import com.github.fge.grappa.debugger.common.JavafxView;
import com.github.fge.grappa.debugger.stats.ParseNode;
import javafx.collections.FXCollections;
import javafx.scene.control.TreeItem;

import java.io.IOException;
import java.util.List;

public final class JavafxTreeTabView
    extends JavafxView<TreeTabPresenter, TreeTabDisplay>
    implements TreeTabView
{
    private final BackgroundTaskRunner taskRunner;

    public JavafxTreeTabView(final BackgroundTaskRunner taskRunner)
        throws IOException
    {
        super("/tabs/treeTab.fxml");
        this.taskRunner = taskRunner;
    }

    @Override
    public void loadTree(final ParseNode rootNode)
    {
        taskRunner.compute(() -> buildTree(rootNode), value -> {
            display.parseTree.setRoot(value);
            display.treeExpand.setDisable(false);
        });
    }

    private TreeItem<ParseNode> buildTree(final ParseNode root)
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
}
