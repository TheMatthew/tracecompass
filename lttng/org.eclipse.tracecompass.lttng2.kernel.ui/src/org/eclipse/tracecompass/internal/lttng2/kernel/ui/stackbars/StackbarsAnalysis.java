package org.eclipse.tracecompass.internal.lttng2.kernel.ui.stackbars;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Stack;
import java.util.TreeMap;
import java.util.Vector;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.jface.action.Action;
import org.eclipse.osgi.util.NLS;
import org.eclipse.tracecompass.analysis.graph.core.base.TmfEdge;
import org.eclipse.tracecompass.analysis.graph.core.base.TmfGraph;
import org.eclipse.tracecompass.analysis.graph.core.base.TmfVertex;
import org.eclipse.tracecompass.analysis.graph.core.building.TmfGraphBuilderModule;
import org.eclipse.tracecompass.analysis.graph.core.criticalpath.CriticalPathModule;
import org.eclipse.tracecompass.analysis.os.linux.core.kernelanalysis.Attributes;
import org.eclipse.tracecompass.internal.analysis.graph.core.base.TmfGraphVisitor;
import org.eclipse.tracecompass.internal.lttng2.kernel.core.analysis.graph.building.LttngKernelExecutionGraph;
import org.eclipse.tracecompass.internal.lttng2.kernel.core.analysis.graph.model.LttngStrings;
import org.eclipse.tracecompass.internal.lttng2.kernel.ui.stackbars.CPComplementPresentationProvider.State;
import org.eclipse.tracecompass.internal.lttng2.kernel.ui.stackbars.StackbarsExecutionsDetection.ExecDefinition;
import org.eclipse.tracecompass.internal.lttng2.kernel.ui.stackbars.StackbarsExecutionsDetection.StackbarsFilter;
import org.eclipse.tracecompass.statesystem.core.ITmfStateSystem;
import org.eclipse.tracecompass.statesystem.core.exceptions.AttributeNotFoundException;
import org.eclipse.tracecompass.statesystem.core.interval.ITmfStateInterval;
import org.eclipse.tracecompass.tmf.core.analysis.IAnalysisModule;
import org.eclipse.tracecompass.tmf.core.component.ITmfEventProvider;
import org.eclipse.tracecompass.tmf.core.event.ITmfEvent;
import org.eclipse.tracecompass.tmf.core.event.ITmfEventField;
import org.eclipse.tracecompass.tmf.core.request.ITmfEventRequest;
import org.eclipse.tracecompass.tmf.core.request.TmfEventRequest;
import org.eclipse.tracecompass.tmf.core.signal.TmfSignalManager;
import org.eclipse.tracecompass.tmf.core.statesystem.TmfStateSystemAnalysisModule;
import org.eclipse.tracecompass.tmf.core.timestamp.ITmfTimestamp;
import org.eclipse.tracecompass.tmf.core.timestamp.TmfTimeRange;
import org.eclipse.tracecompass.tmf.core.timestamp.TmfTimestamp;
import org.eclipse.tracecompass.tmf.core.trace.ITmfTrace;
import org.eclipse.tracecompass.tmf.core.trace.TmfTraceManager;
import org.eclipse.tracecompass.tmf.ui.widgets.timegraph.model.ILinkEvent;
import org.eclipse.tracecompass.tmf.ui.widgets.timegraph.model.ITimeEvent;
import org.eclipse.tracecompass.tmf.ui.widgets.timegraph.model.TimeEvent;
import org.eclipse.tracecompass.tmf.ui.widgets.timegraph.model.TimeGraphEntry;
import org.eclipse.tracecompass.tmf.ui.widgets.timegraph.model.TimeLinkEvent;
import org.eclipse.tracecompass.tmf.ui.widgets.timegraph.widgets.TimeGraphControl;

public final class StackbarsAnalysis{

    private static volatile StackbarsAnalysis instance = null;
    private Vector<Long> fDeadlineByDepth;
    private int fCurrentDepth;
    private StackbarsExecutionsDetection fExecutionsDetection;
    private StackbarsExecution fHeadExecutions;
    private Stack<Integer> fDepthPath;
    private StackbarsView fView;
    private StackbarsEntry fCurrentExecution;
    private ExtendedEntry fCurrentExtendedEntry;
    private List<TimeGraphEntry> fPreemptedList;
    private List<TimeGraphEntry> fExtendedList;
    private List<TimeGraphEntry> fExtendedTimeList;
    private Map<Object, TimeGraphEntry> fMapStackbarsEntries;
    private List<ILinkEvent> fLinkEvents;

    public static final long NO_DEADLINE = -1L;
    public static final String ALLINONEEXEC = "$ALLINONE";

    //TODO mettre dans LttngStrings
    public static final String SCHED_PI_SETPRIO = "sched_pi_setprio";
    public static final String FUTEX = "futex";
    public static final String MQ_SEND = "mq_send";
    public static final String MQ_TIMEDSEND = "mq_timedsend";
    public static final String MQ_RECEIVE = "mq_receive";
    public static final String MQ_TIMEDRECEIVE = "mq_timedreceive";
    public static final String MQDES = "mqdes";
    public static final String UADDR = "uaddr";
    public static final String OLDPRIO = "oldprio";
    public static final String NEWPRIO = "newprio";

    public static final String SYSCALL_ENTRY_FUTEX = "syscall_entry_futex";
    public static final String SYSCALL_ENTRY_MQ_SEND = "syscall_entry_mq_send";
    public static final String SYSCALL_ENTRY_MQ_TIMEDSEND = "syscall_entry_mq_timedsend";
    public static final String SYSCALL_ENTRY_MQ_RECEIVE = "syscall_entry_mq_receive";
    public static final String SYSCALL_ENTRY_MQ_TIMEDRECEIVE = "syscall_entry_mq_timedreceive";
    public static final String SYSCALL_EXIT_FUTEX = "syscall_exit_futex";
    public static final String SYSCALL_EXIT_MQ_SEND = "syscall_exit_mq_send";
    public static final String SYSCALL_EXIT_MQ_TIMEDSEND = "syscall_exit_mq_timedsend";
    public static final String SYSCALL_EXIT_MQ_RECEIVE = "syscall_exit_mq_receive";
    public static final String SYSCALL_EXIT_MQ_TIMEDRECEIVE = "syscall_exit_mq_timedreceive";
    public static final String OP = "op";
    public static final String VAL = "val";
    public static final String UTIME = "utime";
    public static final String UADDR2 = "uaddr2";
    public static final String VAL3 = "val3";
    public static final String CLOCKID = "clockid";
    public static final String MODE = "mode";
    public static final String NOW = "now";
    public static final String FUNCTION = "function";
    public static final String MSG_LEN = "msg_len";
    public static final String U_ABS_TIMEOUT = "u_abs_timeout";
    public static final String RET = "ret";
    public static final String U_MSG_PRIO = "u_msg_prio";
    public static final String MSG_PRIO = "msg_prio";
    public static final String U_MSG_PTR = "u_msg_ptr";
    public static final String EXPIRES = "expires";
    public static final String SOFTEXPIRES = "softexpires";

    private ArrayList<int[]> fResults;
    private String[] fInverseDictionary;

    private StackbarsAnalysis()
    {
        fExecutionsDetection = new StackbarsExecutionsDetection(this);
        fDeadlineByDepth = new Vector<>();
        fDeadlineByDepth.add(NO_DEADLINE);
        fCurrentDepth = 0;
        fDepthPath = new Stack<>();
    }

    public void setView(StackbarsView view)
    {
        fView = view;
    }

    public final static StackbarsAnalysis getInstance() {
        if (StackbarsAnalysis.instance == null) {
           synchronized(StackbarsAnalysis.class) {
             if (StackbarsAnalysis.instance == null) {
                 StackbarsAnalysis.instance = new StackbarsAnalysis();
             }
           }
        }
        return StackbarsAnalysis.instance;
    }

    public void defineBorderEvents(ExecDefinition fExecDef)
    {
        fExecutionsDetection.defineBorderEvents(fExecDef);
        fDeadlineByDepth.clear();
        for(int i = 0; i < fExecDef.fBorderEventsByDepth.size(); ++i)
        {
            long deadline;
            try{
                deadline = Long.parseLong(fExecDef.fBorderEventsByDepth.get(i).fDeadline);
            }
            catch (Exception e)
            {
                deadline = NO_DEADLINE;
            }
            fDeadlineByDepth.add(deadline);
        }
    }

    public Long getCurrentDeadline()
    {
        if(fDeadlineByDepth.size() > fCurrentDepth)
        {
            return fDeadlineByDepth.get(fCurrentDepth);
        }
        return NO_DEADLINE;
    }

    public int getCurrentDepth()
    {
        return fCurrentDepth;
    }

    public void executeAnalysisStats(IProgressMonitor monitor, ITmfTrace trace)
    {
        System.out.println("");
        System.out.println("-------------------------------------");
        System.out.println("Executing Stats");
        System.out.println("-------------------------------------");
        System.out.println("");

        if(trace == null)
        {
            return;
        }

        if(monitor != null && monitor.isCanceled())
        {
            return;
        }

        List<TimeGraphEntry> list = getHeadExecutions(trace, monitor);
        if(list == null)
        {
            return;
        }

        double avgRunning = 0;
        double avgPreempted = 0;
        for(TimeGraphEntry entry : list)
        {
            if(entry instanceof StackbarsEntry)
            {
                StackbarsEntry sbEntry = (StackbarsEntry)entry;
                avgRunning += sbEntry.getRunningTime()/list.size();
                avgPreempted += sbEntry.getPreemptedTime();
            }
        }

        System.out.println(avgRunning + " " + avgPreempted);

        long deadline = fDeadlineByDepth.get(fCurrentDepth);
        if(deadline == NO_DEADLINE)
        {
            System.out.println("There is no deadline defined.");
            return;
        }

        System.out.println("");
        System.out.println("");
        System.out.println("----------------------------------");
        System.out.println("End of Stats");
        System.out.println("----------------------------------");
    }

    public static void executePatternDetection(ITmfTrace trace, int currentTid, HashSet<String> startNames) {

        System.out.println("");
        System.out.println("----------------------------------");
        System.out.println("Executing Blocking Device Analysis");
        System.out.println("----------------------------------");
        System.out.println("");

        if(trace == null)
        {
            return;
        }

        PatternsDetection pD = new PatternsDetection();
        Vector<Integer> vecTest = new Vector<>();
        vecTest.add(currentTid);
        HashSet<String> invalidNames = new HashSet<>();
        invalidNames.add("rcu_utilization");
        invalidNames.add("power_cpu_idle");
        int minSupport = 1000;
        int maxFrequent = 15;
        pD.findPatterns(trace, vecTest, 10000, invalidNames, startNames, minSupport, maxFrequent);
    }

    private class EventInfo
    {
        private String fName;
        private ITmfEvent fEvent;

        public EventInfo()
        {
            fName = ""; //$NON-NLS-1$
        }

        public String getName() {
            return fName;
        }
        public void setName(String fName) {
            this.fName = fName;
        }
        public ITmfEvent getEvent() {
            return fEvent;
        }
        public void setEvent(ITmfEvent fEvent) {
            this.fEvent = fEvent;
        }
    }

    public static void executeAnalysisPI(ITmfTrace trace)
    {
        System.out.println("");
        System.out.println("----------------------------------");
        System.out.println("Executing PI - TODO");
        System.out.println("----------------------------------");
        System.out.println("");

        if(trace == null)
        {
            return;
        }

    }

    public void executeAnalysisExtended(final IProgressMonitor monitor, ITmfTrace trace, int tidFixed, long startTimeFixed, long endTimeFixed, final HashSet<Integer> fSetCheckedTypes)
    {
        System.out.println("");
        System.out.println("----------------------------------");
        System.out.println("Execution Extended Analysis");
        System.out.println("----------------------------------");
        System.out.println("");

        if(trace == null)
        {
            return;
        }

        if(monitor.isCanceled())
        {
            return;
        }

        final StackbarsEntry currentExecution = fCurrentExecution;

        long startTime;
        long endTime;
        final int tidToSearch;

        if(startTimeFixed != ExtendedComparisonOptionsDialog.BLANK && endTimeFixed != ExtendedComparisonOptionsDialog.BLANK &&
                startTimeFixed < endTimeFixed)
        {
            startTime = startTimeFixed;
            endTime = endTimeFixed;
        }
        else
        {
            if(currentExecution == null)
            {
                return;
            }
            startTime = currentExecution.getRealStartTime();
            endTime = currentExecution.getRealStartTime()
                    + currentExecution.getDuration();
        }

        if(tidFixed != ExtendedComparisonOptionsDialog.BLANK)
        {
            tidToSearch = tidFixed;
        }
        else
        {
            if(currentExecution == null)
            {
                return;
            }
            tidToSearch = currentExecution.getTidStart();
        }

        // 1) First pass we will collect only for the current tid(s) and we will collect information about preemption
        //    We do this only for the range of the current execution.
        TmfTimeRange range = new TmfTimeRange(new TmfTimestamp(startTime,
                ITmfTimestamp.NANOSECOND_SCALE), new TmfTimestamp(endTime, ITmfTimestamp.NANOSECOND_SCALE));

        final List<TimeGraphEntry> listExtendedEntry = new ArrayList<>();

        TmfEventRequest request;
        request = new TmfEventRequest(ITmfEvent.class, range,
                0,
                ITmfEventRequest.ALL_DATA,
                TmfEventRequest.ExecutionType.FOREGROUND) {

            int[]threadByCpu = new int[8]; // TODO Fill that with current tid
            int currentTid = tidToSearch;
            boolean preemptionOccured = false; // TODO fill that with current data
            boolean inASyscall = false;
            EventInfo otherSyscall = new EventInfo();
            ITmfEvent wakeupEvent = null;
            ITmfEvent scheduledOutEvent = null;
            ITmfEvent irqEvent = null;
            ITmfEvent softirqEvent = null;
            ITmfEvent hrtimerEvent = null;
            int hrtimerCPU = -1;
            String timerId = "";
            ITmfEvent futexEvent = null;
            ITmfEvent mqsendEvent = null;
            ITmfEvent mqreceiveEvent = null;
            HashMap<Integer, ITmfEvent> mapIRQ = new HashMap<>();
            HashMap<Integer, ITmfEvent> mapSoftIRQ = new HashMap<>();

            @Override
            public void handleData(ITmfEvent event) {
                // If the job is canceled, cancel the request so waitForCompletion() will unlock
                if (monitor.isCanceled()) {
                    cancel();
                    return;
                }

                // 2) Check if we are scheduled out and update current thread
                // TODO check if trace as the context tid
                int source = Integer.parseInt(event.getSource());
                if(source >= threadByCpu.length)
                {
                    // 2.1) Enlarge if too small
                    int[] temp = threadByCpu;
                    threadByCpu = new int[source];
                    for(int i = 0; i < temp.length; ++i)
                    {
                        threadByCpu[i] = temp[i];
                    }
                }

                // 2.2) Check the sched_switch event to update array and data
                String eventName = event.getType().getName();
                if (eventName.equals(LttngStrings.SCHED_SWITCH)) {

                    // 2.3) If we are scheduled out,
                    if(threadByCpu[source] == currentTid)
                    {
                        if(!inASyscall)
                        {
                            scheduledOutEvent = event;
                        }
                    }

                    // 2.4) Set the current scheduled process on the relevant CPU for the next iteration
                    ITmfEventField eventField = event.getContent();
                    long nextTid = (Long) eventField.getField(LttngStrings.NEXT_TID).getValue();
                    threadByCpu[source] = (int) nextTid;

                    // 2.5) If the next tid is our tid, close execution for wakeup
                    if(threadByCpu[source] == currentTid)
                    {
                        preemptionOccured = true;
                        if(fSetCheckedTypes.contains(ExtendedEntry.Type.WAKEUP.ordinal()) && wakeupEvent != null)
                        {
                            //Add execution
                            listExtendedEntry.add(new ExtendedEntry("", wakeupEvent.getTimestamp().getValue(),
                                    event.getTimestamp().getValue(), currentTid, "information", ExtendedEntry.Type.WAKEUP, ExtendedEntry.NO_VALUE));
                            wakeupEvent = null;
                        }
                        if(fSetCheckedTypes.contains(ExtendedEntry.Type.PREEMPTED.ordinal()) && scheduledOutEvent != null)
                        {
                            //Add execution
                            listExtendedEntry.add(new ExtendedEntry("", scheduledOutEvent.getTimestamp().getValue(),
                                    event.getTimestamp().getValue(), currentTid, "information", ExtendedEntry.Type.PREEMPTED, ExtendedEntry.NO_VALUE));
                            scheduledOutEvent = null;
                        }
                    }

                    return;
                }

                // 3) Execution for scheduling time is from sched_wakeup to sched_switch
                if(eventName.equals(LttngStrings.SCHED_WAKEUP) ||
                        eventName.equals(LttngStrings.SCHED_WAKEUP_NEW)) {

                    final int tid = ((Long) event.getContent().getField(LttngStrings.TID).getValue()).intValue();

                    if(tid == currentTid)
                    {
                        wakeupEvent = event;
                        if(scheduledOutEvent != null)
                        {
                            ITmfEvent eventIRQ = mapIRQ.get(source);
                            if(fSetCheckedTypes.contains(ExtendedEntry.Type.PREEMPTED.ordinal()) && eventIRQ != null)
                            {
                                long irq = (long) eventIRQ.getContent().getField(LttngStrings.IRQ).getValue();
                                //Add execution
                                listExtendedEntry.add(new ExtendedEntry("", scheduledOutEvent.getTimestamp().getValue(),
                                        event.getTimestamp().getValue(), currentTid, "irq="+Long.toString(irq), ExtendedEntry.Type.PREEMPTED, ExtendedEntry.NO_VALUE));
                                scheduledOutEvent = null;
                            }
                            else
                            {
                                ITmfEvent eventSoftIRQ = mapSoftIRQ.get(source);
                                if(fSetCheckedTypes.contains(ExtendedEntry.Type.PREEMPTED.ordinal()) && eventSoftIRQ != null)
                                {
                                    long vec = (long) eventSoftIRQ.getContent().getField(LttngStrings.VEC).getValue();
                                    //Add execution
                                    listExtendedEntry.add(new ExtendedEntry("", scheduledOutEvent.getTimestamp().getValue(),
                                            event.getTimestamp().getValue(), currentTid, "vec="+Long.toString(vec), ExtendedEntry.Type.PREEMPTED, ExtendedEntry.NO_VALUE));
                                    scheduledOutEvent = null;
                                }
                                else if (fSetCheckedTypes.contains(ExtendedEntry.Type.HRTIMER.ordinal()) && hrtimerCPU == source)
                                {
                                    //Add execution
                                    listExtendedEntry.add(new ExtendedEntry("", scheduledOutEvent.getTimestamp().getValue(),
                                            event.getTimestamp().getValue(), currentTid, "information", ExtendedEntry.Type.HRTIMER, ExtendedEntry.NO_VALUE));
                                    scheduledOutEvent = null;
                                }
                                else if (fSetCheckedTypes.contains(ExtendedEntry.Type.PREEMPTED.ordinal())){
                                    //Add execution
                                    listExtendedEntry.add(new ExtendedEntry("", scheduledOutEvent.getTimestamp().getValue(),
                                            event.getTimestamp().getValue(), currentTid, "information", ExtendedEntry.Type.PREEMPTED, ExtendedEntry.NO_VALUE));
                                    scheduledOutEvent = null;
                                }
                            }
                        }
                    }
                    return;
                }

                // We are not necessary running
                else if(fSetCheckedTypes.contains(ExtendedEntry.Type.HRTIMER.ordinal()))
                {
                    if(eventName.equals(LttngStrings.HRTIMER_EXPIRE_ENTRY))
                    {
                        if(hrtimerEvent != null)
                        {
                            ITmfEventField eventField = event.getContent();
                            String hrtimer = eventField.getField(LttngStrings.HRTIMER).getFormattedValue();
                            if(timerId.equals(hrtimer))
                            {
                                hrtimerCPU = source;
                                //Add execution
                                listExtendedEntry.add(new ExtendedEntry("", hrtimerEvent.getTimestamp().getValue(),
                                        event.getTimestamp().getValue(), currentTid, "id = " + timerId, ExtendedEntry.Type.HRTIMER, timerId));
                                hrtimerEvent = null;

                            }
                        }
                    }
                    else if(eventName.equals(LttngStrings.HRTIMER_EXPIRE_EXIT))
                    {
                        if(hrtimerEvent != null)
                        {
                            ITmfEventField eventField = event.getContent();
                            String hrtimer = eventField.getField(LttngStrings.HRTIMER).getFormattedValue();
                            if(timerId.equals(hrtimer))
                            {
                                hrtimerCPU = -1;
                                timerId = "";
                            }
                        }
                    }
                }

                if(fSetCheckedTypes.contains(ExtendedEntry.Type.IRQ.ordinal()))
                {
                    if(eventName.equals(LttngStrings.IRQ_HANDLER_ENTRY))
                    {
                        mapIRQ.put(source, event);
                        if(threadByCpu[source] == currentTid)
                        {
                            irqEvent = event;
                        }
                    }
                    else if(eventName.equals(LttngStrings.IRQ_HANDLER_EXIT))
                    {
                        mapIRQ.put(source, null);
                        if(threadByCpu[source] == currentTid)
                        {
                            if(irqEvent != null)
                            {
                                long irq = (long) irqEvent.getContent().getField(LttngStrings.IRQ).getValue();
                                //Add execution
                                listExtendedEntry.add(new ExtendedEntry("", irqEvent.getTimestamp().getValue(),
                                        event.getTimestamp().getValue(), currentTid, "irq=" + Long.toString(irq), ExtendedEntry.Type.IRQ, ExtendedEntry.NO_VALUE));
                                irqEvent = null;
                            }
                        }
                    }
                }

                if(eventName.equals(LttngStrings.SOFTIRQ_ENTRY))
                {
                    mapSoftIRQ.put(source, event);//TODO can be preempted?
                    if(threadByCpu[source] == currentTid)
                    {
                        softirqEvent = event;
                    }
                }
                else if(eventName.equals(LttngStrings.SOFTIRQ_EXIT))
                {
                    mapSoftIRQ.put(source, null);
                    if(threadByCpu[source] == currentTid)
                    {
                        if(fSetCheckedTypes.contains(ExtendedEntry.Type.SOFTIRQ.ordinal()) && softirqEvent != null)
                        {
                            long vec = (long) softirqEvent.getContent().getField(LttngStrings.VEC).getValue();
                            //Add execution
                            listExtendedEntry.add(new ExtendedEntry("", softirqEvent.getTimestamp().getValue(),
                                    event.getTimestamp().getValue(), currentTid, "vec=" + Long.toString(vec), ExtendedEntry.Type.SOFTIRQ, ExtendedEntry.NO_VALUE));
                            softirqEvent = null;
                        }
                    }
                }

                // 4) For the others, we need a matching tid
                if(threadByCpu[source] != currentTid)
                {
                    return;
                }

                if(fSetCheckedTypes.contains(ExtendedEntry.Type.HRTIMER.ordinal()))
                {
                    if(eventName.equals(LttngStrings.HRTIMER_START))
                    {
                        hrtimerEvent = event;
                        ITmfEventField eventField = event.getContent();
                        timerId = eventField.getField(LttngStrings.HRTIMER).getFormattedValue();
                    }

                }

                //For the others, we need a syscall
                if(!eventName.startsWith("syscall_"))//TODO constant
                {
                    return;
                }
                eventName = eventName.substring(8);//TODO constant

                //For the entry
                boolean entry = false;
                if(eventName.startsWith("entry_"))//TODO constant
                {
                    eventName = eventName.substring(6);//TODO constant
                    entry = true;
                    inASyscall = true;
                }
                else // exit_
                {
                    eventName = eventName.substring(5);//TODO constant
                    inASyscall = false;
                }

                // 5) Analysis for futex : from entry to exit
                if(fSetCheckedTypes.contains(ExtendedEntry.Type.FUTEX.ordinal()) && eventName.equals(FUTEX))
                {
                    preemptionOccured = false;
                    if(entry)
                    {
                        futexEvent = event;
                    }
                    else if(futexEvent != null)
                    {
                        //We need to keep the futex address
                        ITmfEventField eventField = futexEvent.getContent();
                        String futexAddr = eventField.getField(UADDR).getFormattedValue();

                        //Op
                        /* 10 #define FUTEX_WAIT              0
                         11 #define FUTEX_WAKE              1
                         12 #define FUTEX_FD                2 -> remove
                         13 #define FUTEX_REQUEUE           3 -> wake the val et wait sur uaddr2 pour les autres
                         14 #define FUTEX_CMP_REQUEUE       4 -> requeue sans race
                         15 #define FUTEX_WAKE_OP           5 -> was previously used by glibc to fix a bug
                         16 #define FUTEX_LOCK_PI           6 -> priority inheritance
                         17 #define FUTEX_UNLOCK_PI         7
                         18 #define FUTEX_TRYLOCK_PI        8
                         19 #define FUTEX_WAIT_BITSET       9 -> explain there ; http://locklessinc.com/articles/futex_cheat_sheet/
                         20 #define FUTEX_WAKE_BITSET       10
                         21 #define FUTEX_WAIT_REQUEUE_PI   11
                         22 #define FUTEX_CMP_REQUEUE_PI    12
                         23
                         24 #define FUTEX_PRIVATE_FLAG      128
                         25 #define FUTEX_CLOCK_REALTIME    256
                         */

                        listExtendedEntry.add(new ExtendedEntry("", futexEvent.getTimestamp().getValue(),
                                event.getTimestamp().getValue(), currentTid, "Futex=" + futexAddr, ExtendedEntry.Type.FUTEX, futexAddr));

                        futexEvent = null;
                    }
                }

                // 7) Analysis for message : mq_send, mq_timedsend
                if(fSetCheckedTypes.contains(ExtendedEntry.Type.MQ_SEND.ordinal())
                        && eventName.equals(MQ_SEND) || eventName.equals(MQ_TIMEDSEND))
                {
                    preemptionOccured = false;
                    if(entry)
                    {
                        mqsendEvent = event;
                    }
                    else if(mqsendEvent != null)
                    {
                        //We need to keep the queue number
                        ITmfEventField eventField = mqsendEvent.getContent();
                        String mqdes = eventField.getField(MQDES).getFormattedValue();
                        listExtendedEntry.add(new ExtendedEntry("", mqsendEvent.getTimestamp().getValue(),
                                event.getTimestamp().getValue(), currentTid, "Queue=" + mqdes,
                                ExtendedEntry.Type.MQ_SEND, mqdes));
                        mqsendEvent = null;
                    }
                }
                else if(fSetCheckedTypes.contains(ExtendedEntry.Type.MQ_RECEIVE.ordinal())
                        && eventName.equals(MQ_RECEIVE) || eventName.equals(MQ_TIMEDRECEIVE))
                {
                    preemptionOccured = false;
                    if(entry)
                    {
                        mqreceiveEvent = event;
                    }
                    else if(mqreceiveEvent != null)
                    {
                        //We need to keep the queue number
                        ITmfEventField eventField = mqreceiveEvent.getContent();
                        String mqdes = eventField.getField(MQDES).getFormattedValue();
                        listExtendedEntry.add(new ExtendedEntry("", mqreceiveEvent.getTimestamp().getValue(),
                                event.getTimestamp().getValue(), currentTid, "Queue=" + mqdes,
                                ExtendedEntry.Type.MQ_RECEIVE, mqdes));
                        mqreceiveEvent = null;
                    }
                }

                // 8) signal
                else if(eventName.equals(""))
                {
                    preemptionOccured = false;
                }

                // 9) waitpid, wait3, wait4, wait, waitid...
                else if(eventName.equals("sys_waitpid") || eventName.equals("sys_wait4"))
                {
                    preemptionOccured = false;
                }

                // 10) block device
                else if(eventName.equals(""))
                {
                    preemptionOccured = false;
                }

                // 11) check for other reasons why it blocks
                else if(!entry)
                {
                    if(preemptionOccured)
                    {
                        // if event match, we add the exec
                        if(fSetCheckedTypes.contains(ExtendedEntry.Type.OTHER.ordinal()) && otherSyscall.getName().equals(eventName))
                        {
                            listExtendedEntry.add(new ExtendedEntry("", otherSyscall.getEvent().getTimestamp().getValue(),
                                    event.getTimestamp().getValue(), currentTid, "Event=" + otherSyscall.getName(),
                                    ExtendedEntry.Type.OTHER, ExtendedEntry.NO_VALUE));
                        }
                        preemptionOccured = false;
                    }
                }
                else //we are on a syscall_entry
                {
                    otherSyscall.setEvent(event);
                    otherSyscall.setName(eventName);
                }
            }

        };

        ((ITmfEventProvider) trace).sendRequest(request);
        try {
            request.waitForCompletion();
        } catch (InterruptedException e) {

        }

        fExtendedList = listExtendedEntry;

        //TODO Debug only
        /*if(listExtendedEntry.size() != 0)
        {
            for(TimeGraphEntry entry : listExtendedEntry)
            {
                ExtendedEntry en = (ExtendedEntry)entry;
                if(en.getType() == ExtendedEntry.Type.FUTEX)
                {
                    executeAnalysisShowMoreInformation(monitor, trace, en);
                    break;
                }
            }
            for(TimeGraphEntry entry : listExtendedEntry)
            {
                ExtendedEntry en = (ExtendedEntry)entry;
                if(en.getType() == ExtendedEntry.Type.HRTIMER)
                {
                    executeAnalysisShowMoreInformation(monitor, trace, en);
                    break;
                }
            }
            for(TimeGraphEntry entry : listExtendedEntry)
            {
                ExtendedEntry en = (ExtendedEntry)entry;
                if(en.getType() == ExtendedEntry.Type.MQ_RECEIVE)
                {
                    executeAnalysisShowMoreInformation(monitor, trace, en);
                    break;
                }
            }
            for(TimeGraphEntry entry : listExtendedEntry)
            {
                ExtendedEntry en = (ExtendedEntry)entry;
                if(en.getType() == ExtendedEntry.Type.MQ_SEND)
                {
                    executeAnalysisShowMoreInformation(monitor, trace, en);
                    break;
                }
            }
        }*/

        /*
        PlatformUI.getWorkbench().getActiveWorkbenchWindow().getActivePage().showView("viewId");
        PlatformUI.getWorkbench()
        .getActiveWorkbenchWindow()
        .getActivePage()
        .activate(workbenchPartToActivate);
        */

        //TODO End Debug only

        // TODO check this version
        /*if(trace == null)
        {
            return;
        }

        CriticalPathModule criticalPathModule = (CriticalPathModule) trace.getAnalysisModule(CPStackbarsParameterProvider.ANALYSIS_ID);

        if (criticalPathModule == null) {
            return;
        }

        if (!criticalPathModule.waitForCompletion(monitor)) {
            System.out.println("This analysis need Critical Path Module.");
            return;
        }
        final TmfGraph graph = criticalPathModule.getCriticalPath();

        if (graph == null) {
            return;
        }

        StackbarsEntry currentExecution = fCurrentExecution;
        if(currentExecution == null)
        {
            return;
        }

        final long executionStartTime = currentExecution.getRealStartTime();
        ITmfTimestamp startTime = new TmfTimestamp(executionStartTime, ITmfTimestamp.NANOSECOND_SCALE);
        final long executionEndTime = executionStartTime + currentExecution.getDuration();
        //ITmfTimestamp executionEndTime = new TmfTimestamp(end);

        final Object worker = criticalPathModule.getParameter(CriticalPathModule.PARAM_WORKER);
        TmfVertex vertex = graph.getVertexAt(startTime, worker);

        System.out.println("Analysis for the thread : " + worker);
        System.out.println("");

        graph.scanLineTraverse(vertex, new TmfGraphVisitor() {

            @Override
            public void visit(TmfEdge link, boolean horizontal) {
                if (horizontal) {

                    Object parent = graph.getParentOf(link.getVertexFrom());

                    long timestamp = link.getVertexFrom().getTs();
                    if(executionEndTime < timestamp || executionStartTime > timestamp)
                    {
                        return;
                    }

                    if(parent == worker)
                    {
                        if(link.getType() == EdgeType.BLOCK_DEVICE)
                        {
                            long ts = link.getVertexFrom().getTs();
                            long duration = link.getDuration();
                            System.out.println("This thread was blocked by device from time : " + Utils.formatTime(ts, TimeFormat.CALENDAR, Resolution.NANOSEC) + " for : " + duration);
                        }
                    }
                }
            }
        });*/

        TmfSignalManager.dispatchSignal(new TmfDataUpdatedSignal(this, ExtendedComparisonView.class));

        System.out.println("");
        System.out.println("----------------------------------");
        System.out.println("End of Blocking Device Analysis");
        System.out.println("----------------------------------");
        System.out.println("");
    }

    public void executeAnalysisShowMoreInformation(final IProgressMonitor monitor, ITmfTrace trace, Vector<String> futexP, Vector<String> queuesP, Vector<String> timersP, long startTimeP, long endTimeP, long nbEventsBackP)
    {
        System.out.println("");
        System.out.println("----------------------------------");
        System.out.println("Execution show more information Analysis");
        System.out.println("----------------------------------");
        System.out.println("");

        if(trace == null)
        {
            return;
        }

        if(monitor.isCanceled())
        {
            return;
        }

        // 1) We want to check the index for the current time and go to the end of the execution

        // 2) We will make the analysis from X events behind
        long nbEventsBack;
        if(nbEventsBackP == ExtendedTimeViewOptionsDialog.BLANK)
        {
            nbEventsBack = 50000L;
        }
        else
        {
            nbEventsBack = nbEventsBackP;
        }

        //Set time
        long start;
        long end;
        if(startTimeP != CPComplementOptionsDialog.BLANK && endTimeP != CPComplementOptionsDialog.BLANK
                && startTimeP < endTimeP)
        {
            start = startTimeP;
            end = endTimeP;

        }
        else
        {
            final StackbarsEntry currentExecution = fCurrentExecution;
            if(currentExecution == null)
            {
                return;
            }
            start = currentExecution.getRealStartTime();
            end = start + currentExecution.getDuration();
        }

        System.out.print(futexP + "" + queuesP + "" + timersP);

        class EntryLastEventTimer{
            public TimeGraphEntry entry;
            public ITmfEvent event;
            public EntryLastEventTimer(TimeGraphEntry entry, ITmfEvent event)
            {
                this.entry = entry;
                this.event = event;
            }
        }

        class EntryWaitingQueue{
            public TimeGraphEntry entry;
            public ITmfEvent event;
            public HashSet<Integer>  waitingReceive;
            public HashSet<Integer> waitingSend;
            public EntryWaitingQueue(ExtendedTimeEntry entry, HashSet<Integer> waitingReceive, HashSet<Integer> waitingSend,ITmfEvent event)
            {
                this.entry = entry;
                this.event = event;
                this.waitingReceive = waitingReceive;
                this.waitingSend = waitingSend;
            }
        }

        class EntryWaitingFutex{
            public TimeGraphEntry entry;
            public ITmfEvent event;
            public HashSet<Integer>  waitingThreads;
            public EntryWaitingFutex(ExtendedTimeEntry entry, HashSet<Integer> waitingThreads,ITmfEvent event)
            {
                this.entry = entry;
                this.event = event;
                this.waitingThreads = waitingThreads;
            }
        }

        HashMap<String,EntryWaitingFutex> futexSetTemp = null;
        HashMap<String,EntryWaitingQueue> queuesSetTemp = null;
        HashMap<String,EntryLastEventTimer> timersSetTemp = null;

        if(futexP != null || queuesP != null || timersP != null)
        {
            if(futexP != null)
            {
                futexSetTemp = new HashMap<>();
                for(String id : futexP)
                {
                    futexSetTemp.put(id, new EntryWaitingFutex(new ExtendedTimeEntry(id, start, end, "", ExtendedTimeEntry.Type.FUTEX),new HashSet<Integer>(),null));
                }
            }
            if(queuesP != null)
            {
                queuesSetTemp = new HashMap<>();
                for(String id : queuesP)
                {
                    queuesSetTemp.put(id,new EntryWaitingQueue(
                            new ExtendedTimeEntry(id, start, end, "", ExtendedTimeEntry.Type.MQ),new HashSet<Integer>(),new HashSet<Integer>(), null));
                }
            }
            if(timersP != null)
            {
                timersSetTemp = new HashMap<>();
                for(String id : timersP)
                {
                    timersSetTemp.put(id,new EntryLastEventTimer(new ExtendedTimeEntry(id, start, end, "", ExtendedTimeEntry.Type.HRTIMER),null));
                }
            }
        }
        else
        {
            final ExtendedEntry entry = fCurrentExtendedEntry;
            if(entry != null)
            {
                if(entry.getType() == ExtendedEntry.Type.HRTIMER)
                {
                    timersSetTemp = new HashMap<>();
                    timersSetTemp.put(entry.getValue(),new EntryLastEventTimer(
                            new ExtendedTimeEntry(entry.getValue(), start, end, "", ExtendedTimeEntry.Type.HRTIMER),null));
                }
                else if(entry.getType() == ExtendedEntry.Type.FUTEX)
                {
                    futexSetTemp = new HashMap<>();
                    futexSetTemp.put(entry.getValue(),new EntryWaitingFutex(
                            new ExtendedTimeEntry(entry.getValue(), start, end, "", ExtendedTimeEntry.Type.FUTEX),new HashSet<Integer>(),null));
                }
                else if(entry.getType() == ExtendedEntry.Type.MQ_RECEIVE ||
                        entry.getType() == ExtendedEntry.Type.MQ_SEND)
                {
                    queuesSetTemp = new HashMap<>();
                    queuesSetTemp.put(entry.getValue(),
                            new EntryWaitingQueue(
                            new ExtendedTimeEntry(entry.getValue(), start, end, "",ExtendedTimeEntry.Type.MQ),
                                    new HashSet<Integer>(),
                                    new HashSet<Integer>(),
                                    null));
                }
                else
                {
                    System.out.println(entry.getType().name + " Not supported yet");
                    return;
                }
            }
            else
            {
                System.out.println("No entry...");
                return;
            }
        }

        final HashMap<String, EntryWaitingFutex> futexSet = futexSetTemp;
        final HashMap<String, EntryWaitingQueue> queuesSet = queuesSetTemp;
        final HashMap<Integer,ITmfEvent> lastEntryEvents = new HashMap<>();
        final HashMap<String, EntryLastEventTimer> timersSet = timersSetTemp;

        long indexStartTime = trace.seekEvent(new TmfTimestamp(start, ITmfTimestamp.NANOSECOND_SCALE)).getRank();
        long indexToSeek = Math.max(0L, indexStartTime - nbEventsBack);
        TmfTimeRange range = new TmfTimeRange(TmfTimestamp.BIG_BANG, new TmfTimestamp(end, ITmfTimestamp.NANOSECOND_SCALE));

        TmfEventRequest request = new TmfEventRequest(ITmfEvent.class, range,
                indexToSeek,
                ITmfEventRequest.ALL_DATA,
                TmfEventRequest.ExecutionType.FOREGROUND) {

        int[]threadByCpu = new int[8]; // TODO Fill that with current tid
        //int currentTid = currentExecution.getTidStart();

        @Override
        public void handleData(ITmfEvent event) {
            // If the job is canceled, cancel the request so waitForCompletion() will unlock
            if (monitor.isCanceled()) {
                cancel();
                return;
            }

            // 2) Check if we are scheduled out and update current thread
            // TODO check if trace as the context tid
            int source = Integer.parseInt(event.getSource());
            if(source >= threadByCpu.length)
            {
                // 2.1) Enlarge if too small
                int[] temp = threadByCpu;
                threadByCpu = new int[source];
                for(int i = 0; i < temp.length; ++i)
                {
                    threadByCpu[i] = temp[i];
                }
            }

            // 2.2) Check the sched_switch event to update array and data
            String eventName = event.getType().getName();
            if (eventName.equals(LttngStrings.SCHED_SWITCH)) {

                // 2.4) Set the current scheduled process on the relevant CPU for the next iteration
                ITmfEventField eventField = event.getContent();
                long nextTid = (Long) eventField.getField(LttngStrings.NEXT_TID).getValue();
                threadByCpu[source] = (int) nextTid;

                return;
            }

            if(timersSet != null)
            {
                if(eventName.equals(LttngStrings.HRTIMER_EXPIRE_ENTRY))
                {
                    ITmfEventField eventField = event.getContent();
                    String hrtimer = eventField.getField(LttngStrings.HRTIMER).getFormattedValue();
                    if(timersSet.containsKey(hrtimer))
                    {
                        EntryLastEventTimer entry_lastEvent = timersSet.get(hrtimer);
                        updateEntryTimer(entry_lastEvent, event);
                        //Add execution
                        //System.out.println(currentTid);
                    }
                    //now
                    //function
                }
                else if(eventName.equals(LttngStrings.HRTIMER_EXPIRE_EXIT))
                {
                    ITmfEventField eventField = event.getContent();
                    String hrtimer = eventField.getField(LttngStrings.HRTIMER).getFormattedValue();
                    if(timersSet.containsKey(hrtimer))
                    {
                        EntryLastEventTimer entry_lastEvent = timersSet.get(hrtimer);
                        updateEntryTimer(entry_lastEvent, event);
                        //Add execution
                        //System.out.println(currentTid);
                    }
                }
                else if(eventName.equals(LttngStrings.HRTIMER_INIT))
                {
                    ITmfEventField eventField = event.getContent();
                    String hrtimer = eventField.getField(LttngStrings.HRTIMER).getFormattedValue();
                    if(timersSet.containsKey(hrtimer))
                    {
                        /*long clockid = (long)eventField.getField(CLOCKID).getValue();
                        System.out.println("clockid"+clockid);

                        long mode = (long)eventField.getField(MODE).getValue();
                        System.out.println("mode"+mode);*/

                        EntryLastEventTimer entry_lastEvent = timersSet.get(hrtimer);
                        updateEntryTimer(entry_lastEvent, event);

                        //Add execution
                        //System.out.println(currentTid);
                    }
                    //clockid

                    /*  CLOCK_REALTIME                  0
                        CLOCK_MONOTONIC                 1
                        CLOCK_PROCESS_CPUTIME_ID        2
                        CLOCK_THREAD_CPUTIME_ID         3
                        CLOCK_MONOTONIC_RAW             4
                        CLOCK_REALTIME_COARSE           5
                        CLOCK_MONOTONIC_COARSE          6
                        CLOCK_BOOTTIME                  7
                        CLOCK_REALTIME_ALARM            8
                        CLOCK_BOOTTIME_ALARM            9
                        CLOCK_SGI_CYCLE                 10      // Hardware specific
                        CLOCK_TAI                       11
                    */

                    //mode
                    /*HRTIMER_MODE_ABS = 0x0,         //Time value is absolute
                      HRTIMER_MODE_REL = 0x1,         //Time value is relative to now
                      HRTIMER_MODE_PINNED = 0x02,     //Timer is bound to CPU
                      HRTIMER_MODE_ABS_PINNED = 0x02,
                      HRTIMER_MODE_REL_PINNED = 0x03,
                    */
                }
                else if(eventName.equals(LttngStrings.HRTIMER_START))
                {
                    ITmfEventField eventField = event.getContent();
                    String hrtimer = eventField.getField(LttngStrings.HRTIMER).getFormattedValue();
                    if(timersSet.containsKey(hrtimer))
                    {
                        /*long expires = (long)eventField.getField(EXPIRES).getValue();
                        System.out.println("expires" + expires);

                        long softexpires = (long)eventField.getField(SOFTEXPIRES).getValue();
                        System.out.println("softexpires" + softexpires);

                        long function = (long)eventField.getField(FUNCTION).getValue();
                        System.out.println("function" + function);*/

                        EntryLastEventTimer entry_lastEvent = timersSet.get(hrtimer);
                        updateEntryTimer(entry_lastEvent, event);

                        //Add execution
                        //System.out.println(currentTid);
                    }
                    //function
                    //expires
                    //softexpires
                }
                else if(eventName.equals(LttngStrings.HRTIMER_CANCEL))
                {
                    ITmfEventField eventField = event.getContent();
                    long hrtimer = (Long) eventField.getField(LttngStrings.HRTIMER).getValue();
                    if(timersSet.containsKey(hrtimer))
                    {
                        //Add execution
                        //System.out.println(currentTid);
                        EntryLastEventTimer entry_lastEvent = timersSet.get(hrtimer);
                        updateEntryTimer(entry_lastEvent, event);
                    }
                }
            }

            else if(futexSet != null)
            {
             // 5) Analysis for futex : from entry to exit
                if(eventName.equals(SYSCALL_ENTRY_FUTEX))
                {
                    //We need to keep the futex address
                    //uaddr
                    ITmfEventField eventField = event.getContent();
                    String futexAddr = eventField.getField(UADDR).getFormattedValue();

                    EntryWaitingFutex entryW = futexSet.get(futexAddr);
                    if(entryW != null)
                    {
                        if(entryW.waitingThreads.size() == 0)
                        {
                            updateEntryFutex(entryW, event, -1);
                        }
                        else
                        {
                            updateEntryFutex(entryW, event, ExtendedTimePresentationProvider.State.FUTEX_WAIT.ordinal());
                        }

                        long op = (long)eventField.getField(OP).getValue();
                        op = op%128; //remove the flags
                        if(op == 0 || op == 9) //FUTEX_WAIT
                        {
                            entryW.waitingThreads.add(threadByCpu[source]);
                        }

                        /*long op = (long)eventField.getField(OP).getValue();
                        System.out.println(op);

                        long val = (long)eventField.getField(VAL).getValue();
                        System.out.println(val);

                        long utime = (long)eventField.getField(UTIME).getValue();
                        System.out.println(utime);

                        long uaddr2 = (long)eventField.getField(UADDR2).getValue();
                        System.out.println(uaddr2);

                        long val3 = (long)eventField.getField(VAL3).getValue();
                        System.out.println(val3);*/
                    }




                    //op


                        //val
                        //utime
                        //uaddr2
                        //val3

                        //Op
                        /* 10 #define FUTEX_WAIT              0
                         11 #define FUTEX_WAKE              1
                         12 #define FUTEX_FD                2 -> remove
                         13 #define FUTEX_REQUEUE           3 -> wake the val et wait sur uaddr2 pour les autres
                         14 #define FUTEX_CMP_REQUEUE       4 -> requeue sans race
                         15 #define FUTEX_WAKE_OP           5 -> was previously used by glibc to fix a bug
                         16 #define FUTEX_LOCK_PI           6 -> priority inheritance
                         17 #define FUTEX_UNLOCK_PI         7
                         18 #define FUTEX_TRYLOCK_PI        8
                         19 #define FUTEX_WAIT_BITSET       9 -> explain there ; http://locklessinc.com/articles/futex_cheat_sheet/
                         20 #define FUTEX_WAKE_BITSET       10
                         21 #define FUTEX_WAIT_REQUEUE_PI   11
                         22 #define FUTEX_CMP_REQUEUE_PI    12
                         23
                         24 #define FUTEX_PRIVATE_FLAG      128
                         25 #define FUTEX_CLOCK_REALTIME    256
                         */
                }
                else if(eventName.equals(SYSCALL_EXIT_FUTEX))
                {
                    //We need to keep the futex address
                    //uaddr
                    ITmfEventField eventField = event.getContent();
                    String futexAddr = eventField.getField(UADDR).getFormattedValue();

                    EntryWaitingFutex entryW = futexSet.get(futexAddr);
                    if(entryW != null)
                    {
                        if(entryW.event == null)
                        {
                            //exit at the start of the trace
                            return;
                        }
                        ITmfEventField pastEventField = entryW.event.getContent();
                        long op = (long)pastEventField.getField(OP).getValue();//TODO error
                        op = op%128; //remove the flags
                        if(op == 0 || op == 9) //FUTEX_WAIT
                        {
                            updateEntryFutex(entryW, event, ExtendedTimePresentationProvider.State.FUTEX_WAIT.ordinal());//WAITING
                            entryW.waitingThreads.remove(threadByCpu[source]);
                        }
                        else if(op == 1 || op == 10)
                        {
                            updateEntryFutex(entryW, event, ExtendedTimePresentationProvider.State.FUTEX_WAKE.ordinal());//WAKE
                        }
                        else if(op == 3 || op == 4)
                        {
                            updateEntryFutex(entryW, event, ExtendedTimePresentationProvider.State.FUTEX_REQUEUE.ordinal());//REQUEUE
                            long ret = (long)eventField.getField(RET).getValue();
                            if(ret == 0)
                            {
                                long uaddr2 = (long)pastEventField.getField(UADDR2).getValue();
                                EntryWaitingFutex entryW2 = futexSet.get(uaddr2);
                                if(entryW2 != null)
                                {
                                    entryW2.waitingThreads.addAll(entryW.waitingThreads);
                                    entryW.waitingThreads.clear();
                                }
                            }
                        }
                        else if(op > 6)
                        {
                            updateEntryFutex(entryW, event, ExtendedTimePresentationProvider.State.FUTEX_PI.ordinal());//REQUEUE
                        }
                    }

                        /*long op = (long)eventField.getField(OP).getValue();
                        System.out.println(op);

                        long val = (long)eventField.getField(VAL).getValue();
                        System.out.println(val);

                        long utime = (long)eventField.getField(UTIME).getValue();
                        System.out.println(utime);

                        long uaddr2 = (long)eventField.getField(UADDR2).getValue();
                        System.out.println(uaddr2);

                        long val3 = (long)eventField.getField(VAL3).getValue();
                        System.out.println(val3);*/


                        /*long ret = (long)eventField.getField(RET).getValue();
                        System.out.println(ret);

                        long uaddr2 = (long)eventField.getField(UADDR2).getValue();
                        System.out.println(uaddr2);*/


                        //ret
                        //uaddr
                        //uaddr2
                }
            }

            else if(queuesSet != null)
            {
             // 7) Analysis for message : mq_send, mq_timedsend
                if(eventName.equals(SYSCALL_ENTRY_MQ_SEND) || eventName.equals(SYSCALL_ENTRY_MQ_TIMEDSEND))
                {
                    //We need to keep the queue number
                    ITmfEventField eventField = event.getContent();
                    String mqdes = eventField.getField(MQDES).getFormattedValue();
                    EntryWaitingQueue entryWQ = queuesSet.get(mqdes);
                    if(entryWQ != null)
                    {
                        lastEntryEvents.put(threadByCpu[source], event);

                        int value;
                        if(entryWQ.waitingReceive.size() != 0)
                        {
                            value = ExtendedTimePresentationProvider.State.RECEIVERS_WAITING.ordinal();
                        }
                        else if(entryWQ.waitingSend.size() != 0)
                        {
                            value = ExtendedTimePresentationProvider.State.SENDERS_WAITING.ordinal();
                        }
                        else
                        {
                            value = -1;
                        }

                        updateEntryMsg(entryWQ, event, value);
                        entryWQ.waitingSend.add(threadByCpu[source]);
                    }

                    //msg_prio
                    //u_abs_timeout
                    //u_msg_ptr
                }
                else if(eventName.equals(SYSCALL_EXIT_MQ_SEND) || eventName.equals(SYSCALL_EXIT_MQ_TIMEDSEND))
                {
                    ITmfEvent pastEvent = lastEntryEvents.remove(threadByCpu[source]);

                    if(pastEvent == null)
                    {
                        return;
                    }

                    ITmfEventField pastEventField = pastEvent.getContent();
                    String mqdes = pastEventField.getField(MQDES).getFormattedValue();
                    EntryWaitingQueue entryWQ = queuesSet.get(mqdes);
                    boolean contains = entryWQ.waitingSend.contains(threadByCpu[source]);

                    //We need to keep the queue number
                    ITmfEventField eventField = event.getContent();
                    long ret = (long)eventField.getField(RET).getValue();

                    //TODO if ret == EAGAIN (11) queue full

                    if(ret == 0)
                    {
                        updateEntryMsg(entryWQ, event, ExtendedTimePresentationProvider.State.SENDERS_WAITING.ordinal());

                    }
                    else {
                        //Change state if we have something new
                        if(ret == -11)//TODO
                        {
                            updateEntryMsg(entryWQ, event, ExtendedTimePresentationProvider.State.QUEUE_FULL_WHILE_SENDERS.ordinal());
                        }
                        else if(contains)
                        {
                            updateEntryMsg(entryWQ, event, ExtendedTimePresentationProvider.State.SENDERS_WAITING.ordinal());
                        }

                    }
                    entryWQ.waitingSend.remove(threadByCpu[source]);

                    //ret
                }
                else if(eventName.equals(SYSCALL_ENTRY_MQ_RECEIVE) || eventName.equals(SYSCALL_ENTRY_MQ_TIMEDRECEIVE))
                {
                  //We need to keep the queue number
                    ITmfEventField eventField = event.getContent();
                    String mqdes = eventField.getField(MQDES).getFormattedValue();
                    EntryWaitingQueue entryWQ = queuesSet.get(mqdes);
                    if(entryWQ != null)
                    {
                        lastEntryEvents.put(threadByCpu[source], event);

                        int value;
                        if(entryWQ.waitingReceive.size() != 0)
                        {
                            value = ExtendedTimePresentationProvider.State.RECEIVERS_WAITING.ordinal();
                        }
                        else if(entryWQ.waitingSend.size() != 0)
                        {
                            value = ExtendedTimePresentationProvider.State.SENDERS_WAITING.ordinal();
                        }
                        else
                        {
                            value = -1;
                        }
                        updateEntryMsg(entryWQ, event, value);

                        entryWQ.waitingReceive.add(threadByCpu[source]);
                    }

                    //msg_len
                    //u_abs_timeout
                }
                else if(eventName.equals(SYSCALL_EXIT_MQ_RECEIVE) || eventName.equals(SYSCALL_EXIT_MQ_TIMEDRECEIVE))
                {
                    ITmfEvent pastEvent = lastEntryEvents.remove(threadByCpu[source]);

                    if(pastEvent == null)
                    {
                        return;
                    }

                    ITmfEventField pastEventField = pastEvent.getContent();
                    String mqdes = pastEventField.getField(MQDES).getFormattedValue();
                    EntryWaitingQueue entryWQ = queuesSet.get(mqdes);
                    boolean contains = entryWQ.waitingReceive.contains(threadByCpu[source]);


                  //We need to keep the queue number
                    ITmfEventField eventField = event.getContent();

                    long ret = (long)eventField.getField(RET).getValue();
                    System.out.println(ret);

                    //TODO if EAGAIN (11) -> empty

                    if(ret == 0)
                    {
                        updateEntryMsg(entryWQ, event, ExtendedTimePresentationProvider.State.RECEIVERS_WAITING.ordinal());
                    }
                    else if(ret == -11)//TODO
                    {
                        updateEntryMsg(entryWQ, event, ExtendedTimePresentationProvider.State.QUEUE_EMPTY_WHILE_RECEIVERS.ordinal());
                    }
                    else if (contains)
                    {
                        updateEntryMsg(entryWQ, event, ExtendedTimePresentationProvider.State.RECEIVERS_WAITING.ordinal());
                    }
                    entryWQ.waitingReceive.remove(threadByCpu[source]);
                    //ret
                    //u_msg_prio
                    //u_msg_ptr
                }
            }
        }

        private void updateEntryFutex(EntryWaitingFutex entryFutex, ITmfEvent currentEvent, int state)
        {
         // If no past event
            if(entryFutex.event == null)
            {
                entryFutex.event = currentEvent;
                return;
            }

            ITmfEvent pastEvent = entryFutex.event;
            if(state != -1)
            {
                long startTime = pastEvent.getTimestamp().getValue();
                long duration = currentEvent.getTimestamp().getValue() - startTime;
                String otherInfo = "Waiting : " + entryFutex.waitingThreads;
                entryFutex.entry.addEvent(new ExtendedTimeEvent(entryFutex.entry,startTime,duration,state,otherInfo));
            }

            entryFutex.event = currentEvent;
        }

        private void updateEntryMsg(EntryWaitingQueue entryQueue, ITmfEvent currentEvent, int state)
        {
         // If no past event
            if(entryQueue.event == null)
            {
                entryQueue.event = currentEvent;
                return;
            }

            ITmfEvent pastEvent = entryQueue.event;
            if(state != -1)
            {
                long startTime = pastEvent.getTimestamp().getValue();
                long duration = currentEvent.getTimestamp().getValue() - startTime;
                String otherInfo = "Waiting receive : " + entryQueue.waitingReceive + " Waiting send : " + entryQueue.waitingSend;
                entryQueue.entry.addEvent(new ExtendedTimeEvent(entryQueue.entry,startTime,duration,state,otherInfo));
            }

            entryQueue.event = currentEvent;
        }

        private void updateEntryTimer(EntryLastEventTimer entry_lastEvent, ITmfEvent currentEvent) {
            // If no past event
            if(entry_lastEvent.event == null)
            {
                entry_lastEvent.event = currentEvent;
                return;
            }

            ITmfEvent pastEvent = entry_lastEvent.event;
            String pastEventName = pastEvent.getType().getName();
            String otherInfo = "";

            int value = -1;
            switch (pastEventName)
            {
                case LttngStrings.HRTIMER_INIT:
                {
                    value = ExtendedTimePresentationProvider.State.TIMER_INIT.ordinal();
                    ITmfEventField eventField = pastEvent.getContent();
                    long clockid = (long)eventField.getField(CLOCKID).getValue();
                    long mode = (long)eventField.getField(MODE).getValue();
                    otherInfo = "clockid : "+clockid +" mode : "+mode;

                    //clockid

                    /*  CLOCK_REALTIME                  0
                        CLOCK_MONOTONIC                 1
                        CLOCK_PROCESS_CPUTIME_ID        2
                        CLOCK_THREAD_CPUTIME_ID         3
                        CLOCK_MONOTONIC_RAW             4
                        CLOCK_REALTIME_COARSE           5
                        CLOCK_MONOTONIC_COARSE          6
                        CLOCK_BOOTTIME                  7
                        CLOCK_REALTIME_ALARM            8
                        CLOCK_BOOTTIME_ALARM            9
                        CLOCK_SGI_CYCLE                 10      // Hardware specific
                        CLOCK_TAI                       11
                    */

                    //mode
                    /*HRTIMER_MODE_ABS = 0x0,         //Time value is absolute
                      HRTIMER_MODE_REL = 0x1,         //Time value is relative to now
                      HRTIMER_MODE_PINNED = 0x02,     //Timer is bound to CPU
                      HRTIMER_MODE_ABS_PINNED = 0x02,
                      HRTIMER_MODE_REL_PINNED = 0x03,
                    */

                    break;
                }
            case LttngStrings.HRTIMER_START:
            {
                ITmfEventField eventField = pastEvent.getContent();
                long expires = (long)eventField.getField(EXPIRES).getValue();
                long softexpires = (long)eventField.getField(SOFTEXPIRES).getValue();
                long function = (long)eventField.getField(FUNCTION).getValue();

                otherInfo = "expires : " + expires + " softexpires : " + softexpires + " function : " + function;
                value = ExtendedTimePresentationProvider.State.TIMER_START.ordinal();
                break;
            }

            case LttngStrings.HRTIMER_EXPIRE_ENTRY:
                    value = ExtendedTimePresentationProvider.State.TIMER_EXPIRED.ordinal();
                break;
            case LttngStrings.HRTIMER_CANCEL:
                    value = ExtendedTimePresentationProvider.State.TIMER_CANCEL.ordinal();
                break;
            default:
                    break;
            }

            if(value != -1)
            {
                long startTime = pastEvent.getTimestamp().getValue();
                long duration = currentEvent.getTimestamp().getValue() - startTime;
                entry_lastEvent.entry.addEvent(new ExtendedTimeEvent(entry_lastEvent.entry,startTime,duration,value,otherInfo));
            }

            entry_lastEvent.event = currentEvent;
        }

        };

        ((ITmfEventProvider) trace).sendRequest(request);
        try {
            request.waitForCompletion();
        } catch (InterruptedException e) {

        }

        if(timersSet != null)
        {
            fExtendedTimeList = new ArrayList<>();
            for(EntryLastEventTimer e_l : timersSet.values())
            {
                fExtendedTimeList.add(e_l.entry);
            }
        }
        if(queuesSet != null)
        {
            fExtendedTimeList = new ArrayList<>();
            for(EntryWaitingQueue e_l : queuesSet.values())
            {
                fExtendedTimeList.add(e_l.entry);
            }
        }
        if(futexSet != null)
        {
            fExtendedTimeList = new ArrayList<>();
            for(EntryWaitingFutex e_l : futexSet.values())
            {
                fExtendedTimeList.add(e_l.entry);
            }
        }

        TmfSignalManager.dispatchSignal(new TmfDataUpdatedSignal(this, ExtendedTimeView.class));

    }

    public void executeAnalysisRunningRelated(final ITmfTrace trace, final int tid, final long startTimeFixed, final long endTimeFixed, final Vector<Integer> cpus)
    {
        Job j = new Job("RT_Analysis_Running"){
            @Override
            protected IStatus run(final IProgressMonitor monitor) {

                if(monitor.isCanceled()){
                    return Status.CANCEL_STATUS;
                }
                executeAnalysisRunning(monitor, trace, tid, startTimeFixed, endTimeFixed, cpus);
                return Status.OK_STATUS;

            }
        };
        j.schedule();

        Job j2 = new Job("RT_Analysis_Related"){
            @Override
            protected IStatus run(final IProgressMonitor monitor) {

                if(monitor.isCanceled()){
                    return Status.CANCEL_STATUS;
                }
                searchDependencies(monitor, trace, startTimeFixed, endTimeFixed);
                return Status.OK_STATUS;

            }
        };
        j2.schedule();

        try {
            j.join();
        } catch (InterruptedException e) {
        }
        try {
            j2.join();
        } catch (InterruptedException e) {
        }

        TmfSignalManager.dispatchSignal(new TmfDataUpdatedSignal(this, CPComplementView.class));
    }

    private void executeAnalysisRunning(final IProgressMonitor monitor, final ITmfTrace trace, int tid, long startTimeFixed, long endTimeFixed, Vector<Integer> cpus)
    {

        System.out.println("");
        System.out.println("-------------------------------------");
        System.out.println("Executing Priority Inversion Analysis");
        System.out.println("-------------------------------------");
        System.out.println("");

        if(trace == null)
        {
            return;
        }

        //1) Set tid
        if(tid != CPComplementOptionsDialog.BLANK)
        {
            //TODO
        }

        StackbarsEntry currentExecution = fCurrentExecution;
        if(currentExecution == null)
        {
            System.out.println("You must select an execution in the stackbars view");
            return;
        }

        int executionId = currentExecution.getRankByStartingTime();

        final StackbarsExecution exec = fHeadExecutions.getChildren().get(executionId);

        CriticalPathModule criticalPathModule = (CriticalPathModule) trace.getAnalysisModule(CPStackbarsParameterProvider.ANALYSIS_ID);
        if (criticalPathModule == null) {
            return;
        }

        final Object worker = criticalPathModule.getParameter(CriticalPathModule.PARAM_WORKER);
        if(!(worker instanceof TmfWorker))
        {
            return;
        }
        TmfWorker tmfWorker = (TmfWorker)worker;

        if(exec.getTidStart() != tmfWorker.getId())
        {
            System.out.println("The tid : " + worker + " is now select "
                    + "in the Control Flow View, you need to select the tid of the stackbars execution ("
                    + exec.getTidStart() + ") and restart the analysis");
            return;
        }

        if (!criticalPathModule.waitForCompletion(monitor)) {
            System.out.println("This analysis need Critical Path Module.");
            return;
        }
        final TmfGraph graph = criticalPathModule.getCriticalPath();

        if (graph == null) {
            return;
        }

        // 2) get the entry info

        //Set time
        long start;
        long end;
        if(startTimeFixed != CPComplementOptionsDialog.BLANK && endTimeFixed != CPComplementOptionsDialog.BLANK
                && startTimeFixed < endTimeFixed)
        {
            start = startTimeFixed;
            end = endTimeFixed;

        }
        else
        {
            start = currentExecution.getRealStartTime();
            end = start + currentExecution.getDuration();
        }

        final long executionStartTime = start;
        final long executionEndTime = end;

        ITmfTimestamp startTimestamp = new TmfTimestamp(start, ITmfTimestamp.NANOSECOND_SCALE);
        TmfVertex vertex = graph.getVertexAt(startTimestamp, worker);

        System.out.println("Analysis for the thread : " + worker);

        // 3) get priority
        /*int priority = Integer.MIN_VALUE;
        for(StateTimePrio stp : exec.getStates())
        {
            if(stp.getPrio() != priority)
            {
                priority = stp.getPrio();
                System.out.println("Priority : " + stp.getPrio() + " from time : " + Utils.formatTime(stp.getTime(), TimeFormat.CALENDAR, Resolution.NANOSEC));
            }
        }*/

        //TODO hashmap
        final Vector<TmfEdge> preemptedLinks = new Vector<>();

        graph.scanLineTraverse(vertex, new TmfGraphVisitor() {

            @Override
            public void visit(TmfEdge link, boolean horizontal) {
                if (horizontal) {

                    Object parent = graph.getParentOf(link.getVertexFrom());

                    if(executionEndTime <= link.getVertexFrom().getTs())
                    {
                        return;
                    }

                    System.out.println(parent);

                    if(executionStartTime >= link.getVertexTo().getTs())
                    {
                        return;
                    }

                    if(link.getType() == EdgeType.PREEMPTED)
                    {
                        preemptedLinks.add(link);
                    }

/*                    if(link.getType() == EdgeType.PREEMPTED)
                    {
                        long linkTs = link.getVertexFrom().getTs();
                        long ts = linkTs;
                        long duration = link.getDuration();

                        if(executionStartTime > linkTs)
                        {
                            duration = duration - (executionStartTime - linkTs);
                            ts = executionStartTime;
                        }

                        if(--duration <= 0)
                        {
                            return;
                        }

                        if(parent != worker)
                        {
                            System.out.println("---");
                            System.out.println("The thread : " + parent + " was preempted when in the critical path of the analysed thread from time : " + Utils.formatTime(ts, TimeFormat.CALENDAR, Resolution.NANOSEC) + " for : " + duration);
                            System.out.println("---");
                            System.out.println("");
                        }
                        else
                        {
                            System.out.println("---");
                            System.out.println("The analysed thread was preempted from time : " + Utils.formatTime(ts, TimeFormat.CALENDAR, Resolution.NANOSEC) + " for : " + duration);
                            System.out.println("---");
                            System.out.println("");
                        }

                        //TODO replace this part to do it only once

                        //Now we need to get the running tasks at this moment
                        ITmfStateSystem ssq = TmfStateSystemAnalysisModule.getStateSystem(trace, LttngKernelAnalysisModule.ID);
                        if (ssq == null) {
                            return;
                        }

                        List<Integer> currentThreadQuarks = ssq.getQuarks(Attributes.CPUS, "*", Attributes.CURRENT_THREAD); //$NON-NLS-1$
                        for (int currentThreadQuark : currentThreadQuarks) {
                            try {
                                List<ITmfStateInterval> currentThreadIntervals = ssq.queryHistoryRange(currentThreadQuark, ts, ts + duration);
                                for (ITmfStateInterval currentThreadInterval : currentThreadIntervals) {
                                    final int currentThread = currentThreadInterval.getStateValue().unboxInt();
                                    if (currentThread <= 0) {
                                        continue;
                                    }

                                    int statusQuark = ssq.getQuarkAbsolute(Attributes.THREADS, Integer.toString(currentThread), Attributes.STATUS);

                                    //time constraint
                                    long currentStartTime = Math.max(ts, currentThreadInterval.getStartTime());
                                    long endTime = Math.min(ts + duration, currentThreadInterval.getEndTime());

                                    long sTime = -1;
                                    long eTime = -1;

                                    long durationInterval = 0;

                                    List<ITmfStateInterval> statusIntervals = ssq.queryHistoryRange(statusQuark, currentStartTime, endTime);
                                    for (ITmfStateInterval statusInterval : statusIntervals) {

                                        int status = -1;
                                        try {
                                            status = statusInterval.getStateValue().unboxInt();
                                        } catch (StateValueTypeException e) {
                                            e.printStackTrace();
                                        }

                                        if(status == StateValues.PROCESS_STATUS_RUN_USERMODE ||
                                                status == StateValues.PROCESS_STATUS_RUN_SYSCALL)
                                        {
                                            long tempStart = Math.max(ts, statusInterval.getStartTime());
                                            if(sTime == -1)
                                            {
                                                sTime = tempStart;
                                            }
                                            eTime = Math.min(ts + duration, statusInterval.getEndTime());
                                            long durationCurrentStatus = eTime - tempStart + 1;
                                            durationInterval += durationCurrentStatus;

                                            //Add to Entry List
                                            List<TimeEventTemp> listTimeEvents = timeEventsByTid.get(currentThread);
                                            if(listTimeEvents == null)
                                            {
                                                listTimeEvents = new ArrayList<>();
                                                timeEventsByTid.put(new Integer(currentThread), listTimeEvents);
                                            }
                                            listTimeEvents.add(new TimeEventTemp(tempStart, durationCurrentStatus, status));
                                        }
                                    }

                                    if(durationInterval != 0)
                                    {
                                        System.out.println("This thread was running when " + parent + " was preempted.");
                                        System.out.println("First time : " + Utils.formatTime(sTime, TimeFormat.CALENDAR, Resolution.NANOSEC));
                                        System.out.println("Thread ID : " + currentThread);
                                        System.out.println("Duration : " + durationInterval);

                                        final long endTimeVerification = eTime;

                                        // We can start from the time until the sched_switch and take the prev_prio //TODO sched_pi_setprio
                                        final Priority threadPriority = new Priority(Integer.MAX_VALUE);
                                        TmfEventRequest request;

                                        request = new TmfEventRequest(ITmfEvent.class,
                                                new TmfTimeRange(new TmfTimestamp(sTime - 1, ITmfTimestamp.NANOSECOND_SCALE),
                                                        TmfTimestamp.BIG_CRUNCH),
                                                0,
                                                ITmfEventRequest.ALL_DATA,
                                                TmfEventRequest.ExecutionType.FOREGROUND) {

                                                @Override
                                                public void handleData(ITmfEvent event) {

                                                    final String eventName = event.getType().getName();
                                                    if (eventName.equals(LttngStrings.SCHED_SWITCH)) {

                                                        ITmfEventField eventField = event.getContent();
                                                        long nextTid = (Long) eventField.getField(LttngStrings.NEXT_TID).getValue();
                                                        if(nextTid == currentThread)
                                                        {
                                                            long prioL = (Long) eventField.getField(StackbarsExecutionsDetection.NEXT_PRIO).getValue();
                                                            if(prioL != threadPriority.getPriority())
                                                            {
                                                                threadPriority.setPriority((int) prioL);
                                                                System.out.println("Priority : " + threadPriority.getPriority() +
                                                                        " (at : " + Utils.formatTime(event.getTimestamp().getValue(), TimeFormat.CALENDAR, Resolution.NANOSEC) + ")");
                                                            }
                                                            checkEnd(event);
                                                        }

                                                        long prevTid = (Long) eventField.getField(LttngStrings.PREV_TID).getValue();
                                                        if(prevTid == currentThread)
                                                        {
                                                            long prioL = (Long) event.getContent().getField(StackbarsExecutionsDetection.PREV_PRIO).getValue();
                                                            if(prioL != threadPriority.getPriority())
                                                            {
                                                                threadPriority.setPriority((int) prioL);
                                                                System.out.println("Priority : " + threadPriority.getPriority() +
                                                                        " (until : " + Utils.formatTime(event.getTimestamp().getValue(), TimeFormat.CALENDAR, Resolution.NANOSEC) + ")");
                                                            }
                                                            checkEnd(event);
                                                        }
                                                    }
                                                    else if (eventName.equals(SCHED_PI_SETPRIO))
                                                    {
                                                        long tid = (Long) event.getContent().getField(LttngStrings.TID).getValue();
                                                        if(tid == currentThread)
                                                        {
                                                            long prioL = (Long) event.getContent().getField(NEWPRIO).getValue();
                                                            threadPriority.setPriority((int) prioL);
                                                            long oldPrioL = (Long) event.getContent().getField(OLDPRIO).getValue();
                                                            System.out.println("Priority : " + threadPriority.getPriority() + " old priority : " + oldPrioL +
                                                                    " (at : " + Utils.formatTime(event.getTimestamp().getValue(), TimeFormat.CALENDAR, Resolution.NANOSEC) + ")");
                                                            checkEnd(event);
                                                        }
                                                    }
                                                }

                                                private void checkEnd(ITmfEvent event)
                                                {
                                                    if(event.getTimestamp().getValue() >= endTimeVerification)
                                                    {
                                                        done();
                                                    }
                                                }

                                            };

                                            ((ITmfEventProvider) trace).sendRequest(request);
                                            try {
                                                request.waitForCompletion();
                                            } catch (InterruptedException e) {
                                                Activator.getDefault().logError("Wait for completion interrupted for analysis priority ", e); //$NON-NLS-1$
                                            }

                                        if(threadPriority.getPriority() == Integer.MAX_VALUE)
                                        {
                                            request = new TmfEventRequest(ITmfEvent.class,
                                                    new TmfTimeRange(new TmfTimestamp(realEndTime - 1, ITmfTimestamp.NANOSECOND_SCALE),
                                                            new TmfTimestamp(realEndTime + 1, ITmfTimestamp.NANOSECOND_SCALE)),
                                                    0,
                                                    ITmfEventRequest.ALL_DATA,
                                                    TmfEventRequest.ExecutionType.FOREGROUND) {

                                                    @Override
                                                    public void handleData(ITmfEvent event) {
                                                        final String eventName = event.getType().getName();
                                                        if (eventName.equals(LttngStrings.SCHED_SWITCH)) {

                                                            //set prio
                                                            long prioL = (Long) event.getContent().getField(StackbarsExecutionsDetection.PREV_PRIO).getValue();
                                                            threadPriority.setPriority((int) prioL);

                                                        }
                                                    }
                                                };

                                                ((ITmfEventProvider) trace).sendRequest(request);
                                                try {
                                                    request.waitForCompletion();
                                                } catch (InterruptedException e) {
                                                    Activator.getDefault().logError("Wait for completion interrupted for analysis priority ", e); //$NON-NLS-1$
                                                }
                                        }
                                        // if we reach the end, go backward until sched_switch next_prio
                                        if(threadPriority.getPriority() == Integer.MAX_VALUE)
                                        {
                                            request = new TmfEventRequest(ITmfEvent.class,
                                                    new TmfTimeRange(new TmfTimestamp(sTime - 1, ITmfTimestamp.NANOSECOND_SCALE),
                                                            new TmfTimestamp(sTime + 1, ITmfTimestamp.NANOSECOND_SCALE)),
                                                    0,
                                                    ITmfEventRequest.ALL_DATA,
                                                    TmfEventRequest.ExecutionType.FOREGROUND) {

                                                    @Override
                                                    public void handleData(ITmfEvent event) {
                                                        final String eventName = event.getType().getName();
                                                        if (eventName.equals(LttngStrings.SCHED_SWITCH)) {

                                                            //set prio
                                                            long prioL = (Long) event.getContent().getField(StackbarsExecutionsDetection.NEXT_PRIO).getValue();
                                                            threadPriority.setPriority((int) prioL);

                                                        }
                                                    }
                                                };

                                                ((ITmfEventProvider) trace).sendRequest(request);
                                                try {
                                                    request.waitForCompletion();
                                                } catch (InterruptedException e) {
                                                    Activator.getDefault().logError("Wait for completion interrupted for analysis priority ", e); //$NON-NLS-1$
                                                }
                                        }
                                            if(threadPriority.getPriority() == Integer.MAX_VALUE)
                                            {
                                                System.out.println("Priority : UNKNOWN");
                                            }

                                            //Update priority in entry list
                                            List<TimeEventTemp> listTimeEvents = timeEventsByTid.get(currentThread);
                                            if(listTimeEvents != null)
                                            {
                                                for(TimeEventTemp temp : listTimeEvents)
                                                {
                                                    if(temp.getPriority() == Integer.MIN_VALUE) {
                                                        temp.setPriority(threadPriority.getPriority());
                                                    }
                                                }
                                            }
                                    }

                                }
                            } catch (AttributeNotFoundException e) {
                                // TODO Auto-generated catch block
                                e.printStackTrace();
                            } catch (StateSystemDisposedException e) {
                                // TODO Auto-generated catch block
                                e.printStackTrace();
                                return;
                            }
                        }
                    }*/
                }
            }
        });


        // 2) fill the event list from first preempted link start to last preempted link end

        if(preemptedLinks.size() == 0)
        {
            return;
        }

        //Now we need to get the running tasks at this moment
        ITmfStateSystem ssq = TmfStateSystemAnalysisModule.getStateSystem(trace, LttngKernelAnalysisModule.ID);
        if (ssq == null) {
            return;
        }

        TreeMap<Long, Integer> mapPrioCurrentExec = fExecutionsDetection.getPrioMap().get(new Integer(exec.getTidStart()));
        if(mapPrioCurrentExec == null)
        {
            mapPrioCurrentExec = new TreeMap<>();
            mapPrioCurrentExec.put(0L, Integer.MAX_VALUE);
        }

        final HashMap<Integer, CPCompRunningEntry> entriesByTid = new HashMap<>();
        for(TmfEdge edge : preemptedLinks)
        {
            //Get time
            long edgeStartTime = edge.getVertexFrom().getTs();
            long edgeEndTime = edge.getVertexTo().getTs() - 1;
            int tidBlocked = (int) ((TmfWorker)graph.getParentOf(edge.getVertexFrom())).getId();
            TreeMap<Long, Integer> mapPrioBlocked = fExecutionsDetection.getPrioMap().get(new Integer(tidBlocked));
            int blockedPrio = Integer.MAX_VALUE;
            if(mapPrioBlocked != null)
            {
                Entry<Long,Integer> timePrio = mapPrioBlocked.floorEntry(edgeStartTime);
                if(timePrio != null)
                {
                    blockedPrio = timePrio.getValue();
                }
            }

            List<Integer> currentCpuQuarks = ssq.getQuarks(Attributes.CPUS, "*"); //$NON-NLS-1$
            for (int currentCpuQuark : currentCpuQuarks) {

                String cpu = ssq.getAttributeName(currentCpuQuark);

                if(cpus != null)
                {
                    try{
                        Integer cpuInt = Integer.parseInt(cpu);
                        if(!cpus.contains(cpuInt))
                        {
                            continue;
                        }
                    }
                    catch(Exception e){}
                }

                int runningThreadQuark = 0;
                try {
                    runningThreadQuark = ssq.getQuarkRelative(currentCpuQuark, Attributes.CURRENT_THREAD);
                } catch (AttributeNotFoundException e1) {
                    // TODO Auto-generated catch block
                    e1.printStackTrace();
                } //$NON-NLS-1$
                try {

                    //Get priority current
                    int currentExecPriority = Integer.MAX_VALUE;
                    Entry<Long,Integer> timePrio = mapPrioCurrentExec.floorEntry(edgeStartTime);
                    if(timePrio != null)
                    {
                        currentExecPriority = timePrio.getValue();
                    }

                    List<ITmfStateInterval> runningThreadIntervals = ssq.queryHistoryRange(runningThreadQuark,edgeStartTime , edgeEndTime);
                    for (ITmfStateInterval runningThreadInterval : runningThreadIntervals) {

                        //Get tid of the running thread
                        final int runningThreadId = runningThreadInterval.getStateValue().unboxInt();
                        if (runningThreadId <= 0) {
                            continue;
                        }

                        //Get corresponding entry
                        CPCompRunningEntry entry = entriesByTid.get(runningThreadId);
                        long eventStartTime = Math.max(runningThreadInterval.getStartTime(), edgeStartTime);
                        long eventEndTime = Math.min(runningThreadInterval.getEndTime(), edgeEndTime);
                        if(entry == null)
                        {
                            entry = new CPCompRunningEntry(runningThreadId,Integer.toString(runningThreadId),
                                    eventStartTime,eventEndTime);
                            entriesByTid.put(runningThreadId, entry);
                        }
                        else
                        {
                            entry.updateEndTime(eventEndTime);
                        }

                        // Get the priority list of the running thread
                        TreeMap<Long, Integer> mapPrioRunningThread = fExecutionsDetection.getPrioMap().get(runningThreadId);
                        Entry<Long,Integer> timePrioRunningStart = mapPrioRunningThread.floorEntry(eventStartTime);

                        // If the thread doesn't have a priority, we have no information
                        if(timePrioRunningStart != null)
                        {
                            long previousStart = eventStartTime;
                            int previousPrio = timePrioRunningStart.getValue();
                            Entry<Long,Integer> timePrioRunningEnd = mapPrioRunningThread.ceilingEntry(eventStartTime);
                            while(timePrioRunningEnd != null)
                            {
                                if(timePrioRunningEnd.getKey() < eventEndTime)
                                {
                                    //Write the previous time  event
                                    if(currentExecPriority < previousPrio)
                                    {
                                        entry.addEvent(new CPComplementTimeEvent(entry,
                                                previousStart, timePrioRunningEnd.getKey() - 1 - previousStart, State.RUNNING_PI.ordinal(), previousPrio, cpu, currentExecPriority, blockedPrio));
                                    }
                                    else
                                    {
                                        entry.addEvent(new CPComplementTimeEvent(entry,
                                                previousStart, timePrioRunningEnd.getKey() - 1 - previousStart, State.RUNNING.ordinal(), previousPrio, cpu, currentExecPriority, blockedPrio));
                                    }
                                    previousStart = timePrioRunningEnd.getKey();
                                    previousPrio = timePrioRunningEnd.getValue();
                                }
                                timePrioRunningEnd = mapPrioRunningThread.ceilingEntry(timePrioRunningEnd.getKey() + 1);
                            }

                            //Write last time event
                            if(currentExecPriority < previousPrio)
                            {
                                entry.addEvent(new CPComplementTimeEvent(entry,
                                        previousStart, eventEndTime - previousStart, State.RUNNING_PI.ordinal(), previousPrio, cpu, currentExecPriority, blockedPrio));
                            }
                            else
                            {
                                entry.addEvent(new CPComplementTimeEvent(entry,
                                        previousStart, eventEndTime - previousStart, State.RUNNING.ordinal(), previousPrio, cpu, currentExecPriority, blockedPrio));
                            }
                        }
                        else
                        {
                            if(currentExecPriority != Integer.MAX_VALUE)
                            {
                                entry.addEvent(new CPComplementTimeEvent(entry,
                                        eventStartTime, eventEndTime - eventStartTime, State.RUNNING_PI.ordinal(), Integer.MAX_VALUE, cpu, currentExecPriority, blockedPrio));
                            }
                            else
                            {
                                entry.addEvent(new CPComplementTimeEvent(entry,
                                        eventStartTime, eventEndTime - eventStartTime, State.RUNNING.ordinal(), Integer.MAX_VALUE, cpu, currentExecPriority, blockedPrio));
                            }
                        }
                    }
                }
                catch (Exception e) {
                    //TODO
                }
            }
        }

        // Add entries
        fPreemptedList = new ArrayList<>();
        for (CPCompRunningEntry entry : entriesByTid.values()) {

            int highestPriority = Integer.MAX_VALUE;
            long totalDuration = 0L;
            java.util.Iterator<ITimeEvent> it = entry.getTimeEventsIterator();
            while(it.hasNext())
            {
                ITimeEvent tempEntry = it.next();
                CPComplementTimeEvent temp = (CPComplementTimeEvent)tempEntry;
                totalDuration += temp.getDuration();
                if(temp.getPriority() < highestPriority)
                {
                    highestPriority = temp.getPriority();
                }
            }

            entry.setDuration(totalDuration);
            entry.setPriority(highestPriority);
            fPreemptedList.add(entry);
        }

        // 2.1) First request to get the currently running threads

        /*

        //Now we need to get the running tasks at this moment
        ITmfStateSystem ssq = TmfStateSystemAnalysisModule.getStateSystem(trace, LttngKernelAnalysisModule.ID);
        if (ssq == null) {
            return;
        }

        final HashMap<Integer, TempStruct> timeEventsByTid = new HashMap<>();
        List<Integer> currentThreadQuarks = ssq.getQuarks(Attributes.CPUS, "*", Attributes.CURRENT_THREAD); //$NON-NLS-1$
        for (int currentThreadQuark : currentThreadQuarks) {
            try {
                long time = preemptedLinks.get(0).getVertexFrom().getTs();
                ITmfStateInterval currentThreadInterval = ssq.querySingleState(time, currentThreadQuark);
                int currentThread = currentThreadInterval.getStateValue().unboxInt();
                TempStruct tempStruct = new TempStruct();
                tempStruct.entry = new CPComplementEntry(Integer.toString(currentThread), time, time);
                tempStruct.lastTimeEventTemp = new TimeEventTemp(time, Integer.MAX_VALUE);
                timeEventsByTid.put(new Integer(currentThread), tempStruct);
            }
            catch (Exception e) {
                //TODO
            }
        }

        // 2.2) Then iterate the events to get the priority

        // We can start from the time until the sched_switch and take the prev_prio
        TmfEventRequest request = new TmfEventRequest(ITmfEvent.class,
                new TmfTimeRange(new TmfTimestamp(preemptedLinks.get(0).getVertexFrom().getTs(), ITmfTimestamp.NANOSECOND_SCALE),
                        TmfTimestamp.BIG_CRUNCH),
                0,
                ITmfEventRequest.ALL_DATA,
                TmfEventRequest.ExecutionType.FOREGROUND) {

            int fLinkIndex = 0;
            TmfEdge fCurrentEdge = preemptedLinks.get(0);
            boolean writeDone = false;

                @Override
                public void handleData(ITmfEvent event) {

                    long time = event.getTimestamp().getValue();
                    if(fCurrentEdge.getVertexTo().getTs() < time)
                    {
                        //Write events
                        for (Entry<Integer, TempStruct> entry : timeEventsByTid.entrySet()) {
                            TempStruct struct = entry.getValue();
                            if(struct.lastTimeEventTemp != null)
                            {
                                TimeEvent tEvent = new CPComplementTimeEvent(struct.entry, struct.lastTimeEventTemp.fTime,
                                        fCurrentEdge.getVertexTo().getTs() - struct.lastTimeEventTemp.getTime(), State.RUNNING.ordinal(), struct.lastTimeEventTemp.getPriority());
                                struct.entry.addEvent(tEvent);
                                struct.entry.updateEndTime(fCurrentEdge.getVertexTo().getTs());
                            }
                        }

                        ++fLinkIndex;
                        if(preemptedLinks.size() == fLinkIndex)
                        {
                            done();
                        }
                        else
                        {
                            fCurrentEdge = preemptedLinks.elementAt(fLinkIndex);
                            writeDone = true;
                        }
                    }

                    if(writeDone && fCurrentEdge.getVertexFrom().getTs() <= time)
                    {
                        writeDone = false;
                        // Update start time
                        for (Entry<Integer, TempStruct> entry : timeEventsByTid.entrySet()) {
                            TempStruct struct = entry.getValue();
                            if(struct.lastTimeEventTemp != null)
                            {
                                struct.lastTimeEventTemp.setTime(fCurrentEdge.getVertexFrom().getTs());
                            }
                        }
                    }

                    final String eventName = event.getType().getName();
                    if (eventName.equals(LttngStrings.SCHED_SWITCH)) {

                        // Next
                        ITmfEventField eventField = event.getContent();
                        long nextTid = (Long) eventField.getField(LttngStrings.NEXT_TID).getValue();
                        long nextPrioL = (Long) eventField.getField(StackbarsExecutionsDetection.NEXT_PRIO).getValue();

                        TempStruct tempStruct = timeEventsByTid.get(new Integer((int)nextTid));
                        if(tempStruct == null)
                        {
                            tempStruct = new TempStruct();
                            tempStruct.entry = new CPComplementEntry(Long.toString(nextTid), time, time);
                            tempStruct.lastTimeEventTemp = new TimeEventTemp(time, (int) nextPrioL);
                            timeEventsByTid.put(new Integer((int) nextTid), tempStruct);
                        }
                        else
                        {
                            tempStruct.lastTimeEventTemp = new TimeEventTemp(time, (int) nextPrioL);
                        }

                        // Previous
                        long prevTid = (Long) eventField.getField(LttngStrings.PREV_TID).getValue();
                        long prevPrioL = (Long) event.getContent().getField(StackbarsExecutionsDetection.PREV_PRIO).getValue();
                        tempStruct = timeEventsByTid.get(new Integer((int) prevTid));
                        if(tempStruct == null)
                        {
                            // TODO error
                        }
                        else
                        {
                            if(!writeDone)
                            {
                                TimeEvent tEvent = new CPComplementTimeEvent(tempStruct.entry, tempStruct.lastTimeEventTemp.fTime,
                                        time - tempStruct.lastTimeEventTemp.getTime(), State.RUNNING.ordinal(), (int)prevPrioL);
                                tempStruct.entry.addEvent(tEvent);
                                tempStruct.entry.updateEndTime(time);
                            }
                            tempStruct.lastTimeEventTemp = null;
                        }
                    }
                    else if (eventName.equals(SCHED_PI_SETPRIO))
                    {
                        long tid = (Long) event.getContent().getField(LttngStrings.TID).getValue();
                        long prioL = (Long) event.getContent().getField(NEWPRIO).getValue();
                        long oldPrioL = (Long) event.getContent().getField(OLDPRIO).getValue();
                        TempStruct tempStruct = timeEventsByTid.get(new Integer((int) tid));
                        if(tempStruct == null)
                        {
                            // TODO error
                        }
                        else
                        {
                            if(!writeDone)
                            {
                                TimeEvent tEvent = new CPComplementTimeEvent(tempStruct.entry, tempStruct.lastTimeEventTemp.fTime,
                                        time - tempStruct.lastTimeEventTemp.getTime(), State.RUNNING.ordinal(), (int)oldPrioL);
                                tempStruct.entry.addEvent(tEvent);
                                tempStruct.entry.updateEndTime(time);
                            }
                            tempStruct.lastTimeEventTemp = new TimeEventTemp(time, (int) prioL);
                        }
                    }
                }
            };

            trace.sendRequest(request);
            try {
                request.waitForCompletion();
            } catch (InterruptedException e) {
                Activator.getDefault().logError("Wait for completion interrupted for analysis priority ", e); //$NON-NLS-1$
            }

        //Fill entrylist and send signal
        if(timeEventsByTid.size() != 0)
        {
            TreeMap<Long, Integer> mapPrio = fExecutionsDetection.getPrioMap().get(new Integer(exec.getTid()));
            if(mapPrio == null)
            {
                mapPrio = new TreeMap<>();
                mapPrio.put(0L, Integer.MAX_VALUE);
            }

            fPreemptedList = new ArrayList<>();
            for (Entry<Integer, TempStruct> entry : timeEventsByTid.entrySet()) {
                TempStruct struct = entry.getValue();

                int highestPriority = Integer.MAX_VALUE;
                long totalDuration = 0L;
                java.util.Iterator<ITimeEvent> it = struct.entry.getTimeEventsIterator();
                while(it.hasNext())
                {
                    ITimeEvent tempEntry = it.next();
                    CPComplementTimeEvent temp = (CPComplementTimeEvent)tempEntry;
                    if(mapPrio.floorEntry(temp.getTime()).getValue() < temp.getPriority())
                    {
                        temp.setValue(State.RUNNING_PI.ordinal());
                    }
                    totalDuration += temp.getDuration();
                    if(temp.getPriority() < highestPriority)
                    {
                        highestPriority = temp.getPriority();
                    }
                }

                struct.entry.setDuration(totalDuration);
                struct.entry.setPriority(highestPriority);
                if(highestPriority != Integer.MAX_VALUE)
                {
                    fPreemptedList.add(struct.entry);
                }
            }
        }
*/

        System.out.println("");
        System.out.println("----------------------------------");
        System.out.println("End of Priority Inversion Analysis");
        System.out.println("----------------------------------");
        System.out.println("");
    }

    /*private class TempStruct
    {
        public TimeEventTemp lastTimeEventTemp;
        public CPComplementEntry entry;
    }

    private class TimeEventTemp
    {
        private long fTime;
        private int fPriority;

        TimeEventTemp(long time, int priority)
        {
            fTime = time;
            fPriority = priority;
        }

        public void setTime(long time)
        {
            fTime = time;
        }
        public long getTime()
        {
            return fTime;
        }
        public int getPriority()
        {
            return fPriority;
        }
    }*/

    public void setCurrentDepth(int currentDepth) {
        fCurrentDepth = currentDepth;
    }

    public List<TimeGraphEntry> getHeadExecutions(ITmfTrace trace, IProgressMonitor monitor) {
        if(fHeadExecutions == null)
        {
            return null;
        }
        List<TimeGraphEntry> list = fHeadExecutions.getEntries();
        if(list == null)
        {
            //make the list
            list = makeTheList(trace, fHeadExecutions, monitor);
            fHeadExecutions.setEntries(list);
        }
        return list;
    }

    private List<TimeGraphEntry> makeTheList(ITmfTrace trace, StackbarsExecution headExecutions, IProgressMonitor monitor) {

        // 1) Initialization
        // 1.1) Get the list of each tid and the max and min time
        // 1.2) Create the TimeGraphEntries

        if(headExecutions == null)
        {
            return null;
        }

        final Map<Integer, List<StackbarsEntry>> mapListByTid = new HashMap<>();
        Vector<StackbarsExecution> vec = headExecutions.getChildren();

        if(vec == null || vec.size() == 0)
        {
            return null;
        }

        List<TimeGraphEntry> listFinal = new ArrayList<>();

        final long startTime = vec.get(0).getStartTime();
        long endTimeTemp = startTime;
        for(int rank = 0; rank < vec.size(); ++rank)
        {
            StackbarsExecution exec = vec.get(rank);
            //Create parent entry
            TimeGraphEntry entry = new StackbarsEntry(exec.getNameStart(), trace, exec.getStartTime(),
                    exec.getEndTime(), exec.getTidStart(), exec.getTidEnd(), rank);
            listFinal.add(entry);

            // if only one tid, add it
            if(exec.getTidStart() == exec.getTidEnd())
            {
                List<StackbarsEntry> listS = mapListByTid.get(exec.getTidStart());
                if(listS == null)
                {
                    listS = new ArrayList<>();
                    mapListByTid.put(exec.getTidStart(), listS);
                }
                listS.add((StackbarsEntry)entry);
                if(endTimeTemp < exec.getEndTime())
                {
                    endTimeTemp = exec.getEndTime();
                }
            }
            else
            {
                // Different tid
                // First one
                TimeGraphEntry entryS = new StackbarsEntry(exec.getNameStart(), trace, exec.getStartTime(),
                        exec.getEndTime(), exec.getTidStart(), -1, -1);
                entry.addChild(entryS);
                List<StackbarsEntry> listS = mapListByTid.get(exec.getTidStart());
                if(listS == null)
                {
                    listS = new ArrayList<>();
                    mapListByTid.put(exec.getTidStart(), listS);
                }
                listS.add((StackbarsEntry)entryS);
                if(endTimeTemp < exec.getEndTime())
                {
                    endTimeTemp = exec.getEndTime();
                }

                //Second one
                TimeGraphEntry entryE = new StackbarsEntry(exec.getNameEnd(), trace, exec.getStartTime(),
                        exec.getEndTime(), -1, exec.getTidEnd(), -1);
                entry.addChild(entryE);
                listS = mapListByTid.get(exec.getTidEnd());
                if(listS == null)
                {
                    listS = new ArrayList<>();
                    mapListByTid.put(exec.getTidEnd(), listS);
                }
                listS.add((StackbarsEntry)entryE);
                if(endTimeTemp < exec.getEndTime())
                {
                    endTimeTemp = exec.getEndTime();
                }
            }

        }
        final long endTime = endTimeTemp;

        // 2) For each tid, get TimeEvent for the duration

        // 2.1) Get the graph

        final Map<Object, List<TmfEdge>> mapEdges = new HashMap<>();

        IAnalysisModule module = trace.getAnalysisModule(LttngKernelExecutionGraph.ANALYSIS_ID);
        if (module instanceof TmfGraphBuilderModule) {
            fGraphModule = (TmfGraphBuilderModule) module;
        }

        if (fGraphModule == null) {
            System.out.println("");
            return null;
        }
        fGraphModule.schedule();

        if (!fGraphModule.waitForCompletion(monitor)) {

            return null;
        }
        final TmfGraph graph = fGraphModule.getGraph();
        if (graph == null) {
            return null;
        }

        // 2.2) Iterate through the graph

        Object objectWorker = null;
        if (fGraphModule.getModelRegistry() == null) {
            return null;
        }

        TmfSystemModel model = fGraphModule.getModelRegistry().getModel(TmfSystemModel.class, true);
        if (model == null) {
            return null;
        }


        for(Integer tidW : mapListByTid.keySet()){

            objectWorker = model.getWorker(trace.getHostId(), 0, tidW);

            if(mapEdges.containsKey(objectWorker))
            {
                continue;
            }

            TmfVertex vertex = graph.getHead(objectWorker);

            /* create all interval entries and horizontal links */
            graph.scanLineTraverse(vertex, new TmfGraphVisitor() {
                @Override
                public void visitHead(TmfVertex node) {
                    /* TODO possible null pointer ? */
                    Object owner = graph.getParentOf(node);

                    if(mapEdges.containsKey(owner))
                    {
                        return;
                    }

                    int tid = 0;
                    if(owner instanceof TmfWorker)
                    {
                        tid = (int) ((TmfWorker)owner).getId();
                        if(mapListByTid.containsKey(tid))
                        {
                            List<TmfEdge> list = new ArrayList<>();
                            mapEdges.put(owner, list);
                        }
                    }
                }

                @Override
                public void visit(TmfEdge link, boolean horizontal) {
                    if (horizontal) {
                        Object owner = graph.getParentOf(link.getVertexFrom());

                        List<TmfEdge> list = mapEdges.get(owner);

                        if(list == null)
                        {
                            return;
                        }

                        long tsTo = link.getVertexTo().getTs();
                        long tsFrom = link.getVertexFrom().getTs();

                        if((tsTo < startTime && tsFrom < startTime) ||
                                (tsTo > endTime && tsFrom > endTime) )
                        {
                            return;
                        }

                        list.add(link);
                    }
                }
            });

        }


        // 3) For each execution, make the TimeGraphEntry with the good TimeEvents

        for (Entry<Object, List<TmfEdge>> entry : mapEdges.entrySet()) {

            Object owner = entry.getKey();
            if(!(owner instanceof TmfWorker))
            {
                continue;
            }
            int tid = (int) ((TmfWorker)owner).getId();

            // 3.1) Sort
            List <StackbarsEntry> listEntries = mapListByTid.get(tid);
            if(listEntries == null || listEntries.size() == 0)
            {
                continue;
            }

            Collections.sort(listEntries);

            int indexEdgeFirst = 0;

            List<TmfEdge> listEdges = entry.getValue();
            if(listEdges == null || listEdges.size() == 0)
            {
                continue;
            }
            for(StackbarsEntry entrySt : listEntries)
            {
                //Initialize statistics
                long preemtpedTotalTime = 0;
                long runningTotalTime = 0;

                // Increase indexEdgeFirst to the current time, the vertex ts should be smaller
                boolean endReach = false;
                while(listEdges.get(indexEdgeFirst).getVertexTo().getTs() <= entrySt.getRealStartTime())
                {
                    ++indexEdgeFirst;
                    if(indexEdgeFirst == listEdges.size())
                    {
                        endReach = true;
                        break;
                    }
                }

                if(endReach)
                {
                    updateEntry(entrySt, preemtpedTotalTime, runningTotalTime); //TODO hmmm
                    break;
                }

                // Check if last
                long endEntryTime = entrySt.getRealStartTime() + entrySt.getDuration();
                long startEventTime = Math.max(listEdges.get(indexEdgeFirst).getVertexFrom().getTs(), entrySt.getRealStartTime());
                if(listEdges.get(indexEdgeFirst).getVertexTo().getTs() >= endEntryTime)
                {
                    // Case begin and end included in first edge
                    StackbarsPresentationProvider.State state = getMatchingStateS(listEdges.get(indexEdgeFirst).getType());
                    long duration = endEntryTime - startEventTime;
                    if(state == StackbarsPresentationProvider.State.RUNNING)
                    {
                        runningTotalTime += duration;
                    }
                    else if (state == StackbarsPresentationProvider.State.PREEMPTED)
                    {
                        preemtpedTotalTime += duration;
                    }

                    entrySt.addEvent(new TimeEvent(entrySt, startEventTime - entrySt.getRealStartTime(),
                            duration, state.ordinal()));
                }
                else
                {
                    //Add the first one

                    StackbarsPresentationProvider.State state = getMatchingStateS(listEdges.get(indexEdgeFirst).getType());
                    long duration = listEdges.get(indexEdgeFirst).getVertexTo().getTs() - startEventTime;
                    if(state == StackbarsPresentationProvider.State.RUNNING)
                    {
                        runningTotalTime += duration;
                    }
                    else if (state == StackbarsPresentationProvider.State.PREEMPTED)
                    {
                        preemtpedTotalTime += duration;
                    }

                    entrySt.addEvent(new TimeEvent(entrySt, startEventTime - entrySt.getRealStartTime(),
                            duration,
                            state.ordinal()));

                    // Iterate and add the states
                    int indexEdgeCurrent = indexEdgeFirst + 1;
                    if(indexEdgeCurrent == listEdges.size())
                    {
                        updateEntry(entrySt, preemtpedTotalTime, runningTotalTime);
                        continue;
                    }

                    while(listEdges.get(indexEdgeCurrent).getVertexTo().getTs() < endEntryTime)
                    {
                        // Add time event

                        state = getMatchingStateS(listEdges.get(indexEdgeCurrent).getType());
                        duration = listEdges.get(indexEdgeCurrent).getVertexTo().getTs() - listEdges.get(indexEdgeCurrent).getVertexFrom().getTs();
                        if(state == StackbarsPresentationProvider.State.RUNNING)
                        {
                            runningTotalTime += duration;
                        }
                        else if (state == StackbarsPresentationProvider.State.PREEMPTED)
                        {
                            preemtpedTotalTime += duration;
                        }

                        entrySt.addEvent(new TimeEvent(entrySt, listEdges.get(indexEdgeCurrent).getVertexFrom().getTs()
                                - entrySt.getRealStartTime(), duration, state.ordinal()));

                        //Iterate
                        ++indexEdgeCurrent;
                        if(indexEdgeCurrent == listEdges.size())
                        {
                            endReach = true;
                            break;
                        }
                    }

                    if(endReach)
                    {
                        updateEntry(entrySt, preemtpedTotalTime, runningTotalTime);
                        continue;
                    }

                    // When end is reach, add the last
                    state = getMatchingStateS(listEdges.get(indexEdgeCurrent).getType());
                    duration = endEntryTime - listEdges.get(indexEdgeCurrent).getVertexFrom().getTs();
                    if(state == StackbarsPresentationProvider.State.RUNNING)
                    {
                        runningTotalTime += duration;
                    }
                    else if (state == StackbarsPresentationProvider.State.PREEMPTED)
                    {
                        preemtpedTotalTime += duration;
                    }
                    entrySt.addEvent(new TimeEvent(entrySt, listEdges.get(indexEdgeCurrent).getVertexFrom().getTs()
                            - entrySt.getRealStartTime(),duration, state.ordinal()));

                }

                updateEntry(entrySt, preemtpedTotalTime, runningTotalTime);
            }
        }

        return listFinal;
    }

    private static void updateEntry(StackbarsEntry entrySt, long preemtpedTotalTime, long runningTotalTime) {
        entrySt.setRunningTime(runningTotalTime);
        entrySt.setPreemptedTime(preemtpedTotalTime);
    }

    public void findExecutions(ITmfTrace trace, int currentTid) {
        fExecutionsDetection.findExecutions(trace, currentTid);
        // In the notify : fHeadExecutions = fExecutionsDetection.getHeadExecutions();
    }


    public boolean executeAddValidExecution(int tid, ITmfTrace trace, TmfTimestamp startTime, TmfTimestamp EndTime)
    {
        StackbarsExecution exec = fExecutionsDetection.addValidExecution(tid, fCurrentDepth, startTime, EndTime, trace);
        if(exec != null /*&& exec.getStates() != null && exec.getStates().size() != 0*/)
        {
            fHeadExecutions.getChildren().add(exec);
            return true;
        }
        return false;
    }

    public void executeSelectValidExecution(int id)
    {
        fExecutionsDetection.selectValidExecution(fCurrentDepth, fHeadExecutions.getChildren().get(id));
    }

    // Methode execution out
    public void executeRemoveExecution(int id)
    {
        StackbarsExecution SE = fHeadExecutions;
        SE.getChildren().remove(id);
        SE.setEntries(null);
    }

    // Methode execution out
    public void getPossibleFilters(int id, ITmfTrace trace, IProgressMonitor monitor)
    {
        Vector<StackbarsFilter> vector = fExecutionsDetection.getPossibleFilters(trace, fCurrentDepth, fHeadExecutions.getChildren().get(id), monitor);
        TmfSignalManager.dispatchSignal(new TmfDataUpdatedSignal(this, vector));
    }

    // bool rebuild
    public boolean executeGoLower(TimeGraphControl control, Action actionLower, Action actionUpper) {
        boolean rebuild = false;
        int maxDepth = fExecutionsDetection.getMaxDepth();
        if(fCurrentDepth < maxDepth)
        {
            if(control instanceof TimeGraphControlStackbars)
            {
                int executionId = getExecutionId() - 1;

                if(executionId != -1 && fHeadExecutions.getChildren().size() > executionId)
                {
                    fCurrentDepth += 1;

                    if(fCurrentDepth == maxDepth)
                    {
                        actionLower.setEnabled(false);
                    }

                    actionUpper.setEnabled(true);

                    fHeadExecutions = fHeadExecutions.getChildren().get(executionId);
                    fDepthPath.push(executionId);
                    rebuild = true;
                }
            }
        }
        return rebuild;
    }

    public boolean executeGoUpper(Action actionLower, Action actionUpper) {
        boolean rebuild = false;
        if(fCurrentDepth > 0)
        {
            fCurrentDepth -= 1;
            if(fCurrentDepth == 0)
            {
                actionUpper.setEnabled(false);
            }
            actionLower.setEnabled(true);
            fDepthPath.pop();
            fHeadExecutions = fExecutionsDetection.getHeadExecutions();
            for(Integer exec : fDepthPath)
            {
                fHeadExecutions = fHeadExecutions.getChildren().get(exec);
            }
            rebuild = true;
            //TODO LineToSelect = fDepthPath.pop() + 1;

        }
        return rebuild;

    }//end executeGoUpper

    public void notifyBuildISFinish(StackbarsExecution headExecutions, ITmfTrace trace, IProgressMonitor monitor) {
        fHeadExecutions = headExecutions;
        if(fHeadExecutions != null)
        {
            List<TimeGraphEntry> list = fHeadExecutions.getEntries();
            if(list == null)
            {
                //make the list
                list = makeTheList(trace, fHeadExecutions, monitor);
                fHeadExecutions.setEntries(list);
            }
        }
        fView.notifyBuildISFinish();
    }

    public void setCurrentExecution(StackbarsEntry selection) {
        fCurrentExecution = selection;
    }

    public void setCurrentExtendedEntry(ExtendedEntry selection) {
        fCurrentExtendedEntry = selection;
    }

    public void updateIcone(Action actionDepthLower) {
        StackbarsEntry currentExecution = fCurrentExecution;
        if(currentExecution == null)
        {
            actionDepthLower.setEnabled(false);
        }
        else if(fExecutionsDetection.getMaxDepth() > fCurrentDepth)
        {
            actionDepthLower.setEnabled(true);
        }
    }

    public void addNewFilters(Vector<StackbarsFilter> filters) {

        fExecutionsDetection.addNewFilters(filters, fCurrentDepth);
    }

    public void addNewFiltersAtDepth(Vector<StackbarsFilter> filters, int depth) {

        fExecutionsDetection.addNewFilters(filters, depth);
    }

    public List<TimeGraphEntry> getAndResetPreemptedList() {

        List<TimeGraphEntry> list = fPreemptedList;
        fPreemptedList = null;
        return list;
    }

    public List<TimeGraphEntry> getAndResetExtendedList() {

        List<TimeGraphEntry> list = fExtendedList;
        fExtendedList = null;
        return list;
    }

    public List<TimeGraphEntry> getAndResetExtendedTimeList() {

        List<TimeGraphEntry> list = fExtendedTimeList;
        fExtendedTimeList = null;
        return list;
    }

    public Collection<TimeGraphEntry> getAndResetDependenciesList() {

        if(fMapStackbarsEntries == null)
        {
            return null;
        }
        Collection<TimeGraphEntry> list = fMapStackbarsEntries.values();
        fMapStackbarsEntries = null;
        return list;
    }

    public Collection<ILinkEvent> getAndResetLinkEventsList() {

        if(fLinkEvents == null)
        {
            return null;
        }
        Collection<ILinkEvent> list = fLinkEvents;
        fLinkEvents = null;
        return list;
    }

    public long getEntryStart() {
        StackbarsEntry currentExecution = fCurrentExecution;
        if(currentExecution == null)
        {
            return 0;
        }
        return currentExecution.getRealStartTime();
    }

    public long getEntryEnd() {
        StackbarsEntry currentExecution = fCurrentExecution;
        if(currentExecution == null)
        {
            return 0;
        }
        return currentExecution.getRealStartTime() + currentExecution.getDuration();
    }

    // Dependencies graph
    private TmfGraphBuilderModule fGraphModule;
    private ITmfTimestamp fCurrentTimeStart;
    private ITmfTimestamp fCurrentTimeEnd;
    public void searchDependencies(IProgressMonitor monitor,ITmfTrace trace, long startTimeFixed, long endTimeFixed)
    {
        StackbarsEntry currentExecution = fCurrentExecution;

        if(trace == null)
        {
            return;
        }

        final long startTime;
        final long endTime;
        if(startTimeFixed != ExtendedComparisonOptionsDialog.BLANK && endTimeFixed != ExtendedComparisonOptionsDialog.BLANK &&
                startTimeFixed < endTimeFixed)
        {
            startTime = startTimeFixed;
            endTime = endTimeFixed;
        }
        else
        {
            if(currentExecution == null)
            {
                System.out.println("You must select an execution in the stackbars view");
                return;
            }

            int executionId = currentExecution.getRankByStartingTime();
            StackbarsExecution head = fHeadExecutions;
            if(head == null)
            {
                return;
            }
            StackbarsExecution exec = head.getChildren().get(executionId);
            startTime = exec.getStartTime();
            endTime = exec.getEndTime();
        }

        if (true/*fGraphModule == null*/) {
            IAnalysisModule module = trace.getAnalysisModule(LttngKernelExecutionGraph.ANALYSIS_ID);
            if (module instanceof TmfGraphBuilderModule) {
                fGraphModule = (TmfGraphBuilderModule) module;
            }
        }

        /*Job job = new Job("Fetching Events") { //$NON-NLS-1$

            @Override
            protected IStatus run(final IProgressMonitor monitor) {*/
                if (fGraphModule == null) {
                    System.out.println("");
                    return;
                }
                fGraphModule.schedule();

                monitor.setTaskName(NLS.bind(Messages.CriticalPathModule_waitingForGraph, fGraphModule.getName()));
                if (!fGraphModule.waitForCompletion(monitor)) {

                    return;
                }
                final TmfGraph graph = fGraphModule.getGraph();
                if (graph == null) {
                    return;
                }

                Object objectWorker = null;
                if (fGraphModule.getModelRegistry() != null) {
                    TmfSystemModel model = fGraphModule.getModelRegistry().getModel(TmfSystemModel.class, true);
                    if (model != null) {
                        objectWorker = model.getWorker(currentExecution.getTrace().getHostId(), 0, currentExecution.getTidStart());
                    }
                    else
                    {
                        objectWorker = currentExecution;
                    }
                }

                final Object worker = objectWorker;

                TmfVertex head = graph.getHead(worker);
                if (head == null) {
                    System.err.println("WARNING: head vertex is null for task " + worker); //$NON-NLS-1$
                    return;
                }
                TmfTimeRange tr = TmfTraceManager.getInstance().getCurrentRange();
                TmfVertex start = graph.getVertexAt(tr.getStartTime(), worker);
                if (start == null) {
                    System.err.println("WARNING: no vertex at time " + tr.getStartTime() + " for task " + worker); //$NON-NLS-1$//$NON-NLS-2$
                    return;
                }

                final List<HashSet<Object>> listNodes = new ArrayList<>();
                final HashSet<Object> mapNodesDirect = new HashSet<>();
                mapNodesDirect.add(worker);

                TmfGraphVisitor visitor = new TmfGraphVisitor() {

                    @Override
                    public void visit(TmfEdge link, boolean horizontal) {
                        if (!horizontal) {

                            long tsTo = link.getVertexTo().getTs();
                            long tsFrom = link.getVertexTo().getTs();

                            if((tsTo < startTime && tsFrom < startTime) ||
                                    (tsTo > endTime && tsFrom > endTime) )
                            {
                                return;
                            }

                            Object parent = graph.getParentOf(link.getVertexFrom());

                            Object otherNode = graph.getParentOf(link.getVertexTo());

                            /*long timestamp = link.getVertexFrom().getTs();
                            if(executionEndTime < timestamp || executionStartTime > timestamp)
                            {
                                return;
                            }*/

                            //TODO just a test, must be done in 2 hashmaps to be ok
                            //Direct

                            if(parent == worker || otherNode == worker)
                            {
                                if(parent != worker)
                                {
                                    mapNodesDirect.add(parent);
                                }
                                else
                                {
                                    mapNodesDirect.add(otherNode);
                                }
                            }

                            //Indirect

                            if(otherNode != worker && parent instanceof TmfWorker)
                            {
                                TmfWorker worker1 = (TmfWorker)parent;
                                if(worker1.getName().startsWith("kworker")|| worker1.getName().startsWith("rcuo"))
                                {
                                    return;
                                }
                            }
                            if(parent != worker && otherNode instanceof TmfWorker)
                            {
                                TmfWorker worker1 = (TmfWorker)otherNode;
                                if(worker1.getName().startsWith("kworker")|| worker1.getName().startsWith("rcuo"))
                                {
                                    return;
                                }
                            }
                            boolean finded = false;
                            for(HashSet<Object> set : listNodes){
                                if(set.contains(parent))
                                {
                                    if (!set.contains(otherNode))
                                    {
                                        for(HashSet<Object> set2 : listNodes){
                                            if(set2 != set)
                                            {
                                                if (set.contains(otherNode))
                                                {
                                                    if(set.size() > set2.size())
                                                    {
                                                        set.addAll(set2);
                                                        listNodes.remove(set2);
                                                    }
                                                    else
                                                    {
                                                        set2.addAll(set);
                                                        listNodes.remove(set);
                                                    }
                                                    finded = true;
                                                    break;
                                                }
                                            }
                                        }
                                        if(!finded)
                                        {
                                            set.add(otherNode);
                                        }
                                    }
                                    finded = true;
                                    break;
                                }
                                else if (set.contains(otherNode))
                                {
                                    if (!set.contains(parent))
                                    {
                                        for(HashSet<Object> set2 : listNodes){
                                            if(set2 != set)
                                            {
                                                if (set.contains(parent))
                                                {
                                                    if(set.size() > set2.size())
                                                    {
                                                        set.addAll(set2);
                                                        listNodes.remove(set2);
                                                    }
                                                    else
                                                    {
                                                        set2.addAll(set);
                                                        listNodes.remove(set);
                                                    }
                                                    finded = true;
                                                    break;
                                                }
                                            }
                                        }
                                        if(!finded)
                                        {
                                            set.add(parent);
                                        }
                                    }
                                    finded = true;
                                    break;
                                }
                            }
                            if(!finded)
                            {
                                HashSet<Object> newSet = new HashSet<>();
                                newSet.add(parent);
                                newSet.add(otherNode);
                                listNodes.add(newSet);
                            }
                        }

                            /*else
                            {
                                mapNodeIndirect.add(parent);
                                mapNodeIndirect.add(otherNode);
                                /*if(mapNodeIndirect.add(parent))
                                {
                                    System.out.println("Indirect : " + parent);
                                }
                                if(mapNodeIndirect.add(otherNode))
                                {
                                    System.out.println("Indirect : " + otherNode);
                                }

                            }*/

                            /*if(parent == worker)
                            {
                                if(link.getType() == EdgeType.BLOCK_DEVICE)
                                {
                                    long ts = link.getVertexFrom().getTs();
                                    long duration = link.getDuration();
                                    System.out.println("This thread was blocked by device from time : " + Utils.formatTime(ts, TimeFormat.CALENDAR, Resolution.NANOSEC) + " for : " + duration);
                                }
                            }*/

                    }
                };

                graph.scanLineTraverse(start, visitor);

                HashSet<Object> tempIndirect = null;

                for(HashSet<Object> set : listNodes){
                    if(set.contains(worker))
                    {
                        tempIndirect = set;
                        break;
                    }
                }
                if(tempIndirect == null){
                    return;
                }

                final HashSet<Object> mapNodesIndirect = tempIndirect;
                final HashSet<Object> mapNodesDirectF = mapNodesDirect;

                if (monitor.isCanceled()) {
                    return;
                }

                final Map<Object, TimeGraphEntry> mapSBEntries = new HashMap<>();
                final List<ILinkEvent> linkEvents = new ArrayList<>();
                TmfVertex vertex = graph.getVertexAt(tr.getStartTime(), worker);

                /* create all interval entries and horizontal links */
                graph.scanLineTraverse(vertex, new TmfGraphVisitor() {
                    @Override
                    public void visitHead(TmfVertex node) {
                        /* TODO possible null pointer ? */
                        Object owner = graph.getParentOf(node);

                        if(!mapNodesIndirect.contains(owner))
                        {
                            return;
                        }

                        if (mapSBEntries.containsKey(owner)) {
                            return;
                        }

                        int tid = 0;
                        if(owner instanceof TmfWorker)
                        {
                            tid = (int) ((TmfWorker)owner).getId();
                        }

                        CPCompRelatedEntry entry = new CPCompRelatedEntry(tid,owner.toString(),startTime, endTime, mapNodesDirectF.contains(owner));
                        mapSBEntries.put(owner, entry);
                    }

                    @Override
                    public void visit(TmfEdge link, boolean horizontal) {

                        long tsTo = link.getVertexTo().getTs();
                        long tsFrom = link.getVertexTo().getTs();

                        if((tsTo < startTime && tsFrom < startTime) ||
                                (tsTo > endTime && tsFrom > endTime) )
                        {
                            return;
                        }

                        if (horizontal) {
                            Object owner = graph.getParentOf(link.getVertexFrom());

                            if(!mapNodesIndirect.contains(owner))
                            {
                                return;
                            }

                            TimeGraphEntry entry = mapSBEntries.get(owner);
                            TimeEvent ev = new TimeEvent(entry, link.getVertexFrom().getTs(), link.getDuration(),
                                    getMatchingStateC(link.getType()).ordinal());
                            entry.addEvent(ev);
                        }
                        else {
                            Object parentFrom = graph.getParentOf(link.getVertexFrom());
                            Object parentTo = graph.getParentOf(link.getVertexTo());
                            TimeGraphEntry entryFrom = mapSBEntries.get(parentFrom);
                            TimeGraphEntry entryTo = mapSBEntries.get(parentTo);
                            TimeLinkEvent lk = new TimeLinkEvent(entryFrom, entryTo, link.getVertexFrom().getTs(),
                                    link.getVertexTo().getTs() - link.getVertexFrom().getTs(), getMatchingStateC(link.getType()).ordinal());
                            linkEvents.add(lk);
                        }
                    }
                });

                fMapStackbarsEntries = mapSBEntries;
                fLinkEvents = linkEvents;

                /*for(Object o : mapNode)
                {
                    System.out.println("Direct : " + o);
                }*/
                /*for(Object o : mapNodeIndirect)
                {
                    System.out.println("Indirect : " + o);
                }*/

                return;

    }
        /*};

        job.schedule();


    }*/

    private static StackbarsPresentationProvider.State getMatchingStateS(EdgeType type) {
        StackbarsPresentationProvider.State state = StackbarsPresentationProvider.State.UNKNOWN;
        switch (type) {
        case RUNNING:
            state = StackbarsPresentationProvider.State.RUNNING;
            break;
        case PREEMPTED:
            state = StackbarsPresentationProvider.State.PREEMPTED;
            break;
        case TIMER:
            state = StackbarsPresentationProvider.State.TIMER;
            break;
        case BLOCK_DEVICE:
            state = StackbarsPresentationProvider.State.BLOCK_DEVICE;
            break;
        case INTERRUPTED:
            state = StackbarsPresentationProvider.State.INTERRUPTED;
            break;
        case NETWORK:
            state = StackbarsPresentationProvider.State.NETWORK;
            break;
        case USER_INPUT:
            state = StackbarsPresentationProvider.State.USER_INPUT;
            break;
        case EPS:
        case UNKNOWN:
        case DEFAULT:
        case BLOCKED:
            break;
        default:
            break;
        }
        return state;
    }

    private static State getMatchingStateC(EdgeType type) {
        State state = State.UNKNOWN;
        switch (type) {
        case RUNNING:
            state = State.RUNNING;
            break;
        case PREEMPTED:
            state = State.PREEMPTED;
            break;
        case TIMER:
            state = State.TIMER;
            break;
        case BLOCK_DEVICE:
            state = State.BLOCK_DEVICE;
            break;
        case INTERRUPTED:
            state = State.INTERRUPTED;
            break;
        case NETWORK:
            state = State.NETWORK;
            break;
        case USER_INPUT:
            state = State.USER_INPUT;
            break;
        case EPS:
        case UNKNOWN:
        case DEFAULT:
        case BLOCKED:
            break;
        default:
            break;
        }
        return state;
    }

    public int getExecutionId() {
        StackbarsEntry currentExecution = fCurrentExecution;
        if(currentExecution != null)
        {
            return currentExecution.getRankByStartingTime(); //TODO disgusting
        }
        return 0;
    }

    public static Object getLinks() {
        // TODO Auto-generated method stub
        return null;
    }

    public void setStartTime(ITmfTimestamp start)
    {
        fCurrentTimeStart = start;
    }

    public void setEndTime(ITmfTimestamp end)
    {
        fCurrentTimeEnd = end;
    }

    public ITmfTimestamp getStartTime()
    {
        return fCurrentTimeStart;
    }

    public ITmfTimestamp getEndTime()
    {
        return fCurrentTimeEnd;
    }

    public void sendResults(ArrayList<int[]> results, String[] inverseDictionary) {
        fResults = results;
        fInverseDictionary = inverseDictionary;
        TmfSignalManager.dispatchSignal(new TmfDataUpdatedSignal(this, this));
    }

    public ArrayList<int[]> getAndResetResults()
    {
        ArrayList<int[]> temp = fResults;
        fResults = null;
        return temp;
    }

    public String[] getAndResetDictio()
    {
        String[] temp = fInverseDictionary;
        fInverseDictionary = null;
        return temp;
    }

} //end class



////////////////////// SCRAP

/*private void executeAnalysisComparison(String executionIdBase)
{
    System.out.println("");
    System.out.println("-------------------------------------");
    System.out.println("Executing Comparison");
    System.out.println("-------------------------------------");
    System.out.println("");

//    if(fModule == null)
//    {
//        return;
//    }

    final ITmfTrace trace = getTrace();
    if(trace == null)
    {
        return;
    }

    //1) Get time for each execution
    TimeGraphControl control = getTimeGraphViewer().getTimeGraphControl();
    if(!(control instanceof TimeGraphControlStackbars))
    {
        return;
    }

    String executionIdCurrent = ((TimeGraphControlStackbars)control).getSelectedExecution();
    if(executionIdCurrent.equals("-1"))
    {
        return;
    }

    List<TimeGraphEntry> list = getEntryList(trace);
    if(list == null)
    {
        return;
    }

    if(list.isEmpty())
    {
        return;
    }

    StackbarsEntry entryCurrent = getEntry(list, executionIdCurrent);
    if(entryCurrent == null)
    {
        return;
    }

    StackbarsEntry entryBase = getEntry(list, executionIdBase);
    if(entryBase == null)
    {
        return;
    }

    //Now we have the time for the current and the base execution
    // 2) Get the currentThread
    int tid = 2;//TODO; fModule.getTid();
    if(tid <= 1)
    {
        return;
    }

    // 3) Get CPU at that times for the thread
  //Now we need to get the running tasks at this moment

    ITmfStateSystem ssq = TmfStateSystemAnalysisModule.getStateSystem(trace, LttngKernelAnalysisModule.ID);
    if (ssq == null) {
        return;
    }

    final long executionStartTimeCurrent = entryCurrent.getRealStartTime();
    final long executionEndTimeCurrent = executionStartTimeCurrent + entryCurrent.getEndTime() - entryCurrent.getStartTime();
    final long executionStartTimeBase = entryBase.getRealStartTime();
    final long executionEndTimeBase = executionStartTimeBase + entryBase.getEndTime() - entryBase.getStartTime();

    final Vector<StackbarsTimeCPU> execCurrentTime = new Vector<>();
    final Vector<StackbarsTimeCPU> execBaseTime = new Vector<>();

    //Get all the CPU
    List<Integer> currentThreadQuarks = ssq.getQuarks(Attributes.CPUS, "*", Attributes.CURRENT_THREAD); //$NON-NLS-1$
    for (int currentThreadQuark : currentThreadQuarks) {
        //For each CPU, try to get the moment running
        try {
            List<ITmfStateInterval> currentThreadIntervals = ssq.queryHistoryRange(currentThreadQuark, executionStartTimeCurrent, executionEndTimeCurrent);

            // For each interval, check if it is our thread
            for (ITmfStateInterval currentThreadInterval : currentThreadIntervals) {
                int currentThread = currentThreadInterval.getStateValue().unboxInt();
                if (currentThread != tid) {
                    continue;
                }

                long startTime = Math.max(currentThreadInterval.getStartTime(), executionStartTimeCurrent);
                long endTime = Math.min(currentThreadInterval.getEndTime(), executionEndTimeCurrent);

                String s = ssq.getFullAttributePath(currentThreadQuark);
                String[] currentThreadPath = s.split("/");
                execCurrentTime.add(new StackbarsTimeCPU(currentThreadPath[1], startTime, endTime));

            }
        } catch (AttributeNotFoundException e) {
            // TODO Auto-generated catch block
            //e.printStackTrace();
        } catch (StateSystemDisposedException e) {
            // TODO Auto-generated catch block
            //e.printStackTrace();
            return;
        }

        //Again for the base
        try {
            List<ITmfStateInterval> currentThreadIntervals = ssq.queryHistoryRange(currentThreadQuark, executionStartTimeBase, executionEndTimeBase);

            // For each interval, check if it is our thread
            for (ITmfStateInterval currentThreadInterval : currentThreadIntervals) {
                int currentThread = currentThreadInterval.getStateValue().unboxInt();
                if (currentThread != tid) {
                    continue;
                }

                long startTime = Math.max(currentThreadInterval.getStartTime(), executionStartTimeBase);
                long endTime = Math.min(currentThreadInterval.getEndTime(), executionEndTimeBase);

                String s = ssq.getFullAttributePath(currentThreadQuark);
                String[] currentThreadPath = s.split("/");
                execBaseTime.add(new StackbarsTimeCPU(currentThreadPath[1], startTime, endTime));

            }
        } catch (AttributeNotFoundException e) {
            // TODO Auto-generated catch block
            //e.printStackTrace();
        } catch (StateSystemDisposedException e) {
            // TODO Auto-generated catch block
            //e.printStackTrace();
            return;
        }
    }

    Collections.sort(execBaseTime);
    Collections.sort(execCurrentTime);

    final Vector<ITmfEvent> currentExecEvents = new Vector<>();
    final Vector<ITmfEvent> baseExecEvents = new Vector<>();

    // 4) get the events
    Job job = new Job("Fetching Events") { //$NON-NLS-1$

        @Override
        protected IStatus run(final IProgressMonitor monitor) {

            //Current
            TmfEventRequest request;
            for(final StackbarsTimeCPU timeCpu : execCurrentTime)
            {
                request = new TmfEventRequest(ITmfEvent.class,
                        new TmfTimeRange(new TmfTimestamp(timeCpu.fStart, ITmfTimestamp.NANOSECOND_SCALE),
                                new TmfTimestamp(timeCpu.fEnd, ITmfTimestamp.NANOSECOND_SCALE)),
                        0,
                        ITmfEventRequest.ALL_DATA,
                        TmfEventRequest.ExecutionType.FOREGROUND) {
                    @Override
                    public void handleData(ITmfEvent event) {
                        // If the job is canceled, cancel the request so waitForCompletion() will unlock
                        if (monitor.isCanceled()) {
                            cancel();
                            return;
                        }

                        if(event.getSource().equals(timeCpu.fCpu))
                        {
                            currentExecEvents.add(event);
                        }
                    }
                };

                ((ITmfEventProvider) getTrace()).sendRequest(request);
                try {
                    request.waitForCompletion();
                } catch (InterruptedException e) {
                    Activator.getDefault().logError("Wait for completion interrupted for get events ", e); //$NON-NLS-1$
                }
            }

            //Base
            for(final StackbarsTimeCPU timeCpu : execBaseTime)
            {
                request = new TmfEventRequest(ITmfEvent.class,
                        new TmfTimeRange(new TmfTimestamp(timeCpu.fStart, ITmfTimestamp.NANOSECOND_SCALE),
                                new TmfTimestamp(timeCpu.fEnd, ITmfTimestamp.NANOSECOND_SCALE)),
                        0,
                        ITmfEventRequest.ALL_DATA,
                        TmfEventRequest.ExecutionType.FOREGROUND) {
                    @Override
                    public void handleData(ITmfEvent event) {
                        // If the job is canceled, cancel the request so waitForCompletion() will unlock
                        if (monitor.isCanceled()) {
                            cancel();
                            return;
                        }

                        if(event.getSource().equals(timeCpu.fCpu))
                        {
                            baseExecEvents.add(event);
                        }
                    }
                };

                ((ITmfEventProvider) getTrace()).sendRequest(request);
                try {
                    request.waitForCompletion();
                } catch (InterruptedException e) {
                    Activator.getDefault().logError("Wait for completion interrupted for populateCache ", e); //$NON-NLS-1$
                }
            }

            // 5) get the longest common subsequence
            Vector<StackbarsEventsIndexes> lcsVector = lcsWithIndexes(baseExecEvents, currentExecEvents);

            System.out.println("Common");
            for(StackbarsEventsIndexes ev : lcsVector)
            {
                System.out.println("Event : " + "B" + ev.fIndex1 + " " + "C" + ev.fIndex2 + " " + ev.fEvent.getTimestamp() + " " + ev.fEvent.getType());
            }

            System.out.println("");
            System.out.println("Supplementary Events base");
            int indexInLCS = 0;
            int indexInVector = 0;

            Vector<ITmfEvent> supEventsBase = new Vector<>();

            for(ITmfEvent ev : baseExecEvents)
            {
                if(lcsVector.size() > indexInLCS && eventsMatch(ev, lcsVector.get(indexInLCS).fEvent))
                {
                    //System.out.println("Not Event : " + index + " " + lscVector.get(index).getTimestamp() + " " + lscVector.get(index).getContent());
                    ++indexInLCS;
                }
                else
                {
                    supEventsBase.add(ev);
                    System.out.println("Event : B" + indexInVector + " " + ev.getTimestamp() + " " + ev.getType());
                }
                ++indexInVector;
            }
            if(baseExecEvents.size() == lcsVector.size())
            {
                System.out.println("No Events");
            }

            System.out.println("");
            System.out.println("Supplementary Events current");
            indexInLCS = 0;
            indexInVector = 0;

            Vector<ITmfEvent> supEventsCurrent = new Vector<>();

            for(ITmfEvent ev : currentExecEvents)
            {
                if(lcsVector.size() > indexInLCS && eventsMatch(ev, lcsVector.get(indexInLCS).fEvent))
                {
                    //System.out.println("Not Event : " + index + " " + lscVector.get(index).getTimestamp() + " " + lscVector.get(index).getContent());
                    ++indexInLCS;
                }
                else
                {
                    supEventsCurrent.add(ev);
                    System.out.println("Event : C" + indexInVector + " " + ev.getTimestamp() + " " + ev.getType());
                }
                ++indexInVector;
            }
            if(currentExecEvents.size() == lcsVector.size())
            {
                System.out.println("No Events");
            }

            boolean[] foundSupEventsCurrent = new boolean[supEventsCurrent.size()];
            boolean[] foundSupEventsBase = new boolean[supEventsBase.size()];

            for(int iB = 0; iB < supEventsBase.size(); ++iB)
            {
                for(int iC = 0; iC < supEventsCurrent.size(); ++iC)
                {
                    if(!foundSupEventsCurrent[iC])
                    {
                        if(eventsMatch(supEventsBase.get(iB), supEventsCurrent.get(iC)))
                        {
                            foundSupEventsBase[iB] = true;
                            foundSupEventsCurrent[iC] = true;
                            break;
                        }
                    }
                }
            }

            System.out.println("");
            System.out.println("Events not in order but in both execution");
            for(int iB = 0; iB < foundSupEventsBase.length; ++iB)
            {
                if(foundSupEventsBase[iB])
                {
                    System.out.println("Event : Name = " + supEventsBase.get(iB).getType());
                }
            }

            System.out.println("");
            System.out.println("Events only in base");
            for(int iB = 0; iB < foundSupEventsBase.length; ++iB)
            {
                System.out.println("Event : Name = " + supEventsBase.get(iB).getType());
            }

            System.out.println("");
            System.out.println("Events only in current");
            for(int iC = 0; iC < foundSupEventsCurrent.length; ++iC)
            {
                System.out.println("Event : Name = " + supEventsCurrent.get(iC).getType());
            }

            /*System.out.println("");
            System.out.println("All Events base");

            for(ITmfEvent ev : baseExecEvents)
            {
                 System.out.println("Event : " + ev.getTimestamp() + " " + ev.getContent());
            }

            System.out.println("");
            System.out.println("All Events current");
            for(ITmfEvent ev : currentExecEvents)
            {
                 System.out.println("Event : " + ev.getTimestamp() + " " + ev.getContent());
            }

            System.out.println("");
            System.out.println("----------------------------------");
            System.out.println("End of Comparison");
            System.out.println("----------------------------------");
            System.out.println("");

            // Flag the UI thread that the cache is ready
            if (monitor.isCanceled()) {
                return Status.CANCEL_STATUS;
            }
            return Status.OK_STATUS;
        }
    };
    //job.setSystem(true);
    job.setPriority(Job.SHORT);
    job.schedule();

}
*/

    /*
    System.out.println("");
    System.out.println("-------------------------------------");
    System.out.println("Detect executions");
    System.out.println("-------------------------------------");
    System.out.println("");

//    if(fModule == null)
//    {
//        return;
//    }

    final ITmfTrace trace = getTrace();
    if(trace == null)
    {
        return;
    }

    final int currentTid = 2; //TODO fModule.getTid();

    // 1) Get the events for the thread
    Job job = new Job("Fetching Events") { //$NON-NLS-1$

        @Override
        protected IStatus run(final IProgressMonitor monitor) {

            //Current
            final Vector<ITmfEvent> threadEvents = new Vector<>();
            TmfEventRequest request;

                request = new TmfEventRequest(ITmfEvent.class, TmfTimeRange.ETERNITY,
                        0,
                        ITmfEventRequest.ALL_DATA,
                        TmfEventRequest.ExecutionType.FOREGROUND) {

                    @Override
                    public void handleData(ITmfEvent event) {
                        // If the job is canceled, cancel the request so waitForCompletion() will unlock
                        if (monitor.isCanceled()) {
                            cancel();
                            return;
                        }

                        final ITmfEventField eventField = event.getContent();
                        ITmfEventField tidField = eventField.getField(CONTEXT_VTID);

                        if(tidField == null)
                        {
                            System.out.println("You must add tid in context when tracing to run this.");
                            cancel();
                            return;
                        }

                        int tid = ((Long) tidField.getValue()).intValue();
                        if(tid == currentTid)
                        {
                            if(!event.getType().getName().equals("rcu_utilization"))
                            {
                                threadEvents.add(event);
                            }
                            //threadEvents.add(event);
                        }
                        else
                        {
                            final String eventName = event.getType().getName();
                            if (eventName.equals(LttngStrings.SCHED_SWITCH)) {

                                // Set the current scheduled process on the relevant CPU for the next iteration
                                long nextTid = (Long) eventField.getField(LttngStrings.NEXT_TID).getValue();
                                if(nextTid == currentTid)
                                {
                                    threadEvents.add(event); //TODO check if ok : we will add it
                                }
                            }
                        }
                    }
                };

                ((ITmfEventProvider) getTrace()).sendRequest(request);
                try {
                    request.waitForCompletion();
                } catch (InterruptedException e) {
                    Activator.getDefault().logError("Wait for completion interrupted for get events ", e); //$NON-NLS-1$
                }

                // 2) Remove events that we don't care

                // 2.5) Algo to remove event that are not useful for the detection
                //      if A is always followed by B, remove all the B that are following A
                //      if B is always preceded by A, remove all the A that are preceding B
                //      Do this until we are removing less than a threshold (TODO defined) events

                int sizeV = threadEvents.size();
                Vector<ITmfEvent> reduceVector = removeEvents(threadEvents);

                while(sizeV != reduceVector.size())
                {
                    sizeV = reduceVector.size();
                    reduceVector = removeEvents(threadEvents);
                }

                System.out.println(reduceVector.size());

                // 3.0) Do a vector with int and a map event-int
                // It is faster to do a hashmap and in O(n), but we want to be easy to change the matchevent function
                // It will than be in O(n * alphabet)

                Vector<ITmfEvent> alphabet = new Vector<>(); //it will be an exemple of the class for some events
                int[] arrayEvents = new int[reduceVector.size()];

                eventsToInt(alphabet, arrayEvents, reduceVector);

                // 3.1) Test 0 : Split the events in 2 longest sequence non-overlapping (this will probably remove the noise)

                // TODO it is way too long, maybe do it on a max of X events

                Vector<ITmfEvent> results = lrs(threadEvents);

                for(ITmfEvent e : results)
                {
                    System.out.println("Event : " + e.getType());
                }

                // 4) Test 1 : Count each events and pgcd for the majority
                // 5) Test 2 : Split again and compare
                // 6) Test 3 : Check space between each same events
                // 7) Test 4 : Test with a base entered by the user

                System.out.println("");
                System.out.println("----------------------------------");
                System.out.println("End of Detection");
                System.out.println("----------------------------------");
                System.out.println("");

            // Flag the UI thread that the cache is ready
            if (monitor.isCanceled()) {
                return Status.CANCEL_STATUS;
            }
            return Status.OK_STATUS;
        }

    };
    //job.setSystem(true);
    job.setPriority(Job.SHORT);
    job.schedule();*/

//}

/*
private class StackbarsTimeCPU implements Comparable<StackbarsTimeCPU>{
    public final String fCpu;
    public final long fStart;
    public final long fEnd;
    public StackbarsTimeCPU( String cpu, long startTime, long endTime){
      this.fCpu = cpu;
      this.fEnd =endTime;
      this.fStart = startTime;
    }

    @Override
    public int compareTo(StackbarsTimeCPU o) {
        // TODO Auto-generated method stub
        return Long.compare(this.fStart, o.fStart);
    }
  }*/
/*
private class StackbarsEventsIndexes {
    public final ITmfEvent fEvent;
    public final int fIndex1;
    public final int fIndex2;
    public StackbarsEventsIndexes( ITmfEvent event, int index1, int index2){
      this.fEvent = event;
      this.fIndex1 =index1;
      this.fIndex2 = index2;
    }
  }*/
/*
private static boolean eventsMatch(ITmfEvent a, ITmfEvent b)
{
    if(a.getType().equals(b.getType())) {
        return true;
    }
    return false;
}*/
/*
private static void eventsToInt(Vector<ITmfEvent> alphabet, int[] arrayEvents,Vector<ITmfEvent> reduceVector)
{
    for(int indexArray = 0; indexArray < arrayEvents.length; ++indexArray)
    {
        ITmfEvent eventToMatch = reduceVector.get(indexArray);
        boolean eventFind = false;
        for(int indexAlpha = 0; indexAlpha < alphabet.size(); ++indexAlpha)
        {
            if(eventsMatch(alphabet.get(indexAlpha),eventToMatch))
            {
                eventFind = true;
                arrayEvents[indexArray] = indexAlpha;
                break;
            }
        }
        if(!eventFind)
        {
            arrayEvents[indexArray] = alphabet.size();
            alphabet.add(eventToMatch);
        }
    }
}*/
/*
// Longest repeated sequence
private static Vector<ITmfEvent> lrs(Vector<ITmfEvent> vSequence) {

    if(vSequence.size() == 0)
    {
        return null;
    }

    int middle = vSequence.size()/2;
    int maxLength = 0;
    int maxId = -1;

    for(int id = middle; id < vSequence.size(); ++id)
    {
        if(vSequence.size() - id <= maxLength)
        {
            break;
        }
        int tempMaxLength = lcsLength(vSequence, id);
        if(tempMaxLength > maxLength)
        {
            maxLength = tempMaxLength;
            maxId = id;
        }
    }

    for(int id = middle - 1; id > 0; --id)
    {
        if(id <= maxLength)
        {
            break;
        }
        int tempMaxLength = lcsLength(vSequence, id);
        if(tempMaxLength > maxLength)
        {
            maxLength = tempMaxLength;
            maxId = id;
        }
    }

    if(maxId == -1)
    {
        return null;
    }

    return lcsWithLimit(vSequence, maxId);
}
*//*
//Longest common sequence length
private static int lcsLength(Vector<ITmfEvent> v, int limit) {

    //v1.size() -> limit
    //v2.size() -> v.size()-limit
    //v1.get(i) -> v1.get(i)
    //v2.get(j) -> v1.get(j+limit)

    int sizeV1 = limit;
    int sizeV2 = v.size()-limit;


    int[] lengths = new int[sizeV2+1];

    // row 0 and column 0 are initialized to 0 already

    int previous = 0;
    int i = 0;
    int j = 0;
    for (; i < sizeV1; i++) {
        for (j = 0; j < sizeV2; j++) {
            if(j == 0)
            {
                previous = 0;
            }
            int tempPrevious = previous;
            previous = lengths[j+1];
            if (eventsMatch(v.get(i),v.get(j + limit))) {
                lengths[j+1] = tempPrevious + 1;
            } else {
                lengths[j+1] =
                    Math.max(lengths[j], lengths[j+1]);
            }
        }
    }

    return lengths[j];
}
*//*
//Longest common subsequence with one vector as input
private static Vector<ITmfEvent> lcsWithLimit(Vector<ITmfEvent> v, int limit) {

    //v1.size() -> limit
    //v2.size() -> v.size()-limit
    //v1.get(i) -> v1.get(i)
    //v2.get(j) -> v1.get(j+limit)

    int sizeV1 = limit;
    int sizeV2 = v.size()-limit;


    StackbarsEventsIndexes[] lengths = new StackbarsEventsIndexes[sizeV2+1];

    //index1 = length
    //index2 = ancestor

    // row 0 and column 0 are initialized to 0 already

    int previous = 0;
    int i = 0;
    int j = 0;

    int max = 0;

    for (; i < sizeV1; i++) {
        for (j = 0; j < sizeV2; j++) {
            if(j == 0)
            {
                previous = 0;
            }
            int tempPrevious = previous;
            previous = lengths[j+1].fIndex1;
            if (eventsMatch(v.get(i),v.get(j + limit))) {
                lengths[j+1].fIndex1 = tempPrevious + 1;
                lengths[j+1].fEvent = v.get(i);
                if(j+2 <= sizeV2 && lengths[j+2].fIndex1 == lengths[j+1].fIndex1)
                {

                }

                max = Math.max(max, lengths[j+1].fIndex1);

            } else {
                lengths[j+1].fIndex1 =
                    Math.max(lengths[j].fIndex1, lengths[j+1].fIndex1);
            }
        }
    }


    //v1.size() -> limit
    //v2.size() -> v.size()-limit
    //v1.get(i) -> v1.get(i)
    //v2.get(j) -> v1.get(j+limit)

    int sizeV1 = limit;
    int sizeV2 = v.size()-limit;


    int[][] lengths = new int[sizeV1+1][sizeV2+1];

    // row 0 and column 0 are initialized to 0 already

    int i = 0;
    int j = 0;
    for (; i < sizeV1; i++) {
        for (j = 0; j < sizeV2; j++) {
            if (eventsMatch(v.get(i),v.get(j + limit))) {
                lengths[i+1][j+1] = lengths[i][j] + 1;
            } else {
                lengths[i+1][j+1] =
                    Math.max(lengths[i+1][j], lengths[i][j+1]);
            }
        }
    }

 // read the substring out from the matrix
    Vector<ITmfEvent> sb = new Vector<>();
    for (int x = sizeV1, y = sizeV2;
         x != 0 && y != 0; ) {
        if (lengths[x][y] == lengths[x-1][y]) {
            x--;
        } else if (lengths[x][y] == lengths[x][y-1]) {
            y--;
        } else {
            sb.add(v.get(x-1)); //same than v.get(y-1 + limit)
            x--;
            y--;
        }
    }

    Collections.reverse(sb);

    return sb;
}

// Longest common subsequence with the indexes in the 2 vectors
private Vector<StackbarsEventsIndexes> lcsWithIndexes(Vector<ITmfEvent> vBase, Vector<ITmfEvent> vCurrent) {
    int[][] lengths = new int[vBase.size()+1][vCurrent.size()+1];

    // row 0 and column 0 are initialized to 0 already

    for (int i = 0; i < vBase.size(); i++) {
        for (int j = 0; j < vCurrent.size(); j++) {
            if (eventsMatch(vBase.get(i),vCurrent.get(j))) {
                lengths[i+1][j+1] = lengths[i][j] + 1;
            } else {
                lengths[i+1][j+1] =
                    Math.max(lengths[i+1][j], lengths[i][j+1]);
            }
        }
    }

    // read the substring out from the matrix
    Vector<StackbarsEventsIndexes> sb = new Vector<>();
    for (int x = vBase.size(), y = vCurrent.size();
         x != 0 && y != 0; ) {
        if (lengths[x][y] == lengths[x-1][y]) {
            x--;
        } else if (lengths[x][y] == lengths[x][y-1]) {
            y--;
        } else {
            sb.add(new StackbarsEventsIndexes(vBase.get(x-1), x-1, y-1)); //same than vCurrent.get(y-1)
            x--;
            y--;
        }
    }

    Collections.reverse(sb);

    return sb;
}

// 3) Algo to remove event that are not useful for the detection
//      if A is always followed by B, remove all the B that are following A
//      if B is always preceded by A, remove all the A that are preceding B
//      Do this until we are removing less than a threshold (TODO defined) events
private Vector<ITmfEvent> removeEvents(Vector<ITmfEvent> vector) {

    if(vector == null || vector.size() < 5)
    {
        return vector;
    }

    Map<String, DataMap> mapEventsToPrecNext = new HashMap<>();

    // First loop : keep each previous name and next name for each event

    String precEventName = "";
    String currEventName = vector.get(0).getType().getName();
    String nextEventName = vector.get(1).getType().getName();

    DataMap newData;

    int maxIter = vector.size() - 1;

    for(int index = 2; index < maxIter; ++index)
    {
        precEventName = currEventName;
        currEventName = nextEventName;
        nextEventName = vector.get(index).getType().getName();
        DataMap precNextData = mapEventsToPrecNext.get(currEventName);
        if(precNextData == null)
        {
            newData = new DataMap(precEventName, nextEventName);
            mapEventsToPrecNext.put(currEventName, newData);
        }
        else
        {
            String precName = precNextData.prec;
            String nextName = precNextData.next;
            if(precName != null)
            {
                if(!precName.equals(precEventName))
                {
                    precNextData.prec = null;
                }
            }
            if(nextName != null)
            {
                if(!nextName.equals(nextEventName))
                {
                    precNextData.next = null;
                }
            }
        }
    }

    //Last iteration
    precEventName = currEventName;
    currEventName = nextEventName;
    DataMap precNextData = mapEventsToPrecNext.get(currEventName);
    if(precNextData == null)
    {
        newData = new DataMap(precEventName, "");
        mapEventsToPrecNext.put(currEventName, newData);
    }
    else
    {
        String precName = precNextData.prec;
        if(precName != null)
        {
            if(!precName.equals(precEventName))
            {
                precNextData.prec = null;
            }
        }
    }

    // First iteration
    currEventName = vector.get(0).getType().getName();
    nextEventName = vector.get(1).getType().getName();
    precNextData = mapEventsToPrecNext.get(currEventName);
    if(precNextData == null)
    {
        newData = new DataMap("", nextEventName);
        mapEventsToPrecNext.put(currEventName, newData);
    }
    else
    {
        String nextName = precNextData.next;
        if(nextName != null)
        {
            if(!nextName.equals(nextEventName))
            {
                precNextData.next = null;
            }
        }
    }

    // Second loop : remove the events
    Vector<ITmfEvent> res = new Vector<>();
    boolean skipNextEvent = false;

    String evName;
    String nextEventName1 = vector.get(0).getType().getName();
    for(int index = 1; index < maxIter; ++index)
    {
        evName = nextEventName1;
        nextEventName1 = vector.get(index).getType().getName();

        if(skipNextEvent)
        {
            skipNextEvent = false;
            continue;
        }

        // if B is always preceded by A, remove all the A that are preceding B
        DataMap precNextDataNext = mapEventsToPrecNext.get(nextEventName1);
        if(evName.equals(precNextDataNext.prec)) {
            continue;
        }

        // if A is always followed by B, remove all the B that are following A
        precNextData = mapEventsToPrecNext.get(evName);
        if(nextEventName1.equals(precNextData.next)) {
            skipNextEvent = true;
            continue;
        }

        res.add(vector.get(index - 1));
    }

    // Add last
    res.add(vector.get(maxIter));

    return res;
}

private class DataMap
{
    public String prec;
    public String next;
    public DataMap(String prec, String next){
        this.prec = prec;
        this.next = next;
    }
}*/
