/*******************************************************************************
 * Copyright (c) 2009, 2014 Ericsson
 *
 * All rights reserved. This program and the accompanying materials are
 * made available under the terms of the Eclipse Public License v1.0 which
 * accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *   William Bourque - Initial API and implementation
 *   Yuriy Vashchuk - Heritage correction.
 *   Francois Chouinard - Cleanup and refactoring
 *   Francois Chouinard - Moved from LTTng to TMF
 *   Simon Delisle - Added a new parameter to the constructor
 *   Xavier Raynaud - Support multi-trace coloring
 *******************************************************************************/

package org.eclipse.tracecompass.tmf.ui.views.histogram;

import org.eclipse.jdt.annotation.NonNull;
import org.eclipse.tracecompass.common.core.collect.BufferedBlockingQueue;
import org.eclipse.tracecompass.tmf.core.event.ITmfEvent;
import org.eclipse.tracecompass.tmf.core.event.ITmfLostEvent;
import org.eclipse.tracecompass.tmf.core.event.TmfEvent;
import org.eclipse.tracecompass.tmf.core.request.ITmfEventRequest;
import org.eclipse.tracecompass.tmf.core.request.TmfEventRequest;
import org.eclipse.tracecompass.tmf.core.timestamp.ITmfTimestamp;
import org.eclipse.tracecompass.tmf.core.timestamp.TmfTimeRange;
import org.eclipse.tracecompass.tmf.core.timestamp.TmfTimestamp;

/**
 * Class to request events for given time range from a trace to fill a
 * HistogramDataModel and HistogramView.
 *
 * @version 1.0
 * @author Francois Chouinard
 *         <p>
 */
public class HistogramRequest extends TmfEventRequest {

    // ------------------------------------------------------------------------
    // Attributes
    // ------------------------------------------------------------------------

    private static final @NonNull ITmfEvent POISON_PILL = new TmfEvent(null, Long.MIN_VALUE, TmfTimestamp.BIG_BANG, null, null);

    /**
     * The histogram data model to fill.
     */
    protected final HistogramDataModel fHistogram;

    private final boolean fFullRange;

    private final BufferedBlockingQueue<ITmfEvent> fEventsQueue;

    private Thread fHistogramPopulator;

    // ------------------------------------------------------------------------
    // Constructor
    // ------------------------------------------------------------------------

    /**
     * Constructor
     *
     * @param histogram
     *            The histogram data model
     * @param range
     *            The time range to request data
     * @param rank
     *            The index of the first event to retrieve
     * @param nbEvents
     *            The number of events requested
     * @param blockSize
     *            The number of events per block
     * @param execType
     *            The requested execution priority
     * @param fullRange
     *            Full range or time range for histogram request
     */
    public HistogramRequest(HistogramDataModel histogram, TmfTimeRange range,
            int rank, int nbEvents, int blockSize,
            ITmfEventRequest.ExecutionType execType, boolean fullRange) {
        super(ITmfEvent.class, range, rank, nbEvents, execType);
        fHistogram = histogram;
        fFullRange = fullRange;
        fEventsQueue = BufferedBlockingQueue.create(127, 127, Thread.currentThread());
        fHistogramPopulator = new Thread() {
            @Override
            public void run() {
                ITmfEvent event = fEventsQueue.take();
                while (event != POISON_PILL) {
                    if (!isCancelled()) {
                        synchronized (fHistogram) {
                            if (event instanceof ITmfLostEvent) {
                                ITmfLostEvent lostEvents = (ITmfLostEvent) event;
                                /* clear the old data when it is a new request */
                                fHistogram.countLostEvent(lostEvents.getTimeRange(), lostEvents.getNbLostEvents(), fFullRange);

                            } else { /* handle lost event */
                                long timestamp = event.getTimestamp().normalize(0, ITmfTimestamp.NANOSECOND_SCALE).getValue();
                                fHistogram.countEvent(getNbRead(), timestamp, event.getTrace());
                            }
                        }
                    }
                    event = fEventsQueue.take();
                }
            }
        };
        fHistogramPopulator.setName("Histogram Populator : " + (fFullRange ? "Full Range" : "Time Range")); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
        fHistogramPopulator.start();
    }

    // ------------------------------------------------------------------------
    // TmfEventRequest
    // ------------------------------------------------------------------------

    /**
     * Handle the event from the trace by updating the histogram data model.
     *
     * @param event
     *            a event from the trace
     * @see org.eclipse.tracecompass.tmf.core.request.TmfEventRequest#handleData(org.eclipse.tracecompass.tmf.core.event.ITmfEvent)
     */
    @Override
    public void handleData(ITmfEvent event) {
        super.handleData(event);
        fEventsQueue.put(event);
    }

    /**
     * Complete the request. It also notifies the histogram model about the
     * completion.
     *
     * @see org.eclipse.tracecompass.tmf.core.request.TmfEventRequest#handleCompleted()
     */
    @Override
    public void handleCompleted() {
        fHistogram.complete();
        fEventsQueue.put(POISON_PILL);
        fEventsQueue.flushInputBuffer();
        try {
            fHistogramPopulator.join();
        } catch (InterruptedException e) {
            // do nothing
        }
        super.handleCompleted();
    }
}
