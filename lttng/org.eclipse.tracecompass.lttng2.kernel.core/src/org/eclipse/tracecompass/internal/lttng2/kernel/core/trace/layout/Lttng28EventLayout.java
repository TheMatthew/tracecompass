/*******************************************************************************
 * Copyright (c) 2015 Ericsson, École Polytechnique de Montréal
 *
 * All rights reserved. This program and the accompanying materials are
 * made available under the terms of the Eclipse Public License v1.0 which
 * accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 ******************************************************************************/

package org.eclipse.tracecompass.internal.lttng2.kernel.core.trace.layout;

/**
 * This file defines all the known event and field names for LTTng kernel
 * traces, for versions of lttng-modules 2.8 and above.
 *
 * @author Francis Giraldeau
 */
public class Lttng28EventLayout extends Lttng27EventLayout {

    private static final String SCHED_WAKING = "sched_waking";

    /**
     * Constructor
     */
    protected Lttng28EventLayout() {
    }

    private static final Lttng28EventLayout INSTANCE = new Lttng28EventLayout();

    public static Lttng28EventLayout getInstance() {
        return INSTANCE;
    }

    // ------------------------------------------------------------------------
    // New definitions in LTTng 2.8
    // ------------------------------------------------------------------------

    @Override
    public String eventSchedProcessWaking() {
        return SCHED_WAKING;
    }

}
