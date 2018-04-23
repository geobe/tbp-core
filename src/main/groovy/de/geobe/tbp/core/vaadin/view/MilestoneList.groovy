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

package de.geobe.tbp.core.vaadin.view

import com.vaadin.spring.annotation.SpringComponent
import com.vaadin.spring.annotation.UIScope
import com.vaadin.ui.Component
import de.geobe.util.vaadin.builder.SubTree
import de.geobe.util.vaadin.type.DetailSelector

// static import reduces code noise in vaadinbuilder pseudomethod strings
import static de.geobe.util.vaadin.builder.VaadinBuilder.C
import static de.geobe.util.vaadin.builder.VaadinBuilder.F

/**
 * Selection component for Milestones
 *
 * @author georg beier on 23.04.2018.
 */
@SpringComponent
@UIScope
class MilestoneList extends SubTree
        implements Serializable, DetailSelector<Tuple2<String, Long>> {

    public static final MILESTONE_LIST = 'milestonelist'
    /**
     * build component subtree.
     * @return topmost component (i.e. root) of subtree
     */
    @Override
    Component build() {
        vaadin."$C.vlayout"() {
            // wrap list in a panel to provide scroll bars, if needed
            "$C.panel"('Milestones', [spacing: true, margin: true]) {
                "$F.list"([uikey   : MILESTONE_LIST,
                           sizeFull: null])
            }
        }
    }

    /**
     * enable and update the selector component after editing an item
     * @param itemId identifies edited item
     * @param caption eventually updated caption of the edited item
     * @param mustReload Component must reload after new item was created
     *        or (tree-) structure changed
     */
    @Override
    void onEditItemDone(Tuple2<String, Long> itemId, String caption, boolean mustReload) {

    }
}
