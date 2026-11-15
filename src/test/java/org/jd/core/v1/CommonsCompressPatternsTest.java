/*
 * Copyright (c) 2026 Nicolas Baumann (@nbauma109), with Claude Sonnet 5.5.
 * This project is distributed under the GPLv3 license.
 * This is a copyleft license that gives the user the right to use,
 * copy and modify the code freely for non-commercial purposes.
 */

package org.jd.core.v1;

import org.jd.core.v1.compiler.CompilerUtil;
import org.jd.core.v1.compiler.InMemoryJavaSourceFileObject;
import org.jd.core.v1.printer.PlainTextPrinter;
import org.junit.Test;

import java.util.Map;

/** Patterns found by decompiling and recompiling Commons Compress */
public class CommonsCompressPatternsTest extends AbstractJdTest {

    private static final String INTERNAL_CLASS_NAME = "compress/Fixture";

    private static final String SOURCE = String.join("\n",
            "package compress;",
            "import java.io.IOException;",
            "import java.util.Iterator;",
            "import java.util.List;",
            "public class Fixture<E extends Number> {",
            "    static final byte LF_DIR = (byte) '5';",
            "    static final byte LF_NORMAL = (byte) '0';",
            "    private byte linkFlag;",
            "    long smallest(boolean unsigned, boolean large) {",
            "        long result;",
            "        if (unsigned) {",
            "            if (large) {",
            "                result = Integer.MIN_VALUE;",
            "            } else {",
            "                result = 0;",
            "            }",
            "        } else {",
            "            result = Integer.MAX_VALUE;",
            "        }",
            "        return result;",
            "    }",
            "    private int skip(int length) { return length > 4 ? 1 / 0 : length; }",
            "    public long skip(long length) { return length; }",
            "    void closeEntry() {",
            "        while (skip((long) Integer.MAX_VALUE) == Integer.MAX_VALUE);",
            "    }",
            "    static long skipBytes(long n) { return n; }",
            "    void properties(Iterator<Integer> it) {",
            "        java.util.BitSet found = null;",
            "        while (true) {",
            "            int type = it.next();",
            "            if (type == 0) { break; }",
            "            switch (type) {",
            "            case 14: { found = new java.util.BitSet(); break; }",
            "            default: { skipBytes(1L); break; }",
            "            }",
            "        }",
            "        System.out.println(found);",
            "    }",
            "    int[] counts = new int[300];",
            "    byte[] bytes = new byte[10];",
            "    int postOfElement(int i) { final int tmp = counts[bytes[i] & 0xff]++; return tmp; }",
            "    int preOfElement(int i) { final int tmp = ++counts[i]; return tmp; }",
            "    int returnedPreOfElement(int i) { return --counts[i]; }",
            "    int returnedPre(int i) { return ++i; }",
            "    long[] map = new long[4];",
            "    void merge(int[] index, long value) { map[index[0]++] |= value; }",
            "    int compoundOfElement(int i) { return counts[i] += 5; }",
            "    int postDecrementOfElement(int i) { final int tmp = counts[i]--; return tmp; }",
            "    char chars(boolean flag) { final char c = flag ? 'a' : 'b'; return c; }",
            "    short shorts(boolean flag) { final short s = flag ? (short) 300 : (short) 7; return s; }",
            "    byte bytes(boolean flag, byte other) { return flag ? (byte) 1 : other; }",
            "    byte constantBytes(boolean flag) { return flag ? (byte) 1 : (byte) 2; }",
            "    long wide(long[] values, int i) { long result = Integer.MAX_VALUE; result += values[i]; values[i]++; return result; }",
            "    interface Maker<T extends Number> { T make(); }",
            "    interface Task<T extends Number> { T run() throws Exception; }",
            "    static <T extends Number> void pick(Maker<T> maker) { }",
            "    static void pick(String text) { }",
            "    static void pick(int count) { }",
            "    static <T extends Number> void pick(Task<T> task) { }",
            "    static Number number() { return 1; }",
            "    void overloaded() { pick((Maker<Number>) Fixture::number); }",
            "    float pair(float first, float second) { return first; }",
            "    float widened() { float x = 0; return pair(x, x = -2147483648f); }",
            "    int field;",
            "    int postOfLocal(int i) { final int t = i++; return t + i; }",
            "    int postOfField() { final int t = field++; return t; }",
            "    int preOfField() { final int t = ++field; return t; }",
            "    int[] other = new int[2];",
            "    int preOfOtherArray(int i) { ++other[i]; final int t = other[i]; return t; }",
            "    int timesOne(int i) { return counts[i] *= 1; }",
            "    char fromInts(boolean f, int x) { final char c = f ? (char) 65 : (char) x; return c; }",
            "    void big() { skip(Long.MAX_VALUE); }",
            "    long limit() { long l = Long.MAX_VALUE; return l; }",
            "    void flag(boolean isDir) {",
            "        this.linkFlag = isDir ? LF_DIR : LF_NORMAL;",
            "    }",
            "    interface Supplier<T extends Number> { T next() throws IOException; }",
            "    static class Source { Integer next() { return 1; } int value(Integer i) { return i; } }",
            "    interface Writer<T extends Number> { void write(T entry, List<Object> out) throws IOException; }",
            "    private <T extends Number> void consume(Supplier<T> supplier, Writer<T> writer, List<Object> out) throws IOException { writer.write(supplier.next(), out); }",
            "    private <T extends Number> void consume(Supplier<T> supplier, Writer<T> writer, String out) throws IOException { writer.write(supplier.next(), null); }",
            "    void methodReference(Source source, List<Object> out) throws IOException {",
            "        consume(source::next, (entry, list) -> list.add(source.value(entry)), out);",
            "    }",
            "    private static final class IntegerIterator implements Iterator<Integer> {",
            "        public boolean hasNext() { return false; }",
            "        public Integer next() { return 1; }",
            "    }",
            "    @SuppressWarnings(\"unchecked\")",
            "    Iterator<E> uncheckedCast() {",
            "        final Iterator<E> iterator = (Iterator<E>) new IntegerIterator();",
            "        return iterator;",
            "    }",
            "}");

    @Test
    public void testPatterns() throws Exception {
        String source = decompileSuccess(RestoreMethodReferenceTest.compileForJava8("-g", Map.of(INTERNAL_CLASS_NAME, SOURCE)), new PlainTextPrinter(), INTERNAL_CLASS_NAME);

        // The long constant -2147483648 is 'Integer.MIN_VALUE', and a long: it goes in the variable of the long
        assertTrue(source, source.contains("result = Integer.MIN_VALUE;"));
        assertTrue(source, source.contains("result = Integer.MAX_VALUE;"));
        // 'skip(Integer.MAX_VALUE)' would call the overload for an int
        assertTrue(source, source.contains("skip((long)Integer.MAX_VALUE)"));
        // The result of 'skipBytes' is a long, which a POP2 discards: it is one value on the stack of the decompiler, and the switch is not lost
        assertTrue(source, source.contains("case 14:"));
        // 'tmp = counts[i]++' is the value before the increment, '++counts[i]' the one after it
        assertTrue(source, source.contains("int tmp = this.counts[this.bytes[i] & 0xFF]++;") || source.contains("return this.counts[this.bytes[i] & 0xFF]++;"));
        assertTrue(source, source.contains("++this.counts[i];"));
        assertTrue(source, source.contains("return --this.counts[i];"));
        assertTrue(source, source.contains("return ++i;"));
        // The index which is incremented by an element of an array is evaluated once
        assertTrue(source, source.contains("this.map[index[0]++] |= value;"));
        assertTrue(source, source.contains("return this.counts[i] += 5;"));
        assertTrue(source, source.contains("this.counts[i]--;"));
        assertTrue(source, source.contains("byte constantBytes(boolean flag)"));
        // The cast is what selects the overload for a Maker, whatever the bound of the type variable
        assertTrue(source, source.contains("pick((Maker<Number>)Fixture::number)"));
        // The value which the assignment keeps for the call is the assignment itself, not a statement before it
        assertTrue(source, source.contains("pair(x, x = Integer.MIN_VALUE)"));
        // A ternary of two int constants is an int: a byte needs the cast
        assertTrue(source, source.contains("this.linkFlag = isDir ? (byte)53 : (byte)48;"));
        // The method reference is no cast to the bound of T, which javac infers as 'Integer' from the method reference
        assertTrue(source, source.contains("consume(source::next, (entry, list) -> list.add(source.value(entry)), out);"));
        // 'new IntegerIterator()' is an Iterator<Integer>: the source casts it to an Iterator<E> (unchecked), the bytecode does not
        assertTrue(source, source.contains("(Iterator)new IntegerIterator()"));
        assertTrue(CompilerUtil.compile("17", new InMemoryJavaSourceFileObject(INTERNAL_CLASS_NAME, source)));
    }
}
