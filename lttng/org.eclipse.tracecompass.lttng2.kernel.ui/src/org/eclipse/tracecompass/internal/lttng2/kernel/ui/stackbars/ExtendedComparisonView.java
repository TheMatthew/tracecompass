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

package org.eclipse.tracecompass.internal.lttng2.kernel.ui.stackbars

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.IToolBarManager;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.tracecompass.tmf.core.signal.TmfDataUpdatedSignal;
import org.eclipse.tracecompass.tmf.core.signal.TmfSelectionRangeUpdatedSignal;
import org.eclipse.tracecompass.tmf.core.signal.TmfSignalHandler;
import org.eclipse.tracecompass.tmf.core.signal.TmfSignalManager;
import org.eclipse.tracecompass.tmf.core.signal.TmfTidSynchSignal;
import org.eclipse.tracecompass.tmf.core.signal.TmfWindowRangeUpdatedSignal;
import org.eclipse.tracecompass.tmf.core.timestamp.ITmfTimestamp;
import org.eclipse.tracecompass.tmf.core.timestamp.TmfTimeRange;
import org.eclipse.tracecompass.tmf.core.timestamp.TmfTimestamp;
import org.eclipse.tracecompass.tmf.core.trace.ITmfTrace;
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
public class ExtendedComparisonView extends AbstractTimeGraphViewStackbars {

    // ------------------------------------------------------------------------
    // Constants
    // ------------------------------------------------------------------------

    /**
     * View ID.
     */
    public static final String ID = "org.eclipse.linuxtools.internal.lttng2.kernel.ui.stackbars.ExtentedComparisonView"; //$NON-NLS-1$

    private static final String COLUMN_TYPE = Messages.Extended_Type;
    private static final String COLUMN_DURATION = Messages.Extended_Duration;
    private static final String COLUMN_INFORMATION = Messages.Extended_Information;

    private static final String[] COLUMN_NAMES = new String[] {
            COLUMN_TYPE,
            COLUMN_DURATION,
            COLUMN_INFORMATION
    };

    private static final String[] FILTER_COLUMN_NAMES = new String[] {
            COLUMN_DURATION
    };

    //
    private StackbarsAnalysis fStackbarsAnalysis;
    private ITmfTrace fTrace;
    private HashSet<Integer> fSetCheckedTypes;
    private ExtendedComparisonOptionsDialog fDialog;

    private class ExtentedTreeLabelProvider extends TreeLabelProvider {

        @Override
        public String getColumnText(Object element, int columnIndex) {
            ExtendedEntry entry = (ExtendedEntry) element;
            if (columnIndex == 0) {
                return entry.getType().name();
            }
            else if (columnIndex == 1) {
                return Long.toString(entry.getEndTime());
            }
            else if (columnIndex == 2) {
                return entry.getAdditionalInfo();
            }
            return ""; //$NON-NLS-1$
        }

    }

    private class EntryComparator implements Comparator<ITimeGraphEntry> {

        @Override
        public int compare(ITimeGraphEntry o1, ITimeGraphEntry o2) {

            ExtendedEntry entry1 = (ExtendedEntry)o1;
            ExtendedEntry entry2 = (ExtendedEntry)o2;
            long result = entry2.getEndTime() - entry1.getEndTime();
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
    public ExtendedComparisonView() {
        super(ID, new ExtendedComparisonPresentationProvider());
        setTreeColumns(COLUMN_NAMES);
        setFilterColumns(FILTER_COLUMN_NAMES);
        setTreeLabelProvider(new ExtentedTreeLabelProvider());
        setEntryComparator(new EntryComparator());
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
        if(signal.getData() == ExtendedComparisonView.class)
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

        List<TimeGraphEntry> list = StackbarsAnalysis.getInstance().getAndResetExtendedList();

        fTrace = trace;

        if(list == null)
        {
            list = new ArrayList<>();
        }
        else
        {
            if(list.size() != 0)
            {
                // min because the first execution is the one with the biggest elapsed time
                TimeGraphEntry entry = Collections.min(list, new EntryComparator());
                setStartTime(0);
                setEndTime((long)(entry.getEndTime() *1.1));
            }
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
                if (event.getSelection() instanceof ExtendedEntry) {
                    ExtendedEntry entry = (ExtendedEntry) event.getSelection();

                    long duration = entry.getEndTime();

                    ITmfTimestamp startTime = new TmfTimestamp(entry.getRealStartTime(), ITmfTimestamp.NANOSECOND_SCALE);
                    ITmfTimestamp endTime = new TmfTimestamp(startTime.getValue() + duration, ITmfTimestamp.NANOSECOND_SCALE);

                    //Send signal for time
                    TmfTimeRange range = new TmfTimeRange(new TmfTimestamp(startTime.getValue() - duration,
                            ITmfTimestamp.NANOSECOND_SCALE), new TmfTimestamp(endTime.getValue() + duration,
                                    ITmfTimestamp.NANOSECOND_SCALE));
                    TmfSignalManager.dispatchSignal(new TmfWindowRangeUpdatedSignal(this, range));
                    TmfSelectionRangeUpdatedSignal signal = new TmfSelectionRangeUpdatedSignal(this, startTime, endTime);
                    TmfSignalManager.dispatchSignal(signal);

                    //Send signal for tid
                    TmfTidSynchSignal signalTid = new TmfTidSynchSignal(this, entry.getTid());
                    TmfSignalManager.dispatchSignal(signalTid);

                    StackbarsAnalysis.getInstance().setCurrentExtendedEntry(entry);
                }
                else
                {
                    StackbarsAnalysis.getInstance().setCurrentExtendedEntry(null);
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
                    fDialog = new ExtendedComparisonOptionsDialog(shell);
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
        int tid = ExtendedComparisonOptionsDialog.BLANK;
        long startTime = ExtendedComparisonOptionsDialog.BLANK;
        long endTime = ExtendedComparisonOptionsDialog.BLANK;
        if(fDialog != null)
        {
            tid = fDialog.getTid();
            startTime = fDialog.getStartTime();
            endTime = fDialog.getEndTime();
        }
        StackbarsAnalysis.getInstance().executeAnalysisExtended(monitor, trace, tid, startTime, endTime,fSetCheckedTypes);
    }

}
