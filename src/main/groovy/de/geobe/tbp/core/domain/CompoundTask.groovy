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

package de.geobe.tbp.core.domain

import de.geobe.util.association.IGetOther
import de.geobe.util.association.IToAny
import de.geobe.util.association.ToMany

import javax.persistence.*

/**
 * A task wrapping several subtasks
 * Created by georg beier on 03.01.2018.
 */
@Entity // markiert die Klasse für die Abbildung in DB
@Table(name="TBL_COMPOUND_TASK") // optionale Angabe der Tabelle
class CompoundTask extends Task {

    @OneToMany(mappedBy = 'supertask',
            cascade = CascadeType.PERSIST, fetch = FetchType.LAZY)
    protected List<Task> subtasks = new ArrayList<>()
    @Transient
    private ToMany<CompoundTask, Task> toSubtasks = new ToMany<>(
            { this.@subtasks } as IToAny.IGet, this,
            { Task t -> t.supertask } as IGetOther
    )
    IToAny<Task> getSubtask() { toSubtasks }


    /**
     * get scheduled completion time by evaluating all subtasks
     */
    @Override
    Date getScheduledCompletionDate() {
        if(subtasks.size() > 0) {
            def enddate = subtasks.collect {it.scheduledCompletionDate}.min()
            return enddate
        } else {
            return super.scheduledCompletionDate
        }
    }

    /**
     * get the time already spent on this task by adding all subtasks
     * @return time in working hours
     */
    @Override
    Float getTimeUsed() {
        float tused = 0.0
        subtasks.each {tused += it.timeUsed}
        return tused
    }
}
