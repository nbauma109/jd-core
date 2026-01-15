/*
 * Copyright (c) 2008, 2019 Emmanuel Dupuy.
 * This project is distributed under the GPLv3 license.
 * This is a Copyleft license that gives the user the right to use,
 * copy and modify the code freely for non-commercial purposes.
 */

package org.jd.core.v1.service.converter.classfiletojavasyntax.visitor;

import org.jd.core.v1.model.javasyntax.AbstractJavaSyntaxVisitor;
import org.jd.core.v1.model.javasyntax.expression.BinaryOperatorExpression;
import org.jd.core.v1.model.javasyntax.expression.Expression;
import org.jd.core.v1.model.javasyntax.expression.MethodInvocationExpression;
import org.jd.core.v1.model.javasyntax.expression.NullExpression;
import org.jd.core.v1.model.javasyntax.statement.AssertStatement;
import org.jd.core.v1.model.javasyntax.statement.BaseStatement;
import org.jd.core.v1.model.javasyntax.statement.BreakStatement;
import org.jd.core.v1.model.javasyntax.statement.CommentStatement;
import org.jd.core.v1.model.javasyntax.statement.ContinueStatement;
import org.jd.core.v1.model.javasyntax.statement.DoWhileStatement;
import org.jd.core.v1.model.javasyntax.statement.ExpressionStatement;
import org.jd.core.v1.model.javasyntax.statement.ForEachStatement;
import org.jd.core.v1.model.javasyntax.statement.ForStatement;
import org.jd.core.v1.model.javasyntax.statement.IfElseStatement;
import org.jd.core.v1.model.javasyntax.statement.IfStatement;
import org.jd.core.v1.model.javasyntax.statement.LabelStatement;
import org.jd.core.v1.model.javasyntax.statement.LambdaExpressionStatement;
import org.jd.core.v1.model.javasyntax.statement.LocalVariableDeclarationStatement;
import org.jd.core.v1.model.javasyntax.statement.NoStatement;
import org.jd.core.v1.model.javasyntax.statement.ReturnExpressionStatement;
import org.jd.core.v1.model.javasyntax.statement.ReturnStatement;
import org.jd.core.v1.model.javasyntax.statement.Statement;
import org.jd.core.v1.model.javasyntax.statement.StatementVisitor;
import org.jd.core.v1.model.javasyntax.statement.Statements;
import org.jd.core.v1.model.javasyntax.statement.SwitchStatement;
import org.jd.core.v1.model.javasyntax.statement.SynchronizedStatement;
import org.jd.core.v1.model.javasyntax.statement.ThrowStatement;
import org.jd.core.v1.model.javasyntax.statement.TryStatement;
import org.jd.core.v1.model.javasyntax.statement.TypeDeclarationStatement;
import org.jd.core.v1.model.javasyntax.statement.WhileStatement;
import org.jd.core.v1.model.javasyntax.statement.YieldExpressionStatement;
import org.jd.core.v1.service.converter.classfiletojavasyntax.model.javasyntax.statement.ClassFileTryStatement;
import org.jd.core.v1.service.converter.classfiletojavasyntax.model.javasyntax.expression.ClassFileLocalVariableReferenceExpression;
import org.jd.core.v1.service.converter.classfiletojavasyntax.model.localvariable.AbstractLocalVariable;
import org.jd.core.v1.model.javasyntax.type.ObjectType;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class MergeTryWithResourcesStatementVisitor implements StatementVisitor {

    @Override
    public void visit(IfElseStatement statement) {
        statement.getStatements().accept(this);
        statement.getElseStatements().accept(this);
    }

    @Override
    public void visit(SwitchStatement statement) {
        for (SwitchStatement.Block block : statement.getBlocks()) {
            block.getStatements().accept(this);
        }
    }

    @Override
    public void visit(TryStatement statement) {
        BaseStatement tryStatements = statement.getTryStatements();

        safeAcceptListStatement(statement.getResources());
        tryStatements.accept(this);
        safeAcceptListStatement(statement.getCatchClauses());
        safeAccept(statement.getFinallyStatements());

        if (tryStatements.isStatements()) {
            stripLeadingExceptionDeclarations((Statements) tryStatements);
        }

        if (tryStatements.size() == 1) {
            Statement first = tryStatements.getFirst();

            if (first.isTryStatement()) {
                ClassFileTryStatement cfswrs1 = (ClassFileTryStatement)statement;
                ClassFileTryStatement cfswrs2 = (ClassFileTryStatement)first;

                List<TryStatement.CatchClause> innerCatchClauses = cfswrs2.getCatchClauses();
                if (cfswrs2.getResources() != null
                        && (innerCatchClauses == null || innerCatchClauses.isEmpty())
                        && cfswrs2.getFinallyStatements() == null) {
                    // Merge 'try' and 'try-with-resources" statements
                    cfswrs1.setTryStatements(cfswrs2.getTryStatements());
                    cfswrs1.addResources(cfswrs2.getResources());
                }
            }
        }
    }

    @Override public void visit(DoWhileStatement statement) { safeAccept(statement.getStatements()); }
    @Override public void visit(ForEachStatement statement) { safeAccept(statement.getStatements()); }
    @Override public void visit(ForStatement statement) { safeAccept(statement.getStatements()); }
    @Override public void visit(IfStatement statement) { safeAccept(statement.getStatements()); }
    @Override
    public void visit(Statements list) {
        for (int i = 0; i < list.size(); i++) {
            Statement statement = list.get(i);
            statement.accept(this);
            if (statement instanceof ClassFileTryStatement tryStatement) {
                int newIndex = unwrapEcjSuppressedWrapper(list, i, tryStatement);
                if (newIndex < i) {
                    i = Math.max(-1, newIndex - 1);
                }
            }
        }
        stripLeadingExceptionDeclarations(list);
    }
    @Override public void visit(SynchronizedStatement statement) { safeAccept(statement.getStatements()); }
    @Override public void visit(TryStatement.CatchClause statement) { safeAccept(statement.getStatements()); }
    @Override public void visit(WhileStatement statement) { safeAccept(statement.getStatements()); }

    @Override public void visit(SwitchStatement.LabelBlock statement) { statement.getStatements().accept(this); }
    @Override public void visit(SwitchStatement.MultiLabelsBlock statement) { statement.getStatements().accept(this); }

    @Override public void visit(AssertStatement statement) {}
    @Override public void visit(BreakStatement statement) {}
    @Override public void visit(CommentStatement statement) {}
    @Override public void visit(ContinueStatement statement) {}
    @Override public void visit(ExpressionStatement statement) {}
    @Override public void visit(LabelStatement statement) {}
    @Override public void visit(LambdaExpressionStatement statement) {}
    @Override public void visit(LocalVariableDeclarationStatement statement) {}
    @Override public void visit(NoStatement statement) {}
    @Override public void visit(ReturnExpressionStatement statement) {}
    @Override public void visit(ReturnStatement statement) {}
    @Override public void visit(SwitchStatement.DefaultLabel statement) {}
    @Override public void visit(SwitchStatement.ExpressionLabel statement) {}
    @Override public void visit(ThrowStatement statement) {}
    @Override public void visit(TryStatement.Resource statement) {}
    @Override public void visit(TypeDeclarationStatement statement) {}
    @Override public void visit(YieldExpressionStatement statement) {}

    private int unwrapEcjSuppressedWrapper(Statements list, int index, ClassFileTryStatement tryStatement) {
        if (tryStatement.getResources() == null || tryStatement.getResources().isEmpty()) {
            return index;
        }
        if (tryStatement.getFinallyStatements() != null) {
            return index;
        }
        List<TryStatement.CatchClause> catchClauses = tryStatement.getCatchClauses();
        if (catchClauses == null || catchClauses.size() != 1) {
            return index;
        }
        TryStatement.CatchClause catchClause = catchClauses.get(0);
        if (!ObjectType.TYPE_THROWABLE.equals(catchClause.getType())) {
            return index;
        }
        BaseStatement catchStatements = catchClause.getStatements();
        if (catchStatements == null || catchStatements.size() == 0 || !catchStatements.getLast().isThrowStatement()) {
            return index;
        }
        if (!containsAddSuppressed(catchStatements)) {
            return index;
        }
        catchClauses.clear();
        int i = index - 1;
        while (i >= 0) {
            Statement previous = list.get(i);
            if (isNullAssignment(previous)) {
                list.remove(i);
                index--;
                i--;
                continue;
            }
            break;
        }
        return index;
    }

    private boolean containsAddSuppressed(BaseStatement statements) {
        if (statements == null || statements.size() == 0) {
            return false;
        }
        AddSuppressedVisitor visitor = new AddSuppressedVisitor();
        statements.accept(visitor);
        return visitor.found;
    }

    private static final class AddSuppressedVisitor extends AbstractJavaSyntaxVisitor {
        private boolean found;

        @Override
        public void visit(MethodInvocationExpression expression) {
            if ("addSuppressed".equals(expression.getName()) && "(Ljava/lang/Throwable;)V".equals(expression.getDescriptor())) {
                found = true;
            } else if (!found) {
                super.visit(expression);
            }
        }
    }

    private boolean isNullAssignment(Statement statement) {
        if (!(statement instanceof ExpressionStatement expressionStatement)) {
            return false;
        }
        Expression expression = expressionStatement.getExpression();
        if (!(expression instanceof BinaryOperatorExpression boe)) {
            return false;
        }
        if (!(boe.getRightExpression() instanceof NullExpression)) {
            return false;
        }
        return boe.getLeftExpression() instanceof ClassFileLocalVariableReferenceExpression;
    }

    private void stripLeadingExceptionDeclarations(Statements list) {
        if (list == null || list.isEmpty()) {
            return;
        }
        int index = 0;
        while (index < list.size()) {
            Statement statement = list.get(index);
            AbstractLocalVariable localVariable = getExceptionNullStatementLocalVariable(statement);
            if (localVariable != null) {
                index++;
                continue;
            }
            if (getAssignedLocalVariable(statement) != null) {
                index++;
                continue;
            }
            break;
        }
        if (index == 0 || list.size() - index != 1) {
            return;
        }
        Statement remaining = list.get(index);
        if (!(remaining instanceof ClassFileTryStatement tryStatement)) {
            return;
        }
        if (tryStatement.getResources() == null || tryStatement.getResources().isEmpty()) {
            return;
        }
        if ((tryStatement.getCatchClauses() != null && !tryStatement.getCatchClauses().isEmpty())
                || tryStatement.getFinallyStatements() != null) {
            return;
        }
        Set<String> resourceNames = getDeclaredResourceNames(tryStatement);
        for (int i = 0; i < index; i++) {
            Statement leading = list.get(i);
            AbstractLocalVariable assignedLocal = getAssignedLocalVariable(leading);
            if (assignedLocal == null) {
                return;
            }
            String assignedName = assignedLocal.getName();
            if (assignedName == null || !resourceNames.contains(assignedName)) {
                return;
            }
        }
        for (int i = 0; i < index; i++) {
            list.remove(0);
        }
    }

    private AbstractLocalVariable getAssignedLocalVariable(Statement statement) {
        if (!(statement instanceof ExpressionStatement expressionStatement)) {
            return null;
        }
        Expression expression = expressionStatement.getExpression();
        if (!(expression instanceof BinaryOperatorExpression boe)) {
            return null;
        }
        if (boe.getRightExpression() instanceof NullExpression) {
            return null;
        }
        Expression leftExpression = boe.getLeftExpression();
        if (!(leftExpression instanceof ClassFileLocalVariableReferenceExpression ref)) {
            return null;
        }
        return ref.getLocalVariable();
    }

    private Set<String> getDeclaredResourceNames(ClassFileTryStatement tryStatement) {
        Set<String> names = new HashSet<>();
        List<TryStatement.Resource> resources = tryStatement.getResources();
        if (resources == null || resources.isEmpty()) {
            return names;
        }
        for (TryStatement.Resource resource : resources) {
            if (resource == null || resource.isExpressionOnly()) {
                continue;
            }
            String name = resource.getName();
            if (name != null) {
                names.add(name);
            }
        }
        return names;
    }

    private AbstractLocalVariable getExceptionNullStatementLocalVariable(Statement statement) {
        if (!(statement instanceof ExpressionStatement expressionStatement)) {
            return null;
        }
        Expression expression = expressionStatement.getExpression();
        if (!(expression instanceof BinaryOperatorExpression boe)) {
            return null;
        }
        if (!(boe.getRightExpression() instanceof NullExpression)) {
            return null;
        }
        if (!(boe.getLeftExpression() instanceof ClassFileLocalVariableReferenceExpression ref)) {
            return null;
        }
        return ref.getLocalVariable();
    }

    protected void safeAccept(BaseStatement list) {
        if (list != null) {
            list.accept(this);
        }
    }

    protected void acceptListStatement(List<? extends Statement> list) {
        for (Statement statement : list) {
            statement.accept(this);
        }
    }

    protected void safeAcceptListStatement(List<? extends Statement> list) {
        if (list != null) {
            for (Statement statement : list) {
                statement.accept(this);
            }
        }
    }
}
