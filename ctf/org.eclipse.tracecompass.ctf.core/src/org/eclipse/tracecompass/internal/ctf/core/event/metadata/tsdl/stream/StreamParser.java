/*******************************************************************************
 * Copyright (c) 2015 Ericsson
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/

package org.eclipse.tracecompass.internal.ctf.core.event.metadata.tsdl.stream;

import static org.eclipse.tracecompass.internal.ctf.core.event.metadata.TsdlUtils.childTypeError;

import java.util.List;

import org.antlr.runtime.tree.CommonTree;
import org.eclipse.tracecompass.ctf.core.event.metadata.DeclarationScope;
import org.eclipse.tracecompass.ctf.core.trace.CTFStream;
import org.eclipse.tracecompass.ctf.core.trace.CTFTrace;
import org.eclipse.tracecompass.ctf.parser.CTFParser;
import org.eclipse.tracecompass.internal.ctf.core.event.metadata.AbstractScopedCommonTreeParser;
import org.eclipse.tracecompass.internal.ctf.core.event.metadata.MetadataStrings;
import org.eclipse.tracecompass.internal.ctf.core.event.metadata.ParseException;
import org.eclipse.tracecompass.internal.ctf.core.event.metadata.tsdl.TypeAliasParser;
import org.eclipse.tracecompass.internal.ctf.core.event.metadata.tsdl.TypedefParser;

public class StreamParser extends AbstractScopedCommonTreeParser {

    public static final class Param implements ICommonTreeParserParameter {

        private final DeclarationScope fCurrentScope;
        private final CTFTrace fTrace;

        /**
         * @param currentScope
         */
        public Param(CTFTrace trace, DeclarationScope currentScope) {
            fTrace = trace;
            fCurrentScope = currentScope;
        }

    }

    public static final StreamParser INSTANCE = new StreamParser();

    private StreamParser() {
    }

    /**
     * Parses an enum declaration and returns the corresponding declaration.
     * @param theEnum
     *            An ENUM node.
     *
     * @return The corresponding enum declaration.
     * @throws ParseException
     */
    @Override
    public CTFStream parse(CommonTree streamNode, ICommonTreeParserParameter param) throws ParseException {
        if (!(param instanceof Param)) {
            throw new IllegalArgumentException("param must be of type EnumBodyParser.Param"); //$NON-NLS-1$
        }
        Param parameter = (Param) param;
        setScope(parameter.fCurrentScope);
        CTFTrace trace = ((Param) param).fTrace;
        CTFStream stream = new CTFStream(trace);

        List<CommonTree> children = streamNode.getChildren();
        if (children == null) {
            throw new ParseException("Empty stream block"); //$NON-NLS-1$
        }

        pushScope(MetadataStrings.STREAM);

        for (CommonTree child : children) {
            switch (child.getType()) {
            case CTFParser.TYPEALIAS:
                TypeAliasParser.INSTANCE.parse(child, new TypeAliasParser.Param(trace, getCurrentScope()));
                break;
            case CTFParser.TYPEDEF:
                TypedefParser.INSTANCE.parse(child, new TypedefParser.Param(trace, getCurrentScope()));
                break;
            case CTFParser.CTF_EXPRESSION_TYPE:
            case CTFParser.CTF_EXPRESSION_VAL:
                StreamDeclarationParser.INSTANCE.parse(child, new StreamDeclarationParser.Param(trace, stream, getCurrentScope()));
                break;
            default:
                throw childTypeError(child);
            }
        }

        if (stream.isIdSet() &&
                (!trace.packetHeaderIsSet() || !trace.getPacketHeader().hasField(MetadataStrings.STREAM_ID))) {
            throw new ParseException("Stream has an ID, but there is no stream_id field in packet header."); //$NON-NLS-1$
        }

        trace.addStream(stream);

        popScope();
        return stream;
    }

}
