/*******************************************************************************
 * Copyright (c) 2015 Ericsson
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/
package org.eclipse.tracecompass.internal.ctf.core.event.metadata.tsdl.event;

import static org.eclipse.tracecompass.internal.ctf.core.event.metadata.TsdlUtils.concatenateUnaryStrings;

import java.util.List;

import org.antlr.runtime.tree.CommonTree;
import org.eclipse.tracecompass.ctf.parser.CTFParser;
import org.eclipse.tracecompass.internal.ctf.core.event.metadata.ICommonTreeParser;
import org.eclipse.tracecompass.internal.ctf.core.event.metadata.MetadataStrings;
import org.eclipse.tracecompass.internal.ctf.core.event.metadata.ParseException;

public final class EventScopeParser implements ICommonTreeParser {

    public static final class Param implements ICommonTreeParserParameter{
        private final List<CommonTree> fList;

        public Param(List<CommonTree> list){
            fList = list;

        }

    }

    /**
     * Instance
     */
    public static final EventScopeParser INSTANCE = new EventScopeParser();

    private EventScopeParser() {
    }

    @Override
    public String parse(CommonTree unused, ICommonTreeParserParameter param, String errorMsg) throws ParseException {
        if(!(param instanceof Param)){
            throw new IllegalArgumentException("Param must be of type Param"); //$NON-NLS-1$
        }
        List<CommonTree> lengthChildren = ((Param) param).fList;
        CommonTree nextElem = (CommonTree) lengthChildren.get(1).getChild(0);
        String lengthName;
        switch (nextElem.getType()) {
        case CTFParser.UNARY_EXPRESSION_STRING:
        case CTFParser.IDENTIFIER:
            List<CommonTree> sublist = lengthChildren.subList(1, lengthChildren.size());
            lengthName = MetadataStrings.EVENT + '.' + concatenateUnaryStrings(sublist);
            break;
        default:
            throw new ParseException("Unsupported scope event." + nextElem); //$NON-NLS-1$
        }
        return lengthName;
    }

}
