/*
 * Copyright (c) 2026 Nicolas Baumann (@nbauma109), with Claude Sonnet 5.5.
 * This project is distributed under the GPLv3 license.
 * This is a Copyleft license that gives the user the right to use,
 * copy and modify the code freely for non-commercial purposes.
 */

package org.jd.core.v1.service.converter.classfiletojavasyntax.model.javasyntax.expression;

import org.jd.core.v1.model.javasyntax.expression.Expression;
import org.jd.core.v1.model.javasyntax.expression.FieldReferenceExpression;

/** A generic field read whose value the byte code casts to an anonymous class, a runtime check which the source cannot write */
public class ClassFileFieldReferenceExpression extends FieldReferenceExpression {
    private final String anonymousCastName;

    public ClassFileFieldReferenceExpression(FieldReferenceExpression field, String anonymousCastName) {
        super(field.getLineNumber(), field.getType(), field.getExpression(), field.getInternalTypeName(), field.getName(), field.getDescriptor());
        this.anonymousCastName = anonymousCastName;
    }

    /** @return the internal name of the anonymous class which the byte code casts the field value to */
    public String getAnonymousCastName() {
        return anonymousCastName;
    }

    @Override
    public Expression copyTo(int lineNumber) {
        return new ClassFileFieldReferenceExpression(new FieldReferenceExpression(lineNumber, getType(), getExpression(), getInternalTypeName(), getName(), getDescriptor()),
                anonymousCastName);
    }
}
