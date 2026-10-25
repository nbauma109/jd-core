/*
 * Copyright (c) 2026 Nicolas Baumann (@nbauma109), with Claude Sonnet 5.5.
 * This project is distributed under the GPLv3 license.
 * This is a Copyleft license that gives the user the right to use,
 * copy and modify the code freely for non-commercial purposes.
 */
package org.jd.core.v1.service.converter.classfiletojavasyntax.model.javasyntax.expression;

import org.jd.core.v1.model.javasyntax.expression.Expression;
import org.jd.core.v1.model.javasyntax.expression.InstanceOfExpression;
import org.jd.core.v1.model.javasyntax.type.Type;
import org.jd.core.v1.service.converter.classfiletojavasyntax.model.localvariable.AbstractLocalVariable;

/**
 * 'o instanceof T v': the name of the variable, which is only known when the local variables are named, is resolved later by
 * {@code ResolveInstanceOfPatternVisitor}.
 */
public class ClassFileInstanceOfPatternExpression extends InstanceOfExpression {
    private final AbstractLocalVariable localVariable;

    public ClassFileInstanceOfPatternExpression(int lineNumber, Expression expression, Type instanceOfType, AbstractLocalVariable localVariable) {
        super(lineNumber, expression, instanceOfType);
        this.localVariable = localVariable;
    }

    public AbstractLocalVariable getLocalVariable() {
        return localVariable;
    }
}
