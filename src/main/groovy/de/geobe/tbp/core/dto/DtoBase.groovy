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

package de.geobe.tbp.core.dto

/**
 * A Query Data Transfer Object that holds a list of QNodes to display in ListViews or similar
 * Created by georg beier on 09.01.2018.
 *
 */
class ListDto {
    LinkedHashMap<Long, NodeDto> all = [:]

    Long getFirstId() {
        if (all) {
            all.keySet().iterator().next()
        } else {
            0
        }
    }
}

/**
 * A Query Data Transfer Object that holds an identifying display string (for humans)
 * and the key of the underlying domain object and a map with lists of QNodes of all
 * kinds of related objects to display associations
 * @param <K>
 */
class NodeDto {
    Long id
    String type
    String displayName
    Map<String, List<NodeDto>> related = [:]

    @Override
    String toString() {
        this.@displayName
    }
}

/**
 * A Data Transfer Object for Query and Command operations that holds the key of the
 * underlying domain object, a map with all its attribute names and values and a map
 * with lists of QNodes of all related objects to display associations
 */
class FullDto {
    Long id = 0
    LinkedHashMap<String, Object> args = [:]
    Map<String, List<NodeDto>> related
}
