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

import de.geobe.tbp.core.domain.Milestone
import de.geobe.tbp.core.domain.MilestoneState
import de.geobe.tbp.core.domain.TaskState
import de.geobe.tbp.core.dto.Dto
import de.geobe.tbp.core.dto.FullDto
import de.geobe.tbp.core.dto.ListItemDto
import de.geobe.tbp.core.repository.MilestoneRepository
import de.geobe.tbp.core.repository.SubtaskRepository
import de.geobe.tbp.core.repository.TaskRepository
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

import java.time.LocalDate

/**
 * Created by georg beier on 20.04.2018.
 */
@Service
@Transactional
class MilestoneService {

    @Autowired
    private MilestoneRepository milestoneRepository
    @Autowired
    private SubtaskRepository subtaskRepository

    /**
     * Create a list sorted by name of Milestone ListItemDto objects
     */
    @Transactional(readOnly = true)
    List<ListItemDto> getMilestoneList() {
        def milestones = []
        milestoneRepository.findAllByOrderByNameAsc().each {
            milestones.add ListItemDto.makeDto(it)
        }
        milestones
    }

    /**
     * Create a Dto with all detail information for a milestone
     * @param id database key
     * @return the dto
     */
    @Transactional(readOnly = true)
    FullDto getMilestoneDetails(Serializable id) {
        Milestone milestone = milestoneRepository.findOne(id)
        FullDto dto = makeFullDto(milestone)
    }

    /**
     * Get a list Subtask dto's that could be related to this milestone.<br>
     * This method contains business logic that is not represented in the static
     * consistency rules of the domain model but should be added to tha
     * association handling methods. Rules implemented here are:
     * <ul><li>A COMPLETED Milestone cannot change its related tasks.</li>
     * <li>A COMPLETED or ABANDONED task cannot be assigned to a milestone.</li>
     * <li><em>Open question:</em> Can a Milestone be assigned to subtasks of
     * different projects?</li></ul>
     * @param id
     * @return list of potentially assignable subtasks
     */
    @Transactional(readOnly = true)
    List<ListItemDto> getSubtaskSelectList(Serializable id) {
        def subtasks = []
        Milestone milestone = milestoneRepository.findOne(id)
        if (milestone.state != MilestoneState.COMPLETED) {
            def subs = subtaskRepository.findAllByStateNotIn(
                    [TaskState.COMPLETED, TaskState.ABANDONED])
            subs.findAll {
                it.milestone.one?.id != id
            }.sort { a, b ->
                a.name <=> b.name
            }.each {
                subtasks.add ListItemDto.makeDto(it)
            }
        }
        subtasks
    }

    @Transactional
    FullDto createOrUpdateMilestone(FullDto command) {
        Milestone milestone
        // for update, database id is set, else it's a create
        if (command.id.second) {
            milestone = milestoneRepository.findOne(command.id.second)
        } else {
            milestone = new Milestone()
        }
        // set all fields
        milestone.name = command.args.name
        def st = command.args.state.find()
        milestone.state = st ?: MilestoneState.OPEN
        LocalDate date = command.args.duedate
        milestone.dueDate = date?.toDate()
        // handle association
        def newSubtaskIds = command.related.subtask.collect { it.id.second }
        def oldSubtaskIds = milestone.subtask.all.id
        def idsToRemove = oldSubtaskIds - newSubtaskIds
        def idsToAdd = newSubtaskIds - oldSubtaskIds
        def subsToRemove = subtaskRepository.findAll idsToRemove
        def subsToAdd = subtaskRepository.findAll idsToAdd
        milestone.subtask.add subsToAdd
        subsToRemove.each {milestone.subtask.remove it}
        milestoneRepository.save milestone
        makeFullDto milestone
    }

    /**
     * actually build the dto from domain object
     * @param milestone
     * @return the dto
     */
    private FullDto makeFullDto(Milestone milestone) {
        FullDto dto = new FullDto()
        if (milestone) {
            // create key
            dto.id = new Tuple2<String, Serializable>(
                    Dto.makeIdKey(milestone), milestone.id
            )
            dto.tag = milestone.name
            // set simple attributes
            dto.args['name'] = milestone.name
            dto.args['state'] = milestone.state
            def duedate = milestone.dueDate
            if (duedate instanceof Date) {
                // show date in localized date format
                dto.args['dueDate'] = duedate.toLocalDate()
            } else {
                // if no date is defined, set it to 1.1.2000
                dto.args['dueDate'] = LocalDate.of(2000, 1, 1)
            }
            // set association to subtasks
            dto.related.subtask = milestone?.subtask.all.collect {
                ListItemDto.makeDto(it)
            } ?: []
        }
        dto
    }
}