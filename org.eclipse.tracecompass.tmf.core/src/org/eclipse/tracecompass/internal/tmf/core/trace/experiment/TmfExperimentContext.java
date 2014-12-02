/*******************************************************************************
 * Copyright (c) 2009, 2014 Ericsson
 *
 * All rights reserved. This program and the accompanying materials are
 * made available under the terms of the Eclipse Public License v1.0 which
 * accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *   Francois Chouinard - Initial API and implementation
 *   Francois Chouinard - Put in shape for 1.0
 *   Patrick Tasse - Updated for removal of context clone
 *******************************************************************************/

package org.eclipse.tracecompass.internal.tmf.core.trace.experiment;

import java.util.ArrayDeque;
import java.util.PriorityQueue;
import java.util.Queue;

import org.eclipse.jdt.annotation.NonNull;
import org.eclipse.tracecompass.tmf.core.event.ITmfEvent;
import org.eclipse.tracecompass.tmf.core.trace.ITmfContext;
import org.eclipse.tracecompass.tmf.core.trace.ITmfTrace;
import org.eclipse.tracecompass.tmf.core.trace.TmfContext;

import com.google.common.base.Joiner;

/**
 * The experiment context in TMF.
 * <p>
 * The experiment keeps track of the next event from each of its traces so it
 * can pick the next one in chronological order.
 * <p>
 * This implies that the "next" event from each trace has already been read and
 * that we at least know its timestamp.
 * <p>
 * The last trace refers to the trace from which the last event was "consumed"
 * at the experiment level.
 */
public final class TmfExperimentContext extends TmfContext {

    // ------------------------------------------------------------------------
    // Constants
    // ------------------------------------------------------------------------

    /**
     * No last trace read indicator
     */
    public static final int NO_TRACE = -1;

    // ------------------------------------------------------------------------
    // Attributes
    // ------------------------------------------------------------------------

    private final @NonNull Queue<TmfExperimentIterator> fIterators;
    private TmfExperimentIterator fTopIterator;

    // ------------------------------------------------------------------------
    // Constructors
    // ------------------------------------------------------------------------

    /**
     * Standard constructor
     *
     * @param nbTraces
     *            The number of traces in the experiment
     */
    public TmfExperimentContext(final int nbTraces) {
        super();
        if (nbTraces <= 0) {
            fIterators = new ArrayDeque<>();
        } else {
            fIterators = new PriorityQueue<>(nbTraces);
        }
    }

    @Override
    public void dispose() {
        while (!fIterators.isEmpty()) {
            fIterators.poll().dispose();
        }
        super.dispose();
    }

    /**
     * Add a context to an experiment
     *
     * @param trace
     *            the trace the context should read
     * @param traceContext
     *            the context
     * @param traceId
     *            the trace id
     */
    public void addContext(ITmfTrace trace, @NonNull ITmfContext traceContext, int traceId) {
        ITmfEvent event;
        if (traceContext instanceof TmfExperimentContext) {
            TmfExperimentContext tmfExperimentContext = (TmfExperimentContext) traceContext;
            event = tmfExperimentContext.getCurrentEvent();
        } else {
            event = trace.getNext(traceContext);
        }
        if (event != null) {
            fIterators.add(new TmfExperimentIterator(event, trace, traceContext, traceId));
        }
        fTopIterator = fIterators.peek();
    }

    /**
     * Get the next event of this trace
     *
     * @return the event
     */
    public ITmfEvent getNext() {
        if (fIterators.isEmpty()) {
            return null;
        }
        fTopIterator = fIterators.poll();
        ITmfEvent retEvent = fTopIterator.getNext();
        if (fTopIterator.getCurrentEvent() != null) {
            fIterators.add(fTopIterator);
        }
        return retEvent;
    }

    // ------------------------------------------------------------------------
    // Accessors
    // ------------------------------------------------------------------------

    /**
     * Return how many traces this experiment context tracks the contexts of
     * (a.k.a., the number of traces in the experiment).
     *
     * @return The number of traces in the experiment
     */
    public int getNbTraces() {
        return fIterators.size();
    }

    /**
     * Get the current event for a context
     *
     * @return the current event
     */
    public ITmfEvent getCurrentEvent() {
        return fTopIterator.getCurrentEvent();
    }

    /**
     * Get the current context
     *
     * @return the current context
     */
    public ITmfContext getCurrentContext() {
        return fTopIterator.getContext();
    }

    /**
     * The id of the trace
     *
     * @return the trace id
     */
    public int getTraceId() {
        return fTopIterator.getTraceId();
    }

    // ------------------------------------------------------------------------
    // Object
    // ------------------------------------------------------------------------

    @Override
    public int hashCode() {
        int result = 17;
        for (TmfExperimentIterator iterator : fIterators) {
            result = 37 * result + iterator.hashCode();
        }
        return result;
    }

    @Override
    public boolean equals(final Object other) {
        if (this == other) {
            return true;
        }
        if (!super.equals(other)) {
            return false;
        }
        if (!(other instanceof TmfExperimentContext)) {
            return false;
        }
        final TmfExperimentContext o = (TmfExperimentContext) other;
        if (!fIterators.containsAll(o.fIterators)) {
            return false;
        }
        if (!o.fIterators.containsAll(fIterators)) {
            return false;
        }
        return true;
    }

    @Override
    @SuppressWarnings("nls")
    public String toString() {
        StringBuilder sb = new StringBuilder("TmfExperimentContext [\n");
        sb.append("\tfLocation=" + getLocation() + ", fRank=" + getRank() + "\n");
        sb.append("\tfContextsWithEvents=[");
        Joiner.on(',').skipNulls().appendTo(sb, fIterators);
        sb.append("]");
        return sb.toString();
    }

}
