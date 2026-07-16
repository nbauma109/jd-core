/*
 * Copyright (c) 2008, 2019 Emmanuel Dupuy.
 * This project is distributed under the GPLv3 license.
 * This is a Copyleft license that gives the user the right to use,
 * copy and modify the code freely for non-commercial purposes.
 */
package org.jd.core.v1;

import org.jd.core.v1.loader.ClassPathLoader;
import org.jd.core.v1.model.javasyntax.expression.BinaryOperatorExpression;
import org.jd.core.v1.model.javasyntax.expression.BooleanExpression;
import org.jd.core.v1.model.javasyntax.expression.Expression;
import org.jd.core.v1.model.javasyntax.expression.Expressions;
import org.jd.core.v1.model.javasyntax.expression.MethodInvocationExpression;
import org.jd.core.v1.model.javasyntax.expression.NullExpression;
import org.jd.core.v1.model.javasyntax.expression.TernaryOperatorExpression;
import org.jd.core.v1.model.javasyntax.statement.BaseStatement;
import org.jd.core.v1.model.javasyntax.statement.BreakStatement;
import org.jd.core.v1.model.javasyntax.statement.ContinueStatement;
import org.jd.core.v1.model.javasyntax.statement.DoWhileStatement;
import org.jd.core.v1.model.javasyntax.statement.ExpressionStatement;
import org.jd.core.v1.model.javasyntax.statement.ForStatement;
import org.jd.core.v1.model.javasyntax.statement.IfElseStatement;
import org.jd.core.v1.model.javasyntax.statement.IfStatement;
import org.jd.core.v1.model.javasyntax.statement.LabelStatement;
import org.jd.core.v1.model.javasyntax.statement.ReturnStatement;
import org.jd.core.v1.model.javasyntax.statement.Statement;
import org.jd.core.v1.model.javasyntax.statement.Statements;
import org.jd.core.v1.model.javasyntax.statement.SynchronizedStatement;
import org.jd.core.v1.model.javasyntax.statement.ThrowStatement;
import org.jd.core.v1.model.javasyntax.statement.TryStatement;
import org.jd.core.v1.model.javasyntax.statement.WhileStatement;
import org.jd.core.v1.model.javasyntax.type.ObjectType;
import org.jd.core.v1.model.javasyntax.type.PrimitiveType;
import org.jd.core.v1.service.converter.classfiletojavasyntax.model.javasyntax.expression.ClassFileLocalVariableReferenceExpression;
import org.jd.core.v1.service.converter.classfiletojavasyntax.model.javasyntax.statement.ClassFileTryStatement;
import org.jd.core.v1.service.converter.classfiletojavasyntax.model.localvariable.AbstractLocalVariable;
import org.jd.core.v1.service.converter.classfiletojavasyntax.model.localvariable.ObjectLocalVariable;
import org.jd.core.v1.service.converter.classfiletojavasyntax.util.TypeMaker;
import org.jd.core.v1.service.converter.classfiletojavasyntax.visitor.FixHoistedCatchThrowVisitor;
import org.jd.core.v1.service.converter.classfiletojavasyntax.visitor.FixMissingNullGuardVisitor;
import org.jd.core.v1.util.DefaultList;
import org.junit.Test;

import junit.framework.TestCase;

/**
 * Direct AST-level exercises of {@link FixMissingNullGuardVisitor} and {@link FixHoistedCatchThrowVisitor}:
 * javac often restructures the source patterns of {@link JsoupPatternsTest} before these visitors see them,
 * so the corner branches (guard lookup through nested constructs, continue-path classification, unsafe break
 * shapes) are pinned down here on hand-built statements.
 */
public class FixVisitorsAstTest extends TestCase {

    private final TypeMaker typeMaker = new TypeMaker(new ClassPathLoader());
    private final AbstractLocalVariable node = new ObjectLocalVariable(typeMaker, 1, 0, ObjectType.TYPE_OBJECT, "node");
    private final AbstractLocalVariable other = new ObjectLocalVariable(typeMaker, 2, 0, ObjectType.TYPE_OBJECT, "other");

    private Expression nodeRef() {
        return new ClassFileLocalVariableReferenceExpression(1, 0, node);
    }

    private Expression otherRef() {
        return new ClassFileLocalVariableReferenceExpression(1, 0, other);
    }

    private Statement nullAssign() {
        return new ExpressionStatement(new BinaryOperatorExpression(1, ObjectType.TYPE_OBJECT, nodeRef(), "=", new NullExpression(ObjectType.TYPE_OBJECT), 16));
    }

    /** {@code if (restartFlag) { node = null; continue; }} — the branch the reducer may have truncated. */
    private IfStatement restartBranch() {
        Statements body = new Statements();
        body.add(nullAssign());
        body.add(ContinueStatement.CONTINUE);
        return new IfStatement(otherRef(), body);
    }

    private IfStatement nullGuard(boolean returnsValue) {
        Expression condition = new BinaryOperatorExpression(1, PrimitiveType.TYPE_BOOLEAN, nodeRef(), "==", new NullExpression(ObjectType.TYPE_OBJECT), 9);
        Statements body = new Statements();
        body.add(returnsValue
            ? new org.jd.core.v1.model.javasyntax.statement.ReturnExpressionStatement(new NullExpression(ObjectType.TYPE_OBJECT))
            : ReturnStatement.RETURN);
        return new IfStatement(condition, body);
    }

    private Statement nodeDeref() {
        return new ExpressionStatement(invocation(nodeRef(), null));
    }

    private MethodInvocationExpression invocation(Expression receiver, org.jd.core.v1.model.javasyntax.expression.BaseExpression parameters) {
        return new MethodInvocationExpression(1, ObjectType.TYPE_OBJECT, receiver, "java/lang/Object", "toString", "()Ljava/lang/String;", parameters, null);
    }

    private static Statements loopIn(Statement... statements) {
        Statements body = new Statements();
        for (Statement s : statements) {
            body.add(s);
        }
        Statements list = new Statements();
        list.add(new WhileStatement(BooleanExpression.TRUE, body));
        return list;
    }

    private static int size(BaseStatement statements) {
        return ((Statements) statements).size();
    }

    // --- FixMissingNullGuardVisitor ---

    @Test
    public void testGuardInsertedWhenDereferenceProven() {
        IfStatement branch = restartBranch();
        Statements list = loopIn(nodeDeref(), branch, nullGuard(true));

        list.accept(new FixMissingNullGuardVisitor());

        // guard cloned before the continue
        assertEquals(3, size(branch.getStatements()));
    }

    @Test
    public void testGuardWithPlainReturnInserted() {
        IfStatement branch = restartBranch();
        Statements list = loopIn(nodeDeref(), branch, nullGuard(false));

        list.accept(new FixMissingNullGuardVisitor());

        assertEquals(3, size(branch.getStatements()));
    }

    @Test
    public void testNoInsertWhenPathReassignsFirst() {
        IfStatement branch = restartBranch();
        Statement reassign = new ExpressionStatement(new BinaryOperatorExpression(1, ObjectType.TYPE_OBJECT, nodeRef(), "=", otherRef(), 16));
        Statements list = loopIn(reassign, branch, nullGuard(true));

        list.accept(new FixMissingNullGuardVisitor());

        assertEquals(2, size(branch.getStatements()));
    }

    @Test
    public void testNoInsertWhenPathNullChecksFirst() {
        IfStatement branch = restartBranch();
        Statement check = new ExpressionStatement(new BinaryOperatorExpression(1, PrimitiveType.TYPE_BOOLEAN, new NullExpression(ObjectType.TYPE_OBJECT), "!=", nodeRef(), 9));
        Statements list = loopIn(check, branch, nullGuard(true));

        list.accept(new FixMissingNullGuardVisitor());

        assertEquals(2, size(branch.getStatements()));
    }

    @Test
    public void testNoInsertWhenTernaryNullChecksFirst() {
        IfStatement branch = restartBranch();
        Expression ternary = new TernaryOperatorExpression(1, ObjectType.TYPE_OBJECT,
            new BinaryOperatorExpression(1, PrimitiveType.TYPE_BOOLEAN, nodeRef(), "==", new NullExpression(ObjectType.TYPE_OBJECT), 9),
            otherRef(), nodeRef());
        Statement useTernaryArg = new ExpressionStatement(invocation(otherRef(), ternary));
        Statements list = loopIn(useTernaryArg, branch, nullGuard(true));

        list.accept(new FixMissingNullGuardVisitor());

        assertEquals(2, size(branch.getStatements()));
    }

    @Test
    public void testMultiArgumentUseScansAllArguments() {
        IfStatement branch = restartBranch();
        Expressions args = new Expressions();
        args.add(otherRef());
        args.add(invocation(nodeRef(), null)); // dereference in the second argument
        Statement call = new ExpressionStatement(invocation(otherRef(), args));
        Statements list = loopIn(call, branch, nullGuard(true));

        list.accept(new FixMissingNullGuardVisitor());

        assertEquals(3, size(branch.getStatements()));
    }

    @Test
    public void testForLoopScansUpdateBeforeBody() {
        IfStatement branch = restartBranch();
        // for (;; node = other) { ... } : the update reassigns before any dereference
        Expression update = new BinaryOperatorExpression(1, ObjectType.TYPE_OBJECT, nodeRef(), "=", otherRef(), 16);
        Statements body = new Statements();
        body.add(nodeDeref());
        body.add(branch);
        body.add(nullGuard(true));
        Statements list = new Statements();
        list.add(new ForStatement(null, BooleanExpression.TRUE, update, body));

        list.accept(new FixMissingNullGuardVisitor());

        assertEquals(2, size(branch.getStatements()));
    }

    @Test
    public void testDoWhileLoopScansConditionBeforeBody() {
        IfStatement branch = restartBranch();
        // do { ... } while (node.toString() != null) : dereferenced by the condition on the continue path
        Expression condition = new BinaryOperatorExpression(1, PrimitiveType.TYPE_BOOLEAN, invocation(nodeRef(), null), "!=", new NullExpression(ObjectType.TYPE_OBJECT), 9);
        Statements body = new Statements();
        body.add(nodeDeref());
        body.add(branch);
        body.add(nullGuard(true));
        Statements list = new Statements();
        list.add(new DoWhileStatement(condition, body));

        list.accept(new FixMissingNullGuardVisitor());

        assertEquals(3, size(branch.getStatements()));
    }

    @Test
    public void testGuardFoundThroughNestedConstructs() {
        // The guard sits behind a label, inside the else of an if-else, inside a try's catch clause,
        // inside a synchronized block: findNullGuard has to walk all of them.
        Statements sync = new Statements();
        sync.add(nullGuard(true));
        Statements catchBody = new Statements();
        catchBody.add(new SynchronizedStatement(otherRef(), sync));
        DefaultList<TryStatement.CatchClause> catchClauses = new DefaultList<>();
        catchClauses.add(new ClassFileTryStatement.CatchClause(1, ObjectType.TYPE_EXCEPTION, other, catchBody));
        Statements tryBody = new Statements();
        tryBody.add(new ExpressionStatement(invocation(otherRef(), null)));
        Statement tryStatement = new ClassFileTryStatement(tryBody, catchClauses, null, false);
        Statements elseBody = new Statements();
        elseBody.add(tryStatement);
        Statement ifElse = new IfElseStatement(otherRef(), new Statements(), elseBody);
        Statement labeled = new LabelStatement("label1", ifElse);

        IfStatement branch = restartBranch();
        Statements list = loopIn(nodeDeref(), branch, labeled);

        list.accept(new FixMissingNullGuardVisitor());

        assertEquals(3, size(branch.getStatements()));
    }

    @Test
    public void testGuardVariantsNotMatched() {
        // "!=" comparison, "==" against a non-null value, and a two-statement body are not guards
        Expression notEquals = new BinaryOperatorExpression(1, PrimitiveType.TYPE_BOOLEAN, nodeRef(), "!=", new NullExpression(ObjectType.TYPE_OBJECT), 9);
        Statements returnBody = new Statements();
        returnBody.add(ReturnStatement.RETURN);
        Statement wrongOperator = new IfStatement(notEquals, returnBody);

        Expression nonNull = new BinaryOperatorExpression(1, PrimitiveType.TYPE_BOOLEAN, nodeRef(), "==", otherRef(), 9);
        Statements returnBody2 = new Statements();
        returnBody2.add(ReturnStatement.RETURN);
        Statement wrongComparand = new IfStatement(nonNull, returnBody2);

        Expression isNull = new BinaryOperatorExpression(1, PrimitiveType.TYPE_BOOLEAN, nodeRef(), "==", new NullExpression(ObjectType.TYPE_OBJECT), 9);
        Statements twoStatements = new Statements();
        twoStatements.add(new ExpressionStatement(invocation(otherRef(), null)));
        twoStatements.add(ReturnStatement.RETURN);
        Statement wrongBody = new IfStatement(isNull, twoStatements);

        IfStatement branch = restartBranch();
        Statements list = loopIn(nodeDeref(), branch, wrongOperator, wrongComparand, wrongBody);

        list.accept(new FixMissingNullGuardVisitor());

        // No valid guard to clone: the branch keeps its two statements
        assertEquals(2, size(branch.getStatements()));
    }

    @Test
    public void testForEachAndDoWhileAndSingleStatementBodies() {
        // ForEach loop containing the branch: continue path starts at the body
        IfStatement branch = restartBranch();
        Statements forEachBody = new Statements();
        forEachBody.add(nodeDeref());
        forEachBody.add(branch);
        forEachBody.add(nullGuard(true));
        Statements list = new Statements();
        list.add(new org.jd.core.v1.model.javasyntax.statement.ForEachStatement(ObjectType.TYPE_OBJECT, "item", otherRef(), forEachBody));

        list.accept(new FixMissingNullGuardVisitor());

        assertEquals(3, size(branch.getStatements()));

        // Loop with a single-statement body: nothing to scan, nothing to break
        Statements single = new Statements();
        single.add(new WhileStatement(BooleanExpression.TRUE, nodeDeref()));
        single.accept(new FixMissingNullGuardVisitor());
    }

    @Test
    public void testGuardWithNullOnLeftSideMatched() {
        // 'if (null == node) return other;' is the same guard with flipped operands
        IfStatement branch = restartBranch();
        Expression condition = new BinaryOperatorExpression(1, PrimitiveType.TYPE_BOOLEAN, new NullExpression(ObjectType.TYPE_OBJECT), "==", nodeRef(), 9);
        Statements guardBody = new Statements();
        guardBody.add(new org.jd.core.v1.model.javasyntax.statement.ReturnExpressionStatement(otherRef()));
        Statement guard = new IfStatement(condition, guardBody);
        Statements list = loopIn(nodeDeref(), branch, guard);

        list.accept(new FixMissingNullGuardVisitor());

        assertEquals(3, size(branch.getStatements()));
    }

    @Test
    public void testGuardFoundInsideNestedLoop() {
        IfStatement branch = restartBranch();
        Statements innerBody = new Statements();
        innerBody.add(nullGuard(true));
        Statement innerLoop = new WhileStatement(BooleanExpression.TRUE, innerBody);
        Statements list = loopIn(nodeDeref(), branch, innerLoop);

        list.accept(new FixMissingNullGuardVisitor());

        assertEquals(3, size(branch.getStatements()));
    }

    @Test
    public void testBranchVariantsIgnored() {
        // A branch whose last statement is not a continue, one with a labeled continue,
        // and one whose second-to-last statement assigns a non-null value: all left alone
        Statements noContinue = new Statements();
        noContinue.add(nullAssign());
        noContinue.add(new ExpressionStatement(invocation(otherRef(), null)));
        IfStatement branch1 = new IfStatement(otherRef(), noContinue);

        Statements labeledContinue = new Statements();
        labeledContinue.add(nullAssign());
        labeledContinue.add(new ContinueStatement("label9"));
        IfStatement branch2 = new IfStatement(otherRef(), labeledContinue);

        Statements nonNullAssign = new Statements();
        nonNullAssign.add(new ExpressionStatement(new BinaryOperatorExpression(1, ObjectType.TYPE_OBJECT, nodeRef(), "=", otherRef(), 16)));
        nonNullAssign.add(ContinueStatement.CONTINUE);
        IfStatement branch3 = new IfStatement(otherRef(), nonNullAssign);

        Statements list = loopIn(nodeDeref(), branch1, branch2, branch3, nullGuard(true));

        list.accept(new FixMissingNullGuardVisitor());

        assertEquals(2, size(branch1.getStatements()));
        assertEquals(2, size(branch2.getStatements()));
        assertEquals(2, size(branch3.getStatements()));
    }

    // --- FixHoistedCatchThrowVisitor ---

    private Expression caughtRef() {
        return new ClassFileLocalVariableReferenceExpression(1, 0, other);
    }

    private Statement tryWithCatchContaining(Statement... catchStatements) {
        Statements catchBody = new Statements();
        for (Statement s : catchStatements) {
            catchBody.add(s);
        }
        DefaultList<TryStatement.CatchClause> catchClauses = new DefaultList<>();
        catchClauses.add(new ClassFileTryStatement.CatchClause(1, ObjectType.TYPE_EXCEPTION, other, catchBody));
        Statements tryBody = new Statements();
        tryBody.add(new ExpressionStatement(invocation(nodeRef(), null)));
        return new ClassFileTryStatement(tryBody, catchClauses, null, false);
    }

    private static Statements listOf(Statement... statements) {
        Statements list = new Statements();
        for (Statement s : statements) {
            list.add(s);
        }
        return list;
    }

    @Test
    public void testHoistedThrowMovedBackIntoCatch() {
        Statements catchBreak = listOf(tryWithCatchContaining(new IfStatement(otherRef(), listOf(BreakStatement.BREAK))));
        Statements list = listOf(new LabelStatement("label1", new WhileStatement(BooleanExpression.TRUE, catchBreak)), new ThrowStatement(caughtRef()));

        list.accept(new FixHoistedCatchThrowVisitor());

        // The trailing 'throw e;' was folded back over the break
        assertEquals(1, list.size());
    }

    @Test
    public void testUnmatchedBreakOutsideCatchIsUnsafe() {
        // An unlabeled break in the loop body proper could be a real loop exit: nothing may change
        Statements body = listOf(
            new IfStatement(otherRef(), listOf(BreakStatement.BREAK)),
            tryWithCatchContaining(new IfStatement(otherRef(), listOf(BreakStatement.BREAK))));
        Statements list = listOf(new WhileStatement(BooleanExpression.TRUE, body), new ThrowStatement(caughtRef()));

        list.accept(new FixHoistedCatchThrowVisitor());

        assertEquals(2, list.size());
    }

    @Test
    public void testLabeledBreakIsIgnored() {
        // Only bare breaks can stand in for the hoisted throw; labeled ones target something else
        Statements body = listOf(tryWithCatchContaining(new BreakStatement("label2")));
        Statements list = listOf(new WhileStatement(BooleanExpression.TRUE, body), new ThrowStatement(caughtRef()));

        list.accept(new FixHoistedCatchThrowVisitor());

        assertEquals(2, list.size());
    }

    @Test
    public void testScanTraversesNestedConstructs() {
        // The break hides behind if-else, synchronized, a label and a finally block on the way
        Statement deep = new LabelStatement("label3", new SynchronizedStatement(otherRef(),
            listOf(new IfElseStatement(otherRef(), new Statements(), listOf(BreakStatement.BREAK)))));
        Statements finallyBody = listOf(new ExpressionStatement(invocation(otherRef(), null)));
        DefaultList<TryStatement.CatchClause> catchClauses = new DefaultList<>();
        catchClauses.add(new ClassFileTryStatement.CatchClause(1, ObjectType.TYPE_EXCEPTION, other, listOf(deep)));
        Statement tryStatement = new ClassFileTryStatement(listOf(nodeDeref()), catchClauses, finallyBody, false);
        Statements list = listOf(new WhileStatement(BooleanExpression.TRUE, listOf(tryStatement)), new ThrowStatement(caughtRef()));

        list.accept(new FixHoistedCatchThrowVisitor());

        assertEquals(1, list.size());
    }

    @Test
    public void testThrowOfNewExpressionUntouched() {
        // Only 'throw <local var>' can be a hoisted catch variable
        Statements body = listOf(tryWithCatchContaining(BreakStatement.BREAK));
        Statements list = listOf(new WhileStatement(BooleanExpression.TRUE, body), new ThrowStatement(invocation(otherRef(), null)));

        list.accept(new FixHoistedCatchThrowVisitor());

        assertEquals(2, list.size());
    }

    @Test
    public void testSingleStatementLoopBodyUntouched() {
        Statements list = listOf(new WhileStatement(BooleanExpression.TRUE, nodeDeref()), new ThrowStatement(caughtRef()));

        list.accept(new FixHoistedCatchThrowVisitor());

        assertEquals(2, list.size());
    }
}
