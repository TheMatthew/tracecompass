/*******************************************************************************
 * Copyright (c) 2015 Ericsson
 *
 * All rights reserved. This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License v1.0 which
 * accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/

package org.eclipse.tracecompass.internal.ctf.core.event.metadata;

import org.antlr.runtime.tree.CommonTree;
import org.eclipse.tracecompass.internal.ctf.core.event.metadata.exceptions.ParseException;

/**
 * Common tree parser interface. Should only have one method
 * {@link #parse(CommonTree, Object, String)}
 *
 * It is recommended to add to the javadoc on this inerface as it is not
 * specific
 *
 * @author Matthew Khouzam
 *
 */
public interface ICommonTreeParser {
    /**
     * The only parse method of the common tree parser. Caution must be used
     * handling this as it can return any type and thus care must be used with
     * the input and output.
     *
     *
     * @param tree
     *            the common tree input
     * @param param
     *            the parameter to pass (for lookups)
     * @param errorMsg
     *            the error message customizer
     * @return the parsed data
     * @throws ParseException
     *             if the tree or data is wrong
     */
    Object parse(CommonTree tree, Object param, String errorMsg) throws ParseException;

}
