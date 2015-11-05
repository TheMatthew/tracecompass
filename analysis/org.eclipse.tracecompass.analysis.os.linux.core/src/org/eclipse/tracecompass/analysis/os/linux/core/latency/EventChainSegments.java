/*******************************************************************************
 * Copyright (c) 2015 Ericsson
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/

package org.eclipse.tracecompass.analysis.os.linux.core.latency;

import java.util.List;

import static org.eclipse.tracecompass.common.core.NonNullUtils.checkNotNull;

import org.eclipse.tracecompass.segmentstore.core.BasicSegment;
import org.eclipse.tracecompass.segmentstore.core.ISegment;

import com.google.common.collect.ImmutableList;

/**
 * @author Bernd Hufmann
 * @since 2.0
 */
public class EventChainSegments extends BasicSegment implements ISegementChain {

    private static final long serialVersionUID = -5122817746069095913L;

    private final List<ISegment> fSubSegments;

    /**
     * Create a new segment.
     *
     * The end position should be equal to or greater than the start position.
     *
     * @param start
     *            Start position of the segment
     * @param end
     *            End position of the segment
     * @param segments
     *            List of sub segments
     */
    public EventChainSegments(long start, long end, List<ISegment> segments) {
        super(start, end, "chain"); //$NON-NLS-1$
        fSubSegments = checkNotNull(ImmutableList.copyOf(segments));
    }

    @Override
    public List<ISegment> getSubSegments() {
        return fSubSegments;
    }

    @Override
    public String toString() {
        StringBuilder builder = new StringBuilder();
        builder.append(super.toString());
        builder.append(", sub-segments["); //$NON-NLS-1$
        for (ISegment segment : fSubSegments) {
            builder.append(segment.toString());
        }
        builder.append("]"); //$NON-NLS-1$
        return checkNotNull(builder.toString());
    }
}
