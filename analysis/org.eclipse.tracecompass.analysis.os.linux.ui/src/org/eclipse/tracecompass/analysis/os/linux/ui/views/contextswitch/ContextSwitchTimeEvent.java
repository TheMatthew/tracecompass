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

import org.eclipse.tracecompass.tmf.ui.widgets.timegraph.model.ITimeEvent;
import org.eclipse.tracecompass.tmf.ui.widgets.timegraph.model.ITimeGraphEntry;
import org.eclipse.tracecompass.tmf.ui.widgets.timegraph.model.TimeEvent;

/**
 * Class used to represent a bucket on the timegraph in the context siwtch view.
 * The only addition is that a TimeEvent has a rate for better classification.
 *
 * @author Alexis Cabana-Loriaux
 * @since 1.0
 *
 */
class ContextSwitchTimeEvent extends TimeEvent {

    /**
     * Possible context switch rates of a given TimeEvent
     *
     * @author Alexis Cabana-Loriaux The rate of context switching for a given
     *         TimeEvent
     */
    public enum ContextSwitchRate {
        NONE,
        LOW,
        MODERATE,
        HIGH,
        CRITICAL
    }

    /** The rate of context switching for this TimeEvent */
    public ContextSwitchRate fRate;

    /**
     * Default constructor
     *
     * @param entry
     *            the entry
     * @param time
     *            the start time
     * @param duration
     *            the duration
     * @param value
     *            the number of context switch
     */
    public ContextSwitchTimeEvent(ITimeGraphEntry entry, long time, long duration, int value) {
        super(entry, time, duration, value);
        fRate = ContextSwitchRate.NONE;
    }

    @Override
    public ITimeEvent splitBefore(long splitTime) {
        ContextSwitchTimeEvent event = (splitTime > fTime ?
                new ContextSwitchTimeEvent(fEntry, fTime, Math.min(fDuration, splitTime - fTime), getValue()) :
                null);
        if (event != null) {
            event.fRate = this.fRate;
        }
        return event;
    }

    @Override
    public ITimeEvent splitAfter(long splitTime) {
        ContextSwitchTimeEvent event = (splitTime < fTime + fDuration ?
                new ContextSwitchTimeEvent(fEntry, Math.max(fTime, splitTime), fDuration - Math.max(0, splitTime - fTime),
                        getValue()) :
                null);
        if (event != null) {
            event.fRate = this.fRate;
        }
        return event;
    }
}
