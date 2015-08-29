/*******************************************************************************
 * Copyright (c) 2015 Ericsson
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/

package org.eclipse.tracecompass.internal.ctf.core.event.metadata.tsdl.trace;

import static org.eclipse.tracecompass.common.core.NonNullUtils.checkNotNull;
import static org.eclipse.tracecompass.internal.ctf.core.event.metadata.TsdlUtils.isAnyUnaryString;

import java.util.UUID;

import org.antlr.runtime.tree.CommonTree;
import org.eclipse.tracecompass.internal.ctf.core.event.metadata.ICommonTreeParser;
import org.eclipse.tracecompass.internal.ctf.core.event.metadata.ParseException;
import org.eclipse.tracecompass.internal.ctf.core.event.metadata.tsdl.UnaryStringParser;

/**
 * UUID parser
 *
 * @author Matthew Khouzam
 *
 */
public class UUIDParser implements ICommonTreeParser {

    /** Instance */
    public static final UUIDParser INSTANCE = new UUIDParser();

    private UUIDParser() {
    }

    @Override
    public Object parse(CommonTree tree, ICommonTreeParserParameter param) throws ParseException {

        CommonTree firstChild = (CommonTree) tree.getChild(0);

        if (isAnyUnaryString(firstChild)) {
            if (tree.getChildCount() > 1) {
                throw new ParseException("Invalid value for UUID"); //$NON-NLS-1$
            }

            String uuidstr = UnaryStringParser.INSTANCE.parse(firstChild, null);

            try {
                return checkNotNull(UUID.fromString(uuidstr));
            } catch (IllegalArgumentException e) {
                throw new ParseException("Invalid format for UUID", e); //$NON-NLS-1$
            }
        }
        throw new ParseException("Invalid value for UUID"); //$NON-NLS-1$
    }

}
