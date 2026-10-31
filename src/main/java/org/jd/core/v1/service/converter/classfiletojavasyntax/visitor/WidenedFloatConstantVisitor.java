/*
 * Copyright (c) 2026 Nicolas Baumann (@nbauma109), with Claude Sonnet 5.5.
 * This project is distributed under the GPLv3 license.
 * This is a Copyleft license that gives the user the right to use,
 * copy and modify the code freely for non-commercial purposes.
 */
package org.jd.core.v1.service.converter.classfiletojavasyntax.visitor;

import org.jd.core.v1.model.javasyntax.declaration.ExpressionVariableInitializer;
import org.jd.core.v1.model.javasyntax.declaration.FieldDeclaration;
import org.jd.core.v1.model.javasyntax.declaration.LocalVariableDeclaration;
import org.jd.core.v1.model.javasyntax.declaration.MethodDeclaration;
import org.jd.core.v1.model.javasyntax.expression.BinaryOperatorExpression;
import org.jd.core.v1.model.javasyntax.expression.Expression;
import org.jd.core.v1.model.javasyntax.expression.FloatConstantExpression;
import org.jd.core.v1.model.javasyntax.expression.LambdaIdentifiersExpression;
import org.jd.core.v1.model.javasyntax.statement.ReturnExpressionStatement;
import org.jd.core.v1.model.javasyntax.type.PrimitiveType;
import org.jd.core.v1.model.javasyntax.type.Type;

/**
 * 'double x = 0.2F' is folded by the compiler into the double constant 0.20000000298023224: print it as the float literal again.
 * <ul>
 * <li>This runs once the byte code is parsed: a float constant would not occupy two slots of the operand stack ('a = b = 0.2F').</li>
 * <li>Only where the value is converted back to a double (initializer, assignment, return of a double method): in a method call or a
 * boxing conversion, a float literal would select another overload or box a Float.</li>
 * </ul>
 */
public class WidenedFloatConstantVisitor extends AbstractUpdateExpressionVisitor {

    private Type declaredType;
    private Type returnedType;
    private int lambdaDepth;

    @Override
    protected Expression updateExpression(Expression expression) {
        return expression;
    }

    @Override
    public void visit(FieldDeclaration declaration) {
        Type previous = declaredType;
        declaredType = declaration.getType();
        super.visit(declaration);
        declaredType = previous;
    }

    @Override
    public void visit(LocalVariableDeclaration declaration) {
        Type previous = declaredType;
        declaredType = declaration.getType();
        super.visit(declaration);
        declaredType = previous;
    }

    @Override
    public void visit(ExpressionVariableInitializer declaration) {
        if (PrimitiveType.TYPE_DOUBLE.equals(declaredType) && declaration.getExpression() != null) {
            declaration.setExpression(shorten(declaration.getExpression()));
        }
        Type previous = declaredType;
        declaredType = null;
        super.visit(declaration);
        declaredType = previous;
    }

    @Override
    public void visit(BinaryOperatorExpression expression) {
        if ("=".equals(expression.getOperator()) && PrimitiveType.TYPE_DOUBLE.equals(expression.getLeftExpression().getType())) {
            expression.setRightExpression(shorten(expression.getRightExpression()));
        }
        super.visit(expression);
    }

    @Override
    public void visit(MethodDeclaration declaration) {
        Type previousReturnedType = returnedType;
        int previousLambdaDepth = lambdaDepth;
        returnedType = declaration.getReturnedType();
        lambdaDepth = 0;
        super.visit(declaration);
        returnedType = previousReturnedType;
        lambdaDepth = previousLambdaDepth;
    }

    @Override
    public void visit(LambdaIdentifiersExpression expression) {
        lambdaDepth++;
        super.visit(expression);
        lambdaDepth--;
    }

    @Override
    public void visit(ReturnExpressionStatement statement) {
        if (lambdaDepth == 0 && PrimitiveType.TYPE_DOUBLE.equals(returnedType)) {
            statement.setExpression(shorten(statement.getExpression()));
        }
        super.visit(statement);
    }

    private static Expression shorten(Expression expression) {
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
