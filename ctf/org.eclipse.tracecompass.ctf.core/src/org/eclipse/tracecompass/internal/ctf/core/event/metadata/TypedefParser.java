/*******************************************************************************
 * Copyright (c) 2015 Ericsson
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/
package org.eclipse.tracecompass.internal.ctf.core.event.metadata;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.antlr.runtime.tree.CommonTree;
import org.eclipse.tracecompass.ctf.core.event.metadata.DeclarationScope;
import org.eclipse.tracecompass.ctf.core.event.types.IDeclaration;
import org.eclipse.tracecompass.ctf.core.event.types.VariantDeclaration;
import org.eclipse.tracecompass.ctf.parser.CTFParser;

public class TypedefParser extends AbstractScopedCommonTreeParser {

    public static final class Param implements ICommonTreeParserParameter {
        private final DeclarationScope fDeclarationScope;

        public Param(DeclarationScope scope) {
            fDeclarationScope = scope;
        }
    }

    public final static TypedefParser INSTANCE = new TypedefParser();

    private TypedefParser() {
    }

    /**
     * Parses a type specifier list as a user-declared type.
     *
     * @param typeSpecifierList
     *            A TYPE_SPECIFIER_LIST node containing a user-declared type.
     * @param param
     *            (pointerList, currentscope) A list of POINTER nodes that apply
     *            to the type specified in typeSpecifierList.
     * @return The corresponding declaration.
     * @throws ParseException
     *             If the type does not exist (has not been found).
     */
    @Override
    public Map<String, IDeclaration> parse(CommonTree typedef, ICommonTreeParserParameter param, String errorMsg) throws ParseException {
        if (!(param instanceof Param)) {
            throw new IllegalArgumentException("Param must be of the type Param"); //$NON-NLS-1$
        }
        setScope(((Param) param).fDeclarationScope);

        CommonTree typeDeclaratorListNode = (CommonTree) typedef.getFirstChildWithType(CTFParser.TYPE_DECLARATOR_LIST);

        CommonTree typeSpecifierListNode = (CommonTree) typedef.getFirstChildWithType(CTFParser.TYPE_SPECIFIER_LIST);

        List<CommonTree> typeDeclaratorList = typeDeclaratorListNode.getChildren();

        Map<String, IDeclaration> declarations = new HashMap<>();

        for (CommonTree typeDeclaratorNode : typeDeclaratorList) {
            StringBuilder identifierSB = new StringBuilder();

            IDeclaration typeDeclaration = TypeDeclarationParser.INSTANCE.parse(typeDeclaratorNode, new TypeDeclaratorParser.Param(typeSpecifierListNode, getCurrentScope(), identifierSB), null);

            if ((typeDeclaration instanceof VariantDeclaration)
                    && !((VariantDeclaration) typeDeclaration).isTagged()) {
                throw new ParseException("Typealias of untagged variant is not permitted"); //$NON-NLS-1$
            }

            getCurrentScope().registerType(identifierSB.toString(),
                    typeDeclaration);

            declarations.put(identifierSB.toString(), typeDeclaration);
        }
        return declarations;
    }

}
