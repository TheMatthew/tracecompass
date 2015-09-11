/*******************************************************************************
 * Copyright (c) 2015 Ericsson
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/
package org.eclipse.tracecompass.internal.ctf.core.event.metadata.tsdl.stream;

import static org.eclipse.tracecompass.internal.ctf.core.event.metadata.TsdlUtils.concatenateUnaryStrings;
import static org.eclipse.tracecompass.internal.ctf.core.event.metadata.TsdlUtils.isUnaryString;

import java.util.List;

import org.antlr.runtime.tree.CommonTree;
import org.eclipse.tracecompass.ctf.parser.CTFParser;
import org.eclipse.tracecompass.internal.ctf.core.event.metadata.ICommonTreeParser;
import org.eclipse.tracecompass.internal.ctf.core.event.metadata.MetadataStrings;
import org.eclipse.tracecompass.internal.ctf.core.event.metadata.ParseException;
import org.eclipse.tracecompass.internal.ctf.core.event.metadata.tsdl.UnaryStringParser;
import org.eclipse.tracecompass.internal.ctf.core.event.metadata.tsdl.event.EventScopeParser;

public final class StreamScopeParser implements ICommonTreeParser {

    public static final class Param implements ICommonTreeParserParameter{
        private final List<CommonTree> fList;

        public Param(List<CommonTree> list){
            fList = list;
        }

    }

    /**
     * Instance
     */
    public static final StreamScopeParser INSTANCE = new StreamScopeParser();

    private StreamScopeParser() {
    }

    @Override
    public String parse(CommonTree unused, ICommonTreeParserParameter param, String errorMsg) throws ParseException {
        if(!(param instanceof Param)){
            throw new IllegalArgumentException("Param must be of type Param"); //$NON-NLS-1$
        }
        List<CommonTree> lengthChildren = ((Param) param).fList;
        List<CommonTree> sublist = lengthChildren.subList(1, lengthChildren.size());

        CommonTree nextElem = (CommonTree) lengthChildren.get(1).getChild(0);
        String lengthName = null;
        if (isUnaryString(nextElem)) {
            lengthName = UnaryStringParser.INSTANCE.parse(nextElem, null, null);
        }

        int type = nextElem.getType();
        if ((CTFParser.tokenNames[CTFParser.EVENT]).equals(lengthName)) {
            type = CTFParser.EVENT;
        }
        switch (type) {
        case CTFParser.IDENTIFIER:
            lengthName = concatenateUnaryStrings(sublist);
            break;
        case CTFParser.EVENT:
            lengthName = EventScopeParser.INSTANCE.parse(null, new EventScopeParser.Param(sublist), null);
            break;
        default:
            if (lengthName == null) {
                throw new ParseException("Unsupported scope stream." + nextElem); //$NON-NLS-1$
            }
        }
        return MetadataStrings.STREAM + '.' + lengthName;
    }

}
