package org.eclipse.tracecompass.internal.ctf.core.event.metadata;

import static org.eclipse.tracecompass.internal.ctf.core.event.metadata.TsdlUtils.isUnaryInteger;

import org.antlr.runtime.tree.CommonTree;
import org.eclipse.tracecompass.internal.ctf.core.event.metadata.exceptions.ParseException;

/**
 * Type size, in bits, for integers and floats is that returned by sizeof() in C
 * multiplied by CHAR_BIT. We require the size of char and unsigned char types
 * (CHAR_BIT) to be fixed to 8 bits for cross-endianness compatibility.
 *
 * TSDL metadata representation:
 *
 * <pre>
 * size = /* value is in bits * /
 * </pre>
 *
 * @author Matthew Khouzam
 * @author Efficios - javadoc preamble.
 */
public class SizeParser implements ICommonTreeParser {
    private static final String INVALID_VALUE_FOR_SIZE = "Invalid value for size"; //$NON-NLS-1$

    /**
     * Gets the value of a "size" integer attribute.
     *
     * @param rightNode
     *            A CTF_RIGHT node.
     * @return The "size" value.
     * @throws ParseException
     *             if the size is not an int or a negative
     */
    @Override
    public Long parse(CommonTree rightNode, Object parser, String error) throws ParseException {
        if (!(parser instanceof UnaryIntegerParser)) {
            throw new IllegalArgumentException("parser must be of type UnaryIntegerParser"); //$NON-NLS-1$
        }
        UnaryIntegerParser integerParser = (UnaryIntegerParser) parser;

        CommonTree firstChild = (CommonTree) rightNode.getChild(0);

        if (isUnaryInteger(firstChild)) {
            if (rightNode.getChildCount() > 1) {
                throw new ParseException(INVALID_VALUE_FOR_SIZE);
            }

            long size = integerParser.parse(firstChild, null, null);

            if (size < 1) {
                throw new ParseException(INVALID_VALUE_FOR_SIZE);
            }

            return size;
        }
        throw new ParseException(INVALID_VALUE_FOR_SIZE);
    }

}
