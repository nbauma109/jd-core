/*
 * Copyright (c) 2026 GPLv3
 * This project is distributed under the GPLv3 license.
 * This is a Copyleft license that gives the user the right to use,
 * copy and modify the code freely for non-commercial purposes.
 */

package org.jd.core.v1.service.converter.classfiletojavasyntax.util;

import org.jd.core.v1.model.javasyntax.AbstractJavaSyntaxVisitor;
import org.jd.core.v1.model.javasyntax.expression.MethodInvocationExpression;

public final class AddSuppressedVisitor extends AbstractJavaSyntaxVisitor {
    private boolean found;

    public boolean found() {
        return found;
    }

    @Override
    public void visit(MethodInvocationExpression expression) {
        if ("addSuppressed".equals(expression.getName()) && "(Ljava/lang/Throwable;)V".equals(expression.getDescriptor())) {
            found = true;
        }
    }
}