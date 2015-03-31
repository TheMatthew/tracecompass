package org.eclipse.tracecompass.internal.lttng2.kernel.core.analysis.container;

import org.eclipse.tracecompass.statesystem.core.statevalue.ITmfStateValue;
import org.eclipse.tracecompass.statesystem.core.statevalue.TmfStateValue;

/**
 * @author Sebastien Lorrain
 *
 *  Representes the valid CPU states used by Lcx Analysis
 */
public class ContainerCpuState {
    /* CPU Status */
    static final int CPU_STATUS_IDLE = 0;
    static final int CPU_STATUS_RUNNING = 1;
    static final int CPU_STATUS_SHARED = 2;

    static final ITmfStateValue CPU_STATUS_IDLE_VALUE = TmfStateValue.newValueInt(CPU_STATUS_IDLE);
    static final ITmfStateValue CPU_STATUS_RUNNING_VALUE = TmfStateValue.newValueInt(CPU_STATUS_RUNNING);
    static final ITmfStateValue CPU_STATUS_SHARED_VALUE = TmfStateValue.newValueInt(CPU_STATUS_SHARED);

}
