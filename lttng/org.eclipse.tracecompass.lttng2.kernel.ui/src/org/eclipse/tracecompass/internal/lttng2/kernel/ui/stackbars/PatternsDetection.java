package org.eclipse.tracecompass.internal.lttng2.kernel.ui.stackbars;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map.Entry;
import java.util.Vector;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.linuxtools.internal.lttng2.kernel.core.LttngStrings;
import org.eclipse.linuxtools.tmf.core.component.ITmfEventProvider;
import org.eclipse.linuxtools.tmf.core.event.ITmfEvent;
import org.eclipse.linuxtools.tmf.core.event.ITmfEventField;
import org.eclipse.linuxtools.tmf.core.request.ITmfEventRequest;
import org.eclipse.linuxtools.tmf.core.request.TmfEventRequest;
import org.eclipse.linuxtools.tmf.core.timestamp.TmfTimeRange;
import org.eclipse.linuxtools.tmf.core.trace.ITmfTrace;

public class PatternsDetection {

    private int fIndexEvents = 0;
    private final HashMap<String,Short> fDictionary = new HashMap<>();
    private String [] fInverseDictionary;
    private long fStartTimeMilli;
    private long fMaxTimeMilli = 30000;

    public void findPatterns(final ITmfTrace trace, final Vector<Integer> tids, final int maxEvents,
            final HashSet<String> invalidNames, final HashSet<String> startNames,
            final int minSupport, final int maxFrequent)
    {
        Job job = new Job("Searching patterns") { //$NON-NLS-1$

            @Override
            protected IStatus run(final IProgressMonitor monitor) {

                final short[] sequence = new short[maxEvents];
                TmfEventRequest request;
                request = new TmfEventRequest(ITmfEvent.class, TmfTimeRange.ETERNITY,
                        0,
                        ITmfEventRequest.ALL_DATA,
                        TmfEventRequest.ExecutionType.FOREGROUND) {

                    private int[]threadByCpu = new int[8];
                    private String[]procNameByCpu = new String[8];
                    private Short nextShort = 0;

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

                            // 4.4) upadate array
                            final String eventName = event.getType().getName();
                            if (eventName.equals(LttngStrings.SCHED_SWITCH)) {

                                ITmfEventField eventField = event.getContent();
                                //Set the current scheduled process on the relevant CPU for the next iteration
                                long nextTid = (Long) eventField.getField(LttngStrings.NEXT_TID).getValue();
                                threadByCpu[source] = (int) nextTid;
                                procNameByCpu[source] = eventField.getField(LttngStrings.NEXT_COMM).getFormattedValue();
                            }

                            if(checkInvalidNames(eventName))
                            {
                                return;
                            }

                            for(Integer tid : tids) //We guess few tid, but probably better if map
                            {
                                if(threadByCpu[source] == tid)
                                {
                                    addEvent(event);
                                    break;
                                }
                            }
                        }

                        private boolean checkInvalidNames(String name) {

                            return invalidNames.contains(name);
                        }

                        //To reduce space, only "maxEvents" first convert to short
                        private void addEvent(ITmfEvent event) {

                            short value = searchDictio(event);
                            sequence[fIndexEvents] = value;

                            if(++fIndexEvents == maxEvents)
                            {
                                done();
                            }

                        }

                        private short searchDictio(ITmfEvent event) {
                            String name = event.getType().getName();
                            Short value = fDictionary.get(name);
                            if(value == null)
                            {
                                value = nextShort;
                                fDictionary.put(name, value);
                                nextShort++;
                            }
                            return value;
                        }
                    };

                    ((ITmfEventProvider) trace).sendRequest(request);
                    try {
                        request.waitForCompletion();
                    } catch (InterruptedException e) {

                    }

                    makeInverseDictio();
                    HashSet<Short> startValues = convertStartNamesToShort(startNames);
                    manepi(sequence, minSupport,maxFrequent,startValues, monitor);

                if (monitor.isCanceled()) {
                    return Status.CANCEL_STATUS;
                }
                return Status.OK_STATUS;


            }

            private HashSet<Short> convertStartNamesToShort(HashSet<String> startNames1) {
                HashSet<Short> values = new HashSet<>();
                for(String name : startNames1)
                {
                    Short value = fDictionary.get(name);
                    if(value != null)
                    {
                        values.add(value);
                    }
                }
                return values;
            }

            private void makeInverseDictio() {
                if(fDictionary.size() != 0)
                {
                    fInverseDictionary = new String[fDictionary.size()];
                    for(Entry<String,Short> e : fDictionary.entrySet())
                    {
                        fInverseDictionary[e.getValue()] = e.getKey();
                    }
                }

            }


        };
        //job.setSystem(true);
        job.setPriority(Job.SHORT);
        job.schedule();

    }

    protected void manepi(short[] sequence, int minSupportParam, int maxFrequentParam, HashSet<Short> startValues, IProgressMonitor monitor) {

        //create root
        Node root = new Node((short) -1);

        //count occurrences to find frequent 1-episode
        int alphabetSize = fInverseDictionary.length;
        int[] nbOccurrences = new int[alphabetSize]; //TODO this can be merge with the first iteration
        for(int i = 0; i < fIndexEvents; ++i)
        {
            ++nbOccurrences[sequence[i]];
        }

        if(monitor.isCanceled())
        {
            return;
        }

        //Determine minimum events required
        int maxFrequent = maxFrequentParam;
        int nbEventsMin = minSupportParam;
        int[] sortArray = Arrays.copyOf(nbOccurrences, nbOccurrences.length);
        Arrays.sort(sortArray);
        if(maxFrequent > 0 && sortArray.length > maxFrequent)
        {
            nbEventsMin = Math.min(nbEventsMin, sortArray[sortArray.length - maxFrequent]);
            if(startValues.size() != 0)
            {
                Iterator<Short> it = startValues.iterator();
                while(it.hasNext())
                {
                    Short st = it.next();
                    if(nbEventsMin > nbOccurrences[st])
                    {
                        nbEventsMin = nbOccurrences[st];
                    }
                }
            }
        }
        else if (sortArray.length != 0)
        {
            nbEventsMin = Math.min(nbEventsMin, sortArray[0]);
        }

        if(monitor.isCanceled())
        {
            return;
        }

        //allocate memory for frequent
        HashMap<Short,ArrayList<Integer>> frequent1Episode = new HashMap<>();
        for(short i = 0; i < alphabetSize; ++i)
        {
            if(nbOccurrences[i] > nbEventsMin)
            {
                ArrayList<Integer> occurrences = new ArrayList<>();
                occurrences.ensureCapacity(nbOccurrences[i]);
                frequent1Episode.put(i, occurrences);
            }
        }

        if(monitor.isCanceled())
        {
            return;
        }

        //For each frequent, get the occurrences
        for(int i = 0; i < fIndexEvents; ++i)
        {
            ArrayList<Integer> list = frequent1Episode.get(sequence[i]);
            if(list != null)
            {
                list.add(i);
            }
        }

        if(monitor.isCanceled())
        {
            return;
        }

        fStartTimeMilli = System.currentTimeMillis();

        //For each frequent 1-episode
        HashSet<Short> alreadyUsed = new HashSet<>();
        for(Entry<Short, ArrayList<Integer>> entry : frequent1Episode.entrySet())
        {
            if(startValues.size() == 0 || startValues.contains(entry.getKey()))
            {
                Node child = new Node(entry.getKey());
                root.addChild(child);
                ArrayList<Interval> occurrencesCurrent = new ArrayList<>();
                for(Integer time : entry.getValue())
                {
                    occurrencesCurrent.add(new Interval(time,time));
                }

                if(monitor.isCanceled())
                {
                    return;
                }

                MineGrow(child, occurrencesCurrent, frequent1Episode,nbEventsMin, monitor, alreadyUsed);
            }
        }

        int[] array = new int[0];
        ArrayList<int[]> results = new ArrayList<>();
        constructResultArrays(root, array, results);
        StackbarsAnalysis.getInstance().sendResults(results, fInverseDictionary);

    }

    private void MineGrow(Node parent, ArrayList<Interval> minimalOccurrencesParent, HashMap<Short,ArrayList<Integer>> frequent1Episode, int nbEventsMin, IProgressMonitor monitor, HashSet<Short> alreadyUsed) {

        if(monitor.isCanceled())
        {
            return;
        }

        if((System.currentTimeMillis() - fStartTimeMilli) > fMaxTimeMilli)
        {
            return;
        }

        //For each frequent 1-episode
        alreadyUsed.add(parent.data);
        for(Entry<Short, ArrayList<Integer>> entry : frequent1Episode.entrySet())
        {
            if(!alreadyUsed.contains(entry.getKey()))
            {
                ArrayList<Interval> mOConcat = ComputeMo(minimalOccurrencesParent, entry.getValue());
                ArrayList<Integer> manOIndexConcat = ComputeMano(mOConcat);
                int support = manOIndexConcat.size();
                if(support > nbEventsMin)
                {
                    Node child = new Node(entry.getKey());
                    parent.addChild(child);
                    MineGrow(child, mOConcat, frequent1Episode, nbEventsMin, monitor,alreadyUsed);
                }
            }
        }
        alreadyUsed.remove(parent.data);
    }

    private void constructResultArrays(Node parent, int[] parents, ArrayList<int[]> results) {

        for(Node child : parent.children)
        {
            int[] array = new int[parents.length + 1];
            System.arraycopy(parents, 0, array, 0, parents.length);
            array[array.length - 1] = child.data;
            if(child.children.size() == 0)
            {
                results.add(array);
            }
            constructResultArrays(child, array, results);
        }
    }

    private static ArrayList<Integer> ComputeMano(ArrayList<Interval> mOConcat) {

        int i = 0;
        int j = i + 1;
        ArrayList<Integer> indexInMO = new ArrayList<>();
        indexInMO.add(i);

        while(j < mOConcat.size())
        {
            if(mOConcat.get(i).end < mOConcat.get(j).begin)
            {
                indexInMO.add(j);
                i = j;
                j = i + 1;
            }
            else
            {
                ++j;
            }
        }

        return indexInMO;
    }

    private static ArrayList<Interval> ComputeMo(ArrayList<Interval> minimalOccurrencesParent, ArrayList<Integer> arrayList1Episode) {

        ArrayList<Interval> intervalResult = new ArrayList<>();
        int index = 0;
        for(Integer timeOccurrence1Episode : arrayList1Episode)
        {
            while(index + 1 < minimalOccurrencesParent.size())
            {
                if(minimalOccurrencesParent.get(index).end < timeOccurrence1Episode)
                {
                    if(minimalOccurrencesParent.get(index + 1).end > timeOccurrence1Episode)
                    {
                        intervalResult.add(new Interval(minimalOccurrencesParent.get(index).begin,
                                timeOccurrence1Episode));
                        break;
                    }
                    ++index;
                }
                else
                {
                    break;
                }
            }
            if(index == minimalOccurrencesParent.size())
            {
                break;
            }
        }
        return intervalResult;
    }

    private static class Node {
        private short data;
        private ArrayList<Node> children;

        public Node(short i) {
            children = new ArrayList<>();
            this.data = i;
        }

        public void addChild(Node child)
        {
            children.add(child);
        }
    }

    private static class Interval {
        private int begin;
        private int end;

        public Interval(int begin, int end) {
            this.begin = begin;
            this.end = end;
        }
    }
}