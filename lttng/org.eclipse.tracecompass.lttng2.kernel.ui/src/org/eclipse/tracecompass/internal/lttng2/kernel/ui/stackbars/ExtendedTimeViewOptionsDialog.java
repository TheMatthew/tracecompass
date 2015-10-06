package org.eclipse.tracecompass.internal.lttng2.kernel.ui.stackbars;

import java.util.HashSet;
import java.util.Vector;

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
import org.eclipse.tracecompass.tmf.core.timestamp.ITmfTimestamp;

class ExtendedTimeViewOptionsDialog extends Dialog {

    public static final long BLANK = -1;
    private Vector<String> fQueues;
    private Vector<String> fFutex;
    private Vector<String> fTimers;
    private long fStartTime;
    private long fEndTime;
    private long fNbEventsBack;

    public ExtendedTimeViewOptionsDialog(Shell parent)
    {
        super(parent, SWT.DIALOG_TRIM | SWT.APPLICATION_MODAL);
        fQueues = new Vector<>();
        fFutex = new Vector<>();
        fTimers = new Vector<>();
    }

    public Vector<String> getQueues() {
        if(fQueues.size() == 0)
        {
            return null;
        }
        return fQueues;
    }

    public Vector<String> getFutex()
    {
        if(fFutex.size() == 0)
        {
            return null;
        }
        return fFutex;
    }

    public Vector<String> getTimers()
    {
        if(fTimers.size() == 0)
        {
            return null;
        }
        return fTimers;
    }

    public long getStartTime()
    {
        return fStartTime;
    }

    public long getEndTime()
    {
        return fEndTime;
    }

    public long getNbEventsBack()
    {
        return fNbEventsBack;
    }

    public void open(HashSet<Integer> fSetCheckedTypes) {
      Shell shell = new Shell(getParent(), getStyle());
      shell.setText(getText());
      createContents(shell, fSetCheckedTypes);
      shell.pack();
      shell.open();
      Display display = getParent().getDisplay();
      while (!shell.isDisposed()) {
        if (!display.readAndDispatch()) {
          display.sleep();
        }
      }
    }

    private void createContents(final Shell shell, final HashSet<Integer> fSetCheckedTypes) {

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
        if(fSetCheckedTypes.contains(types[i].name))
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
                  fSetCheckedTypes.add(table.indexOf(item));
              }
              else
              {
                  fSetCheckedTypes.remove(table.indexOf(item));
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


      //Queues

      Label label = new Label(parametersGroup, SWT.NONE);
      label.setText("Queue(s) (separated by a comma)");
      data = new GridData();
      data.horizontalSpan = 2;
      label.setLayoutData(data);

      final Text textQueues = new Text(parametersGroup, SWT.BORDER);
      data = new GridData(GridData.FILL_HORIZONTAL);
      data.horizontalSpan = 2;
      textQueues.setLayoutData(data);

      //Futex

      label = new Label(parametersGroup, SWT.NONE);
      label.setText("Futex (separated by a comma)");
      data = new GridData();
      data.horizontalSpan = 2;
      label.setLayoutData(data);

      final Text textFutex = new Text(parametersGroup, SWT.BORDER);
      data = new GridData(GridData.FILL_HORIZONTAL);
      data.horizontalSpan = 2;
      textFutex.setLayoutData(data);

      //Timers

      label = new Label(parametersGroup, SWT.NONE);
      label.setText("Timers (separated by a comma)");
      data = new GridData();
      data.horizontalSpan = 2;
      label.setLayoutData(data);

      final Text textTimers = new Text(parametersGroup, SWT.BORDER);
      data = new GridData(GridData.FILL_HORIZONTAL);
      data.horizontalSpan = 2;
      textTimers.setLayoutData(data);

      Group timeGroup = new Group(shell, SWT.SHADOW_ETCHED_IN);
      timeGroup.setLayoutData(new GridData(SWT.FILL, SWT.BEGINNING, true, false, 2, 2));
      timeGroup.setLayout(new GridLayout(2, false));
      timeGroup.setText("Select time options");

      //Start time

      label = new Label(timeGroup, SWT.NONE);
      label.setText("Start time (Blank for selected extended entry)");
      data = new GridData();
      data.horizontalSpan = 2;
      label.setLayoutData(data);

      final Text textStartTime = new Text(timeGroup, SWT.BORDER);
      data = new GridData(GridData.FILL_HORIZONTAL);
      data.horizontalSpan = 2;
      textStartTime.setLayoutData(data);

      //End time

      label = new Label(timeGroup, SWT.NONE);
      label.setText("End time (Blank for selected extended entry)");
      data = new GridData();
      data.horizontalSpan = 2;
      label.setLayoutData(data);

      final Text textEndTime = new Text(timeGroup, SWT.BORDER);
      data = new GridData(GridData.FILL_HORIZONTAL);
      data.horizontalSpan = 2;
      textEndTime.setLayoutData(data);

      //Nb events back

      label = new Label(timeGroup, SWT.NONE);
      label.setText("Nb events back (Blank for default)");
      data = new GridData();
      data.horizontalSpan = 2;
      label.setLayoutData(data);

      final Text textNbEventsBack = new Text(timeGroup, SWT.BORDER);
      data = new GridData(GridData.FILL_HORIZONTAL);
      data.horizontalSpan = 2;
      textNbEventsBack.setLayoutData(data);

   // Load button

      Button loadTimeRange = new Button(timeGroup, SWT.PUSH);
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

            parseParameters(textQueues.getText(), textFutex.getText(),
                    textTimers.getText(), textStartTime.getText(),
                    textEndTime.getText(), textNbEventsBack.getText());
            shell.close();
        }
      });

      shell.setDefaultButton(changeParameters);

    }

    private void parseParameters(String textQueues, String textFutex, String textTimers, String textStartTime, String textEndTime, String textNbEventsBack)
    {
        //Parse start
        try{
            fStartTime = Long.parseLong(textStartTime);
        }
        catch (NumberFormatException e)
        {
            fStartTime = BLANK;
        }

        //Parse end
        try{
            fEndTime = Long.parseLong(textEndTime);
        }
        catch (NumberFormatException e)
        {
            fEndTime = BLANK;
        }

        //Parse end
        try{
            fNbEventsBack = Long.parseLong(textNbEventsBack);
        }
        catch (NumberFormatException e)
        {
            fNbEventsBack = BLANK;
        }

        //parse fQueues
        int indexComa = textQueues.indexOf(',');
        int endIndex = 0;
        String queue;
        while (true)
        {
            if(indexComa == -1)
            {
                queue = textQueues.substring(endIndex);
            }
            else
            {
                queue = textQueues.substring(endIndex, indexComa);
            }

            endIndex = indexComa + 1;
            fQueues.add(queue);

            if(indexComa == -1)
            {
                 break;
            }

            indexComa = textQueues.indexOf(',',endIndex);
        }

        //parse fFutex
        indexComa = textFutex.indexOf(',');
        endIndex = 0;
        String futex;
        while (true)
        {
            if(indexComa == -1)
            {
                futex = textFutex.substring(endIndex);
            }
            else
            {
                futex = textFutex.substring(endIndex, indexComa);
            }

            endIndex = indexComa + 1;
            fFutex.add(futex);

            if(indexComa == -1)
            {
                 break;
            }

            indexComa = textFutex.indexOf(',',endIndex);
        }

        //parse fTimers
        indexComa = textTimers.indexOf(',');
        endIndex = 0;
        String timer;
        while (true)
        {
            if(indexComa == -1)
            {
                timer = textTimers.substring(endIndex);
            }
            else
            {
                timer = textTimers.substring(endIndex, indexComa);
            }

            endIndex = indexComa + 1;
            fTimers.add(timer);
            if(indexComa == -1)
            {
                 break;
            }

            indexComa = textTimers.indexOf(',',endIndex);
        }
    }

}