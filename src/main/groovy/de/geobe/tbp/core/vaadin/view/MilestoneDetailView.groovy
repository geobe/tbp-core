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

import com.vaadin.event.ShortcutAction
import com.vaadin.spring.annotation.SpringComponent
import com.vaadin.spring.annotation.UIScope
import com.vaadin.ui.Button
import com.vaadin.ui.Component
import com.vaadin.ui.ListSelect
import com.vaadin.ui.TextField
import com.vaadin.ui.TwinColSelect
import com.vaadin.ui.themes.ValoTheme
import de.geobe.tbp.core.domain.MilestoneState
import de.geobe.tbp.core.dto.FullDto
import de.geobe.tbp.core.dto.ListItemDto
import de.geobe.util.statemachine.samples.DVEvent
import de.geobe.util.statemachine.samples.DetailViewBehavior
import de.geobe.util.vaadin.builder.SubTree
import de.geobe.util.vaadin.type.VaadinSelectionListener

import static de.geobe.util.vaadin.builder.VaadinBuilder.C
import static de.geobe.util.vaadin.builder.VaadinBuilder.F

/**
 * A detail view for Milestone objects that allows editing
 * directly in the fields
 *
 * Created by georg beier on 24.04.2018.
 * @author georg beier
 */

@SpringComponent
@UIScope
class MilestoneDetailView extends SubTree
        implements VaadinSelectionListener<ListItemDto>,
                Serializable {

    /** define symbolic constants for ui element keys */
    public static final String NAME = 'name'
    public static final String STATE = 'state'
    public static final STATES = Arrays.asList(MilestoneState.values())
    public static final TASKS = 'taskSelect'

    /** variables bound to the generated vaadin elements make programming easier */
    private TextField name
    private ListSelect<String> state
    private TwinColSelect<ListItemDto> twinColSelect
    private Button newButton, editButton, saveButton, cancelButton

    /** item id of currently displayed item */
    private Tuple2<String, Serializable> currentItemId
    /** all we know about the actually displayed item */
    private FullDto currentDto
    /** the top level vaadin component of this view as
     * constructed by the vaadin builder */
    private Component topComponent

    /**
     * build component subtree.
     * @return topmost component (i.e. root) of subtree
     */
    @Override
    Component build() {
        topComponent = vaadin."$C.vlayout"('Milestone Details',
                [spacing: true, margin: true]) {
            "$F.text"('Task', [uikey: NAME,
                               width: '80%'])
            "$F.list"('State', [uikey: STATE,
                                items: STATES,
                                rows : STATES.size()])
            "$C.hlayout"([uikey: 'buttonfield', spacing: true]) {
                "$F.button"('New',
                        [uikey         : 'newbutton',
                         disableOnClick: true,
                         clickListener : { viewBehavior.execute(DVEvent.Create) }])
                "$F.button"('Edit',
                        [uikey         : 'editbutton',
                         disableOnClick: true,
                         clickListener : { viewBehavior.execute(DVEvent.Edit) }])
                "$F.button"('Cancel',
                        [uikey         : 'cancelbutton',
                         disableOnClick: true,
                         enabled       : false,
                         clickListener : { viewBehavior.execute(DVEvent.Cancel) }])
                "$F.button"('Save',
                        [uikey         : 'savebutton',
                         disableOnClick: true, enabled: false,
                         clickShortcut : ShortcutAction.KeyCode.ENTER,
                         styleName     : ValoTheme.BUTTON_PRIMARY,
                         clickListener : { viewBehavior.execute(DVEvent.Save) }])
            }
        }
        topComponent
    }

    /**
     * is fired when an entry of a selection component was selected
     *
     * @param event id of the selected element, normally containing its id and domain class key
     */
    @Override
    void onItemSelected(ListItemDto event) {

    }

    /**
     * View behavior is implemented using this specialised state machine
     */
    private DetailViewBehavior viewBehavior = new DetailViewBehavior() {
        /**
         * When editing an item was finished [Save] or cancelled [Cancel], notify the selector
         * component to enable it and eventually update the items changed caption
         * @param itemId identifies edited item
         * @param caption eventually updated caption of the edited item
         * @param mustReload Component must reload after new item was created
         *        or (tree-) structure changed
         */
        /**
         * Initialize all fields for a new item to display or edit. Given is its id,
         * so its full dto can be addressed and requested by an appropriate service.
         * This method is usually called by the selector component.
         * @param itemId unique identifying key
         */
        @Override
        void initItem(Long itemId) {

        }

        /** prepare for editing in CREATEEMPTY state */
        @Override
        protected void createemptymode() {

        }

        /** prepare for editing in CREATE state */
        @Override
        protected void createmode() {

        }

        /** prepare for editing in EDIT state */
        @Override
        protected void editmode() {

        }

        /** prepare EMPTY state */
        @Override
        protected void emptymode() {

        }

        /** prepare SHOW state */
        @Override
        protected void showmode() {

        }

        /** clear all editable fields */
        @Override
        protected void clearFields() {

        }

        /** leaving EDIT state with cancel,
         * so reset all fields from the current full dto object */
        @Override
        protected void onEditCancel() {

        }

        /**
         * leaving EDIT state with save,
         * saving current item after editing to persistent storage,
         * typically by calling an appropriate service.
         */
        @Override
        protected void onEditSave() {

        }

        @Override
        protected void onEditDone(boolean mustReload) {

        }
    }
}
