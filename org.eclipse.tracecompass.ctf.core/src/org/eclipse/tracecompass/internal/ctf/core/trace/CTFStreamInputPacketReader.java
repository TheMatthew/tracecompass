/*******************************************************************************
 * Copyright (c) 2011, 2014 Ericsson, Ecole Polytechnique de Montreal and others
 *
 * All rights reserved. This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License v1.0 which
 * accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors: Matthew Khouzam - Initial API and implementation
 * Contributors: Simon Marchi - Initial API and implementation
 *******************************************************************************/
package org.eclipse.tracecompass.internal.ctf.core.trace;

import java.nio.ByteBuffer;

import org.eclipse.jdt.annotation.NonNull;
import org.eclipse.jdt.annotation.Nullable;
import org.eclipse.tracecompass.ctf.core.CTFException;
import org.eclipse.tracecompass.ctf.core.CTFStrings;
import org.eclipse.tracecompass.ctf.core.event.EventDefinition;
import org.eclipse.tracecompass.ctf.core.event.IEventDeclaration;
import org.eclipse.tracecompass.ctf.core.event.io.BitBuffer;
import org.eclipse.tracecompass.ctf.core.event.scope.IDefinitionScope;
import org.eclipse.tracecompass.ctf.core.event.scope.ILexicalScope;
import org.eclipse.tracecompass.ctf.core.event.scope.LexicalScope;
import org.eclipse.tracecompass.ctf.core.event.types.Definition;
import org.eclipse.tracecompass.ctf.core.event.types.ICompositeDefinition;
import org.eclipse.tracecompass.ctf.core.event.types.IDeclaration;
import org.eclipse.tracecompass.ctf.core.event.types.IDefinition;
import org.eclipse.tracecompass.ctf.core.event.types.IEventHeaderDeclaration;
import org.eclipse.tracecompass.ctf.core.event.types.IntegerDeclaration;
import org.eclipse.tracecompass.ctf.core.event.types.IntegerDefinition;
import org.eclipse.tracecompass.ctf.core.event.types.SimpleDatatypeDefinition;
import org.eclipse.tracecompass.ctf.core.event.types.StructDeclaration;
import org.eclipse.tracecompass.ctf.core.event.types.StructDefinition;
import org.eclipse.tracecompass.ctf.core.event.types.VariantDefinition;
import org.eclipse.tracecompass.ctf.core.trace.CTFIOException;
import org.eclipse.tracecompass.ctf.core.trace.CTFStream;
import org.eclipse.tracecompass.ctf.core.trace.CTFStreamInput;
import org.eclipse.tracecompass.ctf.core.trace.CTFStreamInputReader;
import org.eclipse.tracecompass.ctf.core.trace.ICTFPacketDescriptor;
import org.eclipse.tracecompass.ctf.core.trace.IPacketReader;
import org.eclipse.tracecompass.internal.ctf.core.event.EventDeclaration;
import org.eclipse.tracecompass.internal.ctf.core.event.types.composite.EventHeaderDefinition;

import com.google.common.collect.ImmutableList;

/**
 * CTF trace packet reader. Reads the events of a packet of a trace file.
 *
 * @author Matthew Khouzam
 * @author Simon Marchi
 */
public final class CTFStreamInputPacketReader implements IPacketReader {

    // ------------------------------------------------------------------------
    // Attributes
    // ------------------------------------------------------------------------

    /** BitBuffer used to read the trace file. */
    @NonNull
    private final BitBuffer fBitBuffer;

    /** CPU id of current packet. */
    private final int fCurrentCpu;

    /** Reference to the index entry of the current packet. */
    @NonNull
    private final ICTFPacketDescriptor fPacket;

    private ICompositeDefinition fCurrentStreamEventHeaderDef;

    private final ICompositeDefinition fCurrentStreamPacketContextDef;
    private final ICompositeDefinition fCurrentTracePacketHeaderDef;
    private boolean fHasLost = false;
    /**
     * Last timestamp recorded.
     *
     * Needed to calculate the complete timestamp values for the events with
     * compact headers.
     */
    private long fLastTimestamp = 0;

    private long fLostEventsDuration;

    private int fLostEventsInThisPacket;

    /** Stream event context definition. */
    private final StructDeclaration fStreamEventContextDecl;

    /** Stream event header definition. */
    private final IDeclaration fStreamEventHeaderDecl;

    private final CTFStreamInputReader fStreamInputReader;

    /** Stream packet context definition. */
    private final StructDeclaration fStreamPacketContextDecl;

    /** Trace packet header. */
    private final StructDeclaration fTracePacketHeaderDecl;

    private final IDefinitionScope fStreamInputScope;

    private final IDefinitionScope fTraceScope;

    private final CTFStream fStream;

    // ------------------------------------------------------------------------
    // Constructors
    // ------------------------------------------------------------------------

    /**
     * Constructor
     *
     * @param currentPacket
     *            The index entry of the packet to switch to.
     * @param stream
     * @param streamInput
     * @param streamInputReader
     *            the parent stream input reader
     * @param bb
     * @throws CTFException
     *             If we get an error reading the packet
     * @since 1.0
     */
    public CTFStreamInputPacketReader(@NonNull ICTFPacketDescriptor currentPacket, CTFStream stream, CTFStreamInput streamInput, CTFStreamInputReader streamInputReader, @Nullable ICTFPacketDescriptor prevPacket, @NonNull ByteBuffer bb)
            throws CTFException {
        fStream = stream;
        fStreamInputScope = streamInput;
        fTraceScope = stream.getTrace();
        fPacket = currentPacket;
        fStreamInputReader = streamInputReader;
        fTracePacketHeaderDecl = stream.getTrace().getPacketHeader();
        fStreamPacketContextDecl = stream.getPacketContextDecl();
        fStreamEventHeaderDecl = stream.getEventHeaderDeclaration();
        fStreamEventContextDecl = stream.getEventContextDecl();
        /*
         * Change the map of the BitBuffer.
         */
        fBitBuffer = new BitBuffer(bb);
        /*
         * Read trace packet header.
         */
        fCurrentTracePacketHeaderDef = (fTracePacketHeaderDecl != null) ? getTracePacketHeaderDefinition(fBitBuffer) : null;

        /*
         * Read stream packet context.
         */
        fCurrentStreamPacketContextDef = (fStreamPacketContextDecl != null) ? getStreamPacketContextDefinition(fBitBuffer) : null;

        if (fCurrentStreamPacketContextDef != null) {

            /* Read CPU ID */
            fCurrentCpu = (getPacketInformation().getTarget() != null) ? (int) getPacketInformation().getTargetId() : 0;

            /* Read number of lost events */
            fLostEventsInThisPacket = (int) getPacketInformation().getLostEvents();
            if (fLostEventsInThisPacket != 0) {
                fHasLost = true;
                /*
                 * Compute the duration of the lost event time range. If the
                 * current packet is the first packet, duration will be set to
                 * 1.
                 */
                long lostEventsStartTime;
                if (prevPacket == null) {
                    lostEventsStartTime = currentPacket.getTimestampBegin() + 1;
                } else {
                    lostEventsStartTime = prevPacket.getTimestampEnd();
                }
                fLostEventsDuration = Math.abs(lostEventsStartTime - currentPacket.getTimestampBegin());
            }
        } else {
            fCurrentCpu = 0;
        }

        /*
         * Use the timestamp begin of the packet as the reference for the
         * timestamp reconstitution.
         */
        fLastTimestamp = currentPacket.getTimestampBegin();
    }

    /**
     * Calculates the timestamp value of the event, possibly using the timestamp
     * from the last event.
     *
     * @param timestampDef
     *            Integer definition of the timestamp.
     * @return The calculated timestamp value.
     */
    private final long calculateTimestamp(IntegerDefinition timestampDef) {
        int len = timestampDef.getDeclaration().getLength();
        final long value = timestampDef.getValue();

        return calculateTimestamp(value, len);
    }

    private final long calculateTimestamp(final long value, int len) {
        long newval;
        long majorasbitmask;
        /*
         * If the timestamp length is 64 bits, it is a full timestamp.
         */
        if (len == Long.SIZE) {
            fLastTimestamp = value;
            return fLastTimestamp;
        }

        /*
         * Bit mask to keep / remove all old / new bits.
         */
        majorasbitmask = (1L << len) - 1;

        /*
         * If the new value is smaller than the corresponding bits of the last
         * timestamp, we assume an overflow of the compact representation.
         */
        newval = value;
        if (newval < (fLastTimestamp & majorasbitmask)) {
            newval = newval + (1L << len);
        }

        /* Keep only the high bits of the old value */
        fLastTimestamp = fLastTimestamp & ~majorasbitmask;

        /* Then add the low bits of the new value */
        fLastTimestamp = fLastTimestamp + newval;

        return fLastTimestamp;
    }

    // ------------------------------------------------------------------------
    // Getters/Setters/Predicates
    // ------------------------------------------------------------------------

    /**
     * Gets the CPU (core) number
     *
     * @return the CPU (core) number
     */
    @Override
    public final int getCPU() {
        return fCurrentCpu;
    }

    /**
     * Gets the packet information
     *
     * @return
     *
     * @return the packet information
     * @since 1.0
     */
    @Override
    public final ICTFPacketDescriptor getPacketInformation() {
        return fPacket;
    }

    /**
     * Get the current packet event header
     *
     * @return the current packet event header
     * @since 1.0
     */
    @Override
    public final ICompositeDefinition getCurrentPacketEventHeader() {
        return fCurrentTracePacketHeaderDef;
    }

    // ------------------------------------------------------------------------
    // Operations
    // ------------------------------------------------------------------------

    /**
     * Get the event context defintiion
     *
     * @param input
     *            the bitbuffer to read from
     * @return an context definition, can be null
     * @throws CTFException
     *             out of bounds exception or such
     * @since 1.0
     */
    @Override
    public final ICompositeDefinition getEventContextDefinition(BitBuffer input) throws CTFException {
        return fStreamEventContextDecl.createDefinition(fStreamInputScope, ILexicalScope.STREAM_EVENT_CONTEXT, input);
    }

    @Override
    public final LexicalScope getScopePath() {
        return ILexicalScope.PACKET;
    }

    /**
     * Get stream event header
     *
     * @return the stream event header
     */
    @Override
    public final ICompositeDefinition getStreamEventHeaderDefinition() {
        return fCurrentStreamEventHeaderDef;
    }

    /**
     * Get the packet context defintiion
     *
     * @param input
     *            the bitbuffer to read from
     * @return an context definition, can be null
     * @throws CTFException
     *             out of bounds exception or such
     * @since 1.0
     */
    @Override
    public final ICompositeDefinition getStreamPacketContextDefinition(BitBuffer input) throws CTFException {
        return fStreamPacketContextDecl.createDefinition(fStreamInputScope, ILexicalScope.STREAM_PACKET_CONTEXT, input);
    }

    /**
     * Get the event header defintiion
     *
     * @param input
     *            the bitbuffer to read from
     * @return an header definition, can be null
     * @throws CTFException
     *             out of bounds exception or such
     * @since 1.0
     */
    @Override
    public final ICompositeDefinition getTracePacketHeaderDefinition(BitBuffer input) throws CTFException {
        return fTracePacketHeaderDecl.createDefinition(fTraceScope, ILexicalScope.TRACE_PACKET_HEADER, input);
    }

    /**
     * Returns whether it is possible to read any more events from this packet.
     *
     * @return True if it is possible to read any more events from this packet.
     */
    @Override
    public final boolean hasMoreEvents() {
        return fHasLost || (fBitBuffer.position() < fPacket.getContentSizeBits());
    }

    @Override
    public final Definition lookupDefinition(String lookupPath) {
        if (lookupPath.equals(ILexicalScope.STREAM_PACKET_CONTEXT.getPath())) {
            return (Definition) fCurrentStreamPacketContextDef;
        }
        if (lookupPath.equals(ILexicalScope.TRACE_PACKET_HEADER.getPath())) {
            return (Definition) fCurrentTracePacketHeaderDef;
        }
        return null;
    }

    /**
     * Reads the next event of the packet into the right event definition.
     *
     * @return The event definition containing the event data that was just
     *         read.
     * @throws CTFException
     *             If there was a problem reading the trace
     */
    @Override
    public final EventDefinition readNextEvent() throws CTFException {
        /* Default values for those fields */
        // compromise since we cannot have 64 bit addressing of arrays yet.
        int eventID = (int) EventDeclaration.UNSET_EVENT_ID;
        long timestamp = 0;
        if (fHasLost) {
            fHasLost = false;
            EventDeclaration lostEventDeclaration = EventDeclaration.getLostEventDeclaration();
            StructDeclaration lostFields = lostEventDeclaration.getFields();
            // this is a hard coded map, we know it's not null
            IntegerDeclaration lostFieldsDecl = (IntegerDeclaration) lostFields.getField(CTFStrings.LOST_EVENTS_FIELD);
            if (lostFieldsDecl == null)
            {
                throw new IllegalStateException("Lost events count not declared!"); //$NON-NLS-1$
            }
            IntegerDeclaration lostEventsDurationDecl = (IntegerDeclaration) lostFields.getField(CTFStrings.LOST_EVENTS_DURATION);
            if (lostEventsDurationDecl == null) {
                throw new IllegalStateException("Lost events duration not declared!"); //$NON-NLS-1$
            }
            IntegerDefinition lostDurationDef = new IntegerDefinition(lostFieldsDecl, null, CTFStrings.LOST_EVENTS_DURATION, fLostEventsDuration);
            IntegerDefinition lostCountDef = new IntegerDefinition(lostEventsDurationDecl, null, CTFStrings.LOST_EVENTS_FIELD, fLostEventsInThisPacket);
            IntegerDefinition[] fields = new IntegerDefinition[] { lostCountDef, lostDurationDef };
            /* this is weird notation, but it's the java notation */
            final ImmutableList<String> fieldNameList = ImmutableList.<String> builder().add(CTFStrings.LOST_EVENTS_FIELD).add(CTFStrings.LOST_EVENTS_DURATION).build();
            return new EventDefinition(
                    lostEventDeclaration,
                    fStreamInputReader,
                    fLastTimestamp,
                    null,
                    null,
                    null,
                    new StructDefinition(
                            lostFields,
                            this, "fields", //$NON-NLS-1$
                            fieldNameList,
                            fields
                    ));

        }

        final long posStart = fBitBuffer.position();
        /* Read the stream event header. */
        if (fStreamEventHeaderDecl != null) {
            if (fStreamEventHeaderDecl instanceof IEventHeaderDeclaration) {
                fCurrentStreamEventHeaderDef = (ICompositeDefinition) fStreamEventHeaderDecl.createDefinition(null, "", fBitBuffer); //$NON-NLS-1$
                EventHeaderDefinition ehd = (EventHeaderDefinition) fCurrentStreamEventHeaderDef;
                eventID = ehd.getId();
                timestamp = calculateTimestamp(ehd.getTimestamp(), ehd.getTimestampLength());
            } else {
                fCurrentStreamEventHeaderDef = ((StructDeclaration) fStreamEventHeaderDecl).createDefinition(null, ILexicalScope.EVENT_HEADER, fBitBuffer);
                StructDefinition StructEventHeaderDef = (StructDefinition) fCurrentStreamEventHeaderDef;
                /* Check for the event id. */
                IDefinition idDef = StructEventHeaderDef.lookupDefinition("id"); //$NON-NLS-1$
                SimpleDatatypeDefinition simpleIdDef = null;
                if (idDef instanceof SimpleDatatypeDefinition) {
                    simpleIdDef = ((SimpleDatatypeDefinition) idDef);
                } else if (idDef != null) {
                    throw new CTFIOException("Id defintion not an integer, enum or float definiton in event header."); //$NON-NLS-1$
                }

                /*
                 * Get the timestamp from the event header (may be overridden
                 * later on)
                 */
                IntegerDefinition timestampDef = StructEventHeaderDef.lookupInteger("timestamp"); //$NON-NLS-1$

                /* Check for the variant v. */
                IDefinition variantDef = StructEventHeaderDef.lookupDefinition("v"); //$NON-NLS-1$
                if (variantDef instanceof VariantDefinition) {

                    /* Get the variant current field */
                    StructDefinition variantCurrentField = (StructDefinition) ((VariantDefinition) variantDef).getCurrentField();

                    /*
                     * Try to get the id field in the current field of the
                     * variant. If it is present, it overrides the previously
                     * read event id.
                     */
                    IDefinition vIdDef = variantCurrentField.lookupDefinition("id"); //$NON-NLS-1$
                    if (vIdDef instanceof IntegerDefinition) {
                        simpleIdDef = (SimpleDatatypeDefinition) vIdDef;
                    }

                    /*
                     * Get the timestamp. This would overwrite any previous
                     * timestamp definition
                     */
                    timestampDef = variantCurrentField.lookupInteger("timestamp"); //$NON-NLS-1$
                }
                if (simpleIdDef != null) {
                    eventID = simpleIdDef.getIntegerValue().intValue();
                }
                if (timestampDef != null) {
                    timestamp = calculateTimestamp(timestampDef);
                } // else timestamp remains 0
            }
        }
        /* Get the right event definition using the event id. */
        IEventDeclaration eventDeclaration = fStream.getEventDeclaration(eventID);
        if (eventDeclaration == null) {
            throw new CTFIOException("Incorrect event id : " + eventID); //$NON-NLS-1$
        }
        EventDefinition eventDef = eventDeclaration.createDefinition(fStreamInputReader, fBitBuffer, timestamp);

        /*
         * Set the event timestamp using the timestamp calculated by
         * updateTimestamp.
         */

        if (posStart == fBitBuffer.position()) {
            throw new CTFIOException("Empty event not allowed, event: " + eventDef.getDeclaration().getName()); //$NON-NLS-1$
        }

        return eventDef;
    }
}
