/*
 * Copyright (c) 2008, 2019 Emmanuel Dupuy.
 * This project is distributed under the GPLv3 license.
 * This is a Copyleft license that gives the user the right to use,
 * copy and modify the code freely for non-commercial purposes.
 */
package org.jd.core.v1.service.converter.classfiletojavasyntax.visitor;

import junit.framework.TestCase;
import org.jd.core.v1.loader.ClassPathLoader;
import org.jd.core.v1.model.javasyntax.declaration.MethodDeclaration;
import org.jd.core.v1.model.javasyntax.expression.BinaryOperatorExpression;
import org.jd.core.v1.model.javasyntax.expression.BooleanExpression;
import org.jd.core.v1.model.javasyntax.expression.Expression;
import org.jd.core.v1.model.javasyntax.expression.MethodInvocationExpression;
import org.jd.core.v1.model.javasyntax.statement.BreakStatement;
import org.jd.core.v1.model.javasyntax.statement.ContinueStatement;
import org.jd.core.v1.model.javasyntax.statement.DoWhileStatement;
import org.jd.core.v1.model.javasyntax.statement.ExpressionStatement;
import org.jd.core.v1.model.javasyntax.statement.IfStatement;
import org.jd.core.v1.model.javasyntax.statement.Statement;
import org.jd.core.v1.model.javasyntax.statement.Statements;
import org.jd.core.v1.model.javasyntax.statement.WhileStatement;
import org.jd.core.v1.model.javasyntax.type.ObjectType;
import org.jd.core.v1.model.javasyntax.type.PrimitiveType;
import org.jd.core.v1.service.converter.classfiletojavasyntax.model.javasyntax.expression.ClassFileLocalVariableReferenceExpression;
import org.jd.core.v1.service.converter.classfiletojavasyntax.model.javasyntax.statement.ClassFileBreakContinueStatement;
import org.jd.core.v1.service.converter.classfiletojavasyntax.model.localvariable.AbstractLocalVariable;
import org.jd.core.v1.service.converter.classfiletojavasyntax.model.localvariable.ObjectLocalVariable;
import org.jd.core.v1.service.converter.classfiletojavasyntax.util.TypeMaker;
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
    public void testNanDoWhileContinueBecomesBreak() {
        ClassFileBreakContinueStatement jump = new ClassFileBreakContinueStatement(10, 20);
        jump.setStatement(ContinueStatement.CONTINUE);
        IfStatement exit = new IfStatement(BooleanExpression.TRUE, jump);
        Statements body = new Statements(exit, new ExpressionStatement(invocation("next", PrimitiveType.TYPE_BOOLEAN)));
        DoWhileStatement loop = new DoWhileStatement(invocation("isNaN", PrimitiveType.TYPE_BOOLEAN), body);

        process(loop);

        assertSame(BreakStatement.BREAK, jump.getStatement());
    }
}
