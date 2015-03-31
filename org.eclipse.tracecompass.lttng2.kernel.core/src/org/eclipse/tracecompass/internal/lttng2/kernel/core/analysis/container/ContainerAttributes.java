/*******************************************************************************
 * Copyright (c) 2012, 2015 Ericsson
 *
 * All rights reserved. This program and the accompanying materials are
 * made available under the terms of the Eclipse Public License v1.0 which
 * accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *   Alexandre Montplaisir - Initial API and implementation
 ******************************************************************************/

package org.eclipse.tracecompass.internal.lttng2.kernel.core.analysis.container;

/**
 * This file defines all the attribute names used in the handler. Both the
 * construction and query steps should use them.
 *
 * These should not be externalized! The values here are used as-is in the
 * history file on disk, so they should be kept the same to keep the file format
 * compatible. If a view shows attribute names directly, the localization should
 * be done on the viewer side.
 *
 * @author Francis Jolivet
 *
 */
@SuppressWarnings({"nls", "javadoc"})
public interface ContainerAttributes {

    static final String ROOT = "Container Root";
    static final String CONTAINER_ID = "Container ";

    static final String CONTAINER_CPU = "Container CPU";
    /* Sub-attributes of the CPU section*/
    //CPUs are represented by numbers, each containing the following sub-attr :
    static final String RUNNING_VTID = "Running_vtid";
    static final String STATUS = "Status";

    static final String CONTAINER_INFO = "Container info";
    /* Sub-attributes of the Container_info */
    static final String HOSTNAME = "Hostname";
    static final String CONTAINER_INODE = "ns_INode";
    static final String PPID = "PPID";

    static final String CONTAINER_TASKS = "Container tasks";
    static final String REAL_TID = "REAL_TID";
    static final String EXEC_NAME = "Exec_name";

    static final String CONTAINERS_SECTION = "Nested Containers";
}
