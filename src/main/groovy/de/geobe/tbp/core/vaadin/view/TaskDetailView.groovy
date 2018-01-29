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
import com.vaadin.ui.*
import com.vaadin.ui.themes.ValoTheme
import de.geobe.tbp.core.domain.TaskState
import de.geobe.tbp.core.dto.FullDto
import de.geobe.tbp.core.dto.NodeDto
import de.geobe.tbp.core.service.TaskService
import de.geobe.util.vaadin.builder.VaadinBuilder
import de.geobe.util.vaadin.type.VaadinSelectionListener
import de.geobe.util.vaadin.type.VaadinTreeRootChangeListener
import de.geobe.util.vaadin.view.DVEvent
import de.geobe.util.vaadin.view.DVState
import de.geobe.util.vaadin.view.DetailViewBase
import org.springframework.beans.factory.annotation.Autowired

import java.sql.Timestamp
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
class TaskDetailView extends DetailViewBase
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

    /** variables bound to the generated vaadin elements make programming easier */
    private TextField name, timeBudget, timeUsed
    private TextArea description
    private ListSelect<String> state
    private DateField scheduledCompletionDate, completionDate
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
            "$F.text"('Assigned Time Budget', [uikey: TIME_BUDGET_PLAN])
            "$F.text"('Used Time Budget', [uikey  : TIME_BUDGET_USED,
                                           enabled: false])
            "$F.date"('Scheduled Completion', [uikey: COMPLETION_DATE_PLAN])
            "$F.date"('Completion achieved', [uikey  : COMPLETION_DATE_DONE,
                                              enabled: false])
            "$C.hlayout"([uikey       : 'buttonfield', spacing: true,
                          gridPosition: [0, 3, 1, 3]]) {
                "$F.button"('New',
                        [uikey         : 'newbutton',
                         disableOnClick: true,
                         clickListener : { sm.execute(DVEvent.Create) }])
                "$F.button"('Edit',
                        [uikey         : 'editbutton',
                         disableOnClick: true,
                         clickListener : { sm.execute(DVEvent.Edit) }])
                "$F.button"('Cancel',
                        [uikey         : 'cancelbutton',
                         disableOnClick: true,
                         enabled       : false,
                         clickListener : { sm.execute(DVEvent.Cancel) }])
                "$F.button"('Save',
                        [uikey         : 'savebutton',
                         disableOnClick: true, enabled: false,
                         clickShortcut : ShortcutAction.KeyCode.ENTER,
                         styleName     : ValoTheme.BUTTON_PRIMARY,
                         clickListener : { sm.execute(DVEvent.Save) }])
            }
        }
        topComponent
    }

    @Override
    void init(Object... value) {
        detailSelector = taskTree
        uiComponents = vaadin.uiComponents
        name = uiComponents."$subkeyPrefix$NAME"
        description = uiComponents."$subkeyPrefix$DESCRIPTION"
        state = uiComponents."$subkeyPrefix$STATE"
        timeBudget = uiComponents."$subkeyPrefix$TIME_BUDGET_PLAN"
        timeUsed = uiComponents."$subkeyPrefix$TIME_BUDGET_USED"
        scheduledCompletionDate = uiComponents."$subkeyPrefix$COMPLETION_DATE_PLAN"
        completionDate = uiComponents."$subkeyPrefix$COMPLETION_DATE_DONE"
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
        initSm(DVState.INIT)
        sm.execute(DVEvent.Init)
    }

    void onItemSelected(NodeDto nodeDto) {
        currentItemId = nodeDto.id
        initItem(currentItemId.second)
        sm.execute(DVEvent.Select)
    }

    void onRootChanged(NodeDto rootNodeDto) {
        currentTopItemId = rootNodeDto.id
        sm.execute(DVEvent.Root)
    }

    @Override
    protected getCurrentItemId() { currentItemId }

    @Override
    protected getCurrentDomainId() { currentItemId }

    @Override
    protected String getCurrentCaption() { currentDto.tag }

    @Override
    protected getMatchForNewItem() { [type: ProjectTree.TASK_TYPE, id: currentDto.id] }

    /** prepare INIT state */
    @Override
    protected initmode() {
        [name, description, completionDate, saveButton, cancelButton, editButton, newButton].each { it.enabled = false }
    }
    /** prepare EMPTY state */
    @Override
    protected emptymode() {
        clearFields()
        currentDto = null
        [name, description, completionDate, saveButton, cancelButton, editButton].each { it.enabled = false }
        [newButton].each { it.enabled = true }
    }
    /** prepare SHOW state */
    @Override
    protected showmode() {
        [name, description, completionDate, saveButton, cancelButton].each { it.enabled = false }
        [editButton, newButton].each { it.enabled = true }
    }

    @Override
    protected createemptymode() {
        createmode()
    }

    @Override
    protected createmode() {
        taskTree.onEditItem()
        [name, description, completionDate, saveButton, cancelButton].
                each { it.enabled = true }
        [editButton, newButton].each { it.enabled = false }
//        saveButton.setClickShortcut(ShortcutAction.KeyCode.ENTER)
    }
    /** prepare for editing in EDIT states */
    @Override
    protected editmode() {
        taskTree.onEditItem()
        if (currentDto.classname == 'Subtask')
            completionDate.enabled = true
        [name, description, saveButton, cancelButton].each { it.enabled = true }
        [editButton, newButton].each { it.enabled = false }
//        saveButton.setClickShortcut(ShortcutAction.KeyCode.ENTER)
    }
    /** prepare for working in DIALOG state */
    protected dialogmode() {
        taskTree.onEditItem()
        [dialog.name, dialog.timeBudget, dialog.spent, dialog.description,
         dialog.completed, dialog.supertask].each { it.clear() }
        [dialog.saveButton, dialog.cancelButton].each { it.enabled = true }
        ui.addWindow(dialog.window)
    }
    /** leaving DIALOG state with save */
    protected saveDialog() {
        createSubtask()
        dialog.window.close()
        taskTree.onEditItemDone(currentItemId, currentCaption, true)
    }
    /** leaving DIALOG state with cancel */
    protected cancelDialog() {
        dialog.window.close()
        taskTree.onEditItemDone(currentItemId, currentCaption)
    }
    /** clear all editable fields */
    @Override
    protected clearFields() {
        [name, description, completionDate].each { it.clear() }
    }
    /**
     * for the given persistent object id, fetch the full dto and save it in field currentDto
     * @param itemId object id
     */
    @Override
    protected void initItem(Long taskId) {
        currentDto = taskService.getTaskDetails(taskId)
        setFieldValues()
    }
    /**
     * set all fields from the current full dto object
     */
    @Override
    protected void setFieldValues() {
        name.value = currentDto.args['name'] ?: ''
        description.value = currentDto.args['description'] ?: ''
        state.select currentDto.args['state'] ?: ''
        timeBudget.value = currentDto.args['timeBudget'] ?: ''
        timeUsed.value = currentDto.args['timeUsed'] ?: ''
        scheduledCompletionDate.value =
                currentDto.args['scheduledCompletionDate'] ?: LocalDate.of(0, 0, 0)
        completionDate.value =
                currentDto.args['completionDate'] ?: LocalDate.of(0, 0, 0)
    }
    /**
     * create or update a domain object from the current field values and
     * update the current dto from the saved domain object
     *
     * @param id domain id of domain object or 0 (zero) to create a new
     * @return updated current dto
     */
    @Override
    protected saveItem(Tuple2<String, Serializable> id = new Tuple2<>('', 0)) {
        FullDto command = new FullDto()
        command.id = id
        command.tag = name.value
        def args = command.args
        args['name'] = name.value
        args['description'] = description.value
        args['state'] = state.value
        args['timeBudget'] = timeBudget.value
//        args['timeUsed'] = timeUsed.value
//        args['scheduledCompletionDate'] = scheduledCompletionDate.value
//        args['completionDate'] = completionDate.value
//        // determine level for a new item
//        if (id.second == 0) {
//            if (!currentDto || currentDto.related.all) {
//                // we are on top level of tasks
//                command.projectId = (Long) currentTopItemId['id']
//            } else {
//                // we are on a lower level
//                command.supertaskId = currentDto.supertask.firstId
//            }
//        }
        currentDto = taskService.createOrUpdateTask(command)
    }

    def createSubtask() {
        FullDto command = new FullDto()
        command.id = new Tuple2<String, Long>('Subtask', 0)
        command.tag = dialog.name.value
        def args = command.args
        args['name'] = dialog.name.value
        args['description'] = dialog.description.value
        args['state'] = dialog.state.value
        args['timeBudget'] = dialog.timeBudget.value
        def newNode = taskService.createOrUpdateTask(command)
        newNode
    }

    private class NewTaskDialog {

        TextField name, timeBudget
        TextArea description
        ListSelect<String> state
        RadioButtonGroup radiobuttongroup
        DateField scheduledCompletionDate

        Button saveButton, cancelButton
        Window window

        private VaadinBuilder winBuilder = new VaadinBuilder()

        public Window build() {
            String keyPrefix = "${subkeyPrefix}dialog."
            winBuilder.keyPrefix = keyPrefix
            window = winBuilder."$C.window"('create Task',
                    [spacing: true, margin: true,
                     modal  : true, closable: false]) {
                "$C.vlayout"('top', [spacing: true, margin: true]) {
                    "$F.text"('Task', [uikey: NAME])
                    "$F.radiobuttongroup"('Type', [uikey: TYPE,
                                                   items: ['Project', 'CompoundTask', 'Subtask']])
                    "$F.textarea"('Description', [uikey: DESCRIPTION])
                    "$F.list"('State', [uikey: STATE,
                                        items: STATES,
                                        rows : STATES.size()])
                    "$F.text"('Assigned Time Budget', [uikey: TIME_BUDGET_PLAN])
                    "$F.date"('Scheduled Completion', [uikey: COMPLETION_DATE_PLAN])
                    "$C.hlayout"([uikey: 'buttonfield', spacing: true]) {
                        "$F.button"('Cancel',
                                [uikey         : 'cancelbutton',
                                 disableOnClick: true, enabled: true,
                                 clickListener : { sm.execute(DVEvent.Cancel) }])
                        "$F.button"('Save',
                                [uikey         : 'savebutton',
                                 disableOnClick: true, enabled: true,
                                 clickListener : { sm.execute(DVEvent.Save) }])
                    }
                }
            }

            def dialogComponents = winBuilder.uiComponents
            name = dialogComponents."$keyPrefix$NAME"
            description = dialogComponents."$keyPrefix$DESCRIPTION"
            timeBudget = dialogComponents."$keyPrefix$TIME_BUDGET_PLAN"
            state = dialogComponents."$keyPrefix$STATE"
            scheduledCompletionDate = dialogComponents."$keyPrefix$COMPLETION_DATE_PLAN"
            radiobuttongroup = dialogComponents."$keyPrefix$TYPE"
            saveButton = dialogComponents."${keyPrefix}savebutton"
            cancelButton = dialogComponents."${keyPrefix}cancelbutton"
            window.center()
        }
    }

}
