package org.eclipse.tracecompass.internal.ctf.core.event.metadata;

import java.util.List;

import org.antlr.runtime.tree.CommonTree;
import org.eclipse.tracecompass.internal.ctf.core.event.metadata.exceptions.ParseException;

public class TypeDeclarationStringParser implements ICommonTreeParser {

    /**
     * Instance
     */
    public static final TypeDeclarationStringParser INSTANCE = new TypeDeclarationStringParser();

    private TypeDeclarationStringParser() {
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
    public String parse(CommonTree typeSpecifierList, Object param, String errorMsg) throws ParseException {
        if (!(param instanceof List)) {
            throw new IllegalArgumentException("Param must be a List<CommonTree>");
        }
        List pointers = (List) param;
        CommonTree temp = new CommonTree();
        temp.addChildren(pointers);
        StringBuilder sb = new StringBuilder();

        sb.append(TypeSpecifierListStringParser.INSTANCE.parse(typeSpecifierList, null, null));
        sb.append(PointerListStringParser.INSTANCE.parse(temp, null, null));

        return sb.toString();
    }

}
