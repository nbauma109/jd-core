/*
 * Copyright (c) 2008-2019 Emmanuel Dupuy.
 * This project is distributed under the GPLv3 license.
 * This is a Copyleft license that gives the user the right to use,
 * copy and modify the code freely for non-commercial purposes.
 */

package org.jd.core.v1.service.converter.classfiletojavasyntax.model.javasyntax.statement;

import org.jd.core.v1.model.javasyntax.expression.Expression;
import org.jd.core.v1.model.javasyntax.statement.BaseStatement;
import org.jd.core.v1.model.javasyntax.statement.ForEachStatement;
import org.jd.core.v1.service.converter.classfiletojavasyntax.model.localvariable.AbstractLocalVariable;

public class ClassFileForEachStatement extends ForEachStatement {
    private final AbstractLocalVariable localVariable;
    private int updateOffset = -1;

    public ClassFileForEachStatement(AbstractLocalVariable localVariable, Expression expression, BaseStatement statements) {
        super(localVariable.getType(), null, expression, statements);
        this.localVariable = localVariable;
    }

    /**
     * @return the offset of the update of the index of an array loop ('i$++'), which is the target of the jumps
     *         ('continue') which were not resolved when the update was removed, or -1
     */
    public int getUpdateOffset() {
        return updateOffset;
    }

    public void setUpdateOffset(int updateOffset) {
        this.updateOffset = updateOffset;
    }

    @Override
    public String getName() {
        return localVariable.getName();
    }

    @Override
    public String toString() {
        return "ClassFileForEachStatement{" + getType() + " " + localVariable.getName() + " : " + expression + "}";
    }
}
