/*******************************************************************************
 * Copyright (c) 2015 Ericsson, Ecole Polytechnique de Montreal and others
 *
 * All rights reserved. This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License v1.0 which
 * accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Matthew Khouzam - Initial API and implementation
 *******************************************************************************/

package org.eclipse.tracecompass.ctf.core.trace;

import org.eclipse.tracecompass.ctf.core.CTFException;
import org.eclipse.tracecompass.ctf.core.event.IEventDeclaration;
import org.eclipse.tracecompass.ctf.core.event.IEventDefinition;

/**
 * Trace reader interface
 *
 * @since 1.0
 */
public interface ICTFTraceReader extends AutoCloseable {

    /**
     * Copy constructor
     *
     * @return The new CTFTraceReader
     * @throws CTFException
     *             if an error occurs
     */
    ICTFTraceReader copyFrom() throws CTFException;

    /**
     * Return the start time of this trace (== timestamp of the first event)
     *
     * @return the trace start time
     */
    long getStartTime();

    /**
     * Update the priority queue to make it match the parent trace
     *
     * @throws CTFException
     *             An error occured
     */
    void update() throws CTFException;

    /**
     * Gets an iterable of the stream input readers, useful for foreaches
     *
     * @return the iterable of the stream input readers
     */
    Iterable<IEventDeclaration> getEventDeclarations();

    /**
     * Get the current event, which is the current event of the trace file
     * reader with the lowest timestamp.
     *
     * @return An event definition, or null of the trace reader reached the end
     *         of the trace.
     * @since 1.0
     */
    IEventDefinition getCurrentEventDef();

    /**
     * Go to the next event.
     *
     * @return True if an event was read.
     * @throws CTFException
     *             if an error occurs
     */
    boolean advance() throws CTFException;

    /**
     * Go to the last event in the trace.
     *
     * @throws CTFException
     *             if an error occurs
     */
    void goToLastEvent() throws CTFException;

    /**
     * Seeks to a given timestamp. It will seek to the nearest event greater or
     * equal to timestamp. If a trace is [10 20 30 40] and you are looking for
     * 19, it will give you 20. If you want 20, you will get 20, if you want 21,
     * you will get 30. The value -inf will seek to the first element and the
     * value +inf will seek to the end of the file (past the last event).
     *
     * @param timestamp
     *            the timestamp to seek to
     * @return true if there are events above or equal the seek timestamp, false
     *         if seek at the end of the trace (no valid event).
     * @throws CTFException
     *             if an error occurs
     */
    boolean seek(long timestamp) throws CTFException;

    /**
     * Does the trace have more events?
     *
     * @return true if yes.
     */
    boolean hasMoreEvents();

    /**
     * Gets the last event timestamp that was read. This is NOT necessarily the
     * last event in a trace, just the last one read so far.
     *
     * @return the last event
     */
    long getEndTime();

    /**
     * Get if the trace is to read live or not
     *
     * @return whether the trace is live or not
     */
    boolean isLive();

    /**
     * Gets the parent trace
     *
     * @return the parent trace
     */
    CTFTrace getTrace();

    @Override
    public void close();

    /**
     * Gets the file name of the file that generated the current event
     *
     * @return the current file name
     */
    String getCurrrentFileName();

}