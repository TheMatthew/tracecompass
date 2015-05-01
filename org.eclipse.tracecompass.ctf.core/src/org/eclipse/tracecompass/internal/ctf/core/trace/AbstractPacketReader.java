package org.eclipse.tracecompass.internal.ctf.core.trace;

import org.eclipse.jdt.annotation.NonNull;
import org.eclipse.tracecompass.ctf.core.CTFException;
import org.eclipse.tracecompass.ctf.core.CTFStrings;
import org.eclipse.tracecompass.ctf.core.event.IEventDeclaration;
import org.eclipse.tracecompass.ctf.core.event.IEventDefinition;
import org.eclipse.tracecompass.ctf.core.event.io.BitBuffer;
import org.eclipse.tracecompass.ctf.core.event.scope.IDefinitionScope;
import org.eclipse.tracecompass.ctf.core.event.scope.ILexicalScope;
import org.eclipse.tracecompass.ctf.core.event.scope.LexicalScope;
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
import org.eclipse.tracecompass.ctf.core.trace.ICTFPacketDescriptor;
import org.eclipse.tracecompass.internal.ctf.core.event.EventDeclaration;
import org.eclipse.tracecompass.internal.ctf.core.event.EventDefinition;
import org.eclipse.tracecompass.internal.ctf.core.event.types.composite.EventHeaderDefinition;

import com.google.common.collect.ImmutableList;

public abstract class AbstractPacketReader implements IPacketReader {
    // ------------------------------------------------------------------------
    // Attributes
    // ------------------------------------------------------------------------

    /** Reference to the index entry of the current packet. */
    @NonNull
    private final ICTFPacketDescriptor fPacket;

    /** Stream event context definition. */
    private final StructDeclaration fStreamEventContextDecl;

    /** Stream event header definition. */
    private final IDeclaration fStreamEventHeaderDecl;

    /** Stream packet context definition. */
    private final StructDeclaration fStreamPacketContextDecl;

    /** Trace packet header. */
    private final StructDeclaration fTracePacketHeaderDecl;

    private ICompositeDefinition fCurrentStreamEventHeaderDef;

    private final IDefinitionScope fStreamInputScope;

    private final IDefinitionScope fTraceScope;

    private final CTFStream fStream;
    /**
     * Last timestamp recorded.
     *
     * Needed to calculate the complete timestamp values for the events with
     * compact headers.
     */
    private long fLastTimestamp;

    private long fLostEventsInThisPacket;

    private final int fCurrentCpu;

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
     * @since 1.0
     */
    public AbstractPacketReader(@NonNull ICTFPacketDescriptor currentPacket, CTFStream stream, CTFStreamInput streamInput, CTFStreamInputReader streamInputReader) {
        fStream = stream;
        fStreamInputScope = streamInput;
        fTraceScope = stream.getTrace();
        fPacket = currentPacket;
        fTracePacketHeaderDecl = stream.getTrace().getPacketHeader();
        fStreamPacketContextDecl = stream.getPacketContextDecl();
        fStreamEventHeaderDecl = stream.getEventHeaderDeclaration();
        fStreamEventContextDecl = stream.getEventContextDecl();
        fLastTimestamp = currentPacket.getTimestampBegin();
        fLostEventsInThisPacket = currentPacket.getLostEvents();

        /* Read CPU ID */
        fCurrentCpu = (getPacketInformation().getTarget() != null) ? (int) getPacketInformation().getTargetId() : 0;

    }

    protected final IDeclaration getStreamEventHeaderDecl() {
        return fStreamEventHeaderDecl;
    }

    protected final StructDeclaration getStreamPacketContextDecl() {
        return fStreamPacketContextDecl;
    }

    protected final StructDeclaration getTracePacketHeaderDecl() {
        return fTracePacketHeaderDecl;
    }

    protected final IDefinitionScope getStreamInputScope() {
        return fStreamInputScope;
    }

    protected final IEventDeclaration getEventDeclaration(int id) {
        return fStream.getEventDeclaration(id);
    }

    protected final IDefinitionScope getTraceScope() {
        return fTraceScope;
    }

    @Override
    public int getCPU() {
        return fCurrentCpu;
    }

    /**
     * Gets the packet information
     *
     * @return the packet information
     * @since 1.0
     */
    @Override
    public final ICTFPacketDescriptor getPacketInformation() {
        return fPacket;
    }

    protected final StructDeclaration getStreamEventContextDecl() {
        return fStreamEventContextDecl;
    }

    @Override
    public final LexicalScope getScopePath() {
        return ILexicalScope.PACKET;
    }

    @Override
    public final IDefinition lookupDefinition(String lookupPath) {
        if (lookupPath.equals(ILexicalScope.STREAM_PACKET_CONTEXT.getPath())) {
            return getCurrentStreamPacketContextDef();
        }
        if (lookupPath.equals(ILexicalScope.TRACE_PACKET_HEADER.getPath())) {
            return getCurrentTracePacketHeaderDef();
        }
        return null;
    }

    protected abstract ICompositeDefinition getCurrentStreamPacketContextDef();

    protected abstract ICompositeDefinition getCurrentTracePacketHeaderDef();

    /**
     * Calculates the timestamp value of the event, possibly using the timestamp
     * from the last event.
     *
     * @param timestampDef
     *            Integer definition of the timestamp.
     * @return The calculated timestamp value.
     */
    protected final long calculateTimestamp(IntegerDefinition timestampDef) {
        int len = timestampDef.getDeclaration().getLength();
        final long value = timestampDef.getValue();
        return calculateTimestamp(value, len);
    }

    protected final long calculateTimestamp(final long value, int len) {
        long newval;
        long majorasbitmask;
        /*
         * If the timestamp length is 64 bits, it is a full timestamp.
         */
        if (len == Long.SIZE) {
            fLastTimestamp = value;
            return getLastTimestamp();
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
        if (newval < (getLastTimestamp() & majorasbitmask)) {
            newval = newval + (1L << len);
        }

        /* Keep only the high bits of the old value */
        fLastTimestamp = getLastTimestamp() & ~majorasbitmask;

        /* Then add the low bits of the new value */
        fLastTimestamp = getLastTimestamp() + newval;

        return getLastTimestamp();
    }

    protected final long getLastTimestamp() {
        return fLastTimestamp;
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
        return getStreamPacketContextDecl().createDefinition(getStreamInputScope(), ILexicalScope.STREAM_PACKET_CONTEXT, input);
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
        return getTracePacketHeaderDecl().createDefinition(getTraceScope(), ILexicalScope.TRACE_PACKET_HEADER, input);
    }

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
        return getStreamEventContextDecl().createDefinition(getStreamInputScope(), ILexicalScope.STREAM_EVENT_CONTEXT, input);
    }

    protected final IEventDefinition createLostEvent(long lostEventsDuration) {
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
        IntegerDefinition lostDurationDef = new IntegerDefinition(lostFieldsDecl, null, CTFStrings.LOST_EVENTS_DURATION, lostEventsDuration);
        IntegerDefinition lostCountDef = new IntegerDefinition(lostEventsDurationDecl, null, CTFStrings.LOST_EVENTS_FIELD, fLostEventsInThisPacket);
        IntegerDefinition[] fields = new IntegerDefinition[] { lostCountDef, lostDurationDef };
        /* this is weird notation, but it's the java notation */
        final ImmutableList<String> fieldNameList = ImmutableList.<String> builder().add(CTFStrings.LOST_EVENTS_FIELD).add(CTFStrings.LOST_EVENTS_DURATION).build();
        return new EventDefinition(
                lostEventDeclaration,
                getLastTimestamp(),
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

    protected IEventDefinition readEvent(BitBuffer bitBuffer) throws CTFException, CTFIOException {
        int eventID = (int) EventDeclaration.UNSET_EVENT_ID;
        long timestamp = 0;
        final long posStart = bitBuffer.position();
        /* Read the stream event header. */
        IDeclaration streamEventHeaderDecl = getStreamEventHeaderDecl();
        if (streamEventHeaderDecl != null) {
            if (streamEventHeaderDecl instanceof IEventHeaderDeclaration) {
                fCurrentStreamEventHeaderDef = (ICompositeDefinition) streamEventHeaderDecl.createDefinition(null, "", bitBuffer); //$NON-NLS-1$
                EventHeaderDefinition ehd = (EventHeaderDefinition) fCurrentStreamEventHeaderDef;
                eventID = ehd.getId();
                timestamp = calculateTimestamp(ehd.getTimestamp(), ehd.getTimestampLength());
            } else {
                fCurrentStreamEventHeaderDef = ((StructDeclaration) streamEventHeaderDecl).createDefinition(null, ILexicalScope.EVENT_HEADER, bitBuffer);
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
        IEventDeclaration eventDeclaration = getEventDeclaration(eventID);
        if (eventDeclaration == null) {
            throw new CTFIOException("Incorrect event id : " + eventID); //$NON-NLS-1$
        }
        IEventDefinition eventDef = createEventDefiniton(eventDeclaration, bitBuffer, timestamp);

        /*
         * Set the event timestamp using the timestamp calculated by
         * updateTimestamp.
         */

        if (posStart == bitBuffer.position()) {
            throw new CTFIOException("Empty event not allowed, event: " + eventDef.getDeclaration().getName()); //$NON-NLS-1$
        }
        return eventDef;
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

    private IEventDefinition createEventDefiniton(IEventDeclaration eventDeclaration, BitBuffer bitBuffer, long timestamp) throws CTFException {
        return eventDeclaration.createDefinition(getStreamEventContextDecl(), getPacketInformation(), bitBuffer, timestamp);
    }
}
