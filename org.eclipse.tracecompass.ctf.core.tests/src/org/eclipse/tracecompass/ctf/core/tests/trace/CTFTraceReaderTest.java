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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assume.assumeTrue;

import org.eclipse.tracecompass.ctf.core.CTFException;
import org.eclipse.tracecompass.ctf.core.event.IEventDefinition;
import org.eclipse.tracecompass.ctf.core.tests.shared.CtfTestTrace;
import org.eclipse.tracecompass.ctf.core.trace.CTFTrace;
import org.eclipse.tracecompass.ctf.core.trace.ICTFTraceReader;
import org.eclipse.tracecompass.internal.ctf.core.trace.CTFTraceReader;
import org.junit.Before;
import org.junit.Test;

/**
 * The class <code>CTFTraceReaderTest</code> contains tests for the class
 * <code>{@link CTFTraceReader}</code>.
 *
 * @author ematkho
 * @version $Revision: 1.0 $
 */
@SuppressWarnings("javadoc")
public class CTFTraceReaderTest {

    private static final CtfTestTrace testTrace = CtfTestTrace.KERNEL;

    private ICTFTraceReader fixture;

    /**
     * Perform pre-test initialization.
     *
     * @throws CTFException
     */
    @Before
    public void setUp() throws CTFException {
        assumeTrue(testTrace.exists());
        fixture = new CTFTraceReader(testTrace.getTrace());
    }

    /**
     * Run the CTFTraceReader(CTFTrace) constructor test. Open a known good
     * trace.
     *
     * @throws CTFException
     */
    @Test
    public void testOpen_existing() throws CTFException {
        CTFTrace trace = testTrace.getTrace();
        try (ICTFTraceReader result = new CTFTraceReader(trace);) {
            assertNotNull(result);
        }
    }

    /**
     * Run the CTFTraceReader(CTFTrace) constructor test. Open a non-existing
     * trace, expect the exception.
     *
     * @throws CTFException
     */
    @Test(expected = org.eclipse.tracecompass.ctf.core.CTFException.class)
    public void testOpen_nonexisting() throws CTFException {
        CTFTrace trace = new CTFTrace("badfile.bad");
        try (ICTFTraceReader result = new CTFTraceReader(trace);) {
            assertNotNull(result);
        }
    }

    /**
     * Run the CTFTraceReader(CTFTrace) constructor test. Try to pen an invalid
     * path, expect exception.
     *
     * @throws CTFException
     */
    @Test(expected = org.eclipse.tracecompass.ctf.core.CTFException.class)
    public void testOpen_invalid() throws CTFException {
        CTFTrace trace = new CTFTrace("");
        try (ICTFTraceReader result = new CTFTraceReader(trace);) {
            assertNotNull(result);
        }
    }

    /**
     * Run the boolean advance() method test. Test advancing normally.
     *
     * @throws CTFException
     *             error
     */
    @Test
    public void testAdvance_normal() throws CTFException {
        boolean result = fixture.advance();
        assertTrue(result);
    }

    /**
     * Run the boolean advance() method test. Test advancing when we're at the
     * end, so we expect that there is no more events.
     *
     * @throws CTFException
     *             error
     */
    @Test
    public void testAdvance_end() throws CTFException {
        int i = 0;
        boolean result = fixture.advance();
        while (result) {
            result = fixture.advance();
            i++;
        }
        fixture.seek(0);
        fixture.advance();
        fixture.goToLastEvent();
        i = 1;
        result = fixture.advance();
        while (result) {
            result = fixture.advance();
            i++;
        }
        assertFalse(result);
        assertEquals(i, 1);
    }

    /**
     * Run the CTFTraceReader copy constructor test.
     *
     * @throws CTFException
     *             error
     */
    @Test
    public void testCopyFrom() throws CTFException {
        try (ICTFTraceReader result = fixture.copyFrom();) {
            assertNotNull(result);
        }
    }

    /**
     * Test the hashCode method.
     */
    @Test
    public void testHash() {
        int result = fixture.hashCode();
        assertTrue(0 != result);
    }

    /**
     * Test the equals method. Uses the class-wide 'fixture' and another
     * method-local 'fixture2', which both point to the same trace.
     *
     * Both trace reader are different objects, so they shouldn't "equals" each
     * other.
     *
     * @throws CTFException
     */
    @Test
    public void testEquals() throws CTFException {
        try (ICTFTraceReader fixture2 = new CTFTraceReader(testTrace.getTrace());) {
            assertEquals(fixture, fixture2);
        }
    }

    /**
     * Run the getCurrentEventDef() method test. Get the first event's
     * definition.
     */
    @Test
    public void testGetCurrentEventDef_first() {
        IEventDefinition result = fixture.getCurrentEventDef();
        assertNotNull(result);
    }

    /**
     * Run the getCurrentEventDef() method test. Get the last event's
     * definition.
     *
     * @throws CTFException
     *             error
     */
    @Test
    public void testGetCurrentEventDef_last() throws CTFException {
        fixture.goToLastEvent();
        IEventDefinition result = fixture.getCurrentEventDef();
        assertNotNull(result);
    }

    /**
     * Run the long getEndTime() method test.
     */
    @Test
    public void testGetEndTime() {
        long result = fixture.getEndTime();
        assertTrue(0L < result);
    }

    /**
     * Run the long getStartTime() method test.
     */
    @Test
    public void testGetStartTime() {
        long result = fixture.getStartTime();
        assertTrue(0L < result);
    }

    /**
     * Run the void goToLastEvent() method test.
     *
     * @throws CTFException
     *             error
     */
    @Test
    public void testGoToLastEvent() throws CTFException {
        fixture.goToLastEvent();
        long ts1 = getTimestamp();
        long ts2 = fixture.getEndTime();
        assertEquals(ts1, ts2);
    }

    /**
     * Run the boolean hasMoreEvents() method test.
     *
     * @throws CTFException
     */
    @Test
    public void testHasMoreEvents() {
        boolean result = fixture.hasMoreEvents();
        assertTrue(result);
    }

    /**
     * Run the boolean seek(long) method test.
     *
     * @throws CTFException
     *             error
     */
    @Test
    public void testSeek() throws CTFException {
        long timestamp = 1L;
        boolean result = fixture.seek(timestamp);
        assertTrue(result);
    }

    /**
     * @return
     */
    private long getTimestamp() {
        if (fixture.getCurrentEventDef() != null) {
            return fixture.getTrace().timestampCyclesToNanos(fixture.getCurrentEventDef().getTimestamp());
        }
        return -1;
    }
}
