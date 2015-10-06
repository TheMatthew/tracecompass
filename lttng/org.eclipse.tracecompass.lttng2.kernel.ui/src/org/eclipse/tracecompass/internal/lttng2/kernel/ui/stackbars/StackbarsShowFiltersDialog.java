package org.eclipse.tracecompass.internal.lttng2.kernel.ui.stackbars;

import java.util.Vector;

import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.ScrolledComposite;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.FillLayout;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Dialog;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Shell;

class StackbarsShowFiltersDialog extends Dialog {
  private Button[] fButtons;
  private Vector<StackbarsExecutionsDetection.StackbarsFilter> fSelectedFilters;


  public StackbarsShowFiltersDialog(Shell parent)
  {
      super(parent, SWT.DIALOG_TRIM | SWT.APPLICATION_MODAL);
      fSelectedFilters = new Vector<>();
  }

  public void open(Vector<StackbarsExecutionsDetection.StackbarsFilter> filters) {
    Shell shell = new Shell(getParent(), getStyle());
    shell.setText(getText());
    createContents(shell, filters);
    shell.open();
    Display display = getParent().getDisplay();
    while (!shell.isDisposed()) {
      if (!display.readAndDispatch()) {
        display.sleep();
      }
    }
  }

  public Vector<StackbarsExecutionsDetection.StackbarsFilter> getSelectedFilters()
  {
      return fSelectedFilters;
  }

  private void createContents(final Shell shell, final Vector<StackbarsExecutionsDetection.StackbarsFilter> filters) {

      shell.setLayout(new FillLayout());

      // set the size of the scrolled content - method 1
      final ScrolledComposite sc1 = new ScrolledComposite(shell, SWT.BORDER | SWT.H_SCROLL | SWT.V_SCROLL);
      final Composite c1 = new Composite(sc1, SWT.NONE);
      sc1.setContent(c1);

    shell.setText("Select filters to apply");

    GridLayout gridLayout = new GridLayout(1, true);
    gridLayout.marginHeight = 10;
    gridLayout.marginWidth = 10;
    gridLayout.marginTop = 15;
    gridLayout.marginBottom = 15;
    gridLayout.horizontalSpacing = 10;
    gridLayout.verticalSpacing = 15;
    gridLayout.marginLeft = 15;
    gridLayout.marginRight = 15;

    c1.setLayout(gridLayout);
    fButtons = new Button[filters.size()];
    for(int i = 0; i < filters.size(); ++i)
    {
        fButtons[i] = new Button(c1, SWT.CHECK);
        fButtons[i].setText(filters.get(i).toString());
    }

    Button ok = new Button(c1, SWT.PUSH);
    ok.setText("Select");
    GridData data = new GridData(GridData.FILL_HORIZONTAL);
    ok.setLayoutData(data);
    ok.addSelectionListener(new SelectionAdapter() {
      @Override
    public void widgetSelected(SelectionEvent event) {

        for(int i = 0; i < fButtons.length; ++i)
        {
            if(fButtons[i].getSelection())
            {
                fSelectedFilters.add(filters.get(i));
            }
        }

        shell.close();
      }
    });

    Button cancel = new Button(c1, SWT.PUSH);
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
    c1.setSize(c1.computeSize(SWT.DEFAULT, SWT.DEFAULT));
    int newX = c1.getSize().x;
    if(newX > 1000)
    {
        newX = 1000;
    }
    int newY = c1.getSize().y;
    if(newY > 800)
    {
        newY = 800;
    }
    c1.setSize(newX,newY);
    shell.pack();
    c1.setSize(c1.computeSize(SWT.DEFAULT, SWT.DEFAULT));
    c1.layout();
  }
}