/*
 * Copyright (c) 2026 Nicolas Baumann (@nbauma109), with Claude Sonnet 5.5.
 * This project is distributed under the GPLv3 license.
 * This is a Copyleft license that gives the user the right to use,
 * copy and modify the code freely for non-commercial purposes.
 */

package org.jd.core.v1.service.converter.classfiletojavasyntax.visitor;

import org.jd.core.v1.model.javasyntax.expression.Expression;
import org.jd.core.v1.service.converter.classfiletojavasyntax.model.javasyntax.expression.ClassFileLocalVariableReferenceExpression;
import org.jd.core.v1.service.converter.classfiletojavasyntax.model.localvariable.AbstractLocalVariable;

import java.util.Map;

/**
 * Replaces the references to local variables by expressions, which are identified by the variable itself and not by its name:
 * the variable which a lambda captures may have no name yet (a synthetic temporary of the compiler has no entry in the local variable table).
 */
public class ReplaceLocalVariableVisitor extends AbstractUpdateExpressionVisitor {

    private final Map<AbstractLocalVariable, Expression> replacements;

    public ReplaceLocalVariableVisitor(Map<AbstractLocalVariable, Expression> replacements) {
        this.replacements = replacements;
    }

    @Override
    protected Expression updateExpression(Expression expression) {
        if (expression instanceof ClassFileLocalVariableReferenceExpression reference) {
            Expression replacement = replacements.get(reference.getLocalVariable());

            if (replacement != null) {
                return replacement;
            }
        }
        return expression;
    }
}
