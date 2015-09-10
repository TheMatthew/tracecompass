/*******************************************************************************
 * Copyright (c) 2015 Ericsson
 *
 * All rights reserved. This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License v1.0 which
 * accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/

package org.eclipse.tracecompass.internal.ctf.core.event.metadata;

import static org.eclipse.tracecompass.common.core.NonNullUtils.checkNotNull;

import org.antlr.runtime.tree.CommonTree;
import org.eclipse.tracecompass.ctf.parser.CTFParser;

/**
 * Unary String Parser, along with Unary integer parser, one of the two most
 * basic parsers in TSDL
 *
 * @author Matthew Khouzam
 *
 */
public final class UnaryStringParser implements ICommonTreeParser {

    /** Instance */
    public static final UnaryStringParser INSTANCE = new UnaryStringParser();

    private UnaryStringParser() {
    }

    /**
     * Parses a unary string node and return the string value.
     *
     * @param unaryString
     *            The unary string node to parse (type UNARY_EXPRESSION_STRING
     *            or UNARY_EXPRESSION_STRING_QUOTES).
     * @return The string value.
     */
    /*
     * It would be really nice to remove the quotes earlier, such as in the
     * parser.
     */
    @Override
    public String parse(CommonTree unaryString, Object notUsed, String errorMsg) {
        CommonTree value = (CommonTree) unaryString.getChild(0);
        if (value.getType() == CTFParser.UNARY_EXPRESSION_STRING) {
            value = (CommonTree) value.getChild(0);
        }
        String strval = value.getText();

        /* Remove quotes */
        if (unaryString.getType() == CTFParser.UNARY_EXPRESSION_STRING_QUOTES) {
            strval = strval.substring(1, strval.length() - 1);
        }

        return checkNotNull(strval);
    }

}
