/*
 * Copyright (c) 2026 Nicolas Baumann (@nbauma109), with Claude Sonnet 5.5.
 * This project is distributed under the GPLv3 license.
 * This is a Copyleft license that gives the user the right to use,
 * copy and modify the code freely for non-commercial purposes.
 */
package org.jd.core.v1.service.converter.classfiletojavasyntax.visitor;

import org.jd.core.v1.model.javasyntax.expression.Expression;
import org.jd.core.v1.model.javasyntax.expression.InstanceOfExpression;
import org.jd.core.v1.model.javasyntax.expression.SwitchExpression;
import org.jd.core.v1.model.javasyntax.pattern.TypePattern;
import org.jd.core.v1.model.javasyntax.statement.SwitchStatement;
import org.jd.core.v1.service.converter.classfiletojavasyntax.model.javasyntax.expression.ClassFileInstanceOfPatternExpression;

import java.util.ArrayList;
import java.util.List;

/** Gives its name to the variable of 'o instanceof T v' and of 'case T v', once the local variables are named */
public class ResolveInstanceOfPatternVisitor extends AbstractUpdateExpressionVisitor {

    @Override
    protected Expression updateExpression(Expression expression) {
        if (expression instanceof ClassFileInstanceOfPatternExpression pattern && pattern.getLocalVariable() != null && pattern.getLocalVariable().getName() != null) {
            return new InstanceOfExpression(pattern.getLineNumber(), pattern.getExpression(), pattern.getInstanceOfType(), pattern.getLocalVariable().getName(), false);
        }
        if (expression instanceof SwitchExpression switchExpression && switchExpression.getRules().stream().anyMatch(rule -> hasUnresolvedLabel(rule.getLabels()))) {
            return resolve(switchExpression);
        }
        return expression;
    }

    @Override
    public void visit(SwitchStatement statement) {
        List<SwitchStatement.Block> blocks = statement.getBlocks();

        for (int i = 0; i < blocks.size(); i++) {
            blocks.set(i, resolve(blocks.get(i)));
        }
        super.visit(statement);
    }

    private static SwitchStatement.Block resolve(SwitchStatement.Block block) {
        if (block instanceof SwitchStatement.LabelBlock labelBlock && labelBlock.getLabel() instanceof SwitchStatement.ExpressionLabel label
                && label.getExpression() instanceof ClassFileInstanceOfPatternExpression pattern) {
            return new SwitchStatement.LabelBlock(new SwitchStatement.PatternLabel(pattern(pattern)), labelBlock.getWhenCondition(), labelBlock.getStatements());
        }
        if (block instanceof SwitchStatement.MultiLabelsBlock multiLabelsBlock && multiLabelsBlock.getLabels().stream().anyMatch(EXPRESSION_PATTERN)) {
            List<SwitchStatement.Label> labels = new ArrayList<>();

            for (SwitchStatement.Label label : multiLabelsBlock.getLabels()) {
                labels.add(label instanceof SwitchStatement.ExpressionLabel expressionLabel && expressionLabel.getExpression() instanceof ClassFileInstanceOfPatternExpression pattern
                        ? new SwitchStatement.PatternLabel(pattern(pattern)) : label);
            }
            return new SwitchStatement.MultiLabelsBlock(labels, multiLabelsBlock.getWhenCondition(), multiLabelsBlock.getStatements());
        }
        return block;
    }

    private static final java.util.function.Predicate<SwitchStatement.Label> EXPRESSION_PATTERN =
            label -> label instanceof SwitchStatement.ExpressionLabel expressionLabel && expressionLabel.getExpression() instanceof ClassFileInstanceOfPatternExpression;

    private static boolean hasUnresolvedLabel(List<SwitchExpression.Label> labels) {
        return labels.stream().anyMatch(label -> label instanceof SwitchExpression.ExpressionLabel expressionLabel
                && expressionLabel.getExpression() instanceof ClassFileInstanceOfPatternExpression);
    }

    private static SwitchExpression resolve(SwitchExpression switchExpression) {
        List<SwitchExpression.Rule> rules = new ArrayList<>();

        for (SwitchExpression.Rule rule : switchExpression.getRules()) {
            List<SwitchExpression.Label> labels = new ArrayList<>();

            for (SwitchExpression.Label label : rule.getLabels()) {
                labels.add(label instanceof SwitchExpression.ExpressionLabel expressionLabel && expressionLabel.getExpression() instanceof ClassFileInstanceOfPatternExpression pattern
                        ? new SwitchExpression.PatternLabel(pattern(pattern)) : label);
            }

            if (rule instanceof SwitchExpression.RuleExpression ruleExpression) {
                rules.add(new SwitchExpression.RuleExpression(labels, ruleExpression.getWhenCondition(), ruleExpression.getExpression()));
            } else if (rule instanceof SwitchExpression.RuleStatement ruleStatement) {
                rules.add(new SwitchExpression.RuleStatement(labels, ruleStatement.getWhenCondition(), ruleStatement.getStatements()));
            } else {
                rules.add(rule);
            }
        }
        return new SwitchExpression(switchExpression.getLineNumber(), switchExpression.getSelector(), rules, switchExpression.getType());
    }

    private static TypePattern pattern(ClassFileInstanceOfPatternExpression expression) {
        String name = expression.getLocalVariable() != null && expression.getLocalVariable().getName() != null ? expression.getLocalVariable().getName() : "obj";
        return new TypePattern(expression.getInstanceOfType(), name);
    }
}
