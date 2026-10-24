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
import org.jd.core.v1.model.javasyntax.expression.BooleanExpression;
import org.jd.core.v1.model.javasyntax.expression.CastExpression;
import org.jd.core.v1.model.javasyntax.expression.ConstructorInvocationExpression;
import org.jd.core.v1.model.javasyntax.expression.ConstructorReferenceExpression;
import org.jd.core.v1.model.javasyntax.expression.DoubleConstantExpression;
import org.jd.core.v1.model.javasyntax.expression.EnumConstantReferenceExpression;
import org.jd.core.v1.model.javasyntax.expression.Expression;
import org.jd.core.v1.model.javasyntax.expression.Expressions;
import org.jd.core.v1.model.javasyntax.expression.FieldReferenceExpression;
import org.jd.core.v1.model.javasyntax.expression.FloatConstantExpression;
import org.jd.core.v1.model.javasyntax.expression.IntegerConstantExpression;
import org.jd.core.v1.model.javasyntax.expression.LambdaIdentifiersExpression;
import org.jd.core.v1.model.javasyntax.expression.LocalVariableReferenceExpression;
import org.jd.core.v1.model.javasyntax.expression.LongConstantExpression;
import org.jd.core.v1.model.javasyntax.expression.MethodInvocationExpression;
import org.jd.core.v1.model.javasyntax.expression.MethodReferenceExpression;
import org.jd.core.v1.model.javasyntax.expression.NewArray;
import org.jd.core.v1.model.javasyntax.expression.NewExpression;
import org.jd.core.v1.model.javasyntax.expression.NoExpression;
import org.jd.core.v1.model.javasyntax.expression.NewInitializedArray;
import org.jd.core.v1.model.javasyntax.expression.NullExpression;
import org.jd.core.v1.model.javasyntax.expression.ObjectTypeReferenceExpression;
import org.jd.core.v1.model.javasyntax.expression.QualifiedSuperExpression;
import org.jd.core.v1.model.javasyntax.expression.SuperConstructorInvocationExpression;
import org.jd.core.v1.model.javasyntax.expression.SuperExpression;
import org.jd.core.v1.model.javasyntax.expression.TernaryOperatorExpression;
import org.jd.core.v1.model.javasyntax.expression.ThisExpression;
import org.jd.core.v1.model.javasyntax.expression.TypeReferenceDotClassExpression;
import org.jd.core.v1.model.javasyntax.statement.LocalVariableDeclarationStatement;
import org.jd.core.v1.model.javasyntax.statement.Statement;
import org.jd.core.v1.model.javasyntax.statement.Statements;
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
import org.jd.core.v1.service.converter.classfiletojavasyntax.model.javasyntax.expression.ClassFileMethodInvocationExpression;
import org.jd.core.v1.service.converter.classfiletojavasyntax.model.javasyntax.expression.ClassFileNewExpression;
import org.jd.core.v1.service.converter.classfiletojavasyntax.model.javasyntax.expression.ClassFileSuperConstructorInvocationExpression;
import org.jd.core.v1.service.converter.classfiletojavasyntax.util.InheritedMethodFinder;
import org.jd.core.v1.service.converter.classfiletojavasyntax.util.SingleAbstractMethodFinder;
import org.jd.core.v1.service.converter.classfiletojavasyntax.util.TypeMaker;
import org.jd.core.v1.service.converter.classfiletojavasyntax.util.TypeMaker.TypeTypes;
import org.jd.core.v1.service.converter.classfiletojavasyntax.util.Utils;
import org.jd.core.v1.util.DefaultList;
import org.jd.core.v1.util.StringConstants;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Deque;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.ListIterator;
import java.util.Objects;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;
import java.util.stream.IntStream;
import java.util.function.Predicate;

import org.apache.bcel.generic.ConstantPoolGen;
import org.apache.bcel.generic.InstructionHandle;
import org.apache.bcel.generic.InstructionList;
import org.apache.bcel.generic.InvokeInstruction;
import org.jd.core.v1.api.loader.Loader;

import static org.apache.bcel.Const.ACC_BRIDGE;
import static org.apache.bcel.Const.ACC_PRIVATE;
import static org.apache.bcel.Const.ACC_STATIC;
import static org.apache.bcel.Const.ACC_SYNTHETIC;
import static org.jd.core.v1.model.javasyntax.type.PrimitiveType.TYPE_BYTE;
import static org.jd.core.v1.model.javasyntax.type.PrimitiveType.TYPE_BOOLEAN;
import static org.jd.core.v1.model.javasyntax.type.PrimitiveType.TYPE_INT;
import static org.jd.core.v1.model.javasyntax.type.PrimitiveType.TYPE_SHORT;
import static org.jd.core.v1.model.javasyntax.type.PrimitiveType.FLAG_BOOLEAN;

public class AddCastExpressionVisitor extends AbstractJavaSyntaxVisitor {
    private final SearchFirstLineNumberVisitor searchFirstLineNumberVisitor = new SearchFirstLineNumberVisitor();

    private final TypeMaker typeMaker;
    private final Loader loader;
    private boolean visitingVarArgsInvocation;
    private Set<String> typeVariablesOfTheCall = Collections.emptySet();
    private final SingleAbstractMethodFinder singleAbstractMethodFinder;
    private final InheritedMethodFinder inheritedMethodFinder;
    private boolean lambdaReturnFromTarget;
    private boolean castToGenericInLambda;
    private Map<String, BaseType> typeBounds;
    private Type returnedType;
    private BaseType exceptionTypes;
    private final Deque<TypeParameter> typeParameters = new ArrayDeque<>();
    private final Map<String, BaseTypeArgument> parameterTypeArguments = new HashMap<>();
    private Type type;
    private boolean visitingAnonymousClass;
    private boolean visitingLambda;
    private Expression memberReceiver;
    private boolean visitingWitnessedInvocation;
    private Set<String> typeVariablesSharedAcrossParameters = Collections.emptySet();
    private final Set<String> fieldNamesInLambda = new HashSet<>();
    private boolean staticContext;
    /**
     * The number of scopes of type variables which are outside the static member (or the static class) which is visited: the type variables of
     * classes are not visible from it
     */
    private int staticBoundary;
    private Type expectedType;

    private record TypeParameter(BaseTypeParameter type, boolean methodLevel) {
        TypeParameter(BaseTypeParameter type) {
            this(type, false);
        }
    }

    public AddCastExpressionVisitor(TypeMaker typeMaker) {
        this(typeMaker, null);
    }

    public AddCastExpressionVisitor(TypeMaker typeMaker, Loader loader) {
        this.typeMaker = typeMaker;
        this.loader = loader;
        this.singleAbstractMethodFinder = loader == null ? null : new SingleAbstractMethodFinder(loader);
        this.inheritedMethodFinder = loader == null ? null : new InheritedMethodFinder(loader);
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
            boolean sc = staticContext;
            int sb = staticBoundary;

            type = declaration.getType();
            if ((declaration.getFlags() & ACC_STATIC) != 0) {
                // The type variables of the class are not visible to its static fields
                staticContext = true;
                staticBoundary = typeParameters.size();
            }
            declaration.getFieldDeclarators().accept(this);
            staticContext = sc;
            staticBoundary = sb;
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

            boolean sc = staticContext;
            int sb = staticBoundary;

            typeBounds = ((ClassFileStaticInitializerDeclaration)declaration).getTypeBounds();
            staticContext = true;
            staticBoundary = typeParameters.size();
            statements.accept(this);
            staticContext = sc;
            staticBoundary = sb;
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
                int sb = staticBoundary;

                typeBounds = ((ClassFileMethodDeclaration) declaration).getTypeBounds();
                returnedType = declaration.getReturnedType();
                exceptionTypes = declaration.getExceptionTypes();
                if (declaration.isStatic()) {
                    staticContext = true;
                    staticBoundary = typeParameters.size();
                }
                pushContext(declaration);
                safeAccept(declaration.getFormalParameters());
                statements.accept(this);
                typeBounds = tb;
                returnedType = rt;
                exceptionTypes = et;
                staticContext = sc;
                staticBoundary = sb;
                popContext(declaration);
            }
        }
    }

    @Override
    public void visit(ClassDeclaration declaration) {
        int sb = staticBoundary;

        if (declaration.isStatic()) {
            staticBoundary = typeParameters.size();
        }
        pushContext(declaration);
        super.visit(declaration);
        popContext(declaration);
        staticBoundary = sb;
    }

    @Override
    public void visit(FormalParameter declaration) {
        if (!visitingAnonymousClass && declaration.getType() instanceof ObjectType ot) {
            parameterTypeArguments.put(declaration.getName(), ot.getTypeArguments());
        }
    }

    public void pushContext(MethodDeclaration declaration) {
        if (declaration.getTypeParameters() != null) {
            typeParameters.push(new TypeParameter(declaration.getTypeParameters(), true));
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
            typeParameters.push(new TypeParameter(declaration.getTypeParameters()));
        }
    }

    public void popContext(InterfaceDeclaration declaration) {
        if (declaration.getTypeParameters() != null) {
            typeParameters.pop();
        }
    }

    public void pushContext(RecordDeclaration declaration) {
        if (declaration.getTypeParameters() != null) {
            typeParameters.push(new TypeParameter(declaration.getTypeParameters()));
        }
    }

    public void popContext(RecordDeclaration declaration) {
        if (declaration.getTypeParameters() != null) {
            typeParameters.pop();
        }
    }

    @Override
    public void visit(LambdaIdentifiersExpression expression) {
        boolean outerVisitingLambda = visitingLambda;
        visitingLambda = true;
        BaseStatement statements = expression.getStatements();

        if (statements != null) {
            Type rt = returnedType;
            boolean lrft = lambdaReturnFromTarget;
            Type lambdaReturnedType = expression.getReturnedType();
            // The synthetic method of a lambda has the erased return type: the target functional interface knows better
            Type functionalReturnedType = lambdaReturnedTypeFromTarget(lambdaReturnedType);
            if (functionalReturnedType != null) {
                returnedType = functionalReturnedType;
            } else {
                returnedType = lambdaReturnedType != null ? lambdaReturnedType : ObjectType.TYPE_OBJECT;
            }
            lambdaReturnFromTarget = functionalReturnedType != null;
            statements.accept(this);
            lambdaReturnFromTarget = lrft;
            returnedType = rt;
        }
        visitingLambda = outerVisitingLambda;
    }

    /**
     * The erased return type of a lambda is Object, or the bound of the type variable (Number for 'T extends Number'):
     * the type argument of the target is then what the lambda returns, when it is a type variable.
     */
    private Type lambdaReturnedTypeFromTarget(Type erasedReturnedType) {
        if (erasedReturnedType != null && !(erasedReturnedType instanceof ObjectType)) {
            return null;
        }

        Type fromTarget = functionalReturnedType(expectedType);
        boolean erasedToObject = erasedReturnedType == null || ObjectType.TYPE_OBJECT.equals(erasedReturnedType);

        return fromTarget != null && (erasedToObject || fromTarget.isGenericType() || isParameterizedErasedType(fromTarget, erasedReturnedType)) ? fromTarget : null;
    }

    /** @return true if the type is the parameterization of the (raw) type which the synthetic method of the lambda returns */
    private static boolean isParameterizedErasedType(Type parameterized, Type erased) {
        return parameterized instanceof ObjectType parameterizedType && erased instanceof ObjectType erasedType
                && parameterizedType.getTypeArguments() != null && erasedType.getTypeArguments() == null
                && parameterizedType.getInternalName().equals(erasedType.getInternalName());
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
        boolean ulr = castToGenericInLambda;
        castToGenericInLambda = visitingLambda && lambdaReturnFromTarget;
        try {
            return updateExpression(typeBindings, localTypeBounds, returnedType, null, expression, false, true, false);
        } finally {
            castToGenericInLambda = ulr;
        }
    }

    /**
     * The type returned by the single abstract method of a generic functional interface, when it is one of the interface's
     * own type variables, bound to the type arguments of the target type; null if it cannot be determined.
     */
    private Type functionalReturnedType(Type target) {
        if (loader == null || !(target instanceof ObjectType targetType) || targetType.getDimension() != 0
                || targetType.getTypeArguments() == null) {
            return null;
        }
        String[] method = singleAbstractMethodFinder.find(targetType.getInternalName());
        if (method.length == 0) {
            return null;
        }
        // The method may be inherited: it is then seen through the parameterization of the interface which declares it
        String internalName = method[0];
        ObjectType declaring = parameterizationOfDeclaringInterface(targetType, internalName);
        if (declaring == null || declaring.getTypeArguments() == null) {
            return null;
        }
        Type returned = typeMaker.makeMethodTypes(internalName, method[1], method[2]).getReturnedType();
        TypeTypes typeTypes = typeMaker.makeTypeTypes(internalName);
        if (typeTypes == null || typeTypes.getTypeParameters() == null || returned.findTypeParametersInType().isEmpty()) {
            return null;
        }
        List<TypeArgument> typeArguments = toTypeArgumentList(declaring.getTypeArguments());
        if (!(returned instanceof GenericType genericReturned) || genericReturned.getDimension() != 0) {
            return bindTypeVariables(returned, typeTypes.getTypeParameters(), typeArguments);
        }
        int index = 0;
        for (org.jd.core.v1.model.javasyntax.type.TypeParameter typeParameter : typeTypes.getTypeParameters()) {
            if (typeParameter.getIdentifier().equals(genericReturned.getName())) {
                return index < typeArguments.size() ? upperBound(typeArguments.get(index)) : null;
            }
            index++;
        }
        return null;
    }

    private ObjectType parameterizationOfDeclaringInterface(ObjectType targetType, String declaringInternalName) {
        return declaringInternalName.equals(targetType.getInternalName()) ? targetType
                : typeMaker.searchSuperParameterizedType(typeMaker.makeFromInternalTypeName(declaringInternalName), targetType);
    }

    /** @return the type which contains type variables of an interface (List&lt;T&gt;), seen through the type arguments of its parameterization */
    private static Type bindTypeVariables(Type returned, BaseTypeParameter typeParameters, List<TypeArgument> typeArguments) {
        Map<String, TypeArgument> bindings = new HashMap<>();
        int index = 0;

        for (org.jd.core.v1.model.javasyntax.type.TypeParameter typeParameter : typeParameters) {
            if (index < typeArguments.size()) {
                bindings.put(typeParameter.getIdentifier(), typeArguments.get(index));
            }
            index++;
        }

        BindTypesToTypesVisitor visitor = new BindTypesToTypesVisitor();

        visitor.setBindings(bindings);
        visitor.init();
        returned.accept(visitor);
        return visitor.getType() instanceof Type bound ? bound : null;
    }

    /** A lambda of a 'Supplier<? extends T>' is typed by its upper bound, one of a 'Supplier<? super T>' by its lower bound (the ground type is Supplier<T>) */
    private static Type upperBound(TypeArgument argument) {
        if (argument instanceof WildcardExtendsTypeArgument wildcard) {
            return wildcard.type();
        }
        if (argument instanceof WildcardSuperTypeArgument wildcard) {
            return wildcard.type();
        }
        return argument instanceof Type argumentType ? argumentType : null;
    }

    @Override
    public void visit(ThrowStatement statement) {
        if (statement.getExpression() instanceof MethodInvocationExpression call) {
            writeTypeArgumentOfInstanceWhoseMethodThrowsIt(call);
        }
        if (exceptionTypes != null && exceptionTypes.size() == 1) {
            Type exceptionType = exceptionTypes.getFirst();

            if (exceptionType.isGenericType() && !statement.getExpression().getType().equals(exceptionType)
                    && !isUncheckedException(statement.getExpression().getType())) {
                statement.setExpression(addCastExpression(exceptionType, statement.getExpression()));
            }
        }
    }

    private boolean isUncheckedException(Type type) {
        // Unchecked exceptions never need a cast to the method's generic 'throws' type
        return type instanceof ObjectType ot
                && (typeMaker.isRawTypeAssignable(ObjectType.TYPE_RUNTIME_EXCEPTION, ot)
                 || typeMaker.isRawTypeAssignable(typeMaker.makeFromInternalTypeName("java/lang/Error"), ot));
    }

    @Override
    public void visit(Statements list) {
        super.visit(list);

        ListIterator<Statement> iterator = list.listIterator();

        while (iterator.hasNext()) {
            Statement statement = iterator.next();

            if (visitingLambda && statement instanceof LocalVariableDeclarationStatement declaration && typeVariableErasedTo(declaration) instanceof GenericType variable
                    && !isReassignedAfter(list, declaration)) {
                LocalVariableDeclarationStatement redeclared = new LocalVariableDeclarationStatement(variable, declaration.getLocalVariableDeclarators());

                redeclared.setFinal(declaration.isFinal());
                iterator.set(redeclared);
            }
        }
    }

    /** A variable which is assigned again keeps the type it was declared with: what it is assigned may not be the type variable */
    private static boolean isReassignedAfter(Statements list, LocalVariableDeclarationStatement declaration) {
        if (!(declaration.getLocalVariableDeclarators() instanceof LocalVariableDeclarator declarator)) {
            return true;
        }
        boolean[] reassigned = new boolean[1];
        AbstractJavaSyntaxVisitor finder = new AbstractJavaSyntaxVisitor() {
            @Override
            public void visit(BinaryOperatorExpression expression) {
                if (expression.getOperator().endsWith("=") && !"==".equals(expression.getOperator()) && !"!=".equals(expression.getOperator())
                        && !"<=".equals(expression.getOperator()) && !">=".equals(expression.getOperator())
                        && expression.getLeftExpression().isLocalVariableReferenceExpression() && declarator.getName().equals(expression.getLeftExpression().getName())) {
                    reassigned[0] = true;
                }
                super.visit(expression);
            }
        };

        list.subList(list.indexOf(declaration) + 1, list.size()).forEach(statement -> statement.accept(finder));
        return reassigned[0];
    }

    /**
     * A lambda's 'K key = keyFunction.apply(input)': the bytecode casts to the erasure of K (an Enum) and the variable gets that type.
     *
     * @return the type variable the initializer is, when the declared type is its erasure
     */
    private Type typeVariableErasedTo(LocalVariableDeclarationStatement declaration) {
        if (!(declaration.getType() instanceof ObjectType declared) || declared.getTypeArguments() != null || declared.getDimension() != 0
                || !(declaration.getLocalVariableDeclarators() instanceof LocalVariableDeclarator declarator) || declarator.getDimension() != 0
                || !(declarator.getVariableInitializer() instanceof ExpressionVariableInitializer initializer)
                || !(initializer.getExpression().getType() instanceof GenericType variable) || variable.getDimension() != 0 || !hasKnownTypeParameters(variable)) {
            return null;
        }
        BaseType bounds = declaredBoundsOf(variable.getName());

        // with several bounds ('T extends Number & Runnable') the variable is also each of the others, which an overloaded method can select
        return bounds != null && !bounds.isList() && bounds.getFirst() instanceof ObjectType firstBound && firstBound.getInternalName().equals(declared.getInternalName()) ? variable : null;
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
            boolean ctgil = castToGenericInLambda;
            // The declared type of a local variable is known even inside a lambda body
            castToGenericInLambda = visitingLambda && type != null && type.isGenericType();
            try {
                declaration.setExpression(updateExpression(Collections.emptyMap(), typeBounds, type, null, expression, false, true, false));
            } finally {
                castToGenericInLambda = ctgil;
            }
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
            expression.setParameters(updateParameters(Collections.emptyMap(), typeBounds, parameterTypes, null, parameters, new CastFlags(forceCast, unique, rawCast)));
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
            expression.setParameters(updateParameters(Collections.emptyMap(), typeBounds, parameterTypes, null, parameters, new CastFlags(forceCast, unique, rawCast)));
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

        if (!Utils.isEmpty(parameters)) {
            BaseType parameterTypes = ((ClassFileMethodInvocationExpression)expression).getParameterTypes();
            BaseType unboundParameterTypes = ((ClassFileMethodInvocationExpression)expression).getUnboundParameterTypes();
            if (!expression.isVarArgs()) {
                Set<String> methodTypeParameterNames = getMethodTypeParameterNames((ClassFileMethodInvocationExpression) expression);
                addImplicitObjectBounds(localTypeBounds, unboundParameterTypes, methodTypeParameterNames);
            }
            boolean unique = typeMaker.matchCount(expression.getInternalTypeName(), expression.getName(), parameters.size(), false) <= 1;
            boolean forceCast = !unique && typeMaker.matchCount(typeBindings, localTypeBounds, expression.getInternalTypeName(), expression.getName(), parameters, false) > 1;
            boolean rawCast = false;
            boolean oldVisitingWitnessedInvocation = visitingWitnessedInvocation;
            // The printer only renders <TypeArgs> when a receiver qualifier is also printed: an implicit
            // 'this' call (or one without receiver, such as an inherited static method) never shows the witness,
            // so it does not actually disambiguate anything there.
            visitingWitnessedInvocation = expression.getNonWildcardTypeArguments() != null
                && hasKnownTypeParameters(expression.getNonWildcardTypeArguments())
                && !expression.getExpression().isThisExpression()
                && !(expression.getExpression() instanceof NoExpression);
            Set<String> oldTypeVariablesSharedAcrossParameters = typeVariablesSharedAcrossParameters;
            typeVariablesSharedAcrossParameters = findTypeVariablesSharedAcrossParameters(unboundParameterTypes);
            boolean oldVisitingVarArgs = visitingVarArgsInvocation;
            Set<String> oldTypeVariablesOfTheCall = typeVariablesOfTheCall;
            visitingVarArgsInvocation = expression.isVarArgs();
            typeVariablesOfTheCall = getMethodTypeParameterNames((ClassFileMethodInvocationExpression) expression);
            expression.setParameters(updateParameters(typeBindings, localTypeBounds, parameterTypes, unboundParameterTypes, parameters, new CastFlags(forceCast, unique, rawCast)));
            typeVariablesOfTheCall = oldTypeVariablesOfTheCall;
            visitingVarArgsInvocation = oldVisitingVarArgs;
            visitingWitnessedInvocation = oldVisitingWitnessedInvocation;
            typeVariablesSharedAcrossParameters = oldTypeVariablesSharedAcrossParameters;
            writeTypeArgumentsOfInstancesSuppliedToTypeVariables((ClassFileMethodInvocationExpression) expression, expression.getParameters(), unboundParameterTypes);
            writeTypeArgumentsOfAccumulatorSuppliedToCollector((ClassFileMethodInvocationExpression) expression);
            castResultsOfLambdasToTheTypesTheCallBinds((ClassFileMethodInvocationExpression) expression, unboundParameterTypes);
            writeTypeArgumentsOfInstancesSuppliedToBoundedTypeVariables((ClassFileMethodInvocationExpression) expression, expression.getParameters(), unboundParameterTypes);
        }

        castReceiverWithUnresolvedBoundedTypeVariablesToRaw(expression);
        writeTypeArgumentOfInstanceWhoseMethodThrowsIt(expression);
        writeTypeArgumentsOfReceiverBoundToWildcardType(expression);
        writeParameterTypesOfLambdasWithoutTarget(expression);
        writeParameterTypesOfLambdasWhoseTypesAreLost(expression);
        castArgumentsMentioningCapturedTypeVariablesToRaw(expression);
        castArgumentsCapturedFromWildcardReceiversToRaw(expression);
        castCapturesToTheTypeVariableOfTheParameter(expression);
        parameterizeCheckCastsPassedWithLambdas(expression);

        if (expression.getNonWildcardTypeArguments() != null) {
            if (hasKnownTypeParameters(expression.getNonWildcardTypeArguments())) {
                safeAccept(expression.getNonWildcardTypeArguments());
            } else {
                expression.setNonWildcardTypeArguments(null);
            }
        }

        if (expression.getExpression() instanceof CastExpression ce && ce.isByteCodeCheckCast()
                && ce.getExpression() instanceof ClassFileMethodInvocationExpression mie && ce.getType() instanceof ObjectType ot) {
            if (isCastToBeRemoved(typeBindings, localTypeBounds, ot, ce, true) || isCastToTypeArgumentOfReceiver(ot, mie)) {
                // Remove cast
                expression.setExpression(ce.getExpression());
            }
            TypeTypes typeTypes = typeMaker.makeTypeTypes(ot.getInternalName());
            if (typeTypes != null && typeTypes.getTypeParameters() != null && ot.getTypeArguments() == null
                    && mie.getUnboundType() instanceof ObjectType) {
                TypeArguments typeArguments = new TypeArguments();
                for (int i = 0; i < typeTypes.getTypeParameters().size(); i++) {
                    typeArguments.add(WildcardTypeArgument.WILDCARD_TYPE_ARGUMENT);
                }
                ce.setType(ot.createType(typeArguments));
            }
        }

        Expression receiver = expression.getExpression();

        if (receiver instanceof CastExpression castReceiver
                && castReceiver.isByteCodeCheckCast()
                && castReceiver.getType() instanceof ObjectType receiverType
                && hasWildcardTypeArgument(receiverType)
                && !containsFunctionalExpression(parameters)) {
            // Calling a generic method through an inferred wildcard implementation cast creates fresh
            // captures that cannot accept the original method argument. The bytecode cast is raw here.
            castReceiver.setType(receiverType.createType(null));
        }

        // Only receivers with a DECLARED type (fields, locals) qualify: on an inferred receiver such as
        // 'stream.map(...)' a modeled wildcard is usually an inference artifact, not a source-level wildcard,
        // and casting it would contradict the type javac infers.
        if (receiver != null && !receiver.isNullExpression()
                && (receiver.isLocalVariableReferenceExpression() || receiver.isFieldReferenceExpression())
                && receiver.getType() instanceof ObjectType receiverType
                && hasWildcardTypeArgument(receiverType)) {
            // The receiver's unbounded wildcard is capture-converted independently at every use, so passing an
            // argument typed for the class's own type variable (e.g. Progress<ProgressContext>.onProgress(...,
            // ProgressContext)) does not type-check without an explicit unchecked cast to a raw-parameterized type.
            ObjectType wildcardCaptureCastType = resolveWildcardCaptureCastType((ClassFileMethodInvocationExpression)expression, receiverType);

            if (wildcardCaptureCastType != null) {
                expression.setExpression(addCastExpression(wildcardCaptureCastType, receiver));
            }
        }

        if (receiver instanceof CastExpression castReceiverWithFunctionalUse
                && containsFunctionalExpression(parameters)) {
            castReceiverWithFunctionalUse.getExpression().accept(this);
        } else {
            Expression oldMemberReceiver = memberReceiver;
            memberReceiver = expression.getExpression();
            expression.getExpression().accept(this);
            memberReceiver = oldMemberReceiver;
        }
        removeEmptyVarArgsArray(expression);
    }

    /**
     * 'Holder&lt;U&gt; d = new Holder&lt;&gt;(list)' inside a Holder&lt;V&gt;: the constructor's V is the instance's U, not the enclosing
     * class's V.
     */
    private BaseType withTypeArgumentsOfTheInstance(ClassFileNewExpression expression, BaseType parameterTypes) {
        ObjectType instanceType = expression.getObjectType();

        if (parameterTypes == null || instanceType.getTypeArguments() == null || hasWildcardTypeArgument(instanceType) || expression.getDescriptor() == null) {
            return parameterTypes;
        }
        TypeMaker.MethodTypes declared = typeMaker.makeMethodTypes(instanceType.getInternalName(), StringConstants.INSTANCE_CONSTRUCTOR, expression.getDescriptor());
        Map<String, TypeArgument> bindings = typeVariablesGiven(instanceType.getInternalName(), toTypeArgumentList(instanceType.getTypeArguments()));

        if (declared == null || declared.getParameterTypes() == null || bindings.isEmpty() || declared.getParameterTypes().size() != parameterTypes.size()) {
            return parameterTypes;
        }
        List<Type> declaredTypes = new ArrayList<>();
        List<Type> currentTypes = new ArrayList<>();
        Types substituted = new Types();

        declared.getParameterTypes().forEach(declaredTypes::add);
        parameterTypes.forEach(currentTypes::add);
        IntStream.range(0, currentTypes.size()).forEach(i -> substituted.add(
                declaredTypes.get(i) instanceof ObjectType && substitute(declaredTypes.get(i), bindings) instanceof Type substitutedType ? substitutedType : currentTypes.get(i)));
        return parameterTypes.isList() ? substituted : substituted.getFirst();
    }

    /** An overload of the same arity which cannot take the arguments (choose(int) for choose(Callable, Object...)) does not matter */
    private boolean hasApplicableOverloadWithoutTheLastParameter(MethodInvocationExpression expression, BaseExpression parameters) {
        if (typeMaker.matchCount(expression.getInternalTypeName(), expression.getName(), parameters.size() - 1, false) == 0) {
            return false;
        }
        Expressions remaining = new Expressions();

        remaining.addAll(parameters.getList().subList(0, parameters.size() - 1));
        return typeMaker.matchCount(getLocalTypeBindings(expression), getLocalTypeBounds(expression), expression.getInternalTypeName(), expression.getName(), remaining, false) > 0;
    }

    /**
     * javac passes an empty array for a varargs method called without any: the call is spelled without it. The printer
     * hides that array too, but loses track of the comma after the block body of a lambda.
     */
    private void removeEmptyVarArgsArray(MethodInvocationExpression expression) {
        BaseExpression parameters = expression.getParameters();

        if (expression.isVarArgs() && parameters != null && parameters.isList() && parameters.size() > 1
                && !hasApplicableOverloadWithoutTheLastParameter(expression, parameters)
                && parameters.getList().get(parameters.size() - 2) instanceof LambdaIdentifiersExpression lambda
                && lambda.getStatements() != null && !lambda.getStatements().isLambdaExpressionStatement()
                && parameters.getLast() instanceof NewArray newArray && newArray.isEmptyNewArray()
                && expression instanceof ClassFileMethodInvocationExpression invocation && invocation.getParameterTypes() != null
                && invocation.getParameterTypes().isList()) {
            parameters.getList().removeLast();
            invocation.getParameterTypes().getList().removeLast();
        }
    }

    private static boolean hasWildcardTypeArgument(ObjectType receiverType) {
        BaseTypeArgument typeArguments = receiverType.getTypeArguments();

        if (isCaptureWildcard(typeArguments)) {
            return true;
        }
        if (typeArguments instanceof TypeArguments list) {
            for (TypeArgument typeArgument : list) {
                if (isCaptureWildcard(typeArgument)) {
                    return true;
                }
            }
        }
        return false;
    }

    /**
     * Wildcards whose capture blocks passing type-variable-typed arguments: unbounded and extends-bounded.
     * A super-bounded wildcard deliberately accepts such arguments (PECS), so it never needs the cast.
     */
    private static boolean isCaptureWildcard(BaseTypeArgument typeArgument) {
        return typeArgument instanceof WildcardTypeArgument
            || typeArgument instanceof WildcardExtendsTypeArgument;
    }

    private ObjectType resolveWildcardCaptureCastType(ClassFileMethodInvocationExpression expression, ObjectType receiverType) {
        BaseExpression parameters = expression.getParameters();

        if (Utils.isEmpty(parameters)) {
            return null;
        }

        BaseType unboundParameterTypes = expression.getUnboundParameterTypes();

        if (unboundParameterTypes == null) {
            // a call within the class has no bound parameter types: use the declared ones
            TypeMaker.MethodTypes methodTypes = typeMaker.makeMethodTypes(expression.getInternalTypeName(), expression.getName(), expression.getDescriptor());

            unboundParameterTypes = methodTypes == null ? null : methodTypes.getParameterTypes();
        }
        if (unboundParameterTypes == null) {
            return null;
        }

        TypeTypes typeTypes = typeMaker.makeTypeTypes(expression.getInternalTypeName());

        if (typeTypes == null || typeTypes.getTypeParameters() == null) {
            return null;
        }

        // A null value means the type variable has no single denotable upper bound (e.g. multiple bounds):
        // no valid type argument exists for it, so the cast must fall back to the raw receiver type.
        Map<String, Type> classTypeParameterBounds = new HashMap<>();

        for (org.jd.core.v1.model.javasyntax.type.TypeParameter typeParameter : typeTypes.getTypeParameters()) {
            classTypeParameterBounds.put(typeParameter.getIdentifier(), boundOf(typeParameter));
        }

        // Only type variables whose OWN receiver argument is a capture wildcard block argument-passing:
        // in Erase.apply(IOFunction<? super T, ? extends R> mapper, T t) the call mapper.apply(t) is legal
        // as-is, because 't' lands in the '? super T' position; '? extends R' only captures the return side.
        Set<String> capturedTypeParameterNames = capturedTypeParameterNames(receiverType, typeTypes);

        if (capturedTypeParameterNames.isEmpty()) {
            return null;
        }

        var unboundTypeIterator = unboundParameterTypes.iterator();
        var parameterIterator = parameters.iterator();

        while (unboundTypeIterator.hasNext() && parameterIterator.hasNext()) {
            Type unboundType = unboundTypeIterator.next();
            Expression argument = parameterIterator.next();

            if (!argument.isNullExpression() && referencesClassTypeParameter(unboundType, capturedTypeParameterNames)) {
                return buildCaptureCastType(receiverType, typeTypes, classTypeParameterBounds);
            }
        }

        return null;
    }

    /** The type variables whose receiver type argument is an unbounded or extends wildcard, matched by position. */
    private static Set<String> capturedTypeParameterNames(ObjectType receiverType, TypeTypes typeTypes) {
        BaseTypeArgument receiverArguments = receiverType.getTypeArguments();
        Set<String> names = new HashSet<>();

        if (isCaptureWildcard(receiverArguments) && typeTypes.getTypeParameters().size() == 1) {
            names.add(typeTypes.getTypeParameters().getFirst().getIdentifier());
        } else if (receiverArguments instanceof TypeArguments receiverArgumentList
                && typeTypes.getTypeParameters().size() == receiverArgumentList.size()) {
            var typeParameterIterator = typeTypes.getTypeParameters().iterator();

            for (TypeArgument receiverArgument : receiverArgumentList) {
                org.jd.core.v1.model.javasyntax.type.TypeParameter typeParameter = typeParameterIterator.next();

                if (isCaptureWildcard(receiverArgument)) {
                    names.add(typeParameter.getIdentifier());
                }
            }
        }

        return names;
    }

    /**
     * Whether the formal parameter's unbound type uses one of the class's type variables, directly
     * ({@code T}) or nested ({@code List<T>}, {@code List<? extends T>}): every such use fails to
     * type-check against a capture-converted receiver.
     */
    private static boolean referencesClassTypeParameter(BaseTypeArgument typeArgument, Set<String> classTypeParameterNames) {
        if (typeArgument instanceof GenericType gt) {
            return classTypeParameterNames.contains(gt.getName());
        }
        if (typeArgument instanceof WildcardExtendsTypeArgument wildcard) {
            return referencesClassTypeParameter(wildcard.type(), classTypeParameterNames);
        }
        // '? super T' positions are contravariant: 'forEach(Consumer<? super T>)' accepts a Consumer of any
        // supertype of the capture, so such a use never needs the receiver cast — do not descend into it.
        if (typeArgument instanceof TypeArguments list) {
            for (TypeArgument nested : list) {
                if (referencesClassTypeParameter(nested, classTypeParameterNames)) {
                    return true;
                }
            }
            return false;
        }
        if (typeArgument instanceof ObjectType objectType && objectType.getTypeArguments() != null) {
            return referencesClassTypeParameter(objectType.getTypeArguments(), classTypeParameterNames);
        }
        return false;
    }

    /**
     * The parameterization to cast the wildcard receiver to: every wildcard argument is replaced with its type
     * variable's upper bound, non-wildcard arguments are kept. Falls back to the raw type as soon as one
     * replacement has no denotable bound.
     */
    private static ObjectType buildCaptureCastType(ObjectType receiverType, TypeTypes typeTypes, Map<String, Type> classTypeParameterBounds) {
        BaseTypeArgument receiverArguments = receiverType.getTypeArguments();

        if (isCaptureWildcard(receiverArguments)) {
            if (StreamSupport.stream(typeTypes.getTypeParameters().spliterator(), false).anyMatch(AddCastExpressionVisitor::hasSeveralBounds)) {
                return receiverType.createType((BaseTypeArgument)null);
            }
            Type bound = classTypeParameterBounds.values().size() == 1 ? classTypeParameterBounds.values().iterator().next() : null;
            Type replacement = wildcardReplacement((TypeArgument)receiverArguments, bound);
            return replacement == null ? receiverType.createType((BaseTypeArgument)null) : receiverType.createType(replacement);
        }

        if (receiverArguments instanceof TypeArguments receiverArgumentList
                && typeTypes.getTypeParameters().size() == receiverArgumentList.size()) {
            TypeArguments newArguments = new TypeArguments(receiverArgumentList.size());
            var typeParameterIterator = typeTypes.getTypeParameters().iterator();

            for (TypeArgument receiverArgument : receiverArgumentList) {
                org.jd.core.v1.model.javasyntax.type.TypeParameter typeParameter = typeParameterIterator.next();

                if (isCaptureWildcard(receiverArgument)) {
                    Type replacement = hasSeveralBounds(typeParameter) ? null : wildcardReplacement(receiverArgument, classTypeParameterBounds.get(typeParameter.getIdentifier()));
                    if (replacement == null) {
                        return receiverType.createType((BaseTypeArgument)null);
                    }
                    newArguments.add(replacement);
                } else {
                    // Non-wildcard and super-wildcard arguments stay as declared: both remain valid
                    // in a cast target and keep their positions type-checking as before
                    newArguments.add(receiverArgument);
                }
            }
            return receiverType.createType(newArguments);
        }

        return receiverType.createType((BaseTypeArgument)null);
    }

    /** 'T extends Number & Runnable': the bound of the wildcard alone does not satisfy the bounds of the variable */
    private static boolean hasSeveralBounds(org.jd.core.v1.model.javasyntax.type.TypeParameter typeParameter) {
        return typeParameter instanceof TypeParameterWithTypeBounds withBounds && withBounds.getTypeBounds().size() > 1;
    }

    /**
     * The type argument standing in for a wildcard in the capture cast: an extends-wildcard keeps its own
     * bound ({@code Box<? extends Number>} casts to {@code Box<Number>}, an unchecked but legal narrowing),
     * the other kinds use the type variable's upper bound. {@code null} means nothing denotable: go raw.
     * A parameterized bound is not denotable either: stripping it raw does not survive the bounds check
     * (a raw type argument never satisfies an F-bound such as {@code T extends FrameworkMember<T>}).
     */
    private static Type wildcardReplacement(TypeArgument wildcard, Type typeParameterBound) {
        if (wildcard instanceof WildcardExtendsTypeArgument extendsWildcard) {
            return extendsWildcard.type() instanceof ObjectType objectType && objectType.getTypeArguments() == null ? objectType : null;
        }
        return typeParameterBound;
    }

    /**
     * The type variable's upper bound when it is denotable as a type argument, or {@code null} otherwise:
     * multiple bounds ({@code T extends Number & Comparable<T>}), type-variable bounds ({@code T extends U})
     * and parameterized bounds ({@code T extends FrameworkMember<T>}) all have no valid parameterization —
     * a raw type argument never satisfies an F-bound, so those cases must cast the receiver to the raw type.
     */
    private static Type boundOf(org.jd.core.v1.model.javasyntax.type.TypeParameter typeParameter) {
        if (!(typeParameter instanceof TypeParameterWithTypeBounds typeParameterWithTypeBounds)) {
            return ObjectType.TYPE_OBJECT;
        }
        if (typeParameterWithTypeBounds.getTypeBounds().size() > 1
                || !(typeParameterWithTypeBounds.getTypeBounds().getFirst() instanceof ObjectType objectType)
                || objectType.getTypeArguments() != null) {
            return null;
        }
        return objectType;
    }

    @Override
    public void visit(NewExpression expression) {
        BaseExpression parameters = expression.getParameters();

        if (parameters != null) {
            updateNewExpressionParameters((ClassFileNewExpression) expression, parameters);
        }

        if (expression.getBodyDeclaration() != null && expression.getBodyDeclaration().isAnonymous()) {
            visitingAnonymousClass = true;
            visitBodyDeclaration(expression.getBodyDeclaration());
            visitingAnonymousClass = false;
            if (expression.getBodyDeclaration() instanceof ClassFileBodyDeclaration anonymousBody && declaresNewMethod(anonymousBody, expression.getObjectType())) {
                // With a diamond, every method of an anonymous class must override one of its supertype
                expression.setDiamondPossible(false);
            }
        }

        if ((visitingLambda || containsFunctionalExpression(parameters))
                && expression.getObjectType().getTypeArguments() == null
                && expression.isDiamondPossible()) {
            prepareDiamondTypeArgumentsIfPossible(expression);
        }

        if (expression.isDiamondPossible() && expression.getObjectType().getTypeArguments() != null
                && isGenericCallToWildcardParameter((ClassFileNewExpression) expression, parameters)) {
            // 'new Foo<>(copyOf(array))': the type arguments cannot be inferred from the target and from a generic call at once
            expression.setDiamondPossible(false);
        }

        if (!hasKnownTypeParameters(expression.getObjectType())) {
            expression.setType(expression.getObjectType().createType(ObjectType.TYPE_UNDEFINED_OBJECT));
        } else if (expression.getBodyDeclaration() == null && violatesTypeParameterBounds(expression.getObjectType())) {
            // 'new Foo<>()' cannot infer type arguments out of the bounds of Foo's type parameters (e.g. for a type
            // variable of the caller which is unbounded): the raw type is then assigned with an unchecked conversion
            expression.setType(expression.getObjectType().createType(null));
        }
    }

    private boolean isGenericCallToWildcardParameter(ClassFileNewExpression expression, BaseExpression parameters) {
        if (Utils.isEmpty(parameters) || !(parameters.getFirst() instanceof ClassFileMethodInvocationExpression call)
                || parameters.size() > 1 && !parameters.getLast().isNullExpression()
                || call.getTypeParameters() == null || expression.getDescriptor() == null) {
            return false;
        }
        // (the access constructor of a private constructor has one more parameter: the generic types are those of the constructor)
        String descriptor = expression.getDescriptor().replaceFirst("L[^;()]*\\$\\d+;\\)", ")");
        TypeMaker.MethodTypes constructor = typeMaker.makeMethodTypes(expression.getObjectType().getInternalName(), "<init>", descriptor);

        return constructor != null && constructor.getParameterTypes() != null
            && constructor.getParameterTypes().getFirst() instanceof ObjectType parameterType && hasWildcardSuperTypeArgument(parameterType);
    }

    private static boolean hasWildcardSuperTypeArgument(ObjectType type) {
        BaseTypeArgument arguments = type.getTypeArguments();

        if (arguments == null) {
            return false;
        }
        for (TypeArgument argument : toTypeArgumentList(arguments)) {
            TypeArgument unwrapped = argument instanceof WildcardExtendsTypeArgument extending ? extending.type() : argument;

            if (unwrapped instanceof WildcardSuperTypeArgument || unwrapped instanceof ObjectType nested && hasWildcardSuperTypeArgument(nested)) {
                return true;
            }
        }
        return false;
    }

    private boolean violatesTypeParameterBounds(ObjectType objectType) {
        if (objectType.getTypeArguments() == null) {
            return false;
        }
        TypeTypes typeTypes = typeMaker.makeTypeTypes(objectType.getInternalName());
        if (typeTypes == null || typeTypes.getTypeParameters() == null) {
            return false;
        }
        List<TypeArgument> typeArguments = toTypeArgumentList(objectType.getTypeArguments());
        int index = 0;
        for (org.jd.core.v1.model.javasyntax.type.TypeParameter typeParameter : typeTypes.getTypeParameters()) {
            if (index >= typeArguments.size()) {
                break;
            }
            TypeArgument typeArgument = typeArguments.get(index++);
            if (typeParameter instanceof TypeParameterWithTypeBounds parameterWithBounds && typeArgument instanceof GenericType genericType
                    && genericType.getDimension() == 0 && !satisfiesBounds(genericType, parameterWithBounds.getTypeBounds())) {
                return true;
            }
        }
        return false;
    }

    /**
     * @return the bounds of the innermost type parameter of this name (null if it has none), as the map of bounds is ambiguous when names are
     * reused
     */
    private BaseType declaredBoundsOf(String typeVariableName) {
        for (TypeParameter scope : typeParameters) {
            for (org.jd.core.v1.model.javasyntax.type.TypeParameter typeParameter : scope.type()) {
                if (typeParameter.getIdentifier().equals(typeVariableName)) {
                    return typeParameter instanceof TypeParameterWithTypeBounds withBounds ? withBounds.getTypeBounds() : null;
                }
            }
        }
        return typeBounds.get(typeVariableName);
    }

    /** @return false only if the type variable is known not to be within the (class) bounds of a type parameter */
    private boolean satisfiesBounds(GenericType typeVariable, BaseType requiredBounds) {
        BaseType declaredBounds = declaredBoundsOf(typeVariable.getName());
        for (Type requiredBound : requiredBounds) {
            if (!(requiredBound instanceof ObjectType requiredObjectBound) || isJavaLangObject(requiredBound)) {
                continue;
            }
            if (declaredBounds == null) {
                return false;
            }
            boolean satisfied = false;
            for (Type declaredBound : declaredBounds) {
                // A bound which is itself a type variable is not resolved: the raw type is always valid, so do not trust it
                if (declaredBound instanceof ObjectType declaredObjectBound && typeMaker.isRawTypeAssignable(requiredObjectBound, declaredObjectBound)
                        && haveCompatibleTypeArguments(requiredObjectBound, declaredObjectBound)) {
                    satisfied = true;
                    break;
                }
            }
            if (!satisfied) {
                return false;
            }
        }
        return true;
    }

    /**
     * The bounds Comparable&lt;String&gt; and Comparable&lt;Integer&gt; have the same raw type, but nothing is a member of both: compare their
     * concrete type arguments
     */
    private static boolean haveCompatibleTypeArguments(ObjectType required, ObjectType declared) {
        BaseTypeArgument requiredArguments = required.getTypeArguments();
        BaseTypeArgument declaredArguments = declared.getTypeArguments();

        return requiredArguments == null || declaredArguments == null
                || !required.findTypeParametersInType().isEmpty() || !declared.findTypeParametersInType().isEmpty()
                || requiredArguments.equals(declaredArguments);
    }

    private void updateNewExpressionParameters(ClassFileNewExpression expression, BaseExpression parameters) {
        inferNewExpressionTypeArgument(expression, parameters);
        boolean unique = typeMaker.matchCount(expression.getObjectType().getInternalName(),
                StringConstants.INSTANCE_CONSTRUCTOR, parameters.size(), true) <= 1;
        boolean forceCast = !unique && typeMaker.matchCount(Collections.emptyMap(), typeBounds,
                expression.getObjectType().getInternalName(), StringConstants.INSTANCE_CONSTRUCTOR,
                parameters, true) > 1;
        ObjectType currentObjectType = expectedType instanceof ObjectType objectType ? objectType : null;
        copyContextualTypeArguments(expression, currentObjectType);
        boolean rawCast = requiresRawConstructorCast(expression, currentObjectType);
        if (rawCast && currentObjectType != null && isGenericClass(expression.getObjectType())
                && !hasUnboundedWildcardTypeArgument(currentObjectType.getTypeArguments())) {
            expression.setObjectType(expression.getObjectType().createType(currentObjectType.getTypeArguments()));
        }
        BaseType parameterTypes = expression.getParameterTypes();
        if (expression.getBodyDeclaration() != null && expression.getBodyDeclaration().isAnonymous()) {
            parameterTypes = restoreErasedConstructorParameterTypes(parameterTypes, expression.getObjectType());
        }
        parameterTypes = withTypeArgumentsOfTheInstance(expression, parameterTypes);
        boolean oldVisitingWitnessedInvocation = visitingWitnessedInvocation;
        visitingWitnessedInvocation = false;
        expression.setParameters(updateParameters(Collections.emptyMap(), typeBounds, parameterTypes, null,
                parameters, new CastFlags(forceCast, unique, rawCast)));
        visitingWitnessedInvocation = oldVisitingWitnessedInvocation;
    }

    /**
     * The fixed type arguments of the class which is instantiated may be disjoint from the bounds of a type variable of the
     * target ('final class C implements I<String>' is no I<T> for T extends Number): the cast then goes through the raw type.
     */
    private Expression castThroughRawTypeIfBounded(Type target, Expression expression) {
        if ((expression instanceof ClassFileNewExpression || expression instanceof FieldReferenceExpression || expression.isLocalVariableReferenceExpression())
                && target instanceof ObjectType targetType && targetType.getTypeArguments() != null
                && hasBoundedTypeVariable(targetType)) {
            Expression raw = addCastExpression(targetType.createType(null), expression);

            return new CastExpression(raw.getLineNumber(), target, raw);
        }
        return addCastExpression(target, expression);
    }

    private boolean hasBoundedTypeVariable(ObjectType type) {
        for (String name : type.findTypeParametersInType()) {
            BaseType bounds = declaredBoundsOf(name);

            if (bounds != null) {
                for (Type bound : bounds) {
                    if (!isJavaLangObject(bound)) {
                        return true;
                    }
                }
            }
        }
        return false;
    }

    private boolean isGenericClass(ObjectType objectType) {
        TypeTypes typeTypes = typeMaker.makeTypeTypes(objectType.getInternalName());

        return typeTypes != null && typeTypes.getTypeParameters() != null;
    }

    private static void copyContextualTypeArguments(ClassFileNewExpression expression,
            ObjectType currentObjectType) {
        if (currentObjectType != null && currentObjectType.rawEquals(expression.getObjectType())
                && currentObjectType.getTypeArguments() != null
                && !hasWildcardTypeArgument(currentObjectType)
                && !containsWildcardSuper(currentObjectType.getTypeArguments())
                && expression.getObjectType().getTypeArguments() == null) {
            expression.setObjectType(expression.getObjectType().createType(currentObjectType.getTypeArguments()));
        }
    }

    private boolean requiresRawConstructorCast(ClassFileNewExpression expression, ObjectType currentObjectType) {
        return currentObjectType != null && expression.getType() instanceof ObjectType
                && typeMaker.isRawTypeAssignable(currentObjectType, expression.getObjectType())
                && !typeMaker.isAssignable(typeBounds, currentObjectType, expression.getObjectType())
                && (expression.getObjectType().getTypeArguments() == null
                        || !hasKnownTypeParameters(expression.getObjectType().getTypeArguments())
                        || containsWildcardSuper(currentObjectType.getTypeArguments()));
    }

    private void inferNewExpressionTypeArgument(ClassFileNewExpression expression, BaseExpression parameters) {
        TypeTypes typeTypes = typeMaker.makeTypeTypes(expression.getObjectType().getInternalName());
        BaseTypeParameter classTypeParameters = typeTypes == null ? null : typeTypes.getTypeParameters();
        BaseType parameterTypes = expression.getParameterTypes();

        if (classTypeParameters == null || classTypeParameters.size() != 1 || parameterTypes == null
                || parameterTypes.size() != parameters.size()) {
            return;
        }
        org.jd.core.v1.model.javasyntax.type.TypeParameter typeParameter = classTypeParameters.getFirst();
        ObjectType parameterBound = firstBoundOf(typeParameter);
        if (parameterBound == null || hasKnownTypeArguments(expression.getObjectType(), parameterBound)) {
            return;
        }

        List<Type> parameterTypeList = parameterTypes.isList()
                ? parameterTypes.getList() : Collections.singletonList(parameterTypes.getFirst());
        List<Expression> parameterList = parameters.isList()
                ? parameters.getList() : Collections.singletonList(parameters.getFirst());
        List<Type> declaredParameterTypeList = declaredConstructorParameterTypes(expression, parameters.size());
        for (int index = 0; index < parameters.size(); index++) {
            // Only a parameter which is declared with the type variable of the class says what the type argument is
            if (!declaredParameterTypeList.isEmpty() && !isTypeVariable(declaredParameterTypeList.get(index), typeParameter)) {
                continue;
            }
            if (parameterType(parameterTypeList.get(index), parameterBound) instanceof ObjectType erasedParameterType
                    && parameterList.get(index).getType() instanceof GenericType genericArgumentType
                    && erasedParameterType.getDimension() == genericArgumentType.getDimension()
                    && hasKnownTypeParameters(genericArgumentType)) {
                // The dimensions belong to the constructor parameter occurrence: T[] erases to Bound[]
                // and an argument U[] therefore infers the class type argument U, not U[].
                expression.setObjectType(expression.getObjectType().createType(
                        new GenericType(genericArgumentType.getName(),
                                genericArgumentType.getDimension() - erasedParameterType.getDimension())));
                return;
            }
        }
    }

    private static ObjectType firstBoundOf(org.jd.core.v1.model.javasyntax.type.TypeParameter typeParameter) {
        return typeParameter instanceof TypeParameterWithTypeBounds withBounds && withBounds.getTypeBounds().getFirst() instanceof ObjectType bound ? bound : null;
    }

    /** @return true if the type has type arguments which are not just the bound of the type variable (what the erasure gives): they are kept */
    private static boolean hasKnownTypeArguments(ObjectType objectType, ObjectType bound) {
        BaseTypeArgument typeArguments = objectType.getTypeArguments();

        return typeArguments != null && !(typeArguments instanceof ObjectType argument && argument.getInternalName().equals(bound.getInternalName()));
    }

    private static boolean isTypeVariable(Type type, org.jd.core.v1.model.javasyntax.type.TypeParameter typeParameter) {
        return type instanceof GenericType declared && declared.getName().equals(typeParameter.getIdentifier());
    }

    /** @return the erased parameter type if it is the bound of the type variable, null otherwise */
    private static Type parameterType(Type erased, ObjectType bound) {
        return erased instanceof ObjectType erasedObjectType && erasedObjectType.getInternalName().equals(bound.getInternalName()) ? erased : null;
    }

    /** @return the parameter types the constructor is declared with (generic ones), empty if the class has no such constructor */
    private List<Type> declaredConstructorParameterTypes(ClassFileNewExpression expression, int parameterCount) {
        TypeMaker.MethodTypes constructor = typeMaker.makeMethodTypes(expression.getObjectType().getInternalName(),
                StringConstants.INSTANCE_CONSTRUCTOR, expression.getDescriptor());
        BaseType declared = constructor == null ? null : constructor.getParameterTypes();

        if (declared == null || declared.size() != parameterCount) {
            return Collections.emptyList();
        }
        return declared.isList() ? declared.getList() : Collections.singletonList(declared.getFirst());
    }

    private static boolean containsWildcardSuper(BaseTypeArgument typeArguments) {
        if (typeArguments == null) {
            return false;
        }
        for (TypeArgument typeArgument : toTypeArgumentList(typeArguments)) {
            if (typeArgument instanceof WildcardSuperTypeArgument) {
                return true;
            }
            if (typeArgument instanceof ObjectType objectType
                    && containsWildcardSuper(objectType.getTypeArguments())) {
                return true;
            }
        }
        return false;
    }

    private BaseType restoreErasedConstructorParameterTypes(BaseType parameterTypes, ObjectType objectType) {
        if (parameterTypes == null || objectType.getTypeArguments() == null) {
            return parameterTypes;
        }
        if (!parameterTypes.isList()) {
            return restoreErasedConstructorParameterType(parameterTypes.getFirst(), objectType);
        }
        Types restoredTypes = new Types(parameterTypes.size());
        for (Type parameterType : parameterTypes) {
            restoredTypes.add(restoreErasedConstructorParameterType(parameterType, objectType));
        }
        return restoredTypes;
    }

    private Type restoreErasedConstructorParameterType(Type parameterType, ObjectType objectType) {
        if (!(parameterType instanceof ObjectType erasedParameterType)) {
            return parameterType;
        }
        for (TypeArgument typeArgument : toTypeArgumentList(objectType.getTypeArguments())) {
            if (typeArgument instanceof GenericType genericType
                    && typeBounds.get(genericType.getName()) instanceof ObjectType boundType
                    && erasedParameterType.getInternalName().equals(boundType.getInternalName())) {
                return genericType.createType(erasedParameterType.getDimension());
            }
        }
        return parameterType;
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

    /** '((A) this).x' names the owner of a field which the classes between hide: the cast is not redundant */
    private static boolean isOwnerCastOfThis(Expression exp) {
        return exp instanceof CastExpression ownerCast && ownerCast.getExpression().isThisExpression() && !ownerCast.isByteCodeCheckCast();
    }

    /** @return the type of the cast when it takes the type variable of the owner of the field ('((Holder<T>) o).value' read as a T), else the type of the owner */
    private Type withTypeVariableOfTheOwnerAsTheCast(FieldReferenceExpression expression, Expression exp, Type localType) {
        if (exp instanceof CastExpression castExpression
                && castExpression.getType() instanceof ObjectType castType
                && castType.getTypeArguments() == null
                && expression.getType() instanceof GenericType fieldType
                && expectedType instanceof GenericType contextualType
                && fieldType.getName().equals(contextualType.getName())) {
            TypeTypes ownerTypes = typeMaker.makeTypeTypes(expression.getInternalTypeName());
            BaseTypeParameter ownerParameters = ownerTypes == null ? null : ownerTypes.getTypeParameters();
            if (ownerParameters != null && ownerParameters.size() == 1
                    && ownerParameters.getFirst().getIdentifier().equals(fieldType.getName())) {
                castExpression.setType(castType.createType(
                        new GenericType(contextualType.getName(), contextualType.getDimension() - fieldType.getDimension())));
                return castExpression.getType();
            }
        }
        return localType;
    }

    @Override
    public void visit(FieldReferenceExpression expression) {
        Expression exp = expression.getExpression();

        if (exp != null && !exp.isObjectTypeReferenceExpression()) {
            Type localType = typeMaker.makeFromInternalTypeName(expression.getInternalTypeName());

            localType = withTypeVariableOfTheOwnerAsTheCast(expression, exp, localType);

            // '((A) this).x' names the owner of a field which the classes between hide: the cast is not redundant
            if (localType.getName() != null && !isOwnerCastOfThis(exp)) {
                Expression oldMemberReceiver = memberReceiver;
                memberReceiver = exp;
                expression.setExpression(updateExpression(Collections.emptyMap(), typeBounds, localType, null, exp, false, true, false));
                memberReceiver = oldMemberReceiver;
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

                if (mie.getTypeParameters() != null && !hasBoundedTypeParameter(mie.getTypeParameters())) {
                    // Do not add cast expression if method contains type parameters
                    rightExpression.accept(this);
                    return;
                }
            }

            if (isFieldOfAnotherInstanceWithTypeVariables(expression.getLeftExpression())) {
                visitAssignmentToFieldOfAnotherInstance(expression);
                return;
            }

            // A call of a generic method with a bounded type variable gets what the bound infers, not what the target type does
            if (rightExpression instanceof ClassFileMethodInvocationExpression call && call.getTypeParameters() != null
                    && hasBoundedTypeParameter(call.getTypeParameters())
                    && expression.getLeftExpression().getType() instanceof ObjectType leftType && leftType.getTypeArguments() != null
                    && !leftType.equals(rightExpression.getType())) {
                rightExpression.accept(this);
                expression.setRightExpression(addCastExpression(leftType.createType(null), rightExpression));
                return;
            }

            expression.setRightExpression(updateExpression(Collections.emptyMap(), typeBounds, expression.getLeftExpression().getType(), null, rightExpression, false, true, false));
            return;
        }

        rightExpression.accept(this);
    }

    private void visitAssignmentToFieldOfAnotherInstance(BinaryOperatorExpression expression) {
        // The field type is declared with the type variables of its class, which the receiver binds differently
        Expression value = dropCastToDeclaredFieldType(expression);

        if (expression.getLeftExpression().getType() instanceof ObjectType fieldType) {
            if (value instanceof ClassFileMethodInvocationExpression call && call.getTypeParameters() != null) {
                // (nothing tells how the type variables of the call relate to those the receiver binds: the assignment is unchecked)
                value = addCastExpression(fieldType.createType(null), value);
                expression.setRightExpression(value);
            } else if (value.getType() instanceof ObjectType valueType && isWildcardWhereConcreteExpected(fieldType, valueType)) {
                value = addCastExpression(fieldType, value);
                expression.setRightExpression(value);
            }
        }
        value.accept(this);
    }

    /** The cast to the declared type, which the bytecode has for the erased result of a generic call, goes with the cast to the field type */
    private static Expression dropCastToDeclaredFieldType(BinaryOperatorExpression assignment) {
        Expression right = assignment.getRightExpression();

        if (right instanceof CastExpression cast && cast.getExpression() instanceof ClassFileMethodInvocationExpression call && call.getTypeParameters() != null
                && cast.getType() instanceof ObjectType castType
                && assignment.getLeftExpression().getType() instanceof ObjectType leftType && castType.rawEquals(leftType)) {
            right = cast.getExpression();
            assignment.setRightExpression(right);
        }
        return right;
    }

    @Override
    public void visit(TernaryOperatorExpression expression) {
        Type expressionType = contextualTernaryType(expression.getType());

        expression.getCondition().accept(this);
        normalizeTernaryBooleanConstants(expression, expression.getType());
        expression.setTrueExpression(updateExpression(Collections.emptyMap(), typeBounds, expressionType, null, expression.getTrueExpression(), false, true, false));
        expression.setFalseExpression(updateExpression(Collections.emptyMap(), typeBounds, expressionType, null, expression.getFalseExpression(), false, true, false));
    }

    /** A type variable with a bound (<C extends Comparable>) cannot be inferred from any target type */
    private static boolean hasBoundedTypeParameter(BaseTypeParameter typeParameters) {
        for (org.jd.core.v1.model.javasyntax.type.TypeParameter typeParameter : typeParameters) {
            if (typeParameter instanceof TypeParameterWithTypeBounds) {
                return true;
            }
        }
        return false;
    }

    /**
     * The result of 'entry.getValue()' was typed before the type arguments of the receiver were known: the bytecode
     * cast to the erased class is redundant when the receiver's type argument is that very class.
     */
    private boolean isCastToTypeArgumentOfReceiver(ObjectType castType, ClassFileMethodInvocationExpression call) {
        if (!(call.getExpression().getType() instanceof ObjectType receiverType)
                || !(receiverType.getTypeArguments() instanceof TypeArguments receiverArguments)) {
            return false;
        }
        TypeMaker.MethodTypes methodTypes = typeMaker.makeMethodTypes(call.getInternalTypeName(), call.getName(), call.getDescriptor());

        if (methodTypes == null || !(methodTypes.getReturnedType() instanceof GenericType returnedVariable) || returnedVariable.getDimension() != 0
                || (methodTypes.getTypeParameters() != null && identifiersOf(methodTypes.getTypeParameters()).contains(returnedVariable.getName()))) {
            // a type variable of the method, which may hide the one of the class, is inferred from the arguments, not from the receiver
            return false;
        }
        TypeTypes typeTypes = typeMaker.makeTypeTypes(receiverType.getInternalName());
        BaseTypeParameter declared = typeTypes == null ? null : typeTypes.getTypeParameters();

        if (declared == null || !declared.isList() || declared.size() != receiverArguments.size()) {
            return false;
        }
        for (int i = 0; i < receiverArguments.size(); i++) {
            if (declared.getList().get(i).getIdentifier().equals(returnedVariable.getName())) {
                // 'Function<T, ? extends Stream<...>>' returns a Stream; what a '? super Value' gives back is only an Object
                TypeArgument receiverArgument = receiverArguments.getList().get(i);

                return !(receiverArgument instanceof WildcardSuperTypeArgument) && upperBoundOrSelf(receiverArgument) instanceof ObjectType argument && argument.rawEquals(castType);
            }
        }
        return false;
    }

    /** 'Class&lt;? extends T[]&gt;': a cast to it is unchecked but valid, a raw one would erase the type the call returns */
    private boolean isWildcardExtendingTypeVariable(BaseTypeArgument typeArgument) {
        return typeArgument instanceof WildcardExtendsTypeArgument wildcard && wildcard.type() instanceof GenericType variable
                && hasKnownTypeParameters(variable);
    }

    /**
     * 'return INSTANCE' where INSTANCE is a 'Foo&lt;Object&gt;' and the method returns a 'Foo&lt;T&gt;' (or 'return this' of a class
     * implementing 'Predicate&lt;Object&gt;'): the source has an unchecked cast, which the bytecode does not
     */
    private boolean isUncheckedTypeVariableMismatch(ObjectType target, Expression expression, ObjectType expressionType) {
        if (!(expression instanceof FieldReferenceExpression || expression.isThisExpression() || expression.isLocalVariableReferenceExpression())
                || target.getTypeArguments() == null || target.getDimension() != 0 || expressionType.getDimension() != 0) {
            return false;
        }
        List<TypeArgument> viewArguments = typeArgumentsSeenAs(target.getInternalName(), expressionType, new HashSet<String>());
        List<TypeArgument> targetArguments = toTypeArgumentList(target.getTypeArguments());

        if (viewArguments.isEmpty()) {
            return false;
        }

        if (targetArguments.size() != viewArguments.size()) {
            return false;
        }
        for (int i = 0; i < targetArguments.size(); i++) {
            if (targetArguments.get(i) instanceof GenericType variable && variable.getDimension() == 0 && hasKnownTypeParameters(variable)
                    && viewArguments.get(i) instanceof ObjectType concrete && concrete.findTypeParametersInType().isEmpty()) {
                return true;
            }
        }
        return false;
    }

    private static List<ObjectType> supertypesOf(TypeTypes typeTypes) {
        List<ObjectType> supertypes = new ArrayList<>();

        if (typeTypes.getSuperType() != null) {
            supertypes.add(typeTypes.getSuperType());
        }
        if (typeTypes.getInterfaces() != null) {
            for (Type interfaceType : typeTypes.getInterfaces()) {
                if (interfaceType instanceof ObjectType interfaceObjectType) {
                    supertypes.add(interfaceObjectType);
                }
            }
        }
        return supertypes;
    }

    private static ObjectType substituteTypeVariables(ObjectType supertype, BaseTypeParameter typeParameters, List<TypeArgument> typeArguments) {
        if (typeParameters == null || supertype.getTypeArguments() == null) {
            return supertype;
        }
        Map<String, TypeArgument> bindings = new HashMap<>();
        int index = 0;

        for (org.jd.core.v1.model.javasyntax.type.TypeParameter typeParameter : typeParameters) {
            if (index < typeArguments.size()) {
                bindings.put(typeParameter.getIdentifier(), typeArguments.get(index));
            }
            index++;
        }
        TypeArguments substituted = new TypeArguments();

        toTypeArgumentList(supertype.getTypeArguments()).forEach(supertypeArgument -> substituted.add(substitute(supertypeArgument, bindings)));
        return supertype.createType(substituted);
    }

    /** The type variables are replaced at any depth of the argument (List&lt;X&gt;, ? extends X) */
    private static TypeArgument substitute(TypeArgument argument, Map<String, TypeArgument> bindings) {
        if (argument instanceof GenericType variable && variable.getDimension() == 0 && bindings.containsKey(variable.getName())) {
            return bindings.get(variable.getName());
        }
        if (argument instanceof WildcardExtendsTypeArgument extending && substitute(extending.type(), bindings) instanceof Type bound) {
            return new WildcardExtendsTypeArgument(bound);
        }
        if (argument instanceof WildcardSuperTypeArgument supering && substitute(supering.type(), bindings) instanceof Type bound) {
            return new WildcardSuperTypeArgument(bound);
        }
        if (argument instanceof ObjectType objectType && objectType.getTypeArguments() != null) {
            TypeArguments nested = new TypeArguments();

            toTypeArgumentList(objectType.getTypeArguments()).forEach(nestedArgument -> nested.add(substitute(nestedArgument, bindings)));
            // one type argument is not a list of one
            return objectType.createType(nested.size() == 1 ? nested.getFirst() : nested);
        }
        return argument;
    }

    /** @return the type arguments of the given type seen as the given (super) class or interface, empty if it is raw or unrelated */
    private List<TypeArgument> typeArgumentsSeenAs(String internalName, ObjectType type, Set<String> visited) {
        if (type.getInternalName().equals(internalName)) {
            return type.getTypeArguments() == null ? Collections.emptyList() : toTypeArgumentList(type.getTypeArguments());
        }
        TypeTypes typeTypes = visited.add(type.getInternalName()) ? typeMaker.makeTypeTypes(type.getInternalName()) : null;

        if (typeTypes == null) {
            return Collections.emptyList();
        }
        List<TypeArgument> typeArguments = type.getTypeArguments() == null ? Collections.emptyList() : toTypeArgumentList(type.getTypeArguments());

        for (ObjectType supertype : supertypesOf(typeTypes)) {
            List<TypeArgument> found = typeArgumentsSeenAs(internalName, substituteTypeVariables(supertype, typeTypes.getTypeParameters(), typeArguments), visited);

            if (!found.isEmpty()) {
                return found;
            }
        }
        return Collections.emptyList();
    }

    /**
     * @return true if the call returns a type parameterized by a bounded type variable of the called method
     * (&lt;C extends Comparable&gt; Ordering&lt;C&gt;) and the expected type is parameterized, at that position, by a type
     * variable which does not have this bound
     */
    private boolean hasBoundedTypeVariableTargetedByUnboundedVariable(ClassFileMethodInvocationExpression call, ObjectType expected) {
        TypeMaker.MethodTypes methodTypes = findMethodTypes(call.getInternalTypeName(), call.getName(), call.getDescriptor());

        if (methodTypes == null || methodTypes.getTypeParameters() == null || !(methodTypes.getReturnedType() instanceof ObjectType returned)
                || returned.getTypeArguments() == null) {
            return false;
        }
        List<TypeArgument> seen = typeArgumentsSeenAs(expected.getInternalName(), returned, new HashSet<String>());
        List<TypeArgument> expectedArguments = toTypeArgumentList(expected.getTypeArguments());

        return seen.size() == expectedArguments.size() && StreamSupport.stream(methodTypes.getTypeParameters().spliterator(), false)
                .anyMatch(typeParameter -> isTargetedByUnboundedVariable(call, typeParameter, seen, expectedArguments));
    }

    private boolean isTargetedByUnboundedVariable(ClassFileMethodInvocationExpression call, org.jd.core.v1.model.javasyntax.type.TypeParameter typeParameter,
            List<TypeArgument> seen, List<TypeArgument> expectedArguments) {
        if (!(typeParameter instanceof TypeParameterWithTypeBounds withBounds) || !(boundOfMethodTypeParameter(call, withBounds) instanceof ObjectType bound)
                || isJavaLangObject(bound)) {
            return false;
        }
        return IntStream.range(0, seen.size()).anyMatch(i -> seen.get(i) instanceof GenericType variable && variable.getName().equals(typeParameter.getIdentifier())
                && (isWithoutTheBound(expectedArguments.get(i), bound) || isBoundedByItselfAndNot(variable, bound, expectedArguments.get(i))));
    }

    /** The target is a type variable which does not have the bound ('Ordering&lt;E&gt; o = Ordering.natural()', E is no Comparable). */
    private boolean isWithoutTheBound(TypeArgument expected, ObjectType bound) {
        return upperBoundOrSelf(expected) instanceof GenericType target && target.getDimension() == 0 && hasKnownTypeParameters(target) && !variableHasBound(target, bound);
    }

    private boolean isBoundedByItselfAndNot(GenericType variable, ObjectType bound, TypeArgument expected) {
        return expected instanceof ObjectType target && isBoundedByItself(variable, bound) && !isItsOwnArgumentOf(bound, target);
    }

    /** 'E extends Enum&lt;E&gt;' */
    private static boolean isBoundedByItself(GenericType variable, ObjectType bound) {
        return bound.getTypeArguments() != null && toTypeArgumentList(bound.getTypeArguments()).stream().anyMatch(argument ->
                argument instanceof GenericType other && other.getName().equals(variable.getName()));
    }

    /** No 'Enum&lt;?&gt;' is the E of 'E extends Enum&lt;E&gt;': E would have to be 'Enum&lt;Enum&lt;?&gt;&gt;'. */
    private boolean isItsOwnArgumentOf(ObjectType bound, ObjectType target) {
        List<TypeArgument> seen = typeArgumentsSeenAs(bound.getInternalName(), target, new HashSet<String>());

        return !seen.isEmpty() && seen.stream().allMatch(argument -> argument.equals(target));
    }

    /**
     * First bound of a method type parameter; 'S extends T' resolves T, a type variable of the class which declares the method, through the type
     * arguments the receiver gives it ('Ordering&lt;Comparable&gt;.reverse()', or 'Impl implements Factory&lt;Number&gt;' for a method of Factory).
     */
    private Type boundOfMethodTypeParameter(ClassFileMethodInvocationExpression call, TypeParameterWithTypeBounds typeParameter) {
        Type bound = typeParameter.getTypeBounds().getFirst();

        if (!(bound instanceof GenericType variable) || variable.getDimension() != 0 || !(call.getExpression().getType() instanceof ObjectType receiverType)) {
            return bound;
        }
        TypeMaker.TypeTypes typeTypes = typeMaker.makeTypeTypes(call.getInternalTypeName());

        if (typeTypes == null || typeTypes.getTypeParameters() == null) {
            return bound;
        }
        List<TypeArgument> given = typeArgumentsSeenAs(call.getInternalTypeName(), receiverType, new HashSet<String>());
        int index = identifiersOf(typeTypes.getTypeParameters()).indexOf(variable.getName());

        return index >= 0 && index < given.size() && given.get(index) instanceof ObjectType argument ? argument : bound;
    }

    /**
     * 'Collector.of(() -> new Builder&lt;&gt;(c), ...)': the type variable of the supplier is only inferred from the supplier, and a
     * diamond infers nothing out of the other arguments: the type arguments of the instance are written.
     */
    private void writeTypeArgumentsOfInstancesSuppliedToTypeVariables(ClassFileMethodInvocationExpression call, BaseExpression parameters, BaseType unboundParameterTypes) {
        Set<String> methodTypeParameterNames = getMethodTypeParameterNames(call);

        if (methodTypeParameterNames.isEmpty() || unboundParameterTypes == null || parameters == null || parameters.size() != unboundParameterTypes.size()) {
            return;
        }
        Iterator<Expression> parameterIterator = parameters.iterator();

        for (Type unboundParameterType : unboundParameterTypes) {
            Expression parameter = parameterIterator.next();

            if (parameter instanceof LambdaIdentifiersExpression lambda && lambda.getStatements() instanceof LambdaExpressionStatement body
                    && body.getExpression() instanceof NewExpression newExpression && newExpression.getObjectType().getTypeArguments() != null
                    && !hasWildcardTypeArgument(newExpression.getObjectType()) && hasKnownTypeParameters(newExpression.getObjectType())
                    && unboundParameterType instanceof ObjectType functionalType && functionalType.getTypeArguments() != null
                    && toTypeArgumentList(functionalType.getTypeArguments()).stream().anyMatch(argument ->
                            upperBoundOrSelf(argument) instanceof GenericType variable && methodTypeParameterNames.contains(variable.getName()))) {
                newExpression.setDiamondPossible(false);
            }
        }
    }

    /**
     * 'Lists.transform((List&lt;V1&gt;) values, v -&gt; ...)': the checkcast is to the erased List and a raw argument types the lambda
     * parameter as Object; keep the operand's type arguments.
     */
    private void parameterizeCheckCastsPassedWithLambdas(MethodInvocationExpression expression) {
        BaseExpression parameters = expression.getParameters();

        if (Utils.isEmpty(parameters) || !containsFunctionalExpression(parameters)) {
            return;
        }
        for (Expression parameter : parameters) {
            if (parameter instanceof CastExpression cast && cast.isByteCodeCheckCast() && cast.getType() instanceof ObjectType castType) {
                ObjectType parameterized = withTheTypeArgumentsOf(castType, cast.getExpression().getType());

                if (parameterized != null) {
                    cast.setType(parameterized);
                }
            }
        }
    }

    /** @return the (raw) type with the type arguments its operand gives it; null if it has some already or the ones of the operand are not known */
    private ObjectType withTheTypeArgumentsOf(ObjectType castType, Type operand) {
        if (castType.getTypeArguments() != null || castType.getDimension() != 0 || !(operand instanceof ObjectType operandType) || operandType.getTypeArguments() == null
                || operandType.getDimension() != 0 || !hasKnownTypeParameters(operandType) || hasWildcardTypeArgument(operandType)) {
            return null;
        }
        TypeTypes castTypes = typeMaker.makeTypeTypes(castType.getInternalName());

        if (castTypes == null || castTypes.getTypeParameters() == null) {
            return null;
        }
        BaseTypeArgument arguments = argumentsBoundByUnifying(castTypes.getTypeParameters(), castType.createType(ownTypeArguments(castTypes.getTypeParameters())), operandType);

        return arguments == null ? null : castType.createType(arguments);
    }

    /** The type variable is inferred from an argument whose type has a capture ('first(box.lists())'): javac does it, a type argument written for it is rejected */
    private boolean hasAnArgumentCapturedFromAWildcardReceiver(ClassFileMethodInvocationExpression call) {
        return !Utils.isEmpty(call.getParameters()) && StreamSupport.stream(call.getParameters().spliterator(), false)
                .anyMatch(argument -> argument instanceof ClassFileMethodInvocationExpression argumentCall && producesCapturesOfWildcardReceiver(argumentCall));
    }

    /**
     * 'Collections.unmodifiableSet(this.delegate.entrySet())' with delegate a 'Multiset&lt;? extends E&gt;': the elements are
     * Entry&lt;capture&gt;, which a type variable fixed by the target (Entry&lt;E&gt;) rejects; the source casts the argument to a raw type.
     */
    private void castArgumentsCapturedFromWildcardReceiversToRaw(MethodInvocationExpression expression) {
        if (!(expression instanceof ClassFileMethodInvocationExpression call) || call.getTypeParameters() == null || Utils.isEmpty(call.getParameters())) {
            return;
        }
        if (expression == memberReceiver) {
            // The result is read ('first(box.lists()).size()'): a raw argument would make it an Object, and a type argument with a wildcard is rejected:
            // javac infers the capture itself
            if (hasAnArgumentCapturedFromAWildcardReceiver(call)) {
                call.setNonWildcardTypeArguments(null);
            }
            return;
        }
        if (StreamSupport.stream(call.getParameters().spliterator(), false).anyMatch(argument -> argument instanceof LambdaIdentifiersExpression || isProperMethodReference(argument))) {
            // a raw argument would give the parameters of an implicit lambda, or the ones a method reference has to accept, the type Object
            return;
        }
        Expressions updated = new Expressions();
        boolean changed = false;

        for (Expression argument : call.getParameters()) {
            if (argument instanceof ClassFileMethodInvocationExpression argumentCall && argumentCall.getType() instanceof ObjectType argumentType
                    && argumentType.getTypeArguments() != null && argumentType.getDimension() == 0 && producesCapturesOfWildcardReceiver(argumentCall)) {
                argument = addCastExpression(argumentType.createType(null), argument);
                changed = true;
            }
            updated.add(argument);
        }
        if (changed) {
            call.setParameters(updated.size() == 1 ? updated.getFirst() : updated);
        }
    }

    /**
     * 'successors((N) endpointPair.nodeU())' with endpointPair an 'EndpointPair&lt;?&gt;': the capture returned by nodeU() is no N, the source casts it.
     */
    private void castCapturesToTheTypeVariableOfTheParameter(MethodInvocationExpression expression) {
        if (!(expression instanceof ClassFileMethodInvocationExpression call) || Utils.isEmpty(call.getParameters()) || !(call.getExpression().getType() instanceof ObjectType receiverType)
                || receiverType.getTypeArguments() == null) {
            return;
        }
        // The class which declares the method with type variables for parameters: the receiver's class may only inherit it ('AbstractBaseGraph.successors(N)')
        String owner = declaringClassOfGenericParameters(call);
        TypeMaker.MethodTypes declared = owner == null ? null : typeMaker.makeMethodTypes(owner, call.getName(), call.getDescriptor());

        if (declared == null || declared.getParameterTypes() == null || declared.getParameterTypes().size() != call.getParameters().size()) {
            return;
        }
        // The type variables of that class, as the receiver gives them
        Map<String, TypeArgument> given = typeVariablesGiven(owner, typeArgumentsSeenAs(owner, receiverType, new HashSet<String>()));
        Expressions updated = new Expressions();
        Iterator<Type> declaredTypes = declared.getParameterTypes().iterator();
        boolean changed = false;

        for (Expression argument : call.getParameters()) {
            GenericType variable = capturedVariableToCastTo(declaredTypes.next(), argument, given);

            if (variable != null) {
                argument = addCastExpression(variable, argument);
                changed = true;
            }
            updated.add(argument);
        }
        if (changed) {
            call.setParameters(updated.size() == 1 ? updated.getFirst() : updated);
        }
    }

    /** @return the type variable the receiver gives to the declared type variable, if the argument is a capture of a wildcard; null otherwise */
    private GenericType capturedVariableToCastTo(Type declaredType, Expression argument, Map<String, TypeArgument> given) {
        if (declaredType instanceof GenericType parameter && parameter.getDimension() == 0 && given.get(parameter.getName()) instanceof GenericType variable
                && variable.getDimension() == 0 && hasKnownTypeParameters(variable)
                && argument instanceof ClassFileMethodInvocationExpression argumentCall && returnsCapturedTypeVariable(argumentCall)) {
            return variable;
        }
        return null;
    }

    /** @return the first class, from the one of the call up its supertypes, which declares the called method with a type variable for a parameter; null if none */
    private String declaringClassOfGenericParameters(ClassFileMethodInvocationExpression call) {
        Deque<String> owners = new ArrayDeque<>();
        Set<String> visited = new HashSet<>();

        owners.add(call.getInternalTypeName());
        while (!owners.isEmpty()) {
            String owner = owners.poll();

            if (!visited.add(owner)) {
                continue;
            }
            TypeMaker.MethodTypes methodTypes = typeMaker.makeMethodTypes(owner, call.getName(), call.getDescriptor());

            if (methodTypes != null && methodTypes.getParameterTypes() != null && StreamSupport.stream(methodTypes.getParameterTypes().spliterator(), false).anyMatch(GenericType.class::isInstance)) {
                return owner;
            }
            TypeTypes typeTypes = typeMaker.makeTypeTypes(owner);

            if (typeTypes != null) {
                supertypesOf(typeTypes).forEach(supertype -> owners.add(supertype.getInternalName()));
            }
        }
        return null;
    }

    /** 'endpointPair.nodeU()' with endpointPair an 'EndpointPair&lt;?&gt;': a call on a variable or field returning a type variable of the class the receiver gives a wildcard to. */
    private boolean returnsCapturedTypeVariable(ClassFileMethodInvocationExpression call) {
        Expression receiver = call.getExpression();

        if (!(receiver.isLocalVariableReferenceExpression() || receiver.isFieldReferenceExpression()) || !(receiver.getType() instanceof ObjectType receiverType)
                || receiverType.getTypeArguments() == null || !receiverType.getInternalName().equals(call.getInternalTypeName())) {
            return false;
        }
        TypeTypes receiverTypes = typeMaker.makeTypeTypes(receiverType.getInternalName());
        TypeMaker.MethodTypes methodTypes = typeMaker.makeMethodTypes(call.getInternalTypeName(), call.getName(), call.getDescriptor());

        return receiverTypes != null && receiverTypes.getTypeParameters() != null && methodTypes != null && methodTypes.getReturnedType() instanceof GenericType returned
                && returned.getDimension() == 0 && typeVariablesGivenAWildcard(receiverType, receiverTypes, AddCastExpressionVisitor::isWildcard).contains(returned.getName());
    }

    private boolean producesCapturesOfWildcardReceiver(ClassFileMethodInvocationExpression call) {
        Expression receiver = call.getExpression();

        if (!(receiver.isLocalVariableReferenceExpression() || receiver.isFieldReferenceExpression()) || !(receiver.getType() instanceof ObjectType receiverType)
                || receiverType.getTypeArguments() == null || !receiverType.getInternalName().equals(call.getInternalTypeName())) {
            return false;
        }
        TypeTypes receiverTypes = typeMaker.makeTypeTypes(receiverType.getInternalName());
        TypeMaker.MethodTypes methodTypes = typeMaker.makeMethodTypes(call.getInternalTypeName(), call.getName(), call.getDescriptor());

        if (receiverTypes == null || receiverTypes.getTypeParameters() == null || methodTypes == null || !(methodTypes.getReturnedType() instanceof ObjectType returned)
                || returned.getTypeArguments() == null) {
            return false;
        }
        Set<String> captured = typeVariablesGivenAWildcard(receiverType, receiverTypes, WildcardExtendsTypeArgument.class::isInstance);

        // a captured variable nested in the result's type argument (Set<Entry<E>>)
        return toTypeArgumentList(returned.getTypeArguments()).stream()
                .anyMatch(argument -> argument instanceof ObjectType nested && nested.getTypeArguments() != null && nested.findTypeParametersInType().stream().anyMatch(captured::contains));
    }

    /**
     * 'bound.getSupertype((Class) supertype)' with bound a 'TypeToken&lt;? super T&gt;': T is a capture, and a 'Class&lt;? super T&gt;'
     * argument is not a 'Class&lt;? super capture&gt;'; the source casts it to a raw type.
     */
    private void castArgumentsMentioningCapturedTypeVariablesToRaw(MethodInvocationExpression expression) {
        if (!(expression instanceof ClassFileMethodInvocationExpression call) || !(call.getExpression().getType() instanceof ObjectType receiverType)
                || call.getExpression().isThisExpression() || call.getExpression().isObjectTypeReferenceExpression() || receiverType.getTypeArguments() == null
                || !receiverType.getInternalName().equals(call.getInternalTypeName()) || Utils.isEmpty(call.getParameters())) {
            return;
        }
        TypeMaker.TypeTypes receiverTypes = typeMaker.makeTypeTypes(receiverType.getInternalName());
        TypeMaker.MethodTypes methodTypes = typeMaker.makeMethodTypes(call.getInternalTypeName(), call.getName(), call.getDescriptor());

        if (receiverTypes == null || receiverTypes.getTypeParameters() == null || methodTypes == null || methodTypes.getParameterTypes() == null
                || call.getParameters().size() != methodTypes.getParameterTypes().size()) {
            return;
        }
        Set<String> captured = typeVariablesGivenAWildcard(receiverType, receiverTypes, AddCastExpressionVisitor::isWildcard);

        if (captured.isEmpty()) {
            return;
        }
        Iterator<Type> declaredTypes = methodTypes.getParameterTypes().iterator();
        Expressions updated = new Expressions();
        boolean changed = false;

        for (Expression argument : call.getParameters()) {
            Type declared = declaredTypes.next();

            if (isToBeCastToRawBecauseOfCapture(declared, argument, captured)) {
                argument = castToRaw(argument, (ObjectType) declared);
                changed = true;
            }
            updated.add(argument);
        }
        if (changed) {
            call.setParameters(updated.size() == 1 ? updated.getFirst() : updated);
        }
    }

    private static boolean isToBeCastToRawBecauseOfCapture(Type declared, Expression argument, Set<String> captured) {
        boolean functional = argument instanceof LambdaIdentifiersExpression || argument instanceof ConstructorReferenceExpression
                || argument instanceof MethodReferenceExpression && !(argument instanceof MethodInvocationExpression);

        return declared instanceof ObjectType declaredObjectType && declaredObjectType.getTypeArguments() != null && declaredObjectType.getDimension() == 0
                && declaredObjectType.findTypeParametersInType().stream().anyMatch(captured::contains)
                && argument.getType() instanceof ObjectType argumentType && argumentType.getTypeArguments() != null && !argumentType.findTypeParametersInType().isEmpty()
                && !argument.isNullExpression() && !functional;
    }

    private Expression castToRaw(Expression argument, ObjectType declared) {
        if (argument instanceof CastExpression cast && cast.getType() instanceof ObjectType castType && castType.getTypeArguments() != null) {
            cast.setType(castType.createType(null));
            return argument;
        }
        return argument.isCastExpression() ? argument : addCastExpression(declared.createType(null), argument);
    }

    /**
     * 'Ordering.natural().onResultOf(params -&gt; ...).compound(...)': nothing infers F of 'onResultOf(Function&lt;F, ?&gt;)' (a receiver has
     * no target type), so the lambda writes its erased parameter types.
     */
    private void writeParameterTypesOfLambdasWithoutTarget(MethodInvocationExpression expression) {
        if (expression == memberReceiver) {
            writeParameterTypesOfLambdas(expression);
        }
        if (expression instanceof ClassFileMethodInvocationExpression outer && outer.getTypeParameters() != null && outer.getUnboundParameterTypes() != null
                && !Utils.isEmpty(outer.getParameters()) && outer.getParameters().size() == outer.getUnboundParameterTypes().size()) {
            // 'a.compound(Ordering.natural().onResultOf(p -> ...))': only the argument gives the Comparator<? super U>'s type variable
            Set<String> outerVariables = getMethodTypeParameterNames(outer);
            Iterator<Expression> argumentIterator = outer.getParameters().iterator();

            for (Type declared : outer.getUnboundParameterTypes()) {
                Expression argument = argumentIterator.next();

                if (argument instanceof ClassFileMethodInvocationExpression inner && declared instanceof ObjectType declaredType && declaredType.getTypeArguments() != null
                        && toTypeArgumentList(declaredType.getTypeArguments()).stream().allMatch(typeArgument -> upperBoundOrSelf(typeArgument) instanceof GenericType variable
                                && outerVariables.contains(variable.getName()))) {
                    writeParameterTypesOfLambdas(inner);
                }
            }
        }
    }

    private void writeParameterTypesOfLambdas(MethodInvocationExpression expression) {
        if (!(expression instanceof ClassFileMethodInvocationExpression call) || call.getTypeParameters() == null
                || call.getUnboundParameterTypes() == null || Utils.isEmpty(call.getParameters()) || call.getParameters().size() != call.getUnboundParameterTypes().size()) {
            return;
        }
        Set<String> methodVariables = getMethodTypeParameterNames(call);
        List<Type> declaredTypes = new ArrayList<>();
        List<Expression> arguments = new ArrayList<>();

        call.getUnboundParameterTypes().forEach(declaredTypes::add);
        call.getParameters().forEach(arguments::add);

        for (int i = 0; i < declaredTypes.size(); i++) {
            if (arguments.get(i) instanceof LambdaIdentifiersExpression lambda && hasTypesForAllItsParameters(lambda)
                    && declaredTypes.get(i) instanceof ObjectType functionalType && functionalType.getTypeArguments() != null
                    && isFreeInTheParameters(lambda, functionalType, methodVariables, typeVariablesOfTheOthers(declaredTypes, i))) {
                lambda.setExplicitParameterTypes(true);
            }
        }
    }

    private static boolean hasTypesForAllItsParameters(LambdaIdentifiersExpression lambda) {
        return lambda.getParameterNames() != null && !lambda.getParameterNames().isEmpty() && lambda.getParameterTypes() != null
                && lambda.getParameterTypes().size() == lambda.getParameterNames().size();
    }

    private static Set<String> typeVariablesOfTheOthers(List<Type> types, int except) {
        return IntStream.range(0, types.size()).filter(i -> i != except).boxed().flatMap(i -> types.get(i).findTypeParametersInType().stream()).collect(Collectors.toSet());
    }

    /** A type variable of the method which only the lambda decides: the first type arguments of a functional interface are its parameters (Function&lt;F, R&gt;, BiFunction&lt;A, B, R&gt;). */
    private static boolean isFreeInTheParameters(LambdaIdentifiersExpression lambda, ObjectType functionalType, Set<String> methodVariables, Set<String> others) {
        List<TypeArgument> functionalArguments = toTypeArgumentList(functionalType.getTypeArguments());

        return functionalArguments.subList(0, Math.min(lambda.getParameterNames().size(), functionalArguments.size())).stream()
                .anyMatch(argument -> upperBoundOrSelf(argument) instanceof GenericType variable && variable.getDimension() == 0 && methodVariables.contains(variable.getName())
                        && !others.contains(variable.getName()));
    }

    /**
     * 'flatMap(list.spliterator(), rowEntry -&gt; map(((Map) rowEntry.getValue()).entrySet().spliterator(), columnEntry -&gt; ...))': lambda bytecode only has erased types.
     * When a raw argument makes the call's type variables Object, or the lambda casts what it takes out of its parameter, an implicit lambda gets types its body does not fit: write the erased types.
     */
    private void writeParameterTypesOfLambdasWhoseTypesAreLost(MethodInvocationExpression expression) {
        if (!(expression instanceof ClassFileMethodInvocationExpression call) || call.getTypeParameters() == null || Utils.isEmpty(call.getParameters())) {
            return;
        }
        boolean rawArgument = false;

        for (Expression argument : call.getParameters()) {
            if (argument instanceof MethodInvocationExpression && argument.getType() instanceof ObjectType argumentType && argumentType.getTypeArguments() == null
                    && argumentType.getDimension() == 0) {
                TypeTypes argumentTypes = typeMaker.makeTypeTypes(argumentType.getInternalName());

                rawArgument |= argumentTypes != null && argumentTypes.getTypeParameters() != null;
            }
        }
        List<Type> declaredTypes = call.getUnboundParameterTypes() == null ? List.of() : new ArrayList<>(StreamSupport.stream(call.getUnboundParameterTypes().spliterator(), false).toList());
        int index = 0;

        for (Expression argument : call.getParameters()) {
            Type declared = index < declaredTypes.size() ? declaredTypes.get(index) : null;

            index++;
            if (argument instanceof LambdaIdentifiersExpression lambda && lambda.getParameterNames() != null && !lambda.getParameterNames().isEmpty()
                    && lambda.getParameterTypes() != null && lambda.getParameterTypes().size() == lambda.getParameterNames().size()
                    && lambda.getParameterTypes().stream().allMatch(this::isRawGenericType)
                    && !targetGivesTheParameterTypes(lambda, declared)
                    && (rawArgument || castsWhatItTakesOutOfItsParameters(lambda))) {
                lambda.setExplicitParameterTypes(true);
            }
        }
    }

    /** 'Function&lt;List&lt;String&gt;, R&gt;': the functional type has the parameterized type of the parameters, which erased types would contradict */
    private static boolean targetGivesTheParameterTypes(LambdaIdentifiersExpression lambda, Type declared) {
        if (!(declared instanceof ObjectType functionalType) || functionalType.getTypeArguments() == null) {
            return false;
        }
        List<TypeArgument> functionalArguments = toTypeArgumentList(functionalType.getTypeArguments());

        return functionalArguments.subList(0, Math.min(lambda.getParameterNames().size(), functionalArguments.size())).stream()
                .anyMatch(argument -> upperBoundOrSelf(argument) instanceof ObjectType parameterType && parameterType.getTypeArguments() != null);
    }

    /** A generic class without type arguments: what a lambda's bytecode knows of its parameter. */
    private boolean isRawGenericType(Type type) {
        if (!(type instanceof ObjectType objectType) || objectType.getTypeArguments() != null || objectType.getDimension() != 0) {
            return false;
        }
        TypeTypes typeTypes = typeMaker.makeTypeTypes(objectType.getInternalName());

        return typeTypes != null && typeTypes.getTypeParameters() != null;
    }

    private static boolean castsWhatItTakesOutOfItsParameters(LambdaIdentifiersExpression lambda) {
        if (lambda.getStatements() == null) {
            return false;
        }
        CastOfCallOnParameterFinder finder = new CastOfCallOnParameterFinder(lambda.getParameterNames());

        lambda.getStatements().accept(finder);
        return finder.found;
    }

    private static final class CastOfCallOnParameterFinder extends AbstractJavaSyntaxVisitor {
        private final List<String> parameterNames;
        private boolean found;

        private CastOfCallOnParameterFinder(List<String> parameterNames) {
            this.parameterNames = parameterNames;
        }

        @Override
        public void visit(CastExpression expression) {
            if (expression.isByteCodeCheckCast() && expression.getExpression() instanceof MethodInvocationExpression call && call.getExpression().isLocalVariableReferenceExpression()
                    && parameterNames.contains(call.getExpression().getName())) {
                found = true;
            }
            super.visit(expression);
        }
    }

    /**
     * 'flatMap(splitrs, splitr -&gt; (Spliterator&lt;T&gt;) splitr)': the function must return a Spliterator&lt;T&gt;, which a
     * Spliterator&lt;? extends T&gt; is not; the source casts it.
     */
    private void castResultsOfLambdasToTheTypesTheCallBinds(ClassFileMethodInvocationExpression call, BaseType unboundParameterTypes) {
        if (unboundParameterTypes == null || call.getTypeBindings() == null || Utils.isEmpty(call.getParameters()) || call.getParameters().size() != unboundParameterTypes.size()) {
            return;
        }
        Iterator<Expression> argumentIterator = call.getParameters().iterator();

        for (Type unboundParameterType : unboundParameterTypes) {
            Expression argument = argumentIterator.next();

            if (argument instanceof LambdaIdentifiersExpression lambda && lambda.getStatements() instanceof LambdaExpressionStatement body
                    && unboundParameterType instanceof ObjectType functionalType && functionalType.getTypeArguments() != null) {
                castTheParameterReturnedBy(call, lambda, body, functionalType);
                if (call == memberReceiver) {
                    castTheFieldOfAnotherInstanceReturnedBy(call, body, functionalType);
                }
            }
        }
    }

    /** A lambda returning its parameter, whose type arguments the lambda's bytecode lacks. */
    private void castTheParameterReturnedBy(ClassFileMethodInvocationExpression call, LambdaIdentifiersExpression lambda, LambdaExpressionStatement body, ObjectType functionalType) {
        Expression returned = body.getExpression();

        if (lambda.getParameterNames() == null || !returned.isLocalVariableReferenceExpression() || !lambda.getParameterNames().contains(returned.getName())
                || !(returned.getType() instanceof ObjectType resultType) || resultType.getTypeArguments() != null || resultType.getDimension() != 0) {
            return;
        }
        List<TypeArgument> functionalArguments = toTypeArgumentList(functionalType.getTypeArguments());

        if (functionalArguments.get(functionalArguments.size() - 1) instanceof ObjectType declaredResult && declaredResult.rawEquals(resultType)
                && declaredResult.getTypeArguments() != null && substitute(declaredResult, call.getTypeBindings()) instanceof ObjectType expected
                && hasKnownTypeParameters(expected) && !hasWildcardTypeArgument(expected) && !expected.findTypeParametersInType().isEmpty()) {
            body.setExpression(addCastExpression(expected, returned));
        }
    }

    /** A field of another instance ('future.state.future'): its type variables are that instance's. */
    private void castTheFieldOfAnotherInstanceReturnedBy(ClassFileMethodInvocationExpression call, LambdaExpressionStatement body, ObjectType functionalType) {
        if (!(body.getExpression() instanceof FieldReferenceExpression field) || field.getExpression().isThisExpression() || field.getExpression().isObjectTypeReferenceExpression()
                || !(field.getType() instanceof ObjectType fieldType) || fieldType.getTypeArguments() == null || fieldType.getDimension() != 0
                || fieldType.findTypeParametersInType().isEmpty()) {
            return;
        }
        List<TypeArgument> functionalArguments = toTypeArgumentList(functionalType.getTypeArguments());

        if (upperBoundOrSelf(functionalArguments.get(functionalArguments.size() - 1)) instanceof GenericType result && getMethodTypeParameterNames(call).contains(result.getName())) {
            TypeArguments wildcards = new TypeArguments();

            toTypeArgumentList(fieldType.getTypeArguments()).forEach(typeArgument -> wildcards.add(WildcardTypeArgument.WILDCARD_TYPE_ARGUMENT));
            ObjectType castType = fieldType.createType(singleOrList(wildcards));

            body.setExpression(addCastExpression(castType, body.getExpression()));
            if (call.getTypeParameters().size() == 1 && call.getNonWildcardTypeArguments() == null) {
                // else T is the capture of the wildcard
                call.setNonWildcardTypeArguments(castType);
            }
        }
    }

    /**
     * 'throw new SneakyThrows&lt;Error&gt;().throwIt(t)' (throwIt throws T): the bytecode has no type argument and a raw instance throws
     * Throwable; use an unchecked type.
     */
    private void writeTypeArgumentOfInstanceWhoseMethodThrowsIt(MethodInvocationExpression expression) {
        if (!(expression.getExpression() instanceof ClassFileNewExpression newExpression) || newExpression.getObjectType().getTypeArguments() != null
                || newExpression.getBodyDeclaration() != null) {
            return;
        }
        TypeTypes instanceTypes = typeMaker.makeTypeTypes(newExpression.getObjectType().getInternalName());
        TypeMaker.MethodTypes methodTypes = typeMaker.makeMethodTypes(expression.getInternalTypeName(), expression.getName(), expression.getDescriptor());

        if (instanceTypes == null || instanceTypes.getTypeParameters() == null || methodTypes == null || methodTypes.getExceptionTypes() == null) {
            return;
        }
        Set<String> thrown = StreamSupport.stream(methodTypes.getExceptionTypes().spliterator(), false)
                .filter(GenericType.class::isInstance).map(exceptionType -> ((GenericType) exceptionType).getName()).collect(Collectors.toSet());
        List<String> identifiers = identifiersOf(instanceTypes.getTypeParameters());

        if (thrown.containsAll(identifiers) && allAreSatisfiedByRuntimeException(instanceTypes.getTypeParameters())) {
            TypeArguments arguments = new TypeArguments();

            identifiers.forEach(identifier -> arguments.add(typeMaker.makeFromInternalTypeName("java/lang/RuntimeException")));
            newExpression.setObjectType(newExpression.getObjectType().createType(singleOrList(arguments)));
            newExpression.setDiamondPossible(false);
        }
    }

    /** A bound such as 'T extends Error' is not satisfied by RuntimeException: the raw instance then throws an unchecked type already. */
    private static boolean allAreSatisfiedByRuntimeException(BaseTypeParameter typeParameters) {
        for (org.jd.core.v1.model.javasyntax.type.TypeParameter typeParameter : typeParameters) {
            if (typeParameter instanceof TypeParameterWithTypeBounds withBounds) {
                BaseType bounds = withBounds.getTypeBounds();

                for (Type bound : bounds.isList() ? bounds.<Type>getList() : List.of(bounds.getFirst())) {
                    if (!(bound instanceof ObjectType objectType) || !RUNTIME_EXCEPTION_BOUNDS.contains(objectType.getInternalName())) {
                        return false;
                    }
                }
            }
        }
        return true;
    }

    /**
     * 'inputs.&lt;FluentFuture&lt;?&gt;&gt;transform(f).toList()': a receiver has no target type, javac makes the type variable the capture of
     * its argument's wildcard; write it.
     */
    private void writeTypeArgumentsOfReceiverBoundToWildcardType(MethodInvocationExpression expression) {
        if (expression != memberReceiver || !(expression instanceof ClassFileMethodInvocationExpression call) || call.getTypeParameters() == null
                || call.getNonWildcardTypeArguments() != null || call.getTypeBindings() == null || call.getExpression().isThisExpression()
                || hasAnArgumentCapturedFromAWildcardReceiver(call)) {
            return;
        }
        TypeArguments arguments = new TypeArguments();
        boolean wildcard = false;

        for (org.jd.core.v1.model.javasyntax.type.TypeParameter typeParameter : call.getTypeParameters()) {
            if (!(call.getTypeBindings().get(typeParameter.getIdentifier()) instanceof ObjectType bound) || !hasKnownTypeParameters(bound)) {
                return;
            }
            wildcard |= bound.getTypeArguments() != null && toTypeArgumentList(bound.getTypeArguments()).stream().anyMatch(WildcardTypeArgument.WILDCARD_TYPE_ARGUMENT::equals);
            arguments.add(bound);
        }
        if (wildcard) {
            call.setNonWildcardTypeArguments(arguments.size() == 1 ? arguments.getFirst() : arguments);
        }
    }

    /**
     * 'Ordering.from(c).onKeys().immutableSortedCopy(list)': a receiver has no target type, and javac infers the bounds ('onKeys': &lt;F
     * extends T&gt;) when the bytecode does not say; the receiver is raw.
     */
    private void castReceiverWithUnresolvedBoundedTypeVariablesToRaw(MethodInvocationExpression expression) {
        if (!(expression.getExpression() instanceof ClassFileMethodInvocationExpression receiver) || receiver.getTypeParameters() == null
                || receiver.getNonWildcardTypeArguments() != null || receiver.getTypeBindings() == null
                || !(receiver.getUnboundType() instanceof ObjectType unbound) || unbound.getTypeArguments() == null || unbound.getDimension() != 0
                || !(receiver.getType() instanceof ObjectType receiverType)) {
            return;
        }
        Set<String> usedVariables = unbound.findTypeParametersInType();

        for (org.jd.core.v1.model.javasyntax.type.TypeParameter typeParameter : receiver.getTypeParameters()) {
            if (usedVariables.contains(typeParameter.getIdentifier()) && typeParameter instanceof TypeParameterWithTypeBounds withBounds
                    && !(withBounds.getTypeBounds().getFirst() instanceof ObjectType bound && isJavaLangObject(bound))) {
                TypeArgument binding = receiver.getTypeBindings().get(typeParameter.getIdentifier());

                if (binding == null || WildcardTypeArgument.WILDCARD_TYPE_ARGUMENT.equals(binding)
                        || binding instanceof ObjectType bindingType && withBounds.getTypeBounds().getFirst() instanceof ObjectType firstBound && bindingType.rawEquals(firstBound)
                        && hasTypeParameterBoundedByTypeVariable(expression)) {
                    expression.setExpression(addCastExpression(receiverType.createType(null), receiver));
                    return;
                }
            }
        }
    }

    /** 'onKeys' (&lt;F extends T&gt;): F is bounded by a type variable of the receiver's class. */
    private static boolean hasTypeParameterBoundedByTypeVariable(MethodInvocationExpression expression) {
        if (!(expression instanceof ClassFileMethodInvocationExpression call) || call.getTypeParameters() == null) {
            return false;
        }
        for (org.jd.core.v1.model.javasyntax.type.TypeParameter typeParameter : call.getTypeParameters()) {
            if (typeParameter instanceof TypeParameterWithTypeBounds withBounds && withBounds.getTypeBounds().getFirst() instanceof GenericType) {
                return true;
            }
        }
        return false;
    }

    /**
     * 'Collectors.toMap(f, g, h, () -&gt; new TreeMap&lt;&gt;(c))' (M extends Map&lt;K, U&gt;): the diamond infers nothing from the other
     * arguments; give the instance the type arguments that make it M's bound.
     */
    private void writeTypeArgumentsOfInstancesSuppliedToBoundedTypeVariables(ClassFileMethodInvocationExpression call, BaseExpression parameters, BaseType unboundParameterTypes) {
        if (call.getTypeParameters() == null || call.getTypeBindings() == null || unboundParameterTypes == null || parameters == null || parameters.size() != unboundParameterTypes.size()) {
            return;
        }
        Iterator<Expression> parameterIterator = parameters.iterator();

        for (Type unboundParameterType : unboundParameterTypes) {
            Expression parameter = parameterIterator.next();

            if (parameter instanceof LambdaIdentifiersExpression lambda && lambda.getStatements() instanceof LambdaExpressionStatement body
                    && body.getExpression() instanceof NewExpression newExpression && newExpression.getBodyDeclaration() == null
                    && isWithoutKnownTypeArguments(newExpression.getObjectType()) && newExpression.getObjectType().getDimension() == 0
                    && unboundParameterType instanceof ObjectType functionalType && functionalType.getTypeArguments() != null
                    && upperBoundOrSelf(toTypeArgumentList(functionalType.getTypeArguments()).get(0)) instanceof GenericType variable) {
                writeTypeArgumentsOfSuppliedInstance(newExpression, boundOfMethodTypeVariable(call, variable));
            }
        }
    }

    /** @param required the bound of the type variable the instance is supplied to */
    private void writeTypeArgumentsOfSuppliedInstance(NewExpression newExpression, ObjectType required) {
        TypeMaker.TypeTypes instanceTypes = required == null ? null : typeMaker.makeTypeTypes(newExpression.getObjectType().getInternalName());

        if (instanceTypes == null || instanceTypes.getTypeParameters() == null) {
            return;
        }
        BaseTypeArgument arguments = argumentsBoundByUnifying(instanceTypes.getTypeParameters(),
                newExpression.getObjectType().createType(ownTypeArguments(instanceTypes.getTypeParameters())), required);

        if (arguments != null) {
            newExpression.setObjectType(newExpression.getObjectType().createType(arguments));
            newExpression.setDiamondPossible(false);
        }
    }

    /** 'new TreeMap&lt;&gt;(c)' is a TreeMap&lt;?, ?&gt; until something types it. */
    private static boolean isWithoutKnownTypeArguments(ObjectType type) {
        return type.getTypeArguments() == null || toTypeArgumentList(type.getTypeArguments()).stream().allMatch(WildcardTypeArgument.WILDCARD_TYPE_ARGUMENT::equals);
    }

    /**
     * @return the first bound of the type parameter, with the call's type arguments substituted; null if it is not parameterized
     */
    private ObjectType boundOfMethodTypeVariable(ClassFileMethodInvocationExpression call, GenericType variable) {
        for (org.jd.core.v1.model.javasyntax.type.TypeParameter typeParameter : call.getTypeParameters()) {
            if (typeParameter.getIdentifier().equals(variable.getName()) && typeParameter instanceof TypeParameterWithTypeBounds withBounds
                    && withBounds.getTypeBounds().getFirst() instanceof ObjectType bound && bound.getTypeArguments() != null
                    && substitute(bound, call.getTypeBindings()) instanceof ObjectType substituted) {
                return substituted;
            }
        }
        return null;
    }

    /**
     * 'Collector.of(Builder::new, ..., Builder::combine, Builder::build)': only the supplier gives the accumulator type, and a generic constructor reference, diamond or generic factory infers nothing from the other arguments.
     * Write its type arguments: those the finisher gives to the result ('build' returns an ImmutableMap&lt;K, V&gt; of a Builder&lt;K, V&gt;).
     */
    private void writeTypeArgumentsOfAccumulatorSuppliedToCollector(ClassFileMethodInvocationExpression call) {
        if (!"java/util/stream/Collector".equals(call.getInternalTypeName()) || !"of".equals(call.getName())
                || !(call.getParameters() instanceof Expressions arguments) || arguments.size() < 4 || call.getTypeBindings() == null) {
            return;
        }
        ObjectType accumulator = accumulatorTypeReturnedBy(arguments.get(3), call.getTypeBindings().get("R"));
        Expression supplier = arguments.get(0);

        if (accumulator == null) {
            writeTypeArgumentsOfFactoryAsTheOnesOfTheResult(supplier, arguments.get(1), call.getTypeBindings().get("T"), call.getTypeBindings().get("R"));
            return;
        }
        Expression created = supplier instanceof LambdaIdentifiersExpression lambda && lambda.getStatements() instanceof LambdaExpressionStatement body ? body.getExpression() : null;

        if (supplier instanceof ConstructorReferenceExpression reference && reference.getObjectType().getTypeArguments() == null
                && reference.getObjectType().getDimension() == 0 && reference.getObjectType().getInternalName().equals(accumulator.getInternalName())) {
            arguments.set(0, new ConstructorReferenceExpression(reference.getLineNumber(), reference.getType(), accumulator, reference.getDescriptor()));
        } else if (created instanceof NewExpression newExpression && newExpression.getBodyDeclaration() == null
                && newExpression.getObjectType().getInternalName().equals(accumulator.getInternalName())) {
            newExpression.setObjectType(accumulator);
            newExpression.setDiamondPossible(false);
        } else if (created instanceof ClassFileMethodInvocationExpression factory && factory.getNonWildcardTypeArguments() == null && factory.getTypeParameters() != null
                && factory.getExpression().isObjectTypeReferenceExpression()) {
            factory.setNonWildcardTypeArguments(typeArgumentsOfMethodReturning(factory.getInternalTypeName(), factory.getName(), factory.getDescriptor(), accumulator));
        } else if (supplier instanceof MethodReferenceExpression reference && reference.getNonWildcardTypeArguments() == null
                && reference.getExpression() instanceof ObjectTypeReferenceExpression) {
            reference.setNonWildcardTypeArguments(typeArgumentsOfMethodReturning(reference.getInternalTypeName(), reference.getName(), reference.getDescriptor(), accumulator));
        }
    }

    /**
     * 'Collector.of(LinkedHashMultiset::create, ..., m -&gt; ImmutableMultiset.copyFromEntries(m.entrySet()))': when nothing else says, a
     * collector of a collection of E accumulates E; javac would infer Object.
     */
    private void writeTypeArgumentsOfFactoryAsTheOnesOfTheResult(Expression supplier, Expression accumulator, TypeArgument element, TypeArgument result) {
        MethodReferenceExpression factory = factoryOf(supplier);

        if (factory == null || factory.getNonWildcardTypeArguments() != null || !(result instanceof ObjectType resultType) || resultType.getTypeArguments() == null
                || !hasKnownTypeParameters(resultType) || hasWildcardTypeArgument(resultType)) {
            return;
        }
        List<TypeArgument> resultArguments = toTypeArgumentList(resultType.getTypeArguments());

        // 'List::add' accumulates the elements themselves: a finisher which changes their type ('Collector<Integer, ?, List<String>>') says nothing about them
        if (accumulatesTheElementsThemselves(accumulator) && resultArguments.size() == 1 && !resultArguments.get(0).equals(element)) {
            return;
        }
        if (returnsItsTypeVariablesInOrder(typeMaker.makeMethodTypes(factory.getInternalTypeName(), factory.getName(), factory.getDescriptor()), resultArguments.size())) {
            TypeArguments arguments = new TypeArguments();

            arguments.addAll(resultArguments);
            factory.setNonWildcardTypeArguments(singleOrList(arguments));
        }
    }

    /** 'List::add', or '(accumulated, element) -> accumulated.add(element)': what is accumulated is the element which the collector gets */
    private static boolean accumulatesTheElementsThemselves(Expression accumulator) {
        if (isProperMethodReference(accumulator)) {
            return true;
        }
        return accumulator instanceof LambdaIdentifiersExpression lambda && lambda.getParameterNames().size() == 2
            && lambda.getStatements() instanceof LambdaExpressionStatement body && body.getExpression() instanceof MethodInvocationExpression call
            && call.getParameters() instanceof LocalVariableReferenceExpression argument && argument.getName().equals(lambda.getParameterNames().get(1));
    }

    /** @return the call of a static factory a lambda returns ('() -&gt; TreeMultiset.create(c)'), or the reference to one ('LinkedHashMultiset::create'); null if the supplier is neither */
    private static MethodReferenceExpression factoryOf(Expression supplier) {
        if (supplier instanceof LambdaIdentifiersExpression lambda && lambda.getStatements() instanceof LambdaExpressionStatement body
                && body.getExpression() instanceof ClassFileMethodInvocationExpression call && call.getExpression().isObjectTypeReferenceExpression()) {
            return call;
        }
        return isProperMethodReference(supplier) && ((MethodReferenceExpression) supplier).getExpression() instanceof ObjectTypeReferenceExpression ? (MethodReferenceExpression) supplier : null;
    }

    /** A generic method of as many type variables as the given count, returning a type made of them in the order they are declared ('&lt;E&gt; LinkedHashMultiset&lt;E&gt; create()'). */
    private static boolean returnsItsTypeVariablesInOrder(TypeMaker.MethodTypes methodTypes, int count) {
        if (methodTypes == null || methodTypes.getTypeParameters() == null || methodTypes.getTypeParameters().size() != count
                || !(methodTypes.getReturnedType() instanceof ObjectType returned) || returned.getTypeArguments() == null) {
            return false;
        }
        List<String> identifiers = identifiersOf(methodTypes.getTypeParameters());
        List<TypeArgument> returnedArguments = toTypeArgumentList(returned.getTypeArguments());

        return returnedArguments.size() >= identifiers.size()
                && IntStream.range(0, identifiers.size()).allMatch(i -> returnedArguments.get(i) instanceof GenericType variable && variable.getName().equals(identifiers.get(i)));
    }

    /**
     * @return the type arguments that make the generic method return the given type; null if unknown or not generic
     */
    private BaseTypeArgument typeArgumentsOfMethodReturning(String internalTypeName, String name, String descriptor, ObjectType returned) {
        TypeMaker.MethodTypes methodTypes = typeMaker.makeMethodTypes(internalTypeName, name, descriptor);

        if (methodTypes == null || methodTypes.getTypeParameters() == null || !(methodTypes.getReturnedType() instanceof ObjectType declared)
                || !declared.getInternalName().equals(returned.getInternalName()) || declared.getTypeArguments() == null || returned.getTypeArguments() == null) {
            return null;
        }
        return argumentsBoundByUnifying(methodTypes.getTypeParameters(), declared, returned);
    }

    /**
     * @param declared a type written with the given type parameters ('Builder&lt;K, V&gt;')
     * @param actual the same type with actual arguments
     * @return the arguments of the type parameters; null if one is unknown
     */
    private BaseTypeArgument argumentsBoundByUnifying(BaseTypeParameter typeParameters, ObjectType declared, ObjectType actual) {
        List<TypeArgument> seen = typeArgumentsSeenAs(actual.getInternalName(), declared, new HashSet<String>());
        List<TypeArgument> actualArguments = toTypeArgumentList(actual.getTypeArguments());

        if (seen.isEmpty() || seen.size() != actualArguments.size()) {
            return null;
        }
        Map<String, TypeArgument> bindings = new HashMap<>();

        for (int i = 0; i < seen.size(); i++) {
            if (seen.get(i) instanceof GenericType variable && variable.getDimension() == 0 && actualArguments.get(i) instanceof Type argument && hasKnownTypeParameters(argument)) {
                bindings.put(variable.getName(), argument);
            }
        }
        TypeArguments arguments = new TypeArguments();

        for (org.jd.core.v1.model.javasyntax.type.TypeParameter typeParameter : typeParameters) {
            TypeArgument argument = bindings.get(typeParameter.getIdentifier());

            if (argument == null) {
                return null;
            }
            arguments.add(argument);
        }
        return arguments;
    }

    /**
     * @return the receiver type of 'Owner::method' (or 'state -&gt; state.method()') with the type arguments that make 'method' return the result; null if unknown
     */
    private ObjectType accumulatorTypeReturnedBy(Expression finisher, TypeArgument result) {
        if (!(result instanceof ObjectType resultType) || resultType.getTypeArguments() == null) {
            return null;
        }
        ObjectType owner;
        String internalTypeName;
        String name;
        String descriptor;

        if (finisher instanceof MethodReferenceExpression reference && reference.getExpression() instanceof ObjectTypeReferenceExpression typeReference) {
            owner = typeReference.getObjectType();
            internalTypeName = reference.getInternalTypeName();
            name = reference.getName();
            descriptor = reference.getDescriptor();
        } else if (finisher instanceof LambdaIdentifiersExpression lambda && lambda.getStatements() instanceof LambdaExpressionStatement body
                && body.getExpression() instanceof ClassFileMethodInvocationExpression call && call.getExpression().isLocalVariableReferenceExpression()
                && call.getExpression().getType() instanceof ObjectType receiverType) {
            owner = receiverType;
            internalTypeName = call.getInternalTypeName();
            name = call.getName();
            descriptor = call.getDescriptor();
        } else {
            return null;
        }
        TypeMaker.MethodTypes methodTypes = typeMaker.makeMethodTypes(internalTypeName, name, descriptor);
        TypeMaker.TypeTypes ownerTypes = typeMaker.makeTypeTypes(owner.getInternalName());

        if (owner.getTypeArguments() != null || methodTypes == null || !(methodTypes.getReturnedType() instanceof ObjectType returned) || ownerTypes == null
                || ownerTypes.getTypeParameters() == null) {
            return null;
        }
        BaseTypeArgument arguments = argumentsBoundByUnifying(ownerTypes.getTypeParameters(), returned, resultType);

        return arguments == null ? null : owner.createType(arguments);
    }

    /** javac names the class of the call, which may inherit the method from one of its superclasses */
    private TypeMaker.MethodTypes findMethodTypes(String internalTypeName, String name, String descriptor) {
        Deque<String> owners = new ArrayDeque<>();
        Set<String> visited = new HashSet<>();
        TypeMaker.MethodTypes first = null;

        owners.add(internalTypeName);
        while (!owners.isEmpty()) {
            String owner = owners.poll();

            if (!visited.add(owner)) {
                continue;
            }
            TypeMaker.MethodTypes methodTypes = typeMaker.makeMethodTypes(owner, name, descriptor);

            if (methodTypes != null && methodTypes.getTypeParameters() != null) {
                return methodTypes;
            }
            if (first == null) {
                first = methodTypes;
            }
            TypeTypes typeTypes = typeMaker.makeTypeTypes(owner);

            if (typeTypes != null) {
                for (ObjectType supertype : supertypesOf(typeTypes)) {
                    owners.add(supertype.getInternalName());
                }
            }
        }
        return first;
    }

    private static TypeArgument upperBoundOrSelf(TypeArgument argument) {
        if (argument instanceof WildcardExtendsTypeArgument extending) {
            return extending.type();
        }
        return argument instanceof WildcardSuperTypeArgument supering ? supering.type() : argument;
    }

    private boolean variableHasBound(GenericType variable, ObjectType required) {
        BaseType bounds = declaredBoundsOf(variable.getName());

        if (bounds != null) {
            for (Type declared : bounds) {
                if (declared instanceof ObjectType declaredObjectType && typeMaker.isRawTypeAssignable(required, declaredObjectType)) {
                    return true;
                }
            }
        }
        return false;
    }

    private static boolean isLambdaWithImplicitParameters(LambdaIdentifiersExpression lambda) {
        return lambda.getParameterNames() != null && !lambda.getParameterNames().isEmpty();
    }

    /** A 'Cell&lt;? extends R, ...&gt;' is not a 'Cell&lt;R, ...&gt;': the source has an unchecked cast */
    private static boolean isWildcardWhereConcreteExpected(ObjectType expected, ObjectType actual) {
        return expected.getDimension() == actual.getDimension() && expected.rawEquals(actual)
            && expected.getTypeArguments() != null && actual.getTypeArguments() != null
            && (!hasWildcardTypeArgument(expected) && hasWildcardTypeArgument(actual) || differsInNestedTypeArguments(expected, actual));
    }

    /** Type arguments are invariant: an ImmutableList&lt;TypeToken&lt;?&gt;&gt; is no ImmutableList&lt;TypeToken&lt;? super T&gt;&gt;. */
    private static boolean differsInNestedTypeArguments(ObjectType expected, ObjectType actual) {
        List<TypeArgument> expectedArguments = toTypeArgumentList(expected.getTypeArguments());
        List<TypeArgument> actualArguments = toTypeArgumentList(actual.getTypeArguments());

        if (expectedArguments.size() != actualArguments.size()) {
            return false;
        }
        for (int i = 0; i < expectedArguments.size(); i++) {
            if (expectedArguments.get(i) instanceof ObjectType expectedArgument && actualArguments.get(i) instanceof ObjectType actualArgument
                    && expectedArgument.getDimension() == 0 && actualArgument.getDimension() == 0 && expectedArgument.rawEquals(actualArgument)
                    && expectedArgument.getTypeArguments() != null && actualArgument.getTypeArguments() != null
                    && !expectedArgument.getTypeArguments().equals(actualArgument.getTypeArguments())) {
                return true;
            }
        }
        return false;
    }

    private boolean isFieldOfAnotherInstanceWithTypeVariables(Expression left) {
        return left instanceof FieldReferenceExpression field
                && !field.getExpression().isThisExpression()
                && !field.getExpression().isObjectTypeReferenceExpression()
                && field.getType() instanceof ObjectType fieldType
                && fieldType.getTypeArguments() != null
                && !fieldType.findTypeParametersInType().isEmpty()
                && field.getExpression().getType() instanceof ObjectType receiverType
                && receiverType.getTypeArguments() != null;
    }

    private Type contextualTernaryType(Type ternaryType) {
        if (expectedType == null) {
            return ternaryType;
        }
        if (expectedType instanceof ObjectType expectedObjectType
                && ternaryType instanceof ObjectType ternaryObjectType
                && expectedObjectType.rawEquals(ternaryObjectType)) {
            return expectedType;
        }
        if (expectedType instanceof GenericType expectedGenericType
                && ternaryType instanceof GenericType ternaryGenericType
                && expectedGenericType.getName().equals(ternaryGenericType.getName())
                && expectedGenericType.getDimension() == ternaryGenericType.getDimension()) {
            return expectedType;
        }
        return expectedType.isPrimitiveType() && ternaryType.isPrimitiveType() ? expectedType : ternaryType;
    }

    private static void normalizeTernaryBooleanConstants(TernaryOperatorExpression expression, Type expressionType) {
        Expression trueExpression = expression.getTrueExpression();
        Expression falseExpression = expression.getFalseExpression();
        if (TYPE_BOOLEAN.equals(expressionType)) {
            if (trueExpression instanceof IntegerConstantExpression value) {
                expression.setTrueExpression(new BooleanExpression(value.getLineNumber(), value.getIntegerValue() != 0));
            }
            if (falseExpression instanceof IntegerConstantExpression value) {
                expression.setFalseExpression(new BooleanExpression(value.getLineNumber(), value.getIntegerValue() != 0));
            }
            return;
        }
        if (trueExpression instanceof BooleanExpression value
                && falseExpression instanceof IntegerConstantExpression) {
            expression.setTrueExpression(new IntegerConstantExpression(
                    expression.getLineNumber(), TYPE_INT, value.isTrue() ? 1 : 0));
            return;
        }
        if (falseExpression instanceof BooleanExpression value
                && trueExpression instanceof IntegerConstantExpression) {
            expression.setFalseExpression(new IntegerConstantExpression(
                    expression.getLineNumber(), TYPE_INT, value.isTrue() ? 1 : 0));
            return;
        }
        if (trueExpression instanceof IntegerConstantExpression trueValue
                && falseExpression instanceof IntegerConstantExpression falseValue) {
            normalizeTernaryIntegerConstants(trueValue, falseValue);
        }
    }

    private static void normalizeTernaryIntegerConstants(IntegerConstantExpression trueValue,
            IntegerConstantExpression falseValue) {
        boolean trueCanBeBoolean = (((PrimitiveType)trueValue.getType()).getFlags() & FLAG_BOOLEAN) != 0;
        boolean falseCanBeBoolean = (((PrimitiveType)falseValue.getType()).getFlags() & FLAG_BOOLEAN) != 0;
        if (trueCanBeBoolean && !falseCanBeBoolean) {
            trueValue.setType(TYPE_INT);
        }
        if (falseCanBeBoolean && !trueCanBeBoolean) {
            falseValue.setType(TYPE_INT);
        }
    }

    @Override
    public void visit(CastExpression expression) {
        if (expression.isByteCodeCheckCast() && expression.getType() instanceof ObjectType objectType
                && objectType.getTypeArguments() != null
                && objectType.findTypeParametersInType().isEmpty()
                && expression != memberReceiver
                && !(expression.getExpression() instanceof MethodReferenceExpression)) {
            // A CHECKCAST records only the erased class. A concrete parameterization inferred for the
            // implementation cast can be invariantly incompatible with the source-level use.
            expression.setType(objectType.createType(null));
        }
        Expression nestedExpression = expression.getExpression();
        nestedExpression.accept(this);
        if (expression.isByteCodeCheckCast()
                && nestedExpression instanceof ClassFileMethodInvocationExpression methodInvocation
                && methodInvocation.getUnboundType() instanceof GenericType) {
            hasProperArgumentConstraint(methodInvocation);
        }
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

    /** Bundles the three cast-insertion flags shared by {@link #updateParameters} and its helpers. */
    private record CastFlags(boolean forceCast, boolean unique, boolean rawCast) {
    }

    protected BaseExpression updateParameters(Map<String, TypeArgument> typeBindings, Map<String, BaseType> localTypeBounds, BaseType types, BaseType unboundTypes, BaseExpression expressions, CastFlags flags) {
        if (expressions == null) {
            return null;
        }
        if (!expressions.isList()) {
            Type unboundType = unboundTypes == null ? null : unboundTypes.getFirst();
            return updateParameter(typeBindings, localTypeBounds, types.getFirst(), unboundType, expressions.getFirst(), flags.forceCast(), flags.unique(), flags.rawCast());
        }

        DefaultList<Type> typeList = types.getList();
        DefaultList<Type> unboundTypeList = unboundTypes == null ? null : unboundTypes.getList();
        DefaultList<Expression> expressionList = expressions.getList();

        for (int i = expressionList.size() - 1; i >= 0; i--) {
            Type unboundType = unboundTypeList == null ? null : unboundTypeList.get(i);
            expressionList.set(i, updateParameter(typeBindings, localTypeBounds, typeList.get(i), unboundType, expressionList.get(i), flags.forceCast(), flags.unique(), flags.rawCast()));
        }

        return expressions;
    }

    private Expression updateParameter(Map<String, TypeArgument> typeBindings, Map<String, BaseType> localTypeBounds, Type type, Type unboundType, Expression expression, boolean forceCast, boolean unique, boolean rawCast) {

        expression = updateExpression(typeBindings, localTypeBounds, type, unboundType, expression, forceCast, unique, rawCast);

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

    private Expression updateExpression(Map<String, TypeArgument> typeBindings, Map<String, BaseType> localTypeBounds, Type type, Type unboundType, Expression expression, boolean forceCast, boolean unique, boolean rawCast) {
        // Only the outermost expression of a returned value or of a local variable initializer is concerned
        boolean genericCastInLambda = castToGenericInLambda;
        castToGenericInLambda = false;

        // The synthetic field holding a captured variable has an erased type: use the enclosing method parameter's
        if (visitingAnonymousClass && expression instanceof FieldReferenceExpression fieldRef
                && expression.getType() instanceof ObjectType ot && ot.getTypeArguments() == null) {
            BaseTypeArgument parameterTypeArgument = parameterTypeArguments.get(expression.getName());
            if (parameterTypeArgument != null) {
                fieldRef.setType(ot.createType(parameterTypeArgument));
            }
        }

        if (expression.isNullExpression()) {
            if (forceCast) {
                searchFirstLineNumberVisitor.init();
                expression.accept(searchFirstLineNumberVisitor);
                expression = new CastExpression(searchFirstLineNumberVisitor.getLineNumber(), type, expression);
            }
        } else if ("java/util/stream/Collectors".equals(expression.getInternalTypeName()) && "toList".equals(expression.getName())) {
        } else if (expression instanceof ClassFileMethodInvocationExpression call && type instanceof ObjectType expected && type.getDimension() == 0
                && hasBoundedTypeVariableTargetedByUnboundedVariable(call, expected)) {
            // 'Ordering.natural()' (natural: <C extends Comparable>) is not an Ordering<E> for any E which is no Comparable
            expression = addCastExpression(expected.createType(null), expression);
            expression.getExpression().accept(this);
        } else if (forceCast && !visitingWitnessedInvocation && unboundType instanceof GenericType gt && unboundType.getDimension() == 0
                && localTypeBounds.get(gt.getName()) instanceof ObjectType ot) {
            // Casting to the type variable's bound contradicts explicit type arguments: skip when the call has a witness
            expression = addCastExpression(ot, expression);
        } else {
            Type expressionType = expression.getType();

            boolean lambdaCastForced = forceCast && expression instanceof LambdaIdentifiersExpression lambda && type instanceof ObjectType && expressionType.equals(type)
                    && isLambdaWithImplicitParameters(lambda);

            if (lambdaCastForced) {
                // 'spliterator.tryAdvance(i -> ...)' is ambiguous between a Consumer and an IntConsumer: javac needs the target type
                expression = addCastExpression(type, expression);
            } else if (!expressionType.equals(type)) {
                if (type.isObjectType()) {
                    if (expressionType.isObjectType()) {
                        ObjectType objectType = (ObjectType) type;
                        ObjectType expressionObjectType = (ObjectType) expressionType;
                        if (rawCast) {
                            expression = addCastExpression(objectType.createType(null), expression);
                        } else if (forceCast && (!objectType.rawEquals(expressionObjectType) || expression instanceof LambdaIdentifiersExpression)) {
                            // Force disambiguation of method invocation => Add cast
                            if (expression.isNewExpression()) {
                                ClassFileNewExpression ne = (ClassFileNewExpression)expression;
                                ne.setObjectType(ne.getObjectType().createType(null));
                            }
                            if (expression instanceof LambdaIdentifiersExpression
                                    || objectType.getTypeArguments() != null && hasKnownTypeParameters(objectType)
                                    && (visitingWitnessedInvocation || !objectType.findTypeParametersInType().isEmpty())) {
                                // (a raw argument would turn the call into an unchecked one, whatever the witness says)
                                expression = addCastExpression(objectType, expression);
                            } else {
                                expression = addCastExpression(objectType.createType(null), expression);
                            }
                        } else if (!isJavaLangObject(type) && (!typeMaker.isAssignable(typeBindings, localTypeBounds, objectType, unboundType, expressionObjectType)
                                || isUncheckedTypeVariableMismatch(objectType, expression, expressionObjectType)
                                || isWildcardWhereConcreteExpected(objectType, expressionObjectType))) {
                            BaseTypeArgument ta1 = objectType.getTypeArguments();
                            BaseTypeArgument ta2 = expressionObjectType.getTypeArguments();
                            Type t = type;

                            if (ta1 != null && ta2 != null && (!ta1.isTypeArgumentAssignableFrom(typeMaker, typeBindings, localTypeBounds, ta2) || differsInNestedTypeArguments(objectType, expressionObjectType))) {
                                if (objectType.rawEquals(expressionObjectType) && isInferredFromTarget(expression, ta1, ta2)) {
                                    // The type arguments of the invoked method are only fixed by the target type: javac infers them
                                    acceptWithExpectedType(expression, type);
                                    return expression;
                                }
                                if (objectType.rawEquals(expressionObjectType) && isWildcardOnTypeVariableMismatch(ta1, ta2, unboundType, expressionObjectType)
                                        && !differsInNestedTypeArguments(objectType, expressionObjectType)) {
                                    // Wildcard type arguments bind to the method's type variables through capture conversion: no cast needed
                                    acceptWithExpectedType(expression, type);
                                    return expression;
                                }
                                if (unboundType == null && ta2.isGenericTypeArgument()
                                        && (ta1 instanceof WildcardExtendsTypeArgument || ta1 instanceof WildcardSuperTypeArgument)) {
                                    // No binding was tracked for this call (e.g. an unwitnessed generic method invocation), and
                                    // the target's wildcard bounds an unrelated type variable of the invoked method itself
                                    // (e.g. <S> Foo<S> m(Bar<? extends S>) called with Bar<V>): we cannot determine whether S
                                    // and V unify without real type inference, and stripping to a raw type is always wrong here.
                                    acceptWithExpectedType(expression, type);
                                    return expression;
                                }
                                // Incompatible typeArgument arguments => Add cast
                                t = objectType.createType(ta1.isGenericTypeArgument() || isWildcardExtendingTypeVariable(ta1) ? ta1 : null);
                            }
                            // A 'new' of a non-generic class has no diamond to infer the type arguments from: its supertypes
                            // are fixed (e.g. 'new ObjectTypeAdapter()' is a TypeAdapter<Object>, never a TypeAdapter<T>)
                            boolean fixedSupertypes = expression instanceof ClassFileNewExpression && ta1 != null && expressionObjectType.getTypeArguments() == null
                                    && typeMaker.isRawTypeAssignable(objectType, expressionObjectType) && hasMethodLevelTypeVariable(objectType);
                            // An anonymous class creation spells its type arguments out (no diamond to infer them from)
                            boolean explicitTypeArguments = expression instanceof ClassFileNewExpression newExpression
                                    && newExpression.getBodyDeclaration() != null && ta1 != null && ta1.isGenericTypeArgument() && ta2 != null;
                            fixedSupertypes |= explicitTypeArguments;
                            // 'new Foo<>(wildcardParameterizedArgument)' is a Foo<capture>, which no target type can be inferred for
                            fixedSupertypes |= expression instanceof ClassFileNewExpression && ta1 != null && ta2 != null
                                    && ta2.equals(WildcardTypeArgument.WILDCARD_TYPE_ARGUMENT) && !hasWildcardTypeArgument(objectType);
                            if ((!expression.isNew() || fixedSupertypes) && hasKnownTypeParameters(t)
                                    && !(unboundType == null && t instanceof ObjectType castType && mentionsTypeVariableTheCallDoesNotBind(castType, typeBindings))
                                    && !isInferredFromItsArgumentsWhereTheBindingIsUnknown(expression)) {
                                if (!(expression instanceof MethodReferenceExpression)
                                        && t instanceof ObjectType targetType
                                        && targetType.getTypeArguments() != null
                                        && targetType.findTypeParametersInType().isEmpty()
                                        && !hasWildcardTypeArgument(targetType)
                                        && typeMaker.isRawTypeAssignable(targetType, expressionObjectType)) {
                                    t = targetType.createType(null);
                                }
                                expression = castThroughRawTypeIfBounded(t, expression);
                            }
                        } else if (isNewOfNonGenericClassToTypeVariableTarget(objectType, expression, expressionObjectType)) {
                            // TypeMaker reports 'class C implements TA<Object>' as having the raw supertype TA, so the assignment
                            // to TA<T> looks fine; it is not ('new C()' is never a TA<T>): an unchecked cast is always valid
                            expression = castThroughRawTypeIfBounded(objectType, expression);
                        }
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
                        && (hasKnownTypeParameters(type) || type.getDimension() > 0 && !staticContext)
                        && (expressionType.isObjectType() || expressionType.isGenericType())
                        && (type.getDimension() != 0 || !visitingLambda || genericCastInLambda)
                        && !(visitingVarArgsInvocation && expression.isNewInitializedArray())) {
                    // (the array of the arguments of a varargs call is printed as its elements: a cast would be printed in front of the first one)
                    expression = addCastExpression(type, expression);
                }
            }

            if (!lambdaCastForced && expression instanceof CastExpression castExpression && isCastToBeRemoved(typeBindings, localTypeBounds, type, castExpression, unique)) {
                // Remove cast expression
                expression = expression.getExpression();
            }
            if (!unique && isProperMethodReference(expression) && type instanceof ObjectType targetType
                    && expression.getType() instanceof ObjectType expressionObjectType
                    && targetType.rawEquals(expressionObjectType) && hasKnownTypeParameters(type)
                    && !hasWildcardTypeArgument(targetType) && !containsErasedRawGenericType(targetType, unboundType)) {
                // A method reference may be compatible with several overloads: cast to the target functional interface type
                expression = addCastExpression(type, expression);
            }
            acceptWithExpectedType(expression, type);
        }

        return expression;
    }

    /** @return true if one of the type variables of the type is declared by a method (class type variables are shared with the inner classes) */
    private boolean hasMethodLevelTypeVariable(ObjectType type) {
        for (String name : type.findTypeParametersInType()) {
            TypeParameter declaringScope = innermostScopeDeclaring(name);

            if (declaringScope != null && declaringScope.methodLevel()) {
                return true;
            }
        }
        return false;
    }

    private TypeParameter innermostScopeDeclaring(String typeVariableName) {
        for (TypeParameter scope : typeParameters) {
            for (org.jd.core.v1.model.javasyntax.type.TypeParameter typeParameter : scope.type()) {
                if (typeParameter.getIdentifier().equals(typeVariableName)) {
                    return scope;
                }
            }
        }
        return null;
    }

    private boolean isNewOfNonGenericClassToTypeVariableTarget(ObjectType targetType, Expression expression, ObjectType expressionObjectType) {
        if (!expression.isNew() || expressionObjectType.getTypeArguments() != null || targetType.getTypeArguments() == null
                || targetType.getDimension() != 0 || expressionObjectType.getDimension() != 0 || isJavaLangObject(targetType)
                || targetType.findTypeParametersInType().isEmpty() || !hasKnownTypeParameters(targetType)
                || !hasMethodLevelTypeVariable(targetType)
                || !typeMaker.isRawTypeAssignable(targetType, expressionObjectType)) {
            return false;
        }
        TypeTypes typeTypes = typeMaker.makeTypeTypes(expressionObjectType.getInternalName());
        return typeTypes != null && typeTypes.getTypeParameters() == null;
    }

    private void acceptWithExpectedType(Expression expression, Type expectedType) {
        Type previousExpectedType = this.expectedType;
        this.expectedType = expectedType;
        expression.accept(this);
        this.expectedType = previousExpectedType;
    }

    private static boolean isProperMethodReference(Expression expression) {
        return expression instanceof MethodReferenceExpression
            && !(expression instanceof MethodInvocationExpression)
            && !"new".equals(expression.getName());
    }

    /**
     * A raw generic type in the cast target only blocks the cast when it replaced a type variable of the
     * declared parameter type (erasure fallback); a raw type written in the method's declaration is legal
     * in a disambiguating cast.
     */
    private boolean containsErasedRawGenericType(ObjectType targetType, Type unboundType) {
        if (isRawGenericType(targetType)) {
            if (unboundType == null) {
                // No binding occurred: the declared parameter type itself is raw
                return false;
            }
            // A raw target type is an erasure artifact unless the parameter was declared raw
            return !(unboundType instanceof ObjectType unboundObjectType) || !targetType.rawEquals(unboundObjectType)
                    || unboundObjectType.getTypeArguments() != null;
        }
        if (!(unboundType instanceof ObjectType unboundObjectType)) {
            return false;
        }
        List<TypeArgument> targetArgs = toTypeArgumentList(targetType.getTypeArguments());
        List<TypeArgument> unboundArgs = toTypeArgumentList(unboundObjectType.getTypeArguments());
        if (targetArgs.size() != unboundArgs.size()) {
            return false;
        }
        for (int i = 0; i < targetArgs.size(); i++) {
            if (targetArgs.get(i) instanceof ObjectType ot && isRawGenericType(ot)
                    && unboundArgs.get(i) instanceof GenericType) {
                return true;
            }
        }
        return false;
    }

    private boolean isRawGenericType(ObjectType objectType) {
        if (objectType.getTypeArguments() == null && objectType.getDimension() == 0) {
            TypeTypes typeTypes = typeMaker.makeTypeTypes(objectType.getInternalName());
            return typeTypes != null && typeTypes.getTypeParameters() != null;
        }
        return false;
    }

    private boolean isWildcardOnTypeVariableMismatch(BaseTypeArgument ta1, BaseTypeArgument ta2, Type unboundType, ObjectType expressionObjectType) {
        if (!(unboundType instanceof ObjectType unboundObjectType)) {
            return false;
        }
        BaseTypeArgument unboundTypeArguments = unboundObjectType.getTypeArguments();
        if (unboundTypeArguments == null) {
            return false;
        }
        List<TypeArgument> boundArgs = toTypeArgumentList(ta1);
        List<TypeArgument> expressionArgs = toTypeArgumentList(ta2);
        List<TypeArgument> unboundArgs = toTypeArgumentList(unboundTypeArguments);
        if (boundArgs.size() != expressionArgs.size() || boundArgs.size() != unboundArgs.size()) {
            return false;
        }
        boolean wildcardOnTypeVariable = false;
        for (int i = 0; i < boundArgs.size(); i++) {
            if (!boundArgs.get(i).equals(expressionArgs.get(i))) {
                // A wildcard binds freely to the method's type variable only when the binder fell back to the
                // variable's erasure bound; a type variable still in scope requires an explicit cast.
                // If another parameter of the same call also constrains this type variable, the wildcard
                // capture here isn't the sole constraint and the mismatch must not be waived.
                if (!isWildcard(expressionArgs.get(i)) || !(unboundArgs.get(i) instanceof GenericType unboundTypeVariable)
                        || boundArgs.get(i) instanceof GenericType
                        || !captureSatisfiesBound(expressionArgs.get(i), expressionObjectType, i, boundArgs.get(i))
                        || typeVariablesSharedAcrossParameters.contains(unboundTypeVariable.getName())) {
                    return false;
                }
                wildcardOnTypeVariable = true;
            }
        }
        return wildcardOnTypeVariable;
    }

    /**
     * Type variable names that appear in more than one of the method's declared (unbound) parameter types:
     * a wildcard-capture argument at one of those positions cannot be assumed to be the type variable's
     * only constraint, since another argument may bind it to an incompatible concrete type.
     */
    private static Set<String> findTypeVariablesSharedAcrossParameters(BaseType unboundParameterTypes) {
        if (unboundParameterTypes == null) {
            return Collections.emptySet();
        }
        Set<String> seen = new HashSet<>();
        Set<String> shared = new HashSet<>();
        for (Type type : unboundParameterTypes) {
            for (String identifier : type.findTypeParametersInType()) {
                if (!seen.add(identifier)) {
                    shared.add(identifier);
                }
            }
        }
        return shared;
    }

    private boolean captureSatisfiesBound(TypeArgument wildcard, ObjectType expressionObjectType, int typeParameterIndex, TypeArgument requiredBound) {
        if (!(requiredBound instanceof ObjectType requiredBoundType)) {
            return false;
        }
        if (isJavaLangObject(requiredBoundType)) {
            return true;
        }
        Type captureUpperBound = null;
        if (wildcard instanceof WildcardExtendsTypeArgument weta) {
            captureUpperBound = weta.type();
        } else if (wildcard instanceof WildcardTypeArgument) {
            // capture conversion bounds an unbounded wildcard by the declaring class's type parameter bound
            TypeTypes typeTypes = typeMaker.makeTypeTypes(expressionObjectType.getInternalName());
            if (typeTypes != null && typeTypes.getTypeParameters() != null) {
                List<org.jd.core.v1.model.javasyntax.type.TypeParameter> declaredTypeParameters = new ArrayList<>();
                typeTypes.getTypeParameters().forEach(declaredTypeParameters::add);
                if (typeParameterIndex < declaredTypeParameters.size()
                        && declaredTypeParameters.get(typeParameterIndex) instanceof TypeParameterWithTypeBounds tpwtb) {
                    captureUpperBound = tpwtb.getTypeBounds().getFirst();
                }
            }
        }
        return captureUpperBound instanceof ObjectType captureBoundType
                && (requiredBoundType.rawEquals(captureBoundType) || typeMaker.isRawTypeAssignable(requiredBoundType, captureBoundType));
    }

    /**
     * 'predicate.apply(Maps.immutableEntry(entry.getKey(), value))': the call's type variables are fixed by its arguments, whose declared
     * types javac knows better than our bindings (Entry&lt;?, V&gt;): do not cast.
     */
    private boolean isInferredFromItsArgumentsWhereTheBindingIsUnknown(Expression expression) {
        if (!(expression instanceof ClassFileMethodInvocationExpression call) || call.getTypeParameters() == null || call.getTypeBindings() == null) {
            return false;
        }
        Set<String> unknown = identifiersOf(call.getTypeParameters()).stream()
                .filter(identifier -> WildcardTypeArgument.WILDCARD_TYPE_ARGUMENT.equals(call.getTypeBindings().get(identifier))).collect(Collectors.toSet());

        return !unknown.isEmpty() && hasProperMethodArgumentConstraint(call, unknown);
    }

    /**
     * A cast to 'Set&lt;E&gt;' with E a type variable of the callee the call does not bind ('typePreservingSet(Set&lt;E&gt;, ...)') casts to
     * nothing the source has.
     */
    private boolean mentionsTypeVariableTheCallDoesNotBind(ObjectType type, Map<String, TypeArgument> typeBindings) {
        for (String name : type.findTypeParametersInType()) {
            TypeArgument binding = typeBindings == null ? null : typeBindings.get(name);

            if (typeVariablesOfTheCall.contains(name) && (binding == null || WildcardTypeArgument.WILDCARD_TYPE_ARGUMENT.equals(binding))) {
                return true;
            }
        }
        return false;
    }

    private static final Set<String> RUNTIME_EXCEPTION_BOUNDS = Set.of("java/lang/Object", "java/lang/Throwable", "java/lang/Exception", "java/lang/RuntimeException");

    private static List<String> identifiersOf(BaseTypeParameter typeParameters) {
        return StreamSupport.stream(typeParameters.spliterator(), false).map(org.jd.core.v1.model.javasyntax.type.TypeParameter::getIdentifier).toList();
    }

    /** The type arguments of a class with no other than its own type variables: 'Holder&lt;V&gt;' for 'class Holder&lt;V&gt;'. */
    private static BaseTypeArgument ownTypeArguments(BaseTypeParameter typeParameters) {
        TypeArguments arguments = new TypeArguments();

        typeParameters.forEach(typeParameter -> arguments.add(new GenericType(typeParameter.getIdentifier())));
        return singleOrList(arguments);
    }

    private static BaseTypeArgument singleOrList(TypeArguments arguments) {
        return arguments.size() == 1 ? arguments.getFirst() : arguments;
    }

    /** @return the type variables of the class, with the type arguments given to them; empty if the class or the number of arguments is not known */
    private Map<String, TypeArgument> typeVariablesGiven(String internalName, List<TypeArgument> given) {
        TypeTypes types = typeMaker.makeTypeTypes(internalName);

        if (types == null || types.getTypeParameters() == null) {
            return Collections.emptyMap();
        }
        List<String> names = identifiersOf(types.getTypeParameters());

        return names.size() == given.size() ? IntStream.range(0, names.size()).boxed().collect(Collectors.toMap(names::get, given::get)) : Collections.emptyMap();
    }

    /** The type variables of the class of the receiver which the receiver gives a wildcard of the kind. */
    private static Set<String> typeVariablesGivenAWildcard(ObjectType receiverType, TypeTypes receiverTypes, Predicate<TypeArgument> kind) {
        List<String> names = identifiersOf(receiverTypes.getTypeParameters());
        List<TypeArgument> given = toTypeArgumentList(receiverType.getTypeArguments());

        return IntStream.range(0, Math.min(names.size(), given.size())).filter(i -> kind.test(given.get(i))).mapToObj(names::get).collect(Collectors.toSet());
    }

    private static boolean isWildcard(TypeArgument typeArgument) {
        return typeArgument instanceof WildcardTypeArgument
            || typeArgument instanceof WildcardExtendsTypeArgument
            || typeArgument instanceof WildcardSuperTypeArgument;
    }

    private static List<TypeArgument> toTypeArgumentList(BaseTypeArgument baseTypeArgument) {
        if (baseTypeArgument instanceof TypeArguments typeArguments) {
            return typeArguments;
        }
        if (baseTypeArgument instanceof TypeArgument typeArgument) {
            return Collections.singletonList(typeArgument);
        }
        return Collections.emptyList();
    }

    private boolean isCastToBeRemoved(Map<String, TypeArgument> typeBindings, Map<String, BaseType> localTypeBounds, Type type, CastExpression expression, boolean unique) {
        if (expression.isByteCodeCheckCast()
                && expression.getExpression() instanceof ClassFileMethodInvocationExpression methodInvocationExpression
                && "java/util/Optional".equals(methodInvocationExpression.getInternalTypeName())
                && "orElseThrow".equals(methodInvocationExpression.getName())) {
            return false;
        }
        if (expression.isByteCodeCheckCast()
                && expression.getExpression().isLocalVariableReferenceExpression()
                && (isJavaLangObject(expression.getExpression().getType())
                        || ObjectType.TYPE_UNDEFINED_OBJECT.equals(expression.getExpression().getType()))) {
            // The cast is what makes an Object local valid for the surrounding constructor/method parameter.
            // Removing it does not merely simplify source: it either stops compilation or tempts local-type
            // inference to move the cast before an earlier instanceof guard.
            return false;
        }
        if (expression.isByteCodeCheckCast()
                && expression.getType() instanceof ObjectType castType
                && expression.getExpression() instanceof ClassFileMethodInvocationExpression methodInvocationExpression
                && methodInvocationExpression.getUnboundType() instanceof GenericType genericReturnType
                && localTypeBounds.get(genericReturnType.getName()) instanceof ObjectType returnBound
                && castType.rawEquals(returnBound)) {
            // javac inserts CHECKCAST to the erasure after invoking a method whose source-level return
            // type is a type variable (notably T extends FieldElement<T>). The type variable is still
            // known here, so retaining and parameterizing that implementation cast produces recursive
            // nonsense such as RealFieldElement<RealFieldElement<T>> on fluent method chains.
            return true;
        }
        if (expression.isByteCodeCheckCast()
                && expression.getType() instanceof ObjectType erasedType
                && expression.getExpression() instanceof ClassFileMethodInvocationExpression call
                && call.getType() instanceof GenericType resultVariable && resultVariable.getDimension() == 0
                && declaredBoundsOf(resultVariable.getName()) != null
                && !declaredBoundsOf(resultVariable.getName()).isList() && declaredBoundsOf(resultVariable.getName()).getFirst() instanceof ObjectType resultBound
                && (erasedType.rawEquals(resultBound) || typeMaker.isRawTypeAssignable(erasedType, resultBound))) {
            // The CHECKCAST after a call returning a type variable is to its erasure (or to a wider bound
            // when the variable is the superclass's)
            return true;
        }
        if (expression.isByteCodeCheckCast()
                && type instanceof GenericType expectedGenericType
                && expression.getExpression() instanceof ClassFileMethodInvocationExpression methodInvocationExpression
                && methodInvocationExpression.getUnboundType() instanceof GenericType returnedGenericType
                && expectedGenericType.getName().equals(returnedGenericType.getName())
                && expectedGenericType.getDimension() == returnedGenericType.getDimension()) {
            return true;
        }
        if (!expression.isByteCodeCheckCast() && expression.getType() instanceof ObjectType castType
                && expression.getExpression().getType() instanceof ObjectType nestedType
                && (isUncheckedTypeVariableMismatch(castType, expression.getExpression(), nestedType)
                    || isWildcardWhereConcreteExpected(castType, nestedType))) {
            return false;
        }
        if (isParameterizedJavaLangObject(expression.getType())) {
            return true;
        }
        if (!expression.isByteCodeCheckCast()
                && expression.getType() instanceof ObjectType castType
                && expression.getExpression().getType() instanceof ObjectType nestedObjectType
                && isNewOfNonGenericClassToTypeVariableTarget(castType, expression.getExpression(), nestedObjectType)) {
            return false;
        }
        if (expression.isByteCodeCheckCast()
                && expression.getType() instanceof ObjectType castType
                && expression.getExpression() instanceof ClassFileMethodInvocationExpression methodInvocation
                && requiresNarrowingCastForNestedConstraint(
                        typeBindings, localTypeBounds, castType, methodInvocation)) {
            methodInvocation.setNonWildcardTypeArguments(null);
            return false;
        }
        if (!hasKnownTypeParameters(expression.getType())) {
            return true;
        }
        Expression nestedExpression = expression.getExpression();
        if (isArrayConstructorToArrayCast(type, nestedExpression)) {
            return true;
        }
        if (nestedExpression.getExpression() instanceof FieldReferenceExpression fre && fieldNamesInLambda.contains(fre.getName())) {
            return false;
        }
        Type nestedExpressionType = nestedExpression.getType();

        if (type.isObjectType() && nestedExpressionType.isObjectType()) {
            ObjectType left = (ObjectType) type;
            ObjectType right = (ObjectType) nestedExpressionType;
            if (unique
                    && expression.isByteCodeCheckCast()
                    && nestedExpression instanceof ClassFileMethodInvocationExpression methodInvocationExpression
                    && methodInvocationExpression.getTypeParameters() != null
                    && methodInvocationExpression.getUnboundType() instanceof GenericType genericReturnType
                    && isMethodTypeParameter(methodInvocationExpression, genericReturnType)
                    && left.getTypeArguments() != null
                    && isJavaLangObject(right)
                    && !hasProperArgumentConstraint(methodInvocationExpression)) {
                return true;
            }
            if (expression.isByteCodeCheckCast()
                    && nestedExpression instanceof ClassFileMethodInvocationExpression methodInvocationExpression
                    && methodInvocationExpression.getUnboundType() instanceof GenericType
                    && hasUnboundedWildcardTypeArgument(methodInvocationExpression.getExpression())) {
                // Keep narrowing casts required by wildcard capture, except for the poly-invocation case above:
                // there the target type is what resolves the method's own type parameters.
                hasProperArgumentConstraint(methodInvocationExpression);
                return false;
            }
            if (unique
                    && nestedExpression.isMethodInvocationExpression()
                    && left.getTypeArguments() != null
                    && right.getTypeArguments() != null
                    && !left.rawEquals(right)
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
        return false;
    }

    private boolean requiresNarrowingCastForNestedConstraint(Map<String, TypeArgument> typeBindings,
            Map<String, BaseType> localTypeBounds, ObjectType castType,
            ClassFileMethodInvocationExpression invocation) {
        if (!(invocation.getUnboundType() instanceof GenericType returnedGenericType)
                || !getMethodTypeParameterNames(invocation).contains(returnedGenericType.getName())
                || invocation.getUnboundParameterTypes() == null
                || invocation.getParameters() == null) {
            return false;
        }
        var parameterTypeIterator = invocation.getUnboundParameterTypes().iterator();
        var argumentIterator = invocation.getParameters().iterator();
        while (parameterTypeIterator.hasNext() && argumentIterator.hasNext()) {
            Type parameterType = parameterTypeIterator.next();
            Expression argument = argumentIterator.next();
            if (returnedGenericType.equals(parameterType)
                    && (!(argument.getType() instanceof ObjectType argumentType)
                            || !typeMaker.isAssignable(typeBindings, localTypeBounds, castType, argumentType))) {
                return true;
            }
            Type functionalResultType = getExactFunctionalResultType(argument);
            if (parameterType.findTypeParametersInType().contains(returnedGenericType.getName())
                    && functionalResultType instanceof ObjectType functionalResultObjectType
                    && !typeMaker.isAssignable(typeBindings, localTypeBounds, castType, functionalResultObjectType)) {
                return true;
            }
        }
        return false;
    }

    private Type getExactFunctionalResultType(Expression expression) {
        if (expression instanceof ConstructorReferenceExpression constructorReference) {
            return constructorReference.getObjectType();
        }
        if (expression instanceof MethodReferenceExpression methodReference
                && !(expression instanceof MethodInvocationExpression)) {
            TypeMaker.MethodTypes methodTypes = typeMaker.makeMethodTypes(methodReference.getInternalTypeName(),
                    methodReference.getName(), methodReference.getDescriptor());
            return methodTypes == null ? null : methodTypes.getReturnedType();
        }
        return null;
    }

    private boolean hasProperArgumentConstraint(ClassFileMethodInvocationExpression expression) {
        BaseType parameterTypes = expression.getUnboundParameterTypes();
        BaseExpression arguments = expression.getParameters();
        Set<String> methodTypeParameters = getMethodTypeParameterNames(expression);

        if (parameterTypes != null && arguments != null) {
            var parameterTypeIterator = parameterTypes.iterator();
            var argumentIterator = arguments.iterator();
            int index = 0;

            while (parameterTypeIterator.hasNext() && argumentIterator.hasNext()) {
                Type parameterType = parameterTypeIterator.next();
                Expression argument = argumentIterator.next();

                if (!Collections.disjoint(parameterType.findTypeParametersInType(), methodTypeParameters)
                        && !argument.isNullExpression()
                        && !isTargetDependentGenericInvocation(argument)) {
                    restoreIndependentGenericArgument(expression, arguments, index, parameterType, argument);
                    return true;
                }
                index++;
            }
        }

        return false;
    }

    private void restoreIndependentGenericArgument(ClassFileMethodInvocationExpression invocation,
            BaseExpression arguments, int index, Type parameterType, Expression argument) {
        if (argument instanceof CastExpression cast
                && cast.getExpression() instanceof ClassFileMethodInvocationExpression nested) {
            invocation.setNonWildcardTypeArguments(null);
            if (nested.getUnboundType() != null
                    && !Collections.disjoint(nested.getUnboundType().findTypeParametersInType(), getMethodTypeParameterNames(nested))
                    && nested.getParameters() == null
                    && isRawReturnCompatibleWithCast(cast.getType(), nested.getUnboundType())
                    && typeMaker.matchCount(invocation.getInternalTypeName(), invocation.getName(), arguments.size(), true) == 1) {
                if (arguments.isList()) {
                    arguments.getList().set(index, cast.getExpression());
                } else {
                    invocation.setParameters(cast.getExpression());
                }
            } else {
                restoreParameterizedArgumentCast(invocation, parameterType, cast, nested);
            }
        }
    }

    private void restoreParameterizedArgumentCast(ClassFileMethodInvocationExpression invocation,
            Type parameterType, CastExpression cast, ClassFileMethodInvocationExpression nested) {
        nested.setNonWildcardTypeArguments(null);
        if (!(parameterType instanceof ObjectType)
                || !(cast.getType() instanceof ObjectType castObjectType)
                || !(replaceMethodTypeParameters(parameterType, getMethodTypeParameterReplacements(invocation))
                        instanceof ObjectType restoredParameterType)) {
            return;
        }
        BaseTypeArgument restoredArguments = restoredParameterType.getTypeArguments();
        TypeTypes castTypeTypes = typeMaker.makeTypeTypes(castObjectType.getInternalName());
        if (restoredArguments != null
                && !Collections.disjoint(restoredArguments.findTypeParametersInType(), getMethodTypeParameterNames(invocation))) {
            cast.setType(castObjectType.createType((BaseTypeArgument)null));
        } else if (restoredArguments != null && restoredArguments.findTypeParametersInType().isEmpty()
                && castTypeTypes != null && castTypeTypes.getTypeParameters() != null
                && castTypeTypes.getTypeParameters().size() == toTypeArgumentList(restoredArguments).size()) {
            cast.setType(castObjectType.createType(restoredArguments));
        }
    }

    private static boolean isRawReturnCompatibleWithCast(Type castType, Type returnedType) {
        return !(returnedType instanceof ObjectType returnedObjectType)
                || castType instanceof ObjectType castObjectType && castObjectType.rawEquals(returnedObjectType);
    }

    private static Map<String, Type> getMethodTypeParameterReplacements(
            ClassFileMethodInvocationExpression invocation) {
        Map<String, Type> replacements = new HashMap<>();
        Set<String> methodTypeParameters = getMethodTypeParameterNames(invocation);
        for (org.jd.core.v1.model.javasyntax.type.TypeParameter typeParameter : invocation.getTypeParameters()) {
            Type replacement = getAccessibleTypeParameterReplacement(typeParameter, methodTypeParameters);
            if (replacement != null) {
                replacements.put(typeParameter.getIdentifier(), replacement);
            }
        }
        return replacements;
    }

    private static Type getAccessibleTypeParameterReplacement(
            org.jd.core.v1.model.javasyntax.type.TypeParameter typeParameter,
            Set<String> methodTypeParameters) {
        if (!(typeParameter instanceof TypeParameterWithTypeBounds bounded)) {
            return ObjectType.TYPE_OBJECT;
        }
        if (bounded.getTypeBounds().size() != 1) {
            return null;
        }
        Type replacement = bounded.getTypeBounds().getFirst();
        return Collections.disjoint(replacement.findTypeParametersInType(), methodTypeParameters) ? replacement : null;
    }

    private static Type replaceMethodTypeParameters(Type type, Map<String, Type> methodTypeParameters) {
        if (type instanceof GenericType genericType) {
            Type replacement = methodTypeParameters.get(genericType.getName());
            if (replacement != null) {
                return replaceMethodTypeParameters(replacement.createType(genericType.getDimension()), methodTypeParameters);
            }
        }
        if (type instanceof ObjectType objectType && objectType.getTypeArguments() != null) {
            return objectType.createType(replaceMethodTypeParameters(objectType.getTypeArguments(), methodTypeParameters));
        }
        return type;
    }

    private static BaseTypeArgument replaceMethodTypeParameters(
            BaseTypeArgument typeArguments, Map<String, Type> methodTypeParameters) {
        if (typeArguments instanceof Type type) {
            return replaceMethodTypeParameters(type, methodTypeParameters);
        }
        if (typeArguments instanceof TypeArguments arguments) {
            TypeArguments replacements = new TypeArguments(arguments.size());
            for (TypeArgument argument : arguments) {
                replacements.add((TypeArgument) replaceMethodTypeParameters(argument, methodTypeParameters));
            }
            return replacements;
        }
        if (typeArguments instanceof WildcardExtendsTypeArgument wildcard) {
            return new WildcardExtendsTypeArgument(replaceMethodTypeParameters(wildcard.type(), methodTypeParameters));
        }
        if (typeArguments instanceof WildcardSuperTypeArgument wildcard) {
            return new WildcardSuperTypeArgument(replaceMethodTypeParameters(wildcard.type(), methodTypeParameters));
        }
        return typeArguments;
    }

    private static boolean isMethodTypeParameter(ClassFileMethodInvocationExpression expression, GenericType type) {
        return getMethodTypeParameterNames(expression).contains(type.getName());
    }

    /**
     * @return true for the invocation of a generic method whose type variables, used by the returned type, no argument fixes,
     * when the type arguments which differ from the ones of the target are only those type variables
     */
    private boolean isInferredFromTarget(Expression expression, BaseTypeArgument targetArguments, BaseTypeArgument expressionArguments) {
        if (!(expression instanceof ClassFileMethodInvocationExpression methodInvocation)
                || methodInvocation.getTypeParameters() == null
                || !(methodInvocation.getUnboundType() instanceof ObjectType unboundObjectType)
                || unboundObjectType.getTypeArguments() == null
                || Collections.disjoint(unboundObjectType.findTypeParametersInType(), getMethodTypeParameterNames(methodInvocation))
                || hasProperMethodArgumentConstraint(methodInvocation, unboundObjectType.findTypeParametersInType())) {
            return false;
        }

        List<TypeArgument> target = toTypeArgumentList(targetArguments);
        List<TypeArgument> actual = toTypeArgumentList(expressionArguments);
        List<TypeArgument> unbound = toTypeArgumentList(unboundObjectType.getTypeArguments());

        if (target.size() != actual.size() || target.size() != unbound.size()) {
            return false;
        }
        Set<String> methodTypeParameterNames = getMethodTypeParameterNames(methodInvocation);

        for (int i = 0; i < target.size(); i++) {
            boolean free = unbound.get(i) instanceof GenericType genericType && genericType.getDimension() == 0
                    && methodTypeParameterNames.contains(genericType.getName());

            if (!free && !target.get(i).equals(actual.get(i))) {
                // (a fixed type argument is not changed by the inference)
                return false;
            }
            if (free && !withinBoundsOfMethodTypeParameter(methodInvocation, ((GenericType) unbound.get(i)).getName(), target.get(i))) {
                // 'Ordering<E> o = Ordering.natural()': E is not a Comparable, the inference of the type variable of 'natural' fails
                return false;
            }
        }
        return true;
    }

    private boolean withinBoundsOfMethodTypeParameter(ClassFileMethodInvocationExpression methodInvocation, String name, TypeArgument target) {
        for (org.jd.core.v1.model.javasyntax.type.TypeParameter typeParameter : methodInvocation.getTypeParameters()) {
            if (typeParameter.getIdentifier().equals(name) && typeParameter instanceof TypeParameterWithTypeBounds withBounds) {
                return target instanceof GenericType targetVariable ? satisfiesBounds(targetVariable, withBounds.getTypeBounds())
                        : !(target instanceof ObjectType targetType) || satisfiesObjectBounds(targetType, withBounds.getTypeBounds());
            }
        }
        return true;
    }

    private boolean satisfiesObjectBounds(ObjectType argument, BaseType bounds) {
        for (Type bound : bounds) {
            if (bound instanceof ObjectType objectBound && !isJavaLangObject(bound) && !typeMaker.isRawTypeAssignable(objectBound, argument)) {
                return false;
            }
        }
        return true;
    }

    private static boolean isTargetDependentGenericInvocation(Expression expression) {
        return expression instanceof ClassFileMethodInvocationExpression methodInvocation
                && methodInvocation.getTypeParameters() != null
                && methodInvocation.getUnboundType() != null
                && !Collections.disjoint(methodInvocation.getUnboundType().findTypeParametersInType(), getMethodTypeParameterNames(methodInvocation))
                && containsFunctionalExpression(methodInvocation.getParameters())
                && !hasProperMethodArgumentConstraint(methodInvocation);
    }

    private static boolean hasProperMethodArgumentConstraint(ClassFileMethodInvocationExpression invocation) {
        return hasProperMethodArgumentConstraint(invocation, getMethodTypeParameterNames(invocation));
    }

    /**
     * @param typeVariables the type variables whose inference is asked about ('compose(not(in(c)), Entry::getKey)' constrains B, not A)
     */
    private static boolean hasProperMethodArgumentConstraint(ClassFileMethodInvocationExpression invocation, Set<String> typeVariables) {
        BaseType parameterTypes = invocation.getUnboundParameterTypes();
        BaseExpression arguments = invocation.getParameters();
        Set<String> methodTypeParameters = new HashSet<>(getMethodTypeParameterNames(invocation));

        methodTypeParameters.retainAll(typeVariables);

        if (parameterTypes != null && arguments != null) {
            var parameterTypeIterator = parameterTypes.iterator();
            var argumentIterator = arguments.iterator();

            while (parameterTypeIterator.hasNext() && argumentIterator.hasNext()) {
                Type parameterType = parameterTypeIterator.next();
                Expression argument = argumentIterator.next();
                if (!Collections.disjoint(parameterType.findTypeParametersInType(), methodTypeParameters)
                        && !argument.isNullExpression()
                        && (!isFunctionalExpression(argument) || hasConstrainingFunctionalResult(argument))
                        && !isTargetDependentGenericInvocation(argument)) {
                    return true;
                }
            }
        }
        return false;
    }

    private static boolean hasConstrainingFunctionalResult(Expression expression) {
        if (!(expression instanceof LambdaIdentifiersExpression lambda) || lambda.getStatements() == null) {
            return false;
        }
        BaseStatement statements = lambda.getStatements();
        if (statements.isLambdaExpressionStatement()) {
            return !statements.getExpression().isNullExpression();
        }
        NonNullReturnExpressionVisitor visitor = new NonNullReturnExpressionVisitor();
        statements.accept(visitor);
        return visitor.found;
    }

    private static final class NonNullReturnExpressionVisitor extends AbstractJavaSyntaxVisitor {
        private boolean found;

        @Override
        public void visit(ReturnExpressionStatement statement) {
            found |= !statement.getExpression().isNullExpression();
        }

        @Override
        public void visit(LambdaIdentifiersExpression expression) {
            // A nested lambda has its own target type and must not constrain the enclosing lambda's result.
        }
    }

    private static boolean isFunctionalExpression(Expression expression) {
        return expression instanceof ConstructorReferenceExpression
                || expression instanceof LambdaIdentifiersExpression
                || expression instanceof MethodReferenceExpression && !(expression instanceof MethodInvocationExpression);
    }

    private static boolean containsFunctionalExpression(BaseExpression parameters) {
        if (parameters == null) {
            return false;
        }
        for (Expression parameter : parameters) {
            if (isFunctionalExpression(parameter)) {
                return true;
            }
        }
        return false;
    }

    /** @return the name and the descriptor of the method which a bridge method calls, null if it cannot be found */
    private static String bridgedMethod(org.apache.bcel.classfile.Method bridge) {
        if (bridge.getCode() == null) {
            return null;
        }

        ConstantPoolGen constants = new ConstantPoolGen(bridge.getConstantPool());

        for (InstructionHandle handle : new InstructionList(bridge.getCode().getCode()).getInstructionHandles()) {
            if (handle.getInstruction() instanceof InvokeInstruction invocation) {
                return invocation.getName(constants) + invocation.getSignature(constants);
            }
        }
        return null;
    }

    private static String packageOf(String internalName) {
        int index = internalName.lastIndexOf('/');

        return index < 0 ? "" : internalName.substring(0, index);
    }

    private boolean declaresNewMethod(ClassFileBodyDeclaration body, ObjectType superType) {
        if (body.getMethodDeclarations() == null) {
            return false;
        }

        // The methods which a bridge method of the class delegates to override a generic method
        Set<String> bridgedMethods = body.getMethodDeclarations().stream()
                .filter(declaration -> (declaration.getFlags() & ACC_BRIDGE) != 0)
                .map(declaration -> bridgedMethod(declaration.getMethod()))
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());

        for (ClassFileConstructorOrMethodDeclaration declaration : body.getMethodDeclarations()) {
            org.apache.bcel.classfile.Method method = declaration.getMethod();

            if ((declaration.getFlags() & (ACC_PRIVATE | ACC_SYNTHETIC | ACC_BRIDGE)) == 0 && method.getName().charAt(0) != '<'
                    && !overridesInheritedMethod(method, superType, bridgedMethods, packageOf(body.getInternalTypeName()))) {
                return true;
            }
        }
        return false;
    }

    private boolean overridesInheritedMethod(org.apache.bcel.classfile.Method method, ObjectType superType, Set<String> bridgedMethods, String inheritingPackage) {
        if (typeMaker.matchCount(superType.getInternalName(), method.getName(), method.getArgumentTypes().length, false) == 0) {
            return false;
        }
        if (inheritedMethodFinder == null || bridgedMethods.contains(method.getName() + method.getSignature())) {
            // (a bridge method is the proof that a method overrides one whose parameter types are generic)
            return true;
        }

        Set<String> inherited = inheritedMethodFinder.parameterDescriptors(superType.getInternalName(), method.getName(), inheritingPackage);
        String signature = method.getSignature();

        // Unknown inherited methods: the name and the number of parameters decide
        return inherited == null || inherited.contains(signature.substring(0, signature.indexOf(')') + 1));
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
        } else if (parameters instanceof Expression expr) {
            parameter = expr;
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

    private boolean hasKnownTypeParameters(BaseTypeArgument type) {
        Set<String> genericIdentifiersInType = type.findTypeParametersInType();
        Set<String> genericIdentifiersInScope = findKnownTypeParameters();
        return genericIdentifiersInScope.containsAll(genericIdentifiersInType);
    }

    private Set<String> findKnownTypeParameters() {
        Set<String> genericIdentifiers = new HashSet<>();
        // (the scopes are iterated from the innermost one)
        int index = typeParameters.size() - 1;
        for (TypeParameter baseTypeParameters : typeParameters) {
            if (index-- >= staticBoundary || baseTypeParameters.methodLevel()) {
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
