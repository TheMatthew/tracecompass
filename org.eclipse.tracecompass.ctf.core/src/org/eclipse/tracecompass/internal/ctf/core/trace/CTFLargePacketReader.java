package org.eclipse.tracecompass.internal.ctf.core.trace;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.channels.FileChannel.MapMode;

import org.eclipse.jdt.annotation.NonNull;
import org.eclipse.tracecompass.common.core.NonNullUtils;
import org.eclipse.tracecompass.ctf.core.CTFException;
import org.eclipse.tracecompass.ctf.core.event.IEventDefinition;
import org.eclipse.tracecompass.ctf.core.event.io.BitBuffer;
import org.eclipse.tracecompass.ctf.core.event.types.ICompositeDefinition;
import org.eclipse.tracecompass.ctf.core.trace.CTFIOException;
import org.eclipse.tracecompass.ctf.core.trace.CTFStream;
import org.eclipse.tracecompass.ctf.core.trace.CTFStreamInput;
import org.eclipse.tracecompass.ctf.core.trace.ICTFPacketDescriptor;
import org.eclipse.tracecompass.internal.ctf.core.SafeMappedByteBuffer;

public class CTFLargePacketReader extends AbstractPacketReader {

    private static final int END_THRESHOLD = 4096 * Byte.SIZE;
    private static final long CHUNK_SIZE = 1024 * 1024;
    private transient BitBuffer fBitBuffer;
    private boolean fHasLost;
    private long fLostEventsDuration;
    private ICompositeDefinition fCurrentStreamEventHeaderDef;
    private long fPositionBits;

    private final ICompositeDefinition fCurrentStreamPacketContextDef;
    private final ICompositeDefinition fCurrentTracePacketHeaderDef;
    private FileChannel fFc;

    public CTFLargePacketReader(ICTFPacketDescriptor packet, CTFStream stream, CTFStreamInput streamInput, CTFStreamInputReader ctfStreamInputReader, ICTFPacketDescriptor prevPacket, FileChannel fc) throws CTFException {
        super(packet, stream, streamInput, ctfStreamInputReader);
        fFc = fc;
        /*
         * make an initial bitBuffer
         */
        @NonNull
        ByteBuffer bb;
        try {
            bb = NonNullUtils.checkNotNull(SafeMappedByteBuffer.map(fFc, MapMode.READ_ONLY, 0, CHUNK_SIZE));
        } catch (IOException e) {
            throw new CTFIOException(e);
        }
        fBitBuffer = new BitBuffer(bb);
        /*
         * Read trace packet header.
         */
        fCurrentTracePacketHeaderDef = (getTracePacketHeaderDecl() != null) ? getTracePacketHeaderDefinition(fBitBuffer) : null;

        /*
         * Read stream packet context.
         */
        fCurrentStreamPacketContextDef = (getStreamPacketContextDecl() != null) ? getStreamPacketContextDefinition(fBitBuffer) : null;
        fPositionBits = fBitBuffer.position();
        if (fCurrentStreamPacketContextDef != null) {

            /* Read CPU ID */

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
                    lostEventsStartTime = packet.getTimestampBegin() + 1;
                } else {
                    lostEventsStartTime = prevPacket.getTimestampEnd();
                }
                fLostEventsDuration = Math.abs(lostEventsStartTime - packet.getTimestampBegin());
            }
        }
    }

    @Override
    public final boolean hasMoreEvents() {
        return fHasLost || (fPositionBits < getPacketInformation().getContentSizeBits());
    }

    @Override
    public IEventDefinition readNextEvent() throws CTFException {
        if (fHasLost) {
            fHasLost = false;
            return createLostEvent(fLostEventsDuration);
        }
        if (bitBufferNearEnd() && !fileNearEnd()) {
            long newStartPos = fBitBuffer.position();
            @NonNull
            ByteBuffer bb;
            try {
                bb = NonNullUtils.checkNotNull(SafeMappedByteBuffer.map(fFc, MapMode.READ_ONLY, newStartPos / Byte.SIZE, Math.min(CHUNK_SIZE, remain())));
            } catch (IOException e) {
                throw new CTFIOException(e);
            }
            fBitBuffer = new BitBuffer(bb);
            fBitBuffer.position(newStartPos & 0x7);
        }
        long posOld = fBitBuffer.position();

        IEventDefinition readEvent = readEvent(fBitBuffer);
        fPositionBits += fBitBuffer.position() - posOld;
        return readEvent;
    }

    private long remain() throws CTFIOException {
        try {
            return fFc.size() * Byte.SIZE - fPositionBits;
        } catch (IOException e) {
            throw new CTFIOException(e);
        }
    }

    private boolean fileNearEnd() throws CTFIOException {
        return remain() < END_THRESHOLD;
    }

    private boolean bitBufferNearEnd() {
        return ((long) fBitBuffer.getByteBuffer().remaining() * Byte.SIZE) < (fBitBuffer.position() & ~0x07L + END_THRESHOLD);
    }

    @Override
    public ICompositeDefinition getCurrentPacketEventHeader() {
        return fCurrentStreamEventHeaderDef;
    }

    @Override
    protected ICompositeDefinition getCurrentStreamPacketContextDef() {
        return fCurrentStreamPacketContextDef;
    }

    @Override
    protected ICompositeDefinition getCurrentTracePacketHeaderDef() {
        return fCurrentTracePacketHeaderDef;
    }

}
