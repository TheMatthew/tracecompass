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
public class ExtendedTimeEvent extends TimeEvent {

    private String fOtherInformation;

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
     * @param otherInfo
     *            Supplementary information to be display in mouse hover
     * @since 2.1
     */
    public ExtendedTimeEvent(ITimeGraphEntry entry, long time, long duration,
            int value, String otherInfo) {
        super(entry, time, duration, value);
        fOtherInformation = otherInfo;
    }

    /**
     * Get the supplementary information
     *
     * @return The supplementary information
     */
    public String getotherInfo() {
        return fOtherInformation;
    }
}
