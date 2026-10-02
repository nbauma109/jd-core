/*
 * Copyright (c) 2008, 2019 Emmanuel Dupuy.
 * This project is distributed under the GPLv3 license.
 * This is a Copyleft license that gives the user the right to use,
 * copy and modify the code freely for non-commercial purposes.
 */
package org.jd.core.v1.service.converter.classfiletojavasyntax.util;

import org.apache.bcel.Const;
import org.apache.bcel.classfile.ConstantPool;
import org.apache.bcel.generic.ConstantPoolGen;
import org.junit.Test;

import java.io.ByteArrayOutputStream;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

/** The 'switch' on the hash code of a string, compiled by ECJ, is only recognized if it really is one. */
public class EcjStringSwitchDetectorTest {
    private static final int FIRST_CODE = 200;
    private static final int SECOND_CODE = 250;
    private static final int DEFAULT_CODE = 300;

    private final ConstantPoolGen pool = new ConstantPoolGen();
    private final int hashCodeMethod = pool.addMethodref("java/lang/String", "hashCode", "()I");
    private final int equalsMethod = pool.addMethodref("java/lang/String", "equals", "(Ljava/lang/Object;)Z");
    private final int lengthMethod = pool.addMethodref("java/lang/String", "length", "()I");
    private final int aString = pool.addString("Aa");
    private final int anotherString = pool.addString("BB");
    private final int aClass = pool.addClass("java/lang/String");

    /** A tiny assembler: the offset of the instructions is the one of the code of a method. */
    private static final class Code {
        private final ByteArrayOutputStream bytes = new ByteArrayOutputStream();

        Code u1(int value) {
            bytes.write(value);
            return this;
        }

        Code u2(int value) {
            return u1(value >> 8).u1(value & 255);
        }

        int offset() {
            return bytes.size();
        }

        Code jump(int opcode, int target) {
            int from = offset();

            return u1(opcode).u2((target - from) & 0xFFFF);
        }

        byte[] toByteArray() {
            return bytes.toByteArray();
        }
    }

    private ConstantPool constants() {
        return pool.getFinalConstantPool();
    }

    /** 'aload X; dup; astore local; invokevirtual String.hashCode()', then the (unused) bytes of the switch: returns the offset of the switch */
    private int prelude(Code code, int local, int hashCodeIndex) {
        code.u1(Const.ALOAD_0).u1(Const.DUP);
        if (local <= 3) {
            code.u1(Const.ASTORE_0 + local);
        } else {
            code.u1(Const.ASTORE).u1(local);
        }
        code.u1(Const.INVOKEVIRTUAL).u2(hashCodeIndex);

        int switchStart = code.offset();

        // The switch instruction itself is not read
        for (int i = 0; i < 16; i++) {
            code.u1(0);
        }
        return switchStart;
    }

    private void compare(Code code, int local, int constantIndex, boolean wide, int equalsIndex, int target) {
        if (local <= 3) {
            code.u1(Const.ALOAD_0 + local);
        } else {
            code.u1(Const.ALOAD).u1(local);
        }
        if (wide) {
            code.u1(Const.LDC_W).u2(constantIndex);
        } else {
            code.u1(Const.LDC).u1(constantIndex);
        }
        code.u1(Const.INVOKEVIRTUAL).u2(equalsIndex).jump(Const.IFNE, target);
    }

    private EcjStringSwitchDetector.Result detect(int local, boolean wide, int constantIndex, int equalsIndex, int hashCode, int gotoTarget) {
        Code code = new Code();
        int switchStart = prelude(code, local, hashCodeMethod);
        int chain = code.offset();

        compare(code, local, constantIndex, wide, equalsIndex, FIRST_CODE);
        code.jump(Const.GOTO, gotoTarget);

        return EcjStringSwitchDetector.detect(constants(), code.toByteArray(), switchStart, new int[] {0, hashCode}, new int[] {DEFAULT_CODE, chain});
    }

    @Test
    public void testAStringWhichHasTheKeyOfTheCase() {
        EcjStringSwitchDetector.Result result = detect(1, false, aString, equalsMethod, "Aa".hashCode(), DEFAULT_CODE);

        assertNotNull(result);
        assertArrayEquals(new String[] {null, "Aa"}, result.strings());
        assertArrayEquals(new int[] {DEFAULT_CODE, FIRST_CODE}, result.offsets());
        assertArrayEquals(new int[] {0, 0}, result.values());
    }

    @Test
    public void testTheConstantOfAWideLoadAndTheLocalsWhichAreNotInTheShortForms() {
        assertNotNull(detect(5, true, aString, equalsMethod, "Aa".hashCode(), DEFAULT_CODE));
    }

    @Test
    public void testAKeyWhichIsNotTheHashCodeOfTheStringIsNotAStringSwitch() {
        assertNull(detect(1, false, aString, equalsMethod, "Aa".hashCode() + 1, DEFAULT_CODE));
    }

    @Test
    public void testTheConstantIsAString() {
        // 'tmp.equals(String.class)' has the same instructions
        assertNull(detect(1, false, aClass, equalsMethod, "Aa".hashCode(), DEFAULT_CODE));
        assertNull(detect(1, true, aClass, equalsMethod, "Aa".hashCode(), DEFAULT_CODE));
    }

    @Test
    public void testTheComparisonIsStringEquals() {
        assertNull(detect(1, false, aString, lengthMethod, "Aa".hashCode(), DEFAULT_CODE));
    }

    @Test
    public void testTheComparisonsEndWithTheJumpToTheDefaultCase() {
        assertNull(detect(1, false, aString, equalsMethod, "Aa".hashCode(), DEFAULT_CODE + 1));
    }

    @Test
    public void testTheStringsWhichHaveTheSameHashCodeAreCompared() {
        Code code = new Code();
        int switchStart = prelude(code, 1, hashCodeMethod);
        int chain = code.offset();

        compare(code, 1, aString, false, equalsMethod, FIRST_CODE);
        compare(code, 1, anotherString, false, equalsMethod, SECOND_CODE);
        code.jump(Const.GOTO, DEFAULT_CODE);

        EcjStringSwitchDetector.Result result =
            EcjStringSwitchDetector.detect(constants(), code.toByteArray(), switchStart, new int[] {0, "Aa".hashCode()}, new int[] {DEFAULT_CODE, chain});

        assertNotNull(result);
        assertArrayEquals(new String[] {null, "Aa", "BB"}, result.strings());
        assertArrayEquals(new int[] {DEFAULT_CODE, FIRST_CODE, SECOND_CODE}, result.offsets());
        assertArrayEquals(new int[] {0, 0, 1}, result.values());
        assertEquals(chain, result.chainFrom());
    }

    @Test
    public void testACaseWhichHasNoComparisonAndTheCaseOfTheDefaultCodeAreNotStringSwitches() {
        Code code = new Code();
        int switchStart = prelude(code, 1, hashCodeMethod);
        int chain = code.offset();

        code.jump(Const.GOTO, DEFAULT_CODE);

        byte[] bytes = code.toByteArray();

        assertNull(EcjStringSwitchDetector.detect(constants(), bytes, switchStart, new int[] {0, 1}, new int[] {DEFAULT_CODE, chain}));
        assertNull(EcjStringSwitchDetector.detect(constants(), bytes, switchStart, new int[] {0, 1}, new int[] {DEFAULT_CODE, DEFAULT_CODE}));
        assertNull(EcjStringSwitchDetector.detect(constants(), bytes, switchStart, new int[] {0}, new int[] {DEFAULT_CODE}));
    }

    @Test
    public void testOnlyTheHashCodeOfAStringIsTheKey() {
        Code code = new Code();
        int switchStart = prelude(code, 1, equalsMethod);

        compare(code, 1, aString, false, equalsMethod, FIRST_CODE);
        code.jump(Const.GOTO, DEFAULT_CODE);

        assertNull(EcjStringSwitchDetector.detect(constants(), code.toByteArray(), switchStart, new int[] {0, "Aa".hashCode()}, new int[] {DEFAULT_CODE, switchStart + 16}));
    }
}
