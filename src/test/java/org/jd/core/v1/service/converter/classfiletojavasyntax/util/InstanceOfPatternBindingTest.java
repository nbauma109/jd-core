/*
 * Copyright (c) 2026 Nicolas Baumann (@nbauma109), with Claude Sonnet 5.5.
 * This project is distributed under the GPLv3 license.
 * This is a Copyleft license that gives the user the right to use,
 * copy and modify the code freely for non-commercial purposes.
 */

package org.jd.core.v1.service.converter.classfiletojavasyntax.util;

import org.junit.Test;

import java.util.Map;

import static org.apache.bcel.Const.ALOAD;
import static org.apache.bcel.Const.ALOAD_0;
import static org.apache.bcel.Const.ALOAD_1;
import static org.apache.bcel.Const.ASTORE;
import static org.apache.bcel.Const.ASTORE_2;
import static org.apache.bcel.Const.CHECKCAST;
import static org.apache.bcel.Const.IFEQ;
import static org.apache.bcel.Const.INSTANCEOF;
import static org.apache.bcel.Const.ISTORE_2;
import static org.apache.bcel.Const.RETURN;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

/** The byte code which javac writes for 'x instanceof T v': aload x; instanceof T; ifeq L; aload x; checkcast T; astore v */
public class InstanceOfPatternBindingTest {

    private static byte[] code(int... opcodes) {
        byte[] code = new byte[opcodes.length];

        for (int i = 0; i < opcodes.length; i++) {
            code[i] = (byte) opcodes[i];
        }
        return code;
    }

    @Test
    public void testShortLoadAndStore() {
        byte[] code = code(ALOAD_0, INSTANCEOF, 0, 7, IFEQ, 0, 12, ALOAD_0, CHECKCAST, 0, 7, ASTORE_2, RETURN);
        Map<Integer, InstanceOfPatternBinding> bindings = InstanceOfPatternBinding.find(code);

        assertEquals(1, bindings.size());

        InstanceOfPatternBinding binding = bindings.get(1);

        assertEquals(2, binding.storeIndex());
        assertEquals(11, binding.storeOffset());
        assertEquals(12, binding.storeEndOffset());
        assertEquals(7, binding.firstSkippedOffset());
        assertEquals(5, binding.skippedLength());
        assertEquals(Map.of(7, 1, 8, 3, 11, 1), binding.skippedInstructions(code));
    }

    @Test
    public void testWideLoadAndStore() {
        byte[] code = code(ALOAD, 5, INSTANCEOF, 0, 7, IFEQ, 0, 14, ALOAD, 5, CHECKCAST, 0, 7, ASTORE, 6, RETURN);
        Map<Integer, InstanceOfPatternBinding> bindings = InstanceOfPatternBinding.find(code);
        InstanceOfPatternBinding binding = bindings.get(2);

        assertEquals(6, binding.storeIndex());
        assertEquals(15, binding.storeEndOffset());
        assertEquals(7, binding.skippedLength());
        assertEquals(Map.of(8, 2, 10, 3, 13, 2), binding.skippedInstructions(code));
    }

    @Test
    public void testOtherVariableIsNoBinding() {
        assertTrue(InstanceOfPatternBinding.find(code(ALOAD_0, INSTANCEOF, 0, 7, IFEQ, 0, 12, ALOAD_1, CHECKCAST, 0, 7, ASTORE_2, RETURN)).isEmpty());
        assertTrue(InstanceOfPatternBinding.find(code(ALOAD, 5, INSTANCEOF, 0, 7, IFEQ, 0, 14, ALOAD, 4, CHECKCAST, 0, 7, ASTORE, 6, RETURN)).isEmpty());
    }

    @Test
    public void testOperandWhichIsNoLoadIsNoBinding() {
        assertTrue(InstanceOfPatternBinding.find(code(RETURN, INSTANCEOF, 0, 7, IFEQ, 0, 12, ALOAD_0, CHECKCAST, 0, 7, ASTORE_2, RETURN)).isEmpty());
    }

    @Test
    public void testOtherCastIsNoBinding() {
        assertTrue(InstanceOfPatternBinding.find(code(ALOAD_0, INSTANCEOF, 0, 7, IFEQ, 0, 12, ALOAD_0, CHECKCAST, 0, 8, ASTORE_2, RETURN)).isEmpty());
        assertTrue(InstanceOfPatternBinding.find(code(ALOAD_0, INSTANCEOF, 0, 7, IFEQ, 0, 12, ALOAD_0, RETURN, 0, 7, ASTORE_2, RETURN)).isEmpty());
    }

    @Test
    public void testStoreOfAnotherKindIsNoBinding() {
        assertTrue(InstanceOfPatternBinding.find(code(ALOAD_0, INSTANCEOF, 0, 7, IFEQ, 0, 12, ALOAD_0, CHECKCAST, 0, 7, ISTORE_2, RETURN)).isEmpty());
    }

    @Test
    public void testTruncatedCodeIsNoBinding() {
        assertTrue(InstanceOfPatternBinding.find(code(ALOAD_0, INSTANCEOF, 0, 7, IFEQ, 0, 12, ALOAD_0)).isEmpty());
        assertTrue(InstanceOfPatternBinding.find(code(ALOAD_0, INSTANCEOF, 0, 7, IFEQ, 0, 12, ALOAD_0, CHECKCAST, 0, 7)).isEmpty());
        assertNull(InstanceOfPatternBinding.find(code(ALOAD_0, INSTANCEOF, 0, 7, IFEQ, 0, 12, ALOAD_0, CHECKCAST, 0, 7, ASTORE_2, RETURN)).get(0));
    }

    @Test
    public void testSupportedVersions() {
        assertTrue(InstanceOfPatternBinding.isSupportedBy(60));
        assertTrue(!InstanceOfPatternBinding.isSupportedBy(52));
    }
}
