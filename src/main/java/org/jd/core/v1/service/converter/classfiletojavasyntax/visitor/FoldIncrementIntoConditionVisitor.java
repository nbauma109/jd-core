/*
 * Copyright (c) 2026 Nicolas Baumann (@nbauma109), with Claude Sonnet 5.5.
 * This project is distributed under the GPLv3 license.
 * This is a Copyleft license that gives the user the right to use,
 * copy and modify the code freely for non-commercial purposes.
 */
package org.jd.core.v1.service.converter.classfiletojavasyntax.visitor;

import org.jd.core.v1.model.javasyntax.AbstractJavaSyntaxVisitor;
import org.jd.core.v1.model.javasyntax.expression.Expression;
import org.jd.core.v1.model.javasyntax.expression.LocalVariableReferenceExpression;
import org.jd.core.v1.model.javasyntax.expression.PreOperatorExpression;
import org.jd.core.v1.service.converter.classfiletojavasyntax.model.javasyntax.expression.ClassFileLocalVariableReferenceExpression;
import org.jd.core.v1.service.converter.classfiletojavasyntax.model.localvariable.AbstractLocalVariable;

/**
 * 'if (i > 10 && ++i != 15)': the 'iinc' of the second operand of a test is a statement of the byte code, which has to be evaluated by the
 * test: the first use of the variable in the operand, in the order of the evaluation, becomes '++i'.
 */
public final class FoldIncrementIntoConditionVisitor {

    private FoldIncrementIntoConditionVisitor() {}

    /** @return the operand where the first use of the variable is '++i' or '--i', null if the operand does not use the variable */
    public static Expression fold(Expression operand, AbstractLocalVariable localVariable, String operator) {
        FirstUse firstUse = new FirstUse(localVariable);

        if (operand instanceof ClassFileLocalVariableReferenceExpression reference && reference.getLocalVariable() == localVariable) {
            return new PreOperatorExpression(reference.getLineNumber(), operator, reference);
        }
        operand.accept(firstUse);

        if (firstUse.found == null) {
            return null;
        }
        operand.accept(new AbstractUpdateExpressionVisitor() {
            private boolean replaced;

            @Override
            protected Expression updateExpression(Expression expression) {
                if (!replaced && expression == firstUse.found) {
                    replaced = true;
                    return new PreOperatorExpression(expression.getLineNumber(), operator, expression);
                }
                return expression;
            }
        });
        return operand;
    }

    /** The visits go through the operands in the order of their evaluation: left before right, the arguments one after the other */
    private static final class FirstUse extends AbstractJavaSyntaxVisitor {
        private final AbstractLocalVariable localVariable;
        private Expression found;

        FirstUse(AbstractLocalVariable localVariable) {
            this.localVariable = localVariable;
        }

        @Override
        public void visit(LocalVariableReferenceExpression expression) {
            if (found == null && expression instanceof ClassFileLocalVariableReferenceExpression reference && reference.getLocalVariable() == localVariable) {
                found = expression;
            }
        }
    }
}
