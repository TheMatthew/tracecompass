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
import java.util.Iterator;
import java.util.List;
import java.util.Vector;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.IToolBarManager;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.tracecompass.internal.lttng2.kernel.ui.stackbars.AbstractTimeGraphViewStackbars.TreeLabelProvider;
import org.eclipse.tracecompass.tmf.core.signal.TmfSignalHandler;
import org.eclipse.tracecompass.tmf.core.signal.TmfSignalManager;
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
public class CPComplementView extends AbstractTimeGraphView {

    // ------------------------------------------------------------------------
    // Constants
    // ------------------------------------------------------------------------

    /**
     * View ID.
     */
    public static final String ID = "org.eclipse.linuxtools.internal.lttng2.kernel.ui.stackbars.CPComplementView"; //$NON-NLS-1$

    private static final String COLUMN_TID = Messages.CPComplement_Tid;
    private static final String COLUMN_DURATION = Messages.CPComplement_Duration;
    private static final String COLUMN_PRIORITY = Messages.CPComplement_Priority;

    private static final String[] COLUMN_NAMES = new String[] {
            COLUMN_TID,
            COLUMN_DURATION,
            COLUMN_PRIORITY
    };

    private static final String[] FILTER_COLUMN_NAMES = new String[] {
            COLUMN_TID
    };


    public static final String RUNNING_OFF = "Running - Off";
    public static final String RUNNING_ON = "Running - On";
    public static final String RELATED_OFF = "Related - Off";
    public static final String RELATED_INDIRECT = "Related - Indirect";
    public static final String RELATED_DIRECT = "Related - Direct";
    public static final String[] RUNNING_OPTIONS = new String[]{RUNNING_ON,RUNNING_OFF,};
    public static final String[] RELATED_OPTIONS = new String[]{RELATED_INDIRECT,RELATED_DIRECT,RELATED_OFF};

    //
    private StackbarsAnalysis fStackbarsAnalysis;
    private int fRunningOptionIndex;
    private int fRelatedOptionIndex;
    private List<TimeGraphEntry> fListRunning;
    private Collection<TimeGraphEntry> fListRelated;
    private ITmfTrace fTrace;
    private CPComplementOptionsDialog fDialog;

    private class CriticalPathTreeLabelProvider extends TreeLabelProvider {

        @Override
        public String getColumnText(Object element, int columnIndex) {
            if(element instanceof CPCompRunningEntry)
            {
                CPCompRunningEntry entry = (CPCompRunningEntry) element;
                if (columnIndex == 0) {
                    return entry.getName();
                }
                else if (columnIndex == 1) {
                    return Long.toString(entry.getDuration());
                }
                else if (columnIndex == 2) {
                    return Integer.toString(entry.getPriority());
                }
            }
            else if(element instanceof CPCompRelatedEntry)
            {
                CPCompRelatedEntry entry = (CPCompRelatedEntry) element;
                if (columnIndex == 0) {
                    return entry.getName();
                }
                else if (columnIndex == 2) {
                    return Integer.toString(entry.getPriority());
                }
            }
            return ""; //$NON-NLS-1$
        }

    }

    private class CriticalPathEntryComparator implements Comparator<ITimeGraphEntry> {

        @Override
        public int compare(ITimeGraphEntry o1, ITimeGraphEntry o2) {

            if(! (o1 instanceof CPCompRunningEntry))
            {
                return -1;
            }
            else if(! (o2 instanceof CPCompRunningEntry))
            {
                return 1;
            }
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
    public CPComplementView() {
        super(ID, new CPComplementPresentationProvider());
        setTreeColumns(COLUMN_NAMES);
        setFilterColumns(FILTER_COLUMN_NAMES);
        setTreeLabelProvider(new CriticalPathTreeLabelProvider());
        setEntryComparator(new CriticalPathEntryComparator());
        fRunningOptionIndex = 0;
        fRelatedOptionIndex = 1;
        fListRunning = new ArrayList<>();
        fListRelated = new ArrayList<>();
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
        if(signal.getData() == CPComplementView.class)
        {
            fStackbarsAnalysis = (StackbarsAnalysis) signal.getSource();
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

        if(fStackbarsAnalysis == null)
        {
            return;
        }

        fListRunning = fStackbarsAnalysis.getAndResetPreemptedList();
        if(fListRunning == null)
        {
            fListRunning = new ArrayList<>();
        }

        fListRelated = fStackbarsAnalysis.getAndResetDependenciesList();
        if(fListRelated == null)
        {
            fListRelated = new ArrayList<>();
        }

        fTrace = trace;

        setStartTime(fStackbarsAnalysis.getEntryStart());
        setEndTime(fStackbarsAnalysis.getEntryEnd());

        display();
    }

    private void display() {

        List<TimeGraphEntry> list = new ArrayList<>();
         if(RUNNING_OPTIONS[fRunningOptionIndex].equals(RUNNING_ON))
         {
             list.addAll(fListRunning);
         }
         if (RELATED_OPTIONS[fRelatedOptionIndex].equals(RELATED_INDIRECT))
         {
             list.addAll(fListRelated);
         }
         else if (RELATED_OPTIONS[fRelatedOptionIndex].equals(RELATED_DIRECT))
         {
             for(TimeGraphEntry entry : fListRelated)
             {
                 if(entry instanceof CPCompRelatedEntry)
                 {
                     CPCompRelatedEntry entryR = (CPCompRelatedEntry)entry;
                     if(entryR.isDirect())
                     {
                         list.add(entry);
                     }
                 }
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
                if (event.getSelection() instanceof CPCompRunningEntry) {
                    CPCompRunningEntry stackbarsEntry = (CPCompRunningEntry) event.getSelection();
                    //Send signal for tid
                    TmfTidSynchSignal signalTid = new TmfTidSynchSignal(this, stackbarsEntry.getTid());
                    TmfSignalManager.dispatchSignal(signalTid);
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
                    fDialog = new CPComplementOptionsDialog(shell,fRunningOptionIndex, fRelatedOptionIndex);
                }
                fDialog.open();
                if(fRunningOptionIndex != fDialog.getRunningOptionIndex() ||
                        fRelatedOptionIndex != fDialog.getRelatedOptionIndex())
                {
                    fRunningOptionIndex = fDialog.getRunningOptionIndex();
                    fRelatedOptionIndex = fDialog.getRelatedOptionIndex();
                    display();
                }
            }
        };
        changeDisplay.setToolTipText("Display options");
        changeDisplay.setImageDescriptor(PlatformUI.getWorkbench().getSharedImages().
                getImageDescriptor(ISharedImages.IMG_TOOL_CUT));

        manager.add(changeDisplay);

     // Execute custom analysis
        Action executeAnalysisRunningCP = new Action() {
            @Override
            public void run() {
                executeAnalysisRunningRelated();

            }
        };
        executeAnalysisRunningCP.setToolTipText("Show the running threads when execution was blocked");
        executeAnalysisRunningCP.setImageDescriptor(PlatformUI.getWorkbench().getSharedImages().
        getImageDescriptor(ISharedImages.IMG_TOOL_REDO));
        manager.add(executeAnalysisRunningCP);


        super.fillLocalToolBar(manager);
    }

    private void executeAnalysisRunningRelated()
    {
        ITmfTrace trace = getTrace();
        int tid = CPComplementOptionsDialog.BLANK;
        long startTime = CPComplementOptionsDialog.BLANK;
        long endTime = CPComplementOptionsDialog.BLANK;
        Vector<Integer> cpus = null;
        if(fDialog != null)
        {
            tid = fDialog.getTid();
            startTime = fDialog.getStartTime();
            endTime = fDialog.getEndTime();
            cpus = fDialog.getCpus();
        }
        StackbarsAnalysis.getInstance().executeAnalysisRunningRelated(trace, tid, startTime, endTime, cpus);
    }

}
