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
import org.jd.core.v1.model.javasyntax.expression.BaseExpression;
import org.jd.core.v1.model.javasyntax.expression.Expression;
import org.jd.core.v1.model.javasyntax.expression.LambdaIdentifiersExpression;
import org.jd.core.v1.model.javasyntax.expression.LocalVariableReferenceExpression;
import org.jd.core.v1.model.javasyntax.expression.MethodReferenceExpression;
import org.jd.core.v1.model.javasyntax.expression.ObjectTypeReferenceExpression;
import org.jd.core.v1.model.javasyntax.statement.BaseStatement;
import org.jd.core.v1.model.javasyntax.statement.ExpressionStatement;
import org.jd.core.v1.model.javasyntax.statement.LambdaExpressionStatement;
import org.jd.core.v1.model.javasyntax.statement.LocalVariableDeclarationStatement;
import org.jd.core.v1.model.javasyntax.statement.ReturnExpressionStatement;
import org.jd.core.v1.model.javasyntax.statement.Statement;
import org.jd.core.v1.model.javasyntax.statement.Statements;
import org.jd.core.v1.service.converter.classfiletojavasyntax.model.javasyntax.expression.ClassFileMethodInvocationExpression;
import org.jd.core.v1.service.converter.classfiletojavasyntax.model.javasyntax.expression.ClassFileMethodReferenceExpression;
import org.jd.core.v1.service.converter.classfiletojavasyntax.model.javasyntax.expression.ClassFileLocalVariableReferenceExpression;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Pattern;

/**
 * When the method which a method reference designates cannot be called from the class which javac emits the call site in (a private member of
 * an inner class, with a class file older than the nests of Java 11), javac compiles 'bag::add' into a synthetic method
 * 'lambda$m$0(TinyBag rec$, Object x$0) { rec$.add(x$0); }' which captures the receiver. Only the names of its parameters, 'x$0', 'x$1'...,
 * tell it from a lambda of the source (the lambdas of the source have the names of the source): the method reference is restored.
 * <p>
 * This runs once the calls of the accessors ('TinyBag.access$700(rec$, x$0)') are replaced by the calls to the members.
 */
public class RestoreMethodReferenceVisitor extends AbstractUpdateExpressionVisitor {

    private static final Pattern SYNTHETIC_PARAMETER_NAME = Pattern.compile("x\\$\\d+");

    /** The variables which javac made for the receivers of the method references restored: 'TinyBag tinyBag1 = bag;' before 'tinyBag1::add' */
    private final Set<String> receiverVariables = new HashSet<>();

    @Override
    protected Expression updateExpression(Expression expression) {
        if (expression instanceof LambdaIdentifiersExpression lambda) {
            Expression methodReference = toMethodReference(lambda);

            if (methodReference != null) {
                return methodReference;
            }
        }
        return expression;
    }

    @Override
    public void visit(Statements statements) {
        super.visit(statements);
        // The receiver is evaluated where the method reference is, and only there: 'tinyBag1 = bag; forEach(tinyBag1::add)' is 'forEach(bag::add)'
        int index = statements.size() - 1;

        while (index > 0) {
            if (isVariableOfReceiver(statements, index - 1) && statements.get(index - 1) instanceof LocalVariableDeclarationStatement declaration
                    && declaration.getLocalVariableDeclarators() instanceof LocalVariableDeclarator declarator
                    && declarator.getVariableInitializer() instanceof ExpressionVariableInitializer initializer) {
                ReceiverReplacer replacer = new ReceiverReplacer(declarator.getName(), initializer.getExpression());

                for (int j = index; j < statements.size(); j++) {
                    statements.get(j).accept(replacer);
                }
                statements.remove(index - 1);
                // The statement before the variable is not the variable of a receiver: it may have the same name as the one folded
                index -= 2;
            } else {
                index--;
            }
        }
    }

    /** 'TinyBag tinyBag1 = bag;' of the receiver of a method reference restored, which is not used anywhere else than by this method reference */
    private boolean isVariableOfReceiver(Statements statements, int index) {
        return statements.get(index) instanceof LocalVariableDeclarationStatement declaration
                && declaration.getLocalVariableDeclarators() instanceof LocalVariableDeclarator declarator
                && declarator.getDimension() == 0
                && receiverVariables.contains(declarator.getName())
                && declarator.getVariableInitializer() instanceof ExpressionVariableInitializer
                && isReferencedOnlyAsReceiver(statements, index + 1, declarator.getName());
    }

    private static boolean isReferencedOnlyAsReceiver(Statements statements, int from, String name) {
        ReferenceCounter counter = new ReferenceCounter(name);

        for (int i = from; i < statements.size(); i++) {
            statements.get(i).accept(counter);
        }
        return counter.references == 1 && counter.receivers == 1;
    }

    private Expression toMethodReference(LambdaIdentifiersExpression lambda) {
        List<String> names = lambda.getParameterNames();

        if (!hasSyntheticParameterNames(names)) {
            return null;
        }
        Statement statement = singleStatement(lambda.getStatements());

        if (!(statement instanceof ExpressionStatement || statement instanceof ReturnExpressionStatement || statement instanceof LambdaExpressionStatement)
                || !(statement.getExpression() instanceof ClassFileMethodInvocationExpression call) || call.isVarArgs()
                || !isReceiverOfMethodReference(call.getExpression(), names) || !callsWithTheParameters(call, names)) {
            return null;
        }
        if (call.getExpression() instanceof ClassFileLocalVariableReferenceExpression reference && reference.getName() != null) {
            receiverVariables.add(reference.getName());
        }
        return new ClassFileMethodReferenceExpression(lambda.getLineNumber(), lambda.getType(), call.getExpression(), call.getInternalTypeName(), call.getName(), call.getDescriptor(), null);
    }

    private static boolean hasSyntheticParameterNames(List<String> names) {
        return names != null && !names.isEmpty() && names.stream().allMatch(name -> name != null && SYNTHETIC_PARAMETER_NAME.matcher(name).matches());
    }

    /** A type ('Bag::remember'), 'this' or a variable which the lambda captures: not a parameter of the lambda, and not the implicit 'this' */
    private static boolean isReceiverOfMethodReference(Expression receiver, List<String> names) {
        if (receiver instanceof ClassFileLocalVariableReferenceExpression reference) {
            return !names.contains(reference.getName());
        }
        return receiver instanceof ObjectTypeReferenceExpression || receiver.isThisExpression();
    }

    /** The arguments of the call are the parameters of the lambda, in the same order */
    private static boolean callsWithTheParameters(ClassFileMethodInvocationExpression call, List<String> names) {
        BaseExpression arguments = call.getParameters();

        if ((arguments == null ? 0 : arguments.size()) != names.size()) {
            return false;
        }
        for (int i = 0; i < names.size(); i++) {
            Expression argument = arguments.isList() ? arguments.getList().get(i) : arguments.getFirst();

            if (!(argument instanceof ClassFileLocalVariableReferenceExpression reference) || !names.get(i).equals(reference.getName())) {
                return false;
            }
        }
        return true;
    }

    /** Counts the references to a variable, and the ones which are the receiver of a method reference */
    private static final class ReferenceCounter extends AbstractJavaSyntaxVisitor {
        private final String name;
        private int references;
        private int receivers;

        private ReferenceCounter(String name) {
            this.name = name;
        }

        @Override
        public void visit(LocalVariableReferenceExpression expression) {
            if (name.equals(expression.getName())) {
                references++;
            }
        }

        @Override
        public void visit(MethodReferenceExpression expression) {
            if (expression.getExpression() instanceof LocalVariableReferenceExpression reference && name.equals(reference.getName())) {
                receivers++;
            }
            super.visit(expression);
        }
    }

    private static final class ReceiverReplacer extends AbstractUpdateExpressionVisitor {
        private final String name;
        private final Expression replacement;

        private ReceiverReplacer(String name, Expression replacement) {
            this.name = name;
            this.replacement = replacement;
        }

        @Override
        protected Expression updateExpression(Expression expression) {
            return expression instanceof LocalVariableReferenceExpression reference && name.equals(reference.getName()) ? replacement : expression;
        }
    }

    private static Statement singleStatement(BaseStatement baseStatement) {
        if (baseStatement instanceof Statements list) {
            return list.size() == 1 ? list.getFirst() : null;
        }
        return baseStatement instanceof Statement statement ? statement : null;
    }
}
