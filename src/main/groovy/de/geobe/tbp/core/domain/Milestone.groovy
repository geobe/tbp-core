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

import de.geobe.util.association.IToAny
import de.geobe.util.association.ToMany

import javax.persistence.CascadeType
import javax.persistence.Entity
import javax.persistence.GeneratedValue
import javax.persistence.GenerationType
import javax.persistence.Id
import javax.persistence.OneToMany
import javax.persistence.Table
import javax.persistence.Transient

/**
 * Created by georg beier on 08.03.2018.
 */
// save entity class into DB
@Entity
// optionally name the db table
@Table(name = "TBL_MILESTONES")
class Milestone {
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private Long id;
    String name
    MilestoneState state
    Date dueDate = new Date() + (14 + new Random(System.currentTimeMillis()).nextInt(42))

    Long getId() { id }

    // association to subtasks
    @OneToMany(mappedBy = 'milestone',
            cascade = CascadeType.PERSIST)
    // mark as an association for JPA
    private List<Subtask> subtasks = new ArrayList<>()
    @Transient
    // exclude from database mapping
    private ToMany<Milestone, Subtask> toSubtasks = new ToMany<>(
            { this.@subtasks }, this,
            { Subtask sub -> sub.milestone }
    )

    IToAny<Subtask> getSubtask() { toSubtasks }
}
