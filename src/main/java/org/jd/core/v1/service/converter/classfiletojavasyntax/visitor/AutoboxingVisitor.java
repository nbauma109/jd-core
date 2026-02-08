/*
 * Copyright (c) 2008, 2019 Emmanuel Dupuy.
 * This project is distributed under the GPLv3 license.
 * This is a Copyleft license that gives the user the right to use,
 * copy and modify the code freely for non-commercial purposes.
 */

package org.jd.core.v1.service.converter.classfiletojavasyntax.visitor;

import org.jd.core.v1.model.javasyntax.declaration.BodyDeclaration;
import org.jd.core.v1.model.javasyntax.expression.CastExpression;
import org.jd.core.v1.model.javasyntax.expression.ConstructorInvocationExpression;
import org.jd.core.v1.model.javasyntax.expression.Expression;
import org.jd.core.v1.model.javasyntax.expression.MethodInvocationExpression;
import org.jd.core.v1.model.javasyntax.type.BaseType;
import org.jd.core.v1.model.javasyntax.type.ObjectType;
import org.jd.core.v1.model.javasyntax.type.Type;
import org.jd.core.v1.service.converter.classfiletojavasyntax.model.javasyntax.declaration.ClassFileBodyDeclaration;
import org.jd.core.v1.service.converter.classfiletojavasyntax.model.javasyntax.expression.ClassFileMethodInvocationExpression;
import org.jd.core.v1.util.StringConstants;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

import static org.apache.bcel.Const.MAJOR_1_5;

public class AutoboxingVisitor extends AbstractUpdateExpressionVisitor {
    protected static final Map<String, String> VALUEOF_DESCRIPTOR_MAP = new HashMap<>();

    protected static final Map<String, String> VALUE_DESCRIPTOR_MAP = new HashMap<>();
    protected static final Map<String, String> VALUE_METHODNAME_MAP = new HashMap<>();

    static {
        VALUEOF_DESCRIPTOR_MAP.put(StringConstants.JAVA_LANG_BYTE, "(B)Ljava/lang/Byte;");
        VALUEOF_DESCRIPTOR_MAP.put(StringConstants.JAVA_LANG_CHARACTER, "(C)Ljava/lang/Character;");
        VALUEOF_DESCRIPTOR_MAP.put(StringConstants.JAVA_LANG_FLOAT, "(F)Ljava/lang/Float;");
        VALUEOF_DESCRIPTOR_MAP.put(StringConstants.JAVA_LANG_INTEGER, "(I)Ljava/lang/Integer;");
        VALUEOF_DESCRIPTOR_MAP.put(StringConstants.JAVA_LANG_LONG, "(J)Ljava/lang/Long;");
        VALUEOF_DESCRIPTOR_MAP.put(StringConstants.JAVA_LANG_SHORT, "(S)Ljava/lang/Short;");
        VALUEOF_DESCRIPTOR_MAP.put(StringConstants.JAVA_LANG_DOUBLE, "(D)Ljava/lang/Double;");
        VALUEOF_DESCRIPTOR_MAP.put(StringConstants.JAVA_LANG_BOOLEAN, "(Z)Ljava/lang/Boolean;");

        VALUE_DESCRIPTOR_MAP.put(StringConstants.JAVA_LANG_BYTE, "()B");
        VALUE_DESCRIPTOR_MAP.put(StringConstants.JAVA_LANG_CHARACTER, "()C");
        VALUE_DESCRIPTOR_MAP.put(StringConstants.JAVA_LANG_FLOAT, "()F");
        VALUE_DESCRIPTOR_MAP.put(StringConstants.JAVA_LANG_INTEGER, "()I");
        VALUE_DESCRIPTOR_MAP.put(StringConstants.JAVA_LANG_LONG, "()J");
        VALUE_DESCRIPTOR_MAP.put(StringConstants.JAVA_LANG_SHORT, "()S");
        VALUE_DESCRIPTOR_MAP.put(StringConstants.JAVA_LANG_DOUBLE, "()D");
        VALUE_DESCRIPTOR_MAP.put(StringConstants.JAVA_LANG_BOOLEAN, "()Z");

        VALUE_METHODNAME_MAP.put(StringConstants.JAVA_LANG_BYTE, "byteValue");
        VALUE_METHODNAME_MAP.put(StringConstants.JAVA_LANG_CHARACTER, "charValue");
        VALUE_METHODNAME_MAP.put(StringConstants.JAVA_LANG_FLOAT, "floatValue");
        VALUE_METHODNAME_MAP.put(StringConstants.JAVA_LANG_INTEGER, "intValue");
        VALUE_METHODNAME_MAP.put(StringConstants.JAVA_LANG_LONG, "longValue");
        VALUE_METHODNAME_MAP.put(StringConstants.JAVA_LANG_SHORT, "shortValue");
        VALUE_METHODNAME_MAP.put(StringConstants.JAVA_LANG_DOUBLE, "doubleValue");
        VALUE_METHODNAME_MAP.put(StringConstants.JAVA_LANG_BOOLEAN, "booleanValue");
    }

    private String currentInternalTypeName;
    private String currentMethodName;

    @Override
    public void visit(BodyDeclaration declaration) {
        ClassFileBodyDeclaration cfbd = (ClassFileBodyDeclaration)declaration;
        boolean autoBoxingSupported = cfbd.getClassFile().getMajorVersion() >= MAJOR_1_5;

        if (autoBoxingSupported) {
            safeAccept(declaration.getMemberDeclarations());
        }
    }

    @Override
    public void visit(org.jd.core.v1.model.javasyntax.declaration.MethodDeclaration declaration) {
        String previousInternalTypeName = currentInternalTypeName;
        String previousMethodName = currentMethodName;

        currentMethodName = declaration.getName();
        if (declaration instanceof org.jd.core.v1.service.converter.classfiletojavasyntax.model.javasyntax.declaration.ClassFileMethodDeclaration cfmd) {
            currentInternalTypeName = cfmd.getBodyDeclaration().getInternalTypeName();
        }

        super.visit(declaration);

        currentInternalTypeName = previousInternalTypeName;
        currentMethodName = previousMethodName;
    }

    @Override
    protected Expression updateExpression(Expression expression) {
        if (isJavaLangMethodInvocation(expression)) {

            if (expression.getExpression().isObjectTypeReferenceExpression()) {
                // static method invocation
                if (isBoxingMethod(expression))
                {
                    return expression.getParameters().getFirst();
                }
            } else // non-static method invocation
            if (isUnboxingMethod(expression))
            {
                return expression.getExpression();
            }
        }

        return expression;
    }

    public static boolean isJavaLangMethodInvocation(Expression expression) {
        return expression != null && expression.isMethodInvocationExpression() && expression.getInternalTypeName().startsWith("java/lang/");
    }

    public static boolean isBoxingMethod(Expression expression) {
        int parameterSize = expression.getParameters() == null ? 0 : expression.getParameters().size();
        String descriptor = VALUEOF_DESCRIPTOR_MAP.get(expression.getInternalTypeName());
        if (parameterSize == 1 &&
                "valueOf".equals(expression.getName()) &&
                expression.getDescriptor().equals(descriptor) &&
                expression instanceof ClassFileMethodInvocationExpression mie) {
            Type expressionType = expression.getParameters().getFirst().getType();
            BaseType parameterTypes = mie.getParameterTypes();
            return Objects.equals(parameterTypes, expressionType);
        }
        return false;
    }

    public static boolean isUnboxingMethod(Expression expression) {
        int parameterSize = expression.getParameters() == null ? 0 : expression.getParameters().size();
        return parameterSize == 0 &&
                expression.getName().equals(VALUE_METHODNAME_MAP.get(expression.getInternalTypeName())) &&
                expression.getDescriptor().equals(VALUE_DESCRIPTOR_MAP.get(expression.getInternalTypeName()));
    }

    @Override
    protected void maybeUpdateParameters(ConstructorInvocationExpression expression) {
        // disable (un)boxing due to possible constructor overloading
    }

    @Override
    protected void maybeUpdateParameters(MethodInvocationExpression expression) {
        BaseType parameterTypes = expression instanceof ClassFileMethodInvocationExpression mie ? mie.getParameterTypes() : null;
        BaseType unboundParameterTypes = expression instanceof ClassFileMethodInvocationExpression mie ? mie.getUnboundParameterTypes() : null;

        if (parameterTypes == null || expression.getParameters() == null) {
            expression.setParameters(updateBaseExpression(expression.getParameters()));
            return;
        }

        boolean isSelfOverload = isSelfOverload(expression);

        if (parameterTypes.isList() && expression.getParameters().isList()) {
            for (int i = 0; i < expression.getParameters().getList().size() && i < parameterTypes.getList().size(); i++) {
                Type parameterType = parameterTypes.getList().get(i);
                Type unboundParameterType = unboundParameterTypes != null && unboundParameterTypes.isList() && i < unboundParameterTypes.getList().size()
                        ? unboundParameterTypes.getList().get(i)
                        : null;
                Expression argument = expression.getParameters().getList().get(i);
                if (isSelfOverload && isBoxingOrUnboxing(expression.getParameters().getList().get(i))) {
                    continue;
                }
                if (shouldPreserveBoxing(expression, i, parameterType, unboundParameterType, argument)) {
                    continue;
                }
                if (shouldUpdateParameter(parameterType, argument)) {
                    expression.getParameters().getList().set(i, updateExpression(argument));
                }
            }
        } else if ((!isSelfOverload || !isBoxingOrUnboxing(expression.getParameters().getFirst()))
                && !shouldPreserveBoxing(expression, 0, parameterTypes.getFirst(), unboundParameterTypes == null ? null : unboundParameterTypes.getFirst(), expression.getParameters().getFirst())
                && shouldUpdateParameter(parameterTypes.getFirst(), expression.getParameters().getFirst())) {
            expression.setParameters(updateExpression(expression.getParameters().getFirst()));
        }
    }

    private static boolean shouldUpdateParameter(Type parameterType, Expression argument) {
        if (parameterType == null) {
            return true;
        }
        if (parameterType.isPrimitiveType() && isJavaLangMethodInvocation(argument) && isBoxingMethod(argument)) {
            return false;
        }
        if (parameterType.isGenericType()) {
            return false;
        }
        if (parameterType.isObjectType()) {
            return !ObjectType.TYPE_OBJECT.equals(parameterType);
        }
        return true;
    }

    private static boolean shouldPreserveBoxing(MethodInvocationExpression expression, int parameterIndex, Type parameterType, Type unboundParameterType, Expression argument) {
        if (!isJavaLangMethodInvocation(argument) || !isBoxingMethod(argument)) {
            return false;
        }
        if (!(parameterType instanceof ObjectType objectType)) {
            return false;
        }
        boolean boxedPrimitiveParameter = switch (objectType.getInternalName()) {
            case StringConstants.JAVA_LANG_BYTE, StringConstants.JAVA_LANG_SHORT, StringConstants.JAVA_LANG_INTEGER,
                    StringConstants.JAVA_LANG_LONG, StringConstants.JAVA_LANG_FLOAT, StringConstants.JAVA_LANG_DOUBLE,
                    StringConstants.JAVA_LANG_CHARACTER, StringConstants.JAVA_LANG_BOOLEAN -> true;
            default -> false;
        };
        if (!boxedPrimitiveParameter) {
            return false;
        }
        if (unboundParameterType != null && unboundParameterType.isGenericType()) {
            return true;
        }
        if (hasNullArgument(expression)) {
            return true;
        }
        return isObjectDescriptorParameter(expression, parameterIndex);
    }

    private static boolean hasNullArgument(MethodInvocationExpression expression) {
        if (expression == null || expression.getParameters() == null) {
            return false;
        }
        if (expression.getParameters().isList()) {
            for (Expression parameter : expression.getParameters().getList()) {
                if (isNullLike(parameter)) {
                    return true;
                }
            }
            return false;
        }
        Expression parameter = expression.getParameters().getFirst();
        return isNullLike(parameter);
    }

    private static boolean isNullLike(Expression expression) {
        if (expression == null) {
            return false;
        }
        if (expression.isNullExpression()) {
            return true;
        }
        if (expression instanceof CastExpression castExpression) {
            return isNullLike(castExpression.getExpression());
        }
        return false;
    }

    private static boolean isObjectDescriptorParameter(MethodInvocationExpression expression, int parameterIndex) {
        String descriptor = expression.getDescriptor();
        if (descriptor == null || descriptor.length() < 2 || descriptor.charAt(0) != '(' || parameterIndex < 0) {
            return false;
        }

        int currentParameterIndex = 0;
        int i = 1;
        while (i < descriptor.length() && descriptor.charAt(i) != ')') {
            char c = descriptor.charAt(i);

            while (c == '[') {
                i++;
                if (i >= descriptor.length()) {
                    return false;
                }
                c = descriptor.charAt(i);
            }

            if (c == 'L') {
                int semiColonIndex = descriptor.indexOf(';', i);
                if (semiColonIndex == -1) {
                    return false;
                }
                if (currentParameterIndex == parameterIndex) {
                    return "Ljava/lang/Object;".equals(descriptor.substring(i, semiColonIndex + 1));
                }
                i = semiColonIndex + 1;
            } else {
                if (currentParameterIndex == parameterIndex) {
                    return false;
                }
                i++;
            }

            currentParameterIndex++;
        }

        return false;
    }

    private static boolean isBoxingOrUnboxing(Expression expression) {
        if (!isJavaLangMethodInvocation(expression)) {
            return false;
        }
        return isBoxingMethod(expression) || isUnboxingMethod(expression);
    }

    private boolean isSelfOverload(MethodInvocationExpression expression) {
        if (currentInternalTypeName == null || currentMethodName == null) {
            return false;
        }
        return currentMethodName.equals(expression.getName())
                && currentInternalTypeName.equals(expression.getInternalTypeName());
    }
}
