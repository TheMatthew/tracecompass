/*******************************************************************************
 * Copyright (c) 2015 Ericsson
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/
package org.eclipse.tracecompass.internal.ctf.core.event.metadata.tsdl.variant;

import static org.eclipse.tracecompass.internal.ctf.core.event.metadata.TsdlUtils.childTypeError;

import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.antlr.runtime.tree.CommonTree;
import org.eclipse.tracecompass.ctf.core.event.metadata.DeclarationScope;
import org.eclipse.tracecompass.ctf.core.event.types.IDeclaration;
import org.eclipse.tracecompass.ctf.core.event.types.VariantDeclaration;
import org.eclipse.tracecompass.ctf.core.trace.CTFTrace;
import org.eclipse.tracecompass.ctf.parser.CTFParser;
import org.eclipse.tracecompass.internal.ctf.core.event.metadata.AbstractScopedCommonTreeParser;
import org.eclipse.tracecompass.internal.ctf.core.event.metadata.MetadataStrings;
import org.eclipse.tracecompass.internal.ctf.core.event.metadata.ParseException;
import org.eclipse.tracecompass.internal.ctf.core.event.metadata.tsdl.TypeAliasParser;
import org.eclipse.tracecompass.internal.ctf.core.event.metadata.tsdl.TypedefParser;

public class VariantBodyParser extends AbstractScopedCommonTreeParser {

    public static final class Param implements ICommonTreeParserParameter {
        private final DeclarationScope fDeclarationScope;
        private final String fName;
        private final VariantDeclaration fVariantDeclaration;
        private final CTFTrace fTrace;

        public Param(VariantDeclaration variantDeclaration, CTFTrace trace, String name, DeclarationScope scope) {
            fVariantDeclaration = variantDeclaration;
            fTrace = trace;
            fDeclarationScope = scope;
            fName = name;
        }
    }

    public final static VariantBodyParser INSTANCE = new VariantBodyParser();

    private VariantBodyParser() {
    }

    @Override
    public VariantDeclaration parse(CommonTree variantBody, ICommonTreeParserParameter param) throws ParseException {
        if (!(param instanceof Param)) {
            throw new IllegalArgumentException("Param must be of the type Param"); //$NON-NLS-1$
        }
        setScope(((Param) param).fDeclarationScope);
        String variantName = ((Param) param).fName;
        VariantDeclaration variantDeclaration = ((Param) param).fVariantDeclaration;
        List<CommonTree> variantDeclarations = variantBody.getChildren();

        pushNamedScope(variantName, MetadataStrings.VARIANT);
        CTFTrace trace = ((Param) param).fTrace;
        for (CommonTree declarationNode : variantDeclarations) {
            switch (declarationNode.getType()) {
            case CTFParser.TYPEALIAS:
                TypeAliasParser.INSTANCE.parse(declarationNode, new TypeAliasParser.Param(trace, getCurrentScope()));
                break;
            case CTFParser.TYPEDEF:
                Map<String, IDeclaration> decs = TypedefParser.INSTANCE.parse(declarationNode, new TypedefParser.Param(trace, getCurrentScope()));
                for (Entry<String, IDeclaration> declarationEntry : decs.entrySet()) {
                    variantDeclaration.addField(declarationEntry.getKey(), declarationEntry.getValue());
                }
                break;
            case CTFParser.SV_DECLARATION:
                VariantDeclarationParser.INSTANCE.parse(declarationNode, new VariantDeclarationParser.Param(variantDeclaration, trace, getCurrentScope()));
                break;
            default:
                throw childTypeError(declarationNode);
            }
        }

        popScope();
        return variantDeclaration;
    }

}
