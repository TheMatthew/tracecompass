package org.eclipse.tracecompass.internal.ctf.core.event.metadata;

import static org.eclipse.tracecompass.internal.ctf.core.event.metadata.TsdlUtils.isUnaryInteger;

import org.antlr.runtime.tree.CommonTree;
import org.eclipse.tracecompass.internal.ctf.core.event.metadata.exceptions.ParseException;

public final class VersionNumberParser implements ICommonTreeParser {

    private static final String ERROR = "Invalid value for major/minor"; //$NON-NLS-1$
    public static final VersionNumberParser INSTANCE = new VersionNumberParser();

    private VersionNumberParser() {
    }

    @Override
    public Long parse(CommonTree tree, Object param, String errorMsg) throws ParseException {

        CommonTree firstChild = (CommonTree) tree.getChild(0);

        if (isUnaryInteger(firstChild)) {
            if (tree.getChildCount() > 1) {
                throw new ParseException(ERROR);
            }
            long version = UnaryIntegerParser.INSTANCE.parse(firstChild, null, null);
            if (version < 0) {
                throw new ParseException(ERROR);
            }
            return version;
        }
        throw new ParseException(ERROR);
    }

}
