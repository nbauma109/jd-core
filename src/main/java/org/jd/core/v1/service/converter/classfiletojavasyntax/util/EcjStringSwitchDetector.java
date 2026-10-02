/*
 * Copyright (c) 2008, 2019 Emmanuel Dupuy.
 * This project is distributed under the GPLv3 license.
 * This is a Copyleft license that gives the user the right to use,
 * copy and modify the code freely for non-commercial purposes.
 */
package org.jd.core.v1.service.converter.classfiletojavasyntax.util;

import org.apache.bcel.Const;
import org.apache.bcel.classfile.ConstantCP;
import org.apache.bcel.classfile.ConstantNameAndType;
import org.apache.bcel.classfile.ConstantString;
import org.apache.bcel.classfile.ConstantPool;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;

/**
 * Recognizes the 'switch' on a string compiled by ECJ: a single 'switch' on the hash code, whose cases only compare the
 * string with their candidates and jump to the code of the matching one:
 *
 * <pre>
 *   aload X; dup; astore T; invokevirtual String.hashCode(); lookupswitch { H1: L1, ..., default: D }
 *   L1: aload T; ldc "a"; invokevirtual String.equals(Object); ifne A;
 *       [aload T; ldc "b"; invokevirtual String.equals(Object); ifne B;]
 *       goto D
 * </pre>
 *
 * The 'switch' is then rewritten as a 'switch' on the string: one case per string, which jumps to its code.
 */
final class EcjStringSwitchDetector {
    /** @param values the synthetic values of the cases, [0] is unused (default case)
     *  @param offsets the offsets of the code of the cases, [0] is the default case
     *  @param strings the strings of the cases, [0] is unused (default case)
     *  @param chainFrom the offset of the first instruction which only compares strings
     *  @param chainTo the offset after the last instruction which only compares strings */
    record Result(int[] values, int[] offsets, String[] strings, int chainFrom, int chainTo) {
        @Override
        public boolean equals(Object object) {
            return object instanceof Result other
                && Arrays.equals(values, other.values) && Arrays.equals(offsets, other.offsets) && Arrays.equals(strings, other.strings)
                && chainFrom == other.chainFrom && chainTo == other.chainTo;
        }

        @Override
        public int hashCode() {
            return Objects.hash(Arrays.hashCode(values), Arrays.hashCode(offsets), Arrays.hashCode(strings), chainFrom, chainTo);
        }

        @Override
        public String toString() {
            return "Result[values=" + Arrays.toString(values) + ", offsets=" + Arrays.toString(offsets) + ", strings=" + Arrays.toString(strings)
                + ", chainFrom=" + chainFrom + ", chainTo=" + chainTo + "]";
        }
    }

    private EcjStringSwitchDetector() {
    }

    /**
     * @param switchStart the offset of the 'lookupswitch' or 'tableswitch' instruction
     * @param values the hash codes of the cases, [0] is unused (default case)
     * @param offsets the offsets of the cases, [0] is the default case
     * @return null if the 'switch' is not the one of a string compiled by ECJ: the key of each case is the hash code of the strings which
     *         are compared in its code
     */
    static Result detect(ConstantPool constants, byte[] code, int switchStart, int[] values, int[] offsets) {
        int local = searchHashCodeReceiver(constants, code, switchStart);

        if (local < 0 || offsets.length < 2) {
            return null;
        }

        int defaultOffset = offsets[0];
        List<String> strings = new ArrayList<>();
        List<Integer> targets = new ArrayList<>();
        int chainFrom = Integer.MAX_VALUE;
        int chainTo = 0;

        for (int j = 1; j < offsets.length; j++) {
            int offset = offsets[j];

            if (offset == defaultOffset) {
                return null;
            }

            chainFrom = Math.min(chainFrom, offset);

            // The comparisons of the strings which have the hash code, then the jump to the default case
            int first = strings.size();
            int end = readComparisons(constants, code, offset, local, strings, targets);

            if (end < 0 || !haveHashCode(strings.subList(first, strings.size()), values[j]) || (code[end] & 255) != Const.GOTO || end + (short) ((code[end + 1] & 255) << 8 | code[end + 2] & 255) != defaultOffset) {
                return null;
            }

            chainTo = Math.max(chainTo, end + 3);
        }

        if (new HashSet<>(strings).size() != strings.size()) {
            // A duplicate 'case' label does not compile
            return null;
        }

        int size = strings.size();
        int[] newValues = new int[size + 1];
        int[] newOffsets = new int[size + 1];
        String[] newStrings = new String[size + 1];

        newOffsets[0] = defaultOffset;

        for (int i = 0; i < size; i++) {
            newValues[i + 1] = i;
            newOffsets[i + 1] = targets.get(i);
            newStrings[i + 1] = strings.get(i);
        }

        return new Result(newValues, newOffsets, newStrings, chainFrom, chainTo);
    }

    /**
     * Reads 'aload T; ldc "s"; invokevirtual String.equals(Object); ifne B' as many times as there are.
     *
     * @return the offset after the last comparison, or -1 if there is none or if one is not made of these instructions
     */
    private static int readComparisons(ConstantPool constants, byte[] code, int offset, int local, List<String> strings, List<Integer> targets) {
        int count = 0;

        while (isLoadOf(code, offset, local)) {
            int next = readComparison(constants, code, offset + (code[offset] == Const.ALOAD ? 2 : 1), strings, targets);

            if (next < 0) {
                return -1;
            }
            offset = next;
            count++;
        }

        return count == 0 ? -1 : offset;
    }

    /** @return the offset after the comparison which starts with the constant, or -1 */
    private static int readComparison(ConstantPool constants, byte[] code, int offset, List<String> strings, List<Integer> targets) {
        int ldc = code[offset] & 255;
        int constantIndex;
        int next;

        if (ldc == Const.LDC) {
            constantIndex = code[offset + 1] & 255;
            next = offset + 2;
        } else if (ldc == Const.LDC_W) {
            constantIndex = (code[offset + 1] & 255) << 8 | code[offset + 2] & 255;
            next = offset + 3;
        } else {
            return -1;
        }

        // Another constant (e.g. a class) is not a string
        if (!(constants.getConstant(constantIndex) instanceof ConstantString)) {
            return -1;
        }

        if ((code[next] & 255) != Const.INVOKEVIRTUAL
         || !isMethod(constants, (code[next + 1] & 255) << 8 | code[next + 2] & 255, "java/lang/String", "equals", "(Ljava/lang/Object;)Z")
         || (code[next + 3] & 255) != Const.IFNE) {
            return -1;
        }

        strings.add(constants.getConstantString(constantIndex, Const.CONSTANT_String));
        targets.add(next + 3 + (short) ((code[next + 4] & 255) << 8 | code[next + 5] & 255));
        return next + 6;
    }

    private static boolean haveHashCode(List<String> strings, int hashCode) {
        return strings.stream().allMatch(string -> string.hashCode() == hashCode);
    }

    /** @return the index of the local variable which receives the string before 'hashCode()', or -1 */
    private static int searchHashCodeReceiver(ConstantPool constants, byte[] code, int switchStart) {
        // dup; astore T; invokevirtual String.hashCode()
        if (switchStart < 5 || (code[switchStart - 3] & 255) != Const.INVOKEVIRTUAL
         || !isMethod(constants, (code[switchStart - 2] & 255) << 8 | code[switchStart - 1] & 255, "java/lang/String", "hashCode", "()I")) {
            return -1;
        }

        if (switchStart >= 6 && (code[switchStart - 5] & 255) == Const.ASTORE && (code[switchStart - 6] & 255) == Const.DUP) {
            return code[switchStart - 4] & 255;
        }

        int store = code[switchStart - 4] & 255;

        if (store >= Const.ASTORE_0 && store <= Const.ASTORE_3 && (code[switchStart - 5] & 255) == Const.DUP) {
            return store - Const.ASTORE_0;
        }

        return -1;
    }

    private static boolean isLoadOf(byte[] code, int offset, int local) {
        int opcode = code[offset] & 255;

        if (opcode == Const.ALOAD) {
            return (code[offset + 1] & 255) == local;
        }

        return local <= 3 && opcode == Const.ALOAD_0 + local;
    }

    private static boolean isMethod(ConstantPool constants, int index, String owner, String name, String descriptor) {
        ConstantCP reference = constants.getConstant(index);
        ConstantNameAndType nameAndType = constants.getConstant(reference.getNameAndTypeIndex());

        return owner.equals(constants.getConstantString(reference.getClassIndex(), Const.CONSTANT_Class))
            && name.equals(constants.getConstantString(nameAndType.getNameIndex(), Const.CONSTANT_Utf8))
            && descriptor.equals(constants.getConstantString(nameAndType.getSignatureIndex(), Const.CONSTANT_Utf8));
    }
}
