/*******************************************************************************
 * Copyright (c) 2015 Ericsson
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/
package org.eclipse.tracecompass.internal.ctf.core.event.metadata.tsdl.environment;

import java.util.List;
import java.util.Map;

import org.antlr.runtime.tree.CommonTree;
import org.eclipse.tracecompass.internal.ctf.core.event.metadata.ICommonTreeParser;

import com.google.common.collect.ImmutableMap;

public final class EnvironmentParser implements ICommonTreeParser {

    public static final EnvironmentParser INSTANCE = new EnvironmentParser();

    private EnvironmentParser() {
    }

    @Override
    public Map<String,String> parse(CommonTree environment, ICommonTreeParserParameter param) {

            ImmutableMap.Builder<String, String> builder = new ImmutableMap.Builder<>();
            List<CommonTree> children = environment.getChildren();
            for (CommonTree child : children) {
                String left;
                String right;
                left = child.getChild(0).getChild(0).getChild(0).getText();
                right = child.getChild(1).getChild(0).getChild(0).getText();
                builder.put(left, right);
            }
            return builder.build();
    }

}
