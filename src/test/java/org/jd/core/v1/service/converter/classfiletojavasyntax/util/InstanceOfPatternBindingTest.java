/*
 * Copyright (c) 2026 Nicolas Baumann (@nbauma109), with Claude Sonnet 5.5.
 * This project is distributed under the GPLv3 license.
 * This is a Copyleft license that gives the user the right to use,
 * copy and modify the code freely for non-commercial purposes.
 */

package org.jd.core.v1.service.converter.classfiletojavasyntax.util;

import org.apache.bcel.Repository;
import org.apache.bcel.classfile.Constant;
import org.apache.bcel.classfile.ConstantClass;
import org.apache.bcel.classfile.ConstantPool;
import org.apache.bcel.classfile.ConstantUtf8;
import org.apache.bcel.classfile.LocalVariable;
import org.apache.bcel.classfile.LocalVariableTable;
import org.apache.bcel.classfile.Method;
import org.jd.core.v1.loader.ClassPathLoader;
import org.junit.After;
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
    private static final ConstantPool POOL = new ConstantPool(new Constant[0]);

    private static byte[] code(int... opcodes) {
        byte[] code = new byte[opcodes.length];

        for (int i = 0; i < opcodes.length; i++) {
            code[i] = (byte) opcodes[i];
        }
        return code;
    }

    @Test
    public void testShortLoadAndStore() {
        byte[] code = code(ALOAD_0, INSTANCEOF, 0, 7, IFEQ, 0, 8, ALOAD_0, CHECKCAST, 0, 7, ASTORE_2, RETURN);
        Map<Integer, InstanceOfPatternBinding> bindings = InstanceOfPatternBinding.find(code, 0, null, null);

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
        byte[] code = code(ALOAD, 5, INSTANCEOF, 0, 7, IFEQ, 0, 10, ALOAD, 5, CHECKCAST, 0, 7, ASTORE, 6, RETURN);
        Map<Integer, InstanceOfPatternBinding> bindings = InstanceOfPatternBinding.find(code, 0, null, null);
        InstanceOfPatternBinding binding = bindings.get(2);

        assertEquals(6, binding.storeIndex());
        assertEquals(15, binding.storeEndOffset());
        assertEquals(7, binding.skippedLength());
        assertEquals(Map.of(8, 2, 10, 3, 13, 2), binding.skippedInstructions(code));
    }

    @Test
    public void testOtherVariableIsNoBinding() {
        assertTrue(InstanceOfPatternBinding.find(code(ALOAD_0, INSTANCEOF, 0, 7, IFEQ, 0, 8, ALOAD_1, CHECKCAST, 0, 7, ASTORE_2, RETURN), 0, null, null).isEmpty());
        assertTrue(InstanceOfPatternBinding.find(code(ALOAD, 5, INSTANCEOF, 0, 7, IFEQ, 0, 10, ALOAD, 4, CHECKCAST, 0, 7, ASTORE, 6, RETURN), 0, null, null).isEmpty());
    }

    @Test
    public void testOperandWhichIsNoLoadIsNoBinding() {
        assertTrue(InstanceOfPatternBinding.find(code(RETURN, INSTANCEOF, 0, 7, IFEQ, 0, 8, ALOAD_0, CHECKCAST, 0, 7, ASTORE_2, RETURN), 0, null, null).isEmpty());
    }

    @Test
    public void testOtherCastIsNoBinding() {
        assertTrue(InstanceOfPatternBinding.find(code(ALOAD_0, INSTANCEOF, 0, 7, IFEQ, 0, 8, ALOAD_0, CHECKCAST, 0, 8, ASTORE_2, RETURN), 0, null, null).isEmpty());
        assertTrue(InstanceOfPatternBinding.find(code(ALOAD_0, INSTANCEOF, 0, 7, IFEQ, 0, 8, ALOAD_0, RETURN, 0, 7, ASTORE_2, RETURN), 0, null, null).isEmpty());
    }

    @Test
    public void testStoreOfAnotherKindIsNoBinding() {
        assertTrue(InstanceOfPatternBinding.find(code(ALOAD_0, INSTANCEOF, 0, 7, IFEQ, 0, 8, ALOAD_0, CHECKCAST, 0, 7, ISTORE_2, RETURN), 0, null, null).isEmpty());
    }

    @Test
    public void testTruncatedCodeIsNoBinding() {
        assertTrue(InstanceOfPatternBinding.find(code(ALOAD_0, INSTANCEOF, 0, 7, IFEQ, 0, 8, ALOAD_0), 0, null, null).isEmpty());
        assertTrue(InstanceOfPatternBinding.find(code(ALOAD_0, INSTANCEOF, 0, 7, IFEQ, 0, 8, ALOAD_0, CHECKCAST, 0, 7), 0, null, null).isEmpty());
        assertNull(InstanceOfPatternBinding.find(code(ALOAD_0, INSTANCEOF, 0, 7, IFEQ, 0, 8, ALOAD_0, CHECKCAST, 0, 7, ASTORE_2, RETURN), 0, null, null).get(0));
    }

    @Test
    public void testSupportedVersions() {
        assertTrue(InstanceOfPatternBinding.isSupportedBy(60));
        assertTrue(!InstanceOfPatternBinding.isSupportedBy(52));
    }

    @Test
    public void testOperandWhichLooksLikeACompactLoad() {
        // 'aload 42' is no 'aload_0' followed by the 'instanceof': 42 is the index of the variable
        assertTrue(InstanceOfPatternBinding.find(code(ALOAD, 42, INSTANCEOF, 0, 7, IFEQ, 0, 8, ALOAD_0, CHECKCAST, 0, 7, ASTORE_2, RETURN), 0, null, null).isEmpty());
    }

    @Test
    public void testParametersAreNoPatternVariables() {
        byte[] code = code(ALOAD_0, INSTANCEOF, 0, 7, IFEQ, 0, 8, ALOAD_0, CHECKCAST, 0, 7, ASTORE_2, RETURN);

        assertTrue(InstanceOfPatternBinding.find(code, 3, null, null).isEmpty());
        assertEquals(1, InstanceOfPatternBinding.find(code, 2, null, null).size());
    }

    @Test
    public void testVariableWhichIsDeclaredByThePattern() {
        byte[] code = code(ALOAD_0, INSTANCEOF, 0, 7, IFEQ, 0, 8, ALOAD_0, CHECKCAST, 0, 7, ASTORE_2, RETURN);
        LocalVariableTable declared = new LocalVariableTable(0, 0, new LocalVariable[] {new LocalVariable(12, 1, 0, 0, 2, POOL)}, POOL);
        LocalVariableTable existing = new LocalVariableTable(0, 0, new LocalVariable[] {new LocalVariable(0, 13, 0, 0, 2, POOL)}, POOL);

        assertEquals(1, InstanceOfPatternBinding.find(code, 0, declared, null).size());
        assertTrue(InstanceOfPatternBinding.find(code, 0, existing, null).isEmpty());
    }

    @Test
    public void testVariableWhichIsDeclaredWithAnotherTypeThanThePattern() {
        // 'Object s = (String) o;' is no 'o instanceof String s'
        ConstantPool pool = new ConstantPool(new Constant[] {null, new ConstantClass(2), new ConstantUtf8("java/lang/String"), new ConstantUtf8("Ljava/lang/String;"), new ConstantUtf8("Ljava/lang/Object;")});
        byte[] code = code(ALOAD_0, INSTANCEOF, 0, 1, IFEQ, 0, 8, ALOAD_0, CHECKCAST, 0, 1, ASTORE_2, RETURN);
        LocalVariableTable sameType = new LocalVariableTable(0, 0, new LocalVariable[] {new LocalVariable(12, 1, 0, 3, 2, pool)}, pool);
        LocalVariableTable otherType = new LocalVariableTable(0, 0, new LocalVariable[] {new LocalVariable(12, 1, 0, 4, 2, pool)}, pool);

        assertEquals(1, InstanceOfPatternBinding.find(code, 0, sameType, pool).size());
        assertTrue(InstanceOfPatternBinding.find(code, 0, otherType, pool).isEmpty());
        assertEquals(1, InstanceOfPatternBinding.find(code, 0, otherType, null).size());
    }

    /** Methods which javac compiles with the debug information, to read the bindings as the decompiler does */
    @SuppressWarnings({ "unchecked", "java:S1172", "java:S1854", "java:S1481", "java:S1488" })
    static class Samples {
        String pattern(Object o) {
            if (o instanceof String) {
                String s = (String) o;
                return s;
            }
            return null;
        }

        String assignedByBothBranches(Object o) {
            String s;
            if (o instanceof String) {
                s = (String) o;
            } else {
                s = "fallback";
            }
            return s;
        }

        String reassigned(Object o) {
            if (o instanceof String) {
                String s = (String) o;
                s = "other";
                return s;
            }
            return null;
        }

        String ofAWildcard(java.util.Collection<?> o) {
            if (o instanceof java.util.List) {
                java.util.List<String> list = (java.util.List<String>) o;
                return list.get(0);
            }
            return null;
        }

        String ofTheSameArguments(java.util.Collection<String> o) {
            if (o instanceof java.util.List) {
                java.util.List<String> list = (java.util.List<String>) o;
                return list.get(0);
            }
            return null;
        }

        String ofARawOperand(Object o) {
            if (o instanceof java.util.List) {
                java.util.List<String> list = (java.util.List<String>) o;
                return list.get(0);
            }
            return null;
        }

        String assignedByBothBranchesInALongSlot(Object o, long a, long b, long c) {
            String s;
            if (o instanceof String) {
                s = (String) o;
            } else {
                s = "fallback";
            }
            return s;
        }

        String patternInALongSlot(Object o, long a, long b, long c) {
            if (o instanceof String) {
                String s = (String) o;
                return s;
            }
            return null;
        }

        String assignedByTwoCasts(Object a, Object b) {
            String s;
            if (a instanceof String) {
                s = (String) a;
            } else if (b instanceof String) {
                s = (String) b;
            } else {
                return "fallback";
            }
            return s;
        }

        String twoSequentialPatterns(Object a, Object b) {
            if (a instanceof String) {
                String s = (String) a;
                return s;
            }
            if (b instanceof String) {
                String s = (String) b;
                return s;
            }
            return null;
        }

        String ofAnArray(java.util.Collection<String>[] o) {
            if (o instanceof java.util.List[]) {
                java.util.List<String>[] lists = (java.util.List<String>[]) o;
                return lists[0].get(0);
            }
            return null;
        }

        @SuppressWarnings("java:S108")
        String castWhichFollowsAnEmptyTest(Object o) {
            if (o instanceof String) {
            }
            String s = (String) o;
            return s;
        }

        interface A<T> {}

        interface B<T> {}

        String ofAnUnrelatedType(A<String> o) {
            if (o instanceof B) {
                B<String> b = (B<String>) o;
                return b.toString();
            }
            return null;
        }

        boolean negated(Object o) {
            return !(o instanceof String s) || s.isEmpty();
        }
    }

    @After
    public void resetTheLoader() {
        InstanceOfPatternBinding.setLoader(null);
    }

    private static int bindings(String method) throws ClassNotFoundException {
        for (Method m : Repository.lookupClass(Samples.class).getMethods()) {
            if (m.getName().equals(method)) {
                return InstanceOfPatternBinding.find(m).size();
            }
        }
        throw new IllegalArgumentException(method);
    }

    @Test
    public void testBindingsOfCompiledMethods() throws ClassNotFoundException {
        InstanceOfPatternBinding.setLoader(new ClassPathLoader());
        assertEquals(1, bindings("pattern"));
        // 's' is declared before the test and assigned by both branches
        assertEquals(0, bindings("assignedByBothBranches"));
        // the variable is assigned again in its scope
        assertEquals(0, bindings("reassigned"));
        // the arguments have to be those of the operand
        assertEquals(0, bindings("ofAWildcard"));
        assertEquals(0, bindings("ofARawOperand"));
        assertEquals(1, bindings("ofTheSameArguments"));
        // ... and its type a super type of the tested one
        assertEquals(0, bindings("ofAnUnrelatedType"));
        assertEquals(0, bindings("assignedByBothBranchesInALongSlot"));
        assertEquals(1, bindings("patternInALongSlot"));
        assertEquals(0, bindings("assignedByTwoCasts"));
        assertEquals(2, bindings("twoSequentialPatterns"));
        assertEquals(1, bindings("ofAnArray"));
        // the failed test does not jump over the cast
        assertEquals(0, bindings("castWhichFollowsAnEmptyTest"));
        // a second range of the variable which no store starts is not an assignment
        assertEquals(1, bindings("negated"));
    }
}
