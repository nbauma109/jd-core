/*
 * Copyright (c) 2008, 2019 Emmanuel Dupuy.
 * This project is distributed under the GPLv3 license.
 * This is a Copyleft license that gives the user the right to use,
 * copy and modify the code freely for non-commercial purposes.
 */
package org.jd.core.v1.service.converter.classfiletojavasyntax.visitor;

import junit.framework.TestCase;
import org.jd.core.v1.loader.ClassPathLoader;
import org.jd.core.v1.model.javasyntax.declaration.LocalVariableDeclarator;
import org.jd.core.v1.model.javasyntax.declaration.MethodDeclaration;
import org.jd.core.v1.model.javasyntax.expression.BinaryOperatorExpression;
import org.jd.core.v1.model.javasyntax.expression.BooleanExpression;
import org.jd.core.v1.model.javasyntax.expression.Expression;
import org.jd.core.v1.model.javasyntax.expression.MethodInvocationExpression;
import org.jd.core.v1.model.javasyntax.expression.PostOperatorExpression;
import org.jd.core.v1.model.javasyntax.statement.BreakStatement;
import org.jd.core.v1.model.javasyntax.statement.ContinueStatement;
import org.jd.core.v1.model.javasyntax.statement.DoWhileStatement;
import org.jd.core.v1.model.javasyntax.statement.ExpressionStatement;
import org.jd.core.v1.model.javasyntax.statement.ForStatement;
import org.jd.core.v1.model.javasyntax.statement.IfStatement;
import org.jd.core.v1.model.javasyntax.statement.IfElseStatement;
import org.jd.core.v1.model.javasyntax.statement.LocalVariableDeclarationStatement;
import org.jd.core.v1.model.javasyntax.statement.Statement;
import org.jd.core.v1.model.javasyntax.statement.Statements;
import org.jd.core.v1.model.javasyntax.statement.SwitchStatement;
import org.jd.core.v1.model.javasyntax.statement.WhileStatement;
import org.jd.core.v1.model.javasyntax.type.ObjectType;
import org.jd.core.v1.model.javasyntax.type.PrimitiveType;
import org.jd.core.v1.service.converter.classfiletojavasyntax.model.javasyntax.expression.ClassFileLocalVariableReferenceExpression;
import org.jd.core.v1.service.converter.classfiletojavasyntax.model.javasyntax.statement.ClassFileBreakContinueStatement;
import org.jd.core.v1.service.converter.classfiletojavasyntax.model.javasyntax.statement.ClassFileContinueStatement;
import org.jd.core.v1.service.converter.classfiletojavasyntax.model.localvariable.AbstractLocalVariable;
import org.jd.core.v1.service.converter.classfiletojavasyntax.model.localvariable.ObjectLocalVariable;
import org.jd.core.v1.service.converter.classfiletojavasyntax.util.TypeMaker;
import org.jd.core.v1.util.DefaultList;
import org.junit.Test;

public class HoistUndeclaredLocalVariablesVisitorTest extends TestCase {

    private final AbstractLocalVariable value = new ObjectLocalVariable(
            new TypeMaker(new ClassPathLoader()), 1, 0, ObjectType.TYPE_OBJECT, "value");

    private Expression valueReference() {
        return new ClassFileLocalVariableReferenceExpression(1, 0, value);
    }

    private MethodInvocationExpression invocation(String name, PrimitiveType returnType) {
        return new MethodInvocationExpression(
                1, returnType, valueReference(), "java/lang/Object", name, "()Z", null, null);
    }

    private BinaryOperatorExpression equalityCondition() {
        return new BinaryOperatorExpression(
                1, PrimitiveType.TYPE_BOOLEAN, BooleanExpression.TRUE, "==", new BooleanExpression(1, false), 9);
    }

    private void process(Statement statement) {
        Statements methodStatements = new Statements();
        methodStatements.add(statement);
        process(methodStatements);
    }

    private void process(Statements methodStatements) {
        MethodDeclaration method = new MethodDeclaration(0, "test", PrimitiveType.TYPE_VOID, "()V", methodStatements);
        new HoistUndeclaredLocalVariablesVisitor().visit(method);
    }

    @Test
    public void testInfiniteLoopExitExpressionGetsBreak() {
        ExpressionStatement exitExpression = new ExpressionStatement(invocation("stop", PrimitiveType.TYPE_BOOLEAN));
        IfStatement exit = new IfStatement(equalityCondition(), exitExpression);
        Statements body = new Statements();
        body.add(exit);
        WhileStatement loop = new WhileStatement(BooleanExpression.TRUE, body);

        process(loop);

        IfStatement transformedExit = (IfStatement) body.getLast();
        Statements exitStatements = (Statements) transformedExit.getStatements();
        assertEquals(2, exitStatements.size());
        assertSame(exitExpression, exitStatements.getFirst());
        assertSame(BreakStatement.BREAK, exitStatements.getLast());
    }

    @Test
    public void testInfiniteLoopExitBlockGetsBreak() {
        ExpressionStatement exitExpression = new ExpressionStatement(invocation("stop", PrimitiveType.TYPE_BOOLEAN));
        Statements exitStatements = new Statements();
        exitStatements.add(exitExpression);
        IfStatement exit = new IfStatement(equalityCondition(), exitStatements);
        Statements body = new Statements();
        body.add(exit);
        WhileStatement loop = new WhileStatement(BooleanExpression.TRUE, body);

        process(loop);

        assertEquals(2, exitStatements.size());
        assertSame(BreakStatement.BREAK, exitStatements.getLast());
    }

    @Test
    public void testNanDoWhilePreservesContinue() {
        ClassFileBreakContinueStatement jump = new ClassFileBreakContinueStatement(10, 20);
        jump.setStatement(ContinueStatement.CONTINUE);
        IfStatement exit = new IfStatement(BooleanExpression.TRUE, jump);
        Statements body = new Statements(exit, new ExpressionStatement(invocation("next", PrimitiveType.TYPE_BOOLEAN)));
        DoWhileStatement loop = new DoWhileStatement(invocation("isNaN", PrimitiveType.TYPE_BOOLEAN), body);

        process(loop);

        assertSame(ContinueStatement.CONTINUE, jump.getStatement());
    }

    @Test
    public void testInfiniteForContinueTargetingUpdateIsNotRewritten() {
        PostOperatorExpression update = new PostOperatorExpression(
                1, new ClassFileLocalVariableReferenceExpression(1, 11, value), "++");
        ClassFileBreakContinueStatement jump = new ClassFileBreakContinueStatement(20, 10);
        jump.setStatement(ContinueStatement.CONTINUE);
        Statements thenStatements = new Statements(new ExpressionStatement(
                invocation("next", PrimitiveType.TYPE_BOOLEAN)), jump);
        Statements loopStatements = new Statements();
        loopStatements.add(new IfStatement(equalityCondition(), thenStatements));
        ForStatement loop = new ForStatement(null, null, update, loopStatements);
        Statements methodStatements = new Statements(
                new LocalVariableDeclarationStatement(PrimitiveType.TYPE_INT, new LocalVariableDeclarator("seed")),
                loop);

        process(methodStatements);

        assertNull(loop.getCondition());
        assertSame(update, loop.getUpdate().getFirst());
        assertEquals(2, methodStatements.size());
        assertSame(jump, thenStatements.getLast());
    }

    @Test
    public void testMisplacedForUpdateRequiresDistinctContinueTarget() {
        PostOperatorExpression update = new PostOperatorExpression(
                1, new ClassFileLocalVariableReferenceExpression(1, 11, value), "++");
        ClassFileBreakContinueStatement jump = new ClassFileBreakContinueStatement(20, 4);
        jump.setStatement(ContinueStatement.CONTINUE);
        ExpressionStatement bodyStatement = new ExpressionStatement(
                invocation("next", PrimitiveType.TYPE_BOOLEAN));
        Statements thenStatements = new Statements(bodyStatement, jump);
        Statements loopStatements = new Statements();
        loopStatements.add(new IfStatement(equalityCondition(), thenStatements));
        ForStatement loop = new ForStatement(null, null, update, loopStatements);
        Statements methodStatements = new Statements(
                new LocalVariableDeclarationStatement(PrimitiveType.TYPE_INT, new LocalVariableDeclarator("seed")),
                loop);

        process(methodStatements);

        assertSame(update, ((ExpressionStatement) methodStatements.getLast()).getExpression());
        assertNull(loop.getUpdate());
        assertSame(bodyStatement, ((Statements) loop.getStatements()).getFirst());
    }

    @Test
    public void testRecoveredUpdateIsCopiedIntoBothIfElseBranches() {
        PostOperatorExpression update = new PostOperatorExpression(
                1, new ClassFileLocalVariableReferenceExpression(1, 11, value), "++");
        Statements thenStatements = new Statements();
        thenStatements.add(new ClassFileContinueStatement(10));
        Statements elseStatements = new Statements();
        elseStatements.add(new ClassFileContinueStatement(10));
        IfElseStatement branch = new IfElseStatement(BooleanExpression.TRUE, thenStatements, elseStatements);
        Statements loopStatements = new Statements();
        loopStatements.add(branch);
        WhileStatement loop = new WhileStatement(BooleanExpression.TRUE, loopStatements);
        Statements methodStatements = new Statements(new ExpressionStatement(update), loop);

        process(methodStatements);

        assertEquals(2, thenStatements.size());
        assertEquals(2, elseStatements.size());
        assertTrue(thenStatements.getFirst() instanceof ExpressionStatement);
        assertTrue(elseStatements.getFirst() instanceof ExpressionStatement);
    }

    @Test
    public void testRecoveredUpdateDescendsThroughNestedIfIntoIfElse() {
        PostOperatorExpression update = new PostOperatorExpression(
                1, new ClassFileLocalVariableReferenceExpression(1, 11, value), "++");
        Statements thenStatements = new Statements();
        thenStatements.add(new ClassFileContinueStatement(10));
        Statements elseStatements = new Statements();
        elseStatements.add(new ClassFileContinueStatement(10));
        IfElseStatement branch = new IfElseStatement(BooleanExpression.TRUE, thenStatements, elseStatements);
        IfStatement nested = new IfStatement(BooleanExpression.TRUE,
                new IfStatement(BooleanExpression.TRUE, branch));
        Statements loopStatements = new Statements();
        loopStatements.add(nested);
        WhileStatement loop = new WhileStatement(BooleanExpression.TRUE, loopStatements);
        Statements methodStatements = new Statements(new ExpressionStatement(update), loop);

        process(methodStatements);

        assertEquals(2, thenStatements.size());
        assertEquals(2, elseStatements.size());
    }

    @Test
    public void testRecoveredUpdateIsCopiedBeforeSwitchContinue() {
        PostOperatorExpression update = new PostOperatorExpression(
                1, new ClassFileLocalVariableReferenceExpression(1, 11, value), "++");
        Statements caseStatements = new Statements();
        caseStatements.add(new ClassFileContinueStatement(10));
        DefaultList<SwitchStatement.Block> blocks = new DefaultList<>();
        blocks.add(new SwitchStatement.LabelBlock(SwitchStatement.DEFAULT_LABEL, caseStatements));
        SwitchStatement switchStatement = new SwitchStatement(BooleanExpression.TRUE, blocks);
        Statements loopStatements = new Statements();
        loopStatements.add(switchStatement);
        WhileStatement loop = new WhileStatement(BooleanExpression.TRUE, loopStatements);
        Statements methodStatements = new Statements(new ExpressionStatement(update), loop);

        process(methodStatements);

        assertEquals(2, caseStatements.size());
        assertTrue(caseStatements.getFirst() instanceof ExpressionStatement);
        assertTrue(caseStatements.getLast() instanceof ClassFileContinueStatement);
    }

    @Test
    public void testTrailingBreakAfterBodylessInfiniteLoopIsRemoved() {
        WhileStatement loop = new WhileStatement(BooleanExpression.TRUE, null);
        Statements methodStatements = new Statements(loop, BreakStatement.BREAK);

        process(methodStatements);

        assertEquals(1, methodStatements.size());
        assertSame(loop, methodStatements.getFirst());
    }

    @Test
    public void testTrailingBreakRemainsWhenInfiniteLoopBodyCanBreak() {
        Statements body = new Statements();
        body.add(BreakStatement.BREAK);
        WhileStatement loop = new WhileStatement(BooleanExpression.TRUE, body);
        Statements methodStatements = new Statements(loop, BreakStatement.BREAK);

        process(methodStatements);

        assertEquals(2, methodStatements.size());
        assertSame(BreakStatement.BREAK, methodStatements.getLast());
    }
}
