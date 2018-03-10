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
import de.geobe.tbp.core.domain.Milestone
import de.geobe.tbp.core.domain.Project
import de.geobe.tbp.core.domain.Subtask
import de.geobe.tbp.core.domain.Task
import de.geobe.tbp.core.domain.TaskState
import de.geobe.tbp.core.dto.FullDto
import de.geobe.tbp.core.dto.ListItemDto
import de.geobe.tbp.core.dto.NodeDto
import de.geobe.tbp.core.dto.TaskNodeDto
import de.geobe.tbp.core.repository.MilestoneRepository
import de.geobe.tbp.core.repository.TaskRepository
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

import java.time.LocalDate

/**
 * Created by georg beier on 09.01.2018.
 */
@Service
@Transactional
class TaskService {

    @Autowired
    TaskRepository taskRepository
    @Autowired
    MilestoneRepository milestoneRepository

    @Transactional(readOnly = true)
    List<TaskNodeDto> getProjectTree() {
        def projects = taskRepository.findAllProject()
        def nodelist = []
        projects.each { Project project ->
            nodelist.add makeTaskSubTree(project)
        }
        nodelist
    }

    @Transactional(readOnly = true)
    FullDto getTaskDetails(Serializable id) {
        Task task = taskRepository.findOne(id)
        FullDto dto = makeFullDto(task)
        dto
    }

    @Transactional(readOnly = true)
    FullDto getProject(Serializable id) {
        Task task = taskRepository.findOne(id)
        Task top = getTopTask(task)
        makeFullDto(top)
    }

    @Transactional(readOnly = true)
    List<ListItemDto> getMilestonesInProject(Serializable id) {
        Task task = taskRepository.findOne(id)
        getMilestones(task)
    }

    @Transactional
    FullDto createOrUpdateTask(FullDto command) {
        Task task
        if (command.id.second) {
            task = taskRepository.findOne(command.id.second)
        } else {
            task = makeTask(command.id)
            def superId = command.related.supertask?.first()?.id?.second
            if (superId) {
                setSupertask(task, superId)
            }
        }
        task.name = command.args['name']
        task.description = command.args['description']
        def st = command.args['state'].find()
        task.state = st ?: TaskState.PLANNED
        def fl = command.args['timeBudget']
        if (fl && fl ==~ /[0-9]*\.{0,1}[0-9]*/) {
            task.timeBudget = Float.valueOf(fl)
        }
        LocalDate date = command.args['completionDate']
        task.completionDate = date?.toDate()//LocalDateExtension.toDate(date)
        date = command.args['scheduledCompletionDate']
        task.scheduledCompletionDate = date?.toDate()//LocalDateExtension.toDate(date)
        task = taskRepository.saveAndFlush task
        makeFullDto(task)
    }

    /**
     * Set appropriate supertask for a new task. If id identifies a Subtask, take its supertask.
     * @param task the new task
     * @param superId id of supertask candidate
     */
    private void setSupertask(Task task, Long superId) {
        Task supertask = taskRepository.findOne(superId)
        if (supertask) {
            if (supertask instanceof Subtask)
                supertask = supertask.supertask.one
            task.supertask.add supertask
        }
    }

    /**
     * Polymorphic version of the method for projects. Per definition, a Project
     * has no supertask, so do nothing
     * @param project ignored
     * @param superId ignored
     */
    private void setSupertask(Project project, Long superId) {
        // do nothing!
    }

    private TaskNodeDto makeTaskSubTree(CompoundTask task) {
        def dto = makeTaskNode(task)
        if (task.subtask) {
            List<NodeDto> subtaskDtos = new ArrayList<>()
            dto.related['subtask'] = subtaskDtos
            task.subtask.all.each {
                subtaskDtos.add(makeTaskSubTree(it))
            }
        }
        dto
    }

    private TaskNodeDto makeTaskSubTree(Subtask task) {
        makeTaskNode(task)
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
            def sccomp = task.scheduledCompletionDate
            if (sccomp instanceof Date) {
                dto.args['scheduledCompletionDate'] = sccomp.toLocalDate();
            } else {
                dto.args['scheduledCompletionDate'] = LocalDate.of(2000, 1, 1)
            }
            def comp = task.completionDate
            if (comp instanceof Date) {
                dto.args['completionDate'] = comp.toLocalDate()
            } else {
                dto.args['completionDate'] = LocalDate.of(2000, 1, 1)
            }
            dto.related.supertask = task?.supertask.all.collect { makeTaskNode(it) } ?: []
            if (task instanceof CompoundTask) {
                dto.related.subtask = task?.subtask.all.collect { makeTaskNode(it) } ?: []
            } else if (task instanceof Subtask) {
                dto.related.milestone = getAssignedMilestones(task)
                dto.related.projectmilestones = getMilestones(task)
                dto.related.unassignedmilestones = getUnassignedMilestones()
            }
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

    private List<ListItemDto> getAssignedMilestones(Subtask task) {
        if(task.milestone.one)
            [makeMilestoneItem(task.milestone.one)]
        else
            []
    }

    private List<ListItemDto> getMilestones(Task task) {
        Task top = getTopTask(task)
        def milestones = getAllSubtasks(top).collect { it.milestone.one }.grep() toSet()
        def used = milestones?.collect { makeMilestoneItem(it)} ?: []
        def free = unassignedMilestones
        used + free
    }

    private List<ListItemDto> getUnassignedMilestones() {
        def milestones = milestoneRepository.findAllBySubtasksIsNull()
        milestones.collect { makeMilestoneItem(it)}
    }

    public TaskNodeDto makeTaskNode(Task task) {
        NodeDto dto = new TaskNodeDto(
                [id : new Tuple2<String, Serializable>(makeIdKey(task), task.id),
                 tag: task.name])
        dto
    }

    public makeMilestoneItem(Milestone mist) {
        new ListItemDto([id : new Tuple2<String, Long>(makeIdKey(mist), mist.id),
                         tag: mist.name])
    }

    private makeIdKey(def persistentObject) {
        def cln = persistentObject.class.name
        def key = cln.replaceFirst(/.*\./, '')
        key
    }

    private List<Subtask> getAllSubtasks(Task t) {
        def subs = []
        if (t instanceof Subtask) {
            subs << t
        } else if(t instanceof CompoundTask) {
            subs = t.subtask.all.collect {getAllSubtasks(it)} flatten()
        }
        subs
    }

    private Task getTopTask(Task task) {
        def top = task?.supertask.one ?: task
        while (top?.supertask.one) {
            top = top.supertask.one
        }
        top
    }

}
