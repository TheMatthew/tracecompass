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

import org.eclipse.linuxtools.tmf.ui.widgets.timegraph.model.TimeGraphEntry;


/**
 * An entry in the Stack bars view
 */
public class ExtendedTimeEntry extends TimeGraphEntry {

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
        MQ("Mq"), //$NON-NLS-1$
        /**
         *
         */
        HRTIMER("HRTimer"), //$NON-NLS-1$
        ;

        /** Name */
        public final String name;

        private Type (String name) {
            this.name = name;
        }
    }

    private String fAdditionalInfo;
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
     * @param information
     *            Additional information to display
     * @param type
     *            The type of the execution
     */
    public ExtendedTimeEntry(String name, long startTime, long endTime, String information, Type type) {
        super(name, startTime, endTime);
        fAdditionalInfo = information;
        fType = type;
    }

    /**
     * @return
     *          The information
     */
    public String getAdditionalInfo() {
        return fAdditionalInfo;
    }

    /**
     * @return
     *      Type
     */
    public Type getType() {
        return fType;
    }
}
