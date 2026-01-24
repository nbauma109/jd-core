/*
 * Copyright (c) 2008, 2019 Emmanuel Dupuy.
 * This project is distributed under the GPLv3 license.
 * This is a Copyleft license that gives the user the right to use,
 * copy and modify the code freely for non-commercial purposes.
 */

package org.jd.core.v1.service.converter.classfiletojavasyntax.util;

import org.jd.core.v1.model.javasyntax.AbstractJavaSyntaxVisitor;
import org.jd.core.v1.model.javasyntax.expression.BinaryOperatorExpression;
import org.jd.core.v1.model.javasyntax.expression.Expression;
import org.jd.core.v1.model.javasyntax.expression.LocalVariableReferenceExpression;
import org.jd.core.v1.model.javasyntax.expression.MethodInvocationExpression;
import org.jd.core.v1.model.javasyntax.expression.NullExpression;
import org.jd.core.v1.model.javasyntax.statement.LocalVariableDeclarationStatement;
import org.jd.core.v1.model.javasyntax.statement.BaseStatement;
import org.jd.core.v1.model.javasyntax.statement.ExpressionStatement;
import org.jd.core.v1.model.javasyntax.statement.IfStatement;
import org.jd.core.v1.model.javasyntax.statement.Statement;
import org.jd.core.v1.model.javasyntax.statement.Statements;
import org.jd.core.v1.model.javasyntax.statement.ThrowStatement;
import org.jd.core.v1.model.javasyntax.statement.TryStatement;
import org.jd.core.v1.model.javasyntax.statement.TryStatement.CatchClause;
import org.jd.core.v1.model.javasyntax.type.ObjectType;
import org.jd.core.v1.model.javasyntax.type.Type;
import org.jd.core.v1.model.javasyntax.declaration.BaseFormalParameter;
import org.jd.core.v1.model.javasyntax.declaration.BaseLocalVariableDeclarator;
import org.jd.core.v1.model.javasyntax.declaration.FormalParameter;
import org.jd.core.v1.model.javasyntax.declaration.LocalVariableDeclarator;
import org.jd.core.v1.service.converter.classfiletojavasyntax.model.javasyntax.declaration.ClassFileFormalParameter;
import org.jd.core.v1.service.converter.classfiletojavasyntax.model.javasyntax.declaration.ClassFileLocalVariableDeclarator;
import org.jd.core.v1.service.converter.classfiletojavasyntax.model.javasyntax.expression.ClassFileLocalVariableReferenceExpression;
import org.jd.core.v1.service.converter.classfiletojavasyntax.model.javasyntax.statement.ClassFileTryStatement;
import org.jd.core.v1.service.converter.classfiletojavasyntax.model.localvariable.AbstractLocalVariable;
import org.jd.core.v1.service.converter.classfiletojavasyntax.visitor.SearchFirstLineNumberVisitor;
import org.jd.core.v1.util.DefaultList;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

public final class TryWithResourcesStatementMaker {
    private TryWithResourcesStatementMaker() {
    }

    private static final class ResourceAssignment {
        private final AbstractLocalVariable localVariable;
        private final Expression expression;

        private ResourceAssignment(AbstractLocalVariable localVariable, Expression expression) {
            this.localVariable = localVariable;
            this.expression = expression;
        }
    }

    private static final class ResourceInfo {
        private final AbstractLocalVariable localVariable;
        private Expression expression;
        private boolean hasDeclaration;

        private ResourceInfo(AbstractLocalVariable localVariable, Expression expression, boolean hasDeclaration) {
            this.localVariable = localVariable;
            this.expression = expression;
            this.hasDeclaration = hasDeclaration;
        }
    }

    private static final class ResourceChain {
        private final DefaultList<ResourceInfo> resources;
        private final Statements bodyStatements;

        private ResourceChain(DefaultList<ResourceInfo> resources, Statements bodyStatements) {
            this.resources = resources;
            this.bodyStatements = bodyStatements;
        }
    }

    private static final class CloseInvocationInfo {
        private final ClassFileTryStatement tryStatement;
        private final AbstractLocalVariable localVariable;
        private final Expression expression;

        private CloseInvocationInfo(
                ClassFileTryStatement tryStatement,
                AbstractLocalVariable localVariable,
                Expression expression) {
            this.tryStatement = tryStatement;
            this.localVariable = localVariable;
            this.expression = expression;
        }
    }

    private static final class AddSuppressedInvocationVisitor extends AbstractJavaSyntaxVisitor {
        private final int primaryIndex;
        private final boolean matchAny;
        private MethodInvocationExpression invocation;

        private AddSuppressedInvocationVisitor(AbstractLocalVariable primaryException) {
            if (primaryException == null) {
                this.primaryIndex = -1;
                this.matchAny = true;
            } else {
                this.primaryIndex = primaryException.getIndex();
                this.matchAny = false;
            }
        }

        public MethodInvocationExpression getInvocation() {
            return invocation;
        }

        @Override
        public void visit(MethodInvocationExpression expression) {
            if (invocation != null) {
                return;
            }
            if ("addSuppressed".equals(expression.getName()) && "(Ljava/lang/Throwable;)V".equals(expression.getDescriptor())) {
                Expression target = expression.getExpression();
                if (target.isLocalVariableReferenceExpression()) {
                    AbstractLocalVariable localVariable = ((ClassFileLocalVariableReferenceExpression) target).getLocalVariable();
                    if (matchAny || localVariable.getIndex() == primaryIndex) {
                        invocation = expression;
                        return;
                    }
                }
            }
            super.visit(expression);
        }
    }

    private static final class CloseInvocationInCatchVisitor extends AbstractJavaSyntaxVisitor {
        private MethodInvocationExpression invocation;
        private AbstractLocalVariable localVariable;

        public MethodInvocationExpression getInvocation() {
            return invocation;
        }

        public AbstractLocalVariable getLocalVariable() {
            return localVariable;
        }

        @Override
        public void visit(TryStatement statement) {
            // Skip nested try statements to avoid matching inner resource closures.
        }

        @Override
        public void visit(MethodInvocationExpression expression) {
            if (invocation != null) {
                return;
            }
            AbstractLocalVariable closeLocalVariable = getCloseInvocationLocalVariable(expression);
            if (closeLocalVariable != null) {
                invocation = expression;
                localVariable = closeLocalVariable;
                return;
            }
            super.visit(expression);
        }
    }

    private static final class CloseInvocationInfoVisitor extends AbstractJavaSyntaxVisitor {
        private CloseInvocationInfo info;

        public CloseInvocationInfo getInfo() {
            return info;
        }

        @Override
        public void visit(TryStatement statement) {
            super.visit(statement);
            if (statement instanceof ClassFileTryStatement tryStatement) {
                CloseInvocationInfo found = getCloseInvocationInfoFromFinally(tryStatement);
                if (found != null) {
                    info = found;
                }
            }
        }
    }

    private static final class ResourceCollectorVisitor extends AbstractJavaSyntaxVisitor {
        private final DefaultList<TryStatement.Resource> resources = new DefaultList<>();

        public DefaultList<TryStatement.Resource> getResources() {
            return resources;
        }

        @Override
        public void visit(TryStatement statement) {
            if (statement.getResources() != null && !statement.getResources().isEmpty()) {
                resources.addAll(statement.getResources());
            }
            super.visit(statement);
        }
    }

    private static final class CloseInvocationExpressionCollector extends AbstractJavaSyntaxVisitor {
        private final DefaultList<Expression> expressions = new DefaultList<>();

        public DefaultList<Expression> getExpressions() {
            return expressions;
        }

        @Override
        public void visit(TryStatement statement) {
            // Skip nested try statements to avoid inner resource closures.
        }

        @Override
        public void visit(MethodInvocationExpression expression) {
            AbstractLocalVariable closeLocalVariable = getCloseInvocationLocalVariable(expression);
            if (closeLocalVariable != null) {
                expressions.add(expression.getExpression());
                return;
            }
            super.visit(expression);
        }
    }

    private static final class LocalVariableReferenceVisitor extends AbstractJavaSyntaxVisitor {
        private final AbstractLocalVariable localVariable;
        private boolean found;

        private LocalVariableReferenceVisitor(AbstractLocalVariable localVariable) {
            this.localVariable = localVariable;
        }

        public boolean isFound() {
            return found;
        }

        @Override
        public void visit(LocalVariableReferenceExpression expression) {
            if (found) {
                return;
            }
            if (((ClassFileLocalVariableReferenceExpression) expression).getLocalVariable() == localVariable) {
                found = true;
                return;
            }
            super.visit(expression);
        }
    }

    public static Statement makeLegacy(
            LocalVariableMaker localVariableMaker, Statements statements, Statements tryStatements,
            DefaultList<TryStatement.CatchClause> catchClauses, Statements finallyStatements) {
        int size = statements.size();

        if (size < 2 || finallyStatements == null || finallyStatements.size() != 1 || !checkThrowable(catchClauses)) {
            return null;
        }

        Statement statement = statements.get(size - 2);

        if (!statement.isExpressionStatement()) {
            return null;
        }

        Expression expression = statement.getExpression();

        if (!expression.isBinaryOperatorExpression()) {
            return null;
        }

        Expression boe = expression;

        expression = boe.getLeftExpression();

        if (!expression.isLocalVariableReferenceExpression()) {
            return null;
        }

        AbstractLocalVariable lv1 = ((ClassFileLocalVariableReferenceExpression) expression).getLocalVariable();

        statement = statements.get(size - 1);

        if (!statement.isExpressionStatement()) {
            return null;
        }

        expression = statement.getExpression();

        if (!expression.isBinaryOperatorExpression()) {
            return null;
        }

        expression = expression.getLeftExpression();

        if (!expression.isLocalVariableReferenceExpression()) {
            return null;
        }

        AbstractLocalVariable lv2 = ((ClassFileLocalVariableReferenceExpression) expression).getLocalVariable();

        statement = finallyStatements.getFirst();

        Statement result = null;
        if (statement.isIfStatement() && lv1 == getLocalVariable(statement.getCondition())) {
            statement = statement.getStatements().getFirst();

            if (statement.isIfElseStatement()) {
                result = parsePatternAddSuppressed(localVariableMaker, statements, tryStatements, finallyStatements, boe, lv1, lv2, statement);
            }
            if (statement.isExpressionStatement()) {
                result = parsePatternCloseResource(localVariableMaker, statements, tryStatements, finallyStatements, boe, lv1, lv2, statement);
            }
        }

        if (result == null && statement.isExpressionStatement()) {
            result = parsePatternCloseResource(localVariableMaker, statements, tryStatements, finallyStatements, boe, lv1, lv2, statement);
        }

        if (result != null && !statements.isEmpty()) {
            removeTrailingResourceAssignments(statements, lv1, lv2);
            removeCatchClauseLocalVariables(localVariableMaker, catchClauses);
        }

        return result;
    }

    public static Statement make(
            LocalVariableMaker localVariableMaker, Statements statements, Statements tryStatements,
            DefaultList<TryStatement.CatchClause> catchClauses, Statements finallyStatements,
            boolean allowEcjPattern, boolean allowResourceExpression) {

        if (!checkThrowable(catchClauses)) {
            return null;
        }

        ResourceAssignment resourceAssignment = findResourceAssignment(statements);
        AbstractLocalVariable lv1 = resourceAssignment == null ? null : resourceAssignment.localVariable;
        Expression resourceExpression = resourceAssignment == null ? null : resourceAssignment.expression;
        boolean hasResourceDeclaration = resourceAssignment != null;

        BaseStatement catchStatements = catchClauses.getFirst().getStatements();

        if (catchStatements == null || catchStatements.size() < 2) {
            return null;
        }

        boolean ecjPatternMatch = false;
        CloseInvocationInfo closeInvocationInfo = getCloseInvocationInfo(catchStatements);
        if (closeInvocationInfo == null) {
            if (allowEcjPattern) {
                closeInvocationInfo = getCloseInvocationInfoFromCatchStatements(catchStatements);
                if (closeInvocationInfo != null) {
                    if (findAddSuppressedInvocation(catchStatements, null) == null) {
                        return null;
                    }
                    ecjPatternMatch = true;
                }
            }
            if (closeInvocationInfo == null) {
                closeInvocationInfo = getCloseInvocationInfoFromTryStatements(tryStatements);
                if (closeInvocationInfo == null) {
                    return null;
                }
                if (!allowEcjPattern && findAddSuppressedInvocation(catchStatements, null) == null) {
                    return null;
                }
                ecjPatternMatch = true;
            }
        }
        if (!catchStatements.getLast().isThrowStatement()) {
            return null;
        }

        if (resourceAssignment == null && closeInvocationInfo != null) {
            resourceAssignment = findResourceAssignmentInStatements(
                    statements,
                    closeInvocationInfo.localVariable,
                    true);
            if (resourceAssignment != null) {
                lv1 = resourceAssignment.localVariable;
                resourceExpression = resourceAssignment.expression;
                hasResourceDeclaration = true;
            }
        }
        if (resourceAssignment == null && closeInvocationInfo != null) {
            resourceAssignment = findResourceAssignmentInTryStatements(
                    tryStatements,
                    closeInvocationInfo.localVariable,
                    true);
            if (resourceAssignment != null) {
                lv1 = resourceAssignment.localVariable;
                resourceExpression = resourceAssignment.expression;
                hasResourceDeclaration = true;
            }
        }

        if (lv1 != null && closeInvocationInfo.localVariable != lv1) {
            return null;
        }

        ThrowStatement throwStatement = (ThrowStatement) catchStatements.getLast();

        Expression expression = throwStatement.getExpression();

        if (!expression.isLocalVariableReferenceExpression()) {
            return null;
        }

        AbstractLocalVariable lv2 = ((ClassFileLocalVariableReferenceExpression) expression).getLocalVariable();

        AbstractLocalVariable suppressedLocalVariable = null;
        AbstractLocalVariable primaryExceptionLocalVariable = lv2;
        Statements tryStatementsForResources = tryStatements;
        if (ecjPatternMatch) {
            MethodInvocationExpression addSuppressedInvocation = findAddSuppressedInvocation(catchStatements, lv2);
            if (addSuppressedInvocation == null) {
                addSuppressedInvocation = findAddSuppressedInvocation(catchStatements, null);
            }
            if (addSuppressedInvocation == null) {
                return null;
            }
            AbstractLocalVariable addSuppressedTarget = getLocalVariableFromExpression(addSuppressedInvocation.getExpression());
            if (addSuppressedTarget != null && addSuppressedTarget != lv2) {
                primaryExceptionLocalVariable = addSuppressedTarget;
            }
            suppressedLocalVariable = getSuppressedLocalVariable(addSuppressedInvocation);

            Expression chainExpression = resourceExpression != null ? resourceExpression : closeInvocationInfo.expression;
            ClassFileTryStatement chainStatement = newTryStatementFromTryChain(
                    localVariableMaker,
                    statements,
                    tryStatements,
                    finallyStatements,
                    primaryExceptionLocalVariable,
                    suppressedLocalVariable,
                    closeInvocationInfo.localVariable,
                    chainExpression,
                    hasResourceDeclaration,
                    allowResourceExpression);
            if (chainStatement != null) {
                return chainStatement;
            }

            if (resourceAssignment == null) {
                resourceAssignment = findResourceAssignmentInTryStatements(
                        tryStatements,
                        closeInvocationInfo.localVariable,
                        true);
                if (resourceAssignment != null) {
                    lv1 = resourceAssignment.localVariable;
                    resourceExpression = resourceAssignment.expression;
                    hasResourceDeclaration = true;
                }
            }

            if (resourceAssignment == null) {
                resourceAssignment = findResourceAssignmentInTryStatements(
                        tryStatementsForResources,
                        closeInvocationInfo.localVariable,
                        true);
                if (resourceAssignment != null) {
                    lv1 = resourceAssignment.localVariable;
                    resourceExpression = resourceAssignment.expression;
                    hasResourceDeclaration = true;
                }
            }
        } else {
            ClassFileTryStatement tryStatement = closeInvocationInfo.tryStatement;
            if (tryStatement.getCatchClauses() == null || tryStatement.getCatchClauses().size() != 1) {
                return null;
            }

            CatchClause catchClause = tryStatement.getCatchClauses().getFirst();

            if (catchClause.getStatements() == null || catchClause.getStatements().size() != 1) {
                return null;
            }

            if (!(catchClause.getStatements().getFirst() instanceof ExpressionStatement)) {
                return null;
            }

            ExpressionStatement expressionStatement = (ExpressionStatement) catchClause.getStatements().getFirst();

            if (!(expressionStatement.getExpression() instanceof MethodInvocationExpression)) {
                return null;
            }

            MethodInvocationExpression mie = (MethodInvocationExpression) expressionStatement.getExpression();

            if (!"addSuppressed".equals(mie.getName()) || !"(Ljava/lang/Throwable;)V".equals(mie.getDescriptor())) {
                return null;
            }

            expression = mie.getExpression();

            if (!expression.isLocalVariableReferenceExpression()) {
                return null;
            }

            AbstractLocalVariable addSuppressedTarget = getLocalVariableFromExpression(mie.getExpression());
            if (addSuppressedTarget == null) {
                return null;
            }
            if (addSuppressedTarget != lv2) {
                primaryExceptionLocalVariable = addSuppressedTarget;
            }
            suppressedLocalVariable = getSuppressedLocalVariable(mie);
        }

        if (lv1 == null) {
            lv1 = closeInvocationInfo.localVariable;
        }
        if (resourceExpression == null) {
            resourceExpression = closeInvocationInfo.expression;
        }
        if (resourceExpression == null) {
            return null;
        }

        DefaultList<TryStatement.Resource> prefixResources = null;
        if (ecjPatternMatch) {
            if (prefixResources == null) {
                prefixResources = collectResourcesFromTryChain(tryStatements);
            }
            if (prefixResources == null) {
                prefixResources = collectResourcesFromCatch(catchStatements, closeInvocationInfo.localVariable);
            }
            if (prefixResources != null
                    && resourceExpression != null
                    && !resourceExpression.isLocalVariableReferenceExpression()) {
                replaceResourceExpression(prefixResources, lv1, resourceExpression);
            }
        }

        return newTryStatement(
                localVariableMaker,
                statements,
                tryStatementsForResources,
                finallyStatements,
                resourceExpression,
                lv1,
                primaryExceptionLocalVariable,
                suppressedLocalVariable,
                hasResourceDeclaration,
                false,
                prefixResources,
                allowResourceExpression);
    }

    private static CloseInvocationInfo getCloseInvocationInfo(BaseStatement catchStatements) {
        Statement firstStatement = catchStatements.getFirst();
        if (firstStatement instanceof ClassFileTryStatement tryStatement) {
            return getCloseInvocationInfo(tryStatement);
        }
        if (firstStatement instanceof IfStatement ifStatement) {
            Expression condition = ifStatement.getCondition();
            BaseStatement thenStatements = ifStatement.getStatements();
            if (condition instanceof BinaryOperatorExpression && thenStatements.size() == 1) {
                BinaryOperatorExpression boe = (BinaryOperatorExpression) condition;
                if ("!=".equals(boe.getOperator())
                        && boe.getRightExpression() instanceof NullExpression
                        && boe.getLeftExpression() instanceof ClassFileLocalVariableReferenceExpression
                        && thenStatements.getFirst() instanceof ClassFileTryStatement) {
                    AbstractLocalVariable conditionVariable = ((ClassFileLocalVariableReferenceExpression) boe.getLeftExpression()).getLocalVariable();
                    CloseInvocationInfo info = getCloseInvocationInfo((ClassFileTryStatement) thenStatements.getFirst());
                    if (info != null && info.localVariable == conditionVariable) {
                        return info;
                    }
                }
            }
        }
        return null;
    }

    private static CloseInvocationInfo getCloseInvocationInfo(ClassFileTryStatement tryStatement) {
        if (tryStatement.getTryStatements() == null || tryStatement.getTryStatements().size() != 1) {
            return null;
        }
        Statement statement = tryStatement.getTryStatements().getFirst();
        if (!(statement instanceof ExpressionStatement)) {
            return null;
        }
        Expression expression = statement.getExpression();
        if (!(expression instanceof MethodInvocationExpression)) {
            return null;
        }
        MethodInvocationExpression mie = (MethodInvocationExpression) expression;
        AbstractLocalVariable localVariable = getCloseInvocationLocalVariable(mie);
        if (localVariable == null) {
            return null;
        }
        return new CloseInvocationInfo(tryStatement, localVariable, mie.getExpression());
    }

    private static CloseInvocationInfo getCloseInvocationInfoFromTryStatements(Statements tryStatements) {
        if (tryStatements == null || tryStatements.isEmpty()) {
            return null;
        }
        CloseInvocationInfoVisitor visitor = new CloseInvocationInfoVisitor();
        tryStatements.accept(visitor);
        return visitor.getInfo();
    }

    private static CloseInvocationInfo getCloseInvocationInfoFromCatchStatements(BaseStatement catchStatements) {
        if (catchStatements == null || catchStatements.size() == 0) {
            return null;
        }
        CloseInvocationInCatchVisitor visitor = new CloseInvocationInCatchVisitor();
        catchStatements.accept(visitor);
        MethodInvocationExpression invocation = visitor.getInvocation();
        if (invocation == null) {
            return null;
        }
        return new CloseInvocationInfo(null, visitor.getLocalVariable(), invocation.getExpression());
    }

    private static CloseInvocationInfo getCloseInvocationInfoFromFinally(ClassFileTryStatement tryStatement) {
        BaseStatement finallyStatements = tryStatement.getFinallyStatements();
        if (finallyStatements == null || finallyStatements.size() != 1) {
            return null;
        }
        Statement statement = finallyStatements.getFirst();
        if (statement instanceof IfStatement ifStatement) {
            Expression condition = ifStatement.getCondition();
            BaseStatement thenStatements = ifStatement.getStatements();
            if (!(condition instanceof BinaryOperatorExpression) || thenStatements.size() != 1) {
                return null;
            }
            BinaryOperatorExpression boe = (BinaryOperatorExpression) condition;
            if (!"!=".equals(boe.getOperator())
                    || !(boe.getRightExpression() instanceof NullExpression)
                    || !(boe.getLeftExpression() instanceof ClassFileLocalVariableReferenceExpression)) {
                return null;
            }
            Statement innerStatement = thenStatements.getFirst();
            if (!(innerStatement instanceof ExpressionStatement)) {
                return null;
            }
            Expression innerExpression = innerStatement.getExpression();
            if (!(innerExpression instanceof MethodInvocationExpression)) {
                return null;
            }
            MethodInvocationExpression mie = (MethodInvocationExpression) innerExpression;
            AbstractLocalVariable localVariable = getCloseInvocationLocalVariable(mie);
            if (localVariable == null) {
                return null;
            }
            return new CloseInvocationInfo(tryStatement, localVariable, mie.getExpression());
        }
        if (statement instanceof ExpressionStatement) {
            Expression expression = statement.getExpression();
            if (expression instanceof MethodInvocationExpression) {
                MethodInvocationExpression mie = (MethodInvocationExpression) expression;
                AbstractLocalVariable localVariable = getCloseInvocationLocalVariable(mie);
                if (localVariable == null) {
                    return null;
                }
                return new CloseInvocationInfo(tryStatement, localVariable, mie.getExpression());
            }
        }
        return null;
    }

    private static MethodInvocationExpression findAddSuppressedInvocation(
            BaseStatement catchStatements, AbstractLocalVariable primaryException) {
        if (catchStatements == null || catchStatements.size() == 0) {
            return null;
        }
        AddSuppressedInvocationVisitor visitor = new AddSuppressedInvocationVisitor(primaryException);
        catchStatements.accept(visitor);
        return visitor.getInvocation();
    }

    private static ResourceAssignment findResourceAssignment(Statements statements) {
        if (statements == null || statements.isEmpty()) {
            return null;
        }
        for (int i = statements.size() - 1; i >= 0; i--) {
            Statement statement = statements.get(i);
            if (!statement.isExpressionStatement()) {
                return null;
            }
            Expression expression = statement.getExpression();
            if (!expression.isBinaryOperatorExpression()) {
                return null;
            }
            Expression leftExpression = expression.getLeftExpression();
            if (!leftExpression.isLocalVariableReferenceExpression()) {
                return null;
            }
            Expression rightExpression = expression.getRightExpression();
            if (rightExpression.isNullExpression()) {
                continue;
            }
            AbstractLocalVariable localVariable = ((ClassFileLocalVariableReferenceExpression) leftExpression).getLocalVariable();
            if (isSelfResourceReference(rightExpression, localVariable)) {
                continue;
            }
            return new ResourceAssignment(localVariable, rightExpression);
        }
        return null;
    }

    private static ResourceAssignment findResourceAssignmentInTryStatements(
            Statements tryStatements, AbstractLocalVariable localVariable, boolean removeAssignment) {
        if (tryStatements == null || tryStatements.isEmpty()) {
            return null;
        }
        for (int i = 0; i < tryStatements.size(); i++) {
            Statement statement = tryStatements.get(i);
            if (!statement.isExpressionStatement()) {
                continue;
            }
            Expression expression = statement.getExpression();
            if (!expression.isBinaryOperatorExpression()) {
                continue;
            }
            Expression leftExpression = expression.getLeftExpression();
            if (!leftExpression.isLocalVariableReferenceExpression()) {
                continue;
            }
            Expression rightExpression = expression.getRightExpression();
            if (rightExpression.isNullExpression()) {
                continue;
            }
            AbstractLocalVariable assignedLocalVariable =
                    ((ClassFileLocalVariableReferenceExpression) leftExpression).getLocalVariable();
            if (matchesLocalVariable(assignedLocalVariable, localVariable)) {
                if (isSelfResourceReference(rightExpression, assignedLocalVariable)) {
                    continue;
                }
                if (removeAssignment) {
                    tryStatements.remove(i);
                }
                return new ResourceAssignment(assignedLocalVariable, rightExpression);
            }
        }
        return null;
    }

    private static ResourceAssignment findResourceAssignmentInStatements(
            Statements statements, AbstractLocalVariable localVariable, boolean removeAssignment) {
        if (statements == null || statements.isEmpty() || localVariable == null) {
            return null;
        }
        for (int i = statements.size() - 1; i >= 0; i--) {
            Statement statement = statements.get(i);
            if (!statement.isExpressionStatement()) {
                continue;
            }
            Expression expression = statement.getExpression();
            if (!expression.isBinaryOperatorExpression()) {
                continue;
            }
            Expression leftExpression = expression.getLeftExpression();
            if (!leftExpression.isLocalVariableReferenceExpression()) {
                continue;
            }
            Expression rightExpression = expression.getRightExpression();
            if (rightExpression.isNullExpression()) {
                continue;
            }
            AbstractLocalVariable assignedLocalVariable =
                    ((ClassFileLocalVariableReferenceExpression) leftExpression).getLocalVariable();
            if (matchesLocalVariable(assignedLocalVariable, localVariable)) {
                if (isSelfResourceReference(rightExpression, assignedLocalVariable)) {
                    continue;
                }
                if (removeAssignment) {
                    statements.remove(i);
                }
                return new ResourceAssignment(assignedLocalVariable, rightExpression);
            }
        }
        return null;
    }

    private static boolean matchesLocalVariable(AbstractLocalVariable assignedLocalVariable, AbstractLocalVariable expectedLocalVariable) {
        if (expectedLocalVariable == null) {
            return true;
        }
        if (assignedLocalVariable == expectedLocalVariable) {
            return true;
        }
        if (assignedLocalVariable == null) {
            return false;
        }
        if (assignedLocalVariable.getIndex() != expectedLocalVariable.getIndex()) {
            return false;
        }
        String assignedName = assignedLocalVariable.getName();
        String expectedName = expectedLocalVariable.getName();
        if (assignedName != null && expectedName != null && !assignedName.equals(expectedName)) {
            return false;
        }
        if (assignedName == null || expectedName == null) {
            return true;
        }
        Type assignedType = assignedLocalVariable.getType();
        Type expectedType = expectedLocalVariable.getType();
        return assignedType == null || expectedType == null || assignedType.equals(expectedType);
    }

    private static AbstractLocalVariable findMatchingTarget(
            AbstractLocalVariable assignedLocalVariable,
            Set<AbstractLocalVariable> targets) {
        if (assignedLocalVariable == null || targets == null || targets.isEmpty()) {
            return null;
        }
        return targets.contains(assignedLocalVariable) ? assignedLocalVariable : null;
    }

    private static Expression resolveResourceExpressionFromParameters(
            LocalVariableMaker localVariableMaker,
            AbstractLocalVariable resourceLocalVariable,
            Expression resourceExpression,
            boolean hasResourceDeclaration) {
        if (localVariableMaker == null || resourceLocalVariable == null || resourceExpression == null) {
            return resourceExpression;
        }
        if (!(resourceExpression instanceof ClassFileLocalVariableReferenceExpression ref)) {
            return resourceExpression;
        }
        if (!matchesLocalVariable(ref.getLocalVariable(), resourceLocalVariable)) {
            return resourceExpression;
        }
        if (resourceLocalVariable.getFromOffset() == 0) {
            return resourceExpression;
        }
        AbstractLocalVariable parameterLocalVariable =
                findSingleCompatibleParameter(localVariableMaker.getFormalParameters(), resourceLocalVariable);
        if (parameterLocalVariable == null) {
            return resourceExpression;
        }
        if (matchesLocalVariable(parameterLocalVariable, resourceLocalVariable)) {
            return resourceExpression;
        }
        if (parameterLocalVariable.getFromOffset() > resourceLocalVariable.getFromOffset()) {
            return resourceExpression;
        }
        return new ClassFileLocalVariableReferenceExpression(ref.getLineNumber(), ref.getOffset(), parameterLocalVariable);
    }

    private static AbstractLocalVariable findSingleCompatibleParameter(
            BaseFormalParameter formalParameters,
            AbstractLocalVariable resourceLocalVariable) {
        if (formalParameters == null || resourceLocalVariable == null) {
            return null;
        }
        Type resourceType = resourceLocalVariable.getType();
        if (resourceType == null) {
            return null;
        }
        AbstractLocalVariable match = null;
        for (FormalParameter parameter : formalParameters) {
            if (!(parameter instanceof ClassFileFormalParameter classFileFormalParameter)) {
                continue;
            }
            AbstractLocalVariable parameterLocalVariable = classFileFormalParameter.getLocalVariable();
            if (parameterLocalVariable == null) {
                continue;
            }
            Type parameterType = parameterLocalVariable.getType();
            if (parameterType == null || !parameterType.equals(resourceType)) {
                continue;
            }
            if (match != null && match != parameterLocalVariable) {
                return null;
            }
            match = parameterLocalVariable;
        }
        return match;
    }

    private static boolean isSelfResourceReference(Expression resourceExpression, AbstractLocalVariable resourceLocalVariable) {
        if (!(resourceExpression instanceof ClassFileLocalVariableReferenceExpression ref)) {
            return false;
        }
        return matchesLocalVariable(ref.getLocalVariable(), resourceLocalVariable);
    }

    private static boolean isFormalParameter(BaseFormalParameter formalParameters, AbstractLocalVariable localVariable) {
        if (formalParameters == null || localVariable == null) {
            return false;
        }
        for (FormalParameter parameter : formalParameters) {
            if (!(parameter instanceof ClassFileFormalParameter classFileFormalParameter)) {
                continue;
            }
            AbstractLocalVariable parameterLocalVariable = classFileFormalParameter.getLocalVariable();
            if (matchesLocalVariable(parameterLocalVariable, localVariable)) {
                return true;
            }
        }
        return false;
    }

    private static boolean preferExpressionOnlyForFormalParameter(
            LocalVariableMaker localVariableMaker,
            AbstractLocalVariable resourceLocalVariable,
            Expression resourceExpression,
            boolean allowResourceExpression) {
        if (!allowResourceExpression || localVariableMaker == null || resourceExpression == null) {
            return false;
        }
        AbstractLocalVariable expressionLocal = getLocalVariableFromExpression(resourceExpression);
        if (!matchesLocalVariable(expressionLocal, resourceLocalVariable)) {
            return false;
        }
        return isFormalParameter(localVariableMaker.getFormalParameters(), expressionLocal);
    }

    private static Statement parsePatternAddSuppressed(
            LocalVariableMaker localVariableMaker, Statements statements, Statements tryStatements,
            Statements finallyStatements, Expression boe, AbstractLocalVariable lv1, AbstractLocalVariable lv2,
            Statement statement) {
        if (!statement.isIfElseStatement()) {
            return null;
        }

        Statement ies = statement;

        statement = ies.getStatements().getFirst();

        if (!statement.isTryStatement()) {
            return null;
        }

        Statement ts = statement;

        statement = ies.getElseStatements().getFirst();

        if (!statement.isExpressionStatement()) {
            return null;
        }

        Expression expression = statement.getExpression();

        if (!expression.isMethodInvocationExpression()) {
            return null;
        }

        MethodInvocationExpression mie = (MethodInvocationExpression) expression;

        if (ts.getFinallyStatements() != null || lv2 != getLocalVariable(ies.getCondition()) ||
                !checkThrowable(ts.getCatchClauses()) || !checkCloseInvocation(mie, lv1)) {
            return null;
        }

        statement = ts.getTryStatements().getFirst();

        if (!statement.isExpressionStatement()) {
            return null;
        }

        expression = statement.getExpression();

        if (!expression.isMethodInvocationExpression()) {
            return null;
        }

        mie = (MethodInvocationExpression) expression;

        if (!checkCloseInvocation(mie, lv1)) {
            return null;
        }

        statement = ts.getCatchClauses().getFirst().getStatements().getFirst();

        if (!statement.isExpressionStatement()) {
            return null;
        }

        expression = statement.getExpression();

        if (!expression.isMethodInvocationExpression()) {
            return null;
        }

        mie = (MethodInvocationExpression) expression;

        if (!"addSuppressed".equals(mie.getName()) || !"(Ljava/lang/Throwable;)V".equals(mie.getDescriptor())) {
            return null;
        }

        expression = mie.getExpression();

        if (!expression.isLocalVariableReferenceExpression()) {
            return null;
        }

        if (((ClassFileLocalVariableReferenceExpression) expression).getLocalVariable() != lv2) {
            return null;
        }

        return newTryStatement(localVariableMaker, statements, tryStatements, finallyStatements, boe, lv1, lv2, false);
    }

    private static boolean checkThrowable(List<? extends TryStatement.CatchClause> catchClauses) {
        return catchClauses.size() == 1 && catchClauses.get(0).getType().equals(ObjectType.TYPE_THROWABLE);
    }

    private static AbstractLocalVariable getLocalVariable(Expression condition) {
        if (!condition.isBinaryOperatorExpression()) {
            return null;
        }

        if (!"!=".equals(condition.getOperator()) || !condition.getRightExpression().isNullExpression() || !condition.getLeftExpression().isLocalVariableReferenceExpression()) {
            return null;
        }

        return ((ClassFileLocalVariableReferenceExpression) condition.getLeftExpression()).getLocalVariable();
    }

    private static boolean checkCloseInvocation(MethodInvocationExpression mie, AbstractLocalVariable lv) {
        AbstractLocalVariable localVariable = getCloseInvocationLocalVariable(mie);
        return localVariable != null && localVariable == lv;
    }

    private static AbstractLocalVariable getCloseInvocationLocalVariable(MethodInvocationExpression mie) {
        if ("close".equals(mie.getName()) && "()V".equals(mie.getDescriptor())) {
            Expression expression = mie.getExpression();
            if (expression.isLocalVariableReferenceExpression()) {
                return ((ClassFileLocalVariableReferenceExpression) expression).getLocalVariable();
            }
        }
        return null;
    }

    private static AbstractLocalVariable getSuppressedLocalVariable(MethodInvocationExpression mie) {
        if (mie.getParameters() == null) {
            return null;
        }
        Expression suppressedExpression;
        if (mie.getParameters().isList()) {
            DefaultList<Expression> parameters = mie.getParameters().getList();
            if (parameters.isEmpty()) {
                return null;
            }
            suppressedExpression = parameters.getFirst();
        } else {
            suppressedExpression = mie.getParameters().getFirst();
        }
        if (suppressedExpression.isLocalVariableReferenceExpression()) {
            return ((ClassFileLocalVariableReferenceExpression) suppressedExpression).getLocalVariable();
        }
        return null;
    }

    private static Statement parsePatternCloseResource(
            LocalVariableMaker localVariableMaker, Statements statements, Statements tryStatements, Statements finallyStatements,
            Expression boe, AbstractLocalVariable lv1, AbstractLocalVariable lv2, Statement statement) {
        Expression expression = statement.getExpression();

        if (!expression.isMethodInvocationExpression()) {
            return null;
        }

        MethodInvocationExpression mie = (MethodInvocationExpression) expression;

        if (!"$closeResource".equals(mie.getName()) || !"(Ljava/lang/Throwable;Ljava/lang/AutoCloseable;)V".equals(mie.getDescriptor())) {
            return null;
        }

        DefaultList<Expression> parameters = mie.getParameters().getList();
        Expression parameter0 = parameters.getFirst();

        if (!parameter0.isLocalVariableReferenceExpression()) {
            return null;
        }
        if (((ClassFileLocalVariableReferenceExpression)parameter0).getLocalVariable() != lv2) {
            return null;
        }

        Expression parameter1 = parameters.get(1);

        if (!parameter1.isLocalVariableReferenceExpression()) {
            return null;
        }
        if (((ClassFileLocalVariableReferenceExpression)parameter1).getLocalVariable() != lv1) {
            return null;
        }

        return newTryStatement(localVariableMaker, statements, tryStatements, finallyStatements, boe, lv1, lv2, false);
    }

    private static ClassFileTryStatement newTryStatement(
            LocalVariableMaker localVariableMaker, Statements statements, Statements tryStatements,
            Statements finallyStatements, Expression boe, AbstractLocalVariable lv1, AbstractLocalVariable lv2,
            boolean allowResourceExpression) {
        return newTryStatement(
                localVariableMaker,
                statements,
                tryStatements,
                finallyStatements,
                boe.getRightExpression(),
                lv1,
                lv2,
                null,
                true,
                false,
                null,
                allowResourceExpression);
    }

    private static ClassFileTryStatement newTryStatement(
            LocalVariableMaker localVariableMaker, Statements statements, Statements tryStatements,
            Statements finallyStatements, Expression resourceExpression, AbstractLocalVariable resourceLocalVariable,
            AbstractLocalVariable primaryExceptionLocalVariable, AbstractLocalVariable suppressedLocalVariable,
            boolean hasResourceDeclaration,
            boolean preserveExceptionLocals,
            DefaultList<TryStatement.Resource> prefixResources,
            boolean allowResourceExpression) {
        if (resourceExpression != null
                && resourceLocalVariable != null
                && isSelfResourceReference(resourceExpression, resourceLocalVariable)) {
            ResourceAssignment assignment = findResourceAssignmentInStatements(
                    statements,
                    resourceLocalVariable,
                    false);
            if (assignment == null) {
                assignment = findResourceAssignmentInTryStatements(
                        tryStatements,
                        resourceLocalVariable,
                        false);
            }
            if (assignment != null && assignment.expression != null) {
                resourceExpression = assignment.expression;
                hasResourceDeclaration = true;
            }
        }
        Expression resolvedExpression = resolveResourceExpressionFromParameters(
                localVariableMaker,
                resourceLocalVariable,
                resourceExpression,
                hasResourceDeclaration);
        if (resolvedExpression != resourceExpression) {
            resourceExpression = resolvedExpression;
            hasResourceDeclaration = true;
        }
        resourceExpression = alignResourceExpressionLineNumber(
                statements,
                tryStatements,
                resourceLocalVariable,
                resourceExpression);
        if (preferExpressionOnlyForFormalParameter(
                localVariableMaker,
                resourceLocalVariable,
                resourceExpression,
                allowResourceExpression)) {
            hasResourceDeclaration = false;
        }
        if (!allowResourceExpression
                && !hasResourceDeclaration
                && isSelfResourceReference(resourceExpression, resourceLocalVariable)) {
            return null;
        }

        // Remove close statements
        tryStatements.accept(new AbstractJavaSyntaxVisitor() {
            @Override
            public void visit(Statements statements) {
                if (statements.isList()) {
                    for (Iterator<Statement> iterator = statements.getList().iterator(); iterator.hasNext();) {
                        Statement statement = iterator.next();
                        if (statement instanceof IfStatement ifStatement) {
                            Expression condition = ifStatement.getCondition();
                            BaseStatement thenStatements = ifStatement.getStatements();
                            if (condition instanceof BinaryOperatorExpression && thenStatements.size() == 1) {
                                Statement singleStatement = thenStatements.getFirst();
                                BinaryOperatorExpression boe = (BinaryOperatorExpression) condition;
                                if ("!=".equals(boe.getOperator())
                                        && boe.getRightExpression() instanceof NullExpression
                                        && boe.getLeftExpression() instanceof ClassFileLocalVariableReferenceExpression
                                        && ((ClassFileLocalVariableReferenceExpression) boe.getLeftExpression()).getLocalVariable() == resourceLocalVariable
                                        && singleStatement.getExpression() instanceof MethodInvocationExpression) {
                                    MethodInvocationExpression mie = (MethodInvocationExpression) singleStatement.getExpression();
                                    if (checkCloseInvocation(mie, resourceLocalVariable)) {
                                        iterator.remove();
                                    }
                                }
                            }
                        }
                        Expression expression = statement.getExpression();
                        if ((expression instanceof MethodInvocationExpression mie) && checkCloseInvocation(mie, resourceLocalVariable)) {
                            if (finallyStatements == null) {
                                iterator.remove();
                            } else {
                                SearchFirstLineNumberVisitor searchFirstLineNumberVisitor = new SearchFirstLineNumberVisitor();
                                searchFirstLineNumberVisitor.safeAccept(finallyStatements);
                                if (searchFirstLineNumberVisitor.getLineNumber() == mie.getLineNumber()) {
                                    iterator.remove();
                                }
                            }
                        }
                    }
                }
                super.visit(statements);
            }
        });

        boolean removePrimaryException;
        boolean removeSuppressedException;
        if (preserveExceptionLocals) {
            removePrimaryException = shouldRemoveLocalVariable(tryStatements, null, primaryExceptionLocalVariable);
            removeSuppressedException = shouldRemoveLocalVariable(tryStatements, null, suppressedLocalVariable);
        } else {
            removePrimaryException = primaryExceptionLocalVariable != null;
            removeSuppressedException = suppressedLocalVariable != null;
        }
        if (resourceLocalVariable != null) {
            if (primaryExceptionLocalVariable == resourceLocalVariable) {
                removePrimaryException = false;
            }
            if (suppressedLocalVariable == resourceLocalVariable) {
                removeSuppressedException = false;
            }
        }

        removeTrailingLocalAssignments(
                statements,
                resourceLocalVariable,
                removePrimaryException ? primaryExceptionLocalVariable : null,
                removeSuppressedException ? suppressedLocalVariable : null);
        Set<AbstractLocalVariable> leadingNullAssignments = removeLeadingNullAssignments(
                statements,
                resourceLocalVariable,
                removePrimaryException ? primaryExceptionLocalVariable : null,
                removeSuppressedException ? suppressedLocalVariable : null);

        if (removePrimaryException) {
            localVariableMaker.removeLocalVariable(primaryExceptionLocalVariable);
        }
        if (removeSuppressedException) {
            localVariableMaker.removeLocalVariable(suppressedLocalVariable);
        }
        if (leadingNullAssignments != null && !leadingNullAssignments.isEmpty()) {
            for (AbstractLocalVariable localVariable : leadingNullAssignments) {
                if (localVariable == primaryExceptionLocalVariable || localVariable == suppressedLocalVariable) {
                    continue;
                }
                if (shouldRemoveLocalVariable(tryStatements, finallyStatements, localVariable)) {
                    localVariableMaker.removeLocalVariable(localVariable);
                }
            }
        }
        removeLeadingNullAssignmentsForUnreferencedLocals(
                localVariableMaker, statements, tryStatements, finallyStatements);

        boolean expressionOnly = shouldUseExpressionOnly(
                resourceLocalVariable, resourceExpression, allowResourceExpression);
        if (!expressionOnly
                && allowResourceExpression
                && resourceExpression != null
                && resourceExpression.isLocalVariableReferenceExpression()
                && !isLocalVariableReferenced(tryStatements, resourceLocalVariable)) {
            expressionOnly = true;
            hasResourceDeclaration = false;
        }
        if (!expressionOnly && resourceLocalVariable != null) {
            resourceLocalVariable.setDeclared(true);
        }
        if (expressionOnly && hasResourceDeclaration && resourceLocalVariable != null) {
            AbstractLocalVariable expressionLocalVariable = getLocalVariableFromExpression(resourceExpression);
            if (resourceLocalVariable != expressionLocalVariable) {
                localVariableMaker.removeLocalVariable(resourceLocalVariable);
            }
        }

        // Create try-with-resources statement
        DefaultList<TryStatement.Resource> resources = new DefaultList<>();

        if (prefixResources != null && !prefixResources.isEmpty()) {
            resources.addAll(prefixResources);
        }

        boolean addCurrentResource = !resourceListContainsLocalVariable(prefixResources, resourceLocalVariable);
        if (addCurrentResource) {
            if (expressionOnly) {
                resources.add(new TryStatement.Resource(resourceExpression));
            } else {
                resources.add(new TryStatement.Resource(resourceLocalVariable.getType(), resourceLocalVariable.getName(), resourceExpression));
            }
        }

        return new ClassFileTryStatement(resources, tryStatements, null, finallyStatements, false, false);
    }

    private static Expression alignResourceExpressionLineNumber(
            Statements statements,
            BaseStatement tryStatements,
            AbstractLocalVariable resourceLocalVariable,
            Expression resourceExpression) {
        if (resourceLocalVariable == null || resourceExpression == null) {
            return resourceExpression;
        }
        if (!(resourceExpression instanceof ClassFileLocalVariableReferenceExpression ref)) {
            return resourceExpression;
        }
        int lineNumber = Expression.UNKNOWN_LINE_NUMBER;
        lineNumber = minLineNumber(lineNumber, findFirstLocalVariableLineNumber(statements, resourceLocalVariable));
        lineNumber = minLineNumber(lineNumber, findFirstLocalVariableLineNumber(tryStatements, resourceLocalVariable));
        lineNumber = minLineNumber(lineNumber, findLeadingNullAssignmentLineNumber(statements));
        lineNumber = minLineNumber(lineNumber, findLeadingNullAssignmentLineNumber(tryStatements));
        lineNumber = minLineNumber(lineNumber, findNullAssignmentsOnlyLineNumber(statements));
        lineNumber = minLineNumber(lineNumber, findNullAssignmentsOnlyLineNumber(tryStatements));
        if (lineNumber == Expression.UNKNOWN_LINE_NUMBER) {
            return resourceExpression;
        }
        if (ref.getLineNumber() == Expression.UNKNOWN_LINE_NUMBER || lineNumber < ref.getLineNumber()) {
            return new ClassFileLocalVariableReferenceExpression(lineNumber, ref.getOffset(), ref.getLocalVariable());
        }
        return resourceExpression;
    }

    private static int minLineNumber(int current, int candidate) {
        if (candidate == Expression.UNKNOWN_LINE_NUMBER) {
            return current;
        }
        if (current == Expression.UNKNOWN_LINE_NUMBER || candidate < current) {
            return candidate;
        }
        return current;
    }

    private static int findFirstLocalVariableLineNumber(
            BaseStatement statements,
            AbstractLocalVariable localVariable) {
        if (statements == null || localVariable == null || statements.size() == 0) {
            return Expression.UNKNOWN_LINE_NUMBER;
        }
        LocalVariableLineNumberVisitor visitor = new LocalVariableLineNumberVisitor(localVariable);
        statements.accept(visitor);
        return visitor.getLineNumber();
    }

    private static int findLeadingNullAssignmentLineNumber(Statements statements) {
        if (statements == null || statements.isEmpty()) {
            return Expression.UNKNOWN_LINE_NUMBER;
        }
        Statement statement = statements.get(0);
        if (isNullAssignmentOrDeclaration(statement)) {
            return statement.getLineNumber();
        }
        return Expression.UNKNOWN_LINE_NUMBER;
    }

    private static int findLeadingNullAssignmentLineNumber(BaseStatement statements) {
        if (!(statements instanceof Statements list)) {
            return Expression.UNKNOWN_LINE_NUMBER;
        }
        return findLeadingNullAssignmentLineNumber(list);
    }

    private static int findNullAssignmentsOnlyLineNumber(Statements statements) {
        if (statements == null || statements.isEmpty()) {
            return Expression.UNKNOWN_LINE_NUMBER;
        }
        for (Statement statement : statements) {
            if (!isNullAssignmentOrDeclaration(statement)) {
                return Expression.UNKNOWN_LINE_NUMBER;
            }
        }
        SearchFirstLineNumberVisitor visitor = new SearchFirstLineNumberVisitor();
        visitor.safeAccept(statements);
        return visitor.getLineNumber();
    }

    private static int findNullAssignmentsOnlyLineNumber(BaseStatement statements) {
        if (!(statements instanceof Statements list)) {
            return Expression.UNKNOWN_LINE_NUMBER;
        }
        return findNullAssignmentsOnlyLineNumber(list);
    }

    private static final class LocalVariableLineNumberVisitor extends AbstractJavaSyntaxVisitor {
        private final AbstractLocalVariable localVariable;
        private int lineNumber = Expression.UNKNOWN_LINE_NUMBER;

        private LocalVariableLineNumberVisitor(AbstractLocalVariable localVariable) {
            this.localVariable = localVariable;
        }

        public int getLineNumber() {
            return lineNumber;
        }

        @Override
        public void visit(LocalVariableReferenceExpression expression) {
            if (!(expression instanceof ClassFileLocalVariableReferenceExpression ref)) {
                super.visit(expression);
                return;
            }
            if (ref.getLocalVariable() == localVariable) {
                int candidate = ref.getLineNumber();
                if (candidate != Expression.UNKNOWN_LINE_NUMBER
                        && (lineNumber == Expression.UNKNOWN_LINE_NUMBER || candidate < lineNumber)) {
                    lineNumber = candidate;
                }
            }
            super.visit(expression);
        }
    }

    private static ClassFileTryStatement newTryStatementFromTryChain(
            LocalVariableMaker localVariableMaker,
            Statements statements,
            Statements tryStatements,
            Statements finallyStatements,
            AbstractLocalVariable primaryExceptionLocalVariable,
            AbstractLocalVariable suppressedLocalVariable,
            AbstractLocalVariable currentLocalVariable,
            Expression currentExpression,
            boolean currentHasDeclaration,
            boolean allowResourceExpression) {
        ResourceChain chain = extractResourceChain(tryStatements, false);
        if (chain == null) {
            ClassFileTryStatement innerTryStatement = findSingleTryWithResources(tryStatements);
            if (innerTryStatement == null) {
                return null;
            }
        }
        SearchFirstLineNumberVisitor lineNumberVisitor = new SearchFirstLineNumberVisitor();
        lineNumberVisitor.safeAccept(chain.bodyStatements);
        int bodyLineNumber = lineNumberVisitor.getLineNumber();
        if (bodyLineNumber == Expression.UNKNOWN_LINE_NUMBER) {
            return null;
        }
        if (currentLocalVariable != null && currentExpression != null && !currentHasDeclaration) {
            Expression resolvedExpression = resolveResourceExpressionFromParameters(
                    localVariableMaker,
                    currentLocalVariable,
                    currentExpression,
                    currentHasDeclaration);
            if (resolvedExpression != currentExpression) {
                currentExpression = resolvedExpression;
                currentHasDeclaration = true;
            }
        }
        if (currentLocalVariable != null && currentExpression != null) {
            currentExpression = alignResourceExpressionLineNumber(
                    statements,
                    tryStatements,
                    currentLocalVariable,
                    currentExpression);
        }
        if (preferExpressionOnlyForFormalParameter(
                localVariableMaker,
                currentLocalVariable,
                currentExpression,
                allowResourceExpression)) {
            currentHasDeclaration = false;
        }
        if (!allowResourceExpression) {
            if (currentLocalVariable != null
                    && !currentHasDeclaration
                    && isSelfResourceReference(currentExpression, currentLocalVariable)) {
                return null;
            }
            for (ResourceInfo resourceInfo : chain.resources) {
                if (resourceInfo == null || resourceInfo.hasDeclaration) {
                    continue;
                }
                Expression resolvedExpression = resolveResourceExpressionFromParameters(
                        localVariableMaker,
                        resourceInfo.localVariable,
                        resourceInfo.expression,
                        resourceInfo.hasDeclaration);
                if (resolvedExpression != resourceInfo.expression) {
                    resourceInfo.expression = resolvedExpression;
                    resourceInfo.hasDeclaration = true;
                }
                if (!resourceInfo.hasDeclaration
                        && isSelfResourceReference(resourceInfo.expression, resourceInfo.localVariable)) {
                    return null;
                }
            }
        }
        ResourceChain committed = extractResourceChain(tryStatements, true);
        if (committed == null) {
            return null;
        }

        DefaultList<ResourceInfo> resourceInfos = committed.resources;
        if (currentLocalVariable != null) {
            ResourceInfo existing = findResourceInfo(resourceInfos, currentLocalVariable);
            if (existing == null) {
                if (currentExpression == null) {
                    return null;
                }
                resourceInfos.add(0, new ResourceInfo(currentLocalVariable, currentExpression, currentHasDeclaration));
            } else if (currentHasDeclaration && currentExpression != null && !currentExpression.isLocalVariableReferenceExpression()) {
                existing.expression = currentExpression;
                existing.hasDeclaration = true;
            }
        }
        for (ResourceInfo resourceInfo : resourceInfos) {
            if (resourceInfo == null || resourceInfo.hasDeclaration) {
                continue;
            }
            ResourceAssignment assignment = findResourceAssignmentInStatements(
                    statements,
                    resourceInfo.localVariable,
                    true);
            if (assignment != null && assignment.expression != null) {
                resourceInfo.expression = assignment.expression;
                resourceInfo.hasDeclaration = true;
                continue;
            }
            Expression resolvedExpression = resolveResourceExpressionFromParameters(
                    localVariableMaker,
                    resourceInfo.localVariable,
                    resourceInfo.expression,
                    resourceInfo.hasDeclaration);
            if (resolvedExpression != resourceInfo.expression) {
                resourceInfo.expression = resolvedExpression;
                resourceInfo.hasDeclaration = true;
            }
            resourceInfo.expression = alignResourceExpressionLineNumber(
                    statements,
                    tryStatements,
                    resourceInfo.localVariable,
                    resourceInfo.expression);
            if (preferExpressionOnlyForFormalParameter(
                    localVariableMaker,
                    resourceInfo.localVariable,
                    resourceInfo.expression,
                    allowResourceExpression)) {
                resourceInfo.hasDeclaration = false;
            }
        }
        Statements bodyStatements = committed.bodyStatements;
        DefaultList<TryStatement.Resource> resources = new DefaultList<>();
        Set<AbstractLocalVariable> resourceLocals = new HashSet<>();
        for (ResourceInfo resourceInfo : resourceInfos) {
            if (resourceInfo != null && resourceInfo.localVariable != null) {
                resourceLocals.add(resourceInfo.localVariable);
            }
        }
        removeCloseStatementsForResources(bodyStatements, resourceLocals, finallyStatements);

        for (ResourceInfo resourceInfo : resourceInfos) {
            AbstractLocalVariable localVariable = resourceInfo.localVariable;
            Expression resourceExpression = resourceInfo.expression;
            boolean expressionOnly = shouldUseExpressionOnly(
                    localVariable, resourceExpression, allowResourceExpression);
            if (!expressionOnly
                    && allowResourceExpression
                    && resourceExpression != null
                    && resourceExpression.isLocalVariableReferenceExpression()
                    && !isLocalVariableReferenced(bodyStatements, localVariable)) {
                expressionOnly = true;
                resourceInfo.hasDeclaration = false;
            }

            if (!expressionOnly && localVariable != null) {
                localVariable.setDeclared(true);
            }
            if (expressionOnly && resourceInfo.hasDeclaration && localVariable != null) {
                AbstractLocalVariable expressionLocalVariable = getLocalVariableFromExpression(resourceExpression);
                if (localVariable != expressionLocalVariable) {
                    localVariableMaker.removeLocalVariable(localVariable);
                }
            }

            if (expressionOnly) {
                resources.add(new TryStatement.Resource(resourceExpression));
            } else {
                resources.add(new TryStatement.Resource(localVariable.getType(), localVariable.getName(), resourceExpression));
            }
            if (localVariable != null) {
                resourceLocals.add(localVariable);
            }
        }

        boolean removePrimaryException = shouldRemoveLocalVariable(bodyStatements, null, primaryExceptionLocalVariable);
        boolean removeSuppressedException = shouldRemoveLocalVariable(bodyStatements, null, suppressedLocalVariable);
        List<AbstractLocalVariable> localsToRemove = new ArrayList<>(resourceLocals);
        if (removePrimaryException) {
            localsToRemove.add(primaryExceptionLocalVariable);
        }
        if (removeSuppressedException) {
            localsToRemove.add(suppressedLocalVariable);
        }
        removeTrailingLocalAssignments(statements, localsToRemove.toArray(new AbstractLocalVariable[0]));
        List<AbstractLocalVariable> leadingNullLocals = new ArrayList<>(resourceLocals);
        if (removePrimaryException) {
            leadingNullLocals.add(primaryExceptionLocalVariable);
        }
        if (removeSuppressedException) {
            leadingNullLocals.add(suppressedLocalVariable);
        }
        Set<AbstractLocalVariable> leadingNullAssignments = removeLeadingNullAssignments(
                statements,
                leadingNullLocals.toArray(new AbstractLocalVariable[0]));
        if (removePrimaryException) {
            localVariableMaker.removeLocalVariable(primaryExceptionLocalVariable);
        }
        if (removeSuppressedException) {
            localVariableMaker.removeLocalVariable(suppressedLocalVariable);
        }
        if (leadingNullAssignments != null && !leadingNullAssignments.isEmpty()) {
            for (AbstractLocalVariable localVariable : leadingNullAssignments) {
                if (localVariable == primaryExceptionLocalVariable || localVariable == suppressedLocalVariable) {
                    continue;
                }
                if (shouldRemoveLocalVariable(bodyStatements, finallyStatements, localVariable)) {
                    localVariableMaker.removeLocalVariable(localVariable);
                }
            }
        }
        removeLeadingNullAssignmentsForUnreferencedLocals(
                localVariableMaker, statements, bodyStatements, finallyStatements);

        return new ClassFileTryStatement(resources, bodyStatements, null, finallyStatements, false, false);
    }

    private static void removeCatchClauseLocalVariables(
            LocalVariableMaker localVariableMaker,
            List<TryStatement.CatchClause> catchClauses) {
        if (localVariableMaker == null || catchClauses == null || catchClauses.isEmpty()) {
            return;
        }
        for (TryStatement.CatchClause catchClause : catchClauses) {
            if (!(catchClause instanceof ClassFileTryStatement.CatchClause classFileCatch)) {
                continue;
            }
            AbstractLocalVariable localVariable = classFileCatch.getLocalVariable();
            if (localVariable == null) {
                continue;
            }
            localVariableMaker.removeLocalVariable(localVariable);
        }
    }

    private static boolean shouldUseExpressionOnly(
            AbstractLocalVariable resourceLocalVariable,
            Expression resourceExpression,
            boolean allowResourceExpression) {
        if (!allowResourceExpression || resourceExpression == null) {
            return false;
        }
        if (!resourceExpression.isLocalVariableReferenceExpression()) {
            return false;
        }
        AbstractLocalVariable expressionLocalVariable = getLocalVariableFromExpression(resourceExpression);
        if (resourceLocalVariable == null || expressionLocalVariable == null) {
            return false;
        }
        if (!matchesLocalVariable(expressionLocalVariable, resourceLocalVariable)) {
            return false;
        }
        return true;
    }

    private static AbstractLocalVariable getLocalVariableFromExpression(Expression expression) {
        if (expression instanceof ClassFileLocalVariableReferenceExpression ref) {
            return ref.getLocalVariable();
        }
        return null;
    }

    private static void removeTrailingLocalAssignments(Statements statements, AbstractLocalVariable... locals) {
        if (statements == null || statements.isEmpty()) {
            return;
        }
        Set<AbstractLocalVariable> targets = new HashSet<>();
        for (AbstractLocalVariable local : locals) {
            if (local != null) {
                targets.add(local);
            }
        }
        while (!targets.isEmpty() && !statements.isEmpty()) {
            Statement lastStatement = statements.getLast();
            AbstractLocalVariable assignedLocalVariable = getAssignedLocalVariable(lastStatement);
            AbstractLocalVariable matchedTarget = findMatchingTarget(assignedLocalVariable, targets);
            if (matchedTarget == null) {
                break;
            }
            statements.removeLast();
            targets.remove(matchedTarget);
        }
    }

    private static void removeTrailingResourceAssignments(
            Statements statements,
            AbstractLocalVariable... locals) {
        if (statements == null || statements.isEmpty()) {
            return;
        }
        Set<AbstractLocalVariable> targets = new HashSet<>();
        for (AbstractLocalVariable local : locals) {
            if (local != null) {
                targets.add(local);
            }
        }
        while (!targets.isEmpty() && !statements.isEmpty()) {
            Statement lastStatement = statements.getLast();
            AbstractLocalVariable assignedLocalVariable = getAssignedLocalVariable(lastStatement);
            if (assignedLocalVariable == null) {
                assignedLocalVariable = getDeclaredLocalVariable(lastStatement);
            }
            if (assignedLocalVariable == null || !targets.contains(assignedLocalVariable)) {
                break;
            }
            statements.removeLast();
            targets.remove(assignedLocalVariable);
        }
    }

    private static Set<AbstractLocalVariable> removeLeadingNullAssignments(
            Statements statements, AbstractLocalVariable... locals) {
        if (statements == null || statements.isEmpty()) {
            return null;
        }
        Set<AbstractLocalVariable> targets = new HashSet<>();
        for (AbstractLocalVariable local : locals) {
            if (local != null) {
                targets.add(local);
            }
        }
        if (targets.isEmpty()) {
            return null;
        }
        Set<AbstractLocalVariable> removed = new HashSet<>();
        while (!statements.isEmpty()) {
            Statement firstStatement = statements.getFirst();
            if (!isNullAssignment(firstStatement)) {
                break;
            }
            AbstractLocalVariable assignedLocalVariable = getAssignedLocalVariable(firstStatement);
            AbstractLocalVariable matchedTarget = findMatchingTarget(assignedLocalVariable, targets);
            if (matchedTarget == null) {
                break;
            }
            statements.removeFirst();
            targets.remove(matchedTarget);
            removed.add(matchedTarget);
        }
        return removed;
    }

    private static boolean shouldRemoveLocalVariable(
            BaseStatement tryStatements, BaseStatement finallyStatements, AbstractLocalVariable localVariable) {
        if (localVariable == null) {
            return false;
        }
        if (isLocalVariableReferenced(tryStatements, localVariable)) {
            return false;
        }
        return !isLocalVariableReferenced(finallyStatements, localVariable);
    }

    private static boolean isLocalVariableReferenced(BaseStatement statements, AbstractLocalVariable localVariable) {
        if (statements == null || localVariable == null || statements.size() == 0) {
            return false;
        }
        LocalVariableReferenceVisitor visitor = new LocalVariableReferenceVisitor(localVariable);
        statements.accept(visitor);
        return visitor.isFound();
    }

    private static boolean isNullAssignment(Statement statement) {
        if (statement == null || !statement.isExpressionStatement()) {
            return false;
        }
        Expression expression = statement.getExpression();
        if (!(expression instanceof BinaryOperatorExpression boe)) {
            return false;
        }
        if (!(boe.getRightExpression() instanceof NullExpression)) {
            return false;
        }
        return boe.getLeftExpression() instanceof ClassFileLocalVariableReferenceExpression;
    }

    private static boolean isNullDeclarationStatement(Statement statement) {
        if (!(statement instanceof LocalVariableDeclarationStatement declarationStatement)) {
            return false;
        }
        BaseLocalVariableDeclarator declarators = declarationStatement.getLocalVariableDeclarators();
        if (declarators == null || declarators.isList()) {
            return false;
        }
        LocalVariableDeclarator declarator = (LocalVariableDeclarator) declarators.getFirst();
        if (declarator.getVariableInitializer() == null
                || !declarator.getVariableInitializer().isExpressionVariableInitializer()) {
            return false;
        }
        Expression initializer = declarator.getVariableInitializer().getExpression();
        return initializer == null || initializer.isNullExpression();
    }

    private static boolean isNullAssignmentOrDeclaration(Statement statement) {
        return isNullAssignment(statement) || isNullDeclarationStatement(statement);
    }

    private static void removeLeadingNullAssignmentsForUnreferencedLocals(
            LocalVariableMaker localVariableMaker,
            Statements statements,
            BaseStatement tryStatements,
            BaseStatement finallyStatements) {
        if (localVariableMaker == null || statements == null || statements.isEmpty()) {
            return;
        }
        while (!statements.isEmpty()) {
            Statement first = statements.getFirst();
            if (!isNullAssignmentOrDeclaration(first)) {
                break;
            }
            AbstractLocalVariable assignedLocal = getAssignedLocalVariable(first);
            if (assignedLocal == null) {
                assignedLocal = getDeclaredLocalVariable(first);
            }
            if (assignedLocal == null || !shouldRemoveLocalVariable(tryStatements, finallyStatements, assignedLocal)) {
                break;
            }
            statements.removeFirst();
            localVariableMaker.removeLocalVariable(assignedLocal);
        }
    }

    private static boolean isPotentialResourceAssignment(Statement statement) {
        if (statement == null) {
            return false;
        }
        if (statement instanceof LocalVariableDeclarationStatement declarationStatement) {
            BaseLocalVariableDeclarator declarators = declarationStatement.getLocalVariableDeclarators();
            if (declarators == null) {
                return false;
            }
            if (declarators.isList()) {
                for (LocalVariableDeclarator declarator : declarators.getList()) {
                    if (hasNonNullInitializer(declarator)) {
                        return true;
                    }
                }
                return false;
            }
            return hasNonNullInitializer((LocalVariableDeclarator) declarators.getFirst());
        }
        if (!statement.isExpressionStatement()) {
            return false;
        }
        Expression expression = statement.getExpression();
        if (!(expression instanceof BinaryOperatorExpression boe)) {
            return false;
        }
        if (!(boe.getLeftExpression() instanceof ClassFileLocalVariableReferenceExpression)) {
            return false;
        }
        Expression rightExpression = boe.getRightExpression();
        return rightExpression != null && !rightExpression.isNullExpression();
    }

    private static boolean hasNonNullInitializer(LocalVariableDeclarator declarator) {
        if (declarator == null
                || declarator.getVariableInitializer() == null
                || !declarator.getVariableInitializer().isExpressionVariableInitializer()) {
            return false;
        }
        Expression initializer = declarator.getVariableInitializer().getExpression();
        return initializer != null && !initializer.isNullExpression();
    }

    private static ClassFileTryStatement findSingleTryWithResources(Statements tryStatements) {
        if (tryStatements == null || tryStatements.isEmpty()) {
            return null;
        }
        ClassFileTryStatement candidate = null;
        for (Statement statement : tryStatements) {
            if (statement instanceof ClassFileTryStatement tryStatement
                    && tryStatement.getResources() != null
                    && !tryStatement.getResources().isEmpty()) {
                if (candidate != null) {
                    return null;
                }
                candidate = tryStatement;
            }
        }
        if (candidate == null) {
            return null;
        }
        DefaultList<TryStatement.Resource> resources = candidate.getResources();
        for (Statement statement : tryStatements) {
            if (statement == candidate) {
                continue;
            }
            if (isNullAssignmentOrDeclaration(statement)) {
                continue;
            }
            if (isCloseStatementForResources(statement, resources)) {
                continue;
            }
            return null;
        }
        return candidate;
    }

    private static boolean isCloseStatementForResources(
            Statement statement, DefaultList<TryStatement.Resource> resources) {
        if (statement == null || resources == null || resources.isEmpty()) {
            return false;
        }
        if (statement instanceof IfStatement ifStatement) {
            Expression condition = ifStatement.getCondition();
            BaseStatement thenStatements = ifStatement.getStatements();
            if (condition instanceof BinaryOperatorExpression boe && thenStatements.size() == 1) {
                Statement singleStatement = thenStatements.getFirst();
                if ("!=".equals(boe.getOperator())
                        && boe.getRightExpression() instanceof NullExpression
                        && boe.getLeftExpression() instanceof ClassFileLocalVariableReferenceExpression
                        && singleStatement.getExpression() instanceof MethodInvocationExpression) {
                    AbstractLocalVariable conditionVariable =
                            ((ClassFileLocalVariableReferenceExpression) boe.getLeftExpression()).getLocalVariable();
                    MethodInvocationExpression mie = (MethodInvocationExpression) singleStatement.getExpression();
                    if (resourceListContainsLocalVariable(resources, conditionVariable) && checkCloseInvocation(mie, conditionVariable)) {
                        return true;
                    }
                }
            }
            return false;
        }
        Expression expression = statement.getExpression();
        if (expression instanceof MethodInvocationExpression mie) {
            AbstractLocalVariable localVariable = getCloseInvocationLocalVariable(mie);
            return localVariable != null && resourceListContainsLocalVariable(resources, localVariable);
        }
        return false;
    }

    private static ResourceChain extractResourceChain(Statements tryStatements, boolean removeAssignments) {
        if (tryStatements == null || tryStatements.isEmpty()) {
            return null;
        }
        int firstTryIndex = indexOfFirstTryStatement(tryStatements);
        if (firstTryIndex > 0) {
            for (int i = 0; i < firstTryIndex; i++) {
                Statement leading = tryStatements.get(i);
                if (isNullAssignmentOrDeclaration(leading) || isPotentialResourceAssignment(leading)) {
                    continue;
                }
                return null;
            }
        }
        DefaultList<ResourceInfo> resources = new DefaultList<>();
        Statements current = tryStatements;
        Statements bodyStatements = tryStatements;

        while (current != null && !current.isEmpty()) {
            ClassFileTryStatement tryStatement = getFirstTryStatement(current);
            if (tryStatement == null) {
                CloseInvocationExpressionCollector collector = new CloseInvocationExpressionCollector();
                current.accept(collector);
                for (Expression closeExpression : collector.getExpressions()) {
                    if (closeExpression instanceof ClassFileLocalVariableReferenceExpression ref) {
                        AbstractLocalVariable localVariable = ref.getLocalVariable();
                        if (findResourceInfo(resources, localVariable) == null) {
                            resources.add(new ResourceInfo(localVariable, closeExpression, false));
                        }
                    }
                }
                if (!resources.isEmpty()) {
                    for (ResourceInfo resourceInfo : resources) {
                        ResourceAssignment assignment = findResourceAssignmentInStatements(current, resourceInfo.localVariable, removeAssignments);
                        if (assignment != null && assignment.expression != null) {
                            resourceInfo.expression = assignment.expression;
                            resourceInfo.hasDeclaration = true;
                        }
                    }
                    bodyStatements = current;
                }
                break;
            }
            CloseInvocationInfo closeInfo = getCloseInvocationInfoFromFinally(tryStatement);
            if (closeInfo == null || closeInfo.localVariable == null || closeInfo.expression == null) {
                break;
            }
            ResourceInfo resourceInfo = findResourceInfo(resources, closeInfo.localVariable);
            if (resourceInfo == null) {
                resourceInfo = new ResourceInfo(closeInfo.localVariable, closeInfo.expression, false);
                resources.add(resourceInfo);
            }
            ResourceAssignment assignment = findResourceAssignmentInStatements(current, closeInfo.localVariable, removeAssignments);
            if (assignment != null && assignment.expression != null) {
                resourceInfo.expression = assignment.expression;
                resourceInfo.hasDeclaration = true;
            }
            BaseStatement inner = tryStatement.getTryStatements();
            if (!(inner instanceof Statements)) {
                bodyStatements = null;
                break;
            }
            current = (Statements) inner;
            bodyStatements = current;
        }

        if (resources.isEmpty() || bodyStatements == null) {
            return null;
        }
        return new ResourceChain(resources, bodyStatements);
    }

    private static ResourceInfo findResourceInfo(DefaultList<ResourceInfo> resources, AbstractLocalVariable localVariable) {
        if (resources == null || resources.isEmpty() || localVariable == null) {
            return null;
        }
        for (ResourceInfo resource : resources) {
            if (resource.localVariable == localVariable) {
                return resource;
            }
        }
        return null;
    }

    private static ClassFileTryStatement getFirstTryStatement(Statements statements) {
        if (statements == null || statements.isEmpty()) {
            return null;
        }
        for (Statement statement : statements) {
            if (statement instanceof ClassFileTryStatement) {
                return (ClassFileTryStatement) statement;
            }
        }
        return null;
    }

    private static int indexOfFirstTryStatement(Statements statements) {
        if (statements == null || statements.isEmpty()) {
            return -1;
        }
        for (int i = 0; i < statements.size(); i++) {
            if (statements.get(i) instanceof ClassFileTryStatement) {
                return i;
            }
        }
        return -1;
    }

    private static void removeCloseStatementsForResources(
            BaseStatement tryStatements,
            Set<AbstractLocalVariable> resourceLocals,
            Statements finallyStatements) {
        if (tryStatements == null || resourceLocals == null || resourceLocals.isEmpty()) {
            return;
        }
        tryStatements.accept(new AbstractJavaSyntaxVisitor() {
            @Override
            public void visit(Statements statements) {
                if (statements.isList()) {
                    for (Iterator<Statement> iterator = statements.getList().iterator(); iterator.hasNext();) {
                        Statement statement = iterator.next();
                        if (statement instanceof IfStatement ifStatement) {
                            Expression condition = ifStatement.getCondition();
                            BaseStatement thenStatements = ifStatement.getStatements();
                            if (condition instanceof BinaryOperatorExpression && thenStatements.size() == 1) {
                                Statement singleStatement = thenStatements.getFirst();
                                BinaryOperatorExpression boe = (BinaryOperatorExpression) condition;
                                if ("!=".equals(boe.getOperator())
                                        && boe.getRightExpression() instanceof NullExpression
                                        && boe.getLeftExpression() instanceof ClassFileLocalVariableReferenceExpression
                                        && singleStatement.getExpression() instanceof MethodInvocationExpression) {
                                    AbstractLocalVariable conditionVariable =
                                            ((ClassFileLocalVariableReferenceExpression) boe.getLeftExpression()).getLocalVariable();
                                    MethodInvocationExpression mie = (MethodInvocationExpression) singleStatement.getExpression();
                                    if (resourceLocals.contains(conditionVariable) && checkCloseInvocation(mie, conditionVariable)) {
                                        iterator.remove();
                                    }
                                }
                            }
                        }
                        Expression expression = statement.getExpression();
                        if (expression instanceof MethodInvocationExpression mie) {
                            AbstractLocalVariable localVariable = getCloseInvocationLocalVariable(mie);
                            if (localVariable != null && resourceLocals.contains(localVariable)) {
                                if (finallyStatements == null) {
                                    iterator.remove();
                                } else {
                                    SearchFirstLineNumberVisitor searchFirstLineNumberVisitor = new SearchFirstLineNumberVisitor();
                                    searchFirstLineNumberVisitor.safeAccept(finallyStatements);
                                    if (searchFirstLineNumberVisitor.getLineNumber() == mie.getLineNumber()) {
                                        iterator.remove();
                                    }
                                }
                            }
                        }
                    }
                }
                super.visit(statements);
            }
        });
    }

    private static DefaultList<TryStatement.Resource> collectResourcesFromCatch(
            BaseStatement catchStatements, AbstractLocalVariable currentLocalVariable) {
        if (catchStatements == null || catchStatements.size() == 0) {
            return null;
        }
        ResourceCollectorVisitor visitor = new ResourceCollectorVisitor();
        catchStatements.accept(visitor);
        DefaultList<TryStatement.Resource> collected = visitor.getResources();
        DefaultList<TryStatement.Resource> filtered = new DefaultList<>();
        if (!collected.isEmpty()) {
            for (TryStatement.Resource resource : collected) {
                if (resourceMatchesLocalVariable(resource, currentLocalVariable)) {
                    continue;
                }
                if (resourceListContains(filtered, resource)) {
                    continue;
                }
                filtered.add(resource);
            }
        }
        CloseInvocationExpressionCollector closeCollector = new CloseInvocationExpressionCollector();
        catchStatements.accept(closeCollector);
        for (Expression expression : closeCollector.getExpressions()) {
            if (!(expression instanceof ClassFileLocalVariableReferenceExpression ref)) {
                continue;
            }
            AbstractLocalVariable localVariable = ref.getLocalVariable();
            if (localVariable == null || localVariable == currentLocalVariable) {
                continue;
            }
            TryStatement.Resource resource = new TryStatement.Resource(expression);
            if (resourceListContains(filtered, resource)) {
                continue;
            }
            filtered.add(resource);
        }
        return filtered.isEmpty() ? null : filtered;
    }

    private static DefaultList<TryStatement.Resource> collectResourcesFromTryChain(Statements tryStatements) {
        if (tryStatements == null || tryStatements.isEmpty()) {
            return null;
        }
        for (Statement statement : tryStatements) {
            if (statement instanceof ClassFileTryStatement tryStatement) {
                DefaultList<TryStatement.Resource> resources = new DefaultList<>();
                collectResourcesFromTryChain(tryStatement, resources);
                return resources.isEmpty() ? null : resources;
            }
        }
        return null;
    }

    private static void collectResourcesFromTryChain(ClassFileTryStatement tryStatement, DefaultList<TryStatement.Resource> resources) {
        if (tryStatement == null) {
            return;
        }
        collectResourcesFromCloseStatements(tryStatement.getFinallyStatements(), resources);
        List<CatchClause> catchClauses = tryStatement.getCatchClauses();
        if (catchClauses != null) {
            for (CatchClause catchClause : catchClauses) {
                collectResourcesFromCloseStatements(catchClause.getStatements(), resources);
            }
        }
        BaseStatement inner = tryStatement.getTryStatements();
        if (inner instanceof Statements innerStatements && innerStatements.size() == 1 && innerStatements.getFirst() instanceof ClassFileTryStatement) {
            collectResourcesFromTryChain((ClassFileTryStatement) innerStatements.getFirst(), resources);
        }
    }

    private static void collectResourcesFromCloseStatements(BaseStatement statements, DefaultList<TryStatement.Resource> resources) {
        if (statements == null || statements.size() == 0) {
            return;
        }
        CloseInvocationExpressionCollector collector = new CloseInvocationExpressionCollector();
        statements.accept(collector);
        for (Expression expression : collector.getExpressions()) {
            TryStatement.Resource resource = new TryStatement.Resource(expression);
            if (!resourceListContains(resources, resource)) {
                resources.add(resource);
            }
        }
    }

    private static boolean resourceMatchesLocalVariable(TryStatement.Resource resource, AbstractLocalVariable localVariable) {
        if (resource == null || localVariable == null) {
            return false;
        }
        String localName = localVariable.getName();
        String resourceName = resource.getName();
        if (resourceName != null && localName != null) {
            return resourceName.equals(localName);
        }
        Expression expression = resource.getExpression();
        if (expression instanceof ClassFileLocalVariableReferenceExpression ref) {
            return ref.getLocalVariable() == localVariable;
        }
        return false;
    }

    private static boolean resourceListContains(DefaultList<TryStatement.Resource> resources, TryStatement.Resource resource) {
        if (resources == null || resources.isEmpty() || resource == null) {
            return false;
        }
        String key = getResourceKey(resource);
        if (key == null) {
            return false;
        }
        for (TryStatement.Resource existing : resources) {
            if (key.equals(getResourceKey(existing))) {
                return true;
            }
        }
        return false;
    }

    private static boolean resourceListContainsLocalVariable(DefaultList<TryStatement.Resource> resources, AbstractLocalVariable localVariable) {
        if (resources == null || resources.isEmpty() || localVariable == null) {
            return false;
        }
        for (TryStatement.Resource resource : resources) {
            if (resourceMatchesLocalVariable(resource, localVariable)) {
                return true;
            }
        }
        return false;
    }

    private static void replaceResourceExpression(
            DefaultList<TryStatement.Resource> resources, AbstractLocalVariable localVariable, Expression resourceExpression) {
        if (resources == null || resources.isEmpty() || localVariable == null) {
            return;
        }
        for (int i = 0; i < resources.size(); i++) {
            TryStatement.Resource resource = resources.get(i);
            if (resourceMatchesLocalVariable(resource, localVariable)) {
                if (resource.isExpressionOnly()) {
                    resources.set(i, new TryStatement.Resource(localVariable.getType(), localVariable.getName(), resourceExpression));
                } else {
                    resource.setExpression(resourceExpression);
                }
                return;
            }
        }
    }

    private static String getResourceKey(TryStatement.Resource resource) {
        if (resource == null) {
            return null;
        }
        String name = resource.getName();
        if (name != null) {
            return "name:" + name;
        }
        Expression expression = resource.getExpression();
        if (expression instanceof ClassFileLocalVariableReferenceExpression ref) {
            return "index:" + ref.getLocalVariable().getIndex();
        }
        return null;
    }

    private static AbstractLocalVariable getAssignedLocalVariable(Statement statement) {
        if (statement == null || !statement.isExpressionStatement()) {
            return null;
        }
        Expression expression = statement.getExpression();
        if (!(expression instanceof BinaryOperatorExpression)) {
            return null;
        }
        Expression leftExpression = expression.getLeftExpression();
        if (!(leftExpression instanceof ClassFileLocalVariableReferenceExpression)) {
            return null;
        }
        return ((ClassFileLocalVariableReferenceExpression) leftExpression).getLocalVariable();
    }

    private static AbstractLocalVariable getDeclaredLocalVariable(Statement statement) {
        if (!(statement instanceof LocalVariableDeclarationStatement declarationStatement)) {
            return null;
        }
        BaseLocalVariableDeclarator declarators = declarationStatement.getLocalVariableDeclarators();
        if (declarators == null || declarators.isList()) {
            return null;
        }
        LocalVariableDeclarator declarator = (LocalVariableDeclarator) declarators.getFirst();
        if (!(declarator instanceof ClassFileLocalVariableDeclarator cfDeclarator)) {
            return null;
        }
        return cfDeclarator.getLocalVariable();
    }

}
