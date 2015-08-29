/*******************************************************************************
 * Copyright (c) 2011, 2015 Ericsson, Ecole Polytechnique de Montreal and others
 *
 * All rights reserved. This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License v1.0 which
 * accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Matthew Khouzam - Initial Design and Grammar
 *     Francis Giraldeau - Initial API and implementation
 *     Simon Marchi - Initial API and implementation
 *******************************************************************************/

package org.eclipse.tracecompass.internal.ctf.core.event.metadata;

import static org.eclipse.tracecompass.internal.ctf.core.event.metadata.TsdlUtils.childTypeError;

import java.util.ArrayList;
import java.util.List;

import org.antlr.runtime.tree.CommonTree;
import org.eclipse.jdt.annotation.NonNull;
import org.eclipse.tracecompass.ctf.core.event.CTFClock;
import org.eclipse.tracecompass.ctf.core.event.metadata.DeclarationScope;
import org.eclipse.tracecompass.ctf.core.trace.CTFStream;
import org.eclipse.tracecompass.ctf.core.trace.CTFTrace;
import org.eclipse.tracecompass.ctf.parser.CTFParser;
import org.eclipse.tracecompass.internal.ctf.core.event.metadata.tsdl.TypeAliasParser;
import org.eclipse.tracecompass.internal.ctf.core.event.metadata.tsdl.TypeSpecifierListParser;
import org.eclipse.tracecompass.internal.ctf.core.event.metadata.tsdl.TypedefParser;
import org.eclipse.tracecompass.internal.ctf.core.event.metadata.tsdl.environment.EnvironmentParser;
import org.eclipse.tracecompass.internal.ctf.core.event.metadata.tsdl.event.EventParser;
import org.eclipse.tracecompass.internal.ctf.core.event.metadata.tsdl.stream.StreamParser;
import org.eclipse.tracecompass.internal.ctf.core.event.metadata.tsdl.trace.TraceDeclarationParser;

import com.google.common.collect.Iterables;

/**
 * IOStructGen
 */
public class IOStructGen {

    // ------------------------------------------------------------------------
    // Attributes
    // ------------------------------------------------------------------------

    private static final @NonNull String LINE = "line"; //$NON-NLS-1$
    private static final @NonNull String FILE = "file"; //$NON-NLS-1$
    private static final @NonNull String IP = "ip"; //$NON-NLS-1$
    private static final @NonNull String FUNC = "func"; //$NON-NLS-1$
    private static final @NonNull String NAME = "name"; //$NON-NLS-1$
    private static final @NonNull String EMPTY_STRING = ""; //$NON-NLS-1$

    /**
     * The trace
     */
    private final CTFTrace fTrace;
    private CommonTree fTree;

    /**
     * The current declaration scope.
     */
    private final DeclarationScope fRoot;
    private DeclarationScope fScope;

    /**
     * Data helpers needed for streaming
     */

    private boolean fHasBeenParsed = false;

    // -()-----------------------------------------------------------------------
    // Constructor
    // ------------------------------------------------------------------------

    /**
     * Constructor
     *
     * @param tree
     *            the tree (ANTLR generated) with the parsed TSDL data.
     * @param trace
     *            the trace containing the places to put all the read metadata
     */
    public IOStructGen(CommonTree tree, CTFTrace trace) {
        fTrace = trace;
        fTree = tree;
        fRoot = trace.getScope();
        fScope = fRoot;
    }

    /**
     * Parse the tree and populate the trace defined in the constructor.
     *
     * @throws ParseException
     *             If there was a problem parsing the metadata
     */
    public void generate() throws ParseException {
        parseRoot(fTree);
    }

    /**
     * Parse a partial tree and populate the trace defined in the constructor.
     * Does not check for a "trace" block as there is only one in the trace and
     * thus
     *
     * @throws ParseException
     *             If there was a problem parsing the metadata
     */
    public void generateFragment() throws ParseException {
        parseIncompleteRoot(fTree);
    }

    // ------------------------------------------------------------------------
    // Operations
    // ------------------------------------------------------------------------

    /**
     * Sets a new tree to parse
     *
     * @param newTree
     *            the new tree to parse
     */
    public void setTree(CommonTree newTree) {
        fTree = newTree;
    }

    /**
     * Parse the root node.
     *
     * @param root
     *            A ROOT node.
     * @throws ParseException
     */
    private void parseRoot(CommonTree root) throws ParseException {

        List<CommonTree> children = root.getChildren();

        CommonTree traceNode = null;
        boolean hasStreams = false;
        List<CommonTree> events = new ArrayList<>();
        resetScope();
        for (CommonTree child : children) {
            final int type = child.getType();
            switch (type) {
            case CTFParser.DECLARATION:
                parseRootDeclaration(child);
                break;
            case CTFParser.TRACE:
                if (traceNode != null) {
                    throw new ParseException("Only one trace block is allowed"); //$NON-NLS-1$
                }
                traceNode = child;
                parseTrace(traceNode);
                break;
            case CTFParser.STREAM:
                StreamParser.INSTANCE.parse(child, new StreamParser.Param(fTrace, getCurrentScope()));
                hasStreams = true;
                break;
            case CTFParser.EVENT:
                events.add(child);
                break;
            case CTFParser.CLOCK:
                CTFClock ctfClock = ClockParser.INSTANCE.parse(child, null);
                String nameValue = ctfClock.getName();
                fTrace.addClock(nameValue, ctfClock);
                break;
            case CTFParser.ENV:
                fTrace.setEnvironment(EnvironmentParser.INSTANCE.parse(child, null));
                break;
            case CTFParser.CALLSITE:
                parseCallsite(child);
                break;
            default:
                throw childTypeError(child);
            }
        }
        if (traceNode == null) {
            throw new ParseException("Missing trace block"); //$NON-NLS-1$
        }
        parseEvents(events, hasStreams);
        popScope();
        fHasBeenParsed = true;
    }

    private void parseEvents(List<CommonTree> events, boolean hasStreams) throws ParseException {
        if (!hasStreams && !events.isEmpty()) {
            /* Add an empty stream that will have a null id */
            fTrace.addStream(new CTFStream(fTrace));
        }
        for (CommonTree event : events) {
            EventParser.INSTANCE.parse(event, new EventParser.Param(fTrace, getCurrentScope()));
        }
    }

    private void parseIncompleteRoot(CommonTree root) throws ParseException {
        if (!fHasBeenParsed) {
            throw new ParseException("You need to run generate first"); //$NON-NLS-1$
        }
        List<CommonTree> children = root.getChildren();
        List<CommonTree> events = new ArrayList<>();
        resetScope();
        for (CommonTree child : children) {
            final int type = child.getType();
            switch (type) {
            case CTFParser.DECLARATION:
                parseRootDeclaration(child);
                break;
            case CTFParser.TRACE:
                throw new ParseException("Trace block defined here, please use generate and not generateFragment to parse this fragment"); //$NON-NLS-1$
            case CTFParser.STREAM:
                StreamParser.INSTANCE.parse(child, new StreamParser.Param(fTrace, getCurrentScope()));
                break;
            case CTFParser.EVENT:
                events.add(child);
                break;
            case CTFParser.CLOCK:
                CTFClock ctfClock = ClockParser.INSTANCE.parse(child, null);
                String nameValue = ctfClock.getName();
                fTrace.addClock(nameValue, ctfClock);
                break;
            case CTFParser.ENV:
                fTrace.setEnvironment(EnvironmentParser.INSTANCE.parse(child, null));
                break;
            case CTFParser.CALLSITE:
                parseCallsite(child);
                break;
            default:
                throw childTypeError(child);
            }
        }
        parseEvents(events, !Iterables.isEmpty(fTrace.getStreams()));
        popScope();
    }

    private void resetScope() {
        fScope = fRoot;
    }

    private void parseCallsite(CommonTree callsite) {

        List<CommonTree> children = callsite.getChildren();
        String name = null;
        String funcName = null;
        long lineNumber = -1;
        long ip = -1;
        String fileName = null;

        for (CommonTree child : children) {
            String left;
            /* this is a regex to find the leading and trailing quotes */
            final String regex = "^\"|\"$"; //$NON-NLS-1$
            /*
             * this is to replace the previous quotes with nothing...
             * effectively deleting them
             */
            final String nullString = EMPTY_STRING;
            left = child.getChild(0).getChild(0).getChild(0).getText();
            if (left.equals(NAME)) {
                name = child.getChild(1).getChild(0).getChild(0).getText().replaceAll(regex, nullString);
            } else if (left.equals(FUNC)) {
                funcName = child.getChild(1).getChild(0).getChild(0).getText().replaceAll(regex, nullString);
            } else if (left.equals(IP)) {
                ip = Long.decode(child.getChild(1).getChild(0).getChild(0).getText());
            } else if (left.equals(FILE)) {
                fileName = child.getChild(1).getChild(0).getChild(0).getText().replaceAll(regex, nullString);
            } else if (left.equals(LINE)) {
                lineNumber = Long.parseLong(child.getChild(1).getChild(0).getChild(0).getText());
            }
        }
        fTrace.addCallsite(name, funcName, ip, fileName, lineNumber);
    }

    private void parseTrace(CommonTree traceNode) throws ParseException {

        CTFTrace trace = fTrace;
        List<CommonTree> children = traceNode.getChildren();
        if (children == null) {
            throw new ParseException("Trace block is empty"); //$NON-NLS-1$
        }

        resetScope();

        for (CommonTree child : children) {
            switch (child.getType()) {
            case CTFParser.TYPEALIAS:
                TypeAliasParser.INSTANCE.parse(child, new TypeAliasParser.Param(trace, getCurrentScope()));
                break;
            case CTFParser.TYPEDEF:
                TypedefParser.INSTANCE.parse(child, new TypedefParser.Param(trace, getCurrentScope()));
                break;
            case CTFParser.CTF_EXPRESSION_TYPE:
            case CTFParser.CTF_EXPRESSION_VAL:
                TraceDeclarationParser.INSTANCE.parse(child, new TraceDeclarationParser.Param(fTrace, getCurrentScope()));
                break;
            default:
                throw childTypeError(child);
            }
        }

        /*
         * If trace byte order was not specified and not using packet based
         * metadata
         */
        if (fTrace.getByteOrder() == null) {
            throw new ParseException("Trace byte order not set"); //$NON-NLS-1$
        }
    }

    /**
     * Parses a declaration at the root level.
     *
     * @param declaration
     *            The declaration subtree.
     * @throws ParseException
     */
    private void parseRootDeclaration(CommonTree declaration)
            throws ParseException {

        List<CommonTree> children = declaration.getChildren();

        for (CommonTree child : children) {
            switch (child.getType()) {
            case CTFParser.TYPEDEF:
                TypedefParser.INSTANCE.parse(child, new TypedefParser.Param(fTrace, getCurrentScope()));
                break;
            case CTFParser.TYPEALIAS:
                TypeAliasParser.INSTANCE.parse(child, new TypeAliasParser.Param(fTrace, getCurrentScope()));
                break;
            case CTFParser.TYPE_SPECIFIER_LIST:
                TypeSpecifierListParser.INSTANCE.parse(child, new TypeSpecifierListParser.Param(fTrace, null, null, getCurrentScope()));
                break;
            default:
                throw childTypeError(child);
            }
        }
    }

    // ------------------------------------------------------------------------
    // Scope management
    // ------------------------------------------------------------------------

    /**
     * Removes the top declaration scope from the scope stack.
     */
    private void popScope() {
        fScope = getCurrentScope().getParentScope();
    }

    /**
     * Returns the current declaration scope.
     *
     * @return The current declaration scope.
     */
    private DeclarationScope getCurrentScope() {
        return fScope;
    }

}
