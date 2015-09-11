/*******************************************************************************
 * Copyright (c) 2015 Ericsson
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/
package org.eclipse.tracecompass.internal.ctf.core.event.metadata;

import static org.eclipse.tracecompass.internal.ctf.core.event.metadata.TsdlUtils.childTypeError;
import static org.eclipse.tracecompass.internal.ctf.core.event.metadata.TsdlUtils.concatenateUnaryStrings;
import static org.eclipse.tracecompass.internal.ctf.core.event.metadata.TsdlUtils.isAnyUnaryString;
import static org.eclipse.tracecompass.internal.ctf.core.event.metadata.TsdlUtils.isUnaryInteger;

import java.util.Collections;
import java.util.LinkedList;
import java.util.List;

import org.antlr.runtime.tree.CommonTree;
import org.eclipse.tracecompass.ctf.core.event.metadata.DeclarationScope;
import org.eclipse.tracecompass.ctf.core.event.types.IDeclaration;
import org.eclipse.tracecompass.ctf.core.event.types.IntegerDeclaration;
import org.eclipse.tracecompass.ctf.parser.CTFParser;
import org.eclipse.tracecompass.internal.ctf.core.event.metadata.tsdl.event.EventScopeParser;
import org.eclipse.tracecompass.internal.ctf.core.event.types.ArrayDeclaration;
import org.eclipse.tracecompass.internal.ctf.core.event.types.SequenceDeclaration;

public class TypeDeclaratorParser extends AbstractScopedCommonTreeParser {

    public static final class Param implements ICommonTreeParserParameter {
        private final DeclarationScope fDeclarationScope;
        private final CommonTree fListNode;
        private final StringBuilder fBuilder;

        public Param(CommonTree listNode, DeclarationScope scope, StringBuilder builder) {
            fListNode = listNode;
            fDeclarationScope = scope;
            fBuilder = builder;
        }
    }

    public final static TypeDeclaratorParser INSTANCE = new TypeDeclaratorParser();

    private TypeDeclaratorParser() {
    }

    /**
     * Parses a pair type declarator / type specifier list and returns the
     * corresponding declaration. If it is present, it also writes the
     * identifier of the declarator in the given {@link StringBuilder}.
     *
     * @param typeDeclarator
     *            A TYPE_DECLARATOR node.
     * @param typeSpecifierList
     *            A TYPE_SPECIFIER_LIST node.
     * @param identifierSB
     *            A StringBuilder that will receive the identifier found in the
     *            declarator.
     * @return The corresponding declaration.
     * @throws ParseException
     *             If there is an error finding or creating the declaration.
     */
    @Override
    public IDeclaration parse(CommonTree typeDeclarator, ICommonTreeParserParameter param, String errorMsg) throws ParseException {
        if (!(param instanceof Param)) {
            throw new IllegalArgumentException("Param must be of the type Param"); //$NON-NLS-1$
        }
        setScope(((Param) param).fDeclarationScope);
        CommonTree typeSpecifierList = ((Param) param).fListNode;

        IDeclaration declaration = null;
        List<CommonTree> children = null;
        List<CommonTree> pointers = new LinkedList<>();
        List<CommonTree> lengths = new LinkedList<>();
        CommonTree identifier = null;

        /* Separate the tokens by type */
        if (typeDeclarator != null) {
            children = typeDeclarator.getChildren();
            for (CommonTree child : children) {

                switch (child.getType()) {
                case CTFParser.POINTER:
                    pointers.add(child);
                    break;
                case CTFParser.IDENTIFIER:
                    identifier = child;
                    break;
                case CTFParser.LENGTH:
                    lengths.add(child);
                    break;
                default:
                    throw childTypeError(child);
                }
            }

        }

        /*
         * Parse the type specifier list, which is the "base" type. For example,
         * it would be int in int a[3][len].
         */
        declaration = parseTypeSpecifierList(typeSpecifierList, pointers, identifier);

        /*
         * Each length subscript means that we must create a nested array or
         * sequence. For example, int a[3][len] means that we have an array of 3
         * (sequences of length 'len' of (int)).
         */
        if (!lengths.isEmpty()) {
            /* We begin at the end */
            Collections.reverse(lengths);

            for (CommonTree length : lengths) {
                /*
                 * By looking at the first expression, we can determine whether
                 * it is an array or a sequence.
                 */
                List<CommonTree> lengthChildren = length.getChildren();

                CommonTree first = lengthChildren.get(0);
                if (isUnaryInteger(first)) {
                    /* Array */
                    int arrayLength = UnaryIntegerParser.INSTANCE.parse(first, null, null).intValue();

                    if (arrayLength < 1) {
                        throw new ParseException("Array length is negative"); //$NON-NLS-1$
                    }

                    /* Create the array declaration. */
                    declaration = new ArrayDeclaration(arrayLength, declaration);
                } else if (isAnyUnaryString(first)) {
                    /* Sequence */
                    String lengthName = concatenateUnaryStrings(lengthChildren);

                    /* check that lengthName was declared */
                    if (isSignedIntegerField(lengthName)) {
                        throw new ParseException("Sequence declared with length that is not an unsigned integer"); //$NON-NLS-1$
                    }
                    /* Create the sequence declaration. */
                    declaration = new SequenceDeclaration(lengthName,
                            declaration);
                } else if (isTrace(first)) {
                    /* Sequence */
                    String lengthName = TraceScopeParser.INSTANCE.parse(null, new TraceScopeParser.Param(lengthChildren), null);

                    /* check that lengthName was declared */
                    if (isSignedIntegerField(lengthName)) {
                        throw new ParseException("Sequence declared with length that is not an unsigned integer"); //$NON-NLS-1$
                    }
                    /* Create the sequence declaration. */
                    declaration = new SequenceDeclaration(lengthName,
                            declaration);

                } else if (isStream(first)) {
                    /* Sequence */
                    String lengthName = StreamScopeParser.INSTANCE.parse(null, new StreamScopeParser.Param(lengthChildren), null);

                    /* check that lengthName was declared */
                    if (isSignedIntegerField(lengthName)) {
                        throw new ParseException("Sequence declared with length that is not an unsigned integer"); //$NON-NLS-1$
                    }
                    /* Create the sequence declaration. */
                    declaration = new SequenceDeclaration(lengthName,
                            declaration);
                } else if (isEvent(first)) {
                    /* Sequence */
                    String lengthName = EventScopeParser.INSTANCE.parse(null, new EventScopeParser.Param(lengthChildren), null);

                    /* check that lengthName was declared */
                    if (isSignedIntegerField(lengthName)) {
                        throw new ParseException("Sequence declared with length that is not an unsigned integer"); //$NON-NLS-1$
                    }
                    /* Create the sequence declaration. */
                    declaration = new SequenceDeclaration(lengthName,
                            declaration);
                } else {
                    throw childTypeError(first);
                }
            }
        }

        if (identifier != null) {
            final String text = identifier.getText();
            ((Param) param).fBuilder.append(text);
            registerType(declaration, text);
        }

        return declaration;
    }

    private boolean isSignedIntegerField(String lengthName) throws ParseException {
        IDeclaration decl = getCurrentScope().lookupIdentifierRecursive(lengthName);
        if (decl instanceof IntegerDeclaration) {
            return ((IntegerDeclaration) decl).isSigned();
        }
        throw new ParseException("Is not an integer: " + lengthName); //$NON-NLS-1$

    }

    private static boolean isEvent(CommonTree first) {
        return first.getType() == CTFParser.EVENT;
    }

    private static boolean isStream(CommonTree first) {
        return first.getType() == CTFParser.STREAM;
    }

    private static boolean isTrace(CommonTree first) {
        return first.getType() == CTFParser.TRACE;
    }

}
