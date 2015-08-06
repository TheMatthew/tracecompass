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

package org.eclipse.tracecompass.ctf.core.trace;

import java.io.File;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.channels.FileChannel;
import java.nio.channels.FileChannel.MapMode;
import java.nio.file.StandardOpenOption;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import org.eclipse.tracecompass.common.core.NonNullUtils;
import org.eclipse.tracecompass.ctf.core.CTFException;
import org.eclipse.tracecompass.ctf.core.event.EventDefinition;
import org.eclipse.tracecompass.ctf.core.event.IEventDeclaration;
import org.eclipse.tracecompass.ctf.core.event.io.BitBuffer;
import org.eclipse.tracecompass.ctf.core.event.types.IDeclaration;
import org.eclipse.tracecompass.ctf.core.event.types.StructDeclaration;
import org.eclipse.tracecompass.ctf.core.event.types.StructDefinition;
import org.eclipse.tracecompass.internal.ctf.core.Activator;
import org.eclipse.tracecompass.internal.ctf.core.SafeMappedByteBuffer;
import org.eclipse.tracecompass.internal.ctf.core.trace.CTFPacketReader;
import org.eclipse.tracecompass.internal.ctf.core.trace.NullPacketReader;

import com.google.common.collect.ImmutableList;

/**
 * A CTF trace event reader. Reads the events of a trace file.
 *
 * @author Matthew Khouzam
 * @author Simon Marchi
 */
@NonNullByDefault
public class CTFStreamInputReader implements AutoCloseable {

    // ------------------------------------------------------------------------
    // Attributes
    // ------------------------------------------------------------------------

    /**
     * The StreamInput we are reading.
     */
    private final File fFile;

    private final CTFStreamInput fStreamInput;

    private final @Nullable FileChannel fFileChannel;

    /**
     * The packet reader used to read packets from this trace file.
     */
    private IPacketReader fPacketReader;
    @Deprecated
    private @Nullable final CTFStreamInputPacketReader fPacketReader_ = null;

    /**
     * Iterator on the packet index
     */
    private int fPacketIndex;

    /**
     * Reference to the current event of this trace file (iow, the last on that
     * was read, the next one to be returned)
     */
    private @Nullable EventDefinition fCurrentEvent = null;

    private int fId;

    /**
     * Live trace reading
     */
    private boolean fLive = false;

    // ------------------------------------------------------------------------
    // Constructors
    // ------------------------------------------------------------------------
    /**
     * Constructs a StreamInputReader that reads a StreamInput.
     *
     * @param streamInput
     *            The StreamInput to read.
     * @throws CTFException
     *             If the file cannot be opened
     */
    public CTFStreamInputReader(CTFStreamInput streamInput) throws CTFException {
        fStreamInput = streamInput;
        fFile = fStreamInput.getFile();
        try {
            fFileChannel = FileChannel.open(fFile.toPath(), StandardOpenOption.READ);
        } catch (IOException e) {
            throw new CTFIOException(e);
        }
        try {
            /*
             * Get the iterator on the packet index.
             */
            fPacketIndex = 0;
            /*
             * Make first packet the current one.
             */
            // did we already index the packet?
            if (getPacketSize() < (fPacketIndex + 1)) {
                // go to the next packet if there is one, index it at the same time
                if (fStreamInput.addPacketHeaderIndex()) {
                    fPacketIndex = getPacketSize() - 1;
                }
            }
            ICTFPacketDescriptor packet = getPacket();
            fPacketReader = getCurrentPacketReader(packet);
        } catch (Exception e) {
            try {
                close();
            } catch (IOException e1) {
                // Ignore
            }
            throw e;
        }
    }

    private IPacketReader getCurrentPacketReader(@Nullable ICTFPacketDescriptor packet) throws CTFException {
        IPacketReader ctfPacketReader = NullPacketReader.INSTANCE;
        if (packet != null) {
            BitBuffer bitBuffer = new BitBuffer(getByteBufferAt(packet.getOffsetBytes(), packet.getContentSizeBits()));
            IDeclaration eventHeaderDeclaration = NonNullUtils.checkNotNull(getStreamInput().getStream().getEventHeaderDeclaration());
            CTFTrace trace = getStreamInput().getStream().getTrace();
            StructDefinition packetHeaderDef = NonNullUtils.checkNotNull(trace.getPacketHeaderDef());
            ctfPacketReader = new CTFPacketReader(bitBuffer, packet, getEventDeclarations(), eventHeaderDeclaration, getStreamEventContextDecl(), packetHeaderDef, trace);
        }
        return ctfPacketReader;
    }

    /**
     * Get a bytebuffer map of the file
     *
     * @param position
     *            start offset
     * @param size
     *            size of the map, use caution
     * @return a byte buffer
     * @throws CTFException
     *             if the map failed in its allocation
     *
     * @since 1.1
     */
    public ByteBuffer getByteBufferAt(long position, long size) throws CTFException {
        ByteBuffer map;
        try {
            map = SafeMappedByteBuffer.map(fFileChannel, MapMode.READ_ONLY, position, size);
        } catch (IOException e) {
            throw new CTFIOException(e.getMessage(), e);
        }
        if (map == null) {
            throw new CTFIOException("Failed to allocate mapped byte buffer"); //$NON-NLS-1$
        }
        return map;
    }

    /**
     * Dispose the StreamInputReader, closes the file channel and its packet
     * reader
     *
     * @throws IOException
     *             If an I/O error occurs
     */
    @Override
    public void close() throws IOException {
        if (fFileChannel != null) {
            fFileChannel.close();
        }
        fPacketReader = NullPacketReader.INSTANCE;
    }

    // ------------------------------------------------------------------------
    // Getters/Setters/Predicates
    // ------------------------------------------------------------------------

    /**
     * Gets the current event in this stream
     *
     * @return the current event in the stream, null if the stream is
     *         finished/empty/malformed
     */
    public @Nullable EventDefinition getCurrentEvent() {
        return fCurrentEvent;
    }

    /**
     * Gets the byte order for a trace
     *
     * @return the trace byte order
     */
    public @Nullable ByteOrder getByteOrder() {
        return fStreamInput.getStream().getByteOrder();
    }

    /**
     * Sets the parent trace reader... don't use it
     *
     * @param deprecated
     *            do not use
     * @deprecated not necessary, adds coupling for nothing
     */
    @Deprecated
    public void setParent(CTFTraceReader deprecated) {
    }

    /**
     * Gets the parent trace reader... don't use it
     *
     * @return deprecated
     *            do not use
     * @deprecated not necessary, adds coupling for nothing
     */
    @Deprecated
    public @Nullable CTFTraceReader getParent() {
        return null;
    }

    /**
     * Gets the name of the stream (it's an id and a number)
     *
     * @return gets the stream name (it's a number)
     */
    public int getName() {
        return fId;
    }

    /**
     * Sets the name of the stream
     *
     * @param name
     *            the name of the stream, (it's a number)
     */
    public void setName(int name) {
        fId = name;
    }

    /**
     * Gets the CPU of a stream. It's the same as the one in /proc or running
     * the asm CPUID instruction
     *
     * @return The CPU id (a number)
     */
    public int getCPU() {
        return fPacketReader.getCPU();
    }

    /**
     * Gets the filename of the stream being read
     *
     * @return The filename of the stream being read
     */
    public String getFilename() {
        return fStreamInput.getFilename();
    }

    /*
     * for internal use only
     */
    CTFStreamInput getStreamInput() {
        return fStreamInput;
    }

    /**
     * Gets the event definition set for this StreamInput
     *
     * @return Unmodifiable set with the event definitions
     */
    public Iterable<IEventDeclaration> getEventDeclarations() {
        return NonNullUtils.checkNotNull(ImmutableList.copyOf(fStreamInput.getStream().getEventDeclarations()));
    }

    /**
     * Set the trace to live mode
     *
     * @param live
     *            whether the trace is read live or not
     */
    public void setLive(boolean live) {
        fLive = live;
    }

    /**
     * Get if the trace is to read live or not
     *
     * @return whether the trace is live or not
     */
    public boolean isLive() {
        return fLive;
    }

    /**
     * Get the event context of the stream
     *
     * @return the event context declaration of the stream
     */
    public @Nullable StructDeclaration getStreamEventContextDecl() {
        return getStreamInput().getStream().getEventContextDecl();
    }

    // ------------------------------------------------------------------------
    // Operations
    // ------------------------------------------------------------------------
    /**
     * Reads the next event in the current event variable.
     *
     * @return If an event has been successfully read.
     * @throws CTFException
     *             if an error occurs
     */
    public CTFResponse readNextEvent() throws CTFException {

        /*
         * Change packet if needed
         */
        if (!fPacketReader.hasMoreEvents()) {
            final ICTFPacketDescriptor prevPacket = fPacketReader.getCurrentPacket();
            if (prevPacket != null || fLive) {
                goToNextPacket();
            }

        }

        /*
         * If an event is available, read it.
         */
        if (fPacketReader.hasMoreEvents()) {
            setCurrentEvent(fPacketReader.readNextEvent());
            return CTFResponse.OK;
        }
        this.setCurrentEvent(null);
        return fLive ? CTFResponse.WAIT : CTFResponse.FINISH;
    }

    /**
     * Change the current packet of the packet reader to the next one.
     *
     * @throws CTFException
     *             if an error occurs
     */
    private void goToNextPacket() throws CTFException {
        fPacketIndex++;
        // did we already index the packet?
        if (getPacketSize() < (fPacketIndex + 1)) {
            // go to the next packet if there is one, index it at the same time
            if (fStreamInput.addPacketHeaderIndex()) {
                fPacketIndex = getPacketSize() - 1;
            }
        }
        ICTFPacketDescriptor packet = getPacket();
        fPacketReader = getCurrentPacketReader(packet);

    }

    /**
     * @return
     */
    private int getPacketSize() {
        return fStreamInput.getIndex().size();
    }

    /**
     * Changes the location of the trace file reader so that the current event
     * is the first event with a timestamp greater or equal the given timestamp.
     *
     * @param timestamp
     *            The timestamp to seek to.
     * @return The offset compared to the current position
     * @throws CTFException
     *             if an error occurs
     */
    public long seek(long timestamp) throws CTFException {
        long offset = 0;

        gotoPacket(timestamp);

        /*
         * index up to the desired timestamp.
         */
        while ((fPacketReader.getCurrentPacket() != null)
                && (fPacketReader.getCurrentPacket().getTimestampEnd() < timestamp)) {
            try {
                fStreamInput.addPacketHeaderIndex();
                goToNextPacket();
            } catch (CTFException e) {
                // do nothing here
                Activator.log(e.getMessage());
            }
        }
        if (fPacketReader.getCurrentPacket() == null) {
            gotoPacket(timestamp);
        }

        /*
         * Advance until either of these conditions are met:
         *
         * - reached the end of the trace file (the given timestamp is after the
         * last event)
         *
         * - found the first event with a timestamp greater or equal the given
         * timestamp.
         */
        readNextEvent();
        EventDefinition currentEvent;
        while (((currentEvent = getCurrentEvent()) != null) && (currentEvent.getTimestamp() < timestamp)) {
            readNextEvent();
            currentEvent = getCurrentEvent();
            offset++;
        }
        return offset;
    }

    /**
     * @param timestamp
     *            the time to seek
     * @throws CTFException
     *             if an error occurs
     */
    private void gotoPacket(long timestamp) throws CTFException {
        fPacketIndex = fStreamInput.getIndex().search(timestamp).previousIndex();
        /*
         * Switch to this packet.
         */
        goToNextPacket();
    }

    /**
     * Seeks the last event of a stream and returns it.
     *
     * @throws CTFException
     *             if an error occurs
     */
    public void goToLastEvent() throws CTFException {

        /*
         * Go to the beginning of the trace
         */
        seek(0);

        /*
         * Check that there is at least one event
         */
        if ((fStreamInput.getIndex().isEmpty()) || (!fPacketReader.hasMoreEvents())) {
            /*
             * This means the trace is empty. abort.
             */
            return;
        }

        fPacketIndex = fStreamInput.getIndex().size() - 1;
        /*
         * Go to last indexed packet
         */
        fPacketReader = getCurrentPacketReader(getPacket());

        /*
         * Keep going until you cannot
         */
        while (fPacketReader.getCurrentPacket() != null) {
            goToNextPacket();
        }

        final int lastPacketIndex = fStreamInput.getIndex().size() - 1;
        /*
         * Go to the last packet that contains events.
         */
        for (int pos = lastPacketIndex; pos > 0; pos--) {
            fPacketIndex = pos;
            fPacketReader = getCurrentPacketReader(getPacket());

            if (fPacketReader.hasMoreEvents()) {
                break;
            }
        }

        /*
         * Go until the end of that packet
         */
        EventDefinition prevEvent = null;
        while (fCurrentEvent != null) {
            prevEvent = fCurrentEvent;
            this.readNextEvent();
        }
        /*
         * Go back to the previous event
         */
        this.setCurrentEvent(prevEvent);
    }

    /**
     * Sets the current event in a stream input reader
     *
     * @param currentEvent
     *            the event to set
     */
    public void setCurrentEvent(@Nullable EventDefinition currentEvent) {
        fCurrentEvent = currentEvent;
    }

    /**
     * @return the packetIndexIt
     */
    private int getPacketIndex() {
        return fPacketIndex;
    }

    private @Nullable ICTFPacketDescriptor getPacket() {
        return fStreamInput.getIndex().getElement(getPacketIndex());
    }

    /**
     * @return the packetReader
     */
    @Deprecated
    public @Nullable CTFStreamInputPacketReader getPacketReader() {
        return fPacketReader_;
    }

    /**
     * Get the current packet reader
     *
     * @return the current packet reader
     * @since 1.1
     */
    public IPacketReader getCurrentPacketReader() {
        return fPacketReader;
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = (prime * result) + fId;
        result = (prime * result)
                + fFile.hashCode();
        return result;
    }

    @Override
    public boolean equals(@Nullable Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null) {
            return false;
        }
        if (!(obj instanceof CTFStreamInputReader)) {
            return false;
        }
        CTFStreamInputReader other = (CTFStreamInputReader) obj;
        if (fId != other.fId) {
            return false;
        }
        return fFile.equals(other.fFile);
    }

    @Override
    public String toString() {
        // this helps debugging
        return fId + ' ' + NonNullUtils.nullToEmptyString(fCurrentEvent);
    }

}
