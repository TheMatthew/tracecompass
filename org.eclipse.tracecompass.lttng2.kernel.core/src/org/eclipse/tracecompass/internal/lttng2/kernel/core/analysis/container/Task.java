package org.eclipse.tracecompass.internal.lttng2.kernel.core.analysis.container;

import org.eclipse.tracecompass.analysis.os.linux.core.kernelanalysis.StateValues;
import org.eclipse.tracecompass.statesystem.core.statevalue.ITmfStateValue;

public class Task
{
    public final long ts;
    public final int tID;
    public final int vTID;
    public final int pPID;
    public final int ns_level;
    public final ITmfStateValue cpuState;
    public final int cpuID;
    public final int ns_inode;

    public Task(long ts, int tID, int vTID, int pPID, int ns_level, int status, int cpu, int ns_inode)
    {
        this.ts = ts;
        this.tID = tID;
        this.vTID = vTID;
        this.pPID = pPID;
        this.ns_level = ns_level;
        this.cpuID = cpu;
        this.ns_inode = ns_inode;

        switch(status)
        {
        case StateValues.PROCESS_STATUS_INTERRUPTED:
        case StateValues.PROCESS_STATUS_UNKNOWN:
        case StateValues.PROCESS_STATUS_WAIT_FOR_CPU:
            this.cpuState = ContainerCpuState.CPU_STATUS_IDLE_VALUE;
            break;
        case StateValues.PROCESS_STATUS_RUN_SYSCALL:
        case StateValues.PROCESS_STATUS_RUN_USERMODE:
            this.cpuState = ContainerCpuState.CPU_STATUS_RUNNING_VALUE;
            break;
        default:
            this.cpuState = ContainerCpuState.CPU_STATUS_IDLE_VALUE;
            break;
        }

    }
}