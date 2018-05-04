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

package de.geobe.tbp.core.vaadin.view.helper

import de.geobe.tbp.core.dto.NodeDto

/**
 * Created by georg beier on 04.05.2018.
 */

/**
 * A helper class for inserting association nodes into the tree view.
 * Keeping it local for a project allows to have a project-specific
 * display options for associations
 */
class AssociationNode extends NodeDto {
    public static final String LINK = 'ASSOCIATED'
    public AssociationNode(NodeDto parent, String rel) {
        id = new Tuple2<String, Long>(
                '-ASSOCIATED',
                parent.id.second)
        tag = rel.capitalize()
        related.ASSOCIATED = parent.related[rel]
    }

    @Override
    String toString() {
        "-> $tag"
    }
}
