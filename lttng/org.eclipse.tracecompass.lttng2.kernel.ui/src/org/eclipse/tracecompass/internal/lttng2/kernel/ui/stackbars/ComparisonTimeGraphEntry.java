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
public class ComparisonTimeGraphEntry extends TimeGraphEntry {

    /**
     *  Real start time
     */
    protected long fRealStartTime;

    /**
     * Constructor
     *
     * @param name
     *            The name of the task
     * @param startTime
     *            The start time of this execution
     * @param endTime
     *            The end time of this execution
     */
    public ComparisonTimeGraphEntry(String name, long startTime, long endTime) {
        super(name, 0, endTime - startTime);
        fRealStartTime = startTime;
    }

    /**
     * @return
     *      Real start time
     */
    public long getRealStartTime() {
        return fRealStartTime;
    }

}
