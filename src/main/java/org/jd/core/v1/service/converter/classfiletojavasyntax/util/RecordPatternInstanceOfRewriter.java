/*
 * Copyright (c) 2026 GPLv3.
 * This project is distributed under the GPLv3 license.
 * This is a Copyleft license that gives the user the right to use,
 * copy and modify the code freely for non-commercial purposes.
 */

package org.jd.core.v1.service.converter.classfiletojavasyntax.util;

import static org.jd.core.v1.model.javasyntax.type.PrimitiveType.TYPE_BOOLEAN;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.jd.core.v1.model.javasyntax.AbstractJavaSyntaxVisitor;
import org.jd.core.v1.model.javasyntax.declaration.LocalVariableDeclarator;
import org.jd.core.v1.model.javasyntax.expression.BinaryOperatorExpression;
import org.jd.core.v1.model.javasyntax.expression.BooleanExpression;
import org.jd.core.v1.model.javasyntax.expression.Expression;
import org.jd.core.v1.model.javasyntax.expression.InstanceOfExpression;
import org.jd.core.v1.model.javasyntax.expression.IntegerConstantExpression;
import org.jd.core.v1.model.javasyntax.expression.LocalVariableReferenceExpression;
import org.jd.core.v1.model.javasyntax.expression.MethodInvocationExpression;
import org.jd.core.v1.model.javasyntax.expression.NewExpression;
import org.jd.core.v1.model.javasyntax.expression.ParenthesesExpression;
import org.jd.core.v1.model.javasyntax.expression.PreOperatorExpression;
import org.jd.core.v1.model.javasyntax.expression.TernaryOperatorExpression;
import org.jd.core.v1.model.javasyntax.pattern.Pattern;
import org.jd.core.v1.model.javasyntax.pattern.RecordPattern;
import org.jd.core.v1.model.javasyntax.pattern.TypePattern;
import org.jd.core.v1.model.javasyntax.statement.BaseStatement;
import org.jd.core.v1.model.javasyntax.statement.ExpressionStatement;
import org.jd.core.v1.model.javasyntax.statement.IfStatement;
import org.jd.core.v1.model.javasyntax.statement.ReturnExpressionStatement;
import org.jd.core.v1.model.javasyntax.statement.Statement;
import org.jd.core.v1.model.javasyntax.statement.Statements;
import org.jd.core.v1.model.javasyntax.statement.ThrowStatement;
import org.jd.core.v1.model.javasyntax.statement.TryStatement;
import org.jd.core.v1.model.javasyntax.type.ObjectType;
import org.jd.core.v1.model.javasyntax.type.Type;
import org.jd.core.v1.service.converter.classfiletojavasyntax.model.localvariable.LocalVariableReference;
import org.jd.core.v1.util.DefaultList;

public final class RecordPatternInstanceOfRewriter {
    private static final Map<String, String> INVERTED_COMPARISON_OPERATORS = Map.of(
            "==", "!=",
            "!=", "==",
            "<", ">=",
            "<=", ">",
            ">", "<=",
            ">=", "<");

    private RecordPatternInstanceOfRewriter() {}

    public static boolean rewrite(Expression condition, Statements thenStatements, Statements statements, int index) {
        if (!(condition instanceof InstanceOfExpression instanceOfExpression)) {
            return false;
        }

        Type instanceOfType = instanceOfExpression.getInstanceOfType();
        if (instanceOfType == null && instanceOfExpression.getPattern() != null) {
            instanceOfType = instanceOfExpression.getPattern().type();
        }
        if (instanceOfType == null) {
            return false;
        }

        if (thenStatements == null || thenStatements.isEmpty() || index <= 0 || index > statements.size()) {
            return false;
        }

        TryStatement tryStatement = extractSyntheticRecordPatternTryStatement(thenStatements);
        if (tryStatement == null || !isSyntheticRecordPatternTryStatement(tryStatement)) {
            return false;
        }

        boolean hasFollowingFalseReturn = false;
        if (index == statements.size()) {
            hasFollowingFalseReturn = false;
        } else if (index + 1 == statements.size()) {
            Statement followingStatement = statements.get(index);
            if (!followingStatement.isReturnExpressionStatement() || !isFalseExpression(followingStatement.getExpression())) {
                return false;
            }
            hasFollowingFalseReturn = true;
        } else {
            return false;
        }

        BaseStatement tryStatements = tryStatement.getTryStatements();
        Expression guardExpression = normalizeRecordPatternGuardExpression(extractRecordPatternGuardExpression(tryStatements));
        if (guardExpression == null) {
            return false;
        }

        Map<Object, VariableComponentBinding> variableBindings = collectVariableComponentBindings(tryStatements);
        if (variableBindings.isEmpty()) {
            return false;
        }
        renameGuardExpressionVariables(guardExpression, variableBindings);

        Set<String> guardVariableNames = collectLocalVariableNames(guardExpression);
        if (guardVariableNames.isEmpty()) {
            return false;
        }

        List<Pattern> componentPatterns = extractRecordComponentPatterns(tryStatements, variableBindings);
        if (componentPatterns.isEmpty()) {
            return false;
        }

        Pattern recordPattern = new RecordPattern(instanceOfType, componentPatterns, null);
        Expression rewrittenInstanceOf = new InstanceOfExpression(condition.getLineNumber(), instanceOfExpression.getExpression(), recordPattern);
        Expression rewrittenCondition = combineWithLogicalAnd(condition.getLineNumber(), rewrittenInstanceOf, guardExpression);

        if (hasFollowingFalseReturn) {
            statements.subList(index - 1, statements.size()).clear();
        } else {
            statements.remove(index - 1);
        }
        statements.add(new ReturnExpressionStatement(condition.getLineNumber(), rewrittenCondition));

        return true;
    }

    private static TryStatement extractSyntheticRecordPatternTryStatement(Statements thenStatements) {
        if (thenStatements.isEmpty() || !(thenStatements.getLast() instanceof TryStatement tryStatement)) {
            return null;
        }

        for (int i = 0; i < thenStatements.size() - 1; i++) {
            Statement statement = thenStatements.get(i);
            if (!statement.isLocalVariableDeclarationStatement() && !statement.isExpressionStatement()) {
                return null;
            }
        }

        if (thenStatements.size() == 1
                || thenStatements.getFirst().isLocalVariableDeclarationStatement()
                || thenStatements.getFirst().isExpressionStatement()) {
            return tryStatement;
        }

        return null;
    }

    private static Expression unwrapParenthesesExpression(Expression expression) {
        while (expression instanceof ParenthesesExpression parenthesesExpression) {
            expression = parenthesesExpression.getExpression();
        }
        return expression;
    }

    private static Expression normalizeRecordPatternGuardExpression(Expression expression) {
        expression = unwrapParenthesesExpression(expression);

        if (expression instanceof TernaryOperatorExpression ternaryOperatorExpression) {
            Expression trueExpression = unwrapParenthesesExpression(ternaryOperatorExpression.getTrueExpression());
            Expression falseExpression = unwrapParenthesesExpression(ternaryOperatorExpression.getFalseExpression());

            if (isTrueExpression(trueExpression) && isFalseExpression(falseExpression)) {
                expression = unwrapParenthesesExpression(ternaryOperatorExpression.getCondition());
            }
            if (isFalseExpression(trueExpression) && isTrueExpression(falseExpression)) {
                expression = new PreOperatorExpression(expression.getLineNumber(), "!", unwrapParenthesesExpression(ternaryOperatorExpression.getCondition()));
            }
        }

        return normalizeNegatedBooleanExpression(expression);
    }

    private static Expression normalizeNegatedBooleanExpression(Expression expression) {
        expression = unwrapParenthesesExpression(expression);

        if (!(expression instanceof PreOperatorExpression preOperatorExpression) || !"!".equals(preOperatorExpression.getOperator())) {
            return expression;
        }

        return negateBooleanExpression(preOperatorExpression.getExpression(), expression.getLineNumber());
    }

    private static Expression negateBooleanExpression(Expression expression, int lineNumber) {
        expression = unwrapParenthesesExpression(expression);

        if (expression instanceof PreOperatorExpression preOperatorExpression && "!".equals(preOperatorExpression.getOperator())) {
            return unwrapParenthesesExpression(preOperatorExpression.getExpression());
        }

        if (expression instanceof BinaryOperatorExpression binaryOperatorExpression) {
            String operator = binaryOperatorExpression.getOperator();
            if ("&&".equals(operator) || "||".equals(operator)) {
                String combinedOperator = "&&".equals(operator) ? "||" : "&&";
                int priority = "&&".equals(combinedOperator) ? 13 : 14;
                Expression leftExpression = negateBooleanExpression(binaryOperatorExpression.getLeftExpression(), lineNumber);
                Expression rightExpression = negateBooleanExpression(binaryOperatorExpression.getRightExpression(), lineNumber);
                return new BinaryOperatorExpression(lineNumber, TYPE_BOOLEAN, leftExpression, combinedOperator, rightExpression, priority);
            }

            String inverseOperator = INVERTED_COMPARISON_OPERATORS.get(operator);
            if (inverseOperator != null) {
                return new BinaryOperatorExpression(
                        lineNumber,
                        TYPE_BOOLEAN,
                        binaryOperatorExpression.getLeftExpression(),
                        inverseOperator,
                        binaryOperatorExpression.getRightExpression());
            }
        }

        return new PreOperatorExpression(lineNumber, "!", expression);
    }

    private static Expression combineWithLogicalAnd(int lineNumber, Expression leftExpression, Expression rightExpression) {
        List<Expression> operands = new ArrayList<>(4);
        collectLogicalAndOperands(operands, leftExpression);
        collectLogicalAndOperands(operands, rightExpression);

        Expression combinedExpression = operands.get(0);
        for (int i = 1; i < operands.size(); i++) {
            combinedExpression = new BinaryOperatorExpression(lineNumber, TYPE_BOOLEAN, combinedExpression, "&&", operands.get(i), 13);
        }

        return combinedExpression;
    }

    private static void collectLogicalAndOperands(List<Expression> operands, Expression expression) {
        expression = unwrapParenthesesExpression(expression);

        if (expression instanceof BinaryOperatorExpression binaryOperatorExpression && "&&".equals(binaryOperatorExpression.getOperator())) {
            collectLogicalAndOperands(operands, binaryOperatorExpression.getLeftExpression());
            collectLogicalAndOperands(operands, binaryOperatorExpression.getRightExpression());
            return;
        }

        operands.add(expression);
    }

    private static boolean isFalseExpression(Expression expression) {
        if (expression instanceof BooleanExpression booleanExpression) {
            return booleanExpression.isFalse();
        }
        if (expression instanceof IntegerConstantExpression integerConstantExpression) {
            return integerConstantExpression.getIntegerValue() == 0;
        }
        return false;
    }

    private static boolean isTrueExpression(Expression expression) {
        if (expression instanceof BooleanExpression booleanExpression) {
            return booleanExpression.isTrue();
        }
        if (expression instanceof IntegerConstantExpression integerConstantExpression) {
            return integerConstantExpression.getIntegerValue() != 0;
        }
        return false;
    }

    private static boolean isSyntheticRecordPatternTryStatement(TryStatement tryStatement) {
        if (tryStatement.getResources() != null && !tryStatement.getResources().isEmpty() || tryStatement.getFinallyStatements() != null && tryStatement.getFinallyStatements().size() > 0) {
            return false;
        }

        DefaultList<TryStatement.CatchClause> catchClauses = tryStatement.getCatchClauses();
        if (catchClauses == null || catchClauses.size() != 1) {
            return false;
        }

        TryStatement.CatchClause catchClause = catchClauses.getFirst();
        if (catchClause.getType() == null || !"java/lang/Throwable".equals(catchClause.getType().getInternalName())) {
            return false;
        }

        BaseStatement catchStatements = catchClause.getStatements();
        if (catchStatements == null || catchStatements.size() != 1) {
            return false;
        }

        Statement catchStatement = catchStatements.getFirst();
        if (!(catchStatement instanceof ThrowStatement throwStatement)) {
            return false;
        }

        Expression throwExpression = throwStatement.getExpression();
        if (!(throwExpression instanceof NewExpression newExpression)) {
            return false;
        }

        ObjectType objectType = newExpression.getObjectType();
        return objectType != null && "java/lang/MatchException".equals(objectType.getInternalName());
    }

    private static Expression extractRecordPatternGuardExpression(BaseStatement tryStatements) {
        if (tryStatements == null || tryStatements.size() == 0) {
            return null;
        }

        Statement lastTryStatement = tryStatements.getLast();
        if (!(lastTryStatement instanceof ReturnExpressionStatement returnExpressionStatement)) {
            return null;
        }

        Expression returnExpression = returnExpressionStatement.getExpression();
        if (!isFalseExpression(returnExpression)) {
            return returnExpression;
        }

        // Synthetic javac/ECJ rewrites can end with "return false;" and encode the guard
        // as nested "if (...) return true;" blocks.
        return extractConditionThatReturnsTrue(tryStatements);
    }

    private static Expression extractConditionThatReturnsTrue(BaseStatement statements) {
        if (statements == null || statements.size() == 0) {
            return null;
        }

        for (Statement statement : statements) {
            if (!(statement instanceof IfStatement ifStatement)) {
                continue;
            }

            Expression condition = unwrapParenthesesExpression(ifStatement.getCondition());
            BaseStatement thenStatements = ifStatement.getStatements();

            Expression nestedTrueCondition = extractConditionThatReturnsTrue(thenStatements);
            if (nestedTrueCondition != null) {
                if (isTrueExpression(condition)) {
                    return nestedTrueCondition;
                }
                return combineWithLogicalAnd(condition.getLineNumber(), condition, nestedTrueCondition);
            }

            if (containsDirectTrueReturn(thenStatements)) {
                return condition;
            }
        }

        return null;
    }

    private static boolean containsDirectTrueReturn(BaseStatement statements) {
        if (statements == null || statements.size() == 0) {
            return false;
        }

        for (Statement statement : statements) {
            if (statement instanceof ReturnExpressionStatement returnExpressionStatement && isTrueExpression(returnExpressionStatement.getExpression())) {
                return true;
            }
        }

        return false;
    }

    private static Set<String> collectLocalVariableNames(Expression expression) {
        Set<String> variableNames = new LinkedHashSet<>();

        expression.accept(new AbstractJavaSyntaxVisitor() {
            @Override
            public void visit(LocalVariableReferenceExpression expression) {
                if (expression.getName() != null) {
                    variableNames.add(expression.getName());
                }
            }
        });

        return variableNames;
    }

    private static Map<Object, VariableComponentBinding> collectVariableComponentBindings(BaseStatement tryStatements) {
        if (tryStatements == null || tryStatements.size() == 0) {
            return Collections.emptyMap();
        }

        Map<Object, VariableComponentBinding> variableBindings = new LinkedHashMap<>();
        Map<String, Type> accessorComponentTypes = new LinkedHashMap<>();
        collectPatternCandidates(tryStatements, variableBindings, accessorComponentTypes);
        return variableBindings;
    }

    private static void renameGuardExpressionVariables(Expression guardExpression, Map<Object, VariableComponentBinding> variableBindings) {
        if (guardExpression == null || variableBindings == null || variableBindings.isEmpty()) {
            return;
        }

        guardExpression.accept(new AbstractJavaSyntaxVisitor() {
            @Override
            public void visit(LocalVariableReferenceExpression expression) {
                String variableName = expression.getName();
                if (variableName == null) {
                    // continue, we can still resolve by local-variable identity
                }

                VariableComponentBinding variableBinding = variableBindings.get(resolveVariableBindingKey(expression));
                if (variableBinding != null && variableBinding.componentName() != null) {
                    expression.setName(variableBinding.componentName());
                }
            }
        });
    }

    private static List<Pattern> extractRecordComponentPatterns(BaseStatement tryStatements, Map<Object, VariableComponentBinding> variableBindings) {
        if (tryStatements == null || tryStatements.size() == 0 || variableBindings == null || variableBindings.isEmpty()) {
            return Collections.emptyList();
        }

        Map<String, Type> accessorComponentTypes = new LinkedHashMap<>();
        collectPatternCandidates(tryStatements, variableBindings, accessorComponentTypes);

        List<Pattern> componentPatterns = new ArrayList<>();
        Set<String> declaredComponentNames = new LinkedHashSet<>();
        for (Map.Entry<String, Type> accessorComponent : accessorComponentTypes.entrySet()) {
            addTypePattern(componentPatterns, accessorComponent.getValue(), new LocalVariableDeclarator(accessorComponent.getKey()), declaredComponentNames);
        }

        return componentPatterns;
    }

    private static void collectPatternCandidates(
            BaseStatement statements,
            Map<Object, VariableComponentBinding> variableBindings,
            Map<String, Type> accessorComponentTypes) {
        if (statements == null || statements.size() == 0) {
            return;
        }

        for (Statement statement : statements) {
            if (statement instanceof ExpressionStatement expressionStatement) {
                collectPatternCandidatesFromAssignment(expressionStatement.getExpression(), variableBindings, accessorComponentTypes);
                continue;
            }

            if (statement instanceof IfStatement ifStatement) {
                collectPatternCandidates(ifStatement.getStatements(), variableBindings, accessorComponentTypes);
                continue;
            }
        }
    }

    private static void collectPatternCandidatesFromAssignment(
            Expression expression,
            Map<Object, VariableComponentBinding> variableBindings,
            Map<String, Type> accessorComponentTypes) {
        if (!(expression instanceof BinaryOperatorExpression binaryOperatorExpression) || !"=".equals(binaryOperatorExpression.getOperator())) {
            return;
        }

        Expression leftExpression = binaryOperatorExpression.getLeftExpression();
        if (leftExpression == null
                || !leftExpression.isLocalVariableReferenceExpression()) {
            return;
        }
        Object leftBindingKey = resolveVariableBindingKey(leftExpression);
        if (leftBindingKey == null) {
            return;
        }

        VariableComponentBinding binding = extractComponentBinding(
                leftExpression.getType(),
                binaryOperatorExpression.getRightExpression(),
                variableBindings);
        if (binding == null) {
            variableBindings.remove(leftBindingKey);
            return;
        }

        variableBindings.put(leftBindingKey, binding);
        registerComponentType(accessorComponentTypes, binding);
    }

    private static VariableComponentBinding extractComponentBinding(
            Type componentType,
            Expression expression,
            Map<Object, VariableComponentBinding> variableBindings) {
        expression = unwrapParenthesesExpression(expression);

        if (expression instanceof BinaryOperatorExpression binaryOperatorExpression && "=".equals(binaryOperatorExpression.getOperator())) {
            VariableComponentBinding rightBinding =
                    extractComponentBinding(componentType, binaryOperatorExpression.getRightExpression(), variableBindings);
            Expression leftExpression = binaryOperatorExpression.getLeftExpression();
            if (leftExpression != null && leftExpression.isLocalVariableReferenceExpression()) {
                Object leftBindingKey = resolveVariableBindingKey(leftExpression);
                if (leftBindingKey != null) {
                    if (rightBinding == null) {
                        variableBindings.remove(leftBindingKey);
                    } else {
                        Type resolvedType = leftExpression.getType() == null ? rightBinding.componentType() : leftExpression.getType();
                        variableBindings.put(leftBindingKey, new VariableComponentBinding(resolvedType, rightBinding.componentName()));
                    }
                }
            }
            return rightBinding;
        }

        if (expression instanceof LocalVariableReferenceExpression referenceExpression) {
            VariableComponentBinding sourceBinding = variableBindings.get(resolveVariableBindingKey(referenceExpression));
            if (sourceBinding == null) {
                return null;
            }
            Type resolvedType = componentType == null ? sourceBinding.componentType() : componentType;
            return new VariableComponentBinding(resolvedType, sourceBinding.componentName());
        }

        if (expression instanceof MethodInvocationExpression methodInvocationExpression
                && methodInvocationExpression.getName() != null
                && (methodInvocationExpression.getParameters() == null || methodInvocationExpression.getParameters().size() == 0)) {
            return new VariableComponentBinding(componentType, methodInvocationExpression.getName());
        }

        return null;
    }

    private static Object resolveVariableBindingKey(Object candidate) {
        if (candidate instanceof LocalVariableReference localVariableReference) {
            return localVariableReference.getLocalVariable();
        }
        if (candidate instanceof Expression expression) {
            return expression.getName();
        }
        if (candidate instanceof LocalVariableDeclarator localVariableDeclarator) {
            return localVariableDeclarator.getName();
        }
        return null;
    }

    private static void registerComponentType(Map<String, Type> accessorComponentTypes, VariableComponentBinding binding) {
        if (binding == null || binding.componentName() == null || binding.componentType() == null) {
            return;
        }
        accessorComponentTypes.putIfAbsent(binding.componentName(), binding.componentType());
    }

    private static void addTypePattern(
            List<Pattern> componentPatterns,
            Type componentType,
            LocalVariableDeclarator declarator,
            Set<String> declaredComponentNames) {
        if (declarator == null || declarator.getName() == null || !declaredComponentNames.add(declarator.getName())) {
            return;
        }
        componentPatterns.add(new TypePattern(componentType, declarator.getName()));
    }

    private record VariableComponentBinding(Type componentType, String componentName) {
    }
}
