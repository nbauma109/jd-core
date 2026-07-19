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
import org.jd.core.v1.model.javasyntax.statement.WhileStatement;
import org.jd.core.v1.model.javasyntax.type.Type;
import org.jd.core.v1.service.converter.classfiletojavasyntax.model.javasyntax.declaration.ClassFileLocalVariableDeclarator;
import org.jd.core.v1.service.converter.classfiletojavasyntax.model.javasyntax.expression.ClassFileLocalVariableReferenceExpression;
import org.jd.core.v1.service.converter.classfiletojavasyntax.model.javasyntax.statement.ClassFileBreakContinueStatement;
import org.jd.core.v1.service.converter.classfiletojavasyntax.model.javasyntax.statement.ClassFileContinueStatement;
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
        private final Map<String, AbstractLocalVariable> uninitializedVariables = new LinkedHashMap<>();
        private final Map<String, AbstractLocalVariable> declaredVariables = new LinkedHashMap<>();
        private Map<String, AbstractLocalVariable> inheritedVariables = new LinkedHashMap<>();
        private Statements currentStatements;

        @Override
        public void visit(Statements statements) {
            Statements previousStatements = currentStatements;
            Map<String, AbstractLocalVariable> previousUninitializedVariables =
                    new LinkedHashMap<>(uninitializedVariables);
            Map<String, AbstractLocalVariable> previousDeclaredVariables = new LinkedHashMap<>(declaredVariables);
            Map<String, Boolean> previousSeenDeclarations = new LinkedHashMap<>(seenDeclarations);
            Map<String, AbstractLocalVariable> previousInheritedVariables = inheritedVariables;
            inheritedVariables = new LinkedHashMap<>(declaredVariables);
            currentStatements = statements;
            int index = 0;
            while (index < statements.size()) {
                int previousSize = statements.size();
                Statement statement = processStatement(statements, index);
                if (statement != null) {
                    statement.accept(this);
                }
                if (statements.size() >= previousSize) {
                    index++;
                }
            }
            uninitializedVariables.clear();
            uninitializedVariables.putAll(previousUninitializedVariables);
            declaredVariables.clear();
            declaredVariables.putAll(previousDeclaredVariables);
            seenDeclarations.clear();
            seenDeclarations.putAll(previousSeenDeclarations);
            inheritedVariables = previousInheritedVariables;
            currentStatements = previousStatements;
        }

        private Statement processStatement(Statements statements, int index) {
            Statement statement = statements.get(index);
            if (statement instanceof LocalVariableDeclarationStatement declarationStatement
                    && replaceSplitVariableDeclaration(statements, index, declarationStatement)) {
                statement = index < statements.size() ? statements.get(index) : null;
            }
            if (statement instanceof ForStatement forStatement
                    && moveMisplacedForUpdateAfterLoop(statements, index, forStatement)) {
                statement = statements.get(index);
            }
            restorePrecedingLoopUpdate(statements, index, statement);
            if (isRedundantBreakAfterInfiniteLoop(statements, index, statement)) {
                statements.remove(index);
                return null;
            }
            return statement;
        }

        private static void restorePrecedingLoopUpdate(Statements statements, int index, Statement statement) {
            if (statement instanceof WhileStatement whileStatement && index > 0
                    && statements.get(index - 1) instanceof ExpressionStatement precedingStatement
                    && precedingStatement.getExpression() instanceof PostOperatorExpression update
                    && ("--".equals(update.getOperator()) || "++".equals(update.getOperator()))
                    && update.getExpression() instanceof ClassFileLocalVariableReferenceExpression updateReference
                    && whileStatement.getStatements() instanceof Statements loopStatements) {
                restoreLoopUpdateBeforeContinue(loopStatements, update, updateReference.getOffset());
            }
        }

        private static boolean isRedundantBreakAfterInfiniteLoop(
                Statements statements, int index, Statement statement) {
            return statement instanceof BreakStatement && index > 0
                    && statements.get(index - 1) instanceof WhileStatement whileStatement
                    && whileStatement.getCondition() instanceof BooleanExpression condition
                    && condition.isTrue() && !containsBreakForCurrentLoop(whileStatement.getStatements());
        }

        private static boolean containsBreakForCurrentLoop(BaseStatement statement) {
            if (statement == null) {
                return false;
            }
            BreakSearch search = new BreakSearch();
            statement.accept(search);
            return search.found;
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
                        && thenDeclarator.getName().equals(elseDeclarator.getName())
                        && (hasSharedVariableLineage(thenVariable, elseVariable)
                                || hasSingleNullInitializer(thenDeclarator, elseDeclarator))) {
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

        private static boolean hasSingleNullInitializer(
                LocalVariableDeclarator first, LocalVariableDeclarator second) {
            return hasNullInitializer(first) != hasNullInitializer(second);
        }

        private static boolean hasNullInitializer(LocalVariableDeclarator declarator) {
            return declarator.getVariableInitializer() instanceof ExpressionVariableInitializer initializer
                    && initializer.getExpression() instanceof NullExpression;
        }

        private static boolean moveMisplacedForUpdateAfterLoop(Statements containingStatements, int index,
                ForStatement statement) {
            if (index == 0
                    || !(containingStatements.get(index - 1) instanceof LocalVariableDeclarationStatement)
                    || !(statement.getCondition() == null
                            || statement.getCondition() instanceof BooleanExpression condition && condition.isTrue())
                    || statement.getUpdate() == null || statement.getUpdate().size() != 1
                    || !(statement.getStatements() instanceof Statements loopStatements)
                    || loopStatements.size() != 1 || !(loopStatements.getFirst() instanceof IfStatement ifStatement)
                    || !(ifStatement.getStatements() instanceof Statements thenStatements) || thenStatements.isEmpty()
                    || !(ifStatement.getCondition() instanceof BinaryOperatorExpression)) {
                return false;
            }

            Expression update = statement.getUpdate().getFirst();
            SearchFromOffsetVisitor offsetSearch = new SearchFromOffsetVisitor();
            update.accept(offsetSearch);
            Integer continueTargetOffset = bytecodeContinueTargetOffset(thenStatements.getLast());
            if (continueTargetOffset == null
                    || offsetSearch.getOffset() == Integer.MAX_VALUE
                    || isUpdateTarget(continueTargetOffset, offsetSearch.getOffset())) {
                return false;
            }

            thenStatements.remove(thenStatements.size() - 1);
            statement.setCondition(ifStatement.getCondition());
            statement.setUpdate(null);
            loopStatements.clear();
            loopStatements.addAll(thenStatements);
            containingStatements.add(index + 1, new ExpressionStatement(update));
            return true;
        }

        private static Integer bytecodeContinueTargetOffset(Statement statement) {
            if (statement instanceof ClassFileContinueStatement classFileContinueStatement) {
                return classFileContinueStatement.getTargetOffset();
            }
            if (statement instanceof ClassFileBreakContinueStatement classFileStatement
                    && classFileStatement.getStatement() instanceof ContinueStatement continueStatement
                    && continueStatement.getLabel() == null) {
                return classFileStatement.getTargetOffset();
            }
            return null;
        }

        private static boolean isUpdateTarget(int targetOffset, int updateOffset) {
            // Assignment updates begin at their first local-variable load; IINC references retain
            // the operand offset immediately after the opcode (or after WIDE's operand sequence).
            return targetOffset == updateOffset || isIincTarget(targetOffset, updateOffset);
        }

        private static void restoreLoopUpdateBeforeContinue(
                Statements statements, PostOperatorExpression update, int updateOffset) {
            for (int index = statements.size() - 1; index >= 0; index--) {
                Statement statement = statements.get(index);
                if (isContinueTargeting(statement, updateOffset)) {
                    statements.add(index, new ExpressionStatement(update.copyTo(update.getLineNumber())));
                } else if (statement instanceof IfElseStatement ifElseStatement) {
                    insertUpdateBeforeContinue(ifElseStatement.getStatements(), update, updateOffset);
                    insertUpdateBeforeContinue(ifElseStatement.getElseStatements(), update, updateOffset);
                } else if (statement instanceof IfStatement ifStatement) {
                    insertUpdateBeforeContinue(ifStatement.getStatements(), update, updateOffset);
                } else if (statement instanceof SwitchStatement switchStatement) {
                    insertUpdateBeforeSwitchContinues(switchStatement, update, updateOffset);
                }
            }
        }

        private static void insertUpdateBeforeContinue(
                BaseStatement statement, PostOperatorExpression update, int updateOffset) {
            if (statement instanceof Statements statements) {
                restoreLoopUpdateBeforeContinue(statements, update, updateOffset);
            } else if (statement instanceof IfElseStatement ifElseStatement) {
                insertUpdateBeforeContinue(ifElseStatement.getStatements(), update, updateOffset);
                insertUpdateBeforeContinue(ifElseStatement.getElseStatements(), update, updateOffset);
            } else if (statement instanceof IfStatement ifStatement) {
                insertUpdateBeforeContinue(ifStatement.getStatements(), update, updateOffset);
            } else if (statement instanceof SwitchStatement switchStatement) {
                insertUpdateBeforeSwitchContinues(switchStatement, update, updateOffset);
            }
        }

        private static void insertUpdateBeforeSwitchContinues(
                SwitchStatement statement, PostOperatorExpression update, int updateOffset) {
            for (SwitchStatement.Block block : statement.getBlocks()) {
                insertUpdateBeforeContinue(block.getStatements(), update, updateOffset);
            }
        }

        private static boolean isContinueTargeting(Statement statement, int targetOffset) {
            if (statement instanceof ClassFileContinueStatement classFileContinueStatement) {
                return isIincTarget(classFileContinueStatement.getTargetOffset(), targetOffset);
            }
            return statement instanceof ClassFileBreakContinueStatement classFileStatement
                    && isIincTarget(classFileStatement.getTargetOffset(), targetOffset)
                    && classFileStatement.getStatement() instanceof ContinueStatement continueStatement
                    && continueStatement.getLabel() == null;
        }

        private static boolean isIincTarget(int jumpTargetOffset, int localVariableOperandOffset) {
            // A normal IINC stores its local index one byte after the opcode. A WIDE IINC stores the
            // reference offset at its final operand, five bytes after WIDE. Only a jump back to that
            // exact instruction proves that the preceding update belongs on the continue path.
            return jumpTargetOffset == localVariableOperandOffset - 1
                    || jumpTargetOffset == localVariableOperandOffset - 5;
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
        public void visit(LocalVariableDeclarationStatement statement) {
            for (LocalVariableDeclarator declarator : statement.getLocalVariableDeclarators()) {
                seenDeclarations.put(key(declarator.getName(), statement.getType().getDescriptor()), Boolean.TRUE);
            }
            if (statement.getLocalVariableDeclarators() instanceof LocalVariableDeclarator declarator) {
                String key = key(declarator.getName(), statement.getType().getDescriptor());
                declarations.putIfAbsent(key, new DeclarationLocation(currentStatements, statement, declarator));
                if (declarator instanceof ClassFileLocalVariableDeclarator classFileDeclarator) {
                    String slotKey = slotKey(classFileDeclarator.getLocalVariable(), statement.getType());
                    declaredVariables.putIfAbsent(slotKey, classFileDeclarator.getLocalVariable());
                    if (declarator.getVariableInitializer() == null) {
                        uninitializedVariables.putIfAbsent(slotKey, classFileDeclarator.getLocalVariable());
                    }
                }
            }
            super.visit(statement);
        }

        @Override
        public void visit(LocalVariableDeclaration declaration) {
            for (LocalVariableDeclarator declarator : declaration.getLocalVariableDeclarators()) {
                seenDeclarations.put(key(declarator.getName(), declaration.getType().getDescriptor()), Boolean.TRUE);
            }
            super.visit(declaration);
        }

        @Override
        public void visit(LocalVariableReferenceExpression expression) {
            if (expression instanceof ClassFileLocalVariableReferenceExpression reference) {
                String slotKey = slotKey(reference.getLocalVariable(), reference.getType());
                AbstractLocalVariable canonical = splitVariables.get(slotKey);
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
            // DUP-based compound array assignments reuse the same array and index expressions. Two source-level
            // post-increments merely look alike, but are distinct nodes and must retain both side effects.
            return first.getExpression() == second.getExpression()
                    && firstVariable == secondVariable;
        }

        @Override
        public void visit(ForEachStatement statement) {
            statement.getExpression().accept(this);
            Map<String, Boolean> previousSeenDeclarations = new LinkedHashMap<>(seenDeclarations);
            seenDeclarations.put(key(statement.getName(), statement.getType().getDescriptor()), Boolean.TRUE);
            restoreConnectionLoopBreak(statement.getStatements());
            safeAccept(statement.getStatements());
            seenDeclarations.clear();
            seenDeclarations.putAll(previousSeenDeclarations);
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
            replaceSplitVariableForDeclaration(statement);
            Map<String, Boolean> previousSeenDeclarations = new LinkedHashMap<>(seenDeclarations);
            super.visit(statement);
            seenDeclarations.clear();
            seenDeclarations.putAll(previousSeenDeclarations);
        }

        private void replaceSplitVariableForDeclaration(ForStatement statement) {
            LocalVariableDeclaration declaration = statement.getDeclaration();
            if (declaration == null
                    || !(declaration.getLocalVariableDeclarators() instanceof ClassFileLocalVariableDeclarator declarator)) {
                return;
            }
            String slotKey = slotKey(declarator.getLocalVariable(), declaration.getType());
            AbstractLocalVariable canonical = splitVariables.get(slotKey);
            if (canonical == null) {
                canonical = uninitializedVariables.get(slotKey);
            }
            if (canonical == null) {
                AbstractLocalVariable inherited = inheritedVariables.get(slotKey);
                AbstractLocalVariable variable = declarator.getLocalVariable();
                if (sameSourceVariable(inherited, variable)) {
                    canonical = inherited;
                }
            }
            if (canonical == null) {
                return;
            }
            declarator.getLocalVariable().setName(canonical.getName());
            statement.setDeclaration(null);
            if (declarator.getVariableInitializer() instanceof ExpressionVariableInitializer initializer) {
                LocalVariableReferenceExpression reference = new LocalVariableReferenceExpression(
                        declarator.getLineNumber(), declaration.getType(), canonical.getName());
                statement.setInit(new BinaryOperatorExpression(declarator.getLineNumber(), declaration.getType(),
                        reference, "=", initializer.getExpression(), 16));
            }
        }

        private static boolean sameSourceVariable(AbstractLocalVariable first, AbstractLocalVariable second) {
            if (first == null) {
                return false;
            }
            return first.getName().equals(second.getName()) || hasSharedVariableLineage(first, second);
        }

        private static boolean hasSharedVariableLineage(
                AbstractLocalVariable first, AbstractLocalVariable second) {
            AbstractLocalVariable firstOriginal = first.getOriginalVariable();
            AbstractLocalVariable secondOriginal = second.getOriginalVariable();
            return first == second || firstOriginal == second || secondOriginal == first
                    || firstOriginal != null && firstOriginal == secondOriginal;
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

        private static final class BreakSearch extends AbstractJavaSyntaxVisitor {
            private boolean found;

            @Override
            public void visit(BreakStatement statement) {
                if (statement.getLabel() == null) {
                    found = true;
                }
            }

            @Override public void visit(DoWhileStatement statement) {
                // A nested loop owns its unlabelled breaks.
            }
            @Override public void visit(ForEachStatement statement) {
                // A nested loop owns its unlabelled breaks.
            }
            @Override public void visit(ForStatement statement) {
                // A nested loop owns its unlabelled breaks.
            }
            @Override public void visit(SwitchStatement statement) {
                // A nested switch owns its unlabelled breaks.
            }
            @Override public void visit(WhileStatement statement) {
                // A nested loop owns its unlabelled breaks.
            }
        }

        @Override
        public void visit(WhileStatement statement) {
            if (statement.getCondition() instanceof BooleanExpression condition && condition.isTrue()
                    && statement.getStatements() instanceof Statements statements && !statements.isEmpty()
                    && statements.getLast() instanceof IfStatement ifStatement
                    && ifStatement.getCondition().isBinaryOperatorExpression()
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
            int insertionIndex = constructorInvocationOffset(methodStatements);
            for (Map.Entry<String, AbstractLocalVariable> entry : splitVariables.entrySet()) {
                methodStatements.add(insertionIndex++, new LocalVariableDeclarationStatement(
                        splitVariableTypes.get(entry.getKey()), new LocalVariableDeclarator(entry.getValue().getName())));
            }
            for (Map.Entry<String, DeclarationLocation> entry : declarations.entrySet()) {
                if (isHoistable(entry)) {
                    hoistDeclaration(methodStatements, entry, insertionIndex);
                    insertionIndex++;
                }
            }
        }

        private static int constructorInvocationOffset(Statements statements) {
            if (!statements.isEmpty() && statements.getFirst() instanceof ExpressionStatement expressionStatement) {
                Expression expression = expressionStatement.getExpression();
                if (expression.isSuperConstructorInvocationExpression()
                        || expression.isConstructorInvocationExpression()) {
                    return 1;
                }
            }
            return 0;
        }

        private boolean isHoistable(Map.Entry<String, DeclarationLocation> entry) {
            DeclarationLocation location = entry.getValue();
            return referencedBeforeDeclaration.containsKey(entry.getKey())
                    && location.declarator().getVariableInitializer() instanceof ExpressionVariableInitializer
                    && location.statements().indexOf(location.statement()) >= 0;
        }

        private void hoistDeclaration(Statements methodStatements,
                Map.Entry<String, DeclarationLocation> entry, int insertionIndex) {
            if (isHoistable(entry)) {
                DeclarationLocation location = entry.getValue();
                ExpressionVariableInitializer initializer =
                        (ExpressionVariableInitializer) location.declarator().getVariableInitializer();
                int index = location.statements().indexOf(location.statement());
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
                methodStatements.add(insertionIndex, new LocalVariableDeclarationStatement(
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
