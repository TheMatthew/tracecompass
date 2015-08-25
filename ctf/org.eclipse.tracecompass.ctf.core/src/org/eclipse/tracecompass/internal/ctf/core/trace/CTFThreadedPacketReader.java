/*******************************************************************************
 * Copyright (c) 2015 Ericsson
 *
 * All rights reserved. This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License v1.0 which
 * accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/

package org.eclipse.tracecompass.internal.ctf.core.trace;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.Executor;

import org.eclipse.jdt.annotation.NonNull;
import org.eclipse.jdt.annotation.Nullable;
import org.eclipse.tracecompass.ctf.core.CTFException;
import org.eclipse.tracecompass.ctf.core.event.EventDefinition;
import org.eclipse.tracecompass.ctf.core.event.IEventDeclaration;
import org.eclipse.tracecompass.ctf.core.event.io.BitBuffer;
import org.eclipse.tracecompass.ctf.core.event.scope.IDefinitionScope;
import org.eclipse.tracecompass.ctf.core.event.types.ICompositeDefinition;
import org.eclipse.tracecompass.ctf.core.event.types.IDeclaration;
import org.eclipse.tracecompass.ctf.core.event.types.StructDeclaration;
import org.eclipse.tracecompass.ctf.core.trace.ICTFPacketDescriptor;
import org.eclipse.tracecompass.ctf.core.trace.IPacketReader;

/**
 * Threaded Packet Reader
 *
 * @author Matthew Khouzam - Initial API and implementation
 *
 */
public class CTFThreadedPacketReader implements IPacketReader {

    private static final int QUEUE_LENGTH = 63;

    private static final @NonNull List<EventDefinition> POISON_PILL = new ArrayList<>(0);

    private final CTFException[] fException = new CTFException[1];

    private final CTFPacketReader fPacketReader;

    private final BlockingQueue<List<EventDefinition>> fEvents = new ArrayBlockingQueue<>(QUEUE_LENGTH);

    private List<EventDefinition> fNextEvent;
    private EventDefinition fCurrentEvent;

    private final Runnable fRunnable = new Runnable() {
        private static final int CHUNK_SIZE = 1023;

        @Override
        public void run() {
            try {
                List<EventDefinition> chunk = new ArrayList<>(CHUNK_SIZE);
                while (fPacketReader.hasMoreEvents()) {

                    chunk.add(fPacketReader.readNextEvent());

                    if (chunk.size() == 1023) {
                        fEvents.put(chunk);
                    }
                }
                fEvents.put(POISON_PILL);
            } catch (CTFException | InterruptedException ex) {
                fException[0] = new CTFException(ex);
            }
        }
    };

    /**
     * Constructor
     *
     * @param executor
     *            Executor to enqueue packet reader
     * @param input
     *            input {@link BitBuffer}
     * @param packetContext
     *            packet_context where we get info like lost events and cpu_id
     * @param declarations
     *            event declarations for this packet reader
     * @param eventHeaderDeclaration
     *            event header declaration, what to read before any given event,
     *            to find it's id
     * @param streamContext
     *            the context declaration
     * @param packetHeader
     *            the header with the magic numbers and such
     * @param packetScope
     *            the scope of the packetHeader
     * @throws CTFException
     *             A ctf error or a timeout
     */
    public CTFThreadedPacketReader(Executor executor, @NonNull BitBuffer input, @NonNull ICTFPacketDescriptor packetContext, @NonNull List<IEventDeclaration> declarations, @Nullable IDeclaration eventHeaderDeclaration, @Nullable StructDeclaration streamContext,
            @NonNull ICompositeDefinition packetHeader,
            @NonNull IDefinitionScope packetScope) throws CTFException {
        fPacketReader = new CTFPacketReader(input, packetContext, declarations, eventHeaderDeclaration, streamContext, packetHeader, packetScope);
        if (input.canRead(1)) {
            fNextEvent = Collections.singletonList(fPacketReader.readNextEvent());
        } else {
            fNextEvent = POISON_PILL;
        }
        executor.execute(fRunnable);
    }

    @Override
    public int getCPU() {
        return fCurrentEvent.getCPU();
    }

    @Override
    public boolean hasMoreEvents() {
        return fNextEvent != POISON_PILL;
    }

    int pos = 0;

    @Override
    public EventDefinition readNextEvent() throws CTFException {
        fCurrentEvent = fNextEvent.get(pos);
        pos++;
        if (pos == fNextEvent.size()) {
            try {
                fNextEvent = fEvents.take();
            } catch (InterruptedException e) {
                throw new CTFException(e);
            }
        }
        if (fNextEvent == POISON_PILL && fException[0] != null) {
            throw fException[0];
        }
        return fCurrentEvent;

    }

    @Override
    public ICTFPacketDescriptor getCurrentPacket() {
        return fPacketReader.getCurrentPacket();
    }

    @Override
    public ICompositeDefinition getCurrentPacketEventHeader() {
        return fPacketReader.getCurrentPacketEventHeader();
    }

}
