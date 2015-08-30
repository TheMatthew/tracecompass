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
 * Singed status, whether an integer is capable of accepting negative values or
 * not.
 *
 * @author Matthew Khouzam
 */
public class SignedParser implements ICommonTreeParser {
    private static final String INVALID_BOOLEAN_VALUE = "Invalid boolean value "; //$NON-NLS-1$

    private static final ICommonTreeParser INTEGER_PARSER = new UnaryIntegerParser();

    @Override
    public Boolean parse(CommonTree tree, Object param, String errorMsg) throws ParseException {
        boolean ret = false;
        CommonTree firstChild = (CommonTree) tree.getChild(0);

        if (isUnaryString(firstChild)) {
            String strval = concatenateUnaryStrings(tree.getChildren());

            if (strval.equals(MetadataStrings.TRUE)
                    || strval.equals(MetadataStrings.TRUE2)) {
                ret = true;
            } else if (strval.equals(MetadataStrings.FALSE)
                    || strval.equals(MetadataStrings.FALSE2)) {
                ret = false;
            } else {
                throw new ParseException(INVALID_BOOLEAN_VALUE
                        + firstChild.getChild(0).getText());
            }
        } else if (isUnaryInteger(firstChild)) {
            /* Happens if the value is something like "1234.hello" */
            if (tree.getChildCount() > 1) {
                throw new ParseException("Invalid boolean value"); //$NON-NLS-1$
            }

            long intval = (Long) INTEGER_PARSER.parse(firstChild, null, null);

            if (intval == 1) {
                ret = true;
            } else if (intval == 0) {
                ret = false;
            } else {
                throw new ParseException(INVALID_BOOLEAN_VALUE
                        + firstChild.getChild(0).getText());
            }
        } else {
            throw new ParseException(INVALID_BOOLEAN_VALUE);
        }
        return ret;
    }

}
