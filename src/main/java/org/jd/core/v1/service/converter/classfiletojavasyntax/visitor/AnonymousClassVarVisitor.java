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
import org.jd.core.v1.model.javasyntax.expression.BaseExpression;
import org.jd.core.v1.model.javasyntax.statement.BaseStatement;
import org.jd.core.v1.model.javasyntax.declaration.LocalVariableDeclaration;
import org.jd.core.v1.model.javasyntax.declaration.BaseLocalVariableDeclarator;
import org.jd.core.v1.model.javasyntax.declaration.ConstructorDeclaration;
import org.jd.core.v1.model.javasyntax.declaration.Declaration;
import org.jd.core.v1.model.javasyntax.declaration.ExpressionVariableInitializer;
import org.jd.core.v1.model.javasyntax.declaration.FieldDeclarator;
import org.jd.core.v1.model.javasyntax.declaration.FormalParameter;
import org.jd.core.v1.model.javasyntax.declaration.LocalVariableDeclarator;
import org.jd.core.v1.model.javasyntax.declaration.MethodDeclaration;
import org.jd.core.v1.model.javasyntax.declaration.VariableInitializer;
import org.jd.core.v1.model.javasyntax.expression.Expression;
import org.jd.core.v1.model.javasyntax.expression.FieldReferenceExpression;
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
import org.jd.core.v1.service.converter.classfiletojavasyntax.model.javasyntax.declaration.ClassFileBodyDeclaration;
import org.jd.core.v1.service.converter.classfiletojavasyntax.model.javasyntax.declaration.ClassFileFieldDeclaration;
import org.jd.core.v1.service.converter.classfiletojavasyntax.model.javasyntax.declaration.ClassFileConstructorOrMethodDeclaration;
import org.jd.core.v1.service.converter.classfiletojavasyntax.model.javasyntax.expression.ClassFileNewExpression;
import org.jd.core.v1.service.converter.classfiletojavasyntax.util.ClassMetadata;
import org.jd.core.v1.service.converter.classfiletojavasyntax.util.TypeMaker;

import java.util.HashSet;
import java.util.List;
import java.util.ListIterator;
import java.util.Set;
import java.util.function.Consumer;

/**
 * The type of an anonymous class cannot be written: 'var printer = new Object() { void println(String s) {...} }; printer.println("...")' is the
 * only way to call a member which the anonymous class adds to its supertype. A local variable initialized by such an instance and used to call
 * one of these members is declared 'var'.
 */
public class AnonymousClassVarVisitor extends AbstractJavaSyntaxVisitor {
    private final TypeMaker typeMaker;
    private final Loader loader;

    public AnonymousClassVarVisitor(TypeMaker typeMaker, Loader loader) {
        this.typeMaker = typeMaker;
        this.loader = loader;
    }
    /** A type variable is written by its name only: no import */
    private static final GenericType VAR = new GenericType("var");
    /** The methods of Object, by name and descriptor: an overload of one of them is a new method, an override is not */
    private static final Set<String> OBJECT_METHODS = Set.of("toString()Ljava/lang/String;", "equals(Ljava/lang/Object;)Z", "hashCode()I", "clone()Ljava/lang/Object;", "finalize()V");

    private static final Set<String> PROTECTED_OBJECT_METHODS = Set.of("clone()Ljava/lang/Object;", "finalize()V");

    @Override
    public void visit(Statements statements) {
        ListIterator<Statement> iterator = statements.listIterator();

        while (iterator.hasNext()) {
            Statement statement = iterator.next();

            if (statement instanceof LocalVariableDeclarationStatement declaration && isDeclaredWithVar(declaration, statements, iterator.nextIndex())) {
                iterator.set(new LocalVariableDeclarationStatement(VAR, declaration.getLocalVariableDeclarators()));
            }
            iterator.previous().accept(this);
            iterator.next();
        }
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

        if (declaration != null && needsVar(declaration.getLocalVariableDeclarators(), search -> {
            for (Object node : new Object[] { statement.getCondition(), statement.getUpdate(), statement.getStatements() }) {
                if (node instanceof BaseExpression expressions) {
                    expressions.accept(search);
                } else if (node instanceof BaseStatement statements) {
                    statements.accept(search);
                }
            }
        })) {
            LocalVariableDeclaration var = new LocalVariableDeclaration(VAR, declaration.getLocalVariableDeclarators());

            var.setFinal(declaration.isFinal());
            statement.setDeclaration(var);
        }
        super.visit(statement);
    }

    private boolean needsVar(BaseLocalVariableDeclarator declarators, Consumer<MemberUseSearch> uses) {
        if (!(declarators instanceof LocalVariableDeclarator declarator) || declarator.getDimension() != 0) {
            return false;
        }

        VariableInitializer initializer = declarator.getVariableInitializer();

        if (!(initializer instanceof ExpressionVariableInitializer evi) || !(evi.getExpression() instanceof ClassFileNewExpression newExpression)
                || !(newExpression.getBodyDeclaration() instanceof ClassFileBodyDeclaration body)) {
            return false;
        }

        Set<String> members = addedMembers(body, newExpression.getObjectType().getInternalName());

        if (members.isEmpty()) {
            return false;
        }

        MemberUseSearch search = new MemberUseSearch(declarator.getName(), members);

        uses.accept(search);
        return search.found;
    }

    /** A method of the anonymous class which overrides a method of its super type: the type of the super type is enough to call it */
    private boolean isInherited(Method method, String superInternalName) {
        // A public override of a protected method (clone, finalize) can be called through the anonymous type only
        boolean widens = method.isPublic() && PROTECTED_OBJECT_METHODS.contains(method.getName() + method.getSignature());

        if (OBJECT_METHODS.contains(method.getName() + method.getSignature())) {
            return !widens || ClassMetadata.declaresPublicMethod(loader, superInternalName, method.getName(), method.getSignature());
        }
        if (ClassMetadata.isLoadable(loader, superInternalName)) {
            return method.isPublic()
                    ? ClassMetadata.declaresPublicMethod(loader, superInternalName, method.getName(), method.getSignature())
                    : ClassMetadata.declaresMethod(loader, superInternalName, method.getName(), method.getSignature());
        }
        // The super type cannot be read: the types which are known to the type maker
        return typeMaker.matchCount(superInternalName, method.getName(), Type.getArgumentTypes(method.getSignature()).length, false) > 0;
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
        if (body.getMethodDeclarations() == null) {
            return;
        }
        for (ClassFileConstructorOrMethodDeclaration method : body.getMethodDeclarations()) {
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
        if (body.getFieldDeclarations() == null) {
            return;
        }
        AbstractJavaSyntaxVisitor fieldNames = new AbstractJavaSyntaxVisitor() {
            @Override
            public void visit(FieldDeclarator declarator) {
                // A field which hides the one of the super type is only referenced by the anonymous class as owner
                members.add(ClassMetadata.declaresField(loader, superInternalName, declarator.getName())
                        ? hiddenField(body.getInternalTypeName(), declarator.getName()) : declarator.getName());
            }
        };
        for (ClassFileFieldDeclaration field : body.getFieldDeclarations()) {
            field.getFieldDeclarators().accept(fieldNames);
        }
    }

    private static String hiddenField(String ownerInternalName, String name) {
        return "@" + ownerInternalName + "." + name;
    }

    private static final class MemberUseSearch extends AbstractJavaSyntaxVisitor {
        private final String variableName;
        private final Set<String> members;
        private boolean found;

        MemberUseSearch(String variableName, Set<String> members) {
            this.variableName = variableName;
            this.members = members;
        }

        /** A method of a local or anonymous class which declares a variable of this name uses its own, not the one of the enclosing method */
        @Override
        public void visit(MethodDeclaration declaration) {
            if (!declaresVariable(declaration)) {
                super.visit(declaration);
            }
        }

        @Override
        public void visit(ConstructorDeclaration declaration) {
            if (!declaresVariable(declaration)) {
                super.visit(declaration);
            }
        }

        private boolean declaresVariable(Declaration declaration) {
            boolean[] declared = new boolean[1];

            declaration.accept(new AbstractJavaSyntaxVisitor() {
                @Override
                public void visit(FormalParameter parameter) {
                    declared[0] |= variableName.equals(parameter.getName());
                }

                @Override
                public void visit(LocalVariableDeclarator declarator) {
                    declared[0] |= variableName.equals(declarator.getName());
                }

                @Override
                public void visit(TryStatement.CatchClause catchClause) {
                    declared[0] |= variableName.equals(catchClause.getName());
                    super.visit(catchClause);
                }

                @Override
                public void visit(ForEachStatement statement) {
                    declared[0] |= variableName.equals(statement.getName());
                    super.visit(statement);
                }
            });
            return declared[0];
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
            return expression instanceof LocalVariableReferenceExpression reference && variableName.equals(reference.getName())
                    || expression instanceof FieldReferenceExpression field && (field.getExpression() == null || field.getExpression() instanceof ThisExpression) && variableName.equals(field.getName());
        }
    }
}
