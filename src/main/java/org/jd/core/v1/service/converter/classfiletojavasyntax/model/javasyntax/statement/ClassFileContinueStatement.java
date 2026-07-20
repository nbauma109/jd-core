/*
 * Copyright (c) 2008-2019 Emmanuel Dupuy.
 * This project is distributed under the GPLv3 license.
 * This is a Copyleft license that gives the user the right to use,
 * copy and modify the code freely for non-commercial purposes.
 */

package org.jd.core.v1.service.converter.classfiletojavasyntax.model.javasyntax.statement;

import org.jd.core.v1.model.javasyntax.statement.ContinueStatement;

/** An unlabelled continue that retains its bytecode jump target. */
public class ClassFileContinueStatement extends ContinueStatement {
    private final int targetOffset;

    public ClassFileContinueStatement(int targetOffset) {
        this.targetOffset = targetOffset;
    }

    public int getTargetOffset() {
        return targetOffset;
    }
}
