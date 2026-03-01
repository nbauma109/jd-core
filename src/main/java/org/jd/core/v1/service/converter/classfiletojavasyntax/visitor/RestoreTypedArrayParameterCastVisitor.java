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
import org.jd.core.v1.model.javasyntax.expression.MethodInvocationExpression;
import org.jd.core.v1.model.javasyntax.type.BaseType;
import org.jd.core.v1.model.javasyntax.type.ObjectType;
import org.jd.core.v1.model.javasyntax.type.Type;
import org.jd.core.v1.service.converter.classfiletojavasyntax.model.javasyntax.expression.ClassFileMethodInvocationExpression;

import java.util.ListIterator;

public class RestoreTypedArrayParameterCastVisitor extends AbstractUpdateExpressionVisitor {
    @Override
    protected void maybeUpdateParameters(MethodInvocationExpression expression) {
        if (!(expression instanceof ClassFileMethodInvocationExpression invocation)
                || invocation.getParameterTypes() == null
                || expression.getParameters() == null) {
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
                iterator.set(updateTypedArrayParameterCast(parameterType, iterator.next()));
                index++;
            }
        } else {
            expression.setParameters(updateTypedArrayParameterCast(parameterTypes.getFirst(), parameters.getFirst()));
        }
    }

    @Override
    protected Expression updateExpression(Expression expression) {
        return expression;
    }

    private Expression updateTypedArrayParameterCast(Type parameterType, Expression parameter) {
        if (!(parameterType instanceof ObjectType parameterObjectType)
                || parameterObjectType.getDimension() == 0
                || isRawObjectArray(parameterObjectType)
                || parameter instanceof CastExpression
                || !(parameter instanceof ClassFileMethodInvocationExpression methodInvocationExpression)
                || !"toArray".equals(methodInvocationExpression.getName())
                || !(parameter.getType() instanceof ObjectType parameterExpressionType)
                || parameterExpressionType.getDimension() != parameterObjectType.getDimension()
                || !requiresTypedArrayResultCastForRawReceiver(parameterObjectType, methodInvocationExpression)) {
            return parameter;
        }

        return new CastExpression(parameterObjectType, parameter);
    }

    private static boolean requiresTypedArrayResultCastForRawReceiver(ObjectType targetType, ClassFileMethodInvocationExpression methodInvocationExpression) {
        if (!(methodInvocationExpression.getExpression() instanceof CastExpression castExpression)
                || !(castExpression.getType() instanceof ObjectType receiverCastType)
                || receiverCastType.getTypeArguments() != null
                || methodInvocationExpression.getParameterTypes() == null
                || methodInvocationExpression.getUnboundParameterTypes() == null) {
            return false;
        }

        Type declaredParameterType = methodInvocationExpression.getParameterTypes().isList()
                ? methodInvocationExpression.getParameterTypes().getList().isEmpty() ? null : methodInvocationExpression.getParameterTypes().getList().getFirst()
                : methodInvocationExpression.getParameterTypes().getFirst();
        Type unboundParameterType = methodInvocationExpression.getUnboundParameterTypes().isList()
                ? methodInvocationExpression.getUnboundParameterTypes().getList().isEmpty() ? null : methodInvocationExpression.getUnboundParameterTypes().getList().getFirst()
                : methodInvocationExpression.getUnboundParameterTypes().getFirst();

        return isRawObjectArray(declaredParameterType)
                && unboundParameterType instanceof org.jd.core.v1.model.javasyntax.type.GenericType genericType
                && genericType.getDimension() == targetType.getDimension();
    }

    private static boolean isRawObjectArray(Type type) {
        return type instanceof ObjectType objectType
                && type.getDimension() > 0
                && "java/lang/Object".equals(objectType.getInternalName());
    }
}
