package org.eclipse.tracecompass.internal.ctf.core.event.metadata;

import org.antlr.runtime.tree.CommonTree;
import org.eclipse.jdt.annotation.NonNull;

/**
 * A reference to the clock map in a given integer.
 *
 * @author Matthew Khouzam
 *
 */
public class ClockParser implements ICommonTreeParser {

    private static final @NonNull String EMPTY_STRING = ""; //$NON-NLS-1$

    @Override
    public String parse(CommonTree tree, Object param, String errorMsg) {
        String clock = tree.getChild(1).getChild(0).getChild(0).getText();
        return clock == null ? EMPTY_STRING : clock;
    }

}
