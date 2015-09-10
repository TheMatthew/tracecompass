package org.eclipse.tracecompass.internal.ctf.core.event.metadata;

import java.util.List;

import org.antlr.runtime.tree.CommonTree;
import org.eclipse.tracecompass.internal.ctf.core.event.metadata.exceptions.ParseException;

public class PointerListStringParser implements ICommonTreeParser {

    /**
     * Instance
     */
    public static final PointerListStringParser INSTANCE = new PointerListStringParser();

    private PointerListStringParser() {
    }

    /**
     * Creates the string representation of a type specifier.
     *
     * @param PointerList
     *            A TYPE_SPECIFIER node.
     * @return A StringBuilder to which will be appended the string.
     * @throws ParseException
     *             invalid node
     */
    @Override
    public StringBuilder parse(CommonTree pointers, Object param, String errorMsg) {
        StringBuilder sb = new StringBuilder();
        List<CommonTree> pointerList = pointers.getChildren();
        if (pointers.getChildCount() == 0) {
            return sb;
        }

        for (CommonTree pointer : pointerList) {

            sb.append(" *"); //$NON-NLS-1$
            if (pointer.getChildCount() > 0) {

                sb.append(" const"); //$NON-NLS-1$
            }
        }
        return sb;
    }

}
