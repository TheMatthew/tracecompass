package org.eclipse.tracecompass.internal.ctf.core.event.metadata;

import java.util.List;

import org.antlr.runtime.tree.CommonTree;
import org.eclipse.tracecompass.internal.ctf.core.event.metadata.exceptions.ParseException;

public class TypeSpecifierListStringParser implements ICommonTreeParser {

    /**
     * Instance
     */
    public static final TypeSpecifierListStringParser INSTANCE = new TypeSpecifierListStringParser();

    private TypeSpecifierListStringParser() {
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
    public StringBuilder parse(CommonTree typeSpecifierList, Object param, String errorMsg) throws ParseException {
        StringBuilder sb = new StringBuilder();
        List<CommonTree> children = typeSpecifierList.getChildren();

        boolean firstItem = true;

        for (CommonTree child : children) {
            if (!firstItem) {
                sb.append(' ');

            }

            firstItem = false;

            /* Append the string that represents this type specifier. */
            sb.append(TypeSpecifierListParser.INSTANCE.parse(child, null, null));
        }
        return sb;
    }

}
