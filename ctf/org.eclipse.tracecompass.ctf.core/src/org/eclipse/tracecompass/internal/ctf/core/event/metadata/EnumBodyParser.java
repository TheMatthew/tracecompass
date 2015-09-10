package org.eclipse.tracecompass.internal.ctf.core.event.metadata;

import java.util.List;

import org.antlr.runtime.tree.CommonTree;
import org.eclipse.tracecompass.ctf.core.event.metadata.DeclarationScope;
import org.eclipse.tracecompass.ctf.core.event.types.EnumDeclaration;
import org.eclipse.tracecompass.internal.ctf.core.event.metadata.exceptions.ParseException;

public class EnumBodyParser extends AbstractScopedCommonTreeParser {

    public static final class Param {

        private final EnumDeclaration fEnumDeclaration;
        private final String fEnumName;
        private final DeclarationScope fCurrentScope;

        public Param(EnumDeclaration enumDeclaration, String enumName, DeclarationScope currentScope) {
            fEnumDeclaration = enumDeclaration;
            fEnumName = enumName;
            fCurrentScope = currentScope;
        }

    }

    public static final EnumBodyParser INSTANCE = new EnumBodyParser();

    private EnumBodyParser() {
    }

    @Override
    public Object parse(CommonTree tree, Object param, String errorMsg) throws ParseException {
        if (!(param instanceof Param)) {
            throw new IllegalArgumentException("param must be of type EnumBodyParser.Param"); //$NON-NLS-1$
        }
        Param parameter = (Param) param;
        setScope(parameter.fCurrentScope);
        List<CommonTree> enumerators = tree.getChildren();
        /* enum body can't be empty (unlike struct). */
        pushNamedScope(parameter.fEnumName, MetadataStrings.ENUM);
        /*
         * Start at -1, so that if the first enumrator has no explicit value, it
         * will choose 0
         */
        for (CommonTree enumerator : enumerators) {
            EnumeratorParser.INSTANCE.parse(enumerator, parameter.fEnumDeclaration,
                    null);
        }
        popScope();
        return param;
    }

}
