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

public class EcjTernaryCompareTest extends AbstractJdTest {

    /** an int ternary which is compared to a constant stays an int: the constant is not a boolean */
    @Test
    public void testTernaryComparedToAConstant() throws Exception {
        String printed;

        try (InputStream is = getClass().getResourceAsStream("/jar/ecj-ternary-compare-ecj-17.jar")) {
            printed = decompileSuccess(new ZipLoader(is), new NoLineNumberPrinter(), "org/jd/core/v1/stub/EcjTernaryCompare");
        }

        String source = printed.replaceAll("\\s+", " ");

        assertFalse(printed, source.contains("== true"));
        assertTrue(printed, source.contains(".id) == 1)"));
        assertTrue(printed, CompilerUtil.compile("17", new InMemoryJavaSourceFileObject("org/jd/core/v1/stub/EcjTernaryCompare", printed)));
    }
}


