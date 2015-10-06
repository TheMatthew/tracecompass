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

import org.eclipse.tracecompass.tmf.ui.widgets.timegraph.model.ITimeGraphEntry;
import org.eclipse.tracecompass.tmf.ui.widgets.timegraph.model.TimeEvent;

/**
 * An entry in the Stack bars view
 */
public class CPComplementTimeEvent extends TimeEvent {

    private int fPriority;
    private int fExecPriority;
    private int fBlockedPriority;
    private String fCpu;
    private int fValue;

    /**
     * Constructor
     *
     * @param entry
     *            The entry to which this time event is assigned
     * @param time
     *            The timestamp of this event
     * @param duration
     *            The duration of this event
     * @param value
     *            The status assigned to the event
     * @param execPrio
     *            TODO
     * @param blockedPrio
     *            TODO
     * @since 2.1
     */
    public CPComplementTimeEvent(ITimeGraphEntry entry, long time, long duration,
            int value, int priority, String cpu, int execPrio, int blockedPrio) {
        super(entry, time, duration, value);
        fPriority = priority;
        fCpu = cpu;
        fExecPriority = execPrio;
        fBlockedPriority = blockedPrio;
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
     * Get the blocked priority
     *
     * @return The blocked priority
     */
    public int getBlockedPriority() {
        return fBlockedPriority;
    }

    /**
     * Get the exec priority
     *
     * @return The exec priority
     */
    public int getExecPriority() {
        return fExecPriority;
    }

    /**
     * Get the cpu
     *
     * @return The cpu
     */
    public String getCpu() {
        return fCpu;
    }

    /**
     * @param value
     *            The status assigned to the event
     */
    public void setValue(int value) {
        fValue = value;
    }

    @Override
    public int getValue() {
        return fValue;
    }
}
