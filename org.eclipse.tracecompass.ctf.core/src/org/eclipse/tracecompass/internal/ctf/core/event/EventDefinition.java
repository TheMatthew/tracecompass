/*******************************************************************************
 * Copyright (c) 2011-2014 Ericsson, Ecole Polytechnique de Montreal and others
 *
 * All rights reserved. This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License v1.0 which
 * accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors: Matthew Khouzam - Initial API and implementation
 * Contributors: Simon Marchi - Initial API and implementation
 *******************************************************************************/

package org.eclipse.tracecompass.internal.ctf.core.event;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.tracecompass.ctf.core.event.IEventDeclaration;
import org.eclipse.tracecompass.ctf.core.event.IEventDefinition;
import org.eclipse.tracecompass.ctf.core.event.scope.IDefinitionScope;
import org.eclipse.tracecompass.ctf.core.event.scope.ILexicalScope;
import org.eclipse.tracecompass.ctf.core.event.scope.LexicalScope;
import org.eclipse.tracecompass.ctf.core.event.types.Definition;
import org.eclipse.tracecompass.ctf.core.event.types.ICompositeDefinition;
import org.eclipse.tracecompass.ctf.core.event.types.IDefinition;
import org.eclipse.tracecompass.ctf.core.event.types.StructDeclaration;
import org.eclipse.tracecompass.ctf.core.event.types.StructDefinition;
import org.eclipse.tracecompass.ctf.core.trace.ICTFPacketDescriptor;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableList.Builder;

/**
 * Representation of a particular instance of an event.
 */
public final class EventDefinition implements IDefinitionScope, IEventDefinition {

    // ------------------------------------------------------------------------
    // Attributes
    // ------------------------------------------------------------------------

    /**
     * The corresponding event declaration.
     */
    private final IEventDeclaration fDeclaration;

    /**
     * The timestamp of the current event.
     */
    private final long fTimestamp;

    /**
     * The event context structure definition.
     */
    private final ICompositeDefinition fEventContext;

    private final ICompositeDefinition fStreamContext;

    private final ICTFPacketDescriptor fPacketContext;

    /**
     * The event fields structure definition.
     */
    private final ICompositeDefinition fFields;

    // ------------------------------------------------------------------------
    // Constructors
    // ------------------------------------------------------------------------

    /**
     * Constructs an event definition.
     *
     * @param declaration
     *            The corresponding event declaration
     * @param timestamp
     *            event timestamp
     * @param eventContext
     *            The event context
     * @param packetContext
     *            the packet context
     * @param streamContext
     *            the stream context
     * @param fields
     *            The event fields
     * @since 1.0
     */
    public EventDefinition(IEventDeclaration declaration,
            long timestamp,
            ICompositeDefinition streamContext,
            ICompositeDefinition eventContext,
            ICTFPacketDescriptor packetContext,
            ICompositeDefinition fields) {
        fDeclaration = declaration;
        fTimestamp = timestamp;
        fFields = fields;
        fEventContext = eventContext;
        fPacketContext = packetContext;
        fStreamContext = streamContext;
    }

    // ------------------------------------------------------------------------
    // Getters/Setters/Predicates
    // ------------------------------------------------------------------------

    /**
     * @since 1.0
     */
    @Override
    public ILexicalScope getScopePath() {
        String eventName = fDeclaration.getName();
        if (eventName == null) {
            return null;
        }
        ILexicalScope myScope = ILexicalScope.EVENT.getChild(eventName);
        if (myScope == null) {
            myScope = new LexicalScope(ILexicalScope.EVENT, eventName);
        }
        return myScope;
    }

    /* (non-Javadoc)
     * @see org.eclipse.tracecompass.ctf.core.event.IEventDefinition#getDeclaration()
     */
    @Override
    public IEventDeclaration getDeclaration() {
        return fDeclaration;
    }

    /* (non-Javadoc)
     * @see org.eclipse.tracecompass.ctf.core.event.IEventDefinition#getFields()
     */
    @Override
    public ICompositeDefinition getFields() {
        return fFields;
    }

    /* (non-Javadoc)
     * @see org.eclipse.tracecompass.ctf.core.event.IEventDefinition#getEventContext()
     */
    @Override
    public ICompositeDefinition getEventContext() {
        return fEventContext;
    }

    /* (non-Javadoc)
     * @see org.eclipse.tracecompass.ctf.core.event.IEventDefinition#getContext()
     */
    @Override
    public ICompositeDefinition getContext() {

        /* Most common case so far */
        if (fStreamContext == null) {
            return fEventContext;
        }

        /* streamContext is not null, but the context of the event is null */
        if (fEventContext == null) {
            return fStreamContext;
        }

        // TODO: cache if this is a performance issue

        /* The stream context and event context are assigned. */
        StructDeclaration mergedDeclaration = new StructDeclaration(1);

        Builder<String> builder = ImmutableList.<String> builder();
        List<Definition> fieldValues = new ArrayList<>();

        /* Add fields from the stream */
        for (String fieldName : fStreamContext.getFieldNames()) {
            Definition definition = fStreamContext.getDefinition(fieldName);
            mergedDeclaration.addField(fieldName, definition.getDeclaration());
            builder.add(fieldName);
            fieldValues.add(definition);
        }

        ImmutableList<String> fieldNames = builder.build();
        /*
         * Add fields from the event context, overwrite the stream ones if
         * needed.
         */
        for (String fieldName : fEventContext.getFieldNames()) {
            Definition definition = fEventContext.getDefinition(fieldName);
            mergedDeclaration.addField(fieldName, definition.getDeclaration());
            if (fieldNames.contains(fieldName)) {
                fieldValues.set((fieldNames.indexOf(fieldName)), definition);
            } else {
                builder.add(fieldName);
                fieldValues.add(definition);
            }
        }
        fieldNames = builder.build();
        StructDefinition mergedContext = new StructDefinition(mergedDeclaration, this, "context", //$NON-NLS-1$
                fieldNames,
                fieldValues.toArray(new Definition[fieldValues.size()]));
        return mergedContext;
    }

    @Override
    public int getCPU() {
        if (fPacketContext != null) {
            return (int) fPacketContext.getTargetId();
        }
        return -1;
    }

    /* (non-Javadoc)
     * @see org.eclipse.tracecompass.ctf.core.event.IEventDefinition#getTimestamp()
     */
    @Override
    public long getTimestamp() {
        return fTimestamp;
    }

    // ------------------------------------------------------------------------
    // Operations
    // ------------------------------------------------------------------------

    /**
     * @since 1.0
     */
    @Override
    public IDefinition lookupDefinition(String lookupPath) {
        if (lookupPath.equals("context")) { //$NON-NLS-1$
            return fEventContext;
        } else if (lookupPath.equals("fields")) { //$NON-NLS-1$
            return fFields;
        } else {
            return null;
        }
    }

    @Override
    public String toString() {
        Iterable<String> list;
        StringBuilder retString = new StringBuilder();
        final String cr = System.getProperty("line.separator");//$NON-NLS-1$

        retString.append("Event type: " + fDeclaration.getName() + cr); //$NON-NLS-1$
        retString.append("Timestamp: " + Long.toString(fTimestamp) + cr); //$NON-NLS-1$

        if (fEventContext != null) {
            list = fEventContext.getFieldNames();

            for (String field : list) {
                retString.append(field
                        + " : " + fEventContext.getDefinition(field).toString() + cr); //$NON-NLS-1$
            }
        }

        if (fFields != null) {
            list = fFields.getFieldNames();

            for (String field : list) {
                retString.append(field
                        + " : " + fFields.getDefinition(field).toString() + cr); //$NON-NLS-1$
            }
        }

        return retString.toString();
    }
}
