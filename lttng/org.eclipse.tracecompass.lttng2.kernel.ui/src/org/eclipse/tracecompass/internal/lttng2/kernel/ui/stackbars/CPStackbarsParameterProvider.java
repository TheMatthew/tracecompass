/*******************************************************************************
 * Copyright (c) 2013 École Polytechnique de Montréal
 *
 * All rights reserved. This program and the accompanying materials are
 * made available under the terms of the Eclipse Public License v1.0 which
 * accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *   Geneviève Bastien - Initial API and implementation
 *******************************************************************************/

package org.eclipse.tracecompass.internal.lttng2.kernel.ui.stackbars;

import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.tracecompass.analysis.graph.core.building.TmfGraphBuilderModule;
import org.eclipse.tracecompass.analysis.graph.core.criticalpath.CriticalPathModule;
import org.eclipse.tracecompass.internal.analysis.graph.ui.criticalpath.view.CriticalPathView;
import org.eclipse.tracecompass.internal.lttng2.kernel.core.analysis.graph.building.LttngKernelExecutionGraph;
import org.eclipse.tracecompass.lttng2.kernel.core.trace.LttngKernelTrace;
import org.eclipse.tracecompass.tmf.core.analysis.IAnalysisModule;
import org.eclipse.tracecompass.tmf.core.analysis.TmfAbstractAnalysisParamProvider;
import org.eclipse.tracecompass.tmf.core.trace.ITmfTrace;
import org.eclipse.tracecompass.tmf.core.trace.TmfTraceManager;
import org.eclipse.ui.IPartListener2;
import org.eclipse.ui.ISelectionListener;
import org.eclipse.ui.IViewPart;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.IWorkbenchPart;
import org.eclipse.ui.IWorkbenchPartReference;
import org.eclipse.ui.PlatformUI;

/**
 * Class that provides parameters to the critical path analysis for lttng kernel
 * traces
 *
 * @author Geneviève Bastien
 */
public class CPStackbarsParameterProvider extends TmfAbstractAnalysisParamProvider {

    private StackbarsEntry fCurrentEntry = null;

    private static final String NAME = "Critical Path Lttng kernel parameter provider"; //$NON-NLS-1$
    public static final String ANALYSIS_ID = "org.eclipse.linuxtools.lttng2.kernel.ui.stackbars.criticalpath";
    public static final String VIEW_ID = "org.eclipse.linuxtools.lttng2.kernel.ui.stackbars.criticalpathview";

    private boolean fActive = false;
    private boolean fEntryChanged = false;

    private ISelectionListener selListener = new ISelectionListener() {
        @Override
        public void selectionChanged(IWorkbenchPart part, ISelection selection) {
            if (selection instanceof IStructuredSelection) {
                Object element = ((IStructuredSelection) selection)
                        .getFirstElement();
                if (element instanceof StackbarsEntry) {
                    StackbarsEntry entry = (StackbarsEntry) element;
                    setCurrentThreadId(entry);
                }
            }
        }
    };

    private IPartListener2 partListener = new IPartListener2() {

        @Override
        public void partActivated(IWorkbenchPartReference partRef) {
            if (partRef.getPart(false) instanceof CriticalPathView) {
                toggleActive(true);
            }
        }

        @Override
        public void partBroughtToTop(IWorkbenchPartReference partRef) {

        }

        @Override
        public void partClosed(IWorkbenchPartReference partRef) {
            if (partRef.getPart(false) instanceof CriticalPathView) {
                toggleActive(false);
            }
        }

        @Override
        public void partDeactivated(IWorkbenchPartReference partRef) {

        }

        @Override
        public void partOpened(IWorkbenchPartReference partRef) {
            if (partRef.getPart(false) instanceof CriticalPathView) {
                toggleActive(true);
            }
        }

        @Override
        public void partHidden(IWorkbenchPartReference partRef) {
            if (partRef.getPart(false) instanceof CriticalPathView) {
                toggleActive(false);
            }
        }

        @Override
        public void partVisible(IWorkbenchPartReference partRef) {
            if (partRef.getPart(false) instanceof CriticalPathView) {
                toggleActive(true);
            }
        }

        @Override
        public void partInputChanged(IWorkbenchPartReference partRef) {

        }

    };

    /**
     * Constructor
     */
    public CPStackbarsParameterProvider() {
        super();
        registerListener();
    }

    @Override
    public String getName() {
        return NAME;
    }

    @Override
    public Object getParameter(String name) {
        if (fCurrentEntry == null) {
            return null;
        }
        if (name.equals(CriticalPathModule.PARAM_WORKER)) {
            /* Try to find the worker for the critical path */
            IAnalysisModule mod = getModule();
            if ((mod != null) && (mod instanceof CriticalPathModule)) {
                CriticalPathModule module = (CriticalPathModule) mod;
                TmfGraphBuilderModule graphModule = (TmfGraphBuilderModule) module.getParameter(CriticalPathModule.PARAM_GRAPH);
                if ((graphModule != null) && (graphModule.getModelRegistry() != null)) {
                    TmfSystemModel model = graphModule.getModelRegistry().getModel(TmfSystemModel.class, true);
                    if (model != null) {
                        // FIXME: the argument cpu in this case does not make sens
                        TmfWorker task = model.getWorker(fCurrentEntry.getTrace().getHostId(), 0, fCurrentEntry.getTidStart());
                        return task;
                    }
                }
            }
            return fCurrentEntry;
        }
        if (name.equals(CriticalPathModule.PARAM_GRAPH)) {
            return LttngKernelExecutionGraph.ANALYSIS_ID;
        }
        return null;
    }

    @Override
    public boolean appliesToTrace(ITmfTrace trace) {
        for (ITmfTrace aTrace : TmfTraceManager.getTraceSetWithExperiment(trace)) {
            if (aTrace instanceof LttngKernelTrace) {
                return true;
            }
        }
        return false;
    }

    private void setCurrentThreadId(StackbarsEntry entry) {
        if (!entry.equals(fCurrentEntry)) {
            fCurrentEntry = entry;
            if (fActive) {
                this.notifyParameterChanged(CriticalPathModule.PARAM_WORKER);
            } else {
                fEntryChanged = true;
            }
        }
    }

    private void toggleActive(boolean active) {
        if (active != fActive) {
            fActive = active;
            if (fActive && fEntryChanged) {
                this.notifyParameterChanged(CriticalPathModule.PARAM_WORKER);
                fEntryChanged = false;
            }
        }
    }

    private void registerListener() {
        final IWorkbench wb = PlatformUI.getWorkbench();

        final IWorkbenchPage activePage = wb.getActiveWorkbenchWindow().getActivePage();

        /* Activate the update if critical path view visible */
        IViewPart view = activePage.findView(CPStackbarsParameterProvider.VIEW_ID);
        if (view != null) {
            if (activePage.isPartVisible(view)) {
                toggleActive(true);
            }
        }

        /* Add the listener to the control flow view */
        view = activePage.findView(StackbarsView.ID);
        if (view != null) {
            view.getSite().getWorkbenchWindow().getSelectionService()
                    .addPostSelectionListener(selListener);
            view.getSite().getWorkbenchWindow().getPartService().addPartListener(partListener);
        }
    }

}
