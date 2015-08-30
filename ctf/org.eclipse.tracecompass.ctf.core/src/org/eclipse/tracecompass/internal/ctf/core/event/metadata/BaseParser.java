/*******************************************************************************
 * Copyright (c) 2015 Ericsson
 *
 * All rights reserved. This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License v1.0 which
 * accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/

package org.eclipse.tracecompass.internal.ctf.core.event.metadata;

import static org.eclipse.tracecompass.internal.ctf.core.event.metadata.TsdlUtils.concatenateUnaryStrings;
import static org.eclipse.tracecompass.internal.ctf.core.event.metadata.TsdlUtils.isUnaryInteger;
import static org.eclipse.tracecompass.internal.ctf.core.event.metadata.TsdlUtils.isUnaryString;

import org.antlr.runtime.tree.CommonTree;
import org.eclipse.tracecompass.internal.ctf.core.event.metadata.exceptions.ParseException;

/**
 * Parse the base of an integer, can return 16, 10, 8 or 2
 *
 * @author Matthew Khouzam
 */
public final class BaseParser implements ICommonTreeParser {

    private static final String INVALID_VALUE_FOR_BASE = "Invalid value for base"; //$NON-NLS-1$
    private static final int INTEGER_BASE_16 = 16;
    private static final int INTEGER_BASE_10 = 10;
    private static final int INTEGER_BASE_8 = 8;
    private static final int INTEGER_BASE_2 = 2;

    private static final UnaryIntegerParser INTEGER_PARSER = new UnaryIntegerParser();

    @Override
    public Integer parse(CommonTree tree, Object param, String errorMsg) throws ParseException {

        CommonTree firstChild = (CommonTree) tree.getChild(0);

        if (isUnaryInteger(firstChild)) {
            if (tree.getChildCount() > 1) {
                throw new ParseException("invalid base value"); //$NON-NLS-1$
            }

            long intval = INTEGER_PARSER.parse(firstChild, null, null);
            if ((intval == INTEGER_BASE_2) || (intval == INTEGER_BASE_8) || (intval == INTEGER_BASE_10)
                    || (intval == INTEGER_BASE_16)) {
                return (int) intval;
            }
            throw new ParseException(INVALID_VALUE_FOR_BASE);
        } else if (isUnaryString(firstChild)) {
            switch (concatenateUnaryStrings(tree.getChildren())) {
            case MetadataStrings.DECIMAL:
            case MetadataStrings.DEC:
            case MetadataStrings.DEC_CTE:
            case MetadataStrings.INT_MOD:
            case MetadataStrings.UNSIGNED_CTE:
                return INTEGER_BASE_10;
            case MetadataStrings.HEXADECIMAL:
            case MetadataStrings.HEX:
            case MetadataStrings.X:
            case MetadataStrings.X2:
            case MetadataStrings.POINTER:
                return INTEGER_BASE_16;
            case MetadataStrings.OCT:
            case MetadataStrings.OCTAL:
            case MetadataStrings.OCTAL_CTE:
                return INTEGER_BASE_8;
            case MetadataStrings.BIN:
            case MetadataStrings.BINARY:
                return INTEGER_BASE_2;
            default:
                throw new ParseException(INVALID_VALUE_FOR_BASE);
            }
        } else {
            throw new ParseException(INVALID_VALUE_FOR_BASE);
        }
    }

}
