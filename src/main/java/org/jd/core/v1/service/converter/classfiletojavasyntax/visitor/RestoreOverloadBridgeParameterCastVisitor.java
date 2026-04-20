/*
 * Copyright (c) 2008, 2019 Emmanuel Dupuy.
 * This project is distributed under the GPLv3 license.
 * This is a Copyleft license that gives the user the right to use,
 * copy and modify the code freely for non-commercial purposes.
 */
package org.jd.core.v1.service.converter.classfiletojavasyntax.visitor;

import org.jd.core.v1.model.javasyntax.expression.BaseExpression;
import org.jd.core.v1.model.javasyntax.expression.CastExpression;
import org.jd.core.v1.model.javasyntax.expression.Expression;
import org.jd.core.v1.model.javasyntax.expression.LambdaIdentifiersExpression;
import org.jd.core.v1.model.javasyntax.expression.MethodInvocationExpression;
import org.jd.core.v1.model.javasyntax.expression.MethodReferenceExpression;
import org.jd.core.v1.model.javasyntax.expression.QualifiedSuperExpression;
import org.jd.core.v1.model.javasyntax.expression.SuperExpression;
import org.jd.core.v1.model.javasyntax.type.BaseType;
import org.jd.core.v1.model.javasyntax.type.ObjectType;
import org.jd.core.v1.model.javasyntax.type.Type;
import org.jd.core.v1.service.converter.classfiletojavasyntax.model.javasyntax.expression.ClassFileMethodInvocationExpression;
import org.jd.core.v1.service.converter.classfiletojavasyntax.util.TypeMaker;

import java.util.ListIterator;

public class RestoreOverloadBridgeParameterCastVisitor extends AbstractUpdateExpressionVisitor {
    private final TypeMaker typeMaker;

    public RestoreOverloadBridgeParameterCastVisitor(TypeMaker typeMaker) {
        this.typeMaker = typeMaker;
    }

    @Override
    protected void maybeUpdateParameters(MethodInvocationExpression expression) {
        if (!(expression instanceof ClassFileMethodInvocationExpression invocation)
                || invocation.getParameterTypes() == null
                || expression.getParameters() == null
                || expression.getExpression() instanceof SuperExpression
                || expression.getExpression() instanceof QualifiedSuperExpression
                || typeMaker.matchCount(expression.getInternalTypeName(), expression.getName(), expression.getParameters().size(), false) <= 1) {
            super.maybeUpdateParameters(expression);
            return;
        }

        // Check if parameters already uniquely select the correct overload
        int typedMatches = typeMaker.matchCount(
                java.util.Collections.emptyMap(),
                java.util.Collections.emptyMap(),
                expression.getInternalTypeName(),
                expression.getName(),
                expression.getParameters(),
                false);
        if (typedMatches <= 1) {
            super.maybeUpdateParameters(expression);
            return;
        }

        BaseExpression parameters = expression.getParameters();
        BaseType parameterTypes = invocation.getParameterTypes();

        if (parameters.isList()) {
            ListIterator<Expression> iterator = parameters.getList().listIterator();
            int index = 0;
            while (iterator.hasNext()) {
                Type parameterType = parameterTypes.isList()
                        ? index < parameterTypes.size() ? parameterTypes.getList().get(index) : null
                        : index == 0 ? parameterTypes.getFirst() : null;
                iterator.set(updateBridgeParameterCast(parameterType, iterator.next()));
                index++;
            }
        } else {
            expression.setParameters(updateBridgeParameterCast(parameterTypes.getFirst(), parameters.getFirst()));
        }
    }

    @Override
    protected Expression updateExpression(Expression expression) {
        return expression;
    }

    private Expression updateBridgeParameterCast(Type parameterType, Expression parameter) {
        if (!(parameterType instanceof ObjectType parameterObjectType)
                || parameter instanceof CastExpression
                || parameter instanceof LambdaIdentifiersExpression
                || parameter instanceof MethodReferenceExpression
                || !(parameter.getType() instanceof ObjectType expressionObjectType)
                || parameterObjectType.rawEquals(expressionObjectType)
                || !typeMaker.isRawTypeAssignable(parameterObjectType, expressionObjectType)) {
            return parameter;
        }

        return new CastExpression(parameterObjectType, parameter);
    }
}
