package org.eclipse.tracecompass.tmf.analysis.xml.core.model;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map.Entry;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import org.eclipse.tracecompass.common.core.NonNullUtils;
import org.eclipse.tracecompass.statesystem.core.exceptions.AttributeNotFoundException;
import org.eclipse.tracecompass.statesystem.core.exceptions.StateValueTypeException;
import org.eclipse.tracecompass.statesystem.core.statevalue.ITmfStateValue;
import org.eclipse.tracecompass.tmf.analysis.xml.core.model.readwrite.TmfXmlReadWriteScenarioStatus;
import org.eclipse.tracecompass.tmf.analysis.xml.core.module.IXmlStateSystemContainer;
import org.eclipse.tracecompass.tmf.analysis.xml.core.stateprovider.TmfXmlStrings;
import org.eclipse.tracecompass.tmf.analysis.xml.core.stateprovider.XmlFilterStateProvider;
import org.eclipse.tracecompass.tmf.core.event.ITmfEvent;
import org.eclipse.tracecompass.tmf.core.event.ITmfEventField;
import org.eclipse.tracecompass.tmf.core.event.ITmfEventType;
import org.eclipse.tracecompass.tmf.core.event.ITmfSynEvent;
import org.eclipse.tracecompass.tmf.core.event.TmfEventField;
import org.eclipse.tracecompass.tmf.core.timestamp.ITmfTimestamp;
import org.eclipse.tracecompass.tmf.core.trace.ITmfTrace;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

/**
 * @author Jean-Christian Kouame
 * @since 2.0
 *
 */
public class TmfXmlSyntheticEvent implements ITmfSynEvent, Serializable {

    /**
     *
     */
    private static final long serialVersionUID = -7695419603794578244L;

    private static final String ROOT_NAME = ":root:"; //$NON-NLS-1$
    private @Nullable ITmfEvent fLastEvent;
    private final TmfXmlEventType fEventType;
    private @Nullable ITmfEventField fRootField;
    private final @Nullable Element fContentElement;
    private final ITmfXmlModelFactory fModelFactory;
    private final IXmlStateSystemContainer fContainer;
    private @Nullable ITmfTimestamp fTimestamp;
    private @Nullable ITmfTimestamp fEndTimestamp;

    /**
     * Constructor
     *
     * @param modelFactory
     *            The factory used to create XML model elements
     * @param node
     *            The XML root of this synthetic event
     * @param parent
     *            The state system container this synthetic event belongs to
     */
    public TmfXmlSyntheticEvent(ITmfXmlModelFactory modelFactory, Element node, IXmlStateSystemContainer parent) {
        fModelFactory = modelFactory;
        fContainer = parent;
        NodeList nodesEventType = node.getElementsByTagName(TmfXmlStrings.SYN_TYPE);
        Element element = (Element) nodesEventType.item(0);
        if (element == null) {
            throw new IllegalArgumentException();
        }
        fEventType = new TmfXmlEventType(element);

        NodeList nodesEventContent = node.getElementsByTagName(TmfXmlStrings.SYN_CONTENT);
        fContentElement = (Element) nodesEventContent.item(0);
    }

    /**
     * Constructor
     *
     * @param synEvent
     *            The synthetic event to copy
     */
    public TmfXmlSyntheticEvent(TmfXmlSyntheticEvent synEvent) {
        fModelFactory = synEvent.fModelFactory;
        fContainer = synEvent.fContainer;
        fContentElement = synEvent.fContentElement;
        fEventType = new TmfXmlEventType(synEvent.fEventType);
    }

    /**
     * Compute all the field and their value for this synthetic event
     *
     * @param event
     *            The current event
     * @param args
     *            The arguments necessary to fill the data
     */
    public void computeSynFields(ITmfEvent event, String... args) {
        fEventType.setEventName(event, args);
        List<ITmfEventField> fieldList = new ArrayList<>();
        if (fContentElement != null) {
            NodeList nodesEventField = fContentElement.getElementsByTagName(TmfXmlStrings.SYN_FIELD);
            for (int i = 0; i < nodesEventField.getLength(); i++) {
                Element fieldElement = (Element) nodesEventField.item(i);
                String fieldName = fieldElement.getAttribute(TmfXmlStrings.NAME);
                String fieldType = fieldElement.getAttribute(TmfXmlStrings.TYPE);
                String fieldValue = fieldElement.getAttribute(TmfXmlStrings.VALUE);
                ITmfXmlStateValue fieldStateValue = null;
                if (fieldValue.isEmpty()) {
                    Element elementFieldStateValue = (Element) fieldElement.getElementsByTagName(TmfXmlStrings.STATE_VALUE).item(0);
                    if (elementFieldStateValue == null) {
                        throw new IllegalArgumentException();
                    }
                    fieldStateValue = fModelFactory.createStateValue(elementFieldStateValue, fContainer, new ArrayList<ITmfXmlStateAttribute>());
                }

                try {
                    if (fieldName == null) {
                        throw new IllegalArgumentException();
                    }
                    ITmfEventField field;
                    // FIXME The state value can only be a query state value and
                    // do not support event field.
                    switch (fieldType) {
                    case TmfXmlStrings.TYPE_INT:
                        int valueInt = (fieldStateValue != null && fieldValue.isEmpty()) ? fieldStateValue.getValue(event, args).unboxInt() : Integer.valueOf(fieldValue);
                        field = new TmfEventField(fieldName, valueInt, null);
                        break;
                    case TmfXmlStrings.TYPE_LONG:
                        long valueLong = (fieldStateValue != null && fieldValue.isEmpty()) ? fieldStateValue.getValue(event, args).unboxLong() : Long.valueOf(fieldValue);
                        field = new TmfEventField(fieldName, valueLong, null);
                        break;
                    case TmfXmlStrings.TYPE_STRING:
                        String valueString = (fieldStateValue != null && fieldValue.isEmpty()) ? fieldStateValue.getValue(event, args).unboxStr() : fieldValue;
                        field = new TmfEventField(fieldName, valueString, null);
                        break;
                    default:
                        throw new IllegalArgumentException("Synthetic field : Synthetic field should be a value or a state value query"); //$NON-NLS-1$
                    }
                    fieldList.add(field);

                } catch (NumberFormatException e) {
                } catch (AttributeNotFoundException e) {
                }
            }
        }
        for (Entry<String, String> entry : ((XmlFilterStateProvider) fContainer).getDefinedFields().entrySet()) {
            ITmfEventField field;
            final String fieldName = entry.getValue();
            ITmfStateValue value = TmfXmlReadWriteScenarioStatus.getInstance().getScenarioSpecialFieldValue(event, fContainer, fieldName, args);
            switch (value.getType()) {
            case STRING:
                String valueString = value.unboxStr();
                field = new TmfEventField(fieldName, valueString, null);
                break;
            case DOUBLE:
                double valueDouble = value.unboxDouble();
                field = new TmfEventField(fieldName, valueDouble, null);
                break;
            case INTEGER:
                int valueInt = value.unboxInt();
                field = new TmfEventField(fieldName, valueInt, null);
                break;
            case LONG:
                Long valueLong = value.unboxLong();
                field = new TmfEventField(fieldName, valueLong, null);
                break;
            case NULL:
            default:
                field = null;
                break;
            }
            if (field != null) {
                fieldList.add(field);
            }
        }
        if (!fieldList.isEmpty()) {
            ITmfEventField[] fieldTab = new ITmfEventField[fieldList.size()];
            fieldList.toArray(fieldTab);
            fRootField = new TmfEventField(ROOT_NAME, null, fieldTab);
        } else {
            fRootField = new TmfEventField(ROOT_NAME, null, new ITmfEventField[] {});
        }
    }

    /**
     * @param timestamp
     *            The new timestamp
     */
    public void setTimestamp(ITmfTimestamp timestamp) {
        fTimestamp = timestamp;
    }

    @Override
    public ITmfTimestamp getTimestampEnd() {
        final ITmfTimestamp fEndTimestamp2 = fEndTimestamp;
        if (fEndTimestamp2 != null) {
            return fEndTimestamp2;
        }
        throw new IllegalStateException("Null traces are only allowed on special kind of events and getTrace() should not be called on them"); //$NON-NLS-1$
    }

    /**
     * @param endTime
     *            The new endTime
     */
    public void setEndTimestamp(ITmfTimestamp endTime) {
        this.fEndTimestamp = endTime;
    }

    /**
     * @param lastEvent
     *            The new last event
     */
    public void setLastEvent(ITmfEvent lastEvent) {
        fLastEvent = lastEvent;
    }

    @Override
    public @Nullable <T> T getAdapter(@Nullable Class<T> adapter) {
        return null;
    }

    @Override
    public ITmfTrace getTrace() {
        final ITmfEvent lastEvent = fLastEvent;
        if (lastEvent != null) {
            return lastEvent.getTrace();
        }
        throw new IllegalStateException("Null traces are only allowed on special kind of events and getTrace() should not be called on them"); //$NON-NLS-1$
    }

    @Override
    public long getRank() {
        final ITmfEvent lastEvent = fLastEvent;
        if (lastEvent != null) {
            return lastEvent.getRank();
        }
        return -1;
    }

    @Override
    public ITmfTimestamp getTimestamp() {
        final ITmfTimestamp timestamp = fTimestamp;
        if (timestamp != null) {
            return timestamp;
        }
        throw new IllegalStateException("Null traces are only allowed on special kind of events and getTrace() should not be called on them"); //$NON-NLS-1$
    }

    @Override
    public ITmfEventType getType() {
        return fEventType;
    }

    @Override
    public @Nullable ITmfEventField getContent() {
        final ITmfEventField rootField = fRootField;
        if (rootField != null) {
            return rootField;
        }
        // TODO handle null value
        return null;
    }

    @NonNullByDefault({})
    private class TmfXmlEventType implements ITmfEventType, Serializable {

        /**
         *
         */
        private static final long serialVersionUID = 4643762541632642257L;
        private static final String UNKNOWN_STRING = "unknown"; //$NON-NLS-1$
        private static final String SYN_PREFIX = "syn/"; //$NON-NLS-1$
        private @Nullable String fEventName;
        private Element fNode;

        public TmfXmlEventType(Element node) {
            fNode = node;
        }

        public TmfXmlEventType(TmfXmlEventType eventType) {
            fNode = eventType.fNode;
        }

        public void setEventName(ITmfEvent event, String... arg) {
            StringBuilder name = new StringBuilder(SYN_PREFIX);
            name.append(fNode.getAttribute(TmfXmlStrings.EVENT_NAME));
            ITmfXmlStateValue nameStateValue = null;
            if (name.toString().equals(SYN_PREFIX)) {
                Element elementEventNameStateValue = (Element) fNode.getElementsByTagName(TmfXmlStrings.STATE_VALUE).item(0);
                if (elementEventNameStateValue == null) {
                    throw new IllegalArgumentException();
                }
                nameStateValue = fModelFactory.createStateValue(elementEventNameStateValue, fContainer, new ArrayList<ITmfXmlStateAttribute>());
                ITmfStateValue value;
                try {
                    value = nameStateValue.getValue(event, arg);
                    switch (value.getType()) {
                    case DOUBLE:
                        name.append(String.valueOf(value.unboxDouble()));
                        break;
                    case INTEGER:
                        name.append(String.valueOf(value.unboxInt()));
                        break;
                    case LONG:
                        name.append(String.valueOf(value.unboxLong()));
                        break;
                    case NULL:
                        name.append(UNKNOWN_STRING);
                        break;
                    case STRING:
                        name.append(value.unboxStr());
                        break;
                    default:
                        throw new StateValueTypeException();
                    }
                } catch (AttributeNotFoundException e) {
                }
            }
            fEventName = name.toString();
        }

        @Override
        public @Nullable String getName() {
            return fEventName;
        }

        @Override
        public @Nullable ITmfEventField getRootField() {
            return fRootField;
        }

        @Override
        public @Nullable Collection<String> getFieldNames() {
            // TODO Auto-generated method stub
            Collection<String> toReturn = (fRootField != null) ? fRootField.getFieldNames() : Collections.EMPTY_SET;
            return toReturn;
        }
//        @Override
//        public Collection<String> getFieldNames() {
//            return (fRootField != null) ? fRootField.getFieldNames() : Collections.EMPTY_SET;
//        }
    }

    /**
     * @return The duration of this synthetic event
     */
    public long getDuration() {
        return getTimestampEnd().getValue() - getTimestamp().getValue();
    }

    @Override
    public String getName() {
        return NonNullUtils.nullToEmptyString(fEventType.getName());
    }
}
