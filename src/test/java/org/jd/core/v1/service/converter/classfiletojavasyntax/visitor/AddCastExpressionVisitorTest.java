/*
 * Copyright (c) 2008, 2019 Emmanuel Dupuy.
 * This project is distributed under the GPLv3 license.
 * This is a Copyleft license that gives the user the right to use,
 * copy and modify the code freely for non-commercial purposes.
 */
package org.jd.core.v1.service.converter.classfiletojavasyntax.visitor;

import junit.framework.TestCase;
import org.jd.core.v1.loader.ClassPathLoader;
import org.jd.core.v1.model.javasyntax.expression.BooleanExpression;
import org.jd.core.v1.model.javasyntax.expression.IntegerConstantExpression;
import org.jd.core.v1.model.javasyntax.expression.TernaryOperatorExpression;
import org.jd.core.v1.model.javasyntax.type.PrimitiveType;
import org.jd.core.v1.service.converter.classfiletojavasyntax.util.TypeMaker;
import org.junit.Test;

public class AddCastExpressionVisitorTest extends TestCase {

    private final AddCastExpressionVisitor visitor =
            new AddCastExpressionVisitor(new TypeMaker(new ClassPathLoader()));

    @Test
    public void testBooleanTernaryConvertsIntegerFalseArm() {
        TernaryOperatorExpression expression = new TernaryOperatorExpression(
                1, PrimitiveType.TYPE_BOOLEAN, BooleanExpression.TRUE, BooleanExpression.TRUE,
                new IntegerConstantExpression(1, PrimitiveType.TYPE_BOOLEAN, 0));

        expression.accept(visitor);

        assertTrue(expression.getTrueExpression() instanceof BooleanExpression);
        assertTrue(expression.getFalseExpression() instanceof BooleanExpression);
        assertFalse(((BooleanExpression) expression.getFalseExpression()).isTrue());
    }

    @Test
    public void testBooleanTernaryConvertsIntegerTrueArm() {
        TernaryOperatorExpression expression = new TernaryOperatorExpression(
                1, PrimitiveType.TYPE_BOOLEAN, BooleanExpression.TRUE,
                new IntegerConstantExpression(1, PrimitiveType.TYPE_BOOLEAN, 1), new BooleanExpression(1, false));

        expression.accept(visitor);

        assertTrue(expression.getTrueExpression() instanceof BooleanExpression);
        assertTrue(((BooleanExpression) expression.getTrueExpression()).isTrue());
        assertTrue(expression.getFalseExpression() instanceof BooleanExpression);
    }

    @Test
    public void testNumericTernaryConvertsBooleanTrueArm() {
        TernaryOperatorExpression expression = new TernaryOperatorExpression(
                1, PrimitiveType.TYPE_INT, BooleanExpression.TRUE, BooleanExpression.TRUE,
                new IntegerConstantExpression(1, PrimitiveType.TYPE_INT, 0));

        expression.accept(visitor);

        assertTrue(expression.getTrueExpression() instanceof IntegerConstantExpression);
        assertEquals(1, ((IntegerConstantExpression) expression.getTrueExpression()).getIntegerValue());
        assertTrue(expression.getFalseExpression() instanceof IntegerConstantExpression);
    }

    @Test
    public void testNumericTernaryConvertsBooleanFalseArm() {
        TernaryOperatorExpression expression = new TernaryOperatorExpression(
                1, PrimitiveType.TYPE_INT, BooleanExpression.TRUE,
                new IntegerConstantExpression(1, PrimitiveType.TYPE_INT, 1), new BooleanExpression(1, false));

        expression.accept(visitor);

        assertTrue(expression.getTrueExpression() instanceof IntegerConstantExpression);
        assertTrue(expression.getFalseExpression() instanceof IntegerConstantExpression);
        assertEquals(0, ((IntegerConstantExpression) expression.getFalseExpression()).getIntegerValue());
    }

    @Test
    public void testNumericTernaryNormalizesBooleanFlaggedIntegerFalseArm() {
        IntegerConstantExpression falseExpression =
                new IntegerConstantExpression(1, PrimitiveType.TYPE_BOOLEAN, 0);
        TernaryOperatorExpression expression = new TernaryOperatorExpression(
                1, PrimitiveType.TYPE_INT, BooleanExpression.TRUE,
                new IntegerConstantExpression(1, PrimitiveType.TYPE_INT, 1), falseExpression);

        expression.accept(visitor);

        assertSame(PrimitiveType.TYPE_INT, falseExpression.getType());
    }
}
