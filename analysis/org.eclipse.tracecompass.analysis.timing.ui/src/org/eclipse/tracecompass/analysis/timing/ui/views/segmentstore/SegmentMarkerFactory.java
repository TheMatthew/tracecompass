package org.eclipse.tracecompass.analysis.timing.ui.views.segmentstore;

import org.eclipse.jdt.annotation.NonNull;
import org.eclipse.jdt.annotation.Nullable;
import org.eclipse.tracecompass.analysis.os.linux.core.latency.EventChainLatencyAnalysis;
import org.eclipse.tracecompass.analysis.timing.core.segmentstore.AbstractSegmentStoreAnalysisModule;
import org.eclipse.tracecompass.tmf.core.analysis.IAnalysisModule;
import org.eclipse.tracecompass.tmf.core.trace.AbstractTmfTraceAdapterFactory;
import org.eclipse.tracecompass.tmf.core.trace.ITmfTrace;
import org.eclipse.tracecompass.tmf.ui.widgets.timegraph.model.IMarkerEventSource;

public class SegmentMarkerFactory extends AbstractTmfTraceAdapterFactory {

    private static final Class[] ADAPTER_LIST = { IMarkerEventSource.class };

    @Override
    public Class<?>[] getAdapterList() {
        // TODO Auto-generated method stub
        return ADAPTER_LIST;
    }

    @Override
    protected <T> @Nullable T getTraceAdapter(@NonNull ITmfTrace trace, @Nullable Class<T> adapterType) {
        if (adapterType != null && IMarkerEventSource.class.equals(adapterType)) {
            @Nullable
            IAnalysisModule analysisModule = trace.getAnalysisModule(EventChainLatencyAnalysis.ID);
            if (analysisModule instanceof AbstractSegmentStoreAnalysisModule) {
                AbstractSegmentStoreAnalysisModule segmentStoreAnalysisModule = (AbstractSegmentStoreAnalysisModule) analysisModule;
                return adapterType.cast(new SegmentMarkerSource(segmentStoreAnalysisModule));
            }
        }
        return null;
    }

}
