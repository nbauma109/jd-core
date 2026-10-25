/*
 * Copyright (c) 2026 Nicolas Baumann (@nbauma109), with Claude Sonnet 5.5.
 * This project is distributed under the GPLv3 license.
 * This is a Copyleft license that gives the user the right to use,
 * copy and modify the code freely for non-commercial purposes.
 */
package org.jd.core.v1.service.converter.classfiletojavasyntax.visitor;

import org.jd.core.v1.model.javasyntax.expression.Expression;
import org.jd.core.v1.model.javasyntax.expression.PreOperatorExpression;
import org.jd.core.v1.service.converter.classfiletojavasyntax.model.javasyntax.expression.ClassFileLocalVariableReferenceExpression;
import org.jd.core.v1.service.converter.classfiletojavasyntax.model.localvariable.AbstractLocalVariable;

/**
 * 'if (i > 10 && ++i != 15)': the 'iinc' of the second operand of a test is a statement of the byte code, which has to be evaluated by the
 * test: the first use of the variable in the operand becomes '++i'.
 */
public class FoldIncrementIntoConditionVisitor extends AbstractUpdateExpressionVisitor {
    private final AbstractLocalVariable localVariable;
    private final String operator;
    private boolean folded;

    public FoldIncrementIntoConditionVisitor(AbstractLocalVariable localVariable, String operator) {
        this.localVariable = localVariable;
        this.operator = operator;
    }

    public boolean isFolded() {
        return folded;
    }

    @Override
    public Expression updateExpression(Expression expression) {
        if (!folded && expression instanceof ClassFileLocalVariableReferenceExpression reference && reference.getLocalVariable() == localVariable) {
            folded = true;
            return new PreOperatorExpression(reference.getLineNumber(), operator, reference);
        }
        return expression;
    }
}
