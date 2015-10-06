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

import org.eclipse.linuxtools.tmf.ui.widgets.timegraph.model.TimeGraphEntry;

/**
 * An entry in the Stack bars view
 */
public class CPCompRelatedEntry extends TimeGraphEntry {

    private boolean fDirect;
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
    public CPCompRelatedEntry(int tid, String name, long startTime, long endTime, boolean direct) {
        super(name, startTime, endTime);
        fTid = tid;
        fDirect = direct;
    }

    /**
     * Get if directly related
     *
     * @return True if directly related
     */
    public boolean isDirect() {
        return fDirect;
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
