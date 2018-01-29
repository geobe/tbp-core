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

import com.vaadin.annotations.Theme
import com.vaadin.server.VaadinRequest
import com.vaadin.spring.annotation.SpringUI
import com.vaadin.ui.Component
import com.vaadin.ui.UI
import de.geobe.util.vaadin.builder.VaadinBuilder
import org.springframework.beans.factory.annotation.Autowired

import static VaadinBuilder.C
import static VaadinBuilder.F


/**
 * The main UI class that builds a frame for all subordinate view classes. <br>
 * A vaadin UI is constructed by a tree of vaadin components and vaadin containers.
 * In this class, the root of the tree and its main "branches" are constructed.
 * Such a branch is a subtree of vaadin ui elements that are constructed and configured
 * in separate components (-> classes) supporting the concept of separation of concerns.

 * Created by georg beier on 18.01.2018.
 */
@SpringUI(path = '/main')
@Theme("valo")
class MainUI extends UI {

    private VaadinBuilder vaadin
    def widgets = [:]

    // here all subordinate views get autowired
    @Autowired
    private TaskStructureTree taskStructureTree
    @Autowired
    private TaskDetailView taskDetailView

    // here are all the ui components
    private Component root, taskSelectTree, taskDetails


    @Override
    protected void init(VaadinRequest request) {
        page.title = 'TimeBudgetPlanning V. 0.1'
        content = initBuilder()
        initComponents()

    }

    Component initBuilder() {
        vaadin = new VaadinBuilder()
        // construct all view building blocks
        taskSelectTree = taskStructureTree.buildSubtree(vaadin, 'tasktree')
        taskDetails = taskDetailView.buildSubtree(vaadin, 'taskdetail')

        // build the top level view, i.e. the root of the vaadin component tree
        root = vaadin."$C.hsplit"([uikey: 'topsplit', splitPosition: 40.0f]) {
            "$F.subtree"(taskSelectTree, [uikey: 'tasktree'])
            "$F.subtree"(taskDetails, [uikey: 'taskpanel', caption: 'Task Editor'])
        }

    }

    void initComponents() {
        taskDetailView.init()
        taskStructureTree.init()
        taskStructureTree.expandAll()
    }
}
