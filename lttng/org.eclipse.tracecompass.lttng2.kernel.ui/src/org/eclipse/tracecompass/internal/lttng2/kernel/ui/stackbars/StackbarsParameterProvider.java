//package org.eclipse.linuxtools.internal.lttng2.kernel.ui.stackbars;
//
//import org.eclipse.jface.viewers.ISelection;
//import org.eclipse.jface.viewers.IStructuredSelection;
//import org.eclipse.linuxtools.internal.lttng2.kernel.ui.views.controlflow.ControlFlowEntry;
//import org.eclipse.linuxtools.internal.lttng2.kernel.ui.views.controlflow.ControlFlowView;
//import org.eclipse.linuxtools.tmf.core.analysis.TmfAbstractAnalysisParamProvider;
//import org.eclipse.linuxtools.tmf.core.trace.ITmfTrace;
//import org.eclipse.ui.IPartListener2;
//import org.eclipse.ui.ISelectionListener;
//import org.eclipse.ui.IViewPart;
//import org.eclipse.ui.IWorkbench;
//import org.eclipse.ui.IWorkbenchPage;
//import org.eclipse.ui.IWorkbenchPart;
//import org.eclipse.ui.IWorkbenchPartReference;
//import org.eclipse.ui.PlatformUI;
//
///**
// * @author frraj
// * @since 3.0
// *
// */
//public class StackbarsParameterProvider /*extends TmfAbstractAnalysisParamProvider*/ {
//
//    private ControlFlowEntry fCurrentEntry = null;
//
//    //private static final String NAME = "My Lttng kernel parameter provider"; //$NON-NLS-1$
//
//    private boolean fActive = false;
//    private boolean fEntryChanged = false;
//
//    private IPartListener2 partListener = new IPartListener2() {
//
//        @Override
//        public void partActivated(IWorkbenchPartReference partRef) {
//            if (partRef.getPart(false) instanceof StackbarsView) {
//                toggleActive(true);
//            }
//        }
//
//        @Override
//        public void partBroughtToTop(IWorkbenchPartReference partRef) {
//
//        }
//
//        @Override
//        public void partClosed(IWorkbenchPartReference partRef) {
//            if (partRef.getPart(false) instanceof StackbarsView) {
//                toggleActive(false);
//            }
//        }
//
//        @Override
//        public void partDeactivated(IWorkbenchPartReference partRef) {
//
//        }
//
//        @Override
//        public void partOpened(IWorkbenchPartReference partRef) {
//            if (partRef.getPart(false) instanceof StackbarsView) {
//                toggleActive(true);
//            }
//        }
//
//        @Override
//        public void partHidden(IWorkbenchPartReference partRef) {
//            if (partRef.getPart(false) instanceof StackbarsView) {
//                toggleActive(false);
//            }
//        }
//
//        @Override
//        public void partVisible(IWorkbenchPartReference partRef) {
//            if (partRef.getPart(false) instanceof StackbarsView) {
//                toggleActive(true);
//            }
//        }
//
//        @Override
//        public void partInputChanged(IWorkbenchPartReference partRef) {
//
//        }
//
//    };
//
//    private ISelectionListener selListener = new ISelectionListener() {
//        @Override
//        public void selectionChanged(IWorkbenchPart part, ISelection selection) {
//            if (selection instanceof IStructuredSelection) {
//                Object element = ((IStructuredSelection) selection).getFirstElement();
//                if (element instanceof ControlFlowEntry) {
//                    ControlFlowEntry entry = (ControlFlowEntry) element;
//                    setCurrentThreadEntry(entry);
//                }
//            }
//        }
//    };
//
//    /**
//     * Constructor
//     */
//    public StackbarsParameterProvider() {
//        super();
//        registerListener();
//    }
///*
//    @Override
//    public String getName() {
//        return NAME;
//    }
//*/
//  /*  @Override
//    public Object getParameter(String name) {
//        if (fCurrentEntry == null) {
//            return null;
//        }
//        if (name.equals(StackbarsStateSystemModule.PARAM_TID)) {
//            return fCurrentEntry.getThreadId();
//        }
//        return null;
//    }
//
//    @Override
//    public boolean appliesToTrace(ITmfTrace trace) {
//        return true;//(trace instanceof LttngKernelTrace);
//    }
//*/
//    private void setCurrentThreadEntry(ControlFlowEntry entry) {
//        if (!entry.equals(fCurrentEntry)) {
//            fCurrentEntry = entry;
//            if (fActive) {
//                this.notifyParameterChanged(StackbarsStateSystemModule.PARAM_TID);
//            } else {
//                fEntryChanged = true;
//            }
//        }
//    }
//
//    private void toggleActive(boolean active) {
//        if (active != fActive) {
//            fActive = active;
//            if (fActive && fEntryChanged) {
//                this.notifyParameterChanged(StackbarsStateSystemModule.PARAM_TID);
//                fEntryChanged = false;
//            }
//        }
//    }
//
//    private void registerListener() {
//        final IWorkbench wb = PlatformUI.getWorkbench();
//
//        final IWorkbenchPage activePage = wb.getActiveWorkbenchWindow().getActivePage();
//
//        /* Activate the update if stackbars view visible */
//        IViewPart view = activePage.findView(StackbarsView.ID);
//        if (view != null) {
//            if (activePage.isPartVisible(view)) {
//                toggleActive(true);
//            }
//        }
//
//        /* Add the listener to the control flow view */
//        view = activePage.findView(ControlFlowView.ID);
//        if (view != null) {
//            view.getSite().getWorkbenchWindow().getSelectionService().addPostSelectionListener(selListener);
//            view.getSite().getWorkbenchWindow().getPartService().addPartListener(partListener);
//        }
//    }
//}
