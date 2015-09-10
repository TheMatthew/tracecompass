package org.eclipse.tracecompass.internal.ctf.core.event.metadata;

import static org.eclipse.tracecompass.internal.ctf.core.event.metadata.TsdlUtils.childTypeError;

import org.antlr.runtime.tree.CommonTree;
import org.eclipse.tracecompass.ctf.parser.CTFParser;
import org.eclipse.tracecompass.internal.ctf.core.event.metadata.exceptions.ParseException;

public class TypeSpecifierListParser implements ICommonTreeParser {

    /**
     * Instance
     */
    public static final TypeSpecifierListParser INSTANCE = new TypeSpecifierListParser();

    private TypeSpecifierListParser() {
    }

    /**
     * Creates the string representation of a type specifier.
     *
     * @param typeSpecifier
     *            A TYPE_SPECIFIER node.
     * @return sb
     *            A StringBuilder to which will be appended the string.
     * @throws ParseException invalid node
     */
    @Override
    public StringBuilder parse(CommonTree typeSpecifier, Object param, String errorMsg) throws ParseException {
        StringBuilder sb = new StringBuilder();
        switch (typeSpecifier.getType()) {
        case CTFParser.FLOATTOK:
        case CTFParser.INTTOK:
        case CTFParser.LONGTOK:
        case CTFParser.SHORTTOK:
        case CTFParser.SIGNEDTOK:
        case CTFParser.UNSIGNEDTOK:
        case CTFParser.CHARTOK:
        case CTFParser.DOUBLETOK:
        case CTFParser.VOIDTOK:
        case CTFParser.BOOLTOK:
        case CTFParser.COMPLEXTOK:
        case CTFParser.IMAGINARYTOK:
        case CTFParser.CONSTTOK:
        case CTFParser.IDENTIFIER:
            parseSimple(typeSpecifier, sb);
            break;
        case CTFParser.STRUCT: {
            parseStruct(typeSpecifier, sb);
            break;
        }
        case CTFParser.VARIANT: {
            parseVariant(typeSpecifier, sb);
            break;
        }
        case CTFParser.ENUM: {
            parseEnum(typeSpecifier, sb);
            break;
        }
        case CTFParser.FLOATING_POINT:
        case CTFParser.INTEGER:
        case CTFParser.STRING:
            throw new ParseException("CTF type found in createTypeSpecifierString"); //$NON-NLS-1$
        default:
            throw childTypeError(typeSpecifier);
        }
        return sb;

    }

    private static void parseEnum(CommonTree typeSpecifier, StringBuilder sb) throws ParseException {
        CommonTree enumName = (CommonTree) typeSpecifier.getFirstChildWithType(CTFParser.ENUM_NAME);
        if (enumName == null) {
            throw new ParseException("nameless enum found in createTypeSpecifierString"); //$NON-NLS-1$
        }

        CommonTree enumNameIdentifier = (CommonTree) enumName.getChild(0);

        parseSimple(enumNameIdentifier, sb);
    }

    private static void parseVariant(CommonTree typeSpecifier, StringBuilder sb) throws ParseException {
        CommonTree variantName = (CommonTree) typeSpecifier.getFirstChildWithType(CTFParser.VARIANT_NAME);
        if (variantName == null) {
            throw new ParseException("nameless variant found in createTypeSpecifierString"); //$NON-NLS-1$
        }

        CommonTree variantNameIdentifier = (CommonTree) variantName.getChild(0);

        parseSimple(variantNameIdentifier, sb);
    }

    private static void parseSimple(CommonTree typeSpecifier, StringBuilder sb) {
        sb.append(typeSpecifier.getText());
    }

    private static void parseStruct(CommonTree typeSpecifier, StringBuilder sb) throws ParseException {
        CommonTree structName = (CommonTree) typeSpecifier.getFirstChildWithType(CTFParser.STRUCT_NAME);
        if (structName == null) {
            throw new ParseException("nameless struct found in createTypeSpecifierString"); //$NON-NLS-1$
        }

        CommonTree structNameIdentifier = (CommonTree) structName.getChild(0);

        parseSimple(structNameIdentifier, sb);
    }

}
