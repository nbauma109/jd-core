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
import org.jd.core.v1.model.javasyntax.expression.NullExpression;
import org.jd.core.v1.model.javasyntax.statement.BaseStatement;
import org.jd.core.v1.model.javasyntax.statement.ContinueStatement;
import org.jd.core.v1.model.javasyntax.statement.DoWhileStatement;
import org.jd.core.v1.model.javasyntax.statement.ForEachStatement;
import org.jd.core.v1.model.javasyntax.statement.ForStatement;
import org.jd.core.v1.model.javasyntax.statement.IfElseStatement;
import org.jd.core.v1.model.javasyntax.statement.IfStatement;
import org.jd.core.v1.model.javasyntax.statement.LabelStatement;
import org.jd.core.v1.model.javasyntax.statement.ReturnExpressionStatement;
import org.jd.core.v1.model.javasyntax.statement.ReturnStatement;
import org.jd.core.v1.model.javasyntax.statement.Statement;
import org.jd.core.v1.model.javasyntax.statement.Statements;
import org.jd.core.v1.model.javasyntax.statement.SynchronizedStatement;
import org.jd.core.v1.model.javasyntax.statement.TryStatement;
import org.jd.core.v1.model.javasyntax.statement.WhileStatement;
import org.jd.core.v1.service.converter.classfiletojavasyntax.model.javasyntax.expression.ClassFileLocalVariableReferenceExpression;
import org.jd.core.v1.service.converter.classfiletojavasyntax.model.javasyntax.statement.ClassFileBreakContinueStatement;
import org.jd.core.v1.service.converter.classfiletojavasyntax.model.localvariable.AbstractLocalVariable;

/**
 * The control flow reducer can lose a shared "if (var == null) return ...;" guard for a loop branch that
 * reassigns 'var' to null and then falls straight into a bare 'continue;': the bytecode's merge block for that
 * check has a predecessor inside an already-reduced nested loop as well as this direct one, so the reducer's
 * natural-loop membership analysis doesn't recognize it as belonging to this loop and the check ends up
 * reachable only from the other predecessor (see org.jsoup.nodes.NodeIterator#findNextNode). Rather than fixing
 * that reduction (which risks the CFG engine broadly), this looks for the exact same guard already present
 * elsewhere in the loop for the same variable and duplicates it in front of the 'continue;', matching what the
 * source must have looked like.
 */
public class FixMissingNullGuardVisitor extends AbstractJavaSyntaxVisitor {

    @Override
    public void visit(Statements list) {
        for (int i = 0; i < list.size(); i++) {
            Statement current = list.get(i);
            Statement loop = current instanceof LabelStatement labelStatement && labelStatement.statement() instanceof Statement inner ? inner : current;

            if (isLoop(loop)) {
                tryFixLoop(loop);
            }
        }

        acceptListStatement(list);
    }

    private static boolean isLoop(Statement statement) {
        return statement instanceof WhileStatement || statement instanceof DoWhileStatement
            || statement instanceof ForStatement || statement instanceof ForEachStatement;
    }

    private void tryFixLoop(Statement loop) {
        if (!(loop.getStatements() instanceof Statements body)) {
            return;
        }

        for (int i = 0; i < body.size(); i++) {
            tryFixBranch(body, i, body.get(i));
        }
    }

    private void tryFixBranch(Statements body, int index, Statement statement) {
        if (!(statement instanceof IfStatement ifStatement) || !(ifStatement.getStatements() instanceof Statements ifBody) || ifBody.size() < 2) {
            return;
        }
        if (!isUnlabeledContinue(ifBody.getLast())) {
            return;
        }

        AbstractLocalVariable target = extractNullAssignmentTarget(ifBody.get(ifBody.size() - 2));

        if (target == null) {
            return;
        }

        IfStatement guard = findNullGuard(body, target);

        if (guard != null) {
            ifBody.add(ifBody.size() - 1, cloneNullGuard(guard, target));
        }
    }

    private static boolean isUnlabeledContinue(Statement statement) {
        if (statement instanceof ClassFileBreakContinueStatement wrapper) {
            statement = wrapper.getStatement();
        }
        return statement instanceof ContinueStatement continueStatement && continueStatement.getLabel() == null;
    }

    private static AbstractLocalVariable extractNullAssignmentTarget(Statement statement) {
        if (!statement.isExpressionStatement()) {
            return null;
        }

        Expression expression = statement.getExpression();

        if (!expression.isBinaryOperatorExpression() || !"=".equals(expression.getOperator()) || !expression.getRightExpression().isNullExpression()) {
            return null;
        }

        return expression.getLeftExpression() instanceof ClassFileLocalVariableReferenceExpression varRef ? varRef.getLocalVariable() : null;
    }

    private static IfStatement findNullGuard(BaseStatement statement, AbstractLocalVariable target) {
        if (!(statement instanceof Statements list)) {
            return null;
        }

        for (Statement s : list) {
            IfStatement found = findNullGuardInStatement(s, target);

            if (found != null) {
                return found;
            }
        }

        return null;
    }

    private static IfStatement findNullGuardInStatement(Statement s, AbstractLocalVariable target) {
        if (s instanceof IfStatement ifStatement) {
            return isNullGuard(ifStatement, target) ? ifStatement : findNullGuard(ifStatement.getStatements(), target);
        }
        if (s instanceof IfElseStatement ifElseStatement) {
            IfStatement found = findNullGuard(ifElseStatement.getStatements(), target);
            return found != null ? found : findNullGuard(ifElseStatement.getElseStatements(), target);
        }
        if (s instanceof TryStatement tryStatement) {
            IfStatement found = findNullGuard(tryStatement.getTryStatements(), target);

            if (found != null) {
                return found;
            }
            if (tryStatement.getCatchClauses() != null) {
                for (TryStatement.CatchClause catchClause : tryStatement.getCatchClauses()) {
                    found = findNullGuard(catchClause.getStatements(), target);
                    if (found != null) {
                        return found;
                    }
                }
            }
            return findNullGuard(tryStatement.getFinallyStatements(), target);
        }
        if (s instanceof SynchronizedStatement synchronizedStatement) {
            return findNullGuard(synchronizedStatement.getStatements(), target);
        }
        if (isLoop(s)) {
            return findNullGuard(s.getStatements(), target);
        }
        if (s instanceof LabelStatement labelStatement && labelStatement.statement() instanceof Statement inner) {
            return findNullGuardInStatement(inner, target);
        }
        return null;
    }

    private static boolean isNullGuard(IfStatement ifStatement, AbstractLocalVariable target) {
        Expression condition = ifStatement.getCondition();

        if (!condition.isBinaryOperatorExpression() || !"==".equals(condition.getOperator())) {
            return false;
        }

        Expression left = condition.getLeftExpression();
        Expression right = condition.getRightExpression();
        boolean targetOnOneSide = referencesTarget(left, target) || referencesTarget(right, target);
        boolean nullOnOtherSide = left.isNullExpression() || right.isNullExpression();

        if (!targetOnOneSide || !nullOnOtherSide) {
            return false;
        }

        return ifStatement.getStatements() instanceof Statements ifBody && ifBody.size() == 1
            && (ifBody.getFirst().isReturnStatement() || ifBody.getFirst().isReturnExpressionStatement());
    }

    private static boolean referencesTarget(Expression expression, AbstractLocalVariable target) {
        return expression instanceof ClassFileLocalVariableReferenceExpression varRef && varRef.getLocalVariable() == target;
    }

    private static IfStatement cloneNullGuard(IfStatement guard, AbstractLocalVariable target) {
        Expression condition = guard.getCondition();
        Expression newCondition = new BinaryOperatorExpression(condition.getLineNumber(), condition.getType(),
            cloneOperand(condition.getLeftExpression(), target), condition.getOperator(), cloneOperand(condition.getRightExpression(), target));

        Statement guardBody = ((Statements) guard.getStatements()).getFirst();
        Statement newBody = guardBody.isReturnExpressionStatement()
            ? new ReturnExpressionStatement(guardBody.getLineNumber(), cloneOperand(guardBody.getExpression(), target))
            : ReturnStatement.RETURN;

        Statements newBodyStatements = new Statements();
        newBodyStatements.add(newBody);
        return new IfStatement(newCondition, newBodyStatements);
    }

    private static Expression cloneOperand(Expression expression, AbstractLocalVariable target) {
        if (expression.isNullExpression()) {
            return new NullExpression(expression.getLineNumber(), expression.getType());
        }
        if (expression instanceof ClassFileLocalVariableReferenceExpression varRef && varRef.getLocalVariable() == target) {
            return new ClassFileLocalVariableReferenceExpression(expression.getLineNumber(), varRef.getOffset(), target);
        }
        return expression;
    }
}
