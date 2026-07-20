/*
 * Copyright (c) 2008, 2019 Emmanuel Dupuy.
 * This project is distributed under the GPLv3 license.
 * This is a Copyleft license that gives the user the right to use,
 * copy and modify the code freely for non-commercial purposes.
 */
package org.jd.core.v1.service.converter.classfiletojavasyntax.visitor;

import junit.framework.TestCase;
import org.jd.core.v1.model.javasyntax.expression.BooleanExpression;
import org.jd.core.v1.model.javasyntax.statement.BreakStatement;
import org.jd.core.v1.model.javasyntax.statement.ContinueStatement;
import org.jd.core.v1.model.javasyntax.statement.ExpressionStatement;
import org.jd.core.v1.model.javasyntax.statement.Statements;
import org.jd.core.v1.model.javasyntax.statement.SwitchStatement;
import org.jd.core.v1.util.DefaultList;
import org.junit.Test;

public class RemoveLastContinueStatementVisitorTest extends TestCase {

    @Test
    public void testContinueInNonTerminalSwitchIsPreserved() {
        Statements caseStatements = new Statements();
        caseStatements.add(ContinueStatement.CONTINUE);
        SwitchStatement switchStatement = switchWith(caseStatements);
        Statements loopStatements = new Statements();
        loopStatements.add(switchStatement);
        loopStatements.add(new ExpressionStatement(BooleanExpression.TRUE));

        loopStatements.accept(new RemoveLastContinueStatementVisitor());

        assertSame(ContinueStatement.CONTINUE, caseStatements.getLast());
    }

    @Test
    public void testContinueInTerminalSwitchBecomesBreak() {
        Statements caseStatements = new Statements();
        caseStatements.add(ContinueStatement.CONTINUE);
        Statements loopStatements = new Statements();
        loopStatements.add(switchWith(caseStatements));

        loopStatements.accept(new RemoveLastContinueStatementVisitor());

        assertSame(BreakStatement.BREAK, caseStatements.getLast());
    }

    private static SwitchStatement switchWith(Statements caseStatements) {
        DefaultList<SwitchStatement.Block> blocks = new DefaultList<>();
        blocks.add(new SwitchStatement.LabelBlock(SwitchStatement.DEFAULT_LABEL, caseStatements));
        return new SwitchStatement(BooleanExpression.TRUE, blocks);
    }
}
