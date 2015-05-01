/*******************************************************************************
 * Copyright (c) 2011, 2015 Ericsson, Ecole Polytechnique de Montreal and others
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
import org.eclipse.tracecompass.ctf.core.event.IEventDefinition;
import org.eclipse.tracecompass.ctf.core.event.io.BitBuffer;
import org.eclipse.tracecompass.ctf.core.event.types.ICompositeDefinition;
import org.eclipse.tracecompass.ctf.core.trace.CTFStream;
import org.eclipse.tracecompass.ctf.core.trace.CTFStreamInput;
import org.eclipse.tracecompass.ctf.core.trace.ICTFPacketDescriptor;

/**
 * CTF trace packet reader. Reads the events of a packet of a trace file.
 *
 * @author Matthew Khouzam
 * @author Simon Marchi
 */
public final class CTFStreamInputPacketReader extends AbstractPacketReader {

    // ------------------------------------------------------------------------
    // Attributes
    // ------------------------------------------------------------------------

    /** BitBuffer used to read the trace file. */
    @NonNull
    private final BitBuffer fBitBuffer;

    private final ICompositeDefinition fCurrentStreamPacketContextDef;
    private final ICompositeDefinition fCurrentTracePacketHeaderDef;
    private boolean fHasLost = false;

    private long fLostEventsDuration;

    // ------------------------------------------------------------------------
    // Constructors
    // ------------------------------------------------------------------------

    /**
     * Constructor
     *
     * @param currentPacket
     *            The index entry of the packet to switch to.
     * @param stream
     *            the parent stream
     * @param streamInput
     *            the stream input
     * @param streamInputReader
     *            the parent stream input reader
     * @param prevPacket
     *            previous packet
     * @param bb
     *            bit buffer
     * @throws CTFException
     *             If we get an error reading the packet
     * @since 1.0
     */
    public CTFStreamInputPacketReader(@NonNull ICTFPacketDescriptor currentPacket, CTFStream stream, CTFStreamInput streamInput, CTFStreamInputReader streamInputReader, @Nullable ICTFPacketDescriptor prevPacket, @NonNull ByteBuffer bb)
            throws CTFException {
        super(currentPacket, stream, streamInput, streamInputReader);
        /*
         * Change the map of the BitBuffer.
         */
        fBitBuffer = new BitBuffer(bb);
        /*
         * Read trace packet header.
         */
        fCurrentTracePacketHeaderDef = (getTracePacketHeaderDecl() != null) ? getTracePacketHeaderDefinition(fBitBuffer) : null;

        /*
         * Read stream packet context.
         */
        fCurrentStreamPacketContextDef = (getStreamPacketContextDecl() != null) ? getStreamPacketContextDefinition(fBitBuffer) : null;

        if (fCurrentStreamPacketContextDef != null) {

            /* Read number of lost events */
            if ((int) getPacketInformation().getLostEvents() != 0) {
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
        }
    }

    // ------------------------------------------------------------------------
    // Getters/Setters/Predicates
    // ------------------------------------------------------------------------

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
     * Returns whether it is possible to read any more events from this packet.
     *
     * @return True if it is possible to read any more events from this packet.
     */
    @Override
    public final boolean hasMoreEvents() {
        return fHasLost || (fBitBuffer.position() < getPacketInformation().getContentSizeBits());
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
    public final IEventDefinition readNextEvent() throws CTFException {
        if (fHasLost) {
            fHasLost = false;
            return createLostEvent(fLostEventsDuration);
        }
        return readEvent(fBitBuffer);
    }

    @Override
    protected final ICompositeDefinition getCurrentStreamPacketContextDef() {
        return fCurrentStreamPacketContextDef;
    }

    @Override
    protected final ICompositeDefinition getCurrentTracePacketHeaderDef() {
        return fCurrentTracePacketHeaderDef;
    }

}
