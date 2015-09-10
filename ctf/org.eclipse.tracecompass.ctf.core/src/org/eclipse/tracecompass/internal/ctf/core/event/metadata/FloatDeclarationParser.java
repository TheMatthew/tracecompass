package org.eclipse.tracecompass.internal.ctf.core.event.metadata;

import static org.eclipse.tracecompass.internal.ctf.core.event.metadata.TsdlUtils.childTypeError;
import static org.eclipse.tracecompass.internal.ctf.core.event.metadata.TsdlUtils.concatenateUnaryStrings;
import static org.eclipse.tracecompass.internal.ctf.core.event.metadata.TsdlUtils.isAnyUnaryString;

import java.nio.ByteOrder;
import java.util.List;

import org.antlr.runtime.tree.CommonTree;
import org.eclipse.tracecompass.ctf.core.event.types.FloatDeclaration;
import org.eclipse.tracecompass.ctf.core.trace.CTFTrace;
import org.eclipse.tracecompass.ctf.parser.CTFParser;
import org.eclipse.tracecompass.internal.ctf.core.event.metadata.exceptions.ParseException;

public class FloatDeclarationParser implements ICommonTreeParser{

    private static final int DEFAULT_FLOAT_EXPONENT = 8;
    private static final int DEFAULT_FLOAT_MANTISSA = 24;
    private static final ICommonTreeParser UNARY_INTEGER_PARSER = new UnaryIntegerParser();
    private static final ICommonTreeParser BYTE_ORDER_PARSER = new ByteOrderParser();
    private static final ICommonTreeParser ALIGNMENT_PARSER = new AlignmentParser();


    @Override
    public Object parse(CommonTree floatingPoint, Object param, String errorMsg) throws ParseException {
        if(!(param instanceof CTFTrace)){
            throw new IllegalArgumentException();
        }
        CTFTrace trace = (CTFTrace) param;
        List<CommonTree> children = floatingPoint.getChildren();

        /*
         * If the integer has no attributes, then it is missing the size
         * attribute which is required
         */
        if (children == null) {
            throw new ParseException("float: missing size attribute"); //$NON-NLS-1$
        }

        /* The return value */
        FloatDeclaration floatDeclaration = null;
        ByteOrder byteOrder = trace.getByteOrder();
        long alignment = 0;

        int exponent = DEFAULT_FLOAT_EXPONENT;
        int mantissa = DEFAULT_FLOAT_MANTISSA;

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

                if (left.equals(MetadataStrings.EXP_DIG)) {
                    exponent = ((Long) UNARY_INTEGER_PARSER.parse((CommonTree) rightNode.getChild(0), null, null)).intValue();
                } else if (left.equals(MetadataStrings.BYTE_ORDER)) {
                    byteOrder = (ByteOrder) BYTE_ORDER_PARSER.parse(rightNode, trace, null);
                } else if (left.equals(MetadataStrings.MANT_DIG)) {
                    mantissa = ((Long) UNARY_INTEGER_PARSER.parse((CommonTree) rightNode.getChild(0), null, null)).intValue();
                } else if (left.equals(MetadataStrings.ALIGN)) {
                    alignment = (Long) ALIGNMENT_PARSER.parse(rightNode, null, null);
                } else {
                    throw new ParseException("Float: unknown attribute " + left); //$NON-NLS-1$
                }

                break;
            default:
                throw childTypeError(child);
            }
        }
        int size = mantissa + exponent;
        if (size == 0) {
            throw new ParseException("Float missing size attribute"); //$NON-NLS-1$
        }

        if (alignment == 0) {
            alignment = 1;
        }

        floatDeclaration = new FloatDeclaration(exponent, mantissa, byteOrder, alignment);

        return floatDeclaration;

    }

}
