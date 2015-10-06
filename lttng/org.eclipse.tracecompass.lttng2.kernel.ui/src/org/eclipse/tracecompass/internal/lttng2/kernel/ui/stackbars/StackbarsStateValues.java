/*******************************************************************************
 * Copyright (c) 2012, 2013 Ericsson
 *
 * All rights reserved. This program and the accompanying materials are
 * made available under the terms of the Eclipse Public License v1.0 which
 * accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *   Alexandre Montplaisir - Initial API and implementation
 ******************************************************************************/

package org.eclipse.tracecompass.internal.lttng2.kernel.ui.stackbars;


/**
 * State values that are used in the kernel event handler. It's much better to
 * use integer values whenever possible, since those take much less space in the
 * history file.
 *
 * @author alexmont
 *
 */
@SuppressWarnings("javadoc")
public interface StackbarsStateValues {

    /* Stackbars status */
    int STATUS_AFTER_START = 0;
    int STATUS_AFTER_END = 1;
    int STATUS_AFTER_SCHED_WAKE = 2;
    int STATUS_AFTER_SCHED_SWITCH_NEXT = 3;
    int STATUS_AFTER_SCHED_SWITCH_PREV = 4;

}
