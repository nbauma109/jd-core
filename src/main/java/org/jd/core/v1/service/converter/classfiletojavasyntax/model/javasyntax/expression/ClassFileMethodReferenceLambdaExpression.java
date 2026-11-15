/*
 * Copyright (c) 2026 Nicolas Baumann (@nbauma109), with Claude Sonnet 5.5.
 * This project is distributed under the GPLv3 license.
 * This is a Copyleft license that gives the user the right to use,
 * copy and modify the code freely for non-commercial purposes.
 */

package org.jd.core.v1.service.converter.classfiletojavasyntax.model.javasyntax.expression;

import org.jd.core.v1.model.javasyntax.expression.Expression;
import org.jd.core.v1.model.javasyntax.expression.LambdaIdentifiersExpression;
import org.jd.core.v1.model.javasyntax.statement.BaseStatement;
import org.jd.core.v1.model.javasyntax.type.Type;

import java.util.List;

/**
 * The synthetic lambda which javac makes of a method reference with a receiver ('bag::add' is 'lambda$m$0(Bag rec$, Object x$0) { rec$.add(x$0); }'): its
 * receiver is the one which the call site captured, and is evaluated once. Only a lambda which captures the receiver in the parameter 'rec$' of javac is one:
 * the lambdas of the source are never method references, whatever the names of their parameters.
 */
public class ClassFileMethodReferenceLambdaExpression extends LambdaIdentifiersExpression {

    public ClassFileMethodReferenceLambdaExpression(int lineNumber, Type type, Type returnedType, List<String> parameterNames, BaseStatement statements) {
        super(lineNumber, type, returnedType, parameterNames, statements);
    }

    @Override
    public Expression copyTo(int lineNumber) {
        ClassFileMethodReferenceLambdaExpression copy = new ClassFileMethodReferenceLambdaExpression(lineNumber, getType(), getReturnedType(), getParameterNames(), getStatements());

        copy.setParameterTypes(getParameterTypes());
        copy.setExplicitParameterTypes(isExplicitParameterTypes());
        return copy;
    }
}
