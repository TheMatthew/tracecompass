/*******************************************************************************
 * Copyright (c) 2014 Ericsson
 *
 * All rights reserved. This program and the accompanying materials are
 * made available under the terms of the Eclipse Public License v1.0 which
 * accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *   Matthew Khouzam - Initial API and implementation
 *******************************************************************************/

package org.eclipse.tracecompass.internal.tmf.core.trace.experiment;

import org.eclipse.jdt.annotation.NonNull;
import org.eclipse.jdt.annotation.Nullable;
import org.eclipse.tracecompass.tmf.core.event.ITmfEvent;
import org.eclipse.tracecompass.tmf.core.trace.ITmfContext;
import org.eclipse.tracecompass.tmf.core.trace.ITmfTrace;

/**
 * Experiment Iterator, it's a context, a trace and an integer id. Useful to
 * help sorting data.
 */
final class TmfExperimentIterator implements Comparable<TmfExperimentIterator> {
    private @Nullable ITmfEvent fCurrentEvent;
    private final @NonNull ITmfTrace fTrace;
    private final @NonNull ITmfContext fContext;
    private final int fTraceId;

    /**
     * Experiment iterator constructor
     *
     * @param event
     *            the event of the experiment, cannot be null, the parent trace
     *            is extracted from this
     * @param trace
     *            the trace that generated this event
     * @param context
     *            the context of the trace
     * @param traceId
     *            the trace id
     */
    public TmfExperimentIterator(@NonNull ITmfEvent event, @NonNull ITmfTrace trace, @NonNull ITmfContext context, int traceId) {
        fCurrentEvent = event;
        fContext = context;
        fTrace = trace;
        fTraceId = traceId;
    }

    /**
     * Get the context of the iterator
     *
     * @return the context of the iterator
     */
    public ITmfContext getContext() {
        return fContext;
    }

    /**
     * Get the next event in the trace
     *
     * @return the next event of the trace, can be null
     */
    public ITmfEvent getNext() {
        final ITmfEvent retVal = fCurrentEvent;
        fCurrentEvent = getTrace().getNext(fContext);
        return retVal;
    }

    /**
     * Gets the current event
     *
     * @return the current event
     */
    public ITmfEvent getCurrentEvent() {
        return fCurrentEvent;
    }

    /**
     * Get the trace of the iterator
     *
     * @return the trace
     */
    public ITmfTrace getTrace() {
        return fTrace;
    }

    /**
     * Gets the trace id
     *
     * @return the trace id
     */
    public int getTraceId() {
        return fTraceId;
    }

    /**
     * Dispose this object
     */
    public void dispose() {
        fContext.dispose();
    }

    // ------------------------------------------------------------------------
    // Comparator
    // ------------------------------------------------------------------------

    @Override
    public int compareTo(TmfExperimentIterator other) {
        int comparison = getCurrentEvent().getTimestamp().compareTo(other.getCurrentEvent().getTimestamp());
        if (comparison == 0) {
            return getTrace().getName().compareTo(other.getTrace().getName());
        }
        return comparison;
    }

    // ------------------------------------------------------------------------
    // Object overrides
    // ------------------------------------------------------------------------

    @Override
    public int hashCode() {
        final int prime = 31;
        final ITmfEvent currentEvent = fCurrentEvent;
        int result = 1;
        result = prime * result + fContext.hashCode();
        result = prime * result + ((currentEvent == null) ? 0 : currentEvent.hashCode());
        result = prime * result + fTrace.hashCode();
        result = prime * result + fTraceId;
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null) {
            return false;
        }
        if (!(obj instanceof TmfExperimentIterator)) {
            return false;
        }
        TmfExperimentIterator other = (TmfExperimentIterator) obj;
        if (!fContext.equals(other.fContext)) {
            return false;
        }
        ITmfEvent currentEvent = fCurrentEvent;
        ITmfEvent otherCurrentEvent = other.fCurrentEvent;
        if (currentEvent == null) {
            if (otherCurrentEvent != null) {
                return false;
            }
        } else if (!currentEvent.equals(otherCurrentEvent)) {
            return false;
        }
        if (!fTrace.equals(other.fTrace)) {
            return false;
        }
        if (fTraceId != other.fTraceId) {
            return false;
        }
        return true;
    }

}