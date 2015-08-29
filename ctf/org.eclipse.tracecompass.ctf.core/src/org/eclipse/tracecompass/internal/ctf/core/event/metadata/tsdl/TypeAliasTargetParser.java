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

import java.util.List;

import org.antlr.runtime.tree.CommonTree;
import org.eclipse.tracecompass.ctf.core.event.metadata.DeclarationScope;
import org.eclipse.tracecompass.ctf.core.event.types.IDeclaration;
import org.eclipse.tracecompass.ctf.core.trace.CTFTrace;
import org.eclipse.tracecompass.ctf.parser.CTFParser;
import org.eclipse.tracecompass.internal.ctf.core.event.metadata.AbstractScopedCommonTreeParser;
import org.eclipse.tracecompass.internal.ctf.core.event.metadata.ParseException;

public class TypeAliasTargetParser extends AbstractScopedCommonTreeParser {

    public static final class Param implements ICommonTreeParserParameter {
        private final DeclarationScope fDeclarationScope;
        private final CTFTrace fTrace;

        public Param(CTFTrace trace, DeclarationScope scope) {
            fTrace = trace;
            fDeclarationScope = scope;
        }
    }

    public final static TypeAliasTargetParser INSTANCE = new TypeAliasTargetParser();

    private TypeAliasTargetParser() {
    }

    /**
     * Parses the target part of a typealias and gets the corresponding
     * declaration.
     * @param target
     *            A TYPEALIAS_TARGET node.
     *
     * @return The corresponding declaration.
     * @throws ParseException
     */
    @Override
    public IDeclaration parse(CommonTree target, ICommonTreeParserParameter param) throws ParseException {
        if (!(param instanceof Param)) {
            throw new IllegalArgumentException("Param must be of the type Param"); //$NON-NLS-1$
        }
        setScope(((Param) param).fDeclarationScope);

        List<CommonTree> children = target.getChildren();

        CommonTree typeSpecifierList = null;
        CommonTree typeDeclaratorList = null;
        CommonTree typeDeclarator = null;
        StringBuilder identifierSB = new StringBuilder();

        for (CommonTree child : children) {
            switch (child.getType()) {
            case CTFParser.TYPE_SPECIFIER_LIST:
                typeSpecifierList = child;
                break;
            case CTFParser.TYPE_DECLARATOR_LIST:
                typeDeclaratorList = child;
                break;
            default:
                throw childTypeError(child);
            }
        }

        if (typeDeclaratorList != null) {
            /*
             * Only allow one declarator
             *
             * eg: "typealias uint8_t *, ** := puint8_t;" is not permitted,
             * otherwise the new type puint8_t would maps to two different
             * types.
             */
            if (typeDeclaratorList.getChildCount() != 1) {
                throw new ParseException("Only one type declarator is allowed in the typealias target"); //$NON-NLS-1$
            }

            typeDeclarator = (CommonTree) typeDeclaratorList.getChild(0);
        }
        CTFTrace trace = ((Param) param).fTrace;
        /* Parse the target type and get the declaration */
        IDeclaration targetDeclaration = TypeDeclaratorParser.INSTANCE.parse(typeDeclarator,
                new TypeDeclaratorParser.Param(trace, typeSpecifierList, getCurrentScope(), identifierSB));

        /*
         * We don't allow identifier in the target
         *
         * eg: "typealias uint8_t* hello := puint8_t;", the "hello" is not
         * permitted
         */
        if (identifierSB.length() > 0) {
            throw new ParseException("Identifier (" + identifierSB.toString() //$NON-NLS-1$
                    + ") not expected in the typealias target"); //$NON-NLS-1$
        }

        return targetDeclaration;
    }

}
