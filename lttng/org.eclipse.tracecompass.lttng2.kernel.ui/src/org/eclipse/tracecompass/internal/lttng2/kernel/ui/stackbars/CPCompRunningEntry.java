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

import org.eclipse.tracecompass.tmf.ui.widgets.timegraph.model.TimeGraphEntry;

/**
 * An entry in the Stack bars view
 */
public class CPCompRunningEntry extends TimeGraphEntry {

    private long fDuration;
    private int fPriority;
    private int fTid;

    /**
     * Constructor
     *
     * @param tid
     *            Tid of the task
     * @param name
     *            The name of the task
     * @param startTime
     *            The start time of this execution
     * @param endTime
     *            The end time of this execution
     */
    public CPCompRunningEntry(int tid, String name, long startTime, long endTime) {
        super(name, startTime, endTime);
        fTid = tid;
    }

    /**
     * Get the total duration
     *
     * @return The total duration
     */
    public long getDuration() {
        return fDuration;
    }

    /**
     * Get the highest priority
     *
     * @return The highest priority
     */
    public int getPriority() {
        return fPriority;
    }

    /**
     * Set the total duration
     * @param duration
     *            The duration of the execution
     */
    public void setDuration(long duration) {
        fDuration = duration;
    }

    /**
     * Set the highest priority
     *
     * @param priority
     *            The highest priority
     */
    public void setPriority(int priority) {
        fPriority = priority;
    }

    /**
     * @return
     *         The tid
     */
    public int getTid() {
        return fTid;
    }

}
