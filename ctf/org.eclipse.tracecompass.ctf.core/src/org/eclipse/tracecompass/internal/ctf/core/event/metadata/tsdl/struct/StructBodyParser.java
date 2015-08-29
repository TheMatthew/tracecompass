/*******************************************************************************
 * Copyright (c) 2015 Ericsson
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/
package org.eclipse.tracecompass.internal.ctf.core.event.metadata.tsdl.struct;

import static org.eclipse.tracecompass.internal.ctf.core.event.metadata.TsdlUtils.childTypeError;

import java.util.Collections;
import java.util.List;

import org.antlr.runtime.tree.CommonTree;
import org.eclipse.tracecompass.ctf.core.event.metadata.DeclarationScope;
import org.eclipse.tracecompass.ctf.core.event.types.StructDeclaration;
import org.eclipse.tracecompass.ctf.core.trace.CTFTrace;
import org.eclipse.tracecompass.ctf.parser.CTFParser;
import org.eclipse.tracecompass.internal.ctf.core.event.metadata.AbstractScopedCommonTreeParser;
import org.eclipse.tracecompass.internal.ctf.core.event.metadata.MetadataStrings;
import org.eclipse.tracecompass.internal.ctf.core.event.metadata.ParseException;
import org.eclipse.tracecompass.internal.ctf.core.event.metadata.tsdl.TypeAliasParser;
import org.eclipse.tracecompass.internal.ctf.core.event.metadata.tsdl.TypedefParser;

public class StructBodyParser extends AbstractScopedCommonTreeParser {

    public static final class Param implements ICommonTreeParserParameter {
        private final DeclarationScope fDeclarationScope;
        private final String fName;
        private final StructDeclaration fStructDeclaration;
        private final CTFTrace fTrace;

        public Param(StructDeclaration structDeclaration, CTFTrace trace, String name, DeclarationScope scope) {
            fStructDeclaration = structDeclaration;
            fTrace = trace;
            fDeclarationScope = scope;
            fName = name;
        }
    }

    public final static StructBodyParser INSTANCE = new StructBodyParser();

    private StructBodyParser() {
    }

    @Override
    public StructDeclaration parse(CommonTree structBody, ICommonTreeParserParameter param) throws ParseException {
        if (!(param instanceof Param)) {
            throw new IllegalArgumentException("Param must be of the type Param"); //$NON-NLS-1$
        }
        setScope(((Param) param).fDeclarationScope);
        String structName = ((Param) param).fName;
        StructDeclaration structDeclaration = ((Param) param).fStructDeclaration;
        List<CommonTree> structDeclarations = structBody.getChildren();
        if (structDeclarations == null) {
            structDeclarations = Collections.emptyList();
        }

        /*
         * If structDeclaration is null, structBody has no children and the
         * struct body is empty.
         */
        pushNamedScope(structName, MetadataStrings.STRUCT);
        CTFTrace trace = ((Param)param).fTrace;

        for (CommonTree declarationNode : structDeclarations) {
            switch (declarationNode.getType()) {
            case CTFParser.TYPEALIAS:
                TypeAliasParser.INSTANCE.parse(declarationNode, new TypeAliasParser.Param(trace , getCurrentScope()));
                break;
            case CTFParser.TYPEDEF:
                TypedefParser.INSTANCE.parse(declarationNode, new TypedefParser.Param(trace, getCurrentScope()));
                StructDeclarationParser.INSTANCE.parse(declarationNode, new StructDeclarationParser.Param(structDeclaration, trace, getCurrentScope()));
                break;
            case CTFParser.SV_DECLARATION:
                StructDeclarationParser.INSTANCE.parse(declarationNode, new StructDeclarationParser.Param(structDeclaration, trace, getCurrentScope()));
                break;
            default:
                throw childTypeError(declarationNode);
            }
        }
        popScope();
        return structDeclaration;
    }

}
