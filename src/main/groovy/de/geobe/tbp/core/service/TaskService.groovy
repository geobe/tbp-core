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

package de.geobe.tbp.core.service

import de.geobe.tbp.core.domain.CompoundTask
import de.geobe.tbp.core.domain.Project
import de.geobe.tbp.core.domain.Subtask
import de.geobe.tbp.core.domain.Task
import de.geobe.tbp.core.domain.TaskState
import de.geobe.tbp.core.dto.FullDto
import de.geobe.tbp.core.dto.NodeDto
import de.geobe.tbp.core.dto.TaskNodeDto
import de.geobe.tbp.core.repository.TaskRepository
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

/**
 * Created by georg beier on 09.01.2018.
 */
@Service
@Transactional
class TaskService {

    @Autowired
    TaskRepository taskRepository

    @Transactional(readOnly = true)
    List<TaskNodeDto> getProjectTreeRoots() {
        def projects = taskRepository.findAllProject()
        def nodelist = []
        projects.each { Project project ->
            nodelist.add makeTaskTree(project)
        }
        nodelist
    }

    @Transactional(readOnly = true)
    FullDto getTaskDetails(Tuple2<String, Serializable> id) {
        def task = taskRepository.getOne(id.value)
        FullDto dto = makeFullDto(task)
        dto
    }

    @Transactional
    FullDto createOrUpdateTask(FullDto command) {
        Task task
        if (command.id.second) {
            task = taskRepository.getOne(command.id.second)
        } else {
            task = makeTask(command.id)
        }
        task.name = command.args['name']
        task.description = command.args['description']
        task.state = TaskState.valueOf command.args['state'].toString()
        task.timeBudget = (Float) command.args['timeBudget']
        task.completionDate = (Date) command.args['completionDate']
        task = taskRepository.saveAndFlush task
        makeFullDto(task)
    }

    private TaskNodeDto makeTaskTreeNode(Task task) {
        NodeDto dto = new TaskNodeDto(
                [id : new Tuple2<String, Serializable>(makeIdKey(task), task.id),
                 tag: task.name])
        dto
    }

    private makeIdKey(def persistentObject) {
        def cln = persistentObject.class.name
        def key = cln.replaceFirst(/.*\./, '')
        key
    }

    private TaskNodeDto makeTaskTree(CompoundTask task) {
        def dto = makeTaskTreeNode(task)
        if (task.subtask) {
            List<NodeDto> subtaskDtos = new ArrayList<>()
            dto.related['subtask'] = subtaskDtos
            task.subtask.all.each {
                subtaskDtos.add(makeTaskTree(it))
            }
        }
        dto
    }

    private TaskNodeDto makeTaskTree(Subtask task) {
        makeTaskTreeNode(task)
    }

    private FullDto makeFullDto(Task task) {
        FullDto dto = new FullDto()
        if (task) {
            dto.id = new Tuple2<String, Serializable>(makeIdKey(task), task.id)
            dto.args['name'] = task.name
            dto.args['description'] = task.description
            dto.args['state'] = task.state
            dto.args['timeUsed'] = task.timeUsed
            dto.args['timeBudget'] = task.timeBudget
            dto.args['scheduledCompletionDate'] = task.scheduledCompletionDate
            dto.args['completionDate'] = task.completionDate
        }
        dto
    }

    private Task makeTask(Tuple2<String, Serializable> id) {
        Task task
        switch (id.first) {
            case ~/.*Project/:
                task = new Project()
                break
            case ~/.*CompoundTask/:
                task = new CompoundTask()
                break
            case ~/.*Subtask/:
                task = new Subtask()
                break
            default:
                throw new IllegalArgumentException("wrong id key: ${id.first}")
        }
        task
    }
}
