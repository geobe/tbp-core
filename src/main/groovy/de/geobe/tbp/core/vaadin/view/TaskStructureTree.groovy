/*
 * The MIT License (MIT)
 *
 * Copyright (c) 2018.  Georg Beier. All rights reserved.
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */

package de.geobe.tbp.core.vaadin.view

import com.vaadin.data.TreeData
import com.vaadin.data.provider.TreeDataProvider
import com.vaadin.event.selection.SelectionEvent
import com.vaadin.server.Page
import com.vaadin.spring.annotation.SpringComponent
import com.vaadin.spring.annotation.UIScope
import com.vaadin.ui.CheckBox
import com.vaadin.ui.Component
import com.vaadin.ui.Layout
import com.vaadin.ui.Tree
import de.geobe.tbp.core.dto.NodeDto
import de.geobe.tbp.core.dto.TaskNodeDto
import de.geobe.tbp.core.service.TaskService
import de.geobe.tbp.core.vaadin.view.helper.AssociationNode
import de.geobe.util.vaadin.builder.SubTree
import de.geobe.util.vaadin.helper.VaadinSelectionModel
import de.geobe.util.vaadin.helper.VaadinTreeHelper
import de.geobe.util.vaadin.type.DetailSelector
import org.springframework.beans.factory.annotation.Autowired

import static de.geobe.util.vaadin.builder.VaadinBuilder.C
import static de.geobe.util.vaadin.builder.VaadinBuilder.F

/**
 * Main selection component is this tree
 *
 * @author georg beier
 */
@SpringComponent
@UIScope
class TaskStructureTree extends SubTree
        implements Serializable, DetailSelector<Tuple2<String, Serializable>> {

    public static final String PROJECT_TYPE = 'Project'
    public static final String COMPOUND_TYPE = 'CompoundTask'
    public static final String SUBTASK_TYPE = 'Subtask'

    private static final Set<String> TYPES = [PROJECT_TYPE, COMPOUND_TYPE, SUBTASK_TYPE]

    private static final String TREELAYOUT = 'treelayout'
    private static final String TASKTREE = 'tasktree'
    private static final String ASSOC_BOX = 'showAssocBox'
    private static final String MENU = 'logoutmenu'
    private Layout treelayout
    private Tree<NodeDto> taskTree
    private VaadinTreeHelper treeHelper
    private CheckBox showAssocBox

    private uiComponents

    private NodeDto selectedProjectNode
    /** store dtos for tree refresh */
    private List<TaskNodeDto> rootNodes
    private VaadinSelectionModel selectionModel = new VaadinSelectionModel()

    private boolean showAssociations = false

    /** Closure to find subnodes to a task node */
    private collector = { def parentNode ->
        if (parentNode instanceof NodeDto) {
            switch (parentNode.id.first) {
                case 'Project':
                case 'CompoundTask':
                    parentNode.related.subtask
                    break
                case 'Subtask':
                    if (showAssociations)
                        [new AssociationNode(parentNode, 'milestone')]
                    break
                default:
                    parentNode.related[AssociationNode.LINK]
                    break
            }
        }
    }

    private getSelectedProjectId() { selectedProjectNode.id }

    @Autowired
    private TaskService taskService

//    @Autowired
//    private VaadinSecurity vaadinSecurity


    @Override
    Component build() {
        vaadin."$C.vlayout"() {
//            "$F.menubar"([uikey: MENU]) {
//                "$F.menuitem"('Logout', [command: { vaadinSecurity.logout() }])
//            }
            "$C.vlayout"([uikey  : TREELAYOUT,
                          spacing: true,
                          margin : true]) {
                "$C.panel"('Projects and Tasks') {
                    "$F.tree"([uikey            : TASKTREE,
                               caption          : 'Task Tree',
                               selectionListener: {
                                   treeValueChanged(it)
                               }])
                }
                "$F.checkbox"('show associations',
                        [uikey              : ASSOC_BOX,
                         value              : false,
                         valueChangeListener: {
                             showAssociations = showAssocBox.value
                             rebuildTree(rootNodes)
                         }])
            }
        }
    }

    @Override
    void init(Object... value) {
        treelayout = subtreeComponent TREELAYOUT
        taskTree = subtreeComponent TASKTREE
        showAssocBox = subtreeComponent ASSOC_BOX
        treeHelper = new VaadinTreeHelper<TaskNodeDto>(taskTree)
        rootNodes = taskService.projectTree
        treeHelper.buildTree(rootNodes, collector)

    }

    /**
     * handle changes of tree selection
     * @param event info on the newly selected tree item
     */
    private void treeValueChanged(SelectionEvent event) {
        def selectNode = event.firstSelectedItem
        if (selectNode.present) {
            def topItemNode = treeHelper.topParentForId(selectNode.get())
            if (topItemNode != selectedProjectNode) {
                selectionModel.notifyRootChange(topItemNode)
                selectedProjectNode = topItemNode
            }
            if (selectNode.get() instanceof NodeDto) {
                def node = selectableItem selectNode.get()
                if (node)
                    selectionModel.notifyChange node
            }
        }
    }

    private selectableItem(NodeDto selected) {
        if (selected?.id.first in TYPES) {
            selected
        } else if (selected) {
            TreeData<NodeDto> treeData =
                    ((TreeDataProvider<NodeDto>) taskTree.dataProvider).treeData
            selectableItem treeData.getParent(selected)
        } else {
            null
        }
    }

    /**
     * disable the tree while a tree item is edited on one of the tab pages
     */
    public void onEditItem() {
        taskTree.enabled = false
    }

    public void expandAll() {
        taskTree.expand(
                treeHelper.allItems().findAll { NodeDto item ->
                    item.id.first in TYPES
                }
        )
    }

    /**
     * enable and update the tree after editing an item
     * @param itemId identifies edited item
     * @param caption eventually updated caption of the edited item
     * @param mustReload tree must reload after new item was created
     *        or structure changed
     */
    public void onEditItemDone(Tuple2<String, Serializable> itemId, String caption, boolean mustReload = false) {
        if (mustReload) {
            rootNodes = taskService.projectTree
            rebuildTree(rootNodes)
        } else {
            def selected = taskTree.selectedItems?.first()
            if (selected?.id == itemId && selected?.tag != caption) {
                selected.tag = caption
                taskTree.dataProvider.refreshAll()
            }
        }
        taskTree.enabled = true
    }

    private rebuildTree(List<NodeDto> roots) {
        def expandedNodeIds = treeHelper.idsOfExpanded
        def selectedNodeId = taskTree.selectedItems.find()?.id
        treeHelper.clear()
        treeHelper.buildTree(roots, collector)
        treeHelper.reexpand(expandedNodeIds)
        treeHelper.reselect(selectedNodeId)
    }


}