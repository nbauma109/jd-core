/*
 * Copyright (c) 2026 Nicolas Baumann (@nbauma109), with Claude Sonnet 5.5.
 * This project is distributed under the GPLv3 license.
 * This is a Copyleft license that gives the user the right to use,
 * copy and modify the code freely for non-commercial purposes.
 */
package org.jd.core.v1.service.converter.classfiletojavasyntax.visitor;

import org.jd.core.v1.api.loader.Loader;
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
import org.jd.core.v1.service.converter.classfiletojavasyntax.util.ClassMetadata;

import java.util.ArrayList;
import java.util.List;
import java.util.ListIterator;
import java.util.function.Function;

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

    /** The variable which holds a component and the name of the accessor which gives it */
    private record Component(String variable, String accessor) {}

    /** The names of the components of a record, in the order of its declaration, none when the type is no record */
    private final Function<String, List<String>> recordComponentNames;

    public RecordPatternIfVisitor(Loader loader) {
        this(internalName -> ClassMetadata.recordComponentNames(loader, internalName));
    }

    public RecordPatternIfVisitor(Function<String, List<String>> recordComponentNames) {
        this.recordComponentNames = recordComponentNames;
    }

    @Override
    public void visit(Statements statements) {
        ListIterator<Statement> iterator = statements.listIterator();

        while (iterator.hasNext()) {
            Statement statement = iterator.next();

            if (statement instanceof IfStatement ifStatement) {
                IfStatement rewritten = rewrite(ifStatement);

                if (rewritten != null) {
                    iterator.set(rewritten);
                }
            }
            iterator.previous().accept(this);
            iterator.next();
        }
    }

    private IfStatement rewrite(IfStatement ifStatement) {
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
        List<String> accessors = new ArrayList<>();
        int index = 0;

        while (index < tryBody.size() && tryBody.get(index) instanceof LocalVariableDeclarationStatement declaration) {
            Component component = component(declaration, recordVariable);

            if (component == null) {
                break;
            }
            components.add(new TypePattern(declaration.getType(), component.variable()));
            accessors.add(component.accessor());
            index++;
        }

        // The accessors are the ones of all the components of the record, in the order of its declaration
        List<String> names = recordComponentNames.apply(instanceOf.getInstanceOfType().getInternalName());

        if (components.isEmpty() || !accessors.equals(names)) {
            return null;
        }

        Statements body = new Statements();

        body.addAll(tryBody.subList(index, tryBody.size()));

        Expression condition = new InstanceOfExpression(instanceOf.getLineNumber(), instanceOf.getExpression(),
                new RecordPattern(instanceOf.getInstanceOfType(), components, null));

        return ifStatement instanceof IfElseStatement ifElse ? new IfElseStatement(condition, body, ifElse.getElseStatements()) : new IfStatement(condition, body);
    }

    /** @return the name of the variable and the name of the accessor of the component which are bound by 'T tmp = record.accessor(), name = tmp;', null if it is no binding */
    private static Component component(LocalVariableDeclarationStatement declaration, String recordVariable) {
        List<LocalVariableDeclarator> declarators = new ArrayList<>();

        if (declaration.getLocalVariableDeclarators() instanceof LocalVariableDeclarator single) {
            declarators.add(single);
        } else {
            declaration.getLocalVariableDeclarators().forEach(declarators::add);
        }
        if (declarators.isEmpty() || declarators.size() > 2 || !isAccessorCall(declarators.get(0), recordVariable)) {
            return null;
        }
        String accessor = ((MethodInvocationExpression) ((ExpressionVariableInitializer) declarators.get(0).getVariableInitializer()).getExpression()).getName();

        if (declarators.size() == 1) {
            return new Component(declarators.get(0).getName(), accessor);
        }
        return isSameExpression(declarators.get(1), declarators.get(0).getName()) ? new Component(declarators.get(1).getName(), accessor) : null;
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
