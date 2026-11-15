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
        // A ternary of two int constants is an int: a byte needs the cast
        assertTrue(source, source.contains("this.linkFlag = isDir ? (byte)53 : (byte)48;"));
        // The method reference is no cast to the bound of T, which javac infers as 'Integer' from the method reference
        assertTrue(source, source.contains("consume(source::next, (entry, list) -> list.add(source.value(entry)), out);"));
        // 'new IntegerIterator()' is an Iterator<Integer>: the source casts it to an Iterator<E> (unchecked), the bytecode does not
        assertTrue(source, source.contains("(Iterator)new IntegerIterator()"));
        assertTrue(CompilerUtil.compile("17", new InMemoryJavaSourceFileObject(INTERNAL_CLASS_NAME, source)));
    }
}
