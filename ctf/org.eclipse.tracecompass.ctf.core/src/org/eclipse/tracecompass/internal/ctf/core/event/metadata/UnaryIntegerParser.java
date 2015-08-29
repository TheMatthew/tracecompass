/*******************************************************************************
 * Copyright (c) 2015 Ericsson
 *
 * All rights reserved. This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License v1.0 which
 * accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/

package org.eclipse.tracecompass.internal.ctf.core.event.metadata;

import java.util.List;

import org.antlr.runtime.tree.CommonTree;
import org.eclipse.tracecompass.internal.ctf.core.event.metadata.exceptions.ParseException;

/**
 * Unary String Parser, along with Unary integer parser, one of the two most
 * basic parsers in TSDL
 *
 * @author Matthew Khouzam
 *
 */
public final class UnaryIntegerParser implements ICommonTreeParser {

    /**
     * Parses an unary integer (dec, hex or oct).
     *
     * @param unaryInteger
     *            An unary integer node.
     * @return The integer value.
     * @throws ParseException
     *             on an invalid integer format ("bob" for example)
     */
    @Override
    public Long parse(CommonTree unaryInteger, Object notUsed, String errorMsg) throws ParseException {
        List<CommonTree> children = unaryInteger.getChildren();
        CommonTree value = children.get(0);
        String strval = value.getText();

        long intval;
        try {
            intval = Long.decode(strval);
        } catch (NumberFormatException e) {
            throw new ParseException("Invalid integer format: " + strval, e); //$NON-NLS-1$
        }

        /* The rest of children are sign */
        if ((children.size() % 2) == 0) {
            return -intval;
        }
        return intval;
    }

}
