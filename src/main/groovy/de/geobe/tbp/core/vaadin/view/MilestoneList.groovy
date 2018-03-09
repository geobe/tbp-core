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

import com.vaadin.event.selection.SelectionEvent
import com.vaadin.spring.annotation.SpringComponent
import com.vaadin.spring.annotation.UIScope
import com.vaadin.ui.Component
import com.vaadin.ui.ListSelect
import de.geobe.tbp.core.dto.ListItemDto
import de.geobe.util.vaadin.builder.SubTree
import de.geobe.util.vaadin.type.DetailSelector

import static de.geobe.util.vaadin.builder.VaadinBuilder.C
import static de.geobe.util.vaadin.builder.VaadinBuilder.F

/**
 * Selection component for Milestones
 *
 * @author Georg Beier
 *
 */
@SpringComponent
@UIScope
class MilestoneList extends SubTree
        implements Serializable, DetailSelector<Tuple2<String, Serializable>> {

    final static String MILESTONELIST = 'milestonelist'

    private ListSelect<ListItemDto> mistList

    @Override
    Component build() {
        vaadin."$C.vlayout"() {
            "$C.panel"('Milestones', [spacing: true, margin: true]) {
                "$F.list"('Milestones',
                        [uikey            : MILESTONELIST,
                         caption          : 'Milestones',
                         rows             : 10,
                         width            : '25.0em',
                         selectionListener: { listValueChanged(it) }])
            }
        }
    }

    @Override
    void init(Object... value) {
        uiComponents = vaadin.uiComponents
        mistList = uiComponents."${subkeyPrefix + MILESTONELIST}"

    }

    private void listValueChanged(SelectionEvent event) {

    }

    @Override
    void onEditItemDone(Tuple2<String, Serializable> itemId, String caption, boolean mustReload) {

    }
}
