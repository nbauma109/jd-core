/*
 * Copyright (c) 2026 Nicolas Baumann (@nbauma109), with Claude Sonnet 5.5.
 * This project is distributed under the GPLv3 license.
 * This is a Copyleft license that gives the user the right to use,
 * copy and modify the code freely for non-commercial purposes.
 */
package org.jd.core.v1.service.converter.classfiletojavasyntax.visitor;

import org.jd.core.v1.loader.ClassPathLoader;
import org.jd.core.v1.model.javasyntax.expression.IntegerConstantExpression;
import org.jd.core.v1.model.javasyntax.expression.LocalVariableReferenceExpression;
import org.jd.core.v1.model.javasyntax.expression.SwitchExpression;
import org.jd.core.v1.model.javasyntax.statement.ReturnExpressionStatement;
import org.jd.core.v1.model.javasyntax.statement.Statements;
import org.jd.core.v1.model.javasyntax.statement.YieldExpressionStatement;
import org.jd.core.v1.model.javasyntax.type.ObjectType;
import org.jd.core.v1.model.javasyntax.type.PrimitiveType;
import org.jd.core.v1.service.converter.classfiletojavasyntax.model.javasyntax.expression.ClassFileInstanceOfPatternExpression;
import org.jd.core.v1.service.converter.classfiletojavasyntax.model.localvariable.ObjectLocalVariable;
import org.jd.core.v1.service.converter.classfiletojavasyntax.util.TypeMaker;
import org.junit.Test;

import java.util.List;

import static org.junit.Assert.assertTrue;

public class ResolveInstanceOfPatternVisitorTest {
    private static final ObjectType STRING = new ObjectType("java/lang/String", "java.lang.String", "String");

    /** 'case String s -> { yield switch (b) { case String t -> 1; default -> 0; }; }': the switch which a rule yields is resolved like the one which a rule gives */
    @Test
    public void testPatternSwitchYieldedByARule() {
        LocalVariableReferenceExpression selector = new LocalVariableReferenceExpression(ObjectType.TYPE_OBJECT, "b");
        ObjectLocalVariable variable = new ObjectLocalVariable(new TypeMaker(new ClassPathLoader()), 2, 0, STRING, "t");
        SwitchExpression inner = new SwitchExpression(0, selector, List.of(
                new SwitchExpression.RuleExpression(List.of(new SwitchExpression.ExpressionLabel(new ClassFileInstanceOfPatternExpression(0, selector, STRING, variable))),
                        new IntegerConstantExpression(1)),
                new SwitchExpression.RuleExpression(List.of(SwitchExpression.DEFAULT_LABEL), new IntegerConstantExpression(0))), PrimitiveType.TYPE_INT);
        YieldExpressionStatement yield = new YieldExpressionStatement(inner);
        SwitchExpression outer = new SwitchExpression(0, new LocalVariableReferenceExpression(ObjectType.TYPE_OBJECT, "a"), List.of(
                new SwitchExpression.RuleStatement(List.of(SwitchExpression.DEFAULT_LABEL), yield)), PrimitiveType.TYPE_INT);
        Statements statements = new Statements();

        statements.add(new ReturnExpressionStatement(outer));
        new ResolveInstanceOfPatternVisitor().visit(statements);

        SwitchExpression resolved = (SwitchExpression) yield.getExpression();

        assertTrue(resolved.getRules().get(0).getLabels().get(0) instanceof SwitchExpression.PatternLabel);
    }
}
