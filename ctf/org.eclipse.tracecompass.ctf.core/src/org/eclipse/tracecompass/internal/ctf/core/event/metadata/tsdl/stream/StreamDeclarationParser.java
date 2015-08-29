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
import static org.eclipse.tracecompass.internal.ctf.core.event.metadata.TsdlUtils.isAnyUnaryString;

import java.nio.ByteOrder;
import java.util.List;

import org.antlr.runtime.tree.CommonTree;
import org.antlr.runtime.tree.Tree;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.tracecompass.ctf.core.CTFStrings;
import org.eclipse.tracecompass.ctf.core.event.metadata.DeclarationScope;
import org.eclipse.tracecompass.ctf.core.event.types.IDeclaration;
import org.eclipse.tracecompass.ctf.core.event.types.IEventHeaderDeclaration;
import org.eclipse.tracecompass.ctf.core.event.types.StructDeclaration;
import org.eclipse.tracecompass.ctf.core.trace.CTFStream;
import org.eclipse.tracecompass.ctf.core.trace.CTFTrace;
import org.eclipse.tracecompass.ctf.parser.CTFParser;
import org.eclipse.tracecompass.internal.ctf.core.Activator;
import org.eclipse.tracecompass.internal.ctf.core.event.metadata.AbstractScopedCommonTreeParser;
import org.eclipse.tracecompass.internal.ctf.core.event.metadata.Messages;
import org.eclipse.tracecompass.internal.ctf.core.event.metadata.MetadataStrings;
import org.eclipse.tracecompass.internal.ctf.core.event.metadata.ParseException;
import org.eclipse.tracecompass.internal.ctf.core.event.metadata.tsdl.ByteOrderParser;
import org.eclipse.tracecompass.internal.ctf.core.event.metadata.tsdl.TypeSpecifierListParser;

public final class StreamDeclarationParser extends AbstractScopedCommonTreeParser {

    public static final class Param implements ICommonTreeParserParameter {
        private final CTFStream fStream;
        private final CTFTrace fTrace;
        private final DeclarationScope fDeclarationScope;

        public Param(CTFTrace trace, CTFStream stream, DeclarationScope scope) {
            fTrace = trace;
            fStream = stream;
            fDeclarationScope = scope;
        }

    }

    /**
     * Instance
     */
    public static final StreamDeclarationParser INSTANCE = new StreamDeclarationParser();

    private StreamDeclarationParser() {
    }

    @Override
    public CTFStream parse(CommonTree streamDecl, ICommonTreeParserParameter param) throws ParseException {
        if (!(param instanceof Param)) {
            throw new IllegalArgumentException("Param must be of type Param"); //$NON-NLS-1$
        }
        setScope(((Param) param).fDeclarationScope);
        CTFStream stream = ((Param) param).fStream;
        CTFTrace fTrace = ((Param) param).fTrace;

        /* There should be a left and right */

        CommonTree leftNode = (CommonTree) streamDecl.getChild(0);
        CommonTree rightNode = (CommonTree) streamDecl.getChild(1);

        List<CommonTree> leftStrings = leftNode.getChildren();

        if (!isAnyUnaryString(leftStrings.get(0))) {
            throw new ParseException("Left side of CTF assignment must be a string"); //$NON-NLS-1$
        }

        String left = concatenateUnaryStrings(leftStrings);

        if (left.equals(MetadataStrings.ID)) {
            if (stream.isIdSet()) {
                throw new ParseException("stream id already defined"); //$NON-NLS-1$
            }

            long streamID = (long) StreamIdParser.INSTANCE.parse(rightNode, null);

            stream.setId(streamID);
        } else if (left.equals(MetadataStrings.BYTE_ORDER)) {
            if (stream.isStreamByteOrderSet()) {
                throw new ParseException("stream byte order already defined"); //$NON-NLS-1$
            }

            ByteOrder byteOrder = ByteOrderParser.INSTANCE.parse(rightNode, new ByteOrderParser.Param(fTrace));

            stream.setByteOrder(byteOrder);
        } else if (left.equals(MetadataStrings.EVENT_HEADER)) {
            if (stream.isEventHeaderSet()) {
                throw new ParseException("event.header already defined"); //$NON-NLS-1$
            }

            CommonTree typeSpecifier = (CommonTree) rightNode.getChild(0);

            if (typeSpecifier.getType() != CTFParser.TYPE_SPECIFIER_LIST) {
                throw new ParseException("event.header expects a type specifier"); //$NON-NLS-1$
            }

            IDeclaration eventHeaderDecl = TypeSpecifierListParser.INSTANCE.parse(typeSpecifier, new TypeSpecifierListParser.Param(fTrace, null, null, getCurrentScope()));
            DeclarationScope scope = getCurrentScope();
            DeclarationScope eventHeaderScope = lookupStructName(typeSpecifier, scope);
            if (eventHeaderScope == null) {
                throw new ParseException("event.header scope not found"); //$NON-NLS-1$
            }
            pushScope(MetadataStrings.EVENT);
            getCurrentScope().addChild(eventHeaderScope);
            eventHeaderScope.setName(CTFStrings.HEADER);
            popScope();
            if (eventHeaderDecl instanceof StructDeclaration) {
                stream.setEventHeader((StructDeclaration) eventHeaderDecl);
            } else if (eventHeaderDecl instanceof IEventHeaderDeclaration) {
                stream.setEventHeader((IEventHeaderDeclaration) eventHeaderDecl);
            } else {
                throw new ParseException("event.header expects a struct"); //$NON-NLS-1$
            }

        } else if (left.equals(MetadataStrings.EVENT_CONTEXT)) {
            if (stream.isEventContextSet()) {
                throw new ParseException("event.context already defined"); //$NON-NLS-1$
            }

            CommonTree typeSpecifier = (CommonTree) rightNode.getChild(0);

            if (typeSpecifier.getType() != CTFParser.TYPE_SPECIFIER_LIST) {
                throw new ParseException("event.context expects a type specifier"); //$NON-NLS-1$
            }

            IDeclaration eventContextDecl = TypeSpecifierListParser.INSTANCE.parse(typeSpecifier, new TypeSpecifierListParser.Param(fTrace, null, null, getCurrentScope()));

            if (!(eventContextDecl instanceof StructDeclaration)) {
                throw new ParseException("event.context expects a struct"); //$NON-NLS-1$
            }

            stream.setEventContext((StructDeclaration) eventContextDecl);
        } else if (left.equals(MetadataStrings.PACKET_CONTEXT)) {
            if (stream.isPacketContextSet()) {
                throw new ParseException("packet.context already defined"); //$NON-NLS-1$
            }

            CommonTree typeSpecifier = (CommonTree) rightNode.getChild(0);

            if (typeSpecifier.getType() != CTFParser.TYPE_SPECIFIER_LIST) {
                throw new ParseException("packet.context expects a type specifier"); //$NON-NLS-1$
            }

            IDeclaration packetContextDecl = TypeSpecifierListParser.INSTANCE.parse(typeSpecifier, new TypeSpecifierListParser.Param(fTrace, null, null, getCurrentScope()));

            if (!(packetContextDecl instanceof StructDeclaration)) {
                throw new ParseException("packet.context expects a struct"); //$NON-NLS-1$
            }

            stream.setPacketContext((StructDeclaration) packetContextDecl);
        } else {
            Activator.log(IStatus.WARNING, Messages.IOStructGen_UnknownStreamAttributeWarning + " " + left); //$NON-NLS-1$
        }
        return stream;
    }

    private static DeclarationScope lookupStructName(CommonTree typeSpecifier, DeclarationScope scope) {
        /*
         * This needs a struct.struct_name.name to work, luckily, that is 99.99%
         * of traces we receive.
         */
        final Tree potentialStruct = typeSpecifier.getChild(0);
        DeclarationScope eventHeaderScope = null;
        if (potentialStruct.getType() == (CTFParser.STRUCT)) {
            final Tree potentialStructName = potentialStruct.getChild(0);
            if (potentialStructName.getType() == (CTFParser.STRUCT_NAME)) {
                final String name = potentialStructName.getChild(0).getText();
                eventHeaderScope = scope.lookupChildRecursive(name);
            }
        }
        /*
         * If that fails, maybe the struct is anonymous
         */
        if (eventHeaderScope == null) {
            eventHeaderScope = scope.lookupChildRecursive(MetadataStrings.STRUCT);
        }
        /*
         * This can still be null
         */
        return eventHeaderScope;
    }

}
