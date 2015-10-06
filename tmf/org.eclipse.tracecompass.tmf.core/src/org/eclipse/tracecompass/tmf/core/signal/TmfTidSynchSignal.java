/*******************************************************************************
 * Copyright (c) 2009, 2014 Ericsson
 *
 * All rights reserved. This program and the accompanying materials are
 * made available under the terms of the Eclipse Public License v1.0 which
 * accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *   Francois Chouinard - Initial API and implementation
 *   Patrick Tasse - Support selection range
 *******************************************************************************/

package org.eclipse.tracecompass.tmf.core.signal;


/**
 * A new time or time range selection has been made.
 *
 * This is the selected time or time range. To synchronize on the visible
 * (zoom) range, use {@link TmfRangeSynchSignal}.
 *
 * @version 1.0
 * @author Francois Chouinard
 * @since 2.0
*/
public class TmfTidSynchSignal extends TmfSignal {

    private final int fTid;

    /**
     * Constructor
     *
     * @param source
     *            Object sending this signal
     * @param tid
     *            Tid of selection
     * @since 2.0
     */
    public TmfTidSynchSignal(Object source, int tid) {
        super(source);
        fTid = tid;
    }

    /**
     * @return The begin timestamp of selection
     * @since 2.1
     */
    public int getTid() {
        return fTid;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("[TmfTidSynchSignal ("); //$NON-NLS-1$
        sb.append('-' + fTid);
        sb.append(")]"); //$NON-NLS-1$
        return sb.toString();
    }
}
