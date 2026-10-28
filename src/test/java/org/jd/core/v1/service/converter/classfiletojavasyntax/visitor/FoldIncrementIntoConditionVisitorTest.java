/*
 * Copyright (c) 2026 Nicolas Baumann (@nbauma109), with Claude Sonnet 5.5.
 * This project is distributed under the GPLv3 license.
 * This is a Copyleft license that gives the user the right to use,
 * copy and modify the code freely for non-commercial purposes.
 */

package org.jd.core.v1.service.converter.classfiletojavasyntax.visitor;

import org.jd.core.v1.model.javasyntax.expression.BinaryOperatorExpression;
import org.jd.core.v1.model.javasyntax.expression.Expression;
import org.jd.core.v1.model.javasyntax.expression.IntegerConstantExpression;
import org.jd.core.v1.model.javasyntax.expression.LengthExpression;
import org.jd.core.v1.model.javasyntax.expression.PostOperatorExpression;
import org.jd.core.v1.model.javasyntax.expression.PreOperatorExpression;
import org.jd.core.v1.model.javasyntax.expression.TernaryOperatorExpression;
import org.jd.core.v1.model.javasyntax.type.PrimitiveType;
import org.jd.core.v1.service.converter.classfiletojavasyntax.model.javasyntax.expression.ClassFileLocalVariableReferenceExpression;
import org.jd.core.v1.service.converter.classfiletojavasyntax.model.localvariable.AbstractLocalVariable;
import org.jd.core.v1.service.converter.classfiletojavasyntax.model.localvariable.PrimitiveLocalVariable;
import org.jd.core.v1.model.javasyntax.expression.FieldReferenceExpression;
import org.jd.core.v1.model.javasyntax.expression.ObjectTypeReferenceExpression;
import org.jd.core.v1.model.javasyntax.type.ObjectType;
import org.junit.Test;

import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;

/** 'i++;' which precedes a test in the byte code is evaluated by the first use of 'i' of the test */
public class FoldIncrementIntoConditionVisitorTest {
    private static final AbstractLocalVariable I = new PrimitiveLocalVariable(1, 0, PrimitiveType.TYPE_INT, "i");
    private static final AbstractLocalVariable J = new PrimitiveLocalVariable(2, 0, PrimitiveType.TYPE_INT, "j");

    private static Expression variable(AbstractLocalVariable localVariable) {
        return new ClassFileLocalVariableReferenceExpression(1, 0, localVariable);
    }

    private static Expression binary(Expression left, String operator, Expression right) {
        return new BinaryOperatorExpression(1, PrimitiveType.TYPE_BOOLEAN, left, operator, right, 9);
    }

    private static Expression two() {
        return new IntegerConstantExpression(PrimitiveType.TYPE_INT, 2);
    }

    @Test
    public void testFirstUseInTheOrderOfTheEvaluation() {
        Expression left = variable(I);
        Expression right = variable(I);
        BinaryOperatorExpression operand = (BinaryOperatorExpression) binary(binary(left, "*", two()), "<=", right);

        assertSame(operand, FoldIncrementIntoConditionVisitor.fold(operand, I, "++"));
        assertTrue(((BinaryOperatorExpression) operand.getLeftExpression()).getLeftExpression() instanceof PreOperatorExpression);
        assertSame(right, operand.getRightExpression());
    }

    @Test
    public void testOperandWhichIsTheVariable() {
        Expression operand = variable(I);

        assertTrue(FoldIncrementIntoConditionVisitor.fold(operand, I, "--") instanceof PreOperatorExpression);
    }

    @Test
    public void testOperandWhichDoesNotUseTheVariable() {
        assertNull(FoldIncrementIntoConditionVisitor.fold(binary(variable(J), "<", two()), I, "++"));
    }

    @Test
    public void testIncrementOfTheVariableIsNotFolded() {
        assertNull(FoldIncrementIntoConditionVisitor.fold(binary(new PostOperatorExpression(1, variable(I), "++"), ">", two()), I, "++"));
        assertNull(FoldIncrementIntoConditionVisitor.fold(binary(new PreOperatorExpression(1, "--", variable(I)), ">", two()), I, "++"));
    }

    @Test
    public void testAssignmentToTheVariableIsNotFolded() {
        assertNull(FoldIncrementIntoConditionVisitor.fold(binary(binary(variable(I), "=", two()), ">", two()), I, "++"));
        assertNull(FoldIncrementIntoConditionVisitor.fold(binary(binary(variable(I), "+=", two()), ">", two()), I, "++"));
    }

    @Test
    public void testComparisonIsNotAnAssignment() {
        for (String comparison : new String[] {"==", "!=", "<=", ">="}) {
            Expression operand = binary(variable(I), comparison, two());

            assertSame(operand, FoldIncrementIntoConditionVisitor.fold(operand, I, "++"));
        }
    }

    @Test
    public void testUseWhichIsEvaluatedSometimesIsNotFolded() {
        Expression flag = variable(J);

        assertNull(FoldIncrementIntoConditionVisitor.fold(binary(flag, "||", binary(variable(I), ">", two())), I, "++"));
        assertNull(FoldIncrementIntoConditionVisitor.fold(binary(flag, "&&", binary(variable(I), ">", two())), I, "++"));
        assertNull(FoldIncrementIntoConditionVisitor.fold(new TernaryOperatorExpression(1, PrimitiveType.TYPE_BOOLEAN, flag, binary(variable(I), ">", two()), flag), I, "++"));
    }

    @Test
    public void testUseWhichIsAlwaysEvaluatedIsFolded() {
        Expression left = binary(variable(I), ">", two());
        Expression operand = binary(left, "||", binary(variable(J), ">", two()));

        assertSame(operand, FoldIncrementIntoConditionVisitor.fold(operand, I, "++"));
        Expression ternary = new TernaryOperatorExpression(1, PrimitiveType.TYPE_BOOLEAN, binary(variable(I), ">", two()), variable(J), variable(J));

        assertSame(ternary, FoldIncrementIntoConditionVisitor.fold(ternary, I, "++"));
    }

    @Test
    public void testReadOfAStaticFieldBeforeTheFirstUseIsNotFolded() {
        ObjectType owner = new ObjectType("p/Broken", "p.Broken", "Broken");
        Expression staticField = new FieldReferenceExpression(PrimitiveType.TYPE_INT, new ObjectTypeReferenceExpression(owner), "p/Broken", "n", "I");

        // The class of the field is initialized by the read, which can throw
        assertNull(FoldIncrementIntoConditionVisitor.fold(binary(staticField, ">", variable(I)), I, "++"));
    }

    @Test
    public void testReadOfTheLengthOfAnArrayBeforeTheFirstUseIsNotFolded() {
        Expression length = new LengthExpression(variable(J));

        // The array can be null
        assertNull(FoldIncrementIntoConditionVisitor.fold(binary(length, ">", variable(I)), I, "++"));
    }
}
