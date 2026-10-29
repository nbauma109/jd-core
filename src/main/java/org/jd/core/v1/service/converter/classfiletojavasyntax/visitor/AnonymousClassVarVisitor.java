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
import org.jd.core.v1.model.javasyntax.expression.CastExpression;
import org.jd.core.v1.model.javasyntax.expression.Expression;
import org.jd.core.v1.model.javasyntax.expression.FieldReferenceExpression;
import org.jd.core.v1.model.javasyntax.expression.TernaryOperatorExpression;
import org.jd.core.v1.model.javasyntax.expression.ThisExpression;
import org.jd.core.v1.model.javasyntax.expression.LocalVariableReferenceExpression;
import org.jd.core.v1.model.javasyntax.expression.MethodInvocationExpression;
import org.jd.core.v1.model.javasyntax.expression.MethodReferenceExpression;
import org.jd.core.v1.model.javasyntax.statement.ForEachStatement;
import org.jd.core.v1.model.javasyntax.statement.LocalVariableDeclarationStatement;
import org.jd.core.v1.model.javasyntax.statement.Statement;
import org.jd.core.v1.model.javasyntax.statement.Statements;
import org.jd.core.v1.model.javasyntax.statement.TryStatement;
import org.jd.core.v1.model.javasyntax.type.GenericType;
import org.jd.core.v1.model.javasyntax.type.ObjectType;
import org.jd.core.v1.service.converter.classfiletojavasyntax.model.javasyntax.declaration.ClassFileBodyDeclaration;
import org.jd.core.v1.service.converter.classfiletojavasyntax.model.javasyntax.declaration.ClassFileFieldDeclaration;
import org.jd.core.v1.service.converter.classfiletojavasyntax.model.javasyntax.declaration.ClassFileConstructorOrMethodDeclaration;
import org.jd.core.v1.service.converter.classfiletojavasyntax.model.javasyntax.expression.ClassFileNewExpression;
import org.jd.core.v1.service.converter.classfiletojavasyntax.util.ClassMetadata;
import org.jd.core.v1.service.converter.classfiletojavasyntax.util.LocalVariableMaker;
import org.jd.core.v1.service.converter.classfiletojavasyntax.util.TypeMaker;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Consumer;
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
    private static final String CLONE = "clone()Ljava/lang/Object;";
    private static final String FINALIZE = "finalize()V";
    /** The methods of Object, by name and descriptor: an overload of one of them is a new method, an override is not */
    private static final Set<String> OBJECT_METHODS = Set.of("toString()Ljava/lang/String;", "equals(Ljava/lang/Object;)Z", "hashCode()I", CLONE, FINALIZE);

    private static final Map<String, Set<String>> OBJECT_METHOD_EXCEPTIONS = Map.of(CLONE, Set.of("java/lang/CloneNotSupportedException"),
            FINALIZE, Set.of("java/lang/Throwable"));

    private static final Set<String> PROTECTED_OBJECT_METHODS = Set.of(CLONE, FINALIZE);

    /** The variables which are declared 'var' because of their anonymous class, with the members which it adds: 'var c = b;' has the type of 'b' */
    private final Map<String, Set<String>> anonymousVariables = new HashMap<>();

    @Override
    public void visit(Statements statements) {
        // The variables of a block are known up to its end only: another block may reuse their names
        Set<String> knownBefore = new HashSet<>(anonymousVariables.keySet());

        for (int i = 0; i < statements.size(); i++) {
            if (statements.get(i) instanceof LocalVariableDeclarationStatement declaration && needsOneDeclaratorEach(declaration)) {
                split(statements, i, declaration);
            }
            if (statements.get(i) instanceof LocalVariableDeclarationStatement declaration
                    && (isDeclaredWithVar(declaration, statements, i + 1) || isAliasWhichNeedsVar(declaration, statements, i + 1))) {
                statements.set(i, new LocalVariableDeclarationStatement(VAR, declaration.getLocalVariableDeclarators()));
            }
            statements.get(i).accept(this);
        }
        anonymousVariables.keySet().retainAll(knownBefore);
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

    /** 'Object c = b;' keeps its type, 'c.added()' needs the type of the anonymous class of 'b' */
    private boolean isAliasWhichNeedsVar(LocalVariableDeclarationStatement declaration, List<Statement> statements, int nextIndex) {
        return aliasNeedsVar(declaration.getLocalVariableDeclarators(), search -> {
            for (int i = nextIndex; i < statements.size() && !search.found; i++) {
                statements.get(i).accept(search);
            }
        });
    }

    private boolean aliasNeedsVar(BaseLocalVariableDeclarator declarators, Consumer<MemberUseSearch> uses) {
        if (!(declarators instanceof LocalVariableDeclarator declarator) || declarator.getDimension() != 0
                || !(declarator.getVariableInitializer() instanceof ExpressionVariableInitializer initializer)
                || references(initializer.getExpression(), anonymousVariables.keySet()) == null) {
            return false;
        }

        Set<String> members = anonymousVariables.get(references(initializer.getExpression(), anonymousVariables.keySet()));
        MemberUseSearch search = new MemberUseSearch(declarator.getName(), members);

        uses.accept(search);
        if (search.found) {
            anonymousVariables.put(declarator.getName(), members);
        }
        return search.found;
    }

    private boolean isDeclaredWithVar(LocalVariableDeclarationStatement declaration, List<Statement> statements, int nextIndex) {
        return needsVar(declaration.getLocalVariableDeclarators(), search -> {
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

        if (declaration != null && (needsVar(declaration.getLocalVariableDeclarators(), uses) || aliasNeedsVar(declaration.getLocalVariableDeclarators(), uses))) {
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

        if (newExpression == null || !(newExpression.getBodyDeclaration() instanceof ClassFileBodyDeclaration body)) {
            return false;
        }

        Set<String> members = addedMembers(body, newExpression.getObjectType().getInternalName());

        if (members.isEmpty()) {
            return false;
        }

        MemberUseSearch search = new MemberUseSearch(name, members);

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
        } else if (value instanceof MethodInvocationExpression call && call.getParameters() != null) {
            List<Expression> arguments = new ArrayList<>();

            call.getParameters().forEach(arguments::add);
            for (int position : positionsOfTheReturnedTypeVariable(call)) {
                if (position < arguments.size()) {
                    values.addAll(underlyingValues(arguments.get(position)));
                }
            }
        } else if (!(value instanceof CastExpression) && !(value instanceof MethodInvocationExpression)) {
            values.add(value);
        }
        return values;
    }

    /** '&lt;T&gt; T f(T first, Object ignored)': the positions of the parameters whose type is the type variable which the generic signature of the method returns */
    private List<Integer> positionsOfTheReturnedTypeVariable(MethodInvocationExpression call) {
        String signature = ClassMetadata.declaredGenericSignature(loader, call.getInternalTypeName(), call.getName(), call.getDescriptor());
        List<Integer> positions = new ArrayList<>();

        if (signature == null || signature.indexOf(')') < 0) {
            return positions;
        }

        // The signature ends with the exceptions which the method throws, '^TE;', if they are type variables
        String returned = signature.substring(signature.indexOf(')') + 1).split("\\^")[0];
        int index = signature.indexOf('(') + 1;
        int position = 0;

        while (returned.startsWith("T") && index < signature.indexOf(')')) {
            int end = endOfSignatureType(signature, index);

            if (signature.substring(index, end).equals(returned)) {
                positions.add(position);
            }
            index = end;
            position++;
        }
        return positions;
    }

    /** @return the index which follows the type of a signature which starts at the index: a class with type arguments, a type variable, an array or a primitive type */
    private static int endOfSignatureType(String signature, int start) {
        int index = start;
        int depth = 0;

        while (signature.charAt(index) == '[') {
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
                search.found |= search.callsClose();
            };

            if (resource.getType() != null && (needsVar(resource.getName(), resource.getExpression(), uses) || resourceAliasNeedsVar(resource, uses))) {
                TryStatement.Resource varResource = new TryStatement.Resource(VAR, resource.getName(), resource.getExpression());

                varResource.setFinal(resource.isFinal());
                resources.set(i, varResource);
            }
        }
        super.visit(statement);
    }

    /** 'try (var r = b)' where 'b' is a tracked variable */
    private boolean resourceAliasNeedsVar(TryStatement.Resource resource, Consumer<MemberUseSearch> uses) {
        String origin = resource.getExpression() == null ? null : references(resource.getExpression(), anonymousVariables.keySet());

        if (origin == null) {
            return false;
        }

        Set<String> members = anonymousVariables.get(origin);
        MemberUseSearch search = new MemberUseSearch(resource.getName(), members);

        uses.accept(search);
        if (search.found) {
            anonymousVariables.put(resource.getName(), members);
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
                || ClassMetadata.isLoadable(loader, superInternalName) && ClassMetadata.declaresMethod(loader, superInternalName, method.getName(), method.getSignature());
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

            if (m != null && !"<init>".equals(m.getName()) && !m.isSynthetic() && !isInherited(m, superInternalName)) {
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

    private final class MemberUseSearch extends AbstractJavaSyntaxVisitor {
        private final String variableName;
        /** The variable and the variables which are declared with its value: 'var c = b;' */
        private final Set<String> variableNames = new HashSet<>();
        private final Set<String> members;
        /** The fields which the local classes around the code declare: they are no capture of the variable */
        private final Deque<Set<String>> declaredFields = new ArrayDeque<>();
        private boolean found;

        MemberUseSearch(String variableName, Set<String> members) {
            this.variableName = variableName;
            this.members = members;
            variableNames.add(variableName);
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

        /** The parameters of the method itself: the ones of the methods of its local classes hide the variable in those methods only */
        private Set<String> parameterNames(BaseFormalParameter parameters) {
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
                if (statement instanceof LocalVariableDeclarationStatement declaration && declaredNames(declaration).contains(variableName)) {
                    break;
                }
            }
            variableNames.retainAll(knownBefore);
        }

        /** An override of 'close()' which declares fewer exceptions than the one of its super type has to be called through its own type */
        boolean callsClose() {
            return members.stream().anyMatch(member -> member.equals("close()V") || member.endsWith(".close()V"));
        }

        /** 'try (var r = b)' closes the variable: the close is a use of it */
        @Override
        public void visit(TryStatement statement) {
            if (statement.getResources() != null && callsClose()
                    && statement.getResources().stream().anyMatch(resource -> resource.getExpression() != null && references(resource.getExpression(), variableNames) != null)) {
                found = true;
            }
            super.visit(statement);
        }

        @Override
        public void visit(TryStatement.CatchClause catchClause) {
            if (!variableName.equals(catchClause.getName())) {
                super.visit(catchClause);
            }
        }

        @Override
        public void visit(ForEachStatement statement) {
            if (variableName.equals(statement.getName())) {
                statement.getExpression().accept(this);
            } else {
                super.visit(statement);
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
            if (isVariable(expression.getExpression()) && isAdded(expression.getInternalTypeName(), expression.getName(), expression.getDescriptor())) {
                found = true;
            }
            super.visit(expression);
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
