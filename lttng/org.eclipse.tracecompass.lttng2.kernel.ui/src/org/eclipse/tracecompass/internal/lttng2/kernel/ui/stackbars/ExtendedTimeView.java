/*******************************************************************************
 * Copyright (c) 2013 Ericsson
 *
 * All rights reserved. This program and the accompanying materials are
 * made available under the terms of the Eclipse Public License v1.0 which
 * accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *   Geneviève Bastien - Initial API and implementation (from ControlFlowView)
 *******************************************************************************/

package org.eclipse.tracecompass.internal.lttng2.kernel.ui.stackbars;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Vector;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.IToolBarManager;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.tracecompass.internal.lttng2.kernel.ui.stackbars.AbstractTimeGraphViewStackbars.TreeLabelProvider;
import org.eclipse.tracecompass.tmf.core.signal.TmfSignalHandler;
import org.eclipse.tracecompass.tmf.core.trace.ITmfTrace;
import org.eclipse.tracecompass.tmf.ui.views.timegraph.AbstractTimeGraphView;
import org.eclipse.tracecompass.tmf.ui.widgets.timegraph.ITimeGraphSelectionListener;
import org.eclipse.tracecompass.tmf.ui.widgets.timegraph.TimeGraphSelectionEvent;
import org.eclipse.tracecompass.tmf.ui.widgets.timegraph.model.ILinkEvent;
import org.eclipse.tracecompass.tmf.ui.widgets.timegraph.model.ITimeEvent;
import org.eclipse.tracecompass.tmf.ui.widgets.timegraph.model.ITimeGraphEntry;
import org.eclipse.tracecompass.tmf.ui.widgets.timegraph.model.TimeGraphEntry;
import org.eclipse.ui.ISharedImages;
import org.eclipse.ui.PlatformUI;

/**
 * The Control Flow view main object
 *
 * @author mcote
 */
public class ExtendedTimeView extends AbstractTimeGraphView {

    // ------------------------------------------------------------------------
    // Constants
    // ------------------------------------------------------------------------

    /**
     * View ID.
     */
    public static final String ID = "org.eclipse.linuxtools.tmf.analysis.graph.ui.stackbars.view.ExtendedTimeView"; //$NON-NLS-1$

    private static final String COLUMN_ID = Messages.Extended_Id;
    private static final String COLUMN_TYPE = Messages.Extended_Type;
    private static final String COLUMN_INFORMATION = Messages.Extended_Information;

    private static final String[] COLUMN_NAMES = new String[] {
            COLUMN_ID,
            COLUMN_TYPE,
            COLUMN_INFORMATION
    };

    private static final String[] FILTER_COLUMN_NAMES = new String[] {
            COLUMN_ID
    };

    //
    private StackbarsAnalysis fStackbarsAnalysis;
    private ITmfTrace fTrace;
    private HashSet<Integer> fSetCheckedTypes;
    private ExtendedTimeViewOptionsDialog fDialog;

    private class CriticalPathTreeLabelProvider extends TreeLabelProvider {

        @Override
        public String getColumnText(Object element, int columnIndex) {
            ExtendedTimeEntry entry = (ExtendedTimeEntry) element;
            if (columnIndex == 0) {
                return entry.getName();
            }
            else if (columnIndex == 1) {
                return entry.getType().name;//TODO
            }
            else if (columnIndex == 2) {
                return entry.getAdditionalInfo();//TODO
            }
            return ""; //$NON-NLS-1$
        }

    }

    private class CriticalPathEntryComparator implements Comparator<ITimeGraphEntry> {

        @Override
        public int compare(ITimeGraphEntry o1, ITimeGraphEntry o2) {

            CPCompRunningEntry entry1 = (CPCompRunningEntry)o1;
            CPCompRunningEntry entry2 = (CPCompRunningEntry)o2;
            long result = entry2.getDuration() - entry1.getDuration();
            if (result > 0)
            {
                return 1;
            }
            else if (result < 0)
            {
                return -1;
            }
            return 0;
        }
    }

    /**
     * Constructor
     */
    public ExtendedTimeView() {
        super(ID, new ExtendedTimePresentationProvider());
        setTreeColumns(COLUMN_NAMES);
        setFilterColumns(FILTER_COLUMN_NAMES);
        setTreeLabelProvider(new CriticalPathTreeLabelProvider());
        setEntryComparator(new CriticalPathEntryComparator());
        fSetCheckedTypes = new HashSet<>();
        ExtendedEntry.Type[] values = ExtendedEntry.Type.values();
        for(ExtendedEntry.Type type : values)
        {
            fSetCheckedTypes.add(type.ordinal());
        }
    }

    // ------------------------------------------------------------------------
    // Internal
    // ------------------------------------------------------------------------

    @Override
    protected List<ITimeEvent> getEventList(TimeGraphEntry entry,
            long startTime, long endTime, long resolution,
            IProgressMonitor monitor) {

        final long realStart = Math.max(startTime, entry.getStartTime());
        final long realEnd = Math.min(endTime, entry.getEndTime());
        if (realEnd <= realStart) {
            return null;
        }
        List<ITimeEvent> eventList = null;
        try {
            entry.setZoomedEventList(null);
            Iterator<ITimeEvent> iterator = entry.getTimeEventsIterator();
            eventList = new ArrayList<>();

            while (iterator.hasNext()) {

                ITimeEvent event = iterator.next();
                /* is event visible */
                if (((event.getTime() >= realStart) && (event.getTime() <= realEnd)) ||
                        ((event.getTime() + event.getDuration() > realStart) &&
                        (event.getTime() + event.getDuration() < realEnd))) {
                    eventList.add(event);
                }
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
        return eventList;
    }

    /**
     * @param signal
     *      Signal send by the StackbarsAnalysis
     */
    @TmfSignalHandler
    public void dataUpdated(final TmfDataUpdatedSignal signal) {
        if(signal.getData() == ExtendedTimeView.class)
        {
            rebuild();
        }
    }

    @Override
    protected List<ILinkEvent> getLinkList(long startTime, long endTime, long resolution, IProgressMonitor monitor) {

        if(fStackbarsAnalysis == null)
        {
            return null;
        }
        List<ILinkEvent> linksInRange = new ArrayList<>();
        Collection<ILinkEvent> list = fStackbarsAnalysis.getAndResetLinkEventsList();
        if(list == null)
        {
            return null;
        }
        for (ILinkEvent link : list) {
            if (((link.getTime() >= startTime) && (link.getTime() <= endTime)) ||
                    ((link.getTime() + link.getDuration() >= startTime) && (link.getTime() + link.getDuration() <= endTime))) {
                linksInRange.add(link);
            }
        }
        return linksInRange;
    }

    @Override
    protected void buildEventList(ITmfTrace trace, ITmfTrace parentTrace, IProgressMonitor monitor) {

        List<TimeGraphEntry> list = StackbarsAnalysis.getInstance().getAndResetExtendedTimeList();

        fTrace = trace;

        if(list == null)
        {
            list = new ArrayList<>();
        }

        if(list.size() != 0)
        {
            setStartTime(list.get(0).getStartTime());
            setEndTime(list.get(0).getEndTime());
        }

        putEntryList(fTrace, list);
        refresh();
    }



// ------------------------------------------------------------------------
    // Part For Button Action
    // ------------------------------------------------------------------------

    @Override
    protected void fillLocalToolBar(IToolBarManager manager) {

        ITimeGraphSelectionListener listener = new ITimeGraphSelectionListener(){

            @Override
            public void selectionChanged(TimeGraphSelectionEvent event) {
                // TODO Auto-generated method stub
                if (event.getSelection() instanceof TimeGraphEntry) {
                    TimeGraphEntry stackbarsEntry = (TimeGraphEntry) event.getSelection();
                    System.out.println(stackbarsEntry); //TODO
                }
            }
        };
        getTimeGraphCombo().addSelectionListener(listener);

        // Dialog to show the different view options
        Action changeDisplay = new Action() {
            @Override
            public void run() {

                if(fDialog == null)
                {
                    Shell shell = PlatformUI.getWorkbench().getActiveWorkbenchWindow().getShell();
                    fDialog = new ExtendedTimeViewOptionsDialog(shell);
                }
                fDialog.open(fSetCheckedTypes);
            }
        };
        changeDisplay.setToolTipText("Display options");
        changeDisplay.setImageDescriptor(PlatformUI.getWorkbench().getSharedImages().
                getImageDescriptor(ISharedImages.IMG_TOOL_CUT));

        manager.add(changeDisplay);

     // Execute custom analysis
        Action executeAnalysisExtended = new Action() {
            @Override
            public void run() {
                Job j = new Job("RT_Analysis"){
                    @Override
                    protected IStatus run(final IProgressMonitor monitor) {

                        if(monitor.isCanceled()){
                            return Status.CANCEL_STATUS;
                        }
                        executeAnalysisExtended(monitor);

                        return Status.OK_STATUS;

                    }
                };
                j.schedule();
            }
        };
        executeAnalysisExtended.setToolTipText("Execute the analysis of the selected execution");
        executeAnalysisExtended.setImageDescriptor(PlatformUI.getWorkbench().getSharedImages().
        getImageDescriptor(ISharedImages.IMG_TOOL_REDO));
        manager.add(executeAnalysisExtended);

        super.fillLocalToolBar(manager);
    }

    private void executeAnalysisExtended(IProgressMonitor monitor)
    {
        ITmfTrace trace = getTrace();
        Vector<String> futex = null;
        Vector<String> queues = null;
        Vector<String> timers = null;
        long startTime = ExtendedComparisonOptionsDialog.BLANK;
        long endTime = ExtendedComparisonOptionsDialog.BLANK;
        long nbEventsBack = ExtendedComparisonOptionsDialog.BLANK;
        if(fDialog != null)
        {
            futex = fDialog.getFutex();
            queues = fDialog.getQueues();
            timers = fDialog.getTimers();
            startTime = fDialog.getStartTime();
            endTime = fDialog.getEndTime();
            nbEventsBack = fDialog.getNbEventsBack();
        }
        StackbarsAnalysis.getInstance().executeAnalysisShowMoreInformation(monitor, trace, futex, queues, timers, startTime, endTime, nbEventsBack);
    }

}
