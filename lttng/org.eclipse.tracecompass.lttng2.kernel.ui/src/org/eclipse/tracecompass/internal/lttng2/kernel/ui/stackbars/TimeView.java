package org.eclipse.tracecompass.internal.lttng2.kernel.ui.stackbars;

import java.util.ArrayList;

import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.GC;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.tracecompass.tmf.core.signal.TmfDataUpdatedSignal;
import org.eclipse.tracecompass.tmf.core.signal.TmfSignalHandler;
import org.eclipse.tracecompass.tmf.core.signal.TmfSignalManager;
import org.eclipse.tracecompass.tmf.core.signal.TmfWindowRangeUpdatedSignal;
import org.eclipse.tracecompass.tmf.core.timestamp.ITmfTimestamp;
import org.eclipse.tracecompass.tmf.core.timestamp.TmfTimeRange;
import org.eclipse.tracecompass.tmf.core.timestamp.TmfTimestamp;
import org.eclipse.tracecompass.tmf.ui.viewers.xycharts.TmfChartTimeStampFormat;
import org.eclipse.tracecompass.tmf.ui.views.TmfView;
import org.swtchart.Chart;
import org.swtchart.IAxis;
import org.swtchart.IAxisTick;
import org.swtchart.IGrid;
import org.swtchart.ILineSeries;
import org.swtchart.ILineSeries.PlotSymbolType;
import org.swtchart.ISeries;
import org.swtchart.ISeries.SeriesType;
import org.swtchart.ISeriesSet;
import org.swtchart.ITitle;
import org.swtchart.LineStyle;
import org.swtchart.Range;


public class TimeView extends TmfView {

    private static final String SERIES_EXEC_NAME = "Executions";
    private static final String SERIES_BOT_NAME = "Mean-SD";
    private static final String SERIES_UP_NAME = "Mean+SD";
    private static final String SERIES_MEAN_NAME = "Mean";
    private static final String Y_AXIS_TITLE = "Duration (ns)";
    private static final String X_AXIS_TITLE = "Timestamp";
    private static final String VIEW_ID = "org.eclipse.linuxtools.internal.lttng2.kernel.ui.stackbars.timeView";
    private Chart chart;

    /* Used to remember location point of mouse down */
    private static double fStartX;
    //private static double fStartY;

    private static int fStartXPos;

    private static int fCurrentX;

    private static boolean fDrag = false;
    private static boolean fDown = false;

    public TimeView() {
        super(VIEW_ID);
    }
    @Override
    public void createPartControl(Composite parent) {

        Color colorGreen = new Color(Display.getDefault(), 0, 150, 0);
        Color colorPurple = new Color(Display.getDefault(), 150, 0, 200);
        Color colorGrey = new Color(Display.getDefault(), 100, 100, 100);
        Color colorBlack = new Color(Display.getDefault(), 0, 0, 0);

        chart = new Chart(parent, SWT.BORDER);
        chart.getTitle().setVisible(false);
        chart.getAxisSet().getXAxis(0).getTitle().setText(X_AXIS_TITLE);
        chart.getAxisSet().getYAxis(0).getTitle().setText(Y_AXIS_TITLE);

        ISeriesSet seriesSet = chart.getSeriesSet();

        seriesSet.createSeries(SeriesType.LINE, SERIES_EXEC_NAME);
        seriesSet.createSeries(SeriesType.LINE, SERIES_BOT_NAME);
        seriesSet.createSeries(SeriesType.LINE, SERIES_UP_NAME);
        seriesSet.createSeries(SeriesType.LINE, SERIES_MEAN_NAME);

        ILineSeries series = (ILineSeries)seriesSet.getSeries(SERIES_BOT_NAME);
        series.setSymbolType(PlotSymbolType.NONE);
        series.setLineColor(colorGreen);

        series = (ILineSeries)seriesSet.getSeries(SERIES_UP_NAME);
        series.setSymbolType(PlotSymbolType.NONE);
        series.setLineColor(colorPurple);

        series = (ILineSeries)seriesSet.getSeries(SERIES_MEAN_NAME);
        series.setSymbolType(PlotSymbolType.NONE);
        series.setLineColor(colorGrey);

        series = (ILineSeries)seriesSet.getSeries(SERIES_EXEC_NAME);
        series.setLineStyle(LineStyle.NONE);

        chart.getLegend().setVisible(true);

        ITitle graphTitle = chart.getTitle();
        ITitle xAxisTitle = chart.getAxisSet().getXAxis(0).getTitle();
        ITitle yAxisTitle = chart.getAxisSet().getYAxis(0).getTitle();
        graphTitle.setForeground(colorBlack);
        xAxisTitle.setForeground(colorBlack);
        yAxisTitle.setForeground(colorBlack);

        IAxisTick xTick = chart.getAxisSet().getXAxis(0).getTick();
        xTick.setForeground(colorBlack);

        IAxisTick yTick = chart.getAxisSet().getYAxis(0).getTick();
        yTick.setForeground(colorBlack);

        IGrid xGrid = chart.getAxisSet().getXAxis(0).getGrid();
        xGrid.setStyle(LineStyle.NONE);

        chart.getAxisSet().getXAxis(0).getTick().setFormat(new TmfChartTimeStampFormat(0));

        //Listenrs
        /* Get the plot area and add the mouse listeners */
        final Composite plotArea = chart.getPlotArea();

        plotArea.addListener(SWT.MouseDown, new Listener() {

            @Override
            public void handleEvent(Event event) {
                IAxis xAxis = chart.getAxisSet().getXAxis(0);
                //IAxis yAxis = chart.getAxisSet().getYAxis(0);

                fStartX = xAxis.getDataCoordinate(event.x);
                //fStartY = yAxis.getDataCoordinate(event.y);

                fStartXPos = event.x;

                fDown = true;
            }
        });

        plotArea.addListener(SWT.MouseUp, new Listener() {

            @Override
            public void handleEvent(Event event) {
                IAxis xAxis = chart.getAxisSet().getXAxis(0);
                IAxis yAxis = chart.getAxisSet().getYAxis(0);

                double endX = xAxis.getDataCoordinate(event.x);
                //double endY = yAxis.getDataCoordinate(event.y);
                //double realEndX = xAxis.getRange().upper * endX;

                if(fDrag)
                {
                    //Send signal
                    double start = Math.min(fStartX, endX);
                    double end = Math.max(fStartX, endX);

                    TmfTimeRange range = new TmfTimeRange(new TmfTimestamp((long) (start),
                            ITmfTimestamp.NANOSECOND_SCALE), new TmfTimestamp((long) (end),
                                    ITmfTimestamp.NANOSECOND_SCALE));
                    TmfSignalManager.dispatchSignal(new TmfWindowRangeUpdatedSignal(this, range));
                }
                else
                {
                    ISeries seriesExec = chart.getSeriesSet().getSeries(SERIES_EXEC_NAME);

                    double[] xS = seriesExec.getXSeries();
                    double[] yS = seriesExec.getYSeries();

                    int index = binarySearchPos(xS, endX);

                    Integer closestIndex = -1;
                    double minDist = Double.MAX_VALUE;

                    int end = Math.min(xS.length, index + 50);

                    /* check nearest 40 points */
                    for (int i = Math.max(index - 50, 0); i < end; i++) {
                        /* compute distance to mouse position */
                        double newDist = Math.sqrt(Math.pow((event.x - xAxis.getPixelCoordinate(xS[i])), 2)
                                + Math.pow((event.y - yAxis.getPixelCoordinate(yS[i])), 2));

                        /* if closer to mouse, remember */
                        if (newDist < minDist) {
                            minDist = newDist;
                            closestIndex = i;
                        }
                    }

                    TmfSignalManager.dispatchSignal(new TmfDataUpdatedSignal(this, closestIndex));

                }

                fDrag = false;
                fDown = false;

                plotArea.redraw();
            }
        });

        plotArea.addListener(SWT.MouseMove, new Listener() {

            @Override
            public void handleEvent(Event event) {
                if(fDown)
                {
                    fDrag = true;
                    fCurrentX = event.x;

                    plotArea.redraw();
                }
            }
        });

        plotArea.addListener(SWT.Paint, new Listener() {

            @Override
            public void handleEvent(Event event) {
                if(fDrag)
                {
                    GC gc = event.gc;

                    gc.setBackground(Display.getDefault().getSystemColor(SWT.COLOR_RED));
                    gc.setAlpha(128);

                    int minX = Math.min(fStartXPos, fCurrentX);

                    int maxX = Math.max(fStartXPos, fCurrentX);

                    int width = maxX - minX;

                    gc.fillRectangle(minX, 0, width, 2000000);//TODO why Integer.MAX_VALUE doesn't work?
                }
            }
        });
    }

    private static int binarySearchPos(final double[] array, final double value) {
        //Integer i = null;
        int idx = java.util.Arrays.binarySearch(array, value);
        if (idx < 0) {
            idx = -(idx) - 1;
            /*if (idx != 0 && idx < array.length) {
                double d0 = Math.abs(array[idx - 1] - value);
                double d1 = Math.abs(array[idx] - value);
                i = (d0 <= d1) ? idx - 1 : idx;
            }
            return i;*/
        }
        return idx;
    }

    @Override
    public void setFocus() {
        chart.setFocus();
    }

    @TmfSignalHandler
    public void dataUpdated(final TmfDataUpdatedSignal signal) {
        if(signal.getData() instanceof StackbarsChartSeries)
        {
            StackbarsChartSeries series = (StackbarsChartSeries) signal.getData();
            updateChart(series.getX(), series.getY(), series.getDeadline());
        }
    }

    private final class DisplayRunnable implements Runnable {
        private final double[] fX;
        private final double[] fXLine;
        private final Color[] fColors;
        private final double[] fY;
        private final ArrayList<Double> fYValues;
        private final double[] fYLineMeanMSD;
        private final double[] fYLineMeanPSD;
        private final double[] fYLineMean;
        private final ArrayList<Double> fXValues;

        private DisplayRunnable(double[] x, double[] xLine, Color[] colors, double[] y, ArrayList<Double> yValues, double[] yLineMeanMSD, double[] yLineMeanPSD, double[] yLineMean, ArrayList<Double> xValues) {
            fX = x;
            fXLine = xLine;
            fColors = colors;
            fY = y;
            fYValues = yValues;
            fYLineMeanMSD = yLineMeanMSD;
            fYLineMeanPSD = yLineMeanPSD;
            fYLineMean = yLineMean;
            fXValues = xValues;
        }

        @Override
        public void run() {

            //Exec
            ILineSeries seriesExec = (ILineSeries)chart.getSeriesSet().getSeries(SERIES_EXEC_NAME);
            seriesExec.setXSeries(fX);
            seriesExec.setYSeries(fY);
            seriesExec.setSymbolColors(fColors);

            //lines
            ILineSeries seriesL = (ILineSeries)chart.getSeriesSet().getSeries(SERIES_BOT_NAME);
            seriesL.setXSeries(fXLine);
            seriesL.setYSeries(fYLineMeanMSD);
            seriesL = (ILineSeries)chart.getSeriesSet().getSeries(SERIES_UP_NAME);
            seriesL.setXSeries(fXLine);
            seriesL.setYSeries(fYLineMeanPSD);
            seriesL = (ILineSeries)chart.getSeriesSet().getSeries(SERIES_MEAN_NAME);
            seriesL.setXSeries(fXLine);
            seriesL.setYSeries(fYLineMean);

            // Set the new range
            if (!fXValues.isEmpty() && !fYValues.isEmpty()) {
                chart.getAxisSet().adjustRange();
                //chart.getAxisSet().getXAxis(0).setRange(new Range(x[0], x[x.length - 1]));
                //chart.getAxisSet().getYAxis(0).setRange(new Range(minY, maxY));
            } else {
                chart.getAxisSet().getXAxis(0).setRange(new Range(0, 1));
                chart.getAxisSet().getYAxis(0).setRange(new Range(0, 1));
            }
            chart.getAxisSet().adjustRange();

            chart.redraw();
        }
    }

    public static class StackbarsChartSeries
    {
        private ArrayList<Double> fXValues;
        private ArrayList<Double> fYValues;
        private double fDeadline;

        public StackbarsChartSeries(ArrayList<Double> x, ArrayList<Double> y, double deadline)
        {
            fXValues = new ArrayList<>(x);
            fYValues = new ArrayList<>(y);
            fDeadline = deadline;
        }

        public ArrayList<Double> getX()
        {
            return fXValues;
        }

        public ArrayList<Double> getY()
        {
            return fYValues;
        }

        public double getDeadline()
        {
            return fDeadline;
        }
    }

    void updateChart(final ArrayList<Double> xValues, final ArrayList<Double> yValues, double deadline)
    {
        int nbElem = xValues.size();

        if(nbElem == 0)
        {
            return;
        }

        final double x[] = new double[nbElem];
        final double y[] = new double[nbElem];

        double mean = 0;
        double sd = 0;

        for (int i = 0; i < nbElem; ++i) {
            x[i] = xValues.get(i);
            y[i] = yValues.get(i);
            sd += y[i]*y[i]/ nbElem;
            mean += y[i]/nbElem;
        }

        //Color
        final Color [] colors = new Color[nbElem];

        Color colorRed = new Color(Display.getDefault(), 200, 0, 0);
        //Color colorGreen = new Color(Display.getDefault(), 0, 150, 0);
        //Color colorPurple = new Color(Display.getDefault(), 150, 0, 200);
        Color colorGrey = new Color(Display.getDefault(), 100, 100, 100);

        sd -= mean*mean;
        sd = Math.sqrt(sd);

        double mean_m_sd = mean - sd;
        double mean_p_sd = mean + sd;
        double limitDeadline;

        if(deadline == StackbarsAnalysis.NO_DEADLINE)
        {
            limitDeadline = Long.MAX_VALUE;
        }
        else
        {
            limitDeadline = deadline;
        }

        for (int i = 0; i < nbElem; ++i) {
            if(y[i] > limitDeadline)
            {
                colors[i] = colorRed;
            }
            /*else if (y[i] > mean_p_sd)
            {
                colors[i] = colorPurple;
            }*/ else {
                colors[i] = colorGrey;
           /* }
            else //if (y[i] < mean_m_sd)
            {
                colors[i] = colorGreen;
            }*/
            }
        }

        //Lines
        final double xLine[] = {x[0], x[x.length - 1]};
        final double yLineMean[] = {mean, mean};
        final double yLineMeanMSD[] = {mean_m_sd, mean_m_sd};
        final double yLineMeanPSD[] = {mean_p_sd, mean_p_sd};

        // This part needs to run on the UI thread since it updates the chart SWT control
        Display.getDefault().asyncExec(new DisplayRunnable(x, xLine, colors, y, yValues, yLineMeanMSD, yLineMeanPSD, yLineMean, xValues));

    }
}
