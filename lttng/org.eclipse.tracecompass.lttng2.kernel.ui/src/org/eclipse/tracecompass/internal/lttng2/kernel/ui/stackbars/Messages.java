/*******************************************************************************
 * Copyright (c) 2013 École Polytechnique de Montréal
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Geneviève Bastien - Initial API and implementation
 *******************************************************************************/

package org.eclipse.tracecompass.internal.lttng2.kernel.ui.stackbars;

import org.eclipse.osgi.util.NLS;

@SuppressWarnings("javadoc")
public class Messages {
    private static final String BUNDLE_NAME = "org.eclipse.linuxtools.internal.lttng2.kernel.ui.stackbars.messages"; //$NON-NLS-1$

    public static String StackbarsView_multipleStates;

    public static String StackbarsView_stateTypeName;

    public static String StackbarsView_columnRankOrder;

    public static String StackbarsView_columnElapsed;

    public static String StackbarsView_columnTid;

    public static String StackbarsView_columnName;

    public static String StackbarsView_columnRunningTime;

    public static String StackbarsView_columnPreemptedTime;

    public static String StackbarsView_columnRankDuration;

    public static String StackbarsModule_waitingForGraph;

    public static String StackbarsView_columnStartingTime;

    public static String CPComplement_Tid;

    public static String CPComplement_Duration;

    public static String CPComplement_Priority;

    public static String Extended_Type;

    public static String Extended_Id;

    public static String Extended_Duration;

    public static String Extended_Information;

    static {
        // initialize resource bundle
        NLS.initializeMessages(BUNDLE_NAME, Messages.class);
    }

    private Messages() {
    }

}
