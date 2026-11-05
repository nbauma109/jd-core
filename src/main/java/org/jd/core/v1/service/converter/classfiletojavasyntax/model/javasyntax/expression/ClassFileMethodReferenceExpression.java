/*
 * Copyright (c) 2026 Nicolas Baumann (@nbauma109), with Claude Sonnet 5.5.
 * This project is distributed under the GPLv3 license.
 * This is a Copyleft license that gives the user the right to use,
 * copy and modify the code freely for non-commercial purposes.
 */

package org.jd.core.v1.service.converter.classfiletojavasyntax.model.javasyntax.expression;

import org.jd.core.v1.model.javasyntax.expression.Expression;
import org.jd.core.v1.model.javasyntax.expression.MethodReferenceExpression;
import org.jd.core.v1.model.javasyntax.type.Type;

/** A method reference which keeps the descriptor of the method type which the call site instantiates ('(LOuter$1;)Ljava/lang/String;' for 'Function&lt;Outer$1, String&gt;') */
public class ClassFileMethodReferenceExpression extends MethodReferenceExpression {
    private final String instantiatedDescriptor;

    public ClassFileMethodReferenceExpression(int lineNumber, Type type, Expression expression, String internalTypeName, String name, String descriptor,
            String instantiatedDescriptor) {
        super(lineNumber, type, expression, internalTypeName, name, descriptor);
        this.instantiatedDescriptor = instantiatedDescriptor;
    }

    /** @return the descriptor of the instantiated method type, null if unknown */
    public String getInstantiatedDescriptor() {
        return instantiatedDescriptor;
    }
}
