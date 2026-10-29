/*
 * Copyright (c) 2026 Nicolas Baumann (@nbauma109), with Claude Sonnet 5.5.
 * This project is distributed under the GPLv3 license.
 * This is a Copyleft license that gives the user the right to use,
 * copy and modify the code freely for non-commercial purposes.
 */
package org.jd.core.v1.service.converter.classfiletojavasyntax.visitor;

import org.jd.core.v1.model.javasyntax.AbstractJavaSyntaxVisitor;
import org.jd.core.v1.model.javasyntax.expression.ArrayExpression;
import org.jd.core.v1.model.javasyntax.expression.BinaryOperatorExpression;
import org.jd.core.v1.model.javasyntax.expression.CastExpression;
import org.jd.core.v1.model.javasyntax.expression.Expression;
import org.jd.core.v1.model.javasyntax.expression.FieldReferenceExpression;
import org.jd.core.v1.model.javasyntax.expression.LocalVariableReferenceExpression;
import org.jd.core.v1.model.javasyntax.expression.LengthExpression;
import org.jd.core.v1.model.javasyntax.expression.MethodInvocationExpression;
import org.jd.core.v1.model.javasyntax.expression.NewArray;
import org.jd.core.v1.model.javasyntax.expression.NewExpression;
import org.jd.core.v1.model.javasyntax.expression.PostOperatorExpression;
import org.jd.core.v1.model.javasyntax.expression.PreOperatorExpression;
import org.jd.core.v1.model.javasyntax.expression.SwitchExpression;
import org.jd.core.v1.model.javasyntax.expression.TernaryOperatorExpression;
import org.jd.core.v1.service.converter.classfiletojavasyntax.model.javasyntax.expression.ClassFileLocalVariableReferenceExpression;
import org.jd.core.v1.service.converter.classfiletojavasyntax.model.localvariable.AbstractLocalVariable;

/**
 * 'if (i > 10 && ++i != 15)': the 'iinc' of the second operand of a test is a statement of the byte code, which has to be evaluated by the
 * test: the first use of the variable in the operand, in the order of the evaluation, becomes '++i'. That use must be a value: '++i' is not
 * the operand of another increment or the target of an assignment, and it executes whenever the statement did: the statement is kept
 * if the use is in the right operand of '&&' or '||' or in a branch of a ternary operator, or if an operation which can throw is evaluated before
 * the use (the handler would see the variable before the increment).
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

        if (firstUse.found == null || firstUse.blocked) {
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
        /** The first use is the operand of an increment, the target of an assignment or only evaluated sometimes: it cannot be '++i' */
        private boolean blocked;
        /** The number of operands which are only evaluated sometimes, around the expression which is visited */
        private int conditional;
        /** An operation which can throw (an array access, a call, a division...) is evaluated before the first use */
        private boolean throwing;

        FirstUse(AbstractLocalVariable localVariable) {
            this.localVariable = localVariable;
        }

        @Override
        public void visit(LocalVariableReferenceExpression expression) {
            if (found == null && isTheVariable(expression)) {
                found = expression;
                blocked = conditional > 0 || throwing;
            }
        }

        @Override
        public void visit(ArrayExpression expression) {
            throwing |= found == null;
            super.visit(expression);
        }

        @Override
        public void visit(LengthExpression expression) {
            throwing |= found == null;
            super.visit(expression);
        }

        @Override
        public void visit(MethodInvocationExpression expression) {
            throwing |= found == null;
            super.visit(expression);
        }

        @Override
        public void visit(FieldReferenceExpression expression) {
            // The read of a static field can initialize its class, which can throw
            throwing |= found == null;
            super.visit(expression);
        }

        @Override
        public void visit(CastExpression expression) {
            throwing |= found == null;
            super.visit(expression);
        }

        @Override
        public void visit(NewArray expression) {
            // A negative size throws
            throwing |= found == null;
            super.visit(expression);
        }

        @Override
        public void visit(NewExpression expression) {
            throwing |= found == null;
            super.visit(expression);
        }

        @Override
        public void visit(PreOperatorExpression expression) {
            if (found == null && isIncrement(expression.getOperator()) && isTheVariable(expression.getExpression())) {
                found = expression.getExpression();
                blocked = true;
            }
            super.visit(expression);
        }

        @Override
        public void visit(PostOperatorExpression expression) {
            if (found == null && isIncrement(expression.getOperator()) && isTheVariable(expression.getExpression())) {
                found = expression.getExpression();
                blocked = true;
            }
            super.visit(expression);
        }

        @Override
        public void visit(BinaryOperatorExpression expression) {
            if (found == null && isAssignment(expression.getOperator()) && isTheVariable(expression.getLeftExpression())) {
                found = expression.getLeftExpression();
                blocked = true;
            }
            throwing |= found == null && ("/".equals(expression.getOperator()) || "%".equals(expression.getOperator()));
            if ("&&".equals(expression.getOperator()) || "||".equals(expression.getOperator())) {
                expression.getLeftExpression().accept(this);
                conditional++;
                expression.getRightExpression().accept(this);
                conditional--;
            } else {
                super.visit(expression);
            }
        }

        @Override
        public void visit(TernaryOperatorExpression expression) {
            expression.getCondition().accept(this);
            conditional++;
            expression.getTrueExpression().accept(this);
            expression.getFalseExpression().accept(this);
            conditional--;
        }

        /** The rules of a switch expression are evaluated sometimes: the one which is taken may not read the variable */
        @Override
        public void visit(SwitchExpression expression) {
            conditional++;
            super.visit(expression);
            conditional--;
        }

        private boolean isTheVariable(Expression expression) {
            return expression instanceof ClassFileLocalVariableReferenceExpression reference && reference.getLocalVariable() == localVariable;
        }

        private static boolean isIncrement(String operator) {
            return "++".equals(operator) || "--".equals(operator);
        }

        /** '=' and the compound assignments, not the comparisons which end with '=' too */
        private static boolean isAssignment(String operator) {
            return operator.endsWith("=") && !"==".equals(operator) && !"!=".equals(operator) && !"<=".equals(operator) && !">=".equals(operator);
        }
    }
}
