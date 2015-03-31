package org.eclipse.tracecompass.internal.lttng2.kernel.core.analysis.container;


import java.util.HashSet;
import java.util.Set;

import org.eclipse.tracecompass.analysis.os.linux.core.kernelanalysis.KernelAnalysis;
import org.eclipse.tracecompass.analysis.os.linux.core.trace.IKernelTrace;
import org.eclipse.tracecompass.internal.lttng2.kernel.core.trace.layout.LttngEventLayout;
import org.eclipse.tracecompass.lttng2.kernel.core.trace.LttngKernelTrace;
import org.eclipse.tracecompass.tmf.core.analysis.IAnalysisModule;
import org.eclipse.tracecompass.tmf.core.statesystem.ITmfStateProvider;
import org.eclipse.tracecompass.tmf.core.statesystem.TmfStateSystemAnalysisModule;
import org.eclipse.tracecompass.tmf.core.trace.ITmfTrace;
import org.eclipse.tracecompass.tmf.core.trace.TmfTraceManager;
import org.eclipse.tracecompass.tmf.core.trace.TmfTraceUtils;

/**
 * @author Francis Jolivet
 *
 */
public class ContainerAnalysis extends TmfStateSystemAnalysisModule{

    /** The ID of this analysis */
    public static final String ID = "org.eclipse.tracecompass.internal.lttng2.kernel.core.analysis.container"; //$NON-NLS-1$

    public ContainerAnalysis(){
        super();
    }

    @Override
    protected ITmfStateProvider createStateProvider() {
        ITmfTrace trace = getTrace();
        LttngEventLayout layout;

        if(!(trace instanceof LttngKernelTrace)){
            throw new IllegalStateException();
        }
        layout = (LttngEventLayout) ((LttngKernelTrace) trace).getKernelEventLayout();
        return new ContainerStateProvider((IKernelTrace) trace, layout);
    }

    @Override
    protected Iterable<IAnalysisModule> getDependentAnalyses() {
        Set<IAnalysisModule> modules = new HashSet<>();
        /* Depends on the LTTng Kernel analysis modules */
        for (ITmfTrace trace : TmfTraceManager.getTraceSet(getTrace())) {
            for (KernelAnalysis module : TmfTraceUtils.getAnalysisModulesOfClass(trace, KernelAnalysis.class)) {
                modules.add(module);
            }
        }
        return modules;
    }

    /*@Override
    protected boolean executeAnalysis(@Nullable final  IProgressMonitor monitor) {
        // TODO Auto-generated method stub
        return true;
    }

    @Override
    protected void canceling() {
        // TODO Auto-generated method stub

    }*/


}
