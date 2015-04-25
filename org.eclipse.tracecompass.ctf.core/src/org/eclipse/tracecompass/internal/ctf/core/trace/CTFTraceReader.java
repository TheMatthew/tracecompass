/*******************************************************************************
 * Copyright (c) 2011, 2014 Ericsson, Ecole Polytechnique de Montreal and others
 *
 * All rights reserved. This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License v1.0 which
 * accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Matthew Khouzam - Initial API and implementation
 *     Alexandre Montplaisir - Initial API and implementation
 *******************************************************************************/

package org.eclipse.tracecompass.internal.ctf.core.trace;

import java.io.IOException;
import java.util.Collections;
import java.util.HashSet;
import java.util.PriorityQueue;
import java.util.Set;

import org.eclipse.tracecompass.ctf.core.CTFException;
import org.eclipse.tracecompass.ctf.core.event.IEventDeclaration;
import org.eclipse.tracecompass.ctf.core.event.IEventDefinition;
import org.eclipse.tracecompass.ctf.core.trace.CTFResponse;
import org.eclipse.tracecompass.ctf.core.trace.CTFStream;
import org.eclipse.tracecompass.ctf.core.trace.CTFStreamInput;
import org.eclipse.tracecompass.ctf.core.trace.CTFTrace;
import org.eclipse.tracecompass.ctf.core.trace.ICTFTraceReader;
import org.eclipse.tracecompass.internal.ctf.core.Activator;

import com.google.common.collect.ImmutableSet;
import com.google.common.collect.ImmutableSet.Builder;

/**
 * A CTF trace reader. Reads the events of a trace.
 *
 * @version 1.0
 * @author Matthew Khouzam
 * @author Alexandre Montplaisir
 */
public class CTFTraceReader implements ICTFTraceReader {

    private static final int MIN_PRIO_SIZE = 16;

    // ------------------------------------------------------------------------
    // Attributes
    // ------------------------------------------------------------------------

    /**
     * The trace to read from.
     */
    private final CTFTrace fTrace;

    /**
     * Vector of all the trace file readers.
     */
    private final Set<CTFStreamInputReader> fStreamInputReaders =
            Collections.synchronizedSet(new HashSet<CTFStreamInputReader>());

    private final Set<CTFStreamInput> fStreamInputs = new HashSet<>();

    /**
     * Priority queue to order the trace file readers by timestamp.
     */
    private PriorityQueue<CTFStreamInputReader> fPrio;

    /**
     * Timestamp of the first event in the trace
     */
    private long fStartTime;

    /**
     * Timestamp of the last event read so far
     */
    private long fEndTime;

    private final boolean fLive;

    // ------------------------------------------------------------------------
    // Constructors
    // ------------------------------------------------------------------------

    /**
     * Constructs a TraceReader to read a trace.
     *
     * @param trace
     *            The trace to read from.
     * @throws CTFException
     *             if an error occurs
     */
    public CTFTraceReader(CTFTrace trace) throws CTFException {
        this(trace, false);
    }

    /**
     * Constructs a TraceReader to read a trace.
     *
     * @param trace
     *            the trace to read
     * @param live
     *            is the trace live or post-mortem
     * @throws CTFException
     *             if an error occurs
     * @since 1.0
     */
    public CTFTraceReader(CTFTrace trace, boolean live) throws CTFException {
        fTrace = trace;
        fLive = live;
        fStreamInputReaders.clear();

        /**
         * Create the trace file readers.
         */
        createStreamInputReaders();

        /**
         * Populate the timestamp-based priority queue.
         */
        populateStreamInputReaderHeap();

        /**
         * Get the start Time of this trace bear in mind that the trace could be
         * empty.
         */
        fStartTime = 0;
        if (hasMoreEvents()) {
            fStartTime = getTopStream().getCurrentEvent().getTimestamp();
            setEndTime(fStartTime);
        }
    }

    /* (non-Javadoc)
     * @see org.eclipse.tracecompass.internal.ctf.core.trace.ICTFTraceReader#copyFrom()
     */
    @Override
    public ICTFTraceReader copyFrom() throws CTFException {
        CTFTraceReader newReader = null;

        newReader = new CTFTraceReader(fTrace);
        newReader.fStartTime = fStartTime;
        newReader.setEndTime(fEndTime);
        return newReader;
    }

    /* (non-Javadoc)
     * @see org.eclipse.tracecompass.internal.ctf.core.trace.ICTFTraceReader#close()
     */
    @Override
    public void close() {
        synchronized (fStreamInputReaders) {
            for (CTFStreamInputReader reader : fStreamInputReaders) {
                if (reader != null) {
                    try {
                        reader.close();
                    } catch (IOException e) {
                        Activator.logError(e.getMessage(), e);
                    }
                }
            }
            fStreamInputReaders.clear();
        }
    }

    // ------------------------------------------------------------------------
    // Getters/Setters/Predicates
    // ------------------------------------------------------------------------

    /* (non-Javadoc)
     * @see org.eclipse.tracecompass.internal.ctf.core.trace.ICTFTraceReader#getStartTime()
     */
    @Override
    public long getStartTime() {
        return fStartTime;
    }

    /**
     * Set the trace's end time
     *
     * @param endTime
     *            The end time to use
     */
    protected final void setEndTime(long endTime) {
        fEndTime = endTime;
    }

    /**
     * Get the priority queue of this trace reader.
     *
     * @return The priority queue of input readers
     */
    protected PriorityQueue<CTFStreamInputReader> getPrio() {
        return fPrio;
    }

    // ------------------------------------------------------------------------
    // Operations
    // ------------------------------------------------------------------------

    /**
     * Creates one trace file reader per trace file contained in the trace.
     *
     * @throws CTFException
     *             if an error occurs
     */
    private void createStreamInputReaders() throws CTFException {
        /*
         * For each stream.
         */
        for (CTFStream stream : fTrace.getStreams()) {
            fStreamInputs.addAll(stream.getStreamInputs());
        }
        /*
         * For each trace file of the stream.
         */
        for (CTFStreamInput streamInput : fStreamInputs) {

            /*
             * Create a reader and add it to the group.
             */
            fStreamInputReaders.add(new CTFStreamInputReader(streamInput, fLive));
        }
    }

    /* (non-Javadoc)
     * @see org.eclipse.tracecompass.internal.ctf.core.trace.ICTFTraceReader#update()
     */
    @Override
    public void update() throws CTFException {
        for (CTFStream stream : fTrace.getStreams()) {
            Set<CTFStreamInput> streamInputs = stream.getStreamInputs();
            for (CTFStreamInput streamInput : streamInputs) {
                /*
                 * If it's not in the group, add it.
                 */
                if (!fStreamInputs.contains(streamInput)) {
                    /*
                     * Create a reader.
                     */
                    CTFStreamInputReader streamInputReader = new CTFStreamInputReader(
                            streamInput, fLive);
                    streamInputReader.readNextEvent();
                    fStreamInputReaders.add(streamInputReader);
                }
            }
        }
        for (CTFStreamInputReader reader : fStreamInputReaders) {
            fPrio.add(reader);
        }
    }

    /* (non-Javadoc)
     * @see org.eclipse.tracecompass.internal.ctf.core.trace.ICTFTraceReader#getEventDeclarations()
     */
    @Override
    public Iterable<IEventDeclaration> getEventDeclarations() {
        ImmutableSet.Builder<IEventDeclaration> builder = new Builder<>();
        for (CTFStreamInputReader sir : fStreamInputReaders) {
            builder.addAll(sir.getEventDeclarations());
        }
        return builder.build();
    }

    /**
     * Initializes the priority queue used to choose the trace file with the
     * lower next event timestamp.
     *
     * @param live
     *            is the trace being read live?
     *
     * @throws CTFException
     *             if an error occurs
     */
    private void populateStreamInputReaderHeap() throws CTFException {
        if (fStreamInputReaders.isEmpty()) {
            fPrio = new PriorityQueue<>(MIN_PRIO_SIZE,
                    new StreamInputReaderTimestampComparator());
            return;
        }

        /*
         * Create the priority queue with a size twice as bigger as the number
         * of reader in order to avoid constant resizing.
         */
        fPrio = new PriorityQueue<>(
                Math.max(fStreamInputReaders.size() * 2, MIN_PRIO_SIZE),
                new StreamInputReaderTimestampComparator());

        for (CTFStreamInputReader reader : fStreamInputReaders) {
            /*
             * Add each trace file reader in the priority queue, if we are able
             * to read an event from it.
             */
            CTFResponse readNextEvent = reader.readNextEvent();
            if (readNextEvent == CTFResponse.OK || readNextEvent == CTFResponse.WAIT) {
                fPrio.add(reader);
            }
        }
    }

    /* (non-Javadoc)
     * @see org.eclipse.tracecompass.internal.ctf.core.trace.ICTFTraceReader#getCurrentEventDef()
     */
    @Override
    public IEventDefinition getCurrentEventDef() {
        CTFStreamInputReader top = getTopStream();
        return (top != null) ? top.getCurrentEvent() : null;
    }

    /* (non-Javadoc)
     * @see org.eclipse.tracecompass.internal.ctf.core.trace.ICTFTraceReader#advance()
     */
    @Override
    public boolean advance() throws CTFException {
        /*
         * Remove the reader from the top of the priority queue.
         */
        CTFStreamInputReader top = fPrio.poll();

        /*
         * If the queue was empty.
         */
        if (top == null) {
            return false;
        }
        /*
         * Read the next event of this reader.
         */
        switch (top.readNextEvent()) {
        case OK: {
            /*
             * Add it back in the queue.
             */
            fPrio.add(top);
            final long topEnd = fTrace.timestampCyclesToNanos(top.getCurrentEvent().getTimestamp());
            setEndTime(Math.max(topEnd, getEndTime()));
            if (top.getCurrentEvent() != null) {
                fEndTime = Math.max(top.getCurrentEvent().getTimestamp(),
                        fEndTime);
            }
            break;
        }
        case WAIT: {
            fPrio.add(top);
            break;
        }
        case FINISH:
            break;
        case ERROR:
        default:
            // something bad happend
        }
        /*
         * If there is no reader in the queue, it means the trace reader reached
         * the end of the trace.
         */
        return hasMoreEvents();
    }

    /* (non-Javadoc)
     * @see org.eclipse.tracecompass.internal.ctf.core.trace.ICTFTraceReader#goToLastEvent()
     */
    @Override
    public void goToLastEvent() throws CTFException {
        seek(getEndTime());
        while (fPrio.size() > 1) {
            advance();
        }
    }

    /* (non-Javadoc)
     * @see org.eclipse.tracecompass.internal.ctf.core.trace.ICTFTraceReader#seek(long)
     */
    @Override
    public boolean seek(long timestamp) throws CTFException {
        /*
         * Remove all the trace readers from the priority queue
         */
        fPrio.clear();
        for (CTFStreamInputReader streamInputReader : fStreamInputReaders) {
            /*
             * Seek the trace reader.
             */
            streamInputReader.seek(timestamp);

            /*
             * Add it to the priority queue if there is a current event.
             */
            if (streamInputReader.getCurrentEvent() != null) {
                fPrio.add(streamInputReader);
            }
        }
        return hasMoreEvents();
    }

    /**
     * Gets the stream with the oldest event
     *
     * @return the stream with the oldest event
     */
    public CTFStreamInputReader getTopStream() {
        return fPrio.peek();
    }

    /* (non-Javadoc)
     * @see org.eclipse.tracecompass.internal.ctf.core.trace.ICTFTraceReader#hasMoreEvents()
     */
    @Override
    public final boolean hasMoreEvents() {
        return fPrio.size() > 0;
    }

    /* (non-Javadoc)
     * @see org.eclipse.tracecompass.internal.ctf.core.trace.ICTFTraceReader#getEndTime()
     */
    @Override
    public long getEndTime() {
        return fEndTime;
    }

    /* (non-Javadoc)
     * @see org.eclipse.tracecompass.internal.ctf.core.trace.ICTFTraceReader#isLive()
     */
    @Override
    public boolean isLive() {
        return fLive;
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = (prime * result) + (int) (fStartTime ^ (fStartTime >>> 32));
        result = (prime * result) + fStreamInputReaders.hashCode();
        result = (prime * result) + ((fTrace == null) ? 0 : fTrace.hashCode());
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
        if (!(obj instanceof CTFTraceReader)) {
            return false;
        }
        CTFTraceReader other = (CTFTraceReader) obj;
        if (!fStreamInputReaders.equals(other.fStreamInputReaders)) {
            return false;
        }
        if (fTrace == null) {
            if (other.fTrace != null) {
                return false;
            }
        } else if (!fTrace.equals(other.fTrace)) {
            return false;
        }
        return true;
    }

    @Override
    public String toString() {
        /* Only for debugging, shouldn't be externalized */
        return "CTFTraceReader [trace=" + fTrace + ']'; //$NON-NLS-1$
    }

    /* (non-Javadoc)
     * @see org.eclipse.tracecompass.internal.ctf.core.trace.ICTFTraceReader#getTrace()
     */
    @Override
    public CTFTrace getTrace() {
        return fTrace;
    }

    /**
     * This will read the entire trace and populate all the indexes. The reader
     * will then be reset to the first event in the trace.
     *
     * Do not call in the fast path.
     *
     * @throws CTFException
     *             A trace reading error occurred
     * @since 1.0
     */
    public void populateIndex() throws CTFException {
        for (CTFStreamInputReader sir : fPrio) {
            sir.goToLastEvent();
        }
        seek(0);

    }

    @Override
    public String getCurrrentFileName() {
        return fPrio.peek().getFilename();
    }
}
