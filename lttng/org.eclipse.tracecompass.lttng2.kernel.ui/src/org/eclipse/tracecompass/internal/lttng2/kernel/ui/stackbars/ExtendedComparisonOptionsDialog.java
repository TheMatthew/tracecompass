package org.eclipse.tracecompass.internal.lttng2.kernel.ui.stackbars

import java.util.HashSet;

import org.eclipse.linuxtools.tmf.core.timestamp.ITmfTimestamp;
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

class ExtendedComparisonOptionsDialog extends Dialog {

  public static final int BLANK = -1;
  private int fTid;
  private long fStartTime;
  private long fEndTime;

  public ExtendedComparisonOptionsDialog(Shell parent)
  {
      super(parent, SWT.DIALOG_TRIM | SWT.APPLICATION_MODAL);
      fTid = BLANK;
  }

  public int getTid() {
      return fTid;
  }

  public long getStartTime()
  {
      return fStartTime;
  }

  public long getEndTime()
  {
      return fEndTime;
  }

  public void open(HashSet<Integer> typesChecked) {
    Shell shell = new Shell(getParent(), getStyle());
    shell.setText(getText());
    createContents(shell, typesChecked);
    shell.pack();
    shell.open();
    Display display = getParent().getDisplay();
    while (!shell.isDisposed()) {
      if (!display.readAndDispatch()) {
        display.sleep();
      }
    }
  }

  private void createContents(final Shell shell, final HashSet<Integer> typesChecked) {

    shell.setText("Display options");
    shell.setMinimumSize(350, 450);

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

    // Create the document type group
    Group typesGroup = new Group(shell, SWT.SHADOW_ETCHED_IN);
    typesGroup.setLayoutData(new GridData(SWT.FILL, SWT.BEGINNING, true, false, 2, 2));
    typesGroup.setLayout(new GridLayout(2, false));
    typesGroup.setText("Select types");

    final Table table = new Table(typesGroup, SWT.CHECK | SWT.BORDER | SWT.V_SCROLL | SWT.H_SCROLL);
    ExtendedEntry.Type[] types = ExtendedEntry.Type.values();
    for (int i = 0; i < types.length; ++i) {
      TableItem item = new TableItem(table, SWT.NONE);
      item.setText(types[i].name);
      if(typesChecked.contains(types[i].ordinal()))
      {
          item.setChecked(true);
      }
    }

    table.addListener(SWT.Selection, new Listener() {
        @Override
        public void handleEvent(Event arg0) {
            TableItem item = ((TableItem)arg0.item);
            if (item.getChecked())
            {
                typesChecked.add(table.indexOf(item));
            }
            else
            {
                typesChecked.remove(table.indexOf(item));
            }
        }
    });

    GridData data = new GridData();
    data.horizontalSpan = 2;
    table.setLayoutData(data);

    Group parametersGroup = new Group(shell, SWT.SHADOW_ETCHED_IN);
    parametersGroup.setLayoutData(new GridData(SWT.FILL, SWT.BEGINNING, true, false, 2, 2));
    parametersGroup.setLayout(new GridLayout(2, false));
    parametersGroup.setText("Select parameters");


    //Tid

    Label label = new Label(parametersGroup, SWT.NONE);
    label.setText("Tid(s) (separated by a comma)");
    data = new GridData();
    data.horizontalSpan = 2;
    label.setLayoutData(data);

    final Text textTid = new Text(parametersGroup, SWT.BORDER);
    data = new GridData(GridData.FILL_HORIZONTAL);
    data.horizontalSpan = 2;
    textTid.setLayoutData(data);

    //Start time

    label = new Label(parametersGroup, SWT.NONE);
    label.setText("Start time");
    data = new GridData();
    data.horizontalSpan = 2;
    label.setLayoutData(data);

    final Text textStartTime = new Text(parametersGroup, SWT.BORDER);
    data = new GridData(GridData.FILL_HORIZONTAL);
    data.horizontalSpan = 2;
    textStartTime.setLayoutData(data);

    //End time

    label = new Label(parametersGroup, SWT.NONE);
    label.setText("End time");
    data = new GridData();
    data.horizontalSpan = 2;
    label.setLayoutData(data);

    final Text textEndTime = new Text(parametersGroup, SWT.BORDER);
    data = new GridData(GridData.FILL_HORIZONTAL);
    data.horizontalSpan = 2;
    textEndTime.setLayoutData(data);

    // Load button

    Button loadTimeRange = new Button(parametersGroup, SWT.PUSH);
    loadTimeRange.setText("Load the current time range");
    data = new GridData(GridData.FILL_HORIZONTAL);
    loadTimeRange.setLayoutData(data);
    loadTimeRange.addSelectionListener(new SelectionAdapter() {
        @Override
        public void widgetSelected(SelectionEvent event) {

            ITmfTimestamp start = StackbarsAnalysis.getInstance().getStartTime();
            ITmfTimestamp end = StackbarsAnalysis.getInstance().getEndTime();

            if(start != null)
            {
                textStartTime.setText(Long.toString(start.getValue()));
            }

            if(end != null)
            {
                textEndTime.setText(Long.toString(end.getValue()));
            }
        }
    });

    //button cancel

    Button cancel = new Button(shell, SWT.PUSH);
    cancel.setText("Cancel");
    data = new GridData(GridData.FILL_HORIZONTAL);
    data.horizontalSpan = 1;
    cancel.setLayoutData(data);
    cancel.addSelectionListener(new SelectionAdapter() {
      @Override
    public void widgetSelected(SelectionEvent event) {
        shell.close();
      }
    });

    // button change parameters

    Button changeParameters = new Button(shell, SWT.PUSH);
    changeParameters.setText("OK");
    data = new GridData(GridData.FILL_HORIZONTAL);
    data.horizontalSpan = 1;
    changeParameters.setLayoutData(data);
    changeParameters.addSelectionListener(new SelectionAdapter() {
      @Override
    public void widgetSelected(SelectionEvent event) {

          parseParameters(textTid.getText(), textStartTime.getText(),
                  textEndTime.getText());
          shell.close();
      }
    });

    shell.setDefaultButton(changeParameters);

  }

  private void parseParameters(String textTid, String textStartTime, String textEndTime)
  {
      try{
          fTid = Integer.parseInt(textTid);
      }
      catch (NumberFormatException e)
      {
          fTid = BLANK;
      }

      try{
          fStartTime = Long.parseLong(textStartTime);
      }
      catch (NumberFormatException e)
      {
          fStartTime = BLANK;
      }

      try{
          fEndTime = Long.parseLong(textEndTime);
      }
      catch (NumberFormatException e)
      {
          fEndTime = BLANK;
      }
  }

}