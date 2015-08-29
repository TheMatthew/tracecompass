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
import static org.eclipse.tracecompass.internal.ctf.core.event.metadata.TsdlUtils.isUnaryString;

import java.nio.ByteOrder;

import org.antlr.runtime.tree.CommonTree;
import org.eclipse.tracecompass.ctf.core.trace.CTFTrace;
import org.eclipse.tracecompass.internal.ctf.core.event.metadata.exceptions.ParseException;

/**
 * TSDL byte order parser
 *
 * @author Matthew Khouzam
 *
 */
public final class ByteOrderParser implements ICommonTreeParser {

    private static final String INVALID_VALUE_FOR_BYTE_ORDER = "Invalid value for byte order"; //$NON-NLS-1$

    /**
     * Gets the value of a "byte_order" integer attribute.
     *
     * @param byteOrderTree
     *            A CTF_RIGHT node.
     * @return The "byte_order" value.
     * @throws ParseException
     *             if the value is invalid
     */
    @Override
    public final ByteOrder parse(CommonTree byteOrderTree, Object param, String errorMsg) throws ParseException {
        if (!(param instanceof CTFTrace)) {
            throw new IllegalArgumentException("Parameter must be a CTFTrace"); //$NON-NLS-1$
        }
        CTFTrace trace = (CTFTrace) param;
        CommonTree firstChild = (CommonTree) byteOrderTree.getChild(0);

        if (isUnaryString(firstChild)) {
            String strval = concatenateUnaryStrings(byteOrderTree.getChildren());

            if (strval.equals(MetadataStrings.LE)) {
                return ByteOrder.LITTLE_ENDIAN;
            } else if (strval.equals(MetadataStrings.BE)
                    || strval.equals(MetadataStrings.NETWORK)) {
                return ByteOrder.BIG_ENDIAN;
            } else if (strval.equals(MetadataStrings.NATIVE)) {
                return trace.getByteOrder();
            } else {
                throw new ParseException(INVALID_VALUE_FOR_BYTE_ORDER);
            }
        }
        throw new ParseException(INVALID_VALUE_FOR_BYTE_ORDER);
    }
}
