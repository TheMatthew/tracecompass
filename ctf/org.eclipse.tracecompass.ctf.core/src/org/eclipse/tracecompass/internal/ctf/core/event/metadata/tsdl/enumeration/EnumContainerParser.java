/*******************************************************************************
 * Copyright (c) 2015 Ericsson
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/

package org.eclipse.tracecompass.internal.ctf.core.event.metadata.tsdl.enumeration;

import org.antlr.runtime.tree.CommonTree;
import org.eclipse.tracecompass.ctf.core.event.metadata.DeclarationScope;
import org.eclipse.tracecompass.ctf.core.event.types.IDeclaration;
import org.eclipse.tracecompass.ctf.core.event.types.IntegerDeclaration;
import org.eclipse.tracecompass.internal.ctf.core.event.metadata.AbstractScopedCommonTreeParser;
import org.eclipse.tracecompass.internal.ctf.core.event.metadata.ParseException;

public class EnumContainerParser extends AbstractScopedCommonTreeParser {

    public static final class Param implements ICommonTreeParserParameter {

        private final DeclarationScope fCurrentScope;

        /**
         * @param currentScope
         */
        public Param(DeclarationScope currentScope) {
            fCurrentScope = currentScope;
        }

    }

    public static final EnumContainerParser INSTANCE = new EnumContainerParser();

    private EnumContainerParser() {
    }

    /**
     * Parses an enum container type node and returns the corresponding integer
     * type.
     *
     * @param enumContainerType
     *            An ENUM_CONTAINER_TYPE node.
     * @return An integer declaration corresponding to the container type.
     * @throws ParseException
     *             If the type does not parse correctly or if it is not an
     *             integer type.
     */
    @Override
    public IntegerDeclaration parse(CommonTree enumContainerType, ICommonTreeParserParameter param, String errorMsg) throws ParseException {
        if (!(param instanceof Param)) {
            throw new IllegalArgumentException("param must be of type EnumBodyParser.Param"); //$NON-NLS-1$
        }
        Param parameter = (Param) param;
        setScope(parameter.fCurrentScope);

        /* Get the child, which should be a type specifier list */
        CommonTree typeSpecifierList = (CommonTree) enumContainerType.getChild(0);

        /* Parse it and get the corresponding declaration */
        IDeclaration decl = parseTypeSpecifierList(typeSpecifierList);

        /* If is is an integer, return it, else throw an error */
        if (decl instanceof IntegerDeclaration) {
            return (IntegerDeclaration) decl;
        }
        throw new ParseException("enum container type must be an integer"); //$NON-NLS-1$

    }

}
