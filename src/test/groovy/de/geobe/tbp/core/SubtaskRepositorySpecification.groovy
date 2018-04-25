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

import de.geobe.tbp.core.domain.TaskState
import de.geobe.tbp.core.repository.SubtaskRepository
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import spock.lang.Specification

/**
 * Created by georg beier on 25.04.2018.
 */
@SpringBootTest
class SubtaskRepositorySpecification extends Specification {

    @Autowired
    private SubtaskRepository subtaskRepository

    def "findAllByStateNotIn should allow to mask certain Subtask states"() {
        when: 'we retrieve all SubTasks'
        def su = subtaskRepository.findAll()
        and: 'we set some subtask states to COMPLETED or ABANDONED'
        su[0].state = TaskState.COMPLETED
        su[1].state = TaskState.COMPLETED
        su[2].state = TaskState.ABANDONED
        and: 'we update these states'
        subtaskRepository.save su
        and: 'get all with filter method'
        def filtered = subtaskRepository.findAllByStateNotIn([TaskState.COMPLETED, TaskState.ABANDONED])
        def filteredId = filtered.id
        then: 'these states should not be retrieved'
        ! filteredId.contains(su[0].id)
        ! filteredId.contains(su[1].id)
        ! filteredId.contains(su[2].id)
        filteredId.contains(su[3].id)
        filteredId.contains(su[-1].id)

    }
}
