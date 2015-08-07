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

import java.util.LinkedHashMap;
import java.util.Map;

import org.eclipse.swt.graphics.RGB;
import org.eclipse.tracecompass.analysis.os.linux.ui.views.resources.ResourcesEntry;
import org.eclipse.tracecompass.analysis.os.linux.ui.views.resources.ResourcesEntry.Type;
import org.eclipse.tracecompass.internal.analysis.os.linux.ui.Messages;
import org.eclipse.tracecompass.tmf.ui.widgets.timegraph.StateItem;
import org.eclipse.tracecompass.tmf.ui.widgets.timegraph.TimeGraphPresentationProvider;
import org.eclipse.tracecompass.tmf.ui.widgets.timegraph.model.ITimeEvent;
import org.eclipse.tracecompass.tmf.ui.widgets.timegraph.model.NullTimeEvent;
import org.eclipse.tracecompass.tmf.ui.widgets.timegraph.model.TimeEvent;

/**
 * PresentationProvider used in the context switch view
 *
 * @author Alexis Cabana-Loriaux
 * @since 1.1
 *
 */
public class ContextSwitchPresentationProvider extends TimeGraphPresentationProvider {

    private enum State {
        CRITICAL(new RGB(161, 37, 37)),
        HIGH(new RGB(246, 40, 40)),
        MODERATE(new RGB(213, 213, 62)),
        LOW(new RGB(0, 119, 255)),
        NONE(new RGB(0, 0, 0));// used for when there's no Context switch

        public final RGB rgb;

        private State(RGB rgb) {
            this.rgb = rgb;
        }
    }

    /** Default Constructor */
    public ContextSwitchPresentationProvider() {
        super();
    }

    private static State[] getStateValues() {
        return State.values();
    }

    private static State getEventState(TimeEvent event) {
        if (event instanceof ContextSwitchTimeEvent) {
            ContextSwitchTimeEvent tevent = (ContextSwitchTimeEvent) event;
            ResourcesEntry entry = (ResourcesEntry) event.getEntry();
            if (entry.getType() == Type.CPU) {
                if (tevent.fRate == ContextSwitchTimeEvent.ContextSwitchRate.NONE) {
                    return State.NONE;
                } else if (tevent.fRate == ContextSwitchTimeEvent.ContextSwitchRate.LOW) {
                    return State.LOW;
                } else if (tevent.fRate == ContextSwitchTimeEvent.ContextSwitchRate.MODERATE) {
                    return State.MODERATE;
                } else if (tevent.fRate == ContextSwitchTimeEvent.ContextSwitchRate.HIGH) {
                    return State.HIGH;
                } else if (tevent.fRate == ContextSwitchTimeEvent.ContextSwitchRate.CRITICAL) {
                    return State.CRITICAL;
                }
            }
        }
        return null;
    }

    @Override
    public int getStateTableIndex(ITimeEvent event) {
        ResourcesEntry entry = (ResourcesEntry) event.getEntry();
        if (entry.getType() != Type.CPU) {
            return TRANSPARENT;
        }
        State state = getEventState((TimeEvent) event);
        if (state == State.NONE) {
            return INVISIBLE;
        }
        if (state != null) {
            return state.ordinal();
        }
        if (event instanceof NullTimeEvent) {
            return INVISIBLE;
        }
        return TRANSPARENT;
    }

    @Override
    public StateItem[] getStateTable() {
        State[] states = getStateValues();
        StateItem[] stateTable = new StateItem[states.length];
        for (int i = 0; i < stateTable.length; i++) {
            State state = states[i];
            stateTable[i] = new StateItem(state.rgb, state.toString());
        }
        return stateTable;
    }

    @Override
    public String getEventName(ITimeEvent event) {
        State state = getEventState((TimeEvent) event);
        if (state != null) {
            return state.toString();
        }
        if (event instanceof NullTimeEvent) {
            return null;
        }
        return Messages.ResourcesView_multipleStates;
    }

    @Override
    public Map<String, String> getEventHoverToolTipInfo(ITimeEvent event, long hoverTime) {
        Map<String, String> retMap = new LinkedHashMap<>();
        if (event instanceof TimeEvent && ((TimeEvent) event).hasValue()) {
            TimeEvent tEvent = (TimeEvent) event;
            ResourcesEntry entry = (ResourcesEntry) event.getEntry();

            if (entry.getType() == Type.CPU) {
                retMap.put(Messages.ContextSwitchView_contextSwitchInIntervalName, Integer.toString(tEvent.getValue()));
            }

        }
        return retMap;
    }
}
