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
import static org.eclipse.tracecompass.internal.ctf.core.event.metadata.TsdlUtils.concatenateUnaryStrings;
import static org.eclipse.tracecompass.internal.ctf.core.event.metadata.TsdlUtils.isAnyUnaryString;

import java.nio.ByteOrder;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.antlr.runtime.tree.CommonTree;
import org.antlr.runtime.tree.Tree;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.jdt.annotation.NonNull;
import org.eclipse.tracecompass.ctf.core.CTFStrings;
import org.eclipse.tracecompass.ctf.core.event.CTFClock;
import org.eclipse.tracecompass.ctf.core.event.metadata.DeclarationScope;
import org.eclipse.tracecompass.ctf.core.event.types.IDeclaration;
import org.eclipse.tracecompass.ctf.core.event.types.IEventHeaderDeclaration;
import org.eclipse.tracecompass.ctf.core.event.types.StructDeclaration;
import org.eclipse.tracecompass.ctf.core.trace.CTFStream;
import org.eclipse.tracecompass.ctf.core.trace.CTFTrace;
import org.eclipse.tracecompass.ctf.parser.CTFParser;
import org.eclipse.tracecompass.internal.ctf.core.Activator;
import org.eclipse.tracecompass.internal.ctf.core.event.EventDeclaration;
import org.eclipse.tracecompass.internal.ctf.core.event.metadata.tsdl.ByteOrderParser;
import org.eclipse.tracecompass.internal.ctf.core.event.metadata.tsdl.TypeAliasParser;
import org.eclipse.tracecompass.internal.ctf.core.event.metadata.tsdl.TypeSpecifierListParser;
import org.eclipse.tracecompass.internal.ctf.core.event.metadata.tsdl.TypedefParser;
import org.eclipse.tracecompass.internal.ctf.core.event.metadata.tsdl.event.EventDeclarationParser;
import org.eclipse.tracecompass.internal.ctf.core.event.metadata.tsdl.stream.StreamIdParser;
import org.eclipse.tracecompass.internal.ctf.core.event.metadata.tsdl.trace.TraceDeclarationParser;

import com.google.common.collect.ImmutableMap;
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
                parseStream(child);
                hasStreams = true;
                break;
            case CTFParser.EVENT:
                events.add(child);
                break;
            case CTFParser.CLOCK:
                CTFClock ctfClock = ClockParser.INSTANCE.parse(child, null, null);
                String nameValue = ctfClock.getName();
                fTrace.addClock(nameValue, ctfClock);
                break;
            case CTFParser.ENV:
                fTrace.setEnvironment(parseEnvironment(child));
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
            parseEvent(event);
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
                parseStream(child);
                break;
            case CTFParser.EVENT:
                events.add(child);
                break;
            case CTFParser.CLOCK:
                CTFClock ctfClock = ClockParser.INSTANCE.parse(child, null, null);
                String nameValue = ctfClock.getName();
                fTrace.addClock(nameValue, ctfClock);
                break;
            case CTFParser.ENV:
                fTrace.setEnvironment(parseEnvironment(child));
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

    private static Map<String, String> parseEnvironment(CommonTree environment) {
        ImmutableMap.Builder<String, String> builder = new ImmutableMap.Builder<>();
        List<CommonTree> children = environment.getChildren();
        for (CommonTree child : children) {
            String left;
            String right;
            left = child.getChild(0).getChild(0).getChild(0).getText();
            right = child.getChild(1).getChild(0).getChild(0).getText();
            builder.put(left, right);
        }
        return builder.build();
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
                TypeAliasParser.INSTANCE.parse(child, new TypeAliasParser.Param(trace, getCurrentScope()), null);
                break;
            case CTFParser.TYPEDEF:
                TypedefParser.INSTANCE.parse(child, new TypedefParser.Param(trace, getCurrentScope()), null);
                break;
            case CTFParser.CTF_EXPRESSION_TYPE:
            case CTFParser.CTF_EXPRESSION_VAL:
                parseTraceDeclaration(child);
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

    private void parseTraceDeclaration(CommonTree traceDecl)
            throws ParseException {
        TraceDeclarationParser.INSTANCE.parse(traceDecl, new TraceDeclarationParser.Param(fTrace, getCurrentScope()), null);
    }

    private void parseStream(CommonTree streamNode) throws ParseException {

        CTFStream stream = new CTFStream(fTrace);

        List<CommonTree> children = streamNode.getChildren();
        if (children == null) {
            throw new ParseException("Empty stream block"); //$NON-NLS-1$
        }

        pushScope(MetadataStrings.STREAM);

        for (CommonTree child : children) {
            switch (child.getType()) {
            case CTFParser.TYPEALIAS:
                TypeAliasParser.INSTANCE.parse(child, new TypeAliasParser.Param(fTrace, getCurrentScope()), null);
                break;
            case CTFParser.TYPEDEF:
                TypedefParser.INSTANCE.parse(child, new TypedefParser.Param(fTrace, getCurrentScope()), null);
                break;
            case CTFParser.CTF_EXPRESSION_TYPE:
            case CTFParser.CTF_EXPRESSION_VAL:
                parseStreamDeclaration(child, stream);
                break;
            default:
                throw childTypeError(child);
            }
        }

        if (stream.isIdSet() &&
                (!fTrace.packetHeaderIsSet() || !fTrace.getPacketHeader().hasField(MetadataStrings.STREAM_ID))) {
            throw new ParseException("Stream has an ID, but there is no stream_id field in packet header."); //$NON-NLS-1$
        }

        fTrace.addStream(stream);

        popScope();
    }

    private void parseStreamDeclaration(CommonTree streamDecl, CTFStream stream)
            throws ParseException {

        /* There should be a left and right */

        CommonTree leftNode = (CommonTree) streamDecl.getChild(0);
        CommonTree rightNode = (CommonTree) streamDecl.getChild(1);

        List<CommonTree> leftStrings = leftNode.getChildren();

        if (!isAnyUnaryString(leftStrings.get(0))) {
            throw new ParseException("Left side of CTF assignment must be a string"); //$NON-NLS-1$
        }

        String left = concatenateUnaryStrings(leftStrings);

        if (left.equals(MetadataStrings.ID)) {
            if (stream.isIdSet()) {
                throw new ParseException("stream id already defined"); //$NON-NLS-1$
            }

            long streamID = (long) StreamIdParser.INSTANCE.parse(rightNode, null, null);

            stream.setId(streamID);
        } else if (left.equals(MetadataStrings.BYTE_ORDER)) {
            if (stream.isStreamByteOrderSet()) {
                throw new ParseException("stream byte order already defined"); //$NON-NLS-1$
            }

            ByteOrder byteOrder = ByteOrderParser.INSTANCE.parse(rightNode, new ByteOrderParser.Param(fTrace), null);

            stream.setByteOrder(byteOrder);
        } else if (left.equals(MetadataStrings.EVENT_HEADER)) {
            if (stream.isEventHeaderSet()) {
                throw new ParseException("event.header already defined"); //$NON-NLS-1$
            }

            CommonTree typeSpecifier = (CommonTree) rightNode.getChild(0);

            if (typeSpecifier.getType() != CTFParser.TYPE_SPECIFIER_LIST) {
                throw new ParseException("event.header expects a type specifier"); //$NON-NLS-1$
            }

            IDeclaration eventHeaderDecl = TypeSpecifierListParser.INSTANCE.parse(typeSpecifier, new TypeSpecifierListParser.Param(fTrace, null, null, getCurrentScope()), null);
            DeclarationScope scope = getCurrentScope();
            DeclarationScope eventHeaderScope = lookupStructName(typeSpecifier, scope);
            if (eventHeaderScope == null) {
                throw new ParseException("event.header scope not found"); //$NON-NLS-1$
            }
            pushScope(MetadataStrings.EVENT);
            getCurrentScope().addChild(eventHeaderScope);
            eventHeaderScope.setName(CTFStrings.HEADER);
            popScope();
            if (eventHeaderDecl instanceof StructDeclaration) {
                stream.setEventHeader((StructDeclaration) eventHeaderDecl);
            } else if (eventHeaderDecl instanceof IEventHeaderDeclaration) {
                stream.setEventHeader((IEventHeaderDeclaration) eventHeaderDecl);
            } else {
                throw new ParseException("event.header expects a struct"); //$NON-NLS-1$
            }

        } else if (left.equals(MetadataStrings.EVENT_CONTEXT)) {
            if (stream.isEventContextSet()) {
                throw new ParseException("event.context already defined"); //$NON-NLS-1$
            }

            CommonTree typeSpecifier = (CommonTree) rightNode.getChild(0);

            if (typeSpecifier.getType() != CTFParser.TYPE_SPECIFIER_LIST) {
                throw new ParseException("event.context expects a type specifier"); //$NON-NLS-1$
            }

            IDeclaration eventContextDecl = TypeSpecifierListParser.INSTANCE.parse(typeSpecifier, new TypeSpecifierListParser.Param(fTrace, null, null, getCurrentScope()), null);

            if (!(eventContextDecl instanceof StructDeclaration)) {
                throw new ParseException("event.context expects a struct"); //$NON-NLS-1$
            }

            stream.setEventContext((StructDeclaration) eventContextDecl);
        } else if (left.equals(MetadataStrings.PACKET_CONTEXT)) {
            if (stream.isPacketContextSet()) {
                throw new ParseException("packet.context already defined"); //$NON-NLS-1$
            }

            CommonTree typeSpecifier = (CommonTree) rightNode.getChild(0);

            if (typeSpecifier.getType() != CTFParser.TYPE_SPECIFIER_LIST) {
                throw new ParseException("packet.context expects a type specifier"); //$NON-NLS-1$
            }

            IDeclaration packetContextDecl = TypeSpecifierListParser.INSTANCE.parse(typeSpecifier, new TypeSpecifierListParser.Param(fTrace, null, null, getCurrentScope()), null);

            if (!(packetContextDecl instanceof StructDeclaration)) {
                throw new ParseException("packet.context expects a struct"); //$NON-NLS-1$
            }

            stream.setPacketContext((StructDeclaration) packetContextDecl);
        } else {
            Activator.log(IStatus.WARNING, Messages.IOStructGen_UnknownStreamAttributeWarning + " " + left); //$NON-NLS-1$
        }
    }

    private static DeclarationScope lookupStructName(CommonTree typeSpecifier, DeclarationScope scope) {
        /*
         * This needs a struct.struct_name.name to work, luckily, that is 99.99%
         * of traces we receive.
         */
        final Tree potentialStruct = typeSpecifier.getChild(0);
        DeclarationScope eventHeaderScope = null;
        if (potentialStruct.getType() == (CTFParser.STRUCT)) {
            final Tree potentialStructName = potentialStruct.getChild(0);
            if (potentialStructName.getType() == (CTFParser.STRUCT_NAME)) {
                final String name = potentialStructName.getChild(0).getText();
                eventHeaderScope = scope.lookupChildRecursive(name);
            }
        }
        /*
         * If that fails, maybe the struct is anonymous
         */
        if (eventHeaderScope == null) {
            eventHeaderScope = scope.lookupChildRecursive(MetadataStrings.STRUCT);
        }
        /*
         * This can still be null
         */
        return eventHeaderScope;
    }

    private void parseEvent(CommonTree eventNode) throws ParseException {

        List<CommonTree> children = eventNode.getChildren();
        if (children == null) {
            throw new ParseException("Empty event block"); //$NON-NLS-1$
        }

        EventDeclaration event = new EventDeclaration();

        pushScope(MetadataStrings.EVENT);

        for (CommonTree child : children) {
            switch (child.getType()) {
            case CTFParser.TYPEALIAS:
                TypeAliasParser.INSTANCE.parse(child, new TypeAliasParser.Param(fTrace, getCurrentScope()), null);
                break;
            case CTFParser.TYPEDEF:
                TypedefParser.INSTANCE.parse(child, new TypedefParser.Param(fTrace, getCurrentScope()), null);
                break;
            case CTFParser.CTF_EXPRESSION_TYPE:
            case CTFParser.CTF_EXPRESSION_VAL:
                EventDeclarationParser.INSTANCE.parse(child, new EventDeclarationParser.Param(fTrace, event, getCurrentScope()), null);
                break;
            default:
                throw childTypeError(child);
            }
        }

        if (!event.nameIsSet()) {
            throw new ParseException("Event name not set"); //$NON-NLS-1$
        }

        /*
         * If the event did not specify a stream, then the trace must be single
         * stream
         */
        if (!event.streamIsSet()) {
            if (fTrace.nbStreams() > 1) {
                throw new ParseException("Event without stream_id with more than one stream"); //$NON-NLS-1$
            }

            /*
             * If the event did not specify a stream, the only existing stream
             * must not have an id. Note: That behavior could be changed, it
             * could be possible to just get the only existing stream, whatever
             * is its id.
             */
            CTFStream stream = fTrace.getStream(null);

            if (stream != null) {
                event.setStream(stream);
            } else {
                throw new ParseException("Event without stream_id, but there is no stream without id"); //$NON-NLS-1$
            }
        }

        /*
         * Add the event to the stream.
         */
        event.getStream().addEvent(event);

        popScope();
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
                TypedefParser.INSTANCE.parse(child, new TypedefParser.Param(fTrace, getCurrentScope()), null);
                break;
            case CTFParser.TYPEALIAS:
                TypeAliasParser.INSTANCE.parse(child, new TypeAliasParser.Param(fTrace, getCurrentScope()), null);
                break;
            case CTFParser.TYPE_SPECIFIER_LIST:
                TypeSpecifierListParser.INSTANCE.parse(child, new TypeSpecifierListParser.Param(fTrace, null, null, getCurrentScope()), null);
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
     * Adds a new declaration scope on the top of the scope stack.
     */
    private void pushScope(String name) {
        fScope = new DeclarationScope(getCurrentScope(), name);
    }

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
