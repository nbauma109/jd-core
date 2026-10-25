/*
 * Copyright (c) 2026 Nicolas Baumann (@nbauma109), with Claude Sonnet 5.5.
 * This project is distributed under the GPLv3 license.
 * This is a Copyleft license that gives the user the right to use,
 * copy and modify the code freely for non-commercial purposes.
 */
package org.jd.core.v1.service.converter.classfiletojavasyntax.visitor;

import org.jd.core.v1.model.javasyntax.expression.Expression;
import org.jd.core.v1.model.javasyntax.expression.FloatConstantExpression;

/**
 * 'double x = 0.2F' is folded by the compiler into the double constant 0.20000000298023224: print it as the float literal again.
 * This runs once the byte code is parsed: a float constant would not occupy two slots of the operand stack ('a = b = 0.2F').
 */
public class WidenedFloatConstantVisitor extends AbstractUpdateExpressionVisitor {

    @Override
    protected Expression updateExpression(Expression expression) {
        if (expression.isDoubleConstantExpression() && isWidenedFloatWithShorterLiteral(expression.getDoubleValue())) {
            return new FloatConstantExpression(expression.getLineNumber(), (float) expression.getDoubleValue());
        }
        return expression;
    }

    static boolean isWidenedFloatWithShorterLiteral(double d) {
        float f = (float) d;
        return Double.compare(f, d) == 0 && d != Math.rint(d) && Float.toString(f).length() < Double.toString(d).length();
    }
}
