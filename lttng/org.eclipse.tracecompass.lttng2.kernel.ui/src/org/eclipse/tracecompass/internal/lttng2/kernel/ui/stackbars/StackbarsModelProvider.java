///*******************************************************************************
// * Copyright (c) 2014 École Polytechnique de Montréal
// *
// * All rights reserved. This program and the accompanying materials are
// * made available under the terms of the Eclipse Public License v1.0 which
// * accompanies this distribution, and is available at
// * http://www.eclipse.org/legal/epl-v10.html
// *
// * Contributors:
// *   Mathieu Cote - Initial API and implementation
// *******************************************************************************/
//
//package org.eclipse.linuxtools.internal.lttng2.kernel.ui.stackbars;
//
//import java.util.ArrayList;
//import java.util.List;
//
//import org.eclipse.jdt.annotation.NonNull;
//import org.eclipse.linuxtools.tmf.core.analysis.TmfAbstractAnalysisParamProvider;
//import org.eclipse.linuxtools.tmf.core.trace.ITmfTrace;
//
//public class StackbarsModelProvider extends
//        TmfAbstractAnalysisParamProvider {
//
//    private List<StackChartEvent> fStartEvents;
//    private List<StackChartEvent> fEndEvents;
//    private List<String> fRawStartEventName;
//    private List<String> fRawStartEventParams;
//    private List<String> fRawEndEventName;
//    private List<String> fRawEndEventParams;
//    private List<String> fRawEndEventsTids;
//    private List<String> fRawStartEventTids;
//    private List<Long> fDeadlines;
//
//    private static StackbarsModelProvider fInstance = null;
//
//    public StackbarsModelProvider() {
//        super();
//        fInstance = this;
//        fStartEvents = new ArrayList<>();
//        fEndEvents = new ArrayList<>();
//    }
//
//    @NonNull
//    public static StackbarsModelProvider getInstance() {
//        if (fInstance == null) {
//            fInstance = new StackbarsModelProvider();
//        }
//        return fInstance;
//    }
//
//    @Override
//    public String getName() {
//        return "Stackbars border events provider";
//    }
//
//    @Override
//    public Object getParameter(String name) {
//        if (name.equals(StackbarsStateSystemModule.PARAM_START_EVENT)) {
//            return fStartEvents;
//        }
//        if (name.equals(StackbarsStateSystemModule.PARAM_END_EVENT)) {
//            return fEndEvents;
//        }
//        return null;
//    }
//
//    public void setModel(List<String> startEventName, List<String> startEventParams,
//            List<String> endEventName, List<String> endEventParams, List<Long> deadlines,
//            List<String> startEventTids, List<String> endEventTids) {
//
//        boolean startEventModified = false;
//        boolean endEventModified = false;
//
//        if(deadlines != null)
//        {
//            fDeadlines = new ArrayList<>(deadlines);
//        }
//
//        // start name
//        if (startEventName != null)
//        {
//            int i = 0;
//            for(; i < startEventName.size(); ++i)
//            {
//                if(fStartEvents.size() <= i)
//                {
//                    fStartEvents.add(i, new StackChartEvent());
//                }
//
//                if(!startEventName.get(i).equals(fStartEvents.get(i).name))
//                {
//                    fStartEvents.get(i).name = startEventName.get(i);
//                    startEventModified = true;
//                }
//            }
//
//            fStartEvents.subList(i, fStartEvents.size()).clear();
//        }
//
//        //end name
//        if (endEventName != null)
//        {
//            int i = 0;
//            for(; i < endEventName.size(); ++i)
//            {
//                if(fEndEvents.size() <= i)
//                {
//                    fEndEvents.add(i, new StackChartEvent());
//                }
//
//                if(!endEventName.get(i).equals(fEndEvents.get(i).name))
//                {
//                    fEndEvents.get(i).name = endEventName.get(i);
//                    endEventModified = true;
//                }
//            }
//
//            fEndEvents.subList(i, fEndEvents.size()).clear();
//        }
//
//        //Start params
//        if (startEventParams != null)
//        {
//            int i = 0;
//            for(; i < startEventParams.size(); ++i)
//            {
//                String testValue = fRawStartEventParams == null ? null : (i < fRawStartEventParams.size() ? fRawStartEventParams.get(i) : null);
//                if (!startEventParams.get(i).equals(testValue))
//                {
//                    if(fStartEvents.size() <= i)
//                    {
//                        fStartEvents.add(i, new StackChartEvent());
//                    }
//
//                    fStartEvents.get(i).params = new ArrayList<>();
//                    int indexEqual = startEventParams.get(i).indexOf('=');
//                    int startIndex = 0;
//                    String name;
//                    String value;
//                    while (indexEqual != -1)
//                    {
//
//                        name = startEventParams.get(i).substring(startIndex, indexEqual);
//                        startIndex = startEventParams.get(i).indexOf(',', indexEqual);
//                        if(startIndex == -1)
//                        {
//                            value = startEventParams.get(i).substring(indexEqual + 1);
//                            indexEqual = -1;
//                        }
//                        else
//                        {
//                            value = startEventParams.get(i).substring(indexEqual + 1, startIndex);
//                            startIndex++;
//                            indexEqual = startEventParams.get(i).indexOf('=', startIndex);
//                        }
//                        fStartEvents.get(i).params.add(new StackChartParam(name,value));
//
//                    }
//                    startEventModified = true;
//                }
//            }
//        }
//
//      //end params
//        if (endEventParams != null)
//        {
//            int i = 0;
//            for(; i < endEventParams.size(); ++i)
//            {
//                String testValue = fRawEndEventParams == null ? null : (i < fRawEndEventParams.size() ? fRawEndEventParams.get(i) : null);
//                if (!endEventParams.get(i).equals(testValue))
//                {
//                    if(fEndEvents.size() <= i)
//                    {
//                        fEndEvents.add(i, new StackChartEvent());
//                    }
//
//                    fEndEvents.get(i).params = new ArrayList<>();
//                    int indexEqual = endEventParams.get(i).indexOf('=');
//                    int startIndex = 0;
//                    String name;
//                    String value;
//                    while (indexEqual != -1)
//                    {
//
//                        name = endEventParams.get(i).substring(startIndex, indexEqual);
//                        startIndex = endEventParams.get(i).indexOf(',', indexEqual);
//                        if(startIndex == -1)
//                        {
//                            value = endEventParams.get(i).substring(indexEqual + 1);
//                            indexEqual = -1;
//                        }
//                        else
//                        {
//                            value = endEventParams.get(i).substring(indexEqual + 1, startIndex);
//                            startIndex++;
//                            indexEqual = endEventParams.get(i).indexOf('=', startIndex);
//                        }
//                        fEndEvents.get(i).params.add(new StackChartParam(name,value));
//
//                    }
//                    endEventModified = true;
//                }
//            }
//        }
//
//      //Start tid
//        if (startEventTids != null)
//        {
//            int i = 0;
//            for(; i < startEventTids.size(); ++i)
//            {
//                String testValue = (fRawStartEventTids == null ? null : (i < fRawStartEventTids.size() ? fRawStartEventTids.get(i) : null));
//                if (!startEventTids.get(i).equals(testValue))
//                {
//                    if(fStartEvents.size() <= i)
//                    {
//                        fStartEvents.add(i, new StackChartEvent());
//                    }
//
//                    fStartEvents.get(i).tids = new ArrayList<>();
//                    int indexComa = startEventTids.get(i).indexOf(',');
//                    int startIndex = 0;
//                    String tid;
//                    while (true)
//                    {
//                        if(indexComa == -1)
//                        {
//                            tid = startEventTids.get(i).substring(startIndex);
//                        }
//                        else
//                        {
//                            tid = startEventTids.get(i).substring(startIndex, indexComa);
//                        }
//
//                        startIndex = indexComa + 1;
//                        try{
//                            int tidInt = Integer.parseInt(tid);
//                            fEndEvents.get(i).tids.add(tidInt);
//                        }
//                        catch (Exception e)
//                        {
//
//                        }
//
//                        if(indexComa == -1)
//                        {
//                            break;
//                        }
//                    }
//                    startEventModified = true;
//                }
//            }
//        }
//
//        //End tid
//        if (endEventTids != null)
//        {
//            int i = 0;
//            for(; i < endEventTids.size(); ++i)
//            {
//                String testValue = (fRawEndEventsTids == null ? null : (i < fRawEndEventsTids.size() ? fRawEndEventsTids.get(i) : null));
//                if (!endEventTids.get(i).equals(testValue))
//                {
//                    if(fEndEvents.size() <= i)
//                    {
//                        fEndEvents.add(i, new StackChartEvent());
//                    }
//
//                    fEndEvents.get(i).tids = new ArrayList<>();
//                    int indexComa = endEventTids.get(i).indexOf(',');
//                    int endIndex = 0;
//                    String tid;
//                    while (true)
//                    {
//                        if(indexComa == -1)
//                        {
//                            tid = endEventTids.get(i).substring(endIndex);
//                        }
//                        else
//                        {
//                            tid = endEventTids.get(i).substring(endIndex, indexComa);
//                        }
//
//                        endIndex = indexComa + 1;
//                        try{
//                            int tidInt = Integer.parseInt(tid);
//                            fEndEvents.get(i).tids.add(tidInt);
//                        }
//                        catch (Exception e)
//                        {
//
//                        }
//
//                        if(indexComa == -1)
//                        {
//                            break;
//                        }
//                    }
//                    endEventModified = true;
//                }
//            }
//        }
//
//        fRawStartEventTids = startEventTids;
//        fRawEndEventsTids = endEventTids;
//        fRawStartEventName = startEventName;
//        fRawEndEventName = endEventName;
//        fRawStartEventParams = startEventParams;
//        fRawEndEventParams = endEventParams;
//
//        if(startEventModified)
//        {
//            this.notifyParameterChanged(StackbarsStateSystemModule.PARAM_START_EVENT);
//        }
//        // TODO Fix-me : without the else, it will start the analysis 2 times
//        else if(endEventModified)
//        {
//            this.notifyParameterChanged(StackbarsStateSystemModule.PARAM_END_EVENT);
//        }
//
//    }
//
//    public class StackChartEvent{
//        public String name = ""; //$NON-NLS-1$
//        public List<StackChartParam> params;
//        public List<Integer> tids;
//    }
//
//    public class StackChartParam {
//        public StackChartParam(String name, String value) {
//            this.name = name;
//            this.value = value;
//        }
//        public String name;
//        public String value;
//    }
//
//    @Override
//    public boolean appliesToTrace(ITmfTrace trace) {
//        return true;
//    }
//
//    public List<String> getRawStartEventName() {
//        if(fRawStartEventName != null)
//        {
//            return new ArrayList<>(fRawStartEventName);
//        }
//        return null;
//    }
//
//    public List<String> getRawEndEventName() {
//        if(fRawEndEventName != null)
//        {
//            return new ArrayList<>(fRawEndEventName);
//        }
//        return null;
//    }
//
//    public List<String> getRawStartEventParams() {
//        if(fRawStartEventParams != null)
//        {
//            return new ArrayList<>(fRawStartEventParams);
//        }
//        return null;
//    }
//
//    public List<String> getRawEndEventParams() {
//        if(fRawEndEventParams != null)
//        {
//        return new ArrayList<>(fRawEndEventParams);
//        }
//        return null;
//    }
//
//    public List<String> getRawTidsStart() {
//        if(fRawStartEventTids != null)
//        {
//            return new ArrayList<>(fRawStartEventTids);
//        }
//        return null;
//    }
//
//    public List<String> getRawTidsEnd() {
//        if(fRawEndEventsTids != null)
//        {
//            return new ArrayList<>(fRawEndEventsTids);
//        }
//        return null;
//    }
//
//    public List<Long> getDeadlines() {
//        return fDeadlines;
//    }
//}
