package org.eclipse.tracecompass.internal.ctf.core.event.metadata;

import static org.eclipse.tracecompass.internal.ctf.core.event.metadata.TsdlUtils.childTypeError;
import static org.eclipse.tracecompass.internal.ctf.core.event.metadata.TsdlUtils.concatenateUnaryStrings;
import static org.eclipse.tracecompass.internal.ctf.core.event.metadata.TsdlUtils.isAnyUnaryString;

import java.util.List;

import org.antlr.runtime.tree.CommonTree;
import org.eclipse.jdt.annotation.NonNull;
import org.eclipse.tracecompass.ctf.core.event.types.Encoding;
import org.eclipse.tracecompass.ctf.core.event.types.StringDeclaration;
import org.eclipse.tracecompass.ctf.parser.CTFParser;
import org.eclipse.tracecompass.internal.ctf.core.event.metadata.exceptions.ParseException;

/**
 * Strings are an array of bytes of variable size and are terminated by a '\0'
 * “NULL” character. Their encoding is described in the TSDL metadata. In
 * absence of encoding attribute information, the default encoding is UTF-8.
 *
 * TSDL metadata representation of a named string type:
 *
 * <pre>
 * typealias string {
 *   encoding = /* UTF8 OR ASCII * /;
 * } := name;
 * </pre>
 *
 * A nameless string type can be declared as a field type:
 *
 * <pre>
 * string field_name; /* use default UTF8 encoding * /
 * </pre>
 *
 * Strings are always aligned on byte size.
 *
 * @author Matthew Khouzam
 * @author Efficios - Javadoc Preable
 *
 */
public class StringDeclarationParser implements ICommonTreeParser {

    /**
     * Instance
     */
    public static final StringDeclarationParser INSTANCE = new StringDeclarationParser();

    private static final @NonNull String ENCODING = "encoding"; //$NON-NLS-1$

    private StringDeclarationParser() {
    }

    @Override
    public StringDeclaration parse(CommonTree string, Object param, String errorMsg) throws ParseException {
        List<CommonTree> children = string.getChildren();
        StringDeclaration stringDeclaration = null;

        if (children == null) {
            stringDeclaration = StringDeclaration.getStringDeclaration(Encoding.UTF8);
        } else {
            Encoding encoding = Encoding.UTF8;
            for (CommonTree child : children) {
                switch (child.getType()) {
                case CTFParser.CTF_EXPRESSION_VAL:
                    /*
                     * An assignment expression must have 2 children, left and
                     * right
                     */

                    CommonTree leftNode = (CommonTree) child.getChild(0);
                    CommonTree rightNode = (CommonTree) child.getChild(1);

                    List<CommonTree> leftStrings = leftNode.getChildren();

                    if (!isAnyUnaryString(leftStrings.get(0))) {
                        throw new ParseException("Left side of ctf expression must be a string"); //$NON-NLS-1$
                    }
                    String left = concatenateUnaryStrings(leftStrings);

                    if (left.equals(ENCODING)) {
                        encoding = EncodingParser.INSTANCE.parse(rightNode, null, null);
                    } else {
                        throw new ParseException("String: unknown attribute " //$NON-NLS-1$
                                + left);
                    }

                    break;
                default:
                    throw childTypeError(child);
                }
            }

            stringDeclaration = StringDeclaration.getStringDeclaration(encoding);
        }

        return stringDeclaration;
    }

}
