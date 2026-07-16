/*
 * Copyright (c) 2008, 2019 Emmanuel Dupuy.
 * This project is distributed under the GPLv3 license.
 * This is a Copyleft license that gives the user the right to use,
 * copy and modify the code freely for non-commercial purposes.
 */
package org.jd.core.v1.service.converter.classfiletojavasyntax.model.cfg;

import org.junit.Test;

import static org.junit.Assert.assertSame;

public class ControlFlowGraphJumpTest {
    @Test
    public void syntheticJumpRetainsTargetIdentity() {
        ControlFlowGraph cfg = new ControlFlowGraph(null);
        BasicBlock predecessor = cfg.newBasicBlock(BasicBlock.TYPE_STATEMENTS, 10, 20);
        BasicBlock target = cfg.newBasicBlock(BasicBlock.TYPE_STATEMENTS, 30, 40);

        BasicBlock jump = cfg.newJumpBasicBlock(predecessor, target);

        assertSame(target, jump.getJumpTarget());
    }
}
