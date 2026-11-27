/*
 * Copyright (c) 2026 Nicolas Baumann (@nbauma109), with Claude Sonnet 5.5.
 * This project is distributed under the GPLv3 license.
 * This is a copyleft license that gives the user the right to use,
 * copy and modify the code freely for non-commercial purposes.
 */

package org.jd.core.v1;

import org.jd.core.v1.compiler.CompilerUtil;
import org.jd.core.v1.compiler.InMemoryJavaSourceFileObject;
import org.jd.core.v1.loader.ZipLoader;
import org.jd.core.v1.printer.NoLineNumberPrinter;
import org.junit.Test;

import java.io.InputStream;

public class EmptyVarArgsOverloadTest extends AbstractJdTest {

    /** the empty array is written when the call without it is the one of an overload: it would call the method itself */
    @Test
    public void testEmptyArrayWhichSelectsTheVariableArityOverload() throws Exception {
        String printed;

        try (InputStream is = getClass().getResourceAsStream("/jar/empty-varargs-overload-javac-8.jar")) {
            printed = decompileSuccess(new ZipLoader(is), new NoLineNumberPrinter(), "org/jd/core/v1/stub/EmptyVarArgsOverload");
        }

        assertTrue(printed, printed.contains("this(text, new Object[0]);"));
        assertTrue(printed, printed.contains("return begin(flow, new Object[0]);"));
        assertTrue(printed, printed.contains("return none(new Object[0]);"));
        assertTrue(printed, printed.contains("new EmptyVarArgsOverload(flow, new Object[0])"));
        // without an overload the call needs no array
        assertTrue(printed, printed.contains("return join(flow);"));
        assertTrue(printed, printed.contains("append(last())"));
        assertTrue(printed, CompilerUtil.compile("17", new InMemoryJavaSourceFileObject("org/jd/core/v1/stub/EmptyVarArgsOverload", printed)));
    }
}
