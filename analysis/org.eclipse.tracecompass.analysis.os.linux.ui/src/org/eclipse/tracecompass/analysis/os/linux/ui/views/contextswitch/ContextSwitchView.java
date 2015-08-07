/*******************************************************************************
 * Copyright (c) 2015 Ericsson Canada
 *
 * All rights reserved. This program and the accompanying materials are
 * made available under the terms of the Eclipse Public License v1.0 which
 * accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *   Alexis Cabana-Loriaux - Initial API and implementation
 *******************************************************************************/

package org.eclipse.tracecompass.analysis.os.linux.ui.views.contextswitch;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jdt.annotation.NonNull;
import org.eclipse.tracecompass.analysis.os.linux.core.contextswitch.KernelContextSwitchAnalysis;
import org.eclipse.tracecompass.analysis.os.linux.core.kernelanalysis.Attributes;
import org.eclipse.tracecompass.analysis.os.linux.ui.views.contextswitch.ContextSwitchTimeEvent.ContextSwitchRate;
import org.eclipse.tracecompass.analysis.os.linux.ui.views.resources.ResourcesEntry;
import org.eclipse.tracecompass.analysis.os.linux.ui.views.resources.ResourcesEntry.Type;
import org.eclipse.tracecompass.internal.analysis.os.linux.ui.Messages;
import org.eclipse.tracecompass.statesystem.core.ITmfStateSystem;
import org.eclipse.tracecompass.tmf.core.statesystem.TmfStateSystemAnalysisModule;
import org.eclipse.tracecompass.tmf.core.trace.ITmfTrace;
import org.eclipse.tracecompass.tmf.core.trace.TmfTraceUtils;
import org.eclipse.tracecompass.tmf.ui.views.timegraph.AbstractTimeGraphView;
import org.eclipse.tracecompass.tmf.ui.widgets.timegraph.TimeGraphPresentationProvider;
import org.eclipse.tracecompass.tmf.ui.widgets.timegraph.model.ITimeEvent;
import org.eclipse.tracecompass.tmf.ui.widgets.timegraph.model.ITimeGraphEntry;
import org.eclipse.tracecompass.tmf.ui.widgets.timegraph.model.TimeGraphEntry;

/**
 * This class represents the Controller and the View parts of the Context Switch
 * View.
 *
 * @author Alexis Cabana-Loriaux
 * @since 1.1
 *
 */
public class ContextSwitchView extends AbstractTimeGraphView {
    // ------------------------------------------------------------------------
    // Fields
    // ------------------------------------------------------------------------

    /** ID string */
    public static final String ID = "org.eclipse.tracecompass.analysis.os.linux.views.contextswitch"; //$NON-NLS-1$

    private static final String[] FILTER_COLUMN_NAMES = new String[] {
            Messages.ContextSwitchView_stateTypeName
    };

    private static final long BUILD_UPDATE_TIMEOUT = 500L;
    private HashMap<ITmfTrace, Integer> fMeanContextSwitchRate = new HashMap<>();

    private final Comparator<ITimeGraphEntry> fAscendingTimeGraphEntryComparator = new Comparator<ITimeGraphEntry>() {

        @Override
        public int compare(ITimeGraphEntry o1, ITimeGraphEntry o2) {

            int left = ((ResourcesEntry) o1).getId(), right = ((ResourcesEntry) o2).getId();
            return Integer.compare(left, right);
        }
    };

    /*
     * Factors used to determine the classification of the events. For example,
     * for an event to be classified as LOW, it must be 50% or less the value of
     * the mean of this group.
     */
    private enum ClassificationFactors {
        LOW(new Float(0.5f)),
        MODERATE(new Float(1f)),
        HIGH(new Float(1.5f)),
        CRITICAL(new Float(2f));

        Float factor;

        ClassificationFactors(Float f) {
            this.factor = f;
        }
    }

    // ------------------------------------------------------------------------
    // Constructors
    // ------------------------------------------------------------------------

    /**
     * Default constructor
     *
     * @param id
     *            the ID of this view
     * @param pres
     *            the presentation provider
     */
    public ContextSwitchView(String id, TimeGraphPresentationProvider pres) {
        super(id, pres);
        setFilterColumns(FILTER_COLUMN_NAMES);
    }

    /**
     * Default constructor
     */
    public ContextSwitchView() {
        super(ID, new ContextSwitchPresentationProvider());
    }

    // ------------------------------------------------------------------------
    // Internal
    // ------------------------------------------------------------------------
    /**
     * Build list of {@link TimeGraphEntry} (in this case, the list of CPUs)
     */
    @Override
    protected void buildEventList(ITmfTrace trace, ITmfTrace parentTrace, IProgressMonitor monitor) {
        ITmfStateSystem ssq = TmfStateSystemAnalysisModule.getStateSystem(trace, KernelContextSwitchAnalysis.ID);
        if (ssq == null) {
            return;
        }

        Map<Integer, TimeGraphEntry> entryMap = new HashMap<>();
        TimeGraphEntry traceEntry = null;

        long startTime = ssq.getStartTime();
        long start = startTime;
        setStartTime(Math.min(getStartTime(), startTime));
        boolean complete = false;
        while (!complete) {
            if (monitor.isCanceled()) {
                return;
            }
            complete = ssq.waitUntilBuilt(BUILD_UPDATE_TIMEOUT);
            if (ssq.isCancelled()) {
                return;
            }
            long end = ssq.getCurrentEndTime();
            if (start == end && !complete) { // when complete execute one last
                                             // time regardless of end time
                continue;
            }
            long endTime = end + 1;
            setEndTime(Math.max(getEndTime(), endTime));

            if (traceEntry == null) {
                traceEntry = new ResourcesEntry(trace, trace.getName(), startTime, endTime, 0);
                List<TimeGraphEntry> entryList = Collections.singletonList(traceEntry);
                addToEntryList(parentTrace, entryList);
            } else {
                traceEntry.updateEndTime(endTime);
            }

            List<Integer> cpuQuarks = ssq.getQuarks(Attributes.CPUS, "*"); //$NON-NLS-1$
            for (Integer cpuQuark : cpuQuarks) {
                int cpu = Integer.parseInt(ssq.getAttributeName(cpuQuark));
                TimeGraphEntry entry = entryMap.get(cpuQuark);
                if (entry == null) {
                    entry = new ResourcesEntry(cpuQuark, trace, startTime, endTime, Type.CPU, cpu);
                    entryMap.put(cpuQuark, entry);
                    traceEntry.addChild(entry);
                } else {
                    entry.updateEndTime(endTime);
                }
            }

            traceEntry.sortChildren(fAscendingTimeGraphEntryComparator);

            if (parentTrace.equals(getTrace())) {
                refresh();
            }
            long resolution = Math.max(1, (endTime - ssq.getStartTime()) / getDisplayWidth());
            for (ITimeGraphEntry child : traceEntry.getChildren()) {
                if (monitor.isCanceled()) {
                    return;
                }
                if (child instanceof TimeGraphEntry) {
                    populateTimeGraphEntry(monitor, start, endTime, resolution, (TimeGraphEntry)child);
                }
            }
            start = end;
        }
    }

    /**
     * @param monitor the progress monitor
     * @param start the start time of the TimeEvent
     * @param endTime the end time of the TimeEvent
     * @param resolution the resolution of the request
     * @param entry the entry to populate
     */
    private void populateTimeGraphEntry(IProgressMonitor monitor, long start, long endTime, long resolution, @NonNull TimeGraphEntry entry) {
        List<ITimeEvent> eventList = getEventList(entry, start, endTime, resolution, monitor);
        if (eventList != null) {
            for (ITimeEvent event : eventList) {
                entry.addEvent(event);
            }
        }
        redraw();
    }

    /**
     * Build the list of all the states in which the CPU(s) will be inside the
     * timerange passed as an argument
     */
    @Override
    protected List<ITimeEvent> getEventList(TimeGraphEntry entry, long startTime, long endTime, long resolution, IProgressMonitor monitor) {

        /* Length of the intervals queried */
        long bucketLength = 20 * resolution;

        /* Get the information about the nb of cpu */
        if (!(entry instanceof ResourcesEntry)) {
            /* Is a parent */
            return null;
        }
        ResourcesEntry resourcesEntry = (ResourcesEntry) entry;
        ITmfStateSystem sS = TmfStateSystemAnalysisModule.getStateSystem(resourcesEntry.getTrace(), KernelContextSwitchAnalysis.ID);
        if (sS == null) {
            return null;
        }
        int nbCPU = sS.getQuarks(Attributes.CPUS, "*").size(); //$NON-NLS-1$
        if (nbCPU == 0) {
            /* Can't get any information on the CPUs state */
            return null;
        }

        /* Make sure the times are correct */
        final long realStart = Math.max(startTime, sS.getStartTime());
        final long realEnd = Math.min(endTime, sS.getCurrentEndTime());
        if (realEnd <= realStart) {
            return null;
        }

        /* Retrieve analysis module */
        KernelContextSwitchAnalysis kernelContextSwitchSS = TmfTraceUtils.getAnalysisModuleOfClass(resourcesEntry.getTrace(), KernelContextSwitchAnalysis.class, KernelContextSwitchAnalysis.ID);
        if (kernelContextSwitchSS == null) {
            return null;
        }

        /*
         * Get the total number of context switch in the width of the view. If
         * the map returned is empty, no context switch can be retrieved for
         * this interval.
         */
        Map<Integer, Long> cs = kernelContextSwitchSS.getContextSwitchesRange(realStart, realEnd);

        /*
         * Get the total nb of sched_switch events between startTime and
         * endTime, and divide by the number of cpu and the time to get the mean
         * number of sched_switch per bucket, per cpu, for better classification
         */
        Long totalCxtSwtInRange = cs.get(KernelContextSwitchAnalysis.TOTAL);
        int deltaPerCPU = Math.round(totalCxtSwtInRange / (float) nbCPU);
        long deltat = realEnd - realStart;

        int meanNbCSPerCPUPerBucket = (int) ((deltaPerCPU * bucketLength) / (double) deltat);
        setMeanForTrace(resourcesEntry.getTrace(), meanNbCSPerCPUPerBucket);

        List<ITimeEvent> eventList = null;
        if (resourcesEntry.getType().equals(Type.CPU)) {
            eventList = new ArrayList<>();
            long queryStart = realStart;
            long queryEnd = queryStart + bucketLength;

            /* Cover 100% of the width */
            while (queryStart <= realEnd) {
                if (monitor.isCanceled()) {
                    return null;
                }
                Map<Integer, Long> map = kernelContextSwitchSS.getContextSwitchesRange(queryStart, queryEnd);

                if (map.containsKey(Integer.valueOf(resourcesEntry.getId()))) {
                    Long nbOfContextSwitchForCPU = map.get(Integer.valueOf(resourcesEntry.getId()));
                    ITimeEvent event = new ContextSwitchTimeEvent(entry, queryStart, bucketLength, nbOfContextSwitchForCPU.intValue());
                    eventList.add(event);
                }
                queryStart = queryEnd;
                queryEnd += bucketLength;
            }
            classifyEvents(eventList);
            eventList = mergeTimeEvents(eventList);
            if (monitor.isCanceled()) {
                return null;
            }
        }
        return eventList;
    }

    /* Method used to merge equally distributed ContextSwitchTimeEvents together */
    private static List<ITimeEvent> mergeTimeEvents(List<ITimeEvent> eventList) {
        List<ITimeEvent> mergedEventList = new ArrayList<>();
        if (eventList.size() <= 1) {
            return eventList;
        }
        // There's at least 1 event in the list, continue
        ContextSwitchTimeEvent first = (ContextSwitchTimeEvent) eventList.get(0);
        int count = 0;
        ContextSwitchTimeEvent event = null;
        /* Scans the list for possible mergeable TimeEvents and merge */
        for (ITimeEvent e : eventList) {
            event = (ContextSwitchTimeEvent) e;
            if (event.fRate == first.fRate) {
                count += event.getValue();
                continue;
            }
            ContextSwitchTimeEvent newTimeEvent = new ContextSwitchTimeEvent(first.getEntry(), first.getTime(), event.getTime() - first.getTime(), count);
            newTimeEvent.fRate = first.fRate;
            mergedEventList.add(newTimeEvent);
            first = event;
            count = first.getValue();
        }
        if (event != null) {
            ContextSwitchTimeEvent newTimeEvent = new ContextSwitchTimeEvent(first.getEntry(), first.getTime(), event.getTime() + event.getDuration() - first.getTime(), count);
            newTimeEvent.fRate = first.fRate;
            mergedEventList.add(newTimeEvent);
        }
        return mergedEventList;
    }

    private void classifyEvents(List<ITimeEvent> eventList) {
        if (eventList == null || eventList.isEmpty()) {
            return;
        }
        /*
         * when this method is called, the mean must be known, otherwise we
         * can't classify them
         */

        /* Get the concerned trace mean */
        ITmfTrace trace = ((ResourcesEntry) eventList.get(0).getEntry()).getTrace();
        Integer mean = getMeanForTrace(trace);

        /* Set and check thresholds */
        long lowRateThreshold = (long) (ClassificationFactors.LOW.factor * mean);
        long moderateRateThreshold = (long) (ClassificationFactors.MODERATE.factor * mean);
        long highRateThreshold = (long) (ClassificationFactors.HIGH.factor * mean);

        if (!(lowRateThreshold > 0)) {
            lowRateThreshold = 1;
        }

        if (!(moderateRateThreshold > lowRateThreshold)) {
            moderateRateThreshold = lowRateThreshold + 1;
        }
        if (!(highRateThreshold > moderateRateThreshold)) {
            highRateThreshold = moderateRateThreshold + 1;
        }

        for (ITimeEvent event : eventList) {
            ContextSwitchTimeEvent csEvent = (ContextSwitchTimeEvent) event;
            long contextSwitchRate = csEvent.getValue();
            if (contextSwitchRate == 0) {
                /* No context switch for this TimeEvent */
                csEvent.fRate = ContextSwitchRate.NONE;
            } else if (contextSwitchRate <= lowRateThreshold) {
                csEvent.fRate = ContextSwitchRate.LOW;
            } else if (contextSwitchRate <= moderateRateThreshold) {
                csEvent.fRate = ContextSwitchRate.MODERATE;
            } else if (contextSwitchRate <= highRateThreshold) {
                csEvent.fRate = ContextSwitchRate.HIGH;
            } else {
                csEvent.fRate = ContextSwitchRate.CRITICAL;
            }
        }
    }

    private Integer getMeanForTrace(ITmfTrace trace) {
        if (!fMeanContextSwitchRate.containsKey(trace)) {
            return 0;
        }
        return fMeanContextSwitchRate.get(trace);
    }

    private void setMeanForTrace(ITmfTrace trace, Integer mean) {
        fMeanContextSwitchRate.put(trace, mean);
    }


}
