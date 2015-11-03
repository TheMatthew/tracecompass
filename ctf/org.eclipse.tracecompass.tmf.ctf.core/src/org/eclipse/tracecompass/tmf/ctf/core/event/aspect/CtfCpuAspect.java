/*******************************************************************************
 * Copyright (c) 2014, 2015 Ericsson
 *
 * All rights reserved. This program and the accompanying materials are
 * made available under the terms of the Eclipse Public License v1.0 which
 * accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *   Alexandre Montplaisir - Initial API and implementation
 *******************************************************************************/

package org.eclipse.tracecompass.tmf.ctf.core.event.aspect;

import org.eclipse.jdt.annotation.NonNull;
import org.eclipse.tracecompass.tmf.core.event.ITmfEvent;
import org.eclipse.tracecompass.tmf.core.event.aspect.TmfCpuAspect;
import org.eclipse.tracecompass.tmf.ctf.core.event.CtfTmfEvent;

/**
 * "CPU" event aspect for CTF traces.
 *
 * @author Alexandre Montplaisir
 */
public class CtfCpuAspect extends TmfCpuAspect {

    /**
     * Instance
     *
     * @since 2.0
     */
    public static final @NonNull CtfCpuAspect INSTANCE = new CtfCpuAspect();

    private CtfCpuAspect() {
        // do nothing
    }

    @Override
    public Integer resolve(ITmfEvent event) {
        if (!(event instanceof CtfTmfEvent)) {
            return null;
        }
        int cpu = ((CtfTmfEvent) event).getCPU();
        return cpu;
    }
}
