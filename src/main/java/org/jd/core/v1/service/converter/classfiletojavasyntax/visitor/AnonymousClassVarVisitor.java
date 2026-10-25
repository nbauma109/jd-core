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
import org.jd.core.v1.model.javasyntax.declaration.BaseLocalVariableDeclarator;
import org.jd.core.v1.model.javasyntax.declaration.ExpressionVariableInitializer;
import org.jd.core.v1.model.javasyntax.declaration.FieldDeclarator;
import org.jd.core.v1.model.javasyntax.declaration.LocalVariableDeclarator;
import org.jd.core.v1.model.javasyntax.declaration.VariableInitializer;
import org.jd.core.v1.model.javasyntax.expression.Expression;
import org.jd.core.v1.model.javasyntax.expression.FieldReferenceExpression;
import org.jd.core.v1.model.javasyntax.expression.LocalVariableReferenceExpression;
import org.jd.core.v1.model.javasyntax.expression.MethodInvocationExpression;
import org.jd.core.v1.model.javasyntax.statement.LocalVariableDeclarationStatement;
import org.jd.core.v1.model.javasyntax.statement.Statement;
import org.jd.core.v1.model.javasyntax.statement.Statements;
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
        BaseLocalVariableDeclarator declarators = declaration.getLocalVariableDeclarators();

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

        for (int i = nextIndex; i < statements.size() && !search.found; i++) {
            statements.get(i).accept(search);
        }
        return search.found;
    }

    /** A method of the anonymous class which overrides a method of its super type: the type of the super type is enough to call it */
    private boolean isInherited(Method method, String superInternalName) {
        if (OBJECT_METHODS.contains(method.getName() + method.getSignature())) {
            return true;
        }
        if (ClassMetadata.isLoadable(loader, superInternalName)) {
            return ClassMetadata.declaresMethod(loader, superInternalName, method.getName(), method.getSignature());
        }
        // The super type cannot be read: the types which are known to the type maker
        return typeMaker.matchCount(superInternalName, method.getName(), Type.getArgumentTypes(method.getSignature()).length, false) > 0;
    }

    private Set<String> addedMembers(ClassFileBodyDeclaration body, String superInternalName) {
        Set<String> members = new HashSet<>();

        if (body.getMethodDeclarations() != null) {
            for (ClassFileConstructorOrMethodDeclaration method : body.getMethodDeclarations()) {
                Method m = method.getMethod();

                if (m != null && !"<init>".equals(m.getName()) && !m.isSynthetic() && !isInherited(m, superInternalName)) {
                    // not declared by the type which the anonymous class extends: the variable needs the type of the class to call it
                    // A method is identified by its name and descriptor: a call of the inherited overload does not need the type of the class
                    members.add(m.getName() + m.getSignature());
                }
            }
        }
        if (body.getFieldDeclarations() != null) {
            AbstractJavaSyntaxVisitor fieldNames = new AbstractJavaSyntaxVisitor() {
                @Override
                public void visit(FieldDeclarator declarator) {
                    // A field which hides the one of the super type is not the one which 'holder.field' refers to
                    if (!ClassMetadata.declaresField(loader, superInternalName, declarator.getName())) {
                        members.add(declarator.getName());
                    }
                }
            };
            for (ClassFileFieldDeclaration field : body.getFieldDeclarations()) {
                field.getFieldDeclarators().accept(fieldNames);
            }
        }
        return members;
    }

    private static final class MemberUseSearch extends AbstractJavaSyntaxVisitor {
        private final String variableName;
        private final Set<String> members;
        private boolean found;

        MemberUseSearch(String variableName, Set<String> members) {
            this.variableName = variableName;
            this.members = members;
        }

        @Override
        public void visit(MethodInvocationExpression expression) {
            if (isVariable(expression.getExpression()) && members.contains(expression.getName() + expression.getDescriptor())) {
                found = true;
            }
            super.visit(expression);
        }

        @Override
        public void visit(FieldReferenceExpression expression) {
            if (isVariable(expression.getExpression()) && members.contains(expression.getName())) {
                found = true;
            }
            super.visit(expression);
        }

        private boolean isVariable(Expression expression) {
            return expression instanceof LocalVariableReferenceExpression reference && variableName.equals(reference.getName());
        }
    }
}
