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
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Observable;
import java.util.Observer;
import java.util.Vector;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.IToolBarManager;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Tree;
import org.eclipse.tracecompass.analysis.os.linux.ui.views.controlflow.ControlFlowEntry;
import org.eclipse.tracecompass.analysis.os.linux.ui.views.controlflow.ControlFlowView;
import org.eclipse.tracecompass.internal.lttng2.kernel.core.analysis.graph.model.LttngStrings;
import org.eclipse.tracecompass.internal.lttng2.kernel.ui.stackbars.StackbarsExecutionsDetection.BorderEvents;
import org.eclipse.tracecompass.internal.lttng2.kernel.ui.stackbars.StackbarsExecutionsDetection.EventDefinition;
import org.eclipse.tracecompass.internal.lttng2.kernel.ui.stackbars.StackbarsExecutionsDetection.ExecDefinition;
import org.eclipse.tracecompass.internal.lttng2.kernel.ui.stackbars.StackbarsExecutionsDetection.StackbarsFilter;
import org.eclipse.tracecompass.internal.lttng2.kernel.ui.stackbars.StackbarsExecutionsDetection.StackbarsFilter.StackbarsFilterType;
import org.eclipse.tracecompass.tmf.core.signal.TmfDataUpdatedSignal;
import org.eclipse.tracecompass.tmf.core.signal.TmfSelectionRangeUpdatedSignal;
import org.eclipse.tracecompass.tmf.core.signal.TmfSignalHandler;
import org.eclipse.tracecompass.tmf.core.signal.TmfSignalManager;
import org.eclipse.tracecompass.tmf.core.signal.TmfTidSynchSignal;
import org.eclipse.tracecompass.tmf.core.signal.TmfWindowRangeUpdatedSignal;
import org.eclipse.tracecompass.tmf.core.timestamp.ITmfTimestamp;
import org.eclipse.tracecompass.tmf.core.timestamp.TmfTimeRange;
import org.eclipse.tracecompass.tmf.core.timestamp.TmfTimestamp;
import org.eclipse.tracecompass.tmf.core.timestamp.TmfTimestampFormat;
import org.eclipse.tracecompass.tmf.core.trace.ITmfTrace;
import org.eclipse.tracecompass.tmf.ui.widgets.timegraph.ITimeGraphSelectionListener;
import org.eclipse.tracecompass.tmf.ui.widgets.timegraph.TimeGraphSelectionEvent;
import org.eclipse.tracecompass.tmf.ui.widgets.timegraph.model.ILinkEvent;
import org.eclipse.tracecompass.tmf.ui.widgets.timegraph.model.ITimeEvent;
import org.eclipse.tracecompass.tmf.ui.widgets.timegraph.model.ITimeGraphEntry;
import org.eclipse.tracecompass.tmf.ui.widgets.timegraph.model.TimeGraphEntry;
import org.eclipse.tracecompass.tmf.ui.widgets.timegraph.widgets.TimeGraphControl;
import org.eclipse.ui.ISelectionListener;
import org.eclipse.ui.ISharedImages;
import org.eclipse.ui.IViewPart;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.IWorkbenchPart;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.progress.UIJob;
//import org.eclipse.linuxtools.internal.lttng2.kernel.ui.stackbars.StackbarsStateSystemModule.LastActionRequested;
/**
 * The Stackbars view main object
 *
 * @author MC
 */
@SuppressWarnings("nls")
public class StackbarsView extends AbstractTimeGraphViewStackbars {

    // ------------------------------------------------------------------------
    // Constants
    // ------------------------------------------------------------------------

    /**
     * View ID.
     */
    public static final String ID = "org.eclipse.linuxtools.tmf.analysis.graph.ui.stackbars.view.stackbarsview"; //$NON-NLS-1$

    private static final String COLUMN_RANK_ORDER = Messages.StackbarsView_columnRankOrder;
    private static final String COLUMN_STARTING_TIME = Messages.StackbarsView_columnStartingTime;
    private static final String COLUMN_RANK_DURATION = Messages.StackbarsView_columnRankDuration;
    private static final String COLUMN_ELAPSED = Messages.StackbarsView_columnElapsed;
    private static final String COLUMN_TID = Messages.StackbarsView_columnTid;
    private static final String COLUMN_NAME = Messages.StackbarsView_columnName;
    private static final String COLUMN_RUNNING_TIME = Messages.StackbarsView_columnRunningTime;
    private static final String COLUMN_PREEMPTED_TIME = Messages.StackbarsView_columnPreemptedTime;
    //private static final String CONTEXT_VTID = "context._vtid"; //$NON-NLS-1$

    private static final String[] COLUMN_NAMES = new String[] {
            COLUMN_RANK_ORDER,
            COLUMN_STARTING_TIME,
            COLUMN_RANK_DURATION,
            COLUMN_ELAPSED,
            COLUMN_TID,
            COLUMN_NAME,
            COLUMN_RUNNING_TIME,
            COLUMN_PREEMPTED_TIME,
    };

    private static final String[] FILTER_COLUMN_NAMES = new String[] {
            COLUMN_RANK_ORDER
    };

    //private StackbarsStateSystemModule fModule;
    private Action fActionDepthLower;
    private Action fActionDepthUpper;
    private boolean fShowDeadline;
    private StackbarsSelectEventsDialog fSelectEventsDialog;
    private ExecDefinition fExecDef;
    private int fCurrentTid;
    private StackbarsAnalysis fStackbarsAnalysis;
    private boolean fParseExecutionsOnCFVSelection;

    private ITmfTimestamp fCurrentTimeStart; //To add new execution
    private ITmfTimestamp fStartTimeNew; //To add new execution - keep the start
    private ITmfTimestamp fCurrentTimeEnd; //To add filter

    private PatternDialog fPatternDialog;

    private class StackbarsTreeLabelProvider extends TreeLabelProvider {

        @Override
        public String getColumnText(Object element, int columnIndex) {
            StackbarsEntry entry = (StackbarsEntry) element;
            if (COLUMN_NAMES[columnIndex].equals(COLUMN_RANK_ORDER)) {
                if(entry.getRankByStartingTime() == -1)
                {
                    return "";
                }
                return Integer.toString(entry.getRankByStartingTime() + 1);
            }
            else if (COLUMN_NAMES[columnIndex].equals(COLUMN_STARTING_TIME)) {
                if(entry.getRankByDuration() == -1)
                {
                    return "";
                }
                return TmfTimestampFormat.getDefaulIntervalFormat().format(entry.getRealStartTime());
            }
            else if (COLUMN_NAMES[columnIndex].equals(COLUMN_RANK_DURATION)) {
                if(entry.getRankByDuration() == -1)
                {
                    return "";
                }
                return Integer.toString(entry.getRankByDuration());
            }
            else if (COLUMN_NAMES[columnIndex].equals(COLUMN_ELAPSED)) {
                if(entry.getRankByDuration() == -1)
                {
                    return "";
                }
                return Long.toString(entry.getDuration());
            }
            else if (COLUMN_NAMES[columnIndex].equals(COLUMN_TID)) {
                if(entry.getTidStart() == -1)
                {
                    return Integer.toString(entry.getTidEnd());
                }
                else if(entry.getTidEnd() == -1)
                {
                    return Integer.toString(entry.getTidStart());
                }
                else if(entry.getTidStart() == entry.getTidEnd())
                {
                    return Integer.toString(entry.getTidStart());
                }
                return Integer.toString(entry.getTidStart()) + "-" + Integer.toString(entry.getTidEnd());
            }
            else if (COLUMN_NAMES[columnIndex].equals(COLUMN_NAME)) {
                return entry.getName();
            }
            else if (COLUMN_NAMES[columnIndex].equals(COLUMN_RUNNING_TIME)) {
                return Long.toString(entry.getRunningTime());
            }
            else if (COLUMN_NAMES[columnIndex].equals(COLUMN_PREEMPTED_TIME)) {
                return Long.toString(entry.getPreemptedTime());
            }
            return ""; //$NON-NLS-1$
        }

    }

    private class TimeGraphEntryComparatorElapsed implements Comparator<ITimeGraphEntry> {

        @Override
        public int compare(ITimeGraphEntry o1, ITimeGraphEntry o2) {

            if ((o1 instanceof StackbarsEntry) && (o2 instanceof StackbarsEntry)) {

                StackbarsEntry entry1 = (StackbarsEntry) o1;
                StackbarsEntry entry2 = (StackbarsEntry) o2;
                long res = entry2.getDuration() - entry1.getDuration();
                if(res != 0)
                {
                    return res > 0 ? 1 : -1;
                }
            }
            return 0;
        }
    }

    private class TimeGraphEntryComparatorStart implements Comparator<ITimeGraphEntry> {

        @Override
        public int compare(ITimeGraphEntry o1, ITimeGraphEntry o2) {

            if ((o1 instanceof StackbarsEntry) && (o2 instanceof StackbarsEntry)) {
                StackbarsEntry entry1 = (StackbarsEntry) o1;
                StackbarsEntry entry2 = (StackbarsEntry) o2;
                long res = entry1.getRealStartTime() - entry2.getRealStartTime();
                if(res != 0)
                {
                    return res > 0 ? 1 : -1;
                }
            }
            return 0;
        }
    }

    private class TimeGraphEntryComparatorRunning implements Comparator<ITimeGraphEntry> {

        @Override
        public int compare(ITimeGraphEntry o1, ITimeGraphEntry o2) {

            if ((o1 instanceof StackbarsEntry) && (o2 instanceof StackbarsEntry)) {
                StackbarsEntry entry1 = (StackbarsEntry) o1;
                StackbarsEntry entry2 = (StackbarsEntry) o2;
                long res = entry1.getRunningTime() - entry2.getRunningTime();
                if(res != 0)
                {
                    return res > 0 ? 1 : -1;
                }
            }
            return 0;
        }
    }

    private class TimeGraphEntryComparatorPreempted implements Comparator<ITimeGraphEntry> {

        @Override
        public int compare(ITimeGraphEntry o1, ITimeGraphEntry o2) {

            if ((o1 instanceof StackbarsEntry) && (o2 instanceof StackbarsEntry)) {
                StackbarsEntry entry1 = (StackbarsEntry) o1;
                StackbarsEntry entry2 = (StackbarsEntry) o2;
                long res = entry1.getPreemptedTime() - entry2.getPreemptedTime();
                if(res != 0)
                {
                    return res > 0 ? 1 : -1;
                }
            }
            return 0;
        }
    }

    private class TimeGraphEntryComparatorTid implements Comparator<ITimeGraphEntry> {

        @Override
        public int compare(ITimeGraphEntry o1, ITimeGraphEntry o2) {

            if ((o1 instanceof StackbarsEntry) && (o2 instanceof StackbarsEntry)) {
                StackbarsEntry entry1 = (StackbarsEntry) o1;
                StackbarsEntry entry2 = (StackbarsEntry) o2;
                if(entry1.getTidStart() > entry2.getTidStart())
                {
                    return 1;
                }
                if(entry2.getTidStart() > entry1.getTidStart())
                {
                    return -1;
                }
                long res = entry2.getDuration() - entry1.getDuration();
                if(res != 0)
                {
                    return res > 0 ? 1 : -1;
                }
            }
            return 0;
        }
    }

    private void setColumnsStackbars()
    {
        SelectionAdapter selectionListenerElapsed = new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent e) {

                Tree tree = getTimeGraphCombo().getTreeViewer().getTree();
                final Comparator<ITimeGraphEntry> ascendindComparator = new TimeGraphEntryComparatorElapsed();
                final Comparator<ITimeGraphEntry> reverseComparator = new Comparator<ITimeGraphEntry>() {
                    @Override
                    public int compare(ITimeGraphEntry o1, ITimeGraphEntry o2) {

                        return -1 * ascendindComparator.compare(o1, o2);
                    }
                };

                if (tree.getSortDirection() == SWT.DOWN) {
                    /*
                     * Puts the ascendant ordering if the selected
                     * column hasn't changed.
                     */
                    setEntryComparator(ascendindComparator);
                    tree.setSortDirection(SWT.UP);
                } else {
                    /*
                     * Puts the descendant order if the old order was up
                     * or if the selected column has changed.
                     */
                    setEntryComparator(reverseComparator);
                    tree.setSortDirection(SWT.DOWN);
                }

                refresh();
            }
        };

        SelectionAdapter selectionListenerStart = new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent e) {

                Tree tree = getTimeGraphCombo().getTreeViewer().getTree();
                final Comparator<ITimeGraphEntry> ascendindComparator = new TimeGraphEntryComparatorStart();
                final Comparator<ITimeGraphEntry> reverseComparator = new Comparator<ITimeGraphEntry>() {
                    @Override
                    public int compare(ITimeGraphEntry o1, ITimeGraphEntry o2) {

                        return -1 * ascendindComparator.compare(o1, o2);
                    }
                };

                if (tree.getSortDirection() == SWT.DOWN) {
                    /*
                     * Puts the ascendant ordering if the selected
                     * column hasn't changed.
                     */
                    setEntryComparator(ascendindComparator);
                    tree.setSortDirection(SWT.UP);
                } else {
                    /*
                     * Puts the descendant order if the old order was up
                     * or if the selected column has changed.
                     */
                    setEntryComparator(reverseComparator);
                    tree.setSortDirection(SWT.DOWN);
                }

                refresh();
            }
        };

        SelectionAdapter selectionListenerRunning = new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent e) {

                Tree tree = getTimeGraphCombo().getTreeViewer().getTree();
                final Comparator<ITimeGraphEntry> ascendindComparator = new TimeGraphEntryComparatorRunning();
                final Comparator<ITimeGraphEntry> reverseComparator = new Comparator<ITimeGraphEntry>() {
                    @Override
                    public int compare(ITimeGraphEntry o1, ITimeGraphEntry o2) {

                        return -1 * ascendindComparator.compare(o1, o2);
                    }
                };

                if (tree.getSortDirection() == SWT.DOWN) {
                    /*
                     * Puts the ascendant ordering if the selected
                     * column hasn't changed.
                     */
                    setEntryComparator(ascendindComparator);
                    tree.setSortDirection(SWT.UP);
                } else {
                    /*
                     * Puts the descendant order if the old order was up
                     * or if the selected column has changed.
                     */
                    setEntryComparator(reverseComparator);
                    tree.setSortDirection(SWT.DOWN);
                }

                refresh();
            }
        };

        SelectionAdapter selectionListenerPreempted = new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent e) {

                Tree tree = getTimeGraphCombo().getTreeViewer().getTree();
                final Comparator<ITimeGraphEntry> ascendindComparator = new TimeGraphEntryComparatorPreempted();
                final Comparator<ITimeGraphEntry> reverseComparator = new Comparator<ITimeGraphEntry>() {
                    @Override
                    public int compare(ITimeGraphEntry o1, ITimeGraphEntry o2) {

                        return -1 * ascendindComparator.compare(o1, o2);
                    }
                };

                if (tree.getSortDirection() == SWT.DOWN) {
                    /*
                     * Puts the ascendant ordering if the selected
                     * column hasn't changed.
                     */
                    setEntryComparator(ascendindComparator);
                    tree.setSortDirection(SWT.UP);
                } else {
                    /*
                     * Puts the descendant order if the old order was up
                     * or if the selected column has changed.
                     */
                    setEntryComparator(reverseComparator);
                    tree.setSortDirection(SWT.DOWN);
                }

                refresh();
            }
        };

        SelectionAdapter selectionListenerTid = new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent e) {

                Tree tree = getTimeGraphCombo().getTreeViewer().getTree();
                final Comparator<ITimeGraphEntry> ascendindComparator = new TimeGraphEntryComparatorTid();
                final Comparator<ITimeGraphEntry> reverseComparator = new Comparator<ITimeGraphEntry>() {
                    @Override
                    public int compare(ITimeGraphEntry o1, ITimeGraphEntry o2) {

                        return -1 * ascendindComparator.compare(o1, o2);
                    }
                };

                if (tree.getSortDirection() == SWT.DOWN) {
                    /*
                     * Puts the ascendant ordering if the selected
                     * column hasn't changed.
                     */
                    setEntryComparator(ascendindComparator);
                    tree.setSortDirection(SWT.UP);
                } else {
                    /*
                     * Puts the descendant order if the old order was up
                     * or if the selected column has changed.
                     */
                    setEntryComparator(reverseComparator);
                    tree.setSortDirection(SWT.DOWN);
                }

                refresh();
            }
        };

        final SelectionAdapter[] selectionAdapters = new SelectionAdapter[] {
                selectionListenerStart,
                selectionListenerStart,
                selectionListenerElapsed,
                selectionListenerElapsed,
                selectionListenerTid,
                selectionListenerTid,
                selectionListenerRunning,
                selectionListenerPreempted
        };

        setTreeColumns(COLUMN_NAMES, selectionAdapters);
    }

    /**
     * Constructor
     */
    public StackbarsView() {
        super(ID, new StackbarsPresentationProvider());
        setColumnsStackbars();
        setFilterColumns(FILTER_COLUMN_NAMES);
        setTreeLabelProvider(new StackbarsTreeLabelProvider());
        setEntryComparator(new TimeGraphEntryComparatorElapsed());
        fStackbarsAnalysis = StackbarsAnalysis.getInstance();
        fStackbarsAnalysis.setView(this);
        fParseExecutionsOnCFVSelection = true;

        //Listener
        final IWorkbench wb = PlatformUI.getWorkbench();
        final IWorkbenchPage activePage = wb.getActiveWorkbenchWindow().getActivePage();
        IViewPart view = activePage.findView(ControlFlowView.ID);
        ISelectionListener selListenerCF = new ISelectionListener() {
          @Override
          public void selectionChanged(IWorkbenchPart part, ISelection selection) {
              if (selection instanceof IStructuredSelection) {
                  Object element = ((IStructuredSelection) selection).getFirstElement();
                  if (element instanceof ControlFlowEntry) {
                      ControlFlowEntry entry = (ControlFlowEntry) element;
                      fCurrentTid = entry.getThreadId();
                      if(fParseExecutionsOnCFVSelection)
                      {
                          executeExecutionsDetection();
                      }
                  }
              }
          }
      };
        if (view != null) {
            view.getSite().getWorkbenchWindow().getSelectionService().addPostSelectionListener(selListenerCF);
        }

        //Select event dialog
        Shell shell = PlatformUI.getWorkbench().getActiveWorkbenchWindow().getShell();
        if(fSelectEventsDialog == null)
        {
            fSelectEventsDialog = new StackbarsSelectEventsDialog(shell);
            fExecDef = new StackbarsExecutionsDetection.ExecDefinitionSameTid();
            fExecDef.fBorderEventsByDepth.add(fSelectEventsDialog.getDefaultBorderEvents());
        }
        fStackbarsAnalysis.defineBorderEvents(fExecDef);

        //Pattern dialog
        if(fPatternDialog == null)
        {
            fPatternDialog = new PatternDialog(shell, new Observer(){
                @Override
                public void update(Observable arg0, Object arg1) {
                    executePatternDetection(fPatternDialog.getIfSchedSwitchMode());
                }

            });
        }
    }

//    private static int getMatchingState(int currentEventState, int nextEventState) {
//        StackbarsPresentationProvider.State state = StackbarsPresentationProvider.State.UNKNOWN;
//        switch (currentEventState) {
//
//        // Start with start event
//        case StackbarsStateValues.STATUS_AFTER_START:
//            {
//                switch (nextEventState) {
//                case StackbarsStateValues.STATUS_AFTER_END:
//                case StackbarsStateValues.STATUS_AFTER_SCHED_SWITCH_PREV:
//                {
//                    state = StackbarsPresentationProvider.State.RUNNING;
//                    break;
//                }
//                case StackbarsStateValues.STATUS_AFTER_SCHED_SWITCH_NEXT:
//                {
//                    state = StackbarsPresentationProvider.State.READY;
//                    break;
//                }
//                /*case StackbarsStateValues.STATUS_AFTER_START:
//                {
//                    return -1;
//                }*/
//                default:
//                    break;
//                }
//            }
//            break;
//
//        // Start with end event
//        case StackbarsStateValues.STATUS_AFTER_END:
//            return -1;
//
//         // Start with wake event
//        case StackbarsStateValues.STATUS_AFTER_SCHED_WAKE:
//            {
//                switch (nextEventState) {
//                case StackbarsStateValues.STATUS_AFTER_SCHED_SWITCH_NEXT:
//                {
//                    state = StackbarsPresentationProvider.State.READY;
//                    break;
//                }
//                /*case StackbarsStateValues.STATUS_AFTER_START:
//                {
//                    return -1;
//                }*/
//                default:
//                    break;
//                }
//            }
//            break;
//
//        // Start with sched prev
//        case StackbarsStateValues.STATUS_AFTER_SCHED_SWITCH_PREV:
//            {
//                switch (nextEventState) {
//                /*case StackbarsStateValues.STATUS_AFTER_WAKE:
//                case StackbarsStateValues.STATUS_AFTER_START:
//                {
//                    return -1;
//                }*/
//                case StackbarsStateValues.STATUS_AFTER_SCHED_SWITCH_NEXT:
//                case StackbarsStateValues.STATUS_AFTER_END:
//                {
//                    state = StackbarsPresentationProvider.State.BLOCKED_OR_PREEMPTED;
//                    break;
//                }
//                default:
//                    break;
//                }
//            }
//            break;
//
//        // Start with sched next
//        case StackbarsStateValues.STATUS_AFTER_SCHED_SWITCH_NEXT:
//            {
//                switch (nextEventState) {
//                case StackbarsStateValues.STATUS_AFTER_START:
//                case StackbarsStateValues.STATUS_AFTER_SCHED_SWITCH_PREV:
//                case StackbarsStateValues.STATUS_AFTER_END:
//                {
//                    state = StackbarsPresentationProvider.State.RUNNING;
//                    break;
//                }
//                default:
//                    break;
//                }
//            }
//            break;
//
//        default:
//            break;
//        }
//        return state.ordinal();
//    }

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

    @Override
    protected List<ILinkEvent> getLinkList(long startTime, long endTime, long resolution, IProgressMonitor monitor) {
        return null;

        /*List<ILinkEvent> linksInRange = new ArrayList<>();
        for (ILinkEvent link : null) {
            if (((link.getTime() >= startTime) && (link.getTime() <= endTime)) ||
                    ((link.getTime() + link.getDuration() >= startTime) && (link.getTime() + link.getDuration() <= endTime))) {
                linksInRange.add(link);
            }
        }
        return linksInRange;*/
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
                if (event.getSelection() instanceof StackbarsEntry) {
                    sendTimeSyncSignalAndSetExecution(event.getSelection());
                }
                else
                {
                    fStackbarsAnalysis.setCurrentExecution(null);
                }
            }
        };
        getTimeGraphCombo().addSelectionListener(listener);

        //End "where to put that?"

        fShowDeadline = true;

        // Show deadline
        Action showDeadline = new Action() {
            @Override
            public void run() {

                fShowDeadline = !fShowDeadline;
                updateDeadline();
            }
        };
        showDeadline.setToolTipText("Show a line at the deadline (when defined)");
        showDeadline.setImageDescriptor(PlatformUI.getWorkbench().getSharedImages().
        getImageDescriptor(ISharedImages.IMG_TOOL_UP));
        showDeadline.setChecked(true);
        manager.add(showDeadline);

        // Parse automatically
        Action detectAutomatically = new Action() {
            @Override
            public void run() {
                fParseExecutionsOnCFVSelection = !fParseExecutionsOnCFVSelection;
            }
        };
        detectAutomatically.setToolTipText("Lauch the executions detection automatically on control flow view selection");
        detectAutomatically.setImageDescriptor(PlatformUI.getWorkbench().getSharedImages().
        getImageDescriptor(ISharedImages.IMG_TOOL_COPY));
        detectAutomatically.setChecked(true);
        manager.add(detectAutomatically);

        // Execute custom analysis
        /*Action executeAnalysisRelated = new Action() {
            @Override
            public void run() {
                Job j = new Job("RT_Analysis"){
                    @Override
                    protected IStatus run(final IProgressMonitor monitor) {

                        if(monitor.isCanceled()){
                            return Status.CANCEL_STATUS;
                        }
                        executeAnalysisRelated(monitor);

                        return Status.OK_STATUS;

                    }
                };
                j.schedule();
            }
        };
        executeAnalysisRelated.setToolTipText("Show related threads");
        executeAnalysisRelated.setImageDescriptor(PlatformUI.getWorkbench().getSharedImages().
        getImageDescriptor(ISharedImages.IMG_TOOL_REDO));
        manager.add(executeAnalysisRelated);*/

        // Execute custom analysis
        Action executeAnalysisStats = new Action() {
            @Override
            public void run() {
                Job j = new Job("RT_Analysis"){
                    @Override
                    protected IStatus run(final IProgressMonitor monitor) {

                        // Priority Inversion
                        if(monitor.isCanceled()){
                            return Status.CANCEL_STATUS;
                        }
                        executeAnalysisStats(monitor);

                        return Status.OK_STATUS;

                    }
                };
                j.schedule();
            }
        };
        executeAnalysisStats.setToolTipText("Show the stats");
        executeAnalysisStats.setImageDescriptor(PlatformUI.getWorkbench().getSharedImages().
        getImageDescriptor(ISharedImages.IMG_TOOL_REDO));
        manager.add(executeAnalysisStats);

        // Execute custom analysis
        Action executePattern = new Action() {
            @Override
            public void run() {
                fPatternDialog.open();
                if(fPatternDialog.getIfLoadNeeded())
                {
                    ExecDefinition execDef = new StackbarsExecutionsDetection.ExecDefinitionSameTid();
                    BorderEvents bEvents = new BorderEvents();
                    execDef.fBorderEventsByDepth.add(bEvents);
                    for(String eventName : fPatternDialog.getPattern())
                    {
                        bEvents.fEventDefinitions.add(new EventDefinition(eventName,""));
                    }

                    openSelectEventsDialog(execDef);
                }
            }
        };
        executePattern.setToolTipText("Open pattern discovery dialog");
        executePattern.setImageDescriptor(PlatformUI.getWorkbench().getSharedImages().
        getImageDescriptor(ISharedImages.IMG_TOOL_REDO));
        manager.add(executePattern);

        // Dialog to chose the events
        Action eventsChoice = new Action() {
            @Override
            public void run() {
                openSelectEventsDialog(fExecDef);
            }
        };
        eventsChoice.setToolTipText("Select the events that define begining and end of the execution");
        eventsChoice.setImageDescriptor(PlatformUI.getWorkbench().getSharedImages().
        getImageDescriptor(ISharedImages.IMG_TOOL_NEW_WIZARD));

        manager.add(eventsChoice);

     // Dialog to compare executions
        Action compareExecutions = new Action() {
            @Override
            public void run() {
                Shell shell = PlatformUI.getWorkbench().getActiveWorkbenchWindow().getShell();
                StackbarsCompareExecutionDialog dialog = new StackbarsCompareExecutionDialog(shell);
                dialog.open();
                String execId = dialog.getExecutionId();
                if(execId != "-1")
                {
                    //executeAnalysisComparison(execId); TODO
                }
            }
        };
        compareExecutions.setToolTipText("Compare executions");
        compareExecutions.setImageDescriptor(PlatformUI.getWorkbench().getSharedImages().
                getImageDescriptor(ISharedImages.IMG_TOOL_CUT));

        manager.add(compareExecutions);

        // Action to detect executions
        Action detectExecutions = new Action() {
            @Override
            public void run() {
                executeExecutionsDetection();
            }
        };
        detectExecutions.setToolTipText("Detect executions");
        detectExecutions.setImageDescriptor(PlatformUI.getWorkbench().getSharedImages().
                getImageDescriptor(ISharedImages.IMG_TOOL_PASTE));

        manager.add(detectExecutions);

        // Action to remove execution
        Action removeExecution = new Action() {
            @Override
            public void run() {
                executeRemoveSelectedExecution();
            }
        };
        removeExecution.setToolTipText("Remove only the selected execution");
        removeExecution.setImageDescriptor(PlatformUI.getWorkbench().getSharedImages().
                getImageDescriptor(ISharedImages.IMG_ELCL_REMOVE));

        manager.add(removeExecution);

        // Action to add invalid execution (and remove all similar)
        Action addInvalidExecution = new Action() {
            @Override
            public void run() {
                executeAddInvalidExecution();
            }
        };
        addInvalidExecution.setToolTipText("Remove all executions similar to the one selected");
        addInvalidExecution.setImageDescriptor(PlatformUI.getWorkbench().getSharedImages().
                getImageDescriptor(ISharedImages.IMG_ELCL_REMOVEALL));

        manager.add(addInvalidExecution);

        // Action to add new  execution - start
        Action defineStartNewExecution = new Action() {
            @Override
            public void run() {
                fStartTimeNew = fCurrentTimeStart;
            }
        };
        defineStartNewExecution.setToolTipText("Define the start event of a new valid execution");
        defineStartNewExecution.setImageDescriptor(PlatformUI.getWorkbench().getSharedImages().
                getImageDescriptor(ISharedImages.IMG_ETOOL_HOME_NAV));

        manager.add(defineStartNewExecution);

        // Action to add new  execution - end
        Action defineEndNewExecution = new Action() {
            @Override
            public void run() {
                executeAddExecution();  //TODO check select event
            }
        };
        defineEndNewExecution.setToolTipText("Define the end event of a new valid execution");
        defineEndNewExecution.setImageDescriptor(PlatformUI.getWorkbench().getSharedImages().
                getImageDescriptor(ISharedImages.IMG_ETOOL_SAVE_EDIT));

        manager.add(defineEndNewExecution);

        // Action to select a valid execution
        Action selectValidExecution = new Action() {
            @Override
            public void run() {
                executeSelectValidExecution();
            }
        };
        selectValidExecution.setToolTipText("Set the selected execution as valid");
        selectValidExecution.setImageDescriptor(PlatformUI.getWorkbench().getSharedImages().
                getImageDescriptor(ISharedImages.IMG_ELCL_COLLAPSEALL));

        manager.add(selectValidExecution);

        // Action to add new  execution
        Action editValidExecutions = new Action() {
            @Override
            public void run() {
                executeAddExecution(); //TODO
            }
        };
        editValidExecutions.setToolTipText("Open dialog to edit valid executions");
        editValidExecutions.setImageDescriptor(PlatformUI.getWorkbench().getSharedImages().
                getImageDescriptor(ISharedImages.IMG_ETOOL_SAVEALL_EDIT));

        manager.add(editValidExecutions);

        // Action to add new  execution
        Action resetEventsDefinition = new Action() {
            @Override
            public void run() {
                executeResetModel(); //TODO
                updateDeadline();
            }
        };
        resetEventsDefinition.setToolTipText("Reset the execution model");
        resetEventsDefinition.setImageDescriptor(PlatformUI.getWorkbench().getSharedImages().
                getImageDescriptor(ISharedImages.IMG_ELCL_COLLAPSEALL));

        manager.add(resetEventsDefinition);

        // Go lower
        fActionDepthLower = new Action() {
            @Override
            public void run() {
                executeGoLower();
            }
        };
        fActionDepthLower.setToolTipText("Go lower");
        fActionDepthLower.setImageDescriptor(PlatformUI.getWorkbench().getSharedImages().
        getImageDescriptor(ISharedImages.IMG_TOOL_FORWARD));
        fActionDepthLower.setEnabled(false); //A line must be selected

        manager.add(fActionDepthLower);

        // Go upper
        fActionDepthUpper = new Action() {
            @Override
            public void run() {
                executeGoUpper();
            }
        };
        fActionDepthUpper.setToolTipText("Go upper");
        fActionDepthUpper.setImageDescriptor(PlatformUI.getWorkbench().getSharedImages().
        getImageDescriptor(ISharedImages.IMG_TOOL_BACK));
        fActionDepthUpper.setEnabled(false); //We are at the upper depth when start

        manager.add(fActionDepthUpper);

        super.fillLocalToolBar(manager);
    }

    protected void openSelectEventsDialog(ExecDefinition def) {

        fSelectEventsDialog.open(def);
        if(fSelectEventsDialog.getIfChangesMadeAndReset())
        {
            fExecDef = fSelectEventsDialog.getExecDef();
            fStackbarsAnalysis.defineBorderEvents(fExecDef);
            if(fSelectEventsDialog.getIfTimeFilterAndReset() && fCurrentTimeStart != null && fCurrentTimeEnd != null)
            {
                Vector<StackbarsFilter> filters = new Vector<>();

                StackbarsFilter filter = new StackbarsFilter();
                filter.setType(StackbarsFilterType.MAX_START_TIME);
                filter.setValue(fCurrentTimeEnd.getValue());
                filters.add(filter);

                filter = new StackbarsFilter();
                filter.setType(StackbarsFilterType.MIN_START_TIME);
                filter.setValue(fCurrentTimeStart.getValue());

                filters.add(filter);
                fStackbarsAnalysis.addNewFiltersAtDepth(filters,StackbarsSelectEventsDialog.START_DEPTH);
            }
            updateDeadline();
        }

    }

    protected void sendTimeSyncSignalAndSetExecution(Object element) {
        StackbarsEntry stackbarsEntry = (StackbarsEntry) element;
        fStackbarsAnalysis.setCurrentExecution(stackbarsEntry);

        long duration = stackbarsEntry.getDuration();

        ITmfTimestamp startTime = new TmfTimestamp(stackbarsEntry.getRealStartTime(), ITmfTimestamp.NANOSECOND_SCALE);
        ITmfTimestamp endTime = new TmfTimestamp(startTime.getValue() + duration, ITmfTimestamp.NANOSECOND_SCALE);

        //Send signal for time
        TmfTimeRange range = new TmfTimeRange(new TmfTimestamp(startTime.getValue() - duration,
                ITmfTimestamp.NANOSECOND_SCALE), new TmfTimestamp(endTime.getValue() + duration,
                        ITmfTimestamp.NANOSECOND_SCALE));
        TmfSignalManager.dispatchSignal(new TmfWindowRangeUpdatedSignal(this, range));
        TmfSelectionRangeUpdatedSignal signal = new TmfSelectionRangeUpdatedSignal(this, startTime, endTime);
        TmfSignalManager.dispatchSignal(signal);

        //Send signal for tid
        TmfTidSynchSignal signalTid = new TmfTidSynchSignal(this, stackbarsEntry.getTidStart());
        TmfSignalManager.dispatchSignal(signalTid);

    }

    protected void updateDeadline() {
        TimeGraphControl control = getTimeGraphViewer().getTimeGraphControl();
        if(control instanceof TimeGraphControlStackbars)
        {
            long deadline = StackbarsAnalysis.NO_DEADLINE;
            if(fShowDeadline)
            {
                deadline = fStackbarsAnalysis.getCurrentDeadline();//StackbarsModelProvider.getInstance().getDeadlines().get(fCurrentDepth);
            }
            ((TimeGraphControlStackbars)control).setDeadline(deadline); //-1 case check lower
        }
    }

    @Override
    protected void buildEventList(ITmfTrace trace, ITmfTrace parentTrace, IProgressMonitor monitor) {

      if (monitor.isCanceled()) {
          return;
      }

      setStartTime(0);
      setEndTime(0);

      List<TimeGraphEntry> listTimeGraphEntry = null;

      if(trace == null)
      {
          return;
      }

      if(fStackbarsAnalysis != null)
      {

          listTimeGraphEntry = fStackbarsAnalysis.getHeadExecutions(trace, monitor);

          if(listTimeGraphEntry != null)
          {
              ArrayList<Double> xTimeView = new ArrayList<>();
              ArrayList<Double> yTimeView = new ArrayList<>();

              for(TimeGraphEntry tEntry: listTimeGraphEntry)
              {
                  if(tEntry instanceof StackbarsEntry)
                  {
                      StackbarsEntry sEntry = (StackbarsEntry) tEntry;
                      //Time view
                      xTimeView.add((double)sEntry.getRealStartTime());
                      yTimeView.add((double)sEntry.getDuration());
                      setEndTime(Math.max(getEndTime(), sEntry.getDuration()));

                      // Compile the running and preempted time for multi-tid executions
                      if(sEntry.hasChildren())
                      {
                          long premptTime = 0;
                          long runningTime = 0;
                          for(ITimeGraphEntry cEntry : sEntry.getChildren())
                          {
                              if(cEntry instanceof StackbarsEntry)
                              {
                                  premptTime += ((StackbarsEntry)cEntry).getPreemptedTime();
                                  runningTime += ((StackbarsEntry)cEntry).getRunningTime();
                              }
                          }
                          sEntry.setPreemptedTime(premptTime);
                          sEntry.setRunningTime(runningTime);
                      }
                  }
              }

              TmfSignalManager.dispatchSignal(new TmfDataUpdatedSignal(
                      this,
                      new TimeView.StackbarsChartSeries(xTimeView,yTimeView,fStackbarsAnalysis.getCurrentDeadline())));

          }



          /*
          Vector<StackbarsExecution> vector = fStackbarsAnalysis.getHeadExecutions();

          if(vector != null)
          {

              ArrayList<Double> xTimeView = new ArrayList<>();
              ArrayList<Double> yTimeView = new ArrayList<>();

              int indexExecution = 0;
              for(StackbarsExecution exec : vector)
              {
                  Vector<StateTimeSource> eventList = exec.getStates();
                  long initTime = 0;
                  long endTime = 0;
                  long durationTotal;
                  long runningTime = 0;
                  StackbarsEntry entry = null;
                  ArrayList<TimeEvent> listEventTemp = new ArrayList<>();

                  boolean entryInitialized = false;
                  int listSize = eventList.size();

                  if (listSize > 1)
                  {
                      StateTimeSource currentEvent = null;
                      StateTimeSource nextEvent = eventList.get(0);

                      for(int i = 1; i < listSize; ++i)
                      {
                          currentEvent = nextEvent;
                          nextEvent = eventList.get(i);
                          if (!entryInitialized)
                          {
                              initTime = currentEvent.getTime();
                              entryInitialized = true;
                          }

                          if(currentEvent == null || nextEvent == null)
                          {
                              continue;
                          }

                          long time = currentEvent.getTime() - initTime;
                          endTime = nextEvent.getTime();
                          long duration = endTime - currentEvent.getTime();
                          int state = getMatchingState(currentEvent.getState(),
                                  nextEvent.getState());
                          if(state != -1) {
                              TimeEvent event = new TimeEvent(entry, time, duration, state);
                              listEventTemp.add(event);
                              if (state == StackbarsPresentationProvider.State.RUNNING.ordinal())
                              {
                                  runningTime += duration;
                              }
                          }

                      }

                      if (listEventTemp.size() != 0)
                      {
                          durationTotal = endTime - initTime;
                          entry = new StackbarsEntry(Integer.toString(++indexExecution), trace, initTime,
                                  durationTotal, exec.getTid(), 0, runningTime);

                          setEndTime(Math.max(getEndTime(), durationTotal));
                          for (TimeEvent event : listEventTemp)
                          {
                              entry.addEvent(event);
                          }
                          listTimeGraphEntry.add(entry);

                          //Time view
                          xTimeView.add((double)entry.getRealStartTime());
                          yTimeView.add((double)durationTotal);
                      }
                  }

              }

              TmfSignalManager.dispatchSignal(new TmfDataUpdatedSignal(
                      this,
                      new TimeView.StackbarsChartSeries(xTimeView,yTimeView,fStackbarsAnalysis.getCurrentDeadline())));

              Collections.sort(listTimeGraphEntry, new TimeGraphEntryComparatorElapsed());

              for (int rank = 1; rank <= listTimeGraphEntry.size(); ++rank)
              {
                  ((StackbarsEntry) listTimeGraphEntry.get(rank-1)).setRankByDuration(rank); //TODO change that scrap to start the rank at 0 everywhere except in the display
              }
          }
      */
      }



      if(listTimeGraphEntry == null)
      {
          listTimeGraphEntry = new ArrayList<>();
      }

      Collections.sort(listTimeGraphEntry, new TimeGraphEntryComparatorElapsed());

      for (int rank = 1; rank <= listTimeGraphEntry.size(); ++rank)
      {
          ((StackbarsEntry) listTimeGraphEntry.get(rank-1)).setRankByDuration(rank);
      }

      putEntryList(trace, listTimeGraphEntry);
      refresh();
    }

    /**
     * This will rebuild the time graph
     */
    public void notifyBuildISFinish()
    {
        rebuild();
    }

    private void executeGoUpper()
    {
        boolean needRebuild = fStackbarsAnalysis.executeGoUpper(fActionDepthLower, fActionDepthUpper);
        if(needRebuild)
        {
            rebuild();
        }
    }

    private void executeGoLower()
    {
        boolean needRebuild = fStackbarsAnalysis.executeGoLower(getTimeGraphViewer().getTimeGraphControl(),
                fActionDepthLower, fActionDepthUpper);
        if(needRebuild)
        {
            rebuild();
        }
    }

    private void executeAnalysisStats(IProgressMonitor monitor)
    {
        ITmfTrace trace = getTrace();
        fStackbarsAnalysis.executeAnalysisStats(monitor, trace);
    }

    private void executePatternDetection(boolean sched_switchMode)
    {
        ITmfTrace trace = getTrace();
        StackbarsAnalysis.executeAnalysisPI(trace);
        HashSet<String> startNames = new HashSet<>();
        if(sched_switchMode)
        {
            startNames.add(LttngStrings.SCHED_SWITCH);
        }
        StackbarsAnalysis.executePatternDetection(trace, fCurrentTid, startNames );
    }

    private void executeExecutionsDetection()
    {
        fStackbarsAnalysis.findExecutions(getTrace(), fCurrentTid);
    }

    private void executeRemoveSelectedExecution()
    {
        TimeGraphControl control = getTimeGraphViewer().getTimeGraphControl();
        if(control instanceof TimeGraphControlStackbars)
        {
            int executionId = fStackbarsAnalysis.getExecutionId();
            if(executionId < 0)
            {
                return;
            }
            fStackbarsAnalysis.executeRemoveExecution(executionId); //From 0
            rebuild();
        }
    }

    private void executeAddInvalidExecution()
    {
        TimeGraphControl control = getTimeGraphViewer().getTimeGraphControl();
        if(control instanceof TimeGraphControlStackbars)
        {
            final int executionId = fStackbarsAnalysis.getExecutionId();
            if(executionId <= 0)
            {
                return;
            }

            Job job = new Job("Comparing executions") { //$NON-NLS-1$

                @Override
                protected IStatus run(final IProgressMonitor monitor) {

                    fStackbarsAnalysis.getPossibleFilters(executionId - 1, getTrace(),monitor); //From 0

                    if (monitor.isCanceled()) {
                        return Status.CANCEL_STATUS;
                    }
                    return Status.OK_STATUS;
                }

            };
            //job.setSystem(true);
            job.setPriority(Job.SHORT);
            job.schedule();
        }
    }



    private void notifyFiltersReady(final Vector<StackbarsExecutionsDetection.StackbarsFilter> filters)
    {
        UIJob job = new UIJob("Show filters") {

            @Override
            public IStatus runInUIThread(IProgressMonitor monitor) {
                if(PlatformUI.getWorkbench().getActiveWorkbenchWindow() == null)
                {
                    return Status.OK_STATUS;
                }

                Shell shell = PlatformUI.getWorkbench().getActiveWorkbenchWindow().getShell();
                if(filters == null || filters.size() == 0)
                {
                    MessageDialog.openInformation(shell, "Information", "There are no valid filter with your current valid executions. Try to edit them.");
                }
                else
                {
                    StackbarsShowFiltersDialog dialog = new StackbarsShowFiltersDialog(shell);
                    dialog.open(filters);
                    Vector<StackbarsExecutionsDetection.StackbarsFilter> selectedFilters = dialog.getSelectedFilters();
                    if(selectedFilters != null && selectedFilters.size() != 0)
                    {
                        fStackbarsAnalysis.addNewFilters(filters);
                        rebuild();
                    }
                }
                return Status.OK_STATUS;
            }
        };
        job.schedule();
    }

    private void executeResetModel()
    {
        fExecDef = new StackbarsExecutionsDetection.ExecDefinitionSameTid();
        fExecDef.fBorderEventsByDepth.add(fSelectEventsDialog.getDefaultBorderEvents());
        fStackbarsAnalysis.defineBorderEvents(fExecDef);
    }

    private void executeAddExecution()
    {
        if(fStartTimeNew != null && fCurrentTimeStart != null)
        {
            boolean modif = fStackbarsAnalysis.executeAddValidExecution(fCurrentTid, getTrace(), (TmfTimestamp)fStartTimeNew, (TmfTimestamp)fCurrentTimeStart);
            if(modif)
            {
                rebuild();
            }
        }
    }

    private void executeSelectValidExecution()
    {
        TimeGraphControl control = getTimeGraphViewer().getTimeGraphControl();
        if(control instanceof TimeGraphControlStackbars)
        {
            int executionId = fStackbarsAnalysis.getExecutionId();
            if(executionId <= 0)
            {
                return;
            }
            fStackbarsAnalysis.executeSelectValidExecution(executionId);
        }
    }

    @TmfSignalHandler
    public void dataUpdated(final TmfDataUpdatedSignal signal) {

        //TODO getSource() instanceof TimeView doesn't work...
        if(signal.getData() instanceof Integer)
        {
            Integer rank = (Integer) signal.getData();
            selectALine(rank);
        }

        //TODO getSource() instanceof StackbarsAnalysis doesn't work...
        if(signal.getData() instanceof Vector<?>)
        {
            Vector<StackbarsExecutionsDetection.StackbarsFilter> filters = (Vector<StackbarsExecutionsDetection.StackbarsFilter>) signal.getData();
            notifyFiltersReady(filters);
        }

        //TODO getSource() instanceof StackbarsAnalysis doesn't work...
        if(signal.getSource() instanceof StackbarsAnalysis && signal.getData() instanceof StackbarsAnalysis)
        {
            UIJob updateJob = new UIJob("") {
                @Override
                public IStatus runInUIThread(IProgressMonitor monitor) {
                    fPatternDialog.updateResults(StackbarsAnalysis.getInstance().getAndResetResults(),
                            StackbarsAnalysis.getInstance().getAndResetDictio());
                    return Status.OK_STATUS;
                }
            };
            updateJob.schedule();
        }
    }

    @TmfSignalHandler
    public void synchToTime(final TmfSelectionRangeUpdatedSignal signal) {

        fCurrentTimeStart = signal.getBeginTime();
        fCurrentTimeEnd = signal.getEndTime();
        StackbarsAnalysis.getInstance().setStartTime(fCurrentTimeStart);
        StackbarsAnalysis.getInstance().setEndTime(fCurrentTimeEnd);
    }

    private void selectALine(int rankFrom0)
    {
        ITimeGraphEntry selection = getEntry(rankFrom0);
        getTimeGraphCombo().setSelection(selection);
        (getTimeGraphViewer().getTimeGraphControl()).treeViewerSelection(selection);
        if(selection instanceof StackbarsEntry)
        {
            sendTimeSyncSignalAndSetExecution(selection);
        }
    }

    private TimeGraphEntry getEntry(int rankFrom0)
    {
        List<TimeGraphEntry> listEntry = getEntryList(getTrace());
        if (listEntry != null && listEntry.size() > rankFrom0)
        {
            TimeGraphEntry entryTest = listEntry.get(rankFrom0);
            if(entryTest instanceof StackbarsEntry)
            {
              //Check if in order
                if(((StackbarsEntry)entryTest).getRankByStartingTime() == rankFrom0)
                {
                    return entryTest;
                }
            }

            //else //TODO array with order -> rank ?
            for(TimeGraphEntry entry : listEntry)
            {
                if(entry instanceof StackbarsEntry)
                {
                    if(((StackbarsEntry)entry).getRankByStartingTime() == rankFrom0)
                    {
                        return entry;
                    }
                }
            }
        }
        return null;
    }
}
