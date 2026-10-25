/*
 * Copyright (c) 2008, 2019 Emmanuel Dupuy.
 * This project is distributed under the GPLv3 license.
 * This is a Copyleft license that gives the user the right to use,
 * copy and modify the code freely for non-commercial purposes.
 */

package org.jd.core.v1.service.converter.classfiletojavasyntax.util;

import org.jd.core.v1.model.javasyntax.AbstractJavaSyntaxVisitor;
import org.jd.core.v1.model.javasyntax.expression.ArrayExpression;
import org.jd.core.v1.model.javasyntax.expression.BaseExpression;
import org.jd.core.v1.model.javasyntax.expression.BooleanExpression;
import org.jd.core.v1.model.javasyntax.expression.CastExpression;
import org.jd.core.v1.model.javasyntax.expression.Expression;
import org.jd.core.v1.model.javasyntax.expression.Expressions;
import org.jd.core.v1.model.javasyntax.expression.FieldReferenceExpression;
import org.jd.core.v1.model.javasyntax.expression.MethodInvocationExpression;
import org.jd.core.v1.model.javasyntax.expression.PostOperatorExpression;
import org.jd.core.v1.model.javasyntax.statement.BaseStatement;
import org.jd.core.v1.model.javasyntax.statement.BreakStatement;
import org.jd.core.v1.model.javasyntax.statement.ContinueStatement;
import org.jd.core.v1.model.javasyntax.statement.DoWhileStatement;
import org.jd.core.v1.model.javasyntax.statement.ForEachStatement;
import org.jd.core.v1.model.javasyntax.statement.ForStatement;
import org.jd.core.v1.model.javasyntax.statement.IfStatement;
import org.jd.core.v1.model.javasyntax.statement.LabelStatement;
import org.jd.core.v1.model.javasyntax.statement.Statement;
import org.jd.core.v1.model.javasyntax.statement.Statements;
import org.jd.core.v1.model.javasyntax.statement.WhileStatement;
import org.jd.core.v1.model.javasyntax.type.BaseType;
import org.jd.core.v1.model.javasyntax.type.GenericType;
import org.jd.core.v1.model.javasyntax.type.ObjectType;
import org.jd.core.v1.model.javasyntax.type.PrimitiveType;
import org.jd.core.v1.model.javasyntax.type.Type;
import org.jd.core.v1.model.javasyntax.type.BaseTypeArgument;
import org.jd.core.v1.model.javasyntax.type.TypeArguments;
import org.jd.core.v1.model.javasyntax.type.WildcardExtendsTypeArgument;
import org.jd.core.v1.model.javasyntax.type.WildcardSuperTypeArgument;
import org.jd.core.v1.model.javasyntax.type.WildcardTypeArgument;
import org.jd.core.v1.service.converter.classfiletojavasyntax.model.cfg.BasicBlock;
import org.jd.core.v1.service.converter.classfiletojavasyntax.model.javasyntax.expression.ClassFileLocalVariableReferenceExpression;
import org.jd.core.v1.service.converter.classfiletojavasyntax.model.javasyntax.expression.ClassFileNewExpression;
import org.jd.core.v1.service.converter.classfiletojavasyntax.model.javasyntax.statement.ClassFileBreakContinueStatement;
import org.jd.core.v1.service.converter.classfiletojavasyntax.model.javasyntax.statement.ClassFileContinueStatement;
import org.jd.core.v1.service.converter.classfiletojavasyntax.model.javasyntax.statement.ClassFileForEachStatement;
import org.jd.core.v1.service.converter.classfiletojavasyntax.model.javasyntax.statement.ClassFileForStatement;
import org.jd.core.v1.service.converter.classfiletojavasyntax.model.javasyntax.statement.ClassFileIfStatement;
import org.jd.core.v1.service.converter.classfiletojavasyntax.model.localvariable.AbstractLocalVariable;
import org.jd.core.v1.service.converter.classfiletojavasyntax.model.localvariable.GenericLocalVariable;
import org.jd.core.v1.service.converter.classfiletojavasyntax.model.localvariable.ObjectLocalVariable;
import org.jd.core.v1.service.converter.classfiletojavasyntax.visitor.ChangeFrameOfLocalVariablesVisitor;
import org.jd.core.v1.service.converter.classfiletojavasyntax.visitor.CreateTypeFromTypeArgumentVisitor;
import org.jd.core.v1.service.converter.classfiletojavasyntax.visitor.RemoveLastContinueStatementVisitor;
import org.jd.core.v1.service.converter.classfiletojavasyntax.visitor.SearchFirstLineNumberVisitor;
import org.jd.core.v1.service.converter.classfiletojavasyntax.visitor.SearchFromOffsetVisitor;
import org.jd.core.v1.service.converter.classfiletojavasyntax.visitor.SearchLocalVariableReferenceVisitor;
import org.jd.core.v1.util.StringConstants;

import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.ListIterator;
import java.util.Map;
import java.util.Set;

import static org.apache.bcel.Const.MAJOR_1_5;
import static org.jd.core.v1.service.converter.classfiletojavasyntax.model.cfg.BasicBlock.GROUP_END;
import static org.jd.core.v1.model.javasyntax.statement.ContinueStatement.CONTINUE;
import static org.jd.core.v1.model.javasyntax.type.ObjectType.TYPE_ITERABLE;
import static org.jd.core.v1.model.javasyntax.type.ObjectType.TYPE_OBJECT;

public final class LoopStatementMaker {

    private LoopStatementMaker() {
    }

    private static final RemoveLastContinueStatementVisitor REMOVE_LAST_CONTINUE_STATEMENT_VISITOR = new RemoveLastContinueStatementVisitor();

    public static Statement makeLoop(
            int majorVersion, Map<String, BaseType> typeBounds, LocalVariableMaker localVariableMaker,
            BasicBlock loopBasicBlock, Statements statements, Expression condition, Statements subStatements,
            Statements jumps) {
        boolean[] labelRequested = new boolean[1];
        Statement loop = makeLoop(new LoopContext(majorVersion, typeBounds, localVariableMaker), loopBasicBlock, statements, condition, subStatements, labelRequested);
        int continueOffset = loopBasicBlock.getSub1().getFromOffset();
        preserveNestedContinueTargets(subStatements, continueOffset);
        int breakOffset = loopBasicBlock.getNext().getFromOffset();

        if (breakOffset <= 0) {
            breakOffset = loopBasicBlock.getToOffset();
        }

        int updateOffset = loop instanceof ClassFileForEachStatement forEach ? forEach.getUpdateOffset() : -1;

        return makeLabels(loopBasicBlock.getIndex(), new LoopOffsets(continueOffset, breakOffset, updateOffset, lowestOffset(loopBasicBlock, continueOffset)), labelRequested[0], loop, jumps);
    }

    private record LoopContext(int majorVersion, Map<String, BaseType> typeBounds, LocalVariableMaker localVariableMaker) {
    }

    private static Statement makeLoop(
            LoopContext context, BasicBlock loopBasicBlock, Statements statements, Expression condition, Statements subStatements,
            boolean[] labelRequested) {
        int majorVersion = context.majorVersion();
        Map<String, BaseType> typeBounds = context.typeBounds();
        LocalVariableMaker localVariableMaker = context.localVariableMaker();
        boolean forEachSupported = majorVersion >= MAJOR_1_5;

        subStatements.accept(REMOVE_LAST_CONTINUE_STATEMENT_VISITOR);

        if (forEachSupported) {
            Statement statement = makeForEachArray(typeBounds, localVariableMaker, statements, condition, subStatements, labelOf(loopBasicBlock.getIndex()), labelRequested);

            if (statement != null) {
                restoreProvenForEachBreaks(statement.getStatements());
                return statement;
            }

            statement = makeForEachList(typeBounds, localVariableMaker, statements, condition, subStatements);

            if (statement != null) {
                restoreProvenForEachBreaks(statement.getStatements());
                return statement;
            }
        }

        Statement forStatement = makeForWithUpdateBeforeContinues(localVariableMaker, loopBasicBlock, statements, condition, subStatements, labelRequested);

        if (forStatement != null) {
            return forStatement;
        }

        int lineNumber = condition == null ? Expression.UNKNOWN_LINE_NUMBER : condition.getLineNumber();
        int subStatementsSize = subStatements.size();

        switch (subStatementsSize) {
            case 0:
                if (lineNumber > 0) {
                    // Known line numbers
                    BaseExpression init = extractInit(statements, lineNumber);

                    if (init != null) {
                        return newClassFileForStatement(localVariableMaker, loopBasicBlock.getFromOffset(), loopBasicBlock.getToOffset(), init, condition, null, null);
                    }
                }
                break;
            case 1:
                if (lineNumber > 0) {
                    // Known line numbers
                    Statement subStatement = subStatements.getFirst();
                    BaseExpression init = extractInit(statements, lineNumber);

                    if (subStatement.isExpressionStatement()) {
                        Expression subExpression = subStatement.getExpression();

                        if (subExpression.getLineNumber() == lineNumber) {
                            return newClassFileForStatement(
                                localVariableMaker, loopBasicBlock.getFromOffset(), loopBasicBlock.getToOffset(), init, condition, subExpression, null);
                        }
                        if (init != null) {
                            return newClassFileForStatement(
                                localVariableMaker, loopBasicBlock.getFromOffset(), loopBasicBlock.getToOffset(), init, condition, null, subStatement);
                        }
                    } else if (init != null) {
                        return newClassFileForStatement(
                            localVariableMaker, loopBasicBlock.getFromOffset(), loopBasicBlock.getToOffset(), init, condition, null, subStatement);
                    }
                } else {
                    // Unknown line numbers => Just try to find 'for (expression;;expression)'
                    return createForStatementWithoutLineNumber(localVariableMaker, loopBasicBlock, statements, condition, subStatements);
                }
                break;
            default:
                if (lineNumber <= 0) {
                    // Unknown line numbers => Just try to find 'for (expression;;expression)'
                    return createForStatementWithoutLineNumber(localVariableMaker, loopBasicBlock, statements, condition, subStatements);
                }
                // Known line numbers
                SearchFirstLineNumberVisitor visitor = new SearchFirstLineNumberVisitor();

                subStatements.getFirst().accept(visitor);

                int firstLineNumber = visitor.getLineNumber();

                // Populates 'update'
                // A 'continue' of a 'while' jumps to its condition, not to an update: the line of the header is no evidence that a trailing
                // expression on this line is an update when the body has some
                Expressions update = extractUpdate(subStatements, firstLineNumber, containsContinue(subStatements) ? Expression.UNKNOWN_LINE_NUMBER : lineNumber);

                if (update.isEmpty()) {
                    // A body which is entirely on the line of the header (its test was merged into the condition) is the
                    // update of the 'for'
                    update = extractUpdateOnLine(subStatements, lineNumber);
                }

                BaseExpression init = extractInit(statements, lineNumber);

                if (init != null || !update.isEmpty()) {
                    return newClassFileForStatement(
                        localVariableMaker, loopBasicBlock.getFromOffset(), loopBasicBlock.getToOffset(), init, condition, update, subStatements);
                }
                break;
        }

        return new WhileStatement(condition, subStatements);
    }

    /** @return the offset of the 'iinc' instruction of an update 'i$++' (the variable is the offset of its first operand), or -1 */
    private static int offsetOfUpdate(Expression update) {
        return update.getExpression() instanceof ClassFileLocalVariableReferenceExpression variable ? variable.getOffset() - 1 : -1;
    }

    /** @return true if the update is 'i++' of the variable */
    private static boolean isUpdateOf(Expression update, AbstractLocalVariable index) {
        return update.isPostOperatorExpression() && "++".equals(update.getOperator())
            && update.getExpression() instanceof ClassFileLocalVariableReferenceExpression variable
            && variable.getLocalVariable() == index;
    }

    /**
     * The body of a loop which never falls through (e.g. it ends with a 'return') has no update at its end: the compiler
     * only copied the update 'i++' before each 'continue' of the loop, including the ones of its nested loops
     * ('continue outer'). The update is the one of the 'for', and the 'continue' of the nested loops are labeled.
     *
     * @return null if the loop is not such a loop
     */
    private static Statement makeForWithUpdateBeforeContinues(
            LocalVariableMaker localVariableMaker, BasicBlock loopBasicBlock, Statements statements, Expression condition,
            Statements subStatements, boolean[] labelRequested) {
        int lineNumber = condition == null ? Expression.UNKNOWN_LINE_NUMBER : condition.getLineNumber();

        if (lineNumber <= 0 || subStatements.isEmpty() || !neverFallsThrough(subStatements.getLast())) {
            return null;
        }

        ContinueUpdatesVisitor visitor = new ContinueUpdatesVisitor(condition, labelOf(loopBasicBlock.getIndex()));

        subStatements.accept(visitor);

        if (!visitor.isValid()) {
            return null;
        }

        visitor.apply = true;
        subStatements.accept(visitor);
        labelRequested[0] = visitor.labelUsed;

        BaseExpression init = extractInit(statements, lineNumber);
        Expressions update = new Expressions();

        update.add(visitor.update);
        return newClassFileForStatement(localVariableMaker, loopBasicBlock.getFromOffset(), loopBasicBlock.getToOffset(), init, condition, update, subStatements);
    }

    private static boolean neverFallsThrough(Statement statement) {
        return statement.isReturnStatement() || statement.isReturnExpressionStatement() || statement.isThrowStatement() || statement.isBreakStatement();
    }

    /**
     * Searches the 'continue' of a loop which follow the same update of a variable of its condition ('i++'), at any depth. The
     * 'continue' of a nested loop which do not follow it are the ones of this nested loop.
     */
    private static final class ContinueUpdatesVisitor extends AbstractJavaSyntaxVisitor {
        private final SearchLocalVariableReferenceVisitor searchVariable = new SearchLocalVariableReferenceVisitor();
        private final Expression condition;
        private final String label;
        private Expression update;
        private int depth;
        private int continues;
        private boolean valid = true;
        private boolean apply;
        private boolean labelUsed;

        private ContinueUpdatesVisitor(Expression condition, String label) {
            this.condition = condition;
            this.label = label;
        }

        private boolean isValid() {
            return valid && continues > 0;
        }

        @Override
        public void visit(Statements statements) {
            int i = 0;

            while (i < statements.size()) {
                Statement statement = statements.get(i);
                int next = i + 1;

                if (statement.isContinueStatement() && ((ContinueStatement)statement).getLabel() == null) {
                    if (i > 0 && isUpdate(statements.get(i - 1))) {
                        if (apply) {
                            removeUpdateBefore(statements, i);
                            // The 'continue' is now before the index
                            next = i;
                        } else {
                            continues++;
                        }
                    } else if (depth == 0) {
                        valid = false;
                    }
                } else {
                    statement.accept(this);
                }
                i = next;
            }
        }

        private void removeUpdateBefore(Statements statements, int continueIndex) {
            statements.remove(continueIndex - 1);
            if (depth > 0) {
                // The 'continue' of a nested loop is the one of this loop
                statements.set(continueIndex - 1, new ContinueStatement(label));
                labelUsed = true;
            }
        }

        @Override
        public void visit(ContinueStatement statement) {
            // A 'continue' which is not in a list of statements cannot follow the update
            if (depth == 0 && statement.getLabel() == null) {
                valid = false;
            }
        }

        private boolean isUpdate(Statement statement) {
            if (!statement.isExpressionStatement()) {
                return false;
            }

            Expression expression = statement.getExpression();
            Expression variable;

            if (expression.isPostOperatorExpression()) {
                variable = expression.getExpression();
            } else if (expression.isBinaryOperatorExpression() && expression.getOperator().length() == 2 && expression.getOperator().endsWith("=")
                    && "+-*/".contains(expression.getOperator().substring(0, 1)) && expression.getRightExpression().isIntegerConstantExpression()) {
                variable = expression.getLeftExpression();
            } else {
                return false;
            }

            if (!variable.isLocalVariableReferenceExpression()) {
                return false;
            }

            if (update == null) {
                AbstractLocalVariable localVariable = ((ClassFileLocalVariableReferenceExpression)variable).getLocalVariable();

                searchVariable.init(localVariable);
                condition.accept(searchVariable);

                // The update of a 'for' is on the line of its condition: another line is the one of a statement of the body
                if (!searchVariable.containsReference() || expression.getLineNumber() != condition.getLineNumber()) {
                    return false;
                }
                update = expression;
                return true;
            }

            return sameUpdate(update, expression);
        }

        private static boolean sameUpdate(Expression expression1, Expression expression2) {
            if (expression1.isPostOperatorExpression()) {
                return expression2.isPostOperatorExpression()
                    && expression1.getOperator().equals(expression2.getOperator())
                    && sameVariable(expression1.getExpression(), expression2.getExpression());
            }

            return expression2.isBinaryOperatorExpression()
                && expression1.getOperator().equals(expression2.getOperator())
                && sameVariable(expression1.getLeftExpression(), expression2.getLeftExpression())
                && expression1.getRightExpression().isIntegerConstantExpression() && expression2.getRightExpression().isIntegerConstantExpression()
                && expression1.getRightExpression().getIntegerValue() == expression2.getRightExpression().getIntegerValue();
        }

        private static boolean sameVariable(Expression expression1, Expression expression2) {
            return expression2.isLocalVariableReferenceExpression()
                && ((ClassFileLocalVariableReferenceExpression)expression1).getLocalVariable() == ((ClassFileLocalVariableReferenceExpression)expression2).getLocalVariable();
        }

        @Override
        public void visit(DoWhileStatement statement) {
            depth++;
            safeAccept(statement.getStatements());
            depth--;
        }

        @Override
        public void visit(ForEachStatement statement) {
            depth++;
            safeAccept(statement.getStatements());
            depth--;
        }

        @Override
        public void visit(ForStatement statement) {
            depth++;
            safeAccept(statement.getStatements());
            depth--;
        }

        @Override
        public void visit(WhileStatement statement) {
            depth++;
            safeAccept(statement.getStatements());
            depth--;
        }
    }

    static void restoreProvenForEachBreaks(BaseStatement statement) {
        statement.accept(new RestoreProvenForEachBreaksVisitor());
    }

    private static final class RestoreProvenForEachBreaksVisitor extends AbstractJavaSyntaxVisitor {
        @Override
        public void visit(IfStatement statement) {
            if (statement instanceof ClassFileIfStatement
                    && statement.getStatements() instanceof Statements thenStatements) {
                thenStatements.add(BreakStatement.BREAK);
            }
            safeAccept(statement.getStatements());
        }

        @Override
        public void visit(DoWhileStatement statement) {
            // A nested loop owns its proven exits.
        }

        @Override
        public void visit(ForEachStatement statement) {
            // A nested loop owns its proven exits.
        }

        @Override
        public void visit(ForStatement statement) {
            // A nested loop owns its proven exits.
        }

        @Override
        public void visit(WhileStatement statement) {
            // A nested loop owns its proven exits.
        }
    }

    public static Statement makeLoop(LocalVariableMaker localVariableMaker, BasicBlock loopBasicBlock, Statements statements, Statements subStatements, Statements jumps) {
        subStatements.accept(REMOVE_LAST_CONTINUE_STATEMENT_VISITOR);

        Statement loop = makeLoop(localVariableMaker, loopBasicBlock, statements, subStatements);
        int continueOffset = loopBasicBlock.getSub1().getFromOffset();
        preserveNestedContinueTargets(subStatements, continueOffset);
        int breakOffset = loopBasicBlock.getNext().getFromOffset();

        if (breakOffset <= 0) {
            breakOffset = loopBasicBlock.getToOffset();
        }

        return makeLabels(loopBasicBlock.getIndex(), new LoopOffsets(continueOffset, breakOffset, -1, lowestOffset(loopBasicBlock, continueOffset)), false, loop, jumps);
    }

    private static Statement makeLoop(LocalVariableMaker localVariableMaker, BasicBlock loopBasicBlock, Statements statements, Statements subStatements) {
        int subStatementsSize = subStatements.size();

        if (subStatementsSize > 0 && subStatements.getLast() == CONTINUE) {
            subStatements.removeLast();
            subStatementsSize--;
        }

        switch (subStatementsSize) {
            case 0:
                break;
            case 1:
                Statement subStatement = subStatements.getFirst();

                if (subStatement.isExpressionStatement()) {
                    Expression subExpression = subStatement.getExpression();
                    int lineNumber = subExpression.getLineNumber();

                    if (lineNumber > 0) {
                        // Known line numbers
                        BaseExpression init = extractInit(statements, lineNumber);

                        if (init != null) {
                            return newClassFileForStatement(localVariableMaker, loopBasicBlock.getFromOffset(), loopBasicBlock.getToOffset(), init, null, subExpression, null);
                        }
                    }
                }
                break;
            default:
                SearchFirstLineNumberVisitor visitor = new SearchFirstLineNumberVisitor();

                subStatements.get(0).accept(visitor);

                int firstLineNumber = visitor.getLineNumber();

                if (firstLineNumber <= 0) {
                    // Unknown line numbers => Just try to find 'for (expression;;expression)'
                    return createForStatementWithoutLineNumber(localVariableMaker, loopBasicBlock, statements, BooleanExpression.TRUE, subStatements);
                }
            // Populates 'update'
            Expressions update = extractUpdate(subStatements, firstLineNumber, Expression.UNKNOWN_LINE_NUMBER);

            if (!update.isEmpty()) {
                // Populates 'init'
                BaseExpression init = extractInit(statements, update.getFirst().getLineNumber());

                return newClassFileForStatement(localVariableMaker, loopBasicBlock.getFromOffset(), loopBasicBlock.getToOffset(), init, null, update, subStatements);
            }
        }

        return new WhileStatement(infiniteLoopCondition(loopBasicBlock, subStatements), subStatements);
    }

    /**
     * ECJ attributes the back jump of a loop to the line where the loop starts ('do', 'while (true)', 'for (;;)'),
     * which is before the first statement of its body. Keep that line on the loop condition, so that the loop
     * header can be aligned on it instead of being stacked right before the first statement of the body.
     */
    private static Expression infiniteLoopCondition(BasicBlock loopBasicBlock, Statements subStatements) {
        int headerLineNumber = loopBasicBlock.getLastLineNumber();

        if (headerLineNumber > 0) {
            SearchFirstLineNumberVisitor visitor = new SearchFirstLineNumberVisitor();

            subStatements.accept(visitor);

            int firstLineNumber = visitor.getLineNumber();

            if (firstLineNumber > headerLineNumber) {
                return new BooleanExpression(headerLineNumber, true);
            }
        }
        return BooleanExpression.TRUE;
    }

    public static Statement makeDoWhileLoop(BasicBlock loopBasicBlock, Expression condition, Statements subStatements, Statements jumps) {
        subStatements.accept(REMOVE_LAST_CONTINUE_STATEMENT_VISITOR);

        Statement loop = new DoWhileStatement(condition, subStatements);
        int continueOffset = loopBasicBlock.getSub1().getFromOffset();
        int breakOffset = loopBasicBlock.getNext().getFromOffset();

        if (breakOffset <= 0) {
            breakOffset = loopBasicBlock.getToOffset();
        }

        return makeLabels(loopBasicBlock.getIndex(), new LoopOffsets(continueOffset, breakOffset, -1, lowestOffset(loopBasicBlock, continueOffset)), false, loop, jumps);
    }

    private static BaseExpression extractInit(Statements statements, int lineNumber) {
        if (lineNumber > 0) {
            switch (statements.size()) {
                case 0:
                    break;
                case 1:
                    Statement statement = statements.getFirst();

                    if (!statement.isExpressionStatement()) {
                        break;
                    }

                    Expression expression = statement.getExpression();

                    if (expression.getLineNumber() != lineNumber ||
                        !expression.isBinaryOperatorExpression() ||
                        expression.getRightExpression().getLineNumber() == 0) {
                        break;
                    }

                    statements.clear();
                    return expression;
                default:
                    Expressions init = new Expressions();
                    ListIterator<Statement> iterator = statements.listIterator(statements.size());

                    while (iterator.hasPrevious()) {
                        statement = iterator.previous();

                        if (!statement.isExpressionStatement()) {
                            break;
                        }

                        expression = statement.getExpression();

                        if (expression.getLineNumber() != lineNumber ||
                            !expression.isBinaryOperatorExpression() ||
                            expression.getRightExpression().getLineNumber() == 0) {
                            break;
                        }

                        init.add(expression);
                        iterator.remove();
                    }

                    if (!init.isEmpty()) {
                        if (init.size() > 1) {
                            Collections.reverse(init);
                        }
                        return init;
                    }
                    break;
            }
        }

        return null;
    }

    private static Expressions extractUpdateOnLine(Statements statements, int lineNumber) {
        for (Statement statement : statements) {
            if (!statement.isExpressionStatement() || statement.getExpression().getLineNumber() != lineNumber) {
                return new Expressions();
            }
        }

        Expressions update = new Expressions();

        statements.stream().map(Statement::getExpression).forEach(update::add);
        statements.clear();
        return update;
    }

    /**
     * @param headerLineNumber the line of the condition of the loop: the updates which are on this line are the ones of the 'for', even if
     *                         the first statement of the body is on this line too
     */
    private static boolean containsContinue(Statements statements) {
        boolean[] found = new boolean[1];

        statements.accept(new AbstractJavaSyntaxVisitor() {
            @Override
            public void visit(ContinueStatement statement) {
                found[0] = true;
            }
        });
        return found[0];
    }

    private static Expressions extractUpdate(Statements statements, int firstLineNumber, int headerLineNumber) {
        Expressions update = new Expressions();
        ListIterator<Statement> iterator = statements.listIterator(statements.size());

        // Populates 'update'
        while (iterator.hasPrevious()) {
            Statement statement = iterator.previous();
            if (!statement.isExpressionStatement()) {
                break;
            }
            Expression expression = statement.getExpression();
            int lineNumber = expression.getLineNumber();

            if (lineNumber >= firstLineNumber && (headerLineNumber <= 0 || lineNumber != headerLineNumber)) {
                break;
            }
            iterator.remove();
            update.add(expression);
        }

        if (update.size() > 1) {
            Collections.reverse(update);
        }

        return update;
    }

    private static Statement createForStatementWithoutLineNumber(
            LocalVariableMaker localVariableMaker, BasicBlock basicBlock, Statements statements, Expression condition, Statements subStatements) {

        if (!statements.isEmpty()) {
            Expression init = statements.getLast().getExpression();

            if (init.getLeftExpression().isLocalVariableReferenceExpression()) {
                AbstractLocalVariable localVariable = ((ClassFileLocalVariableReferenceExpression) init.getLeftExpression()).getLocalVariable();
                Expression update = subStatements.getLast().getExpression();
                Expression expression;

                if (update.isBinaryOperatorExpression()) {
                    expression = update.getLeftExpression();
                } else if (update.isPreOperatorExpression()) {
                    expression = update.getExpression();
                    update = new PostOperatorExpression(update.getLineNumber(), expression, update.getOperator());
                } else if (update.isPostOperatorExpression()) {
                    expression = update.getExpression();
                } else {
                    return new WhileStatement(condition, subStatements);
                }

                if (expression.isLocalVariableReferenceExpression() &&
                        ((ClassFileLocalVariableReferenceExpression) expression).getLocalVariable() == localVariable) {
                    statements.removeLast();
                    subStatements.removeLast();

                    if (condition == BooleanExpression.TRUE) {
                        condition = null;
                    }

                    return newClassFileForStatement(
                        localVariableMaker, basicBlock.getFromOffset(), basicBlock.getToOffset(), init, condition, update, subStatements);
                }
            }
        }

        return new WhileStatement(condition, subStatements);
    }

    private static Statement makeForEachArray(
            Map<String, BaseType> typeBounds, LocalVariableMaker localVariableMaker, Statements statements,
            Expression condition, Statements subStatements, String label, boolean[] labelRequested) {
        if (condition == null) {
            return null;
        }

        int statementsSize = statements.size();

        if (statementsSize < 2 || subStatements.size() < 2) {
            return null;
        }

        // len$ = arr$.length;
        Statement statement = statements.get(statementsSize-2);

        if (!statement.isExpressionStatement()) {
            return null;
        }

        int lineNumber = condition.getLineNumber();
        Expression expression = statement.getExpression();

        if (expression.getLineNumber() != lineNumber || !expression.isBinaryOperatorExpression()) {
            return null;
        }

        if (!expression.getRightExpression().isLengthExpression() || !expression.getLeftExpression().isLocalVariableReferenceExpression()) {
            return null;
        }

        Expression boe = expression;

        expression = boe.getRightExpression().getExpression();

        // ECJ: len$ = (arr$ = array).length;
        Expression arrayAssignment = null;

        if (expression.isBinaryOperatorExpression() && "=".equals(expression.getOperator())
         && expression.getLeftExpression().isLocalVariableReferenceExpression()) {
            arrayAssignment = expression;
            expression = expression.getLeftExpression();
        }

        if (!expression.isLocalVariableReferenceExpression()) {
            return null;
        }

        AbstractLocalVariable syntheticArray = ((ClassFileLocalVariableReferenceExpression)expression).getLocalVariable();
        AbstractLocalVariable syntheticLength = ((ClassFileLocalVariableReferenceExpression)boe.getLeftExpression()).getLocalVariable();

        if (syntheticArray.getName() != null && syntheticArray.getName().indexOf('$') == -1) {
            return null;
        }

        // String s = arr$[i$];
        expression = subStatements.getFirst().getExpression();

        if (!expression.getRightExpression().isArrayExpression() ||
                !expression.getLeftExpression().isLocalVariableReferenceExpression() ||
                expression.getLineNumber() != condition.getLineNumber()) {
            return null;
        }

        ArrayExpression arrayExpression = (ArrayExpression)expression.getRightExpression();

        if (!arrayExpression.getExpression().isLocalVariableReferenceExpression() || !arrayExpression.getIndex().isLocalVariableReferenceExpression()) {
            return null;
        }
        if (((ClassFileLocalVariableReferenceExpression)arrayExpression.getExpression()).getLocalVariable() != syntheticArray) {
            return null;
        }

        AbstractLocalVariable syntheticIndex = ((ClassFileLocalVariableReferenceExpression)arrayExpression.getIndex()).getLocalVariable();
        AbstractLocalVariable item = ((ClassFileLocalVariableReferenceExpression)expression.getLeftExpression()).getLocalVariable();

        if (syntheticIndex.getName() != null && syntheticIndex.getName().indexOf('$') == -1) {
            return null;
        }

        // arr$ = array;
        if (arrayAssignment == null) {
            if (statementsSize < 3) {
                return null;
            }

            expression = statements.get(statementsSize-3).getExpression();
        } else {
            expression = arrayAssignment;
        }

        if (!expression.getLeftExpression().isLocalVariableReferenceExpression()) {
            return null;
        }
        if (((ClassFileLocalVariableReferenceExpression)expression.getLeftExpression()).getLocalVariable() != syntheticArray) {
            return null;
        }

        Type arrayType = expression.getRightExpression().getType();
        Expression array = expression.getRightExpression();

        // i$ = 0;
        expression = statements.get(statementsSize-1).getExpression();

        if (expression.getLineNumber() != lineNumber ||
                !expression.getLeftExpression().isLocalVariableReferenceExpression() ||
                !expression.getRightExpression().isIntegerConstantExpression()) {
            return null;
        }
        if (expression.getRightExpression().getIntegerValue() != 0 ||
                ((ClassFileLocalVariableReferenceExpression)expression.getLeftExpression()).getLocalVariable() != syntheticIndex) {
            return null;
        }

        // i$ < len$;
        if (!condition.getLeftExpression().isLocalVariableReferenceExpression() || !condition.getRightExpression().isLocalVariableReferenceExpression()) {
            return null;
        }
        if (((ClassFileLocalVariableReferenceExpression)condition.getLeftExpression()).getLocalVariable() != syntheticIndex ||
                ((ClassFileLocalVariableReferenceExpression)condition.getRightExpression()).getLocalVariable() != syntheticLength) {
            return null;
        }

        // ++i$;
        expression = subStatements.getLast().getExpression();

        boolean updateAtTheEnd = expression != null && expression.getLineNumber() == lineNumber && expression.isPostOperatorExpression();
        int updateOffset = -1;

        if (updateAtTheEnd) {
            updateOffset = offsetOfUpdate(expression);
        }

        // The compiler copied the update before each 'continue' of the loop (also the ones of its nested loops) if it never falls through
        ContinueUpdatesVisitor continueUpdates = new ContinueUpdatesVisitor(condition, label);

        if (!updateAtTheEnd) {
            subStatements.accept(continueUpdates);

            // Every path which does not 'continue' must leave the loop, else it would advance without updating the index
            if (!neverFallsThrough(subStatements.getLast()) || !continueUpdates.isValid() || !isUpdateOf(continueUpdates.update, syntheticIndex)) {
                return null;
            }
            updateOffset = offsetOfUpdate(continueUpdates.update);
        }

        // Found
        statements.removeLast();
        statements.removeLast();
        if (arrayAssignment == null) {
            statements.removeLast();
        }

        subStatements.removeFirst();
        if (updateAtTheEnd) {
            subStatements.removeLast();
        } else {
            continueUpdates.apply = true;
            subStatements.accept(continueUpdates);
            labelRequested[0] |= continueUpdates.labelUsed;
        }

        item.setDeclared(true);
        Type type = arrayType.createType(arrayType.getDimension()-1);

        if (ObjectType.TYPE_OBJECT.equals(item.getType())) {
            ((ObjectLocalVariable)item).setType(typeBounds, type);
        } else if (item.getType().isGenericType() && type.isGenericType()) {
            ((GenericLocalVariable)item).setType((GenericType)type);
        } else {
            item.typeOnRight(typeBounds, type);
        }

        localVariableMaker.removeLocalVariable(syntheticArray);
        localVariableMaker.removeLocalVariable(syntheticIndex);
        localVariableMaker.removeLocalVariable(syntheticLength);

        if (array instanceof CastExpression castExpression) {
            Type leftArrayType = item.getType().createType(item.getType().getDimension() + 1);
            Type rightArrayType = castExpression.getExpression().getType();
            if (leftArrayType.equals(rightArrayType)
            || (leftArrayType.getDimension() == rightArrayType.getDimension()
             && leftArrayType.isGenericType()
             && StringConstants.JAVA_LANG_OBJECT.equals(rightArrayType.getInternalName()))) {
                return newForEachStatement(item, castExpression.getExpression(), subStatements, updateOffset);
            }
        }

        return newForEachStatement(item, array, subStatements, updateOffset);
    }

    private static ClassFileForEachStatement newForEachStatement(AbstractLocalVariable item, Expression array, Statements subStatements, int updateOffset) {
        ClassFileForEachStatement statement = new ClassFileForEachStatement(item, array, subStatements);

        statement.setUpdateOffset(updateOffset);
        return statement;
    }

    private static boolean isIterable(TypeMaker typeMaker, Type type) {
        return type instanceof ObjectType objectType && typeMaker.isRawTypeAssignable(ObjectType.TYPE_ITERABLE, objectType);
    }

    /** The 'iterator()' of a collection may return a subtype of the interface ('UnmodifiableIterator') */
    private static boolean isIterator(TypeMaker typeMaker, String internalName) {
        return "java/util/Iterator".equals(internalName)
            || typeMaker.isRawTypeAssignable(typeMaker.makeFromInternalTypeName("java/util/Iterator"), typeMaker.makeFromInternalTypeName(internalName));
    }

    private static Statement makeForEachList(
            Map<String, BaseType> typeBounds, LocalVariableMaker localVariableMaker, Statements statements,
            Expression condition, Statements subStatements) {
        if (condition == null) {
            return null;
        }

        if (statements.isEmpty() || subStatements.isEmpty()) {
            return null;
        }

        // i$.hasNext();
        if (!condition.isMethodInvocationExpression()) {
            return null;
        }

        MethodInvocationExpression mie = (MethodInvocationExpression)condition;

        TypeMaker typeMaker = localVariableMaker.getTypeMaker();

        if (!"hasNext".equals(mie.getName()) || !isIterator(typeMaker, mie.getInternalTypeName()) ||
                !mie.getExpression().isLocalVariableReferenceExpression()) {
            return null;
        }

        AbstractLocalVariable syntheticIterator = ((ClassFileLocalVariableReferenceExpression)mie.getExpression()).getLocalVariable();

        // Iterator i$ = list.iterator();
        Expression boe = statements.getLast().getExpression();

        if (boe == null ||
                !boe.getLeftExpression().isLocalVariableReferenceExpression() ||
                !boe.getRightExpression().isMethodInvocationExpression() ||
                boe.getLineNumber() != condition.getLineNumber()) {
            return null;
        }

        mie = (MethodInvocationExpression)boe.getRightExpression();

        if (!"iterator".equals(mie.getName()) || !mie.getDescriptor().startsWith("()L") || !isIterator(typeMaker, mie.getDescriptor().substring(3, mie.getDescriptor().length() - 1))) {
            return null;
        }
        if (((ClassFileLocalVariableReferenceExpression)boe.getLeftExpression()).getLocalVariable() != syntheticIterator) {
            return null;
        }

        Expression list = mie.getExpression();

        if (list.isCastExpression()) {
            list = list.getExpression();
        }
        if (!"()Ljava/util/Iterator;".equals(mie.getDescriptor()) && !isIterable(typeMaker, list.getType())) {
            // a custom 'iterator()' of something which is not an Iterable is no enhanced for statement
            return null;
        }

        // String s = (String)i$.next();
        boe = subStatements.getFirst().getExpression();

        if (!boe.getLeftExpression().isLocalVariableReferenceExpression()) {
            return null;
        }

        Expression expression = boe.getRightExpression();

        if (boe.getRightExpression().isCastExpression()) {
            expression = expression.getExpression();
        }
        if (!expression.isMethodInvocationExpression()) {
            return null;
        }

        mie = (MethodInvocationExpression)expression;

        if (!"next".equals(mie.getName()) ||
                !isIterator(typeMaker, mie.getInternalTypeName()) ||
                !mie.getExpression().isLocalVariableReferenceExpression()) {
            return null;
        }
        if (((ClassFileLocalVariableReferenceExpression)mie.getExpression()).getLocalVariable() != syntheticIterator) {
            return null;
        }

        // Check if 'i$' is not used in sub-statements
        SearchLocalVariableReferenceVisitor visitor1 = new SearchLocalVariableReferenceVisitor();

        visitor1.init(syntheticIterator);

        for (int i=1, len=subStatements.size(); i<len; i++) {
            subStatements.get(i).accept(visitor1);
        }

        if (visitor1.containsReference()) {
            return null;
        }

        // Found
        AbstractLocalVariable item = ((ClassFileLocalVariableReferenceExpression)boe.getLeftExpression()).getLocalVariable();

        statements.removeLast();
        subStatements.remove(0);
        item.setDeclared(true);

        if (syntheticIterator.getReferences().size() == 3) {
            // If local variable is used only for this loop
            localVariableMaker.removeLocalVariable(syntheticIterator);
        }

        Type type = list.getType();

        if (type.isObjectType()) {
            ObjectType listType = (ObjectType)type;

            if (listType.getTypeArguments() == null || listType.getTypeArguments().isWildcardTypeArgument() || listType.getTypeArguments().isGenericTypeArgument()) {
                if (list.isNewExpression()) {
                    ClassFileNewExpression ne = (ClassFileNewExpression) list;
                    ne.setType(listType.createType(item.getType()));
                } else {
                    list = new CastExpression(TYPE_ITERABLE.createType(iterableElementType(item.getType())), list);
                }
            } else {
                CreateTypeFromTypeArgumentVisitor visitor2 = new CreateTypeFromTypeArgumentVisitor();
                listType.getTypeArguments().accept(visitor2);
                type = visitor2.getType();

                if (type != null) {
                    if (TYPE_OBJECT.equals(item.getType())) {
                        if (item instanceof ObjectLocalVariable olv) {
                            olv.setType(typeBounds, type);
                        }
                    } else if (item.getType().isGenericType()) {
                        if ((item instanceof GenericLocalVariable glv) && (type instanceof GenericType)) {
                            glv.setType((GenericType) type);
                        }
                    } else {
                        item.typeOnRight(typeBounds, type);
                    }
                }
            }
        }

        if (list instanceof MethodInvocationExpression && item.getType() instanceof ObjectType) {
            MethodInvocationExpression exp = (MethodInvocationExpression) list;
            ObjectType ot = (ObjectType) item.getType();
            if (ot.getTypeArguments() instanceof WildcardExtendsTypeArgument || ot.getTypeArguments() instanceof WildcardSuperTypeArgument) {
                exp.setNonWildcardTypeArguments(null);
            }
        }

        Type genericFieldType;
        if (list instanceof FieldReferenceExpression fieldReference
                && fieldReference.getExpression() instanceof CastExpression receiverCast
                && receiverCast.getType() instanceof ObjectType receiverType
                && receiverType.getTypeArguments() == null
                && (genericFieldType = getGenericFieldType(localVariableMaker, fieldReference)) != null
                && !TYPE_OBJECT.equals(item.getType())) {
            // A field selected through a raw CHECKCAST has an erased source type even when the local-variable
            // table still describes the foreach item precisely. Keep that item type at the use site: otherwise
            // javac sees Object elements (for example IteratorChain.iteratorQueue in Commons Collections 4.6).
            Type castType = genericFieldType.getDimension() == 0
                    ? TYPE_ITERABLE.createType(box(item.getType()))
                    : box(item.getType()).createType(item.getType().getDimension() + 1);
            list = new CastExpression(castType, list);
        }

        return new ClassFileForEachStatement(item, list, subStatements);
    }

    /** The elements of 'Set&lt;Entry&lt;K, CAP&gt;&gt;' are not the 'Entry&lt;K, ? extends V&gt;' of the loop variable, but extend it */
    private static BaseTypeArgument iterableElementType(Type itemType) {
        if (itemType instanceof ObjectType itemObjectType && itemType.getDimension() == 0 && itemObjectType.getTypeArguments() != null) {
            BaseTypeArgument arguments = itemObjectType.getTypeArguments();

            if (arguments instanceof TypeArguments list ? list.stream().anyMatch(LoopStatementMaker::isWildcard) : isWildcard(arguments)) {
                return new WildcardExtendsTypeArgument(itemObjectType);
            }
        }
        return itemType;
    }

    private static boolean isWildcard(BaseTypeArgument argument) {
        return argument instanceof WildcardExtendsTypeArgument || argument instanceof WildcardSuperTypeArgument || argument instanceof WildcardTypeArgument;
    }

    private static Type getGenericFieldType(LocalVariableMaker localVariableMaker, FieldReferenceExpression field) {
        Type declaredType = localVariableMaker.getTypeMaker().makeFieldType(
                field.getInternalTypeName(), field.getName(), field.getDescriptor());
        return declaredType != null && !declaredType.findTypeParametersInType().isEmpty() ? declaredType : null;
    }

    static Type box(Type type) {
        if (!(type instanceof PrimitiveType primitiveType)) {
            return type;
        }
        return switch (primitiveType.getDescriptor().charAt(0)) {
            case 'B' -> ObjectType.TYPE_BYTE;
            case 'C' -> ObjectType.TYPE_CHARACTER;
            case 'D' -> ObjectType.TYPE_DOUBLE;
            case 'F' -> ObjectType.TYPE_FLOAT;
            case 'I' -> ObjectType.TYPE_INTEGER;
            case 'J' -> ObjectType.TYPE_LONG;
            case 'S' -> ObjectType.TYPE_SHORT;
            case 'Z' -> ObjectType.TYPE_BOOLEAN;
            default -> type;
        };
    }

    /**
     * @param updateOffset the offset of the update of the index of a loop on an array whose update has been removed
     *                     (the loop is laid out with its condition after its body), or -1
     * @param lowOffset    the lowest offset of the loop: its condition, which is at the end of the loop when the compiler lays it out after
     *                     the body (e.g. ECJ), is not the lowest one
     */
    private record LoopOffsets(int continueOffset, int breakOffset, int updateOffset, int lowOffset) {
    }

    private enum Resolution { UNRESOLVED, RESOLVED, RESOLVED_WITH_LABEL }

    private static Statement makeLabels(int loopIndex, LoopOffsets offsets, boolean labelRequested, Statement loop, Statements jumps) {
        String label = labelOf(loopIndex);
        boolean createLabel = labelRequested;
        Iterator<Statement> iterator = jumps.iterator();

        while (iterator.hasNext()) {
            Resolution resolution = resolveJump((ClassFileBreakContinueStatement)iterator.next(), label, offsets);

            if (resolution != Resolution.UNRESOLVED) {
                iterator.remove();
                createLabel |= resolution == Resolution.RESOLVED_WITH_LABEL;
            }
        }

        return createLabel ? new LabelStatement(label, loop) : loop;
    }

    private static Resolution resolveJump(ClassFileBreakContinueStatement statement, String label, LoopOffsets offsets) {
        int offset = statement.getOffset();
        int targetOffset = statement.getTargetOffset();

        if (statement.isLoopExitFromSwitch() || targetOffset == offsets.breakOffset() && targetOffset != offsets.continueOffset()) {
            // Either a loop exit generated inside one of this loop's 'switch' statements (where a bare
            // 'break' would only exit the switch) or a jump to this loop's break target: name this loop
            // and break out of it explicitly.
            statement.setStatement(new BreakStatement(label));
            return Resolution.RESOLVED_WITH_LABEL;
        }
        if (targetOffset == offsets.continueOffset() || targetOffset == offsets.updateOffset()) {
            statement.setStatement(new ContinueStatement(label));
            return Resolution.RESOLVED_WITH_LABEL;
        }
        if (offsets.lowOffset() <= offset && offset < offsets.breakOffset()) {
            if (offsets.lowOffset() <= targetOffset && targetOffset < offsets.breakOffset()) {
                if (statement.isContinueLabel()) {
                    statement.setStatement(new ContinueStatement(label));
                    return Resolution.RESOLVED_WITH_LABEL;
                }
                statement.setStatement(CONTINUE);
                return Resolution.RESOLVED;
            }
            statement.setContinueLabel(true);
        }
        return Resolution.UNRESOLVED;
    }

    private static String labelOf(int loopIndex) {
        return "label" + loopIndex;
    }

    private static int lowestOffset(BasicBlock loopBasicBlock, int continueOffset) {
        int[] lowest = {continueOffset};

        lowestOffset(new HashSet<>(), loopBasicBlock.getSub1(), lowest);
        return lowest[0];
    }

    private static void lowestOffset(Set<BasicBlock> visited, BasicBlock basicBlock, int[] lowest) {
        if (basicBlock == null || basicBlock.matchType(GROUP_END) || !visited.add(basicBlock)) {
            return;
        }
        if (basicBlock.getFromOffset() > 0 && basicBlock.getFromOffset() < lowest[0]) {
            lowest[0] = basicBlock.getFromOffset();
        }
        lowestOffset(visited, basicBlock.getNext(), lowest);
        lowestOffset(visited, basicBlock.getBranch(), lowest);
        lowestOffset(visited, basicBlock.getSub1(), lowest);
        lowestOffset(visited, basicBlock.getSub2(), lowest);
    }

    static void preserveNestedContinueTargets(Statements statements, int targetOffset) {
        statements.accept(new PreserveNestedContinueTargetsVisitor(targetOffset));
    }

    private static final class PreserveNestedContinueTargetsVisitor extends AbstractJavaSyntaxVisitor {
        private final int targetOffset;

        private PreserveNestedContinueTargetsVisitor(int targetOffset) {
            this.targetOffset = targetOffset;
        }

        @Override
        public void visit(Statements statements) {
            for (int index = 0; index < statements.size(); index++) {
                Statement statement = statements.get(index);
                if (statement instanceof ContinueStatement continueStatement
                        && continueStatement.getLabel() == null
                        && !(statement instanceof ClassFileContinueStatement)) {
                    statements.set(index, new ClassFileContinueStatement(targetOffset));
                } else {
                    statement.accept(this);
                }
            }
        }

        @Override
        public void visit(DoWhileStatement statement) {
            // A nested loop owns its unlabelled continues.
        }

        @Override
        public void visit(ForEachStatement statement) {
            // A nested loop owns its unlabelled continues.
        }

        @Override
        public void visit(ForStatement statement) {
            // A nested loop owns its unlabelled continues.
        }

        @Override
        public void visit(WhileStatement statement) {
            // A nested loop owns its unlabelled continues.
        }
    }

    private static ClassFileForStatement newClassFileForStatement(
            LocalVariableMaker localVariableMaker, int fromOffset, int toOffset, BaseExpression init,
            Expression condition, BaseExpression update, BaseStatement statements) {
        if (init != null) {
            SearchFromOffsetVisitor visitor = new SearchFromOffsetVisitor();

            init.accept(visitor);

            int offset = visitor.getOffset();

            if (fromOffset > offset) {
                fromOffset = offset;
            }
        }

        ChangeFrameOfLocalVariablesVisitor visitor = new ChangeFrameOfLocalVariablesVisitor(localVariableMaker);

        if (condition != null) {
            condition.accept(visitor);
        }
        if (update != null) {
            update.accept(visitor);
        }

        return new ClassFileForStatement(fromOffset, toOffset, init, condition, update, statements);
    }
}
