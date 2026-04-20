/*
 * Copyright (c) 2008, 2019 Emmanuel Dupuy.
 * This project is distributed under the GPLv3 license.
 * This is a Copyleft license that gives the user the right to use,
 * copy and modify the code freely for non-commercial purposes.
 */
package org.jd.core.v1.service.converter.classfiletojavasyntax.visitor;

import org.jd.core.v1.model.javasyntax.AbstractJavaSyntaxVisitor;
import org.jd.core.v1.model.javasyntax.declaration.ArrayVariableInitializer;
import org.jd.core.v1.model.javasyntax.declaration.BaseMemberDeclaration;
import org.jd.core.v1.model.javasyntax.declaration.BodyDeclaration;
import org.jd.core.v1.model.javasyntax.declaration.ClassDeclaration;
import org.jd.core.v1.model.javasyntax.declaration.ConstructorDeclaration;
import org.jd.core.v1.model.javasyntax.declaration.ExpressionVariableInitializer;
import org.jd.core.v1.model.javasyntax.declaration.FieldDeclaration;
import org.jd.core.v1.model.javasyntax.declaration.FieldDeclarator;
import org.jd.core.v1.model.javasyntax.declaration.FormalParameter;
import org.jd.core.v1.model.javasyntax.declaration.InterfaceDeclaration;
import org.jd.core.v1.model.javasyntax.declaration.LocalVariableDeclaration;
import org.jd.core.v1.model.javasyntax.declaration.LocalVariableDeclarator;
import org.jd.core.v1.model.javasyntax.declaration.MethodDeclaration;
import org.jd.core.v1.model.javasyntax.declaration.RecordDeclaration;
import org.jd.core.v1.model.javasyntax.declaration.StaticInitializerDeclaration;
import org.jd.core.v1.model.javasyntax.declaration.VariableInitializer;
import org.jd.core.v1.model.javasyntax.expression.BaseExpression;
import org.jd.core.v1.model.javasyntax.expression.BinaryOperatorExpression;
import org.jd.core.v1.model.javasyntax.expression.CastExpression;
import org.jd.core.v1.model.javasyntax.expression.ConstructorInvocationExpression;
import org.jd.core.v1.model.javasyntax.expression.ConstructorReferenceExpression;
import org.jd.core.v1.model.javasyntax.expression.DoubleConstantExpression;
import org.jd.core.v1.model.javasyntax.expression.EnumConstantReferenceExpression;
import org.jd.core.v1.model.javasyntax.expression.Expression;
import org.jd.core.v1.model.javasyntax.expression.FieldReferenceExpression;
import org.jd.core.v1.model.javasyntax.expression.FloatConstantExpression;
import org.jd.core.v1.model.javasyntax.expression.IntegerConstantExpression;
import org.jd.core.v1.model.javasyntax.expression.LambdaIdentifiersExpression;
import org.jd.core.v1.model.javasyntax.expression.LocalVariableReferenceExpression;
import org.jd.core.v1.model.javasyntax.expression.LongConstantExpression;
import org.jd.core.v1.model.javasyntax.expression.MethodInvocationExpression;
import org.jd.core.v1.model.javasyntax.expression.MethodReferenceExpression;
import org.jd.core.v1.model.javasyntax.expression.NewExpression;
import org.jd.core.v1.model.javasyntax.expression.NewInitializedArray;
import org.jd.core.v1.model.javasyntax.expression.NullExpression;
import org.jd.core.v1.model.javasyntax.expression.ObjectTypeReferenceExpression;
import org.jd.core.v1.model.javasyntax.expression.QualifiedSuperExpression;
import org.jd.core.v1.model.javasyntax.expression.SuperConstructorInvocationExpression;
import org.jd.core.v1.model.javasyntax.expression.SuperExpression;
import org.jd.core.v1.model.javasyntax.expression.TernaryOperatorExpression;
import org.jd.core.v1.model.javasyntax.expression.ThisExpression;
import org.jd.core.v1.model.javasyntax.expression.TypeReferenceDotClassExpression;
import org.jd.core.v1.model.javasyntax.statement.BaseStatement;
import org.jd.core.v1.model.javasyntax.statement.BreakStatement;
import org.jd.core.v1.model.javasyntax.statement.ContinueStatement;
import org.jd.core.v1.model.javasyntax.statement.LambdaExpressionStatement;
import org.jd.core.v1.model.javasyntax.statement.ReturnExpressionStatement;
import org.jd.core.v1.model.javasyntax.statement.ThrowStatement;
import org.jd.core.v1.model.javasyntax.type.BaseType;
import org.jd.core.v1.model.javasyntax.type.BaseTypeArgument;
import org.jd.core.v1.model.javasyntax.type.BaseTypeParameter;
import org.jd.core.v1.model.javasyntax.type.GenericType;
import org.jd.core.v1.model.javasyntax.type.InnerObjectType;
import org.jd.core.v1.model.javasyntax.type.ObjectType;
import org.jd.core.v1.model.javasyntax.type.PrimitiveType;
import org.jd.core.v1.model.javasyntax.type.Type;
import org.jd.core.v1.model.javasyntax.type.TypeArgument;
import org.jd.core.v1.model.javasyntax.type.TypeArguments;
import org.jd.core.v1.model.javasyntax.type.TypeParameterWithTypeBounds;
import org.jd.core.v1.model.javasyntax.type.Types;
import org.jd.core.v1.model.javasyntax.type.WildcardExtendsTypeArgument;
import org.jd.core.v1.model.javasyntax.type.WildcardSuperTypeArgument;
import org.jd.core.v1.model.javasyntax.type.WildcardTypeArgument;
import org.jd.core.v1.service.converter.classfiletojavasyntax.model.javasyntax.declaration.ClassFileBodyDeclaration;
import org.jd.core.v1.service.converter.classfiletojavasyntax.model.javasyntax.declaration.ClassFileConstructorOrMethodDeclaration;
import org.jd.core.v1.service.converter.classfiletojavasyntax.model.javasyntax.declaration.ClassFileMethodDeclaration;
import org.jd.core.v1.service.converter.classfiletojavasyntax.model.javasyntax.declaration.ClassFileStaticInitializerDeclaration;
import org.jd.core.v1.service.converter.classfiletojavasyntax.model.javasyntax.expression.ClassFileConstructorInvocationExpression;
import org.jd.core.v1.service.converter.classfiletojavasyntax.model.javasyntax.expression.ClassFileLocalVariableReferenceExpression;
import org.jd.core.v1.service.converter.classfiletojavasyntax.model.javasyntax.expression.ClassFileMethodInvocationExpression;
import org.jd.core.v1.service.converter.classfiletojavasyntax.model.javasyntax.expression.ClassFileNewExpression;
import org.jd.core.v1.service.converter.classfiletojavasyntax.model.javasyntax.expression.ClassFileSuperConstructorInvocationExpression;
import org.jd.core.v1.service.converter.classfiletojavasyntax.model.localvariable.AbstractLocalVariable;
import org.jd.core.v1.service.converter.classfiletojavasyntax.util.TypeMaker;
import org.jd.core.v1.service.converter.classfiletojavasyntax.util.TypeMaker.TypeTypes;
import org.jd.core.v1.service.converter.classfiletojavasyntax.util.Utils;
import org.jd.core.v1.util.DefaultList;
import org.jd.core.v1.util.StringConstants;

import java.util.ArrayDeque;
import java.util.Arrays;
import java.util.Collections;
import java.util.Deque;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import static org.apache.bcel.Const.ACC_BRIDGE;
import static org.apache.bcel.Const.ACC_SYNTHETIC;
import static org.jd.core.v1.model.javasyntax.type.PrimitiveType.TYPE_BYTE;
import static org.jd.core.v1.model.javasyntax.type.PrimitiveType.TYPE_SHORT;

public class AddCastExpressionVisitor extends AbstractJavaSyntaxVisitor {
    private final SearchFirstLineNumberVisitor searchFirstLineNumberVisitor = new SearchFirstLineNumberVisitor();

    private final TypeMaker typeMaker;
    private Map<String, BaseType> typeBounds;
    private Type returnedType;
    private BaseType exceptionTypes;
    private Deque<TypeParameter> typeParameters = new ArrayDeque<>();
    private Map<String, BaseTypeArgument> parameterTypeArguments = new HashMap<>();
    private Type type;
    private boolean visitingAnonymousClass;
    private boolean visitingLambda;
    private Set<String> fieldNamesInLambda = new HashSet<>();
    private boolean staticContext;

    private record TypeParameter(boolean staticContext, BaseTypeParameter type) {}

    public AddCastExpressionVisitor(TypeMaker typeMaker) {
        this.typeMaker = typeMaker;
    }

    @Override
    public void visit(BodyDeclaration declaration) {
        if (!declaration.isAnonymous()) {
            visitBodyDeclaration(declaration);
        }
    }

    private void visitBodyDeclaration(BodyDeclaration declaration) {
        BaseMemberDeclaration memberDeclarations = declaration.getMemberDeclarations();

        if (memberDeclarations != null) {
            Map<String, BaseType> tb = typeBounds;

            typeBounds = ((ClassFileBodyDeclaration)declaration).getTypeBounds();
            memberDeclarations.accept(this);
            typeBounds = tb;
        }
    }

    @Override
    public void visit(FieldDeclaration declaration) {
        if ((declaration.getFlags() & ACC_SYNTHETIC) == 0) {
            Type t = type;

            type = declaration.getType();
            declaration.getFieldDeclarators().accept(this);
            type = t;
        }
    }

    @Override
    public void visit(FieldDeclarator declarator) {

        if (declarator.getName() != null && visitingLambda) {
            fieldNamesInLambda.add(declarator.getName());
        }

        VariableInitializer variableInitializer = declarator.getVariableInitializer();

        if (variableInitializer != null) {
            variableInitializer.accept(this);
        }
    }

    @Override
    public void visit(StaticInitializerDeclaration declaration) {
        BaseStatement statements = declaration.getStatements();

        if (statements != null) {
            Map<String, BaseType> tb = typeBounds;

            typeBounds = ((ClassFileStaticInitializerDeclaration)declaration).getTypeBounds();
            statements.accept(this);
            typeBounds = tb;
        }
    }

    @Override
    public void visit(ConstructorDeclaration declaration) {
        if ((declaration.getFlags() & (ACC_SYNTHETIC|ACC_BRIDGE)) == 0) {
            BaseStatement statements = declaration.getStatements();

            if (statements != null) {
                Map<String, BaseType> tb = typeBounds;
                BaseType et = exceptionTypes;

                typeBounds = ((ClassFileConstructorOrMethodDeclaration) declaration).getTypeBounds();
                exceptionTypes = declaration.getExceptionTypes();
                statements.accept(this);
                typeBounds = tb;
                exceptionTypes = et;
            }
        }
    }

    @Override
    public void visit(MethodDeclaration declaration) {
        if ((declaration.getFlags() & (ACC_SYNTHETIC|ACC_BRIDGE)) == 0) {
            BaseStatement statements = declaration.getStatements();

            if (statements != null) {
                Map<String, BaseType> tb = typeBounds;
                Type rt = returnedType;
                BaseType et = exceptionTypes;
                boolean sc = staticContext;

                typeBounds = ((ClassFileMethodDeclaration) declaration).getTypeBounds();
                returnedType = declaration.getReturnedType();
                exceptionTypes = declaration.getExceptionTypes();
                staticContext = declaration.isStatic();
                pushContext(declaration);
                safeAccept(declaration.getFormalParameters());
                statements.accept(this);
                typeBounds = tb;
                returnedType = rt;
                exceptionTypes = et;
                staticContext = sc;
                popContext(declaration);
            }
        }
    }

    @Override
    public void visit(ClassDeclaration declaration) {
        pushContext(declaration);
        super.visit(declaration);
        popContext(declaration);
    }

    @Override
    public void visit(FormalParameter declaration) {
        if (!visitingAnonymousClass && declaration.getType() instanceof ObjectType) {
            ObjectType ot = (ObjectType) declaration.getType();
            parameterTypeArguments.put(declaration.getName(), ot.getTypeArguments());
        }
    }

    public void pushContext(MethodDeclaration declaration) {
        if (declaration.getTypeParameters() != null) {
            typeParameters.push(new TypeParameter(declaration.isStatic(), declaration.getTypeParameters()));
        }
    }

    public void popContext(MethodDeclaration declaration) {
        if (declaration.getTypeParameters() != null) {
            typeParameters.pop();
        }
        if (!visitingAnonymousClass) {
            parameterTypeArguments.clear();
        }
    }

    public void pushContext(InterfaceDeclaration declaration) {
        if (declaration.getTypeParameters() != null) {
            typeParameters.push(new TypeParameter(declaration.isStatic(), declaration.getTypeParameters()));
        }
    }

    public void popContext(InterfaceDeclaration declaration) {
        if (declaration.getTypeParameters() != null) {
            typeParameters.pop();
        }
    }

    public void pushContext(RecordDeclaration declaration) {
        if (declaration.getTypeParameters() != null) {
            typeParameters.push(new TypeParameter(false, declaration.getTypeParameters()));
        }
    }

    public void popContext(RecordDeclaration declaration) {
        if (declaration.getTypeParameters() != null) {
            typeParameters.pop();
        }
    }

    @Override
    public void visit(LambdaIdentifiersExpression expression) {
        visitingLambda = true;
        BaseStatement statements = expression.getStatements();

        if (statements != null) {
            Type rt = returnedType;
            returnedType = expression.getReturnedType();
            statements.accept(this);
            returnedType = rt;
        }
        visitingLambda = false;
    }

    @Override
    public void visit(ReturnExpressionStatement statement) {
        statement.setExpression(updateStatementExpression(statement.getExpression()));
    }

    @Override
    public void visit(LambdaExpressionStatement statement) {
        statement.setExpression(updateStatementExpression(statement.getExpression()));
    }

    private Expression updateStatementExpression(Expression expression) {
        Map<String, TypeArgument> typeBindings = getLocalTypeBindings(expression);
        Map<String, BaseType> localTypeBounds = getLocalTypeBounds(expression);
        expression = updateExpression(typeBindings, localTypeBounds, returnedType, null, expression, false, true, false);
        if (!(expression instanceof CastExpression)
                && returnedType instanceof GenericType
                && expression instanceof ClassFileMethodInvocationExpression methodInvocationExpression
                && shouldAddExplicitGenericResultCast(returnedType, methodInvocationExpression, expression.getType())) {
            expression = addExplicitCastExpression(returnedType, expression);
        }
        if (!(expression instanceof CastExpression)
                && returnedType instanceof ObjectType returnedObjectType
                && expression instanceof ClassFileMethodInvocationExpression methodInvocationExpression) {
            RawDescriptorMethodTypes descriptorMethodTypes = parseRawDescriptorMethodTypes(methodInvocationExpression.getDescriptor());
            if (descriptorMethodTypes != null
                    && descriptorMethodTypes.returnedType() instanceof ObjectType descriptorObjectType
                    && !returnedObjectType.rawEquals(descriptorObjectType)) {
                expression = addExplicitCastExpression(returnedType, expression);
            }
        }
        return expression;
    }

    @Override
    public void visit(ThrowStatement statement) {
        if (exceptionTypes != null && exceptionTypes.size() == 1) {
            Type exceptionType = exceptionTypes.getFirst();

            if (exceptionType.isGenericType() && !statement.getExpression().getType().equals(exceptionType)) {
                statement.setExpression(addCastExpression(exceptionType, statement.getExpression()));
            }
        }
    }

    @Override
    public void visit(LocalVariableDeclaration declaration) {
        Type t = type;

        type = declaration.getType();
        declaration.getLocalVariableDeclarators().accept(this);
        type = t;
    }

    @Override
    public void visit(LocalVariableDeclarator declarator) {
        VariableInitializer variableInitializer = declarator.getVariableInitializer();

        if (variableInitializer != null) {
            int extraDimension = declarator.getDimension();

            if (extraDimension == 0) {
                variableInitializer.accept(this);
            } else {
                Type t = type;

                type = type.createType(type.getDimension() + extraDimension);
                variableInitializer.accept(this);
                type = t;
            }
        }
    }

    @Override
    public void visit(ArrayVariableInitializer declaration) {
        if (type.getDimension() == 0) {
            acceptListDeclaration(declaration);
        } else {
            Type t = type;

            type = type.createType(Math.max(0, type.getDimension() - 1));
            acceptListDeclaration(declaration);
            type = t;
        }
    }

    @Override
    public void visit(ExpressionVariableInitializer declaration) {
        Expression expression = declaration.getExpression();

        if (expression.isNewInitializedArray()) {
            NewInitializedArray nia = (NewInitializedArray)expression;
            Type t = type;

            type = nia.getType();
            nia.getArrayInitializer().accept(this);
            type = t;
        } else {
            declaration.setExpression(updateExpression(Collections.emptyMap(), typeBounds, type, null, expression, false, true, false));
        }
    }

    @Override
    public void visit(SuperConstructorInvocationExpression expression) {
        BaseExpression parameters = expression.getParameters();

        if (!Utils.isEmpty(parameters)) {
            boolean unique = typeMaker.matchCount(expression.getObjectType().getInternalName(), StringConstants.INSTANCE_CONSTRUCTOR, parameters.size(), true) <= 1;
            boolean forceCast = !unique && typeMaker.matchCount(Collections.emptyMap(), typeBounds, expression.getObjectType().getInternalName(), StringConstants.INSTANCE_CONSTRUCTOR, parameters, true) > 1;
            boolean rawCast = false;
            BaseType parameterTypes = ((ClassFileSuperConstructorInvocationExpression)expression).getParameterTypes();
            expression.setParameters(updateParameters(Collections.emptyMap(), typeBounds, parameterTypes, null, parameters, forceCast, unique, rawCast, false));
        }
    }

    @Override
    public void visit(ConstructorInvocationExpression expression) {
        BaseExpression parameters = expression.getParameters();

        if (!Utils.isEmpty(parameters)) {
            boolean unique = typeMaker.matchCount(expression.getObjectType().getInternalName(), StringConstants.INSTANCE_CONSTRUCTOR, parameters.size(), true) <= 1;
            boolean forceCast = !unique && typeMaker.matchCount(Collections.emptyMap(), typeBounds, expression.getObjectType().getInternalName(), StringConstants.INSTANCE_CONSTRUCTOR, parameters, true) > 1;
            boolean rawCast = false;
            BaseType parameterTypes = ((ClassFileConstructorInvocationExpression)expression).getParameterTypes();
            expression.setParameters(updateParameters(Collections.emptyMap(), typeBounds, parameterTypes, null, parameters, forceCast, unique, rawCast, false));
        }
    }

    private static Map<String, TypeArgument> getLocalTypeBindings(Expression exp) {
        if (exp instanceof MethodInvocationExpression mie) {
            return mie.getTypeBindings();
        }
        return Collections.emptyMap();
    }

    private Map<String, BaseType> getLocalTypeBounds(Expression exp) {
        Map<String, BaseType> localTypeBounds = new HashMap<>(typeBounds);
        if (exp instanceof MethodInvocationExpression mie && mie.getTypeBounds() != null) {
            localTypeBounds.putAll(mie.getTypeBounds());
        }
        return localTypeBounds;
    }

    private static void addImplicitObjectBounds(Map<String, BaseType> localTypeBounds, BaseType unboundParameterTypes, Set<String> methodTypeParameters) {
        if (unboundParameterTypes == null || methodTypeParameters.isEmpty()) {
            return;
        }

        for (Type type : unboundParameterTypes) {
            addImplicitObjectBounds(localTypeBounds, type, methodTypeParameters);
        }
    }

    private static void addImplicitObjectBounds(Map<String, BaseType> localTypeBounds, Type type, Set<String> methodTypeParameters) {
        if (type instanceof GenericType gt) {
            if (methodTypeParameters.contains(gt.getName())) {
                localTypeBounds.putIfAbsent(gt.getName(), ObjectType.TYPE_OBJECT);
            }
            return;
        }

        if (type instanceof ObjectType ot) {
            BaseTypeArgument typeArguments = ot.getTypeArguments();
            if (typeArguments != null) {
                for (String identifier : typeArguments.findTypeParametersInType()) {
                    if (methodTypeParameters.contains(identifier)) {
                        localTypeBounds.putIfAbsent(identifier, ObjectType.TYPE_OBJECT);
                    }
                }
            }
            ObjectType outerType = ot.getOuterType();
            if (outerType != null && !ObjectType.TYPE_UNDEFINED_OBJECT.equals(outerType) && outerType != ot) {
                addImplicitObjectBounds(localTypeBounds, outerType, methodTypeParameters);
            }
        }
    }

    private static Set<String> getMethodTypeParameterNames(ClassFileMethodInvocationExpression expression) {
        BaseTypeParameter methodTypeParameters = expression.getTypeParameters();
        if (methodTypeParameters == null) {
            return Collections.emptySet();
        }
        Set<String> names = new HashSet<>();
        methodTypeParameters.forEach(typeParameter -> names.add(typeParameter.getIdentifier()));
        return names;
    }

    @Override
    public void visit(MethodInvocationExpression expression) {
        BaseExpression parameters = expression.getParameters();

        Map<String, TypeArgument> typeBindings = getLocalTypeBindings(expression);
        Map<String, BaseType> localTypeBounds = getLocalTypeBounds(expression);

        if (expression instanceof ClassFileMethodInvocationExpression classFileMethodInvocationExpression) {
            applyDirectInvocationTypeBindings(classFileMethodInvocationExpression);
        }

        restoreConcreteToArrayReceiverType(expression);

        if (!Utils.isEmpty(parameters)) {
            BaseType parameterTypes = ((ClassFileMethodInvocationExpression)expression).getParameterTypes();
            BaseType unboundParameterTypes = ((ClassFileMethodInvocationExpression)expression).getUnboundParameterTypes();
            if (!expression.isVarArgs()) {
                Set<String> methodTypeParameterNames = getMethodTypeParameterNames((ClassFileMethodInvocationExpression) expression);
                addImplicitObjectBounds(localTypeBounds, unboundParameterTypes, methodTypeParameterNames);
            }
            boolean unique = typeMaker.matchCount(expression.getInternalTypeName(), expression.getName(), parameters.size(), false) <= 1;
            int rawMatches = typeMaker.matchCount(expression.getInternalTypeName(), expression.getName(), parameters.size(), false);
            int typedMatches = typeMaker.matchCount(typeBindings, localTypeBounds, expression.getInternalTypeName(), expression.getName(), parameters, false);
            if ("use".equals(expression.getName())) {
                System.out.println("[DEBUG-VISIT] params=" + parameters + " parameterTypes=" + parameterTypes + " unbound=" + unboundParameterTypes + " typeBindings=" + typeBindings + " typeBounds=" + localTypeBounds + " unique=" + unique + " rawMatches=" + rawMatches + " typedMatches=" + typedMatches + " descriptor=" + expression.getDescriptor());
            }
            boolean generalOverloadCast = shouldForceGeneralOverloadCast(
                    expression,
                    typeBindings,
                    localTypeBounds,
                    parameters,
                    parameterTypes,
                    typedMatches,
                    rawMatches);
            boolean selfSubtypeOverloadCast = shouldForceSelfOverloadSubtypeCast(expression, typeBindings, localTypeBounds, parameters, parameterTypes, typedMatches);
            boolean forceCast = !unique
                    && (generalOverloadCast || selfSubtypeOverloadCast);
            RawDescriptorMethodTypes descriptorMethodTypes = parseRawDescriptorMethodTypes(expression.getDescriptor());
            boolean useDescriptorBridgeParameterTypes = shouldUseDescriptorBridgeParameterTypes(unique, parameterTypes, descriptorMethodTypes);
            if ("use".equals(expression.getName())) {
                System.out.println("[DEBUG-VISIT] generalOverloadCast=" + generalOverloadCast + " selfSubtypeOverloadCast=" + selfSubtypeOverloadCast + " forceCast=" + forceCast + " descriptorTypes=" + (descriptorMethodTypes == null ? null : descriptorMethodTypes.parameterTypes()) + " useDescriptorBridge=" + useDescriptorBridgeParameterTypes);
            }
            if (useDescriptorBridgeParameterTypes && descriptorMethodTypes != null) {
                ((ClassFileMethodInvocationExpression) expression).setParameterTypes(descriptorMethodTypes.parameterTypes());
                if (descriptorMethodTypes.returnedType() != null
                        && hasDifferentErasedSignature(expression.getType(), descriptorMethodTypes.returnedType())) {
                    expression.setType(descriptorMethodTypes.returnedType());
                }
            }
            BaseType effectiveParameterTypes = useDescriptorBridgeParameterTypes
                    ? descriptorMethodTypes.parameterTypes()
                    : parameterTypes;
            BaseExpression effectiveParameters = collapseSingleElementVarArgs((ClassFileMethodInvocationExpression) expression, parameters);
            effectiveParameterTypes = adjustVarArgsParameterTypes((ClassFileMethodInvocationExpression) expression, effectiveParameterTypes, effectiveParameters);
            boolean rawCast = false;
            BaseExpression updatedParameters = updateParameters(typeBindings, localTypeBounds, effectiveParameterTypes, unboundParameterTypes, effectiveParameters, forceCast, unique, rawCast, useDescriptorBridgeParameterTypes);
            updatedParameters = addTargetTypedArrayConstructorReferenceCasts(expression, effectiveParameterTypes, updatedParameters);
            expression.setParameters(useDescriptorBridgeParameterTypes
                    ? adjustDescriptorSensitiveParameters(expression, parameterTypes, descriptorMethodTypes, updatedParameters)
                    : updatedParameters);
        }

        if (expression.getNonWildcardTypeArguments() != null) {
            if ("java/util/Objects".equals(expression.getInternalTypeName()) && "requireNonNull".equals(expression.getName())) {
                expression.setNonWildcardTypeArguments(null);
            } else if (shouldClearRedundantMethodTypeArguments(expression)) {
                expression.setNonWildcardTypeArguments(null);
            } else if (visitingLambda) {
                expression.setNonWildcardTypeArguments(null);
            } else if (hasKnownTypeParameters(expression.getNonWildcardTypeArguments())) {
                safeAccept(expression.getNonWildcardTypeArguments());
            } else {
                expression.setNonWildcardTypeArguments(null);
            }
        }

        if (expression.getExpression() instanceof CastExpression ce && ce.isByteCodeCheckCast() && ce.getExpression() instanceof ClassFileMethodInvocationExpression && ce.getType() instanceof ObjectType) {
            ObjectType ot = (ObjectType) ce.getType();
            if (ot != null) {
                if (isCastToBeRemoved(typeBindings, localTypeBounds, ot, null, ce, true)) {
                    // Remove cast
                    expression.setExpression(ce.getExpression());
                }
                TypeTypes typeTypes = typeMaker.makeTypeTypes(ot.getInternalName());
                if (typeTypes != null && typeTypes.getTypeParameters() != null && ot.getTypeArguments() == null) {
                    ClassFileMethodInvocationExpression mie = (ClassFileMethodInvocationExpression) ce.getExpression();
                    if (mie.getUnboundType() instanceof ObjectType) {
                        TypeArguments typeArguments = new TypeArguments();
                        for (int i = 0; i < typeTypes.getTypeParameters().size(); i++) {
                            typeArguments.add(WildcardTypeArgument.WILDCARD_TYPE_ARGUMENT);
                        }
                        ce.setType(ot.createType(typeArguments));
                    }
                }
            }
        }

        if (requiresSpecificReceiverCast(expression)) {
            Expression receiver = expression.getExpression();
            ObjectType ownerType = typeMaker.makeFromInternalTypeName(expression.getInternalTypeName());
            expression.setExpression(addExplicitCastExpression(ownerType.createType(null), receiver));
        } else if (requiresRawReceiverCast(expression)) {
            Expression receiver = expression.getExpression();
            ObjectType receiverType = (ObjectType) receiver.getType();
            expression.setExpression(addCastExpression(receiverType.createType(null), receiver));
        }

        expression.getExpression().accept(this);
    }

    private void applyDirectInvocationTypeBindings(ClassFileMethodInvocationExpression expression) {
        Map<String, TypeArgument> typeBindings = expression.getTypeBindings();
        if (typeBindings == null || typeBindings.isEmpty()) {
            return;
        }

        expression.setType(applyDirectTypeBindings(typeBindings, expression.getType()));
        expression.setUnboundType(applyDirectTypeBindings(typeBindings, expression.getUnboundType()));
        expression.setParameterTypes(applyDirectTypeBindings(typeBindings, expression.getParameterTypes()));
        expression.setUnboundParameterTypes(applyDirectTypeBindings(typeBindings, expression.getUnboundParameterTypes()));
    }

    @Override
    public void visit(NewExpression expression) {
        BaseExpression parameters = expression.getParameters();

        if (parameters != null) {
            boolean unique = typeMaker.matchCount(expression.getObjectType().getInternalName(), StringConstants.INSTANCE_CONSTRUCTOR, parameters.size(), true) <= 1;
            boolean forceCast = !unique && typeMaker.matchCount(Collections.emptyMap(), typeBounds, expression.getObjectType().getInternalName(), StringConstants.INSTANCE_CONSTRUCTOR, parameters, true) > 1;
            Type currentType = type == null ? returnedType : type;
            boolean rawCast = currentType instanceof ObjectType && expression.getType() instanceof ObjectType
                    && typeMaker.isRawTypeAssignable((ObjectType) currentType, expression.getObjectType())
                    && !typeMaker.isAssignable(typeBounds, (ObjectType) currentType, expression.getObjectType());
            if (rawCast) {
                expression.setObjectType(expression.getObjectType().createType(((ObjectType) currentType).getTypeArguments()));
            }
            ClassFileNewExpression classFileNewExpression = (ClassFileNewExpression) expression;
            BaseType parameterTypes = classFileNewExpression.getParameterTypes();
            RawDescriptorMethodTypes descriptorMethodTypes = parseRawDescriptorMethodTypes(expression.getDescriptor());
            if (shouldUseDescriptorBridgeParameterTypes(unique, parameterTypes, descriptorMethodTypes)) {
                classFileNewExpression.setParameterTypes(descriptorMethodTypes.parameterTypes());
                parameterTypes = classFileNewExpression.getParameterTypes();
            }
            BaseType unboundParameterTypes = resolveConstructorUnboundParameterTypes((ClassFileNewExpression) expression, parameterTypes, parameters);
            BaseExpression updatedParameters = updateParameters(Collections.emptyMap(), typeBounds, parameterTypes, unboundParameterTypes, parameters, forceCast, unique, rawCast, false);
            if (shouldUseDescriptorBridgeParameterTypes(unique, ((ClassFileNewExpression) expression).getParameterTypes(), descriptorMethodTypes)) {
                updatedParameters = adjustDescriptorSensitiveParameters(((ClassFileNewExpression) expression).getParameterTypes(), descriptorMethodTypes, updatedParameters);
            }
            updatedParameters = addRequiredRawCollectionConstructorCasts(expression.getObjectType(), parameterTypes, updatedParameters);
            expression.setParameters(addTargetTypedCollectionConstructorCasts(currentType, parameterTypes, updatedParameters));
        }

        if (expression.getBodyDeclaration() != null && expression.getBodyDeclaration().isAnonymous()) {
            visitingAnonymousClass = true;
            visitBodyDeclaration(expression.getBodyDeclaration());
            visitingAnonymousClass = false;
        }

        Type currentType = type == null ? returnedType : type;
        if (expression.getObjectType().getTypeArguments() == null
                && expression.isDiamondPossible()
                && (visitingLambda || canUseDiamond(currentType, expression))) {
            prepareDiamondTypeArgumentsIfPossible(expression);
        }

        if (!hasKnownTypeParameters(expression.getObjectType())) {
            expression.setType(expression.getObjectType().createType(ObjectType.TYPE_UNDEFINED_OBJECT));
        }
    }

    @Override
    public void visit(NewInitializedArray expression) {
        ArrayVariableInitializer arrayInitializer = expression.getArrayInitializer();

        if (arrayInitializer != null) {
            Type t = type;

            type = expression.getType();
            arrayInitializer.accept(this);
            type = t;
        }
    }

    @Override
    public void visit(FieldReferenceExpression expression) {
        Expression exp = expression.getExpression();

        if (exp != null && !exp.isObjectTypeReferenceExpression()) {
            Type localType = typeMaker.makeFromInternalTypeName(expression.getInternalTypeName());

            if (localType.getName() != null) {
                expression.setExpression(updateExpression(Collections.emptyMap(), typeBounds, localType, null, exp, false, true, false));
            }
        }
    }

    @Override
    public void visit(BinaryOperatorExpression expression) {
        expression.getLeftExpression().accept(this);

        Expression rightExpression = expression.getRightExpression();

        if ("=".equals(expression.getOperator())) {
            if (rightExpression.isMethodInvocationExpression()) {
                ClassFileMethodInvocationExpression mie = (ClassFileMethodInvocationExpression)rightExpression;

                if (mie.getTypeParameters() != null) {
                    // Do not add cast expression if method contains type parameters
                    rightExpression.accept(this);
                    return;
                }
            }

            expression.setRightExpression(updateExpression(Collections.emptyMap(), typeBounds, expression.getLeftExpression().getType(), null, rightExpression, false, true, false));
            return;
        }

        rightExpression.accept(this);
    }

    @Override
    public void visit(TernaryOperatorExpression expression) {
        Type expressionType = expression.getType();

        expression.getCondition().accept(this);
        expression.setTrueExpression(updateExpression(Collections.emptyMap(), typeBounds, expressionType, null, expression.getTrueExpression(), false, true, false));
        expression.setFalseExpression(updateExpression(Collections.emptyMap(), typeBounds, expressionType, null, expression.getFalseExpression(), false, true, false));
    }

    @Override
    public void visit(TypeArguments type) {
        TypeParameter baseTypeParameter = typeParameters.peek();
        TypeWithBoundsToGenericVisitor typeParameterVisitor = new TypeWithBoundsToGenericVisitor();
        if (baseTypeParameter != null) {
            baseTypeParameter.type().accept(typeParameterVisitor);
            type.accept(typeParameterVisitor);
        }
    }

    protected BaseExpression updateParameters(Map<String, TypeArgument> typeBindings, Map<String, BaseType> localTypeBounds, BaseType types, BaseType unboundTypes, BaseExpression expressions, boolean forceCast, boolean unique, boolean rawCast, boolean descriptorBridgeParameterTypes) {
        if (expressions != null) {
            if (expressions.isList()) {
                DefaultList<Type> typeList = types.getList();
                DefaultList<Type> unboundTypeList = unboundTypes == null ? null : unboundTypes.getList();
                DefaultList<Expression> expressionList = expressions.getList();

                for (int i = expressionList.size() - 1; i >= 0; i--) {
                    Type unboundType = unboundTypes == null ?  null : unboundTypeList.get(i);
                    Type parameterType = typeList.get(i);
                    parameterType = applyDirectTypeBindings(typeBindings, parameterType);
                    unboundType = applyDirectTypeBindings(typeBindings, unboundType);
                    parameterType = resolveErasedObjectParameterType(localTypeBounds, parameterType, unboundType);
                    if (shouldPreferUnboundParameterType(localTypeBounds, parameterType, unboundType)) {
                        parameterType = unboundType;
                    }
                    expressionList.set(i, updateParameter(typeBindings, localTypeBounds, parameterType, unboundType, expressionList.get(i), forceCast, unique, rawCast, descriptorBridgeParameterTypes));
                }
            } else {
                Type unboundType = unboundTypes == null ?  null : unboundTypes.getFirst();
                Type parameterType = types.getFirst();
                parameterType = applyDirectTypeBindings(typeBindings, parameterType);
                unboundType = applyDirectTypeBindings(typeBindings, unboundType);
                parameterType = resolveErasedObjectParameterType(localTypeBounds, parameterType, unboundType);
                if (shouldPreferUnboundParameterType(localTypeBounds, parameterType, unboundType)) {
                    parameterType = unboundType;
                }
                expressions = updateParameter(typeBindings, localTypeBounds, parameterType, unboundType, expressions.getFirst(), forceCast, unique, rawCast, descriptorBridgeParameterTypes);
            }
        }

        return expressions;
    }

    private Type applyDirectTypeBindings(Map<String, TypeArgument> typeBindings, Type type) {
        if (type == null || typeBindings == null || typeBindings.isEmpty()) {
            return type;
        }

        if (type instanceof GenericType genericType) {
            TypeArgument boundArgument = typeBindings.get(genericType.getName());
            if (boundArgument instanceof Type boundType) {
                return genericType.getDimension() == 0 ? boundType : boundType.createType(genericType.getDimension());
            }
            return type;
        }

        if (!(type instanceof ObjectType objectType)
                || objectType.getTypeArguments() == null
                || !objectType.getTypeArguments().isTypeArgumentList()) {
            return type;
        }

        TypeArguments resolvedArguments = new TypeArguments(objectType.getTypeArguments().getTypeArgumentList().size());
        boolean changed = false;
        for (TypeArgument typeArgument : objectType.getTypeArguments().getTypeArgumentList()) {
            TypeArgument resolvedArgument = applyDirectTypeBindings(typeBindings, typeArgument);
            resolvedArguments.add(resolvedArgument);
            changed |= resolvedArgument != typeArgument;
        }

        return changed ? objectType.createType(resolvedArguments) : type;
    }

    private BaseType applyDirectTypeBindings(Map<String, TypeArgument> typeBindings, BaseType baseType) {
        if (baseType == null || typeBindings == null || typeBindings.isEmpty()) {
            return baseType;
        }

        if (!baseType.isList()) {
            return applyDirectTypeBindings(typeBindings, baseType.getFirst());
        }

        Types resolvedTypes = new Types(baseType.size());
        boolean changed = false;
        for (Type type : baseType) {
            Type resolvedType = applyDirectTypeBindings(typeBindings, type);
            resolvedTypes.add(resolvedType);
            changed |= resolvedType != type;
        }
        return changed ? resolvedTypes : baseType;
    }

    private TypeArgument applyDirectTypeBindings(Map<String, TypeArgument> typeBindings, TypeArgument typeArgument) {
        if (!(typeArgument instanceof Type type)) {
            if (typeArgument instanceof WildcardExtendsTypeArgument wildcardExtendsTypeArgument
                    && wildcardExtendsTypeArgument.type() != null) {
                Type resolvedType = applyDirectTypeBindings(typeBindings, wildcardExtendsTypeArgument.type());
                return resolvedType == wildcardExtendsTypeArgument.type()
                        ? typeArgument
                        : new WildcardExtendsTypeArgument(resolvedType);
            }
            if (typeArgument instanceof WildcardSuperTypeArgument wildcardSuperTypeArgument
                    && wildcardSuperTypeArgument.type() != null) {
                Type resolvedType = applyDirectTypeBindings(typeBindings, wildcardSuperTypeArgument.type());
                return resolvedType == wildcardSuperTypeArgument.type()
                        ? typeArgument
                        : new WildcardSuperTypeArgument(resolvedType);
            }
            return typeArgument;
        }

        return applyDirectTypeBindings(typeBindings, type);
    }

    private Expression updateParameter(Map<String, TypeArgument> typeBindings, Map<String, BaseType> localTypeBounds, Type type, Type unboundType, Expression expression, boolean forceCast, boolean unique, boolean rawCast, boolean descriptorBridgeParameterTypes) {
        if (type instanceof ObjectType objectType
                && ObjectType.TYPE_OBJECT.rawEquals(objectType)
                && unboundType != null
                && hasKnownTypeParameters(unboundType)
                && unboundType.equals(expression.getType())) {
            type = unboundType;
        }
        if (!(expression instanceof CastExpression)
                && forceCast
                && shouldForceAlternativeObjectOverloadCast(expression)
                && type instanceof ObjectType objectType
                && !ObjectType.TYPE_OBJECT.rawEquals(objectType)) {
            expression = addCastExpression(ObjectType.TYPE_OBJECT, expression);
            expression.accept(this);
            return expression;
        }
        if (!(expression instanceof CastExpression)
                && expression instanceof ThisExpression
                && unboundType instanceof GenericType) {
            expression = addCastExpression(unboundType, expression);
        }
        if (!(expression instanceof CastExpression)
                && unboundType instanceof GenericType
                && expression.isMethodInvocationExpression()
                && type instanceof ObjectType parameterObjectType
                && expression.getType() instanceof ObjectType expressionObjectType
                && !ObjectType.TYPE_OBJECT.rawEquals(parameterObjectType)
                && !AutoboxingVisitor.isBoxingMethod(expression)
                && parameterObjectType.rawEquals(expressionObjectType)) {
            expression = addCastExpression(unboundType, expression);
        }
        if (!(expression instanceof CastExpression)
                && unboundType instanceof GenericType genericType
                && expression.isMethodInvocationExpression()
                && expression.getType() instanceof ObjectType expressionObjectType
                && !AutoboxingVisitor.isBoxingMethod(expression)
                && localTypeBounds.get(genericType.getName()) instanceof ObjectType boundObjectType
                && boundObjectType.rawEquals(expressionObjectType)) {
            expression = addCastExpression(unboundType, expression);
        }
        if (!(expression instanceof CastExpression)
                && expression instanceof ThisExpression
                && type instanceof GenericType) {
            expression = addCastExpression(type, expression);
        }
        Type selfGenericCastType = resolveSelfGenericCastTarget(localTypeBounds, type, expression);
        if (!(expression instanceof CastExpression) && selfGenericCastType != null) {
            expression = addCastExpression(selfGenericCastType, expression);
        }
        if (!forceCast && shouldPreserveConcreteArgumentForGenericParameter(localTypeBounds, type, unboundType, expression)) {
            expression.accept(this);
            return expression;
        }
        if (!forceCast && shouldPreserveGenericClassArgument(type, unboundType, expression)) {
            expression.accept(this);
            return expression;
        }

        if (visitingAnonymousClass && expression instanceof FieldReferenceExpression fieldRef && expression.getType() instanceof ObjectType) {
            ObjectType ot = (ObjectType) expression.getType();
            if (ot.getTypeArguments() == null) {
                BaseTypeArgument parameterTypeArgument = parameterTypeArguments.get(expression.getName());
                if (parameterTypeArgument != null) {
                    fieldRef.setType(ot.createType(parameterTypeArgument));
                }
            }
        }
        expression = updateExpression(typeBindings, localTypeBounds, type, unboundType, expression, forceCast, unique, rawCast);

        if (!(expression instanceof CastExpression)
                && shouldAddTypedArrayInvocationParameterCast(type, expression)) {
            expression = addExplicitCastExpression(type, expression);
        }

        if (!(expression instanceof CastExpression)
                && shouldAddParameterizedArgumentCast(typeBindings, localTypeBounds, type, expression)) {
            expression = addExplicitCastExpression(type, expression);
        }

        if (!(expression instanceof CastExpression)
                && shouldAddParameterizedArrayConstructorReferenceCast(type, expression)) {
            expression = addExplicitCastExpression(type, expression);
        }

        if (!(expression instanceof CastExpression)
                && shouldAddOverloadBridgeParameterCast(typeBindings, localTypeBounds, type, unboundType, expression, forceCast, unique, descriptorBridgeParameterTypes)) {
            expression = addCastExpression(type, expression);
        }

        if (type == TYPE_BYTE) {
            if (expression.isIntegerConstantExpression()) {
                expression = new CastExpression(TYPE_BYTE, expression);
            } else if (expression.isTernaryOperatorExpression()) {
                Expression exp = expression.getTrueExpression();

                if (exp.isIntegerConstantExpression() || exp.isTernaryOperatorExpression()) {
                    expression = new CastExpression(TYPE_BYTE, expression);
                } else {
                    exp = expression.getFalseExpression();

                    if (exp.isIntegerConstantExpression() || exp.isTernaryOperatorExpression()) {
                        expression = new CastExpression(TYPE_BYTE, expression);
                    }
                }
            }
        }

        for (PrimitiveType primitiveType : Arrays.asList(TYPE_BYTE, TYPE_SHORT)) {
            if (primitiveType.createType(type.getDimension()).equals(type) && expression.isNewInitializedArray()) {
                NewInitializedArray newInitializedArray = (NewInitializedArray) expression;
                for (VariableInitializer variableInitializer : newInitializedArray.getArrayInitializer()) {
                    if (variableInitializer instanceof ExpressionVariableInitializer evi) {
                        evi.setExpression(new CastExpression(primitiveType, evi.getExpression()));
                    }
                }
            }
        }

        return expression;
    }

    private BaseType resolveConstructorUnboundParameterTypes(ClassFileNewExpression expression, BaseType parameterTypes, BaseExpression parameters) {
        TypeArguments typeArguments = getConstructedTypeArguments(expression);

        if (parameterTypes == null
                || parameters == null) {
            return null;
        }

        if (parameterTypes.isList()) {
            if (!parameters.isList()) {
                return null;
            }
            Types resolved = new Types(parameterTypes.size());
            DefaultList<Type> parameterTypeList = parameterTypes.getList();
            DefaultList<Expression> parameterExpressionList = parameters.getList();
            for (int i = 0; i < parameterTypeList.size(); i++) {
                Type resolvedType = resolveConstructorUnboundParameterType(expression, parameterTypeList.get(i), parameterExpressionList.get(i), typeArguments);
                resolved.add(resolvedType == null ? parameterTypeList.get(i) : resolvedType);
            }
            return resolved;
        }

        Type resolvedType = resolveConstructorUnboundParameterType(expression, parameterTypes.getFirst(), parameters.getFirst(), typeArguments);
        return resolvedType == null ? null : resolvedType;
    }

    private TypeArguments getConstructedTypeArguments(ClassFileNewExpression expression) {
        if (expression.getObjectType().getTypeArguments() instanceof TypeArguments typeArguments) {
            return typeArguments;
        }
        if (expression.getType() instanceof ObjectType expressionType
                && expressionType.getTypeArguments() instanceof TypeArguments typeArguments) {
            return typeArguments;
        }
        return null;
    }

    private Type resolveConstructorUnboundParameterType(
            ClassFileNewExpression expression,
            Type parameterType,
            Expression parameterExpression,
            TypeArguments typeArguments) {
        if (!(parameterType instanceof ObjectType parameterObjectType)
                || !(parameterExpression instanceof ThisExpression)
                || !(parameterExpression.getType() instanceof ObjectType expressionObjectType)) {
            return null;
        }

        if (typeArguments != null) {
            for (TypeArgument typeArgument : typeArguments) {
                if (typeArgument instanceof GenericType genericType
                        && typeBounds.get(genericType.getName()) instanceof ObjectType boundObjectType
                        && parameterObjectType.rawEquals(boundObjectType)
                        && expressionObjectType.rawEquals(boundObjectType)) {
                    return genericType;
                }
            }
        }

        TypeTypes typeTypes = typeMaker.makeTypeTypes(expression.getObjectType().getInternalName());
        if (typeTypes == null || typeTypes.getTypeParameters() == null) {
            return null;
        }

        for (org.jd.core.v1.model.javasyntax.type.TypeParameter typeParameter : typeTypes.getTypeParameters()) {
            if (typeParameter instanceof TypeParameterWithTypeBounds parameterWithTypeBounds
                    && parameterWithTypeBounds.getTypeBounds() instanceof ObjectType boundObjectType
                    && parameterObjectType.rawEquals(boundObjectType)
                    && expressionObjectType.rawEquals(boundObjectType)) {
                return new GenericType(parameterWithTypeBounds.getIdentifier());
            }
        }

        return null;
    }

    private Expression updateExpression(Map<String, TypeArgument> typeBindings, Map<String, BaseType> localTypeBounds, Type type, Type unboundType, Expression expression, boolean forceCast, boolean unique, boolean rawCast) {
        if (type instanceof ObjectType targetObjectType
                && expression instanceof CastExpression castExpression
                && castExpression.getType() instanceof ObjectType castObjectType
                && !(castExpression.getExpression() instanceof TypeReferenceDotClassExpression)
                && targetObjectType.rawEquals(castObjectType)
                && hasKnownTypeParameters(targetObjectType)) {
            expression = addCastExpression(type, expression);
        }
        if (expression.isNullExpression()) {
            if (forceCast) {
                searchFirstLineNumberVisitor.init();
                expression.accept(searchFirstLineNumberVisitor);
                expression = new CastExpression(searchFirstLineNumberVisitor.getLineNumber(), type, expression);
            }
        } else if ("java/util/stream/Collectors".equals(expression.getInternalTypeName()) && "toList".equals(expression.getName())) {
        } else if (forceCast
                && (expression instanceof LambdaIdentifiersExpression || expression instanceof MethodReferenceExpression)
                && unboundType instanceof GenericType gt
                && unboundType.getDimension() == 0
                && localTypeBounds.get(gt.getName()) instanceof ObjectType ot) {
            expression = addCastExpression(ot, expression);
        } else {
            Type expressionType = expression.getType();

            if (shouldAddReflectiveResultCast(type, expression)) {
                expression = addExplicitCastExpression(type, expression);
                if (expression instanceof CastExpression castExpression && isCastToBeRemoved(typeBindings, localTypeBounds, type, unboundType, castExpression, unique)) {
                    expression = expression.getExpression();
                }
                expression.accept(this);
                return expression;
            }
            if (shouldAddOriginalVariableCast(localTypeBounds, type, expression)) {
                expression = addExplicitCastExpression(type, expression);
                if (expression instanceof CastExpression castExpression && isCastToBeRemoved(typeBindings, localTypeBounds, type, unboundType, castExpression, unique)) {
                    expression = expression.getExpression();
                }
                expression.accept(this);
                return expression;
            }

            if (forceCast
                    && type.isObjectType()
                    && expressionType.isObjectType()
                    && type.equals(expressionType)
                    && (expression instanceof LambdaIdentifiersExpression || expression instanceof MethodReferenceExpression)) {
                expression = addCastExpression(type, expression);
            }

            if (!expressionType.equals(type)) {
                if (type.isObjectType()) {
                    if (expressionType.isObjectType()) {
                        ObjectType objectType = (ObjectType) type;
                        ObjectType expressionObjectType = (ObjectType) expressionType;
                        if (rawCast) {
                            expression = addCastExpression(objectType.createType(null), expression);
                        } else if (expression instanceof TypeReferenceDotClassExpression
                                && "java/lang/Class".equals(objectType.getInternalName())) {
                            expression.accept(this);
                            return expression;
                        } else if (!(expression instanceof ThisExpression)
                                && unboundType instanceof GenericType genericType
                                && localTypeBounds.get(genericType.getName()) instanceof ObjectType boundObjectType
                                && boundObjectType.rawEquals(objectType)
                                && boundObjectType.rawEquals(expressionObjectType)) {
                            expression.accept(this);
                            return expression;
                        } else if (forceCast && (!objectType.rawEquals(expressionObjectType)
                                || expression instanceof LambdaIdentifiersExpression
                                || expression instanceof MethodReferenceExpression)) {
                            if (expression.isNewExpression()) {
                                ClassFileNewExpression ne = (ClassFileNewExpression) expression;
                                ne.setObjectType(ne.getObjectType().createType(null));
                            }
                            if (expression instanceof LambdaIdentifiersExpression || expression instanceof MethodReferenceExpression) {
                                expression = addCastExpression(objectType, expression);
                            } else {
                                expression = addCastExpression(objectType.createType(null), expression);
                            }
                        } else if (!isJavaLangObject(type) && !typeMaker.isAssignable(typeBindings, localTypeBounds, objectType, unboundType, expressionObjectType)) {
                            if (shouldAddTypedArrayToArrayResultCast(objectType, expressionObjectType, expression)) {
                                expression = addExplicitCastExpression(type, expression);
                            } else if (sameRawArrayType(objectType, expressionObjectType)) {
                                if (shouldPreserveParameterizedSameRawArrayCast(objectType, expressionObjectType, expression)) {
                                    expression = addExplicitCastExpression(type, expression);
                                } else {
                                    expression.accept(this);
                                    return expression;
                                }
                            }
                            if (expression.isMethodInvocationExpression()
                                    && objectType.getTypeArguments() != null
                                    && isErasedHierarchyAssignable(objectType, expressionObjectType)
                                    && !shouldKeepParameterizedClassResultCast(objectType, expressionObjectType)) {
                                expression.accept(this);
                                return expression;
                            }
                            BaseTypeArgument ta1 = objectType.getTypeArguments();
                            BaseTypeArgument ta2 = expressionObjectType.getTypeArguments();
                            Type t = type;

                            if (ta1 != null && ta2 != null && !ta1.isTypeArgumentAssignableFrom(typeMaker, typeBindings, localTypeBounds, ta2)) {
                                t = objectType.createType(ta1.isGenericTypeArgument() ? ta1 : null);
                            }
                            if (!expression.isNew() && hasKnownTypeParameters(t) && !(ta1 instanceof WildcardSuperTypeArgument)) {
                                expression = addCastExpression(t, expression);
                            }
                        }
                    } else if (forceCast
                            && type instanceof ObjectType objectType
                            && ObjectType.TYPE_OBJECT.rawEquals(objectType)
                            && expressionType.isPrimitiveType()) {
                        expression = addCastExpression(type, expression);
                    } else if (type.getDimension() == 0 && expressionType.isGenericType() && (!isJavaLangObject(type) || forceCast)) {
                        boolean cast = true;
                        if (expressionType instanceof GenericType gt) {
                            BaseType boundType = typeBounds.get(gt.getName());
                            if (boundType instanceof ObjectType boundObjectType && boundObjectType.rawEquals(type)) {
                                cast = false;
                            }
                        }
                        if (cast) {
                            expression = addCastExpression(type, expression);
                        }
                    }
                } else if (type.isGenericType()
                        && hasKnownTypeParameters(type)
                        && (expressionType.isObjectType() || expressionType.isGenericType())
                        && (type.getDimension() != 0 || !visitingLambda)
                        && !shouldPreserveBoxedGenericArgument(localTypeBounds, type, expression)
                        && !shouldDeferToGenericMethodInference(type, unboundType, expression)
                        && !shouldDeferToSourceGenericResult(type, unboundType, expression)) {
                    expression = addCastExpression(type, expression);
                }
            }

            if (expression instanceof CastExpression castExpression
                    && isCastToBeRemoved(typeBindings, localTypeBounds, type, unboundType, castExpression, unique)) {
                expression = expression.getExpression();
            }
            expression.accept(this);
        }

        return expression;
    }

    private boolean shouldPreserveBoxedGenericArgument(
            Map<String, BaseType> localTypeBounds,
            Type targetType,
            Expression expression) {
        if (!(targetType instanceof GenericType genericType)
                || !(expression instanceof MethodInvocationExpression)
                || !AutoboxingVisitor.isBoxingMethod(expression)
                || !(expression.getType() instanceof ObjectType expressionObjectType)
                || localTypeBounds == null
                || !(localTypeBounds.get(genericType.getName()) instanceof ObjectType boundObjectType)) {
            return false;
        }

        return boundObjectType.rawEquals(expressionObjectType);
    }

    private boolean shouldDeferToSourceGenericResult(Type targetType, Type unboundType, Expression expression) {
        if (!(expression instanceof ClassFileMethodInvocationExpression)
                || !(targetType instanceof GenericType)
                || !hasKnownTypeParameters(targetType)
                || !isSourceInferredGenericHelper(expression)
                || unboundType == null) {
            return false;
        }

        if (targetType.equals(unboundType)) {
            return true;
        }

        return unboundType instanceof ObjectType objectType
                && objectType.findTypeParametersInType().contains(((GenericType) targetType).getName());
    }

    private boolean isCastToBeRemoved(Map<String, TypeArgument> typeBindings, Map<String, BaseType> localTypeBounds, Type type, Type unboundType, CastExpression expression, boolean unique) {
        if (expression.getIntersectType() != null) {
            return false;
        }
        if (expression.isByteCodeCheckCast()
                && expression.getExpression() instanceof ClassFileMethodInvocationExpression methodInvocationExpression
                && "java/util/Optional".equals(methodInvocationExpression.getInternalTypeName())
                && "orElseThrow".equals(methodInvocationExpression.getName())) {
            return false;
        }
        if (expression.isByteCodeCheckCast()
                && expression.getExpression() instanceof ClassFileMethodInvocationExpression methodInvocationExpression
                && "unwrap".equals(methodInvocationExpression.getName())
                && (methodInvocationExpression.getParameters() == null || methodInvocationExpression.getParameters().size() == 0)) {
            return true;
        }
        if (shouldPreserveReflectiveResultCast(type, expression)) {
            return false;
        }
        if (shouldPreserveOriginalVariableCast(type, expression)) {
            return false;
        }
        if (shouldPreserveThisGenericCast(type, expression)) {
            return false;
        }
        Expression nestedExpression = expression.getExpression();
        if (type instanceof ObjectType castObjectType
                && castObjectType.getDimension() > 0
                && nestedExpression instanceof MethodInvocationExpression methodInvocationExpression
                && requiresTypedArrayResultCastForRawReceiver(castObjectType, methodInvocationExpression)) {
            return false;
        }
        if (isParameterizedJavaLangObject(expression.getType())) {
            return true;
        }
        if (!hasKnownTypeParameters(expression.getType())) {
            return true;
        }
        if (isArrayConstructorToArrayCast(type, nestedExpression)) {
            return true;
        }
        if (isTypedArrayToArrayCast(type, nestedExpression)) {
            return true;
        }
        if (nestedExpression.getExpression() instanceof FieldReferenceExpression fre && fieldNamesInLambda.contains(fre.getName())) {
            return false;
        }
        Type nestedExpressionType = nestedExpression.getType();

        if (shouldAddTypedArrayToArrayResultCast(type, nestedExpressionType, nestedExpression)) {
            return false;
        }
        if (type instanceof ObjectType leftObjectType
                && nestedExpressionType instanceof ObjectType rightObjectType
                && shouldPreserveParameterizedSameRawArrayCast(leftObjectType, rightObjectType, nestedExpression)) {
            return false;
        }

        if (type.isObjectType() && nestedExpressionType.isObjectType()) {
            ObjectType left = (ObjectType) type;
            ObjectType right = (ObjectType) nestedExpressionType;
            if (expression.isByteCodeCheckCast()
                    && nestedExpression instanceof ClassFileMethodInvocationExpression methodInvocationExpression
                    && methodInvocationExpression.getUnboundType() instanceof GenericType
                    && hasUnboundedWildcardTypeArgument(methodInvocationExpression.getExpression())) {
                return false;
            }
            if (unique
                    && expression.isByteCodeCheckCast()
                    && nestedExpression instanceof ClassFileMethodInvocationExpression methodInvocationExpression
                    && methodInvocationExpression.getTypeParameters() != null
                    && left.getTypeArguments() != null
                    && isJavaLangObject(right)) {
                return true;
            }
            if (unique
                    && nestedExpression.isMethodInvocationExpression()
                    && left.getTypeArguments() != null
                    && right.getTypeArguments() != null
                    && !left.rawEquals(right)
                    && typeMaker.isRawTypeAssignable(left, right)) {
                return true;
            }
            if (expression.isByteCodeCheckCast()
                    && nestedExpression.isMethodInvocationExpression()
                    && left.getTypeArguments() == null
                    && left.rawEquals(right)
                    && hasUnboundedWildcardTypeArgument(right)) {
                return true;
            }
            if (nestedExpression.isMethodInvocationExpression()
                    && left.getTypeArguments() != null
                    && right.getTypeArguments() != null
                    && !left.rawEquals(right)
                    && typeMaker.isRawTypeAssignable(left, right)
                    && !hasUnboundedWildcardTypeArgument(right)) {
                return true;
            }
            if (expression.isByteCodeCheckCast()
                    && nestedExpression instanceof ClassFileMethodInvocationExpression methodInvocationExpression
                    && "java/util/Arrays".equals(methodInvocationExpression.getInternalTypeName())
                    && "asList".equals(methodInvocationExpression.getName())
                    && left.getTypeArguments() != null
                    && typeMaker.isRawTypeAssignable(left, right)) {
                return true;
            }
            if (!visitingLambda
                    && nestedExpression instanceof ClassFileMethodInvocationExpression mie
                    && mie.getUnboundType() instanceof GenericType
                    && mie.getParameters() != null
                    && typeMaker.matchCount(mie.getInternalTypeName(), mie.getName(), mie.getParameters().size(), true) > 1
                    && typeMaker.matchCount(mie.getTypeBindings(), mie.getTypeBounds(), mie.getInternalTypeName(), mie.getName(), mie.getParameters(), true) > 1) {
                return false;
            }
            if (left.equals(right) || unique && typeMaker.isAssignable(typeBindings, localTypeBounds, left, right)) {
                return true;
            }
        }
        if (type instanceof GenericType genericType
                && localTypeBounds.get(genericType.getName()) instanceof ObjectType boundObjectType
                && expression.getType().isObjectType()
                && boundObjectType.rawEquals((ObjectType) expression.getType())) {
            if (nestedExpressionType instanceof GenericType nestedGenericType
                    && nestedGenericType.getName().equals(genericType.getName())) {
                return true;
            }
            if (nestedExpressionType.isObjectType()
                    && (boundObjectType.rawEquals((ObjectType) nestedExpressionType)
                    || typeMaker.isRawTypeAssignable(boundObjectType, (ObjectType) nestedExpressionType))) {
                return true;
            }
            if (nestedExpression instanceof MethodInvocationExpression methodInvocationExpression
                    && methodInvocationExpression.getExpression() != null
                    && methodInvocationExpression.getExpression().getType() instanceof ObjectType receiverType
                    && receiverType.findTypeParametersInType().contains(genericType.getName())) {
                return true;
            }
        }
        return false;
    }

    private boolean shouldAddTypedArrayToArrayResultCast(Type targetType, Type expressionType, Expression expression) {
        if (!(targetType instanceof ObjectType targetObjectType)
                || !(expressionType instanceof ObjectType expressionObjectType)
                || targetType.getDimension() == 0
                || !expression.isMethodInvocationExpression()
                || isRawObjectArray(targetObjectType)) {
            return false;
        }

        if ("toArray".equals(expression.getName())) {
            return isRawObjectArray(expressionObjectType)
                    || (sameRawArrayType(targetObjectType, expressionObjectType)
                    && (hasKnownTypeParameters(targetObjectType) || hasUnboundedWildcardTypeArgument(expressionObjectType)));
        }

        return isRawObjectArray(expressionObjectType)
                && expression instanceof ClassFileMethodInvocationExpression methodInvocationExpression
                && hasGenericArrayInvocationShape(methodInvocationExpression)
                || (sameRawArrayType(targetObjectType, expressionObjectType)
                && (hasKnownTypeParameters(targetObjectType) || hasUnboundedWildcardTypeArgument(expressionObjectType)));
    }

    private boolean hasGenericArrayInvocationShape(ClassFileMethodInvocationExpression methodInvocationExpression) {
        if (hasKnownTypeParameters(methodInvocationExpression.getType())
                || hasKnownTypeParameters(methodInvocationExpression.getUnboundType())
                || methodInvocationExpression.getTypeParameters() != null
                || hasKnownTypeParameters(methodInvocationExpression.getParameterTypes())
                || hasKnownTypeParameters(methodInvocationExpression.getUnboundParameterTypes())) {
            return true;
        }

        BaseExpression parameters = methodInvocationExpression.getParameters();
        if (parameters == null) {
            return false;
        }

        for (Expression parameter : parameters) {
            Expression coreParameter = unwrapCastExpression(parameter);
            if (coreParameter instanceof LambdaIdentifiersExpression
                    || coreParameter instanceof MethodReferenceExpression
                    || hasKnownTypeParameters(coreParameter.getType())) {
                return true;
            }
        }
        return false;
    }

    private boolean shouldAddTypedArrayInvocationParameterCast(Type targetType, Expression expression) {
        Expression coreExpression = unwrapCastExpression(expression);
        if (!(targetType instanceof ObjectType targetObjectType)
                || targetType.getDimension() == 0
                || isRawObjectArray(targetObjectType)
                || !(coreExpression instanceof MethodInvocationExpression methodInvocationExpression)
                || !"toArray".equals(methodInvocationExpression.getName())
                || !(coreExpression.getType() instanceof ObjectType expressionObjectType)
                || expressionObjectType.getDimension() != targetObjectType.getDimension()) {
            return false;
        }

        return isRawObjectArray(expressionObjectType)
                || sameRawArrayType(targetObjectType, expressionObjectType)
                || requiresTypedArrayResultCastForRawReceiver(targetObjectType, methodInvocationExpression);
    }

    private boolean requiresTypedArrayResultCastForRawReceiver(ObjectType targetType, MethodInvocationExpression expression) {
        if (!(expression instanceof ClassFileMethodInvocationExpression methodInvocationExpression)
                || !(expression.getExpression() instanceof CastExpression castExpression)
                || !(castExpression.getType() instanceof ObjectType castType)
                || castType.getTypeArguments() != null
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
                && unboundParameterType instanceof GenericType genericType
                && genericType.getDimension() == targetType.getDimension();
    }

    private boolean shouldPreserveParameterizedSameRawArrayCast(
            ObjectType targetType,
            ObjectType expressionType,
            Expression expression) {
        if (!sameRawArrayType(targetType, expressionType)
                || !hasKnownTypeParameters(targetType)) {
            return false;
        }

        return hasUnboundedWildcardTypeArgument(expressionType)
                || expression instanceof FieldReferenceExpression
                || expression instanceof LocalVariableReferenceExpression
                || expression.isMethodInvocationExpression();
    }

    private boolean shouldKeepExplicitMethodInvocationCast(Type type, Type unboundType, CastExpression expression) {
        if (!(expression.getExpression() instanceof ClassFileMethodInvocationExpression methodInvocationExpression)
                || methodInvocationExpression.getTypeParameters() != null
                || !(type instanceof ObjectType targetType)
                || !(expression.getType() instanceof ObjectType castType)
                || !(methodInvocationExpression.getType() instanceof ObjectType expressionType)
                || !castType.equals(targetType)
                || !castType.rawEquals(expressionType)
                || castType.getTypeArguments() == null
                || !hasUnboundedWildcardTypeArgument(expressionType)) {
            return false;
        }

        if (unboundType == null) {
            return true;
        }
        if (unboundType instanceof ObjectType unboundObjectType) {
            return castType.rawEquals(unboundObjectType);
        }
        return unboundType.equals(castType);
    }

    private boolean shouldAddRequiredMethodInvocationParameterCast(Type type, Expression expression) {
        if (!(type instanceof ObjectType targetType)
                || !(expression instanceof ClassFileMethodInvocationExpression methodInvocationExpression)
                || methodInvocationExpression.getTypeParameters() != null
                || !(expression.getType() instanceof ObjectType expressionType)
                || !targetType.rawEquals(expressionType)
                || targetType.getTypeArguments() == null
                || !hasUnboundedWildcardTypeArgument(expressionType)) {
            return false;
        }

        return !targetType.equals(expressionType);
    }

    private boolean shouldPreserveParameterizedClassLiteralCast(Type type, CastExpression expression) {
        return type instanceof ObjectType objectType
                && "java/lang/Class".equals(objectType.getInternalName())
                && objectType.getTypeArguments() != null
                && expression.getType() instanceof ObjectType castObjectType
                && "java/lang/Class".equals(castObjectType.getInternalName())
                && castObjectType.getTypeArguments() != null
                && expression.getExpression() instanceof TypeReferenceDotClassExpression;
    }

    private boolean shouldPreserveReflectiveResultCast(Type type, CastExpression expression) {
        return expression.getExpression() instanceof ClassFileMethodInvocationExpression methodInvocationExpression
                && shouldAddReflectiveResultCast(type, methodInvocationExpression);
    }

    private boolean shouldAddOriginalVariableCast(Map<String, BaseType> localTypeBounds, Type type, Expression expression) {
        if (!(type instanceof ObjectType targetObjectType)
                || !(expression instanceof ClassFileLocalVariableReferenceExpression localVariableReferenceExpression)
                || !(expression.getType() instanceof ObjectType expressionObjectType)
                || !targetObjectType.rawEquals(expressionObjectType)) {
            return false;
        }

        ObjectType originalObjectType = resolveSourceObjectType(localVariableReferenceExpression.getLocalVariable());
        if (originalObjectType == null) {
            return false;
        }

        return !typeMaker.isAssignable(
                localTypeBounds == null ? Collections.emptyMap() : localTypeBounds,
                targetObjectType,
                originalObjectType);
    }

    private boolean shouldPreserveOriginalVariableCast(Type type, CastExpression expression) {
        if (!(expression.getExpression() instanceof ClassFileLocalVariableReferenceExpression localVariableReferenceExpression)) {
            return false;
        }
        return shouldAddOriginalVariableCast(typeBounds, type, localVariableReferenceExpression);
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

    private boolean shouldPreserveThisGenericCast(Type type, CastExpression expression) {
        return type instanceof GenericType && expression.getExpression() instanceof ThisExpression;
    }

    private boolean prepareDiamondTypeArgumentsIfPossible(NewExpression expression) {
        TypeTypes typeTypes = typeMaker.makeTypeTypes(expression.getObjectType().getInternalName());
        if (typeTypes == null || typeTypes.getTypeParameters() == null) {
            return false;
        }
        TypeArguments typeArguments = new TypeArguments(typeTypes.getTypeParameters().size());
        for (int i = 0; i < typeTypes.getTypeParameters().size(); i++) {
            typeArguments.add(WildcardTypeArgument.WILDCARD_TYPE_ARGUMENT);
        }
        expression.setObjectType(expression.getObjectType().createType(typeArguments));
        return true;
    }

    private boolean canUseDiamond(Type targetType, NewExpression expression) {
        BaseExpression parameters = expression.getParameters();
        if (parameters == null || parameters.size() == 0) {
            return false;
        }
        ObjectType objectType = expression.getObjectType();
        if (!(targetType instanceof ObjectType targetObjectType) || targetObjectType.getTypeArguments() == null) {
            return false;
        }
        return typeMaker.isRawTypeAssignable(targetObjectType, objectType);
    }

    private static boolean isJavaLangObject(Type type) {
        return type instanceof ObjectType ot && ObjectType.TYPE_OBJECT.rawEquals(ot);
    }

    private static boolean isParameterizedJavaLangObject(Type type) {
        return type instanceof ObjectType ot
                && ObjectType.TYPE_OBJECT.rawEquals(ot)
                && ot.getTypeArguments() != null;
    }

    private static boolean isArrayConstructorToArrayCast(Type castType, Expression nestedExpression) {
        if (castType == null || nestedExpression == null || !nestedExpression.isMethodInvocationExpression() || !"toArray".equals(nestedExpression.getName())) {
            return false;
        }
        BaseExpression parameters = nestedExpression.getParameters();
        if (parameters == null || parameters.size() != 1) {
            return false;
        }
        Expression parameter;
        if (parameters.isList()) {
            parameter = parameters.getFirst();
        } else if (parameters instanceof Expression) {
            parameter = (Expression) parameters;
        } else {
            return false;
        }
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

    private static boolean hasUnboundedWildcardTypeArgument(Expression expression) {
        return expression != null && hasUnboundedWildcardTypeArgument(expression.getType());
    }

    private static boolean hasUnboundedWildcardTypeArgument(Type type) {
        return type instanceof ObjectType objectType && hasUnboundedWildcardTypeArgument(objectType.getTypeArguments());
    }

    private static boolean hasUnboundedWildcardTypeArgument(BaseTypeArgument typeArgument) {
        if (typeArgument == null) {
            return false;
        }
        if (typeArgument.isWildcardTypeArgument() || typeArgument.isWildcardExtendsTypeArgument() || typeArgument.isWildcardSuperTypeArgument()) {
            return true;
        }
        if (typeArgument instanceof TypeArguments typeArguments) {
            for (TypeArgument argument : typeArguments) {
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

    private static boolean isPackagePrivateType(ObjectType objectType) {
        String internalName = objectType.getInternalName();
        int slash = internalName.lastIndexOf('/');
        if (slash < 0) {
            return false;
        }
        String simpleName = internalName.substring(slash + 1);
        return !simpleName.isEmpty() && Character.isLowerCase(simpleName.charAt(0));
    }

    private static Expression unwrapCastExpression(Expression expression) {
        while (expression instanceof CastExpression castExpression) {
            expression = castExpression.getExpression();
        }
        return expression;
    }

    private static boolean isSourceInferredGenericHelper(Expression expression) {
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

    private boolean shouldClearRedundantMethodTypeArguments(MethodInvocationExpression expression) {
        BaseTypeArgument nonWildcardTypeArguments = expression.getNonWildcardTypeArguments();
        BaseExpression parameters = expression.getParameters();

        if (nonWildcardTypeArguments == null || parameters == null || parameters.size() == 0) {
            return false;
        }

        if (nonWildcardTypeArguments.isTypeArgumentList()) {
            for (TypeArgument typeArgument : nonWildcardTypeArguments.getTypeArgumentList()) {
                if (!canInferMethodTypeArgumentFromParameters(typeArgument, parameters)) {
                    return false;
                }
            }
            return true;
        }

        return canInferMethodTypeArgumentFromParameters((TypeArgument) nonWildcardTypeArguments, parameters);
    }

    private boolean canInferMethodTypeArgumentFromParameters(TypeArgument typeArgument, BaseExpression parameters) {
        if (!(typeArgument instanceof Type typeArgumentType)) {
            return false;
        }

        for (Expression parameter : parameters) {
            Type parameterType = unwrapCastExpression(parameter).getType();
            if (parameterType == null) {
                continue;
            }
            if (typeArgumentType instanceof GenericType genericType
                    && parameterType.findTypeParametersInType().contains(genericType.getName())) {
                return true;
            }
            if (typeArgumentType.isObjectType()
                    && parameterType.isObjectType()
                    && (typeMaker.isRawTypeAssignable((ObjectType) parameterType, (ObjectType) typeArgumentType)
                    || ((ObjectType) typeArgumentType).rawEquals((ObjectType) parameterType))) {
                return true;
            }
        }

        return false;
    }

    private boolean shouldAddExplicitGenericResultCast(Type type, Expression expression, Type expressionType) {
        if (!(type instanceof GenericType genericType)
                || !(expression instanceof ClassFileMethodInvocationExpression methodInvocationExpression)) {
            return false;
        }

        boolean matchesGenericResult = expressionType instanceof GenericType
                && genericType.getName().equals(((GenericType) expressionType).getName());
        if (!matchesGenericResult) {
            Type unboundType = methodInvocationExpression.getUnboundType();
            matchesGenericResult = unboundType instanceof GenericType
                    && genericType.getName().equals(((GenericType) unboundType).getName());
        }
        if (!matchesGenericResult) {
            return false;
        }

        Expression receiver = methodInvocationExpression.getExpression();
        if (receiver != null
                && !receiver.isObjectTypeReferenceExpression()
                && !(receiver instanceof ThisExpression)
                && !(receiver instanceof SuperExpression)
                && !(receiver instanceof QualifiedSuperExpression)) {
            return false;
        }

        BaseExpression parameters = methodInvocationExpression.getParameters();
        if (parameters != null) {
            for (Expression parameter : parameters) {
                Type parameterType = parameter.getType();
                if (parameterType != null && infersGenericType(parameterType, genericType.getName())) {
                    return false;
                }
            }
        }

        return true;
    }

    private static boolean shouldAddReflectiveResultCast(Type type, Expression expression) {
        if (!(type instanceof ObjectType)
                || !(expression instanceof ClassFileMethodInvocationExpression methodInvocationExpression)) {
            return false;
        }

        String name = methodInvocationExpression.getName();
        return "newInstance".equals(name)
                || ("invoke".equals(name) && "java/lang/reflect/Method".equals(methodInvocationExpression.getInternalTypeName()))
                || ("forName".equals(name) && "java/lang/Class".equals(methodInvocationExpression.getInternalTypeName()));
    }

    private boolean infersGenericType(Type parameterType, String genericName) {
        if (parameterType instanceof GenericType genericType) {
            return genericName.equals(genericType.getName());
        }
        if (!(parameterType instanceof ObjectType objectType) || objectType.getTypeArguments() == null) {
            return false;
        }
        if ("java/lang/Class".equals(objectType.getInternalName())) {
            return false;
        }
        return objectType.findTypeParametersInType().contains(genericName);
    }

    private boolean shouldDeferToGenericMethodInference(Type type, Type unboundType, Expression expression) {
        Expression coreExpression = unwrapCastExpression(expression);
        if (!(coreExpression instanceof ClassFileMethodInvocationExpression methodInvocationExpression)) {
            return false;
        }

        Type expressionType = coreExpression.getType();
        Type expressionUnboundType = methodInvocationExpression.getUnboundType();
        boolean genericMethodResult = methodInvocationExpression.getTypeParameters() != null
                || hasTypeParameters(unboundType)
                || hasTypeParameters(expressionUnboundType)
                || hasTypeParameters(expressionType);

        if (!genericMethodResult) {
            return false;
        }

        if (type instanceof GenericType genericType
                && expressionType instanceof GenericType expressionGenericType
                && genericType.getName().equals(expressionGenericType.getName())) {
            return true;
        }

        if (canInferGenericMethodResultFromParameters(type, methodInvocationExpression, expressionType, expressionUnboundType)) {
            return true;
        }

        if (canInferGenericArrayResultFromClassParameter(type, methodInvocationExpression, expressionType, expressionUnboundType)) {
            return true;
        }

        if (type.isObjectType() && expressionType.isObjectType()) {
            return ((ObjectType) type).rawEquals((ObjectType) expressionType);
        }

        return type.getDimension() > 0
                && expressionType.getDimension() == type.getDimension()
                && (expressionType.isGenericType() || hasTypeParameters(unboundType) || hasTypeParameters(expressionUnboundType));
    }

    private boolean canInferGenericMethodResultFromParameters(
            Type targetType,
            ClassFileMethodInvocationExpression methodInvocationExpression,
            Type expressionType,
            Type expressionUnboundType) {
        String genericName = extractGenericName(expressionUnboundType);
        if (genericName == null) {
            genericName = extractGenericName(expressionType);
        }
        if (genericName == null) {
            return false;
        }

        BaseExpression parameters = methodInvocationExpression.getParameters();
        BaseType parameterTypes = methodInvocationExpression.getParameterTypes();
        BaseType unboundParameterTypes = methodInvocationExpression.getUnboundParameterTypes();
        if (parameters == null || parameterTypes == null) {
            return false;
        }

        if (parameters.isList()) {
            for (int i = 0; i < parameters.size() && i < parameterTypes.size(); i++) {
                Type declaredType = resolveDeclaredParameterType(parameterTypes, unboundParameterTypes, i);
                if (declaredType != null
                        && declaredType.findTypeParametersInType().contains(genericName)
                        && matchesGenericInferenceTarget(targetType, unwrapCastExpression(parameters.getList().get(i)).getType())) {
                    return true;
                }
            }
            return false;
        }

        Type declaredType = resolveDeclaredParameterType(parameterTypes, unboundParameterTypes, 0);
        return declaredType != null
                && declaredType.findTypeParametersInType().contains(genericName)
                && matchesGenericInferenceTarget(targetType, unwrapCastExpression(parameters.getFirst()).getType());
    }

    private boolean canInferGenericArrayResultFromClassParameter(
            Type targetType,
            ClassFileMethodInvocationExpression methodInvocationExpression,
            Type expressionType,
            Type expressionUnboundType) {
        if (targetType == null
                || targetType.getDimension() == 0
                || expressionType == null
                || expressionType.getDimension() != targetType.getDimension()) {
            return false;
        }

        String genericName = extractGenericName(expressionUnboundType);
        if (genericName == null) {
            genericName = extractGenericName(expressionType);
        }
        if (genericName == null) {
            return false;
        }

        BaseExpression parameters = methodInvocationExpression.getParameters();
        BaseType parameterTypes = methodInvocationExpression.getParameterTypes();
        BaseType unboundParameterTypes = methodInvocationExpression.getUnboundParameterTypes();
        if (parameters == null || parameterTypes == null) {
            return false;
        }

        if (parameters.isList()) {
            for (int i = 0; i < parameters.size() && i < parameterTypes.size(); i++) {
                Type declaredType = resolveDeclaredParameterType(parameterTypes, unboundParameterTypes, i);
                Expression parameter = unwrapCastExpression(parameters.getList().get(i));
                if (isClassParameterInferringArrayGeneric(targetType, declaredType, parameter, genericName)) {
                    return true;
                }
            }
            return false;
        }

        Type declaredType = resolveDeclaredParameterType(parameterTypes, unboundParameterTypes, 0);
        return isClassParameterInferringArrayGeneric(targetType, declaredType, unwrapCastExpression(parameters.getFirst()), genericName);
    }

    private static boolean isClassParameterInferringArrayGeneric(
            Type targetType,
            Type declaredType,
            Expression parameter,
            String genericName) {
        if (!(declaredType instanceof ObjectType declaredObjectType)
                || !"java/lang/Class".equals(declaredObjectType.getInternalName())
                || !declaredType.findTypeParametersInType().contains(genericName)
                || parameter == null
                || !(parameter.getType() instanceof ObjectType parameterObjectType)
                || !"java/lang/Class".equals(parameterObjectType.getInternalName())) {
            return false;
        }

        Set<String> targetGenericNames = targetType.findTypeParametersInType();
        if (targetGenericNames.isEmpty()) {
            return false;
        }

        Set<String> parameterGenericNames = parameterObjectType.findTypeParametersInType();
        if (parameterGenericNames.isEmpty()) {
            return false;
        }

        for (String targetGenericName : targetGenericNames) {
            if (parameterGenericNames.contains(targetGenericName)) {
                return true;
            }
        }

        return false;
    }

    private static String extractGenericName(Type type) {
        if (type instanceof GenericType genericType) {
            return genericType.getName();
        }

        if (type != null) {
            Set<String> genericNames = type.findTypeParametersInType();
            if (genericNames.size() == 1) {
                return genericNames.iterator().next();
            }
        }

        return null;
    }

    private static Type resolveDeclaredParameterType(BaseType parameterTypes, BaseType unboundParameterTypes, int index) {
        if (unboundParameterTypes != null) {
            if (unboundParameterTypes.isList()) {
                if (index < unboundParameterTypes.size()) {
                    return unboundParameterTypes.getList().get(index);
                }
            } else if (index == 0) {
                return unboundParameterTypes.getFirst();
            }
        }

        if (parameterTypes.isList()) {
            return index < parameterTypes.size() ? parameterTypes.getList().get(index) : null;
        }
        return index == 0 ? parameterTypes.getFirst() : null;
    }

    private boolean matchesGenericInferenceTarget(Type targetType, Type argumentType) {
        if (targetType == null || argumentType == null) {
            return false;
        }
        if (targetType instanceof GenericType targetGenericType
                && argumentType instanceof GenericType argumentGenericType
                && targetGenericType.getName().equals(argumentGenericType.getName())) {
            return true;
        }
        if (targetType.getDimension() > 0) {
            return targetType.getDimension() == argumentType.getDimension();
        }
        if (targetType.isObjectType() && argumentType.isObjectType()) {
            ObjectType targetObjectType = (ObjectType) targetType;
            ObjectType argumentObjectType = (ObjectType) argumentType;
            return targetObjectType.rawEquals(argumentObjectType)
                    || typeMaker.isRawTypeAssignable(targetObjectType, argumentObjectType)
                    || typeMaker.isRawTypeAssignable(argumentObjectType, targetObjectType);
        }
        return false;
    }

    private boolean shouldAddOverloadBridgeParameterCast(
            Map<String, TypeArgument> typeBindings,
            Map<String, BaseType> localTypeBounds,
            Type type,
            Type unboundType,
            Expression expression,
            boolean forceCast,
            boolean unique,
            boolean descriptorBridgeParameterTypes) {
        if ((!(descriptorBridgeParameterTypes || hasTypeParameters(unboundType) || !unique))
                || forceCast
                || !type.isObjectType()
                || !expression.getType().isObjectType()) {
            return false;
        }
        if (expression.isNullExpression()) {
            return false;
        }
        if (expression instanceof LambdaIdentifiersExpression || expression instanceof MethodReferenceExpression) {
            return false;
        }

        ObjectType targetType = (ObjectType) type;
        ObjectType expressionType = (ObjectType) expression.getType();

        if (expression.isMethodInvocationExpression()
                && targetType.getTypeArguments() != null
                && hasTypeParameters(unboundType)) {
            return false;
        }

        if (!descriptorBridgeParameterTypes
                && !hasTypeParameters(unboundType)) {
            return false;
        }

        return !targetType.rawEquals(expressionType)
                && (typeMaker.isAssignable(typeBindings, localTypeBounds, targetType, unboundType, expressionType)
                || typeMaker.isRawTypeAssignable(targetType, expressionType));
    }

    private boolean shouldAddParameterizedArgumentCast(
            Map<String, TypeArgument> typeBindings,
            Map<String, BaseType> localTypeBounds,
            Type type,
            Expression expression) {
        if (!(type instanceof ObjectType targetType)
                || !(expression.getType() instanceof ObjectType expressionType)
                || targetType.getTypeArguments() == null
                || expressionType.getTypeArguments() == null
                || "java/lang/Class".equals(targetType.getInternalName())
                || !(expression instanceof LocalVariableReferenceExpression || expression instanceof FieldReferenceExpression)
                || !typeMaker.isRawTypeAssignable(targetType, expressionType)) {
            return false;
        }

        return !targetType.getTypeArguments().isTypeArgumentAssignableFrom(
                typeMaker,
                typeBindings,
                localTypeBounds,
                expressionType.getTypeArguments());
    }

    private boolean shouldAddParameterizedArrayConstructorReferenceCast(Type type, Expression expression) {
        if (!(type instanceof ObjectType targetType)
                || !(expression instanceof MethodReferenceExpression methodReference)
                || !"new".equals(methodReference.getName())
                || targetType.getTypeArguments() == null
                || !targetType.getTypeArguments().isTypeArgumentList()) {
            return false;
        }

        for (TypeArgument typeArgument : targetType.getTypeArguments().getTypeArgumentList()) {
            if (typeArgument instanceof Type argumentType
                    && argumentType.getDimension() > 0
                    && argumentType.isObjectType()
                    && ((ObjectType) argumentType).getTypeArguments() != null) {
                return true;
            }
        }

        return false;
    }

    private BaseExpression addRequiredRawCollectionConstructorCasts(
            ObjectType objectType,
            BaseType parameterTypes,
            BaseExpression parameters) {
        if (!(objectType.getTypeArguments() instanceof TypeArguments)
                || parameterTypes == null
                || parameters == null) {
            return parameters;
        }

        if (parameters.isList()) {
            DefaultList<Expression> parameterList = parameters.getList();
            DefaultList<Type> parameterTypeList = parameterTypes.isList() ? parameterTypes.getList() : null;
            for (int i = 0; i < parameterList.size() && parameterTypeList != null && i < parameterTypeList.size(); i++) {
                parameterList.set(i, addRequiredRawCollectionConstructorCast(parameterTypeList.get(i), parameterList.get(i)));
            }
            return parameters;
        }

        return addRequiredRawCollectionConstructorCast(parameterTypes.getFirst(), parameters.getFirst());
    }

    private Expression addRequiredRawCollectionConstructorCast(Type parameterType, Expression parameter) {
        if (parameter instanceof CastExpression
                || !(parameterType instanceof ObjectType parameterObjectType)
                || parameterObjectType.getTypeArguments() != null
                || !"java/util/Collection".equals(parameterObjectType.getInternalName())
                || !(parameter.getType() instanceof ObjectType expressionObjectType)
                || expressionObjectType.getTypeArguments() == null
                || !(parameter instanceof LocalVariableReferenceExpression || parameter instanceof FieldReferenceExpression)
                || !(parameterObjectType.rawEquals(expressionObjectType)
                || "java/util/List".equals(expressionObjectType.getInternalName())
                || "java/util/Set".equals(expressionObjectType.getInternalName()))) {
            return parameter;
        }

        return addExplicitCastExpression(parameterType, parameter);
    }

    private BaseExpression addTargetTypedCollectionConstructorCasts(
            Type currentType,
            BaseType parameterTypes,
            BaseExpression parameters) {
        if (!(currentType instanceof ObjectType currentObjectType)
                || currentObjectType.getTypeArguments() == null
                || parameterTypes == null
                || parameters == null) {
            return parameters;
        }

        TypeArgument targetArgument = currentObjectType.getTypeArguments().isTypeArgumentList()
                ? currentObjectType.getTypeArguments().getTypeArgumentList().getFirst()
                : currentObjectType.getTypeArguments().getTypeArgumentFirst();
        if (targetArgument == null) {
            return parameters;
        }

        if (parameters.isList()) {
            DefaultList<Expression> parameterList = parameters.getList();
            DefaultList<Type> parameterTypeList = parameterTypes.isList() ? parameterTypes.getList() : null;
            if (parameterTypeList == null) {
                return parameters;
            }
            for (int i = 0; i < parameterList.size() && i < parameterTypeList.size(); i++) {
                parameterList.set(i, addTargetTypedCollectionConstructorCast(targetArgument, parameterTypeList.get(i), parameterList.get(i)));
            }
            return parameters;
        }

        return addTargetTypedCollectionConstructorCast(targetArgument, parameterTypes.getFirst(), parameters.getFirst());
    }

    private Expression addTargetTypedCollectionConstructorCast(TypeArgument targetArgument, Type parameterType, Expression parameter) {
        if (parameter instanceof CastExpression
                || !(targetArgument instanceof Type targetTypeArgument)
                || !(parameterType instanceof ObjectType parameterObjectType)
                || !"java/util/Collection".equals(parameterObjectType.getInternalName())
                || !(parameter.getType() instanceof ObjectType expressionObjectType)
                || expressionObjectType.getTypeArguments() == null
                || !(parameter instanceof LocalVariableReferenceExpression || parameter instanceof FieldReferenceExpression)) {
            return parameter;
        }

        TypeArguments targetCollectionArguments = new TypeArguments();
        targetCollectionArguments.add(new WildcardExtendsTypeArgument(targetTypeArgument));
        ObjectType targetCollectionType = parameterObjectType.createType(targetCollectionArguments);

        if (typeMaker.isAssignable(
                Collections.emptyMap(),
                Collections.emptyMap(),
                targetCollectionType,
                null,
                expressionObjectType)) {
            return parameter;
        }

        return addExplicitCastExpression(parameterObjectType.createType(null), parameter);
    }

    private BaseExpression adjustDescriptorSensitiveParameters(
            MethodInvocationExpression invocationExpression,
            BaseType declaredParameterTypes,
            RawDescriptorMethodTypes descriptorMethodTypes,
            BaseExpression parameters) {
        if (declaredParameterTypes == null
                || descriptorMethodTypes == null
                || descriptorMethodTypes.parameterTypes() == null
                || parameters == null) {
            return parameters;
        }

        if (parameters.isList()) {
            DefaultList<Expression> parameterList = parameters.getList();
            DefaultList<Type> declaredTypes = declaredParameterTypes.isList() ? declaredParameterTypes.getList() : null;
            DefaultList<Type> descriptorTypes = descriptorMethodTypes.parameterTypes().isList() ? descriptorMethodTypes.parameterTypes().getList() : null;
            if (declaredTypes == null || descriptorTypes == null) {
                return parameters;
            }
            for (int i = 0; i < parameterList.size() && i < declaredTypes.size() && i < descriptorTypes.size(); i++) {
                parameterList.set(i, adjustDescriptorSensitiveParameter(invocationExpression, declaredTypes.get(i), descriptorTypes.get(i), parameterList.get(i)));
            }
            return parameters;
        }

        return adjustDescriptorSensitiveParameter(invocationExpression, declaredParameterTypes.getFirst(), descriptorMethodTypes.parameterTypes().getFirst(), parameters.getFirst());
    }

    private BaseExpression adjustDescriptorSensitiveParameters(
            BaseType declaredParameterTypes,
            RawDescriptorMethodTypes descriptorMethodTypes,
            BaseExpression parameters) {
        return adjustDescriptorSensitiveParameters(null, declaredParameterTypes, descriptorMethodTypes, parameters);
    }

    private Expression adjustDescriptorSensitiveParameter(
            MethodInvocationExpression invocationExpression,
            Type declaredType,
            Type descriptorType,
            Expression parameter) {
        if (parameter == null || declaredType == null || descriptorType == null) {
            return parameter;
        }

        declaredType = resolveDescriptorSensitiveDeclaredType(invocationExpression, declaredType);

        Expression coreExpression = unwrapCastExpression(parameter);
        Type expressionType = coreExpression.getType();
        if (expressionType == null) {
            return parameter;
        }
        if (invocationExpression != null && "use".equals(invocationExpression.getName())) {
            System.out.println("[DEBUG-ADJ] declared=" + declaredType + " descriptor=" + descriptorType + " exprType=" + expressionType + " param=" + parameter);
        }

        if (declaredType instanceof ObjectType declaredObjectType
                && "java/lang/Class".equals(declaredObjectType.getInternalName())
                && descriptorType instanceof ObjectType descriptorObjectType
                && descriptorObjectType.getTypeArguments() == null
                && descriptorObjectType.rawEquals(declaredObjectType)
                && expressionType instanceof ObjectType expressionObjectType
                && expressionObjectType.rawEquals(descriptorObjectType)
                && (coreExpression instanceof LocalVariableReferenceExpression || coreExpression instanceof FieldReferenceExpression)) {
            return coreExpression;
        }

        if (!(declaredType instanceof ObjectType declaredObjectType)
                || !(descriptorType instanceof ObjectType descriptorObjectType)
                || !(expressionType instanceof ObjectType expressionObjectType)) {
            return parameter;
        }

        if (invocationExpression != null
                && (invocationExpression.getExpression() instanceof SuperExpression
                || invocationExpression.getExpression() instanceof QualifiedSuperExpression)
                && declaredObjectType.rawEquals(expressionObjectType)
                && typeMaker.isRawTypeAssignable(descriptorObjectType, declaredObjectType)
                && !typeMaker.isRawTypeAssignable(declaredObjectType, descriptorObjectType)) {
            return parameter;
        }

        if ((coreExpression instanceof MethodInvocationExpression
                || coreExpression instanceof NewExpression)
                && declaredObjectType.rawEquals(expressionObjectType)
                && typeMaker.isRawTypeAssignable(descriptorObjectType, declaredObjectType)
                && !typeMaker.isRawTypeAssignable(declaredObjectType, descriptorObjectType)) {
            return parameter;
        }

        if (!descriptorObjectType.rawEquals(declaredObjectType)
                && typeMaker.isRawTypeAssignable(descriptorObjectType, expressionObjectType)
                && !typeMaker.isRawTypeAssignable(expressionObjectType, descriptorObjectType)) {
            if (invocationExpression != null && "use".equals(invocationExpression.getName())) {
                System.out.println("[DEBUG-ADJ] adding cast to " + descriptorType);
            }
            return addExplicitCastExpression(descriptorType, coreExpression);
        }

        if ("java/util/Collection".equals(descriptorObjectType.getInternalName())
                && descriptorObjectType.getTypeArguments() == null
                && declaredObjectType.getTypeArguments() != null
                && (coreExpression instanceof LocalVariableReferenceExpression || coreExpression instanceof FieldReferenceExpression)
                && typeMaker.isRawTypeAssignable(descriptorObjectType, expressionObjectType)) {
            return addExplicitCastExpression(descriptorType, coreExpression);
        }

        return parameter;
    }

    private Type resolveDescriptorSensitiveDeclaredType(MethodInvocationExpression invocationExpression, Type declaredType) {
        if (!(invocationExpression instanceof ClassFileMethodInvocationExpression classFileInvocationExpression)) {
            return declaredType;
        }

        Type resolvedDeclaredType = applyDirectTypeBindings(classFileInvocationExpression.getTypeBindings(), declaredType);
        if (resolvedDeclaredType instanceof GenericType genericType
                && classFileInvocationExpression.getTypeBounds() != null
                && classFileInvocationExpression.getTypeBounds().get(genericType.getName()) instanceof ObjectType boundObjectType) {
            return genericType.getDimension() == 0
                    ? boundObjectType
                    : boundObjectType.createType(genericType.getDimension());
        }

        return resolvedDeclaredType;
    }

    private boolean shouldAddDescriptorBridgeResultCast(Type type, Expression expression) {
        if (type == null || !(expression instanceof ClassFileMethodInvocationExpression methodInvocationExpression)) {
            return false;
        }
        if (!hasOverloadedMatches(methodInvocationExpression)) {
            return false;
        }

        RawDescriptorMethodTypes descriptorMethodTypes = parseRawDescriptorMethodTypes(methodInvocationExpression.getDescriptor());
        if (descriptorMethodTypes == null) {
            return false;
        }

        Type descriptorReturnType = descriptorMethodTypes.returnedType();
        Type expressionType = methodInvocationExpression.getType();
        if (descriptorReturnType == null || descriptorReturnType.equals(expressionType)) {
            return false;
        }
        if (type.isObjectType() && expressionType.isObjectType()) {
            return descriptorReturnType.isObjectType()
                    && !ObjectType.TYPE_OBJECT.rawEquals((ObjectType) descriptorReturnType)
                    && !isRawObjectArray(descriptorReturnType)
                    && ((ObjectType) type).rawEquals((ObjectType) expressionType)
                    && !((ObjectType) descriptorReturnType).rawEquals((ObjectType) expressionType);
        }

        return type.getDimension() > 0
                && type.getDimension() == expressionType.getDimension()
                && descriptorReturnType.getDimension() > 0
                && descriptorReturnType.getDimension() != expressionType.getDimension();
    }

    private boolean hasOverloadedMatches(ClassFileMethodInvocationExpression expression) {
        BaseExpression parameters = expression.getParameters();
        int parameterCount = parameters == null ? 0 : parameters.size();
        return typeMaker.matchCount(expression.getInternalTypeName(), expression.getName(), parameterCount, false) > 1;
    }

    private boolean shouldUseDescriptorBridgeParameterTypes(boolean unique, BaseType parameterTypes, RawDescriptorMethodTypes descriptorMethodTypes) {
        if (parameterTypes == null || descriptorMethodTypes == null || descriptorMethodTypes.parameterTypes() == null) {
            return false;
        }

        BaseType descriptorParameterTypes = descriptorMethodTypes.parameterTypes();
        if (parameterTypes.isList() != descriptorParameterTypes.isList() || parameterTypes.size() != descriptorParameterTypes.size()) {
            return false;
        }

        if (parameterTypes.isList()) {
            for (int i = 0; i < parameterTypes.size(); i++) {
                if (hasDifferentErasedSignature(parameterTypes.getList().get(i), descriptorParameterTypes.getList().get(i))) {
                    return true;
                }
            }
            return false;
        }

        return hasDifferentErasedSignature(parameterTypes.getFirst(), descriptorParameterTypes.getFirst());
    }

    private static boolean hasDifferentErasedSignature(Type left, Type right) {
        if (left == null || right == null) {
            return false;
        }
        if (left.isObjectType() && right.isObjectType()) {
            if (((ObjectType) left).rawEquals((ObjectType) right)
                    && ((ObjectType) left).getTypeArguments() != null
                    && ((ObjectType) right).getTypeArguments() == null) {
                String internalName = ((ObjectType) right).getInternalName();
                if ("java/lang/Class".equals(internalName) || "java/util/Collection".equals(internalName)) {
                    return true;
                }
            }
            if (ObjectType.TYPE_OBJECT.rawEquals((ObjectType) right)) {
                return left.getDimension() == 0 && isPrimitiveWrapperObjectType((ObjectType) left);
            }
            if (isRawObjectArray(right)) {
                return false;
            }
            return !((ObjectType) left).rawEquals((ObjectType) right);
        }
        return left.getDimension() != right.getDimension() || !left.equals(right);
    }

    private static boolean isRawObjectArray(Type type) {
        return type.isObjectType()
                && type.getDimension() > 0
                && "java/lang/Object".equals(((ObjectType) type).getInternalName());
    }

    private static boolean isPrimitiveWrapperObjectType(ObjectType type) {
        if (type == null) {
            return false;
        }
        String internalName = type.getInternalName();
        return "java/lang/Byte".equals(internalName)
                || "java/lang/Short".equals(internalName)
                || "java/lang/Character".equals(internalName)
                || "java/lang/Integer".equals(internalName)
                || "java/lang/Long".equals(internalName)
                || "java/lang/Float".equals(internalName)
                || "java/lang/Double".equals(internalName)
                || "java/lang/Boolean".equals(internalName);
    }

    private static boolean shouldKeepParameterizedClassResultCast(ObjectType targetType, ObjectType expressionType) {
        return "java/lang/Class".equals(targetType.getInternalName())
                && targetType.getTypeArguments() != null
                && !targetType.equals(expressionType);
    }

    private BaseType adjustVarArgsParameterTypes(
            ClassFileMethodInvocationExpression expression,
            BaseType parameterTypes,
            BaseExpression parameters) {
        if ((!isSourceLevelVarArgs(expression) && !isSyntheticGenericVarArgsExpansion(expression, parameters))
                || parameterTypes == null
                || parameters == null
                || !parameterTypes.isList()
                || !parameters.isList()
                || parameterTypes.size() == 0) {
            return parameterTypes;
        }

        if (parameterTypes.size() == parameters.size() + 1) {
            Types adjustedTypes = new Types();
            adjustedTypes.addAll(parameterTypes.getList());
            adjustedTypes.remove(adjustedTypes.size() - 1);
            return adjustedTypes;
        }
        if (parameterTypes.size() != parameters.size()) {
            return parameterTypes;
        }

        int lastIndex = parameterTypes.size() - 1;
        Type lastParameterType = parameterTypes.getList().get(lastIndex);
        Expression lastParameter = parameters.getList().get(lastIndex);
        if (lastParameterType.getDimension() == 0
                || lastParameter == null
                || lastParameter.isNewArray()
                || lastParameter.isNewInitializedArray()) {
            return parameterTypes;
        }

        Type lastParameterExpressionType = lastParameter.getType();
        if (lastParameterExpressionType != null
                && lastParameterExpressionType.getDimension() > 0
                && isAssignableVarArgsArray(lastParameterType, lastParameterExpressionType)) {
            return parameterTypes;
        }

        Types adjustedTypes = new Types();
        adjustedTypes.addAll(parameterTypes.getList());
        adjustedTypes.set(lastIndex, lastParameterType.createType(lastParameterType.getDimension() - 1));
        return adjustedTypes;
    }

    private boolean isAssignableVarArgsArray(Type parameterType, Type expressionType) {
        if (parameterType == null || expressionType == null || parameterType.getDimension() == 0 || expressionType.getDimension() == 0) {
            return false;
        }
        if (parameterType.equals(expressionType)) {
            return true;
        }
        if (parameterType.isObjectType()
                && expressionType.isObjectType()
                && parameterType.getDimension() == expressionType.getDimension()
                && ((ObjectType) parameterType).rawEquals((ObjectType) expressionType)) {
            return true;
        }
        return parameterType.isObjectType()
                && expressionType.isObjectType()
                && typeMaker.isAssignable(typeBounds, (ObjectType) parameterType, (ObjectType) expressionType);
    }

    private BaseExpression collapseSingleElementVarArgs(
            ClassFileMethodInvocationExpression expression,
            BaseExpression parameters) {
        if (parameters == null
                || !parameters.isList()
                || parameters.size() == 0) {
            return parameters;
        }

        if (!isSourceLevelVarArgs(expression) && !isSyntheticGenericVarArgsExpansion(expression, parameters)) {
            return parameters;
        }

        int lastIndex = parameters.size() - 1;
        Expression lastParameter = parameters.getList().get(lastIndex);
        Expression arrayParameter = unwrapCastExpression(lastParameter);
        if (!(arrayParameter instanceof NewInitializedArray newInitializedArray)
                || newInitializedArray.getArrayInitializer() == null) {
            return parameters;
        }

        if (newInitializedArray.getArrayInitializer().isEmpty()) {
            parameters.getList().remove(lastIndex);
            return parameters.getList().isEmpty() ? null : parameters;
        }
        if (newInitializedArray.getArrayInitializer().size() != 1
                || !(newInitializedArray.getArrayInitializer().getFirst() instanceof ExpressionVariableInitializer evi)) {
            return parameters;
        }

        parameters.getList().set(lastIndex, evi.getExpression());
        return parameters;
    }

    private boolean isSourceLevelVarArgs(ClassFileMethodInvocationExpression expression) {
        if (expression.isVarArgs()) {
            return true;
        }

        TypeMaker.MethodTypes methodTypes =
                typeMaker.makeMethodTypes(expression.getInternalTypeName(), expression.getName(), expression.getDescriptor());
        return methodTypes != null && methodTypes.isVarArgs();
    }

    private boolean isSyntheticGenericVarArgsExpansion(
            ClassFileMethodInvocationExpression expression,
            BaseExpression parameters) {
        return expression.getTypeParameters() != null
                && typeMaker.matchCount(expression.getInternalTypeName(), expression.getName(), parameters.size(), false) > 1;
    }

    private boolean shouldDeferToSourceGenericHelper(Type type, Type unboundType, Expression expression, Type expressionType) {
        if (!(expression instanceof ClassFileMethodInvocationExpression)
                || !(type instanceof ObjectType targetType)
                || !(expressionType instanceof ObjectType expressionObjectType)
                || !isSourceInferredGenericHelper(expression)
                || !hasTypeParameters(unboundType)) {
            return false;
        }

        return targetType.rawEquals(expressionObjectType) || typeMaker.isRawTypeAssignable(targetType, expressionObjectType);
    }

    private Type resolveExplicitMethodInvocationCastTarget(
            Type type,
            Type unboundType,
            Expression expression,
            Type expressionType) {
        if (!(expression instanceof ClassFileMethodInvocationExpression methodInvocationExpression)
                || methodInvocationExpression.getTypeParameters() != null
                || !(type instanceof ObjectType targetType)
                || !(unboundType instanceof ObjectType unboundObjectType)
                || !(expressionType instanceof ObjectType expressionObjectType)
                || !targetType.rawEquals(expressionObjectType)
                || !unboundObjectType.rawEquals(expressionObjectType)
                || !hasKnownTypeParameters(targetType)
                || !hasUnboundedWildcardTypeArgument(unboundObjectType)) {
            return null;
        }

        return targetType;
    }

    private Type resolveGenericCastTarget(Type type, Type unboundType, Expression expression, Type expressionType) {
        if (!(type instanceof ObjectType targetType)
                || !(expressionType instanceof ObjectType expressionObjectType)
                || !(expression instanceof ClassFileMethodInvocationExpression methodInvocationExpression)) {
            return null;
        }

        if (!targetType.rawEquals(expressionObjectType)
                || methodInvocationExpression.getTypeParameters() != null
                || (hasKnownTypeParameters(expressionObjectType) && !hasUnboundedWildcardTypeArgument(expressionType))) {
            return null;
        }

        if (unboundType instanceof ObjectType unboundObjectType
                && unboundObjectType.getTypeArguments() != null
                && hasKnownTypeParameters(unboundObjectType)
                && unboundObjectType.rawEquals(expressionObjectType)) {
            return unboundObjectType;
        }

        if (targetType.getTypeArguments() != null && hasKnownTypeParameters(targetType)) {
            return targetType;
        }

        return null;
    }

    private RawDescriptorMethodTypes parseRawDescriptorMethodTypes(String descriptor) {
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

        String returnDescriptor = descriptor.substring(index + 1);
        Type returnedType = "V".equals(returnDescriptor) ? null : makeRawDescriptorType(returnDescriptor);
        return new RawDescriptorMethodTypes(parameterTypes, returnedType);
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

    private record RawDescriptorMethodTypes(BaseType parameterTypes, Type returnedType) {
    }

    private static boolean hasTypeParameters(Type type) {
        return type != null && (type.isGenericType() || !type.findTypeParametersInType().isEmpty());
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

    private boolean requiresDescriptorResultReceiverCast(ObjectType targetType, ClassFileMethodInvocationExpression methodInvocationExpression) {
        RawDescriptorMethodTypes descriptorMethodTypes = parseRawDescriptorMethodTypes(methodInvocationExpression.getDescriptor());
        if (descriptorMethodTypes == null || descriptorMethodTypes.returnedType() == null) {
            return false;
        }

        Type descriptorReturnType = descriptorMethodTypes.returnedType();
        Type expressionType = methodInvocationExpression.getType();
        if (!(descriptorReturnType instanceof ObjectType descriptorObjectType)
                || !(expressionType instanceof ObjectType expressionObjectType)) {
            return false;
        }

        return targetType.rawEquals(expressionObjectType)
                && !targetType.rawEquals(descriptorObjectType);
    }

    private boolean shouldForceOverloadCast(BaseExpression parameters, BaseType parameterTypes) {
        if (parameters == null) {
            return false;
        }
        if (parameters.isList()) {
            DefaultList<Expression> parameterList = parameters.getList();
            DefaultList<Type> parameterTypeList = parameterTypes != null && parameterTypes.isList() ? parameterTypes.getList() : null;
            for (int i = 0; i < parameterList.size(); i++) {
                Type parameterType = parameterTypeList == null || i >= parameterTypeList.size() ? null : parameterTypeList.get(i);
                if (shouldForceOverloadCast(parameterList.get(i), parameterType)) {
                    return true;
                }
            }
            return false;
        }
        Type parameterType = parameterTypes == null ? null : parameterTypes.getFirst();
        return shouldForceOverloadCast(parameters.getFirst(), parameterType);
    }

    private boolean shouldForceGeneralOverloadCast(
            MethodInvocationExpression expression,
            Map<String, TypeArgument> typeBindings,
            Map<String, BaseType> localTypeBounds,
            BaseExpression parameters,
            BaseType parameterTypes,
            int typedMatches,
            int rawMatches) {
        if (parameters == null) {
            return false;
        }

        boolean ambiguousMethodReference = shouldForceSourceMethodReferenceOverloadCast(rawMatches, parameters, parameterTypes);
        if (typedMatches <= 1 && !ambiguousMethodReference) {
            return false;
        }

        if (parameters.isList()) {
            DefaultList<Expression> parameterList = parameters.getList();
            DefaultList<Type> parameterTypeList = parameterTypes != null && parameterTypes.isList() ? parameterTypes.getList() : null;
            for (int i = 0; i < parameterList.size(); i++) {
                Type parameterType = parameterTypeList == null || i >= parameterTypeList.size() ? null : parameterTypeList.get(i);
                if (shouldForceConflictingSourceMethodReferenceOverloadCast(expression, typeBindings, localTypeBounds, parameters, i, parameterList.get(i), parameterType)) {
                    return true;
                }
                if (shouldForceOverloadCast(parameterList.get(i), parameterType)
                        && !isOverloadPinnedByOtherDescriptorParameter(expression, parameters, i, parameterType)
                        && wouldCastResolveOverload(expression, typeBindings, localTypeBounds, parameters, i, parameterType)) {
                    return true;
                }
                if (shouldForceAlternativeObjectOverloadCast(parameterList.get(i))
                        && wouldCastResolveOverload(expression, typeBindings, localTypeBounds, parameters, i, ObjectType.TYPE_OBJECT)) {
                    return true;
                }
            }
            return false;
        }

        Type parameterType = parameterTypes == null ? null : parameterTypes.getFirst();
        if (shouldForceConflictingSourceMethodReferenceOverloadCast(expression, typeBindings, localTypeBounds, parameters, 0, parameters.getFirst(), parameterType)) {
            return true;
        }
        if (shouldForceOverloadCast(parameters.getFirst(), parameterType)
                && !isOverloadPinnedByOtherDescriptorParameter(expression, parameters, 0, parameterType)
                && wouldCastResolveOverload(expression, typeBindings, localTypeBounds, parameters, 0, parameterType)) {
            return true;
        }

        return shouldForceAlternativeObjectOverloadCast(parameters.getFirst())
                && wouldCastResolveOverload(expression, typeBindings, localTypeBounds, parameters, 0, ObjectType.TYPE_OBJECT);
    }

    private boolean shouldForceConflictingSourceMethodReferenceOverloadCast(
            MethodInvocationExpression expression,
            Map<String, TypeArgument> typeBindings,
            Map<String, BaseType> localTypeBounds,
            BaseExpression parameters,
            int index,
            Expression parameter,
            Type parameterType) {
        if (!shouldForceSourceMethodReferenceOverloadCast(parameter, parameterType)) {
            return false;
        }

        ObjectType receiverType = getMethodReferenceReceiverType(parameter);
        return receiverType != null
                && wouldCastSelectUniqueOverload(expression, typeBindings, localTypeBounds, parameters, index, receiverType);
    }

    private boolean shouldForceOverloadCast(Expression parameter, Type parameterType) {
        return parameter != null
                && (parameter.isNullExpression()
                || parameter instanceof LambdaIdentifiersExpression
                || parameter instanceof MethodReferenceExpression
                || shouldForceBoxedObjectOverloadCast(parameter, parameterType)
                || shouldForcePrimitiveObjectOverloadCast(parameter, parameterType)
                || shouldForceObjectOverloadCast(parameter, parameterType));
    }

    private boolean shouldForceSelfOverloadSubtypeCast(
            MethodInvocationExpression expression,
            Map<String, TypeArgument> typeBindings,
            Map<String, BaseType> localTypeBounds,
            BaseExpression parameters,
            BaseType parameterTypes,
            int typedMatches) {
        if (typedMatches <= 1) {
            return false;
        }

        Expression receiver = expression.getExpression();
        if (receiver instanceof SuperExpression || receiver instanceof QualifiedSuperExpression) {
            return false;
        }
        if (receiver != null
                && !(receiver instanceof ThisExpression)
                && !receiver.isObjectTypeReferenceExpression()) {
            return false;
        }

        if (parameters == null) {
            return false;
        }
        if (parameters.isList()) {
            DefaultList<Expression> parameterList = parameters.getList();
            DefaultList<Type> parameterTypeList = parameterTypes != null && parameterTypes.isList() ? parameterTypes.getList() : null;
            for (int i = 0; i < parameterList.size(); i++) {
                Type parameterType = parameterTypeList == null || i >= parameterTypeList.size() ? null : parameterTypeList.get(i);
                if (shouldForceSubtypeOverloadCast(parameterList.get(i), parameterType)
                        && !isOverloadPinnedByOtherDescriptorParameter(expression, parameters, i, parameterType)
                        && wouldCastResolveOverload(expression, typeBindings, localTypeBounds, parameters, i, parameterType)) {
                    return true;
                }
            }
            return false;
        }

        Type parameterType = parameterTypes == null ? null : parameterTypes.getFirst();
        return shouldForceSubtypeOverloadCast(parameters.getFirst(), parameterType)
                && !isOverloadPinnedByOtherDescriptorParameter(expression, parameters, 0, parameterType)
                && wouldCastResolveOverload(expression, typeBindings, localTypeBounds, parameters, 0, parameterType);
    }

    private boolean shouldForceSubtypeOverloadCast(Expression parameter, Type parameterType) {
        Expression coreExpression = unwrapCastExpression(parameter);
        if (coreExpression instanceof MethodInvocationExpression) {
            return false;
        }

        if (!(parameterType instanceof ObjectType parameterObjectType)
                || !(coreExpression.getType() instanceof ObjectType expressionObjectType)
                || parameterObjectType.rawEquals(expressionObjectType)
                || ObjectType.TYPE_OBJECT.rawEquals(parameterObjectType)
                || ObjectType.TYPE_OBJECT.rawEquals(expressionObjectType)) {
            return false;
        }

        return typeMaker.isRawTypeAssignable(parameterObjectType, expressionObjectType);
    }

    private boolean wouldCastResolveOverload(
            MethodInvocationExpression expression,
            Map<String, TypeArgument> typeBindings,
            Map<String, BaseType> localTypeBounds,
            BaseExpression parameters,
            int index,
            Type parameterType) {
        if (expression == null || parameters == null || parameterType == null) {
            return false;
        }

        int matchesWithCurrentParameters = typeMaker.matchCount(
                typeBindings,
                localTypeBounds,
                expression.getInternalTypeName(),
                expression.getName(),
                parameters,
                false);
        if (matchesWithCurrentParameters == 1) {
            return false;
        }

        return wouldCastSelectUniqueOverload(expression, typeBindings, localTypeBounds, parameters, index, parameterType);
    }

    private boolean isOverloadPinnedByOtherDescriptorParameter(
            MethodInvocationExpression expression,
            BaseExpression parameters,
            int castIndex,
            Type parameterType) {
        if (parameters == null
                || parameters.size() <= 1
                || parameterType == null
                || !(expression instanceof ClassFileMethodInvocationExpression)) {
            return false;
        }

        RawDescriptorMethodTypes descriptorMethodTypes = parseRawDescriptorMethodTypes(expression.getDescriptor());
        if (descriptorMethodTypes == null || descriptorMethodTypes.parameterTypes() == null) {
            return false;
        }

        BaseType descriptorParameterTypes = descriptorMethodTypes.parameterTypes();
        // Allow collapsed varargs: descriptor may have one more param than the call
        if (descriptorParameterTypes.size() != parameters.size()
                && descriptorParameterTypes.size() != parameters.size() + 1) {
            return false;
        }

        Type descriptorParameterType = resolveDeclaredParameterType(descriptorParameterTypes, descriptorParameterTypes, castIndex);
        if (!sameDescriptorType(descriptorParameterType, parameterType)) {
            return false;
        }

        if (parameters.isList()) {
            for (int i = 0; i < parameters.size(); i++) {
                if (i == castIndex) {
                    continue;
                }

                Type otherDescriptorType = resolveDeclaredParameterType(descriptorParameterTypes, descriptorParameterTypes, i);
                Type otherExpressionType = unwrapCastExpression(parameters.getList().get(i)).getType();
                if (sameDescriptorType(otherDescriptorType, otherExpressionType)) {
                    return true;
                }
            }
        }

        return false;
    }

    private boolean wouldCastSelectUniqueOverload(
            MethodInvocationExpression expression,
            Map<String, TypeArgument> typeBindings,
            Map<String, BaseType> localTypeBounds,
            BaseExpression parameters,
            int index,
            Type parameterType) {
        if (expression == null || parameters == null || parameterType == null) {
            return false;
        }

        if (parameters.isList()) {
            DefaultList<Expression> parameterList = parameters.getList();
            if (index >= parameterList.size()) {
                return false;
            }

            Expression originalParameter = parameterList.get(index);
            parameterList.set(index, new CastExpression(parameterType, originalParameter));
            int matchesWithCast = typeMaker.matchCount(
                    typeBindings,
                    localTypeBounds,
                    expression.getInternalTypeName(),
                    expression.getName(),
                    parameters,
                    false);
            parameterList.set(index, originalParameter);
            return matchesWithCast == 1;
        }

        if (index != 0) {
            return false;
        }

        Expression castParameter = new CastExpression(parameterType, parameters.getFirst());
        int matchesWithCast = typeMaker.matchCount(
                typeBindings,
                localTypeBounds,
                expression.getInternalTypeName(),
                expression.getName(),
                castParameter,
                false);
        return matchesWithCast == 1;
    }

    private static boolean shouldForceSourceMethodReferenceOverloadCast(BaseExpression parameters, BaseType parameterTypes) {
        return shouldForceSourceMethodReferenceOverloadCast(Integer.MAX_VALUE, parameters, parameterTypes);
    }

    private static boolean shouldForceSourceMethodReferenceOverloadCast(int rawMatches, BaseExpression parameters, BaseType parameterTypes) {
        if (rawMatches <= 1 || parameters == null) {
            return false;
        }
        if (parameters.isList()) {
            DefaultList<Expression> parameterList = parameters.getList();
            DefaultList<Type> parameterTypeList = parameterTypes != null && parameterTypes.isList() ? parameterTypes.getList() : null;
            for (int i = 0; i < parameterList.size(); i++) {
                Type parameterType = parameterTypeList == null || i >= parameterTypeList.size() ? null : parameterTypeList.get(i);
                if (shouldForceSourceMethodReferenceOverloadCast(parameterList.get(i), parameterType)) {
                    return true;
                }
            }
            return false;
        }
        Type parameterType = parameterTypes == null ? null : parameterTypes.getFirst();
        return shouldForceSourceMethodReferenceOverloadCast(parameters.getFirst(), parameterType);
    }

    private static boolean shouldForceSourceMethodReferenceOverloadCast(Expression parameter, Type parameterType) {
        if (!(parameter instanceof MethodReferenceExpression methodReference)
                || "new".equals(methodReference.getName())
                || !(parameterType instanceof ObjectType targetType)
                || targetType.getTypeArguments() == null
                || methodReference.getExpression() == null
                || !(methodReference.getExpression().getType() instanceof ObjectType receiverType)) {
            return false;
        }

        return !targetType.rawEquals(receiverType);
    }

    private static ObjectType getMethodReferenceReceiverType(Expression parameter) {
        if (!(parameter instanceof MethodReferenceExpression methodReference)
                || methodReference.getExpression() == null
                || !(methodReference.getExpression().getType() instanceof ObjectType receiverType)) {
            return null;
        }

        return receiverType;
    }

    private static boolean shouldForceBoxedObjectOverloadCast(Expression parameter, Type parameterType) {
        if (!(parameterType instanceof ObjectType parameterObjectType)
                || !ObjectType.TYPE_OBJECT.rawEquals(parameterObjectType)) {
            return false;
        }

        Expression coreExpression = unwrapCastExpression(parameter);
        return coreExpression != null
                && AutoboxingVisitor.isBoxingMethod(coreExpression)
                && coreExpression.getParameters() != null
                && coreExpression.getParameters().size() == 1
                && coreExpression.getParameters().getFirst().getType() != null
                && coreExpression.getParameters().getFirst().getType().isPrimitiveType();
    }

    private static boolean shouldForcePrimitiveObjectOverloadCast(Expression parameter, Type parameterType) {
        if (!(parameterType instanceof ObjectType parameterObjectType)
                || !ObjectType.TYPE_OBJECT.rawEquals(parameterObjectType)) {
            return false;
        }

        Expression coreExpression = unwrapCastExpression(parameter);
        return coreExpression != null
                && coreExpression.getType() != null
                && coreExpression.getType().isPrimitiveType();
    }

    private static boolean shouldForceAlternativeObjectOverloadCast(Expression parameter) {
        return shouldForceBoxedObjectOverloadCast(parameter, ObjectType.TYPE_OBJECT)
                || shouldForcePrimitiveObjectOverloadCast(parameter, ObjectType.TYPE_OBJECT);
    }

    private static boolean shouldForceObjectOverloadCast(Expression parameter, Type parameterType) {
        if (parameterType == null || parameter == null) {
            return false;
        }
        Type expressionType = unwrapCastExpression(parameter).getType();
        if (!(expressionType instanceof ObjectType expressionObjectType)
                || !ObjectType.TYPE_OBJECT.rawEquals(expressionObjectType)) {
            return false;
        }
        if (parameterType.getDimension() > 0) {
            return true;
        }
        return parameterType.isObjectType() && !ObjectType.TYPE_OBJECT.rawEquals((ObjectType) parameterType);
    }

    private boolean requiresRawReceiverCast(MethodInvocationExpression expression) {
        if (!(expression instanceof ClassFileMethodInvocationExpression methodInvocationExpression)
                || expression.getExpression() == null
                || !(expression.getExpression().getType() instanceof ObjectType receiverType)
                || !hasUnboundedWildcardTypeArgument(receiverType)) {
            return false;
        }

        if ("toArray".equals(expression.getName())
                && expression.getParameters() != null
                && expression.getParameters().size() == 1) {
            Type parameterType = expression.getParameters().getFirst().getType();
            if (parameterType != null && parameterType.getDimension() > 0) {
                return false;
            }
        }

        if (shouldPreserveGenericReceiver(expression)) {
            return false;
        }

        BaseExpression parameters = expression.getParameters();
        BaseType parameterTypes = methodInvocationExpression.getParameterTypes();
        BaseType unboundParameterTypes = methodInvocationExpression.getUnboundParameterTypes();
        if (parameters == null || parameterTypes == null) {
            return false;
        }

        if (parameters.isList()) {
            DefaultList<Expression> parameterList = parameters.getList();
            for (int i = 0; i < parameterList.size(); i++) {
                Type parameterType = resolveDeclaredParameterType(parameterTypes, unboundParameterTypes, i);
                if (requiresRawReceiverCast(parameterList.get(i), parameterType)) {
                    return true;
                }
            }
            return false;
        }

        return requiresRawReceiverCast(parameters.getFirst(), resolveDeclaredParameterType(parameterTypes, unboundParameterTypes, 0));
    }

    private boolean shouldPreserveGenericReceiver(MethodInvocationExpression expression) {
        Expression coreReceiver = unwrapCastExpression(expression.getExpression());
        ClassFileMethodInvocationExpression receiverInvocation =
                coreReceiver instanceof ClassFileMethodInvocationExpression candidate ? candidate : null;

        if (receiverInvocation == null) {
            return false;
        }

        if ("unwrap".equals(receiverInvocation.getName())) {
            return true;
        }

        if ((receiverInvocation.getUnboundType() instanceof GenericType
                || hasTypeParameters(receiverInvocation.getUnboundType())
                || hasTypeParameters(receiverInvocation.getType()))
                && (receiverInvocation.getParameters() == null || receiverInvocation.getParameters().size() == 0)) {
            return true;
        }

        if (!"toArray".equals(expression.getName())) {
            return false;
        }

        BaseExpression parameters = expression.getParameters();
        if (parameters == null || parameters.size() != 1) {
            return false;
        }

        Type parameterType = parameters.getFirst().getType();
        if (parameterType == null || parameterType.getDimension() == 0) {
            return false;
        }

        return receiverInvocation.getUnboundType() instanceof GenericType
                || hasTypeParameters(receiverInvocation.getType())
                || hasTypeParameters(receiverInvocation.getUnboundType())
                || hasParameterizedGenericHelperSource(receiverInvocation);
    }

    private void restoreConcreteToArrayReceiverType(MethodInvocationExpression expression) {
        if (!"toArray".equals(expression.getName())
                || !(expression instanceof ClassFileMethodInvocationExpression methodInvocationExpression)
                || expression.getExpression() == null) {
            return;
        }

        Expression receiverExpression = expression.getExpression();
        ClassFileMethodInvocationExpression receiverInvocation = null;
        ObjectType receiverType = null;

        if (receiverExpression instanceof ClassFileMethodInvocationExpression candidate
                && candidate.getType() instanceof ObjectType candidateType) {
            receiverInvocation = candidate;
            receiverType = candidateType;
        } else if (receiverExpression instanceof CastExpression castExpression
                && castExpression.getExpression() instanceof ClassFileMethodInvocationExpression candidate
                && castExpression.getType() instanceof ObjectType castType) {
            receiverInvocation = candidate;
            receiverType = castType;
        }

        if (receiverInvocation == null
                || receiverType == null
                || receiverType.getTypeArguments() != null) {
            return;
        }

        Type sourceReceiverType = resolveConcreteGenericHelperSourceType(receiverInvocation);
        if (!(sourceReceiverType instanceof ObjectType sourceObjectType)
                || !receiverType.rawEquals(sourceObjectType)) {
            return;
        }

        expression.setExpression(receiverInvocation);
        receiverInvocation.setType(sourceObjectType);
        if (receiverInvocation.getUnboundType() == null) {
            receiverInvocation.setUnboundType(sourceObjectType);
        }

        Type arrayType = resolveToArrayComponentArrayType(sourceObjectType, methodInvocationExpression.getUnboundParameterTypes());
        if (arrayType == null) {
            return;
        }

        methodInvocationExpression.setParameterTypes(arrayType);

        Type expressionType = expression.getType();
        if (expressionType != null
                && expressionType.getDimension() > 0
                && expressionType.isObjectType()
                && "java/lang/Object".equals(((ObjectType) expressionType).getInternalName())) {
            expression.setType(arrayType);
        }
    }

    private Type resolveConcreteGenericHelperSourceType(ClassFileMethodInvocationExpression receiverInvocation) {
        if (receiverInvocation.getParameters() == null
                || receiverInvocation.getParameters().size() != 1) {
            return null;
        }

        Expression source = receiverInvocation.getParameters().getFirst();
        if (source == null
                || !(source.getType() instanceof ObjectType sourceType)
                || sourceType.getTypeArguments() == null) {
            return null;
        }

        if (isSourceInferredGenericHelper(receiverInvocation)) {
            return sourceType;
        }

        Type unboundType = receiverInvocation.getUnboundType();
        if (!(unboundType instanceof GenericType)
                || !(receiverInvocation.getType() instanceof ObjectType receiverType)
                || !receiverType.rawEquals(sourceType)) {
            return null;
        }

        return sourceType;
    }

    private static Type resolveToArrayComponentArrayType(ObjectType receiverType, BaseType unboundParameterTypes) {
        if (receiverType.getTypeArguments() == null
                || !receiverType.getTypeArguments().isTypeArgumentList()
                || receiverType.getTypeArguments().getTypeArgumentList().isEmpty()
                || unboundParameterTypes == null) {
            return null;
        }

        Type unboundParameterType = unboundParameterTypes.isList()
                ? unboundParameterTypes.getList().isEmpty() ? null : unboundParameterTypes.getList().getFirst()
                : unboundParameterTypes.getFirst();
        if (!(unboundParameterType instanceof GenericType genericType)
                || genericType.getDimension() == 0) {
            return null;
        }

        TypeArgument elementArgument = receiverType.getTypeArguments().getTypeArgumentList().getFirst();
        if (!(elementArgument instanceof Type elementType)) {
            return null;
        }

        return elementType.createType(genericType.getDimension());
    }

    private boolean hasParameterizedGenericHelperSource(ClassFileMethodInvocationExpression receiverInvocation) {
        if (!isSourceInferredGenericHelper(receiverInvocation)
                || receiverInvocation.getParameters() == null
                || receiverInvocation.getParameters().size() != 1
                || !(receiverInvocation.getType() instanceof ObjectType receiverType)
                || receiverType.getTypeArguments() != null) {
            return false;
        }

        Expression source = receiverInvocation.getParameters().getFirst();
        return source != null
                && source.getType() instanceof ObjectType sourceType
                && sourceType.getTypeArguments() != null
                && receiverType.rawEquals(sourceType);
    }

    private BaseExpression addTargetTypedArrayConstructorReferenceCasts(
            MethodInvocationExpression expression,
            BaseType parameterTypes,
            BaseExpression parameters) {
        if (!"toArray".equals(expression.getName())
                || parameterTypes == null
                || parameters == null
                || parameters.size() != 1
                || !(parameterTypes.getFirst() instanceof ObjectType parameterObjectType)
                || !(parameters.getFirst() instanceof MethodReferenceExpression methodReference)
                || methodReference.getExpression() == null
                || !"new".equals(methodReference.getName())
                || !methodReference.getExpression().isObjectTypeReferenceExpression()) {
            return parameters;
        }

        Type targetArrayType = resolveTargetArrayTypeForArrayConstructorCast(expression);
        if (!(targetArrayType instanceof ObjectType targetArrayObjectType)
                || !hasKnownTypeParameters(targetArrayType)) {
            return parameters;
        }

        ObjectType arrayConstructorType = methodReference.getExpression().getObjectType();
        if (arrayConstructorType == null
                || arrayConstructorType.getDimension() == 0
                || !arrayConstructorType.rawEquals(targetArrayObjectType)) {
            return parameters;
        }

        TypeArguments typeArguments = new TypeArguments(1);
        typeArguments.add(targetArrayType);
        Expression castExpression = addExplicitCastExpression(parameterObjectType.createType(typeArguments), parameters.getFirst());
        if (parameters.isList()) {
            parameters.getList().set(0, castExpression);
            return parameters;
        }
        return castExpression;
    }

    private Type resolveTargetArrayTypeForArrayConstructorCast(MethodInvocationExpression expression) {
        Type contextualType = type == null ? returnedType : type;
        if (contextualType != null
                && contextualType.getDimension() > 0
                && hasKnownTypeParameters(contextualType)) {
            return contextualType;
        }

        if (expression.getExpression() == null
                || !(expression.getExpression().getType() instanceof ObjectType receiverType)
                || receiverType.getTypeArguments() == null
                || !receiverType.getTypeArguments().isTypeArgumentList()) {
            return null;
        }

        TypeArgument receiverElementArgument = receiverType.getTypeArguments().getTypeArgumentList().getFirst();
        if (!(receiverElementArgument instanceof Type receiverElementType)
                || !hasKnownTypeParameters(receiverElementType)) {
            return null;
        }

        return receiverElementType.createType(1);
    }

    private static boolean requiresRawReceiverCast(Expression parameter, Type parameterType) {
        if (parameter == null || parameterType == null) {
            return false;
        }
        Type expressionType = unwrapCastExpression(parameter).getType();
        return expressionType instanceof ObjectType expressionObjectType
                && ObjectType.TYPE_OBJECT.rawEquals(expressionObjectType)
                && hasTypeParameters(parameterType);
    }

    private boolean requiresSpecificReceiverCast(MethodInvocationExpression expression) {
        Expression receiver = expression.getExpression();
        if (!(expression instanceof ClassFileMethodInvocationExpression)
                || receiver == null
                || receiver instanceof ThisExpression
                || receiver instanceof SuperExpression
                || receiver instanceof QualifiedSuperExpression) {
            return false;
        }

        ObjectType ownerType = typeMaker.makeFromInternalTypeName(expression.getInternalTypeName());
        if (ownerType == null) {
            return false;
        }

        if (receiver.getType() instanceof ObjectType receiverType) {
            if (shouldPreserveInheritedGenericReceiver(ownerType, receiver, receiverType)) {
                return false;
            }
            if (receiver instanceof ClassFileMethodInvocationExpression receiverInvocation
                    && ownerType.rawEquals(receiverType)
                    && requiresDescriptorResultReceiverCast(ownerType, receiverInvocation)) {
                return true;
            }
            return !ownerType.rawEquals(receiverType)
                    && typeMaker.isRawTypeAssignable(receiverType, ownerType)
                    && !typeMaker.isRawTypeAssignable(ownerType, receiverType);
        }

        if (receiver.getType() instanceof GenericType receiverGenericType
                && receiver instanceof ClassFileMethodInvocationExpression receiverInvocation
                && receiverInvocation.getTypeBounds() != null
                && receiverInvocation.getTypeBounds().get(receiverGenericType.getName()) instanceof ObjectType boundObjectType) {
            return !ownerType.rawEquals(boundObjectType)
                    && typeMaker.isRawTypeAssignable(boundObjectType, ownerType);
        }

        if (receiver.getType() instanceof GenericType receiverGenericType) {
            if ("java/lang/Object".equals(ownerType.getInternalName())) {
                return false;
            }
            // Check if the generic type's bound (from the method's typeBounds) makes the cast redundant
            // e.g., N extends BstNode<K, N> - casting to BstNode is not needed
            if (expression instanceof ClassFileMethodInvocationExpression cfmie
                    && cfmie.getTypeBounds() != null
                    && cfmie.getTypeBounds().get(receiverGenericType.getName()) instanceof ObjectType boundOt
                    && (ownerType.rawEquals(boundOt) || typeMaker.isRawTypeAssignable(ownerType, boundOt))) {
                return false;
            }
            return true;
        }

        return false;
    }

    private boolean shouldPreserveInheritedGenericReceiver(ObjectType ownerType, Expression receiver, ObjectType receiverType) {
        Expression coreReceiver = unwrapCastExpression(receiver);
        if (!(coreReceiver instanceof ClassFileMethodInvocationExpression receiverInvocation)
                || !"unwrap".equals(receiverInvocation.getName())
                || (receiverInvocation.getParameters() != null && receiverInvocation.getParameters().size() > 0)
                || !typeMaker.isRawTypeAssignable(receiverType, ownerType)) {
            return false;
        }

        return receiverInvocation.getType() instanceof GenericType
                || receiverInvocation.getUnboundType() instanceof GenericType
                || hasTypeParameters(receiverInvocation.getType())
                || hasTypeParameters(receiverInvocation.getUnboundType());
    }

    private boolean hasKnownTypeParameters(BaseTypeArgument type) {
        Set<String> genericIdentifiersInType = type.findTypeParametersInType();
        Set<String> genericIdentifiersInScope = findKnownTypeParameters();
        return genericIdentifiersInScope.containsAll(genericIdentifiersInType);
    }

    private boolean hasKnownTypeParameters(Type type) {
        return type != null && hasKnownTypeParameters((BaseTypeArgument) type);
    }

    private boolean hasKnownTypeParameters(BaseType baseType) {
        if (baseType == null) {
            return false;
        }
        for (Type type : baseType) {
            if (hasKnownTypeParameters(type)) {
                return true;
            }
        }
        return false;
    }

    private boolean shouldPreferUnboundParameterType(Map<String, BaseType> localTypeBounds, Type parameterType, Type unboundType) {
        if (!(parameterType instanceof ObjectType parameterObjectType)
                || !(unboundType instanceof GenericType genericType)
                || !hasKnownTypeParameters(unboundType)) {
            return false;
        }
        return localTypeBounds.get(genericType.getName()) instanceof ObjectType boundObjectType
                && !ObjectType.TYPE_OBJECT.rawEquals(boundObjectType)
                && sameErasedObjectType(parameterObjectType, boundObjectType);
    }

    private Type resolveErasedObjectParameterType(Map<String, BaseType> localTypeBounds, Type parameterType, Type unboundType) {
        if (!(parameterType instanceof ObjectType parameterObjectType)
                || !(unboundType instanceof GenericType genericType)
                || localTypeBounds == null
                || !hasKnownTypeParameters(unboundType)
                || !(localTypeBounds.get(genericType.getName()) instanceof ObjectType boundObjectType)
                || ObjectType.TYPE_OBJECT.rawEquals(boundObjectType)) {
            return parameterType;
        }

        boolean rawObjectParameter = ObjectType.TYPE_OBJECT.rawEquals(parameterObjectType);
        boolean rawObjectArrayParameter = parameterObjectType.getDimension() > 0 && ObjectType.TYPE_OBJECT.rawEquals(parameterObjectType);
        if (!rawObjectParameter && !rawObjectArrayParameter) {
            return parameterType;
        }

        return unboundType.getDimension() == 0
                ? boundObjectType
                : boundObjectType.createType(unboundType.getDimension());
    }

    private boolean shouldPreserveConcreteArgumentForGenericParameter(
            Map<String, BaseType> localTypeBounds,
            Type parameterType,
            Type unboundType,
            Expression expression) {
        if (expression == null
                || expression instanceof CastExpression
                || expression instanceof ThisExpression
                || expression.isNullExpression()
                || !(expression.getType() instanceof ObjectType expressionObjectType)) {
            return false;
        }

        if (parameterType instanceof GenericType genericType
                && localTypeBounds != null
                && localTypeBounds.get(genericType.getName()) instanceof ObjectType boundObjectType) {
            if (expression instanceof MethodInvocationExpression
                    && !isStrictConcreteSubtypeArgument(boundObjectType, expressionObjectType)) {
                return false;
            }
            return isErasedHierarchyAssignable(boundObjectType, expressionObjectType);
        }

        if (parameterType instanceof ObjectType parameterObjectType
                && parameterObjectType.getTypeArguments() != null
                && !parameterObjectType.rawEquals(expressionObjectType)
                && (!(expression instanceof MethodInvocationExpression)
                || isStrictConcreteSubtypeArgument(parameterObjectType, expressionObjectType))
                && isErasedHierarchyAssignable(parameterObjectType, expressionObjectType)) {
            return true;
        }

        return parameterType instanceof ObjectType parameterObjectType
                && unboundType instanceof GenericType
                && (!(expression instanceof MethodInvocationExpression)
                || isStrictConcreteSubtypeArgument(parameterObjectType, expressionObjectType))
                && isErasedHierarchyAssignable(parameterObjectType, expressionObjectType);
    }

    private boolean isStrictConcreteSubtypeArgument(ObjectType targetType, ObjectType expressionType) {
        return !targetType.rawEquals(expressionType)
                && isErasedHierarchyAssignable(targetType, expressionType);
    }

    private Type resolveSelfGenericCastTarget(Map<String, BaseType> localTypeBounds, Type parameterType, Expression expression) {
        if (!(expression instanceof ThisExpression)
                || !(parameterType instanceof ObjectType parameterObjectType)
                || !(expression.getType() instanceof ObjectType expressionObjectType)
                || !parameterObjectType.rawEquals(expressionObjectType)) {
            return null;
        }

        if (!(parameterObjectType.getTypeArguments() instanceof TypeArguments arguments)) {
            return null;
        }

        for (TypeArgument argument : arguments) {
            if (argument instanceof GenericType genericType
                    && localTypeBounds.get(genericType.getName()) instanceof ObjectType boundObjectType
                    && boundObjectType.rawEquals(expressionObjectType)) {
                return genericType;
            }
        }

        return null;
    }

    private static boolean shouldPreserveGenericClassArgument(Type parameterType, Type unboundType, Expression expression) {
        if (!(parameterType instanceof ObjectType parameterObjectType)
                || !(expression.getType() instanceof ObjectType expressionObjectType)
                || !"java/lang/Class".equals(parameterObjectType.getInternalName())
                || !parameterObjectType.rawEquals(expressionObjectType)
                || !(expression instanceof LocalVariableReferenceExpression || expression instanceof FieldReferenceExpression)
                || expressionObjectType.getTypeArguments() == null) {
            return false;
        }

        return hasTypeParameters(parameterType) || hasTypeParameters(unboundType);
    }

    private boolean sameErasedObjectType(ObjectType left, ObjectType right) {
        return left.rawEquals(right)
                || left.getInternalName().equals(right.getInternalName())
                || (typeMaker.isRawTypeAssignable(left, right) && typeMaker.isRawTypeAssignable(right, left));
    }

    private static boolean sameRawArrayType(Type left, Type right) {
        if (left == null || right == null || left.getDimension() == 0 || left.getDimension() != right.getDimension()) {
            return false;
        }
        if (!left.isObjectType() || !right.isObjectType()) {
            return left.equals(right);
        }
        return ((ObjectType) left).rawEquals((ObjectType) right);
    }

    private boolean isErasedHierarchyAssignable(ObjectType targetType, ObjectType sourceType) {
        return isErasedHierarchyAssignable(targetType, sourceType, new HashSet<>());
    }

    private boolean isErasedHierarchyAssignable(ObjectType targetType, ObjectType sourceType, Set<String> visitedTypes) {
        if (sameErasedObjectType(targetType, sourceType)) {
            return true;
        }
        if (!visitedTypes.add(sourceType.getInternalName())) {
            return false;
        }

        TypeTypes sourceTypeTypes = typeMaker.makeTypeTypes(sourceType.getInternalName());
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

    private Set<String> findKnownTypeParameters() {
        Set<String> genericIdentifiers = new HashSet<>();
        for (TypeParameter baseTypeParameters : typeParameters) {
            if (!staticContext || baseTypeParameters.staticContext()) {
                baseTypeParameters.type().forEach(typeParameter -> genericIdentifiers.add(typeParameter.getIdentifier()));
            }
        }
        return genericIdentifiers;
    }

    private Expression addCastExpression(Type type, Expression expression) {
        if (!expression.isCastExpression()) {
            searchFirstLineNumberVisitor.init();
            expression.accept(searchFirstLineNumberVisitor);
            return new CastExpression(searchFirstLineNumberVisitor.getLineNumber(), type, expression);
        }
        if (type.equals(expression.getExpression().getType())) {
            // Don't strip bytecode checkcast on raw receiver method invocations -
            // the erasure returns Object and the cast is needed for compilation.
            // Keep the original raw cast type (e.g. (Class) not (Class<?>)).
            CastExpression ce = (CastExpression) expression;
            if (ce.isByteCodeCheckCast()
                    && ce.getExpression() instanceof ClassFileMethodInvocationExpression mie
                    && mie.getExpression() != null
                    && mie.getExpression().getType() instanceof ObjectType receiverType
                    && receiverType.getTypeArguments() == null
                    && !ObjectType.TYPE_OBJECT.rawEquals(receiverType)) {
                return ce;
            }
            return expression.getExpression();
        }
        CastExpression ce = (CastExpression)expression;
        ce.setType(type);
        return ce;
    }

    private Expression addExplicitCastExpression(Type type, Expression expression) {
        if (!expression.isCastExpression()) {
            searchFirstLineNumberVisitor.init();
            expression.accept(searchFirstLineNumberVisitor);
            return new CastExpression(searchFirstLineNumberVisitor.getLineNumber(), type, expression);
        }

        CastExpression castExpression = (CastExpression) expression;
        if (type.equals(castExpression.getType())) {
            return castExpression;
        }
        castExpression.setType(type);
        return castExpression;
    }

    @Override
    public void visit(FloatConstantExpression expression) {}
    @Override
    public void visit(IntegerConstantExpression expression) {}
    @Override
    public void visit(ConstructorReferenceExpression expression) {}
    @Override
    public void visit(DoubleConstantExpression expression) {}
    @Override
    public void visit(EnumConstantReferenceExpression expression) {}
    @Override
    public void visit(LocalVariableReferenceExpression expression) {}
    @Override
    public void visit(LongConstantExpression expression) {}
    @Override
    public void visit(BreakStatement statement) {}
    @Override
    public void visit(ContinueStatement statement) {}
    @Override
    public void visit(NullExpression expression) {}
    @Override
    public void visit(ObjectTypeReferenceExpression expression) {}
    @Override
    public void visit(SuperExpression expression) {}
    @Override
    public void visit(QualifiedSuperExpression expression) {}
    @Override
    public void visit(ThisExpression expression) {}
    @Override
    public void visit(TypeReferenceDotClassExpression expression) {}
    @Override
    public void visit(WildcardExtendsTypeArgument type) {}
    @Override
    public void visit(ObjectType type) {}
    @Override
    public void visit(InnerObjectType type) {}
    @Override
    public void visit(WildcardSuperTypeArgument type) {}
    @Override
    public void visit(Types list) {}
    @Override
    public void visit(TypeParameterWithTypeBounds type) {}
}
