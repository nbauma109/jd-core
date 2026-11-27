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

public class EcjPatternOfThisTest extends AbstractJdTest {

    /** the Eclipse compiler keeps 'this' in a temporary before it tests it */
    @Test
    public void testPatternOfThis() throws Exception {
        String printed;

        try (InputStream is = getClass().getResourceAsStream("/jar/ecj-pattern-of-this-ecj-17.jar")) {
            printed = decompileSuccess(new ZipLoader(is), new NoLineNumberPrinter(), "org/jd/core/v1/stub/EcjPatternOfThis");
        }

        String source = printed.replaceAll("\\s+", " ");

        assertTrue(printed, source.contains("instanceof Yield yield1 && yield1.implicit"));
        assertTrue(printed, source.contains("instanceof Yield yield2 && yield2.implicit"));
        assertTrue(printed, source.contains("instanceof Yield yield3 && yield3.implicit"));
        assertTrue(printed, CompilerUtil.compile("17", new InMemoryJavaSourceFileObject("org/jd/core/v1/stub/EcjPatternOfThis", printed)));
    }

    /** without the local variable table, the type of a parameter is still known */
    @Test
    public void testPatternOfAParameterWithoutDebugInformation() throws Exception {
        String printed;

        try (InputStream is = getClass().getResourceAsStream("/jar/ecj-pattern-no-debug-ecj-17.jar")) {
            printed = decompileSuccess(new ZipLoader(is), new NoLineNumberPrinter(), "org/jd/core/v1/stub/EcjPatternNoDebug");
        }

        assertTrue(printed, printed.contains("instanceof String"));
        assertTrue(printed, CompilerUtil.compile("17", new InMemoryJavaSourceFileObject("org/jd/core/v1/stub/EcjPatternNoDebug", printed)));
    }
}
