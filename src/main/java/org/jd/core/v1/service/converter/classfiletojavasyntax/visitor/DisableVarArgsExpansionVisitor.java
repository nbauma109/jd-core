/*
 * Copyright (c) 2008, 2019 Emmanuel Dupuy.
 * This project is distributed under the GPLv3 license.
 * This is a Copyleft license that gives the user the right to use,
 * copy and modify the code freely for non-commercial purposes.
 */
package org.jd.core.v1.service.converter.classfiletojavasyntax.visitor;

import org.jd.core.v1.model.javasyntax.declaration.ArrayVariableInitializer;
import org.jd.core.v1.model.javasyntax.declaration.BaseFormalParameter;
import org.jd.core.v1.model.javasyntax.declaration.ExpressionVariableInitializer;
import org.jd.core.v1.model.javasyntax.declaration.FormalParameter;
import org.jd.core.v1.model.javasyntax.declaration.MethodDeclaration;
import org.jd.core.v1.model.javasyntax.declaration.VariableInitializer;
import org.jd.core.v1.model.javasyntax.expression.BaseExpression;
import org.jd.core.v1.model.javasyntax.expression.Expression;
import org.jd.core.v1.model.javasyntax.expression.NewInitializedArray;
import org.jd.core.v1.service.converter.classfiletojavasyntax.model.javasyntax.expression.ClassFileMethodInvocationExpression;

import java.util.ArrayList;
import java.util.List;
import java.util.ListIterator;

public class DisableVarArgsExpansionVisitor extends AbstractUpdateExpressionVisitor {
    private String currentMethodName;
    private List<String> currentParameterNames;

    @Override
    public void visit(MethodDeclaration declaration) {
        String previousMethodName = currentMethodName;
        List<String> previousParameterNames = currentParameterNames;
        currentMethodName = declaration.getName();
        currentParameterNames = collectParameterNames(declaration.getFormalParameters());
        super.visit(declaration);
        currentMethodName = previousMethodName;
        currentParameterNames = previousParameterNames;
    }

    @Override
    protected Expression updateExpression(Expression expression) {
        if (expression instanceof ClassFileMethodInvocationExpression invocation) {
            if (shouldDisableVarArgs(invocation)) {
                invocation.setVarArgsOverride(Boolean.FALSE);
            }
        }
        return expression;
    }

    private boolean shouldDisableVarArgs(ClassFileMethodInvocationExpression invocation) {
        if (!invocation.isVarArgs()) {
            return false;
        }

        BaseExpression parameters = invocation.getParameters();
        if (parameters == null || parameters.size() != 1) {
            return false;
        }

        return shouldDisableVarArgsExpansion(invocation, parameters.getFirst());
    }

    private boolean shouldDisableVarArgsExpansion(ClassFileMethodInvocationExpression invocation, Expression parameter) {
        if (currentMethodName == null || currentParameterNames == null) {
            return false;
        }

        if (!currentMethodName.equals(invocation.getName())) {
            return false;
        }

        if (parameter instanceof NewInitializedArray nia) {
            return isParameterForwardingArray(nia.getArrayInitializer());
        }
        return false;
    }

    private static List<String> collectParameterNames(BaseFormalParameter formalParameters) {
        if (formalParameters == null || formalParameters.size() == 0) {
            return null;
        }

        List<String> names = new ArrayList<>(formalParameters.size());
        for (FormalParameter formalParameter : formalParameters) {
            names.add(formalParameter.getName());
        }
        return names;
    }

    private boolean isParameterForwardingArray(ArrayVariableInitializer initializer) {
        if (initializer == null || currentParameterNames == null) {
            return false;
        }
        if (initializer.size() != currentParameterNames.size()) {
            return false;
        }

        ListIterator<VariableInitializer> iterator = initializer.listIterator();
        int index = 0;
        while (iterator.hasNext()) {
            VariableInitializer variableInitializer = iterator.next();
            if (!(variableInitializer instanceof ExpressionVariableInitializer evi)) {
                return false;
            }
            Expression element = evi.getExpression();
            if (!element.isLocalVariableReferenceExpression()) {
                return false;
            }
            String expectedName = currentParameterNames.get(index++);
            if (!expectedName.equals(element.getName())) {
                return false;
            }
        }
        return true;
    }
}
