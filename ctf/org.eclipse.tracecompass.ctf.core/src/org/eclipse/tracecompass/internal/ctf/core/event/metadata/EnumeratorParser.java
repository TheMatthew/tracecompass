package org.eclipse.tracecompass.internal.ctf.core.event.metadata;

import static org.eclipse.tracecompass.internal.ctf.core.event.metadata.TsdlUtils.childTypeError;
import static org.eclipse.tracecompass.internal.ctf.core.event.metadata.TsdlUtils.isAnyUnaryString;

import java.math.BigInteger;
import java.util.List;

import org.antlr.runtime.tree.CommonTree;
import org.eclipse.tracecompass.ctf.core.event.types.EnumDeclaration;
import org.eclipse.tracecompass.ctf.parser.CTFParser;
import org.eclipse.tracecompass.internal.ctf.core.event.metadata.exceptions.ParseException;

public final class EnumeratorParser implements ICommonTreeParser {

    /**
     * Instance
     */
    public static final EnumeratorParser INSTANCE = new EnumeratorParser();

    private EnumeratorParser() {
    }

    /**
     * Parses an enumerator node and adds an enumerator declaration to an
     * enumeration declaration.
     *
     * The high value of the range of the last enumerator is needed in case the
     * current enumerator does not specify its value.
     *
     * @param enumerator
     *            An ENUM_ENUMERATOR node.
     * @param declaration
     *            an enumeration declaration to which will be added the
     *            enumerator.
     * @return The high value of the value range of the current enumerator.
     * @throws ParseException
     *             if the element failed to add
     */
    @Override
    public Long parse(CommonTree enumerator, Object declaration, String errorMsg) throws ParseException {
        if (!(declaration instanceof EnumDeclaration)) {
            throw new IllegalArgumentException("enumDeclaration must be an EnumDeclaration"); //$NON-NLS-1$
        }
        EnumDeclaration enumDeclaration = (EnumDeclaration) declaration;

        List<CommonTree> children = enumerator.getChildren();

        long low = 0, high = 0;
        boolean valueSpecified = false;
        String label = null;

        for (CommonTree child : children) {
            if (isAnyUnaryString(child)) {
                label = UnaryStringParser.INSTANCE.parse(child, null, null);
            } else if (child.getType() == CTFParser.ENUM_VALUE) {

                valueSpecified = true;

                low = UnaryIntegerParser.INSTANCE.parse((CommonTree) child.getChild(0), null, null);
                high = low;
            } else if (child.getType() == CTFParser.ENUM_VALUE_RANGE) {

                valueSpecified = true;

                low = UnaryIntegerParser.INSTANCE.parse((CommonTree) child.getChild(0), null, null);
                high = UnaryIntegerParser.INSTANCE.parse((CommonTree) child.getChild(1), null, null);
            } else {
                throw childTypeError(child);
            }
        }

        if (low > high) {
            throw new ParseException("enum low value greater than high value"); //$NON-NLS-1$
        }
        if (valueSpecified && !enumDeclaration.add(low, high, label)) {
            throw new ParseException("enum declarator values overlap."); //$NON-NLS-1$
        } else if (!enumDeclaration.add(label)) {
            throw new ParseException("enum cannot add element " + label); //$NON-NLS-1$
        }

        if (valueSpecified && (BigInteger.valueOf(low).compareTo(enumDeclaration.getContainerType().getMinValue()) == -1 ||
                BigInteger.valueOf(high).compareTo(enumDeclaration.getContainerType().getMaxValue()) == 1)) {
            throw new ParseException("enum value is not in range"); //$NON-NLS-1$
        }

        return high;
    }

}
