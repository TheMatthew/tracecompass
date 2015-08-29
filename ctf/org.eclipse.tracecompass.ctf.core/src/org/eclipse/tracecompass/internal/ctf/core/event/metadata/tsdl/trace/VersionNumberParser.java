/*******************************************************************************
 * Copyright (c) 2015 Ericsson
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/

package org.eclipse.tracecompass.internal.ctf.core.event.metadata.tsdl.trace;

import static org.eclipse.tracecompass.internal.ctf.core.event.metadata.TsdlUtils.isUnaryInteger;

import org.antlr.runtime.tree.CommonTree;
import org.eclipse.tracecompass.internal.ctf.core.event.metadata.ICommonTreeParser;
import org.eclipse.tracecompass.internal.ctf.core.event.metadata.ParseException;
import org.eclipse.tracecompass.internal.ctf.core.event.metadata.tsdl.UnaryIntegerParser;

public final class VersionNumberParser implements ICommonTreeParser {

    private static final String ERROR = "Invalid value for major/minor"; //$NON-NLS-1$
    public static final VersionNumberParser INSTANCE = new VersionNumberParser();

    private VersionNumberParser() {
    }

    @Override
    public Long parse(CommonTree tree, ICommonTreeParserParameter param) throws ParseException {

        CommonTree firstChild = (CommonTree) tree.getChild(0);

        if (isUnaryInteger(firstChild)) {
            if (tree.getChildCount() > 1) {
                throw new ParseException(ERROR);
            }
            long version = UnaryIntegerParser.INSTANCE.parse(firstChild, null);
            if (version < 0) {
                throw new ParseException(ERROR);
            }
            return version;
        }
        throw new ParseException(ERROR);
    }

}
