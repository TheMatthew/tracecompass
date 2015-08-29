package org.eclipse.tracecompass.internal.ctf.core.event.metadata;

import java.util.List;

import org.eclipse.tracecompass.internal.ctf.core.event.metadata.UnaryStringParser;

import org.antlr.runtime.tree.CommonTree;
import org.eclipse.tracecompass.ctf.parser.CTFParser;

/**
 * TSDL utils, this class provides some simple verifications for a common tree.
 * These are useful before parsing.
 *
 * @author Matthew Khouzam
 *
 */
public final class TsdlUtils {

    private static UnaryStringParser stringParser = new UnaryStringParser();

    private TsdlUtils() {
    }

    /**
     * Is the tree a unary string
     *
     * @param node
     *            The node to check.
     * @return True if the given node is an unary string.
     */
    public static boolean isUnaryString(CommonTree node) {
        return ((node.getType() == CTFParser.UNARY_EXPRESSION_STRING));
    }

    /**
     * Is the tree a unary string or a quoted string
     *
     * @param node
     *            The node to check.
     * @return True if the given node is any type of unary string (no quotes,
     *         quotes, etc).
     */
    public static boolean isAnyUnaryString(CommonTree node) {
        return (isUnaryString(node) || (node.getType() == CTFParser.UNARY_EXPRESSION_STRING_QUOTES));
    }

    /**
     * Is the tree a unary integer
     *
     * @param node
     *            The node to check.
     * @return True if the given node is an unary integer.
     */
    public static boolean isUnaryInteger(CommonTree node) {
        return ((node.getType() == CTFParser.UNARY_EXPRESSION_DEC) ||
                (node.getType() == CTFParser.UNARY_EXPRESSION_HEX) || (node.getType() == CTFParser.UNARY_EXPRESSION_OCT));
    }

    /**
     * Concatenates a list of unary strings separated by arrows (->) or dots.
     *
     * @param strings
     *            A list, first element being an unary string, subsequent
     *            elements being ARROW or DOT nodes with unary strings as child.
     * @return The string representation of the unary string chain.
     */
    public static String concatenateUnaryStrings(List<CommonTree> strings) {

        StringBuilder sb = new StringBuilder();

        CommonTree first = strings.get(0);
        sb.append(stringParser.parse(first, null, null));

        boolean isFirst = true;

        for (CommonTree ref : strings) {
            if (isFirst) {
                isFirst = false;
                continue;
            }

            CommonTree id = (CommonTree) ref.getChild(0);

            if (ref.getType() == CTFParser.ARROW) {
                sb.append("->"); //$NON-NLS-1$
            } else { /* DOT */
                sb.append('.');
            }

            sb.append(stringParser.parse(id, null, null));
        }

        return sb.toString();
    }

}
