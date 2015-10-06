///*******************************************************************************
// * Copyright (c) 2013 École Polytechnique de Montréal
// *
// * All rights reserved. This program and the accompanying materials are
// * made available under the terms of the Eclipse Public License v1.0 which
// * accompanies this distribution, and is available at
// * http://www.eclipse.org/legal/epl-v10.html
// *
// * Contributors:
// *   Geneviève Bastien - Initial API and implementation
// *******************************************************************************/
//
//package org.eclipse.linuxtools.internal.lttng2.kernel.ui.stackbars;
//
//import java.io.File;
//import java.util.List;
//
//import org.eclipse.core.runtime.IProgressMonitor;
//import org.eclipse.jdt.annotation.NonNull;
//import org.eclipse.linuxtools.internal.lttng2.kernel.ui.stackbars.StackbarsModelProvider.StackChartEvent;
//import org.eclipse.linuxtools.tmf.core.exceptions.TmfAnalysisException;
//import org.eclipse.linuxtools.tmf.core.signal.TmfAnalysisFinishedSignal;
//import org.eclipse.linuxtools.tmf.core.signal.TmfSignalManager;
//import org.eclipse.linuxtools.tmf.core.statesystem.ITmfStateProvider;
//import org.eclipse.linuxtools.tmf.core.statesystem.TmfStateSystemAnalysisModule;
//import org.eclipse.linuxtools.tmf.core.trace.ITmfTrace;
//import org.eclipse.linuxtools.tmf.core.trace.TmfTraceManager;
//
///**
// * State System Module for stackbars
// *
// * @author GBastien, MCote
// * @since 3.0
// */
//public class StackbarsStateSystemModule extends TmfStateSystemAnalysisModule {
//
//    /**
//     * The file name of the History Tree
//     */
//    public final static String HISTORY_TREE_FILE_NAME = "stackbarsHistory.ht"; //$NON-NLS-1$
//
//    /** The ID of this analysis module */
//    public final static String ID = "org.eclipse.linuxtools.lttng2.kernel.ui.stackbarsWithSS"; //$NON-NLS-1$
//    static public final String PARAM_TID = "tid"; //$NON-NLS-1$
//    static public final String PARAM_START_EVENT = "startEvent"; //$NON-NLS-1$
//    static public final String PARAM_END_EVENT = "endEvent"; //$NON-NLS-1$
//
//    private StackbarsStateProvider fStackbarsStateProvider;
//    private int fTid;
//    private LastActionRequested fLastAction;
//
//    enum LastActionRequested{
//        GLOBAL_CHANGE,
//        DEPTH_CHANGED
//    }
//
//    public StackbarsStateSystemModule()
//    {
//        fLastAction = LastActionRequested.GLOBAL_CHANGE;
//    }
//
//    @Override
//    public void setTrace(ITmfTrace trace) throws TmfAnalysisException {
//        super.setTrace(trace);
//        deleteHistoryFiles();
//    }
//
//    public ITmfTrace getTracePublic()
//    {
//        return getTrace();
//    }
//
//    // TODO maybe it is better to do that at the end or to save the information in the file to be
//    // able to use it when reload, now, it is not possible to use it so we delete it to avoid that the module
//    // use the state system because the file exist, it is not the same method to remove the file that when the events
//    // are changed because the backend files are null at the beginning
//    private void deleteHistoryFiles() {
//        ITmfTrace trace = getTrace();
//        if(trace != null)
//        {
//            File stateFile = new File(TmfTraceManager.getSupplementaryFileDir(trace) + HISTORY_TREE_FILE_NAME);
//            if (stateFile.exists()) {
//                stateFile.delete();
//            }
//        }
//    }
//
//    public LastActionRequested getLastActionRequested()
//    {
//        return fLastAction;
//    }
//
//    public void setLastActionToDepthChanged()
//    {
//        fLastAction = LastActionRequested.DEPTH_CHANGED;
//    }
//
//    public int getTid() {
//        return fTid;
//    }
//
//    @Override
//    protected void parameterChanged(String name) {
//        fLastAction = LastActionRequested.GLOBAL_CHANGE;
//        if (name.equals(PARAM_TID)) {
//            fTid = (int) getParameter(PARAM_TID);
//            cancel();
//            resetAnalysis();
//            schedule();
//        }
//        else if (name.equals(PARAM_START_EVENT) || name.equals(PARAM_END_EVENT)) {
//            cancel();
//            deleteBackendFiles();
//            resetAnalysis();
//            schedule();
//        }
//    }
//
//    @SuppressWarnings("null")
//    @Override
//    protected String getSsFileName() {
//        return HISTORY_TREE_FILE_NAME;
//    }
//
//    @Override
//    protected @NonNull ITmfStateProvider createStateProvider() {
//        fStackbarsStateProvider = new StackbarsStateProvider(getTrace());
//        Object startEvent = super.getParameter(PARAM_START_EVENT);
//        if (startEvent instanceof List<?>) {
//            fStackbarsStateProvider.setStartEvent((List<StackChartEvent>) startEvent);
//        }
//
//        Object endEvent = super.getParameter(PARAM_END_EVENT);
//        if (endEvent instanceof List<?>) {
//            fStackbarsStateProvider.setEndEvent((List<StackChartEvent>) endEvent);
//        }
//        return fStackbarsStateProvider;
//    }
//
//    @Override
//    protected StateSystemBackendType getBackendType() {
//        return StateSystemBackendType.FULL;
//    }
//
//    @Override
//    protected boolean executeAnalysis(IProgressMonitor monitor) {
//
//        boolean returnValue = super.executeAnalysis(monitor);
//        if (returnValue == true)
//        {
//            TmfSignalManager.dispatchSignal(new TmfAnalysisFinishedSignal(this, this));
//        }
//        return returnValue;
//    }
//
//    public int getMaxDepth() {
//        return fStackbarsStateProvider.getMaxDepth();
//    }
//
//}
