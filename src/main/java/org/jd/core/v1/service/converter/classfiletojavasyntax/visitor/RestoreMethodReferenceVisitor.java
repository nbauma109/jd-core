/*
 * Copyright (c) 2026 Nicolas Baumann (@nbauma109), with Claude Sonnet 5.5.
 * This project is distributed under the GPLv3 license.
 * This is a Copyleft license that gives the user the right to use,
 * copy and modify the code freely for non-commercial purposes.
 */

package org.jd.core.v1.service.converter.classfiletojavasyntax.visitor;

import org.jd.core.v1.model.javasyntax.expression.BaseExpression;
import org.jd.core.v1.model.javasyntax.expression.Expression;
import org.jd.core.v1.model.javasyntax.expression.LambdaIdentifiersExpression;
import org.jd.core.v1.model.javasyntax.expression.ObjectTypeReferenceExpression;
import org.jd.core.v1.model.javasyntax.statement.BaseStatement;
import org.jd.core.v1.model.javasyntax.statement.Statement;
import org.jd.core.v1.model.javasyntax.statement.Statements;
import org.jd.core.v1.service.converter.classfiletojavasyntax.model.javasyntax.expression.ClassFileMethodInvocationExpression;
import org.jd.core.v1.service.converter.classfiletojavasyntax.model.javasyntax.expression.ClassFileMethodReferenceExpression;
import org.jd.core.v1.service.converter.classfiletojavasyntax.model.javasyntax.expression.ClassFileMethodReferenceLambdaExpression;
import org.jd.core.v1.service.converter.classfiletojavasyntax.model.javasyntax.expression.ClassFileLocalVariableReferenceExpression;

import org.jd.core.v1.service.converter.classfiletojavasyntax.util.Utils;

import java.util.List;
import java.util.stream.IntStream;
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

    private static Expression toMethodReference(LambdaIdentifiersExpression lambda) {
        // A lambda which javac made of a method reference with a receiver captures it in 'rec$': no other lambda is known to be one. The lambda of a static method
        // ('Bag::remember') has no receiver to evaluate, and only the names of its parameters, 'x$0', 'x$1'..., tell it from a lambda of the source.
        boolean capturesReceiver = lambda instanceof ClassFileMethodReferenceLambdaExpression;
        List<String> names = lambda.getParameterNames();
        Statement statement = singleStatement(lambda.getStatements());

        if (!capturesReceiver && !hasSyntheticParameterNames(names) || statement == null
                || !(statement.getExpression() instanceof ClassFileMethodInvocationExpression call)
                // A method reference with a receiver is one whatever the method: its arguments are the parameters of the lambda, an array for a varargs method
                || !capturesReceiver && (call.isVarArgs() || !isTypeOrThis(call.getExpression()))
                || !callsWithTheParameters(call, names)) {
            return null;
        }
        return new ClassFileMethodReferenceExpression(lambda.getLineNumber(), lambda.getType(), call.getExpression(), call.getInternalTypeName(), call.getName(), call.getDescriptor(), null);
    }

    private static boolean hasSyntheticParameterNames(List<String> names) {
        return !names.isEmpty() && names.stream().allMatch(name -> SYNTHETIC_PARAMETER_NAME.matcher(name).matches());
    }

    /** A receiver which the lambda evaluates at each call would not be evaluated once, where the method reference is: only a type ('Bag::remember') or 'this' is not */
    private static boolean isTypeOrThis(Expression receiver) {
        return receiver instanceof ObjectTypeReferenceExpression || receiver.isThisExpression();
    }

    /** The arguments of the call are the parameters of the lambda, in the same order */
    private static boolean callsWithTheParameters(ClassFileMethodInvocationExpression call, List<String> names) {
        BaseExpression arguments = call.getParameters();

        if (names.isEmpty()) {
            return Utils.isEmpty(arguments);
        }
        return arguments != null && arguments.size() == names.size()
                && IntStream.range(0, names.size()).allMatch(i -> (arguments.isList() ? arguments.getList().get(i) : arguments.getFirst()) instanceof ClassFileLocalVariableReferenceExpression reference
                        && names.get(i).equals(reference.getName()));
    }

    /** @return the statement of a lambda which has one only, null otherwise */
    public static Statement singleStatement(BaseStatement baseStatement) {
        if (baseStatement instanceof Statements list) {
            return list.size() == 1 ? list.getFirst() : null;
        }
        return baseStatement instanceof Statement statement ? statement : null;
    }
}
