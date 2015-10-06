/*******************************************************************************
 * Copyright (c) 2013 École Polytechnique de Montréal
 *
 * All rights reserved. This program and the accompanying materials are
 * made available under the terms of the Eclipse Public License v1.0 which
 * accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *   Geneviève Bastien - Initial API and implementation
 *******************************************************************************/

package org.eclipse.tracecompass.internal.lttng2.kernel.ui.stackbars;

import java.util.HashMap;
import java.util.Map;

import org.eclipse.linuxtools.tmf.ui.widgets.timegraph.StateItem;
import org.eclipse.linuxtools.tmf.ui.widgets.timegraph.TimeGraphPresentationProvider;
import org.eclipse.linuxtools.tmf.ui.widgets.timegraph.model.ITimeEvent;
import org.eclipse.linuxtools.tmf.ui.widgets.timegraph.model.TimeEvent;
import org.eclipse.swt.graphics.RGB;

/**
 * Presentation provider for the critical path view
 */
public class ExtendedTimePresentationProvider extends TimeGraphPresentationProvider {

    /**
     * The enumeration of possible states for the view
     */
    public static enum State {

        /** Timer init */
        TIMER_INIT         (new RGB(100, 200, 0)),
        /** Timer start*/
        TIMER_START     (new RGB(150, 200, 0)),
        /** Time expired */
        TIMER_EXPIRED       (new RGB(200, 200, 0)),
        /** Timer cancel */
        TIMER_CANCEL         (new RGB(250, 200, 0)),

        /** Queue full */
        QUEUE_FULL_WHILE_SENDERS         (new RGB(200, 0, 0)),
        /** Queue empty*/
        QUEUE_EMPTY_WHILE_RECEIVERS     (new RGB(200, 100, 0)),
        /** Receivers */
        RECEIVERS_WAITING       (new RGB(200, 150, 0)),
        /** Senders */
        SENDERS_WAITING         (new RGB(200, 50, 0)),

        /** Futex wait */
        FUTEX_WAIT         (new RGB(0, 0, 200)),
        /** Futex wake*/
        FUTEX_WAKE     (new RGB(0, 50, 200)),
        /** Futex requeue */
        FUTEX_REQUEUE       (new RGB(0, 100, 200)),
        /** Futex PI */
        FUTEX_PI       (new RGB(0, 150, 200)),

        /** Unknown */
        UNKNOWN         (new RGB(200, 200, 200));

        /** RGB color associated with a state */
        public final RGB rgb;

        private State (RGB rgb) {
            this.rgb = rgb;
        }
    }

    @Override
    public String getStateTypeName() {
        return Messages.StackbarsView_stateTypeName;
    }

    @Override
    public StateItem[] getStateTable() {
        StateItem[] stateTable = new StateItem[State.values().length];
        for (int i = 0; i < stateTable.length; i++) {
            State state = State.values()[i];
            stateTable[i] = new StateItem(state.rgb, state.toString());
        }
        return stateTable;
    }

    @Override
    public int getStateTableIndex(ITimeEvent event) {
        if (event instanceof TimeEvent && ((TimeEvent) event).hasValue()) {
            return ((TimeEvent) event).getValue();
        }
        return TRANSPARENT;
    }

    private static State getMatchingState(int status) {
        switch (status) {
        case 0:
            return State.TIMER_INIT;
        case 1:
            return State.TIMER_START;
        case 2:
            return State.TIMER_EXPIRED;
        case 3:
            return State.TIMER_CANCEL;
        case 4:
            return State.QUEUE_FULL_WHILE_SENDERS;
        case 5:
            return State.QUEUE_EMPTY_WHILE_RECEIVERS;
        case 6:
            return State.RECEIVERS_WAITING;
        case 7:
            return State.SENDERS_WAITING;
        case 8:
            return State.FUTEX_WAIT;
        case 9:
            return State.FUTEX_WAKE;
        case 10:
            return State.FUTEX_REQUEUE;
        case 11:
            return State.FUTEX_PI;
        default:
            return State.UNKNOWN;
        }
    }


    @Override
    public String getEventName(ITimeEvent event) {
        if (event instanceof TimeEvent) {
            TimeEvent ev = (TimeEvent) event;
            if (ev.hasValue()) {
                return getMatchingState(ev.getValue()).toString();
            }
        }
        return Messages.StackbarsView_multipleStates;
    }

    @Override
    public Map<String, String> getEventHoverToolTipInfo(ITimeEvent event) {

        Map<String, String> map = super.getEventHoverToolTipInfo(event);

        if (event instanceof ExtendedTimeEvent) {
            if(map == null)
            {
                map = new HashMap<>();
            }
            map.put("Info", ((ExtendedTimeEvent)event).getotherInfo());
        }

        return map;
    }
}


