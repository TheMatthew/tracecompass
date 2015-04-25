package org.eclipse.tracecompass.ctf.core.trace;

import org.eclipse.jdt.annotation.NonNull;
import org.eclipse.tracecompass.ctf.core.CTFException;
import org.eclipse.tracecompass.ctf.core.event.IEventDefinition;
import org.eclipse.tracecompass.ctf.core.event.io.BitBuffer;
import org.eclipse.tracecompass.ctf.core.event.scope.IDefinitionScope;
import org.eclipse.tracecompass.ctf.core.event.types.ICompositeDefinition;

/**
 * Stream input reader interface
 * @since 1.0
 */
public interface IPacketReader extends IDefinitionScope {

    /**
     * Gets the CPU (core) number
     *
     * @return the CPU (core) number
     */
    int getCPU();

    /**
     * Gets the packet information
     *
     * @return the packet information
     * @since 1.0
     */
    ICTFPacketDescriptor getPacketInformation();

    /**
     * Get the current packet event header
     *
     * @return the current packet event header
     * @since 1.0
     */
    ICompositeDefinition getCurrentPacketEventHeader();

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
    ICompositeDefinition getEventContextDefinition(@NonNull BitBuffer input) throws CTFException;

    /**
     * Get stream event header
     *
     * @return the stream event header
     */
    ICompositeDefinition getStreamEventHeaderDefinition();

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
    ICompositeDefinition getStreamPacketContextDefinition(@NonNull BitBuffer input) throws CTFException;

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
    ICompositeDefinition getTracePacketHeaderDefinition(@NonNull BitBuffer input) throws CTFException;

    /**
     * Returns whether it is possible to read any more events from this packet.
     *
     * @return True if it is possible to read any more events from this packet.
     */
    boolean hasMoreEvents();

    /**
     * Reads the next event of the packet into the right event definition.
     *
     * @return The event definition containing the event data that was just
     *         read.
     * @throws CTFException
     *             If there was a problem reading the trace
     */
    IEventDefinition readNextEvent() throws CTFException;

}