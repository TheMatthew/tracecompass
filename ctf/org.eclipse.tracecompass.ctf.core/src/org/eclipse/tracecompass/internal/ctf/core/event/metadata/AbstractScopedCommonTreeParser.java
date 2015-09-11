/*******************************************************************************
 * Copyright (c) 2015 Ericsson
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/

package org.eclipse.tracecompass.internal.ctf.core.event.metadata;

import org.eclipse.jdt.annotation.Nullable;
import org.eclipse.tracecompass.ctf.core.event.metadata.DeclarationScope;

public abstract class AbstractScopedCommonTreeParser implements ICommonTreeParser {

    private DeclarationScope fScope;

    // ------------------------------------------------------------------------
    // Scope management
    // ------------------------------------------------------------------------
    /**
     * Set the scope
     *
     * @param scope
     *            the scope
     */
    protected void setScope(DeclarationScope scope) {
        fScope = scope;
    }

    /**
     * Adds a new declaration scope on the top of the scope stack.
     */
    protected void pushScope(String name) {
        fScope = new DeclarationScope(getCurrentScope(), name);
    }

    /**
     * Removes the top declaration scope from the scope stack.
     */
    protected void popScope() {
        fScope = getCurrentScope().getParentScope();
    }

    /**
     * Adds a new declarationscope that may have a name and if not will take the
     * default name
     *
     * @param name
     *            the name
     * @param defaultName
     *            the default name
     */
    protected void pushNamedScope(@Nullable String name, String defaultName) {
        pushScope(name == null ? defaultName : name);
    }

    /**
     * Returns the current declaration scope.
     *
     * @return The current declaration scope.
     */
    protected DeclarationScope getCurrentScope() {
        return fScope;
    }

}
