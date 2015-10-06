/*******************************************************************************
 * Copyright (c) 2013 École Polytechnique de Montréal
 *
 * All rights reserved. This program and the accompanying materials are
 * made available under the terms of the Eclipse Public License v1.0 which
 * accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *   Geneviève Bastien - Initial API and implementation (from ControlFlowEntry)
 *   Mathieu Cote - Adapt to stackbars
 *******************************************************************************/

package org.eclipse.tracecompass.internal.lttng2.kernel.ui.stackbars;

import org.eclipse.tracecompass.tmf.ui.widgets.timegraph.model.TimeEvent;

/**
 * An entry in the Stack bars view
 */
public class ExtendedEntry extends ComparisonTimeGraphEntry {

    /**
     *
     */
    public static String NO_VALUE = "";

    /**
     * @author mcote
     *
     */
    public static enum Type {

        /**
         *
         */
        FUTEX("Futex"), //$NON-NLS-1$
        /**
         *
         */
        MQ_SEND("Mq_send"), //$NON-NLS-1$
        /**
         *
         */
        MQ_RECEIVE("Mq_receive"), //$NON-NLS-1$
        /**
         *
         */
        WAKEUP("Wakeup"), //$NON-NLS-1$
        /**
        *
        */
        IRQ("Irq"), //$NON-NLS-1$
        /**
        *
        */
        PREEMPTED("Preempted in userspace"), //$NON-NLS-1$
        /**
        *
        */
        OTHER("Preempted in other syscall"), //$NON-NLS-1$
        /**
         *
         */
        SOFTIRQ("Softirq"), //$NON-NLS-1$
        /**
         *
         */
        HRTIMER("HRTimer"), //$NON-NLS-1$
        ;

        /** Name */
        public final String name;

        private Type(String name) {
            this.name = name;
        }
    }

    private int fTid;
    private String fAdditionalInfo;
    private String fValue;
    private Type fType;

    /**
     * Constructor
     *
     * @param name
     *            The name of the task
     * @param startTime
     *            The start time of this execution
     * @param endTime
     *            The end time of this execution
     * @param tid
     *            Tid of the task
     * @param information
     *            Additional information to display
     * @param type
     *            The type of the execution
     * @param value
     *            The value to check for additional information
     */
    public ExtendedEntry(String name, long startTime, long endTime, int tid, String information, Type type, String value) {
        super(name, startTime, endTime);
        fTid = tid;
        fValue = value;
        setAdditionalInfo(information);
        setType(type);
        addEvent(new TimeEvent(this, 0, endTime - startTime, 0));
    }

    /**
     * @return The tid
     */
    public int getTid() {
        return fTid;
    }

    /**
     * @return The information
     */
    public String getAdditionalInfo() {
        return fAdditionalInfo;
    }

    /**
     * @param fAdditionalInfo
     *            Additional information to display
     */
    public void setAdditionalInfo(String fAdditionalInfo) {
        this.fAdditionalInfo = fAdditionalInfo;
    }

    /**
     * @return Type
     */
    public Type getType() {
        return fType;
    }

    /**
     * @param type
     *            The type
     */
    public void setType(Type type) {
        this.fType = type;
    }

    /**
     * @return value
     */
    public String getValue() {
        return fValue;
    }
}
