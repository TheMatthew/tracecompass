/*******************************************************************************
 * Copyright (c) 2012, 2014 Ericsson, École Polytechnique de Montréal
 *
 * All rights reserved. This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License v1.0 which
 * accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *   Matthew Khouzam - Initial API and implementation
 *   Florian Wininger - Performance improvements
 *******************************************************************************/

package org.eclipse.tracecompass.internal.tmf.ctf.core.trace.iterator;

import static org.eclipse.tracecompass.common.core.NonNullUtils.equalsNullable;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.eclipse.jdt.annotation.NonNull;
import org.eclipse.tracecompass.common.core.NonNullUtils;
import org.eclipse.tracecompass.ctf.core.CTFException;
import org.eclipse.tracecompass.ctf.core.event.IEventDeclaration;
import org.eclipse.tracecompass.ctf.core.event.IEventDefinition;
import org.eclipse.tracecompass.ctf.core.trace.CTFTrace;
import org.eclipse.tracecompass.ctf.core.trace.ICTFTraceReader;
import org.eclipse.tracecompass.internal.tmf.ctf.core.Activator;
import org.eclipse.tracecompass.tmf.core.event.ITmfEventField;
import org.eclipse.tracecompass.tmf.core.event.TmfEventField;
import org.eclipse.tracecompass.tmf.core.trace.ITmfContext;
import org.eclipse.tracecompass.tmf.core.trace.location.ITmfLocation;
import org.eclipse.tracecompass.tmf.ctf.core.context.CtfLocation;
import org.eclipse.tracecompass.tmf.ctf.core.context.CtfLocationInfo;
import org.eclipse.tracecompass.tmf.ctf.core.event.CtfTmfEvent;
import org.eclipse.tracecompass.tmf.ctf.core.event.CtfTmfEventFactory;
import org.eclipse.tracecompass.tmf.ctf.core.event.CtfTmfEventType;
import org.eclipse.tracecompass.tmf.ctf.core.trace.CtfTmfTrace;

import com.google.common.collect.ImmutableMap;

/**
 * The CTF trace reader iterator.
 *
 * It doesn't reserve a file handle, so many iterators can be used without
 * worries of I/O errors or resource exhaustion.
 *
 * @author Matthew Khouzam
 */
public class CtfIterator
        implements ITmfContext, Comparable<CtfIterator>, AutoCloseable {

    /** An invalid location */
    public static final CtfLocation NULL_LOCATION = new CtfLocation(CtfLocation.INVALID_LOCATION);

    private final CtfTmfTrace fTrace;
    private final ICTFTraceReader fReader;

    private CtfLocation fCurLocation;
    private long fCurRank;

    // ------------------------------------------------------------------------
    // Constructors
    // ------------------------------------------------------------------------

    /**
     * Create a new CTF trace iterator, which initially points at the first
     * event in the trace.
     *
     * @param ctfTrace
     *            The {@link CTFTrace} linked to the trace. It should be
     *            provided by the corresponding 'ctfTmfTrace'.
     *
     * @param ctfTmfTrace
     *            The {@link CtfTmfTrace} to iterate over
     * @throws CTFException
     *             If the iterator couldn't not be instantiated, probably due to
     *             a read error.
     */
    public CtfIterator(CTFTrace ctfTrace, CtfTmfTrace ctfTmfTrace) throws CTFException {

        fTrace = ctfTmfTrace;
        fReader = ctfTrace.createReader(false);
        if (fReader.hasMoreEvents()) {
            fCurLocation = new CtfLocation(ctfTmfTrace.getStartTime());
            fCurRank = 0;
        } else {
            setUnknownLocation();
        }
    }

    /**
     * Create a new CTF trace iterator, which will initially point to the given
     * location/rank.
     *
     * @param ctfTrace
     *            The {@link CTFTrace} linked to the trace. It should be
     *            provided by the corresponding 'ctfTmfTrace'.
     * @param ctfTmfTrace
     *            The {@link CtfTmfTrace} to iterate over
     * @param ctfLocationData
     *            The initial timestamp the iterator will be pointing to
     * @param rank
     *            The initial rank
     * @throws CTFException
     *             If the iterator couldn't not be instantiated, probably due to
     *             a read error.
     */
    public CtfIterator(CTFTrace ctfTrace, CtfTmfTrace ctfTmfTrace, CtfLocationInfo ctfLocationData, long rank)
            throws CTFException {

        fTrace = ctfTmfTrace;
        fReader = ctfTrace.createReader(false);
        if (fReader.hasMoreEvents()) {
            fCurLocation = new CtfLocation(ctfLocationData);
            if (this.getCurrentEvent().getTimestamp().getValue() != ctfLocationData.getTimestamp()) {
                this.seek(ctfLocationData);
                this.fCurRank = rank;
            }
        } else {
            setUnknownLocation();
        }
    }

    @Override
    public void dispose() {
        close();
    }

    @Override
    public void close() {
        fReader.close();
    }

    private void setUnknownLocation() {
        fCurLocation = NULL_LOCATION;
        fCurRank = UNKNOWN_RANK;
    }

    // ------------------------------------------------------------------------
    // Accessors
    // ------------------------------------------------------------------------

    /**
     * Return this iterator's trace.
     *
     * @return CtfTmfTrace The iterator's trace
     */
    public CtfTmfTrace getCtfTmfTrace() {
        return fTrace;
    }

    /**
     * Return the current event pointed to by the iterator.
     *
     * @return CtfTmfEvent The current event
     */
    public synchronized CtfTmfEvent getCurrentEvent() {
        return CtfTmfEventFactory.createEvent(fReader.getCurrentEventDef(), fReader.getCurrrentFileName(), fTrace);
    }

    /**
     * Return the current timestamp location pointed to by the iterator. This is
     * the timestamp for use in CtfLocation, not the event timestamp.
     *
     * @return long The current timestamp location
     */
    public synchronized long getCurrentTimestamp() {
        IEventDefinition currentEventDef = fReader.getCurrentEventDef();
        if (currentEventDef != null) {
            long ts = currentEventDef.getTimestamp();
            return fTrace.timestampCyclesToNanos(ts);
        }
        return 0;
    }

    /**
     * Seek this iterator to a given location.
     *
     * @param ctfLocationData
     *            The LocationData representing the position to seek to
     * @return boolean True if the seek was successful, false if there was an
     *         error seeking.
     */
    public synchronized boolean seek(CtfLocationInfo ctfLocationData) {
        boolean ret = false;

        /* Avoid the cost of seeking at the current location. */
        if (fCurLocation.getLocationInfo().equals(ctfLocationData)) {
            return fReader.hasMoreEvents();
        }

        /* Adjust the timestamp depending on the trace's offset */
        long currTimestamp = ctfLocationData.getTimestamp();
        final long offsetTimestamp = this.getCtfTmfTrace().timestampNanoToCycles(currTimestamp);
        try {
            if (offsetTimestamp < 0) {
                ret = fReader.seek(0L);
            } else {
                ret = fReader.seek(offsetTimestamp);
            }
        } catch (CTFException e) {
            Activator.getDefault().logError(e.getMessage(), e);
            return false;
        }
        /*
         * Check if there is already one or more events for that timestamp, and
         * assign the location index correctly
         */
        long index = 0;
        final CtfTmfEvent currentEvent = this.getCurrentEvent();
        if (currentEvent != null) {
            currTimestamp = currentEvent.getTimestamp().getValue();

            for (long i = 0; i < ctfLocationData.getIndex(); i++) {
                if (currTimestamp == currentEvent.getTimestamp().getValue()) {
                    index++;
                } else {
                    index = 0;
                }
                this.advance();
            }
        } else {
            ret = false;
        }
        /* Seek the current location accordingly */
        if (ret) {
            fCurLocation = new CtfLocation(new CtfLocationInfo(getCurrentEvent().getTimestamp().getValue(), index));
        } else {
            fCurLocation = NULL_LOCATION;
        }

        return ret;
    }

    // ------------------------------------------------------------------------
    // CTFTraceReader
    // ------------------------------------------------------------------------

    /**
     * Seek to a given timestamp
     *
     * @param timestamp
     *            the timestamp
     * @return whether the seek was successful or not
     */
    public boolean seek(long timestamp) {
        return seek(new CtfLocationInfo(timestamp, 0));
    }

    /**
     * Advance the iterator to the next event
     *
     * @return if the operation is successful
     */
    public synchronized boolean advance() {
        boolean ret = false;
        try {
            ret = fReader.advance();
        } catch (CTFException e) {
            Activator.getDefault().logError(e.getMessage(), e);
        }

        if (ret) {
            long timestamp = fCurLocation.getLocationInfo().getTimestamp();
            final long timestampValue = getCurrentTimestamp();
            if (timestamp == timestampValue) {
                long index = fCurLocation.getLocationInfo().getIndex();
                fCurLocation = new CtfLocation(timestampValue, index + 1);
            } else {
                fCurLocation = new CtfLocation(timestampValue, 0L);
            }
        } else {
            fCurLocation = NULL_LOCATION;
        }
        return ret;
    }

    // ------------------------------------------------------------------------
    // ITmfContext
    // ------------------------------------------------------------------------

    @Override
    public long getRank() {
        return fCurRank;
    }

    @Override
    public void setRank(long rank) {
        fCurRank = rank;
    }

    @Override
    public void increaseRank() {
        /* Only increase the rank if it's valid */
        if (hasValidRank()) {
            fCurRank++;
        }
    }

    @Override
    public boolean hasValidRank() {
        return (getRank() >= 0);
    }

    @Override
    public void setLocation(ITmfLocation location) {
        // FIXME alex: isn't there a cleaner way than a cast here?
        fCurLocation = (CtfLocation) location;
        seek(((CtfLocation) location).getLocationInfo());
    }

    @Override
    public CtfLocation getLocation() {
        return fCurLocation;
    }

    /**
     * Get the start time of the of trace
     *
     * @return get the start time of the trace
     */
    public long getStartTime() {
        return fReader.getStartTime();
    }

    /**
     * Get the end time of the trace
     *
     * @return the end time of the trace
     */
    public long getEndTime() {
        return fReader.getEndTime();
    }

    /**
     * Get the event types, slow, don't use in the fast path
     *
     * @return a Map of the event types
     */
    @NonNull
    public Map<String, CtfTmfEventType> getEventTypes() {
        ImmutableMap.Builder<String, CtfTmfEventType> returnBuilder = new ImmutableMap.Builder<>();
        List<ITmfEventField> content = new ArrayList<>();
        for (IEventDeclaration ied : fReader.getEventDeclarations()) {
            String eventName = ied.getName();
            /* Should only return null the first time */
            for (String fieldName : ied.getFields().getFieldsList()) {
                content.add(new TmfEventField(fieldName, null, null));
            }
            ITmfEventField contentTree = new TmfEventField(
                    ITmfEventField.ROOT_FIELD_ID,
                    null,
                    content.toArray(new ITmfEventField[content.size()])
                    );

            CtfTmfEventType ctfTmfEventType = new CtfTmfEventType(eventName, contentTree);
            returnBuilder.put(eventName, ctfTmfEventType);
        }
        return NonNullUtils.checkNotNull(returnBuilder.build());
    }

    // ------------------------------------------------------------------------
    // Comparable
    // ------------------------------------------------------------------------

    @Override
    public int compareTo(final CtfIterator o) {
        if (getRank() < o.getRank()) {
            return -1;
        } else if (getRank() > o.getRank()) {
            return 1;
        }
        return 0;
    }

    // ------------------------------------------------------------------------
    // Object
    // ------------------------------------------------------------------------

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = super.hashCode();
        result = (prime * result)
                + ((fTrace == null) ? 0 : fTrace.hashCode());
        result = (prime * result)
                + ((fCurLocation == null) ? 0 : fCurLocation.hashCode());
        result = (prime * result) + (int) (fCurRank ^ (fCurRank >>> 32));
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (!super.equals(obj)) {
            return false;
        }
        if (!(obj instanceof CtfIterator)) {
            return false;
        }
        CtfIterator other = (CtfIterator) obj;
        if (!equalsNullable(fTrace, other.fTrace)) {
            return false;
        }
        if (!equalsNullable(fCurLocation, other.fCurLocation)) {
            return false;
        }
        if (fCurRank != other.fCurRank) {
            return false;
        }
        return true;
    }
}
