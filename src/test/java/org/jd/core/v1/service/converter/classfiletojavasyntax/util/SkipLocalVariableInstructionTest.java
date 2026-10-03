package org.jd.core.v1.service.converter.classfiletojavasyntax.util;

import org.junit.Test;

import static org.apache.bcel.Const.ALOAD;
import static org.apache.bcel.Const.ALOAD_0;
import static org.apache.bcel.Const.ALOAD_3;
import static org.apache.bcel.Const.ASTORE;
import static org.apache.bcel.Const.ASTORE_0;
import static org.apache.bcel.Const.ASTORE_3;
import static org.apache.bcel.Const.ICONST_0;
import static org.apache.bcel.Const.WIDE;
import static org.junit.Assert.assertEquals;

public class SkipLocalVariableInstructionTest {
    private static int skipStore(byte[] code, int offset) {
        return ControlFlowGraphReducer.skipLocalVariableInstruction(code, offset, ASTORE, ASTORE_0, ASTORE_3);
    }

    @Test
    public void testStoreWithAnIndex() {
        assertEquals(2, skipStore(new byte[] {(byte) ASTORE, 5}, 0));
    }

    @Test
    public void testShortcut() {
        assertEquals(1, skipStore(new byte[] {(byte) ASTORE_0}, 0));
        assertEquals(1, skipStore(new byte[] {(byte) ASTORE_3}, 0));
    }

    @Test
    public void testWideStore() {
        assertEquals(4, skipStore(new byte[] {(byte) WIDE, (byte) ASTORE, 1, 0}, 0));
    }

    @Test
    public void testWideInstructionWhichIsNotTheExpectedOne() {
        assertEquals(-1, ControlFlowGraphReducer.skipLocalVariableInstruction(new byte[] {(byte) WIDE, (byte) ASTORE, 1, 0}, 0, ALOAD, ALOAD_0, ALOAD_3));
        assertEquals(-1, skipStore(new byte[] {(byte) WIDE}, 0));
    }

    @Test
    public void testOtherInstruction() {
        assertEquals(-1, skipStore(new byte[] {(byte) ICONST_0}, 0));
    }

    @Test
    public void testNothingLeft() {
        assertEquals(-1, skipStore(new byte[] {(byte) ASTORE, 1}, 2));
    }
}
