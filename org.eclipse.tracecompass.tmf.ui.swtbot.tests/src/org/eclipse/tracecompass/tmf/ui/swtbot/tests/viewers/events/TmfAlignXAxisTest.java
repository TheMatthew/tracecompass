/*******************************************************************************
 * Copyright (c) 2015 Ericsson
 *
 * All rights reserved. This program and the accompanying materials are
 * made available under the terms of the Eclipse Public License v1.0 which
 * accompanies this distribution, and is available at
 * http:/www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *   Matthew Khouzam - Initial API and implementation
 *   Patrick Tasse - Fix editor handling
 *******************************************************************************/

package org.eclipse.tracecompass.tmf.ui.swtbot.tests.viewers.events;

import static org.junit.Assert.assertEquals;

import java.io.File;
import java.io.IOException;

import org.apache.log4j.ConsoleAppender;
import org.apache.log4j.Logger;
import org.apache.log4j.SimpleLayout;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.widgets.Sash;
import org.eclipse.swtbot.eclipse.finder.SWTWorkbenchBot;
import org.eclipse.swtbot.eclipse.finder.widgets.SWTBotView;
import org.eclipse.swtbot.swt.finder.junit.SWTBotJunit4ClassRunner;
import org.eclipse.swtbot.swt.finder.utils.SWTBotPreferences;
import org.eclipse.tracecompass.tmf.core.io.BufferedRandomAccessFile;
import org.eclipse.tracecompass.tmf.ui.project.wizards.NewTmfProjectWizard;
import org.eclipse.tracecompass.tmf.ui.swtbot.tests.shared.SWTBotSash;
import org.eclipse.tracecompass.tmf.ui.swtbot.tests.shared.SWTBotUtils;
import org.eclipse.tracecompass.tmf.ui.views.callstack.CallStackView;
import org.eclipse.tracecompass.tmf.ui.views.histogram.HistogramView;
import org.eclipse.tracecompass.tmf.ui.views.timechart.TimeChartView;
import org.eclipse.ui.IFolderLayout;
import org.eclipse.ui.IPageLayout;
import org.eclipse.ui.IPerspectiveFactory;
import org.hamcrest.BaseMatcher;
import org.hamcrest.Description;
import org.junit.After;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;

/**
 * Test common x axis for views
 *
 * @author Matthew Khouzam
 */
@RunWith(SWTBotJunit4ClassRunner.class)
public class TmfAlignXAxisTest {

    private static final String TRACE_START = "<trace>";
    private static final String EVENT_BEGIN = "<event timestamp=\"";
    private static final String EVENT_MIDDLE = " \" name=\"event\"><field name=\"field\" value=\"";
    private static final String EVENT_END = "\" type=\"int\" />" + "</event>";
    private static final String TRACE_END = "</trace>";

    private static final String PROJET_NAME = "TestForOffsetting";
    private static final int NUM_EVENTS = 100;

    /** The Log4j logger instance. */
    private static final Logger fLogger = Logger.getRootLogger();
    private static SWTWorkbenchBot fBot;

    private static String makeEvent(int ts, int val) {
        return EVENT_BEGIN + Integer.toString(ts) + EVENT_MIDDLE + Integer.toString(val) + EVENT_END + "\n";
    }

    private File fLocation;
    private static final BaseMatcher<Sash> SASH_MATCHER = new SashMatcher();

    /**
     * Initialization, creates a temp trace
     *
     * @throws IOException
     *             should not happen
     */
    @Before
    public void init() throws IOException {
        SWTBotUtils.failIfUIThread();
        Thread.currentThread().setName("SWTBot Thread"); // for the debugger
        /* set up for swtbot */
        SWTBotPreferences.TIMEOUT = 20000; /* 20 second timeout */
        fLogger.removeAllAppenders();
        fLogger.addAppender(new ConsoleAppender(new SimpleLayout()));
        fBot = new SWTWorkbenchBot();

        SWTBotUtils.closeView("welcome", fBot);

        SWTBotUtils.switchToTracingPerspective();
        /* finish waiting for eclipse to load */
        SWTBotUtils.waitForJobs();
        fLocation = File.createTempFile("sample", ".xml");
        try (BufferedRandomAccessFile braf = new BufferedRandomAccessFile(fLocation, "rw")) {
            braf.writeBytes(TRACE_START);
            for (int i = 0; i < NUM_EVENTS; i++) {
                braf.writeBytes(makeEvent(i * 100, i % 4));
            }
            braf.writeBytes(TRACE_END);
        }
        SWTBotUtils.createProject(PROJET_NAME);
        SWTBotUtils.selectTracesFolder(fBot, PROJET_NAME);
        SWTBotUtils.openTrace(PROJET_NAME, fLocation.getAbsolutePath(), "org.eclipse.linuxtools.tmf.core.tests.xmlstub");
    }

    /**
     * Delete file
     */
    @After
    public void cleanup() {
        SWTBotUtils.deleteProject(PROJET_NAME, fBot);
        fLocation.delete();
        fLogger.removeAllAppenders();
    }

    /**
     * Test
     */
    @Test
    public void testAlignment1() {
        SWTBotUtils.switchToPerspective(AlignPerspectiveFactory1.ID);
        SWTBotUtils.waitForJobs();
        SWTBotUtils.delay(500); // wait for throttler
        SWTBotView view = fBot.viewById(HistogramView.ID);
        final Sash sash = view.bot().widget(SASH_MATCHER, 0);
        SWTBotSash sashBot = new SWTBotSash(sash, null);
        Point p = sashBot.getPoint();
        SWTBotView view2 = fBot.viewById(CallStackView.ID);
        final Sash sash2 = view2.bot().widget(SASH_MATCHER, 0);
        SWTBotSash sashBot2 = new SWTBotSash(sash2, null);
        double histogramOriginalSashX = sashBot.getPoint().x;
        assertEquals("Approx align", histogramOriginalSashX, sashBot2.getPoint().x, 100);
        sashBot.drag(new Point(p.x + 100, p.y));
        SWTBotUtils.waitForJobs();
        SWTBotUtils.delay(500); // wait for throttler
        double histogramNewSashX = sashBot.getPoint().x;
        assertEquals("Approx align", histogramNewSashX, sashBot2.getPoint().x, 100);
        assertEquals(histogramOriginalSashX, histogramNewSashX - 100, 50);
    }

    /**
     * Test
     */
    @Ignore
    @Test
    public void testAlignment2() {
        SWTBotUtils.switchToPerspective(AlignPerspectiveFactory2.ID);
    }

    /**
     * Test
     */
    @Test
    public void testAlignment3() {
        SWTBotUtils.switchToPerspective(AlignPerspectiveFactory3.ID);
    }

    private static final class SashMatcher extends BaseMatcher<Sash> {
        @Override
        public boolean matches(Object item) {
            return (item instanceof Sash);
        }

        @Override
        public void describeTo(Description description) {
        }
    }

    /**
     * Callstack and timechart overlapping
     */
    public static class AlignPerspectiveFactory1 implements IPerspectiveFactory {

        /** The Perspective ID */
        public static final String ID = "org.eclipse.linuxtools.tmf.test.align.1"; //$NON-NLS-1$

        @Override
        public void createInitialLayout(IPageLayout layout) {
            if (layout == null) {
                return;
            }

            // Editor area
            layout.setEditorAreaVisible(true);

            // Editor area
            layout.setEditorAreaVisible(true);

            // Create the top left folder
            IFolderLayout topLeftFolder = layout.createFolder("topLeftFolder", IPageLayout.LEFT, 0.15f, IPageLayout.ID_EDITOR_AREA); //$NON-NLS-1$
            topLeftFolder.addView(IPageLayout.ID_PROJECT_EXPLORER);

            // Create the top right folder
            IFolderLayout topRightFolder = layout.createFolder("topRightFolder", IPageLayout.BOTTOM, 0.30f, IPageLayout.ID_EDITOR_AREA); //$NON-NLS-1$
            topRightFolder.addView(HistogramView.ID);

            // Create the middle right folder
            IFolderLayout middleRightFolder = layout.createFolder("middleRightFolder", IPageLayout.BOTTOM, 0.50f, "topRightFolder"); //$NON-NLS-1$
            middleRightFolder.addView(CallStackView.ID);

            // Create the bottom right folder
            IFolderLayout bottomRightFolder = layout.createFolder("bottomRightFolder", IPageLayout.BOTTOM, 0.65f, "middleRightFolder"); //$NON-NLS-1$ //$NON-NLS-2$
            bottomRightFolder.addView(TimeChartView.ID);

            // Populate menus, etc
            layout.addPerspectiveShortcut(ID);
            layout.addNewWizardShortcut(NewTmfProjectWizard.ID);
        }

    }

    /**
     * Callstack and Histogram overlapping
     */
    public static class AlignPerspectiveFactory2 implements IPerspectiveFactory {

        /** The Perspective ID */
        public static final String ID = "org.eclipse.linuxtools.tmf.test.align.2"; //$NON-NLS-1$

        @Override
        public void createInitialLayout(IPageLayout layout) {
            if (layout == null) {
                return;
            }

            // Editor area
            layout.setEditorAreaVisible(true);

            // Editor area
            layout.setEditorAreaVisible(true);

            // Create the top left folder
            IFolderLayout topLeftFolder = layout.createFolder("topLeftFolder", IPageLayout.LEFT, 0.15f, IPageLayout.ID_EDITOR_AREA); //$NON-NLS-1$
            topLeftFolder.addView(IPageLayout.ID_PROJECT_EXPLORER);

            // Create the middle right folder
            IFolderLayout middleRightFolder = layout.createFolder("middleRightFolder", IPageLayout.BOTTOM, 0.40f, IPageLayout.ID_EDITOR_AREA); //$NON-NLS-1$
            middleRightFolder.addView(HistogramView.ID);
            middleRightFolder.addView(CallStackView.ID);

            // Create the bottom right folder
            IFolderLayout bottomRightFolder = layout.createFolder("bottomRightFolder", IPageLayout.BOTTOM, 0.65f, "middleRightFolder"); //$NON-NLS-1$ //$NON-NLS-2$
            bottomRightFolder.addView(TimeChartView.ID);

            // Populate menus, etc
            layout.addPerspectiveShortcut(ID);
            layout.addNewWizardShortcut(NewTmfProjectWizard.ID);
        }

    }

    /**
     * no overlap, callstack not aligned
     */
    public static class AlignPerspectiveFactory3 implements IPerspectiveFactory {

        /** The Perspective ID */
        public static final String ID = "org.eclipse.linuxtools.tmf.test.align.3"; //$NON-NLS-1$

        @Override
        public void createInitialLayout(IPageLayout layout) {
            if (layout == null) {
                return;
            }

            // Editor area
            layout.setEditorAreaVisible(true);

            // Editor area
            layout.setEditorAreaVisible(true);

            // Create the top left folder
            IFolderLayout topLeftFolder = layout.createFolder("topLeftFolder", IPageLayout.LEFT, 0.15f, IPageLayout.ID_EDITOR_AREA); //$NON-NLS-1$
            topLeftFolder.addView(CallStackView.ID);
            topLeftFolder.addView(IPageLayout.ID_PROJECT_EXPLORER);

            // Create the middle right folder
            IFolderLayout middleRightFolder = layout.createFolder("middleRightFolder", IPageLayout.BOTTOM, 0.40f, IPageLayout.ID_EDITOR_AREA); //$NON-NLS-1$
            middleRightFolder.addView(HistogramView.ID);

            // Create the bottom right folder
            IFolderLayout bottomRightFolder = layout.createFolder("bottomRightFolder", IPageLayout.BOTTOM, 0.65f, "middleRightFolder"); //$NON-NLS-1$ //$NON-NLS-2$
            bottomRightFolder.addView(TimeChartView.ID);

            // Populate menus, etc
            layout.addPerspectiveShortcut(ID);
            layout.addNewWizardShortcut(NewTmfProjectWizard.ID);
        }

    }

}