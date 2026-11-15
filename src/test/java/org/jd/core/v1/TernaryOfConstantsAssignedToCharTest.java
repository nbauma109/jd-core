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

/**
 * jsoup 1.24.1 has 'char combinator = cond ? ' ' : 0;' and later 'combinator = consume();': javac starts the range of the variable after
 * the first store, and the type of the ternary of two constants must not be an 'int', which 'char' cannot receive.
 */
public class TernaryOfConstantsAssignedToCharTest extends AbstractJdTest {

    private static final String INTERNAL_CLASS_NAME = "ternary/Fixture";

    private static final String SOURCE = String.join("\n",
            "package ternary;",
            "import java.util.ArrayList;",
            "import java.util.List;",
            "public class Fixture {",
            "    private int position;",
            "    private boolean blank() { return position++ % 2 == 0; }",
            "    private char next() { return 'a'; }",
            "    List<Character> combinators(boolean other) {",
            "        List<Character> combinators = new ArrayList<>();",
            "        while (position < 10) {",
            "            char combinator = blank() ? ' ' : 0;",
            "            if (other) combinator = next();",
            "            if (combinator == 0) break;",
            "            combinators.add(combinator);",
            "        }",
            "        return combinators;",
            "    }",
            "    int plain(boolean flag) {",
            "        int value = flag ? 32 : 0;",
            "        return value + 1;",
            "    }",
            "}");

    @Test
    public void testTernaryOfConstantsAssignedToChar() throws Exception {
        String source = decompileSuccess(RestoreMethodReferenceTest.compileForJava8("-g", Map.of(INTERNAL_CLASS_NAME, SOURCE)), new PlainTextPrinter(), INTERNAL_CLASS_NAME);

        assertTrue(source, source.contains("char combinator = blank() ? ' ' : '\\000';"));
        // Without anything to tell it is a 'char', the value stays an 'int'
        assertTrue(source, source.contains("int value = flag ? 32 : 0;"));
        assertTrue(CompilerUtil.compile("17", new InMemoryJavaSourceFileObject(INTERNAL_CLASS_NAME, source)));
    }
}
