/*******************************************************************************
 * Copyright (c) 2015 Ericsson
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/

package org.eclipse.tracecompass.internal.ctf.core.event.metadata;

import java.util.List;

import org.antlr.runtime.tree.CommonTree;
import org.eclipse.tracecompass.ctf.core.event.CTFClock;
import org.eclipse.tracecompass.ctf.parser.CTFParser;

public class ClockParser implements ICommonTreeParser {

    /**
     * Instance
     */
    public static final ClockParser INSTANCE =  new ClockParser();

    private ClockParser() {
    }

    @Override
    public CTFClock parse(CommonTree clock, ICommonTreeParserParameter unused) throws ParseException {
            List<CommonTree> children = clock.getChildren();
            CTFClock ctfClock = new CTFClock();
            for (CommonTree child : children) {
                final String key = child.getChild(0).getChild(0).getChild(0).getText();
                final CommonTree value = (CommonTree) child.getChild(1).getChild(0).getChild(0);
                final int type = value.getType();
                final String text = value.getText();
                switch (type) {
                case CTFParser.INTEGER:
                case CTFParser.DECIMAL_LITERAL:
                    /*
                     * Not a pretty hack, this is to make sure that there is no
                     * number overflow due to 63 bit integers. The offset should
                     * only really be an issue in the year 2262. the tracer in C/ASM
                     * can write an offset in an unsigned 64 bit long. In java, the
                     * last bit, being set to 1 will be read as a negative number,
                     * but since it is too big a positive it will throw an
                     * exception. this will happen in 2^63 ns from 1970. Therefore
                     * 293 years from 1970
                     */
                    Long numValue;
                    try {
                        numValue = Long.parseLong(text);
                    } catch (NumberFormatException e) {
                        throw new ParseException("Number conversion issue with " + text, e); //$NON-NLS-1$
                    }
                    ctfClock.addAttribute(key, numValue);
                    break;
                default:
                    ctfClock.addAttribute(key, text);
                }

            }
            return ctfClock;

    }

}
