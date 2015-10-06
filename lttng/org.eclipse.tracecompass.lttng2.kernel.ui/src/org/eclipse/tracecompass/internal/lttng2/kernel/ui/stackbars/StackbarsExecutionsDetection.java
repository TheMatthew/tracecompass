package org.eclipse.tracecompass.internal.lttng2.kernel.ui.stackbars;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map.Entry;
import java.util.TreeMap;
import java.util.Vector;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.tracecompass.internal.lttng2.kernel.core.analysis.graph.model.LttngStrings;
import org.eclipse.tracecompass.internal.lttng2.kernel.ui.stackbars.StackbarsExecutionsDetection.StackbarsFilter.StackbarsFilterStatus;
import org.eclipse.tracecompass.internal.lttng2.kernel.ui.stackbars.StackbarsExecutionsDetection.StackbarsFilter.StackbarsFilterType;
import org.eclipse.tracecompass.tmf.core.component.ITmfEventProvider;
import org.eclipse.tracecompass.tmf.core.event.ITmfEvent;
import org.eclipse.tracecompass.tmf.core.event.ITmfEventField;
import org.eclipse.tracecompass.tmf.core.request.ITmfEventRequest;
import org.eclipse.tracecompass.tmf.core.request.TmfEventRequest;
import org.eclipse.tracecompass.tmf.core.timestamp.TmfTimeRange;
import org.eclipse.tracecompass.tmf.core.timestamp.TmfTimestamp;
import org.eclipse.tracecompass.tmf.core.trace.ITmfTrace;

public class StackbarsExecutionsDetection {

    // --- Constants

    //private static final String CONTEXT_VTID = "context._vtid"; //$NON-NLS-1$
    public static final String NEXT_PRIO = "next_prio"; //$NON-NLS-1$
    public static final String PREV_PRIO = "prev_prio"; //$NON-NLS-1$

    public static final String SCHED_PI_SETPRIO = "sched_pi_setprio";
    public static final String OLDPRIO = "oldprio";
    public static final String NEWPRIO = "newprio";

    public static final String SCHED_PROCESS_FREE = "sched_process_free";

    public static final int MAX_USER_RT_PRIO = 100;
    public static final int MAX_RT_PRIO = MAX_USER_RT_PRIO;

    private static final int EVENT_DO_NOT_MATCH = -1;

    // --- To match

    // Matching sequence
    private MatchingDefinition fMatchingDefinition;

    // Trace
    // Array avec les events?
    // Score par depth

    // --- Results

    // Executions
    private StackbarsExecution fHeadExecution;
    private HashMap<Integer, TreeMap<Long,Integer>> fPrioritiesByTid;     //TODO maybe not there
    private StackbarsAnalysis fAnalysis;

    public StackbarsExecutionsDetection(StackbarsAnalysis analysis)
    {
        fMatchingDefinition = new MatchingDefinition();
        fHeadExecution = new StackbarsExecution();
        fAnalysis = analysis;
    }

    // --- Get Results

    public HashMap<Integer, TreeMap<Long,Integer>> getPrioMap()
    {
        return fPrioritiesByTid;
    }

    //Get executions
    public StackbarsExecution getHeadExecutions()
    {
        return fHeadExecution;
    }

    public int getMaxDepth()
    {
        return fMatchingDefinition.getMaxDepth();
    }

    // --- Compute results

    public void findExecutions(final ITmfTrace trace, final int currentTid)
    {

        Job job = new Job("Searching executions") { //$NON-NLS-1$

            @Override
            protected IStatus run(final IProgressMonitor monitor) {

                if(fPrioritiesByTid == null)
                {
                    fPrioritiesByTid = new HashMap<>();
                }

                //1) take a new vector for executions (maybe the other is in use so don't clear it)
                final StackbarsExecution tempHeadExecution = new StackbarsExecution();

                //2) Construct matching states
                final AbstractMatchingStatesContainer matchingStatesContainer = getNewContainer(fMatchingDefinition, tempHeadExecution, currentTid);

                TmfEventRequest request;
                request = new TmfEventRequest(ITmfEvent.class, TmfTimeRange.ETERNITY,
                        0,
                        ITmfEventRequest.ALL_DATA,
                        TmfEventRequest.ExecutionType.FOREGROUND) {

                    int[]threadByCpu = new int[8];
                    String[]procNameByCpu = new String[8];

                        @Override
                        public void handleData(ITmfEvent event) {
                            // If the job is canceled, cancel the request so waitForCompletion() will unlock
                            if (monitor.isCanceled()) {
                                cancel();
                                return;
                            }

                            // 3) Get source
                            int source = Integer.parseInt(event.getSource());

                            // 4) get the tid of the events
                            int tid;
                            boolean endOfThread = false;

                            if(source >= threadByCpu.length)
                            {
                                // Enlarge if too small
                                int[] temp = threadByCpu;
                                threadByCpu = new int[source];
                                for(int i = 0; i < temp.length; ++i)
                                {
                                    threadByCpu[i] = temp[i];
                                }

                                String[] temp2 = procNameByCpu;
                                procNameByCpu = new String[source];
                                for(int i = 0; i < temp2.length; ++i)
                                {
                                    procNameByCpu[i] = temp2[i];
                                }

                            }

                            // 4.1) look if CONTEXT_VTID is activate in context
                            /*final ITmfEventField eventField = event.getContent(); //TODO 2/3 of time is in that call
                            ITmfEventField tidField = eventField.getField(CONTEXT_VTID);
                            if(tidField != null)
                            {
                                tid = ((Long) tidField.getValue()).intValue();
                            }
                            else
                            {
                                // 4.2) else from the array*/
                                tid = threadByCpu[source];
                            //}

                            // 4.4) upadate array
                            final String eventName = event.getType().getName();
                            if (eventName.equals(LttngStrings.SCHED_SWITCH)) {

                                ITmfEventField eventField = event.getContent();
                                /* Set the current scheduled process on the relevant CPU for the next iteration */
                                long nextTid = (Long) eventField.getField(LttngStrings.NEXT_TID).getValue();
                                threadByCpu[source] = (int) nextTid;
                                procNameByCpu[source] = eventField.getField(LttngStrings.NEXT_COMM).getFormattedValue();

                                //set prio
                                long nextPrio = (Long) eventField.getField(NEXT_PRIO).getValue();
                                updatePriority(event.getTimestamp().getValue(),nextTid,nextPrio);

                                matchingStatesContainer.checkToAddMachine(tempHeadExecution,nextTid,procNameByCpu[source]);
                            }
                            //4.6) Update array for policy and priority TODO
                            else if (eventName.equals(SCHED_PI_SETPRIO))
                            {
                                ITmfEventField eventField = event.getContent();
                                long tidL = (Long) eventField.getField(LttngStrings.TID).getValue();
                                long prioL = (Long) eventField.getField(NEWPRIO).getValue();
                                updatePriority(event.getTimestamp().getValue(),tidL,prioL);
                            }
                            else if(eventName.equals(SCHED_PROCESS_FREE))
                            {
                                ITmfEventField eventField = event.getContent();
                                endOfThread = true;
                                tid = (int)(long) eventField.getField(LttngStrings.TID).getValue();
                            }

                            // 5) Check if the event match
                            /*if(tid==0 && threadByCpu[source] == 0)
                            {
                                return;
                            }*/
                            matchingStatesContainer.checkIfEventMatch(endOfThread, tid,event,threadByCpu[source], procNameByCpu[source]);
                        }
                    };

                    ((ITmfEventProvider) trace).sendRequest(request);
                    try {
                        request.waitForCompletion();
                    } catch (InterruptedException e) {

                    }

                if (monitor.isCanceled()) {
                    return Status.CANCEL_STATUS;
                }
                notifyAnalysis(tempHeadExecution, trace, monitor);
                return Status.OK_STATUS;

            }
        };
        //job.setSystem(true);
        job.setPriority(Job.SHORT);
        job.schedule();

    }

    // TODO will be in order so maybe better just 2 array
    private void updatePriority(long time,long tid, long prio) {
        Integer key = new Integer((int)tid);
        TreeMap<Long,Integer> map = fPrioritiesByTid.get(key);
        if(map == null)
        {
            map = new TreeMap<>();
            fPrioritiesByTid.put(key, map);
        }
        if(map.size()!= 0)
        {
            if(map.lastEntry().getValue() == prio)
            {
                return;
            }
        }
        map.put(time, (int)prio);
    }

    private void notifyAnalysis(StackbarsExecution headExecution, ITmfTrace trace, IProgressMonitor monitor)
    {
        fHeadExecution = headExecution;
        fAnalysis.notifyBuildISFinish(headExecution, trace, monitor);
    }


    // --- Set data to match

    // Methode define start/end
    public void defineBorderEvents(ExecDefinition fExecDef)
    {
        MatchingDefinition mDef = new MatchingDefinition();

        boolean isSameTid = fExecDef instanceof ExecDefinitionSameTid;
        for(int i = 0; i < fExecDef.fBorderEventsByDepth.size(); ++i)
        {
            defineBorderEventsByDepth(fExecDef.fBorderEventsByDepth.get(i), isSameTid, i, mDef);
        }
        mDef.fDepth = fExecDef.fBorderEventsByDepth.size();
        if(isSameTid)
        {
            mDef.fMode = MatchGlobalMode.SAME_TID;
        }
        else
        {
            mDef.fMode = MatchGlobalMode.DIFFERENT_TIDS;
        }
        //Tids
        String startTids = "";
        String endTids = "";
        if(isSameTid)
        {
            startTids = ((ExecDefinitionSameTid)fExecDef).tids;
        }
        else
        {
            startTids = ((ExecDefinitionDiffTids)fExecDef).tidsStart;
            endTids = ((ExecDefinitionDiffTids)fExecDef).tidsEnd;
        }
        parseTids(startTids, endTids, mDef.fBorderEventsTids);
        fMatchingDefinition = mDef;
    }

    private static void parseName(String eventName, Vector<String> vector)
    {
        String[] names = eventName.split("\\Q||\\E");
        for(String name : names)
        {
            if(!name.isEmpty())
            {
                vector.add(name.trim());
            }
        }
    }

    private boolean parseParams(String borderEventsParams, Vector<StackbarsMatchingParam> vector, Vector<StackbarsMatchingParam> vectorWithTid)
    {
        if(borderEventsParams == null)
        {
            return false;
        }

        if(borderEventsParams.isEmpty())
        {
            return false;
        }

        boolean needToMatchTid = false;

        String[] params =  borderEventsParams.split("\\s*,\\s*");

        for(String param : params)
        {
            if(param.isEmpty())
            {
                continue;
            }
            String name = null;
            String value = null;
            int operatorValue = -1;
            EnumSet<OperatorFlag> enumSet = null;

            if(param.contains("&"))
            {
                int index = param.indexOf("&");
                int nextIndex = param.indexOf("!");
                if(nextIndex == -1)
                {
                    nextIndex = param.indexOf("=");
                }
                operatorValue = Integer.parseInt(param.substring(index + 1, nextIndex));
                param = param.substring(0, index) + param.substring(nextIndex);
                enumSet = EnumSet.of(OperatorFlag.AND);
            }

            if(param.contains("$tid"))
            {
                needToMatchTid = true;
                if(enumSet == null)
                {
                    enumSet = EnumSet.of(OperatorFlag.TID_MATCH);
                }
                else
                {
                    enumSet.add(OperatorFlag.TID_MATCH);
                }
            }

            if(param.contains("!="))
            {
                int index = param.indexOf("!");
                name = param.substring(0, index);
                value = param.substring(index + 2);
                if(enumSet == null)
                {
                    enumSet = EnumSet.of(OperatorFlag.NEQ);
                }
                else
                {
                    enumSet.add(OperatorFlag.NEQ);
                }
            }
            else if(param.contains("="))
            {
                int index = param.indexOf("=");
                name = param.substring(0, index);
                value = param.substring(index + 1);
                if(enumSet == null)
                {
                    enumSet = EnumSet.of(OperatorFlag.EQ);
                }
                else
                {
                    enumSet.add(OperatorFlag.EQ);
                }
            }
            else
            {
                return false;
            }

            if(enumSet.contains(OperatorFlag.TID_MATCH))
            {
                vectorWithTid.add(new StackbarsMatchingParam(name, new StackbarsMatchingParamValue(value, enumSet, operatorValue)));
            }
            else
            {
                vector.add(new StackbarsMatchingParam(name, new StackbarsMatchingParamValue(value, enumSet, operatorValue)));
            }

        }

        return needToMatchTid;
    }

    public void parseTids(String startTids, String endTids, BorderEventsTids borderEventsTids)
    {
        //Start
        int indexComa = startTids.indexOf(',');
        int endIndex = 0;
        String tid;
        while (true)
        {
            if(indexComa == -1)
            {
                tid = startTids.substring(endIndex);
            }
            else
            {
                tid = startTids.substring(endIndex, indexComa);
            }

            endIndex = indexComa + 1;
            try{
                int tidInt = Integer.parseInt(tid);
                borderEventsTids.fTidsIntStart.add(tidInt);
            }
            catch (NumberFormatException e){
                if(!tid.isEmpty()){
                    if(tid.equals("*"))
                    {
                        //All tids are good, clear the vectors
                        borderEventsTids.fTidsIntStart.clear();
                        borderEventsTids.fProcNamesStart.clear();
                        borderEventsTids.fProcNamesStart.add(tid);
                        break;
                    }
                    borderEventsTids.fProcNamesStart.add(tid);
                }
            }

            if(indexComa == -1)
            {
                 break;
            }

            indexComa = startTids.indexOf(',',endIndex);
        }

        //end
        indexComa = endTids.indexOf(',');
        endIndex = 0;
        while (true)
        {
            if(indexComa == -1)
            {
                tid = endTids.substring(endIndex);
            }
            else
            {
                tid = endTids.substring(endIndex, indexComa);
            }

            endIndex = indexComa + 1;
            try{
                int tidInt = Integer.parseInt(tid);
                borderEventsTids.fTidsIntEnd.add(tidInt);
            }
            catch (NumberFormatException e){
                if(!tid.isEmpty()){
                    if(tid.equals("*"))
                    {
                        //All tids are good, clear the vectors
                        borderEventsTids.fTidsIntEnd.clear();
                        borderEventsTids.fProcNamesEnd.clear();
                        borderEventsTids.fProcNamesEnd.add(tid);
                        break;
                    }
                    borderEventsTids.fProcNamesEnd.add(tid);
                }
            }

            if(indexComa == -1)
            {
                 break;
            }

            indexComa = endTids.indexOf(',',endIndex);
        }
    }

    // Methode define start/end
    private void defineBorderEventsByDepth(BorderEvents borderEvents, boolean isSameTid, int depth, MatchingDefinition mDef)
    {
        if(borderEvents == null)
        {
            return;
        }

        Vector<StackbarsMatchingEvent> sequence = new Vector<>();
        MatchByDepthMode mode = MatchByDepthMode.DEFAULT;

        if(isSameTid)
        {

            for(EventDefinition ed : borderEvents.fEventDefinitions)
            {
                //Start Event
                Vector<StackbarsMatchingParam> vectorParams = new Vector<>();
                Vector<StackbarsMatchingParam> vectorParamsWithTid = new Vector<>();
                parseParams(ed.eventParams, vectorParams,vectorParamsWithTid);
                Vector<String> vectorNames = new Vector<>();
                parseName(ed.eventName, vectorNames);
                if(vectorNames.size() != 0 || vectorParams.size() != 0)
                {
                    StackbarsMatchingEvent event = new StackbarsMatchingEvent(vectorNames,
                            vectorParams/* , Integer.MAX_VALUE*/, vectorParamsWithTid);
                    sequence.add(event);
                }
            }
            if(borderEvents.fContinuous)
            {
                mode = MatchByDepthMode.CONTINUOUS;
            }
            else
            {
                mode = MatchByDepthMode.DISJOINT;
            }
        }
        else
        {
            //Start Event
            Vector<StackbarsMatchingParam> vectorParams = new Vector<>();
            Vector<StackbarsMatchingParam> vectorParamsWithTid = new Vector<>();
            parseParams(borderEvents.getStartEvent().eventParams, vectorParams,vectorParamsWithTid);
            Vector<String> vectorNames = new Vector<>();
            parseName(borderEvents.getStartEvent().eventName, vectorNames);

            if(vectorNames.size() != 0 || vectorParams.size() != 0)
            {
                StackbarsMatchingEvent start = new StackbarsMatchingEvent(vectorNames,
                        vectorParams/* , Integer.MAX_VALUE*/, vectorParamsWithTid);

                sequence.add(start);
            }

            //End event
            vectorParams = new Vector<>();
            vectorParamsWithTid = new Vector<>();
            parseParams(borderEvents.getEndEvent().eventParams, vectorParams,vectorParamsWithTid);
            vectorNames = new Vector<>();
            parseName(borderEvents.getEndEvent().eventName, vectorNames);
            if(vectorNames.size() != 0 || vectorParams.size() != 0)
            {
                StackbarsMatchingEvent end = new StackbarsMatchingEvent(vectorNames,
                        vectorParams/*, Integer.MAX_VALUE*/, vectorParamsWithTid);

                sequence.add(end);
            }
            if(borderEvents.fContinuous)
            {
                mode = MatchByDepthMode.CONTINUOUS;
            }
            else
            {
                mode = MatchByDepthMode.DISJOINT;
            }
        }

        if(sequence.size() != 0 && sequence.get(0).fName.equals(StackbarsAnalysis.ALLINONEEXEC))
        {
            mode = MatchByDepthMode.ALLINONE;
        }

        // Add the sequence
        if(mDef.fMatchingSequences.size() == depth)
        {
            mDef.fMatchingSequences.add(new StackbarsMatchingSequences(sequence,mode));
        }
    }


    // --- Other stuff

    // Methode execution in
    public StackbarsExecution addValidExecution(final int currentTid, int depth, TmfTimestamp startTime, TmfTimestamp endTime, ITmfTrace trace)
    {
        System.out.println(depth + " " + startTime + " " + endTime + "" + trace);
        //TmfTimeRange range = new TmfTimeRange(startTime,endTime);
        final StackbarsExecution execution = new StackbarsExecution();
        /*TmfEventRequest request = new TmfEventRequest(ITmfEvent.class, range,
                0,
                ITmfEventRequest.ALL_DATA,
                TmfEventRequest.ExecutionType.FOREGROUND)
        {
            @Override
            public void handleData(ITmfEvent event) {
                String name = event.getType().getName();
                if (name.equals(LttngStrings.SCHED_SWITCH)) {
                    long prevTid = (Long) event.getContent().getField(LttngStrings.PREV_TID).getValue();
                    if(prevTid == currentTid)
                    {
                        execution.addState(new StateTimeSource(event.getTimestamp().getValue(),
                                StackbarsStateValues.STATUS_AFTER_SCHED_SWITCH_PREV, event.getSource()));
                        return;
                    }

                    long nextTid = (Long) event.getContent().getField(LttngStrings.NEXT_TID).getValue();
                    if(nextTid == currentTid)
                    {
                        execution.addState(new StateTimeSource(event.getTimestamp().getValue(),
                                StackbarsStateValues.STATUS_AFTER_SCHED_SWITCH_NEXT, event.getSource()));
                        return;
                    }
                }
                else if (name.equals(LttngStrings.SCHED_WAKEUP) || name.equals(LttngStrings.SCHED_WAKEUP_NEW))
                {
                    long tid = (Long) event.getContent().getField(LttngStrings.TID).getValue();
                    if(tid == currentTid)
                    {
                        execution.addState(new StateTimeSource(event.getTimestamp().getValue(),
                                StackbarsStateValues.STATUS_AFTER_SCHED_WAKE,event.getSource()));
                        return;
                    }
                }
            }
        };

        ((ITmfEventProvider) trace).sendRequest(request);
        try {
            request.waitForCompletion();
        } catch (InterruptedException e) {
            Activator.getDefault().logError("Wait for completion interrupted for get events ", e); //$NON-NLS-1$
        }*/

        execution.setStartTime(startTime.getValue());
        execution.setEndTime(endTime.getValue());
        execution.setTid(currentTid);

        return execution;

    }

    // Methode execution out
    public void addInvalidExecution(int depth, int id)
    {
        System.out.println(depth + " " + id);
    }

    private enum MatchByDepthMode{
        ALLINONE,
        CONTINUOUS,
        DISJOINT,
        DEFAULT
    }

    private enum MatchGlobalMode{
        SAME_TID,
        DIFFERENT_TIDS,
        DEFAULT
    }

    private class StackbarsMatchingSequences{
        public final Vector<StackbarsFilter> fFiltersTime;
        public final Vector<StackbarsFilter> fFiltersCount;
        public final Vector<StackbarsExecution> fValidExecutions;
        public final Vector<StackbarsMatchingEvent> fMatchingSequence;
        public final MatchByDepthMode fMode;
        public StackbarsMatchingSequences(Vector<StackbarsMatchingEvent> sequence, MatchByDepthMode mode){
          this.fMatchingSequence = sequence;
          this.fFiltersTime = new Vector();
          this.fFiltersCount = new Vector();
          this.fValidExecutions = new Vector();
          this.fMode = mode;
        }
    }

    private class StackbarsMatchingEvent{
        public final Vector<String> fName;
        public final Vector<StackbarsMatchingParam> fParams;
        public final Vector<StackbarsMatchingParam> fParamsWithTid;
        //TODO public final int fMaxLengthBeforeNext;
        public StackbarsMatchingEvent( Vector<String> name, Vector<StackbarsMatchingParam> params/*, int maxLength*/, Vector<StackbarsMatchingParam> paramsWithTid){
          this.fName = name;
          this.fParams = params;
          this.fParamsWithTid = paramsWithTid;
          //this.fMaxLengthBeforeNext = maxLength;
        }

        @Override
        public String toString()
        {
            String text = "Event name : ";
            for(String name : fName)
            {
                text += name;
                text += "||";
            }
            if(text.length() > 2)
            {
                text = text.substring(0, text.length() - 2);
            }

            text+= " ";

            if(fParams != null)
            {
                for(StackbarsMatchingParam param : fParams)
                {
                    text += param.toString();
                    text += ";";
                }
            }
            return text;
        }

        public boolean getIfNeedToMatchTid() {
            return fParamsWithTid != null && fParamsWithTid.size() != 0;
        }
      }

    private class StackbarsMatchingParam{
        public final String fName;
        public final StackbarsMatchingParamValue fValues;
        public StackbarsMatchingParam( String name, StackbarsMatchingParamValue values){
          this.fName = name;
          this.fValues = values;
        }

        @Override
        public String toString()
        {
            return fName + " " + fValues.toString();
        }
      }

    private class StackbarsMatchingParamValue{
        public final String fComparisonValue;
        public final int fOperatorValue; // for &= and |=
        public final EnumSet<OperatorFlag> fOperators;
        public StackbarsMatchingParamValue( String comparisonValue, EnumSet<OperatorFlag> operators, int operatorValue){
          this.fComparisonValue = comparisonValue;
          this.fOperatorValue = operatorValue;
          this.fOperators = operators;
        }

        @Override
        public String toString()
        {
            return "Value : " + fComparisonValue + "Operator value : " + fOperatorValue + " Operators : " + fOperators;
        }
      }

    public static class StackbarsFilter {

        public enum StackbarsFilterType {
             MORE_THAN, //Value = int
             LESS_THAN, //value = int
             //EVENT_BEFORE, //value = StackbarsMatchingEvent
             //PARAMETERS, //value = Vector<StackbarsMatchingParam>
             MIN_START_TIME, //value = long
             MAX_START_TIME, //value = long
        }

        public enum StackbarsFilterStatus {
            STATUS_MATCHING,
            STATUS_NOT_MATCHING_YET,
            STATUS_WILL_NOT_MATCH,
        }

        private StackbarsMatchingEvent fEvent;
        private Object fValue;
        private StackbarsFilterType fType;
        public StackbarsMatchingEvent getEvent() {
            return fEvent;
        }
        public void setEvent(StackbarsMatchingEvent event) {
            this.fEvent = event;
        }
        public Object getValue() {
            return fValue;
        }
        public void setValue(Object value) {
            this.fValue = value;
        }
        public StackbarsFilterType getType() {
            return fType;
        }
        public void setType(StackbarsFilterType type) {
            this.fType = type;
        }

        @Override
        public String toString()
        {
            String text = "Not defined";

            if(fType == null || fValue == null)
            {
                return text;
            }

            switch (fType){
                case MORE_THAN:
                    return "Need more than <" + fValue + "> of <" + (fEvent == null ? "" : fEvent.toString()) + ">";
                case MAX_START_TIME:
                    return "Need to start before " + fValue;
                //case EVENT_BEFORE:
                //    return "This : <" + (fEvent == null ? "" : fEvent.fName) + "> must be before : <" + fType + ">";
                case LESS_THAN:
                    return "Need less than " + fValue + " of <" + (fEvent == null ? "" : fEvent.toString()) + ">";
                //case PARAMETERS:
                //    return "The parameters of <" + (fEvent == null ? "" : fEvent.fName) + "> should be <" + fValue + ">";
                case MIN_START_TIME:
                    return "Need to start after " + fValue;
            default:
                return text;
            }
        }
    }

    public enum OperatorFlag {
        /* Operator flag */
        EQ,NEQ,GREATER,LOWER,AND,OR,TID_MATCH,PRIO_MATCH;

        //GOE = GREATER + EQ;
        //LOE = LOWER + EQ;

    }

    private class BorderEventsTids{
        public final Vector<Integer> fTidsIntStart;
        public final Vector<String> fProcNamesStart;
        public final Vector<Integer> fTidsIntEnd;
        public final Vector<String> fProcNamesEnd;
        /*public final Vector<Integer> fTidsEnd;*/
        public BorderEventsTids()
        {
            fTidsIntStart = new Vector<>();
            fProcNamesStart = new Vector<>();
            fTidsIntEnd = new Vector<>();
            fProcNamesEnd = new Vector<>();
            /*fTidsEnd = new Vector<>();*/
        }
    }

    private abstract class AbstractMatchingStatesContainer
    {
        public abstract void checkIfEventMatch(boolean endOfThread, int tid, ITmfEvent event, int nextTid, String nextString);
        public abstract void checkToAddMachine(StackbarsExecution tempHeadExecution, long nextTid, String next_comm);
        public abstract boolean isEmpty();
    }

    private AbstractMatchingStatesContainer getNewContainer(MatchingDefinition fMatchingDefinition1, StackbarsExecution tempHeadExecution, int currentTid) {

        if(fMatchingDefinition1.getMode() == MatchGlobalMode.SAME_TID)
        {
            Vector<StackbarsMatchingSequences> vecMatchingSequences = new Vector<>(fMatchingDefinition1.fMatchingSequences);
            return new MatchingStatesContainerSameTid(tempHeadExecution, currentTid,vecMatchingSequences);
        }
        else if(fMatchingDefinition1.getMode() == MatchGlobalMode.DIFFERENT_TIDS)
        {
            Vector<StackbarsMatchingSequences> vecMatchingSequences = new Vector<>(fMatchingDefinition1.fMatchingSequences);
            return new MatchingStatesContainerDiffTids(tempHeadExecution, currentTid, vecMatchingSequences);
        }
        return null;
    }

    private class MatchingStatesContainerSameTid extends AbstractMatchingStatesContainer{

        private HashMap<Integer, AbstractMatchingState> fMatchingStatesMap;
        private Vector<StackbarsMatchingSequences> fVecMatchingSequences;
        private ArrayList<StackbarsMatchingEvent> fSMENeedTid;
        private ArrayList<Integer> fTidsInclude;
        private ArrayList<Integer> fTidsExclude;

        public MatchingStatesContainerSameTid(StackbarsExecution tempHeadExecution, int currentTid, Vector<StackbarsMatchingSequences> vecMatchingSequences) {
            fVecMatchingSequences = vecMatchingSequences;
            fMatchingStatesMap = constructMatchingStates(tempHeadExecution, currentTid);
            fSMENeedTid = new ArrayList<>();
            fTidsInclude = new ArrayList<>();
            fTidsExclude = new ArrayList<>();
            for(StackbarsMatchingSequences vec : fVecMatchingSequences)
            {
                for(StackbarsMatchingEvent eventDef : vec.fMatchingSequence)
                {
                    if(eventDef.getIfNeedToMatchTid())
                    {
                        fSMENeedTid.add(eventDef);
                    }
                }
            }
        }

        @Override
        public void checkIfEventMatch(boolean endOfThread, int tid, ITmfEvent event, int nextTid, String nextString) {

            //check matching tid events
            fTidsInclude.clear();
            fTidsExclude.clear();
            for(StackbarsMatchingEvent eventToMatch : fSMENeedTid)
            {
                getTidsUsage(eventToMatch, event, fTidsInclude, fTidsExclude);
            }

            //If exclude, go on all
            if(fTidsExclude.size() != 0)
            {
                for(AbstractMatchingState a : fMatchingStatesMap.values())
                {
                    a.parseNextEvent(event, tid, nextTid, nextString);
                }
            }
            else
            {
                //Send to the good statesMap
                if(!fTidsInclude.contains(tid)) //TODO this can be slow, not in constant time
                {
                    fTidsInclude.add(tid);
                }
                if(tid != nextTid && !fTidsInclude.contains(tid))
                {
                    fTidsInclude.add(nextTid);
                }
                for(Integer tidToCheck : fTidsInclude)
                {
                    AbstractMatchingState mS = fMatchingStatesMap.get(tidToCheck);
                    if(mS != null)
                    {
                        mS.parseNextEvent(event, tid, nextTid, nextString);
                    }
                }
            }

            //Remove zombie from hashmap
            if(endOfThread)
            {
                fMatchingStatesMap.remove(nextTid);
            }
        }

        @Override
        public void checkToAddMachine(StackbarsExecution tempHeadExecution, long nextTid, String next_comm) {
          //4.5) If string, check to add it
            if(fMatchingDefinition.fBorderEventsTids.fProcNamesStart.size() != 0)
            {
                //All tid
                if(fMatchingDefinition.fBorderEventsTids.fProcNamesStart.get(0).equals("*"))
                {
                    //Check if already there
                    if(!fMatchingStatesMap.containsKey((int)nextTid))
                    {
                        TidsProvider prov = new TidsProviderSameTid((int)nextTid);
                        //Add the matching state corresponding to the tid
                        AbstractMatchingState childMatchingState = null;
                        // Only the top parent will be add to the array
                        for(int i = fMatchingDefinition.fDepth-1; i >= 0; --i)
                        {
                            if(fVecMatchingSequences.get(i).fMatchingSequence.size() == 0)
                            {
                                continue;
                            }
                            MatchByDepthMode mode = fVecMatchingSequences.get(i).fMode;
                            if(mode == MatchByDepthMode.ALLINONE)
                            {
                                childMatchingState = new MatchingStateAllInOneExecution(fVecMatchingSequences.get(i),prov);
                            }
                            else if(mode == MatchByDepthMode.CONTINUOUS)
                            {
                                childMatchingState = new MatchingStateContinuous(fVecMatchingSequences.get(i),
                                        childMatchingState,prov , next_comm);
                            }
                            else if(mode == MatchByDepthMode.DISJOINT)
                            {
                                childMatchingState = new MatchingStateDisjoint(fVecMatchingSequences.get(i),
                                        childMatchingState, prov, next_comm);
                            }
                            continue;
                        }

                        if (childMatchingState != null)
                        {
                            fMatchingStatesMap.put((int) nextTid,childMatchingState);

                            // the top parent will be the child of the head execution
                            childMatchingState.setParentExecution(tempHeadExecution);
                        }
                    }
                }
                else //By name
                {
                    for(String token : fMatchingDefinition.fBorderEventsTids.fProcNamesStart)
                    {
                        if(next_comm.equals(token))
                        {
                          //Check if already there
                            if(!fMatchingStatesMap.containsKey((int)nextTid))
                            {
                                TidsProvider prov = new TidsProviderSameTid((int)nextTid);
                                //Add the matching state corresponding to the tid
                                AbstractMatchingState childMatchingState = null;
                                // Only the top parent will be add to the array
                                for(int i = fMatchingDefinition.getMaxDepth()-1; i >= 0; --i)
                                {
                                    if(fVecMatchingSequences.get(i).fMatchingSequence.size() == 0)
                                    {
                                        continue;
                                    }
                                    MatchByDepthMode mode = fVecMatchingSequences.get(i).fMode;
                                    if(mode == MatchByDepthMode.ALLINONE)
                                    {
                                        childMatchingState = new MatchingStateAllInOneExecution(fVecMatchingSequences.get(i),prov);
                                    }
                                    else if(mode == MatchByDepthMode.CONTINUOUS)
                                    {
                                        childMatchingState = new MatchingStateContinuous(fVecMatchingSequences.get(i),
                                                childMatchingState, prov, next_comm);
                                    }
                                    else if(mode == MatchByDepthMode.DISJOINT)
                                    {
                                        childMatchingState = new MatchingStateDisjoint(fVecMatchingSequences.get(i),
                                                childMatchingState, prov, next_comm);
                                    }
                                    continue;
                                }

                                if (childMatchingState != null)
                                {
                                    fMatchingStatesMap.put((int) nextTid,childMatchingState);

                                    // the top parent will be the child of the head execution
                                    childMatchingState.setParentExecution(tempHeadExecution);
                                }
                            }

                            break;
                        }
                    }
                }
            }

        }

        @Override
        public boolean isEmpty() {
            return fMatchingStatesMap == null;
        }

        protected HashMap<Integer,AbstractMatchingState> constructMatchingStates(StackbarsExecution tempHeadExecution, int currentTid) {
            // 1) to be sure there is something defined
            if(fMatchingDefinition.fBorderEventsTids == null)
            {
                return null;
            }

            // 2) if there is no integers and no strings, we take the current tid
            boolean empty = false;
            int sizeInt = fMatchingDefinition.fBorderEventsTids.fTidsIntStart.size();
            HashMap<Integer,AbstractMatchingState> matchingStatesTemp = new HashMap<>();
            if(sizeInt == 0 && fMatchingDefinition.fBorderEventsTids.fProcNamesStart.size() == 0)
            {
                fMatchingDefinition.fBorderEventsTids.fTidsIntStart.add(currentTid);
                sizeInt = 1;
                empty = true;
            }

            for(int j = 0; j < sizeInt; ++j)
            {
                AbstractMatchingState childMatchingState = null;
                int tid = fMatchingDefinition.fBorderEventsTids.fTidsIntStart.get(j);
                TidsProvider prov = new TidsProviderSameTid(tid);
                for(int i = fMatchingDefinition.getMaxDepth()-1; i >= 0; --i)
                {
                    if(fVecMatchingSequences.get(i).fMatchingSequence.size() == 0)
                    {
                        continue;
                    }
                    MatchByDepthMode mode = fVecMatchingSequences.get(i).fMode;
                    if(mode == MatchByDepthMode.ALLINONE)
                    {
                        childMatchingState = new MatchingStateAllInOneExecution(fVecMatchingSequences.get(i),prov);
                    }
                    else if(mode == MatchByDepthMode.CONTINUOUS)
                    {
                        childMatchingState = new MatchingStateContinuous(fVecMatchingSequences.get(i),
                                childMatchingState, prov, "");
                    }
                    else if(mode == MatchByDepthMode.DISJOINT)
                    {
                        childMatchingState = new MatchingStateDisjoint(fVecMatchingSequences.get(i),
                                childMatchingState, prov, "");
                    }
                    else if(mode == MatchByDepthMode.DEFAULT)
                    {
                        childMatchingState = new MatchingStateDisjoint(fVecMatchingSequences.get(i),
                                childMatchingState, prov, "");
                    }
                    continue;
                }

                if (childMatchingState == null)
                {
                    if(empty)
                    {
                        fMatchingDefinition.fBorderEventsTids.fTidsIntStart.clear();
                    }
                    return null;

                }

                // the top parents will be under the head execution
                matchingStatesTemp.put(tid,childMatchingState);
                childMatchingState.setParentExecution(tempHeadExecution);
            }

            if(empty)
            {
                fMatchingDefinition.fBorderEventsTids.fTidsIntStart.clear();
            }
            return matchingStatesTemp;
        }

    }

    private class MatchingStatesContainerDiffTids extends AbstractMatchingStatesContainer{

        private AbstractMatchingState fMatchingStatesMap;
        private Vector<StackbarsMatchingSequences> fVecMatchingSequences;
        private TidsProvider fProv;
        public MatchingStatesContainerDiffTids(StackbarsExecution tempHeadExecution, int currentTid, Vector<StackbarsMatchingSequences> vecMatchingSequences) {
            fVecMatchingSequences = vecMatchingSequences;
            fMatchingStatesMap = constructMatchingStates(tempHeadExecution,currentTid);
        }

        @Override
        public void checkIfEventMatch(boolean endOfThread, int tid, ITmfEvent event, int nextTid, String nextString) {

            //check matching tid events
            fMatchingStatesMap.parseNextEvent(event, tid, nextTid, nextString);

        }

        @Override
        public void checkToAddMachine(StackbarsExecution tempHeadExecution, long nextTid, String next_comm) {
            if(fMatchingDefinition.fBorderEventsTids.fProcNamesStart.size() != 0 && !fProv.validateTid((int)nextTid)){
                if(fMatchingDefinition.fBorderEventsTids.fProcNamesStart.size() != 0)
                {
                    for(String name : fMatchingDefinition.fBorderEventsTids.fProcNamesStart)
                    {
                        if(name.equals(next_comm))
                        {
                            ((TidsProviderDiffTids)fProv).addTidStart((int)nextTid);
                            break;
                        }
                    }
                }
            }
            if(fMatchingDefinition.fBorderEventsTids.fProcNamesEnd.size() != 0 && !fProv.validateTidEnd((int)nextTid)){
                if(fMatchingDefinition.fBorderEventsTids.fProcNamesEnd.size() != 0)
                {
                    for(String name : fMatchingDefinition.fBorderEventsTids.fProcNamesEnd)
                    {
                        if(name.equals(next_comm))
                        {
                            ((TidsProviderDiffTids)fProv).addTidEnd((int)nextTid);
                            break;
                        }
                    }
                }
            }
        }

        @Override
        public boolean isEmpty() {
            return fMatchingStatesMap == null;
        }

        protected AbstractMatchingState constructMatchingStates(StackbarsExecution tempHeadExecution, int currentTid) {
            // 1) to be sure there is something defined
            if(fMatchingDefinition.fBorderEventsTids == null)
            {
                return null;
            }

            // 2) if there is no integers and no strings, we take the current tid
            boolean empty = false;
            int sizeInt = fMatchingDefinition.fBorderEventsTids.fTidsIntStart.size();
            AbstractMatchingState matchingStatesTemp = null;
            if(sizeInt == 0 && fMatchingDefinition.fBorderEventsTids.fProcNamesStart.size() == 0)
            {
                fMatchingDefinition.fBorderEventsTids.fTidsIntStart.add(currentTid);
                sizeInt = 1;
                empty = true;
            }

            HashSet<Integer> validTidsStart = new HashSet<>();
            if(fMatchingDefinition.fBorderEventsTids.fProcNamesStart.size() == 0 &&
                    fMatchingDefinition.fBorderEventsTids.fTidsIntStart.size() == 0)
            {
                validTidsStart.add(currentTid);
            }
            else
            {
                validTidsStart.addAll(fMatchingDefinition.fBorderEventsTids.fTidsIntStart);
            }
            HashSet<Integer> validTidsEnd = new HashSet<>();
            if(fMatchingDefinition.fBorderEventsTids.fProcNamesEnd.size() == 0 &&
                    fMatchingDefinition.fBorderEventsTids.fTidsIntEnd.size() == 0)
            {
                validTidsEnd.add(currentTid);
            }
            else
            {
                validTidsEnd.addAll(fMatchingDefinition.fBorderEventsTids.fTidsIntEnd);
            }
            fProv = new TidsProviderDiffTids(validTidsStart, validTidsEnd);

            for(int j = 0; j < sizeInt; ++j)
            {
                AbstractMatchingState childMatchingState = null;
                for(int i = fMatchingDefinition.getMaxDepth()-1; i >= 0; --i)
                {
                    if(fVecMatchingSequences.get(i).fMatchingSequence.size() == 0)
                    {
                        continue;
                    }
                    MatchByDepthMode mode = fVecMatchingSequences.get(i).fMode;
                    if(mode == MatchByDepthMode.ALLINONE)
                    {
                        childMatchingState = new MatchingStateAllInOneExecution(fVecMatchingSequences.get(i),fProv);
                    }
                    else if(mode == MatchByDepthMode.CONTINUOUS)
                    {
                        childMatchingState = new MatchingStateContinuous(fVecMatchingSequences.get(i),
                                childMatchingState, fProv, "");
                    }
                    else if(mode == MatchByDepthMode.DISJOINT)
                    {
                        childMatchingState = new MatchingStateDisjoint(fVecMatchingSequences.get(i),
                                childMatchingState, fProv, "");
                    }
                    else if(mode == MatchByDepthMode.DEFAULT)
                    {
                        childMatchingState = new MatchingStateDisjoint(fVecMatchingSequences.get(i),
                                childMatchingState, fProv, "");
                    }
                    continue;
                }

                if (childMatchingState == null)
                {
                    if(empty)
                    {
                        fMatchingDefinition.fBorderEventsTids.fTidsIntStart.clear();
                    }
                    return null;

                }

                // the top parents will be under the head execution
                matchingStatesTemp = childMatchingState;
                childMatchingState.setParentExecution(tempHeadExecution);
            }

            if(empty)
            {
                fMatchingDefinition.fBorderEventsTids.fTidsIntStart.clear();
            }
            return matchingStatesTemp;
        }

    }

/*    private class MatchingStatesContainerList extends AbstractMatchingStatesContainer{

        private LinkedList<AbstractMatchingState> fMatchingStatesList;
        HashSet<Integer> tidActivated = new HashSet<>();
        Vector<StackbarsMatchingSequences> fVecMatchingSequences;

        public MatchingStatesContainerList(StackbarsExecution tempHeadExecution, int currentTid, Vector<StackbarsMatchingSequences> vecMatchingSequences) {
            fVecMatchingSequences = vecMatchingSequences;
            fMatchingStatesList = constructMatchingStates(tempHeadExecution, currentTid);
        }

        @Override
        public void checkIfEventMatch(boolean endOfThread, int tid, ITmfEvent event, int nextTid, String nextString) {

            ListIterator<AbstractMatchingState> listIterator = fMatchingStatesList.listIterator();
            while (listIterator.hasNext()) {
                AbstractMatchingState AMS = listIterator.next();
                AMS.parseNextEvent(event, tid, nextTid, nextString);
                if(endOfThread && AMS.getStartTid() == tid)
                {
                    listIterator.remove();
                }
            }

        }

        @Override
        public void checkToAddMachine(StackbarsExecution tempHeadExecution, long nextTid, String next_comm) {
          //4.5) If string, check to add it
            if(fBorderEventsTids.fProcNamesStart.size() != 0)
            {
                //All tid
                if(fBorderEventsTids.fProcNamesStart.get(0).equals("*"))
                {
                    //Check if already there
                    if(!tidActivated.contains((int)nextTid))
                    {
                        //Add the matching state corresponding to the tid
                        AbstractMatchingState childMatchingState = null;
                        // Only the top parent will be add to the array
                        for(int i = fDepth-1; i >= 0; --i)
                        {
                            Vector<String> vec = fVecMatchingSequences.get(i).fEventStart.fName;
                            if(vec.size()!= 0 && vec.get(0).equals(StackbarsAnalysis.ALLINONEEXEC))
                            {
                                childMatchingState = new MatchingStateAllInOneExecution(fVecMatchingSequences.get(i),(int) nextTid);
                            }
                            else if(fVecMatchingSequences.get(i).fEventEnd.fName.size() != 0)
                            {
                                childMatchingState = new MatchingStateTwoEvents(fVecMatchingSequences.get(i),
                                        childMatchingState, (int) nextTid,next_comm);
                            }
                            else
                            {
                                childMatchingState = new MatchingStateOneEvent(fVecMatchingSequences.get(i),
                                        childMatchingState, (int) nextTid,next_comm);
                            }
                        }

                        if (childMatchingState != null)
                        {
                            fMatchingStatesList.add(childMatchingState);

                            // the top parent will be the child of the head execution
                            childMatchingState.setParentExecution(tempHeadExecution);
                        }

                        tidActivated.add((int) nextTid);
                    }
                }
                else //By name
                {
                    for(String token : fBorderEventsTids.fProcNamesStart)
                    {
                        if(next_comm.equals(token))
                        {
                          //Check if already there
                            if(!tidActivated.contains((int)nextTid))
                            {
                                //Add the matching state corresponding to the tid
                                AbstractMatchingState childMatchingState = null;
                                // Only the top parent will be add to the array
                                for(int i = fDepth-1; i >= 0; --i)
                                {
                                    Vector<String> vec = fVecMatchingSequences.get(i).fEventStart.fName;
                                    if(vec.size()!= 0 && vec.get(0).equals(StackbarsAnalysis.ALLINONEEXEC))
                                    {
                                        childMatchingState = new MatchingStateAllInOneExecution(fVecMatchingSequences.get(i),(int) nextTid);
                                    }
                                    else if(fVecMatchingSequences.get(i).fEventEnd.fName.size() != 0)
                                    {
                                        childMatchingState = new MatchingStateTwoEvents(fVecMatchingSequences.get(i),
                                                childMatchingState, (int) nextTid,next_comm);
                                    }
                                    else
                                    {
                                        childMatchingState = new MatchingStateOneEvent(fVecMatchingSequences.get(i),
                                                childMatchingState, (int) nextTid,next_comm);
                                    }
                                }

                                if (childMatchingState != null)
                                {
                                    fMatchingStatesList.add(childMatchingState);

                                    // the top parent will be the child of the head execution
                                    childMatchingState.setParentExecution(tempHeadExecution);
                                }

                                tidActivated.add((int) nextTid);
                            }

                            break;
                        }
                    }
                }
            }

        }

        @Override
        public boolean isEmpty() {
            return fMatchingStatesList == null;
        }

        protected LinkedList<AbstractMatchingState> constructMatchingStates(StackbarsExecution tempHeadExecution, int currentTid) {
            // 1) to be sure there is something defined
            if(fBorderEventsTids == null)
            {
                return null;
            }

            // 2) if there is no integers and no strings, we take the current tid
            boolean empty = false;
            int sizeInt = fBorderEventsTids.fTidsIntStart.size();
            LinkedList<AbstractMatchingState> matchingStatesTemp = new LinkedList<>();
            if(sizeInt == 0 && fBorderEventsTids.fProcNamesStart.size() == 0)
            {
                fBorderEventsTids.fTidsIntStart.add(currentTid);
                sizeInt = 1;
                empty = true;
            }

            for(int j = 0; j < sizeInt; ++j)
            {
                AbstractMatchingState childMatchingState = null;
                int tid = fBorderEventsTids.fTidsIntStart.get(j);
                for(int i = fDepth-1; i >= 0; --i)
                {
                    Vector<String> vec = fVecMatchingSequences.get(i).fEventStart.fName;
                    if(vec.size()!= 0 && vec.get(0).equals(StackbarsAnalysis.ALLINONEEXEC))
                    {
                        childMatchingState = new MatchingStateAllInOneExecution(fVecMatchingSequences.get(i),tid);
                    }
                    else if(fVecMatchingSequences.get(i).fEventEnd.fName.size() != 0)
                    {
                        childMatchingState = new MatchingStateTwoEvents(fVecMatchingSequences.get(i),
                                childMatchingState, tid, "");
                    }
                    else
                    {
                        childMatchingState = new MatchingStateOneEvent(fVecMatchingSequences.get(i),
                                childMatchingState, tid, "");
                    }
                }

                if (childMatchingState == null)
                {
                    if(empty)
                    {
                        fBorderEventsTids.fTidsIntStart.clear();
                    }
                    return null;
                }

                // the top parents will be under the head execution
                matchingStatesTemp.add(childMatchingState);
                tidActivated.add(tid);

                childMatchingState.setParentExecution(tempHeadExecution);
            }

            if(empty)
            {
                fBorderEventsTids.fTidsIntStart.clear();
            }
            return matchingStatesTemp;
        }

    }*/

    private abstract class AbstractMatchingState{
        public abstract void parseNextEvent(ITmfEvent event, int tid, int nextTid, String string);

        protected StackbarsExecution fCurrentExecution;
        protected boolean fInExecution;
        protected AbstractMatchingState fChildMatchingState;
        protected StackbarsMatchingSequences fSequencesToMatch;
        protected StackbarsExecution fParentExecution;
        protected TidsProvider fTidsProvider;

        protected void openCurrentExecution(ITmfEvent event, String procName, int tid)
        {
            fCurrentExecution = new StackbarsExecution();
            fCurrentExecution.setStartTime(event.getTimestamp().getValue());
            fCurrentExecution.setNameStart(procName);
            fCurrentExecution.setTid(tid);
            if(fChildMatchingState != null)
            {
                fChildMatchingState.setParentExecution(fCurrentExecution);
            }
        }

        protected void setParentExecution(StackbarsExecution execution)
        {
            fParentExecution = execution;
        }

        /*protected void addStartState(ITmfEvent event)
        {
            String name = event.getType().getName();
            int state = StackbarsStateValues.STATUS_AFTER_START;
            if (name.equals(LttngStrings.SCHED_SWITCH)) {
                long prevTid = (Long) event.getContent().getField(LttngStrings.PREV_TID).getValue();
                if(prevTid == fTidStart)
                {
                    state = StackbarsStateValues.STATUS_AFTER_SCHED_SWITCH_PREV;
                }
                else
                {
                    long nextTid = (Long) event.getContent().getField(LttngStrings.NEXT_TID).getValue();
                    if(nextTid == fTidStart)
                    {
                        state = StackbarsStateValues.STATUS_AFTER_SCHED_SWITCH_NEXT;
                    }
                }
            }
            else if (name.equals(LttngStrings.SCHED_WAKEUP) || name.equals(LttngStrings.SCHED_WAKEUP_NEW))
            {

                long tid = (Long) event.getContent().getField(LttngStrings.TID).getValue();
                if(tid == fTidStart)
                {
                    state = StackbarsStateValues.STATUS_AFTER_SCHED_WAKE;
                }
            }
            fCurrentExecution.addState(new StateTimeSource(event.getTimestamp().getValue(),
                    state, event.getSource()));
        }*/

        /*protected void addEndState(ITmfEvent event)
        {
            fCurrentExecution.addState(new StateTimeSource(event.getTimestamp().getValue(),
                    StackbarsStateValues.STATUS_AFTER_END, event.getSource()));
        }*/

        /*protected void addStates(ITmfEvent event)
        {
            String name = event.getType().getName();
            if (name.equals(LttngStrings.SCHED_SWITCH)) {
                long prevTid = (Long) event.getContent().getField(LttngStrings.PREV_TID).getValue();
                if(prevTid == fTidStart)
                {
                    fCurrentExecution.addState(new StateTimeSource(event.getTimestamp().getValue(),
                            StackbarsStateValues.STATUS_AFTER_SCHED_SWITCH_PREV, event.getSource()));
                    return;
                }

                long nextTid = (Long) event.getContent().getField(LttngStrings.NEXT_TID).getValue();
                if(nextTid == fTidStart)
                {
                    fCurrentExecution.addState(new StateTimeSource(event.getTimestamp().getValue(),
                            StackbarsStateValues.STATUS_AFTER_SCHED_SWITCH_NEXT, event.getSource()));
                    return;
                }
            }
            else if (name.equals(LttngStrings.SCHED_WAKEUP) || name.equals(LttngStrings.SCHED_WAKEUP_NEW))
            {
                long tid = (Long) event.getContent().getField(LttngStrings.TID).getValue();
                if(tid == fTidStart)
                {
                    fCurrentExecution.addState(new StateTimeSource(event.getTimestamp().getValue(),
                            StackbarsStateValues.STATUS_AFTER_SCHED_WAKE, event.getSource()));
                    return;
                }
            }
        }*/

        protected void closeChildExecution()
        {
            fInExecution = false;
            if(fChildMatchingState != null)
            {
                fChildMatchingState.closeChildExecution();
            }
        }
    }

    //It will make only one execution by thread
    private class MatchingStateAllInOneExecution extends AbstractMatchingState{

        public MatchingStateAllInOneExecution(StackbarsMatchingSequences sequence, TidsProvider tidsProvider)
        {
            fSequencesToMatch = sequence;
            fInExecution = false;
            fTidsProvider = tidsProvider;
        }

        @Override
        public void parseNextEvent(ITmfEvent event, int tid, int nextTid, String procName){

            //Time filter
            for(StackbarsFilter filter : fSequencesToMatch.fFiltersTime)
            {
                if(filter.getType() == StackbarsFilterType.MAX_START_TIME)
                {
                    if((Long)filter.getValue() < event.getTimestamp().getValue())
                    {
                        return;
                    }
                }
                else // MIN_START_TIME
                {
                    if((Long)filter.getValue() > event.getTimestamp().getValue())
                    {
                        return;
                    }
                }
            }

            if(!fInExecution) // Try to match the start
            {
                if(fTidsProvider.validateTid(tid) && fTidsProvider.validateTid(nextTid))
                {
                    return;
                }

                fInExecution = true;
                if(fTidsProvider.getTids().size() != 0)
                {
                    openCurrentExecution(event, procName, fTidsProvider.getTidExec(tid));
                    //addStartState(event);
                    fParentExecution.addChild(fCurrentExecution);
                }
            }
            else
            {
                fCurrentExecution.setEndTime(event.getTimestamp().getValue());
                //addStates(event);
            }
        }
    }

    private class MatchingStateDisjoint extends AbstractMatchingState{
        private int fCurrentEventID;
        //private int fCounter; TODO
        //private int fMaxCount;
        private int[] fEventCountForFilters;
        protected String fProcNameStart;

        public MatchingStateDisjoint(StackbarsMatchingSequences sequence,
                AbstractMatchingState childMatchingState, TidsProvider tidsProvider, String procName)
        {
            fSequencesToMatch = sequence;
            fEventCountForFilters = new int[sequence.fFiltersCount.size()];
            fChildMatchingState = childMatchingState;
            fCurrentEventID = 0;
            //fCounter = 0;
            //fMaxCount = -1; //infinite
            fInExecution = false;
            fTidsProvider = tidsProvider;
            fProcNameStart = procName;
        }

        //Iterate count filters
        private StackbarsFilterStatus iterateCountFilters(ITmfEvent event, int tid)
        {
            for(int i = 0; i < fEventCountForFilters.length; ++i)
            {
                StackbarsFilter filter = fSequencesToMatch.fFiltersCount.get(i);
                if(matchStartEvent(filter.getEvent(),event, tid) != EVENT_DO_NOT_MATCH)
                {
                    if(filter.getType() == StackbarsFilterType.LESS_THAN)
                    {
                        if((Integer)filter.getValue() == 1)
                        {
                            return StackbarsFilterStatus.STATUS_WILL_NOT_MATCH;
                        }
                        /*else
                        {
                           TODO
                        }*/
                    }
                    else if(filter.getType() == StackbarsFilterType.MORE_THAN)
                    {
                        fEventCountForFilters[i] += 1;
                        if((Integer)filter.getValue() > fEventCountForFilters[i])
                        {
                            return StackbarsFilterStatus.STATUS_NOT_MATCHING_YET;
                        }
                    }
                }
            }
            return StackbarsFilterStatus.STATUS_MATCHING;
        }

        private void resetFiltersCount()
        {
            for(int i = 0; i < fEventCountForFilters.length; ++i)
            {
                fEventCountForFilters[i] = 0;
            }
        }

        @Override
        public void parseNextEvent(ITmfEvent event, int tid, int nextTid, String procName)
        {
            //TODO before and after...

            if(!fInExecution) // Try to match the start
            {
                //Start filter
                for(StackbarsFilter filter : fSequencesToMatch.fFiltersTime)
                {
                    if(filter.getType() == StackbarsFilterType.MAX_START_TIME)
                    {
                        if((Long)filter.getValue() < event.getTimestamp().getValue())
                        {
                            return;
                        }
                    }
                    else // MIN_START_TIME
                    {
                        if((Long)filter.getValue() > event.getTimestamp().getValue())
                        {
                            return;
                        }
                    }
                }

                int matchTid = matchStartEvent(fSequencesToMatch.fMatchingSequence.get(0),event, tid);
                if(matchTid != EVENT_DO_NOT_MATCH)
                {
                    fCurrentEventID = 1;
                    fInExecution = true;
                    if(!fProcNameStart.isEmpty())
                    {
                        openCurrentExecution(event, fProcNameStart, matchTid);
                    }
                    else
                    {
                        openCurrentExecution(event, procName, matchTid);
                    }
                    //addStartState(event);
                    iterateCountFilters(event, tid);
                }
            }
            else // Try to match the end
            {
                StackbarsFilterStatus filterStatus = iterateCountFilters(event, tid);

                if(filterStatus == StackbarsFilterStatus.STATUS_NOT_MATCHING_YET)
                {
                    //addStates(event);
                }
                else if (filterStatus == StackbarsFilterStatus.STATUS_WILL_NOT_MATCH)
                {
                    fInExecution = false;
                    resetFiltersCount();
                }
                else if(fSequencesToMatch.fMatchingSequence == null ||
                        fCurrentEventID == fSequencesToMatch.fMatchingSequence.size()-1)
                {
                    int matchTid = matchEndEvent(fSequencesToMatch.fMatchingSequence.get(fSequencesToMatch.fMatchingSequence.size()-1), event, tid);
                    if(matchTid != EVENT_DO_NOT_MATCH)
                    {
                        fCurrentEventID = 0;
                        fInExecution = false;
                        //addEndState(event);
                        if(fSequencesToMatch.fMatchingSequence.get(fSequencesToMatch.fMatchingSequence.size()-1).getIfNeedToMatchTid() == true)
                        {
                            closeCurrentExecution(event, fTidsProvider.getTidExec(tid), fProcNameStart);
                        }
                        else
                        {
                            closeCurrentExecution(event, fTidsProvider.getTidExec(tid), procName);
                        }
                    }
                    else
                    {
                        //addStates(event);
                    }
                }
                else // Try to match the sequence
                {
                    //addStates(event);
                    if(matchStartEvent(fSequencesToMatch.fMatchingSequence.get(fCurrentEventID), event, tid) == 100)
                    {
                        ++fCurrentEventID;
                    }
                }
            }

            if(fInExecution && fChildMatchingState != null)
            {
                fChildMatchingState.parseNextEvent(event, tid, 0, "");
            }
        }

        private void closeCurrentExecution(ITmfEvent event, int tid, String procName)
        {
            resetFiltersCount();
            fCurrentExecution.setEndTime(event.getTimestamp().getValue());
            fCurrentExecution.setTidEnd(tid);
            fCurrentExecution.setNameEnd(procName);
            fParentExecution.addChild(fCurrentExecution);
            if(fChildMatchingState != null)
            {
                fChildMatchingState.closeChildExecution();
            }
        }

        @Override
        protected void closeChildExecution()
        {
            resetFiltersCount();
            super.closeChildExecution();
        }

        private int matchStartEvent(StackbarsMatchingEvent eventBase, ITmfEvent eventToMatch, int tid)
        {
            if(!eventBase.getIfNeedToMatchTid() && !fTidsProvider.validateTid(tid))
            {
                return EVENT_DO_NOT_MATCH;
            }
            return matchEvent(eventBase, eventToMatch,fTidsProvider.getTids(), tid);
        }

        private int matchEndEvent(StackbarsMatchingEvent eventBase, ITmfEvent eventToMatch, int tid)
        {
            if(!eventBase.getIfNeedToMatchTid() && !fTidsProvider.validateTidEnd(tid))
            {
                return EVENT_DO_NOT_MATCH;
            }
            return matchEvent(eventBase, eventToMatch,fTidsProvider.getTidsEnd(), tid);
        }
    }

    private class MatchingStateContinuous extends AbstractMatchingState{
        protected String fProcNameStart;

        public MatchingStateContinuous(StackbarsMatchingSequences sequence,
                AbstractMatchingState childMatchingState, TidsProvider tidsProvider, String procName)
        {
            fSequencesToMatch = sequence;
            fChildMatchingState = childMatchingState;
            fInExecution = false;
            fProcNameStart = procName;
            fTidsProvider = tidsProvider;
        }

        @Override
        public void parseNextEvent(ITmfEvent event, int tid, int nextTid, String procName)
        {
            if(!fInExecution) // Try to match the start -> only for the first execution
            {
                //TODO else
                int tidMatch = matchNextEvent(fSequencesToMatch.fMatchingSequence.get(0),event, tid);
                if(tidMatch != EVENT_DO_NOT_MATCH)
                {
                    fInExecution = true;
                    if(!fProcNameStart.isEmpty())
                    {
                        openCurrentExecution(event, fProcNameStart, tidMatch);
                    }
                    else
                    {
                        openCurrentExecution(event, procName, tidMatch);
                    }
                }
            }
            else // Try to match the event
            {
                int tidMatch = matchNextEvent(fSequencesToMatch.fMatchingSequence.get(0), event, tid);
                if(tidMatch != EVENT_DO_NOT_MATCH)
                {
                    if(fSequencesToMatch.fMatchingSequence.get(0).getIfNeedToMatchTid() == true)
                    {
                        closeCurrentExecution(event, tidMatch, fProcNameStart);
                        if(!fProcNameStart.isEmpty())
                        {
                            openCurrentExecution(event, fProcNameStart, tidMatch);
                        }
                        else
                        {
                            openCurrentExecution(event, procName, tidMatch);
                        }
                    }
                    else
                    {
                        closeCurrentExecution(event, tid, procName);
                        if(!fProcNameStart.isEmpty())
                        {
                            openCurrentExecution(event, fProcNameStart, tidMatch);
                        }
                        else
                        {
                            openCurrentExecution(event, procName, tidMatch);
                        }
                    }
                }
            }

            if(fInExecution && fChildMatchingState != null)
            {
                fChildMatchingState.parseNextEvent(event, tid, 0, "");
            }
        }

        private void closeCurrentExecution(ITmfEvent event, int tid, String procName)
        {
            fCurrentExecution.setEndTime(event.getTimestamp().getValue());
            fCurrentExecution.setTidEnd(tid);
            fCurrentExecution.setNameEnd(procName);
            fParentExecution.addChild(fCurrentExecution);
            if(fChildMatchingState != null)
            {
                fChildMatchingState.closeChildExecution();
            }
        }

        private int matchNextEvent(StackbarsMatchingEvent eventBase, ITmfEvent eventToMatch, int tid)
        {
            if(!eventBase.getIfNeedToMatchTid() && !fTidsProvider.validateTid(tid))
            {
                return EVENT_DO_NOT_MATCH;
            }
            return matchEvent(eventBase, eventToMatch,fTidsProvider.getTids(),tid);
        }
    }

    private abstract class TidsProvider
    {
        public abstract boolean validateTid(int tid);
        public HashSet<Integer> getTidsEnd() {
            return getTids();
        }
        public boolean validateTidEnd(int tid){
            return validateTid(tid);
        }
        public abstract HashSet<Integer> getTids();
        public abstract int getTidExec(int tid);
    }

    private class TidsProviderSameTid extends TidsProvider
    {
        private int fStartTid;
        private HashSet<Integer> fSetTids;

        public TidsProviderSameTid(int tid)
        {
            fStartTid = tid;
            fSetTids = new HashSet<>();
            fSetTids.add(fStartTid);
        }

        @Override
        public boolean validateTid(int tid)
        {
            return fStartTid == tid;
        }

        @Override
        public HashSet<Integer> getTids() {
            return fSetTids;
        }

        @Override
        public int getTidExec(int tid) {
            return fStartTid;
        }
    }

    private class TidsProviderDiffTids extends TidsProvider
    {
        private HashSet<Integer> fValidTidsStart;
        private HashSet<Integer> fValidTidsEnd;

        public TidsProviderDiffTids(HashSet<Integer> validTidsStart, HashSet<Integer> validTidsEnd) {
            fValidTidsStart = validTidsStart;
            fValidTidsEnd = validTidsEnd;
        }

        public void addTidEnd(int tid) {
            fValidTidsEnd.add(tid);
        }

        public void addTidStart(int tid) {
            fValidTidsStart.add(tid);
        }

        @Override
        public boolean validateTid(int tid)
        {
            return fValidTidsStart.contains(tid);
        }

        @Override
        public HashSet<Integer> getTids()
        {
            return fValidTidsStart;
        }

        @Override
        public HashSet<Integer> getTidsEnd()
        {
            return fValidTidsEnd;
        }

        @Override
        public boolean validateTidEnd(int tid)
        {
            return fValidTidsEnd.contains(tid);
        }

        @Override
        public int getTidExec(int tid) {
            return tid;
        }
    }

    private static int matchEvent(StackbarsMatchingEvent eventBase, ITmfEvent eventToMatch, HashSet<Integer> hashSet, int currentTid)
    {
        boolean valid = false;
        String eventToMatchName = eventToMatch.getType().getName();
        for(String name : eventBase.fName)
        {
            if(name.equals(eventToMatchName))
            {
                valid = true;
            }
        }
        if(!valid)
        {
            return EVENT_DO_NOT_MATCH;
        }

        if(eventBase.fParams != null)
        {

            ITmfEventField eventField = eventToMatch.getContent();

            for(StackbarsMatchingParam paramToMatch : eventBase.fParams)
            {
                if(!paramMatch(paramToMatch, eventField))
                {
                    return EVENT_DO_NOT_MATCH;
                }
            }
        }

        if(eventBase.fParamsWithTid != null && eventBase.fParamsWithTid.size() != 0)
        {
            for(int tid : hashSet)
            {
                boolean ok = true;
                ITmfEventField eventField = eventToMatch.getContent();

                for(StackbarsMatchingParam paramToMatch : eventBase.fParamsWithTid)
                {
                    if(!paramMatchWithTid(paramToMatch, eventField, tid))
                    {
                        ok = false;
                        break;
                    }
                }
                if(ok)
                {
                    return tid;
                }
            }
            return EVENT_DO_NOT_MATCH;
        }

        return currentTid;
    }

    private static boolean paramMatchWithTid(StackbarsMatchingParam paramToMatch, ITmfEventField eventField, int tid) {
        ITmfEventField field = eventField.getField(paramToMatch.fName);

        if (field != null)
        {
            String eventValue = field.getValue().toString();
            StackbarsMatchingParamValue value = paramToMatch.fValues;
            {
                int intValue = 0;
                int intComparison = 0;

                if (value.fOperators.contains(OperatorFlag.AND))
                {
                    //TODO exception
                    intValue = Integer.parseInt(eventValue) & value.fOperatorValue;
                    intComparison = tid;
                }
                else
                {
                    //TODO exception
                    intComparison = tid;
                    intValue = Integer.parseInt(eventValue);
                }

                if(value.fOperators.contains(OperatorFlag.EQ))
                {
                    if(intComparison != intValue)
                    {
                        return false;
                    }
                }
                else if (value.fOperators.contains(OperatorFlag.NEQ))
                {
                    if(intComparison == intValue)
                    {
                        return false;
                    }
                }
            }
        } else {
            return false;
        }
        return true;
    }

    private static boolean paramMatch(StackbarsMatchingParam paramToMatch, ITmfEventField eventField) {

        ITmfEventField field = eventField.getField(paramToMatch.fName);

        if (field != null)
        {
            String eventValue = field.getValue().toString();
            StackbarsMatchingParamValue value = paramToMatch.fValues;
            {
                if (value.fOperators.contains(OperatorFlag.AND))
                {
                    //TODO exception
                    int intValue = Integer.parseInt(eventValue) & value.fOperatorValue;
                    eventValue = Integer.toString(intValue);
                }

                if(value.fOperators.contains(OperatorFlag.EQ))
                {
                    if(!eventValue.equals(value.fComparisonValue))
                    {
                        return false;
                    }
                }
                else if (value.fOperators.contains(OperatorFlag.NEQ))
                {
                    if(eventValue.equals(value.fComparisonValue))
                    {
                        return false;
                    }
                }
            }
        } else {
            return false;
        }
        return true;
    }

    /*
     * matchEvent(StackbarsMatchingEvent eventBase, ITmfEvent eventToMatch, int tidStart)
    {
        boolean valid = false;
        String eventToMatchName = eventToMatch.getType().getName();
        for(String name : eventBase.fName)
        {
            if(name.equals(eventToMatchName))
            {
                valid = true;
            }
        }
        if(!valid)
        {
            return 0;
        }

        if(eventBase.fParams != null)
        {

            ITmfEventField eventField = eventToMatch.getContent();

            for(StackbarsMatchingParam paramToMatch : eventBase.fParams)
            {
                if(!paramMatch(paramToMatch, eventField, tidStart))
                {
                    return 0;
                }
            }
        }
        return 100;
     *
     * */

    private static void getTidsUsage(StackbarsMatchingEvent eventBase, ITmfEvent eventToMatch, List<Integer> tidEQ, List<Integer> tidNEQ)
    {
        boolean valid = false;
        String eventToMatchName = eventToMatch.getType().getName();
        for(String name : eventBase.fName)
        {
            if(name.equals(eventToMatchName))
            {
                valid = true;
            }
        }
        if(!valid)
        {
            return;
        }

        ITmfEventField eventField = eventToMatch.getContent();

        for(StackbarsMatchingParam paramToMatch : eventBase.fParamsWithTid)
        {

            ITmfEventField field = eventField.getField(paramToMatch.fName);

            if (field != null)
            {
                String eventValue = field.getValue().toString();
                StackbarsMatchingParamValue value = paramToMatch.fValues;
                {
                    int intValue = 0;

                    if (value.fOperators.contains(OperatorFlag.AND))
                    {
                        //TODO exception
                        intValue = Integer.parseInt(eventValue) & value.fOperatorValue;
                    }
                    else
                    {
                        //TODO exception
                        intValue = Integer.parseInt(eventValue);
                    }

                    if(value.fOperators.contains(OperatorFlag.EQ))
                    {
                        tidEQ.add(intValue);
                    }
                    else if (value.fOperators.contains(OperatorFlag.NEQ))
                    {
                        tidNEQ.add(intValue);
                    }
                }
            }
        }
    }

    private class MatchingDefinition{
        private Vector<StackbarsMatchingSequences> fMatchingSequences;
        private BorderEventsTids fBorderEventsTids;        // Threads debut, fin par depth
        private int fDepth;
        private MatchGlobalMode fMode;

        public MatchingDefinition()
        {
            fMatchingSequences = new Vector<>();
            fBorderEventsTids = new BorderEventsTids();
            fDepth = 0;
            fMode = MatchGlobalMode.SAME_TID;
        }

        public int getMaxDepth()
        {
            return fDepth;
        }

        public MatchGlobalMode getMode()
        {
            return fMode;
        }

    }

    public abstract static class ExecDefinition{
        public List<BorderEvents> fBorderEventsByDepth;
        public ExecDefinition()
        {
            fBorderEventsByDepth = new ArrayList<>();
        }
    }

    public static class ExecDefinitionSameTid extends ExecDefinition
    {
        public String tids;
        public ExecDefinitionSameTid()
        {
            tids = "";
        }
    }

    public static class ExecDefinitionDiffTids extends ExecDefinition
    {
        public String tidsStart;
        public String tidsEnd;
        public ExecDefinitionDiffTids()
        {
            tidsStart = "";
            tidsEnd = "";
        }
    }

    public static class BorderEvents{
        public ArrayList<EventDefinition> fEventDefinitions;
        public String fDeadline;
        public boolean fContinuous;
        public BorderEvents()
        {
            fEventDefinitions = new ArrayList<>();
            fDeadline = "";
            fContinuous = false;
        }
        public BorderEvents(BorderEvents borderEvents) {
            fEventDefinitions = new ArrayList<>();
            for(EventDefinition ed : borderEvents.fEventDefinitions)
            {
                fEventDefinitions.add(new EventDefinition(ed));
            }
            this.fDeadline = borderEvents.fDeadline;
        }

        public EventDefinition getStartEvent()
        {
            if(fEventDefinitions == null)
            {
                fEventDefinitions = new ArrayList<>();
            }
            if(fEventDefinitions.size() == 0)
            {
                fEventDefinitions.add(new EventDefinition("",""));
            }
            return fEventDefinitions.get(0);
        }

        public EventDefinition getEndEvent()
        {
            if(fEventDefinitions == null)
            {
                fEventDefinitions = new ArrayList<>();
            }
            if(fEventDefinitions.size() == 0)
            {
                fEventDefinitions.add(new EventDefinition("",""));
            }
            return fEventDefinitions.get(fEventDefinitions.size()-1);
        }
    }

    public static class EventDefinition{
        public EventDefinition(String name, String params) {
            eventName = name;
            eventParams = params;
        }
        public EventDefinition(EventDefinition ed) {
            this.eventName = ed.eventName;
            this.eventParams = ed.eventParams;
        }
        public String eventName;
        public String eventParams;
    }

    private static class Param_NameValuesCount{
        private String fName;
        private HashMap<String, Integer> fValuesCountMap;

        public Param_NameValuesCount(String name)
        {
            fName = name;
            fValuesCountMap = new HashMap<>();
        }

        public String getName()
        {
            return fName;
        }

        public HashMap<String, Integer> getMap()
        {
            return fValuesCountMap;
        }
    }

    private static class Event_CountParams{
        private int fEventCount;
        private Param_NameValuesCount[] fParams;

        public Event_CountParams(int eventCount, Param_NameValuesCount[] params)
        {
            fEventCount = eventCount;
            fParams = params;
        }

        public int getCount()
        {
            return fEventCount;
        }

        public Param_NameValuesCount[] getParams()
        {
            return fParams;
        }

        public void incrementCount() {
            ++fEventCount;
        }
    }

    private static HashMap<String, Event_CountParams> calculateMap(StackbarsExecution exec, final IProgressMonitor monitor, ITmfTrace trace){
      //1) Get the valid executions
            final HashMap<String, Event_CountParams> map = new HashMap<>();

            System.out.print(exec + "" + monitor + "" + trace);

            //--------------------------
            //
            //TODO !!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!
            // I just scrap this method to support multiple tid, but it is useful
            //
            //--------------------------------

            //StateTimeSource nextState = exec.getStates().get(0);
            for(int i = 1; i < 0/*exec.getStates().size()*/; ++i)
            {
                //final StateTimeSource state = nextState;
                //nextState = exec.getStates().get(i);
                boolean needToRequest = false;
                TmfTimeRange range = TmfTimeRange.NULL_RANGE;

                /*switch (state.getState())
                {
                    //We care only of the first event
                    case StackbarsStateValues.STATUS_AFTER_SCHED_WAKE:
                        needToRequest = true;
                        range = new TmfTimeRange(
                                new TmfTimestamp(state.getTime(), ITmfTimestamp.NANOSECOND_SCALE),
                                new TmfTimestamp(state.getTime(), ITmfTimestamp.NANOSECOND_SCALE));
                        break;

                    //We care
                    case StackbarsStateValues.STATUS_AFTER_START:
                    case StackbarsStateValues.STATUS_AFTER_SCHED_SWITCH_NEXT:
                        needToRequest = true;
                        range = new TmfTimeRange(
                                new TmfTimestamp(state.getTime(), ITmfTimestamp.NANOSECOND_SCALE),
                                new TmfTimestamp(nextState.getTime(), ITmfTimestamp.NANOSECOND_SCALE));
                        break;

                    //We don't care
                    case StackbarsStateValues.STATUS_AFTER_END:
                    case StackbarsStateValues.STATUS_AFTER_SCHED_SWITCH_PREV:
                    default:
                        break;

                }*/

                if(needToRequest)
                {
                    TmfEventRequest request;
                    request = new TmfEventRequest(ITmfEvent.class, range,
                            0,
                            ITmfEventRequest.ALL_DATA,
                            TmfEventRequest.ExecutionType.FOREGROUND)
                    {

                            @Override
                            public void handleData(ITmfEvent event) {
                                // If the job is canceled, cancel the request so waitForCompletion() will unlock
                                if (monitor.isCanceled()) {
                                    cancel();
                                    return;
                                }

                                if(event.getSource().equals(0/*state.getSource()*/))
                                {
                                    String eventName = event.getType().getName();
                                    Event_CountParams value = map.get(eventName);
                                    ITmfEventField content = event.getContent();

                                    //Count each event occurrence
                                    if(value == null)
                                    {
                                        Param_NameValuesCount[] params = null;

                                        if (content != null) {
                                            int count = content.getFields().size();
                                            params = new Param_NameValuesCount[count];
                                            for (ITmfEventField field: content.getFields()) {
                                                params[--count] = new Param_NameValuesCount(field.getName());
                                            }
                                        }
                                        value = new Event_CountParams(1, params);
                                        map.put(eventName, value);
                                    }
                                    else
                                    {
                                        value.incrementCount();
                                    }

                                    //Count for each parameters, each value occurrence
                                    if (content != null) {
                                        Param_NameValuesCount[] array = value.getParams();
                                        for (Param_NameValuesCount param : array) {
                                            ITmfEventField field = content.getField(param.getName());
                                            Integer countParam = param.getMap().get(field.getValue().toString());
                                            if(countParam == null)
                                            {
                                                countParam = 0;
                                            }
                                            param.getMap().put(field.getValue().toString(), ++countParam);
                                        }
                                    }
                                }
                            }
                    };

                    ((ITmfEventProvider) trace).sendRequest(request);
                    try {
                        request.waitForCompletion();
                    } catch (InterruptedException e) {

                    }
                }
            }

            return map;
    }

    public Vector<StackbarsFilter> getPossibleFilters(final ITmfTrace trace, final int depth, final StackbarsExecution executionInvalid, IProgressMonitor monitor) {
        // TODO Auto-generated method stub

        if(fMatchingDefinition.fMatchingSequences.get(depth).fValidExecutions.size() == 0)
        {
            return null;
        }

        Vector<HashMap<String, Event_CountParams>> mapsValid = new Vector<>();

        //1) Get the valid executions
        for(StackbarsExecution exec: fMatchingDefinition.fMatchingSequences.get(depth).fValidExecutions)
        {
            final HashMap<String, Event_CountParams> map = calculateMap(exec, monitor, trace);
            mapsValid.add(map);
            if (monitor.isCanceled()) {
                return null;
            }
        }

        //1.2) Parse the invalid execution
        HashMap<String, Event_CountParams> mapInvalid = calculateMap(executionInvalid, monitor, trace);

        //2) Count each event
        //2.1) Get the lowest valid event count to do less work
        HashMap<String, Event_CountParams> minMap = mapsValid.firstElement();
        int minCount = Integer.MAX_VALUE;
        for(HashMap<String, Event_CountParams> map : mapsValid)
        {
            if (map.size() < minCount)
            {
                minMap = map;
            }
        }

        //Collect filters
        Vector<StackbarsFilter> filters = new Vector<>();

        //2.2) for each event, check in the invalid the count
        HashMap<String, Event_CountParams> eventsInBoth = new HashMap<>();
        for(Entry<String, Event_CountParams> entryMinMap : minMap.entrySet())
        {
            if (monitor.isCanceled()) {
                return null;
            }
            int countInvalid = 0;
            Event_CountParams eventInvalid = mapInvalid.remove(entryMinMap.getKey());
            if(eventInvalid != null)
            {
                countInvalid = eventInvalid.getCount();
            }

            int countValid = entryMinMap.getValue().getCount();

            //2.3) if invalid > valid1, check max in valid -> if invalid > max Valid add filter
            if(countInvalid > countValid)
            {
                int maxCount = countValid;
                for(HashMap<String, Event_CountParams> map : mapsValid)
                {
                    if(map != minMap)
                    {
                        int tempCount = map.get(entryMinMap.getKey()).getCount();
                        if(tempCount > maxCount)
                        {
                            maxCount = tempCount;
                        }
                    }
                }

                if(countInvalid > maxCount)
                {
                    StackbarsFilter filter = new StackbarsFilter();
                    filter.setType(StackbarsFilterType.LESS_THAN);
                    filter.setValue(maxCount + 1);
                    Vector<String> filterEventName = new Vector<>();
                    filterEventName.add(entryMinMap.getKey());
                    filter.setEvent(new StackbarsMatchingEvent(filterEventName, null, null));
                    filters.add(filter);
                    if(countInvalid - 1 != maxCount)
                    {
                        filter = new StackbarsFilter();
                        filter.setType(StackbarsFilterType.LESS_THAN);
                        filter.setValue(countInvalid);
                        filterEventName = new Vector<>();
                        filterEventName.add(entryMinMap.getKey());
                        filter.setEvent(new StackbarsMatchingEvent(filterEventName, null, null));
                        filters.add(filter);
                    }
                }

                if(countInvalid != 0 && maxCount != 0)
                {
                    eventsInBoth.put(entryMinMap.getKey(), eventInvalid);
                }
            }
            //2.4) if valid1 > invalid, check min in valid -> if min Valid > invalid add filter
            else if (countInvalid < countValid)
            {
                int minimumCount = countValid;
                for(HashMap<String, Event_CountParams> map : mapsValid)
                {
                    if(map != minMap)
                    {
                        int tempCount = map.get(entryMinMap.getKey()).getCount();
                        if(tempCount < minimumCount)
                        {
                            minimumCount = tempCount;
                        }
                    }
                }

                if(countInvalid < minimumCount)
                {
                    StackbarsFilter filter = new StackbarsFilter();
                    filter.setType(StackbarsFilterType.MORE_THAN);
                    filter.setValue(minimumCount - 1);
                    Vector<String> filterEventName = new Vector<>();
                    filterEventName.add(entryMinMap.getKey());
                    filter.setEvent(new StackbarsMatchingEvent(filterEventName, null, null));
                    filters.add(filter);
                    if(countInvalid + 1 != minimumCount)
                    {
                        filter = new StackbarsFilter();
                        filter.setType(StackbarsFilterType.MORE_THAN);
                        filter.setValue(countInvalid);
                        filterEventName = new Vector<>();
                        filterEventName.add(entryMinMap.getKey());
                        filter.setEvent(new StackbarsMatchingEvent(filterEventName, null, null));
                        filters.add(filter);
                    }
                }

                if(countInvalid != 0 && minimumCount != 0)
                {
                    eventsInBoth.put(entryMinMap.getKey(), eventInvalid);
                }
            }
            else
            {
                if(countInvalid != 0 && countValid != 0)
                {
                    eventsInBoth.put(entryMinMap.getKey(), eventInvalid);
                }
            }
        }

        //2.5) check in invalid the non-checked event -> add filter
        for(Entry<String, Event_CountParams> entry : mapInvalid.entrySet())
        {
            int countInvalid = entry.getValue().getCount();

            //if invalid > max Valid add filter
            int maxCount = 0;
            for(HashMap<String, Event_CountParams> map : mapsValid)
            {
                if(map != minMap)
                {
                    Event_CountParams ecp = map.get(entry.getKey());
                    if(ecp != null)
                    {
                        int tempCount = ecp.getCount();
                        if(tempCount > maxCount)
                        {
                            maxCount = tempCount;
                        }
                    }
                }
            }

            if(countInvalid > maxCount)
            {
                StackbarsFilter filter = new StackbarsFilter();
                filter.setType(StackbarsFilterType.LESS_THAN);
                filter.setValue(maxCount + 1);
                Vector<String> filterEventName = new Vector<>();
                filterEventName.add(entry.getKey());
                filter.setEvent(new StackbarsMatchingEvent(filterEventName, null, null));
                filters.add(filter);
                if(countInvalid - 1 != maxCount)
                {
                    filter = new StackbarsFilter();
                    filter.setType(StackbarsFilterType.LESS_THAN);
                    filter.setValue(countInvalid);
                    filterEventName = new Vector<>();
                    filterEventName.add(entry.getKey());
                    filter.setEvent(new StackbarsMatchingEvent(filterEventName, null, null));
                    filters.add(filter);
                }
            }
        }

        //3) get start - end time
        //3.1) check max start, min start in valid
        if (monitor.isCanceled()) {
            return null;
        }
        long minStartTime = Integer.MAX_VALUE;
        long maxStartTime = Integer.MIN_VALUE;
        for(StackbarsExecution exec: fMatchingDefinition.fMatchingSequences.get(depth).fValidExecutions)
        {
            long tempStart = exec.getStartTime();
            if(tempStart < minStartTime)
            {
                minStartTime = tempStart;
            }
            if(tempStart > maxStartTime)
            {
                maxStartTime = tempStart;
            }
        }

        //3.2) if min start valid > start invalid -> add filter
        if(minStartTime > executionInvalid.getStartTime())
        {
            StackbarsFilter filter = new StackbarsFilter();
            filter.setType(StackbarsFilterType.MIN_START_TIME);
            filter.setValue(minStartTime);
            filters.add(filter);
            filter = new StackbarsFilter();
            filter.setType(StackbarsFilterType.MIN_START_TIME);
            filter.setValue(executionInvalid.getStartTime() + 1);
            filters.add(filter);
        }

        //3.3) if max start valid < start invalid -> add filter
        if(maxStartTime < executionInvalid.getStartTime())
        {
            StackbarsFilter filter = new StackbarsFilter();
            filter.setType(StackbarsFilterType.MAX_START_TIME);
            filter.setValue(maxStartTime);
            filters.add(filter);
            filter = new StackbarsFilter();
            filter.setType(StackbarsFilterType.MAX_START_TIME);
            filter.setValue(executionInvalid.getStartTime() - 1);
            filters.add(filter);
        }

        //4) get for each pair of 2 events with the order ??

        //5) check events parameters
        for(Entry<String, Event_CountParams> eventInBoth : eventsInBoth.entrySet())
        {
            if (monitor.isCanceled()) {
                return null;
            }
            HashMap<String,HashMap<String,MinMax>> totalParams = new HashMap<>();
            for(HashMap<String, Event_CountParams> map : mapsValid)
            {
                for(Param_NameValuesCount currentParam : map.get(eventInBoth.getKey()).getParams())
                {
                    HashMap<String, MinMax> mapValue = totalParams.get(currentParam.getName());
                    if(mapValue == null)
                    {
                        //First iteration
                        mapValue = new HashMap<>();
                        for(Entry<String, Integer> currentValue : currentParam.getMap().entrySet())
                        {
                            mapValue.put(currentValue.getKey(), new MinMax(currentValue.getValue(), currentValue.getValue()));
                        }
                        totalParams.put(currentParam.getName(), mapValue);
                    }
                    else
                    {
                        //other iteration
                        for(Entry<String, MinMax> totalValue : mapValue.entrySet())
                        {
                            //if not there, put it to 0
                            Integer result = currentParam.getMap().get(totalValue.getKey());
                            if(result == null)
                            {
                                totalValue.getValue().min = 0;
                            }
                            else
                            {
                                //check for min and max
                                totalValue.getValue().min = Math.min(totalValue.getValue().min, result);
                                totalValue.getValue().max = Math.min(totalValue.getValue().max, result);
                                currentParam.getMap().remove(totalValue.getKey());
                            }

                        }
                        for(Entry<String, Integer> currentValue : currentParam.getMap().entrySet())
                        {
                            //max = current, min = 0
                            mapValue.put(currentValue.getKey(), new MinMax(0, currentValue.getValue()));
                        }
                    }
                }
            }

            //5.1) for each events in invalid, check in all valid until it match -> if not, add filter !=
            //5.2) also check if all valid have the same -> add filter count 1 with == in parameter
            for(Param_NameValuesCount invalidParamValues : eventInBoth.getValue().getParams())
            {

                HashMap<String, MinMax> params = totalParams.get(invalidParamValues.getName());
                if(params == null)
                {
                    //strange...
                    continue;
                }

                for(Entry<String, MinMax> validValues : params.entrySet())
                {
                    Integer countValue = invalidParamValues.getMap().remove(validValues.getKey());
                    if(countValue == null || countValue < validValues.getValue().min)
                    {
                        if(validValues.getValue().min != 0)
                        {
                            StackbarsFilter filter = new StackbarsFilter();
                            filter.setType(StackbarsFilterType.MORE_THAN);
                            filter.setValue(validValues.getValue().min - 1);
                            Vector<String> nameEventFilter = new Vector<>();
                            nameEventFilter.add(eventInBoth.getKey());
                            Vector<StackbarsMatchingParam> paramsFilter = new Vector<>();
                            paramsFilter.add(new StackbarsMatchingParam(invalidParamValues.getName(), new StackbarsMatchingParamValue(validValues.getKey(), EnumSet.of(OperatorFlag.EQ), -1)));
                            filter.setEvent(new StackbarsMatchingEvent(nameEventFilter, paramsFilter, null));
                            filters.add(filter);
                        }
                    }
                    else if(countValue > validValues.getValue().max)
                    {
                        StackbarsFilter filter = new StackbarsFilter();
                        filter.setType(StackbarsFilterType.LESS_THAN);
                        filter.setValue(validValues.getValue().max);
                        Vector<String> nameEventFilter = new Vector<>();
                        nameEventFilter.add(eventInBoth.getKey());
                        Vector<StackbarsMatchingParam> paramsFilter = new Vector<>();
                        paramsFilter.add(new StackbarsMatchingParam(invalidParamValues.getName(), new StackbarsMatchingParamValue(validValues.getKey(), EnumSet.of(OperatorFlag.EQ), -1)));
                        filter.setEvent(new StackbarsMatchingEvent(nameEventFilter, paramsFilter, null));
                        filters.add(filter);
                    }
                }

                if(invalidParamValues.getMap().size() < 3) //TODO 3?
                {
                    for(Entry<String, Integer> invalidValue : invalidParamValues.getMap().entrySet()){
                        StackbarsFilter filter = new StackbarsFilter();
                        filter.setType(StackbarsFilterType.LESS_THAN);
                        filter.setValue(invalidValue.getValue());
                        Vector<String> nameEventFilter = new Vector<>();
                        nameEventFilter.add(eventInBoth.getKey());
                        Vector<StackbarsMatchingParam> paramsFilter = new Vector<>();
                        paramsFilter.add(new StackbarsMatchingParam(invalidParamValues.getName(), new StackbarsMatchingParamValue(invalidValue.getKey(), EnumSet.of(OperatorFlag.EQ), -1)));
                        filter.setEvent(new StackbarsMatchingEvent(nameEventFilter, paramsFilter, null));
                        filters.add(filter);
                    }
                }
            }
        }

        System.out.println(depth + " " + executionInvalid);

        return filters;
    }

    private static class MinMax
    {
        public MinMax(Integer value, Integer value2) {
            min = value;
            max = value2;
        }
        public int min;
        public int max;
    }

    public void addNewFilters(Vector<StackbarsFilter> filters, int depth) {
        for(StackbarsFilter filter : filters)
        {
            if(filter.getType() == StackbarsFilterType.MAX_START_TIME || filter.getType() == StackbarsFilterType.MIN_START_TIME)
            {
                fMatchingDefinition.fMatchingSequences.get(depth).fFiltersTime.add(filter);
            }
            else if (filter.getType() == StackbarsFilterType.LESS_THAN || filter.getType() == StackbarsFilterType.MORE_THAN)
            {
                fMatchingDefinition.fMatchingSequences.get(depth).fFiltersCount.add(filter);
            }
        }
    }

    public void selectValidExecution(int depth, StackbarsExecution stackbarsExecution) {
        fMatchingDefinition.fMatchingSequences.get(depth).fValidExecutions.add(stackbarsExecution);
    }
}
