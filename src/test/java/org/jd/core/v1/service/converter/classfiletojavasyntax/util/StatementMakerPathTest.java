/*
 * Copyright (c) 2008, 2019 Emmanuel Dupuy.
 * This project is distributed under the GPLv3 license.
 * This is a Copyleft license that gives the user the right to use,
 * copy and modify the code freely for non-commercial purposes.
 */
package org.jd.core.v1.service.converter.classfiletojavasyntax.util;

import org.jd.core.v1.model.javasyntax.expression.BooleanExpression;
import org.jd.core.v1.model.javasyntax.statement.LabelStatement;
import org.jd.core.v1.model.javasyntax.statement.ReturnStatement;
import org.jd.core.v1.model.javasyntax.statement.Statement;
import org.jd.core.v1.model.javasyntax.statement.Statements;
import org.jd.core.v1.model.javasyntax.statement.SwitchStatement;
import org.jd.core.v1.util.DefaultList;
import org.junit.Test;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Predicate;

import static org.junit.Assert.assertNotNull;

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

    private static Object findPath(Statements root, Statement target) throws Exception {
        Method findPath = StatementMaker.class.getDeclaredMethod(
                "findPath", org.jd.core.v1.model.javasyntax.statement.BaseStatement.class,
                Predicate.class, List.class);
        findPath.setAccessible(true);
        Predicate<Statement> matcher = statement -> statement == target;
        return findPath.invoke(null, root, matcher, new ArrayList<>());
    }
}
