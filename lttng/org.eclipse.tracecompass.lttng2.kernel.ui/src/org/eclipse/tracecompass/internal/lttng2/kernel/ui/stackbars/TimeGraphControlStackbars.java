package org.eclipse.tracecompass.internal.lttng2.kernel.ui.stackbars;

import org.eclipse.swt.events.MouseEvent;
import org.eclipse.swt.graphics.GC;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.tracecompass.tmf.core.signal.TmfSelectionRangeUpdatedSignal;
import org.eclipse.tracecompass.tmf.core.signal.TmfSignalManager;
import org.eclipse.tracecompass.tmf.core.signal.TmfWindowRangeUpdatedSignal;
import org.eclipse.tracecompass.tmf.core.timestamp.ITmfTimestamp;
import org.eclipse.tracecompass.tmf.core.timestamp.TmfTimeRange;
import org.eclipse.tracecompass.tmf.core.timestamp.TmfTimestamp;
import org.eclipse.tracecompass.tmf.ui.widgets.timegraph.model.ITimeEvent;
import org.eclipse.tracecompass.tmf.ui.widgets.timegraph.model.ITimeGraphEntry;
import org.eclipse.tracecompass.tmf.ui.widgets.timegraph.widgets.ITimeDataProvider;
import org.eclipse.tracecompass.tmf.ui.widgets.timegraph.widgets.TimeGraphColorScheme;
import org.eclipse.tracecompass.tmf.ui.widgets.timegraph.widgets.TimeGraphControl;
import org.eclipse.tracecompass.tmf.ui.widgets.timegraph.widgets.Utils;

public class TimeGraphControlStackbars extends TimeGraphControl {

    private long fDeadline;

    public TimeGraphControlStackbars(Composite parent, TimeGraphColorScheme colors) {
        super(parent, colors);
        fDeadline = -1;
    }

    public void setDeadline(long deadline)
    {
        fDeadline = deadline;
        redraw();
    }

    @Override
    public void postPaintAction(GC gc, Rectangle bounds, double pixelsPerNanoSec, long time0, int nameSpace) {

        if (fDeadline == -1)
        {
            return;
        }

        int x0 = bounds.x + nameSpace + (int) ((fDeadline - time0) * pixelsPerNanoSec);

        // draw selection lines
        gc.setForeground(getColorScheme().getColor(TimeGraphColorScheme.RED));
        if (x0 >= nameSpace && x0 < bounds.x + bounds.width) {
            gc.setLineWidth(2);
            gc.drawLine(x0, bounds.y, x0, bounds.y + bounds.height);
        }
    }

    @Override
    protected void setSelectedTime(MouseEvent e)
    {
        long time = getTimeAtX(e.x);
        ITimeDataProvider timeProvider = getTimeProvider();
        if (timeProvider != null)
        {
            timeProvider.setSelectedTime(time, false);
        }

        Point p = new Point(e.x, e.y);
        ITimeGraphEntry entry = getEntry(p);

        if (entry == null) {
            return;
        }

        if (entry instanceof ComparisonTimeGraphEntry) {
            ComparisonTimeGraphEntry comparisonEntry = (ComparisonTimeGraphEntry) entry;

            if (comparisonEntry.hasTimeEvents()) {
                long currPixelTime = getTimeAtX(e.x);
                long nextPixelTime = getTimeAtX(e.x + 1);
                if (nextPixelTime == currPixelTime) {
                    nextPixelTime++;
                }
                ITimeEvent currEvent = Utils.findEvent(comparisonEntry, currPixelTime, 0);
                ITimeEvent nextEvent = Utils.findEvent(comparisonEntry, currPixelTime, 1);

                // if there is no current event at the start of the current pixel range,
                // or if the current event starts before the current pixel range,
                // use the next event as long as it starts within the current pixel range
                if ((currEvent == null || currEvent.getTime() < currPixelTime) &&
                    (nextEvent != null && nextEvent.getTime() < nextPixelTime)) {
                    currEvent = nextEvent;
                }

                if(currEvent != null)
                {
                    ITmfTimestamp startTime = new TmfTimestamp(comparisonEntry.getRealStartTime() + currEvent.getTime(), ITmfTimestamp.NANOSECOND_SCALE);
                    ITmfTimestamp endTime = new TmfTimestamp(startTime.getValue() + currEvent.getDuration(), ITmfTimestamp.NANOSECOND_SCALE);

                    //Send signal
                    TmfTimeRange range = new TmfTimeRange(new TmfTimestamp(startTime.getValue() - currEvent.getDuration(), ITmfTimestamp.NANOSECOND_SCALE), new TmfTimestamp(endTime.getValue() + currEvent.getDuration(), ITmfTimestamp.NANOSECOND_SCALE));
                    TmfSignalManager.dispatchSignal(new TmfWindowRangeUpdatedSignal(this, range));
                    TmfSignalManager.dispatchSignal(new TmfSelectionRangeUpdatedSignal(this, startTime, endTime));

                    /*Test to make a selection
                     *
                     * if (timeProvider instanceof TimeGraphViewer)
                    {
                        TimeGraphViewer viewer = (TimeGraphViewer)timeProvider;
                        viewer.notifyTimeListeners(startTime.getValue(), endTime.getValue());
                    }*/
                }
            }
        }
    }
}
