/*******************************************************************************
 * Copyright (c) 2013, 2014 Ericsson
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Matthew Khouzam - Initial API and implementation
 *******************************************************************************/

package org.eclipse.tracecompass.ctf.core.tests.trace;

import static org.junit.Assert.assertNotNull;

import org.eclipse.tracecompass.ctf.core.CTFException;
import org.eclipse.tracecompass.internal.ctf.core.trace.CTFPacketContext;
import org.eclipse.tracecompass.internal.ctf.core.trace.PacketDescriptorIndex;
import org.junit.Before;
import org.junit.Test;

/**
 * The class <code>PacketDescriptorIndex</code> contains tests for the class
 * <code>{@link PacketDescriptorIndex}</code>.
 *
 */
public class PacketDescriptorIndexTest {

    private PacketDescriptorIndex fixture;

    /**
     * Perform pre-test initialization.
     *
     * @throws CTFException
     *             an error occured
     */
    @Before
    public void setUp() throws CTFException {
        fixture = new PacketDescriptorIndex();
        fixture.append(new CTFPacketContext(1L, 0L));
    }

    /**
     * Run the StreamInputPacketIndex() constructor test.
     */
    @Test
    public void testStreamInputPacketIndex() {
        assertNotNull(fixture);
        assertNotNull(fixture.getElement(0));
    }

}