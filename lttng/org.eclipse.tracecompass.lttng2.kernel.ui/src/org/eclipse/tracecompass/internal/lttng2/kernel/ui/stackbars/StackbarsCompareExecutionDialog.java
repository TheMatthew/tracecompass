package org.eclipse.tracecompass.internal.lttng2.kernel.ui.stackbars;

import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Dialog;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Text;

class StackbarsCompareExecutionDialog extends Dialog {
  private String fExecutionId;
  private Text fTextExecution;

  private boolean fCompare;

  public StackbarsCompareExecutionDialog(Shell parent)
  {
      super(parent, SWT.DIALOG_TRIM | SWT.APPLICATION_MODAL);
      fCompare = false;
  }

  public String getExecutionId()
  {
      if (fCompare)
      {
          return fExecutionId;
      }

      return "-1"; //$NON-NLS-1$
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

    shell.setText("Compare executions");

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

    Label label = new Label(shell, SWT.NONE);
    label.setText("Enter the starting rank of the execution to compare with the current execution");
    GridData data = new GridData();
    data.horizontalSpan = 2;
    label.setLayoutData(data);

    fTextExecution = new Text(shell, SWT.BORDER);
    data = new GridData(GridData.FILL_HORIZONTAL);
    data.horizontalSpan = 2;
    fTextExecution.setLayoutData(data);

    Button ok = new Button(shell, SWT.PUSH);
    ok.setText("Compare");
    data = new GridData(GridData.FILL_HORIZONTAL);
    ok.setLayoutData(data);
    ok.addSelectionListener(new SelectionAdapter() {
      @Override
    public void widgetSelected(SelectionEvent event) {

          try{
              Integer.parseInt(fExecutionId);
              fExecutionId = fTextExecution.getText();
          }
          catch (NumberFormatException e)
          {
              fExecutionId = "-1"; //$NON-NLS-1$
              //e.printStackTrace();
          }

        fCompare = true;
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
        fCompare = false;
        shell.close();
      }
    });

    shell.setDefaultButton(ok);
  }
}