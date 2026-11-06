/*
 * Copyright (c) 2026 Nicolas Baumann (@nbauma109), with Claude Sonnet 5.5.
 * This project is distributed under the GPLv3 license.
 * This is a Copyleft license that gives the user the right to use,
 * copy and modify the code freely for non-commercial purposes.
 */
package org.jd.core.v1.service.converter.classfiletojavasyntax.visitor;

import org.apache.bcel.classfile.Method;
import org.apache.bcel.generic.Type;
import org.jd.core.v1.api.loader.Loader;
import org.jd.core.v1.model.javasyntax.AbstractJavaSyntaxVisitor;
import org.jd.core.v1.model.javasyntax.statement.BaseStatement;
import org.jd.core.v1.model.javasyntax.statement.ForStatement;
import org.jd.core.v1.model.javasyntax.declaration.LocalVariableDeclaration;
import org.jd.core.v1.model.javasyntax.declaration.BaseFormalParameter;
import org.jd.core.v1.model.javasyntax.declaration.BaseLocalVariableDeclarator;
import org.jd.core.v1.model.javasyntax.declaration.BodyDeclaration;
import org.jd.core.v1.model.javasyntax.declaration.ConstructorDeclaration;
import org.jd.core.v1.model.javasyntax.declaration.ExpressionVariableInitializer;
import org.jd.core.v1.model.javasyntax.declaration.FieldDeclarator;
import org.jd.core.v1.model.javasyntax.declaration.FormalParameter;
import org.jd.core.v1.model.javasyntax.declaration.LocalVariableDeclarator;
import org.jd.core.v1.model.javasyntax.declaration.LocalVariableDeclarators;
import org.jd.core.v1.model.javasyntax.declaration.MethodDeclaration;
import org.jd.core.v1.model.javasyntax.declaration.VariableInitializer;
import org.jd.core.v1.model.javasyntax.expression.BaseExpression;
import org.jd.core.v1.model.javasyntax.expression.BinaryOperatorExpression;
import org.jd.core.v1.model.javasyntax.expression.CastExpression;
import org.jd.core.v1.model.javasyntax.expression.Expression;
import org.jd.core.v1.model.javasyntax.expression.FieldReferenceExpression;
import org.jd.core.v1.model.javasyntax.expression.SwitchExpression;
import org.jd.core.v1.model.javasyntax.expression.TernaryOperatorExpression;
import org.jd.core.v1.model.javasyntax.expression.ThisExpression;
import org.jd.core.v1.model.javasyntax.expression.LambdaIdentifiersExpression;
import org.jd.core.v1.model.javasyntax.expression.LocalVariableReferenceExpression;
import org.jd.core.v1.model.javasyntax.expression.MethodInvocationExpression;
import org.jd.core.v1.model.javasyntax.expression.MethodReferenceExpression;
import org.jd.core.v1.model.javasyntax.expression.NewExpression;
import org.jd.core.v1.model.javasyntax.expression.NewInitializedArray;
import org.jd.core.v1.model.javasyntax.statement.ForEachStatement;
import org.jd.core.v1.model.javasyntax.statement.LocalVariableDeclarationStatement;
import org.jd.core.v1.model.javasyntax.statement.Statement;
import org.jd.core.v1.model.javasyntax.statement.Statements;
import org.jd.core.v1.model.javasyntax.statement.YieldExpressionStatement;
import org.jd.core.v1.model.javasyntax.statement.ThrowStatement;
import org.jd.core.v1.model.javasyntax.statement.TryStatement;
import org.jd.core.v1.model.javasyntax.type.GenericType;
import org.jd.core.v1.model.javasyntax.type.BaseType;
import org.jd.core.v1.model.javasyntax.type.BaseTypeArgument;
import org.jd.core.v1.model.javasyntax.type.ObjectType;
import org.jd.core.v1.model.javasyntax.type.TypeArgument;
import org.jd.core.v1.model.javasyntax.type.TypeArguments;
import org.jd.core.v1.model.javasyntax.type.WildcardExtendsTypeArgument;
import org.jd.core.v1.service.converter.classfiletojavasyntax.model.javasyntax.declaration.ClassFileBodyDeclaration;
import org.jd.core.v1.service.converter.classfiletojavasyntax.model.javasyntax.declaration.ClassFileFieldDeclaration;
import org.jd.core.v1.service.converter.classfiletojavasyntax.model.javasyntax.declaration.ClassFileConstructorOrMethodDeclaration;
import org.jd.core.v1.service.converter.classfiletojavasyntax.model.javasyntax.expression.ClassFileMethodInvocationExpression;
import org.jd.core.v1.service.converter.classfiletojavasyntax.model.javasyntax.expression.ClassFileConstructorReferenceExpression;
import org.jd.core.v1.service.converter.classfiletojavasyntax.model.javasyntax.expression.ClassFileMethodReferenceExpression;
import org.jd.core.v1.service.converter.classfiletojavasyntax.model.javasyntax.expression.ClassFileNewExpression;
import org.jd.core.v1.service.converter.classfiletojavasyntax.util.ClassMetadata;
import org.jd.core.v1.service.converter.classfiletojavasyntax.util.LocalVariableMaker;
import org.jd.core.v1.service.converter.classfiletojavasyntax.util.TypeMaker;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Deque;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.function.Consumer;
import java.util.function.Predicate;
import java.util.stream.Stream;

/**
 * The type of an anonymous class cannot be written: 'var printer = new Object() { void println(String s) {...} }; printer.println("...")' is the
 * only way to call a member which the anonymous class adds to its supertype. A local variable initialized by such an instance and used to call
 * one of these members is declared 'var'.
 */
public class AnonymousClassVarVisitor extends AbstractJavaSyntaxVisitor {
    private final TypeMaker typeMaker;
    private final Loader loader;
    /** The class which contains the code: its package is the one from which the protected members of the super types are accessible */
    private String internalTypeName;

    public AnonymousClassVarVisitor(TypeMaker typeMaker, Loader loader) {
        this.typeMaker = typeMaker;
        this.loader = loader;
    }
    public void setInternalTypeName(String internalTypeName) {
        this.internalTypeName = internalTypeName;
    }

    private static String packageOf(String internalName) {
        return internalName == null || internalName.lastIndexOf('/') < 0 ? "" : internalName.substring(0, internalName.lastIndexOf('/'));
    }

    /** A type variable is written by its name only: no import */
    private static final GenericType VAR = new GenericType("var");
    private static final String INIT = "<init>";
    private static final String RUNTIME_EXCEPTION = "java/lang/RuntimeException";
    private static final String CLONE = "clone()Ljava/lang/Object;";
    private static final String FINALIZE = "finalize()V";
    /** The methods of Object, by name and descriptor: an overload of one of them is a new method, an override is not */
    private static final Set<String> OBJECT_METHODS = Set.of("toString()Ljava/lang/String;", "equals(Ljava/lang/Object;)Z", "hashCode()I", CLONE, FINALIZE);

    private static final String CLOSE = "close";

    private static final String THROWABLE = "java/lang/Throwable";

    private static final Map<String, Set<String>> OBJECT_METHOD_EXCEPTIONS = Map.of(CLONE, Set.of("java/lang/CloneNotSupportedException"),
            FINALIZE, Set.of(THROWABLE));

    private static final Set<String> PROTECTED_OBJECT_METHODS = Set.of(CLONE, FINALIZE);

    /** The variables which are declared 'var' because of their anonymous class, with the members which it adds: 'var c = b;' has the type of 'b' */
    private final Map<String, Set<String>> anonymousVariables = new HashMap<>();
    /** The internal name of the anonymous class of the variables which are known to hold one directly or through an alias */
    private final Map<String, String> anonymousClassNames = new HashMap<>();

    /** The parameters of the lambdas which hold the type of a tracked variable as a type argument ('xs' of 'ToIntFunction<List<T>>'), with the members which it adds */
    private final Map<String, Set<String>> anonymousContainers = new HashMap<>();

    /** The type of each of these parameters and the type variable of the tracked variable which it holds: 'Ljava/util/Map<TT;LOther;>;' holds 'TT;' */
    private final Map<String, Container> containerShapes = new HashMap<>();

    private record Container(String type, String variable) {}

    /** The 'try' statements around the code being visited, whose handlers can catch what the resources close throw */
    private final Deque<TryStatement> enclosingTries = new ArrayDeque<>();

    /** The internal names of the exceptions which the method or the lambda being visited may throw; empty while none is visited */
    private Set<String> permittedExceptions = Set.of();

    /** The bounds of the type variables of the method being visited: a thrown 'E extends IOException' is an 'IOException' */
    private Map<String, BaseType> typeBounds = Map.of();

    @Override
    public void visit(MethodDeclaration declaration) {
        Map<String, BaseType> saved = typeBounds;

        typeBounds = boundsOf(declaration);
        inExecutable(namesOf(declaration.getExceptionTypes()), () -> super.visit(declaration));
        typeBounds = saved;
    }

    @Override
    public void visit(ConstructorDeclaration declaration) {
        Map<String, BaseType> saved = typeBounds;

        typeBounds = boundsOf(declaration);
        inExecutable(namesOf(declaration.getExceptionTypes()), () -> super.visit(declaration));
        typeBounds = saved;
    }

    private static Map<String, BaseType> boundsOf(Object declaration) {
        return declaration instanceof ClassFileConstructorOrMethodDeclaration classFile && classFile.getTypeBounds() != null ? classFile.getTypeBounds() : Map.of();
    }

    /** The body of a lambda is run later: neither the handlers around it nor the throws clause of its method protect it, the one of its functional method does */
    @Override
    public void visit(LambdaIdentifiersExpression lambda) {
        Set<String> permitted = lambda.getType() instanceof ObjectType functional ? thrownByTheFunctionalMethod(functional) : Set.of();

        inExecutable(permitted, () -> super.visit(lambda));
    }

    /** @return the exceptions which the functional method of the target of a lambda throws, where its type variables are the type arguments of the target ('Task&lt;IOException&gt;') */
    private Set<String> thrownByTheFunctionalMethod(ObjectType functional) {
        String name = functional.getInternalName();
        List<String> typeParameters = ClassMetadata.typeParameterNames(loader, name);
        List<TypeArgument> typeArguments = functional.getTypeArguments() == null ? List.of() : typeArgumentList(functional.getTypeArguments());
        Set<String> thrown = new HashSet<>();

        Map<String, String> variableBounds = new HashMap<>();
        List<String> targetTokens = targetTokens(typeParameters, typeArguments, variableBounds);

        for (String token : ClassMetadata.functionalMethodThrown(loader, name, targetTokens, variableBounds)) {
            if (!token.startsWith("T")) {
                thrown.add(token.substring(1, token.length() - 1));
            } else if (variableBounds.containsKey(token.substring(1, token.length() - 1))) {
                // 'Task<E>' with 'E extends IOException' permits 'E' itself, not every 'IOException'
                thrown.add(token);
            } else {
                // The type argument is unknown: the erased bound of the variable is what the method may throw at most
                thrown.addAll(ClassMetadata.functionalMethodExceptions(loader, name));
            }
        }
        return thrown;
    }

    /**
     * @return the signature tokens of the type arguments of a target for the type parameters of its interface: a class, the variable of the enclosing declaration which is a type
     * argument (with its bound added to the map), or the type parameter itself if the argument is unknown
     */
    private List<String> targetTokens(List<String> typeParameters, List<TypeArgument> typeArguments, Map<String, String> variableBounds) {
        List<String> tokens = new ArrayList<>();

        for (int i = 0; i < typeParameters.size(); i++) {
            String argument = i < typeArguments.size() ? nameOf(typeArguments.get(i)) : null;

            if (argument == null) {
                tokens.add("T" + typeParameters.get(i) + ";");
            } else if (isVariable(argument)) {
                addBoundChain(argument, variableBounds);
                tokens.add(argument);
            } else {
                tokens.add("L" + argument + ";");
            }
        }
        return tokens;
    }

    private static List<TypeArgument> typeArgumentList(BaseTypeArgument typeArguments) {
        return typeArguments.isTypeArgumentList() ? typeArguments.getTypeArgumentList() : List.of(typeArguments.getTypeArgumentFirst());
    }

    /** @return the names of the exceptions which a method declares, a type variable ('throws E' with 'E extends IOException') by its first bound */
    private Set<String> namesOf(BaseType exceptions) {
        Set<String> names = new HashSet<>();

        if (exceptions != null) {
            for (org.jd.core.v1.model.javasyntax.type.Type type : exceptions) {
                String name = nameOf(type);

                if (name != null) {
                    names.add(name);
                }
            }
        }
        return names;
    }

    /** The handlers around a method, a constructor or a lambda do not protect its body: it is run elsewhere */
    private void inExecutable(Set<String> permitted, Runnable visit) {
        Set<String> saved = permittedExceptions;
        Deque<TryStatement> around = new ArrayDeque<>(enclosingTries);

        permittedExceptions = permitted;
        enclosingTries.clear();
        visit.run();
        permittedExceptions = saved;
        enclosingTries.clear();
        enclosingTries.addAll(around);
    }

    /**
     * @return the internal name of the type which the type variable of the class which declares the method stands for: a type argument of the type which the method is called
     * on, or the one which this type inherits it with ('IOThrower extends Thrower&lt;IOException&gt;'); null if there is none
     */
    private String typeVariableOf(String typeVariable, String owner, String name, String descriptor, org.jd.core.v1.model.javasyntax.type.Type receiverType) {
        String declaring = ClassMetadata.declaringClass(loader, owner, name, descriptor).orElse(owner);
        String inTheTermsOfTheOwner = typeVariable;

        if (!declaring.equals(owner)) {
            List<String> ancestor = ClassMetadata.ancestorTypeArguments(loader, owner, declaring).orElse(List.of());
            int declared = ClassMetadata.typeParameterNames(loader, declaring).indexOf(typeVariable.substring(1, typeVariable.length() - 1));

            if (declared < 0 || declared >= ancestor.size()) {
                return null;
            }
            inTheTermsOfTheOwner = ancestor.get(declared);
            if (inTheTermsOfTheOwner.startsWith("L")) {
                int generic = inTheTermsOfTheOwner.indexOf('<');

                return inTheTermsOfTheOwner.substring(1, generic < 0 ? inTheTermsOfTheOwner.length() - 1 : generic);
            }
        }

        int position = ClassMetadata.typeParameterNames(loader, owner).indexOf(inTheTermsOfTheOwner.substring(1, inTheTermsOfTheOwner.length() - 1));

        if (position < 0 || !(receiverType instanceof ObjectType receiver) || receiver.getTypeArguments() == null) {
            return null;
        }

        List<TypeArgument> receiverArguments = typeArgumentList(receiver.getTypeArguments());

        return position < receiverArguments.size() ? nameOf(receiverArguments.get(position)) : null;
    }

    /**
     * @return the internal name of a type argument; for a type variable which has a bound ('Closer&lt;E&gt;' with 'E extends IOException') the token 'TE;', which stands for the
     * variable and not for its bound: 'throws E' does not permit every 'IOException'. Null if unknown.
     */
    private String nameOf(TypeArgument argument) {
        if (argument instanceof GenericType generic && typeBounds.get(generic.getName()) != null) {
            return "T" + generic.getName() + ";";
        }
        return argument instanceof ObjectType type ? type.getInternalName() : null;
    }

    private static boolean isVariable(String exception) {
        return exception.startsWith("T") && exception.endsWith(";");
    }

    /** @return the internal name of the first bound of the type variable which the token stands for, the name itself for a class */
    private String boundOf(String exception) {
        if (!isVariable(exception)) {
            return exception;
        }

        String name = classBoundOf(exception.substring(1, exception.length() - 1));

        return name != null ? name : "java/lang/Exception";
    }

    /** Records the bound of the variable and of the variables which bound it in turn ('E extends X', 'X extends IOException'), even those which are no type argument */
    private void addBoundChain(String variable, Map<String, String> variableBounds) {
        String current = variable;

        while (current != null && !variableBounds.containsKey(current.substring(1, current.length() - 1))) {
            String bound = immediateBoundOf(current);

            variableBounds.put(current.substring(1, current.length() - 1), bound);
            current = isVariable(bound) ? bound : null;
        }
    }

    /** @return the bound of the variable as the metadata expects it: the token of the variable which bounds it directly ('TX;' for 'E extends X'), else the internal name of its class bound */
    private String immediateBoundOf(String variable) {
        BaseType bound = typeBounds.get(variable.substring(1, variable.length() - 1));

        return bound != null && bound.getFirst() instanceof GenericType generic ? "T" + generic.getName() + ";" : boundOf(variable);
    }

    /** @return the internal name of the class which bounds the type variable, found through the chain of variables which bound each other ('E extends X', 'X extends IOException'); null if unknown */
    private String classBoundOf(String variable) {
        Set<String> visited = new HashSet<>();
        String current = variable;

        while (visited.add(current)) {
            BaseType bound = typeBounds.get(current);

            if (bound == null) {
                return null;
            }
            if (bound.getFirst() instanceof ObjectType first) {
                return first.getInternalName();
            }
            if (!(bound.getFirst() instanceof GenericType generic)) {
                return null;
            }
            current = generic.getName();
        }
        return null;
    }

    /** @return what the closes which an interface inherits all allow, with the type arguments of the declared type substituted ('C extends A, B'); empty if it inherits one at most */
    private Optional<Set<String>> inheritedCloseExceptions(ObjectType declared) {
        String owner = declared.getInternalName();
        Map<String, String> variableBounds = new HashMap<>();
        List<TypeArgument> arguments = declared.getTypeArguments() == null ? List.of() : typeArgumentList(declared.getTypeArguments());
        Optional<List<String>> inherited = ClassMetadata.inheritedCloseThrown(loader, owner,
                targetTokens(ClassMetadata.typeParameterNames(loader, owner), arguments, variableBounds), variableBounds);

        if (inherited.isEmpty()) {
            return Optional.empty();
        }

        Set<String> allowed = new HashSet<>();

        for (String token : inherited.get()) {
            if (token.startsWith("L")) {
                allowed.add(token.substring(1, token.length() - 1));
            } else if (variableBounds.containsKey(token.substring(1, token.length() - 1))) {
                allowed.add(token);
            } else {
                // The type argument is unknown: the erased bound is what it throws at most
                allowed.addAll(ClassMetadata.declaredExceptions(loader, owner, CLOSE, "()V"));
            }
        }
        return Optional.of(allowed);
    }

    /** @return the exceptions which the close of the declared type of a resource throws, where the type variable of its signature is a type argument of the type ('Closer&lt;IOException&gt;') */
    private Set<String> closeExceptions(ObjectType declared) {
        String owner = declared.getInternalName();
        Optional<Set<String>> intersected = inheritedCloseExceptions(declared);

        if (intersected.isPresent()) {
            return intersected.get();
        }

        String signature = ClassMetadata.declaredGenericSignature(loader, owner, CLOSE, "()V");

        if (signature == null || signature.indexOf('^') < 0) {
            return ClassMetadata.declaredExceptions(loader, owner, CLOSE, "()V");
        }

        Set<String> names = new HashSet<>();

        for (String token : signature.substring(signature.indexOf('^') + 1).split("\\^")) {
            String resolved = token.startsWith("T") ? typeVariableOf(token, owner, CLOSE, "()V", declared) : token.substring(1, token.length() - 1);

            if (resolved == null) {
                // The type argument is unknown: the erased bound is what it throws at most
                names.addAll(ClassMetadata.declaredExceptions(loader, owner, CLOSE, "()V"));
            } else {
                names.add(resolved);
            }
        }
        return names;
    }

    /**
     * @return true if the exceptions which the close of the declared type of the resource throws are unchecked or permitted by the method: the explicit type is valid then,
     * the implicit close needs no narrower type
     */
    private boolean methodCoversTheCloseOf(TryStatement statement, TryStatement.Resource resource) {
        return resource.getType() instanceof ObjectType declared && closeIsPermitted(statement, declared);
    }

    /** @return true if the exceptions which the close of the type throws are unchecked, declared, or caught by the handlers of the statement or the ones around it */
    private boolean closeIsPermitted(TryStatement statement, ObjectType declaredType) {
        return closeIsPermitted(statement, declaredType, List.of());
    }

    /** @param nested the 'try' statements between the code being visited and the statement, which the visit has not pushed: a search which looks ahead meets them first */
    private boolean closeIsPermitted(TryStatement statement, ObjectType declaredType, Collection<TryStatement> nested) {
        for (String exception : closeExceptions(declaredType)) {
            String bound = boundOf(exception);

            if (!isUnchecked(bound) && !declaredByTheMethod(exception) && !caughtAround(bound) && !caughtBy(statement, bound)
                    && nested.stream().noneMatch(around -> caughtBy(around, bound))) {
                return false;
            }
        }
        return true;
    }

    private boolean isUnchecked(String exception) {
        return ClassMetadata.isSubtype(loader, exception, RUNTIME_EXCEPTION) || ClassMetadata.isSubtype(loader, exception, "java/lang/Error");
    }

    /** @return true if a handler of a 'try' around the code catches the exception */
    private boolean caughtAround(String exception) {
        return enclosingTries.stream().anyMatch(around -> caughtBy(around, exception));
    }

    private boolean caughtBy(TryStatement statement, String exception) {
        return statement.getCatchClauses() != null && statement.getCatchClauses().stream().anyMatch(clause -> catches(clause, exception));
    }

    /** @return true if the exception is the permitted type variable, or a type variable bounded by it through the chain of variables ('E extends X' is permitted by 'throws X') */
    private boolean isBoundedBy(String exception, String permitted) {
        Set<String> visited = new HashSet<>();
        String current = exception;

        while (isVariable(current) && visited.add(current)) {
            if (current.equals(permitted)) {
                return true;
            }

            BaseType bound = typeBounds.get(current.substring(1, current.length() - 1));

            if (bound == null || !(bound.getFirst() instanceof GenericType next)) {
                return false;
            }
            current = "T" + next.getName() + ";";
        }
        return false;
    }

    private boolean declaredByTheMethod(String exception) {
        // A type variable is permitted by itself, or by a class which its bound is a sub type of; a class is not permitted by a type variable
        String bound = boundOf(exception);

        return permittedExceptions.stream().anyMatch(permitted -> isVariable(permitted) ? isBoundedBy(exception, permitted) : ClassMetadata.isSubtype(loader, bound, permitted));
    }

    @Override
    public void visit(Statements statements) {
        // The variables of a block are known up to its end only: another block may reuse their names
        Set<String> knownBefore = new HashSet<>(anonymousVariables.keySet());
        Set<String> containersBefore = new HashSet<>(anonymousContainers.keySet());

        for (int i = 0; i < statements.size(); i++) {
            if (statements.get(i) instanceof LocalVariableDeclarationStatement declaration && needsOneDeclaratorEach(declaration)) {
                split(statements, i, declaration);
            }
            if (statements.get(i) instanceof LocalVariableDeclarationStatement declaration
                    && (isDeclaredWithVar(declaration, statements, i + 1) || isAliasWhichNeedsVar(declaration, statements, i + 1)
                    || isContainerElementWhichNeedsVar(declaration, statements, i + 1))) {
                statements.set(i, new LocalVariableDeclarationStatement(VAR, declaration.getLocalVariableDeclarators()));
            }
            statements.get(i).accept(this);
        }
        anonymousVariables.keySet().retainAll(knownBefore);
        anonymousContainers.keySet().retainAll(containersBefore);
        containerShapes.keySet().retainAll(containersBefore);
    }

    /** 'var' declares one variable: 'Object a = new Object() {...}, b = new Object() {...};' or 'Object b = a, c = a;' which was merged is split */
    private boolean needsOneDeclaratorEach(LocalVariableDeclarationStatement declaration) {
        return declaration.getLocalVariableDeclarators() instanceof LocalVariableDeclarators declarators
                && declarators.stream().anyMatch(declarator -> isAnonymousClassInstance(declarator) || isAliasOfAnonymousVariable(declarator));
    }

    private static void split(Statements statements, int index, LocalVariableDeclarationStatement declaration) {
        List<Statement> split = new ArrayList<>();

        for (LocalVariableDeclarator declarator : declaration.getLocalVariableDeclarators()) {
            split.add(new LocalVariableDeclarationStatement(declaration.getType(), declarator));
        }
        statements.remove(index);
        statements.addAll(index, split);
    }

    private boolean isAliasOfAnonymousVariable(LocalVariableDeclarator declarator) {
        return declarator.getVariableInitializer() instanceof ExpressionVariableInitializer initializer && references(initializer.getExpression(), anonymousVariables.keySet()) != null;
    }

    private boolean isAnonymousClassInstance(LocalVariableDeclarator declarator) {
        return declarator.getVariableInitializer() instanceof ExpressionVariableInitializer initializer && anonymousCreation(initializer.getExpression()) != null;
    }

    /** 'Object y = xs.get(0);' in a lambda whose 'xs' holds the type of a tracked variable: 'y.added()' needs the type of the anonymous class */
    private boolean isContainerElementWhichNeedsVar(LocalVariableDeclarationStatement declaration, List<Statement> statements, int nextIndex) {
        if (anonymousContainers.isEmpty() || !(declaration.getLocalVariableDeclarators() instanceof LocalVariableDeclarator declarator) || declarator.getDimension() != 0
                || !(declarator.getVariableInitializer() instanceof ExpressionVariableInitializer initializer)
                || !(initializer.getExpression() instanceof MethodInvocationExpression element) || !(element.getExpression() instanceof LocalVariableReferenceExpression container)
                || !anonymousContainers.containsKey(container.getName()) || !returnsATypeVariableOfItsClass(element, containerShapes.get(container.getName()))) {
            return false;
        }

        Set<String> members = anonymousContainers.get(container.getName());
        MemberUseSearch search = new MemberUseSearch(declarator.getName(), members);

        for (int i = nextIndex; i < statements.size() && !search.found; i++) {
            statements.get(i).accept(search);
        }
        if (search.found) {
            anonymousVariables.put(declarator.getName(), members);
        }
        return search.found;
    }

    /** 'Object c = b;' keeps its type, 'c.added()' needs the type of the anonymous class of 'b' */
    private boolean isAliasWhichNeedsVar(LocalVariableDeclarationStatement declaration, List<Statement> statements, int nextIndex) {
        return aliasFeedsAnAnonymousCast(declaration, statements, nextIndex) || aliasNeedsVar(declaration.getLocalVariableDeclarators(), search -> {
            for (int i = nextIndex; i < statements.size() && !search.found; i++) {
                statements.get(i).accept(search);
            }
        });
    }

    /** The alias has the anonymous class of its origin, when it is known */
    private void rememberClassName(String alias, String origin) {
        String className = anonymousClassNames.get(origin);

        if (className != null) {
            anonymousClassNames.put(alias, className);
        }
    }

    private boolean aliasNeedsVar(BaseLocalVariableDeclarator declarators, Consumer<MemberUseSearch> uses) {
        if (!(declarators instanceof LocalVariableDeclarator declarator) || declarator.getDimension() != 0
                || !(declarator.getVariableInitializer() instanceof ExpressionVariableInitializer initializer)
                || references(initializer.getExpression(), anonymousVariables.keySet()) == null) {
            return false;
        }

        String origin = references(initializer.getExpression(), anonymousVariables.keySet());
        Set<String> members = anonymousVariables.get(origin);
        MemberUseSearch search = new MemberUseSearch(declarator.getName(), members);

        // The alias gives the type of its origin to a lambda as well
        search.anonymousName = anonymousClassNames.get(origin);
        search.lambdaAdaptation = search.anonymousName != null;
        uses.accept(search);
        if (search.found) {
            anonymousVariables.put(declarator.getName(), members);
            if (search.anonymousName != null) {
                anonymousClassNames.put(declarator.getName(), search.anonymousName);
            }
        }
        return search.found;
    }

    /**
     * 'var origin = new Object() {}; var value = bad(origin);' where the byte code casts the result of 'bad' to the anonymous class: 'origin' has to give its type to
     * the call, otherwise the runtime check is lost.
     */
    private boolean feedsAnAnonymousCast(LocalVariableDeclarationStatement declaration, List<Statement> statements, int nextIndex) {
        if (!(declaration.getLocalVariableDeclarators() instanceof LocalVariableDeclarator declarator) || declarator.getDimension() != 0
                || !(declarator.getVariableInitializer() instanceof ExpressionVariableInitializer initializer)
                || !(initializer.getExpression() instanceof ClassFileNewExpression creation) || !(creation.getBodyDeclaration() instanceof ClassFileBodyDeclaration)) {
            return false;
        }
        boolean found = dependsOnAnAnonymousCast(declarator.getName(), remaining(statements, nextIndex));
        if (found) {
            ClassFileBodyDeclaration body = (ClassFileBodyDeclaration) creation.getBodyDeclaration();

            anonymousVariables.put(declarator.getName(), addedMembers(body, creation.getObjectType().getInternalName()));
            anonymousClassNames.put(declarator.getName(), body.getInternalTypeName());
        }
        return found;
    }

    /**
     * 'for (var v = new Object() {}; ...; ...) { bad(v) }' or 'for (var v = origin; ...)': the condition, the update and the body of the loop are searched like the statements
     * which follow a declaration
     */
    private boolean feedsAnAnonymousCastInTheLoop(LocalVariableDeclaration declaration, ForStatement loop) {
        if (!(declaration.getLocalVariableDeclarators() instanceof LocalVariableDeclarator declarator) || declarator.getDimension() != 0
                || !(declarator.getVariableInitializer() instanceof ExpressionVariableInitializer initializer)) {
            return false;
        }

        Set<String> members = membersOfTheInitializer(initializer.getExpression());

        if (members == null) {
            return false;
        }

        boolean found = castDependencyIn(Set.of(declarator.getName()), visitor -> {
            Stream.of(loop.getCondition(), loop.getUpdate()).filter(Objects::nonNull).forEach(expression -> expression.accept(visitor));
            Stream.of(loop.getStatements()).filter(Objects::nonNull).forEach(statements -> statements.accept(visitor));
        }) != null;

        if (found) {
            anonymousVariables.put(declarator.getName(), members);
        }
        return found;
    }

    /** @return the members which the anonymous class of a creation adds, or the ones of the tracked variable which an alias copies; null if the initializer is none of them */
    private Set<String> membersOfTheInitializer(Expression initializer) {
        if (initializer instanceof ClassFileNewExpression creation && creation.getBodyDeclaration() instanceof ClassFileBodyDeclaration body) {
            return addedMembers(body, creation.getObjectType().getInternalName());
        }

        String tracked = references(initializer, anonymousVariables.keySet());

        return tracked == null ? null : anonymousVariables.get(tracked);
    }

    /** The parameters of the method itself: the ones of the methods of its local classes hide the variable in those methods only */
    private static Set<String> parameterNames(BaseFormalParameter parameters) {
        Set<String> names = new HashSet<>();

        if (parameters != null) {
            parameters.accept(new AbstractJavaSyntaxVisitor() {
                @Override
                public void visit(FormalParameter parameter) {
                    names.add(parameter.getName());
                }
            });
        }
        return names;
    }


    /**
     * 'var value = bad(origin);', 'value = bad(null);' or 'bad(origin).toString()' anywhere in the statement, nested blocks included: the byte code casts the result to
     * the anonymous class, which the variable (and the aliases of it which the statement declares) has to keep
     */
    private boolean dependsOnAnAnonymousCast(String name, BaseStatement statement) {
        return castDependency(Set.of(name), statement) != null;
    }

    /** @return the name of the variable which gives its anonymous type to a cast-bearing call or which a cast-bearing assignment targets, null if none does */
    private String castDependency(Set<String> variables, BaseStatement statement) {
        return castDependencyIn(variables, (AbstractJavaSyntaxVisitor visitor) -> statement.accept(visitor));
    }

    private String castDependencyIn(Set<String> variables, Consumer<AbstractJavaSyntaxVisitor> target) {
        Set<String> names = new HashSet<>(variables);
        String[] found = {null};
        AbstractJavaSyntaxVisitor search = new AbstractJavaSyntaxVisitor() {
            @Override
            public void visit(LocalVariableDeclarator declarator) {
                String used = declaratorDependency(declarator, names);

                found[0] = used == null ? found[0] : used;
                super.visit(declarator);
            }

            /** 'bad(origin).toString()': the cast-bearing call is used where it stands */
            @Override
            public void visit(MethodInvocationExpression expression) {
                String used = expression instanceof ClassFileMethodInvocationExpression call && call.isAnonymousCast() ? references(call, names) : null;

                found[0] = used == null ? found[0] : used;
                super.visit(expression);
            }

            /** 'value = bad(null);': the target typing gives the anonymous type to the call, whatever its arguments */
            @Override
            public void visit(BinaryOperatorExpression expression) {
                if ("=".equals(expression.getOperator()) && expression.getLeftExpression() instanceof LocalVariableReferenceExpression target && names.contains(target.getName())
                        && hasAnonymousCast(expression.getRightExpression())) {
                    found[0] = target.getName();
                }
                super.visit(expression);
            }

            /** The aliases and the shadowing locals of a block end with it */
            @Override
            public void visit(Statements statements) {
                Set<String> before = new HashSet<>(names);

                super.visit(statements);
                names.clear();
                names.addAll(before);
            }

            @Override
            public void visit(MethodDeclaration declaration) {
                visitWithout(parameterNames(declaration.getFormalParameters()), () -> super.visit(declaration));
            }

            @Override
            public void visit(ConstructorDeclaration declaration) {
                visitWithout(parameterNames(declaration.getFormalParameters()), () -> super.visit(declaration));
            }

            private void visitWithout(Set<String> hidden, Runnable visit) {
                Set<String> removed = new HashSet<>(names);

                removed.retainAll(hidden);
                names.removeAll(removed);
                visit.run();
                names.addAll(removed);
            }
        };

        target.accept(search);
        return found[0];
    }

    /**
     * @return the name of the variable which a cast-bearing declaration uses ('var value = bad(origin);'), null if it is none; a local of the name of a variable hides it, and
     * a declaration with the value of a variable gives an alias, which the names get ('var alias = origin;')
     */
    private String declaratorDependency(LocalVariableDeclarator declarator, Set<String> names) {
        if (names.remove(declarator.getName()) || !(declarator.getVariableInitializer() instanceof ExpressionVariableInitializer value)) {
            return null;
        }

        String used = references(value.getExpression(), names);

        if (used != null && !hasAnonymousCast(value.getExpression())) {
            names.add(declarator.getName());
            return null;
        }
        return used;
    }

    /** 'var alias = origin; return bad(alias).toString();': the alias gives its type to the cast-bearing call, so it is 'var' too */
    private boolean aliasFeedsAnAnonymousCast(LocalVariableDeclarationStatement declaration, List<Statement> statements, int nextIndex) {
        if (!(declaration.getLocalVariableDeclarators() instanceof LocalVariableDeclarator declarator) || declarator.getDimension() != 0
                || !(declarator.getVariableInitializer() instanceof ExpressionVariableInitializer initializer)
                || references(initializer.getExpression(), anonymousVariables.keySet()) == null) {
            return false;
        }
        if (dependsOnAnAnonymousCast(declarator.getName(), remaining(statements, nextIndex))) {
            String origin = references(initializer.getExpression(), anonymousVariables.keySet());

            anonymousVariables.put(declarator.getName(), anonymousVariables.get(origin));
            rememberClassName(declarator.getName(), origin);
            return true;
        }
        return false;
    }

    /** @return the statements from the index on, as one block: the aliases which a statement declares are known by the next ones */
    private static Statements remaining(List<Statement> statements, int nextIndex) {
        Statements rest = new Statements();

        rest.addAll(statements.subList(nextIndex, statements.size()));
        return rest;
    }

    private boolean isDeclaredWithVar(LocalVariableDeclarationStatement declaration, List<Statement> statements, int nextIndex) {
        return feedsAnAnonymousCast(declaration, statements, nextIndex) || needsVar(declaration.getLocalVariableDeclarators(), search -> {
            for (int i = nextIndex; i < statements.size() && !search.found; i++) {
                statements.get(i).accept(search);
            }
        });
    }

    /** 'for (var v = new Object() { int n; }; v.n > 0; v.n--)': the declaration of the loop is searched in its condition, its update and its body */
    @Override
    public void visit(ForStatement statement) {
        LocalVariableDeclaration declaration = statement.getDeclaration();

        Consumer<MemberUseSearch> uses = search -> {
            Stream.of(statement.getCondition(), statement.getUpdate()).filter(Objects::nonNull).forEach(expression -> expression.accept(search));
            Stream.of(statement.getStatements()).filter(Objects::nonNull).forEach(statements -> statements.accept(search));
        };

        if (declaration != null && (needsVar(declaration.getLocalVariableDeclarators(), uses) || aliasNeedsVar(declaration.getLocalVariableDeclarators(), uses)
                || feedsAnAnonymousCastInTheLoop(declaration, statement))) {
            LocalVariableDeclaration varDeclaration = new LocalVariableDeclaration(VAR, declaration.getLocalVariableDeclarators());

            varDeclaration.setFinal(declaration.isFinal());
            statement.setDeclaration(varDeclaration);
        }
        super.visit(statement);
    }

    private boolean needsVar(BaseLocalVariableDeclarator declarators, Consumer<MemberUseSearch> uses) {
        if (!(declarators instanceof LocalVariableDeclarator declarator) || declarator.getDimension() != 0
                || !(declarator.getVariableInitializer() instanceof ExpressionVariableInitializer initializer)) {
            return false;
        }
        return needsVar(declarator.getName(), initializer.getExpression(), uses);
    }

    private boolean needsVar(String name, Expression value, Consumer<MemberUseSearch> uses) {
        ClassFileNewExpression newExpression = anonymousCreation(value);

        if (newExpression == null && hasAnonymousCast(value) && references(value, anonymousVariables.keySet()) != null) {
            // The anonymous type comes through a variable which is 'var'
            String origin = references(value, anonymousVariables.keySet());

            anonymousVariables.put(name, anonymousVariables.get(origin));
            rememberClassName(name, origin);
            return true;
        }
        if (newExpression == null || !(newExpression.getBodyDeclaration() instanceof ClassFileBodyDeclaration body)) {
            return false;
        }

        Set<String> members = addedMembers(body, newExpression.getObjectType().getInternalName());

        if (!(value instanceof ClassFileNewExpression) && hasAnonymousCast(value)) {
            // The type comes through a generic call: the cast to the anonymous class which the byte code holds is a runtime check which the explicit type loses
            anonymousVariables.put(name, members);
            return true;
        }

        MemberUseSearch search = new MemberUseSearch(name, members).declaredAs(newExpression.getObjectType());

        // A lambda which the variable gives its type to is adapted to the anonymous class at run time when it was compiled for it: 'apply(value, x -> "ok")' checks its argument
        search.lambdaAdaptation = true;
        search.anonymousName = body.getInternalTypeName();
        anonymousClassNames.put(name, body.getInternalTypeName());

        uses.accept(search);
        if (search.found) {
            anonymousVariables.put(name, members);
        }
        return search.found;
    }

    /**
     * @return the expressions which give their value, so their type, to the one: itself, the value of a cast to the type of an anonymous class, the
     * branches of a conditional, the arguments of a generic call which returns one of its arguments ('requireNonNull(b)': '&lt;T&gt; T f(T)'); any other
     * call, or cast, gives a type of its own
     */
    private List<Expression> underlyingValues(Expression value) {
        List<Expression> values = new ArrayList<>();

        if (value instanceof CastExpression cast && cast.getType() instanceof ObjectType type && LocalVariableMaker.isAnonymousClassName(type.getInternalName())) {
            values.addAll(underlyingValues(cast.getExpression()));
        } else if (value instanceof TernaryOperatorExpression ternary) {
            values.addAll(underlyingValues(ternary.getTrueExpression()));
            values.addAll(underlyingValues(ternary.getFalseExpression()));
        } else if (value instanceof SwitchExpression switchExpression) {
            values.addAll(switchResults(switchExpression));
        } else if (value instanceof MethodInvocationExpression call) {
            values.addAll(callResults(call));
        } else if (value instanceof NewInitializedArray array) {
            // The elements of an array which a generic method receives for its varargs parameter ('T...') give their type to its result
            for (VariableInitializer element : array.getArrayInitializer()) {
                if (element instanceof ExpressionVariableInitializer initializer) {
                    values.addAll(underlyingValues(initializer.getExpression()));
                }
            }
        } else if (value != null && value.isArrayExpression()) {
            // An element of an array has the type of its elements: 'var y = xs[0];'
            values.addAll(underlyingValues(value.getExpression()));
        } else if (!(value instanceof CastExpression) && !(value instanceof MethodInvocationExpression)) {
            values.add(value);
        }
        return values;
    }

    /** @return the expressions which a switch expression gives: the results of its rules and the values which its blocks yield */
    private List<Expression> switchResults(SwitchExpression switchExpression) {
        List<Expression> results = new ArrayList<>();

        for (SwitchExpression.Rule rule : switchExpression.getRules()) {
            if (rule instanceof SwitchExpression.RuleExpression ruleExpression) {
                results.addAll(underlyingValues(ruleExpression.getExpression()));
            } else if (rule instanceof SwitchExpression.RuleStatement ruleStatement && ruleStatement.getStatements() != null) {
                ruleStatement.getStatements().accept(new AbstractJavaSyntaxVisitor() {
                    @Override
                    public void visit(YieldExpressionStatement statement) {
                        results.addAll(underlyingValues(statement.getExpression()));
                    }
                });
            }
        }
        return results;
    }

    /**
     * @return the expressions which give their type to the result of a generic call: the arguments whose type is the type variable which the method
     * returns ('&lt;T&gt; T f(T first, Object ignored)'), the type arguments of the receiver when the method returns a type variable of its class ('List&lt;E&gt;.get')
     */
    private List<Expression> callResults(MethodInvocationExpression call) {
        List<Expression> results = new ArrayList<>(matchingArguments(call, true, -1));
        String signature = ClassMetadata.declaredGenericSignature(loader, call.getInternalTypeName(), call.getName(), call.getDescriptor());

        if (signature != null && signature.contains(")")) {
            String returned = returnedType(signature);

            if (returned.startsWith("T") && !declaresTypeVariable(signature, returned) && call.getExpression() instanceof MethodInvocationExpression receiver) {
                // The type argument of the receiver which is the one the method returns: 'Pair&lt;K, V&gt;.get()' returns the second
                int typeArgument = ClassMetadata.typeParameterNames(loader, call.getInternalTypeName()).indexOf(returned.substring(1, returned.length() - 1));

                results.addAll(matchingArguments(receiver, false, typeArgument));
            }
        }
        return results;
    }

    /** @return the type arguments of a generic type signature, 'LPair&lt;TK;TV;&gt;;' gives 'TK;', 'TV;' */
    private static List<String> typeArguments(String type) {
        List<String> arguments = new ArrayList<>();
        int start = type.indexOf('<');
        int index = start + 1;

        while (start >= 0 && index < type.lastIndexOf('>')) {
            int end = endOfSignatureType(type, index);

            // The bound of a wildcard is the type of the argument for the lambda: '? super T' gives T
            arguments.add(type.substring(index, end).replaceFirst("^[+-]", ""));
            index = end;
        }
        return arguments;
    }

    /** @return the signatures of the parameters of a generic method signature */
    private static List<String> parameterSignatures(String signature) {
        List<String> parameters = new ArrayList<>();
        int index = signature.indexOf('(') + 1;

        while (index < signature.indexOf(')')) {
            int end = endOfSignatureType(signature, index);

            parameters.add(signature.substring(index, end));
            index = end;
        }
        return parameters;
    }

    private static String returnedType(String signature) {
        // The signature ends with the exceptions which the method throws, '^TE;', if they are type variables
        return signature.substring(signature.indexOf(')') + 1).split("\\^")[0];
    }

    /**
     * @param exact true for the arguments whose type is the returned type ('&lt;T&gt; T f(T)'), false for the ones which the returned type only contains as a
     * type argument ('&lt;E&gt; List&lt;E&gt; of(E)')
     * @param typeArgument the index of the only type argument of the returned type which counts, -1 for all of them
     */
    private List<Expression> matchingArguments(MethodInvocationExpression call, boolean exact, int typeArgument) {
        String signature = ClassMetadata.declaredGenericSignature(loader, call.getInternalTypeName(), call.getName(), call.getDescriptor());
        List<Expression> results = new ArrayList<>();

        if (signature == null || signature.indexOf(')') < 0) {
            return results;
        }

        String returned = returnedType(signature);
        List<String> counted = typeArguments(returned);
        List<Expression> arguments = new ArrayList<>();

        if (call.getParameters() != null) {
            call.getParameters().forEach(arguments::add);
        }

        int index = signature.indexOf('(') + 1;
        int position = 0;

        while (index < signature.indexOf(')')) {
            int end = endOfSignatureType(signature, index);
            String parameter = signature.substring(index, end);
            boolean matches = exact ? returned.equals(parameter) || parameter.equals("[" + returned) : parameter.startsWith("T") && returned.contains(parameter) && !returned.equals(parameter)
                    && (typeArgument < 0 || typeArgument >= counted.size() || counted.get(typeArgument).equals(parameter));

            if (matches && position < arguments.size()) {
                results.addAll(underlyingValues(arguments.get(position)));
            }
            index = end;
            position++;
        }
        return results;
    }

    /** @return true if the type variable 'TE;' is one of the type parameters of the method: '&lt;E:Ljava/lang/Object;&gt;(TE;)...' */
    private static boolean declaresTypeVariable(String signature, String typeVariable) {
        int end = signature.indexOf(">(");

        return signature.startsWith("<") && end > 0 && signature.substring(0, end + 1).matches(".*[<;]" + java.util.regex.Pattern.quote(typeVariable.substring(1, typeVariable.length() - 1)) + ":.*");
    }

    /** @return the index which follows the type of a signature which starts at the index: a class with type arguments, a type variable, an array or a primitive type */
    private static int endOfSignatureType(String signature, int start) {
        int index = start;
        int depth = 0;

        // The wildcard indicators: '+TT;' is '? extends T', '-TT;' is '? super T', '*' is '?'
        while (signature.charAt(index) == '[' || signature.charAt(index) == '+' || signature.charAt(index) == '-') {
            index++;
        }
        if (signature.charAt(index) != 'L' && signature.charAt(index) != 'T') {
            return index + 1;
        }
        do {
            char c = signature.charAt(index++);

            if (c == '<') {
                depth++;
            } else if (c == '>') {
                depth--;
            }
            if (c == ';' && depth == 0) {
                break;
            }
        } while (index < signature.length());
        return index;
    }

    /** @return the name of the first of these variables which the value is, or which an argument of the call which returns it is, null if none */
    private String references(Expression value, Set<String> names) {
        return underlyingValues(value).stream().filter(LocalVariableReferenceExpression.class::isInstance).map(reference -> ((LocalVariableReferenceExpression) reference).getName())
                .filter(names::contains).findFirst().orElse(null);
    }

    /** @return true if the byte code which gives the value casts a call result to an anonymous class: the casts of the arguments which give no type to it do not count */
    private boolean hasAnonymousCast(Expression value) {
        if (value instanceof ClassFileMethodInvocationExpression call && call.isAnonymousCast()) {
            return true;
        }
        if (value instanceof MethodInvocationExpression call) {
            return callResults(call).stream().anyMatch(this::hasAnonymousCast);
        }
        if (value instanceof CastExpression cast) {
            return hasAnonymousCast(cast.getExpression());
        }
        if (value instanceof TernaryOperatorExpression ternary) {
            return hasAnonymousCast(ternary.getTrueExpression()) || hasAnonymousCast(ternary.getFalseExpression());
        }
        return value instanceof SwitchExpression switchExpression && switchResults(switchExpression).stream().anyMatch(this::hasAnonymousCast);
    }

    /** @return the creation of an anonymous class which gives its type to the value, null if none does */
    private ClassFileNewExpression anonymousCreation(Expression value) {
        return underlyingValues(value).stream().filter(ClassFileNewExpression.class::isInstance).map(ClassFileNewExpression.class::cast)
                .filter(creation -> creation.getBodyDeclaration() != null).findFirst().orElse(null);
    }

    /** 'try (var r = new AutoCloseable() { int added() {...} }) { r.added(); }': the resources which follow and the body use the resource */
    @Override
    public void visit(TryStatement statement) {
        List<TryStatement.Resource> resources = statement.getResources();

        for (int i = 0; resources != null && i < resources.size(); i++) {
            TryStatement.Resource resource = resources.get(i);
            List<TryStatement.Resource> following = resources.subList(i + 1, resources.size());
            Consumer<MemberUseSearch> uses = search -> {
                following.forEach(next -> next.getExpression().accept(search));
                Stream.of(statement.getTryStatements()).filter(Objects::nonNull).forEach(statements -> statements.accept(search));
                // The try calls 'close()' itself: an override of it which declares fewer exceptions needs the type of the class
                // When the declared type of the resource closes with exceptions which the handlers or the method permit, the explicit type is valid: it stays, with the cast which only 'var' would add
                search.found |= hasNoCheckedHandler(statement, closeExceptionsOfAll(resources, resource)) && search.callsClose()
                        && !methodCoversTheCloseOf(statement, resource);
            };

            if (resource.getType() != null && (needsVar(resource.getName(), resource.getExpression(), uses) || resourceAliasNeedsVar(resource, uses))) {
                TryStatement.Resource varResource = new TryStatement.Resource(VAR, resource.getName(), resource.getExpression());

                varResource.setFinal(resource.isFinal());
                resources.set(i, varResource);
            }
        }
        // The handlers of a 'try' protect its resources and its body only, not its handlers nor its 'finally'
        enclosingTries.push(statement);
        if (statement.getResources() != null) {
            statement.getResources().forEach(resource -> resource.accept(this));
        }
        statement.getTryStatements().accept(this);
        enclosingTries.pop();
        if (statement.getCatchClauses() != null) {
            statement.getCatchClauses().forEach(clause -> clause.accept(this));
        }
        if (statement.getFinallyStatements() != null) {
            statement.getFinallyStatements().accept(this);
        }
    }

    /**
     * A handler of a checked exception which the close can throw is invalid when the resource has the narrower type: the explicit type stays. The handlers of the
     * unchecked exceptions and of 'Exception' and 'Throwable' are valid whatever the close throws.
     */
    private boolean hasNoCheckedHandler(TryStatement statement, Set<String> closeExceptions) {
        if (statement.getCatchClauses() == null) {
            return true;
        }
        for (TryStatement.CatchClause clause : statement.getCatchClauses()) {
            List<ObjectType> types = new ArrayList<>();

            types.add(clause.getType());
            if (clause.getOtherTypes() != null) {
                types.addAll(clause.getOtherTypes());
            }
            for (ObjectType type : types) {
                if (!isUncheckedOrGeneral(type.getInternalName()) && !bodyThrows(statement, type.getInternalName()) && !closeThrows(closeExceptions, type.getInternalName())) {
                    return false;
                }
            }
        }
        return true;
    }

    /** @return true if the handler catches the exception: its type, or one of the alternatives of the clause, is the exception or a super type of it */
    private boolean catches(TryStatement.CatchClause clause, String exception) {
        List<ObjectType> types = new ArrayList<>();

        types.add(clause.getType());
        if (clause.getOtherTypes() != null) {
            types.addAll(clause.getOtherTypes());
        }
        return types.stream().anyMatch(type -> ClassMetadata.isSubtype(loader, exception, type.getInternalName()));
    }

    /** The body or a resource throws the exception itself ('throw new IOException()'): the handler is valid whatever the close throws */
    private boolean bodyThrows(TryStatement statement, String handled) {
        ExceptionSearch search = new ExceptionSearch(handled);

        if (statement.getTryStatements() != null) {
            statement.getTryStatements().accept(search);
        }
        if (statement.getResources() != null) {
            // The initializers of the resources are run in the 'try' too: the handlers catch what they throw
            statement.getResources().forEach(resource -> resource.getExpression().accept(search));
        }
        return search.thrown;
    }

    /** Searches what throws the exception which a handler catches: the statements and the calls of the body, but not the lambdas and the classes, which are run later */
    private final class ExceptionSearch extends AbstractJavaSyntaxVisitor {
        private final String handled;
        private boolean thrown;

        ExceptionSearch(String handled) {
            this.handled = handled;
        }

        @Override
        public void visit(ThrowStatement throwStatement) {
            String type = thrownTypeName(throwStatement.getExpression());

            thrown |= type != null && reaches(type);
            super.visit(throwStatement);
        }

        /** A method or a constructor which declares the exception (or one which the handler is narrower than) is a source of it */
        @Override
        public void visit(MethodInvocationExpression call) {
            thrown |= throwsThe(call);
            super.visit(call);
        }

        @Override
        public void visit(NewExpression creation) {
            String owner = ownerOfTheConstructor(creation);

            thrown |= owner != null && declares(owner, INIT, creation.getDescriptor(), handled);
            super.visit(creation);
        }

        /** The type of the creation of an anonymous class is its super type: the constructor which declares the exceptions is the one of the anonymous class */
        private String ownerOfTheConstructor(NewExpression creation) {
            if (creation instanceof ClassFileNewExpression anonymous && anonymous.getBodyDeclaration() instanceof ClassFileBodyDeclaration body) {
                return body.getInternalTypeName();
            }
            return creation.getObjectType() == null ? null : creation.getObjectType().getInternalName();
        }

        /** @return the internal name of the type which a thrown expression has: its class, or the first bound of its type variable; null if unknown */
        private String thrownTypeName(Expression expression) {
            if (expression.getType() instanceof ObjectType type) {
                return type.getInternalName();
            }
            if (expression.getType() instanceof GenericType generic && typeBounds.get(generic.getName()) != null) {
                return classBoundOf(generic.getName());
            }
            return null;
        }

        /** @return true if the handler catches what is thrown: the same exception, a sub type (caught by it) or a super type (the handler is narrower) */
        private boolean reaches(String exception) {
            return ClassMetadata.isSubtype(loader, exception, handled) || ClassMetadata.isSubtype(loader, handled, exception);
        }

        /** @return true if the method declares an exception which the handler catches: the same, a sub type (caught by it) or a super type (the handler is narrower) */
        private boolean declares(String owner, String name, String descriptor, String handled) {
            Set<String> exceptions = ClassMetadata.declaredExceptions(loader, owner, name, descriptor);

            if (exceptions.isEmpty() && INIT.equals(name) && LocalVariableMaker.isAnonymousClassName(owner)) {
                // The descriptor of the creation has lost the outer instance of the anonymous class: its only constructor declares what the one of its super class does
                exceptions = ClassMetadata.declaredConstructorExceptions(loader, owner);
            }
            for (String exception : exceptions) {
                if (ClassMetadata.isSubtype(loader, exception, handled) || ClassMetadata.isSubtype(loader, handled, exception)) {
                    return true;
                }
            }
            return false;
        }

        /**
         * @return true if the call throws the exception: what the method declares, where a type variable ('&lt;E extends Exception&gt; void raise(E e) throws E') is the type
         * of the argument which has it, not its erased bound
         */
        private boolean throwsThe(MethodInvocationExpression call) {
            String signature = ClassMetadata.declaredGenericSignature(loader, call.getInternalTypeName(), call.getName(), call.getDescriptor());

            if (signature == null || !signature.contains(")") || !signature.substring(signature.indexOf(')')).contains("^T")) {
                return declares(call.getInternalTypeName(), call.getName(), call.getDescriptor(), handled);
            }

            List<Expression> arguments = new ArrayList<>();
            List<String> parameters = parameterSignatures(signature);
            String[] thrownTypes = signature.substring(signature.indexOf(')') + 1).split("\\^");

            if (call.getParameters() != null) {
                call.getParameters().forEach(arguments::add);
            }
            for (int i = 1; i < thrownTypes.length; i++) {
                String exception = thrownTypes[i].startsWith("T") ? inferred(thrownTypes[i], parameters, arguments, call)
                        : thrownTypes[i].substring(1, thrownTypes[i].length() - 1);

                if (exception != null && (ClassMetadata.isSubtype(loader, exception, handled) || ClassMetadata.isSubtype(loader, handled, exception))) {
                    return true;
                }
                if (exception == null && thrownTypes[i].startsWith("T") && boundThrown(call)) {
                    return true;
                }
            }
            return false;
        }

        /**
         * @return true if a thrown type variable which no argument constrains ('&lt;E extends IOException&gt; void raise() throws E') is inferred as its bound, which the handler catches:
         * a bound which RuntimeException is a sub type of gives RuntimeException, which is unchecked
         */
        private boolean boundThrown(MethodInvocationExpression call) {
            for (String bound : ClassMetadata.declaredExceptions(loader, call.getInternalTypeName(), call.getName(), call.getDescriptor())) {
                if (!ClassMetadata.isSubtype(loader, RUNTIME_EXCEPTION, bound) && reaches(bound)) {
                    return true;
                }
            }
            return false;
        }

        /**
         * @return the internal name of the type of the argument which has the type variable as its type, or the type argument of the receiver which the type variable of
         * its class stands for ('Thrower&lt;IOException&gt;'); null if there is none
         */
        private String inferred(String typeVariable, List<String> parameters, List<Expression> arguments, MethodInvocationExpression call) {
            String common = null;

            for (int i = 0; i < parameters.size() && i < arguments.size(); i++) {
                // All the arguments which have the variable constrain it: it is their common super type
                for (String constraint : constraintsOf(parameters.get(i), arguments.get(i), typeVariable)) {
                    common = common == null ? constraint : ClassMetadata.commonSuperclass(loader, common, constraint);
                }
            }
            return common != null ? common : fromTheReceiver(typeVariable, call);
        }

        /** @return the types which the argument gives to the type variable: its own, or the ones of the elements when the parameter is an array of it ('E... values') */
        private List<String> constraintsOf(String parameter, Expression passed, String typeVariable) {
            List<String> constraints = new ArrayList<>();
            Expression argument = withoutRawCast(passed);

            if (parameter.equals(typeVariable)) {
                addType(constraints, argument);
            } else if (parameter.startsWith("[") && parameter.replaceFirst("^\\[+", "").equals(typeVariable)) {
                addElementTypes(constraints, argument);
            } else if (parameter.startsWith("L") && parameter.contains("<")) {
                addContainedTypes(constraints, parameter, argument, typeVariable);
            }
            return constraints;
        }

        /** The decompiler writes a cast to the raw type where the generic call needs one ('raiseFrom((List) failures)'): the type of the argument is the one of its operand */
        private Expression withoutRawCast(Expression argument) {
            return argument instanceof CastExpression cast && cast.getType() instanceof ObjectType type && type.getTypeArguments() == null ? cast.getExpression() : argument;
        }

        /** The array which a call builds for its varargs gives the types of its elements, an array argument the type of its elements */
        private void addElementTypes(List<String> constraints, Expression argument) {
            if (argument instanceof NewInitializedArray array) {
                for (VariableInitializer element : array.getArrayInitializer()) {
                    if (element instanceof ExpressionVariableInitializer initializer) {
                        addType(constraints, initializer.getExpression());
                    }
                }
            } else if (argument.getType() instanceof ObjectType type && type.getDimension() > 0) {
                constraints.add(type.getInternalName());
            }
        }

        /** 'List&lt;E&gt;' passed a 'List&lt;IOException&gt;' or an 'IOFailures extends ArrayList&lt;IOException&gt;': the type which stands where the parameter has the variable gives it its type */
        private void addContainedTypes(List<String> constraints, String parameter, Expression argument, String typeVariable) {
            if (argument.getType() instanceof ObjectType type) {
                addContainedTypes(constraints, parameter, type, typeVariable);
            }
        }

        /** 'List<? extends E>': the elements are an 'E' as well */
        private String withoutExtendsWildcard(String formal) {
            return formal.startsWith("+") ? formal.substring(1) : formal;
        }

        /** 'List<? extends IOException>' or 'List<? extends E>': the upper bound, a type variable included, is what the type variable stands for */
        private TypeArgument upperBound(TypeArgument argument) {
            if (argument instanceof WildcardExtendsTypeArgument wildcard) {
                return wildcard.type();
            }
            return argument;
        }

        /** The type arguments are matched recursively: 'List&lt;List&lt;E&gt;&gt;' passed a 'List&lt;List&lt;IOException&gt;&gt;' gives 'IOException' */
        private void addContainedTypes(List<String> constraints, String parameter, ObjectType type, String typeVariable) {
            List<String> parameterArguments = typeArguments(parameter);
            List<TypeArgument> actual = actualTypeArguments(type, parameter.substring(1, parameter.indexOf('<')));

            for (int i = 0; i < parameterArguments.size() && i < actual.size(); i++) {
                String formal = withoutExtendsWildcard(parameterArguments.get(i));
                TypeArgument contained = upperBound(actual.get(i));

                if (formal.equals(typeVariable)) {
                    // 'List<E>' of a method variable 'E extends IOException': the variable itself, with its bound
                    String name = nameOf(contained);

                    if (name != null) {
                        constraints.add(boundOf(name));
                    }
                } else if (formal.startsWith("L") && formal.contains("<") && contained instanceof ObjectType object) {
                    addContainedTypes(constraints, formal, object, typeVariable);
                }
            }
        }

        /**
         * @return the type arguments which the type has for its ancestor (or itself) of this name, null where one is unknown: 'IOFailures extends ArrayList&lt;IOException&gt;'
         * gives 'IOException' for 'List', the 'T' of a 'Failures&lt;T&gt; extends ArrayList&lt;T&gt;' the type argument of the type
         */
        private List<TypeArgument> actualTypeArguments(ObjectType type, String ancestor) {
            List<TypeArgument> own = type.getTypeArguments() == null ? List.of() : typeArgumentList(type.getTypeArguments());

            if (type.getInternalName().equals(ancestor)) {
                return own;
            }

            List<String> tokens = ClassMetadata.ancestorTypeArguments(loader, type.getInternalName(), ancestor).orElse(List.of());
            List<String> typeParameters = ClassMetadata.typeParameterNames(loader, type.getInternalName());
            List<TypeArgument> actual = new ArrayList<>();

            for (String token : tokens) {
                actual.add(typeOfToken(token, own, typeParameters));
            }
            return actual;
        }

        /** @return the type which a signature token of an ancestor stands for, its nested type arguments included; null where it is unknown (a variable the type gives no argument for) */
        private TypeArgument typeOfToken(String token, List<TypeArgument> own, List<String> typeParameters) {
            if (token.startsWith("T")) {
                int position = typeParameters.indexOf(token.substring(1, token.length() - 1));

                return position >= 0 && position < own.size() ? own.get(position) : null;
            }
            if (!token.startsWith("L")) {
                return null;
            }

            int generic = token.indexOf('<');
            String name = token.substring(1, generic < 0 ? token.length() - 1 : generic);
            List<TypeArgument> nested = new ArrayList<>();

            for (String argument : typeArguments(token)) {
                TypeArgument resolved = typeOfToken(argument, own, typeParameters);

                if (resolved == null) {
                    nested.clear();
                    break;
                }
                nested.add(resolved);
            }

            BaseTypeArgument arguments = nested.isEmpty() ? null : new TypeArguments(nested);

            return new ObjectType(name, name.replace('/', '.'), name.substring(name.lastIndexOf('/') + 1), arguments);
        }

        private void addType(List<String> constraints, Expression expression) {
            String type = thrownTypeName(expression);

            if (type != null) {
                constraints.add(type);
            }
        }

        private String fromTheReceiver(String typeVariable, MethodInvocationExpression call) {
            String name = typeVariableOf(typeVariable, call.getInternalTypeName(), call.getName(), call.getDescriptor(), call.getExpression() == null ? null : call.getExpression().getType());

            return name == null ? null : boundOf(name);
        }

        private void searchNested(TryStatement nested) {
            if (nested.getTryStatements() != null) {
                nested.getTryStatements().accept(this);
            }
            if (nested.getResources() != null) {
                nested.getResources().forEach(resource -> resource.getExpression().accept(this));
                thrown |= closeThrows(closeExceptionsOfAll(nested.getResources(), null), handled);
            }
        }

        /** The body of a lambda or of a method of an anonymous class is run later, if ever: it does not throw while the body of the 'try' is run */
        @Override
        public void visit(LambdaIdentifiersExpression lambda) {
            // not searched
        }

        @Override
        public void visit(BodyDeclaration body) {
            // not searched
        }

        /** What a nested 'try' throws may be caught by its own handlers: its body is searched unless one of them catches the exception, and its handlers and its 'finally' always are */
        @Override
        public void visit(TryStatement nested) {
            boolean consumed = nested.getCatchClauses() != null && nested.getCatchClauses().stream().anyMatch(clause -> catches(clause, handled));

            // A handler of another exception consumes nothing: what the body, the resources and their close throw leave the nested 'try' ('finally' only runs)
            if (!consumed) {
                searchNested(nested);
            }
            if (nested.getCatchClauses() != null) {
                nested.getCatchClauses().forEach(clause -> clause.accept(this));
            }
            if (nested.getFinallyStatements() != null) {
                nested.getFinallyStatements().accept(this);
            }
        }
    }

    /**
     * @return the exceptions which the implicit close of the resources declare: the handlers of the 'try' are valid when one of them throws what they catch. The
     * declared type of the resource itself does not count: it is the explicit type which the restoration of 'var' replaces
     */
    private Set<String> closeExceptionsOfAll(List<TryStatement.Resource> resources, TryStatement.Resource current) {
        Set<String> exceptions = new HashSet<>();

        for (TryStatement.Resource resource : resources) {
            exceptions.addAll(closeExceptionsOf(resource));
            if (resource != current && resource.getType() != null) {
                exceptions.addAll(ClassMetadata.declaredExceptions(loader, resource.getType().getInternalName(), CLOSE, "()V"));
            }
        }
        return exceptions;
    }

    /** @return the exceptions which the 'close()' of the anonymous class of the resource declares: the implicit close of the resource throws them */
    private Set<String> closeExceptionsOf(TryStatement.Resource resource) {
        ClassFileNewExpression creation = resource.getExpression() == null ? null : anonymousCreation(resource.getExpression());

        if (creation != null && creation.getBodyDeclaration() instanceof ClassFileBodyDeclaration body && body.getMethodDeclarations() != null) {
            for (ClassFileConstructorOrMethodDeclaration declaration : body.getMethodDeclarations()) {
                if (declaration.getMethod() != null && CLOSE.equals(declaration.getMethod().getName()) && "()V".equals(declaration.getMethod().getSignature())) {
                    return ClassMetadata.exceptionNames(declaration.getMethod());
                }
            }
        }
        return Set.of();
    }

    /** @return true if an exception which the close declares is caught by the handler, or the handler is narrower than it */
    private boolean closeThrows(Set<String> closeExceptions, String handled) {
        return closeExceptions.stream().anyMatch(exception -> ClassMetadata.isSubtype(loader, exception, handled) || ClassMetadata.isSubtype(loader, handled, exception));
    }

    private boolean isUncheckedOrGeneral(String internalName) {
        return "java/lang/Exception".equals(internalName) || THROWABLE.equals(internalName)
                || ClassMetadata.isSubtype(loader, internalName, RUNTIME_EXCEPTION) || ClassMetadata.isSubtype(loader, internalName, "java/lang/Error");
    }

    /** 'try (var r = b)' where 'b' is a tracked variable */
    private boolean resourceAliasNeedsVar(TryStatement.Resource resource, Consumer<MemberUseSearch> uses) {
        String origin = resource.getExpression() == null ? null : references(resource.getExpression(), anonymousVariables.keySet());

        if (origin == null) {
            return false;
        }

        Set<String> members = anonymousVariables.get(origin);
        MemberUseSearch search = new MemberUseSearch(resource.getName(), members);

        // The alias gives the type of its origin to a lambda as well
        search.anonymousName = anonymousClassNames.get(origin);
        search.lambdaAdaptation = search.anonymousName != null;
        uses.accept(search);
        if (search.found) {
            anonymousVariables.put(resource.getName(), members);
            if (search.anonymousName != null) {
                anonymousClassNames.put(resource.getName(), search.anonymousName);
            }
        }
        return search.found;
    }

    /** A method of the anonymous class which overrides a method of its super type: the type of the super type is enough to call it */
    private boolean isInherited(Method method, String superInternalName) {
        // A public override of a protected method (clone, finalize) can be called through the anonymous type only
        boolean widens = method.isPublic() && PROTECTED_OBJECT_METHODS.contains(method.getName() + method.getSignature());

        if (OBJECT_METHODS.contains(method.getName() + method.getSignature())) {
            return (!widens || ClassMetadata.declaresPublicMethod(loader, superInternalName, method.getName(), method.getSignature()))
                    && ClassMetadata.exceptionNames(method).containsAll(exceptionsOfTheSuperMethod(method, superInternalName));
        }
        if (!method.isPublic() && !packageOf(superInternalName).equals(packageOf(internalTypeName))) {
            // A protected method of a super type of another package cannot be called through the super type from here, only through the anonymous class
            return false;
        }
        if (ClassMetadata.isLoadable(loader, superInternalName)) {
            return (method.isPublic()
                    ? ClassMetadata.declaresPublicMethod(loader, superInternalName, method.getName(), method.getSignature())
                    : ClassMetadata.declaresMethod(loader, superInternalName, method.getName(), method.getSignature()))
                    && ClassMetadata.exceptionNames(method).containsAll(ClassMetadata.declaredExceptions(loader, superInternalName, method.getName(), method.getSignature()))
                    // A return type with other type arguments ('List<String>' for 'List<?>') is the one of the anonymous class only
                    && Objects.equals(ClassMetadata.genericSignature(method), ClassMetadata.declaredGenericSignature(loader, superInternalName, method.getName(), method.getSignature()));
        }
        // The super type cannot be read: the types which are known to the type maker
        return typeMaker.matchCount(superInternalName, method.getName(), Type.getArgumentTypes(method.getSignature()).length, false) > 0;
    }

    /** The exceptions which the method of the super type declares: the ones of the method of Object if no super class declares it */
    private Set<String> exceptionsOfTheSuperMethod(Method method, String superInternalName) {
        String key = method.getName() + method.getSignature();

        if (ClassMetadata.isLoadable(loader, superInternalName) && ClassMetadata.declaresMethod(loader, superInternalName, method.getName(), method.getSignature())) {
            return ClassMetadata.declaredExceptions(loader, superInternalName, method.getName(), method.getSignature());
        }
        return OBJECT_METHOD_EXCEPTIONS.getOrDefault(key, Set.of());
    }

    private boolean overrides(Method method, String superInternalName) {
        return OBJECT_METHODS.contains(method.getName() + method.getSignature())
                || ClassMetadata.isLoadable(loader, superInternalName) && (ClassMetadata.declaresMethod(loader, superInternalName, method.getName(), method.getSignature())
                || ClassMetadata.declaresPrivateMethod(loader, superInternalName, method.getName(), method.getSignature()));
    }

    private Set<String> addedMembers(ClassFileBodyDeclaration body, String superInternalName) {
        Set<String> members = new HashSet<>();

        addedMethods(body, superInternalName, members);
        addedFields(body, superInternalName, members);
        return members;
    }

    private void addedMethods(ClassFileBodyDeclaration body, String superInternalName, Set<String> members) {
        for (ClassFileConstructorOrMethodDeclaration method : Objects.requireNonNullElse(body.getMethodDeclarations(), List.<ClassFileConstructorOrMethodDeclaration>of())) {
            Method m = method.getMethod();

            if (m != null && !INIT.equals(m.getName()) && !m.isSynthetic() && !isInherited(m, superInternalName)) {
                // not declared by the type which the anonymous class extends: the variable needs the type of the class to call it
                // A method is identified by its name and descriptor: a call of the inherited overload does not need the type of the class
                // An override which widens the access is called through the super type too, whose call is owned by the super type
                String signature = m.getName() + m.getSignature();

                members.add(overrides(m, superInternalName) ? hiddenField(body.getInternalTypeName(), signature) : signature);
            }
        }
    }

    private void addedFields(ClassFileBodyDeclaration body, String superInternalName, Set<String> members) {
        AbstractJavaSyntaxVisitor fieldNames = new AbstractJavaSyntaxVisitor() {
            @Override
            public void visit(FieldDeclarator declarator) {
                // A field which hides the one of the super type is only referenced by the anonymous class as owner
                members.add(ClassMetadata.declaresField(loader, superInternalName, declarator.getName())
                        ? hiddenField(body.getInternalTypeName(), declarator.getName()) : declarator.getName());
            }
        };
        for (ClassFileFieldDeclaration field : Objects.requireNonNullElse(body.getFieldDeclarations(), List.<ClassFileFieldDeclaration>of())) {
            field.getFieldDeclarators().accept(fieldNames);
        }
    }

    private static String hiddenField(String ownerInternalName, String name) {
        return "@" + ownerInternalName + "." + name;
    }

    /** The lambdas which infer the type of their parameters from a tracked variable: their parameters are aliases of it while the lambda is visited */
    @Override
    public void visit(MethodInvocationExpression expression) {
        Set<String> registered = registerLambdaParameters(expression.getInternalTypeName(), expression.getName(), expression.getDescriptor(), expression.getParameters());

        super.visit(expression);
        registered.forEach(name -> {
            anonymousVariables.remove(name);
            anonymousContainers.remove(name);
            containerShapes.remove(name);
        });
    }

    /** 'new Box&lt;&gt;(b, x -> ...)': the constructor infers the type of the lambda like a generic method */
    @Override
    public void visit(NewExpression expression) {
        Set<String> registered = expression.getObjectType() == null ? Set.of()
                : registerLambdaParameters(expression.getObjectType().getInternalName(), INIT, expression.getDescriptor(), expression.getParameters());

        super.visit(expression);
        registered.forEach(name -> {
            anonymousVariables.remove(name);
            anonymousContainers.remove(name);
            containerShapes.remove(name);
        });
    }

    /** @return the names of the parameters of the lambdas which were registered as aliases of the tracked variables which the call passes */
    private Set<String> registerLambdaParameters(String ownerInternalName, String name, String descriptor, BaseExpression callParameters) {
        Set<String> registered = new HashSet<>();
        String signature = ClassMetadata.declaredGenericSignature(loader, ownerInternalName, name, descriptor);

        if (callParameters != null && signature != null && signature.contains(")") && !anonymousVariables.isEmpty()) {
            List<Expression> arguments = new ArrayList<>();
            List<String> parameters = parameterSignatures(signature);

            callParameters.forEach(arguments::add);
            for (int i = 0; i < arguments.size() && i < parameters.size(); i++) {
                String tracked = parameters.get(i).startsWith("T") ? references(arguments.get(i), anonymousVariables.keySet()) : null;

                if (tracked != null) {
                    registerLambdaParameters(arguments, parameters, i, anonymousVariables.get(tracked), registered);
                }
            }
        }
        return registered;
    }

    private void registerLambdaParameters(List<Expression> arguments, List<String> parameters, int index, Set<String> members, Set<String> registered) {
        for (int j = 0; j < arguments.size() && j < parameters.size(); j++) {
            if (j != index && arguments.get(j) instanceof LambdaIdentifiersExpression lambda && parameters.get(j).contains(parameters.get(index))) {
                register(anonymousVariables, parametersOfTheType(lambda, parameters.get(j), parameters.get(index)), members, registered);
                registerContainers(containerParametersOfTheType(lambda, parameters.get(j), parameters.get(index), parameters.get(index)), members, registered);
            }
        }
    }

    private void registerContainers(Map<String, Container> containers, Set<String> members, Set<String> registered) {
        containers.forEach((name, container) -> {
            if (!anonymousContainers.containsKey(name)) {
                anonymousContainers.put(name, members);
                containerShapes.put(name, container);
                registered.add(name);
            }
        });
    }

    /** Registers the names which are not tracked yet as aliases of the variable with these members, up to the end of the call */
    private static void register(Map<String, Set<String>> tracked, List<String> names, Set<String> members, Set<String> registered) {
        for (String name : names) {
            if (!tracked.containsKey(name)) {
                tracked.put(name, members);
                registered.add(name);
            }
        }
    }

    /**
     * @return the names of the parameters of the lambda which have the type variable: the i-th parameter has the i-th type argument of its functional
     * interface ('ToIntBiFunction&lt;T, Other&gt;': 'x' is a 'T', 'y' is an 'Other'), the first one when there are fewer type arguments ('Comparator&lt;T&gt;')
     */
    private List<String> parametersOfTheType(LambdaIdentifiersExpression lambda, String functionalInterface, String typeVariable) {
        // 'ToIntFunction<T[]>': the parameter is an array of the type, its elements have it
        return new ArrayList<>(parametersWhere(lambda, functionalInterface, type -> type.replaceFirst("^\\[+", "").equals(typeVariable)).keySet());
    }

    /**
     * @return true if the method returns the type argument of the container which is the type variable of the tracked variable ('List&lt;E&gt;.get' of a 'List&lt;T&gt;'): the
     * element has the type of the variable. 'Map&lt;T, Other&gt;.get' returns the 'Other'.
     */
    private boolean returnsATypeVariableOfItsClass(MethodInvocationExpression call, Container container) {
        String signature = ClassMetadata.declaredGenericSignature(loader, call.getInternalTypeName(), call.getName(), call.getDescriptor());

        if (signature == null || !signature.contains(")") || !returnedType(signature).startsWith("T") || declaresTypeVariable(signature, returnedType(signature))) {
            return false;
        }

        String returned = returnedType(signature);
        String owner = call.getInternalTypeName();

        // The class of the method may be a super type of the container ('Collection' of a 'List'): its type parameters are then not the arguments of the container
        if (!container.type().startsWith("L" + owner + "<")) {
            return true;
        }

        String declaring = ClassMetadata.declaringClass(loader, owner, call.getName(), call.getDescriptor()).orElse(owner);
        String inTheTermsOfTheOwner = returned;

        if (!declaring.equals(owner)) {
            // 'Reversed<K, V> extends Map<V, K>': the 'V' which 'Map.get' returns is the 'K' of 'Reversed'
            List<String> ancestor = ClassMetadata.ancestorTypeArguments(loader, owner, declaring).orElse(List.of());
            int position = ClassMetadata.typeParameterNames(loader, declaring).indexOf(returned.substring(1, returned.length() - 1));

            if (position < 0 || position >= ancestor.size()) {
                return true;
            }
            inTheTermsOfTheOwner = ancestor.get(position);
        }
        if (!inTheTermsOfTheOwner.startsWith("T")) {
            return false;
        }

        int index = ClassMetadata.typeParameterNames(loader, owner).indexOf(inTheTermsOfTheOwner.substring(1, inTheTermsOfTheOwner.length() - 1));
        List<String> arguments = typeArguments(container.type());

        return index >= 0 && index < arguments.size() && arguments.get(index).equals(container.variable());
    }

    /** @return the parameters of the lambda which hold the type variable as a type argument ('ToIntFunction&lt;List&lt;T&gt;&gt;'), with their types: the elements which they give have it */
    private Map<String, Container> containerParametersOfTheType(LambdaIdentifiersExpression lambda, String functionalInterface, String typeVariable, String variable) {
        Map<String, Container> containers = new HashMap<>();

        parametersWhere(lambda, functionalInterface, type -> type.startsWith("L") && typeArguments(type).contains(typeVariable))
                .forEach((name, type) -> containers.put(name, new Container(type, variable)));
        return containers;
    }

    private Map<String, String> parametersWhere(LambdaIdentifiersExpression lambda, String functionalInterface, Predicate<String> hasTheType) {
        List<String> typeArguments = typeArguments(functionalInterface);
        String interfaceName = functionalInterface.startsWith("L") && functionalInterface.contains("<") ? functionalInterface.substring(1, functionalInterface.indexOf('<')) : null;
        List<String> methodParameters = ClassMetadata.functionalMethodParameters(loader, interfaceName);
        List<String> typeParameters = ClassMetadata.typeParameterNames(loader, interfaceName);
        Map<String, String> names = new LinkedHashMap<>();

        for (int i = 0; i < lambda.getParameterNames().size(); i++) {
            String type = parameterType(i, methodParameters, typeParameters, typeArguments);

            if (hasTheType.test(type)) {
                names.put(lambda.getParameterNames().get(i), type);
            }
        }
        return names;
    }

    /** @return the type of the i-th parameter of the lambda: the one of the functional method, or the i-th type argument when the method is not known */
    private String parameterType(int i, List<String> methodParameters, List<String> typeParameters, List<String> typeArguments) {
        if (i < methodParameters.size()) {
            return substitute(methodParameters.get(i), typeParameters, typeArguments);
        }
        return typeArguments.isEmpty() ? "" : typeArguments.get(Math.min(i, typeArguments.size() - 1));
    }

    /** @return the type of a parameter of the functional method where the type parameters of the interface are replaced by the type arguments of its use */
    private static String substitute(String parameter, List<String> typeParameters, List<String> typeArguments) {
        return ClassMetadata.substituteTypeVariables(parameter, typeParameters, typeArguments);
    }

    private final class MemberUseSearch extends AbstractJavaSyntaxVisitor {
        private final String variableName;
        /** The variable and the variables which are declared with its value: 'var c = b;' */
        private final Set<String> variableNames = new HashSet<>();
        private final Set<String> members;
        /** The fields which the local classes around the code declare: they are no capture of the variable */
        private final Deque<Set<String>> declaredFields = new ArrayDeque<>();
        private boolean found;
        /** True where a lambda which the variable gives its type to is enough to keep 'var', whether it uses a member or not */
        private boolean lambdaAdaptation;
        private String anonymousName;

        /** The type which the variable gets when it is declared without 'var': the one of its close is the one which an implicit close of it calls; null if unknown */
        private ObjectType declaredType;

        MemberUseSearch(String variableName, Set<String> members) {
            this.variableName = variableName;
            this.members = members;
            variableNames.add(variableName);
        }

        MemberUseSearch declaredAs(ObjectType type) {
            this.declaredType = type;
            return this;
        }

        /** A method of a local or anonymous class which has a parameter of this name uses its own variable, not the one of the enclosing method */
        @Override
        public void visit(MethodDeclaration declaration) {
            Set<String> parameters = parameterNames(declaration.getFormalParameters());

            if (!parameters.contains(variableName)) {
                visitWithout(parameters, () -> super.visit(declaration));
            }
        }

        @Override
        public void visit(ConstructorDeclaration declaration) {
            Set<String> parameters = parameterNames(declaration.getFormalParameters());

            if (!parameters.contains(variableName)) {
                visitWithout(parameters, () -> super.visit(declaration));
            }
        }

        /** The parameters hide the aliases of the variable which have their names, in the executable only */
        private void visitWithout(Set<String> hidden, Runnable visit) {
            Set<String> removed = new HashSet<>(variableNames);

            removed.retainAll(hidden);
            variableNames.removeAll(removed);
            visit.run();
            variableNames.addAll(removed);
        }

        @Override
        public void visit(BodyDeclaration declaration) {
            Set<String> fields = new HashSet<>();

            if (declaration instanceof ClassFileBodyDeclaration body) {
                Objects.requireNonNullElse(body.getFieldDeclarations(), List.<ClassFileFieldDeclaration>of())
                        .forEach(field -> field.getFieldDeclarators().accept(new AbstractJavaSyntaxVisitor() {
                            @Override
                            public void visit(FieldDeclarator declarator) {
                                fields.add(declarator.getName());
                            }
                        }));
            }
            declaredFields.push(fields);
            super.visit(declaration);
            declaredFields.pop();
        }

        /** A local which is not declared with the value of the variable has the name of an alias: it hides it up to the end of the block */
        private void shadowAliases(LocalVariableDeclarationStatement declaration) {
            for (LocalVariableDeclarator declarator : declaration.getLocalVariableDeclarators()) {
                if (!(declarator.getVariableInitializer() instanceof ExpressionVariableInitializer initializer) || references(initializer.getExpression(), variableNames) == null) {
                    variableNames.remove(declarator.getName());
                }
            }
        }

        private static List<String> declaredNames(LocalVariableDeclarationStatement declaration) {
            List<String> names = new ArrayList<>();

            for (LocalVariableDeclarator declarator : declaration.getLocalVariableDeclarators()) {
                names.add(declarator.getName());
            }
            return names;
        }

        /** The variable of this name which a block declares hides the one of the enclosing method up to the end of the block */
        @Override
        public void visit(Statements statements) {
            // The aliases which a block declares are known up to its end
            Set<String> knownBefore = new HashSet<>(variableNames);

            for (Statement statement : statements) {
                statement.accept(this);
                if (statement instanceof LocalVariableDeclarationStatement declaration) {
                    if (declaredNames(declaration).contains(variableName)) {
                        break;
                    }
                    shadowAliases(declaration);
                }
            }
            variableNames.clear();
            variableNames.addAll(knownBefore);
        }

        /** An override of 'close()' which declares fewer exceptions than the one of its super type has to be called through its own type */
        boolean callsClose() {
            return members.stream().anyMatch(member -> member.equals("close()V") || member.endsWith(".close()V"));
        }

        /** The 'try' statements which the search has entered: their handlers protect what it meets in their resources and body */
        private final Deque<TryStatement> nestedTries = new ArrayDeque<>();

        /** 'try (var r = b)' closes the variable: the close is a use of it */
        @Override
        public void visit(TryStatement statement) {
            if (statement.getResources() != null && hasNoCheckedHandler(statement, Set.of()) && callsClose()
                    && !(declaredType != null && closeIsPermitted(statement, declaredType, nestedTries))
                    && statement.getResources().stream().anyMatch(resource -> resource.getExpression() != null && references(resource.getExpression(), variableNames) != null)) {
                found = true;
            }
            nestedTries.push(statement);
            if (statement.getResources() != null) {
                statement.getResources().forEach(resource -> resource.accept(this));
            }
            statement.getTryStatements().accept(this);
            nestedTries.pop();
            if (statement.getCatchClauses() != null) {
                statement.getCatchClauses().forEach(clause -> clause.accept(this));
            }
            if (statement.getFinallyStatements() != null) {
                statement.getFinallyStatements().accept(this);
            }
        }

        /** The variable of a handler hides the original variable and its aliases which have its name, in the handler only */
        @Override
        public void visit(TryStatement.CatchClause catchClause) {
            if (!variableName.equals(catchClause.getName())) {
                visitWithout(Set.of(catchClause.getName()), () -> super.visit(catchClause));
            }
        }

        @Override
        public void visit(ForEachStatement statement) {
            if (variableName.equals(statement.getName())) {
                statement.getExpression().accept(this);
            } else {
                visitWithout(Set.of(statement.getName()), () -> super.visit(statement));
            }
        }

        @Override
        public void visit(LocalVariableDeclarator declarator) {
            if (declarator.getVariableInitializer() instanceof ExpressionVariableInitializer initializer && references(initializer.getExpression(), variableNames) != null) {
                variableNames.add(declarator.getName());
            }
            super.visit(declarator);
        }

        @Override
        public void visit(MethodInvocationExpression expression) {
            if (isVariable(expression.getExpression()) && isAdded(expression.getInternalTypeName(), expression.getName(), expression.getDescriptor())
                    || passesTheVariableToALambdaWhichUsesAMember(expression.getInternalTypeName(), expression.getName(), expression.getDescriptor(), expression.getParameters())) {
                found = true;
            }
            super.visit(expression);
        }

        /** 'new Box&lt;&gt;(b, x -> x.added())': the constructor infers the type of the lambda like a generic method */
        @Override
        public void visit(NewExpression expression) {
            if (expression.getObjectType() != null
                    && passesTheVariableToALambdaWhichUsesAMember(expression.getObjectType().getInternalName(), INIT, expression.getDescriptor(), expression.getParameters())) {
                found = true;
            }
            super.visit(expression);
        }

        /**
         * 'apply(b, x -> x.added())' with '&lt;T&gt; int apply(T value, ToIntFunction&lt;T&gt; f)': the parameter of the lambda has the type variable of the parameter which
         * the variable is passed to, so it has the type of the variable
         */
        private boolean passesTheVariableToALambdaWhichUsesAMember(String ownerInternalName, String name, String descriptor, BaseExpression callParameters) {
            String signature = ClassMetadata.declaredGenericSignature(loader, ownerInternalName, name, descriptor);

            if (callParameters == null || signature == null || !signature.contains(")")) {
                return false;
            }

            List<Expression> arguments = new ArrayList<>();

            callParameters.forEach(arguments::add);

            List<String> parameters = spreadVarargs(parameterSignatures(signature), arguments.size());

            for (int i = 0; i < arguments.size() && i < parameters.size(); i++) {
                String variable = typeVariableFedBy(arguments.get(i), parameters.get(i));

                if (variable != null && hasALambdaOfTheTypeVariable(arguments, parameters, i, variable)) {
                    return true;
                }
            }
            return false;
        }

        /** 'T...' is the signature '[TT;', each of the arguments which the array gathers has the type '"TT;"' */
        private List<String> spreadVarargs(List<String> parameters, int argumentCount) {
            if (parameters.isEmpty() || !parameters.get(parameters.size() - 1).startsWith("[T") || argumentCount < parameters.size()) {
                return parameters;
            }

            List<String> spread = new ArrayList<>(parameters.subList(0, parameters.size() - 1));

            while (spread.size() < argumentCount) {
                spread.add(parameters.get(parameters.size() - 1).substring(1));
            }
            return spread;
        }

        /**
         * @return the type variable of the parameter which the argument gives the type of the variable to: the variable itself ('T'), or an element of the container which a call
         * builds from it ('List.of(value)' for 'List&lt;T&gt;'); null if none
         */
        private String typeVariableFedBy(Expression argument, String parameter) {
            if (parameter.startsWith("T")) {
                return references(argument, variableNames) != null ? parameter : null;
            }
            if (!parameter.startsWith("L")) {
                return null;
            }
            if (argument instanceof MethodInvocationExpression call && call.getParameters() != null) {
                List<Expression> elements = new ArrayList<>();

                call.getParameters().forEach(elements::add);
                for (int i = 0; i < elements.size(); i++) {
                    String variable = typeVariableOfTheElement(call, elements.get(i), i, parameter);

                    if (variable != null) {
                        return variable;
                    }
                }
            }
            return argument instanceof NewExpression creation ? typeVariableFedByConstructor(creation, parameter) : null;
        }

        /** 'new ArrayList<>(List.of(value))': the copy has the type arguments of the container it copies */
        private String typeVariableFedByConstructor(NewExpression creation, String parameter) {
            if (creation.getParameters() == null) {
                return null;
            }

            List<Expression> elements = new ArrayList<>();

            creation.getParameters().forEach(elements::add);
            for (int i = 0; i < elements.size(); i++) {
                Expression element = elements.get(i);
                String argument = constructorTypeArgument(creation, i, parameter);
                String variable;

                if (argument != null) {
                    variable = references(element, variableNames) != null ? (argument.startsWith("T") ? argument : null)
                            : (argument.startsWith("L") ? typeVariableFedBy(element, argument) : null);
                } else {
                    variable = references(element, variableNames) != null ? firstTypeVariable(parameter) : typeVariableFedBy(element, parameter);
                }

                if (variable != null) {
                    return variable;
                }
            }
            return null;
        }

        /** @return the type argument which the parameter of the constructor at the index is the type of: 'Pair(K key, V value)' for 'Pair<K,V>' gives V for the second one; null if unknown */
        private String constructorTypeArgument(NewExpression creation, int index, String parameter) {
            String signature = creation.getObjectType() == null ? null
                    : ClassMetadata.declaredGenericSignature(loader, creation.getObjectType().getInternalName(), "<init>", creation.getDescriptor());

            if (signature == null || !signature.contains(")")) {
                return null;
            }

            List<String> constructorParameters = spreadVarargs(parameterSignatures(signature), index + 1);
            List<String> typeArguments = typeArguments(parameter);
            List<String> names = ClassMetadata.typeParameterNames(loader, creation.getObjectType().getInternalName());

            if (index >= constructorParameters.size() || !constructorParameters.get(index).startsWith("T") || names.size() != typeArguments.size()) {
                return null;
            }

            int position = names.indexOf(constructorParameters.get(index).substring(1, constructorParameters.get(index).length() - 1));

            return position < 0 ? null : typeArguments.get(position);
        }

        private String firstTypeVariable(String parameter) {
            return typeArguments(parameter).stream().filter(typeArgument -> typeArgument.startsWith("T")).findFirst().orElse(null);
        }

        /**
         * @return the type variable which an element of a container call feeds: the type argument which the element corresponds to when it is the variable, what a nested
         * container ('List.of(List.of(value))' for 'List<List<T>>') feeds when it is a call
         */
        private String typeVariableOfTheElement(MethodInvocationExpression call, Expression element, int index, String parameter) {
            String typeArgument = containerTypeArgument(call, index, parameter);

            if (typeArgument == null) {
                return null;
            }
            if (references(element, variableNames) != null) {
                return typeArgument.startsWith("T") ? typeArgument : null;
            }
            return typeArgument.startsWith("L") ? typeVariableFedBy(element, typeArgument) : null;
        }

        /** @return the type argument of the parameter which the element at the index of the container call feeds: 'Map.of(k, v)' for 'Map<K,V>' gives V for the second element */
        private String containerTypeArgument(MethodInvocationExpression call, int index, String parameter) {
            List<String> typeArguments = typeArguments(parameter);
            String signature = ClassMetadata.declaredGenericSignature(loader, call.getInternalTypeName(), call.getName(), call.getDescriptor());

            if (signature != null && signature.contains(")")) {
                List<String> callParameters = spreadVarargs(parameterSignatures(signature), index + 1);
                List<String> returnedArguments = typeArguments(returnedType(signature));

                if (index < callParameters.size() && returnedArguments.size() == typeArguments.size()) {
                    int position = returnedArguments.indexOf(callParameters.get(index));

                    if (position >= 0) {
                        return typeArguments.get(position);
                    }
                }
            }
            return typeArguments.isEmpty() ? null : typeArguments.get(0);
        }

        /** @return true if a lambda which uses a member is passed to a parameter whose type contains the type variable of the parameter at the index */
        private boolean hasALambdaOfTheTypeVariable(List<Expression> arguments, List<String> parameters, int index, String variable) {
            for (int j = 0; j < arguments.size() && j < parameters.size(); j++) {
                if (j != index && lambdaAdaptation && parameters.get(j).contains(variable) && isInstantiatedWithTheDeclaredType(arguments.get(j))) {
                    return true;
                }
                if (j != index && arguments.get(j) instanceof LambdaIdentifiersExpression lambda && parameters.get(j).contains(variable)
                        && ((lambdaAdaptation && isCompiledWithTheDeclaredType(lambda)) || checksTheAnonymousType(lambda) || callsAMember(lambda, parametersOfTheType(lambda, parameters.get(j), variable),
                                containerParametersOfTheType(lambda, parameters.get(j), variable, variable)))) {
                    return true;
                }
            }
            return false;
        }

        /** @return true if the body of the lambda casts a call result to the anonymous class ('xs.get(0).toString()' for a 'List<T>' parameter): the explicit type would lose the check */
        private boolean checksTheAnonymousType(Expression lambda) {
            boolean[] checks = new boolean[1];

            lambda.accept(new AbstractJavaSyntaxVisitor() {
                @Override
                public void visit(MethodInvocationExpression expression) {
                    checks[0] |= expression instanceof ClassFileMethodInvocationExpression call && call.isAnonymousCast()
                            && (anonymousName == null || call.getAnonymousCastName() == null || anonymousName.equals(call.getAnonymousCastName()));
                    super.visit(expression);
                }
            });
            return checks[0];
        }

        /** @return true if a method or constructor reference was instantiated for the anonymous class ('apply(value, Object::toString)' checks its argument at run time) */
        private boolean isInstantiatedWithTheDeclaredType(Expression argument) {
            String descriptor = null;

            if (argument instanceof ClassFileMethodReferenceExpression reference) {
                descriptor = reference.getInstantiatedDescriptor();
            } else if (argument instanceof ClassFileConstructorReferenceExpression reference) {
                descriptor = reference.getInstantiatedDescriptor();
            }
            return anonymousName != null && descriptor != null && descriptor.contains("L" + anonymousName + ";");
        }

        /** @return false if the lambda was compiled for 'Object' ('Object value = new Object() {}': nothing is adapted to the anonymous class); true when its types are unknown */
        private boolean isCompiledWithTheDeclaredType(LambdaIdentifiersExpression lambda) {
            var types = lambda.getParameterTypes();

            return types == null || anonymousName == null
                    || types.stream().anyMatch(type -> type instanceof ObjectType objectType && objectType.getInternalName().equals(anonymousName));
        }

        /** @return true if the body of the lambda uses a member of the anonymous class on one of these parameters of the lambda */
        private boolean callsAMember(Expression lambda, List<String> parameterNames, Map<String, Container> containers) {
            boolean[] calls = new boolean[1];
            Set<String> names = new HashSet<>(parameterNames);

            lambda.accept(new AbstractJavaSyntaxVisitor() {
                @Override
                public void visit(MethodInvocationExpression expression) {
                    calls[0] |= isParameter(expression.getExpression()) && isAdded(expression.getInternalTypeName(), expression.getName(), expression.getDescriptor());
                    super.visit(expression);
                }

                @Override
                public void visit(FieldReferenceExpression expression) {
                    calls[0] |= isParameter(expression.getExpression())
                            && (members.contains(expression.getName()) || members.contains(hiddenField(expression.getInternalTypeName(), expression.getName())));
                    super.visit(expression);
                }

                @Override
                public void visit(MethodReferenceExpression expression) {
                    calls[0] |= isParameter(expression.getExpression()) && isAdded(expression.getInternalTypeName(), expression.getName(), expression.getDescriptor());
                    super.visit(expression);
                }

                /** 'var y = x;' inside the lambda: 'y' has the type of the parameter */
                @Override
                public void visit(LocalVariableDeclarator declarator) {
                    if (declarator.getVariableInitializer() instanceof ExpressionVariableInitializer initializer && isParameter(initializer.getExpression())) {
                        names.add(declarator.getName());
                    } else {
                        // A local of the same name which has another value shadows the alias
                        names.remove(declarator.getName());
                    }
                    super.visit(declarator);
                }

                /** The aliases of a block end with it: a sibling block may reuse their names */
                @Override
                public void visit(Statements statements) {
                    Set<String> before = new HashSet<>(names);

                    super.visit(statements);
                    names.clear();
                    names.addAll(before);
                }

                /** The parameter itself, or an element of it when its type is an array of the type variable */
                private boolean isParameter(Expression receiver) {
                    // A captured variable of an anonymous class has no receiver
                    Expression value = receiver != null && receiver.isArrayExpression() ? receiver.getExpression() : receiver;

                    return value instanceof LocalVariableReferenceExpression reference && names.contains(reference.getName())
                            || value instanceof MethodInvocationExpression element && element.getExpression() instanceof LocalVariableReferenceExpression container
                            && containers.containsKey(container.getName()) && returnsATypeVariableOfItsClass(element, containers.get(container.getName()));
                }
            });
            return calls[0];
        }

        private boolean isAdded(String ownerInternalName, String name, String descriptor) {
            return members.contains(name + descriptor) || members.contains(hiddenField(ownerInternalName, name + descriptor));
        }

        /** 'v::added': the method reference is a use of the method, by the descriptor of the referenced method */
        @Override
        public void visit(MethodReferenceExpression expression) {
            if (isVariable(expression.getExpression()) && isAdded(expression.getInternalTypeName(), expression.getName(), expression.getDescriptor())) {
                found = true;
            }
            super.visit(expression);
        }

        @Override
        public void visit(FieldReferenceExpression expression) {
            if (isVariable(expression.getExpression())
                    && (members.contains(expression.getName()) || members.contains(hiddenField(expression.getInternalTypeName(), expression.getName())))) {
                found = true;
            }
            super.visit(expression);
        }

        private boolean isVariable(Expression expression) {
            // A local class or an anonymous class reaches the variable by a captured field of itself, which is named like the variable: 'holder.name' is no capture
            // The receiver may be the variable itself or a call which returns it: 'Objects.requireNonNull(b).added()'
            return references(expression, variableNames) != null
                    || expression instanceof FieldReferenceExpression field && (field.getExpression() == null || field.getExpression() instanceof ThisExpression)
                    && variableNames.contains(field.getName()) && declaredFields.stream().noneMatch(fields -> fields.contains(field.getName()));
        }
    }
}
