/*
 * Copyright (c) 2026 Nicolas Baumann (@nbauma109).
 * This project is distributed under the GPLv3 license.
 * This is a Copyleft license that gives the user the right to use,
 * copy and modify the code freely for non-commercial purposes.
 */
package org.jd.core.v1.service.converter.classfiletojavasyntax.util;

import org.jd.core.v1.model.javasyntax.expression.BinaryOperatorExpression;
import org.jd.core.v1.model.javasyntax.expression.BooleanExpression;
import org.jd.core.v1.model.javasyntax.expression.DoubleConstantExpression;
import org.jd.core.v1.model.javasyntax.expression.Expression;
import org.jd.core.v1.model.javasyntax.expression.IntegerConstantExpression;
import org.jd.core.v1.model.javasyntax.expression.LocalVariableReferenceExpression;
import org.jd.core.v1.model.javasyntax.expression.NullExpression;
import org.jd.core.v1.model.javasyntax.expression.ParenthesesExpression;
import org.jd.core.v1.model.javasyntax.expression.PreOperatorExpression;
import org.jd.core.v1.model.javasyntax.expression.TernaryOperatorExpression;
import org.jd.core.v1.model.javasyntax.statement.BaseStatement;
import org.jd.core.v1.model.javasyntax.statement.ExpressionStatement;
import org.jd.core.v1.model.javasyntax.statement.IfElseStatement;
import org.jd.core.v1.model.javasyntax.statement.IfStatement;
import org.jd.core.v1.model.javasyntax.statement.ReturnExpressionStatement;
import org.jd.core.v1.model.javasyntax.statement.Statement;
import org.jd.core.v1.model.javasyntax.statement.Statements;
import org.jd.core.v1.model.javasyntax.type.ObjectType;
import org.jd.core.v1.model.javasyntax.type.PrimitiveType;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/** The tests of one 'return' of a boolean expression are compiled into jumps to a few returns of 'true' and 'false'. */
public class BooleanReturnRewriterTest {
    private static final int LINE = 10;

    private static Expression variable(String name) {
        return new LocalVariableReferenceExpression(LINE, PrimitiveType.TYPE_BOOLEAN, name);
    }

    private static Statement returns(boolean value, int line) {
        return new ReturnExpressionStatement(line, new BooleanExpression(line, value));
    }

    private static Statement returns(boolean value) {
        return returns(value, LINE);
    }

    private static Statement ifThen(Expression condition, BaseStatement body) {
        return new IfStatement(condition, body);
    }

    private static Statement ifThenElse(Expression condition, BaseStatement body, BaseStatement elseBody) {
        return new IfElseStatement(condition, body, elseBody);
    }

    private static Statements list(Statement... statements) {
        Statements list = new Statements();

        for (Statement statement : statements) {
            list.add(statement);
        }
        return list;
    }

    private static String render(Expression expression) {
        if (expression instanceof BinaryOperatorExpression binary) {
            return "(" + render(binary.getLeftExpression()) + " " + binary.getOperator() + " " + render(binary.getRightExpression()) + ")";
        }
        if (expression instanceof TernaryOperatorExpression ternary) {
            return "(" + render(ternary.getCondition()) + " ? " + render(ternary.getTrueExpression()) + " : " + render(ternary.getFalseExpression()) + ")";
        }
        if (expression instanceof PreOperatorExpression preOperator) {
            return preOperator.getOperator() + render(preOperator.getExpression());
        }
        if (expression instanceof ParenthesesExpression parentheses) {
            return render(parentheses.getExpression());
        }
        if (expression instanceof DoubleConstantExpression constant) {
            return String.valueOf(constant.getDoubleValue());
        }
        if (expression instanceof BooleanExpression bool) {
            return String.valueOf(bool.isTrue());
        }
        if (expression instanceof IntegerConstantExpression integer) {
            return String.valueOf(integer.getIntegerValue());
        }
        return ((LocalVariableReferenceExpression) expression).getName();
    }

    /** @return the returned expression if the statements are one 'return', or the number of statements */
    private static String rewrite(Statement... statements) {
        Statements list = list(statements);

        new BooleanReturnRewriter().visit(list);

        if (list.size() == 1 && list.getFirst() instanceof ReturnExpressionStatement returned) {
            return render(returned.getExpression());
        }
        return "statements: " + list.size();
    }

    @Test
    public void testATestWhichReturnsItself() {
        assertEquals("a", rewrite(ifThen(variable("a"), returns(true)), returns(false)));
    }

    @Test
    public void testATestWhichReturnsItsNegation() {
        assertEquals("!a", rewrite(ifThen(variable("a"), returns(false)), returns(true)));
    }

    @Test
    public void testTheNegationOfAComparisonIsTheOppositeComparison() {
        Expression comparison = new BinaryOperatorExpression(LINE, PrimitiveType.TYPE_BOOLEAN, new IntegerConstantExpression(LINE, 1), "==", new IntegerConstantExpression(LINE, 2));

        assertEquals("(1 != 2)", rewrite(ifThen(comparison, returns(false)), returns(true)));
    }

    @Test
    public void testTheNegationOfAFloatingPointComparisonKeepsTheNot() {
        Expression comparison = new BinaryOperatorExpression(LINE, PrimitiveType.TYPE_BOOLEAN, new DoubleConstantExpression(LINE, 1.0), "<", new DoubleConstantExpression(LINE, 2.0));

        assertEquals("!(1.0 < 2.0)", rewrite(ifThen(comparison, returns(false)), returns(true)));
    }

    @Test
    public void testNestedTestsAreAConjunction() {
        assertEquals("(a && b)", rewrite(ifThen(variable("a"), list(ifThen(variable("b"), returns(true)))), returns(false)));
    }

    @Test
    public void testATestWithAnElseIsADisjunction() {
        assertEquals("(a || !b)", rewrite(ifThenElse(variable("a"), returns(true), list(ifThenElse(variable("b"), returns(false), returns(true))))));
        assertEquals("(!a || b)", rewrite(ifThenElse(variable("a"), list(ifThenElse(variable("b"), returns(true), returns(false))), returns(true))));
        assertEquals("(!a && b)", rewrite(ifThenElse(variable("a"), returns(false), list(ifThenElse(variable("b"), returns(true), returns(false))))));
    }

    @Test
    public void testTheTestsWhichChooseBetweenTwoOthersAreATernaryOperator() {
        assertEquals("(a ? b : !c)",
            rewrite(ifThenElse(variable("a"), list(ifThenElse(variable("b"), returns(true), returns(false))), list(ifThenElse(variable("c"), returns(false), returns(true))))));
    }

    @Test
    public void testWhatFollowsATestWhichFallsThroughIsFactoredInAConjunction() {
        assertEquals("((a ? !b : !c) && d)",
            rewrite(
                ifThenElse(variable("a"), list(ifThen(variable("b"), returns(false))), list(ifThen(variable("c"), returns(false)))),
                ifThen(variable("d"), returns(true)),
                returns(false)));
    }

    @Test
    public void testWhatFollowsATestWhichFallsThroughIsFactoredInADisjunction() {
        assertEquals("((a ? b : c) || !d)",
            rewrite(
                ifThenElse(variable("a"), list(ifThen(variable("b"), returns(true))), list(ifThen(variable("c"), returns(true)))),
                ifThen(variable("d"), returns(false)),
                returns(true)));
    }

    @Test
    public void testWhatFollowsATestWhichFallsThroughIsNotCopiedInTheBranches() {
        // The returns of 'true' and 'false' of the test cannot be factored: only the end is rewritten
        assertEquals("statements: 2",
            rewrite(
                ifThenElse(variable("a"), list(ifThen(variable("b"), returns(true))), list(ifThen(variable("c"), returns(false)))),
                ifThen(variable("d"), returns(true)),
                returns(false)));
    }

    @Test
    public void testTheReturnsAreOnTheLineOfTheFirstTest() {
        assertEquals("statements: 2", rewrite(ifThen(variable("a"), returns(true, LINE + 1)), returns(false, LINE + 2)));
        assertEquals("statements: 2", rewrite(ifThen(new LocalVariableReferenceExpression(PrimitiveType.TYPE_BOOLEAN, "a"), returns(true, 0)), returns(false, 0)));
    }

    @Test
    public void testOnlyReturnsOfTrueAndFalse() {
        Statement returnsAnInteger = new ReturnExpressionStatement(LINE, new IntegerConstantExpression(LINE, 5));

        assertEquals("statements: 2", rewrite(ifThen(variable("a"), returnsAnInteger), returns(false)));
        assertEquals("statements: 2", rewrite(ifThen(variable("a"), returns(true)), new ExpressionStatement(new NullExpression(ObjectType.TYPE_OBJECT))));
    }

    @Test
    public void testAllThePathsReturn() {
        assertEquals("statements: 1", rewrite(ifThen(variable("a"), returns(true))));
        assertEquals("statements: 2", rewrite(ifThen(variable("a"), returns(true)), ifThen(variable("b"), returns(false))));
    }

    @Test
    public void testTheStatementsOfTheBodiesAreRewritten() {
        Statements body = list(ifThen(variable("a"), returns(true)), returns(false));
        Statements outer = list(ifThen(variable("c"), body), new ExpressionStatement(new NullExpression(ObjectType.TYPE_OBJECT)));

        new BooleanReturnRewriter().visit(outer);

        assertEquals(2, outer.size());
        assertTrue(((IfStatement) outer.getFirst()).getStatements() instanceof Statements);
        assertEquals(1, ((IfStatement) outer.getFirst()).getStatements().size());
    }
}
