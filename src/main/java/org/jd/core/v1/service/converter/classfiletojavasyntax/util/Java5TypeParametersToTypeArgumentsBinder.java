/*
 * Copyright (c) 2008, 2019 Emmanuel Dupuy.
 * This project is distributed under the GPLv3 license.
 * This is a Copyleft license that gives the user the right to use,
 * copy and modify the code freely for non-commercial purposes.
 */
package org.jd.core.v1.service.converter.classfiletojavasyntax.util;

import org.apache.bcel.Const;
import org.jd.core.v1.model.javasyntax.expression.AbstractNopExpressionVisitor;
import org.jd.core.v1.model.javasyntax.expression.ArrayExpression;
import org.jd.core.v1.model.javasyntax.expression.BaseExpression;
import org.jd.core.v1.model.javasyntax.expression.BinaryOperatorExpression;
import org.jd.core.v1.model.javasyntax.expression.BooleanExpression;
import org.jd.core.v1.model.javasyntax.expression.CastExpression;
import org.jd.core.v1.model.javasyntax.expression.CommentExpression;
import org.jd.core.v1.model.javasyntax.expression.ConstructorInvocationExpression;
import org.jd.core.v1.model.javasyntax.expression.ConstructorReferenceExpression;
import org.jd.core.v1.model.javasyntax.expression.DoubleConstantExpression;
import org.jd.core.v1.model.javasyntax.expression.EnumConstantReferenceExpression;
import org.jd.core.v1.model.javasyntax.expression.Expression;
import org.jd.core.v1.model.javasyntax.expression.ExpressionVisitor;
import org.jd.core.v1.model.javasyntax.expression.Expressions;
import org.jd.core.v1.model.javasyntax.expression.FieldReferenceExpression;
import org.jd.core.v1.model.javasyntax.expression.FloatConstantExpression;
import org.jd.core.v1.model.javasyntax.expression.InstanceOfExpression;
import org.jd.core.v1.model.javasyntax.expression.IntegerConstantExpression;
import org.jd.core.v1.model.javasyntax.expression.LambdaFormalParametersExpression;
import org.jd.core.v1.model.javasyntax.expression.LambdaIdentifiersExpression;
import org.jd.core.v1.model.javasyntax.expression.LengthExpression;
import org.jd.core.v1.model.javasyntax.expression.LocalVariableReferenceExpression;
import org.jd.core.v1.model.javasyntax.expression.LongConstantExpression;
import org.jd.core.v1.model.javasyntax.expression.MethodInvocationExpression;
import org.jd.core.v1.model.javasyntax.expression.MethodReferenceExpression;
import org.jd.core.v1.model.javasyntax.expression.NewArray;
import org.jd.core.v1.model.javasyntax.expression.NewExpression;
import org.jd.core.v1.model.javasyntax.expression.NewInitializedArray;
import org.jd.core.v1.model.javasyntax.expression.NoExpression;
import org.jd.core.v1.model.javasyntax.expression.NullExpression;
import org.jd.core.v1.model.javasyntax.expression.ObjectTypeReferenceExpression;
import org.jd.core.v1.model.javasyntax.expression.ParenthesesExpression;
import org.jd.core.v1.model.javasyntax.expression.PostOperatorExpression;
import org.jd.core.v1.model.javasyntax.expression.PreOperatorExpression;
import org.jd.core.v1.model.javasyntax.expression.QualifiedSuperExpression;
import org.jd.core.v1.model.javasyntax.expression.StringConstantExpression;
import org.jd.core.v1.model.javasyntax.expression.SuperConstructorInvocationExpression;
import org.jd.core.v1.model.javasyntax.expression.SuperExpression;
import org.jd.core.v1.model.javasyntax.expression.TernaryOperatorExpression;
import org.jd.core.v1.model.javasyntax.expression.ThisExpression;
import org.jd.core.v1.model.javasyntax.expression.TypeReferenceDotClassExpression;
import org.jd.core.v1.model.javasyntax.type.BaseType;
import org.jd.core.v1.model.javasyntax.type.BaseTypeArgument;
import org.jd.core.v1.model.javasyntax.type.BaseTypeParameter;
import org.jd.core.v1.model.javasyntax.type.GenericType;
import org.jd.core.v1.model.javasyntax.type.ObjectType;
import org.jd.core.v1.model.javasyntax.type.Type;
import org.jd.core.v1.model.javasyntax.type.TypeArgument;
import org.jd.core.v1.model.javasyntax.type.TypeArguments;
import org.jd.core.v1.model.javasyntax.type.TypeParameter;
import org.jd.core.v1.model.javasyntax.type.WildcardExtendsTypeArgument;
import org.jd.core.v1.model.javasyntax.type.WildcardSuperTypeArgument;
import org.jd.core.v1.model.javasyntax.type.WildcardTypeArgument;
import org.jd.core.v1.service.converter.classfiletojavasyntax.model.javasyntax.declaration.ClassFileConstructorOrMethodDeclaration;
import org.jd.core.v1.service.converter.classfiletojavasyntax.model.javasyntax.expression.ClassFileConstructorInvocationExpression;
import org.jd.core.v1.service.converter.classfiletojavasyntax.model.javasyntax.expression.ClassFileLocalVariableReferenceExpression;
import org.jd.core.v1.service.converter.classfiletojavasyntax.model.javasyntax.expression.ClassFileMethodInvocationExpression;
import org.jd.core.v1.service.converter.classfiletojavasyntax.model.javasyntax.expression.ClassFileNewExpression;
import org.jd.core.v1.service.converter.classfiletojavasyntax.model.javasyntax.expression.ClassFileSuperConstructorInvocationExpression;
import org.jd.core.v1.service.converter.classfiletojavasyntax.model.localvariable.AbstractLocalVariable;
import org.jd.core.v1.service.converter.classfiletojavasyntax.util.TypeMaker.TypeTypes;
import org.jd.core.v1.service.converter.classfiletojavasyntax.visitor.AutoboxingVisitor;
import org.jd.core.v1.service.converter.classfiletojavasyntax.visitor.BaseTypeToTypeArgumentVisitor;
import org.jd.core.v1.service.converter.classfiletojavasyntax.visitor.BindTypeParametersToNonWildcardTypeArgumentsVisitor;
import org.jd.core.v1.service.converter.classfiletojavasyntax.visitor.BindTypesToTypesVisitor;
import org.jd.core.v1.service.converter.classfiletojavasyntax.visitor.GetTypeArgumentVisitor;
import org.jd.core.v1.service.converter.classfiletojavasyntax.visitor.PopulateBindingsWithTypeArgumentVisitor;
import org.jd.core.v1.service.converter.classfiletojavasyntax.visitor.PopulateBindingsWithTypeParameterVisitor;
import org.jd.core.v1.service.converter.classfiletojavasyntax.visitor.SearchInTypeArgumentVisitor;
import org.jd.core.v1.service.converter.classfiletojavasyntax.visitor.TypeArgumentToTypeVisitor;
import org.jd.core.v1.util.StringConstants;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

import static org.apache.bcel.Const.ACC_STATIC;
import static org.jd.core.v1.model.javasyntax.type.ObjectType.TYPE_OBJECT;
import static org.jd.core.v1.model.javasyntax.type.ObjectType.TYPE_STRING;
import static org.jd.core.v1.model.javasyntax.type.ObjectType.TYPE_UNDEFINED_OBJECT;

public final class Java5TypeParametersToTypeArgumentsBinder extends AbstractTypeParametersToTypeArgumentsBinder implements ExpressionVisitor {
    private final RemoveNonWildcardTypeArgumentsVisitor removeNonWildcardTypeArgumentsVisitor = new RemoveNonWildcardTypeArgumentsVisitor();

    private final PopulateBindingsWithTypeParameterVisitor populateBindingsWithTypeParameterVisitor = new PopulateBindingsWithTypeParameterVisitor();
    private final BindTypesToTypesVisitor bindTypesToTypesVisitor = new BindTypesToTypesVisitor();
    private final SearchInTypeArgumentVisitor searchInTypeArgumentVisitor = new SearchInTypeArgumentVisitor();
    private final TypeArgumentToTypeVisitor typeArgumentToTypeVisitor = new TypeArgumentToTypeVisitor();
    private final BaseTypeToTypeArgumentVisitor baseTypeToTypeArgumentVisitor = new BaseTypeToTypeArgumentVisitor();
    private final GetTypeArgumentVisitor getTypeArgumentVisitor = new GetTypeArgumentVisitor();
    private final BindTypeParametersToNonWildcardTypeArgumentsVisitor bindTypeParametersToNonWildcardTypeArgumentsVisitor = new BindTypeParametersToNonWildcardTypeArgumentsVisitor();

    private final TypeMaker typeMaker;
    private final String internalTypeName;
    private final boolean staticMethod;
    private final PopulateBindingsWithTypeArgumentVisitor populateBindingsWithTypeArgumentVisitor;
    private final Map<String, TypeArgument> contextualBindings;
    private final Map<String, BaseType> contextualTypeBounds;

    private boolean parametersFirst;
    private boolean omitNonWildcardTypeArguments;

    public Java5TypeParametersToTypeArgumentsBinder(TypeMaker typeMaker, String internalTypeName, ClassFileConstructorOrMethodDeclaration comd) {
        this.typeMaker = typeMaker;
        this.internalTypeName = internalTypeName;
        this.staticMethod = (comd.getFlags() & ACC_STATIC) != 0;
        this.populateBindingsWithTypeArgumentVisitor = new PopulateBindingsWithTypeArgumentVisitor(typeMaker);
        this.contextualBindings = comd.getBindings();
        this.contextualTypeBounds = comd.getTypeBounds();
        this.omitNonWildcardTypeArguments = comd.getClassFile().getMajorVersion() > Const.MAJOR_1_7;
    }

    @Override
    public ClassFileConstructorInvocationExpression newConstructorInvocationExpression(
            int lineNumber, ObjectType objectType, String descriptor,
            TypeMaker.MethodTypes methodTypes, BaseExpression parameters) {
        Map<String, TypeArgument> bindings = new HashMap<>();
        BaseType parameterTypes = clone(methodTypes.getParameterTypes());
        BaseTypeParameter methodTypeParameters = methodTypes.getTypeParameters();
        BaseType exceptionTypes = methodTypes.getExceptionTypes();

        Map<String, BaseType> typeBounds = new HashMap<>();
        populateBindings(bindings, null, exceptionTypes, null, null, methodTypeParameters, TYPE_OBJECT, null, null, null, typeBounds);

        parameterTypes = bind(bindings, parameterTypes);
        bindParameters(parameterTypes, parameters);

        return new ClassFileConstructorInvocationExpression(lineNumber, objectType, descriptor, parameterTypes, parameters, methodTypes.isVarArgs());
    }

    @Override
    public ClassFileSuperConstructorInvocationExpression newSuperConstructorInvocationExpression(
            int lineNumber, ObjectType objectType, String descriptor,
            TypeMaker.MethodTypes methodTypes, BaseExpression parameters) {
        BaseType parameterTypes = clone(methodTypes.getParameterTypes());
        Map<String, TypeArgument> bindings = contextualBindings;
        TypeMaker.TypeTypes typeTypes = typeMaker.makeTypeTypes(internalTypeName);

        if (typeTypes != null && typeTypes.getSuperType() != null && typeTypes.getSuperType().getTypeArguments() != null) {
            TypeMaker.TypeTypes superTypeTypes = typeMaker.makeTypeTypes(objectType.getInternalName());

            if (superTypeTypes != null) {
                bindings = new HashMap<>();
                BaseTypeParameter typeParameters = superTypeTypes.getTypeParameters();
                BaseTypeArgument typeArguments = typeTypes.getSuperType().getTypeArguments();
                BaseTypeParameter methodTypeParameters = methodTypes.getTypeParameters();
                BaseType exceptionTypes = methodTypes.getExceptionTypes();

                Map<String, BaseType> typeBounds = new HashMap<>();
                populateBindings(bindings, null, exceptionTypes, typeParameters, typeArguments, methodTypeParameters, TYPE_OBJECT, null, null, null, typeBounds);
            }
        }

        parameterTypes = bind(bindings, parameterTypes);
        bindParameters(parameterTypes, parameters);

        return new ClassFileSuperConstructorInvocationExpression(lineNumber, objectType, descriptor, parameterTypes, parameters, methodTypes.isVarArgs());
    }

    @Override
    public ClassFileMethodInvocationExpression newMethodInvocationExpression(
            int lineNumber, Expression expression, ObjectType objectType, String name, String descriptor,
            TypeMaker.MethodTypes methodTypes, BaseExpression parameters) {
        return new ClassFileMethodInvocationExpression(
            lineNumber, methodTypes.getReturnedType(), expression,
            objectType.getInternalName(), name, descriptor, parameters, methodTypes);
    }

    @Override
    public FieldReferenceExpression newFieldReferenceExpression(
            int lineNumber, Type type, Expression expression, ObjectType objectType, String name, String descriptor) {
        Type expressionType = expression.getType();

        if (expressionType.isObjectType()) {
            ObjectType expressionObjectType = (ObjectType) expressionType;

            if ((staticMethod || !expressionObjectType.getInternalName().equals(internalTypeName)) && type.isObjectType()) {
                ObjectType ot = (ObjectType) type;

                if (ot.getTypeArguments() != null) {
                    TypeMaker.TypeTypes typeTypes = typeMaker.makeTypeTypes(expressionObjectType.getInternalName());

                    if (typeTypes == null) {
                        type = (Type)bind(contextualBindings, type);
                    } else {
                        Map<String, TypeArgument> bindings = new HashMap<>();
                        BaseTypeParameter typeParameters = typeTypes.getTypeParameters();
                        BaseTypeArgument typeArguments = expressionObjectType.getTypeArguments();
                        Map<String, BaseType> typeBounds = new HashMap<>();
                        boolean partialBinding = populateBindings(bindings, expression, null, typeParameters, typeArguments, null, TYPE_OBJECT, null, null, null, typeBounds);

                        if (!partialBinding) {
                            type = (Type) bind(bindings, type);
                        }
                    }
                }
            }
        }

        return new FieldReferenceExpression(lineNumber, type, expression, objectType.getInternalName(), name, descriptor);
    }

    @Override
    public void bindParameterTypesWithArgumentTypes(Type type, Expression expression, boolean parametersFirst) {
        this.type = type;
        this.parametersFirst = parametersFirst;
        expression.accept(this);
        expression.accept(removeNonWildcardTypeArgumentsVisitor);
    }

    private Type checkTypeArguments(Type type, AbstractLocalVariable localVariable) {
        if (type.isObjectType()) {
            ObjectType objectType = (ObjectType)type;

            if (objectType.getTypeArguments() != null) {
                Type localVariableType = localVariable.getType();

                if (localVariableType.isObjectType()) {
                    ObjectType localVariableObjectType = (ObjectType)localVariableType;
                    TypeMaker.TypeTypes typeTypes = typeMaker.makeTypeTypes(localVariableObjectType.getInternalName());

                    if (typeTypes != null && typeTypes.getTypeParameters() == null) {
                        type = ((ObjectType)type).createType(null);
                    }
                }
            }
        }

        return type;
    }

    private void bindParameters(BaseType parameterTypes, BaseExpression parameters) {
        if (parameterTypes != null) {
            if (parameterTypes.isList() && parameters.isList()) {
                Iterator<Type> parameterTypesIterator = parameterTypes.iterator();
                Iterator<Expression> parametersIterator = parameters.iterator();

                Expression parameter;
                while (parametersIterator.hasNext() && parameterTypesIterator.hasNext()) {
                    parameter = parametersIterator.next();
                    this.type = parameterTypesIterator.next();
                    parameter.accept(this);
                    parameter.accept(removeNonWildcardTypeArgumentsVisitor);
                }
            } else {
                Expression parameter = parameters.getFirst();
                this.type = parameterTypes.getFirst();
                parameter.accept(this);
                parameter.accept(removeNonWildcardTypeArgumentsVisitor);
            }
        }
    }

    private boolean populateBindings(
            Map<String, TypeArgument> bindings, Expression expression, BaseType mieExceptionTypes,
            BaseTypeParameter typeParameters, BaseTypeArgument typeArguments, BaseTypeParameter methodTypeParameters,
            Type returnType, Type returnExpressionType, BaseType parameterTypes, BaseExpression parameters, Map<String, BaseType> typeBounds) {
        boolean statik = expression != null && expression.isObjectTypeReferenceExpression();

        if (!statik) {
            bindings.putAll(contextualBindings);

            if (typeParameters != null) {
                populateBindingsWithTypeParameterVisitor.init(bindings, typeBounds);
                typeParameters.accept(populateBindingsWithTypeParameterVisitor);

                if (typeArguments != null) {
                    if (typeParameters.isList() && typeArguments.isTypeArgumentList()) {
                        Iterator<TypeParameter> iteratorTypeParameter = typeParameters.iterator();
                        Iterator<TypeArgument> iteratorTypeArgument = typeArguments.getTypeArgumentList().iterator();

                        while (iteratorTypeParameter.hasNext()) {
                            bindings.put(iteratorTypeParameter.next().getIdentifier(), iteratorTypeArgument.next());
                        }
                    } else {
                        bindings.put(typeParameters.getFirst().getIdentifier(), typeArguments.getTypeArgumentFirst());
                    }
                }
            }
        }

        if (methodTypeParameters != null) {
            populateBindingsWithTypeParameterVisitor.init(bindings, typeBounds);
            methodTypeParameters.accept(populateBindingsWithTypeParameterVisitor);
        }

        if (!TYPE_OBJECT.equals(returnType) && returnExpressionType != null) {
            populateBindingsWithTypeArgumentVisitor.init(contextualTypeBounds, bindings, typeBounds, returnType);
            returnExpressionType.accept(populateBindingsWithTypeArgumentVisitor);
        }

        if (parameterTypes != null) {
            if (parameterTypes.isList() && parameters.isList()) {
                Iterator<Type> parameterTypesIterator = parameterTypes.iterator();
                Iterator<Expression> parametersIterator = parameters.iterator();

                while (parametersIterator.hasNext()) {
                    populateBindingsWithTypeArgument(bindings, typeBounds, parameterTypesIterator.next(), parametersIterator.next());
                }
            } else {
                populateBindingsWithTypeArgument(bindings, typeBounds, parameterTypes.getFirst(), parameters.getFirst());
            }
        }

        if (exceptionTypes != null && !exceptionTypes.isList() && mieExceptionTypes != null && !mieExceptionTypes.isList()) {
            Type exceptionType = exceptionTypes.getFirst();
            Type mieExceptionType = mieExceptionTypes.getFirst();
            populateBindingsWithTypeArgumentVisitor.init(contextualTypeBounds, bindings, typeBounds, exceptionType);
            mieExceptionType.accept(populateBindingsWithTypeArgumentVisitor);
        }
        
        boolean bindingsContainsNull = bindings.containsValue(null);

        if (bindingsContainsNull) {
            if (eraseTypeArguments(expression, typeParameters, typeArguments)) {
                for (Map.Entry<String, TypeArgument> entry : bindings.entrySet()) {
                    entry.setValue(null);
                }
            } else {
                for (Map.Entry<String, TypeArgument> entry : bindings.entrySet()) {
                    if (entry.getValue() == null) {
                        BaseType baseType = typeBounds.get(entry.getKey());

                        if (baseType == null) {
                            entry.setValue(WildcardTypeArgument.WILDCARD_TYPE_ARGUMENT);
                        } else {
                            bindTypesToTypesVisitor.setBindings(bindings);
                            bindTypesToTypesVisitor.init();
                            baseType.accept(bindTypesToTypesVisitor);
                            baseType = bindTypesToTypesVisitor.getType();

                            baseTypeToTypeArgumentVisitor.init();
                            baseType.accept(baseTypeToTypeArgumentVisitor);
                            entry.setValue(baseTypeToTypeArgumentVisitor.getTypeArgument());
                        }
                    }
                }
            }
        }

        return bindingsContainsNull;
    }

    private TypeArgument searchTypeFromParameters(GenericType genericType, BaseType parameterTypes, BaseExpression parameters) {
        if (parameterTypes != null) {
            if (parameterTypes.isList() && parameters.isList()) {
                Iterator<Type> parameterTypesIterator = parameterTypes.iterator();
                Iterator<Expression> parametersIterator = parameters.iterator();

                Expression parameter;
                while (parametersIterator.hasNext() && parameterTypesIterator.hasNext()) {
                    parameter = parametersIterator.next();
                    Type parameterType = parameterTypesIterator.next();
                    Set<String> parametersInType = parameterType.findTypeParametersInType();
                    if (parametersInType.contains(genericType.getName()) && parameter.getType() instanceof ObjectType) {
                        TypeArgument typeFound = searchTypeFromParameter(genericType, parameter.getType(), parameterType, parametersInType);
                        if (typeFound != null) {
                            return typeFound;
                        }
                    }
                }
            } else {
                Expression parameter = parameters.getFirst();
                Type parameterType = parameterTypes.getFirst();
                Set<String> parametersInType = parameterType.findTypeParametersInType();
                if (parametersInType.contains(genericType.getName()) && parameter.getType() instanceof ObjectType) {
                    TypeArgument typeFound = searchTypeFromParameter(genericType, parameter.getType(), parameterType, parametersInType);
                    if (typeFound != null) {
                        return typeFound;
                    }
                }
            }
        }
        return null;
    }

    private TypeArgument searchTypeFromParameter(GenericType genericType, Type parameterTypeWithArguments, Type parameterTypeWithParameters, Set<String> parametersInType) {
        if (!TYPE_OBJECT.equals(parameterTypeWithArguments) && parameterTypeWithParameters != null) {
            TypeTypes parameterTypeTypes = typeMaker.makeTypeTypes(parameterTypeWithArguments.getInternalName());
            if (parameterTypeTypes != null) {
                if (parameterTypeWithArguments.getInternalName().equals(parameterTypeWithParameters.getInternalName())) {
                    Map<String, TypeArgument> bindings = new HashMap<>();
                    for (String typeParameter : parametersInType) {
                        bindings.put(typeParameter, null);
                    }
                    PopulateBindingsWithTypeArgumentVisitor bindingVisitor = new PopulateBindingsWithTypeArgumentVisitor(typeMaker);
                    bindingVisitor.init(contextualTypeBounds, bindings, new HashMap<>(), parameterTypeWithArguments);
                    parameterTypeWithParameters.accept(bindingVisitor);
                    TypeArgument boundType = bindings.get(genericType.getName());
                    if (boundType != null) {
                        return boundType;
                    }
                }
                ObjectType parameterSuperType = parameterTypeTypes.getSuperType();
                if (parameterSuperType != null) {
                    TypeArgument typeFound = searchTypeFromParameter(genericType, parameterSuperType, parameterTypeWithParameters, parametersInType);
                    if (typeFound != null) {
                        return typeFound;
                    }
                }
                BaseType parameterInterfaces = parameterTypeTypes.getInterfaces();
                if (parameterInterfaces != null) {
                    for (Type parameterInterface : parameterInterfaces) {
                        TypeArgument typeFound = searchTypeFromParameter(genericType, parameterInterface, parameterTypeWithParameters, parametersInType);
                        if (typeFound != null) {
                            return typeFound;
                        }
                    }
                }
            }
        }
        return null;
    }

    private static boolean eraseTypeArguments(Expression expression, BaseTypeParameter typeParameters, BaseTypeArgument typeArguments) {
        if (typeParameters != null && typeArguments == null && expression != null) {
            if (expression.isCastExpression()) {
                expression = expression.getExpression();
            }
            if (expression.isFieldReferenceExpression() || expression.isMethodInvocationExpression() || expression.isLocalVariableReferenceExpression()) {
                return true;
            }
        }

        return false;
    }

    private void populateBindingsWithTypeArgument(Map<String, TypeArgument> bindings, Map<String, BaseType> typeBounds, Type type, Expression expression) {
        if (type.isGenericType() && expression.isThisExpression()) {
            // TODO FIXME find the real rule to apply
            return;
        }
        Type t = getExpressionType(expression);

        populateBindingsWithTypeArgument(bindings, typeBounds, type, t);
    }

    private void populateBindingsWithTypeArgument(Map<String, TypeArgument> bindings, Map<String, BaseType> typeBounds, Type typeWithParameters, Type typeWithArguments) {
        if (typeWithArguments != null && typeWithArguments != TYPE_UNDEFINED_OBJECT && !ObjectType.TYPE_OBJECT.equals(typeWithArguments)) {
            populateBindingsWithTypeArgumentVisitor.init(contextualTypeBounds, bindings, typeBounds, typeWithArguments);
            typeWithParameters.accept(populateBindingsWithTypeArgumentVisitor);
        }
    }

    private BaseType bind(Map<String, TypeArgument> bindings, BaseType parameterTypes) {
        if (parameterTypes != null && !bindings.isEmpty()) {
            bindTypesToTypesVisitor.setBindings(bindings);
            bindTypesToTypesVisitor.init();
            parameterTypes.accept(bindTypesToTypesVisitor);
            parameterTypes = bindTypesToTypesVisitor.getType();
        }

        return parameterTypes;
    }

    private Type getExpressionType(Expression expression) {
        if (expression.isMethodInvocationExpression()) {
            return getExpressionType((ClassFileMethodInvocationExpression)expression);
        }
        if (expression.isNewExpression()) {
            return getExpressionType((ClassFileNewExpression)expression);
        }

        return expression.getType();
    }

    private Type getExpressionType(ClassFileMethodInvocationExpression mie) {
        Type t = mie.getType();

        searchInTypeArgumentVisitor.init();
        t.accept(searchInTypeArgumentVisitor);

        if (!searchInTypeArgumentVisitor.containsGeneric()) {
            return t;
        }

        if (mie.getTypeParameters() != null) {
            return null;
        }

        if (staticMethod || !mie.getInternalTypeName().equals(internalTypeName)) {
            TypeMaker.TypeTypes typeTypes = typeMaker.makeTypeTypes(mie.getInternalTypeName());

            if (typeTypes != null && typeTypes.getTypeParameters() != null) {
                return null;
            }
        }

        return t;
    }

    private Type getExpressionType(ClassFileNewExpression ne) {
        ObjectType ot = ne.getObjectType();

        if (staticMethod || !ot.getInternalName().equals(internalTypeName)) {
            TypeMaker.TypeTypes typeTypes = typeMaker.makeTypeTypes(ot.getInternalName());

            if (typeTypes != null && typeTypes.getTypeParameters() != null) {
                return null;
            }
        }

        return ot;
    }

    private class RemoveNonWildcardTypeArgumentsVisitor extends AbstractNopExpressionVisitor {
        @Override
        public void visit(MethodInvocationExpression expression) {
            if (omitNonWildcardTypeArguments) {
                if (expression.getExpression() instanceof CastExpression ce
                        && ce.getExpression() instanceof MethodInvocationExpression mie
                        && ce.getType().getInternalName().equals(expression.getInternalTypeName())
                        && AutoboxingVisitor.isJavaLangMethodInvocation(expression)
                        && AutoboxingVisitor.isUnboxingMethod(expression)) {
                    mie.setNonWildcardTypeArguments(null);
                } else {
                    expression.setNonWildcardTypeArguments(null);
                }
            }
        }
    }

    /** --- ExpressionVisitor --- */
    private Type type;

    @Override
    public void visit(MethodInvocationExpression expression) {
        ClassFileMethodInvocationExpression mie = (ClassFileMethodInvocationExpression)expression;

        if (! mie.isBound()) {
            BaseType parameterTypes = mie.getParameterTypes();
            BaseExpression parameters = mie.getParameters();
            Expression exp = mie.getExpression();
            Type expressionType = exp.getType();

            if (parametersFirst) {
                Type typeBeforeVisitingExpression = this.type;
                exp.accept(this);
                this.type = typeBeforeVisitingExpression;
                
                Type typeBeforeBindParameters = this.type;
                bindParameters(parameterTypes, parameters);
                this.type = typeBeforeBindParameters;
            }            

            if (staticMethod || mie.getTypeParameters() != null || !mie.getInternalTypeName().equals(internalTypeName)) {
                TypeMaker.TypeTypes typeTypes = typeMaker.makeTypeTypes(mie.getInternalTypeName());

                if (typeTypes != null) {
                    BaseTypeParameter typeParameters = typeTypes.getTypeParameters();
                    BaseTypeParameter methodTypeParameters = mie.getTypeParameters();
                    BaseTypeArgument typeArguments = null;

                    if (exp.isSuperExpression()) {
                        typeTypes = typeMaker.makeTypeTypes(internalTypeName);
                        typeArguments = typeTypes == null || typeTypes.getSuperType() == null ? null : typeTypes.getSuperType().getTypeArguments();
                    } else if (exp.isMethodInvocationExpression()) {
                        Type t = getExpressionType((ClassFileMethodInvocationExpression) exp);
                        if (t != null && t.isObjectType()) {
                            typeArguments = ((ObjectType)t).getTypeArguments();
                        }
                    } else if (expressionType.isGenericType()) {
                        BaseType typeBound = contextualTypeBounds.get(expressionType.getName());

                        if (typeBound != null) {
                            getTypeArgumentVisitor.init();
                            typeBound.accept(getTypeArgumentVisitor);
                            typeArguments = getTypeArgumentVisitor.getTypeArguments();
                        }
                    } else if (expressionType instanceof ObjectType) { // to convert to jdk16 pattern matching only when spotbugs #1617 and eclipse #577987 are solved
                        ObjectType ot = (ObjectType) expressionType;
                        typeArguments = ot.getTypeArguments();
                    }

                    Type t = mie.getType();

                    boolean wildcardSuperOrExtends = false;
                    if (type.isObjectType()) {
                        ObjectType objectType = (ObjectType) type;
                        if (t.isObjectType()) {
                            ObjectType mieTypeObjectType = (ObjectType) t;
                            t = typeMaker.searchSuperParameterizedType(objectType, mieTypeObjectType);
                            if (t == null) {
                                t = mie.getType();
                            }
                        }
                        if (t.isGenericType()) {
                            GenericType mieTypeGenericType = (GenericType) t;
                            TypeArgument typeArgument = searchTypeFromParameters(mieTypeGenericType, parameterTypes, parameters);
                            if (typeArgument == null) {
                                type = objectType;
                            } else if (typeArgument.isWildcardExtendsTypeArgument() || typeArgument.isWildcardSuperTypeArgument()) {
                                wildcardSuperOrExtends = true;
                            }
                            if (typeArgument instanceof Type) {
                                type = (Type) typeArgument;
                            }
                        }
                    }

                    Map<String, TypeArgument> bindings = new HashMap<>();
                    Map<String, BaseType> typeBounds = new HashMap<>();
                    boolean partialBinding = populateBindings(bindings, exp, mie.getExceptionTypes(), typeParameters, typeArguments, methodTypeParameters, type, t, parameterTypes, parameters, typeBounds);

                    mie.setUnboundParameterTypes(parameterTypes);
                    parameterTypes = bind(bindings, parameterTypes);
                    mie.setParameterTypes(parameterTypes);
                    mie.setUnboundType(mie.getType());
                    mie.setType((Type) bind(bindings, mie.getType()));
                    mie.setTypeBounds(typeBounds);
                    mie.setTypeBindings(bindings);

                    if (methodTypeParameters != null && !partialBinding && !wildcardSuperOrExtends) {
                        bindTypeParametersToNonWildcardTypeArgumentsVisitor.init(bindings);
                        methodTypeParameters.accept(bindTypeParametersToNonWildcardTypeArgumentsVisitor);
                        if (isNonWildcardableBaseExpression(parameters, bindTypeParametersToNonWildcardTypeArgumentsVisitor.getTypeArgument())) {
                            mie.setNonWildcardTypeArguments(bindTypeParametersToNonWildcardTypeArgumentsVisitor.getTypeArgument());
                        }
                    }

                    if (expressionType.isObjectType()) {
                        ObjectType expressionObjectType = (ObjectType) expressionType;

                        if (bindings.isEmpty() || partialBinding || exp.isObjectTypeReferenceExpression() || typeParameters == null) {
                            expressionType = expressionObjectType.createType(null);
                        } else if (typeParameters.isList()) {
                            TypeArguments tas = new TypeArguments(typeParameters.size());
                            for (TypeParameter typeParameter : typeParameters) {
                                tas.add(bindings.get(typeParameter.getIdentifier()));
                            }
                            expressionType = expressionObjectType.createType(tas);
                        } else {
                            expressionType = expressionObjectType.createType(bindings.get(typeParameters.getFirst().getIdentifier()));
                        }
                    } else if (expressionType.isGenericType()) {
                        if (bindings.isEmpty() || partialBinding) {
                            expressionType = TYPE_OBJECT;
                        } else {
                            TypeArgument typeArgument = bindings.get(expressionType.getName());
                            if (typeArgument == null) {
                                expressionType = TYPE_OBJECT;
                            } else {
                                typeArgumentToTypeVisitor.init();
                                typeArgument.accept(typeArgumentToTypeVisitor);
                                expressionType = typeArgumentToTypeVisitor.getType();
                            }
                        }
                    }
                }
            }

            this.type = expressionType;

            if (!parametersFirst) {
                exp.accept(this);
                bindParameters(parameterTypes, parameters);
            }

            mie.setBound(true);
        }
    }

    private static boolean isNonWildcardableBaseExpression(BaseExpression parameters, BaseTypeArgument nonWildcardTypeArgument) {
        if (nonWildcardTypeArgument instanceof ObjectType ot && StringConstants.JAVA_LANG_OBJECT.equals(ot.getInternalName())) {
            // Do not use Object or Object array as explicit type parameter
            return false;
        }
        if (parameters instanceof LambdaIdentifiersExpression) {
            return false;
        }
        if (parameters != null) {
            if (parameters.isList()) {
                for (Expression parameter : parameters.getList()) {
                    if (!isNonWildCardableExpression(parameter, nonWildcardTypeArgument)) {
                        return false;
                    }
                }
            } else {
                Expression parameter = parameters.getFirst();
                if (!isNonWildCardableExpression(parameter, nonWildcardTypeArgument)) {
                    return false;
                }
            }
        }
        return true;
    }

    private static boolean isNonWildCardableExpression(Expression parameter, BaseTypeArgument nonWildcardTypeArgument) {
        if (parameter.getType() instanceof ObjectType) {
            ObjectType ot = (ObjectType) parameter.getType();
            BaseTypeArgument typeArguments = ot.getTypeArguments();
            if (typeArguments != null) {
                if (typeArguments.isTypeArgumentList()) {
                    for (TypeArgument typeArgument : typeArguments.getTypeArgumentList()) {
                        if (!isNonWildcardableTypeArgument(typeArgument, nonWildcardTypeArgument)) {
                            return false;
                        }
                    }
                } else {
                    TypeArgument typeArgument = typeArguments.getTypeArgumentFirst();
                    if (!isNonWildcardableTypeArgument(typeArgument, nonWildcardTypeArgument)) {
                        return false;
                    }
                }
            }
        }
        return true;
    }

    private static boolean isNonWildcardableTypeArgument(TypeArgument typeArgument, BaseTypeArgument nonWildcardTypeArgument) {
        if (typeArgument instanceof WildcardExtendsTypeArgument) {
            WildcardExtendsTypeArgument weta = (WildcardExtendsTypeArgument) typeArgument;
            if (weta.type().equals(nonWildcardTypeArgument)) {
                return false;
            }
        }
        if (typeArgument instanceof WildcardSuperTypeArgument) {
            WildcardSuperTypeArgument wsta = (WildcardSuperTypeArgument) typeArgument;
            if (wsta.type().equals(nonWildcardTypeArgument)) {
                return false;
            }
        }
        return true;
    }

    @Override
    public void visit(LocalVariableReferenceExpression expression) {
        AbstractLocalVariable localVariable = ((ClassFileLocalVariableReferenceExpression) expression).getLocalVariable();
        if (localVariable.getFromOffset() > 0) {
            // Do not update parameter
            localVariable.typeOnLeft(contextualTypeBounds, checkTypeArguments(type, localVariable));
        }
    }

    @Override
    public void visit(NewExpression expression) {
        ClassFileNewExpression ne = (ClassFileNewExpression)expression;

        if (! ne.isBound()) {
            BaseType parameterTypes = ne.getParameterTypes();
            BaseExpression parameters = ne.getParameters();
            ObjectType neObjectType = ne.getObjectType();

            if (staticMethod || !neObjectType.getInternalName().equals(internalTypeName)) {
                TypeMaker.TypeTypes typeTypes = typeMaker.makeTypeTypes(neObjectType.getInternalName());

                if (typeTypes != null) {
                    BaseTypeParameter typeParameters = typeTypes.getTypeParameters();
                    BaseTypeArgument typeArguments = neObjectType.getTypeArguments();

                    if (typeParameters != null && typeArguments == null) {
                        if (typeParameters.isList()) {
                            TypeArguments tas = new TypeArguments(typeParameters.size());
                            for (TypeParameter typeParameter : typeParameters) {
                                tas.add(new GenericType(typeParameter.getIdentifier()));
                            }
                            neObjectType = neObjectType.createType(tas);
                        } else {
                            neObjectType = neObjectType.createType(new GenericType(typeParameters.getFirst().getIdentifier()));
                        }
                    }

                    Type t = neObjectType;

                    if (type.isObjectType()) {
                        ObjectType objectType = (ObjectType)type;
                        t = typeMaker.searchSuperParameterizedType(objectType, neObjectType);
                        if (t == null) {
                            t = neObjectType;
                        }
                    }

                    Map<String, TypeArgument> bindings = new HashMap<>();
                    Map<String, BaseType> typeBounds = new HashMap<>();
                    boolean partialBinding = populateBindings(bindings, null, null, typeParameters, typeArguments, null, type, t, parameterTypes, parameters, typeBounds);

                    parameterTypes = bind(bindings, parameterTypes);
                    ne.setParameterTypes(parameterTypes);

                    // Replace wildcards
                    for (Map.Entry<String, TypeArgument> entry : bindings.entrySet()) {
                        typeArgumentToTypeVisitor.init();
                        entry.getValue().accept(typeArgumentToTypeVisitor);
                        entry.setValue(typeArgumentToTypeVisitor.getType());
                    }

                    if (!partialBinding) {
                        ne.setType((ObjectType) bind(bindings, neObjectType));
                    }
                }
            }

            bindParameters(parameterTypes, parameters);

            ne.setBound(true);
        }
    }

    @Override
    public void visit(CastExpression expression) {
        if ((!TYPE_OBJECT.equals(type) && (type.getDimension() != expression.getType().getDimension()))) {
            throw new IllegalArgumentException("TypeParametersToTypeArgumentsBinder.visit(CastExpression ce) : invalid array type");
        }

        if (type.isObjectType()) {
            ObjectType objectType = (ObjectType)type;

            if (objectType.getTypeArguments() != null && !objectType.getTypeArguments().equals(WildcardTypeArgument.WILDCARD_TYPE_ARGUMENT)) {
                if (!expression.getType().isObjectType()) {
                    throw new IllegalArgumentException("TypeParametersToTypeArgumentsBinder.visit(CastExpression ce) : invalid object type");
                }

                ObjectType expressionObjectType = (ObjectType) expression.getType();

                if (objectType.getInternalName().equals(expressionObjectType.getInternalName())) {
                    Type expressionExpressionType = expression.getExpression().getType();

                    if (expressionExpressionType.isObjectType()) {
                        ObjectType expressionExpressionObjectType = (ObjectType)expressionExpressionType;

                        if (expressionExpressionObjectType.getTypeArguments() == null || objectType.getTypeArguments().isTypeArgumentAssignableFrom(typeMaker, contextualBindings, contextualTypeBounds, expressionExpressionObjectType.getTypeArguments())) {
                            expression.setType(objectType);
                        }
                    } else if (expressionExpressionType.isGenericType()) {
                        expression.setType(objectType);
                    }
                }
            }
        }

        if (!type.isGenericType()) {
            type = expression.getType();
        }
        expression.getExpression().accept(this);
    }

    @Override
    public void visit(TernaryOperatorExpression expression) {
        Type t = type;

        expression.setType(t);
        expression.getTrueExpression().accept(this);
        type = t;
        expression.getFalseExpression().accept(this);
    }

    @Override
    public void visit(BinaryOperatorExpression expression) {
        if (expression.getType() == TYPE_STRING && "+".equals(expression.getOperator())) {
            type = TYPE_OBJECT;
        }

        Type t = type;

        expression.getLeftExpression().accept(this);
        type = t;
        expression.getRightExpression().accept(this);
    }

    @Override
    public void visit(ArrayExpression expression) {}
    @Override
    public void visit(BooleanExpression expression) {}
    @Override
    public void visit(CommentExpression expression) {}
    @Override
    public void visit(ConstructorInvocationExpression expression) {}
    @Override
    public void visit(ConstructorReferenceExpression expression) {}
    @Override
    public void visit(DoubleConstantExpression expression) {}
    @Override
    public void visit(EnumConstantReferenceExpression expression) {}
    @Override
    public void visit(Expressions expression) {}
    @Override
    public void visit(FieldReferenceExpression expression) {}
    @Override
    public void visit(FloatConstantExpression expression) {}
    @Override
    public void visit(IntegerConstantExpression expression) {}
    @Override
    public void visit(InstanceOfExpression expression) {}
    @Override
    public void visit(LambdaFormalParametersExpression expression) {}
    @Override
    public void visit(LambdaIdentifiersExpression expression) {}
    @Override
    public void visit(LengthExpression expression) {}
    @Override
    public void visit(LongConstantExpression expression) {}
    @Override
    public void visit(MethodReferenceExpression expression) {}
    @Override
    public void visit(NewArray expression) {}
    @Override
    public void visit(NewInitializedArray expression) {}
    @Override
    public void visit(NoExpression expression) {}
    @Override
    public void visit(NullExpression expression) {}
    @Override
    public void visit(ObjectTypeReferenceExpression expression) {}
    @Override
    public void visit(ParenthesesExpression expression) {}
    @Override
    public void visit(PostOperatorExpression expression) {}
    @Override
    public void visit(PreOperatorExpression expression) {}
    @Override
    public void visit(QualifiedSuperExpression expression) {}
    @Override
    public void visit(StringConstantExpression expression) {}
    @Override
    public void visit(SuperConstructorInvocationExpression expression) {}
    @Override
    public void visit(SuperExpression expression) {}
    @Override
    public void visit(ThisExpression expression) {}
    @Override
    public void visit(TypeReferenceDotClassExpression expression) {}
}
