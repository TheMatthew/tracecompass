/*******************************************************************************
 * Copyright (c) 2015 Ericsson
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/
package org.eclipse.tracecompass.internal.ctf.core.event.metadata.tsdl;

import java.util.List;

import org.antlr.runtime.tree.CommonTree;
import org.eclipse.tracecompass.ctf.core.event.metadata.DeclarationScope;
import org.eclipse.tracecompass.ctf.core.event.types.IDeclaration;
import org.eclipse.tracecompass.internal.ctf.core.event.metadata.AbstractScopedCommonTreeParser;
import org.eclipse.tracecompass.internal.ctf.core.event.metadata.ParseException;

public class TypeDeclarationParser extends AbstractScopedCommonTreeParser {

    public static final class Param implements ICommonTreeParserParameter {
        private final DeclarationScope fDeclarationScope;
        private final List<CommonTree> fPointerList;

        public Param(List<CommonTree> pointerList, DeclarationScope scope) {
            fPointerList = pointerList;
            fDeclarationScope = scope;
        }
    }

    public final static TypeDeclarationParser INSTANCE = new TypeDeclarationParser();

    private TypeDeclarationParser() {
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
    public IDeclaration parse(CommonTree typeSpecifierList, ICommonTreeParserParameter param, String errorMsg) throws ParseException {
        if (!(param instanceof Param)) {
            throw new IllegalArgumentException("Param must be of the type Param"); //$NON-NLS-1$
        }
        setScope(((Param) param).fDeclarationScope);

        List<CommonTree> pointerList = ((Param) param).fPointerList;
        /* Create the string representation of the type declaration */
        String typeStringRepresentation = TypeDeclarationStringParser.INSTANCE.parse(typeSpecifierList, new TypeDeclarationStringParser.Param(pointerList), null);

        /*
         * Use the string representation to search the type in the current scope
         */
        IDeclaration decl = getCurrentScope().lookupTypeRecursive(
                typeStringRepresentation);

        if (decl == null) {
            throw new ParseException("Type " + typeStringRepresentation //$NON-NLS-1$
                    + " has not been defined."); //$NON-NLS-1$
        }

        return decl;
    }

}
