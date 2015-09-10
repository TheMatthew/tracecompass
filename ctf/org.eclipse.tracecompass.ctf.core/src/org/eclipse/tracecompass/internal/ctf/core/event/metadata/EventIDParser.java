package org.eclipse.tracecompass.internal.ctf.core.event.metadata;

import static org.eclipse.tracecompass.internal.ctf.core.event.metadata.TsdlUtils.isUnaryInteger;

import org.antlr.runtime.tree.CommonTree;
import org.eclipse.tracecompass.internal.ctf.core.event.metadata.exceptions.ParseException;

public final class EventIDParser implements ICommonTreeParser {

    private static final String ERROR = "Invalid value for event id"; //$NON-NLS-1$
    public static final EventIDParser INSTANCE = new EventIDParser();

    private EventIDParser() {
    }

    @Override
    public Long parse(CommonTree tree, Object param, String errorMsg) throws ParseException {

        CommonTree firstChild = (CommonTree) tree.getChild(0);

        if (isUnaryInteger(firstChild)) {
            if (tree.getChildCount() > 1) {
                throw new ParseException(ERROR);
            }
            long intval = UnaryIntegerParser.INSTANCE.parse(firstChild, null, null);
            if (intval > Integer.MAX_VALUE) {
                throw new ParseException("Event id larger than int.maxvalue, something is amiss"); //$NON-NLS-1$
            }
            return intval;
        }
        throw new ParseException(ERROR);
    }

}
