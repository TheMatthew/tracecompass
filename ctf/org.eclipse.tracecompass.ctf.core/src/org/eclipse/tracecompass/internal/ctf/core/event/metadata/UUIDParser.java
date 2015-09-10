package org.eclipse.tracecompass.internal.ctf.core.event.metadata;

import static org.eclipse.tracecompass.common.core.NonNullUtils.checkNotNull;
import static org.eclipse.tracecompass.internal.ctf.core.event.metadata.TsdlUtils.isAnyUnaryString;

import java.util.UUID;

import org.antlr.runtime.tree.CommonTree;
import org.eclipse.tracecompass.internal.ctf.core.event.metadata.exceptions.ParseException;

/**
 * UUID parser
 *
 * @author Matthew Khouzam
 *
 */
public class UUIDParser implements ICommonTreeParser {

    /** Instance */
    public static final UUIDParser INSTANCE = new UUIDParser();

    private UUIDParser() {
    }

    @Override
    public Object parse(CommonTree tree, Object param, String errorMsg) throws ParseException {

        CommonTree firstChild = (CommonTree) tree.getChild(0);

        if (isAnyUnaryString(firstChild)) {
            if (tree.getChildCount() > 1) {
                throw new ParseException("Invalid value for UUID"); //$NON-NLS-1$
            }

            String uuidstr = UnaryStringParser.INSTANCE.parse(firstChild, null, null);

            try {
                return checkNotNull(UUID.fromString(uuidstr));
            } catch (IllegalArgumentException e) {
                throw new ParseException("Invalid format for UUID", e); //$NON-NLS-1$
            }
        }
        throw new ParseException("Invalid value for UUID"); //$NON-NLS-1$
    }

}
