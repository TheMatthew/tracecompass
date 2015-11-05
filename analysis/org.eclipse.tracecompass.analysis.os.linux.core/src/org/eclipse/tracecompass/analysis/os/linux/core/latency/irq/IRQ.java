package org.eclipse.tracecompass.analysis.os.linux.core.latency.irq;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.jdt.annotation.NonNull;
import org.eclipse.jdt.annotation.Nullable;
import org.eclipse.tracecompass.analysis.os.linux.core.latency.irq.XmlIrqUtils.TYPE;
import org.eclipse.tracecompass.common.core.NonNullUtils;
import org.eclipse.tracecompass.segmentstore.core.ISegment;
import org.eclipse.tracecompass.tmf.analysis.xml.core.model.TmfXmlSyntheticEvent;
import org.eclipse.tracecompass.tmf.core.event.aspect.TmfContentFieldAspect;

/**
 * This class represents an interruption
 * @since 2.0
 *
 */
public class IRQ implements ISegment {
    /**
     *
     */
    private static final long serialVersionUID = 2111306681847732952L;

    /**
     * The synthetic event corresponding to this irq
     */
    public @Nullable TmfXmlSyntheticEvent synIrq;
    /**
     * The list of child elements
     */
    public List<IRQ> childs;
    /**
     * The label of this interruption
     */
    public String label;
    /**
     * The type of this interrupt
     */
    public TYPE type;
    private TmfContentFieldAspect entryAspect = new TmfContentFieldAspect("entry", "entry"); //$NON-NLS-1$ //$NON-NLS-2$
    private TmfContentFieldAspect softExitAspect = new TmfContentFieldAspect("exit", "exit"); //$NON-NLS-1$ //$NON-NLS-2$

    /**
     * Constructor
     *
     * @param synEvent
     *            The synthetic event for this interruption
     * @param type
     *            The type of the interruption
     */
    public IRQ(@Nullable TmfXmlSyntheticEvent synEvent, TYPE type) {
        this.type = type;
        synIrq = synEvent;
        childs = new ArrayList<>();
        label = XmlIrqUtils.getIRQLabel(synIrq, this.type);
    }

    @Override
    public int compareTo(ISegment arg0) {
        return 0;
    }

    @Override
    public long getStart() {
        if (synIrq != null) {
            return synIrq.getTimestamp().getValue();
        }
        return -1;
    }

    @Override
    public long getEnd() {
        if (synIrq != null) {
            return synIrq.getTimestampEnd().getValue();
        }
        return -1;
    }

    @Override
    public long getLength() {
        if (synIrq != null) {
            return synIrq.getDuration();
        }
        return -1;
    }

    /**
     * Get the duration of the first step of this interruption pattern
     *
     * @return The duration
     */
    public long getD1() {
        if (synIrq != null && type == TYPE.SOFTIRQ) {
            return Long.parseLong(NonNullUtils.nullToEmptyString(entryAspect.resolve(synIrq))) - NonNullUtils.checkNotNull(synIrq).getTimestamp().getValue();
        }
        return -1;
    }

    @Override
    public @NonNull String getName() {
        return label;
    }

    /**
     * Get the duration of the second step of this interruption pattern
     *
     * @return The duration
     */
    public long getD2() {
        if (synIrq != null) {
            if (type == TYPE.IRQ) {
                return synIrq.getDuration();
            }
            return Long.parseLong(NonNullUtils.nullToEmptyString(softExitAspect.resolve(NonNullUtils.checkNotNull(synIrq))))
                    - Long.parseLong(NonNullUtils.nullToEmptyString(entryAspect.resolve(NonNullUtils.checkNotNull(synIrq))));
        }
        return -1;
    }
}