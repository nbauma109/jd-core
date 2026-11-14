/*
 * Copyright (c) 2008, 2019 Emmanuel Dupuy.
 * This project is distributed under the GPLv3 license.
 * This is a Copyleft license that gives the user the right to use,
 * copy and modify the code freely for non-commercial purposes.
 */

package org.jd.core.v1.service.converter.classfiletojavasyntax.model.javasyntax.expression;

import org.jd.core.v1.model.javasyntax.expression.BaseExpression;
import org.jd.core.v1.model.javasyntax.expression.SuperConstructorInvocationExpression;
import org.jd.core.v1.model.javasyntax.type.BaseType;
import org.jd.core.v1.model.javasyntax.type.BaseTypeParameter;
import org.jd.core.v1.model.javasyntax.type.ObjectType;

public class ClassFileSuperConstructorInvocationExpression extends SuperConstructorInvocationExpression {
    private BaseType parameterTypes;
    private final BaseTypeParameter typeParameters;

    public ClassFileSuperConstructorInvocationExpression(int lineNumber, ObjectType type, String descriptor, BaseType parameterTypes, BaseTypeParameter typeParameters, BaseExpression parameters, boolean varArgs) {
        super(lineNumber, type, descriptor, parameters, varArgs);
        this.parameterTypes = parameterTypes;
        this.typeParameters = typeParameters;
    }

    public BaseTypeParameter getTypeParameters() { return typeParameters; }

    public BaseType getParameterTypes() {
        return parameterTypes;
    }

    public void setParameterTypes(BaseType parameterTypes) {
        this.parameterTypes = parameterTypes;
    }
}
