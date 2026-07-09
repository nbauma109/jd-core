/*
 * Copyright (c) 2008, 2019 Emmanuel Dupuy.
 * This project is distributed under the GPLv3 license.
 * This is a Copyleft license that gives the user the right to use,
 * copy and modify the code freely for non-commercial purposes.
 */
package org.jd.core.v1.service.converter.classfiletojavasyntax.visitor;

import org.jd.core.v1.model.javasyntax.AbstractJavaSyntaxVisitor;
import org.jd.core.v1.model.javasyntax.statement.BaseStatement;
import org.jd.core.v1.model.javasyntax.statement.BreakStatement;
import org.jd.core.v1.model.javasyntax.statement.DoWhileStatement;
import org.jd.core.v1.model.javasyntax.statement.ForEachStatement;
import org.jd.core.v1.model.javasyntax.statement.ForStatement;
import org.jd.core.v1.model.javasyntax.statement.IfElseStatement;
import org.jd.core.v1.model.javasyntax.statement.IfStatement;
import org.jd.core.v1.model.javasyntax.statement.LabelStatement;
import org.jd.core.v1.model.javasyntax.statement.Statement;
import org.jd.core.v1.model.javasyntax.statement.Statements;
import org.jd.core.v1.model.javasyntax.statement.SynchronizedStatement;
import org.jd.core.v1.model.javasyntax.statement.ThrowStatement;
import org.jd.core.v1.model.javasyntax.statement.TryStatement;
import org.jd.core.v1.model.javasyntax.statement.WhileStatement;
import org.jd.core.v1.service.converter.classfiletojavasyntax.model.javasyntax.expression.ClassFileLocalVariableReferenceExpression;
import org.jd.core.v1.service.converter.classfiletojavasyntax.model.javasyntax.statement.ClassFileTryStatement;
import org.jd.core.v1.service.converter.classfiletojavasyntax.model.localvariable.AbstractLocalVariable;

/**
 * A short-circuit condition such as {@code if (expired() || timeout == 0) throw e;} placed at the
 * end of a catch block compiles to a single 'athrow' block reached both by a direct jump and by
 * fallthrough from the loop's back-edge test. The control flow reducer can mistake that shared
 * 'athrow' block for code located after the enclosing loop and hoist it there as a trailing
 * 'throw e;', replacing its occurrences inside the loop with 'break;'. That hoisted statement no
 * longer compiles because 'e' is scoped to the catch clause. This visitor detects the shape and
 * moves the throw back inside the catch clause, replacing the break(s) it stands in for.
 */
public class FixHoistedCatchThrowVisitor extends AbstractJavaSyntaxVisitor {
    private AbstractLocalVariable target;
    private int lineNumber;
    private int offset;
    private boolean safe;
    private int matchCount;

    @Override
    public void visit(Statements list) {
        for (int i = 0; i < list.size() - 1; i++) {
            Statement current = list.get(i);
            Statement loop = current instanceof LabelStatement labelStatement && labelStatement.statement() instanceof Statement inner ? inner : current;

            if (!isLoop(loop)) {
                continue;
            }

            if (!(list.get(i + 1) instanceof ThrowStatement throwStatement)) {
                continue;
            }
            if (!(throwStatement.getExpression() instanceof ClassFileLocalVariableReferenceExpression varRef)) {
                continue;
            }

            BaseStatement loopBody = getLoopBody(loop);

            if (!(loopBody instanceof Statements)) {
                continue;
            }

            target = varRef.getLocalVariable();
            lineNumber = throwStatement.getExpression().getLineNumber();
            offset = varRef.getOffset();
            safe = true;
            matchCount = 0;
            scanStatements(loopBody, false, true);

            if (safe && matchCount > 0) {
                scanStatements(loopBody, false, false);
                list.remove(i + 1);
            }
        }

        acceptListStatement(list);
    }

    private static boolean isLoop(Statement statement) {
        return statement instanceof WhileStatement || statement instanceof DoWhileStatement
            || statement instanceof ForStatement || statement instanceof ForEachStatement;
    }

    private static BaseStatement getLoopBody(Statement loop) {
        return loop.getStatements();
    }

    private void scanStatements(BaseStatement statement, boolean insideMatchingCatch, boolean dryRun) {
        if (!(statement instanceof Statements list)) {
            return;
        }

        for (int i = 0; i < list.size(); i++) {
            Statement s = list.get(i);

            if (s instanceof BreakStatement breakStatement) {
                if (breakStatement.getLabel() != null) {
                    continue;
                }
                if (insideMatchingCatch) {
                    matchCount++;
                    if (!dryRun) {
                        list.set(i, new ThrowStatement(new ClassFileLocalVariableReferenceExpression(lineNumber, offset, target)));
                    }
                } else {
                    safe = false;
                }
            } else {
                scanStatement(s, insideMatchingCatch, dryRun);
            }
        }
    }

    private void scanStatement(Statement s, boolean insideMatchingCatch, boolean dryRun) {
        if (s instanceof IfStatement ifStatement) {
            scanStatements(ifStatement.getStatements(), insideMatchingCatch, dryRun);
        } else if (s instanceof IfElseStatement ifElseStatement) {
            scanStatements(ifElseStatement.getStatements(), insideMatchingCatch, dryRun);
            scanStatements(ifElseStatement.getElseStatements(), insideMatchingCatch, dryRun);
        } else if (s instanceof TryStatement tryStatement) {
            scanStatements(tryStatement.getTryStatements(), insideMatchingCatch, dryRun);

            if (tryStatement.getCatchClauses() != null) {
                for (TryStatement.CatchClause catchClause : tryStatement.getCatchClauses()) {
                    boolean matches = insideMatchingCatch || isMatchingCatch(catchClause);
                    scanStatements(catchClause.getStatements(), matches, dryRun);
                }
            }

            BaseStatement finallyStatements = tryStatement.getFinallyStatements();

            if (finallyStatements instanceof Statements) {
                scanStatements(finallyStatements, insideMatchingCatch, dryRun);
            }
        } else if (s instanceof SynchronizedStatement synchronizedStatement) {
            scanStatements(synchronizedStatement.getStatements(), insideMatchingCatch, dryRun);
        } else if (s instanceof LabelStatement labelStatement) {
            if (labelStatement.statement() instanceof Statement inner) {
                scanStatement(inner, insideMatchingCatch, dryRun);
            }
        }
        // Nested loops and switches are opaque: their unlabeled 'break's target the nested
        // construct, not the loop being fixed here, so they are intentionally not visited.
    }

    private boolean isMatchingCatch(TryStatement.CatchClause catchClause) {
        return catchClause instanceof ClassFileTryStatement.CatchClause classFileCatchClause
            && classFileCatchClause.getLocalVariable() == target;
    }
}
