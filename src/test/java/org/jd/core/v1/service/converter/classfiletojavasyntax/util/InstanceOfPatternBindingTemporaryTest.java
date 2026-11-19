/*
 * Copyright (c) 2026 Nicolas Baumann (@nbauma109), with Claude Sonnet 5.5.
 * This project is distributed under the GPLv3 license.
 * This is a copyleft license that gives the user the right to use,
 * copy and modify the code freely for non-commercial purposes.
 */
package org.jd.core.v1.service.converter.classfiletojavasyntax.util;

import org.junit.Test;

import java.util.Map;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/**
 * The Eclipse compiler keeps the value which a pattern tests in a variable: 'getfield f; dup; astore v; instanceof T; ifeq L; aload v; checkcast T; astore t'.
 * The test is a pattern of the value only if the variable is a temporary of the compiler: no parameter, and nothing but the pattern reads it.
 */
public class InstanceOfPatternBindingTemporaryTest {

    private static final int ALOAD_0 = 0x2a;
    private static final int ALOAD_2 = 0x2c;
    private static final int ALOAD = 0x19;
    private static final int ASTORE = 0x3a;
    private static final int ASTORE_1 = 0x4c;
    private static final int ASTORE_2 = 0x4d;
    private static final int ASTORE_3 = 0x4e;
    private static final int DUP = 0x59;
    private static final int POP = 0x57;
    private static final int ICONST_0 = 0x03;
    private static final int ACONST_NULL = 0x01;
    private static final int IFEQ_OPCODE = 0x99;
    private static final int RETURN = 0xb1;
    private static final int INSTANCEOF = 0xc1;
    private static final int CHECKCAST = 0xc0;
    private static final int IFEQ = 0x99;

    private static byte[] code(int... instructions) {
        byte[] code = new byte[instructions.length];

        for (int i = 0; i < code.length; i++) {
            code[i] = (byte) instructions[i];
        }
        return code;
    }

    @Test
    public void testTheVariableOfADuplicateWhichOnlyThePatternReadsIsATemporary() {
        // 0: aload_0; 1: dup; 2: astore_2; 3: instanceof #1; 6: ifeq 14; 9: aload_2; 10: checkcast #1; 13: astore_3; 14: return
        byte[] code = code(ALOAD_0, DUP, ASTORE_2, INSTANCEOF, 0, 1, IFEQ, 0, 8, ALOAD_2, CHECKCAST, 0, 1, ASTORE_3, RETURN);

        Map<Integer, InstanceOfPatternBinding> bindings = InstanceOfPatternBinding.find(code, 1, null, null);

        assertEquals(1, bindings.size());
        assertTrue(bindings.containsKey(3));
        assertEquals(3, bindings.get(3).storeIndex());
    }

    @Test
    public void testTheIndexOfTheVariableOfADuplicateIsTheOperandOfTheStore() {
        // 0: aload_0; 1: dup; 2: astore 5; 4: instanceof #1; 7: ifeq 17; 10: aload 5; 12: checkcast #1; 15: astore_3; 16: return
        byte[] code = code(ALOAD_0, DUP, ASTORE, 5, INSTANCEOF, 0, 1, IFEQ, 0, 9, ALOAD, 5, CHECKCAST, 0, 1, ASTORE_3, RETURN);

        Map<Integer, InstanceOfPatternBinding> bindings = InstanceOfPatternBinding.find(code, 1, null, null);

        assertEquals(1, bindings.size());
        assertTrue(bindings.containsKey(4));
    }

    @Test
    public void testTheVariableWhichTheCodeReadsAfterThePatternIsNoTemporary() {
        // the same, and 'aload_2; pop' after the binding: '(x = field) instanceof T t' then 'use(x)'
        byte[] code = code(ALOAD_0, DUP, ASTORE_2, INSTANCEOF, 0, 1, IFEQ, 0, 8, ALOAD_2, CHECKCAST, 0, 1, ASTORE_3, ALOAD_2, POP, RETURN);

        assertTrue(InstanceOfPatternBinding.find(code, 1, null, null).isEmpty());
    }

    @Test
    public void testTheVariableWhichIsAParameterIsNoTemporary() {
        // 'astore_1' stores a parameter: '(value = field) instanceof T t'
        byte[] code = code(ALOAD_0, DUP, ASTORE_1, INSTANCEOF, 0, 1, IFEQ, 0, 8, 0x2b, CHECKCAST, 0, 1, ASTORE_3, RETURN);

        assertTrue(InstanceOfPatternBinding.find(code, 2, null, null).isEmpty());
    }

    @Test
    public void testAnotherStoreOfTheSlotEndsTheSearchOfTheReads() {
        // the slot is stored again after the pattern: the load which follows reads the other value (a second temporary of the compiler)
        byte[] code = code(ALOAD_0, DUP, ASTORE_2, INSTANCEOF, 0, 1, IFEQ, 0, 11, ALOAD_2, CHECKCAST, 0, 1, ASTORE_3, ALOAD_0, ASTORE_2, ALOAD_2, POP, RETURN);

        assertEquals(1, InstanceOfPatternBinding.find(code, 1, null, null).size());
    }

    @Test
    public void testAStoreWhichDuplicatesNothingIsNoTemporary() {
        // 0: aload_0; 1: astore_2; 2: instanceof: the value was loaded and stored, not duplicated: no operand
        byte[] code = code(ALOAD_0, ASTORE_2, INSTANCEOF, 0, 1, IFEQ, 0, 8, ALOAD_2, CHECKCAST, 0, 1, ASTORE_3, RETURN);

        assertTrue(InstanceOfPatternBinding.find(code, 1, null, null).isEmpty());
    }

    @Test
    public void testAStoreWhichStartsTheCodeIsNoTemporary() {
        // 0: astore_2; 1: instanceof: there is no instruction before the store
        byte[] code = code(ASTORE_2, INSTANCEOF, 0, 1, IFEQ, 0, 8, ALOAD_2, CHECKCAST, 0, 1, ASTORE_3, RETURN);

        assertTrue(InstanceOfPatternBinding.find(code, 1, null, null).isEmpty());
    }

    @Test
    public void testAnEarlierUseWhichIsNoTemporaryOfTheCompilerMakesTheVariableOneOfTheSource() {
        // 0: aload_0; 1: astore_2; 2: aload_2; 3: pop; 4: aload_0; 5: dup; 6: astore_2; 7: instanceof; 10: ifeq 18; 13: aload_2; 14: checkcast; 17: astore_3; 18: return
        byte[] code = code(ALOAD_0, ASTORE_2, ALOAD_2, POP, ALOAD_0, DUP, ASTORE_2, INSTANCEOF, 0, 1, IFEQ, 0, 8, ALOAD_2, CHECKCAST, 0, 1, ASTORE_3, RETURN);

        assertTrue(InstanceOfPatternBinding.find(code, 1, null, null).isEmpty());
    }

    @Test
    public void testTheTemporariesOfTheCompilerMaySharetheSlot() {
        // two patterns, each with its own 'dup; astore_2': the stores and the loads of the first are the ones of a temporary
        // 0: aload_0; 1: dup; 2: astore_2; 3: instanceof; 6: ifeq 14; 9: aload_2; 10: checkcast; 13: astore_3;
        // 14: aload_0; 15: dup; 16: astore_2; 17: instanceof; 20: ifeq 29; 23: aload_2; 24: checkcast; 27: astore 4; 29: return
        byte[] code = code(ALOAD_0, DUP, ASTORE_2, INSTANCEOF, 0, 1, IFEQ, 0, 8, ALOAD_2, CHECKCAST, 0, 1, ASTORE_3,
                ALOAD_0, DUP, ASTORE_2, INSTANCEOF, 0, 1, IFEQ, 0, 9, ALOAD_2, CHECKCAST, 0, 1, ASTORE, 4, RETURN);

        Map<Integer, InstanceOfPatternBinding> bindings = InstanceOfPatternBinding.find(code, 1, null, null);

        assertEquals(2, bindings.size());
        assertTrue(bindings.containsKey(3));
        assertTrue(bindings.containsKey(17));
    }

    @Test
    public void testAStoreWhichAConditionMayNotExecuteDoesNotHideTheReadAfterIt() {
        // 14: iconst_0; 15: ifeq 20; 18: aconst_null; 19: astore_2; 20: aload_2 (reached without the store); 21: pop; 22: return
        byte[] code = code(ALOAD_0, DUP, ASTORE_2, INSTANCEOF, 0, 1, IFEQ, 0, 8, ALOAD_2, CHECKCAST, 0, 1, ASTORE_3,
                ICONST_0, IFEQ_OPCODE, 0, 5, ACONST_NULL, ASTORE_2, ALOAD_2, POP, RETURN);

        assertTrue(InstanceOfPatternBinding.find(code, 1, null, null).isEmpty());
    }

    @Test
    public void testAStoreWhichEveryWayExecutesEndsTheSearchOfTheReads() {
        // 14: aconst_null; 15: astore_2; 16: aload_2 (reads the value which is stored now); 17: pop; 18: return
        byte[] code = code(ALOAD_0, DUP, ASTORE_2, INSTANCEOF, 0, 1, IFEQ, 0, 8, ALOAD_2, CHECKCAST, 0, 1, ASTORE_3,
                ACONST_NULL, ASTORE_2, ALOAD_2, POP, RETURN);

        assertEquals(1, InstanceOfPatternBinding.find(code, 1, null, null).size());
    }
}
