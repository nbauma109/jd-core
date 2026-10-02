/*
 * Copyright (c) 2008, 2019 Emmanuel Dupuy.
 * This project is distributed under the GPLv3 license.
 * This is a Copyleft license that gives the user the right to use,
 * copy and modify the code freely for non-commercial purposes.
 */
package org.jd.core.v1.service.converter.classfiletojavasyntax.util;

import org.jd.core.v1.model.javasyntax.AbstractJavaSyntaxVisitor;
import org.jd.core.v1.model.javasyntax.expression.BinaryOperatorExpression;
import org.jd.core.v1.model.javasyntax.expression.BooleanExpression;
import org.jd.core.v1.model.javasyntax.expression.Expression;
import org.jd.core.v1.model.javasyntax.expression.TernaryOperatorExpression;
import org.jd.core.v1.model.javasyntax.statement.BaseStatement;
import org.jd.core.v1.model.javasyntax.statement.IfElseStatement;
import org.jd.core.v1.model.javasyntax.statement.IfStatement;
import org.jd.core.v1.model.javasyntax.statement.ReturnExpressionStatement;
import org.jd.core.v1.model.javasyntax.statement.Statement;
import org.jd.core.v1.model.javasyntax.statement.Statements;
import org.jd.core.v1.service.converter.classfiletojavasyntax.visitor.SearchFirstLineNumberVisitor;

import java.util.ArrayList;
import java.util.List;

import static org.jd.core.v1.api.printer.Printer.UNKNOWN_LINE_NUMBER;
import static org.jd.core.v1.model.javasyntax.type.PrimitiveType.TYPE_BOOLEAN;

/**
 * The tests of 'return a &amp;&amp; b || c ? d : e;' are compiled into jumps to a few 'return true' and 'return false' (e.g. by ECJ), and
 * decompiled into a tree of 'if' which ends with these returns. The compiler gives all of them the line of the statement, which is the
 * line of the first test: a tree of 'if' whose returns are all on the line of its first test is one 'return' statement.
 * <p>Statements which are on their own lines ('if (a) return true;' ... 'return false;') are not touched.</p>
 */
public final class BooleanReturnRewriter extends AbstractJavaSyntaxVisitor {
    private static final int AND_PRIORITY = 13;
    private static final int OR_PRIORITY = 14;

    private final SearchFirstLineNumberVisitor searchFirstLineNumberVisitor = new SearchFirstLineNumberVisitor();

    /** A tree of tests. The leaves are the values returned, or where the control leaves the list of statements without returning. */
    private sealed interface Tree permits Value, Fall, Test, Operation, Leaf {
    }

    private record Value(boolean value) implements Tree {
    }

    private record Fall() implements Tree {
    }

    private record Test(Expression condition, Tree whenTrue, Tree whenFalse) implements Tree {
    }

    private record Operation(String operator, Tree left, Tree right) implements Tree {
    }

    private record Leaf(Expression expression) implements Tree {
    }

    private static final Fall FALL = new Fall();
    private static final Value TRUE = new Value(true);
    private static final Value FALSE = new Value(false);

    @Override
    public void visit(Statements statements) {
        super.visit(statements);
        rewrite(statements);
    }

    private void rewrite(Statements statements) {
        for (int i = 0, size = statements.size(); i < size; i++) {
            if (statements.get(i).isIfStatement()) {
                List<ReturnExpressionStatement> returns = new ArrayList<>();
                Tree tree = build(statements, i, returns);

                if (tree != null && hasOneLine(statements.get(i), returns)) {
                    Expression expression = toExpression(tree);

                    if (expression != null) {
                        statements.subList(i, size).clear();
                        statements.add(new ReturnExpressionStatement(returns.get(0).getLineNumber(), expression));
                        return;
                    }
                }
            }
        }
    }

    /** @return true if all the returns are on the line of the first test: they come from one statement */
    private boolean hasOneLine(Statement ifStatement, List<ReturnExpressionStatement> returns) {
        if (returns.size() < 2) {
            return false;
        }

        searchFirstLineNumberVisitor.init();
        ((IfStatement)ifStatement).getCondition().accept(searchFirstLineNumberVisitor);

        int lineNumber = searchFirstLineNumberVisitor.getLineNumber();

        if (lineNumber == UNKNOWN_LINE_NUMBER) {
            return false;
        }

        for (ReturnExpressionStatement returnStatement : returns) {
            if (returnStatement.getLineNumber() != lineNumber) {
                return false;
            }
        }
        return true;
    }

    /** @return the tree of the statements from 'index', or null if one of them is not a test or a return of 'true' or 'false' */
    private Tree build(List<Statement> statements, int index, List<ReturnExpressionStatement> returns) {
        if (index >= statements.size()) {
            return FALL;
        }

        Statement statement = statements.get(index);

        if (statement.isReturnExpressionStatement()) {
            Expression expression = RecordPatternInstanceOfRewriter.unwrapParenthesesExpression(statement.getExpression());

            if (RecordPatternInstanceOfRewriter.isTrueExpression(expression) || RecordPatternInstanceOfRewriter.isFalseExpression(expression)) {
                returns.add((ReturnExpressionStatement)statement);
                return RecordPatternInstanceOfRewriter.isTrueExpression(expression) ? TRUE : FALSE;
            }
            return null;
        }

        if (!statement.isIfStatement()) {
            return null;
        }

        IfStatement ifStatement = (IfStatement)statement;
        Tree whenTrue = buildBody(ifStatement.getStatements(), returns);
        Tree whenFalse = ifStatement.isIfElseStatement() ? buildBody(((IfElseStatement)ifStatement).getElseStatements(), returns) : FALL;
        Tree next = build(statements, index + 1, returns);

        if (whenTrue == null || whenFalse == null || next == null) {
            return null;
        }

        return resolve(new Test(ifStatement.getCondition(), whenTrue, whenFalse), next);
    }

    private Tree buildBody(BaseStatement body, List<ReturnExpressionStatement> returns) {
        if (body instanceof Statements statements) {
            return build(statements, 0, returns);
        }
        return build(List.of((Statement)body), 0, returns);
    }

    /**
     * Replaces the leaves where the control leaves a test by what follows it.
     *
     * @return null if what follows would be duplicated
     */
    private static Tree resolve(Tree tree, Tree next) {
        int falls = countFalls(tree);

        if (falls == 0) {
            return tree;
        }
        if (falls == 1 || next instanceof Value || next instanceof Fall) {
            return replaceFalls(tree, next);
        }
        // All the other leaves are 'false': a conjunction. All the other leaves are 'true': a disjunction.
        if (isOnlyMadeOf(tree, FALSE)) {
            return new Operation("&&", replaceFalls(tree, TRUE), next);
        }
        if (isOnlyMadeOf(tree, TRUE)) {
            return new Operation("||", replaceFalls(tree, FALSE), next);
        }
        return null;
    }

    private static int countFalls(Tree tree) {
        if (tree instanceof Fall) {
            return 1;
        }
        if (tree instanceof Test test) {
            return countFalls(test.whenTrue()) + countFalls(test.whenFalse());
        }
        if (tree instanceof Operation operation) {
            return countFalls(operation.left()) + countFalls(operation.right());
        }
        return 0;
    }

    private static boolean isOnlyMadeOf(Tree tree, Value value) {
        if (tree instanceof Fall) {
            return true;
        }
        if (tree instanceof Test test) {
            return isOnlyMadeOf(test.whenTrue(), value) && isOnlyMadeOf(test.whenFalse(), value);
        }
        return tree == value;
    }

    private static Tree replaceFalls(Tree tree, Tree replacement) {
        if (tree instanceof Fall) {
            return replacement;
        }
        if (tree instanceof Test test) {
            return new Test(test.condition(), replaceFalls(test.whenTrue(), replacement), replaceFalls(test.whenFalse(), replacement));
        }
        if (tree instanceof Operation operation) {
            return new Operation(operation.operator(), replaceFalls(operation.left(), replacement), replaceFalls(operation.right(), replacement));
        }
        return tree;
    }

    /** @return null if the control may leave the statements without returning */
    private static Expression toExpression(Tree tree) {
        Tree simple = simplify(tree);

        return containsFall(simple) ? null : expression(simple);
    }

    private static boolean containsFall(Tree tree) {
        return countFalls(tree) > 0;
    }

    private static Tree simplify(Tree tree) {
        if (tree instanceof Test test) {
            return simplifyTest(test.condition(), simplify(test.whenTrue()), simplify(test.whenFalse()));
        }
        if (tree instanceof Operation operation) {
            return new Operation(operation.operator(), simplify(operation.left()), simplify(operation.right()));
        }
        return tree;
    }

    private static Tree simplifyTest(Expression test, Tree whenTrue, Tree whenFalse) {
        Leaf condition = new Leaf(test);
        Leaf negation = new Leaf(RecordPatternInstanceOfRewriter.negateBooleanExpression(test, test.getLineNumber()));

        if (whenTrue == TRUE && whenFalse == FALSE) {
            return condition;
        }
        if (whenTrue == FALSE && whenFalse == TRUE) {
            return negation;
        }
        if (whenFalse == FALSE) {
            return new Operation("&&", condition, whenTrue);
        }
        if (whenFalse == TRUE) {
            return new Operation("||", negation, whenTrue);
        }
        if (whenTrue == TRUE) {
            return new Operation("||", condition, whenFalse);
        }
        if (whenTrue == FALSE) {
            return new Operation("&&", negation, whenFalse);
        }
        return new Test(test, whenTrue, whenFalse);
    }

    private static Expression expression(Tree tree) {
        if (tree instanceof Value value) {
            return new BooleanExpression(UNKNOWN_LINE_NUMBER, value.value());
        }
        if (tree instanceof Leaf leaf) {
            return leaf.expression();
        }
        if (tree instanceof Operation operation) {
            Expression left = expression(operation.left());
            boolean and = "&&".equals(operation.operator());

            return new BinaryOperatorExpression(left.getLineNumber(), TYPE_BOOLEAN, left, operation.operator(), expression(operation.right()), and ? AND_PRIORITY : OR_PRIORITY);
        }

        Test test = (Test)tree;
        Expression condition = test.condition();

        return new TernaryOperatorExpression(condition.getLineNumber(), TYPE_BOOLEAN, condition, expression(test.whenTrue()), expression(test.whenFalse()));
    }
}
