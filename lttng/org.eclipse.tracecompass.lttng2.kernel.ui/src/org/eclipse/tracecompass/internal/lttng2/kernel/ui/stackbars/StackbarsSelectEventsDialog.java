package org.eclipse.tracecompass.internal.lttng2.kernel.ui.stackbars;

import java.util.Collections;
import java.util.List;
import java.util.Vector;

import org.eclipse.swt.SWT;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Dialog;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Group;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Table;
import org.eclipse.swt.widgets.TableItem;
import org.eclipse.swt.widgets.Text;
import org.eclipse.tracecompass.internal.lttng2.kernel.core.analysis.graph.model.LttngStrings;
import org.eclipse.tracecompass.internal.lttng2.kernel.ui.stackbars.StackbarsExecutionsDetection.BorderEvents;
import org.eclipse.tracecompass.internal.lttng2.kernel.ui.stackbars.StackbarsExecutionsDetection.EventDefinition;
import org.eclipse.tracecompass.internal.lttng2.kernel.ui.stackbars.StackbarsExecutionsDetection.ExecDefinition;
import org.eclipse.tracecompass.internal.lttng2.kernel.ui.stackbars.StackbarsExecutionsDetection.ExecDefinitionDiffTids;
import org.eclipse.tracecompass.internal.lttng2.kernel.ui.stackbars.StackbarsExecutionsDetection.ExecDefinitionSameTid;

class StackbarsSelectEventsDialog extends Dialog {

    private ExecDefinitionSameTid fExecDefs1Tid;
    private ExecDefinitionDiffTids fExecDefs2Tids;
    private Short fMode;

    private int fCurrentDepth;
    private Label fLabelDepth;
    private boolean fChangesMade;
    private Text fTextDeadline;
    private Text fTextTidStart;
    private Boolean fAddTimeFilter;
    private Text fTextTidEnd;
    private Text fTextSName;
    private Text fTextEParams;
    private Text fTextSParams;
    private Text fTextEName;

    private Text fTextEventName;
    private Text fTextEventParams;
    private Text fTextTids;
    private int fTableIndex;
    private Button bRemove;
    private Button bUp;
    private Button bDown;
    private Table table;

    //0 = StartEventName, 1 = StartEventParams, 2 = EndEventName, 3 = EndEventParams
    private static final String[] PRESET_RUNNING = {LttngStrings.SCHED_WAKEUP + "||" + LttngStrings.SCHED_WAKEUP_NEW,
        LttngStrings.TID + "=$tid", LttngStrings.SCHED_SWITCH, LttngStrings.PREV_TID + "=$tid,prev_state!=0,"};

    private static final String[] PRESET_IRQ = {LttngStrings.IRQ_HANDLER_ENTRY,"", LttngStrings.IRQ_HANDLER_EXIT, ""};

    private static final String[] PRESET_ALL = {StackbarsAnalysis.ALLINONEEXEC,"","",""};

    private static final String[] PRESET_FUTEX = {"syscall_entry_futex","","syscall_exit_futex",""};

    private static final String[] PRESET_CLOCK_NANOSLEEP = {"syscall_exit_clock_nanosleep","","syscall_entry_clock_nanosleep",""};

    private static final String[] PRESET_ALL_PREEMPTION = {"sched_switch","prev_state=0,"+ LttngStrings.PREV_TID +"=$tid",LttngStrings.SCHED_SWITCH,"next_tid=$tid"};

    private static final String[][] PRESETS = {PRESET_ALL, PRESET_RUNNING, PRESET_IRQ,PRESET_FUTEX,PRESET_CLOCK_NANOSLEEP,PRESET_ALL_PREEMPTION};

    public static final int START_DEPTH = 0; //TODO not there
    public static final short ONE_TID = 0;
    public static final short TWO_TIDS = 1;

    public StackbarsSelectEventsDialog(Shell parent)
    {
        super(parent, SWT.DIALOG_TRIM | SWT.APPLICATION_MODAL);
    }

    public BorderEvents getDefaultBorderEvents()
    {
        return getBorderEventsForPreset(PRESET_RUNNING);
    }

    public BorderEvents getBorderEventsForPreset(String[] preset)
    {
        BorderEvents bE = new BorderEvents();
        bE.fEventDefinitions.add(new EventDefinition(preset[0], preset[1]));
        bE.fEventDefinitions.add(new EventDefinition(preset[2], preset[3]));
        bE.fDeadline = "";

        return bE;
    }

    private void loadPreset(String[] preset)
    {
        if(fMode == TWO_TIDS)
        {
            fTextSName.setText(preset[0]);
            fTextSParams.setText(preset[1]);
            fTextEName.setText(preset[2]);
            fTextEParams.setText(preset[3]);
        }
        else
        {
            BorderEvents bE = fExecDefs1Tid.fBorderEventsByDepth.get(fCurrentDepth);
            bE.fEventDefinitions.clear();
            bE.fEventDefinitions.add(new EventDefinition(preset[0], preset[1]));
            bE.fEventDefinitions.add(new EventDefinition(preset[2], preset[3]));
            updateTexts();
        }
    }

    public boolean getIfChangesMadeAndReset()
    {
        boolean temp = fChangesMade;
        fChangesMade = false;
        return temp;
    }

    public boolean getIfTimeFilterAndReset()
    {
        boolean temp = fAddTimeFilter;
        fAddTimeFilter = false;
        return temp;
    }

    public ExecDefinition getExecDef()
    {
        if(fMode == ONE_TID)
        {
            return fExecDefs1Tid;
        }
        return fExecDefs2Tids;
    }

    public void open(ExecDefinition execDef) {
        Shell shell = new Shell(getParent(), getStyle());
        shell.setText(getText());

        fExecDefs1Tid = new ExecDefinitionSameTid();
        fExecDefs2Tids = new ExecDefinitionDiffTids();
        fMode = ONE_TID;
        fCurrentDepth = START_DEPTH;
        fChangesMade = false;
        fAddTimeFilter = false;

        if(execDef instanceof ExecDefinitionSameTid)
        {
            for(int depth = 0; depth < execDef.fBorderEventsByDepth.size(); ++depth)
            {
                fExecDefs1Tid.fBorderEventsByDepth.add(new BorderEvents(execDef.fBorderEventsByDepth.get(depth)));
            }
            fExecDefs1Tid.tids = ((ExecDefinitionSameTid) execDef).tids;
            fExecDefs2Tids.fBorderEventsByDepth.add(getDefaultBorderEvents());
            fMode = ONE_TID;
        }
        else if(execDef instanceof ExecDefinitionDiffTids)
        {
            for(int depth = 0; depth < execDef.fBorderEventsByDepth.size(); ++depth)
            {
                fExecDefs2Tids.fBorderEventsByDepth.add(new BorderEvents(execDef.fBorderEventsByDepth.get(depth)));
            }
            fExecDefs2Tids.tidsStart = ((ExecDefinitionDiffTids) execDef).tidsStart;
            fExecDefs2Tids.tidsEnd = ((ExecDefinitionDiffTids) execDef).tidsEnd;
            fExecDefs1Tid.fBorderEventsByDepth.add(getDefaultBorderEvents());
            fMode = TWO_TIDS;
        }

        if(fExecDefs1Tid.fBorderEventsByDepth.size() == 0)
        {
            fExecDefs1Tid.fBorderEventsByDepth.add(getDefaultBorderEvents());
            fExecDefs2Tids.fBorderEventsByDepth.add(getDefaultBorderEvents());
            fMode = ONE_TID;
        }

        createContents(shell);
        shell.pack();
        shell.open();
        Display display = getParent().getDisplay();
        while (!shell.isDisposed()) {
            if (!display.readAndDispatch()) {
                display.sleep();
            }
        }
    }

    private void createContents(final Shell shell) {

        //Get 1tid

        shell.setText("Events Selection");

        GridLayout gridLayout = new GridLayout(2, true);
        gridLayout.marginHeight = 10;
        gridLayout.marginWidth = 10;
        gridLayout.marginTop = 15;
        gridLayout.marginBottom = 15;
        gridLayout.horizontalSpacing = 10;
        gridLayout.verticalSpacing = 15;
        gridLayout.marginLeft = 15;
        gridLayout.marginRight = 15;

        shell.setLayout(gridLayout);

        // Groups section

        Group executionGroup = new Group(shell, SWT.SHADOW_ETCHED_IN);
        executionGroup.setLayoutData(new GridData(SWT.FILL, SWT.BEGINNING, true, false, 2, 2));
        GridLayout gridExec = new GridLayout(2, true);
        gridExec.horizontalSpacing = 10;
        gridExec.verticalSpacing = 10;
        gridExec.marginLeft = 5;
        gridExec.marginRight = 5;
        executionGroup.setLayout(gridExec);
        executionGroup.setText("Define executions");

        Group deadlineGroup = new Group(shell, SWT.SHADOW_ETCHED_IN);
        deadlineGroup.setLayoutData(new GridData(SWT.FILL, SWT.BEGINNING, true, false, 2, 2));
        deadlineGroup.setLayout(new GridLayout(2, true));
        deadlineGroup.setText("Enter the deadline for this execution (blank for none)");

        Group depthGroup = new Group(shell, SWT.SHADOW_ETCHED_IN);
        depthGroup.setLayoutData(new GridData(SWT.FILL, SWT.BEGINNING, true, false, 2, 2));
        depthGroup.setLayout(new GridLayout(2, true));
        depthGroup.setText("Select the depth to edit (Upper = 0).");

        /*Label label = new Label(shell, SWT.NONE);
        label.setText("Enter the deadline for this execution (blank for none)");
        GridData data = new GridData();
        data.horizontalSpan = 2;
        label.setLayoutData(data);*/

        // Deadline section

        fTextDeadline = new Text(deadlineGroup, SWT.BORDER);
        GridData data = new GridData(SWT.FILL, SWT.FILL,true, true, 2,1);
        fTextDeadline.setLayoutData(data);

        // Executions section

        String[] options = {"Executions start and end on the same tid (support sequence matching)",
                "Executions can start and end on different tids",
                };
        final Combo cExec = new Combo(executionGroup, SWT.READ_ONLY);
        cExec.setItems(options);
        data = new GridData(SWT.FILL, SWT.FILL,true, true, 2,1);
        cExec.setLayoutData(data);

        Button addTimeFilterBtn = new Button(executionGroup, SWT.CHECK);
        addTimeFilterBtn.addSelectionListener(new SelectionAdapter()
        {
            @Override
            public void widgetSelected(SelectionEvent e)
            {
                Button button = (Button) e.widget;
                if (button.getSelection())
                {
                    fAddTimeFilter = true;
                }
                else
                {
                    fAddTimeFilter = false;
                }
            }
        });
        addTimeFilterBtn.setText("Check to use the selected range as time limit");
        data = new GridData(SWT.FILL, SWT.FILL,true, true, 2,1);
        addTimeFilterBtn.setLayoutData(data);

        String[] presetStrings = { "All in one execution", "Running sequences", "IRQ","Futex","CLOCK_NANOSLEEP","All preemptions"};//TODO make the first parameter name
        final Combo c = new Combo(executionGroup, SWT.READ_ONLY);
        c.setItems(presetStrings);
        data = new GridData(SWT.FILL, SWT.FILL,true, true, 1,1);
        c.setLayoutData(data);

        Button loadPreset = new Button(executionGroup, SWT.PUSH);
        loadPreset.setText("Load this preset");
        data = new GridData(SWT.FILL, SWT.FILL,true, true, 1,1);
        loadPreset.setLayoutData(data);
        loadPreset.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent event) {
                loadPreset(PRESETS[c.getSelectionIndex()]);
            }
        });

        Label label = new Label(executionGroup, SWT.SEPARATOR | SWT.HORIZONTAL);
        data = new GridData(SWT.FILL, SWT.FILL,true, true, 2,1);
        label.setLayoutData(data);

        // Option 2

        final Vector<Control> option2TidsControl = new Vector<>();
        final Vector<GridData> option2TidsGridData = new Vector<>();

        label = new Label(executionGroup, SWT.NONE);
        label.setText("Enter the starting tid(s) or process name (blank for current only, separate by coma, * for all)");
        data = new GridData(SWT.FILL, SWT.FILL,true, true, 2,1);
        label.setLayoutData(data);
        option2TidsControl.add(label);
        option2TidsGridData.add(data);

        fTextTidStart = new Text(executionGroup, SWT.BORDER);
        data = new GridData(SWT.FILL, SWT.FILL,true, true, 2,1);
        fTextTidStart.setLayoutData(data);
        option2TidsControl.add(fTextTidStart);
        option2TidsGridData.add(data);

        label = new Label(executionGroup, SWT.NONE);
        label.setText("Enter the ending tid(s) or process name (blank for same, separate by coma, * for all)");
        data = new GridData(SWT.FILL, SWT.FILL,true, true, 2,1);
        label.setLayoutData(data);
        option2TidsControl.add(label);
        option2TidsGridData.add(data);

        fTextTidEnd = new Text(executionGroup, SWT.BORDER);
        data = new GridData(SWT.FILL, SWT.FILL,true, true, 2,1);
        fTextTidEnd.setLayoutData(data);
        option2TidsControl.add(fTextTidEnd);
        option2TidsGridData.add(data);

        label = new Label(executionGroup, SWT.NONE);
        label.setText("Enter start event name");
        data = new GridData(SWT.FILL, SWT.FILL,true, true, 2,1);
        label.setLayoutData(data);
        option2TidsControl.add(label);
        option2TidsGridData.add(data);

        fTextSName = new Text(executionGroup, SWT.BORDER);
        data = new GridData(SWT.FILL, SWT.FILL,true, true, 2,1);
        fTextSName.setLayoutData(data);
        option2TidsControl.add(fTextSName);
        option2TidsGridData.add(data);

        label = new Label(executionGroup, SWT.NONE);
        label.setText("Enter start event params (\"param1=value1, param2=value2\") or blank for none");
        data = new GridData(SWT.FILL, SWT.FILL,true, true, 2,1);
        label.setLayoutData(data);
        option2TidsControl.add(label);
        option2TidsGridData.add(data);

        fTextSParams = new Text(executionGroup, SWT.BORDER);
        data = new GridData(SWT.FILL, SWT.FILL,true, true, 2,1);
        fTextSParams.setLayoutData(data);
        option2TidsControl.add(fTextSParams);
        option2TidsGridData.add(data);

        label = new Label(executionGroup, SWT.NONE);
        label.setText("Enter end event name or blank to use only the start event");
        data = new GridData(SWT.FILL, SWT.FILL,true, true, 2,1);
        label.setLayoutData(data);
        option2TidsControl.add(label);
        option2TidsGridData.add(data);

        fTextEName = new Text(executionGroup, SWT.BORDER);
        data = new GridData(SWT.FILL, SWT.FILL,true, true, 2,1);
        fTextEName.setLayoutData(data);
        option2TidsControl.add(fTextEName);
        option2TidsGridData.add(data);

        label = new Label(executionGroup, SWT.NONE);
        label.setText("Enter end event params (\"param1=value1, param2=value2\") or blank for none");
        data = new GridData(SWT.FILL, SWT.FILL,true, true, 2,1);
        label.setLayoutData(data);
        option2TidsControl.add(label);
        option2TidsGridData.add(data);

        fTextEParams = new Text(executionGroup, SWT.BORDER);
        data = new GridData(SWT.FILL, SWT.FILL,true, true, 2,1);
        fTextEParams.setLayoutData(data);
        option2TidsControl.add(fTextEParams);
        option2TidsGridData.add(data);

        fTextTidStart.addModifyListener(new ModifyListener() {

            @Override
            public void modifyText(ModifyEvent e) {
                fExecDefs2Tids.tidsStart = fTextTidStart.getText();
            }
        });

        fTextTidEnd.addModifyListener(new ModifyListener() {

            @Override
            public void modifyText(ModifyEvent e) {
                fExecDefs2Tids.tidsEnd = fTextTidEnd.getText();
            }
        });

        fTextSName.addModifyListener(new ModifyListener() {

            @Override
            public void modifyText(ModifyEvent e) {
                fExecDefs2Tids.fBorderEventsByDepth.get(fCurrentDepth).getStartEvent().eventName = fTextSName.getText();
            }
        });

        fTextEName.addModifyListener(new ModifyListener() {

            @Override
            public void modifyText(ModifyEvent e) {
                fExecDefs2Tids.fBorderEventsByDepth.get(fCurrentDepth).getEndEvent().eventName = fTextEName.getText();
            }
        });

        fTextSParams.addModifyListener(new ModifyListener() {

            @Override
            public void modifyText(ModifyEvent e) {
                fExecDefs2Tids.fBorderEventsByDepth.get(fCurrentDepth).getStartEvent().eventParams = fTextSParams.getText();
            }
        });

        fTextEParams.addModifyListener(new ModifyListener() {

            @Override
            public void modifyText(ModifyEvent e) {
                fExecDefs2Tids.fBorderEventsByDepth.get(fCurrentDepth).getEndEvent().eventParams = fTextEParams.getText();
            }
        });

        fTextDeadline.addModifyListener(new ModifyListener() {

            @Override
            public void modifyText(ModifyEvent e) {
                if(fMode == ONE_TID)
                {
                    fExecDefs1Tid.fBorderEventsByDepth.get(fCurrentDepth).fDeadline = fTextDeadline.getText();
                }
                else
                {
                    fExecDefs2Tids.fBorderEventsByDepth.get(fCurrentDepth).fDeadline = fTextDeadline.getText();
                }
            }
        });

        // End option 2

        // Option 1

        final Vector<Control> option1TidControl = new Vector<>();
        final Vector<GridData> option1TidGridData = new Vector<>();

        label = new Label(executionGroup, SWT.NONE);
        label.setText("Enter the tid(s) or process name (blank for current only, separate by coma, * for all)");
        data = new GridData();
        data.horizontalSpan = 2;
        label.setLayoutData(data);
        option1TidControl.add(label);
        option1TidGridData.add(data);

        fTextTids = new Text(executionGroup, SWT.BORDER);
        data = new GridData(SWT.FILL, SWT.FILL,true, true, 2,1);
        fTextTids.setLayoutData(data);
        option1TidControl.add(fTextTids);
        option1TidGridData.add(data);

        label = new Label(executionGroup, SWT.SEPARATOR | SWT.HORIZONTAL);
        data = new GridData(SWT.FILL, SWT.FILL,true, true, 2,1);
        label.setLayoutData(data);
        option1TidControl.add(label);
        option1TidGridData.add(data);

        label = new Label(executionGroup, SWT.NONE);
        label.setText("Edit start event name");
        data = new GridData(SWT.FILL, SWT.FILL,true, true, 2,1);
        label.setLayoutData(data);
        option1TidControl.add(label);
        option1TidGridData.add(data);

        fTextEventName = new Text(executionGroup, SWT.BORDER);
        data = new GridData(SWT.FILL, SWT.FILL,true, true, 2,1);
        fTextEventName.setLayoutData(data);
        option1TidControl.add(fTextEventName);
        option1TidGridData.add(data);

        label = new Label(executionGroup, SWT.NONE);
        label.setText("Edit start event params (\"param1=value1, param2=value2\") or blank for none");
        data = new GridData();
        data.horizontalSpan = 2;
        label.setLayoutData(data);
        option1TidControl.add(label);
        option1TidGridData.add(data);

        fTextEventParams = new Text(executionGroup, SWT.BORDER);
        data = new GridData(SWT.FILL, SWT.FILL,true, true, 2,1);
        fTextEventParams.setLayoutData(data);
        option1TidControl.add(fTextEventParams);
        option1TidGridData.add(data);

        table = new Table(executionGroup, SWT.BORDER | SWT.V_SCROLL | SWT.H_SCROLL);
        data = new GridData(SWT.FILL, SWT.FILL,true, true, 1,5);
        table.setLayoutData(data);
        option1TidControl.add(table);
        option1TidGridData.add(data);

        Button bAdd = new Button(executionGroup, SWT.PUSH);
        bAdd.setText("Add new event definition");
        data = new GridData(SWT.FILL, SWT.FILL,true, true, 1,1);
        bAdd.setLayoutData(data);
        option1TidControl.add(bAdd);
        option1TidGridData.add(data);

        bRemove = new Button(executionGroup, SWT.PUSH);
        bRemove.setText("Remove");
        data = new GridData(SWT.FILL, SWT.FILL,true, true, 1,1);
        bRemove.setLayoutData(data);
        option1TidControl.add(bRemove);
        option1TidGridData.add(data);

        bUp = new Button(executionGroup, SWT.PUSH);
        bUp.setText("Up");
        data = new GridData(SWT.FILL, SWT.FILL,true, true, 1,1);
        bUp.setLayoutData(data);
        option1TidControl.add(bUp);
        option1TidGridData.add(data);

        bDown = new Button(executionGroup, SWT.PUSH);
        bDown.setText("Down");
        data = new GridData(SWT.FILL, SWT.FILL,true, true, 1,1);
        bDown.setLayoutData(data);
        option1TidControl.add(bDown);
        option1TidGridData.add(data);

        fTextTids.addModifyListener(new ModifyListener() {

            @Override
            public void modifyText(ModifyEvent e) {
                fExecDefs1Tid.tids = fTextTids.getText();
            }
        });

        fTextEventName.addModifyListener(new ModifyListener() {

            @Override
            public void modifyText(ModifyEvent e) {
                if(fTableIndex != -1)
                {
                    getEDList().get(fTableIndex).eventName = fTextEventName.getText();
                    table.getItem(fTableIndex).setText(fTextEventName.getText());
                }
            }
        });

        fTextEventParams.addModifyListener(new ModifyListener() {
            @Override
            public void modifyText(ModifyEvent e) {
                if(fTableIndex != -1)
                {
                    getEDList().get(fTableIndex).eventParams = fTextEventParams.getText();
                }
            }
        });

        table.addListener(SWT.Selection, new Listener() {
            @Override
            public void handleEvent(Event arg0) {
                TableItem item = ((TableItem)arg0.item);
                fTableIndex = table.indexOf(item);
                bUp.setEnabled(fTableIndex != 0 && fTableIndex != -1);
                bDown.setEnabled(fTableIndex != -1 && fTableIndex != table.getItemCount() - 1);
                bRemove.setEnabled(fTableIndex != -1);
                updateEventTexts();
            }
        });

        bAdd.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent event) {

                getEDList().add(new EventDefinition("EventName", ""));
                TableItem item = new TableItem(table, SWT.NONE);
                item.setText("EventName");
                fTableIndex = table.getItemCount() - 1;
                table.select(fTableIndex);

                // Update buttons
                bRemove.setEnabled(true);
                bUp.setEnabled(fTableIndex != 0);
                bDown.setEnabled(false);
                updateEventTexts();
            }
        });

        bRemove.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent event) {
                if(fTableIndex != -1)
                {
                    if(fTableIndex < getEDList().size())
                    {
                        getEDList().remove(fTableIndex);
                        table.remove(fTableIndex);
                        if(fTableIndex != table.getItemCount())
                        {
                            table.select(fTableIndex);

                            // Update buttons
                            bUp.setEnabled(fTableIndex != 0);
                            bDown.setEnabled(fTableIndex != table.getItemCount() - 1);
                        }
                        else
                        {
                            bRemove.setEnabled(false);
                            bUp.setEnabled(false);
                            bDown.setEnabled(false);
                            fTableIndex = -1;
                        }
                        updateEventTexts();
                    }
                }
            }
        });

        bUp.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent event) {
                if(fTableIndex != -1)
                {
                    if(fTableIndex < getEDList().size())
                    {
                        if(fTableIndex != 0)
                        {
                            moveTableItem(fTableIndex,fTableIndex-1);
                            Collections.swap(getEDList(), fTableIndex, fTableIndex-1);
                            fTableIndex = fTableIndex-1;
                            table.select(fTableIndex);

                            // Update buttons
                            bUp.setEnabled(fTableIndex != 0);
                            bDown.setEnabled(fTableIndex != table.getItemCount() - 1);
                        }
                    }
                }
            }
        });

        bDown.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent event) {
                if(fTableIndex != -1)
                {
                    if(fTableIndex < getEDList().size())
                    {
                        if(fTableIndex != getEDList().size()-1)
                        {
                            moveTableItem(fTableIndex + 1,fTableIndex);
                            Collections.swap(getEDList(), fTableIndex, fTableIndex+1);
                            fTableIndex = fTableIndex+1;
                            table.select(fTableIndex);

                            // Update buttons
                            bUp.setEnabled(fTableIndex != 0);
                            bDown.setEnabled(fTableIndex != table.getItemCount() - 1);
                        }
                    }
                }
            }
        });


        // End option 1

        cExec.addSelectionListener(new SelectionListener() {

            @Override
            public void widgetSelected(SelectionEvent e) {
                selectThings();
            }

            @Override
            public void widgetDefaultSelected(SelectionEvent e) {
                selectThings();
            }

            private void selectThings() {
                if(cExec.getSelectionIndex() == TWO_TIDS)
                {
                    fMode = TWO_TIDS;
                    for(Control c1 : option2TidsControl)
                    {
                        c1.setVisible(true);
                    }
                    for(GridData g1 : option2TidsGridData)
                    {
                        g1.exclude = false;
                    }
                    for(Control c2 : option1TidControl)
                    {
                        c2.setVisible(false);
                    }
                    for(GridData g2 : option1TidGridData)
                    {
                        g2.exclude = true;
                    }
                    shell.layout(true);
                }
                else
                {
                    fMode = ONE_TID;
                    for(Control c1 : option2TidsControl)
                    {
                        c1.setVisible(false);
                    }
                    for(GridData g1 : option2TidsGridData)
                    {
                        g1.exclude = true;
                    }
                    for(Control c2 : option1TidControl)
                    {
                        c2.setVisible(true);
                    }
                    for(GridData g2 : option1TidGridData)
                    {
                        g2.exclude = false;
                    }
                    shell.layout(true);
                }
                fCurrentDepth = 0;
                fLabelDepth.setText("Current = " + Integer.toString(fCurrentDepth) + " Max = " + getMaxDepth());
                updateTexts();
            }

        });

        cExec.select(fMode);
        if(cExec.getSelectionIndex() == TWO_TIDS)
        {
            for(Control c1 : option2TidsControl)
            {
                c1.setVisible(true);
            }
            for(GridData g1 : option2TidsGridData)
            {
                g1.exclude = false;
            }
            for(Control c2 : option1TidControl)
            {
                c2.setVisible(false);
            }
            for(GridData g2 : option1TidGridData)
            {
                g2.exclude = true;
            }
            shell.layout(true);
        }
        else
        {
            for(Control c1 : option2TidsControl)
            {
                c1.setVisible(false);
            }
            for(GridData g1 : option2TidsGridData)
            {
                g1.exclude = true;
            }
            for(Control c2 : option1TidControl)
            {
                c2.setVisible(true);
            }
            for(GridData g2 : option1TidGridData)
            {
                g2.exclude = false;
            }
            shell.layout(true);
        }

        /*label = new Label(shell, SWT.NONE);
        label.setText("Enter the tid(s) for end event (blank for end event to be on the same thread than the corresponding start event)");
        data = new GridData();
        data.horizontalSpan = 2;
        label.setLayoutData(data);

        fTextTidEnd = new Text(shell, SWT.BORDER);
        data = new GridData(GridData.FILL_HORIZONTAL);
        data.horizontalSpan = 2;
        fTextTidEnd.setLayoutData(data);*/

        // Depth section

        fLabelDepth = new Label(depthGroup, SWT.NONE);
        data = new GridData(SWT.FILL, SWT.FILL,true, true, 1,1);
        fLabelDepth.setLayoutData(data);

        final Button removeLastDepth = new Button(depthGroup, SWT.PUSH);
        removeLastDepth.setText("Remove last depth definition");
        data = new GridData(SWT.FILL, SWT.FILL,true, true, 1,1);
        removeLastDepth.setLayoutData(data);

        final Button increaseDepth = new Button(depthGroup, SWT.PUSH);
        increaseDepth.setText("Increase current depth to edit");
        data = new GridData(SWT.FILL, SWT.FILL,true, true, 1,1);
        increaseDepth.setLayoutData(data);

        final Button decreaseDepth = new Button(depthGroup, SWT.PUSH);
        decreaseDepth.setText("Decrease current depth to edit");
        data = new GridData(SWT.FILL, SWT.FILL,true, true, 1,1);
        decreaseDepth.setLayoutData(data);

        removeLastDepth.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent event) {

                if(fCurrentDepth == getMaxDepth())
                {
                    if(fCurrentDepth > 0)
                    {
                        --fCurrentDepth;
                        updateTexts();
                    }
                    if(fCurrentDepth == 0)
                    {
                        decreaseDepth.setEnabled(false);
                    }
                }

                removeLastDepth();

                if(getMaxDepth() == 0)
                {
                    removeLastDepth.setEnabled(false);
                }
            }
        });
        if(getMaxDepth() == 0)
        {
            removeLastDepth.setEnabled(false);
        }

        decreaseDepth.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent event) {

                if(fCurrentDepth > 0)
                {
                    --fCurrentDepth;
                    updateTexts();
                }
                if(fCurrentDepth == 0)
                {
                    decreaseDepth.setEnabled(false);
                }
                if(getMaxDepth() == 0)
                {
                    removeLastDepth.setEnabled(false);
                }
            }
        });
        decreaseDepth.setEnabled(false);

        increaseDepth.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent event) {
                increaseDepth();
                updateTexts();
                decreaseDepth.setEnabled(true);
                removeLastDepth.setEnabled(true);
            }
        });

        // End depth section

        // Buttons

        Button ok = new Button(shell, SWT.PUSH);
        ok.setText("OK");
        data = new GridData(GridData.FILL_HORIZONTAL);
        ok.setLayoutData(data);
        ok.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent event) {
                fChangesMade = true;
                shell.close();
            }
        });

        Button cancel = new Button(shell, SWT.PUSH);
        cancel.setText("Cancel");
        data = new GridData(GridData.FILL_HORIZONTAL);
        cancel.setLayoutData(data);
        cancel.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent event) {
                shell.close();
            }
        });

        shell.setDefaultButton(ok);
        updateTexts();
    }

    protected void removeLastDepth() {
        if(fMode == ONE_TID)
        {
            fExecDefs1Tid.fBorderEventsByDepth.remove(fExecDefs1Tid.fBorderEventsByDepth.size() - 1);
        }
        else
        {
            fExecDefs2Tids.fBorderEventsByDepth.remove(fExecDefs2Tids.fBorderEventsByDepth.size() - 1);
        }
        fLabelDepth.setText("Current = " + Integer.toString(fCurrentDepth) + " Max = " + getMaxDepth());
    }

    protected void increaseDepth() {

        if(getMaxDepth() == fCurrentDepth)
        {
            if(fMode == ONE_TID)
            {
                fExecDefs1Tid.fBorderEventsByDepth.add(getDefaultBorderEvents());
            }
            else
            {
                fExecDefs2Tids.fBorderEventsByDepth.add(getDefaultBorderEvents());
            }
        }
        ++fCurrentDepth;
    }

    private int getMaxDepth() {
        if(fMode == ONE_TID)
        {
            return fExecDefs1Tid.fBorderEventsByDepth.size() - 1;
        }
        return fExecDefs2Tids.fBorderEventsByDepth.size() -1;
    }

    protected void updateEventTexts() {

        if(fTableIndex != -1)
        {
            fTextEventName.setEnabled(true);
            fTextEventParams.setEnabled(true);
            fTextEventName.setText(getEDList().get(fTableIndex).eventName);
            fTextEventParams.setText(getEDList().get(fTableIndex).eventParams);
        }
        else
        {
            fTextEventName.setEnabled(false);
            fTextEventParams.setEnabled(false);
            fTextEventName.setText("");
            fTextEventParams.setText("");
        }
    }

    public void moveTableItem(int from, int to) {
        TableItem item2Move = table.getItem(from);
        TableItem newTableItem = new TableItem(table, SWT.NONE, to);
        newTableItem.setText(item2Move.getText());
        item2Move.dispose();
    }

    private void updateTexts()
    {
        if(fMode == ONE_TID)
        {
            fTextDeadline.setText(fExecDefs1Tid.fBorderEventsByDepth.get(fCurrentDepth).fDeadline);
            // Update buttons
            table.setRedraw(false);
            table.removeAll();
            for (EventDefinition def : fExecDefs1Tid.fBorderEventsByDepth.get(fCurrentDepth).fEventDefinitions) {
                TableItem item = new TableItem(table, SWT.NONE);
                item.setText(def.eventName);
              }
            table.setRedraw(true);
            fTableIndex = table.getItemCount() == 0 ? -1 : 0;
            bUp.setEnabled(fTableIndex != 0 && fTableIndex != -1);
            bDown.setEnabled(fTableIndex != -1 && fTableIndex != table.getItemCount() - 1);
            bRemove.setEnabled(fTableIndex != -1);
            updateEventTexts();
        }
        else
        {
            fTextDeadline.setText(fExecDefs2Tids.fBorderEventsByDepth.get(fCurrentDepth).fDeadline);
            fTextTidStart.setText(fExecDefs2Tids.tidsStart);
            fTextTidEnd.setText(fExecDefs2Tids.tidsEnd);
            fTextSName.setText(fExecDefs2Tids.fBorderEventsByDepth.get(fCurrentDepth).getStartEvent().eventName);
            fTextEName.setText(fExecDefs2Tids.fBorderEventsByDepth.get(fCurrentDepth).getEndEvent().eventName);
            fTextSParams.setText(fExecDefs2Tids.fBorderEventsByDepth.get(fCurrentDepth).getStartEvent().eventParams);
            fTextEParams.setText(fExecDefs2Tids.fBorderEventsByDepth.get(fCurrentDepth).getEndEvent().eventParams);
        }

        fLabelDepth.setText("Current = " + Integer.toString(fCurrentDepth) + " Max = " + getMaxDepth());

    }

    private List<EventDefinition> getEDList()
    {
        if(fMode == ONE_TID)
        {
            return fExecDefs1Tid.fBorderEventsByDepth.get(fCurrentDepth).fEventDefinitions;
        }
        return fExecDefs2Tids.fBorderEventsByDepth.get(fCurrentDepth).fEventDefinitions;
    }
}