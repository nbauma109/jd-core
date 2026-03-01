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
import org.jd.core.v1.model.javasyntax.type.BaseType;
import org.jd.core.v1.model.javasyntax.type.ObjectType;
import org.jd.core.v1.model.javasyntax.type.PrimitiveType;
import org.jd.core.v1.model.javasyntax.type.Type;
import org.jd.core.v1.model.javasyntax.type.Types;
import org.jd.core.v1.service.converter.classfiletojavasyntax.model.javasyntax.expression.ClassFileMethodInvocationExpression;
import org.jd.core.v1.service.converter.classfiletojavasyntax.util.TypeMaker;

import java.util.ListIterator;

public class RemoveRedundantGenericHelperParameterCastVisitor extends AbstractUpdateExpressionVisitor {
    private final TypeMaker typeMaker;

    public RemoveRedundantGenericHelperParameterCastVisitor(TypeMaker typeMaker) {
        this.typeMaker = typeMaker;
    }

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
        BaseType unboundParameterTypes = invocation.getUnboundParameterTypes();

        if (parameters.isList()) {
            ListIterator<Expression> iterator = parameters.getList().listIterator();
            int index = 0;
            while (iterator.hasNext()) {
                Type parameterType = resolveParameterType(parameterTypes, index);
                Type unboundParameterType = resolveParameterType(unboundParameterTypes, index);
                Expression parameter = iterator.next();
                iterator.set(removeRedundantCast(invocation, parameters, index, parameterType, unboundParameterType, parameter));
                index++;
            }
        } else {
            expression.setParameters(removeRedundantCast(invocation,
                    parameters,
                    0,
                    parameterTypes.getFirst(),
                    unboundParameterTypes == null ? null : unboundParameterTypes.getFirst(),
                    parameters.getFirst()));
        }
    }

    @Override
    protected Expression updateExpression(Expression expression) {
        return expression;
    }

    private Expression removeRedundantCast(
            ClassFileMethodInvocationExpression invocation,
            BaseExpression parameters,
            int index,
            Type parameterType,
            Type unboundParameterType,
            Expression parameter) {
        if (!(parameter instanceof CastExpression castExpression)
                || castExpression.getExpression() instanceof LambdaIdentifiersExpression
                || castExpression.getExpression() instanceof MethodReferenceExpression) {
            return parameter;
        }

        boolean genericHelperCast = isGenericHelperParameter(invocation, parameterType, unboundParameterType)
                && matchesWithoutCast(invocation, parameters, index, castExpression);
        boolean descriptorPinnedCast = isPinnedByOtherDescriptorParameter(invocation, parameters, index, castExpression);
        if (!genericHelperCast && !descriptorPinnedCast) {
            return parameter;
        }

        Expression innerExpression = castExpression.getExpression();
        Type castType = castExpression.getType();
        Type innerType = innerExpression.getType();

        if (castType == null || innerType == null) {
            return parameter;
        }

        if (innerType.getDimension() > 0) {
            if (isErasedObjectCast(castType)) {
                return innerExpression;
            }
            if (isErasedObjectArrayCast(castType) && innerType.getDimension() == castType.getDimension()) {
                return innerExpression;
            }
            if (castType.isObjectType()
                    && innerType.isObjectType()
                    && castType.getDimension() == innerType.getDimension()
                    && typeMaker.isRawTypeAssignable((ObjectType) castType, (ObjectType) innerType)) {
                return innerExpression;
            }
            return parameter;
        }

        if (!(castType instanceof ObjectType castObjectType)
                || !(innerType instanceof ObjectType innerObjectType)
                || !typeMaker.isRawTypeAssignable(castObjectType, innerObjectType)) {
            return parameter;
        }

        if (castObjectType.rawEquals(innerObjectType)) {
            return innerExpression;
        }

        return innerExpression;
    }

    private static Type resolveParameterType(BaseType parameterTypes, int index) {
        if (parameterTypes == null) {
            return null;
        }
        if (parameterTypes.isList()) {
            return index < parameterTypes.size() ? parameterTypes.getList().get(index) : null;
        }
        return index == 0 ? parameterTypes.getFirst() : null;
    }

    private static boolean isGenericHelperParameter(
            ClassFileMethodInvocationExpression invocation,
            Type parameterType,
            Type unboundParameterType) {
        return hasTypeParameters(parameterType)
                || hasTypeParameters(unboundParameterType)
                || unboundParameterType instanceof org.jd.core.v1.model.javasyntax.type.GenericType
                || hasTypeParameters(invocation.getUnboundType())
                || invocation.getTypeParameters() != null;
    }

    private boolean matchesWithoutCast(
            ClassFileMethodInvocationExpression invocation,
            BaseExpression parameters,
            int index,
            CastExpression castExpression) {
        if (parameters == null
                || invocation.getTypeBindings() == null
                || invocation.getTypeBounds() == null) {
            return true;
        }

        BaseExpression candidateParameters = parameters;
        Expression originalParameter = parameters.isList() ? parameters.getList().get(index) : parameters.getFirst();
        if (parameters.isList()) {
            parameters.getList().set(index, castExpression.getExpression());
        } else {
            candidateParameters = castExpression.getExpression();
        }

        int matches = typeMaker.matchCount(
                invocation.getTypeBindings(),
                invocation.getTypeBounds(),
                invocation.getInternalTypeName(),
                invocation.getName(),
                candidateParameters,
                false);

        if (parameters.isList()) {
            parameters.getList().set(index, originalParameter);
        }

        return matches == 1;
    }

    private boolean isPinnedByOtherDescriptorParameter(
            ClassFileMethodInvocationExpression invocation,
            BaseExpression parameters,
            int castIndex,
            CastExpression castExpression) {
        if (parameters == null
                || parameters.size() <= 1
                || typeMaker.matchCount(invocation.getInternalTypeName(), invocation.getName(), parameters.size(), false) <= 1) {
            return false;
        }

        BaseType descriptorParameterTypes = parseRawDescriptorParameterTypes(invocation.getDescriptor());
        if (descriptorParameterTypes == null
                || descriptorParameterTypes.size() != parameters.size()) {
            return false;
        }

        Type castType = castExpression.getType();
        Type descriptorParameterType = resolveParameterType(descriptorParameterTypes, castIndex);
        if (!sameDescriptorType(descriptorParameterType, castType)) {
            return false;
        }

        if (!(castType instanceof ObjectType castObjectType)
                || !(castExpression.getExpression().getType() instanceof ObjectType expressionObjectType)
                || castObjectType.rawEquals(expressionObjectType)
                || !typeMaker.isRawTypeAssignable(castObjectType, expressionObjectType)) {
            return false;
        }

        if (!parameters.isList()) {
            return false;
        }

        for (int i = 0; i < parameters.size(); i++) {
            if (i == castIndex) {
                continue;
            }

            Type actualType = parameters.getList().get(i).getType();
            Type descriptorType = resolveParameterType(descriptorParameterTypes, i);
            if (sameDescriptorType(descriptorType, actualType)) {
                return true;
            }
        }

        return false;
    }

    private BaseType parseRawDescriptorParameterTypes(String descriptor) {
        if (descriptor == null || descriptor.isEmpty() || descriptor.charAt(0) != '(') {
            return null;
        }

        Types parameterTypes = new Types();
        int index = 1;
        while (descriptor.charAt(index) != ')') {
            int nextIndex = skipDescriptorType(descriptor, index);
            parameterTypes.add(makeRawDescriptorType(descriptor.substring(index, nextIndex)));
            index = nextIndex;
        }
        return parameterTypes;
    }

    private static int skipDescriptorType(String descriptor, int index) {
        while (descriptor.charAt(index) == '[') {
            index++;
        }
        if (descriptor.charAt(index) == 'L') {
            return descriptor.indexOf(';', index) + 1;
        }
        return index + 1;
    }

    private Type makeRawDescriptorType(String descriptor) {
        int dimension = 0;
        while (descriptor.charAt(dimension) == '[') {
            dimension++;
        }

        Type elementType = switch (descriptor.substring(dimension)) {
            case "B" -> PrimitiveType.TYPE_BYTE;
            case "C" -> PrimitiveType.TYPE_CHAR;
            case "D" -> PrimitiveType.TYPE_DOUBLE;
            case "F" -> PrimitiveType.TYPE_FLOAT;
            case "I" -> PrimitiveType.TYPE_INT;
            case "J" -> PrimitiveType.TYPE_LONG;
            case "S" -> PrimitiveType.TYPE_SHORT;
            case "Z" -> PrimitiveType.TYPE_BOOLEAN;
            default -> typeMaker.makeFromInternalTypeName(descriptor.substring(dimension + 1, descriptor.length() - 1));
        };

        return dimension == 0 ? elementType : elementType.createType(dimension);
    }

    private static boolean sameDescriptorType(Type descriptorType, Type actualType) {
        if (descriptorType == null || actualType == null) {
            return false;
        }
        if (descriptorType.isObjectType() && actualType.isObjectType()) {
            return ((ObjectType) descriptorType).rawEquals((ObjectType) actualType);
        }
        return descriptorType.equals(actualType);
    }

    private static boolean hasTypeParameters(Type type) {
        return type != null && !type.findTypeParametersInType().isEmpty();
    }

    private static boolean isErasedObjectCast(Type type) {
        return type instanceof ObjectType objectType
                && type.getDimension() == 0
                && "java/lang/Object".equals(objectType.getInternalName());
    }

    private static boolean isErasedObjectArrayCast(Type type) {
        return type instanceof ObjectType objectType
                && type.getDimension() > 0
                && "java/lang/Object".equals(objectType.getInternalName());
    }
}
