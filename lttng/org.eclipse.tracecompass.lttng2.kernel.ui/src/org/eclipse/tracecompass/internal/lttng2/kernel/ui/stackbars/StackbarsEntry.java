/*******************************************************************************
 * Copyright (c) 2013 École Polytechnique de Montréal
 *
 * All rights reserved. This program and the accompanying materials are
 * made available under the terms of the Eclipse Public License v1.0 which
 * accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *   Geneviève Bastien - Initial API and implementation (from ControlFlowEntry)
 *   Mathieu Cote - Adapt to stackbars
 *******************************************************************************/

package org.eclipse.tracecompass.internal.lttng2.kernel.ui.stackbars;

import org.eclipse.jdt.annotation.NonNull;
import org.eclipse.tracecompass.tmf.core.trace.ITmfTrace;

/**
 * An entry in the Stack bars view
 */
public class StackbarsEntry extends ComparisonTimeGraphEntry implements Comparable<StackbarsEntry>{

    private long fRunningTime;
    private long fPreemptedTime;
    private int fRankByDuration;
    private int fRankByStartingTime;
    private int fTidStart;
    private int fTidEnd;
    private final @NonNull ITmfTrace fTrace;

    /**
     * Constructor
     *
     * @param taskname
     *            Name of the task
     * @param trace
     *            The trace on which we are working
     * @param startTime
     *            The start time of this execution
     * @param endTime
     *            The end time of the execution
     * @param tidStart
     *            The tid of the execution start event
     * @param tidEnd
     *            The tid of the execution end event
     * @param rankByStartingTime
     *            The sum of all running time in the execution
     */
    public StackbarsEntry(String taskname, @NonNull ITmfTrace trace, long startTime, long endTime, int tidStart, int tidEnd, int rankByStartingTime) {
        super(taskname, startTime, endTime);
        fTidStart = tidStart;
        fTidEnd = tidEnd;
        fTrace = trace;
        fRankByStartingTime = rankByStartingTime;
        fRankByDuration = -1;
    }

    /**
     * Get the entry's trace
     *
     * @return the entry's trace
     */
    public @NonNull ITmfTrace getTrace() {
        return fTrace;
    }

    /**
     * Get the rank by starting time
     *
     * @return The rank by starting time from 0
     */
    public int getRankByStartingTime() {
        return fRankByStartingTime;
    }

    /**
     * Get the running time
     *
     * @return The running time
     */
    public long getRunningTime() {
        return fRunningTime;
    }

    /**
     * Get the Preempted Time
     *
     * @return The Preempted Time
     */
    public long getPreemptedTime() {
        return fPreemptedTime;
    }

    /**
     * Get the rank by duration
     *
     * @return The rank by duration
     */
    public int getRankByDuration() {
        return fRankByDuration;
    }

    /**
     * Get duration
     *
     * @return The duration
     */
    public long getDuration() {
        return getEndTime();
    }

    /**
     * Get the tid of the thread on which the start event occurred
     *
     * @return The tid
     */
    public int getTidStart() {
        return fTidStart;
    }

    /**
     * Get the tid of the thread on which the end event occurred
     *
     * @return The tid
     */
    public int getTidEnd() {
        return fTidEnd;
    }

    /**
     * Set the rank by duration
     *
     * @param rankByDuration
     *      the rank by duration
     */
    public void setRankByDuration(int rankByDuration) {
        fRankByDuration = rankByDuration;
    }

    /**
     * Set the running time
     *
     * @param runningTime
     *      the running time
     */
    public void setRunningTime(long runningTime) {
        fRunningTime = runningTime;
    }

    /**
     * Set the preempted Time
     *
     * @param preemptedTime
     *      the preempted Time
     */
    public void setPreemptedTime(long preemptedTime) {
        fPreemptedTime = preemptedTime;
    }

    @Override
    public int compareTo(StackbarsEntry o) {
        if((this.fRealStartTime < o.fRealStartTime))
        {
            return -1;
        }
        if(this.fRealStartTime > o.fRealStartTime)
        {
            return 1;
        }
        return 0 ;
    }
}
