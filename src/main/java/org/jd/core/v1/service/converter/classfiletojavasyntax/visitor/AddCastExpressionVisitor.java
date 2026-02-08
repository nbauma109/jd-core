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
import org.jd.core.v1.model.javasyntax.type.AbstractTypeArgumentVisitor;
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
import org.jd.core.v1.service.converter.classfiletojavasyntax.model.javasyntax.expression.ClassFileMethodInvocationExpression;
import org.jd.core.v1.service.converter.classfiletojavasyntax.model.javasyntax.expression.ClassFileNewExpression;
import org.jd.core.v1.service.converter.classfiletojavasyntax.model.javasyntax.expression.ClassFileSuperConstructorInvocationExpression;
import org.jd.core.v1.service.converter.classfiletojavasyntax.util.TypeMaker;
import org.jd.core.v1.service.converter.classfiletojavasyntax.util.TypeMaker.TypeTypes;
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
    private String currentBodyInternalTypeName;
    private String currentMethodName;
    private String currentMethodDescriptor;

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
            String previousBodyInternalTypeName = currentBodyInternalTypeName;

            typeBounds = ((ClassFileBodyDeclaration)declaration).getTypeBounds();
            currentBodyInternalTypeName = ((ClassFileBodyDeclaration)declaration).getInternalTypeName();
            memberDeclarations.accept(this);
            currentBodyInternalTypeName = previousBodyInternalTypeName;
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
                String previousMethodName = currentMethodName;
                String previousMethodDescriptor = currentMethodDescriptor;

                typeBounds = ((ClassFileMethodDeclaration) declaration).getTypeBounds();
                returnedType = declaration.getReturnedType();
                exceptionTypes = declaration.getExceptionTypes();
                staticContext = declaration.isStatic();
                currentMethodName = declaration.getName();
                currentMethodDescriptor = declaration.getDescriptor();
                pushContext(declaration);
                safeAccept(declaration.getFormalParameters());
                statements.accept(this);
                typeBounds = tb;
                returnedType = rt;
                exceptionTypes = et;
                staticContext = sc;
                currentMethodName = previousMethodName;
                currentMethodDescriptor = previousMethodDescriptor;
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
            returnedType = ObjectType.TYPE_OBJECT;
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
        return updateExpression(typeBindings, localTypeBounds, returnedType, null, expression, false, true, false, false);
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
            declaration.setExpression(updateExpression(Collections.emptyMap(), typeBounds, type, null, expression, false, true, false, false));
        }
    }

    @Override
    public void visit(SuperConstructorInvocationExpression expression) {
        BaseExpression parameters = expression.getParameters();

        if (parameters != null && parameters.size() > 0) {
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

        if (parameters != null && parameters.size() > 0) {
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

    private static boolean containsMethodReference(BaseExpression parameters) {
        if (parameters == null) {
            return false;
        }
        for (Expression parameter : parameters) {
            if (parameter instanceof MethodReferenceExpression) {
                return true;
            }
        }
        return false;
    }

    private static boolean containsLambda(BaseExpression parameters) {
        if (parameters == null) {
            return false;
        }
        for (Expression parameter : parameters) {
            if (parameter instanceof LambdaIdentifiersExpression) {
                return true;
            }
        }
        return false;
    }

    private static boolean isMethodHandleInvoke(Expression expression) {
        return expression != null
                && expression.isMethodInvocationExpression()
                && "java/lang/invoke/MethodHandle".equals(expression.getInternalTypeName())
                && ("invokeExact".equals(expression.getName()) || "invoke".equals(expression.getName()));
    }

    private static boolean isPredicatesTruePredicate(Expression expression) {
        return expression != null
                && expression.isMethodInvocationExpression()
                && "org/apache/commons/lang3/function/Predicates".equals(expression.getInternalTypeName())
                && "truePredicate".equals(expression.getName());
    }

    private static boolean isJavaLangBoxedPrimitive(ObjectType type) {
        if (type == null) {
            return false;
        }
        return switch (type.getInternalName()) {
            case "java/lang/Byte", "java/lang/Short", "java/lang/Integer", "java/lang/Long",
                    "java/lang/Float", "java/lang/Double", "java/lang/Character", "java/lang/Boolean" -> true;
            default -> false;
        };
    }

    private static boolean hasBoxedPrimitiveObjectDisambiguation(BaseType parameterTypes, BaseExpression parameters) {
        if (parameterTypes == null || parameters == null || parameterTypes.size() != parameters.size()) {
            return false;
        }
        if (parameterTypes.isList() && parameters.isList()) {
            DefaultList<Type> typeList = parameterTypes.getList();
            DefaultList<Expression> expressionList = parameters.getList();
            for (int i = 0; i < expressionList.size() && i < typeList.size(); i++) {
                if (isBoxedPrimitiveObjectPair(typeList.get(i), expressionList.get(i))) {
                    return true;
                }
            }
            return false;
        }
        return isBoxedPrimitiveObjectPair(parameterTypes.getFirst(), parameters.getFirst());
    }

    private static boolean isBoxedPrimitiveObjectPair(Type parameterType, Expression expression) {
        if (!(parameterType instanceof ObjectType) || !ObjectType.TYPE_OBJECT.equals(parameterType) || expression == null) {
            return false;
        }
        return AutoboxingVisitor.isJavaLangMethodInvocation(expression)
                && AutoboxingVisitor.isBoxingMethod(expression)
                && !"java/lang/Boolean".equals(expression.getInternalTypeName());
    }

    private Type toDisambiguationCastType(ObjectType objectType) {
        if ("java/util/Collection".equals(objectType.getInternalName()) && objectType.getTypeArguments() != null) {
            return objectType.createType(null);
        }
        return hasKnownTypeParameters(objectType) ? objectType : objectType.createType(null);
    }

    private boolean isSelfOverloadForwardingCall(MethodInvocationExpression expression) {
        BaseExpression parameters = expression.getParameters();
        return currentBodyInternalTypeName != null
                && currentMethodName != null
                && currentMethodDescriptor != null
                && currentMethodName.equals(expression.getName())
                && expression.getDescriptor() != null
                && parameters != null
                && parameters.size() > 0
                && typeMaker.matchCount(currentBodyInternalTypeName, expression.getName(), parameters.size(), false) > 1
                && isCurrentTypeReceiver(expression);
    }

    private boolean isCurrentTypeReceiver(MethodInvocationExpression expression) {
        if (currentBodyInternalTypeName.equals(expression.getInternalTypeName())) {
            return true;
        }

        Expression receiver = expression.getExpression();
        if (receiver == null || receiver instanceof ThisExpression) {
            return true;
        }
        if (receiver instanceof SuperExpression || receiver instanceof QualifiedSuperExpression) {
            return false;
        }

        if (receiver.isObjectTypeReferenceExpression()) {
            ObjectType receiverObjectType = receiver.getObjectType();
            return receiverObjectType != null && currentBodyInternalTypeName.equals(receiverObjectType.getInternalName());
        }

        Type receiverType = receiver.getType();
        return receiverType instanceof ObjectType receiverObjectType
                && currentBodyInternalTypeName.equals(receiverObjectType.getInternalName());
    }

    private static String getParameterDescriptor(String methodDescriptor, int parameterIndex) {
        if (methodDescriptor == null || methodDescriptor.length() < 3 || methodDescriptor.charAt(0) != '(' || parameterIndex < 0) {
            return null;
        }
        int index = 0;
        int i = 1;
        while (i < methodDescriptor.length()) {
            char c = methodDescriptor.charAt(i);
            if (c == ')') {
                return null;
            }

            int start = i;
            while (c == '[') {
                i++;
                if (i >= methodDescriptor.length()) {
                    return null;
                }
                c = methodDescriptor.charAt(i);
            }

            if (c == 'L') {
                int semiColon = methodDescriptor.indexOf(';', i);
                if (semiColon == -1) {
                    return null;
                }
                if (index == parameterIndex) {
                    return methodDescriptor.substring(start, semiColon + 1);
                }
                i = semiColon + 1;
            } else {
                if (index == parameterIndex) {
                    return methodDescriptor.substring(start, i + 1);
                }
                i++;
            }
            index++;
        }
        return null;
    }

    private Type makeTypeFromDescriptor(String descriptor) {
        if (descriptor == null || descriptor.isEmpty()) {
            return null;
        }
        if (descriptor.length() > 2 && descriptor.charAt(0) == 'L' && descriptor.endsWith(";")) {
            return typeMaker.makeFromInternalTypeName(descriptor.substring(1, descriptor.length() - 1));
        }
        return switch (descriptor.charAt(0)) {
            case 'B' -> PrimitiveType.TYPE_BYTE;
            case 'C' -> PrimitiveType.TYPE_CHAR;
            case 'D' -> PrimitiveType.TYPE_DOUBLE;
            case 'F' -> PrimitiveType.TYPE_FLOAT;
            case 'I' -> PrimitiveType.TYPE_INT;
            case 'J' -> PrimitiveType.TYPE_LONG;
            case 'S' -> PrimitiveType.TYPE_SHORT;
            case 'Z' -> PrimitiveType.TYPE_BOOLEAN;
            default -> null;
        };
    }

    private BaseExpression enforceSelfOverloadParameterCasts(Map<String, BaseType> localTypeBounds, BaseType parameterTypes, BaseType unboundParameterTypes, BaseExpression parameters, String methodDescriptor) {
        if (parameterTypes == null || parameters == null || parameterTypes.size() != parameters.size()) {
            return parameters;
        }

        if (parameterTypes.isList() && parameters.isList()) {
            DefaultList<Type> typeList = parameterTypes.getList();
            DefaultList<Type> unboundTypeList = unboundParameterTypes == null ? null : unboundParameterTypes.getList();
            DefaultList<Expression> expressionList = parameters.getList();
            for (int i = 0; i < expressionList.size() && i < typeList.size(); i++) {
                Type unboundType = unboundTypeList == null ? null : unboundTypeList.get(i);
                String descriptorParameter = getParameterDescriptor(methodDescriptor, i);
                String currentMethodParameter = getParameterDescriptor(currentMethodDescriptor, i);
                expressionList.set(i, addSelfOverloadCastIfNeeded(localTypeBounds, typeList.get(i), unboundType, descriptorParameter, currentMethodParameter, expressionList.get(i)));
            }
            return parameters;
        }

        Type unboundType = unboundParameterTypes == null ? null : unboundParameterTypes.getFirst();
        String descriptorParameter = getParameterDescriptor(methodDescriptor, 0);
        String currentMethodParameter = getParameterDescriptor(currentMethodDescriptor, 0);
        return addSelfOverloadCastIfNeeded(localTypeBounds, parameterTypes.getFirst(), unboundType, descriptorParameter, currentMethodParameter, parameters.getFirst());
    }

    private Type resolveSelfOverloadCastType(Map<String, BaseType> localTypeBounds, Type parameterType, Type unboundType, String descriptorParameterType) {
        if (parameterType instanceof GenericType genericType) {
            BaseType boundType = localTypeBounds.get(genericType.getName());
            if (boundType instanceof ObjectType boundObjectType && !ObjectType.TYPE_OBJECT.equals(boundObjectType)) {
                return boundObjectType;
            }
            if (unboundType instanceof ObjectType unboundObjectType && !ObjectType.TYPE_OBJECT.equals(unboundObjectType)) {
                return unboundObjectType;
            }
            if (descriptorParameterType != null
                    && descriptorParameterType.length() > 2
                    && descriptorParameterType.charAt(0) == 'L'
                    && descriptorParameterType.endsWith(";")) {
                return typeMaker.makeFromInternalTypeName(descriptorParameterType.substring(1, descriptorParameterType.length() - 1));
            }
            return null;
        }
        return parameterType;
    }

    private Expression addSelfOverloadCastIfNeeded(Map<String, BaseType> localTypeBounds, Type parameterType, Type unboundType, String descriptorParameterType, String currentMethodParameterDescriptor, Expression argument) {
        Type castType = resolveSelfOverloadCastType(localTypeBounds, parameterType, unboundType, descriptorParameterType);
        if (argument instanceof LocalVariableReferenceExpression
                && descriptorParameterType != null
                && currentMethodParameterDescriptor != null
                && !descriptorParameterType.equals(currentMethodParameterDescriptor)) {
            Type descriptorType = makeTypeFromDescriptor(descriptorParameterType);
            if (descriptorType != null) {
                castType = descriptorType;
            }
        }
        if (castType == null || argument == null) {
            return argument;
        }

        if (argument instanceof CastExpression castExpression && castType.equals(castExpression.getType())) {
            return argument;
        }

        Type argumentType = argument.getType();
        if (argumentType == null || castType.equals(argumentType)) {
            return argument;
        }

        String parameterDescriptor = castType.getDescriptor();
        String argumentDescriptor = argumentType.getDescriptor();
        if (parameterDescriptor == null || argumentDescriptor == null || parameterDescriptor.equals(argumentDescriptor)) {
            return argument;
        }

        if (castType instanceof ObjectType parameterObjectType && argumentType instanceof ObjectType argumentObjectType) {
            if (!typeMaker.isRawTypeAssignable(parameterObjectType, argumentObjectType)) {
                return argument;
            }
            return addCastExpression(castType, argument);
        }

        if (castType.isPrimitiveType() && argumentType.isPrimitiveType()) {
            return addCastExpression(castType, argument);
        }

        return argument;
    }

    @Override
    public void visit(MethodInvocationExpression expression) {
        BaseExpression parameters = expression.getParameters();

        Map<String, TypeArgument> typeBindings = getLocalTypeBindings(expression);
        Map<String, BaseType> localTypeBounds = getLocalTypeBounds(expression);
        boolean selfOverloadForwardingCall = isSelfOverloadForwardingCall(expression);

        if (parameters != null && parameters.size() > 0) {
            BaseType parameterTypes = ((ClassFileMethodInvocationExpression)expression).getParameterTypes();
            BaseType unboundParameterTypes = ((ClassFileMethodInvocationExpression)expression).getUnboundParameterTypes();
            if (!expression.isVarArgs()) {
                Set<String> methodTypeParameterNames = getMethodTypeParameterNames((ClassFileMethodInvocationExpression) expression);
                addImplicitObjectBounds(localTypeBounds, unboundParameterTypes, methodTypeParameterNames);
            }
            boolean unique = typeMaker.matchCount(expression.getInternalTypeName(), expression.getName(), parameters.size(), false) <= 1;
            boolean hasMethodReference = containsMethodReference(parameters);
            boolean hasLambda = containsLambda(parameters);
            int contextualMatchCount = typeMaker.matchCount(typeBindings, localTypeBounds, expression.getInternalTypeName(), expression.getName(), parameters, false);
            boolean boxedPrimitiveObjectDisambiguation = !unique && hasBoxedPrimitiveObjectDisambiguation(parameterTypes, parameters);
            boolean forceCast = boxedPrimitiveObjectDisambiguation
                    || (!unique && contextualMatchCount > 1)
                    || (hasMethodReference && !unique)
                    || (hasLambda && !unique && contextualMatchCount > 0);
            boolean rawCast = false;
            BaseExpression updatedParameters = updateParameters(typeBindings, localTypeBounds, parameterTypes, unboundParameterTypes, parameters, forceCast, unique, rawCast, boxedPrimitiveObjectDisambiguation);
            if (selfOverloadForwardingCall) {
                updatedParameters = enforceSelfOverloadParameterCasts(localTypeBounds, parameterTypes, unboundParameterTypes, updatedParameters, expression.getDescriptor());
            }
            expression.setParameters(updatedParameters);
        }

        if (expression.getNonWildcardTypeArguments() != null) {
            if (shouldEraseExplicitTypeArguments(expression)) {
                expression.setNonWildcardTypeArguments(null);
            } else if (hasKnownTypeParameters(expression.getNonWildcardTypeArguments())) {
                safeAccept(expression.getNonWildcardTypeArguments());
            } else {
                expression.setNonWildcardTypeArguments(null);
            }
        }

        maybeDisambiguatePrimitiveReceiverOverload(expression);

        if (expression.getExpression() instanceof CastExpression ce && ce.isByteCodeCheckCast() && ce.getExpression() instanceof ClassFileMethodInvocationExpression && ce.getType() instanceof ObjectType) {
            ObjectType ot = (ObjectType) ce.getType();
            if (ot != null) {
                if (isCastToBeRemoved(typeBindings, localTypeBounds, ot, ce, true)) {
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

        expression.getExpression().accept(this);
    }

    private void maybeDisambiguatePrimitiveReceiverOverload(MethodInvocationExpression expression) {
        BaseExpression parameters = expression.getParameters();
        if (parameters != null && parameters.size() != 0) {
            return;
        }

        ObjectType objectType = null;
        ClassFileMethodInvocationExpression receiverCall = null;
        if (expression.getExpression() instanceof ClassFileMethodInvocationExpression mie) {
            receiverCall = mie;
        } else if (expression.getExpression() instanceof CastExpression ce && ce.getExpression() instanceof ClassFileMethodInvocationExpression mie) {
            receiverCall = mie;
            if (ce.getType() instanceof ObjectType castType && isJavaLangBoxedPrimitive(castType)) {
                objectType = castType;
            }
        }
        if (receiverCall == null) {
            return;
        }
        if (objectType == null) {
            Type boxedReceiverType = typeMaker.makeFromInternalTypeName(expression.getInternalTypeName());
            if (!(boxedReceiverType instanceof ObjectType boxedObjectType) || !isJavaLangBoxedPrimitive(boxedObjectType)) {
                return;
            }
            objectType = boxedObjectType;
        }

        BaseExpression receiverParameters = receiverCall.getParameters();
        if (receiverParameters == null || receiverParameters.size() != 1) {
            return;
        }

        Expression argument = receiverParameters.getFirst();
        if (argument == null || argument.getType() == null) {
            return;
        }
        if (argument instanceof CastExpression castExpression && objectType.equals(castExpression.getType())) {
            return;
        }

        Expression castedArgument = addCastExpression(objectType, argument);
        if (receiverParameters.isList()) {
            receiverParameters.getList().set(0, castedArgument);
        } else {
            receiverCall.setParameters(castedArgument);
        }
    }

    private boolean shouldEraseExplicitTypeArguments(MethodInvocationExpression expression) {
        BaseTypeArgument nonWildcardTypeArguments = expression.getNonWildcardTypeArguments();
        if (nonWildcardTypeArguments == null) {
            return false;
        }

        if ("of".equals(expression.getName()) && hasSingleWildcardTypedArgument(expression.getParameters())) {
            return true;
        }

        if (nonWildcardTypeArguments.isTypeArgumentList()) {
            for (TypeArgument typeArgument : nonWildcardTypeArguments.getTypeArgumentList()) {
                if (isOverConstrainedTypeArgument(typeArgument)) {
                    return true;
                }
            }
            return false;
        }

        return isOverConstrainedTypeArgument(nonWildcardTypeArguments.getTypeArgumentFirst());
    }

    private static boolean hasSingleWildcardTypedArgument(BaseExpression parameters) {
        if (parameters == null || parameters.size() != 1) {
            return false;
        }
        Expression parameter = parameters.getFirst();
        if (parameter == null || !(parameter.getType() instanceof ObjectType objectType)) {
            return false;
        }
        return containsWildcardTypeArgument(objectType.getTypeArguments());
    }

    private boolean isOverConstrainedTypeArgument(TypeArgument typeArgument) {
        if (typeArgument instanceof GenericType genericType) {
            return !findKnownTypeParameters().contains(genericType.getName());
        }
        if (typeArgument instanceof ObjectType objectType) {
            String internalName = objectType.getInternalName();
            if (objectType.getTypeArguments() == null) {
                TypeTypes typeTypes = typeMaker.makeTypeTypes(internalName);
                if (typeTypes != null && typeTypes.getTypeParameters() != null) {
                    return true;
                }
            }
            return "java/lang/Enum".equals(internalName)
                    || "java/lang/Object".equals(internalName)
                    || "java/lang/Throwable".equals(internalName);
        }
        return false;
    }

    @Override
    public void visit(NewExpression expression) {
        BaseExpression parameters = expression.getParameters();

        if (parameters != null) {
            boolean unique = typeMaker.matchCount(expression.getObjectType().getInternalName(), StringConstants.INSTANCE_CONSTRUCTOR, parameters.size(), true) <= 1;
            boolean hasMethodReference = containsMethodReference(parameters);
            int contextualMatchCount = typeMaker.matchCount(Collections.emptyMap(), typeBounds, expression.getObjectType().getInternalName(), StringConstants.INSTANCE_CONSTRUCTOR, parameters, true);
            boolean forceCast = hasMethodReference || (!unique && contextualMatchCount > 1);
            Type currentType = type == null ? returnedType : type;
            boolean rawCast = currentType instanceof ObjectType && expression.getType() instanceof ObjectType
                    && typeMaker.isRawTypeAssignable((ObjectType) currentType, expression.getObjectType())
                    && !typeMaker.isAssignable(typeBounds, (ObjectType) currentType, expression.getObjectType());
            if (rawCast) {
                expression.setObjectType(expression.getObjectType().createType(((ObjectType) currentType).getTypeArguments()));
            } else if (expression.getBodyDeclaration() == null && currentType instanceof ObjectType && expression.getObjectType().getTypeArguments() == null) {
                Type expressionType = expression.getType();
                if (expressionType instanceof ObjectType objectType
                        && expression.getObjectType().getInternalName().equals(objectType.getInternalName())
                        && objectType.getTypeArguments() != null) {
                    expression.setObjectType(objectType);
                } else {
                    TypeTypes typeTypes = typeMaker.makeTypeTypes(expression.getObjectType().getInternalName());
                    if (typeTypes != null && typeTypes.getTypeParameters() != null) {
                        expression.setObjectType(expression.getObjectType().createType(ObjectType.TYPE_UNDEFINED_OBJECT));
                    }
                }
            }
            BaseType parameterTypes = ((ClassFileNewExpression)expression).getParameterTypes();
            expression.setParameters(updateParameters(Collections.emptyMap(), typeBounds, parameterTypes, null, parameters, forceCast, unique, rawCast, false));
        }

        if (expression.getBodyDeclaration() != null && expression.getBodyDeclaration().isAnonymous()) {
            visitingAnonymousClass = true;
            visitBodyDeclaration(expression.getBodyDeclaration());
            visitingAnonymousClass = false;
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
                expression.setExpression(updateExpression(Collections.emptyMap(), typeBounds, localType, null, exp, false, true, false, false));
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

            expression.setRightExpression(updateExpression(Collections.emptyMap(), typeBounds, expression.getLeftExpression().getType(), null, rightExpression, false, true, false, false));
            return;
        }

        rightExpression.accept(this);
    }

    @Override
    public void visit(TernaryOperatorExpression expression) {
        Type expressionType = expression.getType();

        expression.getCondition().accept(this);
        expression.setTrueExpression(updateExpression(Collections.emptyMap(), typeBounds, expressionType, null, expression.getTrueExpression(), false, true, false, false));
        expression.setFalseExpression(updateExpression(Collections.emptyMap(), typeBounds, expressionType, null, expression.getFalseExpression(), false, true, false, false));
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

    protected BaseExpression updateParameters(Map<String, TypeArgument> typeBindings, Map<String, BaseType> localTypeBounds, BaseType types, BaseType unboundTypes, BaseExpression expressions, boolean forceCast, boolean unique, boolean rawCast, boolean boxedPrimitiveObjectDisambiguation) {
        if (expressions != null) {
            if (expressions.isList()) {
                DefaultList<Type> typeList = types.getList();
                DefaultList<Type> unboundTypeList = unboundTypes == null ? null : unboundTypes.getList();
                DefaultList<Expression> expressionList = expressions.getList();

                for (int i = expressionList.size() - 1; i >= 0; i--) {
                    Type unboundType = unboundTypes == null ?  null : unboundTypeList.get(i);
                    expressionList.set(i, updateParameter(typeBindings, localTypeBounds, typeList.get(i), unboundType, expressionList.get(i), forceCast, unique, rawCast, boxedPrimitiveObjectDisambiguation));
                }
            } else {
                Type unboundType = unboundTypes == null ?  null : unboundTypes.getFirst();
                expressions = updateParameter(typeBindings, localTypeBounds, types.getFirst(), unboundType, expressions.getFirst(), forceCast, unique, rawCast, boxedPrimitiveObjectDisambiguation);
            }
        }

        return expressions;
    }

    private Expression updateParameter(Map<String, TypeArgument> typeBindings, Map<String, BaseType> localTypeBounds, Type type, Type unboundType, Expression expression, boolean forceCast, boolean unique, boolean rawCast, boolean boxedPrimitiveObjectDisambiguation) {
        if (visitingAnonymousClass && expression instanceof FieldReferenceExpression fieldRef && expression.getType() instanceof ObjectType) {
            ObjectType ot = (ObjectType) expression.getType();
            if (ot.getTypeArguments() == null) {
                BaseTypeArgument parameterTypeArgument = parameterTypeArguments.get(expression.getName());
                if (parameterTypeArgument != null) {
                    fieldRef.setType(ot.createType(parameterTypeArgument));
                }
            }
        }

        expression = updateExpression(typeBindings, localTypeBounds, type, unboundType, expression, forceCast, unique, rawCast, boxedPrimitiveObjectDisambiguation);

        if (expression instanceof CastExpression castExpression) {
            Expression nestedExpression = castExpression.getExpression();
            if (isPredicatesTruePredicate(nestedExpression)) {
                expression = nestedExpression;
            } else if (castExpression.getType() instanceof ObjectType castObjectType
                    && "java/lang/Object".equals(castObjectType.getInternalName())
                    && castObjectType.getDimension() == 1
                    && nestedExpression != null
                    && nestedExpression.isMethodInvocationExpression()
                    && "org/apache/commons/lang3/ArrayUtils".equals(nestedExpression.getInternalTypeName())
                    && "newInstance".equals(nestedExpression.getName())) {
                expression = nestedExpression;
            } else if (castExpression.getType() instanceof ObjectType castObjectType
                    && "java/util/stream/Stream".equals(castObjectType.getInternalName())
                    && nestedExpression != null
                    && nestedExpression.isMethodInvocationExpression()
                    && "java/util/stream/Stream".equals(nestedExpression.getInternalTypeName())
                    && "map".equals(nestedExpression.getName())) {
                expression = nestedExpression;
            } else if (castExpression.getType() instanceof ObjectType castObjectType
                    && "java/util/function/Supplier".equals(castObjectType.getInternalName())
                    && nestedExpression != null
                    && nestedExpression.isMethodInvocationExpression()
                    && "org/apache/commons/lang3/function/Suppliers".equals(nestedExpression.getInternalTypeName())
                    && "nul".equals(nestedExpression.getName())) {
                expression = nestedExpression;
            } else if (ObjectType.TYPE_OBJECT.equals(castExpression.getType())
                    && AutoboxingVisitor.isJavaLangMethodInvocation(nestedExpression)
                    && AutoboxingVisitor.isBoxingMethod(nestedExpression)
                    && "java/lang/Boolean".equals(nestedExpression.getInternalTypeName())) {
                expression = nestedExpression;
            } else if (!(unboundType instanceof GenericType)
                    && castExpression.getType() instanceof ObjectType castObjectType
                    && type.equals(castObjectType)
                    && isJavaLangBoxedPrimitive(castObjectType)
                    && AutoboxingVisitor.isJavaLangMethodInvocation(nestedExpression)
                    && AutoboxingVisitor.isBoxingMethod(nestedExpression)
                    && castObjectType.getInternalName().equals(nestedExpression.getInternalTypeName())) {
                expression = nestedExpression;
            } else if (type instanceof ObjectType objectType
                    && "java/util/Collection".equals(objectType.getInternalName())
                    && objectType.getTypeArguments() instanceof WildcardExtendsTypeArgument
                    && castExpression.getType() instanceof ObjectType castObjectType
                    && castObjectType.equals(objectType)
                    && nestedExpression.getType() instanceof ObjectType nestedObjectType
                    && typeMaker.isRawTypeAssignable(objectType, nestedObjectType)
                    && nestedObjectType.getTypeArguments() != null
                    && objectType.getTypeArguments().isTypeArgumentAssignableFrom(typeMaker, typeBindings, localTypeBounds, nestedObjectType.getTypeArguments())) {
                expression = nestedExpression;
            }
        }

        if (forceCast
                && !boxedPrimitiveObjectDisambiguation
                && unboundType instanceof GenericType genericType
                && ObjectType.TYPE_OBJECT.equals(localTypeBounds.get(genericType.getName()))
                && expression instanceof CastExpression castExpression
                && castExpression.getType() instanceof ObjectType castObjectType
                && isJavaLangBoxedPrimitive(castObjectType)) {
            expression = new CastExpression(castExpression.getLineNumber(), ObjectType.TYPE_OBJECT, castExpression.getExpression());
        }

        if (forceCast
                && type instanceof ObjectType objectType
                && isJavaLangBoxedPrimitive(objectType)
                && unboundType instanceof GenericType genericType
                && !boxedPrimitiveObjectDisambiguation
                && ObjectType.TYPE_OBJECT.equals(localTypeBounds.get(genericType.getName()))
                && AutoboxingVisitor.isJavaLangMethodInvocation(expression)
                && AutoboxingVisitor.isBoxingMethod(expression)
                && objectType.getInternalName().equals(expression.getInternalTypeName())) {
            expression = addCastExpression(ObjectType.TYPE_OBJECT, expression);
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

    private Expression updateExpression(Map<String, TypeArgument> typeBindings, Map<String, BaseType> localTypeBounds, Type type, Type unboundType, Expression expression, boolean forceCast, boolean unique, boolean rawCast, boolean boxedPrimitiveObjectDisambiguation) {
        if (expression.isNullExpression()) {
            if (forceCast) {
                searchFirstLineNumberVisitor.init();
                expression.accept(searchFirstLineNumberVisitor);
                expression = new CastExpression(searchFirstLineNumberVisitor.getLineNumber(), type, expression);
            }
        } else if ("java/util/stream/Collectors".equals(expression.getInternalTypeName()) && "toList".equals(expression.getName())) {
        } else if (isMethodHandleInvoke(expression) && type instanceof ObjectType objectType && !ObjectType.TYPE_OBJECT.equals(objectType)) {
            expression = addCastExpression(hasKnownTypeParameters(objectType) ? objectType : objectType.createType(null), expression);
        } else if (forceCast && unboundType instanceof GenericType gt && unboundType.getDimension() == 0
                && localTypeBounds.get(gt.getName()) instanceof ObjectType ot
                && !ObjectType.TYPE_OBJECT.equals(ot)
                && !"java/util/Collection".equals(ot.getInternalName())
                && (ot.getTypeArguments() == null || !ot.getTypeArguments().findTypeParametersInType().isEmpty())
                && !"java/lang/Throwable".equals(ot.getInternalName())
                && !isPredicatesTruePredicate(expression)
                && expression.getType().isObjectType()
                && !(expression instanceof LocalVariableReferenceExpression)) {
            if (type instanceof ObjectType targetType && isJavaLangBoxedPrimitive(targetType)) {
                if (boxedPrimitiveObjectDisambiguation) {
                    expression = addCastExpression(ObjectType.TYPE_OBJECT, expression);
                }
            } else {
                expression = addCastExpression(ot, expression);
            }
        } else {
            if (expression instanceof ClassFileMethodInvocationExpression methodInvocationExpression
                    && isSelfOverloadForwardingCall(methodInvocationExpression)) {
                BaseExpression invocationParameters = methodInvocationExpression.getParameters();
                if (invocationParameters != null && invocationParameters.size() > 0) {
                    BaseExpression updatedInvocationParameters = enforceSelfOverloadParameterCasts(
                            localTypeBounds,
                            methodInvocationExpression.getParameterTypes(),
                            methodInvocationExpression.getUnboundParameterTypes(),
                            invocationParameters,
                            methodInvocationExpression.getDescriptor());
                    methodInvocationExpression.setParameters(updatedInvocationParameters);
                }
            }

            Type expressionType = expression.getType();

            if (forceCast && type instanceof ObjectType objectType
                    && (expression instanceof LambdaIdentifiersExpression || expression instanceof MethodReferenceExpression)) {
                ObjectType lambdaCastType = objectType;
                if (unboundType instanceof ObjectType unboundObjectType && hasKnownTypeParameters(unboundObjectType)) {
                    lambdaCastType = unboundObjectType;
                } else if (!hasKnownTypeParameters(objectType)) {
                    lambdaCastType = objectType.createType(null);
                }
                if (!(containsThrowableTypeArgument(lambdaCastType.getTypeArguments()) || containsWildcardTypeArgument(lambdaCastType.getTypeArguments()))) {
                    expression = addCastExpression(expression instanceof LambdaIdentifiersExpression ? lambdaCastType : toDisambiguationCastType(lambdaCastType), expression);
                }
                expressionType = expression.getType();
            }

            if (!expressionType.equals(type)) {
                if (type.isObjectType()) {
                    if (expressionType.isObjectType()) {
                        ObjectType objectType = (ObjectType) type;
                        ObjectType expressionObjectType = (ObjectType) expressionType;
                        BaseTypeArgument ta1 = objectType.getTypeArguments();
                        BaseTypeArgument ta2 = expressionObjectType.getTypeArguments();

                        if (objectType.rawEquals(expressionObjectType)
                                && ta1 != null
                                && ta2 != null
                                && !ta1.equals(ta2)
                                && expression instanceof LocalVariableReferenceExpression
                                && !containsWildcardTypeArgument(ta1)
                                && !("java/lang/Class".equals(objectType.getInternalName()) && containsWildcardTypeArgument(ta2))) {
                            expression = addCastExpression(objectType, expression);
                            expressionType = expression.getType();
                            expressionObjectType = (ObjectType) expressionType;
                            ta2 = expressionObjectType.getTypeArguments();
                        }

                        if (objectType.rawEquals(expressionObjectType)
                                && "java/lang/Class".equals(objectType.getInternalName())
                                && ta1 != null
                                && !ta1.findTypeParametersInType().isEmpty()
                                && (ta2 == null || containsWildcardTypeArgument(ta2))
                                && expression.isMethodInvocationExpression()
                                && ("getClass".equals(expression.getName()) || "getComponentType".equals(expression.getName()))) {
                            expression = addCastExpression(objectType, expression);
                            expressionType = expression.getType();
                            expressionObjectType = (ObjectType) expressionType;
                        }

                        if (rawCast) {
                            expression = addCastExpression(objectType.createType(null), expression);
                        } else if (forceCast && !isPredicatesTruePredicate(expression) && (!objectType.rawEquals(expressionObjectType)
                                || expression instanceof LambdaIdentifiersExpression
                                || expression instanceof MethodReferenceExpression)) {
                            boolean castedBooleanToObject = "java/lang/Object".equals(objectType.getInternalName())
                                    && "java/lang/Boolean".equals(expressionObjectType.getInternalName());
                            boolean collectionDisambiguation = "java/util/Collection".equals(objectType.getInternalName()) && objectType.getTypeArguments() != null;
                            boolean assignableNoDisambiguationNeeded = !expression.isNewArray()
                                    && !expression.isNewInitializedArray()
                                    && !ObjectType.TYPE_OBJECT.equals(objectType)
                                    && !collectionDisambiguation
                                    && typeMaker.isAssignable(typeBindings, localTypeBounds, objectType, unboundType, expressionObjectType);
                            if (!castedBooleanToObject
                                    && !assignableNoDisambiguationNeeded
                                    && !(expression instanceof LocalVariableReferenceExpression && objectType.rawEquals(expressionObjectType))) {
                                // Force disambiguation of method invocation => Add cast
                                if (expression.isNewExpression()) {
                                    ClassFileNewExpression ne = (ClassFileNewExpression)expression;
                                    ne.setObjectType(ne.getObjectType().createType(null));
                                }
                                if (expression instanceof LambdaIdentifiersExpression) {
                                    if (!(containsThrowableTypeArgument(objectType.getTypeArguments()) || containsWildcardTypeArgument(objectType.getTypeArguments()))) {
                                        expression = addCastExpression(objectType, expression);
                                    }
                                } else if (expression instanceof MethodReferenceExpression) {
                                    if (!(containsThrowableTypeArgument(objectType.getTypeArguments()) || containsWildcardTypeArgument(objectType.getTypeArguments()))) {
                                        expression = addCastExpression(toDisambiguationCastType(objectType), expression);
                                    }
                                } else {
                                    expression = addCastExpression(toDisambiguationCastType(objectType), expression);
                                }
                            }
                        } else if (forceCast
                                && unboundType instanceof GenericType genericType
                                && (boxedPrimitiveObjectDisambiguation || ObjectType.TYPE_OBJECT.equals(localTypeBounds.get(genericType.getName())))
                                && isJavaLangBoxedPrimitive(objectType)
                                && objectType.rawEquals(expressionObjectType)) {
                            expression = addCastExpression(ObjectType.TYPE_OBJECT, expression);
                        } else if (!ObjectType.TYPE_OBJECT.equals(type)
                                && !typeMaker.isAssignable(typeBindings, localTypeBounds, objectType, unboundType, expressionObjectType)
                                && !(objectType.rawEquals(expressionObjectType)
                                     && objectType.getTypeArguments() == null
                                     && expressionObjectType.getTypeArguments() == null)
                                && !(expression instanceof LambdaIdentifiersExpression)
                                && !(expression instanceof MethodReferenceExpression)
                                && !(expression instanceof LocalVariableReferenceExpression
                                     && objectType.rawEquals(expressionObjectType)
                                     && objectType.getTypeArguments() == null
                                     && expressionObjectType.getTypeArguments() == null)) {
                            Type t = type;
                            boolean typeArgumentsIncompatible = false;
                            boolean rawTypeAssignable = typeMaker.isRawTypeAssignable(objectType, expressionObjectType);
                            boolean missingTargetTypeArguments = ta1 != null && ta2 == null;
                            boolean differentKnownGenericTypeArguments = ta1 instanceof GenericType leftGeneric
                                    && ta2 instanceof GenericType rightGeneric
                                    && findKnownTypeParameters().contains(leftGeneric.getName())
                                    && findKnownTypeParameters().contains(rightGeneric.getName())
                                    && !leftGeneric.getName().equals(rightGeneric.getName());

                            if (ta1 != null && ta2 != null && !ta1.isTypeArgumentAssignableFrom(typeMaker, typeBindings, localTypeBounds, ta2)) {
                                // Incompatible typeArgument arguments => Add cast
                                typeArgumentsIncompatible = true;
                                t = objectType.createType(ta1.isGenericTypeArgument() ? ta1 :  null);
                            }
                            if (!expression.isNew()
                                    && hasKnownTypeParameters(t)
                                    && !(ta1 instanceof WildcardSuperTypeArgument)
                                    && (!rawTypeAssignable || typeArgumentsIncompatible || missingTargetTypeArguments)
                                    && !differentKnownGenericTypeArguments) {
                                expression = addCastExpression(t, expression);
                            }
                        }
                    } else if (type.getDimension() == 0 && expressionType.isGenericType() && (!ObjectType.TYPE_OBJECT.equals(type) || forceCast)) {
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
                        && (type.getDimension() != 0 || !visitingLambda)) {
                    expression = addCastExpression(type, expression);
                }
            }

            if (expression instanceof CastExpression && isCastToBeRemoved(typeBindings, localTypeBounds, type, (CastExpression) expression, unique)) {
                // Remove cast expression
                expression = expression.getExpression();
            }
            if (expression instanceof CastExpression castExpression
                    && castExpression.getExpression() instanceof ClassFileMethodInvocationExpression methodInvocationExpression
                    && currentMethodName != null
                    && currentMethodName.equals(methodInvocationExpression.getName())
                    && isCurrentTypeReceiver(methodInvocationExpression)) {
                BaseExpression invocationParameters = methodInvocationExpression.getParameters();
                if (invocationParameters != null && invocationParameters.size() > 0) {
                    BaseExpression updatedInvocationParameters = enforceSelfOverloadParameterCasts(
                            localTypeBounds,
                            methodInvocationExpression.getParameterTypes(),
                            methodInvocationExpression.getUnboundParameterTypes(),
                            invocationParameters,
                            methodInvocationExpression.getDescriptor());
                    methodInvocationExpression.setParameters(updatedInvocationParameters);
                }
            }
            expression.accept(this);
        }

        return expression;
    }

    private boolean isCastToBeRemoved(Map<String, TypeArgument> typeBindings, Map<String, BaseType> localTypeBounds, Type type, CastExpression expression, boolean unique) {
        if (!hasKnownTypeParameters(expression.getType())) {
            return true;
        }
        Expression nestedExpression = expression.getExpression();
        if (nestedExpression instanceof LambdaIdentifiersExpression || nestedExpression instanceof MethodReferenceExpression) {
            if (nestedExpression instanceof MethodReferenceExpression
                    && expression.getType() instanceof ObjectType castType
                    && "java/util/function/IntFunction".equals(castType.getInternalName())
                    && castType.getTypeArguments() == null) {
                return true;
            }
            if (!unique) {
                if (nestedExpression instanceof LambdaIdentifiersExpression
                        && expression.getType() instanceof ObjectType castType
                        && castType.getTypeArguments() == null
                        && type instanceof ObjectType targetType
                        && targetType.getTypeArguments() == null
                        && castType.rawEquals(targetType)) {
                    return true;
                }
                return false;
            }
            if (type instanceof ObjectType objectType && objectType.getTypeArguments() != null
                    && !objectType.getTypeArguments().findTypeParametersInType().isEmpty()) {
                return false;
            }
        }
        if (ObjectType.TYPE_OBJECT.equals(type)
                && "java/lang/Boolean".equals(nestedExpression.getInternalTypeName())
                && "valueOf".equals(nestedExpression.getName())) {
            return true;
        }
        if ("org/apache/commons/lang3/function/Predicates".equals(nestedExpression.getInternalTypeName())
                && "truePredicate".equals(nestedExpression.getName())) {
            return true;
        }
        if (expression.getType() instanceof ObjectType castObjectType
                && isJavaLangBoxedPrimitive(castObjectType)
                && AutoboxingVisitor.isJavaLangMethodInvocation(nestedExpression)
                && AutoboxingVisitor.isBoxingMethod(nestedExpression)
                && castObjectType.getInternalName().equals(nestedExpression.getInternalTypeName())) {
            return false;
        }
        if (isMethodHandleInvoke(nestedExpression) && type instanceof ObjectType left && !ObjectType.TYPE_OBJECT.equals(left)) {
            return false;
        }
        if (isArrayConstructorToArrayCast(type, nestedExpression)) {
            return true;
        }
        if (nestedExpression instanceof LocalVariableReferenceExpression
                && expression.getType() instanceof ObjectType castType
                && castType.getTypeArguments() == null
                && nestedExpression.getType() instanceof ObjectType nestedObjectType
                && castType.rawEquals(nestedObjectType)) {
            return true;
        }
        if (nestedExpression.getExpression() instanceof FieldReferenceExpression fre && fieldNamesInLambda.contains(fre.getName())) {
            return false;
        }
        Type nestedExpressionType = nestedExpression.getType();

        if (type.isObjectType() && nestedExpressionType.isObjectType()) {
            ObjectType left = (ObjectType) type;
            ObjectType right = (ObjectType) nestedExpressionType;
            if (left.rawEquals(right)) {
                BaseTypeArgument leftTypeArguments = left.getTypeArguments();
                BaseTypeArgument rightTypeArguments = right.getTypeArguments();
                if (leftTypeArguments == null && rightTypeArguments != null) {
                    return true;
                }
                if (leftTypeArguments != null && (rightTypeArguments == null || !leftTypeArguments.equals(rightTypeArguments))) {
                    return false;
                }
            }
            if (!visitingLambda
                    && nestedExpression instanceof ClassFileMethodInvocationExpression mie
                    && mie.getUnboundType() instanceof GenericType
                    && mie.getParameters() != null
                    && typeMaker.matchCount(mie.getInternalTypeName(), mie.getName(), mie.getParameters().size(), true) > 1
                    && typeMaker.matchCount(mie.getTypeBindings(), mie.getTypeBounds(), mie.getInternalTypeName(), mie.getName(), mie.getParameters(), true) > 1) {
                return false;
            }
            if (nestedExpression instanceof ClassFileMethodInvocationExpression mie
                    && mie.getParameters() != null
                    && isJavaLangBoxedPrimitive(left)
                    && typeMaker.matchCount(mie.getInternalTypeName(), mie.getName(), mie.getParameters().size(), true) > 1) {
                return false;
            }
            if (left.equals(right) || unique && typeMaker.isAssignable(typeBindings, localTypeBounds, left, right)) {
                return true;
            }
        }
        return false;
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

    private boolean hasKnownTypeParameters(BaseTypeArgument type) {
        Set<String> genericIdentifiersInType = type.findTypeParametersInType();
        Set<String> genericIdentifiersInScope = findKnownTypeParameters();
        return genericIdentifiersInScope.containsAll(genericIdentifiersInType);
    }

    private static boolean containsThrowableTypeArgument(BaseTypeArgument typeArgument) {
        if (typeArgument == null) {
            return false;
        }

        final boolean[] containsThrowable = { false };
        typeArgument.accept(new AbstractTypeArgumentVisitor() {
            @Override
            public void visit(ObjectType type) {
                if ("java/lang/Throwable".equals(type.getInternalName())) {
                    containsThrowable[0] = true;
                    return;
                }
                super.visit(type);
            }
        });
        return containsThrowable[0];
    }

    private static boolean containsWildcardTypeArgument(BaseTypeArgument typeArgument) {
        if (typeArgument == null) {
            return false;
        }

        final boolean[] containsWildcard = { false };
        typeArgument.accept(new AbstractTypeArgumentVisitor() {
            @Override
            public void visit(WildcardTypeArgument type) {
                containsWildcard[0] = true;
            }

            @Override
            public void visit(WildcardExtendsTypeArgument type) {
                containsWildcard[0] = true;
            }

            @Override
            public void visit(WildcardSuperTypeArgument type) {
                containsWildcard[0] = true;
            }
        });
        return containsWildcard[0];
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
            return expression.getExpression();
        }
        CastExpression ce = (CastExpression)expression;
        ce.setType(type);
        return ce;
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
