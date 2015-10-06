package org.eclipse.linuxtools.internal.lttng2.kernel.ui.stackbars;

import java.util.ArrayList;
import java.util.Observable;
import java.util.Observer;

import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
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

class PatternDialog extends Dialog{

    private Table fTableResults;
    private Table fTableLegend;
    private boolean fSchedSwitchMode;
    private Text fTextSupportRatio;
    private Text fTextMaxEvents;
    private Label fCurrentPattern;
    private ArrayList<String> fCurrentPatternS;
    private boolean fLoad;
    private ArrayList<int[]> fResults;
    private String[] fDictio;
    private PatternDialogObservable pdo = new PatternDialogObservable();
    private boolean fOpen;

    private class PatternDialogObservable extends Observable
    {
        public void notifyChange()
        {
            setChanged();
            notifyObservers();
        }

    }

    public PatternDialog(Shell parent, Observer obr)
    {
        super(parent, SWT.DIALOG_TRIM | SWT.APPLICATION_MODAL);
        fSchedSwitchMode = true;
        pdo.addObserver(obr);
        fCurrentPatternS = new ArrayList<>();
    }

    public void open() {
        fLoad = false;
        Shell shell = new Shell(getParent(), getStyle());
        shell.setText(getText());

        createContents(shell);
        shell.pack();
        shell.open();
        fOpen = true;
        Display display = getParent().getDisplay();
        while (!shell.isDisposed()) {
            if (!display.readAndDispatch()) {
                display.sleep();
            }
        }
    }

    public boolean getIfLoadNeeded()
    {
        return fLoad;
    }

    public ArrayList<String> getPattern()
    {
        return fCurrentPatternS;

    }

    public boolean getIfSchedSwitchMode()
    {
        return fSchedSwitchMode;
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

        Group searchGroup = new Group(shell, SWT.SHADOW_ETCHED_IN);
        searchGroup.setLayoutData(new GridData(SWT.FILL, SWT.BEGINNING, true, false, 2, 2));
        GridLayout gridGroup = new GridLayout(2, true);
        gridGroup.horizontalSpacing = 10;
        gridGroup.verticalSpacing = 10;
        gridGroup.marginLeft = 5;
        gridGroup.marginRight = 5;
        searchGroup.setLayout(gridGroup);
        searchGroup.setText("Search options");

        Group resultGroup = new Group(shell, SWT.SHADOW_ETCHED_IN);
        resultGroup.setLayoutData(new GridData(SWT.FILL, SWT.BEGINNING, true, false, 2, 2));
        resultGroup.setLayout(gridGroup);
        resultGroup.setText("Results section");

        // Search section

        Label label = new Label(searchGroup, SWT.NONE);
        label.setText("Enter support ratio (blank for automatic)");
        GridData data = new GridData(SWT.FILL, SWT.FILL,true, true, 1,1);
        label.setLayoutData(data);

        fTextSupportRatio = new Text(searchGroup, SWT.BORDER);
        data = new GridData(SWT.FILL, SWT.FILL,true, true, 1,1);
        fTextSupportRatio.setLayoutData(data);

        label = new Label(searchGroup, SWT.NONE);
        label.setText("Max events to include in calcul (blank for 10000)");
        data = new GridData(SWT.FILL, SWT.FILL,true, true, 1,1);
        label.setLayoutData(data);

        fTextMaxEvents = new Text(searchGroup, SWT.BORDER);
        data = new GridData(SWT.FILL, SWT.FILL,true, true, 1,1);
        fTextMaxEvents.setLayoutData(data);

        Button sched_switch_modeBtn = new Button(searchGroup, SWT.CHECK);
        sched_switch_modeBtn.addSelectionListener(new SelectionAdapter()
        {
            @Override
            public void widgetSelected(SelectionEvent e)
            {
                Button button = (Button) e.widget;
                if (button.getSelection())
                {
                    fSchedSwitchMode = true;
                }
                else
                {
                    fSchedSwitchMode = false;
                }
            }
        });
        sched_switch_modeBtn.setText("Pattern must start and end with a sched_switch");
        data = new GridData(SWT.FILL, SWT.FILL,true, true, 1,1);
        sched_switch_modeBtn.setLayoutData(data);
        sched_switch_modeBtn.setSelection(fSchedSwitchMode);

        Button search = new Button(searchGroup, SWT.PUSH);
        search.setText("Search for patterns");
        data = new GridData(SWT.FILL, SWT.FILL,true, true, 1,1);
        search.setLayoutData(data);
        search.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent event) {
                //TODO
                pdo.notifyChange();
            }
        });

        // Results

        fTableResults = new Table(resultGroup, SWT.BORDER | SWT.V_SCROLL | SWT.H_SCROLL);
        data = new GridData(SWT.FILL, SWT.FILL,true, true, 1,5);
        fTableResults.setLayoutData(data);
        fTableResults.addListener(SWT.Selection, new Listener() {
            @Override
            public void handleEvent(Event arg0) {
                TableItem item = ((TableItem)arg0.item);
                int tableIndex = fTableResults.indexOf(item);
                updatePatternText(tableIndex);
            }
        });

        fTableLegend = new Table(resultGroup, SWT.BORDER | SWT.V_SCROLL | SWT.H_SCROLL);
        data = new GridData(SWT.FILL, SWT.FILL,true, true, 1,5);
        fTableLegend.setLayoutData(data);

        fCurrentPattern = new Label(resultGroup, SWT.NONE);
        fCurrentPattern.setText("Current Pattern : none");
        data = new GridData(SWT.FILL, SWT.FILL,true, true, 2,1);
        fCurrentPattern.setLayoutData(data);

        // Buttons

        Button load = new Button(resultGroup, SWT.PUSH);
        load.setText("Load this pattern");
        data = new GridData(GridData.FILL_HORIZONTAL);
        load.setLayoutData(data);
        load.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent event) {
                fLoad = true;
                fOpen = false;
                shell.close();
            }
        });

        Button cancel = new Button(resultGroup, SWT.PUSH);
        cancel.setText("Close window");
        data = new GridData(GridData.FILL_HORIZONTAL);
        cancel.setLayoutData(data);
        cancel.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent event) {
                fOpen = false;
                shell.close();
            }
        });

        shell.setDefaultButton(load);
    }

    protected void updatePatternText(int tableIndex) {
        if(fResults != null)
        {
            fCurrentPatternS.clear();
            String pattern = "Current pattern : ";
            int maxElemIndex = fResults.get(tableIndex).length - 1;
            for(int i = 0; i < maxElemIndex; ++i)
            {
                String nextElem = fDictio[fResults.get(tableIndex)[i]];
                pattern += nextElem + " | ";
                fCurrentPatternS.add(nextElem);
            }
            String nextElem = fDictio[fResults.get(tableIndex)[maxElemIndex]];
            pattern += nextElem;
            fCurrentPatternS.add(nextElem);
            fCurrentPattern.setText(pattern);
        }
    }

    public void updateResults(ArrayList<int[]> results, String[] inverseDictionary)
    {
        if(!fOpen)
        {
            return;
        }
        fResults = results;
        fTableResults.setRedraw(false);
        fTableResults.removeAll();
        for (int[] res : results) {
            if(res.length != 0)
            {
                TableItem item = new TableItem(fTableResults, SWT.NONE);
                String resS = "";
                int maxElemIndex = res.length - 1;
                for(int i = 0; i < maxElemIndex; ++i)
                {
                    resS = resS + res[i] + " - ";

                }
                resS += res[maxElemIndex];
                item.setText(resS);
            }
        }
        fTableResults.setRedraw(true);

        fDictio = inverseDictionary;
        fTableLegend.setRedraw(false);
        fTableLegend.removeAll();
        for (int i = 0; i < inverseDictionary.length; ++i) {
            TableItem item = new TableItem(fTableLegend, SWT.NONE);
            item.setText(i + " : " + inverseDictionary[i]);
          }
        fTableLegend.setRedraw(true);
    }
}