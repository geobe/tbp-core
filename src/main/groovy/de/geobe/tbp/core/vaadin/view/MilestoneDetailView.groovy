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

import com.vaadin.data.provider.DataProvider
import com.vaadin.event.ShortcutAction
import com.vaadin.spring.annotation.SpringComponent
import com.vaadin.spring.annotation.UIScope
import com.vaadin.ui.Button
import com.vaadin.ui.Component
import com.vaadin.ui.DateField
import com.vaadin.ui.ListSelect
import com.vaadin.ui.TextField
import com.vaadin.ui.TwinColSelect
import com.vaadin.ui.themes.ValoTheme
import de.geobe.tbp.core.domain.MilestoneState
import de.geobe.tbp.core.dto.FullDto
import de.geobe.tbp.core.dto.ListItemDto
import de.geobe.tbp.core.service.MilestoneService
import de.geobe.util.statemachine.samples.DVEvent
import de.geobe.util.statemachine.samples.DVState
import de.geobe.util.statemachine.samples.DetailViewBehavior
import de.geobe.util.vaadin.builder.SubTree
import de.geobe.util.vaadin.type.VaadinSelectionListener
import org.springframework.beans.factory.annotation.Autowired

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
    public static final String DATE_DUE = 'dateDue'
    public static final STATES = Arrays.asList(MilestoneState.values())
    public static final SUBTASKS = 'taskSelect'

    /** variables bound to the generated vaadin elements make programming easier */
    private TextField name
    private ListSelect<String> state
    private DateField dateDue
    private TwinColSelect<ListItemDto> subtaskSelect
    private Button newButton, editButton, saveButton, cancelButton

    /** item id of currently displayed item */
    private Tuple2<String, Serializable> currentItemId
    /** all we know about the actually displayed item */
    private FullDto currentDto
    /** selectable subtasks for current milestone */
    private List<ListItemDto> selectableSubtasks = []

    /** the top level vaadin component of this view as
     * constructed by the vaadin builder */
    private Component topComponent

    /** access to the service layer */
    @Autowired
    private MilestoneService milestoneService
    /** the corresponding selection component */
    @Autowired
    private MilestoneList milestoneList

    /**
     * build component subtree.
     * @return topmost component (i.e. root) of subtree
     */
    @Override
    Component build() {
        topComponent = vaadin."$C.vlayout"('Milestone Details',
                [spacing: true, margin: true]) {
            "$F.text"('Milestone', [uikey: NAME,
                                    width: '80%'])
            "$F.list"('State', [uikey: STATE,
                                items: STATES,
                                rows : STATES.size()])
            "$F.date"('Date due', [uikey: DATE_DUE])
            "$F.twincol"('Subtasks',
                    [uikey             : SUBTASKS,
                     rows              : 9,
                     width             : '80%',
                     leftColumnCaption : 'available',
                     rightColumnCaption: 'assigned'])
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
     * Initialize subtree components. Should be called after whole
     * component tree is built. Call sequence of different subtrees +
     * may be important if they have dependencies.
     * @param value various parameters needed for initialization, optional
     */
    @Override
    void init(Object... value) {
        // assign all Vaadin dialog components to local variables
        // for easy access
        name = subtreeComponent NAME
        state = subtreeComponent STATE
        dateDue = subtreeComponent DATE_DUE
        subtaskSelect = subtreeComponent SUBTASKS
        newButton = subtreeComponent "newbutton"
        editButton = subtreeComponent "editbutton"
        saveButton = subtreeComponent "savebutton"
        cancelButton = subtreeComponent "cancelbutton"
        // register this component as listener to selection components
        milestoneList.milestoneDetailView = this
        // create and initialize state machine in appropriate creation state
        viewBehavior.initSm(DVState.TOPVIEW)
        // then execute init event
        viewBehavior.execute(DVEvent.Init)

    }

    /**
     * is called when an entry of a selection component was selected
     *
     * @param event id of the selected element
     */
    @Override
    void onItemSelected(ListItemDto event) {
        currentItemId = event.id
        viewBehavior.execute(DVEvent.Select, currentItemId.second)
    }

    /**
     * View behavior is implemented using this specialised state machine
     */
    private DetailViewBehavior viewBehavior = new DetailViewBehavior() {

        /**
         * prepare EMPTY state:
         * All inputs disabled, only New button enabled
         */
        @Override
        protected void emptymode() {
            clearFields()
            currentDto = null
            [name, state, dateDue, subtaskSelect, saveButton,
             cancelButton, editButton].each { it.enabled = false }
            [newButton].each { it.enabled = true }
        }

        /** prepare for editing in CREATEEMPTY state */
        @Override
        protected void createemptymode() {
            prepareEdit()
            presetEmpty()
        }

        /** prepare for editing in CREATE state */
        @Override
        protected void createmode() {
            prepareEdit()
            presetEmpty()
        }

        /** prepare for editing in EDIT state */
        @Override
        protected void editmode() {
            prepareEdit()
        }

        /** prepare SHOW state */
        @Override
        protected void showmode() {
            [name, state, dateDue, subtaskSelect, saveButton,
             cancelButton].each { it.enabled = false }
            [newButton, editButton].each { it.enabled = true }
        }

        /**
         * Any transition triggered by select event (i.e. a new item was selected in
         * the selector component) has to load the new item from the service layer
         * and load its attributes into the field components.
         * Deprecates initItem method
         * @param itemId unique identifying key
         */
        @Override
        protected void onItemSelected(Long itemId) {
            currentDto = milestoneService.getMilestoneDetails(itemId)
            def selSub = milestoneService.getSubtaskSelectList(itemId)
            selectableSubtasks = selSub
            subtaskSelect.dataProvider = DataProvider.ofCollection([])
            setFieldsFromDto()
        }

        /**
         * leaving CREATE or CREATEEMPTY state with save,
         * saving created item to persistent storage by calling
         * appropriate milestoneService method.
         */
        @Override
        protected void onCreateSave() {
            createOrUpdate(new Tuple2<String, Long>('Milestone', 0L))
            onEditDone(true)
        }

        /**
         * leaving CREATE or CREATEEMPTY  state with cancel
         * */
        @Override
        protected void onCreateCancel() {
            onEditDone(false)
        }

        /** leaving EDIT state with cancel,
         * so reset all fields from the current full dto object */
        @Override
        protected void onEditCancel() {
            setFieldsFromDto()
        }

        /**
         * leaving EDIT state with save,
         * saving current item after editing to persistent storage,
         * typically by calling an appropriate service.
         */
        @Override
        protected void onEditSave() {
            def mustReload = name.value != currentDto.args.name
            createOrUpdate(currentItemId)
            onEditDone(mustReload)
        }

        private void createOrUpdate(Tuple2<String, Long> id) {
            FullDto command = new FullDto()
            command.id = id
            def args = command.args
            command.args.name = name.value
            command.args.state = state.value
            command.related.subtask = subtaskSelect?.selectedItems.asList() ?: []

            currentDto = milestoneService.createOrUpdateMilestone(command)
            currentItemId = currentDto.id
            selectableSubtasks =
                    milestoneService.getSubtaskSelectList(currentItemId.second)
        }

        /**
         * When editing an item was finished [Save] or cancelled [Cancel], notify the
         * selector component to enable itself and eventually update the
         * items changed caption
         * @param itemId identifies edited item
         * @param caption eventually updated caption of the edited item
         * @param mustReload Component must reload after new item was created
         *        or (tree-) structure changed
         */
        @Override
        protected void onEditDone(boolean mustReload) {
            milestoneList.onEditItemDone(
                    currentItemId,
                    currentDto.tag ?: '',
                    mustReload)
        }

        /** clear all editable fields */
        @Override
        protected void clearFields() {
            state.deselectAll()
            subtaskSelect.dataProvider = DataProvider.ofCollection([])
            [name, dateDue].each { it.clear() }
        }

        /**
         * set fields and buttons ready for editing and inhibit
         * selection component
         */
        private void prepareEdit() {
            milestoneList.onEditItem()
            [name, state, dateDue, subtaskSelect, saveButton,
             cancelButton].each { it.enabled = true }
            [newButton, editButton].each { it.enabled = false }
        }

        /**
         * load selectable subtasks for new milestone
         */
        private void presetEmpty() {
            def all = milestoneService.getSubtaskSelectList(0L)
            subtaskSelect.dataProvider = DataProvider.ofCollection(all)
        }

        /**
         * set all dialog fields from currentDto values
         * provide default values to avoid vaadin exceptions
         */
        private void setFieldsFromDto() {
            name.value = currentDto.args.name ?: ''
            state.deselectAll()
            state.select currentDto.args.state ?: ''
            def select = currentDto.related.subtask
            def all = selectableSubtasks + select
            subtaskSelect.deselectAll()
            subtaskSelect.dataProvider = DataProvider.ofCollection(all)
            subtaskSelect.deselectAll()
            subtaskSelect.select(select.toArray())
        }

    }
}
