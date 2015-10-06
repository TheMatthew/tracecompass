///*******************************************************************************
// * Copyright (c) 2012, 2014 Ericsson
// * Copyright (c) 2010, 2011 École Polytechnique de Montréal
// * Copyright (c) 2010, 2011 Alexandre Montplaisir <alexandre.montplaisir@gmail.com>
// *
// * All rights reserved. This program and the accompanying materials are
// * made available under the terms of the Eclipse Public License v1.0 which
// * accompanies this distribution, and is available at
// * http://www.eclipse.org/legal/epl-v10.html
// *
// *******************************************************************************/
//
//package org.eclipse.linuxtools.internal.lttng2.kernel.ui.stackbars;
//
//import java.util.ArrayList;
//import java.util.HashMap;
//import java.util.List;
//import java.util.Map;
//
//import org.eclipse.linuxtools.internal.lttng2.kernel.core.Attributes;
//import org.eclipse.linuxtools.internal.lttng2.kernel.core.LttngStrings;
//import org.eclipse.linuxtools.internal.lttng2.kernel.ui.stackbars.StackbarsModelProvider.StackChartEvent;
//import org.eclipse.linuxtools.internal.lttng2.kernel.ui.stackbars.StackbarsModelProvider.StackChartParam;
//import org.eclipse.linuxtools.statesystem.core.ITmfStateSystemBuilder;
//import org.eclipse.linuxtools.statesystem.core.exceptions.AttributeNotFoundException;
//import org.eclipse.linuxtools.statesystem.core.exceptions.StateValueTypeException;
//import org.eclipse.linuxtools.statesystem.core.exceptions.TimeRangeException;
//import org.eclipse.linuxtools.statesystem.core.statevalue.ITmfStateValue;
//import org.eclipse.linuxtools.statesystem.core.statevalue.TmfStateValue;
//import org.eclipse.linuxtools.tmf.core.event.ITmfEvent;
//import org.eclipse.linuxtools.tmf.core.event.ITmfEventField;
//import org.eclipse.linuxtools.tmf.core.statesystem.AbstractTmfStateProvider;
//import org.eclipse.linuxtools.tmf.core.trace.ITmfTrace;
//
///**
// * This is the state change input plugin for TMF's state system which handles
// * the LTTng 2.0 kernel traces in CTF format.
// *
// * It uses the reference handler defined in CTFKernelHandler.java.
// *
// * @author alexmont, MC
// *
// */
//public class StackbarsStateProvider extends AbstractTmfStateProvider {
//
//    /**
//     * Version number of this state provider. Please bump this if you modify the
//     * contents of the generated state history in some way.
//     */
//    private static final int VERSION = 4;
//    private static final String NEXT_PRIO = "next_prio";
//    public static final String ATTRIB_PRIORITY = "priority";
//    private List<StackbarsModelProvider.StackChartEvent> fStartEvents;
//    private List<StackbarsModelProvider.StackChartEvent> fEndEvents;
//    private List<Boolean> fEndEventsDefined;
//    private List<Boolean> fStartEventsDefined;
//    private int fMaxExecutionDepth;
//
//    private Map<Integer, StackbarsNode> fMapCurrentExecutionByTid;
//
//    //private StackChart fStackChart;
//    boolean fValid = false;
//
//    private static String NO_CUSTOM_EVENTS = ""; //$NON-NLS-1$
//
//    // ------------------------------------------------------------------------
//    // Constructor
//    // ------------------------------------------------------------------------
//
//    /**
//     * Instantiate a new state provider plugin.
//     *
//     * @param trace
//     *            The LTTng 2.0 kernel trace directory
//     */
//    public StackbarsStateProvider(ITmfTrace trace) {
//        super(trace, ITmfEvent.class, "Stackbars"); //$NON-NLS-1$
//        fMapCurrentExecutionByTid = new HashMap<>();
//        fEndEventsDefined = new ArrayList<>();
//        fStartEventsDefined = new ArrayList<>();
//        fEndEventsDefined.add(0,false);
//        fStartEventsDefined.add(0,false);
//        fMaxExecutionDepth = 1;
//    }
//
//    // ------------------------------------------------------------------------
//    // IStateChangeInput
//    // ------------------------------------------------------------------------
//
//    @Override
//    public int getVersion() {
//        return VERSION;
//    }
//
//    @Override
//    public void assignTargetStateSystem(ITmfStateSystemBuilder ssb) {
//        /* We can only set up the locations once the state system is assigned */
//        super.assignTargetStateSystem(ssb);
//    }
//
//    @Override
//    public StackbarsStateProvider getNewInstance() {
//        return new StackbarsStateProvider(this.getTrace());
//    }
//
//    @Override
//    protected void eventHandle(ITmfEvent event) {
//        /*
//         * AbstractStateChangeInput should have already checked for the correct
//         * class type
//         */
//
//        final String eventName = event.getType().getName();
//        final ITmfEventField eventField = event.getContent();
//        final long ts = event.getTimestamp().getValue();
//
//        try {
//            /* Shortcut for the "current CPU" attribute node */
//            final Integer currentCPUNode = ss.getQuarkRelativeAndAdd(getNodeCPUs(), event.getSource());
//
//            /*
//             * Shortcut for the "current thread" attribute node. It requires
//             * querying the current CPU's current thread.
//             */
//            int quark = ss.getQuarkRelativeAndAdd(currentCPUNode, Attributes.CURRENT_THREAD);
//            ITmfStateValue value = ss.queryOngoingState(quark);
//            int threadId = value.isNull() ? -1 : value.unboxInt();
//
//            /*
//             * Feed event to the history system if it's known to cause a state
//             * transition.
//             */
//            processEventsIteration(0, eventName, eventField, ts, threadId);
//
//            if (eventName.equals(LttngStrings.SCHED_SWITCH)) {
//
//                /* Set the current scheduled process on the relevant CPU for the next iteration */
//                long nextTid = (Long) eventField.getField(LttngStrings.NEXT_TID).getValue();
//                quark = ss.getQuarkRelativeAndAdd(currentCPUNode, Attributes.CURRENT_THREAD);
//                value = TmfStateValue.newValueInt(safeLongToInt(nextTid));
//                ss.modifyAttribute(ts, value, quark);
//
//                // We also need the priority
//                long nextPrio = (Long) eventField.getField(NEXT_PRIO).getValue();
//                quark = ss.getQuarkRelativeAndAdd(quark, ATTRIB_PRIORITY);
//                value = TmfStateValue.newValueInt(safeLongToInt(nextPrio));
//                ss.modifyAttribute(ts, value, quark);
//
//            }
//
//        } catch (AttributeNotFoundException ae) {
//            /*
//             * This would indicate a problem with the logic of the manager here,
//             * so it shouldn't happen.
//             */
//            ae.printStackTrace();
//
//        } catch (TimeRangeException tre) {
//            /*
//             * This would happen if the events in the trace aren't ordered
//             * chronologically, which should never be the case ...
//             */
//            System.err.println("TimeRangeExcpetion caught in the state system's event manager."); //$NON-NLS-1$
//            System.err.println("Are the events in the trace correctly ordered?"); //$NON-NLS-1$
//            tre.printStackTrace();
//
//        } catch (StateValueTypeException sve) {
//            /*
//             * This would happen if we were trying to push/pop attributes not of
//             * type integer. Which, once again, should never happen.
//             */
//            sve.printStackTrace();
//        }
//    }
//
//    private void processStartEventIteration(int depth, String eventName, ITmfEventField eventField, long ts, int quarkCurrentParent, int threadId, int quarkCurrentNext) throws StateValueTypeException, AttributeNotFoundException
//    {
//        int quark;
//        ITmfStateValue value;
//
//        // If we have a custom starting event
//        if(fStartEventsDefined.get(depth))
//        {
//            // If this is the starting event
//            if(eventName.equals(fStartEvents.get(depth).name))
//            {
//                boolean valid = true;
//                // Try to match the parameters
//                if(fStartEvents.get(depth).params != null)
//                {
//                    for(StackChartParam param : fStartEvents.get(depth).params)
//                    {
//                        ITmfEventField field = eventField.getField(param.name);
//                        if (field != null)
//                        {
//                            if(!field.getValue().toString().equals(param.value))
//                            {
//                                valid = false;
//                                break;
//                            }
//                        }
//                        else
//                        {
//                            valid = false;
//                            break;
//                        }
//                    }
//                }
//                if(valid)
//                {
//                    int executionId;
//                    //update oldexecution if no endEvent
//                    if(!fEndEventsDefined.get(depth))
//                    {
//                        executionId = getExecutionIdAndActionOthers(threadId, depth);
//                        if(executionId >= 0)
//                        {
//                            quark = ss.getQuarkRelativeAndAdd(quarkCurrentParent, Integer.toString(executionId));
//                            value = StackbarsStateValues.STATUS_AFTER_START_VALUE;
//                            ss.modifyAttribute(ts, value, quark);
//                        }
//                    }
//
//                    //new execution
//                    executionId = getExecutionIdAndActionStart(threadId, depth);
//                    if(executionId >= 0)
//                    {
//                        quark = ss.getQuarkRelativeAndAdd(quarkCurrentParent, Integer.toString(executionId));
//                        value = StackbarsStateValues.STATUS_AFTER_START_VALUE;
//                        ss.modifyAttribute(ts, value, quark);
//                    }
//                }
//            }
//        }
//        // If we have not a custom starting event
//        else {
//            if (eventName.equals(LttngStrings.SCHED_WAKEUP) || eventName.equals(LttngStrings.SCHED_WAKEUP_NEW)) {
//                Long tid = (Long) eventField.getField(LttngStrings.TID).getValue();
//                int intTid = safeLongToInt(tid);
//
//                //new execution for the thread
//                int executionId = getExecutionIdAndActionStart(intTid, depth);
//                if(executionId >= 0)
//                {
//                    quark = ss.getQuarkRelativeAndAdd(quarkCurrentNext, Integer.toString(executionId));
//                    value = StackbarsStateValues.STATUS_AFTER_WAKE_VALUE;
//                    ss.modifyAttribute(ts, value, quark);
//                }
//            }
//        }
//    }
//
//    private void processEndEventIteration(int depth, String eventName, ITmfEventField eventField, long ts, int quarkCurrentParent, int threadId, int nextThreadNode) throws StateValueTypeException, AttributeNotFoundException
//    {
//        boolean modifState = false;
//        int quark;
//        ITmfStateValue value;
//        if (fEndEventsDefined.get(depth))
//        {
//            if (eventName.equals(fEndEvents.get(depth).name))
//            {
//                boolean valid = true;
//                // Try to match the parameters
//                if(fEndEvents.get(depth).params != null)
//                {
//                    for(StackChartParam param : fEndEvents.get(depth).params)
//                    {
//                        ITmfEventField field = eventField.getField(param.name);
//                        if (field != null)
//                        {
//                            if(!field.getValue().toString().equals(param.value))
//                            {
//                                valid = false;
//                                break;
//                            }
//                        }
//                    }
//                }
//                if(valid)
//                {
//                    modifState = true;
//                    //end of execution
//                    int executionId = getExecutionIdAndActionEnd(threadId, depth);
//                    if(executionId >= 0)
//                    {
//                        quark = ss.getQuarkRelativeAndAdd(quarkCurrentParent, Integer.toString(executionId));
//                        value = StackbarsStateValues.STATUS_AFTER_END_VALUE;
//                        ss.modifyAttribute(ts, value, quark);
//                    }
//                }
//            }
//        }
//
//        if (eventName.equals(LttngStrings.SCHED_SWITCH)) {
//
//            long prevTid = (Long) eventField.getField(LttngStrings.PREV_TID).getValue();
//            long nextTid = (Long) eventField.getField(LttngStrings.NEXT_TID).getValue();
//            //long prevState = (Long) eventField.getField("prev_state").getValue(); //$NON-NLS-1$
//            /*Integer prevPrio = (Integer) eventField.getField("prev_prio").getValue(); //$NON-NLS-1$
//            Integer nextPrio = (Integer) eventField.getField("next_prio").getValue(); //$NON-NLS-1$*/
//            int executionId;
//
//            if (!modifState && prevTid == threadId)
//            {
//                /*threadId = safeLongToInt(prevTid);
//                currentThreadNode = ss.getQuarkRelativeAndAdd(getNodeThreads(), String.valueOf(threadId));*/
//
//                //Last tid
//                executionId = getExecutionIdAndActionOthers(threadId, depth);
//                if(executionId >= 0)
//                {
//                    quark = ss.getQuarkRelativeAndAdd(quarkCurrentParent, Integer.toString(executionId));
//                    value = StackbarsStateValues.STATUS_AFTER_SWITCH_PREV_VALUE;
//                    ss.modifyAttribute(ts, value, quark);
//                }
//            }
//
//            //set the next tid to running
//            executionId = getExecutionIdAndActionOthers(safeLongToInt(nextTid), depth);
//            if(executionId >= 0)
//            {
//                quark = ss.getQuarkRelativeAndAdd(nextThreadNode, Integer.toString(executionId));
//                ss.modifyAttribute(ts, StackbarsStateValues.STATUS_AFTER_SWITCH_NEXT_VALUE, quark);
//            }
//        }
//    }
//
//    private void processEventsIteration(int startDepth, String eventName, ITmfEventField eventField, long ts, int threadId) throws StateValueTypeException, AttributeNotFoundException
//    {
//        // TODO find a way to avoid calculating for each event
//        // TODO some fields better modal
//
//        int currentThreadNode = ss.getQuarkRelativeAndAdd(getNodeThreads(), String.valueOf(threadId));
//        StackbarsNode node = fMapCurrentExecutionByTid.get(threadId);
//        for(int i = 0; i < startDepth;++i)
//        {
//            if(node != null)
//            {
//                currentThreadNode = ss.getQuarkRelativeAndAdd(currentThreadNode, String.valueOf(node.fId));
//                node = node.fCurrentChildNode;
//            }
//        }
//
//        // Update quark for start event
//        int newThreadNode = 0;
//        int nextThreadNode = 0;
//        if (eventName.equals(LttngStrings.SCHED_WAKEUP) || eventName.equals(LttngStrings.SCHED_WAKEUP_NEW)) {
//            Long tid = (Long) eventField.getField(LttngStrings.TID).getValue();
//            int intTid = safeLongToInt(tid);
//            newThreadNode = ss.getQuarkRelativeAndAdd(getNodeThreads(), String.valueOf(intTid));
//            node = fMapCurrentExecutionByTid.get(intTid);
//            for(int i = 0; i < startDepth;++i)
//            {
//                if(node != null)
//                {
//                    newThreadNode = ss.getQuarkRelativeAndAdd(newThreadNode, String.valueOf(node.fId));
//                    node = node.fCurrentChildNode;
//                }
//            }
//        }
//
//        // Process start event
//        processStartEventIteration(startDepth, eventName, eventField, ts, currentThreadNode, threadId, newThreadNode);
//
//        // Process recursively nested execution
//        if(startDepth < fMaxExecutionDepth - 1)
//        {
//            processEventsIteration(startDepth + 1, eventName, eventField, ts, threadId);
//        }
//
//        // Update quark for end event
//        if (eventName.equals(LttngStrings.SCHED_SWITCH)) {
//            long nextTid = (Long) eventField.getField(LttngStrings.NEXT_TID).getValue();
//            nextThreadNode = ss.getQuarkRelativeAndAdd(getNodeThreads(), String.valueOf(nextTid));
//            node = fMapCurrentExecutionByTid.get(safeLongToInt(nextTid));
//            for(int i = 0; i < startDepth;++i)
//            {
//                if(node != null)
//                {
//                    nextThreadNode = ss.getQuarkRelativeAndAdd(nextThreadNode, String.valueOf(node.fId));
//                    node = node.fCurrentChildNode;
//                }
//            }
//        }
//
//        // Process end event
//        processEndEventIteration(startDepth, eventName, eventField, ts, currentThreadNode, threadId, nextThreadNode);
//    }
//
//    // ------------------------------------------------------------------------
//    // Convenience methods for commonly-used attribute tree locations
//    // ------------------------------------------------------------------------
//
//    /*
//     * If endEvent is defined, the modification to the execution id :
//     *          start -> 0 end -> -1 start -> 1 end -> -2 ...
//     * Else :
//     *          start -> 0 start -> 1 start -> 2 ...
//     * */
//    private int getExecutionIdAndActionOthers(int threadId, int depth) {
//        StackbarsNode node = fMapCurrentExecutionByTid.get(threadId);
//        Integer executionId = null;
//
//        for(int i = 0; i <= depth; ++i)
//        {
//            if(node == null)
//            {
//                return -1;
//            }
//            executionId = node.fId;
//            node = node.fCurrentChildNode;
//        }
//
//        if (executionId == null)
//        {
//            return -1;
//        }
//
//        return executionId;
//    }
//
//    private int getExecutionIdAndActionStart(int threadId, int depth) {
//
//        if(threadId < 0)
//        {
//            return -1;
//        }
//
//        StackbarsNode node = fMapCurrentExecutionByTid.get(threadId);
//        Integer executionId = null;
//
//        for(int i = 0; i < depth; ++i)
//        {
//            if(node != null)
//            {
//                node = node.fCurrentChildNode;
//            }
//        }
//
//        if(node != null)
//        {
//            executionId = node.fId;
//        }
//
//        if (executionId == null)
//        {
//            addExecutionId(threadId, 0, depth);
//            return 0;
//        }
//        else if (fEndEventsDefined.get(depth))
//        {
//            //start after end
//            if(executionId < 0)
//            {
//                executionId *= -1;
//                addExecutionId(threadId, executionId, depth);
//                return executionId;
//            }
//            //start after start
//            return -1;
//        }
//        //start after start with no end event
//        ++executionId;
//        addExecutionId(threadId, executionId, depth);
//        closeChild(threadId, depth);
//        return executionId;
//    }
//
//    private int getExecutionIdAndActionEnd(int threadId, int depth) {
//        StackbarsNode node = fMapCurrentExecutionByTid.get(threadId);
//        Integer executionId = null;
//
//        // Get the current node
//        for(int i = 0; i < depth; ++i)
//        {
//            if(node != null)
//            {
//                node = node.fCurrentChildNode;
//            }
//        }
//
//        // Get the associate execution id
//        if(node != null)
//        {
//            executionId = node.fId;
//        }
//
//        // If there is no id
//        if (executionId == null)
//        {
//            return -1;
//        }
//
//        // If we need to close an execution
//        if(executionId >= 0)
//        {
//            int newExecutionId = executionId + 1;
//            newExecutionId *= -1;
//            addExecutionId(threadId, newExecutionId, depth);
//            closeChild(threadId, depth);
//        }
//        return executionId;
//    }
//
//    private void closeChild(int threadId, int depth)
//    {
//        // Close his child
//        StackbarsNode node = fMapCurrentExecutionByTid.get(threadId);
//
//        // Get the current node
//        for(int i = 0; i < depth; ++i)
//        {
//            if(node != null)
//            {
//                node = node.fCurrentChildNode;
//            }
//        }
//        if(node != null)
//        {
//            node.fCurrentChildNode = null;
//        }
//    }
//
//    private int getNodeCPUs() {
//        return ss.getQuarkAbsoluteAndAdd(Attributes.CPUS);
//    }
//
//    private int getNodeThreads() {
//        return ss.getQuarkAbsoluteAndAdd(Attributes.THREADS);
//    }
//
//    public void setStartEvent(List<StackChartEvent> startEvent) {
//        this.fStartEvents = startEvent;
//        if(fStartEvents != null)
//        {
//            for(int i = 0; i < fStartEvents.size(); ++i)
//            {
//                if(!fStartEvents.get(i).name.equals(NO_CUSTOM_EVENTS))
//                {
//                    fStartEventsDefined.add(i,true);
//                }
//                else
//                {
//                    fStartEventsDefined.add(i,false);
//                }
//            }
//            fMaxExecutionDepth = startEvent.size();
//        }
//        else
//        {
//            fMaxExecutionDepth = 1;
//        }
//    }
//
//    public void setEndEvent(List<StackChartEvent> endEvent) {
//        this.fEndEvents = endEvent;
//        if(fEndEvents != null)
//        {
//            for(int i = 0; i < fEndEvents.size(); ++i)
//            {
//                if(!fEndEvents.get(i).name.equals(NO_CUSTOM_EVENTS))
//                {
//                    fEndEventsDefined.add(i,true);
//                }
//                else
//                {
//                    fEndEventsDefined.add(i,false);
//                }
//            }
//            fMaxExecutionDepth = endEvent.size();
//        }
//        else
//        {
//            fMaxExecutionDepth = 1;
//        }
//    }
//
//    private static int safeLongToInt(long l) {
//        if (l < Integer.MIN_VALUE || l > Integer.MAX_VALUE) {
//            throw new IllegalArgumentException
//                (l + " cannot be cast to int without changing its value.");
//        }
//        return (int) l;
//    }
//
//    private class StackbarsNode {
//        private StackbarsNode(int executionId) {
//            fId = executionId;
//        }
//        private int fId;
//        private StackbarsNode fCurrentChildNode;
//    }
//
//    private void addExecutionId(int threadId, int executionId, int depth)
//    {
//        StackbarsNode executionParent = null;
//        StackbarsNode execution = fMapCurrentExecutionByTid.get(threadId);
//
//        for(int i = 0; i < depth; ++i)
//        {
//            if(execution == null)
//            {
//                return;
//            }
//            executionParent = execution;
//            execution = executionParent.fCurrentChildNode;
//        }
//
//        if(execution == null)
//        {
//            if(executionParent == null)
//            {
//                fMapCurrentExecutionByTid.put(threadId, new StackbarsNode(executionId));
//            }
//            else
//            {
//                executionParent.fCurrentChildNode = new StackbarsNode(executionId);
//            }
//        }
//        else
//        {
//            execution.fId = executionId;
//        }
//    }
//
//    public int getMaxDepth()
//    {
//        return fMaxExecutionDepth;
//    }
//}
