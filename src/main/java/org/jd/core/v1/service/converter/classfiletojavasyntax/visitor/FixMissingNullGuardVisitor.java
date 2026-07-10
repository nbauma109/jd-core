/*
 * Copyright (c) 2008, 2019 Emmanuel Dupuy.
 * This project is distributed under the GPLv3 license.
 * This is a Copyleft license that gives the user the right to use,
 * copy and modify the code freely for non-commercial purposes.
 */
package org.jd.core.v1.service.converter.classfiletojavasyntax.visitor;

import org.jd.core.v1.model.javasyntax.AbstractJavaSyntaxVisitor;
import org.jd.core.v1.model.javasyntax.expression.BaseExpression;
import org.jd.core.v1.model.javasyntax.expression.BinaryOperatorExpression;
import org.jd.core.v1.model.javasyntax.expression.Expression;
import org.jd.core.v1.model.javasyntax.expression.Expressions;
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

import static org.jd.core.v1.model.javasyntax.expression.NoExpression.NO_EXPRESSION;

/**
 * The control flow reducer can lose a shared "if (var == null) return ...;" guard for a loop branch that
 * reassigns 'var' to null and then falls straight into a bare 'continue;': the bytecode's merge block for that
 * check has a predecessor inside an already-reduced nested loop as well as this direct one, so the reducer's
 * natural-loop membership analysis doesn't recognize it as belonging to this loop and the check ends up
 * reachable only from the other predecessor (see org.jsoup.nodes.NodeIterator#findNextNode). Rather than fixing
 * that reduction (which risks the CFG engine broadly), this looks for the exact same guard already present
 * elsewhere in the loop for the same variable and duplicates it in front of the 'continue;', matching what the
 * source must have looked like.
 *
 * <p>Legitimate source can also contain "var = null; continue;", so the guard is only inserted when the loop's
 * continuation path provably dereferences 'var' before any null check or reassignment — i.e. when the decompiled
 * loop is already guaranteed to throw a NullPointerException that the original bytecode could not.</p>
 */
public class FixMissingNullGuardVisitor extends AbstractJavaSyntaxVisitor {

    /** Classification of the first decisive use of the target variable along the loop's continue path. */
    private enum Use {
        /** No decisive use found yet; keep scanning. */
        NONE,
        /** Null-checked, reassigned, or flow becomes untrackable: do not touch the loop. */
        SAFE,
        /** Dereferenced while provably null: the guard was lost by the reducer. */
        DEREF
    }

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

        for (Statement statement : body) {
            tryFixBranch(loop, body, statement);
        }
    }

    private void tryFixBranch(Statement loop, Statements body, Statement statement) {
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

        if (guard != null && classifyContinuePath(loop, target) == Use.DEREF) {
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

    /**
     * Follows the loop's continue path (for-update, then condition, then the body statements in order) and
     * classifies the first decisive use of 'target' encountered.
     */
    private static Use classifyContinuePath(Statement loop, AbstractLocalVariable target) {
        Use use = Use.NONE;

        if (loop.isForStatement()) {
            use = classifyUse(loop.getUpdate(), target);
        }
        if (use == Use.NONE && (loop.isForStatement() || loop.isWhileStatement() || loop.isDoWhileStatement())) {
            use = classifyUse(loop.getCondition(), target);
        }
        if (use == Use.NONE && loop.getStatements() instanceof Statements body) {
            for (Statement s : body) {
                use = classifyStatement(s, target);
                if (use != Use.NONE) {
                    break;
                }
            }
        }

        return use;
    }

    private static Use classifyStatement(Statement s, AbstractLocalVariable target) {
        if (s.isExpressionStatement()) {
            return classifyUse(s.getExpression(), target);
        }
        if (s.isIfStatement() || s.isIfElseStatement() || s.isSwitchStatement() || s.isWhileStatement()) {
            Use use = classifyUse(s.getCondition(), target);
            // Once control forks, the linear scan cannot follow: stop without inserting.
            return use == Use.NONE ? Use.SAFE : use;
        }
        // Return, throw, break, declarations, try blocks, ...: the linear fallthrough ends here.
        return Use.SAFE;
    }

    private static Use classifyUse(BaseExpression base, AbstractLocalVariable target) {
        if (base == null || base == NO_EXPRESSION) {
            return Use.NONE;
        }
        if (base instanceof Expressions list) {
            for (Expression e : list) {
                Use use = classifyUse(e, target);
                if (use != Use.NONE) {
                    return use;
                }
            }
            return Use.NONE;
        }
        if (!(base instanceof Expression expression)) {
            return Use.NONE;
        }
        if (expression.isBinaryOperatorExpression()) {
            return classifyBinaryUse(expression, target);
        }
        if (expression.isMethodInvocationExpression()) {
            if (referencesTarget(expression.getExpression(), target)) {
                return Use.DEREF;
            }
            Use use = classifyUse(expression.getExpression(), target);
            return use == Use.NONE ? classifyUse(expression.getParameters(), target) : use;
        }
        if (expression.isFieldReferenceExpression() || expression.isArrayExpression() || expression.isLengthExpression()) {
            if (referencesTarget(expression.getExpression(), target)) {
                return Use.DEREF;
            }
            Use use = classifyUse(expression.getExpression(), target);
            return use == Use.NONE ? classifyUse(expression.getIndex(), target) : use;
        }
        if (expression.isTernaryOperatorExpression()) {
            Use use = classifyUse(expression.getCondition(), target);
            if (use == Use.NONE) {
                use = classifyUse(expression.getTrueExpression(), target);
            }
            if (use == Use.NONE) {
                use = classifyUse(expression.getFalseExpression(), target);
            }
            return use;
        }

        Expression inner = expression.getExpression();
        return inner == expression ? Use.NONE : classifyUse(inner, target);
    }

    private static Use classifyBinaryUse(Expression expression, AbstractLocalVariable target) {
        String operator = expression.getOperator();
        Expression left = expression.getLeftExpression();
        Expression right = expression.getRightExpression();

        if ("=".equals(operator) && referencesTarget(left, target)) {
            // The right-hand side is evaluated before the variable is overwritten
            Use use = classifyUse(right, target);
            return use == Use.NONE ? Use.SAFE : use;
        }
        if (("==".equals(operator) || "!=".equals(operator))
                && (referencesTarget(left, target) && right.isNullExpression()
                 || referencesTarget(right, target) && left.isNullExpression())) {
            return Use.SAFE;
        }

        Use use = classifyUse(left, target);
        return use == Use.NONE ? classifyUse(right, target) : use;
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
            return findNullGuardInTry(tryStatement, target);
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

    private static IfStatement findNullGuardInTry(TryStatement tryStatement, AbstractLocalVariable target) {
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
