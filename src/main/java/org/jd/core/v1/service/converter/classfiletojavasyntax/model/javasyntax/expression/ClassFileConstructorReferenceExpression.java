/*
 * Copyright (c) 2026 Nicolas Baumann (@nbauma109), with Claude Sonnet 5.5.
 * This project is distributed under the GPLv3 license.
 * This is a Copyleft license that gives the user the right to use,
 * copy and modify the code freely for non-commercial purposes.
 */

package org.jd.core.v1.service.converter.classfiletojavasyntax.model.javasyntax.expression;

import org.jd.core.v1.model.javasyntax.expression.ConstructorReferenceExpression;
import org.jd.core.v1.model.javasyntax.type.ObjectType;
import org.jd.core.v1.model.javasyntax.type.Type;

/** A constructor reference which keeps the descriptor of the method type which the call site instantiates ('(LOuter$1;)Ljava/lang/Object;' for 'Function&lt;Outer$1, Box&gt;') */
public class ClassFileConstructorReferenceExpression extends ConstructorReferenceExpression {
    private final String instantiatedDescriptor;

    public ClassFileConstructorReferenceExpression(int lineNumber, Type type, ObjectType objectType, String descriptor, String instantiatedDescriptor) {
        super(lineNumber, type, objectType, descriptor);
        this.instantiatedDescriptor = instantiatedDescriptor;
    }

    /** @return the descriptor of the instantiated method type, null if unknown */
    public String getInstantiatedDescriptor() {
        return instantiatedDescriptor;
    }
}
