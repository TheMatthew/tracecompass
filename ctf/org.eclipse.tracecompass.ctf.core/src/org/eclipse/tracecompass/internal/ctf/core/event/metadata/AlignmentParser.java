/*******************************************************************************
 * Copyright (c) 2015 Ericsson
 *
 * All rights reserved. This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License v1.0 which
 * accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/

package org.eclipse.tracecompass.internal.ctf.core.event.metadata;

import static org.eclipse.tracecompass.internal.ctf.core.event.metadata.TsdlUtils.isUnaryInteger;

import org.antlr.runtime.tree.CommonTree;
import org.eclipse.tracecompass.ctf.parser.CTFParser;
import org.eclipse.tracecompass.internal.ctf.core.event.metadata.exceptions.ParseException;

/**
 * Alignment parser, we define byte-packed types as aligned on the byte size,
 * namely 8-bit. We define bit-packed types as following on the next bit, as
 * defined by the Integers section.
 *
 * Each basic type must specify its alignment, in bits. Examples of possible
 * alignments are: bit-packed (align = 1), byte-packed (align = 8), or
 * word-aligned (e.g. align = 32 or align = 64). The choice depends on the
 * architecture preference and compactness vs performance trade-offs of the
 * implementation. Architectures providing fast unaligned write byte-packed
 * basic types to save space, aligning each type on byte boundaries (8-bit).
 * Architectures with slow unaligned writes align types on specific alignment
 * values. If no specific alignment is declared for a type, it is assumed to be
 * bit-packed for integers with size not multiple of 8 bits and for gcc
 * bitfields. All other basic types are byte-packed by default. It is however
 * recommended to always specify the alignment explicitly. Alignment values must
 * be power of two. Compound types are aligned as specified in their individual
 * specification.
 *
 * The base offset used for field alignment is the start of the packet
 * containing the field. For instance, a field aligned on 32-bit needs to be at
 * an offset multiple of 32-bit from the start of the packet that contains it.
 *
 * TSDL metadata attribute representation of a specific alignment:
 *
 * @author Matthew Khouzam
 * @author Efficios (javadoc preamble)
 *
 */
public final class AlignmentParser implements ICommonTreeParser {

    /**
     * Alignment parser instance
     */
    public static final AlignmentParser INSTANCE = new AlignmentParser();

    private static final String INVALID_VALUE_FOR_ALIGNMENT = "Invalid value for alignment"; //$NON-NLS-1$

    private AlignmentParser(){}
    /**
     * Gets the value of a "align" integer or struct attribute.
     *
     * @param node
     *            A CTF_RIGHT node or directly an unary integer.
     * @return The align value.
     * @throws ParseException
     *             Invalid alignment value
     */
    @Override
    public Long parse(CommonTree node, Object parser, String unused2) throws ParseException {
        if (!(parser instanceof UnaryIntegerParser)) {
            throw new IllegalArgumentException("The Object must be a UnaryIntegerParser"); //$NON-NLS-1$
        }
        UnaryIntegerParser integerParser = (UnaryIntegerParser) parser;
        /*
         * If a CTF_RIGHT node was passed, call getAlignment with the first
         * child
         */
        if (node.getType() == CTFParser.CTF_RIGHT) {
            if (node.getChildCount() > 1) {
                throw new ParseException(INVALID_VALUE_FOR_ALIGNMENT);
            }

            return parse((CommonTree) node.getChild(0), (Object) integerParser, null);
        } else if (isUnaryInteger(node)) {
            long alignment = integerParser.parse(node, null, null);

            if (!isValidAlignment(alignment)) {
                throw new ParseException(INVALID_VALUE_FOR_ALIGNMENT + " : " //$NON-NLS-1$
                        + alignment);
            }

            return alignment;
        }
        throw new ParseException(INVALID_VALUE_FOR_ALIGNMENT); // $NON-NLS-1$
    }

    /**
     * Determines if the given value is a valid alignment value.
     *
     * @param alignment
     *            The value to check.
     * @return True if it is valid.
     */
    private static boolean isValidAlignment(long alignment) {
        return !((alignment <= 0) || ((alignment & (alignment - 1)) != 0));
    }

}
