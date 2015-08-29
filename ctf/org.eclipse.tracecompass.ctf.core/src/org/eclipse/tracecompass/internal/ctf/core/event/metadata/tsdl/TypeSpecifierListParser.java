/*******************************************************************************
 * Copyright (c) 2015 Ericsson
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/
package org.eclipse.tracecompass.internal.ctf.core.event.metadata.tsdl;

import static org.eclipse.tracecompass.internal.ctf.core.event.metadata.TsdlUtils.childTypeError;

import java.nio.ByteOrder;
import java.util.List;

import org.antlr.runtime.tree.CommonTree;
import org.eclipse.tracecompass.ctf.core.event.metadata.DeclarationScope;
import org.eclipse.tracecompass.ctf.core.event.types.EnumDeclaration;
import org.eclipse.tracecompass.ctf.core.event.types.IDeclaration;
import org.eclipse.tracecompass.ctf.core.event.types.StructDeclaration;
import org.eclipse.tracecompass.ctf.core.trace.CTFTrace;
import org.eclipse.tracecompass.ctf.parser.CTFParser;
import org.eclipse.tracecompass.internal.ctf.core.event.metadata.AbstractScopedCommonTreeParser;
import org.eclipse.tracecompass.internal.ctf.core.event.metadata.ParseException;
import org.eclipse.tracecompass.internal.ctf.core.event.metadata.tsdl.enumeration.EnumParser;
import org.eclipse.tracecompass.internal.ctf.core.event.metadata.tsdl.floatingpoint.FloatDeclarationParser;
import org.eclipse.tracecompass.internal.ctf.core.event.metadata.tsdl.integer.IntegerDeclarationParser;
import org.eclipse.tracecompass.internal.ctf.core.event.metadata.tsdl.string.StringDeclarationParser;
import org.eclipse.tracecompass.internal.ctf.core.event.metadata.tsdl.struct.StructParser;
import org.eclipse.tracecompass.internal.ctf.core.event.metadata.tsdl.variant.VariantParser;
import org.eclipse.tracecompass.internal.ctf.core.event.types.composite.EventHeaderCompactDeclaration;
import org.eclipse.tracecompass.internal.ctf.core.event.types.composite.EventHeaderLargeDeclaration;

public class TypeSpecifierListParser extends AbstractScopedCommonTreeParser {

    public static final class Param implements ICommonTreeParserParameter {
        private final DeclarationScope fDeclarationScope;
        private final List<CommonTree> fListNode;
        private final CTFTrace fTrace;
        private final CommonTree fIdentifier;

        public Param(CTFTrace trace, List<CommonTree> listNode, CommonTree identifier , DeclarationScope scope) {
            fTrace = trace;
            fListNode = listNode;
            fIdentifier = identifier;
            fDeclarationScope = scope;
        }
    }

    public final static TypeSpecifierListParser INSTANCE = new TypeSpecifierListParser();

    private TypeSpecifierListParser() {
    }

    /**
     * Parses a type specifier list and returns the corresponding declaration.
     * @param typeSpecifierList
     *            A TYPE_SPECIFIER_LIST node.
     * @param pointerList
     *            A list of POINTER nodes that apply to the specified type.
     *
     * @return The corresponding declaration.
     * @throws ParseException
     *             If the type has not been defined or if there is an error
     *             creating the declaration.
     */
    @Override
    public IDeclaration parse(CommonTree typeSpecifierList, ICommonTreeParserParameter param) throws ParseException {
        if (!(param instanceof Param)) {
            throw new IllegalArgumentException("Param must be of the type Param"); //$NON-NLS-1$
        }
        setScope(((Param) param).fDeclarationScope);
        List<CommonTree> pointerList = ((Param) param).fListNode;
        CTFTrace trace = ((Param) param).fTrace;
        CommonTree identifier= ((Param) param).fIdentifier;
        IDeclaration declaration = null;

        /*
         * By looking at the first element of the type specifier list, we can
         * determine which type it belongs to.
         */
        CommonTree firstChild = (CommonTree) typeSpecifierList.getChild(0);

        switch (firstChild.getType()) {
        case CTFParser.FLOATING_POINT:
            declaration = (IDeclaration) FloatDeclarationParser.INSTANCE.parse(firstChild, new FloatDeclarationParser.Param(trace));
            break;
        case CTFParser.INTEGER:
            declaration = IntegerDeclarationParser.INSTANCE.parse(firstChild, new IntegerDeclarationParser.Param(trace));
            break;
        case CTFParser.STRING:
            declaration = StringDeclarationParser.INSTANCE.parse(firstChild, null);
            break;
        case CTFParser.STRUCT:
            declaration = StructParser.INSTANCE.parse(firstChild, new StructParser.Param(trace, identifier, getCurrentScope()));
            StructDeclaration structDeclaration = (StructDeclaration) declaration;
            IDeclaration idEnumDecl = structDeclaration.getFields().get("id"); //$NON-NLS-1$
            if (idEnumDecl instanceof EnumDeclaration) {
                EnumDeclaration enumDeclaration = (EnumDeclaration) idEnumDecl;
                ByteOrder bo = enumDeclaration.getContainerType().getByteOrder();
                if (EventHeaderCompactDeclaration.getEventHeader(bo).isCompactEventHeader(structDeclaration)) {
                    declaration = EventHeaderCompactDeclaration.getEventHeader(bo);
                } else if (EventHeaderLargeDeclaration.getEventHeader(bo).isLargeEventHeader(structDeclaration)) {
                    declaration = EventHeaderLargeDeclaration.getEventHeader(bo);
                }
            }
            break;
        case CTFParser.VARIANT:
            declaration = VariantParser.INSTANCE.parse(firstChild, new VariantParser.Param(trace, getCurrentScope()));
            break;
        case CTFParser.ENUM:
            declaration = EnumParser.INSTANCE.parse(firstChild, new EnumParser.Param(trace, getCurrentScope()));
            break;
        case CTFParser.IDENTIFIER:
        case CTFParser.FLOATTOK:
        case CTFParser.INTTOK:
        case CTFParser.LONGTOK:
        case CTFParser.SHORTTOK:
        case CTFParser.SIGNEDTOK:
        case CTFParser.UNSIGNEDTOK:
        case CTFParser.CHARTOK:
        case CTFParser.DOUBLETOK:
        case CTFParser.VOIDTOK:
        case CTFParser.BOOLTOK:
        case CTFParser.COMPLEXTOK:
        case CTFParser.IMAGINARYTOK:
            declaration = TypeDeclarationParser.INSTANCE.parse(typeSpecifierList, new TypeDeclarationParser.Param(pointerList, getCurrentScope()));
            break;
        default:
            throw childTypeError(firstChild);
        }

        return declaration;
    }
}
