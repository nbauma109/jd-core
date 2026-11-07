/*
 * Copyright (c) 2008, 2019 Emmanuel Dupuy.
 * This project is distributed under the GPLv3 license.
 * This is a Copyleft license that gives the user the right to use,
 * copy and modify the code freely for non-commercial purposes.
 */

package org.jd.core.v1.service.converter.classfiletojavasyntax.model.javasyntax.expression;

import org.jd.core.v1.model.javasyntax.expression.BaseExpression;
import org.jd.core.v1.model.javasyntax.expression.Expression;
import org.jd.core.v1.model.javasyntax.expression.MethodInvocationExpression;
import org.jd.core.v1.model.javasyntax.type.BaseType;
import org.jd.core.v1.model.javasyntax.type.BaseTypeParameter;
import org.jd.core.v1.model.javasyntax.type.Type;
import org.jd.core.v1.service.converter.classfiletojavasyntax.util.AbstractTypeParametersToTypeArgumentsBinder;
import org.jd.core.v1.service.converter.classfiletojavasyntax.util.TypeMaker.MethodTypes;

public class ClassFileMethodInvocationExpression extends MethodInvocationExpression {
    private BaseTypeParameter typeParameters;
    private BaseType parameterTypes;
    private BaseType unboundParameterTypes;
    private Type unboundType;
    private boolean bound;
    private Boolean varArgsOverride;
    private boolean anonymousCast;
    private String anonymousCastName;
    private final MethodTypes methodTypes;

    public ClassFileMethodInvocationExpression(
            int lineNumber, Type type, Expression expression,
            String internalTypeName, String name, String descriptor, BaseExpression parameters, MethodTypes methodTypes) {
        super(lineNumber, type, expression, internalTypeName, name, descriptor, parameters, methodTypes);
        this.methodTypes = methodTypes;
        this.typeParameters = methodTypes == null ? null : methodTypes.getTypeParameters();
        this.parameterTypes = methodTypes == null ? null : AbstractTypeParametersToTypeArgumentsBinder.clone(methodTypes.getParameterTypes());
    }

    /** The copy has to be a {@link ClassFileMethodInvocationExpression}: the visitors of the conversion cast the method invocations to it */
    @Override
    public Expression copyTo(int lineNumber) {
        ClassFileMethodInvocationExpression copy = new ClassFileMethodInvocationExpression(lineNumber, getType(), getExpression(), getInternalTypeName(), getName(),
                getDescriptor(), getParameters(), methodTypes);

        copy.typeParameters = typeParameters;
        copy.parameterTypes = parameterTypes;
        copy.unboundParameterTypes = unboundParameterTypes;
        copy.unboundType = unboundType;
        copy.bound = bound;
        copy.varArgsOverride = varArgsOverride;
        copy.anonymousCast = anonymousCast;
        copy.anonymousCastName = anonymousCastName;
        copy.setTypeBounds(getTypeBounds());
        copy.setTypeBindings(getTypeBindings());
        return copy;
    }

    public BaseTypeParameter getTypeParameters() {
        return typeParameters;
    }

    public BaseType getParameterTypes() {
        return parameterTypes;
    }

    public void setParameterTypes(BaseType parameterTypes) {
        this.parameterTypes = parameterTypes;
    }

    public boolean isBound() {
        return bound;
    }

    public void setBound(boolean bound) {
        this.bound = bound;
    }

    @Override
    public boolean isVarArgs() {
        if (varArgsOverride != null) {
            return varArgsOverride;
        }
        return super.isVarArgs();
    }

    /** @return true if the byte code casts the result to an anonymous class, a runtime check which the source cannot write */
    public boolean isAnonymousCast() {
        return anonymousCast;
    }

    /** @return the internal name of the anonymous class which the byte code casts the result to, null if it is not known or the result is not cast to one */
    public String getAnonymousCastName() {
        return anonymousCastName;
    }

    public void setAnonymousCastName(String anonymousCastName) {
        this.anonymousCastName = anonymousCastName;
    }

    public void setAnonymousCast(boolean anonymousCast) {
        this.anonymousCast = anonymousCast;
    }

    public void setVarArgsOverride(Boolean varArgsOverride) {
        this.varArgsOverride = varArgsOverride;
    }

    public BaseType getUnboundParameterTypes() {
        return unboundParameterTypes;
    }

    public void setUnboundParameterTypes(BaseType unboundParameterTypes) {
        this.unboundParameterTypes = unboundParameterTypes;
    }

    public Type getUnboundType() {
        return unboundType;
    }

    public void setUnboundType(Type unboundType) {
        this.unboundType = unboundType;
    }
}
