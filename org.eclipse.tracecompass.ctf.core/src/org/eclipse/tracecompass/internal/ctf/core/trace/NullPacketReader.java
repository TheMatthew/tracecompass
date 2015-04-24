package org.eclipse.tracecompass.internal.ctf.core.trace;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import org.eclipse.tracecompass.ctf.core.CTFException;
import org.eclipse.tracecompass.ctf.core.event.EventDefinition;
import org.eclipse.tracecompass.ctf.core.event.io.BitBuffer;
import org.eclipse.tracecompass.ctf.core.event.scope.LexicalScope;
import org.eclipse.tracecompass.ctf.core.event.types.Definition;
import org.eclipse.tracecompass.ctf.core.event.types.ICompositeDefinition;
import org.eclipse.tracecompass.ctf.core.trace.ICTFPacketDescriptor;
import org.eclipse.tracecompass.ctf.core.trace.IPacketReader;

/**
 * Null packet reader
 * @since 1.0
 */
@NonNullByDefault
public final class NullPacketReader implements IPacketReader {

    private NullPacketReader() {
    }

    @Override
    public int getCPU() {
        return 0;
    }

    @Override
    @Nullable public ICTFPacketDescriptor getPacketInformation() {
        return null;
    }

    @Override
    @Nullable public ICompositeDefinition getCurrentPacketEventHeader() {
        return null;
    }

    @Override
    @Nullable public ICompositeDefinition getEventContextDefinition(BitBuffer input) throws CTFException {
        return null;
    }

    @Override
    @Nullable public LexicalScope getScopePath() {
        return null;
    }

    @Override
    @Nullable public ICompositeDefinition getStreamEventHeaderDefinition() {
        return null;
    }

    @Override
    @Nullable public ICompositeDefinition getStreamPacketContextDefinition(BitBuffer input) throws CTFException {
        return null;
    }

    @Override
    @Nullable public ICompositeDefinition getTracePacketHeaderDefinition(BitBuffer input) throws CTFException {
        return null;
    }

    @Override
    public boolean hasMoreEvents() {
        return false;
    }

    @Override
    @Nullable public Definition lookupDefinition(@Nullable String lookupPath) {
        return null;
    }

    @Override
    @Nullable public EventDefinition readNextEvent() throws CTFException {
        return null;
    }

    private static final NullPacketReader INSTANCE = new NullPacketReader();

    /**
     * Get the null packet reader
     * @return the null packet reader
     */
    public static IPacketReader getInstance() {
        return INSTANCE;
    }

}
