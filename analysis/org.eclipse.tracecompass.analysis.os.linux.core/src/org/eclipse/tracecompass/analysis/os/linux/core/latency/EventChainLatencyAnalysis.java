/*******************************************************************************
 * Copyright (c) 2015 Ericsson
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/

package org.eclipse.tracecompass.analysis.os.linux.core.latency;

import static org.eclipse.tracecompass.common.core.NonNullUtils.checkNotNull;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.text.NumberFormat;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

import org.eclipse.jdt.annotation.Nullable;
import org.eclipse.tracecompass.analysis.os.linux.core.kernelanalysis.KernelTidAspect;
import org.eclipse.tracecompass.analysis.os.linux.core.trace.IKernelAnalysisEventLayout;
import org.eclipse.tracecompass.analysis.os.linux.core.trace.IKernelTrace;
import org.eclipse.tracecompass.analysis.timing.core.segmentstore.AbstractSegmentStoreAnalysisModule;
import org.eclipse.tracecompass.segmentstore.core.BasicSegment;
import org.eclipse.tracecompass.segmentstore.core.ISegment;
import org.eclipse.tracecompass.segmentstore.core.ISegmentStore;
import org.eclipse.tracecompass.tmf.core.event.ITmfEvent;
import org.eclipse.tracecompass.tmf.core.event.ITmfEventField;
import org.eclipse.tracecompass.tmf.core.segment.ISegmentAspect;
import org.eclipse.tracecompass.tmf.ctf.core.event.aspect.CtfCpuAspect;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;

/**
 * @author Bernd Hufmann
 * @since 2.0
 */
public class EventChainLatencyAnalysis extends AbstractSegmentStoreAnalysisModule {

    /**
     * The ID of this analysis
     */
    public static final String ID = "org.eclipse.tracecompass.analysis.os.linux.core.latencychain"; //$NON-NLS-1$

    private static final String DATA_FILENAME = "latency-chain-analysis.dat"; //$NON-NLS-1$

    /* TODO event names should be provided by the event layout*/
    private static final String HR_TIMER_START_EVENT_NAME = "timer_hrtimer_start"; //$NON-NLS-1$
    private static final String HR_TIMER_EXPIRE_ENTRY_EVENT_NAME = "timer_hrtimer_expire_entry"; //$NON-NLS-1$
    private static final String SYSCALL_EXIT_CLOCK_EVENT_NAME = "syscall_exit_clock_nanosleep"; //$NON-NLS-1$

    /* TODO field constants should be provided by the event layout*/
    private static final String HRTIMER_FIELD_NAME = "hrtimer"; //$NON-NLS-1$
    private static final String HRTIMER_EXPIRES_FIELD_NAME = "expires"; //$NON-NLS-1$
    private static final String HRTIMER_NOW_FIELD_NAME = "now"; //$NON-NLS-1$
    private static final String FUNCTION_FIELD_NAME = "function"; //$NON-NLS-1$
    private static final String COMM_FIELD_NAME = "comm"; //$NON-NLS-1$

    // TODO Replace hardcoded values
    private static int TID_VALUE = 18395;
    /* TODO Hard-code value, only works for specific trace,
       it would be good to determine this value or
       LTTng will provide the function name (hrtimer_wakeup)instead of the address
     */
    private static long HRTIMER_WAKEUP_ADDRESS = -2130122992L;

    private static String PROCESS_NAME = "cyclictest"; //$NON-NLS-1$

    private static final CtfCpuAspect CPU_ASPECT = CtfCpuAspect.INSTANCE;

    private static final NumberFormat FORMATTER = checkNotNull(NumberFormat.getNumberInstance(Locale.getDefault()));

    private static final int HR_TIMER_START = 1;
    private static final int HR_TIMER_EXPIRE_ENTRY = 2;
    private static final int SCHED_WAKEUP_INDEX = 3;
    private static final int SCHED_SWITCH_INDEX = 4;
    private static final int SYSCALL_EXIT_CLOCK_INDEX = 5;

    private static final String[] DURATION_NAMES = new String[] {
            Messages.LatencyChain_TimerElapsedLabel,
            Messages.LatencyChain_TimeToSchedWakeUpLabel,
            Messages.LatencyChain_TimeToSchedSwitchLabel
    };

    private static final Collection<ISegmentAspect> BASE_ASPECTS =
            checkNotNull(ImmutableList.of(
                    (ISegmentAspect) new SubSegementsAspect(0, checkNotNull(DURATION_NAMES[0])),
                    (ISegmentAspect) new SubSegementsAspect(1, checkNotNull(DURATION_NAMES[1])),
                    (ISegmentAspect) new SubSegementsAspect(2, checkNotNull(DURATION_NAMES[2]))));

    @Override
    public String getId() {
        return ID;
    }

    @Override
    public Iterable<ISegmentAspect> getSegmentAspects() {
        return BASE_ASPECTS;
    }

    @Override
    public String getDataFileName() {
        return DATA_FILENAME;
    }

    @Override
    public AbstractSegmentStoreAnalysisRequest createAnalysisRequest(ISegmentStore<ISegment> segmentStore) {
        return new EventChainLatencyAnalysisRequest(segmentStore);
    }

    @Override
    protected Object[] readObject(ObjectInputStream ois) throws ClassNotFoundException, IOException {
        return checkNotNull((Object[]) ois.readObject());
    }

    private static class EventChainLatencyAnalysisRequest extends AbstractSegmentStoreAnalysisRequest {

        private @Nullable Map<String, Integer> fEventNames;

        // map timer-instance to data structure
        // htTimer -> HrTimer
        private final Map<Long, HrTimer> fOngoingHrTimers = new HashMap<>();
        // cpu -> HrTimer
        private final Map<Integer, HrTimer> fOngoingTimerTasks = new HashMap<>();

        private @Nullable HrTimer fCurrentHrTimer = null;

        private @Nullable IKernelAnalysisEventLayout fLayout;

        public EventChainLatencyAnalysisRequest(ISegmentStore<ISegment> syscalls) {
            super(syscalls);
        }

        @Override
        public void handleData(final ITmfEvent event) {
            super.handleData(event);
            IKernelAnalysisEventLayout layout = fLayout;
            Map<String, Integer> eventNames = fEventNames;
            if (layout == null) {
                IKernelTrace trace = checkNotNull((IKernelTrace) event.getTrace());
                layout = trace.getKernelEventLayout();
                eventNames = buildEventNames(layout);
                fEventNames = eventNames;
                fLayout = layout;
            }
            final String eventName = event.getType().getName();

            Integer idx = checkNotNull(eventNames).get(eventName);
            int intval = (idx == null ? -1 : idx.intValue());
            switch (intval) {
            case HR_TIMER_START: {
                Integer tid = KernelTidAspect.INSTANCE.resolve(event);
                if (tid == null) {
                    // no information on this event/trace ?
                    return;
                }
                if (!tid.equals(TID_VALUE)) {
                    return;
                }
                ITmfEventField content = event.getContent();
                Long hrTimer = (Long) content.getField(HRTIMER_FIELD_NAME).getValue();
                Long function = (Long) content.getField(FUNCTION_FIELD_NAME).getValue();
                // hrtimer wakeup
                if (function == HRTIMER_WAKEUP_ADDRESS) {
                    HrTimer hrTimerInst = fOngoingHrTimers.get(hrTimer);
                    if (hrTimerInst != null) {
                        // should not happen
                        break;
                    }
                    hrTimerInst = new HrTimer();
                    hrTimerInst.expires = (Long) content.getField(HRTIMER_EXPIRES_FIELD_NAME).getValue();
                    fOngoingHrTimers.put(hrTimer, hrTimerInst);
                }
                break;
            }

            case HR_TIMER_EXPIRE_ENTRY: {
                ITmfEventField content = event.getContent();
                Long hrTimer = (Long) content.getField(HRTIMER_FIELD_NAME).getValue();
                Long function = (Long) content.getField(FUNCTION_FIELD_NAME).getValue();

                if (function == HRTIMER_WAKEUP_ADDRESS) {
                    HrTimer hrTimerInst = fOngoingHrTimers.get(hrTimer);
                    if (hrTimerInst == null) {
                        break;
                    }
                    hrTimerInst.now =  (Long) content.getField(HRTIMER_NOW_FIELD_NAME).getValue();
                    hrTimerInst.t1 = event.getTimestamp().getValue() - (hrTimerInst.now - hrTimerInst.expires);

                    fOngoingHrTimers.remove(hrTimer);

                    Integer cpu = CPU_ASPECT.resolve(event);
                    if (cpu == null) {
                        return;
                    }

                    HrTimer other = fOngoingTimerTasks.get(cpu);

                    if (other != null) {
                        return;
                    }
                    fOngoingTimerTasks.put(cpu, hrTimerInst);
                }

                break;
            }

            case SCHED_WAKEUP_INDEX: {

                ITmfEventField content = event.getContent();
                String comm = (String) content.getField(COMM_FIELD_NAME).getValue();
                if (comm.equalsIgnoreCase(PROCESS_NAME)) {
                    Integer cpu = CPU_ASPECT.resolve(event);
                    if (cpu == null) {
                        return;
                    }

                    HrTimer other = fOngoingTimerTasks.get(cpu);

                    if (other == null) {
                        return;
                    }
                    other.t3 = event.getTimestamp().getValue();
                }
                break;
            }

            case SCHED_SWITCH_INDEX: {
                ITmfEventField content = event.getContent();
                String comm = (String) content.getField(layout.fieldNextComm()).getValue();
                if (comm.equals(PROCESS_NAME)) {
                    Integer cpu = CPU_ASPECT.resolve(event);
                    if (cpu == null) {
                        return;
                    }

                    HrTimer other = fOngoingTimerTasks.get(cpu);
                    if (other == null) {
                        return;
                    }
                    other.t4 = event.getTimestamp().getValue();
                    fOngoingTimerTasks.remove(cpu);
                    fCurrentHrTimer = other;
                }

                break;
            }

            case SYSCALL_EXIT_CLOCK_INDEX: {
                Integer tid = KernelTidAspect.INSTANCE.resolve(event);
                if (tid == null) {
                    // no information on this event/trace ?
                    return;
                }
                if (!tid.equals(TID_VALUE)) {
                    return;
                }
                HrTimer current = fCurrentHrTimer;
                if (current != null) {
                    current.t5 = event.getTimestamp().getValue();

                    EventChainSegments seg = new EventChainSegments(current.t1, current.t5,
                            checkNotNull(new ImmutableList.Builder<ISegment>()
                                    .add(new BasicSegment(current.t1, current.t3, "seg1"))
                                    .add(new BasicSegment(current.t3, current.t4, "seg2"))
                                    .add(new BasicSegment(current.t4, current.t5, "seg3"))
                                    .build()));

                    getSegmentStore().add(seg);
                }

                break;
            }
            default:
                break;
            }
        }

        @Override
        public void handleCompleted() {
            fOngoingHrTimers.clear();
            fOngoingTimerTasks.clear();
            fCurrentHrTimer = null;
        }

        private static Map<String, Integer> buildEventNames(IKernelAnalysisEventLayout layout) {
            ImmutableMap.Builder<String, Integer> builder = ImmutableMap.builder();

            builder.put(HR_TIMER_START_EVENT_NAME, HR_TIMER_START);
            builder.put(HR_TIMER_EXPIRE_ENTRY_EVENT_NAME, HR_TIMER_EXPIRE_ENTRY);
            builder.put(layout.eventSchedSwitch(), SCHED_SWITCH_INDEX);

            for (String eventSchedWakeup : layout.eventsSchedWakeup()) {
                builder.put(eventSchedWakeup, SCHED_WAKEUP_INDEX);
            }
            builder.put(SYSCALL_EXIT_CLOCK_EVENT_NAME, SYSCALL_EXIT_CLOCK_INDEX);

            return checkNotNull(builder.build());
        }

    }

    private static class SubSegementsAspect implements ISegmentAspect {
        private final int fIndex;
        private final String fName;
        private SubSegementsAspect(int index, String name) {
            fIndex = index;
            fName = name;
        }
        @Override
        public String getHelpText() {
            return fName;
        }
        @Override
        public String getName() {
            return fName;
        }
        @Override
        public @Nullable Comparator<?> getComparator() {
            return INTERVAL_LENGTH_COMPARATOR;
        }
        @Override
        public @Nullable String resolve(ISegment segment) {
            if (segment instanceof EventChainSegments) {
                EventChainSegments chain = (EventChainSegments) segment;

                String percentageText = toPercentageText((double) chain.getSubSegments().get(fIndex).getLength() / chain.getLength());
                return String.valueOf(chain.getSubSegments().get(fIndex).getLength()) + percentageText;
            }
            return EMPTY_STRING;
        }

        private Comparator<ISegment> INTERVAL_LENGTH_COMPARATOR = new Comparator<ISegment>() {
            @Override
            public int compare(@Nullable ISegment o1, @Nullable ISegment o2) {

                if (o1 == null || o2 == null) {
                    throw new IllegalArgumentException();
                }

                if (o2 instanceof EventChainSegments && o2 instanceof EventChainSegments) {
                    EventChainSegments chain1 = (EventChainSegments) o1;
                    EventChainSegments chain2 = (EventChainSegments) o2;
                    return Long.compare(chain1.getSubSegments().get(fIndex).getLength(), chain2.getSubSegments().get(fIndex).getLength());
                }
                return Long.compare(o1.getLength(), o2.getLength());
            }
        };

    }

    private static String toPercentageText(double percentage) {
        // The cast to long is needed because the formatter cannot truncate the number.
        double truncPercentage = ((long) (1000.0 * percentage)) / 10.0;
        String percentageString = checkNotNull(String.format("%s%s%s", "  (", FORMATTER.format(truncPercentage), " %)")); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
        return percentageString;
    }

    private static class HrTimer {
        public long expires;
        public long now;
        public long t1;
        public long t3;
        public long t4;
        public long t5;
    }

}

