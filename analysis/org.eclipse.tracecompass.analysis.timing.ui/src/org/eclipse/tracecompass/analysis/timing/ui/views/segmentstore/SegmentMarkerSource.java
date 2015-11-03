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

public class SegmentMarkerSource implements IMarkerEventSource {

    private final AbstractSegmentStoreAnalysisModule fModule;

    private final Map<String, Color> fColorMap = new LinkedHashMap<>();

    public SegmentMarkerSource(AbstractSegmentStoreAnalysisModule module) {
        fModule = module;
    }

    @Override
    public List<@NonNull String> getMarkerCategories() {
        if (fColorMap.isEmpty()) {
            ISegmentStore<@NonNull ISegment> results = fModule.getResults();
            if (results != null) {
                Set<String> keys = Collections.singleton("chain");//results.stream().map(ISegment::getName).collect(Collectors.toSet());
                Display display = Display.getDefault();
                for (String key : keys) {
                    int hashCode = key.hashCode();
                    int r = hashCode & 255;
                    hashCode >>= 8;
                    int g = hashCode & 255;
                    hashCode >>= 8;
                    int b = hashCode & 255;
                    fColorMap.put(key, new Color(display, r, g, b, 64));
                }
            }
        }
        return new ArrayList<>(fColorMap.keySet());
    }

    @Override
    public List<@NonNull IMarkerEvent> getMarkerList(@NonNull String category, long startTime, long endTime, long resolution, @NonNull IProgressMonitor monitor) {
        @Nullable
        ISegmentStore<@NonNull ISegment> results = fModule.getResults();
        if (results != null) {
            Builder<IMarkerEvent> builder = ImmutableList.<IMarkerEvent> builder();
            results.stream().filter(t -> (t.getEnd() > startTime))
            .filter(t -> (t.getStart() < endTime))
            //.filter(t -> (t.getName().equals(category)))
            .forEach(c -> builder.add(createMarker(category, c)));
            return builder.build();
        }
        return Collections.EMPTY_LIST;
    }

    private MarkerEvent createMarker(String category, ISegment c) {
        return new MarkerEvent(null, c.getStart(), c.getLength(), category, fColorMap.get(category), "", true); //$NON-NLS-1$
    }

}
