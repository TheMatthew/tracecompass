/*******************************************************************************
 * Copyright (c) 2015 Ericsson
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/
package org.eclipse.tracecompass.internal.ctf.core.event.metadata;

import java.util.List;

import org.antlr.runtime.tree.CommonTree;
import org.eclipse.tracecompass.ctf.core.event.metadata.DeclarationScope;
import org.eclipse.tracecompass.ctf.core.event.types.IDeclaration;
import org.eclipse.tracecompass.ctf.core.event.types.VariantDeclaration;
import org.eclipse.tracecompass.ctf.parser.CTFParser;

public class VariantDeclarationParser extends AbstractScopedCommonTreeParser {

    public static final class Param implements ICommonTreeParserParameter {
        private final VariantDeclaration fVariant;
        private final DeclarationScope fDeclarationScope;

        public Param(VariantDeclaration variant, DeclarationScope scope) {
            fVariant = variant;
            fDeclarationScope = scope;
        }
    }

    public final static VariantDeclarationParser INSTANCE = new VariantDeclarationParser();

    private VariantDeclarationParser() {
    }

    @Override
    public VariantDeclaration parse(CommonTree declaration, ICommonTreeParserParameter param, String errorMsg) throws ParseException {
        if (!(param instanceof Param)) {
            throw new IllegalArgumentException("Param must be of the type Param"); //$NON-NLS-1$
        }
        VariantDeclaration variant = ((Param) param).fVariant;
        setScope(((Param) param).fDeclarationScope);
        /* Get the type specifier list node */
        CommonTree typeSpecifierListNode = (CommonTree) declaration.getFirstChildWithType(CTFParser.TYPE_SPECIFIER_LIST);

        /* Get the type declarator list node */
        CommonTree typeDeclaratorListNode = (CommonTree) declaration.getFirstChildWithType(CTFParser.TYPE_DECLARATOR_LIST);

        /* Get the type declarator list */
        List<CommonTree> typeDeclaratorList = typeDeclaratorListNode.getChildren();

        /*
         * For each type declarator, parse the declaration and add a field to
         * the variant
         */
        for (CommonTree typeDeclaratorNode : typeDeclaratorList) {

            StringBuilder identifierSB = new StringBuilder();

            IDeclaration decl = parseTypeDeclarator(typeDeclaratorNode,
                    typeSpecifierListNode, identifierSB);

            String name = identifierSB.toString();

            if (variant.hasField(name)) {
                throw new ParseException("variant: duplicate field " //$NON-NLS-1$
                        + name);
            }

            getCurrentScope().registerIdentifier(name, decl);

            variant.addField(name, decl);
        }
        return variant;
    }

}
