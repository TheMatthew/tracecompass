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
     *
     * @param PointerList
     *            A TYPE_SPECIFIER node.
     * @return A StringBuilder to which will be appended the string.
     * @throws ParseException
     *             invalid node
     */
    @Override
    public String parse(CommonTree typeSpecifierList, ICommonTreeParserParameter param, String errorMsg) throws ParseException {
        if (!(param instanceof Param)) {
            throw new IllegalArgumentException("Param must be a List<CommonTree>");
        }
        Param param2 = (Param) param;
        List<CommonTree> pointers = param2.fList;
        CommonTree temp = new CommonTree();
        temp.addChildren(pointers);
        StringBuilder sb = new StringBuilder();

        sb.append(TypeSpecifierListStringParser.INSTANCE.parse(typeSpecifierList, null, null));
        sb.append(PointerListStringParser.INSTANCE.parse(temp, null, null));

        return sb.toString();
    }

}
