/*
 * Copyright (c) 2008-2026 Emmanuel Dupuy.
 * This project is distributed under the GPLv3 license.
 * This is a Copyleft license that gives the user the right to use,
 * copy and modify the code freely for non-commercial purposes.
 */

package org.jd.core.v1.service.converter.classfiletojavasyntax.model.javasyntax.statement;

import org.jd.core.v1.model.javasyntax.expression.Expression;
import org.jd.core.v1.model.javasyntax.statement.BaseStatement;
import org.jd.core.v1.model.javasyntax.statement.IfStatement;

/** An if statement that retains evidence that its true branch exits the current loop. */
public class ClassFileIfStatement extends IfStatement {
    public ClassFileIfStatement(Expression condition, BaseStatement statements) {
        super(condition, statements);
    }
}
