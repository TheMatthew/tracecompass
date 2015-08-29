/*******************************************************************************
 * Copyright (c) 2015 Ericsson
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/
package org.eclipse.tracecompass.internal.ctf.core.event.metadata.tsdl.stream;

import static org.eclipse.tracecompass.internal.ctf.core.event.metadata.TsdlUtils.isUnaryInteger;

import org.antlr.runtime.tree.CommonTree;
import org.eclipse.tracecompass.internal.ctf.core.event.metadata.ICommonTreeParser;
import org.eclipse.tracecompass.internal.ctf.core.event.metadata.ParseException;
import org.eclipse.tracecompass.internal.ctf.core.event.metadata.tsdl.UnaryIntegerParser;

/**
 *
 * @author Matthew Khouzam
 *
 */
public class StreamIdParser implements ICommonTreeParser {

    /** Instance */
    public static final StreamIdParser INSTANCE = new StreamIdParser();

    private StreamIdParser() {
    }

    @Override
    public Object parse(CommonTree tree, ICommonTreeParserParameter param) throws ParseException {
        CommonTree firstChild = (CommonTree) tree.getChild(0);
        if (isUnaryInteger(firstChild)) {
            if (tree.getChildCount() > 1) {
                throw new ParseException("invalid value for stream id"); //$NON-NLS-1$
            }
            long intval = UnaryIntegerParser.INSTANCE.parse(firstChild, null);
            return intval;
        }
        throw new ParseException("invalid value for stream id"); //$NON-NLS-1$
    }

}
