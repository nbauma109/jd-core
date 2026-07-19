/*
 * Copyright (c) 2008-2026 Emmanuel Dupuy.
 * This project is distributed under the GPLv3 license.
 * This is a Copyleft license that gives the user the right to use,
 * copy and modify the code freely for non-commercial purposes.
 */

package org.jd.core.v1.service.converter.classfiletojavasyntax.util;

import junit.framework.TestCase;
import org.jd.core.v1.model.javasyntax.expression.BooleanExpression;
import org.jd.core.v1.model.javasyntax.statement.BreakStatement;
import org.jd.core.v1.model.javasyntax.statement.ContinueStatement;
import org.jd.core.v1.model.javasyntax.statement.ExpressionStatement;
import org.jd.core.v1.model.javasyntax.statement.IfStatement;
import org.jd.core.v1.model.javasyntax.statement.Statements;
import org.jd.core.v1.model.javasyntax.statement.TryStatement;
import org.jd.core.v1.service.converter.classfiletojavasyntax.model.javasyntax.statement.ClassFileContinueStatement;
import org.jd.core.v1.service.converter.classfiletojavasyntax.model.javasyntax.statement.ClassFileIfStatement;
import org.junit.Test;

public class LoopStatementMakerTest extends TestCase {
    @Test
    public void testRestoresProvenExitNestedInOrdinaryIf() {
        Statements exitStatements = new Statements();
        exitStatements.add(new ExpressionStatement(BooleanExpression.TRUE));
        ClassFileIfStatement provenExit = new ClassFileIfStatement(BooleanExpression.TRUE, exitStatements);
        Statements wrapperStatements = new Statements();
        wrapperStatements.add(provenExit);
        Statements loopStatements = new Statements();
        loopStatements.add(new IfStatement(BooleanExpression.TRUE, wrapperStatements));

        LoopStatementMaker.restoreProvenForEachBreaks(loopStatements);

        assertSame(BreakStatement.BREAK, exitStatements.getLast());
    }

    @Test
    public void testPreservesContinueTargetInsideTry() {
        Statements tryStatements = new Statements();
        tryStatements.add(ContinueStatement.CONTINUE);
        Statements loopStatements = new Statements();
        loopStatements.add(new TryStatement(tryStatements, null, null));

        LoopStatementMaker.preserveNestedContinueTargets(loopStatements, 42);

        assertTrue(tryStatements.getFirst() instanceof ClassFileContinueStatement);
        assertEquals(42, ((ClassFileContinueStatement) tryStatements.getFirst()).getTargetOffset());
    }
}
