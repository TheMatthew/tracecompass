/*******************************************************************************
 * Copyright (c) 2015 Ericsson
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/

package org.eclipse.tracecompass.internal.ctf.core.event.metadata;

import static org.eclipse.tracecompass.internal.ctf.core.event.metadata.TsdlUtils.concatenateUnaryStrings;
import static org.eclipse.tracecompass.internal.ctf.core.event.metadata.TsdlUtils.isAnyUnaryString;

import java.nio.ByteOrder;
import java.util.List;
import java.util.Map.Entry;
import java.util.UUID;

import org.antlr.runtime.tree.CommonTree;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.tracecompass.ctf.core.event.metadata.DeclarationScope;
import org.eclipse.tracecompass.ctf.core.event.types.EnumDeclaration;
import org.eclipse.tracecompass.ctf.core.event.types.EnumDeclaration.Pair;
import org.eclipse.tracecompass.ctf.core.event.types.FloatDeclaration;
import org.eclipse.tracecompass.ctf.core.event.types.IDeclaration;
import org.eclipse.tracecompass.ctf.core.event.types.IntegerDeclaration;
import org.eclipse.tracecompass.ctf.core.event.types.StructDeclaration;
import org.eclipse.tracecompass.ctf.core.event.types.VariantDeclaration;
import org.eclipse.tracecompass.ctf.core.trace.CTFTrace;
import org.eclipse.tracecompass.ctf.parser.CTFParser;
import org.eclipse.tracecompass.internal.ctf.core.Activator;
import org.eclipse.tracecompass.internal.ctf.core.event.metadata.tsdl.integer.ByteOrderParser;
import org.eclipse.tracecompass.internal.ctf.core.event.metadata.tsdl.integer.IntegerDeclarationParser.Param;

public class TraceDeclarationParser extends AbstractScopedCommonTreeParser {

    public static final class Param implements ICommonTreeParserParameter{

        private final DeclarationScope fCurrentScope;
        private final CTFTrace fTrace;

        /**
         * @param trace
         * @param currentScope
         */
        public Param(CTFTrace trace, DeclarationScope currentScope) {
            fTrace = trace;
            fCurrentScope = currentScope;
        }

    }

    public static final TraceDeclarationParser INSTANCE = new TraceDeclarationParser();

    private TraceDeclarationParser() {
    }

    @Override
    public Object parse(CommonTree traceDecl, ICommonTreeParserParameter param, String errorMsg) throws ParseException {
        if (!(param instanceof Param)) {
            throw new IllegalArgumentException("param must be a TraceParser.Param"); //$NON-NLS-1$
        }
        CTFTrace trace = ((Param) param).fTrace;
        setScope(((Param) param).fCurrentScope);

        /* There should be a left and right */
        CommonTree leftNode = (CommonTree) traceDecl.getChild(0);
        CommonTree rightNode = (CommonTree) traceDecl.getChild(1);

        List<CommonTree> leftStrings = leftNode.getChildren();

        if (!isAnyUnaryString(leftStrings.get(0))) {
            throw new ParseException("Left side of CTF assignment must be a string"); //$NON-NLS-1$
        }

        String left = concatenateUnaryStrings(leftStrings);

        if (left.equals(MetadataStrings.MAJOR)) {
            if (trace.majorIsSet()) {
                throw new ParseException("major is already set"); //$NON-NLS-1$
            }

            trace.setMajor(VersionNumberParser.INSTANCE.parse(rightNode, null, null));
        } else if (left.equals(MetadataStrings.MINOR)) {
            if (trace.minorIsSet()) {
                throw new ParseException("minor is already set"); //$NON-NLS-1$
            }

            trace.setMinor(VersionNumberParser.INSTANCE.parse(rightNode, null, null));
        } else if (left.equals(MetadataStrings.UUID_STRING)) {
            UUID uuid = (UUID) UUIDParser.INSTANCE.parse(rightNode, null, null);

            /*
             * If uuid was already set by a metadata packet, compare it to see
             * if it matches
             */
            if (trace.uuidIsSet()) {
                if (trace.getUUID().compareTo(uuid) != 0) {
                    throw new ParseException("UUID mismatch. Packet says " //$NON-NLS-1$
                            + trace.getUUID() + " but metadata says " + uuid); //$NON-NLS-1$
                }
            } else {
                trace.setUUID(uuid);
            }

        } else if (left.equals(MetadataStrings.BYTE_ORDER)) {
            ByteOrder byteOrder = ByteOrderParser.INSTANCE.parse(rightNode, new ByteOrderParser.Param(trace), null);

            /*
             * If byte order was already set by a metadata packet, compare it to
             * see if it matches
             */
            if (trace.getByteOrder() != null) {
                if (trace.getByteOrder() != byteOrder) {
                    throw new ParseException(
                            "Endianness mismatch. Magic number says " //$NON-NLS-1$
                                    + trace.getByteOrder()
                                    + " but metadata says " + byteOrder); //$NON-NLS-1$
                }
            } else {
                trace.setByteOrder(byteOrder);

                final DeclarationScope currentScope = getCurrentScope();
                for (String type : currentScope.getTypeNames()) {
                    IDeclaration d = currentScope.lookupType(type);
                    if (d instanceof IntegerDeclaration) {
                        addByteOrder(byteOrder, currentScope, type, (IntegerDeclaration) d);
                    } else if (d instanceof FloatDeclaration) {
                        addByteOrder(byteOrder, currentScope, type, (FloatDeclaration) d);
                    } else if (d instanceof EnumDeclaration) {
                        addByteOrder(byteOrder, currentScope, type, (EnumDeclaration) d);
                    } else if (d instanceof StructDeclaration) {
                        setAlign(currentScope, (StructDeclaration) d, byteOrder);
                    }
                }
            }
        } else if (left.equals(MetadataStrings.PACKET_HEADER)) {
            if (trace.packetHeaderIsSet()) {
                throw new ParseException("packet.header already defined"); //$NON-NLS-1$
            }

            CommonTree typeSpecifier = (CommonTree) rightNode.getChild(0);

            if (typeSpecifier.getType() != CTFParser.TYPE_SPECIFIER_LIST) {
                throw new ParseException("packet.header expects a type specifier"); //$NON-NLS-1$
            }

            IDeclaration packetHeaderDecl = parseTypeSpecifierList(typeSpecifier);

            if (!(packetHeaderDecl instanceof StructDeclaration)) {
                throw new ParseException("packet.header expects a struct"); //$NON-NLS-1$
            }

            trace.setPacketHeader((StructDeclaration) packetHeaderDecl);
        } else {
            Activator.log(IStatus.WARNING, Messages.IOStructGen_UnknownTraceAttributeWarning + " " + left); //$NON-NLS-1$
        }
    }

    private static void addByteOrder(ByteOrder byteOrder,
            final DeclarationScope parentScope, String name,
            IntegerDeclaration decl) throws ParseException {

        if (decl.getByteOrder() != byteOrder) {
            IntegerDeclaration newI;
            newI = IntegerDeclaration.createDeclaration(decl.getLength(), decl.isSigned(),
                    decl.getBase(), byteOrder, decl.getEncoding(),
                    decl.getClock(), decl.getAlignment());
            parentScope.replaceType(name, newI);
        }
    }

    private static void addByteOrder(ByteOrder byteOrder, DeclarationScope parentScope, String name, EnumDeclaration decl) throws ParseException {
        final IntegerDeclaration containerType = decl.getContainerType();
        if (containerType.getByteOrder() != byteOrder) {
            EnumDeclaration newEnum = new EnumDeclaration(IntegerDeclaration.createDeclaration(containerType.getLength(), containerType.isSigned(),
                    containerType.getBase(), byteOrder, containerType.getEncoding(),
                    containerType.getClock(), containerType.getAlignment()));
            for (Entry<String, Pair> entry : decl.getEnumTable().entrySet()) {
                newEnum.add(entry.getValue().getFirst(), entry.getValue().getSecond(), entry.getKey());
            }

            parentScope.replaceType(name, newEnum);
        }
    }

    private static void addByteOrder(ByteOrder byteOrder, DeclarationScope parentScope, String name, FloatDeclaration decl) throws ParseException {
        if (decl.getByteOrder() != byteOrder) {
            FloatDeclaration newFloat = new FloatDeclaration(decl.getExponent(), decl.getMantissa(), byteOrder, decl.getAlignment());
            parentScope.replaceType(name, newFloat);
        }
    }

    private void setAlign(DeclarationScope parentScope, StructDeclaration sd,
            ByteOrder byteOrder) throws ParseException {

        for (String s : sd.getFieldsList()) {
            IDeclaration d = sd.getField(s);

            if (d instanceof StructDeclaration) {
                setAlign(parentScope, (StructDeclaration) d, byteOrder);

            } else if (d instanceof VariantDeclaration) {
                setAlign(parentScope, (VariantDeclaration) d, byteOrder);
            } else if (d instanceof IntegerDeclaration) {
                IntegerDeclaration decl = (IntegerDeclaration) d;
                if (decl.getByteOrder() != byteOrder) {
                    IntegerDeclaration newI;
                    newI = IntegerDeclaration.createDeclaration(decl.getLength(),
                            decl.isSigned(), decl.getBase(), byteOrder,
                            decl.getEncoding(), decl.getClock(),
                            decl.getAlignment());
                    sd.getFields().put(s, newI);
                }
            }
        }
    }

    private void setAlign(DeclarationScope parentScope, VariantDeclaration vd,
            ByteOrder byteOrder) throws ParseException {

        for (String s : vd.getFields().keySet()) {
            IDeclaration d = vd.getFields().get(s);

            if (d instanceof StructDeclaration) {
                setAlign(parentScope, (StructDeclaration) d, byteOrder);

            } else if (d instanceof IntegerDeclaration) {
                IntegerDeclaration decl = (IntegerDeclaration) d;
                IntegerDeclaration newI;
                newI = IntegerDeclaration.createDeclaration(decl.getLength(),
                        decl.isSigned(), decl.getBase(), byteOrder,
                        decl.getEncoding(), decl.getClock(),
                        decl.getAlignment());
                vd.getFields().put(s, newI);
            }
        }
    }

}
