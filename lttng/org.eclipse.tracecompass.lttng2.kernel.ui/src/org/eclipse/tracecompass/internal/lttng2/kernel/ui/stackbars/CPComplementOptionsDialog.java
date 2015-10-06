package org.eclipse.tracecompass.internal.lttng2.kernel.ui.stackbars;

import java.util.Vector;

import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Dialog;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Text;
import org.eclipse.tracecompass.tmf.core.timestamp.ITmfTimestamp;

class CPComplementOptionsDialog extends Dialog {

  private int fRelatedOptionIndex;
  private int fRunningOptionIndex;
  private int fTid;
  private long fStartTime;
  private long fEndTime;
  private Vector<Integer> fCpus;
  public static int BLANK = -1;

  public CPComplementOptionsDialog(Shell parent, int indexRunning, int indexRelated)
  {
      super(parent, SWT.DIALOG_TRIM | SWT.APPLICATION_MODAL);
      fRunningOptionIndex = indexRunning;
      fRelatedOptionIndex = indexRelated;
      fTid = BLANK;
      fCpus = new Vector<>();
  }

  public int getRunningOptionIndex()
  {
      return fRunningOptionIndex;
  }

  public int getRelatedOptionIndex()
  {
      return fRelatedOptionIndex;
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

  public Vector<Integer> getCpus()
  {
      if(fCpus.size() == 0)
      {
          return null;
      }
      return fCpus;
  }

  public void open() {
    Shell shell = new Shell(getParent(), getStyle());
    shell.setText(getText());
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

    shell.setText("Display options");

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

    final Combo c = new Combo(shell, SWT.READ_ONLY);
    c.setItems(CPComplementView.RUNNING_OPTIONS);
    GridData data = new GridData(GridData.FILL_HORIZONTAL);
    data.horizontalSpan = 1;
    c.setLayoutData(data);
    c.select(fRunningOptionIndex);

    Button changeDisplay = new Button(shell, SWT.PUSH);
    changeDisplay.setText("Change display");
    data = new GridData(GridData.FILL_HORIZONTAL);
    changeDisplay.setLayoutData(data);

    final Combo f = new Combo(shell, SWT.READ_ONLY);
    f.setItems(CPComplementView.RELATED_OPTIONS);
    data = new GridData(GridData.FILL_HORIZONTAL);
    data.horizontalSpan = 1;
    f.setLayoutData(data);
    f.select(fRelatedOptionIndex);
    changeDisplay.addSelectionListener(new SelectionAdapter() {
        @Override
      public void widgetSelected(SelectionEvent event) {

          fRunningOptionIndex = c.getSelectionIndex();
          fRelatedOptionIndex = f.getSelectionIndex();
          shell.close();
        }
      });

    Label label = new Label(shell, SWT.SEPARATOR | SWT.HORIZONTAL);
    data = new GridData(GridData.FILL_HORIZONTAL);
    data.horizontalSpan = 2;
    label.setLayoutData(data);

    //Tid

    label = new Label(shell, SWT.NONE);
    label.setText("Select tid (blank for current execution)");
    data = new GridData();
    data.horizontalSpan = 2;
    label.setLayoutData(data);

    final Text textTid = new Text(shell, SWT.BORDER);
    data = new GridData(GridData.FILL_HORIZONTAL);
    data.horizontalSpan = 2;
    textTid.setLayoutData(data);

    //Start time

    label = new Label(shell, SWT.NONE);
    label.setText("Select start time (blank for current execution)");
    data = new GridData();
    data.horizontalSpan = 2;
    label.setLayoutData(data);

    final Text textStartTime = new Text(shell, SWT.BORDER);
    data = new GridData(GridData.FILL_HORIZONTAL);
    data.horizontalSpan = 2;
    textStartTime.setLayoutData(data);

    //End time

    label = new Label(shell, SWT.NONE);
    label.setText("Select end time (blank for current execution)");
    data = new GridData();
    data.horizontalSpan = 2;
    label.setLayoutData(data);

    final Text textEndTime = new Text(shell, SWT.BORDER);
    data = new GridData(GridData.FILL_HORIZONTAL);
    data.horizontalSpan = 2;
    textEndTime.setLayoutData(data);

 // Load button

    Button loadTimeRange = new Button(shell, SWT.PUSH);
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

    //Cpus

    label = new Label(shell, SWT.NONE);
    label.setText("Select running cpus (separated by a comma, empty for all)");
    data = new GridData();
    data.horizontalSpan = 2;
    label.setLayoutData(data);

    final Text textCpus = new Text(shell, SWT.BORDER);
    data = new GridData(GridData.FILL_HORIZONTAL);
    data.horizontalSpan = 2;
    textCpus.setLayoutData(data);

    // button change parameters

    Button changeParameters = new Button(shell, SWT.PUSH);
    changeParameters.setText("Change parameters");
    data = new GridData(GridData.FILL_HORIZONTAL);
    data.horizontalSpan = 1;
    changeParameters.setLayoutData(data);
    changeParameters.addSelectionListener(new SelectionAdapter() {
      @Override
    public void widgetSelected(SelectionEvent event) {

          parseParameters(textTid.getText(), textStartTime.getText(),
                  textEndTime.getText(), textCpus.getText());


          shell.close();
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

    shell.setDefaultButton(changeParameters);

  }

  private void parseParameters(String textTid, String textStartTime, String textEndTime, String textCpus)
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

      //parse cpus
      fCpus = new Vector<>(); //Do not clear because the other one can be in use
      int indexComa = textCpus.indexOf(',');
      int endIndex = 0;
      String cpu;
      while (true)
      {
          if(indexComa == -1)
          {
              cpu = textCpus.substring(endIndex);
          }
          else
          {
              cpu = textCpus.substring(endIndex, indexComa);
          }

          endIndex = indexComa + 1;
          try{
              int tidInt = Integer.parseInt(cpu);
              fCpus.add(tidInt);
          }
          catch (NumberFormatException e){

          }

          if(indexComa == -1)
          {
               break;
          }

          indexComa = textCpus.indexOf(',',endIndex);
      }
  }

}