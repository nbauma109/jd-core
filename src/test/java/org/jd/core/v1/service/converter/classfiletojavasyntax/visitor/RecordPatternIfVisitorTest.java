/*
 * Copyright (c) 2026 Nicolas Baumann (@nbauma109), with Claude Sonnet 5.5.
 * This project is distributed under the GPLv3 license.
 * This is a Copyleft license that gives the user the right to use,
 * copy and modify the code freely for non-commercial purposes.
 */

package org.jd.core.v1.service.converter.classfiletojavasyntax.visitor;

import org.jd.core.v1.model.javasyntax.declaration.ExpressionVariableInitializer;
import org.jd.core.v1.model.javasyntax.declaration.LocalVariableDeclarator;
import org.jd.core.v1.model.javasyntax.declaration.LocalVariableDeclarators;
import org.jd.core.v1.model.javasyntax.expression.Expression;
import org.jd.core.v1.model.javasyntax.expression.InstanceOfExpression;
import org.jd.core.v1.model.javasyntax.expression.LocalVariableReferenceExpression;
import org.jd.core.v1.model.javasyntax.expression.MethodInvocationExpression;
import org.jd.core.v1.model.javasyntax.expression.NewExpression;
import org.jd.core.v1.model.javasyntax.pattern.RecordPattern;
import org.jd.core.v1.model.javasyntax.pattern.TypePattern;
import org.jd.core.v1.model.javasyntax.statement.ExpressionStatement;
import org.jd.core.v1.model.javasyntax.statement.IfElseStatement;
import org.jd.core.v1.model.javasyntax.statement.IfStatement;
import org.jd.core.v1.model.javasyntax.statement.LocalVariableDeclarationStatement;
import org.jd.core.v1.model.javasyntax.statement.Statement;
import org.jd.core.v1.model.javasyntax.statement.Statements;
import org.jd.core.v1.model.javasyntax.statement.ThrowStatement;
import org.jd.core.v1.model.javasyntax.statement.TryStatement;
import org.jd.core.v1.model.javasyntax.type.ObjectType;
import org.jd.core.v1.model.javasyntax.type.PrimitiveType;
import org.jd.core.v1.model.javasyntax.type.Type;
import org.jd.core.v1.util.DefaultList;
import org.junit.Test;

import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;

/** The lowered form of 'if (r instanceof R(int x, Object o)) {...}' which javac writes, and the shapes which look like it */
public class RecordPatternIfVisitorTest {
    private static final ObjectType RECORD = new ObjectType("p/R", "p.R", "R");
    private static final ObjectType THROWABLE = new ObjectType("java/lang/Throwable", "java.lang.Throwable", "Throwable");
    private static final ObjectType EXCEPTION = new ObjectType("java/lang/Exception", "java.lang.Exception", "Exception");
    private static final ObjectType MATCH_EXCEPTION = new ObjectType("java/lang/MatchException", "java.lang.MatchException", "MatchException");

    private static Expression variable(Type type, String name) {
        return new LocalVariableReferenceExpression(type, name);
    }

    private static LocalVariableDeclarator declarator(String name, Expression value) {
        return new LocalVariableDeclarator(1, name, new ExpressionVariableInitializer(value));
    }

    private static Expression accessor(String recordName, String name, Type type) {
        return new MethodInvocationExpression(type, variable(RECORD, recordName), "p/R", name, "()V", null);
    }

    /** 'int i = r1.x(), x = i;' */
    private static Statement twoDeclarators(Type type, String recordName, String accessor, String temporary, String name) {
        LocalVariableDeclarators declarators = new LocalVariableDeclarators(2);

        declarators.add(declarator(temporary, accessor(recordName, accessor, type)));
        declarators.add(declarator(name, variable(type, temporary)));
        return new LocalVariableDeclarationStatement(type, declarators);
    }

    /** 'int x = r1.x();' */
    private static Statement oneDeclarator(Type type, String recordName, String accessor, String name) {
        return new LocalVariableDeclarationStatement(type, declarator(name, accessor(recordName, accessor, type)));
    }

    private static Statement body() {
        return new ExpressionStatement(variable(PrimitiveType.TYPE_INT, "body"));
    }

    private static TryStatement matchExceptionTry(Statements tryStatements) {
        Statements handler = new Statements();
        handler.add(new ThrowStatement(new NewExpression(1, MATCH_EXCEPTION, "(Ljava/lang/String;Ljava/lang/Throwable;)V", false, false)));

        DefaultList<TryStatement.CatchClause> catchClauses = new DefaultList<>();
        catchClauses.add(new TryStatement.CatchClause(1, THROWABLE, "e", handler));
        return new TryStatement(tryStatements, catchClauses, null);
    }

    private static Statements statements(Statement... statements) {
        Statements list = new Statements();

        for (Statement statement : statements) {
            list.add(statement);
        }
        return list;
    }

    /** @param recordComponents the names of the components of the record 'p.R', none if it is no record */
    private static Statement rewrite(IfStatement ifStatement, String... recordComponents) {
        Statements statements = statements(ifStatement);

        new RecordPatternIfVisitor(name -> List.of(recordComponents)).visit(statements);
        return statements.getFirst();
    }

    private static Statements lowered(Statement... components) {
        Statements tryStatements = statements(components);

        tryStatements.add(body());
        return statements(new LocalVariableDeclarationStatement(RECORD, declarator("r1", variable(RECORD, "r"))), matchExceptionTry(tryStatements));
    }

    private static IfStatement ifInstanceOf(Statements then) {
        return new IfStatement(new InstanceOfExpression(1, variable(RECORD, "r"), RECORD), then);
    }

    private static void assertRewritten(Statement statement, String... componentNames) {
        InstanceOfExpression condition = (InstanceOfExpression) ((IfStatement) statement).getCondition();
        RecordPattern pattern = (RecordPattern) condition.getPattern();

        assertEquals(componentNames.length, pattern.componentPatterns().size());
        for (int i = 0; i < componentNames.length; i++) {
            assertEquals(componentNames[i], ((TypePattern) pattern.componentPatterns().get(i)).variableName());
        }
        assertEquals(1, ((Statements) ((IfStatement) statement).getStatements()).size());
    }

    @Test
    public void testRecordVariableCopy() {
        Statement rewritten = rewrite(ifInstanceOf(lowered(twoDeclarators(PrimitiveType.TYPE_INT, "r1", "x", "i", "x"), twoDeclarators(ObjectType.TYPE_OBJECT, "r1", "o", "o1", "o"))), "x", "o");

        assertRewritten(rewritten, "x", "o");
    }

    @Test
    public void testSingleDeclarator() {
        assertRewritten(rewrite(ifInstanceOf(lowered(oneDeclarator(PrimitiveType.TYPE_INT, "r1", "x", "x"))), "x"), "x");
    }

    @Test
    public void testVariableOfTheTest() {
        Statements tryStatements = statements(twoDeclarators(PrimitiveType.TYPE_INT, "r1", "x", "i", "x"), body());
        IfStatement ifStatement = new IfStatement(new InstanceOfExpression(1, variable(RECORD, "r"), RECORD, "r1", false), statements(matchExceptionTry(tryStatements)));

        assertRewritten(rewrite(ifStatement, "x"), "x");
    }

    @Test
    public void testElseIsKept() {
        Statements elseStatements = statements(body());
        IfStatement ifStatement = new IfElseStatement(new InstanceOfExpression(1, variable(RECORD, "r"), RECORD),
                lowered(oneDeclarator(PrimitiveType.TYPE_INT, "r1", "x", "x")), elseStatements);
        Statement rewritten = rewrite(ifStatement, "x");

        assertTrue(rewritten instanceof IfElseStatement);
        assertSame(elseStatements, ((IfElseStatement) rewritten).getElseStatements());
        assertRewritten(rewritten, "x");
    }

    @Test
    public void testNestedStatementsAreVisited() {
        Statements inner = statements(ifInstanceOf(lowered(oneDeclarator(PrimitiveType.TYPE_INT, "r1", "x", "x"))));
        Statement outer = new IfStatement(variable(PrimitiveType.TYPE_BOOLEAN, "flag"), inner);

        assertRewritten(((Statements) rewrite((IfStatement) outer, "x").getStatements()).getFirst(), "x");
    }

    @Test
    public void testNotRewritten() {
        Statement condition = new IfStatement(variable(PrimitiveType.TYPE_BOOLEAN, "flag"), statements(body()));
        Statement emptyThen = ifInstanceOf(new Statements());
        Statement noTry = ifInstanceOf(statements(body(), body()));
        Statement noRecordVariable = ifInstanceOf(statements(body(), matchExceptionTry(statements(body()))));
        Statement withoutComponents = ifInstanceOf(lowered());
        Statement otherVariable = ifInstanceOf(lowered(twoDeclarators(PrimitiveType.TYPE_INT, "other", "x", "i", "x")));
        Statement badSecondDeclarator = ifInstanceOf(lowered(declarationWithSecondDeclaratorOfAnotherVariable()));

        for (Statement statement : new Statement[] {condition, emptyThen, noTry, noRecordVariable, withoutComponents, otherVariable, badSecondDeclarator}) {
            assertSame(statement, rewrite((IfStatement) statement, "x"));
        }
    }

    private static Statement declarationWithSecondDeclaratorOfAnotherVariable() {
        LocalVariableDeclarators declarators = new LocalVariableDeclarators(2);

        declarators.add(declarator("i", accessor("r1", "x", PrimitiveType.TYPE_INT)));
        declarators.add(declarator("x", variable(PrimitiveType.TYPE_INT, "unrelated")));
        return new LocalVariableDeclarationStatement(PrimitiveType.TYPE_INT, declarators);
    }

    @Test
    public void testTryWhichIsNoMatchException() {
        Statements tryStatements = statements(twoDeclarators(PrimitiveType.TYPE_INT, "r1", "x", "i", "x"), body());
        DefaultList<TryStatement.CatchClause> otherType = new DefaultList<>();
        otherType.add(new TryStatement.CatchClause(1, EXCEPTION, "e", statements(new ThrowStatement(new NewExpression(1, MATCH_EXCEPTION, "()V", false, false)))));
        DefaultList<TryStatement.CatchClause> notAThrow = new DefaultList<>();
        notAThrow.add(new TryStatement.CatchClause(1, THROWABLE, "e", statements(body())));
        DefaultList<TryStatement.CatchClause> twoCatches = new DefaultList<>();
        twoCatches.add(new TryStatement.CatchClause(1, THROWABLE, "e", statements(body())));
        twoCatches.add(new TryStatement.CatchClause(1, EXCEPTION, "f", statements(body())));

        TryStatement[] tries = {
            new TryStatement(tryStatements, otherType, null),
            new TryStatement(tryStatements, notAThrow, null),
            new TryStatement(tryStatements, twoCatches, null),
            new TryStatement(tryStatements, new DefaultList<>(), null),
            new TryStatement(tryStatements, matchExceptionTry(tryStatements).getCatchClauses(), statements(body()))
        };

        for (TryStatement tryStatement : tries) {
            IfStatement ifStatement = ifInstanceOf(statements(new LocalVariableDeclarationStatement(RECORD, declarator("r1", variable(RECORD, "r"))), tryStatement));

            assertSame(ifStatement, rewrite(ifStatement, "x"));
        }
    }

    @Test
    public void testTypeWhichIsNoRecordOrWhoseAccessorsDifferIsNoRecordPattern() {
        IfStatement noRecord = ifInstanceOf(lowered(oneDeclarator(PrimitiveType.TYPE_INT, "r1", "length", "n")));
        IfStatement otherAccessor = ifInstanceOf(lowered(oneDeclarator(PrimitiveType.TYPE_INT, "r1", "x", "x")));
        IfStatement missingComponent = ifInstanceOf(lowered(oneDeclarator(PrimitiveType.TYPE_INT, "r1", "x", "x")));

        assertSame(noRecord, rewrite(noRecord));
        assertSame(otherAccessor, rewrite(otherAccessor, "y"));
        assertSame(missingComponent, rewrite(missingComponent, "x", "o"));
    }
}
