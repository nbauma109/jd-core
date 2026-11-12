/*
 * Copyright (c) 2026 Nicolas Baumann (@nbauma109), with Claude Sonnet 5.5.
 * This project is distributed under the GPLv3 license.
 * This is a Copyleft license that gives the user the right to use,
 * copy and modify the code freely for non-commercial purposes.
 */
package org.jd.core.v1.service.converter.classfiletojavasyntax.visitor;

import org.jd.core.v1.model.javasyntax.AbstractJavaSyntaxVisitor;
import org.jd.core.v1.model.javasyntax.expression.Expression;
import org.jd.core.v1.model.javasyntax.expression.InstanceOfExpression;
import org.jd.core.v1.model.javasyntax.expression.ParenthesesExpression;
import org.jd.core.v1.model.javasyntax.expression.PreOperatorExpression;
import org.jd.core.v1.model.javasyntax.pattern.TypePattern;
import org.jd.core.v1.model.javasyntax.statement.BaseStatement;
import org.jd.core.v1.model.javasyntax.statement.IfElseStatement;
import org.jd.core.v1.model.javasyntax.statement.IfStatement;
import org.jd.core.v1.model.javasyntax.statement.Statement;
import org.jd.core.v1.model.javasyntax.statement.Statements;

import java.util.ListIterator;

/**
 * 'if (o instanceof String s) {} else return false; use(s);' binds 's' after the statement only when the test is negated:
 * 'if (!(o instanceof String s)) return false; use(s);'
 */
public class EmptyThenWithPatternVisitor extends AbstractJavaSyntaxVisitor {

    @Override
    public void visit(Statements statements) {
        ListIterator<Statement> iterator = statements.listIterator();

        while (iterator.hasNext()) {
            Statement statement = iterator.next();

            if (statement instanceof IfElseStatement ifElse && isEmpty(ifElse.getStatements()) && containsPattern(ifElse.getCondition())) {
                iterator.set(new IfStatement(negate(ifElse.getCondition()), ifElse.getElseStatements()));
            }
            iterator.previous().accept(this);
            iterator.next();
        }
    }

    private static boolean isEmpty(BaseStatement statement) {
        return statement == null || statement.isStatements() && statement.size() == 0;
    }

    private static boolean containsPattern(Expression condition) {
        boolean[] found = new boolean[1];

        condition.accept(new AbstractJavaSyntaxVisitor() {
            @Override
            public void visit(InstanceOfExpression expression) {
                found[0] |= expression.getPattern() instanceof TypePattern pattern && pattern.variableName() != null;
                super.visit(expression);
            }
        });
        return found[0];
    }

    private static Expression negate(Expression condition) {
        if (condition instanceof PreOperatorExpression preOperator && "!".equals(preOperator.getOperator())) {
            return preOperator.getExpression();
        }

        boolean needParentheses = condition.isBinaryOperatorExpression() || condition instanceof InstanceOfExpression || condition.isTernaryOperatorExpression();
        return new PreOperatorExpression(condition.getLineNumber(), "!", needParentheses ? new ParenthesesExpression(condition.getLineNumber(), condition) : condition);
    }
}
