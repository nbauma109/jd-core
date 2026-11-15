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

/** Patterns found by decompiling and recompiling Jackson Core */
public class JacksonCorePatternsTest extends AbstractJdTest {

    private static final String INTERNAL_CLASS_NAME = "jackson/Fixture";

    private static final String SOURCE = String.join("\n",
            "package jackson;",
            "import java.io.IOException;",
            "import java.util.Arrays;",
            "public class Fixture {",
            "    static class Buffer { char[] finish() throws IOException { return new char[10]; } }",
            "    private int append(char escaped, char[] buffer) { return 2; }",
            // 'JsonStringEncoder.quoteAsString': two variables 'c' and 'd' share a slot, the one which is read in the loop is 'c'
            "    char[] quote(String input, int[] escapes) {",
            "        final int inputLen = input.length();",
            "        char[] output = new char[10];",
            "        int inPtr = 0;",
            "        Buffer buffer = null;",
            "        int outPtr = 0;",
            "        char[] qbuf = null;",
            "        outer:",
            "        while (inPtr < inputLen) {",
            "            tight:",
            "            while (true) {",
            "                char c = input.charAt(inPtr);",
            "                if (c < escapes.length && escapes[c] != 0) {",
            "                    break tight;",
            "                }",
            "                if (outPtr >= output.length) {",
            "                    if (buffer == null) { buffer = new Buffer(); }",
            "                    try { output = buffer.finish(); } catch (IOException e) { throw new IllegalStateException(e); }",
            "                    outPtr = 0;",
            "                }",
            "                output[outPtr++] = c;",
            "                if (++inPtr >= inputLen) { break outer; }",
            "            }",
            "            if (qbuf == null) { qbuf = new char[6]; }",
            "            char d = input.charAt(inPtr++);",
            "            int length = append(d, qbuf);",
            "            System.arraycopy(qbuf, 0, output, outPtr, length);",
            "            outPtr += length;",
            "        }",
            "        return Arrays.copyOfRange(output, 0, outPtr);",
            "    }",
            // 'ParserMinimalBase.skipChildren': the report is a call which throws, and the loop goes on after it for the compiler
            "    int open;",
            "    int next() { return open; }",
            "    void report(String message) throws IOException { throw new IOException(message); }",
            "    Fixture skip() throws IOException {",
            "        while (true) {",
            "            int t = next();",
            "            if (t == 0) { return this; }",
            "            if (t == 1) { ++open; } else if (t == 2) { if (--open == 0) { return this; } } else if (t == 3) { report(\"not available\"); }",
            "        }",
            "    }",
            "}");

    @Test
    public void testPatterns() throws Exception {
        String source = decompileSuccess(RestoreMethodReferenceTest.compileForJava8("-g", Map.of(INTERNAL_CLASS_NAME, SOURCE)), new PlainTextPrinter(), INTERNAL_CLASS_NAME);

        // The 'char' which is read in the loop is not the one which is declared after it
        assertTrue(source, source.contains("output[outPtr++] = c;"));
        // The loop which ends the method has no break: the method returns from inside it
        assertFalse(source, source.contains("break;\n      } \n    } \n  }"));
        assertTrue(CompilerUtil.compile("17", new InMemoryJavaSourceFileObject(INTERNAL_CLASS_NAME, source)));
    }
}
