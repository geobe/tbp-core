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
import com.vaadin.data.TreeData
import com.vaadin.data.provider.TreeDataProvider
import com.vaadin.server.VaadinRequest
import com.vaadin.spring.annotation.SpringUI
import com.vaadin.ui.Tree
import com.vaadin.ui.UI
import de.geobe.tbp.core.dto.TaskNodeDto
import de.geobe.tbp.core.repository.TaskRepository
import de.geobe.tbp.core.service.TaskService
import org.springframework.beans.factory.annotation.Autowired

/**
 * Created by georg beier on 15.01.2018.
 */
@Theme('valo')
@SpringUI(path = "/testtree")
class TreeUI extends UI {

    @Autowired
    TaskRepository taskRepository
    @Autowired
    TaskService taskService

    private Tree<TaskNodeDto> taskTree = new Tree<>()
    private TreeData<TaskNodeDto> taskData = new TreeData()
    private TreeDataProvider<TaskNodeDto> taskDataProvider

    @Override
    protected void init(VaadinRequest request) {
        taskDataProvider = makeProjectTreeDataProvider()
        taskTree.dataProvider = taskDataProvider
        setContent(taskTree)

    }

    private makeProjectTreeDataProvider() {
        def projNodes = taskService.getProjectTree()
        projNodes.each { TaskNodeDto projNode ->
            taskData.addItem(null, projNode)
            addSubNodes(taskData, projNode)
        }
        new TreeDataProvider<TaskNodeDto>(taskData)
    }

    private addSubNodes(TreeData<TaskNodeDto> data, TaskNodeDto base) {
        def subtasks = base.related['subtask']
        if(subtasks) {
            data.addItems(base, subtasks)
            subtasks.each { TaskNodeDto sub ->
                    addSubNodes(data, sub)
            }
        }
    }
}
