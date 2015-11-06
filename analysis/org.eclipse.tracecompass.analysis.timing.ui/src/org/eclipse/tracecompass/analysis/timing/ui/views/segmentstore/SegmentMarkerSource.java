/*******************************************************************************
 * Copyright (c) 2015 Ericsson
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/

package org.eclipse.tracecompass.analysis.timing.ui.views.segmentstore;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jdt.annotation.NonNull;
import org.eclipse.jdt.annotation.Nullable;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.widgets.Display;
import org.eclipse.tracecompass.analysis.timing.core.segmentstore.AbstractSegmentStoreAnalysisModule;
import org.eclipse.tracecompass.segmentstore.core.ISegment;
import org.eclipse.tracecompass.segmentstore.core.ISegmentStore;
import org.eclipse.tracecompass.tmf.ui.widgets.timegraph.model.IMarkerEvent;
import org.eclipse.tracecompass.tmf.ui.widgets.timegraph.model.IMarkerEventSource;
import org.eclipse.tracecompass.tmf.ui.widgets.timegraph.model.MarkerEvent;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableList.Builder;

/**
 * A converter for segments to make them into markers.
 *
 * @author Matthew Khouzam
 *
 */
public class SegmentMarkerSource implements IMarkerEventSource {

    /** key so no internalization is needed */
    private static final String EVEN = "even"; //$NON-NLS-1$

    /** key so no internalization is needed */
    private static final String ODD = "odd"; //$NON-NLS-1$

    private final AbstractSegmentStoreAnalysisModule fModule;

    private final Map<String, Color> fColorMap = new LinkedHashMap<>();

    /**
     * Constructor
     *
     * @param module
     *            the segment store module
     */
    public SegmentMarkerSource(AbstractSegmentStoreAnalysisModule module) {
        fModule = module;
    }

    @Override
    public List<@NonNull String> getMarkerCategories() {
        if (fColorMap.isEmpty()) {
            Display display = Display.getDefault();
            Set<String> keys = Collections.singleton("chain");// results.stream().map(ISegment::getName).collect(Collectors.toSet()); //$NON-NLS-1$
            for (String key : keys) {
                int hashCode = key.hashCode();
                int g = hashCode & 255;
                hashCode >>= 8;
                int r = hashCode & 255;
                hashCode >>= 8;
                int b = hashCode & 255;
                fColorMap.put(key, new Color(display, r, g, b, 128));
            }
            fColorMap.put(ODD, new Color(display, 100, 100, 100, 32));
            fColorMap.put(EVEN, new Color(display, 200, 200, 200, 32));
        }
        return new ArrayList<>(fColorMap.keySet());
    }

    @Override
    public List<@NonNull IMarkerEvent> getMarkerList(@NonNull String category, long startTime, long endTime, long resolution, @NonNull IProgressMonitor monitor) {
        @Nullable
        ISegmentStore<@NonNull ISegment> results = fModule.getResults();
        if (results != null) {
            List<ISegment> intervalList = new ArrayList<>();
            Builder<IMarkerEvent> builder = ImmutableList.<IMarkerEvent> builder();
            results.stream().filter(t -> (t.getEnd() > startTime)).filter(t -> (t.getStart() < endTime)).forEach(c -> builder.add(createMarker(category, c)));
            results.stream().filter(t -> (t.getEnd() > startTime)).filter(t -> (t.getStart() < endTime)).forEach(c -> intervalList.add(c));

            for (int i = 0; i < intervalList.size() - 1; i += 2) {
                long start1 = intervalList.get(i).getStart();
                long start2 = intervalList.get(i + 1).getStart();
                builder.add(new MarkerEvent(null, start1, start2 - start1, ODD, fColorMap.get(ODD), "", true)); //$NON-NLS-1$
                if (intervalList.size() > i + 2) {
                    long start3 = intervalList.get(i + 2).getStart();
                    builder.add(new MarkerEvent(null, start2, start3 - start2, EVEN, fColorMap.get(EVEN), "", true)); //$NON-NLS-1$
                }
            }
            return builder.build();
        }
        return Collections.EMPTY_LIST;
    }

    private MarkerEvent createMarker(String category, ISegment c) {
        return new MarkerEvent(null, c.getStart(), c.getLength(), category, fColorMap.get(category), "", true); //$NON-NLS-1$
    }

}
