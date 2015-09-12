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
import org.eclipse.tracecompass.common.core.NonNullUtils;
import org.eclipse.tracecompass.internal.ctf.core.event.metadata.ICommonTreeParser;
import org.eclipse.tracecompass.internal.ctf.core.event.metadata.ParseException;
import org.eclipse.tracecompass.internal.ctf.core.event.metadata.PointerListStringParser;

public class TypeDeclarationStringParser implements ICommonTreeParser {

    public static final class Param implements ICommonTreeParserParameter {
        private final List<CommonTree> fList;

        public Param(List<CommonTree> list) {
            fList = list;
        }
    }

    /**
     * Instance
     */
    public static final TypeDeclarationStringParser INSTANCE = new TypeDeclarationStringParser();

    private TypeDeclarationStringParser() {
    }

    /**
     * Creates the string representation of a type specifier.
     * @param typeSpecifierList
     *            A TYPE_SPECIFIER node.
     *
     * @return A StringBuilder to which will be appended the string.
     * @throws ParseException
     *             invalid node
     */
    @Override
    public String parse(CommonTree typeSpecifierList, ICommonTreeParserParameter param) throws ParseException {
        if (!(param instanceof Param)) {
            throw new IllegalArgumentException("Param must be a " + Param.class.getCanonicalName()); //$NON-NLS-1$
        }
        List<CommonTree> pointers = ((Param) param).fList;
        CommonTree temp = new CommonTree();
        temp.addChildren(pointers);
        StringBuilder sb = new StringBuilder();

        sb.append(TypeSpecifierListStringParser.INSTANCE.parse(typeSpecifierList, null));
        sb.append(PointerListStringParser.INSTANCE.parse(temp, null));

        return NonNullUtils.nullToEmptyString(sb);
    }

}
