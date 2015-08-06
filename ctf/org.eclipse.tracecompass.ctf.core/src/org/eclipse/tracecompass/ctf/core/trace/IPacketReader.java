/*******************************************************************************
 * Copyright (c) 2015 Ericsson
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
import org.eclipse.tracecompass.ctf.core.event.EventDefinition;
import org.eclipse.tracecompass.ctf.core.event.types.ICompositeDefinition;

/**
 * Packet reader interface, allows for more flexible packet readers
 *
 * @since 1.1
 */
public interface IPacketReader {

    /**
     * the value of a cpu if it is unknown to the packet reader
     */
    int UNKNOWN_CPU = -1;

    /**
     * Gets the CPU (core) number
     *
     * @return the CPU (core) number
     */
    int getCPU();

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
    EventDefinition readNextEvent() throws CTFException;

    /**
     * Get the packet being read
     *
     * @return the packet being read
     */
    ICTFPacketDescriptor getCurrentPacket();

    /**
     * Get the current event header definition
     *
     * @return the current event header definition
     */
    ICompositeDefinition getCurrentPacketEventHeader();

}