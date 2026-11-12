/*
 * Copyright (c) 2008, 2019 Emmanuel Dupuy.
 * This project is distributed under the GPLv3 license.
 * This is a Copyleft license that gives the user the right to use,
 * copy and modify the code freely for non-commercial purposes.
 */

package org.jd.core.v1.service.converter.classfiletojavasyntax.model.javasyntax.statement;

import org.jd.core.v1.model.javasyntax.statement.BaseStatement;
import org.jd.core.v1.model.javasyntax.statement.TryStatement;
import org.jd.core.v1.model.javasyntax.type.ObjectType;
import org.jd.core.v1.service.converter.classfiletojavasyntax.model.localvariable.AbstractLocalVariable;
import org.jd.core.v1.util.DefaultList;

public class ClassFileTryStatement extends TryStatement {
    private final boolean jsr;
    /** True when the exception table protects nothing but the invocations of accessors with the handler of the statement: the lowering of a record pattern */
    private boolean protectsAccessorsOnly;

    public ClassFileTryStatement(BaseStatement tryStatements, DefaultList<TryStatement.CatchClause> catchClauses, BaseStatement finallyStatements, boolean jsr) {
        super(tryStatements, catchClauses, finallyStatements);
        this.jsr = jsr;
    }

    public ClassFileTryStatement(DefaultList<Resource> resources, BaseStatement tryStatements, DefaultList<TryStatement.CatchClause> catchClauses, BaseStatement finallyStatements, boolean jsr) {
        super(resources, tryStatements, catchClauses, finallyStatements);
        this.jsr = jsr;
    }

    public void addResources(DefaultList<Resource> resources) {
        if (resources != null) {
            if (this.resources == null) {
                this.resources = resources;
            } else {
                this.resources.addAll(resources);
            }
        }
    }

    public boolean isProtectsAccessorsOnly() {
        return protectsAccessorsOnly;
    }

    public void setProtectsAccessorsOnly(boolean protectsAccessorsOnly) {
        this.protectsAccessorsOnly = protectsAccessorsOnly;
    }

    /**
     * @return false for the 'try' of the byte code whose handler also protects other instructions: a hand-written 'try' which looks like the lowering of a record pattern.
     * The statements which were not made from byte code carry no such information and are accepted.
     */
    public static boolean protectsAccessorsOnly(TryStatement statement) {
        return !(statement instanceof ClassFileTryStatement classFileTry) || classFileTry.isProtectsAccessorsOnly();
    }

    public boolean isJsr() {
        return jsr;
    }

    public static class CatchClause extends TryStatement.CatchClause {
        private final AbstractLocalVariable localVariable;

        public CatchClause(int lineNumber, ObjectType type, AbstractLocalVariable localVariable, BaseStatement statements) {
            super(lineNumber, type, null, statements);
            this.localVariable = localVariable;
        }

        public AbstractLocalVariable getLocalVariable() {
            return localVariable;
        }

        @Override
        public String getName() {
            return localVariable.getName();
        }
    }
}
