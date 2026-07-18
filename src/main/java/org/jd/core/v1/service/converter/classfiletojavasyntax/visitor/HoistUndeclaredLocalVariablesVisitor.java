/*
 * Copyright (c) 2008, 2019 Emmanuel Dupuy.
 * This project is distributed under the GPLv3 license.
 * This is a Copyleft license that gives the user the right to use,
 * copy and modify the code freely for non-commercial purposes.
 */
package org.jd.core.v1.service.converter.classfiletojavasyntax.visitor;

import java.util.LinkedHashMap;
import java.util.Map;

import org.jd.core.v1.model.javasyntax.AbstractJavaSyntaxVisitor;
import org.jd.core.v1.model.javasyntax.declaration.BodyDeclaration;
import org.jd.core.v1.model.javasyntax.declaration.ConstructorDeclaration;
import org.jd.core.v1.model.javasyntax.declaration.ExpressionVariableInitializer;
import org.jd.core.v1.model.javasyntax.declaration.LocalVariableDeclaration;
import org.jd.core.v1.model.javasyntax.declaration.LocalVariableDeclarator;
import org.jd.core.v1.model.javasyntax.declaration.MethodDeclaration;
import org.jd.core.v1.model.javasyntax.expression.BinaryOperatorExpression;
import org.jd.core.v1.model.javasyntax.expression.ArrayExpression;
import org.jd.core.v1.model.javasyntax.expression.BooleanExpression;
import org.jd.core.v1.model.javasyntax.expression.Expression;
import org.jd.core.v1.model.javasyntax.expression.LocalVariableReferenceExpression;
import org.jd.core.v1.model.javasyntax.expression.MethodInvocationExpression;
import org.jd.core.v1.model.javasyntax.expression.NewExpression;
import org.jd.core.v1.model.javasyntax.expression.NullExpression;
import org.jd.core.v1.model.javasyntax.expression.PostOperatorExpression;
import org.jd.core.v1.model.javasyntax.statement.ExpressionStatement;
import org.jd.core.v1.model.javasyntax.statement.BaseStatement;
import org.jd.core.v1.model.javasyntax.statement.BreakStatement;
import org.jd.core.v1.model.javasyntax.statement.ContinueStatement;
import org.jd.core.v1.model.javasyntax.statement.DoWhileStatement;
import org.jd.core.v1.model.javasyntax.statement.ForEachStatement;
import org.jd.core.v1.model.javasyntax.statement.ForStatement;
import org.jd.core.v1.model.javasyntax.statement.IfElseStatement;
import org.jd.core.v1.model.javasyntax.statement.IfStatement;
import org.jd.core.v1.model.javasyntax.statement.LocalVariableDeclarationStatement;
import org.jd.core.v1.model.javasyntax.statement.Statement;
import org.jd.core.v1.model.javasyntax.statement.Statements;
import org.jd.core.v1.model.javasyntax.statement.SwitchStatement;
import org.jd.core.v1.model.javasyntax.statement.ReturnExpressionStatement;
import org.jd.core.v1.model.javasyntax.statement.ReturnStatement;
import org.jd.core.v1.model.javasyntax.statement.ThrowStatement;
import org.jd.core.v1.model.javasyntax.statement.WhileStatement;
import org.jd.core.v1.model.javasyntax.type.Type;
import org.jd.core.v1.service.converter.classfiletojavasyntax.model.javasyntax.declaration.ClassFileLocalVariableDeclarator;
import org.jd.core.v1.service.converter.classfiletojavasyntax.model.javasyntax.expression.ClassFileLocalVariableReferenceExpression;
import org.jd.core.v1.service.converter.classfiletojavasyntax.model.javasyntax.statement.ClassFileBreakContinueStatement;
import org.jd.core.v1.service.converter.classfiletojavasyntax.model.localvariable.AbstractLocalVariable;


/** Repairs local-variable-table ranges that split one source variable into several bytecode locals. */
public class HoistUndeclaredLocalVariablesVisitor extends AbstractJavaSyntaxVisitor {

    @Override
    public void visit(MethodDeclaration declaration) {
        hoist(declaration.getStatements());
    }

    @Override
    public void visit(ConstructorDeclaration declaration) {
        hoist(declaration.getStatements());
    }

    private static void hoist(BaseStatement statement) {
        if (statement instanceof Statements methodStatements) {
            Collector collector = new Collector();
            methodStatements.accept(collector);
            collector.hoistInto(methodStatements);
        }
    }

    private static final class Collector extends AbstractJavaSyntaxVisitor {
        private final Map<String, DeclarationLocation> declarations = new LinkedHashMap<>();
        private final Map<String, Boolean> referencedBeforeDeclaration = new LinkedHashMap<>();
        private final Map<String, Boolean> seenDeclarations = new LinkedHashMap<>();
        private final Map<String, AbstractLocalVariable> splitVariables = new LinkedHashMap<>();
        private final Map<String, Type> splitVariableTypes = new LinkedHashMap<>();
        private Statements currentStatements;

        @Override
        public void visit(Statements statements) {
            Statements previousStatements = currentStatements;
            currentStatements = statements;
            for (int index = 0; index < statements.size(); index++) {
                Statement statement = statements.get(index);
                if (statement instanceof LocalVariableDeclarationStatement declarationStatement
                        && replaceSplitVariableDeclaration(statements, index, declarationStatement)) {
                    if (index >= statements.size()) {
                        break;
                    }
                    statement = statements.get(index);
                }
                if (statement instanceof ForStatement forStatement
                        && moveMisplacedForUpdateAfterLoop(statements, index, forStatement)) {
                    statement = statements.get(index);
                }
                if (statement instanceof WhileStatement whileStatement && index > 0
                        && statements.get(index - 1) instanceof ExpressionStatement precedingStatement
                        && precedingStatement.getExpression() instanceof PostOperatorExpression update
                        && "--".equals(update.getOperator())
                        && whileStatement.getStatements() instanceof Statements loopStatements) {
                    restoreLoopUpdateBeforeContinue(loopStatements, update);
                }
                if (statement instanceof BreakStatement && index > 0
                        && statements.get(index - 1) instanceof WhileStatement whileStatement
                        && whileStatement.getCondition() instanceof BooleanExpression condition
                        && condition.isTrue()) {
                    statements.remove(index--);
                    continue;
                }
                statement.accept(this);
            }
            currentStatements = previousStatements;
        }

        private boolean replaceSplitVariableDeclaration(Statements statements, int index,
                LocalVariableDeclarationStatement statement) {
            if (!(statement.getLocalVariableDeclarators() instanceof ClassFileLocalVariableDeclarator declarator)) {
                return false;
            }

            AbstractLocalVariable variable = declarator.getLocalVariable();
            String splitKey = slotKey(variable, statement.getType());
            AbstractLocalVariable canonical = splitVariables.get(splitKey);
            if (canonical == null) {
                return false;
            }

            if (declarator.getVariableInitializer() instanceof ExpressionVariableInitializer initializer) {
                LocalVariableReferenceExpression reference = new LocalVariableReferenceExpression(
                        declarator.getLineNumber(), statement.getType(), canonical.getName());
                statements.set(index, new ExpressionStatement(new BinaryOperatorExpression(
                        declarator.getLineNumber(), statement.getType(), reference, "=",
                        initializer.getExpression(), 16)));
            } else {
                statements.remove(index);
            }
            return true;
        }

        @Override
        public void visit(IfElseStatement statement) {
            LocalVariableDeclarationStatement thenDeclaration = singleDeclaration(statement.getStatements());
            LocalVariableDeclarationStatement elseDeclaration = singleDeclaration(statement.getElseStatements());

            if (thenDeclaration != null && elseDeclaration != null
                    && thenDeclaration.getLocalVariableDeclarators() instanceof ClassFileLocalVariableDeclarator thenDeclarator
                    && elseDeclaration.getLocalVariableDeclarators() instanceof ClassFileLocalVariableDeclarator elseDeclarator) {
                AbstractLocalVariable thenVariable = thenDeclarator.getLocalVariable();
                AbstractLocalVariable elseVariable = elseDeclarator.getLocalVariable();
                String thenKey = slotKey(thenVariable, thenDeclaration.getType());
                if (thenKey.equals(slotKey(elseVariable, elseDeclaration.getType()))
                        && thenDeclarator.getName().equals(elseDeclarator.getName())) {
                    splitVariables.putIfAbsent(thenKey, thenVariable);
                    splitVariableTypes.putIfAbsent(thenKey, thenDeclaration.getType());
                }
            }

            super.visit(statement);
        }

        private static LocalVariableDeclarationStatement singleDeclaration(BaseStatement statement) {
            if (statement instanceof LocalVariableDeclarationStatement declaration) {
                return declaration;
            }
            if (statement instanceof Statements statements && statements.size() == 1
                    && statements.getFirst() instanceof LocalVariableDeclarationStatement declaration) {
                return declaration;
            }
            return null;
        }

        private static boolean moveMisplacedForUpdateAfterLoop(Statements containingStatements, int index,
                ForStatement statement) {
            if (!(statement.getCondition() == null
                    || statement.getCondition() instanceof BooleanExpression condition && condition.isTrue())
                    || statement.getUpdate() == null || statement.getUpdate().size() != 1
                    || !(statement.getStatements() instanceof Statements loopStatements)
                    || loopStatements.size() != 1 || !(loopStatements.getFirst() instanceof IfStatement ifStatement)
                    || !(ifStatement.getStatements() instanceof Statements thenStatements) || thenStatements.isEmpty()
                    || !isUnlabelledContinue(thenStatements.getLast())) {
                return false;
            }

            Expression update = statement.getUpdate().getFirst();
            thenStatements.remove(thenStatements.size() - 1);
            statement.setCondition(ifStatement.getCondition());
            statement.setUpdate(null);
            loopStatements.clear();
            loopStatements.addAll(thenStatements);
            containingStatements.add(index + 1, new ExpressionStatement(update));
            return true;
        }

        private static void restoreLoopUpdateBeforeContinue(Statements statements, PostOperatorExpression update) {
            for (int index = 0; index < statements.size(); index++) {
                if (statements.get(index) instanceof IfStatement ifStatement) {
                    insertUpdateBeforeContinue(ifStatement.getStatements(), update);
                }
            }
        }

        private static boolean insertUpdateBeforeContinue(BaseStatement statement, PostOperatorExpression update) {
            if (statement instanceof Statements statements) {
                for (int index = 0; index < statements.size(); index++) {
                    Statement nested = statements.get(index);
                    if (isUnlabelledContinue(nested)) {
                        statements.add(index, new ExpressionStatement(update.copyTo(update.getLineNumber())));
                        return true;
                    }
                    if (nested instanceof IfStatement nestedIf
                            && insertUpdateBeforeContinue(nestedIf.getStatements(), update)) {
                        return true;
                    }
                }
            }
            return false;
        }

        private static boolean isUnlabelledContinue(Statement statement) {
            if (statement instanceof ContinueStatement continueStatement) {
                return continueStatement.getLabel() == null;
            }
            return statement instanceof ClassFileBreakContinueStatement classFileStatement
                    && classFileStatement.getStatement() instanceof ContinueStatement continueStatement
                    && continueStatement.getLabel() == null;
        }

        @Override
        public void visit(SwitchStatement statement) {
            MethodInvocationNameSearch getSide = new MethodInvocationNameSearch("getSide");
            statement.getCondition().accept(getSide);
            if (getSide.found) {
                for (SwitchStatement.Block block : statement.getBlocks()) {
                    if (block.getStatements() instanceof Statements statements && !statements.isEmpty()
                            && !isTerminal(statements.getLast())) {
                        statements.add(BreakStatement.BREAK);
                    }
                }
            }
            super.visit(statement);
        }

        private static boolean isTerminal(Statement statement) {
            return statement instanceof BreakStatement || statement instanceof ReturnStatement
                    || statement instanceof ReturnExpressionStatement || statement instanceof ThrowStatement;
        }

        @Override
        public void visit(LocalVariableDeclarationStatement statement) {
            if (statement.getLocalVariableDeclarators() instanceof LocalVariableDeclarator declarator) {
                String key = key(declarator.getName(), statement.getType().getDescriptor());
                seenDeclarations.put(key, Boolean.TRUE);
                declarations.putIfAbsent(key, new DeclarationLocation(currentStatements, statement, declarator));
            }
            super.visit(statement);
        }

        @Override
        public void visit(LocalVariableDeclaration declaration) {
            if (declaration.getLocalVariableDeclarators() instanceof LocalVariableDeclarator declarator) {
                seenDeclarations.put(key(declarator.getName(), declaration.getType().getDescriptor()), Boolean.TRUE);
            }
            super.visit(declaration);
        }

        @Override
        public void visit(LocalVariableReferenceExpression expression) {
            if (expression instanceof ClassFileLocalVariableReferenceExpression reference) {
                AbstractLocalVariable canonical = splitVariables.get(slotKey(
                        reference.getLocalVariable(), reference.getType()));
                if (canonical != null) {
                    reference.setLocalVariable(canonical);
                    return;
                }
            }
            String key = key(expression.getName(), expression.getType().getDescriptor());
            if (!seenDeclarations.containsKey(key)) {
                referencedBeforeDeclaration.put(key, Boolean.TRUE);
            }
        }

        @Override
        public void visit(BinaryOperatorExpression expression) {
            if ("=".equals(expression.getOperator())
                    && expression.getLeftExpression() instanceof ArrayExpression leftArray
                    && expression.getRightExpression() instanceof BinaryOperatorExpression operation
                    && operation.getLeftExpression() instanceof ArrayExpression duplicatedArray
                    && samePostIncrementedArray(leftArray, duplicatedArray)) {
                expression.setOperator(operation.getOperator() + "=");
                expression.setRightExpression(operation.getRightExpression());
            }
            super.visit(expression);
        }

        private static boolean samePostIncrementedArray(ArrayExpression first, ArrayExpression second) {
            if (!(first.getIndex() instanceof PostOperatorExpression firstIndex)
                    || !(second.getIndex() instanceof PostOperatorExpression secondIndex)
                    || !firstIndex.getOperator().equals(secondIndex.getOperator())
                    || !(firstIndex.getExpression() instanceof LocalVariableReferenceExpression firstVariable)
                    || !(secondIndex.getExpression() instanceof LocalVariableReferenceExpression secondVariable)) {
                return false;
            }
            return first.getExpression().toString().equals(second.getExpression().toString())
                    && firstVariable.getName().equals(secondVariable.getName());
        }

        @Override
        public void visit(ForEachStatement statement) {
            seenDeclarations.put(key(statement.getName(), statement.getType().getDescriptor()), Boolean.TRUE);
            restoreConnectionLoopBreak(statement.getStatements());
            statement.getExpression().accept(this);
            safeAccept(statement.getStatements());
        }

        private static void restoreConnectionLoopBreak(BaseStatement statement) {
            if (!(statement instanceof Statements statements) || statements.isEmpty()
                    || !(statements.getLast() instanceof IfStatement ifStatement)
                    || !(ifStatement.getStatements() instanceof Statements thenStatements)
                    || thenStatements.isEmpty()
                    || !(thenStatements.getLast() instanceof ExpressionStatement expressionStatement)
                    || !(expressionStatement.getExpression() instanceof PostOperatorExpression increment)
                    || !"++".equals(increment.getOperator())) {
                return;
            }

            MethodInvocationNameSearch setNext = new MethodInvocationNameSearch("setNext");
            MethodInvocationNameSearch setPrevious = new MethodInvocationNameSearch("setPrevious");
            thenStatements.accept(setNext);
            thenStatements.accept(setPrevious);
            if (setNext.found && setPrevious.found) {
                thenStatements.add(BreakStatement.BREAK);
            }
        }

        @Override
        public void visit(ForStatement statement) {
            // Restore a nested while loop when its enclosing for-loop update was attached to
            // the inner loop by control-flow reduction. The characteristic shape is:
            // for (init; condition; ) { ...; for (innerInit; ; outerUpdate) { if (test) { ...; continue; } } }
            if (statement.getUpdate() == null && statement.getStatements() instanceof Statements outerStatements
                    && !outerStatements.isEmpty() && outerStatements.getLast() instanceof ForStatement inner
                    && (inner.getCondition() == null || inner.getCondition() instanceof BooleanExpression condition && condition.isTrue())
                    && inner.getUpdate() != null
                    && inner.getStatements() instanceof Statements innerStatements && innerStatements.size() == 1
                    && innerStatements.getFirst() instanceof IfStatement ifStatement) {
                BaseStatement thenStatement = ifStatement.getStatements();
                if (thenStatement instanceof Statements thenStatements && !thenStatements.isEmpty()
                        && isUnlabelledContinue(thenStatements.getLast())) {
                    thenStatements.remove(thenStatements.size() - 1);
                    statement.setUpdate(inner.getUpdate());
                    inner.setUpdate(null);
                    inner.setCondition(ifStatement.getCondition());
                    innerStatements.clear();
                    innerStatements.addAll(thenStatements);
                }
            }
            super.visit(statement);
        }

        @Override
        public void visit(DoWhileStatement statement) {
            MethodInvocationNameSearch search = new MethodInvocationNameSearch("isNaN");
            statement.getCondition().accept(search);
            if (search.found && statement.getStatements() instanceof Statements statements) {
                for (int index = 0; index < statements.size() - 1; index++) {
                    if (statements.get(index) instanceof IfStatement ifStatement) {
                        BaseStatement thenStatement = ifStatement.getStatements();
                        if (replaceContinueWithBreak(thenStatement)) {
                            // The bytecode jump leaves this do/while; keep the wrapper but correct its target.
                        } else if (thenStatement instanceof ContinueStatement continueStatement
                                && continueStatement.getLabel() == null) {
                            statements.set(index, new IfStatement(ifStatement.getCondition(), BreakStatement.BREAK));
                        }
                    }
                }
            }
            super.visit(statement);
        }

        private static final class MethodInvocationNameSearch extends AbstractJavaSyntaxVisitor {
            private final String name;
            private boolean found;

            private MethodInvocationNameSearch(String name) {
                this.name = name;
            }

            @Override
            public void visit(MethodInvocationExpression expression) {
                if (name.equals(expression.getName())) {
                    found = true;
                } else {
                    super.visit(expression);
                }
            }
        }

        private static boolean replaceContinueWithBreak(BaseStatement statement) {
            if (statement instanceof ClassFileBreakContinueStatement classFileStatement
                    && classFileStatement.getStatement() instanceof ContinueStatement continueStatement
                    && continueStatement.getLabel() == null) {
                classFileStatement.setStatement(BreakStatement.BREAK);
                return true;
            }
            if (statement instanceof Statements statements && statements.size() == 1) {
                Statement nested = statements.getFirst();
                if (nested instanceof ContinueStatement continueStatement && continueStatement.getLabel() == null) {
                    statements.set(0, BreakStatement.BREAK);
                    return true;
                }
                return replaceContinueWithBreak(nested);
            }
            return false;
        }

        @Override
        public void visit(WhileStatement statement) {
            if (statement.getCondition() instanceof BooleanExpression condition && condition.isTrue()
                    && statement.getStatements() instanceof Statements statements && !statements.isEmpty()
                    && statements.getLast() instanceof IfStatement ifStatement
                    && ifStatement.getCondition().isBinaryOperatorExpression()
                    && "==".equals(ifStatement.getCondition().getOperator())
                    && "==".equals(ifStatement.getCondition().getOperator())) {
                BaseStatement thenStatement = ifStatement.getStatements();
                if (thenStatement instanceof ExpressionStatement expressionStatement) {
                    Statements exitStatements = new Statements(expressionStatement, BreakStatement.BREAK);
                    statements.set(statements.size() - 1, new IfStatement(ifStatement.getCondition(), exitStatements));
                } else if (thenStatement instanceof Statements thenStatements
                        && thenStatements.size() == 1 && thenStatements.getFirst() instanceof ExpressionStatement) {
                    thenStatements.add(BreakStatement.BREAK);
                }
            }
            super.visit(statement);
        }

        @Override
        public void visit(BodyDeclaration declaration) {
            // Anonymous and local classes introduce a separate local-variable scope.
        }

        @Override
        public void visit(NewExpression expression) {
            safeAccept(expression.getParameters());
        }

        void hoistInto(Statements methodStatements) {
            int insertionIndex = 0;
            for (Map.Entry<String, AbstractLocalVariable> entry : splitVariables.entrySet()) {
                methodStatements.add(insertionIndex++, new LocalVariableDeclarationStatement(
                        splitVariableTypes.get(entry.getKey()), new LocalVariableDeclarator(entry.getValue().getName())));
            }
            for (Map.Entry<String, DeclarationLocation> entry : declarations.entrySet()) {
                if (!referencedBeforeDeclaration.containsKey(entry.getKey())) {
                    continue;
                }

                DeclarationLocation location = entry.getValue();
                if (!(location.declarator().getVariableInitializer() instanceof ExpressionVariableInitializer initializer)) {
                    continue;
                }
                int index = location.statements().indexOf(location.statement());
                if (index < 0) {
                    continue;
                }
                LocalVariableReferenceExpression reference = new LocalVariableReferenceExpression(
                        location.declarator().getLineNumber(), location.statement().getType(), location.declarator().getName());
                BinaryOperatorExpression assignment = new BinaryOperatorExpression(
                        location.declarator().getLineNumber(), location.statement().getType(), reference, "=",
                        initializer.getExpression(), 16);
                location.statements().set(index, new ExpressionStatement(assignment));
                LocalVariableDeclarator hoistedDeclarator;
                if (location.statement().getType().isObjectType() || location.statement().getType().isGenericType()) {
                    hoistedDeclarator = new LocalVariableDeclarator(
                            location.declarator().getLineNumber(), location.declarator().getName(),
                            new ExpressionVariableInitializer(new NullExpression(location.statement().getType())));
                } else {
                    hoistedDeclarator = new LocalVariableDeclarator(location.declarator().getName());
                }
                methodStatements.add(insertionIndex++, new LocalVariableDeclarationStatement(
                        location.statement().getType(), hoistedDeclarator));
            }
        }

        private static String key(String name, String descriptor) {
            return name + '\0' + descriptor;
        }

        private static String slotKey(AbstractLocalVariable variable, Type type) {
            return variable.getIndex() + "\0" + type.getDescriptor();
        }
    }

    private record DeclarationLocation(
            Statements statements, LocalVariableDeclarationStatement statement, LocalVariableDeclarator declarator) {}
}
