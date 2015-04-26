/*******************************************************************************
 * Copyright (c) 2011, 2014 Ericsson, Ecole Polytechnique de Montreal and others
 *
 * All rights reserved. This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License v1.0 which
 * accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors: Matthew Khouzam - Initial API and implementation
 * Contributors: Simon Marchi    - Initial API and implementation
 *******************************************************************************/

package org.eclipse.tracecompass.internal.ctf.core.event;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import org.eclipse.jdt.annotation.NonNull;
import org.eclipse.tracecompass.common.core.NonNullUtils;
import org.eclipse.tracecompass.ctf.core.CTFException;
import org.eclipse.tracecompass.ctf.core.CTFStrings;
import org.eclipse.tracecompass.ctf.core.event.EventDefinition;
import org.eclipse.tracecompass.ctf.core.event.IEventDeclaration;
import org.eclipse.tracecompass.ctf.core.event.io.BitBuffer;
import org.eclipse.tracecompass.ctf.core.event.scope.IDefinitionScope;
import org.eclipse.tracecompass.ctf.core.event.scope.ILexicalScope;
import org.eclipse.tracecompass.ctf.core.event.types.Declaration;
import org.eclipse.tracecompass.ctf.core.event.types.ICompositeDefinition;
import org.eclipse.tracecompass.ctf.core.event.types.IntegerDeclaration;
import org.eclipse.tracecompass.ctf.core.event.types.StructDeclaration;
import org.eclipse.tracecompass.ctf.core.trace.CTFStreamInputReader;

/**
 * Representation of one type of event. A bit like "int" or "long" but for trace
 * events.
 */
public class EventDeclaration implements IEventDeclaration {

    /** Id of lost events */
    public static final long LOST_EVENT_ID = -1L;

    /** Id of events when not set */
    public static final long UNSET_EVENT_ID = -2L;

    // ------------------------------------------------------------------------
    // Attributes
    // ------------------------------------------------------------------------

    /**
     * Name of the event
     */
    private String fName;

    /**
     * Event context structure declaration
     */
    private StructDeclaration fContext = null;

    /**
     * Event fields structure declaration
     */
    private StructDeclaration fFields = null;

    /**
     * parent trace
     */
    private IDefinitionScope fTraceScope;

    /**
     * Loglevel of an event
     */
    private long fLogLevel;

    /** Map of this event type's custom CTF attributes */
    private final Map<String, String> fCustomAttributes = new HashMap<>();

    private int fId = (int) UNSET_EVENT_ID;

    // ------------------------------------------------------------------------
    // Constructors
    // ------------------------------------------------------------------------

    /**
     * Default constructor. Use the setters afterwards to set the fields
     * accordingly.
     */
    public EventDeclaration() {
    }

    @Override
    public EventDefinition createDefinition(CTFStreamInputReader streamInputReader, @NonNull BitBuffer input, long timestamp) throws CTFException {
        StructDeclaration streamEventContextDecl = streamInputReader.getStreamEventContextDecl();
        ICompositeDefinition streamEventContext = streamEventContextDecl != null ? streamEventContextDecl.createDefinition(fTraceScope, ILexicalScope.STREAM_EVENT_CONTEXT, input) : null;
        ICompositeDefinition packetContext = streamInputReader.getPacketReader().getCurrentPacketEventHeader();
        ICompositeDefinition eventContext = fContext != null ? fContext.createDefinition(fTraceScope, ILexicalScope.CONTEXT, input) : null;
        ICompositeDefinition eventPayload = fFields != null ? fFields.createDefinition(fTraceScope, ILexicalScope.FIELDS, input) : null;

        // a bit lttng specific
        // CTF doesn't require a timestamp,
        // but it's passed to us
        return new EventDefinition(
                this,
                streamInputReader,
                timestamp,
                streamEventContext,
                eventContext,
                packetContext,
                eventPayload);
    }

    /**
     * Creates a "lost" event. This is a synthetic event that is there to show
     * that there should be something there.
     *
     * @return the lost event
     */
    public static synchronized EventDeclaration getLostEventDeclaration() {
        EventDeclaration lostEvent = new EventDeclaration();
        String[] fieldNames = new String[] { CTFStrings.LOST_EVENTS_FIELD, CTFStrings.LOST_EVENTS_DURATION };
        Declaration[] fieldDeclarations = new Declaration[] { IntegerDeclaration.UINT_32B_DECL, IntegerDeclaration.UINT_64B_DECL };
        lostEvent.fFields = new StructDeclaration(fieldNames, fieldDeclarations);
        lostEvent.fId = (int) LOST_EVENT_ID;
        lostEvent.fName = CTFStrings.LOST_EVENT_NAME;
        return lostEvent;
    }

    // ------------------------------------------------------------------------
    // Getters/Setters/Predicates
    // ------------------------------------------------------------------------

    /**
     * Sets a name for an event Declaration
     *
     * @param name
     *            the name
     */
    public void setName(String name) {
        fName = name;
    }

    @Override
    public String getName() {
        return fName;
    }

    /**
     * Sets the context for an event declaration (see CTF specification)
     *
     * @param context
     *            the context in structdeclaration format
     */
    public void setContext(StructDeclaration context) {
        fContext = context;
    }

    /**
     * Sets the fields of an event declaration
     *
     * @param fields
     *            the fields in structdeclaration format
     */
    public void setFields(StructDeclaration fields) {
        fFields = fields;
    }

    @Override
    public StructDeclaration getFields() {
        return fFields;
    }

    @Override
    public StructDeclaration getContext() {
        return fContext;
    }

    /**
     * Sets the id of an event declaration
     *
     * @param id
     *            the id
     */
    public void setId(long id) {
        if (id < 0 || id > Integer.MAX_VALUE) {
            throw new IllegalArgumentException("id out of range"); //$NON-NLS-1$
        }
        fId = (int) id;
    }

    @Override
    public Long getId() {
        return Long.valueOf(fId);
    }

    /**
     * Faster get id assuming you have less than a billion event types
     *
     * @return the event id
     */
    public int id() {
        return fId;
    }

    /**
     * Sets the stream of an event declaration
     *
     * @param traceScope
     *            The trace scope, typically a CTFTrace
     */
    public void setTraceScope(IDefinitionScope traceScope) {
        fTraceScope = traceScope;
    }

    /**
     * Is the name of the event declaration set
     *
     * @return is the name set?
     */
    public boolean nameIsSet() {
        return fName != null;
    }

    /**
     * Is the context set
     *
     * @return is the context set
     */
    public boolean contextIsSet() {
        return fContext != null;
    }

    /**
     * Is a field set?
     *
     * @return Is the field set?
     */
    public boolean fieldsIsSet() {
        return fFields != null;
    }

    /**
     * Is the id set?
     *
     * @return is the id set?
     */
    public boolean idIsSet() {
        return (fId != UNSET_EVENT_ID);
    }

    /**
     * Is the stream set?
     *
     * @return is the stream set?
     */
    public boolean streamIsSet() {
        return fTraceScope != null;
    }

    @Override
    public long getLogLevel() {
        return fLogLevel;
    }

    /**
     * Sets the log level
     *
     * @param level
     *            the log level
     */
    public void setLogLevel(long level) {
        fLogLevel = level;
    }

    @Override
    public Set<String> getCustomAttributes() {
        return fCustomAttributes.keySet();
    }

    @Override
    public String getCustomAttribute(String key) {
        return fCustomAttributes.get(key);
    }

    /**
     * Sets a custom attribute value.
     *
     * @param key
     *            the key of the attribute
     * @param value
     *            the value of the attribute
     */
    public void setCustomAttribute(String key, String value) {
        fCustomAttributes.put(key, value);
    }

    // ------------------------------------------------------------------------
    // Operations
    // ------------------------------------------------------------------------

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null) {
            return false;
        }
        if (!(obj instanceof EventDeclaration)) {
            return false;
        }
        EventDeclaration other = (EventDeclaration) obj;
        if (fId != (other.fId)) {
            return false;
        }
        if(!NonNullUtils.equalsNullable(fContext, other.fContext)){
            return false;
        }
        if(!NonNullUtils.equalsNullable(fFields, other.fFields)){
            return false;
        }
        if (!NonNullUtils.equalsNullable(fName, other.fName)) {
            return false;
        }
        if (!NonNullUtils.equalsNullable(fTraceScope, other.fTraceScope)) {
            return false;
        }
        if (!fCustomAttributes.equals(other.fCustomAttributes)) {
            return false;
        }
        return true;
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = (prime * result)
                + ((fContext == null) ? 0 : fContext.hashCode());
        result = (prime * result) + ((fFields == null) ? 0 : fFields.hashCode());
        result = (prime * result) + fId;
        result = (prime * result) + ((fName == null) ? 0 : fName.hashCode());
        result = (prime * result) + ((fTraceScope == null) ? 0 : fTraceScope.hashCode());
        result = (prime * result) + fCustomAttributes.hashCode();
        return result;
    }

}
