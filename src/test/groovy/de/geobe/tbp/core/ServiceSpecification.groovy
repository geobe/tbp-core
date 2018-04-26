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

package de.geobe.tbp.core

import de.geobe.tbp.core.domain.MilestoneState
import de.geobe.tbp.core.domain.TaskState
import de.geobe.tbp.core.dto.FullDto
import de.geobe.tbp.core.repository.TaskRepository
import de.geobe.tbp.core.service.MilestoneService
import de.geobe.tbp.core.service.TaskService
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import spock.lang.Specification

/**
 * Created by georg beier on 20.04.2018.
 */
@SpringBootTest
// use Spring test framework
class ServiceSpecification extends Specification {

    /** Spring beans are accessible */
    @Autowired
    private MilestoneService milestoneService
    @Autowired
    private TaskService taskService

    /**
     * A Spock specification should have a long meaningful name
     */
    def "getMilestoneList should give all milestone dtos"() {
        when: "a list of milestone dtos is fetched"
        def miles = milestoneService.milestoneList
        then: 'all 4 milestones should be in the list'
        miles.size() == 4
        miles.tag.contains('Slides Available')
        miles.tag.contains('Monolithic Examples Done')
        miles.tag.contains('Microservice Examples Done')
        miles.tag.contains('Simple Examples Done')
    }

    def 'getMilestoneDetails should give full milestone dto'() {
        when: 'we have a milestone'
        def mst = milestoneService.getMilestoneDetails(1L)
        println "\n\n-------------"
        print "\t\t$mst.tag: "
        println mst.related.subtask.id
        println "-------------\n\n"
        then:
        mst.tag.startsWith('Slides')
        mst.args.name.startsWith('Slides')
        mst.related.subtask.size() == 3
        true
    }

    def 'getSubtaskSelectList should give the right subtasks'() {
        when: 'we have a milestone'
        def mst1 = milestoneService.getMilestoneDetails(1L)
        def mst1subt = mst1.related.subtask.id
        and: 'we abandon the subtask of a second milestone'
        def mst2 = milestoneService.getMilestoneDetails(2L)
        def mst2subt = mst2.related.subtask.id
        mst2.related.subtask.each {
            def sub = taskService.getTaskDetails(it.id.second)
            sub.args.state = TaskState.ABANDONED
            taskService.createOrUpdateTask(sub)
        }
        println "\n\n-------------"
        print "\t\t$mst1.tag: "
        println mst1.related.subtask.id
        print "\t\t$mst2.tag: "
        println mst2.related.subtask.tag
        and: 'we get the subtask select list for the first milestone'
        def available = milestoneService.getSubtaskSelectList(1L)
        def gotIds = available.id
        available.each {println it}
        println "-------------\n\n"
        then: 'abandoned, completed and own tasks should not show'
        ! mst1subt.intersect(gotIds) // own subtasks not contained
        ! mst2subt.intersect(gotIds) // abandoned subtasks not contained
    }

    def 'createOrUpdateMilestone should update an existing Milestone'(){
        when: 'we have milestone FullDtos'
        def mst1 = milestoneService.getMilestoneDetails(1L)
        def mst2 = milestoneService.getMilestoneDetails(2L)
        and: 'we change attributes and associated subtasks'
        mst1.args.name = 'TestNameToUpdate'
        mst1.args.state = MilestoneState.COMPLETED
        def subs1 = mst1.related.subtask
        def subs2 = mst2.related.subtask
        def removed = subs1[2].id.second
        def added = subs2[1].id.second
        subs1.remove(2)
        subs1.add(subs2[1])
        mst1.related.subtask = subs1
        and: 'we update the Milestone'
        milestoneService.createOrUpdateMilestone(mst1)
        and: 'retrieve it back from db'
        def mstDb = milestoneService.getMilestoneDetails(1L)
        def rel = mstDb.related.subtask.collect {it.id.second}
        then: 'all changes should be there'
        mstDb.args.name == 'TestNameToUpdate'
        mstDb.args.state == MilestoneState.COMPLETED
        rel.contains(added)
        ! rel.contains(removed)
    }

    def 'createOrUpdateMilestone should create a new Milestone'(){
        when: 'we have milestone FullDtos'
        def mstcr1 = new FullDto()
        def mst3 = milestoneService.getMilestoneDetails(3L)
        and: 'we set attributes and associated subtasks'
        mstcr1.args.name = 'TestNameToCreate'
        mstcr1.args.state = MilestoneState.OPEN
        def subs2 = mst3.related.subtask
        mstcr1.related.subtask = subs2
        def ids = subs2.id.second
        and: 'we update the Milestone'
        def mstsaved = milestoneService.createOrUpdateMilestone(mstcr1)
        def newid = mstsaved.id.second
        and: 'retrieve it back from db'
        def mstDb = milestoneService.getMilestoneDetails(newid)
        def rel = mstDb.related.subtask.collect {it.id.second}
        then: 'all values should be there'
        newid
        mstDb.args.name == 'TestNameToCreate'
        rel.containsAll(ids)
        mstDb.args.state == MilestoneState.OPEN
    }
}
