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

package de.geobe.tbp.core.repository;

import de.geobe.tbp.core.domain.CompoundTask;
import de.geobe.tbp.core.domain.Project;
import de.geobe.tbp.core.domain.Subtask;
import de.geobe.tbp.core.domain.Task;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

/**
 * Created by georg beier on 04.01.2018.
 */
public interface TaskRepository extends JpaRepository<Task, Long> {
//    List<Task> findByProjectId(Long id);
//    List<Task> findByProjectIdAndIdNotIn(Long pid, Collection<Long> taskIds);
//    List<Task> findBySprintsIdAndIdNotIn(Long spid, Collection<Long> taskIds);
//    List<Task> findBySprintsId(Long spid);
    List<Task> findAllByOrderByNameAsc();
    @Query(value = "SELECT * FROM TBL_TASK NATURAL JOIN TBL_COMPOUND_TASK", nativeQuery = true)
    List<CompoundTask> findAllCompoundTask();
    @Query(value = "SELECT * FROM TBL_TASK NATURAL JOIN TBL_SUBTASK", nativeQuery = true)
    List<Subtask> findAllSubtask();
    @Query(value = "SELECT * FROM TBL_TASK NATURAL JOIN TBL_COMPOUND_TASK NATURAL JOIN TBL_PROJECT", nativeQuery = true)
    List<Project> findAllProject();
}
