/*
 * Copyright (c) 2026 Nicolas Baumann (@nbauma109), with Claude Sonnet 5.5.
 * This project is distributed under the GPLv3 license.
 * This is a Copyleft license that gives the user the right to use,
 * copy and modify the code freely for non-commercial purposes.
 */

package org.jd.core.v1.service.converter.classfiletojavasyntax.visitor;

import org.jd.core.v1.model.javasyntax.expression.Expression;
import org.jd.core.v1.model.javasyntax.expression.InstanceOfExpression;
import org.jd.core.v1.model.javasyntax.expression.LocalVariableReferenceExpression;
import org.jd.core.v1.model.javasyntax.expression.ParenthesesExpression;
import org.jd.core.v1.model.javasyntax.expression.PreOperatorExpression;
import org.jd.core.v1.model.javasyntax.statement.ExpressionStatement;
import org.jd.core.v1.model.javasyntax.statement.IfElseStatement;
import org.jd.core.v1.model.javasyntax.statement.IfStatement;
import org.jd.core.v1.model.javasyntax.statement.Statement;
import org.jd.core.v1.model.javasyntax.statement.Statements;
import org.jd.core.v1.model.javasyntax.type.ObjectType;
import org.jd.core.v1.model.javasyntax.type.PrimitiveType;
import org.junit.Test;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;

/** 'if (o instanceof String s) {} else return false;' only binds 's' after the statement when the test is negated */
public class EmptyThenWithPatternVisitorTest {
    private static final ObjectType STRING = new ObjectType("java/lang/String", "java.lang.String", "String");

    private static Expression object() {
        return new LocalVariableReferenceExpression(ObjectType.TYPE_OBJECT, "o");
    }

    private static Statement statement() {
        return new ExpressionStatement(new LocalVariableReferenceExpression(PrimitiveType.TYPE_INT, "i"));
    }

    private static Statement visit(Statement statement) {
        Statements statements = new Statements();

        statements.add(statement);
        new EmptyThenWithPatternVisitor().visit(statements);
        return statements.getFirst();
    }

    private static Statements statements(Statement... list) {
        Statements statements = new Statements();

        for (Statement statement : list) {
            statements.add(statement);
        }
        return statements;
    }

    @Test
    public void testPatternIsNegated() {
        Statement visited = visit(new IfElseStatement(new InstanceOfExpression(1, object(), STRING, "s", false), new Statements(), statements(statement())));

        assertFalse(visited instanceof IfElseStatement);

        Expression condition = ((IfStatement) visited).getCondition();

        assertTrue(condition instanceof PreOperatorExpression);
        assertTrue(((PreOperatorExpression) condition).getExpression() instanceof ParenthesesExpression);
    }

    @Test
    public void testNegatedPatternIsNotNegatedTwice() {
        InstanceOfExpression test = new InstanceOfExpression(1, object(), STRING, "s", false);
        Statement visited = visit(new IfElseStatement(new PreOperatorExpression(1, "!", new ParenthesesExpression(test)), new Statements(), statements(statement())));

        assertTrue(((IfStatement) visited).getCondition() instanceof ParenthesesExpression);
    }

    @Test
    public void testConditionOfAnotherKindIsNegatedWithoutParentheses() {
        Expression flag = new LocalVariableReferenceExpression(PrimitiveType.TYPE_BOOLEAN, "flag");
        Statement visited = visit(new IfElseStatement(flag, new Statements(), statements(statement())));

        assertSame(IfElseStatement.class, visited.getClass());
    }

    @Test
    public void testMissingThenIsEmpty() {
        Statement visited = visit(new IfElseStatement(new InstanceOfExpression(1, object(), STRING, "s", false), null, statements(statement())));

        assertFalse(visited instanceof IfElseStatement);
    }

    @Test
    public void testOtherIfAreKept() {
        Statement withThen = new IfElseStatement(new InstanceOfExpression(1, object(), STRING, "s", false), statements(statement()), statements(statement()));
        Statement withoutVariable = new IfElseStatement(new InstanceOfExpression(1, object(), STRING), new Statements(), statements(statement()));
        Statement withoutElse = new IfStatement(new InstanceOfExpression(1, object(), STRING, "s", false), statements(statement()));

        assertSame(withThen, visit(withThen));
        assertSame(withoutVariable, visit(withoutVariable));
        assertSame(withoutElse, visit(withoutElse));
    }
}
