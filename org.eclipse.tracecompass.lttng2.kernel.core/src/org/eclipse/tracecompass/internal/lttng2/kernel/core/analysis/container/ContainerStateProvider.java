package org.eclipse.tracecompass.internal.lttng2.kernel.core.analysis.container;

import static org.eclipse.tracecompass.common.core.NonNullUtils.checkNotNull;

import java.util.Map;

import org.eclipse.jdt.annotation.Nullable;
import org.eclipse.tracecompass.statesystem.core.ITmfStateSystemBuilder;
import org.eclipse.tracecompass.statesystem.core.exceptions.StateValueTypeException;
import org.eclipse.tracecompass.statesystem.core.exceptions.TimeRangeException;
import org.eclipse.tracecompass.tmf.core.event.ITmfEvent;
import org.eclipse.tracecompass.tmf.core.event.ITmfEventField;
import org.eclipse.tracecompass.tmf.core.event.aspect.TmfCpuAspect;
import org.eclipse.tracecompass.tmf.core.statesystem.AbstractTmfStateProvider;
import org.eclipse.tracecompass.tmf.core.trace.ITmfTrace;
import org.eclipse.tracecompass.tmf.core.trace.TmfTraceUtils;
import org.eclipse.tracecompass.analysis.os.linux.core.trace.IKernelAnalysisEventLayout;
import org.eclipse.tracecompass.analysis.os.linux.core.trace.IKernelTrace;
import org.eclipse.tracecompass.internal.lttng2.kernel.core.trace.layout.LttngEventLayout;

import com.google.common.collect.ImmutableMap;

/**
 * @author Sébastien Lorrain
 *
 */
public class ContainerStateProvider extends AbstractTmfStateProvider {

    private static final int VERSION = 1;
    private final Map<String, Integer> fEventNames;
    private final LttngEventLayout fLayout;

    private ContainerStatedumpBuilder statedumpBuilder;

    /**
     * @param IKernelTrace
     */
    public ContainerStateProvider(IKernelTrace trace, LttngEventLayout layout)
    {
        super(trace, "Lxc State Provider"); //$NON-NLS-1$
        fLayout = layout;
        fEventNames = buildEventNames(layout);
        statedumpBuilder = new ContainerStatedumpBuilder();
    }

    @Override
    public IKernelTrace getTrace()
    {
        ITmfTrace trace = super.getTrace();
        if (trace instanceof IKernelTrace)
        {
            return (IKernelTrace) trace;
        }
        throw new IllegalStateException("LxcStateProvider : The associated trace should be an experiment"); //$NON-NLS-1$
    }

    @Override
    public int getVersion() {
        return VERSION;
    }

    @Override
    public ContainerStateProvider getNewInstance() {

        IKernelTrace trace = getTrace();
        return new ContainerStateProvider(trace, fLayout);
    }


    @Override
    protected void eventHandle(@Nullable ITmfEvent event) {
        if (event == null) {
            return;
        }

        try {

            final ITmfStateSystemBuilder ssb = checkNotNull(getStateSystemBuilder());

            final String eventName = event.getType().getName();
            final long ts = event.getTimestamp().getValue();

            /*
             * Feed event to the history system if it's known to cause a state
             * transition.
             */
            Integer idx = fEventNames.get(eventName);
            int intval = (idx == null ? -1 : idx.intValue());

            switch (intval){

            case SCHED_PROCESS_FORK:
            {
                ITmfEventField content = event.getContent();
                final int parentTid, parentNSInum, childTid, childVTid, childNSInum;
                try{
                    parentTid = ((Long) content.getField(fLayout.fieldParentTid()).getValue()).intValue();
                    parentNSInum = ((Long) content.getField(fLayout.fieldParentNSInum()).getValue()).intValue();
                    childTid = ((Long) content.getField(fLayout.fieldChildTid()).getValue()).intValue();
                    childVTid = ((Long) content.getField(fLayout.fieldChildVTid()).getValue()).intValue();
                    childNSInum = ((Long) content.getField(fLayout.fieldChildNSInum()).getValue()).intValue();

                }
                catch(NullPointerException ex) {
                    throw new IllegalStateException("LxcStateProvider : The associated trace require " + fLayout.fieldChildNSInum() + " and " + fLayout.fieldParentNSInum() + " fields"); //$NON-NLS-1$
                }

                int containerQuark = ContainerManager.findContainerQuark(ssb, childNSInum);
                //We found the task container, add it to it.
                if(containerQuark != ContainerManager.getRootContainerQuark(ssb))
                {
                    ContainerManager.appendTask(ssb, containerQuark, ts, childVTid, childTid);
                }
                else if(parentNSInum != childNSInum) //The container does not exist. Add it
                {
                    ContainerInfo cInfo = new ContainerInfo(childNSInum, parentTid, "TODO_ADD_HOSTNAME");
                    ContainerManager.addContainerAndTask(ssb, ts, parentNSInum, childTid, childVTid, cInfo);
                }
                //else the task is in the root namespace, dont process it.

            }
                break;

            case SCHED_SWITCH:
            {
                ITmfEventField content = event.getContent();
                Integer prevTid = ((Long) content.getField(fLayout.fieldPrevTid()).getValue()).intValue();
                Integer nextTid = ((Long) content.getField(fLayout.fieldNextTid()).getValue()).intValue();

                Object cpuObj = TmfTraceUtils.resolveEventAspectOfClassForEvent(event.getTrace(), TmfCpuAspect.class, event);
                if (cpuObj == null) {
                    /* We couldn't find any CPU information, ignore this event */
                    return;
                }
                Integer cpu = (Integer) cpuObj;

                //Set the CPU in the container that has been sched_switched to IDLE
                int prevTidParentContainerQuark = ContainerManager.getContainerFromTask(ssb, prevTid);
                ContainerManager.updateContainerCPUState(ssb, prevTidParentContainerQuark, ts , cpu, ContainerCpuState.CPU_STATUS_IDLE_VALUE);

                //Set the CPU in the container that acquired run to RUNNING
                int nextTidParentContainerQuark = ContainerManager.getContainerFromTask(ssb, nextTid);
                ContainerManager.updateContainerCPUState(ssb, nextTidParentContainerQuark, ts,  cpu, ContainerCpuState.CPU_STATUS_RUNNING_VALUE);

                //Set the running vtid of the CPU
                ContainerManager.setCpuCurrentlyRunningTask(ssb, nextTidParentContainerQuark, ts, cpu, nextTid);

            }
                break;

            case SCHED_PROCESS_FREE:
            {
//                Integer tid = ((Long) event.getContent().getField(fLayout.fieldTid()).getValue()).intValue();
//                ContainerManager.removeTask(ssb, ts, tid);
            }
                break;


            case STATEDUMP_PROCESS:
            {
                ITmfEventField content = event.getContent();
                final int tID, vtID, ppID, ns_level, status, ns_inum;
                try{
                    tID = ((Long) content.getField(fLayout.fieldTid()).getValue()).intValue();
                    vtID = ((Long) content.getField(fLayout.fieldVTid()).getValue()).intValue();
                    ppID = ((Long) content.getField(fLayout.fieldPPid()).getValue()).intValue();
                    ns_level = ((Long) content.getField(fLayout.fieldNSLevel()).getValue()).intValue();
                    status = ((Long) content.getField(fLayout.fieldStatus()).getValue()).intValue();
                    ns_inum = ((Long) content.getField(fLayout.fieldNSInum()).getValue()).intValue();
                }
                catch(NullPointerException ex) {
                    throw new IllegalStateException("LxcStateProvider : The associated trace require " + fLayout.fieldNSInum() + " field"); //$NON-NLS-1$
                }

                Object cpuObj = TmfTraceUtils.resolveEventAspectOfClassForEvent(event.getTrace(), TmfCpuAspect.class, event);
                if (cpuObj == null) {
                    /* We couldn't find any CPU information, ignore this event */
                    return;
                }
                Integer cpu = (Integer) cpuObj;

                Task t = new Task(ts, tID, vtID, ppID, ns_level, status, cpu, ns_inum);
                statedumpBuilder.addTask(ssb, t);
            }
                break;

            default:
                break;
            }
        }
        catch (TimeRangeException tre) {
            /*
             * This would happen if the events in the trace aren't ordered
             * chronologically, which should never be the case ...
             */
            System.err.println("TimeRangeExcpetion caught in the state system's event manager."); //$NON-NLS-1$
            System.err.println("Are the events in the trace correctly ordered?"); //$NON-NLS-1$
            tre.printStackTrace();

        } catch (StateValueTypeException sve) {
            /*
             * This would happen if we were trying to push/pop attributes not of
             * type integer. Which, once again, should never happen.
             */
            sve.printStackTrace();
        }

    }


    // ------------------------------------------------------------------------
    // Event names management
    // ------------------------------------------------------------------------

    private static final int SCHED_PROCESS_FORK = 1;
    private static final int SCHED_PROCESS_FREE = 2;
    private static final int SCHED_SWITCH       = 3;
    private static final int STATEDUMP_PROCESS  = 4;

    private static Map<String, Integer> buildEventNames(IKernelAnalysisEventLayout layout) {
        ImmutableMap.Builder<String, Integer> builder = ImmutableMap.builder();

        builder.put(layout.eventSchedProcessFork(),         SCHED_PROCESS_FORK );
        builder.put(layout.eventSchedProcessFree(),         SCHED_PROCESS_FREE );
        builder.put(layout.eventSchedSwitch(),              SCHED_SWITCH       );
        builder.put(layout.eventStatedumpProcessState(),    STATEDUMP_PROCESS  );

        return checkNotNull(builder.build());
    }
}
