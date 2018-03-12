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

import com.vaadin.data.provider.ListDataProvider
import com.vaadin.event.ShortcutAction
import com.vaadin.spring.annotation.SpringComponent
import com.vaadin.spring.annotation.UIScope
import com.vaadin.ui.*
import com.vaadin.ui.themes.ValoTheme
import de.geobe.tbp.core.domain.TaskState
import de.geobe.tbp.core.dto.FullDto
import de.geobe.tbp.core.dto.ListItemDto
import de.geobe.tbp.core.dto.NodeDto
import de.geobe.tbp.core.service.TaskService
import de.geobe.util.statemachine.samples.DVEvent
import de.geobe.util.statemachine.samples.DVState
import de.geobe.util.statemachine.samples.DetailViewBehavior
import de.geobe.util.vaadin.builder.SubTree
import de.geobe.util.vaadin.builder.VaadinBuilder
import de.geobe.util.vaadin.type.VaadinSelectionListener
import de.geobe.util.vaadin.type.VaadinTreeRootChangeListener
import org.springframework.beans.factory.annotation.Autowired

import java.time.LocalDate

import static VaadinBuilder.C
import static VaadinBuilder.F

/**
 * editor tab for tasks
 *
 * @author georg beier
 */

@SpringComponent
@UIScope
class TaskDetailView extends SubTree
        implements VaadinSelectionListener<NodeDto>, VaadinTreeRootChangeListener<NodeDto>,
                Serializable {

    /** define symbolic constants for ui element keys */
    public static final String TYPE = 'type'
    public static final String NAME = 'name'
    public static final String DESCRIPTION = 'description'
    public static final String STATE = 'state'
    public static final STATES = Arrays.asList(TaskState.values())
    public static final String TIME_BUDGET_PLAN = 'timeBudgetPlan'
    public static final String TIME_BUDGET_USED = 'timeBudgetUsed'
    public static final String COMPLETION_DATE_PLAN = 'completionDatePlan'
    public static final String COMPLETION_DATE_DONE = 'completionDateDone'
    public static final String MILESTONE = 'milestoneSelect'

    /** variables bound to the generated vaadin elements make programming easier */
    private TextField name, timeBudget, timeUsed
    private TextArea description
    private ListSelect<String> state
    private DateField scheduledCompletionDate, completionDate
    private ListSelect<ListItemDto> milestoneSelect
    private Button newButton, editButton, saveButton, cancelButton

    /** item id of currently displaed item */
    private Tuple2<String, Serializable> currentItemId
    /** sometimes we may need the root object id on top of the object tree */
    private Tuple2<String, Serializable> currentTopItemId
    /** all we know about the actually displayed item */
    private FullDto currentDto
    /** the top level vaadin ui element (window) */
    private UI ui
    /** the top level vaadin component of this view as constructed by the vaadin builder */
    private Component topComponent

    private NewTaskDialog dialog = new NewTaskDialog()

    @Autowired
    private TaskService taskService
    @Autowired
    private TaskStructureTree taskTree

    @Override
    Component build() {
        topComponent = vaadin."$C.vlayout"('TaskView',
                [spacing: true, margin: true]) {
            "$F.text"('Task', [uikey: NAME,
                               width: '80%'])
            "$F.textarea"('Description', [uikey: DESCRIPTION,
                                          width: '80%'])
            "$F.list"('State', [uikey: STATE,
                                items: STATES,
                                rows : STATES.size()])
            "$C.hlayout"([uikey       : 'timebudgets',
                          spacing     : true,
                          gridPosition: [0, 1, 1, 1]]) {
                "$F.text"('Assigned Time Budget', [uikey: TIME_BUDGET_PLAN])
                "$F.text"('Used Time Budget', [uikey  : TIME_BUDGET_USED,
                                               enabled: false])
            }
            "$C.hlayout"([uikey       : 'schedules',
                          spacing     : true,
                          gridPosition: [0, 1, 1, 1]]) {
                "$F.date"('Scheduled Completion', [uikey: COMPLETION_DATE_PLAN])
                "$F.date"('Completion achieved', [uikey  : COMPLETION_DATE_DONE,
                                                  enabled: false])
            }
            "$F.list"('Milestone', [uikey: MILESTONE,
                                    rows : 5,
                                    width: '80%'])
            "$C.hlayout"([uikey       : 'buttonfield', spacing: true,
                          gridPosition: [0, 3, 1, 3]]) {
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

    @Override
    void init(Object... value) {
        uiComponents = vaadin.uiComponents
        name = uiComponents."$subkeyPrefix$NAME"
        description = uiComponents."$subkeyPrefix$DESCRIPTION"
        state = uiComponents."$subkeyPrefix$STATE"
        timeBudget = uiComponents."$subkeyPrefix$TIME_BUDGET_PLAN"
        timeUsed = uiComponents."$subkeyPrefix$TIME_BUDGET_USED"
        scheduledCompletionDate = uiComponents."$subkeyPrefix$COMPLETION_DATE_PLAN"
        completionDate = uiComponents."$subkeyPrefix$COMPLETION_DATE_DONE"
        milestoneSelect = uiComponents."$subkeyPrefix$MILESTONE"
        newButton = uiComponents."${subkeyPrefix}newbutton"
        editButton = uiComponents."${subkeyPrefix}editbutton"
        saveButton = uiComponents."${subkeyPrefix}savebutton"
        cancelButton = uiComponents."${subkeyPrefix}cancelbutton"
        taskTree.selectionModel.addListenerForKey(this, 'CompoundTask')
        taskTree.selectionModel.addListenerForKey(this, 'Subtask')
        taskTree.selectionModel.addListenerForKey(this, 'Project')
        taskTree.selectionModel.addRootChangeListener(this)
        // find the top level Vaadin Window
        ui = getVaadinUi(topComponent)
        // build dialog window
        dialog.build()
        // create and initialize state machine
        viewBehavior.initSm(DVState.TOPVIEW)
        viewBehavior.execute(DVEvent.Init)
    }

    void onItemSelected(NodeDto nodeDto) {
        currentItemId = nodeDto.id
        viewBehavior.initItem(currentItemId.second)
        viewBehavior.execute(DVEvent.Select)
    }

    void onRootChanged(NodeDto rootNodeDto) {
        currentTopItemId = rootNodeDto.id
        viewBehavior.execute(DVEvent.Root)
    }

    /**
     * View behavior is implemented using this specialised state machine
     */
    private DetailViewBehavior viewBehavior = new DetailViewBehavior() {
        /** prepare INIT state, not needed now */
        @Override
        protected void initmode() {
            [name, description, completionDate, state, timeBudget, timeUsed,
             scheduledCompletionDate, completionDate, milestoneSelect,
             saveButton, cancelButton, editButton, newButton].each { it.enabled = false }
        }
        /** prepare CREATEEMPTY state, using a dialog window */
        @Override
        protected void createemptymode() {
            dialog.typeSelection.setSelectedItem('Project')
            dialog.typeSelection.enabled = false
            dialog.saveButton.enabled = true
            prepareDialog()
        }
        /** prepare CREATE state, using a dialog window */
        @Override
        protected void createmode() {
            dialog.typeSelection.setSelectedItem(null)
            dialog.typeSelection.enabled = true
            dialog.saveButton.enabled = false
            def mistlist = taskService.getMilestonesInProject(currentItemId.second)
            dialog.milestones = new ListDataProvider<ListItemDto>(mistlist)
            dialog.milestones.sortComparator = { ListItemDto t1, ListItemDto t2 ->
                t1.tag.compareTo(t2.tag)
            }
            prepareDialog()
        }
        /** prepare for editing in EDIT state */
        @Override
        protected void editmode() {
            taskTree.onEditItem()
            if (currentDto.id.first == 'Subtask')
                [timeUsed, completionDate].each { it.enabled = true }
            [name, description, state, timeBudget, scheduledCompletionDate, milestoneSelect,
             saveButton, cancelButton].each { it.enabled = true }
            [editButton, newButton].each { it.enabled = false }
            saveButton.setClickShortcut(ShortcutAction.KeyCode.ENTER)
        }
        /** prepare EMPTY state */
        @Override
        protected void emptymode() {
            clearFields()
            currentDto = null
            [name, description, completionDate, state, timeBudget,
             saveButton, cancelButton, editButton].each { it.enabled = false }
            [newButton].each { it.enabled = true }
        }
        /** prepare SHOW state */
        @Override
        protected void showmode() {
            [name, description, state, timeBudget, timeUsed,
             completionDate, scheduledCompletionDate, milestoneSelect,
             saveButton, cancelButton].each { it.enabled = false }
            [editButton, newButton].each { it.enabled = true }
        }

        /** common functionality for create dialogs */
        private void prepareDialog() {
            taskTree.onEditItem()
            [dialog.name, dialog.timeBudget, dialog.description,
             dialog.scheduledCompletionDate].each { it.clear() }
            dialog.state.deselectAll()
            dialog.milestone.dataProvider = dialog.empty
            dialog.milestone.enabled = false
            dialog.cancelButton.enabled = true
            dialog.saveButton.setClickShortcut(ShortcutAction.KeyCode.ENTER)
            ui.addWindow(dialog.window)
        }
        /** leaving CREATE or CREATEEMPTY state with save */
        protected void onCreateSave() {
            currentDto = createNewItem()
            currentItemId = currentDto.id
            setFieldValues()
            dialog.window.close()
            onEditDone(true)
        }

        /** leaving CREATE or CREATEEMPTY state with cancel */
        protected void onCreateCancel() {
            dialog.window.close()
            onEditDone()
        }

        /** clear all editable fields */
        @Override
        protected void clearFields() {
            state.deselectAll()
            [name, description, timeBudget, timeUsed,
             scheduledCompletionDate, completionDate].each { it.clear() }
        }

        /**
         * Initialize all fields for a new item to display or edit. Given is its id,
         * so its full dto can be addressed and requested by an appropriate service.
         * This method is usually called by the selector component.
         * @param itemId unique identifying key
         */
        @Override
        void initItem(Long itemId) {
            currentDto = taskService.getTaskDetails(itemId)
            setFieldValues()
        }

        /**
         * set all fields from the current full dto object
         */
        @Override
        protected void setFieldValues() {
            name.value = currentDto.args['name'] ?: ''
            description.value = currentDto.args['description'] ?: ''
            state.deselectAll()
            state.select currentDto.args['state'] ?: ''
            timeBudget.value = currentDto.args['timeBudget']?.toString() ?: ''
            timeUsed.value = currentDto.args['timeUsed']?.toString() ?: ''
            milestoneSelect.deselectAll()
            if (currentDto.related.projectmilestones || currentDto.related.unassignedmilestones) {
                def mists = (currentDto.related.projectmilestones ?: []) + currentDto.related.unassignedmilestones ?: []
                def listData = new ListDataProvider<ListItemDto>(mists)
                listData.sortComparator = { ListItemDto t1, ListItemDto t2 ->
                    t1.tag.compareTo(t2.tag)
                }
                milestoneSelect.dataProvider = listData
                List<ListItemDto> relatedMist = currentDto.related.milestone
                if (relatedMist) {
                    def toSelect = mists.find { it.id.second == relatedMist[0].id.second }
                    milestoneSelect.select(toSelect)
                }
            } else {
                milestoneSelect.dataProvider = new ListDataProvider<ListItemDto>([])
            }
            scheduledCompletionDate.value =
                    currentDto.args['scheduledCompletionDate'] ?: LocalDate.of(0, 0, 0)
            completionDate.value =
                    currentDto.args['completionDate'] ?: LocalDate.of(0, 0, 0)

        }

        /**
         * Save current item after editing to persistent storage, typically by calling an appropriate service.
         */
        @Override
        protected void saveItem() {
            FullDto command = new FullDto()
            command.id = currentItemId
            command.tag = name.value
            def args = command.args
            args['name'] = name.value
            args['description'] = description.value
            args['state'] = state.value
            args['timeBudget'] = timeBudget.value
            args['timeUsed'] = timeUsed.value
            args['scheduledCompletionDate'] = scheduledCompletionDate.value
            args['completionDate'] = completionDate.value
            if (currentItemId.first == 'Subtask') {
                def mist = milestoneSelect.selectedItems.find()
                command.related.milestone = mist ? [mist] : []
            }

            currentDto = taskService.createOrUpdateTask(command)
        }

        private FullDto createNewItem() {
            FullDto command = new FullDto()
            def newType = dialog.typeSelection.getSelectedItem()
            if (newType.present) {
                command.id = new Tuple2<String, Long>(newType.get(), 0)
                command.tag = dialog.name.value
                def args = command.args
                args['name'] = dialog.name.value
                args['description'] = dialog.description.value
                args['state'] = dialog.state.value
                args['timeBudget'] = dialog.timeBudget.value
                args['scheduledCompletionDate'] = dialog.scheduledCompletionDate.value
                if (newType.get() != 'Project') {
                    command.related.supertask = [currentDto]
                }
                if (newType.get() == 'Subtask') {
                    def mist = dialog.milestone.selectedItems.find()
                    command.related.milestone = mist ? [mist] : []
                }
                def newNode = taskService.createOrUpdateTask(command)
                newNode
            }
        }

        /**
         * When editing an item was finished [Save] or cancelled [Cancel], notify the selector component
         * to enable it and eventually update the items changed caption
         * @param itemId identifies edited item
         * @param caption eventually updated caption of the edited item
         * @param mustReload Component must reload after new item was created
         *        or (tree-) structure changed
         */
        @Override
        protected void onEditDone(boolean mustReload) {
            taskTree.onEditItemDone(currentItemId, currentDto?.args?.name?.toString() ?: '', mustReload)
        }
    }

    private class NewTaskDialog {

        TextField name, timeBudget
        TextArea description
        ListSelect<String> state
        RadioButtonGroup<String> typeSelection
        DateField scheduledCompletionDate
        ListDataProvider<ListItemDto> milestones, empty = new ListDataProvider<>([])
        private ListSelect<ListItemDto> milestone

        Button saveButton, cancelButton
        Window window

        private VaadinBuilder winBuilder = new VaadinBuilder()

        public Window build() {
            String keyPrefix = "${subkeyPrefix}dialog."
            winBuilder.keyPrefix = keyPrefix
            window = winBuilder."$C.window"('create Task',
                    [spacing : true,
                     margin  : true,
                     width   : '50%',
                     modal   : true,
                     closable: false]) {
                "$C.vlayout"('top', [spacing: true, margin: true]) {
                    "$F.text"('Task', [uikey: NAME])
                    "$F.textarea"('Description', [uikey: DESCRIPTION])
                    "$C.hlayout"([uikey       : 'schedules',
                                  spacing     : true,
                                  gridPosition: [0, 1, 1, 1]]) {
                        "$F.radiobuttongroup"('Type',
                                [uikey            : TYPE,
                                 items            : ['Project',
                                                     'CompoundTask',
                                                     'Subtask'],
                                 selectionListener: {
                                     typeSelectionListener()
                                 }])
                        "$F.list"('State', [uikey: STATE,
                                            items: STATES,
                                            rows : STATES.size()])
                    }
                    "$C.hlayout"([uikey       : 'schedules',
                                  spacing     : true,
                                  gridPosition: [0, 1, 1, 1]]) {
                        "$F.text"('Assigned Time Budget', [uikey: TIME_BUDGET_PLAN])
                        "$F.date"('Scheduled Completion', [uikey: COMPLETION_DATE_PLAN])
                    }
                    "$F.list"('Milestone', [uikey: MILESTONE,
                                            rows : 5,
                                            width: '80%'])
                    "$C.hlayout"([uikey: 'buttonfield', spacing: true]) {
                        "$F.button"('Cancel',
                                [uikey         : 'cancelbutton',
                                 disableOnClick: true, enabled: true,
                                 clickListener : { viewBehavior.execute(DVEvent.Cancel) }])
                        "$F.button"('Save',
                                [uikey         : 'savebutton',
                                 disableOnClick: true, enabled: false,
                                 clickListener : { viewBehavior.execute(DVEvent.Save) }])
                    }
                }
            }

            def dialogComponents = winBuilder.uiComponents
            name = dialogComponents."$keyPrefix$NAME"
            description = dialogComponents."$keyPrefix$DESCRIPTION"
            timeBudget = dialogComponents."$keyPrefix$TIME_BUDGET_PLAN"
            state = dialogComponents."$keyPrefix$STATE"
            scheduledCompletionDate = dialogComponents."$keyPrefix$COMPLETION_DATE_PLAN"
            milestone = dialogComponents."$keyPrefix$MILESTONE"
            typeSelection = dialogComponents."$keyPrefix$TYPE"
            saveButton = dialogComponents."${keyPrefix}savebutton"
            cancelButton = dialogComponents."${keyPrefix}cancelbutton"
            window.center()
            window
        }

        def typeSelectionListener() {
            saveButton.enabled = true
            Optional typeOptional = typeSelection.selectedItem//.get()
            if(typeOptional.present) {
                def type = typeSelection.selectedItem.get()
                if (type == 'Subtask') {
                    milestone.dataProvider = milestones ?: empty
                    milestone.enabled = true
                } else {
                    milestone.dataProvider = empty
                    milestone.enabled = false
                }
            }
        }
    }

}
