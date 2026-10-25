/*
 * Copyright (c) 2026 Nicolas Baumann (@nbauma109), with Claude Sonnet 5.5.
 * This project is distributed under the GPLv3 license.
 * This is a Copyleft license that gives the user the right to use,
 * copy and modify the code freely for non-commercial purposes.
 */
package org.jd.core.v1.service.converter.classfiletojavasyntax.visitor;

import org.jd.core.v1.model.javasyntax.AbstractJavaSyntaxVisitor;
import org.jd.core.v1.model.javasyntax.declaration.ExpressionVariableInitializer;
import org.jd.core.v1.model.javasyntax.declaration.LocalVariableDeclarator;
import org.jd.core.v1.model.javasyntax.expression.Expression;
import org.jd.core.v1.model.javasyntax.expression.InstanceOfExpression;
import org.jd.core.v1.model.javasyntax.expression.LocalVariableReferenceExpression;
import org.jd.core.v1.model.javasyntax.expression.MethodInvocationExpression;
import org.jd.core.v1.model.javasyntax.expression.NewExpression;
import org.jd.core.v1.model.javasyntax.pattern.Pattern;
import org.jd.core.v1.model.javasyntax.pattern.RecordPattern;
import org.jd.core.v1.model.javasyntax.pattern.TypePattern;
import org.jd.core.v1.model.javasyntax.statement.IfElseStatement;
import org.jd.core.v1.model.javasyntax.statement.IfStatement;
import org.jd.core.v1.model.javasyntax.statement.LocalVariableDeclarationStatement;
import org.jd.core.v1.model.javasyntax.statement.Statement;
import org.jd.core.v1.model.javasyntax.statement.Statements;
import org.jd.core.v1.model.javasyntax.statement.ThrowStatement;
import org.jd.core.v1.model.javasyntax.statement.TryStatement;
import org.jd.core.v1.model.javasyntax.type.Type;

import java.util.ArrayList;
import java.util.List;
import java.util.ListIterator;

/**
 * javac compiles 'if (r instanceof R(int x, Object o)) { body }' to
 * <pre>
 * if (r instanceof R) {
 *     R r1 = r;
 *     try {
 *         int i = r1.x(), x = i;
 *         Object o1 = r1.o(), o = o1;
 *         body
 *     } catch (Throwable e) {
 *         throw new MatchException(e.toString(), e);
 *     }
 * }</pre>
 * which is turned back to the record pattern.
 */
public class RecordPatternIfVisitor extends AbstractJavaSyntaxVisitor {
    private static final String MATCH_EXCEPTION = "java/lang/MatchException";
    private static final String THROWABLE = "java/lang/Throwable";

    @Override
    public void visit(Statements statements) {
        ListIterator<Statement> iterator = statements.listIterator();

        while (iterator.hasNext()) {
            Statement statement = iterator.next();

            if (statement instanceof IfStatement ifStatement && !(statement instanceof IfElseStatement)) {
                IfStatement rewritten = rewrite(ifStatement);

                if (rewritten != null) {
                    iterator.set(rewritten);
                }
            }
            iterator.previous().accept(this);
            iterator.next();
        }
    }

    private static IfStatement rewrite(IfStatement ifStatement) {
        if (!(ifStatement.getCondition() instanceof InstanceOfExpression instanceOf) || !(ifStatement.getStatements() instanceof Statements thenStatements)
                || thenStatements.isEmpty() || !(thenStatements.getLast() instanceof TryStatement tryStatement) || !isMatchExceptionTry(tryStatement)
                || !(tryStatement.getTryStatements() instanceof Statements tryBody)) {
            return null;
        }

        // The variable which holds the record: the variable of 'r instanceof R r1' or 'R r1 = r;'
        String recordVariable;

        if (instanceOf.getPattern() instanceof TypePattern typePattern && typePattern.variableName() != null && thenStatements.size() == 1) {
            recordVariable = typePattern.variableName();
        } else if (thenStatements.size() == 2 && thenStatements.getFirst() instanceof LocalVariableDeclarationStatement declaration
                && declaration.getLocalVariableDeclarators() instanceof LocalVariableDeclarator declarator && isSameExpression(declarator, instanceOf.getExpression())) {
            recordVariable = declarator.getName();
        } else {
            return null;
        }

        // 'int i = r1.x(), x = i;': one component each
        List<Pattern> components = new ArrayList<>();
        int index = 0;

        while (index < tryBody.size() && tryBody.get(index) instanceof LocalVariableDeclarationStatement declaration) {
            String name = componentName(declaration, recordVariable);

            if (name == null) {
                break;
            }
            components.add(new TypePattern(declaration.getType(), name));
            index++;
        }
        if (components.isEmpty()) {
            return null;
        }

        Statements body = new Statements();

        body.addAll(tryBody.subList(index, tryBody.size()));
        return new IfStatement(new InstanceOfExpression(instanceOf.getLineNumber(), instanceOf.getExpression(),
                new RecordPattern(instanceOf.getInstanceOfType(), components, null)), body);
    }

    /** @return the name of the component which is bound by the declaration of 'T tmp = record.accessor(), name = tmp;', null if it is no binding */
    private static String componentName(LocalVariableDeclarationStatement declaration, String recordVariable) {
        List<LocalVariableDeclarator> declarators = new ArrayList<>();

        if (declaration.getLocalVariableDeclarators() instanceof LocalVariableDeclarator single) {
            declarators.add(single);
        } else {
            declaration.getLocalVariableDeclarators().forEach(d -> declarators.add((LocalVariableDeclarator) d));
        }
        if (declarators.isEmpty() || declarators.size() > 2 || !isAccessorCall(declarators.get(0), recordVariable)) {
            return null;
        }
        if (declarators.size() == 1) {
            return declarators.get(0).getName();
        }
        return isSameExpression(declarators.get(1), declarators.get(0).getName()) ? declarators.get(1).getName() : null;
    }

    private static boolean isAccessorCall(LocalVariableDeclarator declarator, String recordVariable) {
        return declarator.getVariableInitializer() instanceof ExpressionVariableInitializer initializer
                && initializer.getExpression() instanceof MethodInvocationExpression call
                && call.getExpression() instanceof LocalVariableReferenceExpression reference && recordVariable.equals(reference.getName())
                && (call.getParameters() == null || call.getParameters().size() == 0);
    }

    private static boolean isSameExpression(LocalVariableDeclarator declarator, Expression expression) {
        return declarator.getVariableInitializer() instanceof ExpressionVariableInitializer initializer && sameVariable(initializer.getExpression(), expression);
    }

    private static boolean isSameExpression(LocalVariableDeclarator declarator, String variableName) {
        return declarator.getVariableInitializer() instanceof ExpressionVariableInitializer initializer
                && initializer.getExpression() instanceof LocalVariableReferenceExpression reference && variableName.equals(reference.getName());
    }

    private static boolean sameVariable(Expression first, Expression second) {
        return first instanceof LocalVariableReferenceExpression a && second instanceof LocalVariableReferenceExpression b && a.getName() != null && a.getName().equals(b.getName());
    }

    /** 'catch (Throwable e) { throw new MatchException(e.toString(), e); }' */
    private static boolean isMatchExceptionTry(TryStatement tryStatement) {
        if (tryStatement.getFinallyStatements() != null || tryStatement.getResources() != null && !tryStatement.getResources().isEmpty()
                || tryStatement.getCatchClauses() == null || tryStatement.getCatchClauses().size() != 1) {
            return false;
        }

        TryStatement.CatchClause catchClause = tryStatement.getCatchClauses().get(0);
        Type type = catchClause.getType();

        return type != null && THROWABLE.equals(type.getInternalName()) && catchClause.getStatements() instanceof Statements statements && statements.size() == 1
                && statements.getFirst() instanceof ThrowStatement throwStatement && throwStatement.getExpression() instanceof NewExpression newExpression
                && MATCH_EXCEPTION.equals(newExpression.getObjectType().getInternalName());
    }
}
