package org.eclipse.tracecompass.internal.ctf.core.event.metadata;

import static org.eclipse.tracecompass.internal.ctf.core.event.metadata.TsdlUtils.childTypeError;
import static org.eclipse.tracecompass.internal.ctf.core.event.metadata.TsdlUtils.concatenateUnaryStrings;
import static org.eclipse.tracecompass.internal.ctf.core.event.metadata.TsdlUtils.isAnyUnaryString;

import java.nio.ByteOrder;
import java.util.List;

import org.antlr.runtime.tree.CommonTree;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.jdt.annotation.NonNull;
import org.eclipse.tracecompass.ctf.core.event.types.Encoding;
import org.eclipse.tracecompass.ctf.core.event.types.IntegerDeclaration;
import org.eclipse.tracecompass.ctf.core.trace.CTFTrace;
import org.eclipse.tracecompass.ctf.parser.CTFParser;
import org.eclipse.tracecompass.internal.ctf.core.Activator;
import org.eclipse.tracecompass.internal.ctf.core.event.metadata.exceptions.ParseException;

/**
 * Signed integers are represented in two-complement. Integer alignment, size,
 * signedness and byte ordering are defined in the TSDL metadata. Integers
 * aligned on byte size (8-bit) and with length multiple of byte size (8-bit)
 * correspond to the C99 standard integers. In addition, integers with alignment
 * and/or size that are not a multiple of the byte size are permitted; these
 * correspond to the C99 standard bitfields, with the added specification that
 * the CTF integer bitfields have a fixed binary representation. Integer size
 * needs to be a positive integer. Integers of size 0 are forbidden. An
 * MIT-licensed reference implementation of the CTF portable bitfields is
 * available here.
 *
 * Binary representation of integers:
 * <ul>
 * <li>On little and big endian: Within a byte, high bits correspond to an
 * integer high bits, and low bits correspond to low bits</li>
 * <li>On little endian: Integer across multiple bytes are placed from the less
 * significant to the most significant Consecutive integers are placed from
 * lower bits to higher bits (even within a byte)</li>
 * <li>On big endian: Integer across multiple bytes are placed from the most
 * significant to the less significant Consecutive integers are placed from
 * higher bits to lower bits (even within a byte)</li>
 * </ul>
 *
 * This binary representation is derived from the bitfield implementation in GCC
 * for little and big endian. However, contrary to what GCC does, integers can
 * cross units boundaries (no padding is required). Padding can be explicitly
 * added to follow the GCC layout if needed.
 *
 * @author Matthew Khouzam
 * @author Efficios - javadoc preamble
 *
 */
public class IntegerDeclarationParser implements ICommonTreeParser {

    private static final @NonNull String ENCODING = "encoding"; //$NON-NLS-1$
    private static final @NonNull String EMPTY_STRING = ""; //$NON-NLS-1$
    private static final int DEFAULT_INT_BASE = 10;
    private static final @NonNull String MAP = "map"; //$NON-NLS-1$
    private static final @NonNull String BASE = "base"; //$NON-NLS-1$
    private static final @NonNull String SIZE = "size"; //$NON-NLS-1$
    private static final @NonNull String SIGNED = "signed"; //$NON-NLS-1$

    private static final ICommonTreeParser SIGNED_PARSER = new SignedParser();
    private static final ICommonTreeParser BASE_PARSER = new BaseParser();
    private static final ICommonTreeParser CLOCK_PARSER = new ClockParser();
    private static final ICommonTreeParser SIZE_PARSER = new SizeParser();
    private static final ICommonTreeParser BYTE_ORDER_PARSER = new ByteOrderParser();
    private static final ICommonTreeParser ALIGNMENT_PARSER = new AlignmentParser();
    private static final ICommonTreeParser ENCODING_PARSER = new EncodingParser();

    /**
     * Parses an integer declaration node.
     *
     * @param param
     *            parent trace, for byte orders
     * @return The corresponding integer declaration.
     */
    @Override
    public IntegerDeclaration parse(CommonTree integer, Object param, String errorMsg) throws ParseException {
        if (!(param instanceof CTFTrace)) {
            throw new IllegalArgumentException("Param must be a CTFTrace"); //$NON-NLS-1$
        }
        CTFTrace trace = (CTFTrace) param;
        List<CommonTree> children = integer.getChildren();

        /*
         * If the integer has no attributes, then it is missing the size
         * attribute which is required
         */
        if (children == null) {
            throw new ParseException("integer: missing size attribute"); //$NON-NLS-1$
        }

        /* The return value */
        IntegerDeclaration integerDeclaration = null;
        boolean signed = false;
        ByteOrder byteOrder = trace.getByteOrder();
        long size = 0;
        long alignment = 0;
        int base = DEFAULT_INT_BASE;
        @NonNull
        String clock = EMPTY_STRING;

        Encoding encoding = Encoding.NONE;

        /* Iterate on all integer children */
        for (CommonTree child : children) {
            switch (child.getType()) {
            case CTFParser.CTF_EXPRESSION_VAL:
                /*
                 * An assignment expression must have 2 children, left and right
                 */

                CommonTree leftNode = (CommonTree) child.getChild(0);
                CommonTree rightNode = (CommonTree) child.getChild(1);

                List<CommonTree> leftStrings = leftNode.getChildren();

                if (!isAnyUnaryString(leftStrings.get(0))) {
                    throw new ParseException("Left side of ctf expression must be a string"); //$NON-NLS-1$
                }
                String left = concatenateUnaryStrings(leftStrings);

                switch (left) {
                case SIGNED:
                    signed = (Boolean) SIGNED_PARSER.parse(rightNode, null, null);
                    break;
                case MetadataStrings.BYTE_ORDER:
                    byteOrder = (ByteOrder) BYTE_ORDER_PARSER.parse(rightNode, trace, null);
                    break;
                case SIZE:
                    size = (Long) SIZE_PARSER.parse(rightNode, null, null);
                    break;
                case MetadataStrings.ALIGN:
                    alignment = (Long) ALIGNMENT_PARSER.parse(rightNode, null, null);
                    break;
                case BASE:
                    base = (int) BASE_PARSER.parse(rightNode, null, null);
                    break;
                case ENCODING:
                    encoding = (Encoding) ENCODING_PARSER.parse(rightNode, null, null);
                    break;
                case MAP:
                    clock = (String) CLOCK_PARSER.parse(rightNode, null, null);
                    break;
                default:
                    Activator.log(IStatus.WARNING, Messages.IOStructGen_UnknownIntegerAttributeWarning + " " + left); //$NON-NLS-1$
                    break;
                }

                break;
            default:
                throw childTypeError(child);
            }
        }

        if (size <= 0) {
            throw new ParseException("Invalid size attribute in Integer: " + size); //$NON-NLS-1$
        }

        if (alignment == 0) {
            alignment = 1;
        }

        integerDeclaration = IntegerDeclaration.createDeclaration((int) size, signed, base,
                byteOrder, encoding, clock, alignment);

        return integerDeclaration;
    }
}
