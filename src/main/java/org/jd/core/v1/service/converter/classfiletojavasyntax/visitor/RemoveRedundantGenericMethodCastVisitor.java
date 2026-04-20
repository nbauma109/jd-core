/*
 * Copyright (c) 2008, 2019 Emmanuel Dupuy.
 * This project is distributed under the GPLv3 license.
 * This is a Copyleft license that gives the user the right to use,
 * copy and modify the code freely for non-commercial purposes.
 */

package org.jd.core.v1.service.converter.classfiletojavasyntax.visitor;

import org.jd.core.v1.model.javasyntax.expression.BaseExpression;
import org.jd.core.v1.model.javasyntax.expression.CastExpression;
import org.jd.core.v1.model.javasyntax.expression.ConstructorInvocationExpression;
import org.jd.core.v1.model.javasyntax.expression.Expression;
import org.jd.core.v1.model.javasyntax.expression.LambdaIdentifiersExpression;
import org.jd.core.v1.model.javasyntax.expression.LocalVariableReferenceExpression;
import org.jd.core.v1.model.javasyntax.expression.MethodInvocationExpression;
import org.jd.core.v1.model.javasyntax.expression.MethodReferenceExpression;
import org.jd.core.v1.model.javasyntax.expression.NewExpression;
import org.jd.core.v1.model.javasyntax.expression.SuperConstructorInvocationExpression;
import org.jd.core.v1.model.javasyntax.expression.ThisExpression;
import org.jd.core.v1.model.javasyntax.type.BaseType;
import org.jd.core.v1.model.javasyntax.type.BaseTypeArgument;
import org.jd.core.v1.model.javasyntax.type.GenericType;
import org.jd.core.v1.model.javasyntax.type.ObjectType;
import org.jd.core.v1.model.javasyntax.type.Type;
import org.jd.core.v1.model.javasyntax.type.TypeArgument;
import org.jd.core.v1.model.javasyntax.type.WildcardExtendsTypeArgument;
import org.jd.core.v1.model.javasyntax.type.WildcardSuperTypeArgument;
import org.jd.core.v1.service.converter.classfiletojavasyntax.model.javasyntax.expression.ClassFileConstructorInvocationExpression;
import org.jd.core.v1.service.converter.classfiletojavasyntax.model.javasyntax.expression.ClassFileLocalVariableReferenceExpression;
import org.jd.core.v1.service.converter.classfiletojavasyntax.model.javasyntax.expression.ClassFileMethodInvocationExpression;
import org.jd.core.v1.service.converter.classfiletojavasyntax.model.javasyntax.expression.ClassFileNewExpression;
import org.jd.core.v1.service.converter.classfiletojavasyntax.model.javasyntax.expression.ClassFileSuperConstructorInvocationExpression;
import org.jd.core.v1.service.converter.classfiletojavasyntax.model.localvariable.AbstractLocalVariable;
import org.jd.core.v1.service.converter.classfiletojavasyntax.util.TypeMaker;
import org.jd.core.v1.util.StringConstants;

import java.util.Collections;
import java.util.ListIterator;

public class RemoveRedundantGenericMethodCastVisitor extends AbstractUpdateExpressionVisitor {
    private final TypeMaker typeMaker;

    public RemoveRedundantGenericMethodCastVisitor(TypeMaker typeMaker) {
        this.typeMaker = typeMaker;
    }

    @Override
    protected Expression updateExpression(Expression expression) {
        if (expression instanceof MethodInvocationExpression methodInvocationExpression
                && "java/util/Objects".equals(methodInvocationExpression.getInternalTypeName())
                && "requireNonNull".equals(methodInvocationExpression.getName())) {
            methodInvocationExpression.setNonWildcardTypeArguments(null);
        }
        if (expression instanceof CastExpression castExpression
                && castExpression.getType() instanceof GenericType
                && castExpression.getExpression() != null
                && castExpression.getExpression().getType() != null
                && castExpression.getExpression().getType().isPrimitiveType()) {
            return castExpression.getExpression();
        }
        if (expression instanceof CastExpression castExpression && shouldRemoveStandaloneGenericMethodCast(castExpression)) {
            return castExpression.getExpression();
        }
        return expression;
    }

    @Override
    protected void maybeUpdateParameters(MethodInvocationExpression expression) {
        if (!(expression instanceof ClassFileMethodInvocationExpression methodInvocationExpression)) {
            super.maybeUpdateParameters(expression);
            return;
        }

        expression.setParameters(updateParameters(
                expression,
                methodInvocationExpression.getParameterTypes(),
                methodInvocationExpression.getUnboundParameterTypes(),
                methodInvocationExpression.getTypeBounds(),
                updateBaseExpression(expression.getParameters())));
    }

    @Override
    public void visit(NewExpression expression) {
        if (expression.getParameters() != null) {
            if (expression instanceof ClassFileNewExpression classFileNewExpression) {
                expression.setParameters(updateConstructorParameters(
                        classFileNewExpression.getObjectType(),
                        classFileNewExpression.getParameterTypes(),
                        updateBaseExpression(expression.getParameters())));
            } else {
                expression.setParameters(updateBaseExpression(expression.getParameters()));
            }
            expression.getParameters().accept(this);
        }
    }

    @Override
    protected void maybeUpdateParameters(ConstructorInvocationExpression expression) {
        if (expression instanceof ClassFileConstructorInvocationExpression classFileExpression) {
            expression.setParameters(updateConstructorParameters(
                    classFileExpression.getObjectType(),
                    classFileExpression.getParameterTypes(),
                    updateBaseExpression(expression.getParameters())));
            return;
        }
        super.maybeUpdateParameters(expression);
    }

    @Override
    public void visit(SuperConstructorInvocationExpression expression) {
        if (expression.getParameters() != null) {
            if (expression instanceof ClassFileSuperConstructorInvocationExpression classFileExpression) {
                expression.setParameters(updateConstructorParameters(
                        classFileExpression.getObjectType(),
                        classFileExpression.getParameterTypes(),
                        updateBaseExpression(expression.getParameters())));
            } else {
                expression.setParameters(updateBaseExpression(expression.getParameters()));
            }
            expression.getParameters().accept(this);
        }
    }

    @Override
    public void visit(MethodInvocationExpression expression) {
        if (expression.getExpression() instanceof MethodInvocationExpression receiver
                && "java/util/Objects".equals(receiver.getInternalTypeName())
                && "requireNonNull".equals(receiver.getName())) {
            receiver.setNonWildcardTypeArguments(null);
        }
        super.visit(expression);
        expression.setParameters(stripRedundantSuperParameterCasts(expression));
    }

    private BaseExpression stripRedundantSuperParameterCasts(MethodInvocationExpression expression) {
        Expression receiver = expression.getExpression();
        if (receiver == null
                || (!receiver.isSuperExpression()
                && !(receiver instanceof org.jd.core.v1.model.javasyntax.expression.QualifiedSuperExpression))) {
            return expression.getParameters();
        }

        BaseExpression parameters = expression.getParameters();
        if (parameters == null) {
            return null;
        }

        if (parameters.isList()) {
            java.util.List<Expression> parameterList = parameters.getList();
            for (int i = 0; i < parameterList.size(); i++) {
                parameterList.set(i, unwrapRedundantWideningCast(parameterList.get(i)));
            }
            return parameters;
        }

        return unwrapRedundantWideningCast(parameters.getFirst());
    }

    private Expression unwrapRedundantWideningCast(Expression parameter) {
        if (!(parameter instanceof CastExpression castExpression)
                || castExpression.getIntersectType() != null
                || !(castExpression.getType() instanceof ObjectType castObjectType)
                || castExpression.getExpression() == null
                || !(castExpression.getExpression().getType() instanceof ObjectType expressionObjectType)
                || ObjectType.TYPE_OBJECT.rawEquals(castObjectType)) {
            return parameter;
        }

        return typeMaker.isRawTypeAssignable(castObjectType, expressionObjectType)
                ? castExpression.getExpression()
                : parameter;
    }

    private BaseExpression updateParameters(
            MethodInvocationExpression invocationExpression,
            BaseType parameterTypes,
            BaseType unboundParameterTypes,
            java.util.Map<String, BaseType> typeBounds,
            BaseExpression parameters) {
        if (parameters == null || parameterTypes == null) {
            return parameters;
        }

        if (parameters.isList()) {
            java.util.List<Expression> parameterList = parameters.getList();
            java.util.List<Type> parameterTypeList = parameterTypes.getList();
            java.util.List<Type> unboundParameterTypeList =
                    unboundParameterTypes == null ? null : unboundParameterTypes.getList();

            for (int i = 0; i < parameterList.size() && i < parameterTypeList.size(); i++) {
                Expression parameter = parameterList.get(i);
                Type unboundParameterType = unboundParameterTypeList == null || i >= unboundParameterTypeList.size()
                        ? null
                        : unboundParameterTypeList.get(i);
                Type parameterType = resolveErasedObjectParameterType(typeBounds, parameterTypeList.get(i), unboundParameterType);
                parameter = maybeRemoveGenericMethodCast(
                        shouldPreserveOverloadDisambiguationCast(invocationExpression, parameters, i),
                        parameterType,
                        unboundParameterType,
                        typeBounds,
                        parameter);
                parameter = ensureRequiredInvocationParameterCast(
                        invocationExpression,
                        parameters,
                        i,
                        parameterType,
                        unboundParameterType,
                        typeBounds,
                        parameter);
                parameterList.set(i, ensureRequiredParameterizedMethodResultCast(parameterType, unboundParameterType, typeBounds, parameter));
            }

            return parameters;
        }

        Type unboundParameterType = unboundParameterTypes == null ? null : unboundParameterTypes.getFirst();
        Type parameterType = resolveErasedObjectParameterType(typeBounds, parameterTypes.getFirst(), unboundParameterType);
        Expression parameter = maybeRemoveGenericMethodCast(
                false,
                parameterType,
                unboundParameterType,
                typeBounds,
                parameters.getFirst());
        parameter = ensureRequiredInvocationParameterCast(
                invocationExpression,
                parameters,
                0,
                parameterType,
                unboundParameterType,
                typeBounds,
                parameter);
        return ensureRequiredParameterizedMethodResultCast(parameterType, unboundParameterType, typeBounds, parameter);
    }

    private BaseExpression updateConstructorParameters(
            ObjectType objectType,
            BaseType parameterTypes,
            BaseExpression parameters) {
        if (parameters == null || parameterTypes == null) {
            return parameters;
        }

        if (parameters.isList()) {
            java.util.List<Expression> parameterList = parameters.getList();
            java.util.List<Type> parameterTypeList = parameterTypes.getList();

            for (int i = 0; i < parameterList.size() && i < parameterTypeList.size(); i++) {
                parameterList.set(i, updateConstructorParameter(objectType, parameters, i, parameterTypeList.get(i), parameterList.get(i)));
            }
            return parameters;
        }

        return updateConstructorParameter(objectType, parameters, 0, parameterTypes.getFirst(), parameters.getFirst());
    }

    private Expression updateConstructorParameter(
            ObjectType objectType,
            BaseExpression parameters,
            int index,
            Type parameterType,
            Expression parameter) {
        parameter = maybeRemoveConstructorParameterCast(objectType, parameters, index, parameterType, parameter);
        return ensureRequiredConstructorParameterCast(parameterType, parameter);
    }

    private Expression maybeRemoveConstructorParameterCast(
            ObjectType objectType,
            BaseExpression parameters,
            int index,
            Type parameterType,
            Expression parameter) {
        if (!(parameter instanceof CastExpression castExpression)
                || shouldPreserveConstructorOverloadDisambiguationCast(objectType, parameters, index)) {
            return parameter;
        }

        if (shouldPreserveOriginalVariableCast(parameterType, Collections.emptyMap(), castExpression)) {
            return parameter;
        }

        if (shouldPreserveTargetTypedCollectionConstructorCast(objectType, castExpression)) {
            return parameter;
        }

        if (isRedundantParameterCast(parameterType, Collections.emptyMap(), castExpression)) {
            return castExpression.getExpression();
        }

        return parameter;
    }

    private Expression ensureRequiredConstructorParameterCast(Type parameterType, Expression parameter) {
        if (parameter == null || parameter instanceof CastExpression || !(parameter instanceof ThisExpression)) {
            return parameter;
        }
        if (parameterType instanceof GenericType genericType) {
            return new CastExpression(genericType, parameter);
        }
        return parameter;
    }

    private Expression ensureRequiredInvocationParameterCast(
            MethodInvocationExpression invocationExpression,
            BaseExpression parameters,
            int index,
            Type parameterType,
            Type unboundParameterType,
            java.util.Map<String, BaseType> typeBounds,
            Expression parameter) {
        if (parameter == null || parameter instanceof CastExpression || parameterType == null) {
            return parameter;
        }
        if (parameter instanceof ThisExpression
                && unboundParameterType instanceof GenericType genericType
                && typeBounds != null
                && typeBounds.get(genericType.getName()) instanceof ObjectType boundObjectType
                && parameter.getType() instanceof ObjectType expressionObjectType
                && boundObjectType.rawEquals(expressionObjectType)) {
            return new CastExpression(unboundParameterType, parameter);
        }
        if (parameter.isNullExpression()
                && parameterType.isObjectType()
                && shouldAddNullParameterCast(invocationExpression, parameters, parameterType, unboundParameterType)) {
            return new CastExpression(parameterType, parameter);
        }
        if (parameterType.getDimension() > 0
                && parameter instanceof ClassFileMethodInvocationExpression
                && parameter.getType().getDimension() == parameterType.getDimension()
                && !parameterType.equals(parameter.getType())
                && !sameRawArrayType(parameterType, parameter.getType())) {
            ClassFileMethodInvocationExpression methodInvocationExpression = (ClassFileMethodInvocationExpression) parameter;
            if (hasTypeParameters(unboundParameterType)
                    || hasTypeParameters(methodInvocationExpression.getUnboundType())
                    || hasTypeParameters(methodInvocationExpression.getType())
                    || methodInvocationExpression.getTypeParameters() != null) {
                return parameter;
            }
            return new CastExpression(parameterType, parameter);
        }
        if (parameter instanceof LocalVariableReferenceExpression
                && parameterType instanceof ObjectType parameterObjectType
                && parameterType.getDimension() == 0
                && parameter.getType() instanceof ObjectType expressionObjectType
                && !isWildcardCaptureCompatibleClassArgument(parameterObjectType, expressionObjectType)
                && !typeMaker.isAssignable(
                        java.util.Collections.emptyMap(),
                        typeBounds == null ? java.util.Collections.emptyMap() : typeBounds,
                        parameterObjectType,
                        unboundParameterType,
                        expressionObjectType)) {
            return new CastExpression(parameterType, parameter);
        }
        if ((parameter.isNewArray() || parameter.isNewInitializedArray())
                && parameterType instanceof ObjectType parameterObjectType
                && parameter.getType() instanceof ObjectType expressionObjectType
                && !parameterObjectType.rawEquals(expressionObjectType)
                && hasAmbiguousInvocationMatch(invocationExpression, parameters)) {
            return new CastExpression(parameterType, parameter);
        }
        return parameter;
    }

    private Type resolveErasedObjectParameterType(
            java.util.Map<String, BaseType> typeBounds,
            Type parameterType,
            Type unboundParameterType) {
        if (!(parameterType instanceof ObjectType parameterObjectType)
                || !(unboundParameterType instanceof GenericType genericType)
                || typeBounds == null
                || !(typeBounds.get(genericType.getName()) instanceof ObjectType boundObjectType)
                || ObjectType.TYPE_OBJECT.rawEquals(boundObjectType)) {
            return parameterType;
        }

        boolean rawObjectParameter = ObjectType.TYPE_OBJECT.rawEquals(parameterObjectType);
        boolean rawObjectArrayParameter = parameterObjectType.getDimension() > 0 && ObjectType.TYPE_OBJECT.rawEquals(parameterObjectType);
        if (!rawObjectParameter && !rawObjectArrayParameter) {
            return parameterType;
        }

        return unboundParameterType.getDimension() == 0
                ? boundObjectType
                : boundObjectType.createType(unboundParameterType.getDimension());
    }

    private boolean shouldPreserveTargetTypedCollectionConstructorCast(
            ObjectType objectType,
            CastExpression castExpression) {
        if (objectType.getTypeArguments() == null
                || !(castExpression.getType() instanceof ObjectType castObjectType)
                || castObjectType.getTypeArguments() != null
                || !"java/util/Collection".equals(castObjectType.getInternalName())
                || !(castExpression.getExpression() instanceof LocalVariableReferenceExpression
                || castExpression.getExpression() instanceof org.jd.core.v1.model.javasyntax.expression.FieldReferenceExpression)
                || !(castExpression.getExpression().getType() instanceof ObjectType expressionObjectType)
                || expressionObjectType.getTypeArguments() == null
                || !typeMaker.isRawTypeAssignable(castObjectType, expressionObjectType)) {
            return false;
        }

        TypeArgument targetArgument = objectType.getTypeArguments().isTypeArgumentList()
                ? objectType.getTypeArguments().getTypeArgumentList().getFirst()
                : objectType.getTypeArguments().getTypeArgumentFirst();
        if (!(targetArgument instanceof Type targetType)) {
            return false;
        }

        org.jd.core.v1.model.javasyntax.type.TypeArguments targetCollectionArguments =
                new org.jd.core.v1.model.javasyntax.type.TypeArguments();
        targetCollectionArguments.add(new WildcardExtendsTypeArgument(targetType));
        ObjectType targetCollectionType = castObjectType.createType(targetCollectionArguments);

        return !typeMaker.isAssignable(
                Collections.emptyMap(),
                Collections.emptyMap(),
                targetCollectionType,
                null,
                expressionObjectType);
    }

    private boolean isWildcardCaptureCompatibleClassArgument(ObjectType parameterObjectType, ObjectType expressionObjectType) {
        if (!"java/lang/Class".equals(parameterObjectType.getInternalName())
                || parameterObjectType.getTypeArguments() == null
                || expressionObjectType.getTypeArguments() == null
                || !parameterObjectType.rawEquals(expressionObjectType)) {
            return false;
        }

        TypeArgument targetArgument = parameterObjectType.getTypeArguments().isTypeArgumentList()
                ? parameterObjectType.getTypeArguments().getTypeArgumentList().getFirst()
                : parameterObjectType.getTypeArguments().getTypeArgumentFirst();
        TypeArgument sourceArgument = expressionObjectType.getTypeArguments().isTypeArgumentList()
                ? expressionObjectType.getTypeArguments().getTypeArgumentList().getFirst()
                : expressionObjectType.getTypeArguments().getTypeArgumentFirst();

        if (!(targetArgument instanceof Type targetType)
                || !(sourceArgument instanceof WildcardExtendsTypeArgument wildcardExtendsTypeArgument)
                || wildcardExtendsTypeArgument.type() == null) {
            return false;
        }

        Type sourceType = wildcardExtendsTypeArgument.type();

        if (targetType.equals(sourceType)) {
            return true;
        }

        if (targetType.isObjectType() && sourceType.isObjectType()) {
            return ((ObjectType) targetType).rawEquals((ObjectType) sourceType);
        }

        return false;
    }

    private boolean hasAmbiguousInvocationMatch(MethodInvocationExpression invocationExpression, BaseExpression parameters) {
        if (parameters == null) {
            return false;
        }
        if (invocationExpression instanceof ClassFileMethodInvocationExpression classFileInvocationExpression) {
            java.util.Map<String, TypeArgument> typeBindings = classFileInvocationExpression.getTypeBindings();
            java.util.Map<String, BaseType> localTypeBounds = classFileInvocationExpression.getTypeBounds();
            if (typeBindings != null && localTypeBounds != null) {
                return typeMaker.matchCount(
                        typeBindings,
                        localTypeBounds,
                        invocationExpression.getInternalTypeName(),
                        invocationExpression.getName(),
                        parameters,
                        false) > 1;
            }
        }
        return typeMaker.matchCount(invocationExpression.getInternalTypeName(), invocationExpression.getName(), parameters.size(), false) > 1;
    }

    private boolean shouldAddNullParameterCast(
            MethodInvocationExpression invocationExpression,
            BaseExpression parameters,
            Type parameterType,
            Type unboundParameterType) {
        if (parameterType instanceof ObjectType objectType
                && "java/lang/Class".equals(objectType.getInternalName())
                && (hasTypeParameters(parameterType) || hasTypeParameters(unboundParameterType))) {
            return false;
        }
        return hasAmbiguousInvocationMatch(invocationExpression, parameters)
                || typeMaker.matchCount(invocationExpression.getInternalTypeName(), invocationExpression.getName(), parameters.size(), false) > 1;
    }

    private Expression maybeRemoveGenericMethodCast(
            boolean preserveOverloadDisambiguationCast,
            Type parameterType,
            Type unboundParameterType,
            java.util.Map<String, BaseType> typeBounds,
            Expression parameter) {
        if (!(parameter instanceof CastExpression castExpression)) {
            return parameter;
        }
        if (castExpression.getIntersectType() != null) {
            return parameter;
        }
        if (castExpression.getType() instanceof GenericType
                && castExpression.getExpression().getType() != null
                && castExpression.getExpression().getType().isPrimitiveType()) {
            return castExpression.getExpression();
        }
        if (castExpression.getType() instanceof ObjectType castObjectType
                && "java/lang/Class".equals(castObjectType.getInternalName())
                && castExpression.getExpression().getType() instanceof ObjectType expressionObjectType
                && castObjectType.rawEquals(expressionObjectType)
                && (castExpression.getExpression() instanceof LocalVariableReferenceExpression
                || castExpression.getExpression() instanceof org.jd.core.v1.model.javasyntax.expression.FieldReferenceExpression)) {
            return castExpression.getExpression();
        }
        if (parameterType instanceof GenericType genericType
                && castExpression.getExpression().getType() instanceof GenericType expressionGenericType
                && genericType.getName().equals(expressionGenericType.getName())) {
            return castExpression.getExpression();
        }
        if (castExpression.getType() instanceof GenericType castGenericType
                && typeBounds != null
                && typeBounds.get(castGenericType.getName()) instanceof ObjectType boundObjectType
                && castExpression.getExpression() instanceof ClassFileMethodInvocationExpression methodInvocationExpression
                && methodInvocationExpression.getType() instanceof ObjectType expressionObjectType
                && AutoboxingVisitor.isBoxingMethod(methodInvocationExpression)
                && boundObjectType.rawEquals(expressionObjectType)) {
            return castExpression.getExpression();
        }
        if (parameterType instanceof GenericType genericType
                && typeBounds != null
                && typeBounds.get(genericType.getName()) instanceof ObjectType boundObjectType
                && castExpression.getExpression() instanceof ClassFileMethodInvocationExpression methodInvocationExpression
                && methodInvocationExpression.getType() instanceof ObjectType expressionObjectType
                && AutoboxingVisitor.isBoxingMethod(methodInvocationExpression)
                && boundObjectType.rawEquals(expressionObjectType)) {
            return castExpression.getExpression();
        }

        if (preserveOverloadDisambiguationCast) {
            return parameter;
        }

        if (castExpression.getType().isObjectType()
                && ObjectType.TYPE_OBJECT.rawEquals((ObjectType) castExpression.getType())
                && parameterType != null
                && parameterType.isObjectType()
                && !ObjectType.TYPE_OBJECT.rawEquals((ObjectType) parameterType)
                && castExpression.getExpression() instanceof ClassFileMethodInvocationExpression methodInvocationExpression
                && AutoboxingVisitor.isBoxingMethod(methodInvocationExpression)
                && hasTypeParameters(unboundParameterType)) {
            return parameter;
        }

        if (shouldPreserveOriginalVariableCast(parameterType, typeBounds, castExpression)) {
            return parameter;
        }
        if (castExpression.getType() instanceof ObjectType castObjectType
                && castObjectType.getDimension() > 0
                && castExpression.getExpression() instanceof ClassFileMethodInvocationExpression methodInvocationExpression
                && requiresTypedArrayResultCastForRawReceiver(castObjectType, methodInvocationExpression)) {
            return parameter;
        }

        if (isRedundantParameterCast(parameterType, typeBounds, castExpression)) {
            return castExpression.getExpression();
        }

        if (!(castExpression.getExpression() instanceof ClassFileMethodInvocationExpression methodInvocationExpression)) {
            if (parameterType instanceof GenericType genericType
                    && typeBounds != null
                    && typeBounds.get(genericType.getName()) instanceof ObjectType boundObjectType
                    && castExpression.getType() instanceof ObjectType castObjectType
                    && castObjectType.rawEquals(boundObjectType)
                    && castExpression.getExpression().getType() instanceof ObjectType expressionObjectType
                    && isErasedHierarchyAssignable(boundObjectType, expressionObjectType)) {
                return castExpression.getExpression();
            }
            if (parameterType instanceof ObjectType parameterObjectType
                    && "java/lang/Class".equals(parameterObjectType.getInternalName())
                    && hasTypeParameters(unboundParameterType)
                    && castExpression.getType() instanceof ObjectType castObjectType
                    && castObjectType.rawEquals(parameterObjectType)
                    && castExpression.getExpression().getType() instanceof ObjectType expressionObjectType
                    && expressionObjectType.rawEquals(parameterObjectType)
                    && (castExpression.getExpression() instanceof LocalVariableReferenceExpression
                    || castExpression.getExpression() instanceof org.jd.core.v1.model.javasyntax.expression.FieldReferenceExpression)) {
                return castExpression.getExpression();
            }
            if (shouldPreserveParameterizedClassCast(parameterType, unboundParameterType, castExpression)) {
                return parameter;
            }
            if (parameterType instanceof ObjectType parameterObjectType
                    && unboundParameterType instanceof GenericType
                    && castExpression.getType() instanceof ObjectType castObjectType
                    && castObjectType.rawEquals(parameterObjectType)
                    && castExpression.getExpression().getType() instanceof ObjectType expressionObjectType
                    && isErasedHierarchyAssignable(parameterObjectType, expressionObjectType)) {
                return castExpression.getExpression();
            }
            if (castExpression.isByteCodeCheckCast()
                    && parameterType != null
                    && parameterType.isObjectType()
                    && castExpression.getType().isObjectType()
                    && ((ObjectType) castExpression.getType()).rawEquals((ObjectType) parameterType)
                    && hasTypeParameters(unboundParameterType)) {
                return castExpression.getExpression();
            }
            if (isJavaLangObjectArray(castExpression.getType())
                    && castExpression.getExpression().getType() instanceof ObjectType expressionObjectType
                    && expressionObjectType.getDimension() == castExpression.getType().getDimension()
                    && !isJavaLangObjectArray(expressionObjectType)) {
                return castExpression.getExpression();
            }
            BaseType genericBound = resolveGenericBound(typeBounds, unboundParameterType, castExpression.getExpression().getType());
            if (genericBound instanceof ObjectType boundObjectType
                    && castExpression.getType().isObjectType()
                    && parameterType != null
                    && parameterType.isObjectType()
                    && ((ObjectType) castExpression.getType()).rawEquals((ObjectType) parameterType)
                    && castExpression.getExpression().getType() instanceof ObjectType expressionObjectType
                    && (boundObjectType.rawEquals(expressionObjectType)
                    || typeMaker.isRawTypeAssignable(boundObjectType, expressionObjectType))) {
                return castExpression.getExpression();
            }
            if (parameterType != null
                    && castExpression.getType().isObjectType()
                    && (castExpression.getExpression() instanceof LambdaIdentifiersExpression
                    || castExpression.getExpression() instanceof MethodReferenceExpression)
                    && parameterType.isObjectType()
                    && !shouldPreserveParameterizedArrayConstructorReferenceCast(castExpression)
                    && ((ObjectType) castExpression.getType()).rawEquals((ObjectType) parameterType)
                    && (hasTypeParameters(parameterType) || hasTypeParameters(unboundParameterType))) {
                return castExpression.getExpression();
            }
            return parameter;
        }

        if (parameterType != null
                && parameterType.isObjectType()
                && castExpression.getType().isObjectType()
                && methodInvocationExpression.getType().isObjectType()
                && ((ObjectType) parameterType).getTypeArguments() != null
                && ((ObjectType) castExpression.getType()).getTypeArguments() == null
                && ((ObjectType) parameterType).rawEquals((ObjectType) castExpression.getType())
                && (parameterType.equals(methodInvocationExpression.getType())
                || typeMaker.isRawTypeAssignable((ObjectType) parameterType, (ObjectType) methodInvocationExpression.getType()))) {
            return castExpression.getExpression();
        }

        if (parameterType instanceof ObjectType parameterObjectType
                && castExpression.getType() instanceof ObjectType castObjectType
                && methodInvocationExpression.getType() instanceof ObjectType expressionObjectType
                && parameterObjectType.getTypeArguments() == null
                && castObjectType.getTypeArguments() == null
                && castObjectType.rawEquals(parameterObjectType)
                && typeMaker.isRawTypeAssignable(castObjectType, expressionObjectType)) {
            return castExpression.getExpression();
        }

        if (parameterType instanceof GenericType genericType
                && typeBounds != null
                && typeBounds.get(genericType.getName()) instanceof ObjectType boundObjectType
                && castExpression.getType() instanceof ObjectType castObjectType
                && methodInvocationExpression.getType() instanceof ObjectType expressionObjectType
                && castObjectType.rawEquals(boundObjectType)
                && typeMaker.isRawTypeAssignable(boundObjectType, expressionObjectType)) {
            return castExpression.getExpression();
        }

        if (parameterType instanceof ObjectType parameterObjectType
                && unboundParameterType instanceof GenericType
                && castExpression.getType() instanceof ObjectType castObjectType
                && methodInvocationExpression.getType() instanceof ObjectType expressionObjectType
                && castObjectType.rawEquals(parameterObjectType)
                && isErasedHierarchyAssignable(parameterObjectType, expressionObjectType)) {
            return castExpression.getExpression();
        }

        if (parameterType instanceof ObjectType parameterObjectType
                && unboundParameterType instanceof GenericType
                && castExpression.getType() instanceof ObjectType castObjectType
                && methodInvocationExpression.getType() instanceof ObjectType expressionObjectType
                && parameterObjectType.rawEquals(expressionObjectType)
                && isErasedHierarchyAssignable(castObjectType, parameterObjectType)) {
            return castExpression.getExpression();
        }

        if (isJavaLangObjectArray(castExpression.getType())
                && parameterType != null
                && parameterType.getDimension() == castExpression.getType().getDimension()
                && methodInvocationExpression.getType().getDimension() == parameterType.getDimension()
                && (methodInvocationExpression.getTypeParameters() != null
                || hasTypeParameters(unboundParameterType)
                || hasTypeParameters(methodInvocationExpression.getUnboundType())
                || hasTypeParameters(methodInvocationExpression.getType()))) {
            return castExpression.getExpression();
        }

        if (castExpression.getType().isObjectType()
                && ObjectType.TYPE_OBJECT.rawEquals((ObjectType) castExpression.getType())
                && hasTypeParameters(unboundParameterType)
                && (parameterType == null
                || (parameterType.isObjectType() && ObjectType.TYPE_OBJECT.rawEquals((ObjectType) parameterType)))) {
            return castExpression.getExpression();
        }

        Type expressionType = methodInvocationExpression.getType();
        Type expressionUnboundType = methodInvocationExpression.getUnboundType();
        boolean genericMethodResult = methodInvocationExpression.getTypeParameters() != null
                || hasTypeParameters(unboundParameterType)
                || hasTypeParameters(expressionType)
                || hasTypeParameters(expressionUnboundType);

        if (!genericMethodResult) {
            return parameter;
        }

        if (castExpression.getType().isObjectType()
                && parameterType != null
                && parameterType.isObjectType()
                && expressionType.isObjectType()
                && ((ObjectType) parameterType).rawEquals((ObjectType) castExpression.getType())
                && ((ObjectType) parameterType).rawEquals((ObjectType) expressionType)) {
            return castExpression.getExpression();
        }

        if (parameterType != null
                && parameterType.getDimension() > 0
                && castExpression.getType().getDimension() == parameterType.getDimension()
                && expressionType.getDimension() == parameterType.getDimension()
                && (expressionType.isGenericType() || hasTypeParameters(unboundParameterType) || hasTypeParameters(expressionUnboundType))) {
            return castExpression.getExpression();
        }

        return parameter;
    }

    private static boolean shouldPreserveParameterizedArrayConstructorReferenceCast(CastExpression castExpression) {
        if (!(castExpression.getExpression() instanceof MethodReferenceExpression methodReference)
                || !"new".equals(methodReference.getName())
                || !(castExpression.getType() instanceof ObjectType castObjectType)
                || castObjectType.getTypeArguments() == null
                || !castObjectType.getTypeArguments().isTypeArgumentList()) {
            return false;
        }

        for (TypeArgument typeArgument : castObjectType.getTypeArguments().getTypeArgumentList()) {
            if (typeArgument instanceof Type targetType
                    && targetType.getDimension() > 0
                    && hasTypeParameters(targetType)) {
                return true;
            }
        }

        return false;
    }

    private static boolean shouldPreserveParameterizedClassCast(
            Type parameterType,
            Type unboundParameterType,
            CastExpression castExpression) {
        if (!(parameterType instanceof ObjectType parameterObjectType)
                || !"java/lang/Class".equals(parameterObjectType.getInternalName())
                || !castExpression.isByteCodeCheckCast()
                || !(castExpression.getType() instanceof ObjectType castObjectType)
                || !castObjectType.rawEquals(parameterObjectType)) {
            return false;
        }

        return parameterObjectType.getTypeArguments() != null || hasTypeParameters(unboundParameterType);
    }

    private boolean isRedundantParameterCast(
            Type parameterType,
            java.util.Map<String, BaseType> typeBounds,
            CastExpression castExpression) {
        if (castExpression.getIntersectType() != null) {
            return false;
        }
        if (shouldPreserveOriginalVariableCast(parameterType, typeBounds, castExpression)) {
            return false;
        }

        if (!(castExpression.getType() instanceof ObjectType castObjectType)
                || parameterType == null) {
            return false;
        }

        Type expressionType = castExpression.getExpression().getType();
        if (!castExpression.isByteCodeCheckCast()) {
            if (castExpression.getExpression() instanceof MethodReferenceExpression
                    || castExpression.getExpression() instanceof LambdaIdentifiersExpression) {
                return false;
            }
            if (castExpression.getExpression() instanceof LocalVariableReferenceExpression
                    && (!(parameterType instanceof ObjectType parameterObjectType)
                    || !(expressionType instanceof ObjectType expressionObjectType)
                    || !parameterObjectType.rawEquals(expressionObjectType))
                    && !(parameterType instanceof ObjectType po
                    && castObjectType.rawEquals(po)
                    && expressionType instanceof ObjectType eo
                    && typeMaker.isRawTypeAssignable(castObjectType, eo))) {
                return false;
            }
        }

        if (parameterType instanceof GenericType genericType
                && expressionType instanceof GenericType expressionGenericType
                && expressionGenericType.getName().equals(genericType.getName())) {
            return true;
        }
        if (parameterType instanceof GenericType genericType
                && typeBounds != null
                && typeBounds.get(genericType.getName()) instanceof ObjectType boundObjectType) {
            return castObjectType.rawEquals(boundObjectType);
        }

        if (!(parameterType instanceof ObjectType parameterObjectType)) {
            return false;
        }

        if (parameterType.getDimension() > 0
                && expressionType instanceof ObjectType expressionObjectType
                && castObjectType.getDimension() == parameterType.getDimension()
                && expressionObjectType.getDimension() == parameterType.getDimension()
                && castObjectType.rawEquals(parameterObjectType)
                && castObjectType.rawEquals(expressionObjectType)) {
            return true;
        }

        if (hasTypeParameters(parameterObjectType) && hasUnboundedWildcardTypeArgument(expressionType)) {
            return false;
        }

        if (expressionType instanceof ObjectType expressionObjectType) {
            if (castExpression.getExpression() instanceof LocalVariableReferenceExpression
                    && typeMaker.isAssignable(
                    java.util.Collections.emptyMap(),
                    typeBounds == null ? java.util.Collections.emptyMap() : typeBounds,
                    parameterObjectType,
                    null,
                    expressionObjectType)) {
                return true;
            }
            if (castExpression.getExpression() instanceof LocalVariableReferenceExpression
                    && typeMaker.isRawTypeAssignable(expressionObjectType, parameterObjectType)) {
                return true;
            }
            if (castExpression.getExpression() instanceof LocalVariableReferenceExpression
                    && typeMaker.isRawTypeAssignable(expressionObjectType.createType(null), parameterObjectType.createType(null))) {
                return true;
            }
            return typeMaker.isRawTypeAssignable(castObjectType, parameterObjectType)
                    && typeMaker.isRawTypeAssignable(castObjectType, expressionObjectType);
        }

        if (castExpression.getExpression().isIntegerConstantExpression()
                && isPrimitiveWrapper(castObjectType)
                && castObjectType.rawEquals(parameterObjectType)) {
            return true;
        }
        if (expressionType instanceof GenericType expressionGenericType
                && typeBounds != null
                && typeBounds.get(expressionGenericType.getName()) instanceof ObjectType boundObjectType) {
            return castObjectType.rawEquals(boundObjectType)
                    && typeMaker.isRawTypeAssignable(castObjectType, parameterObjectType);
        }

        return false;
    }

    private Expression ensureRequiredParameterizedMethodResultCast(
            Type parameterType,
            Type unboundParameterType,
            java.util.Map<String, BaseType> typeBounds,
            Expression parameter) {
        if (parameter instanceof CastExpression
                || !(parameter instanceof ClassFileMethodInvocationExpression methodInvocationExpression)) {
            return parameter;
        }

        if (unboundParameterType instanceof GenericType genericType
                && parameter.getType() instanceof ObjectType expressionObjectType
                && resolveGenericBound(
                        methodInvocationExpression.getTypeBounds() != null
                                ? methodInvocationExpression.getTypeBounds()
                                : typeBounds,
                        unboundParameterType,
                        parameter.getType()) instanceof ObjectType boundObjectType
                && boundObjectType.rawEquals(expressionObjectType)) {
            return new CastExpression(unboundParameterType, parameter);
        }

        if (parameter instanceof CastExpression
                || !(parameterType instanceof ObjectType parameterObjectType)
                || !"java/lang/Class".equals(parameterObjectType.getInternalName())
                || parameterObjectType.getTypeArguments() == null
                || methodInvocationExpression.getTypeParameters() != null
                || !(parameter.getType() instanceof ObjectType expressionObjectType)
                || !parameterObjectType.rawEquals(expressionObjectType)
                || !hasUnboundedWildcardTypeArgument(expressionObjectType)) {
            return parameter;
        }

        return new CastExpression(parameterType, parameter);
    }

    private boolean shouldPreserveOriginalVariableCast(
            Type parameterType,
            java.util.Map<String, BaseType> typeBounds,
            CastExpression castExpression) {
        ObjectType targetObjectType = resolveTargetObjectType(parameterType, typeBounds);
        if (targetObjectType == null
                || !(castExpression.getExpression() instanceof ClassFileLocalVariableReferenceExpression localVariableReferenceExpression)) {
            return false;
        }

        ObjectType originalObjectType = resolveSourceObjectType(localVariableReferenceExpression.getLocalVariable());
        if (originalObjectType == null) {
            return false;
        }

        return !typeMaker.isAssignable(
                Collections.emptyMap(),
                typeBounds == null ? Collections.emptyMap() : typeBounds,
                targetObjectType,
                null,
                originalObjectType);
    }

    private static ObjectType resolveTargetObjectType(Type parameterType, java.util.Map<String, BaseType> typeBounds) {
        if (parameterType instanceof ObjectType objectType) {
            return objectType;
        }
        if (parameterType instanceof GenericType genericType
                && typeBounds != null
                && typeBounds.get(genericType.getName()) instanceof ObjectType boundObjectType) {
            return boundObjectType;
        }
        return null;
    }

    private static ObjectType resolveSourceObjectType(AbstractLocalVariable localVariable) {
        if (localVariable.getOriginalVariable() != null
                && localVariable.getOriginalVariable().getType() instanceof ObjectType originalObjectType) {
            return originalObjectType;
        }
        if (localVariable.getDeclaredType() instanceof ObjectType declaredObjectType) {
            return declaredObjectType;
        }
        return null;
    }

    private boolean shouldPreserveOverloadDisambiguationCast(
            MethodInvocationExpression invocationExpression,
            BaseExpression parameters,
            int index) {
        if (!(invocationExpression instanceof ClassFileMethodInvocationExpression classFileInvocationExpression)
                || !parameters.isList()
                || !(parameters.getList().get(index) instanceof CastExpression castExpression)
                || castExpression.isByteCodeCheckCast()
                || classFileInvocationExpression.getTypeBindings() == null
                || classFileInvocationExpression.getTypeBounds() == null) {
            return false;
        }

        if (shouldPreserveDescriptorBridgeParameterCast(classFileInvocationExpression, index, castExpression)) {
            return true;
        }
        if ((castExpression.getExpression() instanceof MethodReferenceExpression
                || castExpression.getExpression() instanceof LambdaIdentifiersExpression)
                && castExpression.getType() instanceof ObjectType castObjectType
                && castObjectType.getTypeArguments() != null
                && typeMaker.matchCount(
                invocationExpression.getInternalTypeName(),
                invocationExpression.getName(),
                parameters.size(),
                false) > 1) {
            return true;
        }

        int currentMatches = typeMaker.matchCount(
                classFileInvocationExpression.getTypeBindings(),
                classFileInvocationExpression.getTypeBounds(),
                invocationExpression.getInternalTypeName(),
                invocationExpression.getName(),
                parameters,
                false);

        if (currentMatches != 1) {
            return false;
        }

        parameters.getList().set(index, castExpression.getExpression());
        int matchesWithoutCast = typeMaker.matchCount(
                classFileInvocationExpression.getTypeBindings(),
                classFileInvocationExpression.getTypeBounds(),
                invocationExpression.getInternalTypeName(),
                invocationExpression.getName(),
                parameters,
                false);
        parameters.getList().set(index, castExpression);

        return matchesWithoutCast != 1;
    }

    private boolean shouldPreserveConstructorOverloadDisambiguationCast(
            ObjectType objectType,
            BaseExpression parameters,
            int index) {
        if (!parameters.isList()
                || !(parameters.getList().get(index) instanceof CastExpression castExpression)) {
            return false;
        }

        int currentMatches = typeMaker.matchCount(
                java.util.Collections.emptyMap(),
                java.util.Collections.emptyMap(),
                objectType.getInternalName(),
                StringConstants.INSTANCE_CONSTRUCTOR,
                parameters,
                true);

        if (currentMatches != 1) {
            return false;
        }

        parameters.getList().set(index, castExpression.getExpression());
        int matchesWithoutCast = typeMaker.matchCount(
                java.util.Collections.emptyMap(),
                java.util.Collections.emptyMap(),
                objectType.getInternalName(),
                StringConstants.INSTANCE_CONSTRUCTOR,
                parameters,
                true);
        parameters.getList().set(index, castExpression);

        return matchesWithoutCast != 1;
    }

    private static BaseType resolveGenericBound(java.util.Map<String, BaseType> typeBounds, Type unboundParameterType, Type expressionType) {
        if (typeBounds == null) {
            return null;
        }
        if (unboundParameterType instanceof org.jd.core.v1.model.javasyntax.type.GenericType genericType) {
            return typeBounds.get(genericType.getName());
        }
        if (expressionType instanceof org.jd.core.v1.model.javasyntax.type.GenericType genericType) {
            return typeBounds.get(genericType.getName());
        }
        return null;
    }

    private boolean isErasedHierarchyAssignable(ObjectType targetType, ObjectType sourceType) {
        return isErasedHierarchyAssignable(targetType, sourceType, new java.util.HashSet<>());
    }

    private boolean isErasedHierarchyAssignable(ObjectType targetType, ObjectType sourceType, java.util.Set<String> visitedTypes) {
        if (targetType.rawEquals(sourceType)
                || targetType.getInternalName().equals(sourceType.getInternalName())
                || (typeMaker.isRawTypeAssignable(targetType, sourceType) && typeMaker.isRawTypeAssignable(sourceType, targetType))) {
            return true;
        }
        if (!visitedTypes.add(sourceType.getInternalName())) {
            return false;
        }

        TypeMaker.TypeTypes sourceTypeTypes = typeMaker.makeTypeTypes(sourceType.getInternalName());
        if (sourceTypeTypes == null) {
            return false;
        }

        ObjectType superType = sourceTypeTypes.getSuperType();
        if (superType != null && isErasedHierarchyAssignable(targetType, superType, visitedTypes)) {
            return true;
        }

        BaseType interfaces = sourceTypeTypes.getInterfaces();
        if (interfaces != null) {
            for (Type interfaceType : interfaces) {
                if (interfaceType instanceof ObjectType objectInterfaceType
                        && isErasedHierarchyAssignable(targetType, objectInterfaceType, visitedTypes)) {
                    return true;
                }
            }
        }

        return false;
    }

    private boolean shouldPreserveDescriptorBridgeParameterCast(
            ClassFileMethodInvocationExpression invocationExpression,
            int index,
            CastExpression castExpression) {
        RawDescriptorMethodTypes descriptorMethodTypes = parseRawDescriptorMethodTypes(invocationExpression.getDescriptor());
        if (descriptorMethodTypes == null
                || descriptorMethodTypes.parameterTypes() == null) {
            return false;
        }

        Type descriptorParameterType = getParameterType(descriptorMethodTypes.parameterTypes(), index);
        if (descriptorParameterType == null
                || isRawJavaLangObject(descriptorParameterType)
                || isRawJavaLangObjectArray(descriptorParameterType)
                || !sameRawType(castExpression.getType(), descriptorParameterType)) {
            return false;
        }

        Expression receiver = invocationExpression.getExpression();
        if (receiver != null
                && (receiver.isSuperExpression()
                || receiver instanceof org.jd.core.v1.model.javasyntax.expression.QualifiedSuperExpression)) {
            return false;
        }
        Type unboundParameterType = getParameterType(invocationExpression.getUnboundParameterTypes(), index);
        if (unboundParameterType instanceof GenericType genericType
                && invocationExpression.getTypeBounds() != null
                && invocationExpression.getTypeBounds().get(genericType.getName()) instanceof ObjectType boundObjectType
                && castExpression.getExpression().getType() instanceof ObjectType expressionObjectType
                && isErasedHierarchyAssignable(boundObjectType, expressionObjectType)) {
            return false;
        }

        return !sameRawType(castExpression.getExpression().getType(), descriptorParameterType);
    }

    private static Type getParameterType(BaseType parameterTypes, int index) {
        if (parameterTypes == null) {
            return null;
        }
        if (parameterTypes.isList()) {
            return index < parameterTypes.size() ? parameterTypes.getList().get(index) : null;
        }
        return index == 0 ? parameterTypes.getFirst() : null;
    }

    private RawDescriptorMethodTypes parseRawDescriptorMethodTypes(String descriptor) {
        if (descriptor == null || descriptor.isEmpty() || descriptor.charAt(0) != '(') {
            return null;
        }

        org.jd.core.v1.model.javasyntax.type.Types parameterTypes = new org.jd.core.v1.model.javasyntax.type.Types();
        int index = 1;
        while (descriptor.charAt(index) != ')') {
            int nextIndex = skipDescriptorType(descriptor, index);
            parameterTypes.add(makeRawDescriptorType(descriptor.substring(index, nextIndex)));
            index = nextIndex;
        }

        return new RawDescriptorMethodTypes(parameterTypes);
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
            case "B" -> org.jd.core.v1.model.javasyntax.type.PrimitiveType.TYPE_BYTE;
            case "C" -> org.jd.core.v1.model.javasyntax.type.PrimitiveType.TYPE_CHAR;
            case "D" -> org.jd.core.v1.model.javasyntax.type.PrimitiveType.TYPE_DOUBLE;
            case "F" -> org.jd.core.v1.model.javasyntax.type.PrimitiveType.TYPE_FLOAT;
            case "I" -> org.jd.core.v1.model.javasyntax.type.PrimitiveType.TYPE_INT;
            case "J" -> org.jd.core.v1.model.javasyntax.type.PrimitiveType.TYPE_LONG;
            case "S" -> org.jd.core.v1.model.javasyntax.type.PrimitiveType.TYPE_SHORT;
            case "Z" -> org.jd.core.v1.model.javasyntax.type.PrimitiveType.TYPE_BOOLEAN;
            default -> typeMaker.makeFromInternalTypeName(descriptor.substring(dimension + 1, descriptor.length() - 1));
        };

        return dimension == 0 ? elementType : elementType.createType(dimension);
    }

    private static boolean sameRawType(Type left, Type right) {
        if (left == null || right == null || left.getDimension() != right.getDimension()) {
            return false;
        }
        if (!left.isObjectType() || !right.isObjectType()) {
            return left.equals(right);
        }
        return ((ObjectType) left).rawEquals((ObjectType) right);
    }

    private static boolean sameRawArrayType(Type left, Type right) {
        return left != null
                && right != null
                && left.getDimension() > 0
                && sameRawType(left, right);
    }

    private static boolean isRawJavaLangObject(Type type) {
        return type instanceof ObjectType objectType
                && type.getDimension() == 0
                && ObjectType.TYPE_OBJECT.rawEquals(objectType);
    }

    private static boolean isRawJavaLangObjectArray(Type type) {
        return type instanceof ObjectType objectType
                && type.getDimension() > 0
                && ObjectType.TYPE_OBJECT.rawEquals(objectType);
    }

    private record RawDescriptorMethodTypes(BaseType parameterTypes) {
    }

    private boolean shouldRemoveStandaloneGenericMethodCast(CastExpression castExpression) {
        if (castExpression.getIntersectType() != null) {
            return false;
        }
        if ((castExpression.getExpression() instanceof LambdaIdentifiersExpression
                || castExpression.getExpression() instanceof MethodReferenceExpression)
                && castExpression.getType().isObjectType()) {
            ObjectType castObjectType = (ObjectType) castExpression.getType();
            if (castObjectType.getInternalName().startsWith("org/apache/commons/lang3/function/Failable")
                    && containsThrowableTypeArgument(castObjectType)) {
                return true;
            }
        }

        if (!(castExpression.getExpression() instanceof ClassFileMethodInvocationExpression methodInvocationExpression)) {
            return false;
        }

        Type castType = castExpression.getType();
        Type expressionType = methodInvocationExpression.getType();

        if (shouldPreserveReflectiveResultCast(castExpression, methodInvocationExpression)) {
            return false;
        }
        if (castExpression.getType() instanceof ObjectType castObjectType
                && castObjectType.getDimension() > 0
                && requiresTypedArrayResultCastForRawReceiver(castObjectType, methodInvocationExpression)) {
            return false;
        }

        if ((isArrayConstructorToArrayCast(castExpression.getType(), methodInvocationExpression)
                || isTypedArrayToArrayCast(castExpression.getType(), methodInvocationExpression))
                && !shouldPreserveTypedArrayToArrayCast(castExpression.getType(), methodInvocationExpression)) {
            return true;
        }

        if (castType instanceof GenericType genericType
                && !canInferGenericMethodResultFromParameters(methodInvocationExpression, genericType.getName())) {
            return false;
        }

        if (!castExpression.isByteCodeCheckCast() && castType.equals(expressionType)) {
            return true;
        }
        if (!castExpression.isByteCodeCheckCast()
                && castType.isObjectType()
                && expressionType.isObjectType()
                && isSourceInferredGenericHelper(methodInvocationExpression)
                && ((ObjectType) castType).getTypeArguments() != null
                && typeMaker.isRawTypeAssignable((ObjectType) castType, (ObjectType) expressionType)) {
            return true;
        }
        if (!castExpression.isByteCodeCheckCast()
                && castType.isObjectType()
                && expressionType instanceof GenericType expressionGenericType
                && resolveGenericBound(methodInvocationExpression.getTypeBounds(), methodInvocationExpression.getUnboundType(), expressionGenericType) instanceof ObjectType boundObjectType
                && ((ObjectType) castType).rawEquals(boundObjectType)) {
            return true;
        }

        if (shouldRemoveParameterizedSuperBridgeResultCast(castExpression, methodInvocationExpression)) {
            methodInvocationExpression.setType(castExpression.getType());
            return true;
        }
        if (requiresDescriptorBridgeResultCast(castExpression, methodInvocationExpression)) {
            return false;
        }
        BaseType genericBound = resolveGenericBound(methodInvocationExpression.getTypeBounds(), methodInvocationExpression.getUnboundType(), methodInvocationExpression.getType());
        if (castExpression.isByteCodeCheckCast()
                && genericBound instanceof ObjectType boundObjectType
                && castExpression.getType().isObjectType()
                && boundObjectType.rawEquals((ObjectType) castExpression.getType())) {
            return true;
        }
        // Non-bytecode-checkcast on a generic method result where the cast type rawEquals
        // the resolved expression type. The cast is redundant since the type was already
        // correctly resolved from the receiver's type arguments.
        // Example: ((Map)map.get(key)).put(...) where map is Map<String, Map<String, String>>
        if (!castExpression.isByteCodeCheckCast()
                && castExpression.getType().isObjectType()
                && expressionType.isObjectType()
                && ((ObjectType) castExpression.getType()).rawEquals((ObjectType) expressionType)
                && ((ObjectType) castExpression.getType()).getTypeArguments() == null
                && methodInvocationExpression.getExpression() != null
                && methodInvocationExpression.getExpression().getType() instanceof ObjectType receiverType
                && receiverType.getTypeArguments() != null) {
            return true;
        }
        if (isJavaLangObjectArray(castExpression.getType())
                && methodInvocationExpression.getType().getDimension() == castExpression.getType().getDimension()
                && hasGenericArrayInvocationShape(methodInvocationExpression)) {
            return true;
        }
        if (sameRawArrayType(castExpression.getType(), methodInvocationExpression.getType())) {
            return true;
        }

        Type unboundType = methodInvocationExpression.getUnboundType();
        boolean genericMethodResult = methodInvocationExpression.getTypeParameters() != null
                || hasTypeParameters(expressionType)
                || hasTypeParameters(unboundType);

        if (!genericMethodResult) {
            return false;
        }

        if (castType.isObjectType() && expressionType.isObjectType()) {
            return ((ObjectType) castType).rawEquals((ObjectType) expressionType);
        }

        return castType.getDimension() > 0 && castType.getDimension() == expressionType.getDimension();
    }

    private boolean canInferGenericMethodResultFromParameters(
            ClassFileMethodInvocationExpression methodInvocationExpression,
            String genericName) {
        BaseExpression parameters = methodInvocationExpression.getParameters();
        if (parameters == null) {
            return false;
        }

        if (parameters.isList()) {
            for (Expression parameter : parameters) {
                if (parameterContainsGenericReference(parameter, genericName)) {
                    return true;
                }
            }
            return false;
        }

        return parameterContainsGenericReference(parameters.getFirst(), genericName);
    }

    private static boolean parameterContainsGenericReference(Expression parameter, String genericName) {
        if (parameter == null) {
            return false;
        }

        Type parameterType = parameter.getType();
        if (parameterType != null && parameterType.findTypeParametersInType().contains(genericName)) {
            return true;
        }

        if (parameter instanceof CastExpression castExpression) {
            Type castType = castExpression.getType();
            if (castType != null && castType.findTypeParametersInType().contains(genericName)) {
                return true;
            }
        }

        return false;
    }

    private static boolean isSourceInferredGenericHelper(MethodInvocationExpression expression) {
        String internalTypeName = expression.getInternalTypeName();
        String name = expression.getName();

        if ("org/apache/commons/lang3/function/Predicates".equals(internalTypeName) && "truePredicate".equals(name)) {
            return true;
        }
        if ("org/apache/commons/lang3/function/Suppliers".equals(internalTypeName) && "nul".equals(name)) {
            return true;
        }
        if ("java/util/Arrays".equals(internalTypeName) && "asList".equals(name)) {
            return true;
        }
        if ("java/util/Collections".equals(internalTypeName) && "emptyList".equals(name)) {
            return true;
        }
        if ("java/util/Collections".equals(internalTypeName) && "emptySet".equals(name)) {
            return true;
        }
        if ("org/apache/commons/lang3/ArrayUtils".equals(internalTypeName) && "newInstance".equals(name)) {
            return true;
        }
        return "java/util/Objects".equals(internalTypeName) && "requireNonNull".equals(name);
    }

    private static boolean shouldRemoveParameterizedSuperBridgeResultCast(
            CastExpression castExpression,
            ClassFileMethodInvocationExpression methodInvocationExpression) {
        Expression receiver = methodInvocationExpression.getExpression();
        if (receiver == null
                || !(castExpression.getType() instanceof ObjectType castObjectType)
                || (!receiver.isSuperExpression()
                && !(receiver instanceof org.jd.core.v1.model.javasyntax.expression.QualifiedSuperExpression))) {
            return false;
        }

        RawDescriptorType descriptorReturnType = parseRawDescriptorReturnType(methodInvocationExpression.getDescriptor());
        return castExpression.isByteCodeCheckCast()
                && (hasTypeParameters(methodInvocationExpression.getUnboundType())
                || hasTypeParameters(methodInvocationExpression.getType()))
                && descriptorReturnType != null
                && descriptorReturnType.internalName() != null
                && !descriptorReturnType.internalName().equals(castObjectType.getInternalName());
    }

    private static boolean requiresDescriptorBridgeResultCast(
            CastExpression castExpression,
            ClassFileMethodInvocationExpression methodInvocationExpression) {
        RawDescriptorType descriptorReturnType = parseRawDescriptorReturnType(methodInvocationExpression.getDescriptor());
        Type expressionType = methodInvocationExpression.getType();

        if (descriptorReturnType == null || expressionType == null) {
            return false;
        }

        Type castType = castExpression.getType();
        if (castType.isObjectType() && expressionType.isObjectType()) {
            return descriptorReturnType.internalName() != null
                    && !"java/lang/Object".equals(descriptorReturnType.internalName())
                    && !(descriptorReturnType.dimension() > 0 && "java/lang/Object".equals(descriptorReturnType.internalName()))
                    && ((ObjectType) castType).rawEquals((ObjectType) expressionType)
                    && !descriptorReturnType.internalName().equals(((ObjectType) expressionType).getInternalName());
        }

        return castType.getDimension() > 0
                && castType.getDimension() == expressionType.getDimension()
                && descriptorReturnType.dimension() > 0
                && descriptorReturnType.dimension() != expressionType.getDimension();
    }

    private static boolean shouldPreserveReflectiveResultCast(
            CastExpression castExpression,
            ClassFileMethodInvocationExpression methodInvocationExpression) {
        if (!(castExpression.getType() instanceof ObjectType)) {
            return false;
        }

        String name = methodInvocationExpression.getName();
        return "newInstance".equals(name)
                || ("invoke".equals(name) && "java/lang/reflect/Method".equals(methodInvocationExpression.getInternalTypeName()))
                || ("forName".equals(name) && "java/lang/Class".equals(methodInvocationExpression.getInternalTypeName()));
    }

    private static RawDescriptorType parseRawDescriptorReturnType(String descriptor) {
        if (descriptor == null) {
            return null;
        }

        int index = descriptor.indexOf(')');
        if (index < 0 || index + 1 >= descriptor.length()) {
            return null;
        }

        String returnDescriptor = descriptor.substring(index + 1);
        if ("V".equals(returnDescriptor)) {
            return null;
        }

        int dimension = 0;
        while (dimension < returnDescriptor.length() && returnDescriptor.charAt(dimension) == '[') {
            dimension++;
        }

        int typeIndex = dimension;
        if (typeIndex >= returnDescriptor.length()) {
            return null;
        }

        if (returnDescriptor.charAt(typeIndex) == 'L') {
            return new RawDescriptorType(returnDescriptor.substring(typeIndex + 1, returnDescriptor.length() - 1), dimension);
        }

        return new RawDescriptorType(null, dimension);
    }

    private record RawDescriptorType(String internalName, int dimension) {
    }

    private static boolean isArrayConstructorToArrayCast(Type castType, Expression nestedExpression) {
        if (castType == null || nestedExpression == null || !nestedExpression.isMethodInvocationExpression() || !"toArray".equals(nestedExpression.getName())) {
            return false;
        }
        BaseExpression parameters = nestedExpression.getParameters();
        if (parameters == null || parameters.size() != 1) {
            return false;
        }
        Expression parameter = parameters.isList() ? parameters.getFirst() : parameters.getFirst();
        if (!(parameter instanceof MethodReferenceExpression methodReference) || !"new".equals(methodReference.getName())) {
            return false;
        }
        String castDescriptor = castType.getDescriptor();
        if (castDescriptor == null || !castDescriptor.equals(methodReference.getInternalTypeName())) {
            return false;
        }
        Expression targetExpression = methodReference.getExpression();
        if (targetExpression == null || !targetExpression.isObjectTypeReferenceExpression()) {
            return false;
        }
        ObjectType targetType = targetExpression.getObjectType();
        return targetType != null
                && targetType.getDimension() > 0
                && castDescriptor.equals(targetType.getDescriptor());
    }

    private static boolean isTypedArrayToArrayCast(Type castType, Expression nestedExpression) {
        if (castType == null
                || nestedExpression == null
                || !nestedExpression.isMethodInvocationExpression()
                || !"toArray".equals(nestedExpression.getName())
                || castType.getDimension() == 0) {
            return false;
        }

        BaseExpression parameters = nestedExpression.getParameters();
        if (parameters == null || parameters.size() != 1) {
            return false;
        }

        Expression parameter = parameters.isList() ? parameters.getFirst() : parameters.getFirst();
        Type parameterType = parameter == null ? null : parameter.getType();
        if (parameterType == null || parameterType.getDimension() != castType.getDimension()) {
            return false;
        }
        if (castType.equals(parameterType)) {
            return true;
        }
        return castType.isObjectType()
                && parameterType.isObjectType()
                && ((ObjectType) castType).rawEquals((ObjectType) parameterType);
    }

    private static boolean shouldPreserveTypedArrayToArrayCast(Type castType, Expression nestedExpression) {
        if (!(castType instanceof ObjectType castObjectType)
                || !(nestedExpression instanceof ClassFileMethodInvocationExpression methodInvocationExpression)
                || !isTypedArrayToArrayCast(castType, nestedExpression)
                || isRawJavaLangObjectArray(castObjectType)
                || !(methodInvocationExpression.getType() instanceof ObjectType expressionObjectType)) {
            return false;
        }

        return isRawJavaLangObjectArray(expressionObjectType)
                || (sameRawArrayType(castObjectType, expressionObjectType)
                && (hasTypeParameters(castObjectType) || hasUnboundedWildcardTypeArgument(expressionObjectType)));
    }

    private static boolean requiresTypedArrayResultCastForRawReceiver(ObjectType castType, ClassFileMethodInvocationExpression methodInvocationExpression) {
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

        return isRawJavaLangObjectArray(declaredParameterType)
                && unboundParameterType instanceof GenericType genericType
                && genericType.getDimension() == castType.getDimension();
    }

    private static boolean hasTypeParameters(Type type) {
        return type != null && (type.isGenericType() || !type.findTypeParametersInType().isEmpty());
    }

    private static boolean hasTypeParameters(BaseType baseType) {
        if (baseType == null) {
            return false;
        }
        for (Type type : baseType) {
            if (hasTypeParameters(type)) {
                return true;
            }
        }
        return false;
    }

    private static boolean hasGenericArrayInvocationShape(ClassFileMethodInvocationExpression methodInvocationExpression) {
        if (hasTypeParameters(methodInvocationExpression.getUnboundType())
                || hasTypeParameters(methodInvocationExpression.getType())
                || methodInvocationExpression.getTypeParameters() != null
                || hasTypeParameters(methodInvocationExpression.getParameterTypes())
                || hasTypeParameters(methodInvocationExpression.getUnboundParameterTypes())) {
            return true;
        }

        BaseExpression parameters = methodInvocationExpression.getParameters();
        if (parameters == null) {
            return false;
        }

        for (Expression parameter : parameters) {
            Expression coreParameter = parameter instanceof CastExpression castExpression
                    ? castExpression.getExpression()
                    : parameter;
            if (coreParameter instanceof LambdaIdentifiersExpression
                    || coreParameter instanceof MethodReferenceExpression
                    || (coreParameter.getType() != null && hasTypeParameters(coreParameter.getType()))) {
                return true;
            }
        }
        return false;
    }

    private static boolean hasUnboundedWildcardTypeArgument(Type type) {
        return type instanceof ObjectType objectType && hasUnboundedWildcardTypeArgument(objectType.getTypeArguments());
    }

    private static boolean hasUnboundedWildcardTypeArgument(BaseTypeArgument typeArgument) {
        if (typeArgument == null) {
            return false;
        }
        if (typeArgument.isWildcardTypeArgument()
                || typeArgument.isWildcardExtendsTypeArgument()
                || typeArgument.isWildcardSuperTypeArgument()) {
            return true;
        }
        if (typeArgument.isTypeArgumentList()) {
            for (TypeArgument argument : typeArgument.getTypeArgumentList()) {
                if (argument != null
                        && (argument.isWildcardTypeArgument()
                        || argument.isWildcardExtendsTypeArgument()
                        || argument.isWildcardSuperTypeArgument())) {
                    return true;
                }
            }
        }
        return false;
    }

    private static boolean isPrimitiveWrapper(ObjectType objectType) {
        String internalName = objectType.getInternalName();
        return "java/lang/Byte".equals(internalName)
                || "java/lang/Short".equals(internalName)
                || "java/lang/Character".equals(internalName)
                || "java/lang/Integer".equals(internalName)
                || "java/lang/Long".equals(internalName)
                || "java/lang/Float".equals(internalName)
                || "java/lang/Double".equals(internalName)
                || "java/lang/Boolean".equals(internalName);
    }

    private static boolean containsThrowableTypeArgument(ObjectType objectType) {
        BaseTypeArgument typeArguments = objectType.getTypeArguments();
        if (typeArguments == null) {
            return false;
        }
        if (typeArguments.isTypeArgumentList()) {
            for (TypeArgument typeArgument : typeArguments.getTypeArgumentList()) {
                if (containsThrowableTypeArgument(typeArgument)) {
                    return true;
                }
            }
            return false;
        }
        return containsThrowableTypeArgument(typeArguments.getTypeArgumentFirst());
    }

    private static boolean containsThrowableTypeArgument(TypeArgument typeArgument) {
        if (typeArgument instanceof ObjectType objectType) {
            return "java/lang/Throwable".equals(objectType.getInternalName()) || containsThrowableTypeArgument(objectType);
        }
        if (typeArgument instanceof WildcardExtendsTypeArgument wildcardExtendsTypeArgument) {
            return containsThrowableTypeArgument((TypeArgument) wildcardExtendsTypeArgument.type());
        }
        if (typeArgument instanceof WildcardSuperTypeArgument wildcardSuperTypeArgument) {
            return containsThrowableTypeArgument((TypeArgument) wildcardSuperTypeArgument.type());
        }
        return false;
    }

    private static boolean isJavaLangObjectArray(Type type) {
        return type instanceof ObjectType objectType
                && objectType.getDimension() > 0
                && "java/lang/Object".equals(objectType.getInternalName());
    }
}
