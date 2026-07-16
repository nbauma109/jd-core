/*
 * Copyright (c) 2008, 2019 Emmanuel Dupuy.
 * This project is distributed under the GPLv3 license.
 * This is a Copyleft license that gives the user the right to use,
 * copy and modify the code freely for non-commercial purposes.
 */
package org.jd.core.v1.service.converter.classfiletojavasyntax.util;

import org.jd.core.v1.model.javasyntax.expression.BooleanExpression;
import org.jd.core.v1.model.javasyntax.statement.BreakStatement;
import org.jd.core.v1.model.javasyntax.statement.LabelStatement;
import org.jd.core.v1.model.javasyntax.statement.ReturnStatement;
import org.jd.core.v1.model.javasyntax.statement.Statement;
import org.jd.core.v1.model.javasyntax.statement.Statements;
import org.jd.core.v1.model.javasyntax.statement.SwitchStatement;
import org.jd.core.v1.service.converter.classfiletojavasyntax.model.javasyntax.statement.ClassFileBreakContinueStatement;
import org.jd.core.v1.util.DefaultList;
import org.junit.Test;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Predicate;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;

public class StatementMakerPathTest {
    @Test
    public void findsStatementInsideExistingLabel() throws Exception {
        Statement target = ReturnStatement.RETURN;
        Statements root = new Statements();
        root.add(new LabelStatement("existing", target));

        assertNotNull(findPath(root, target));
    }

    @Test
    public void findsStatementInsideSwitchBlock() throws Exception {
        Statement target = ReturnStatement.RETURN;
        Statements caseBody = new Statements();
        caseBody.add(target);
        DefaultList<SwitchStatement.Block> blocks = new DefaultList<>();
        blocks.add(new SwitchStatement.LabelBlock(SwitchStatement.DEFAULT_LABEL, caseBody));
        Statements root = new Statements();
        root.add(new SwitchStatement(BooleanExpression.TRUE, blocks));

        assertNotNull(findPath(root, target));
    }

    @Test
    public void unresolvedJumpUsesAlwaysThrowingFallback() throws Exception {
        ClassFileBreakContinueStatement jump = new ClassFileBreakContinueStatement(1, 2);
        Method fallback = StatementMaker.class.getDeclaredMethod("useThrowFallback", List.class);
        fallback.setAccessible(true);

        fallback.invoke(null, List.of(jump));

        assertTrue(jump.getStatement().isThrowStatement());
        assertTrue(jump.getStatement().getExpression().isNullExpression());
    }

    @Test
    public void wrapsCommonPrefixWithLabel() throws Exception {
        ClassFileBreakContinueStatement jump = new ClassFileBreakContinueStatement(1, 2);
        Statement target = ReturnStatement.RETURN;
        Statements root = statements(jump, target);

        assertTrue(wrap(root, List.of(jump), target));
        assertTrue(root.getFirst().isLabelStatement());
        assertSame(target, root.getLast());
    }

    @Test
    public void rejectsMissingTargetOrBreakSite() throws Exception {
        ClassFileBreakContinueStatement jump = new ClassFileBreakContinueStatement(1, 2);
        Statement target = ReturnStatement.RETURN;

        assertFalse(wrap(statements(jump), List.of(jump), target));
        assertFalse(wrap(statements(target), List.of(jump), target));
    }

    @Test
    public void rejectsTargetAtScopeStartOrBreakAfterTarget() throws Exception {
        Statement target = new BreakStatement("target");
        ClassFileBreakContinueStatement jump = new ClassFileBreakContinueStatement(1, 2);

        assertFalse(wrap(statements(target), List.of(), target));
        assertFalse(wrap(statements(ReturnStatement.RETURN, target, jump), List.of(jump), target));
    }

    private static Object findPath(Statements root, Statement target) throws Exception {
        Method findPath = StatementMaker.class.getDeclaredMethod(
                "findPath", org.jd.core.v1.model.javasyntax.statement.BaseStatement.class,
                Predicate.class, List.class);
        findPath.setAccessible(true);
        Predicate<Statement> matcher = statement -> statement == target;
        return findPath.invoke(null, root, matcher, new ArrayList<>());
    }

    private static boolean wrap(Statements root, List<ClassFileBreakContinueStatement> jumps,
                                Statement target) throws Exception {
        Method wrap = StatementMaker.class.getDeclaredMethod("wrapCommonScopeWithLabel",
                Statements.class, List.class, Statement.class, String.class);
        wrap.setAccessible(true);
        return (boolean) wrap.invoke(null, root, jumps, target, "testLabel");
    }

    private static Statements statements(Statement... statements) {
        Statements result = new Statements();
        for (Statement statement : statements) {
            result.add(statement);
        }
        return result;
    }
}
