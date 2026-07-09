/*
 * Copyright (c) 2008-2019 Emmanuel Dupuy.
 * This project is distributed under the GPLv3 license.
 * This is a Copyleft license that gives the user the right to use,
 * copy and modify the code freely for non-commercial purposes.
 */

package org.jd.core.v1.service.converter.classfiletojavasyntax.visitor;

import org.jd.core.v1.model.javasyntax.AbstractJavaSyntaxVisitor;
import org.jd.core.v1.model.javasyntax.declaration.LocalVariableDeclarator;
import org.jd.core.v1.model.javasyntax.expression.Expression;
import org.jd.core.v1.model.javasyntax.expression.LambdaIdentifiersExpression;
import org.jd.core.v1.model.javasyntax.expression.LocalVariableReferenceExpression;
import org.jd.core.v1.service.converter.classfiletojavasyntax.model.javasyntax.expression.ClassFileLocalVariableReferenceExpression;
import org.jd.core.v1.service.converter.classfiletojavasyntax.model.localvariable.AbstractLocalVariable;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class SearchLocalVariableReferenceVisitor extends AbstractJavaSyntaxVisitor {
    private int index;
    private boolean found;
    private String name;
    private Deque<LambdaIdentifiersExpression> lambdas = new ArrayDeque<>();
    private Set<String> namesBoundInLambdas = new HashSet<>();

    public void init(int index, String name) {
        this.index = index;
        this.name = name;
        this.found = false;
    }

    public void init(AbstractLocalVariable localVariable) {
        init(localVariable.getIndex(), localVariable.getName());
    }

    public boolean containsReference() {
        return found;
    }

    /**
     * Checks whether {@code expression} references a local variable declared outside of it
     * (a genuine capture), ignoring local variables that are declared and only ever used within
     * a lambda body nested inside {@code expression} (lambda parameters or lambda-body locals).
     */
    public boolean containsExternalReference(Expression expression) {
        lambdas.clear();
        namesBoundInLambdas.clear();
        init(-1, null);
        expression.accept(this);
        return found;
    }

    @Override
    public void visit(LocalVariableReferenceExpression expression) {
        if (index < 0) {
            // Only a genuine capture of a variable declared outside every enclosing lambda blocks
            // extraction; lambda parameters and locals declared within the lambda body itself are
            // self-contained.
            if (lambdas.isEmpty() || (!isLambdaParameter(expression.getName()) && !namesBoundInLambdas.contains(expression.getName()))) {
                found = true;
            }
        } else {
            ClassFileLocalVariableReferenceExpression referenceExpression = (ClassFileLocalVariableReferenceExpression) expression;
            if (lambdas.isEmpty()) {
                found |= referenceExpression.getLocalVariable().getIndex() == index;
            } else {
                found |= referenceExpression.getLocalVariable().getName().equals(name);
            }
        }
    }

    @Override
    public void visit(LambdaIdentifiersExpression expression) {
        lambdas.push(expression);
        super.visit(expression);
        lambdas.pop();
    }

    @Override
    public void visit(LocalVariableDeclarator declarator) {
        if (!lambdas.isEmpty()) {
            namesBoundInLambdas.add(declarator.getName());
        }
        super.visit(declarator);
    }

    private boolean isLambdaParameter(String name) {
        for (LambdaIdentifiersExpression lambda : lambdas) {
            List<String> parameterNames = lambda.getParameterNames();
            if (parameterNames != null && parameterNames.contains(name)) {
                return true;
            }
        }
        return false;
    }
}
