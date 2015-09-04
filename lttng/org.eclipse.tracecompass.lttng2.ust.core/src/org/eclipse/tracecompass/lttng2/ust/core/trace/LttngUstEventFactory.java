package org.eclipse.tracecompass.lttng2.ust.core.trace;

import org.eclipse.tracecompass.ctf.core.CTFStrings;
import org.eclipse.tracecompass.ctf.core.event.EventDefinition;
import org.eclipse.tracecompass.ctf.core.event.IEventDeclaration;
import org.eclipse.tracecompass.tmf.core.timestamp.TmfNanoTimestamp;
import org.eclipse.tracecompass.tmf.core.trace.ITmfContext;
import org.eclipse.tracecompass.tmf.ctf.core.event.CtfTmfEvent;
import org.eclipse.tracecompass.tmf.ctf.core.event.CtfTmfEventFactory;
import org.eclipse.tracecompass.tmf.ctf.core.trace.CtfTmfTrace;

public class LttngUstEventFactory extends CtfTmfEventFactory {

    public LttngUstEventFactory(){}

    @Override
    public CtfTmfEvent createEvent(EventDefinition eventDef,
            String fileName, CtfTmfTrace originTrace) {
        /* Prepare what to pass to CtfTmfEvent's constructor */
        final IEventDeclaration eventDecl = eventDef.getDeclaration();
        final long ts = eventDef.getTimestamp();
        final TmfNanoTimestamp timestamp = originTrace.createTimestamp(
                originTrace.timestampCyclesToNanos(ts));

        int sourceCPU = eventDef.getCPU();

        String reference = fileName == null ? NO_STREAM : fileName;

        /* Handle the special case of lost events */
        if (eventDecl.getName().equals(CTFStrings.LOST_EVENT_NAME)) {
            return createLostEvent(eventDef, originTrace, eventDecl, ts, timestamp, sourceCPU, reference);
        }

        /* Handle standard event types */
        return new LttngUstEvent(originTrace,
                ITmfContext.UNKNOWN_RANK,
                timestamp,
                reference, // filename
                sourceCPU,
                eventDecl,
                eventDef);
    }
}
