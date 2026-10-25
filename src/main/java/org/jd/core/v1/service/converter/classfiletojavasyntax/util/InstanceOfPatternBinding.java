/*
 * Copyright (c) 2026 Nicolas Baumann (@nbauma109), with Claude Sonnet 5.5.
 * This project is distributed under the GPLv3 license.
 * This is a Copyleft license that gives the user the right to use,
 * copy and modify the code freely for non-commercial purposes.
 */
package org.jd.core.v1.service.converter.classfiletojavasyntax.util;

import java.util.HashMap;
import java.util.Map;

import static org.apache.bcel.Const.ALOAD;
import static org.apache.bcel.Const.ALOAD_0;
import static org.apache.bcel.Const.ALOAD_3;
import static org.apache.bcel.Const.ASTORE;
import static org.apache.bcel.Const.ASTORE_0;
import static org.apache.bcel.Const.ASTORE_3;
import static org.apache.bcel.Const.CHECKCAST;
import static org.apache.bcel.Const.IFEQ;
import static org.apache.bcel.Const.INSTANCEOF;

/**
 * The binding of the variable of 'x instanceof T v': javac compiles it to
 * <pre>aload x; instanceof T; ifeq L; aload x; checkcast T; astore v</pre>
 * The three instructions which follow the test only bind the variable: they are no statement.
 *
 * @param storeIndex index of the local variable which is bound
 * @param storeOffset offset of the 'astore'
 * @param storeEndOffset offset which follows the 'astore'
 * @param firstSkippedOffset offset of the 'aload' which follows the test
 * @param skippedLength length in bytes of 'aload; checkcast; astore'
 */
public record InstanceOfPatternBinding(int storeIndex, int storeOffset, int storeEndOffset, int firstSkippedOffset, int skippedLength) {

    /** Java 16 */
    private static final int MIN_MAJOR_VERSION = 60;

    /** The configuration key which turns the patterns off, for output which has to be recompiled with a source level older than 16 */
    public static final String CONFIGURATION_KEY = "instanceOfPatterns";

    private static final ThreadLocal<Boolean> ENABLED = ThreadLocal.withInitial(() -> Boolean.TRUE);

    /** Enables the patterns for the current thread, which decompiles one class at a time */
    public static void setEnabled(boolean enabled) {
        ENABLED.set(enabled);
    }

    /** An older class file cannot come from a pattern: its hand-written cast has to stay recompilable with its own source level */
    public static boolean isSupportedBy(int majorVersion) {
        return majorVersion >= MIN_MAJOR_VERSION && ENABLED.get();
    }

    /** @return the bindings, by the offset of their 'instanceof' */
    public static Map<Integer, InstanceOfPatternBinding> find(byte[] code) {
        Map<Integer, InstanceOfPatternBinding> bindings = new HashMap<>();

        for (int p = 1; p + 6 < code.length; p++) {
            if ((code[p] & 255) == INSTANCEOF && (code[p + 3] & 255) == IFEQ) {
                InstanceOfPatternBinding binding = match(code, p);

                if (binding != null) {
                    bindings.put(p, binding);
                }
            }
        }
        return bindings;
    }

    /** @return the offsets of the instructions which only bind the variable, with their lengths */
    public Map<Integer, Integer> skippedInstructions(byte[] code) {
        Map<Integer, Integer> lengths = new HashMap<>();
        int offset = firstSkippedOffset;
        int end = offset + skippedLength;

        while (offset < end) {
            int opcode = code[offset] & 255;
            int length = opcode == ALOAD || opcode == ASTORE ? 2 : opcode == CHECKCAST ? 3 : 1;
            lengths.put(offset, length);
            offset += length;
        }
        return lengths;
    }

    private static InstanceOfPatternBinding match(byte[] code, int instanceOfOffset) {
        // The operand of 'instanceof' is a local variable
        int operandIndex = loadedIndex(code, instanceOfOffset - 1, true);
        if (operandIndex < 0) {
            return null;
        }

        int offset = instanceOfOffset + 6;
        int loadLength = (code[offset] & 255) == ALOAD ? 2 : 1;
        if (loadedIndex(code, offset, false) != operandIndex || offset + loadLength + 3 >= code.length) {
            return null;
        }

        int checkcastOffset = offset + loadLength;
        if ((code[checkcastOffset] & 255) != CHECKCAST
                || code[checkcastOffset + 1] != code[instanceOfOffset + 1] || code[checkcastOffset + 2] != code[instanceOfOffset + 2]) {
            return null;
        }

        int storeOffset = checkcastOffset + 3;
        int opcode = code[storeOffset] & 255;
        int storeIndex;
        int storeLength;
        if (opcode == ASTORE) {
            storeIndex = code[storeOffset + 1] & 255;
            storeLength = 2;
        } else if (opcode >= ASTORE_0 && opcode <= ASTORE_3) {
            storeIndex = opcode - ASTORE_0;
            storeLength = 1;
        } else {
            return null;
        }
        return new InstanceOfPatternBinding(storeIndex, storeOffset, storeOffset + storeLength, offset, loadLength + 3 + storeLength);
    }

    /** @return the index of the local variable which is loaded by the instruction at 'offset' (or which ends at 'offset' when 'ends' is set), -1 if it is no load */
    private static int loadedIndex(byte[] code, int offset, boolean ends) {
        if (offset < 0 || offset >= code.length) {
            return -1;
        }
        int opcode = code[offset] & 255;
        if (opcode >= ALOAD_0 && opcode <= ALOAD_3) {
            return opcode - ALOAD_0;
        }
        if (ends) {
            return offset >= 1 && (code[offset - 1] & 255) == ALOAD ? code[offset] & 255 : -1;
        }
        return opcode == ALOAD && offset + 1 < code.length ? code[offset + 1] & 255 : -1;
    }
}
