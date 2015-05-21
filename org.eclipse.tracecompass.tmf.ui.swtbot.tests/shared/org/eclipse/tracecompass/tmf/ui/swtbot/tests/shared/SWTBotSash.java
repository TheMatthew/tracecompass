package org.eclipse.tracecompass.tmf.ui.swtbot.tests.shared;

import java.awt.AWTException;
import java.awt.Robot;
import java.awt.event.InputEvent;

import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.widgets.Sash;
import org.eclipse.swtbot.swt.finder.exceptions.WidgetNotFoundException;
import org.eclipse.swtbot.swt.finder.finders.UIThreadRunnable;
import org.eclipse.swtbot.swt.finder.results.Result;
import org.eclipse.swtbot.swt.finder.results.VoidResult;
import org.eclipse.swtbot.swt.finder.widgets.AbstractSWTBotControl;
import org.hamcrest.SelfDescribing;

/**
 * SWT Bot sash control
 */
public class SWTBotSash extends AbstractSWTBotControl<Sash> {

    /**
     * The widget wrapper
     *
     * @param w
     *            the sash
     * @param description
     *            the description
     * @throws WidgetNotFoundException
     *             if there is no widget
     */
    public SWTBotSash(Sash w, SelfDescribing description) throws WidgetNotFoundException {
        super(w, description);
    }

    /**
     * Get the central point of the sash
     *
     * @return the center point, good for dragging
     */
    public Point getPoint() {
        return UIThreadRunnable.syncExec(new Result<Point>() {

            @Override
            public Point run() {
                Rectangle rect = widget.getBounds();
                Point p = widget.toDisplay(0, 0);
                return new Point(p.x + rect.width / 2, p.y + rect.height / 2);
            }

        });
    }

    /**
     * Simulate a drag
     *
     * @param dst
     *            to this destination
     */
    public void drag(final Point dst) {
        final Point src = getPoint();
        /*
         * dn : MouseEvent{Sash {} time=262463957 data=null button=1
         * stateMask=0x0 x=5 y=59 count=1}
         *
         * move : MouseEvent{Sash {} time=262464038 data=null button=0
         * stateMask=0x80000 x=131 y=103 count=0}
         *
         * move : MouseEvent{Sash {} time=262464171 data=null button=0
         * stateMask=0x80000 x=90 y=116 count=0}
         *
         * up : MouseEvent{Sash {} time=262464796 data=null button=1
         * stateMask=0x80000 x=5 y=116 count=1}
         */
        try {
            final Robot awtRobot = new Robot();

            final int stepX = (dst.x - src.x) / 5;
            final int stepY = (dst.y - src.y) / 5;
            syncExec(new VoidResult() {
                @Override
                public void run() {
                    awtRobot.mouseMove(src.x, src.y);
                    SWTBotUtils.delay(15);
                    awtRobot.mousePress(InputEvent.BUTTON1_MASK);

                }
            });
            for (int i = 0; i < 5; i++) {
                SWTBotUtils.delay(15);
                final int index = i;
                syncExec(new VoidResult() {
                    @Override
                    public void run() {
                        int x = src.x + index * stepX;
                        int y = src.y + index * stepY;
                        awtRobot.mouseMove(x, y);
                    }
                });
            }
            asyncExec(new VoidResult() {
                @Override
                public void run() {
                    awtRobot.mouseMove(dst.x, dst.y);
                }});
            SWTBotUtils.delay(15);
            syncExec(new VoidResult() {
                @Override
                public void run() {
                    awtRobot.mouseRelease(InputEvent.BUTTON1_MASK);
                }
            });

        } catch (final AWTException e) {
            // log.error(e.getMessage(), e);
            throw new RuntimeException(e);
        }
    }

}
