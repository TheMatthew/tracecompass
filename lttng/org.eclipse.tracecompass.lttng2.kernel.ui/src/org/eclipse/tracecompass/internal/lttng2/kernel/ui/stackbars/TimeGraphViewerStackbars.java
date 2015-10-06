package org.eclipse.tracecompass.internal.lttng2.kernel.ui.stackbars;

import org.eclipse.swt.widgets.Composite;
import org.eclipse.tracecompass.tmf.ui.widgets.timegraph.TimeGraphViewer;
import org.eclipse.tracecompass.tmf.ui.widgets.timegraph.widgets.TimeGraphColorScheme;
import org.eclipse.tracecompass.tmf.ui.widgets.timegraph.widgets.TimeGraphControl;

public class TimeGraphViewerStackbars extends TimeGraphViewer{

    public TimeGraphViewerStackbars(Composite parent, int style) {
        super(parent, style);
    }

    /**
     * Create a new time graph control.
     *
     * @param parent
     *            The parent composite
     * @param colors
     *            The color scheme
     * @return The new TimeGraphControl
     * @since 2.0
     */
    @Override
    protected TimeGraphControl createTimeGraphControl(Composite parent,
            TimeGraphColorScheme colors) {
        return new TimeGraphControlStackbars(parent, colors);
    }

}
