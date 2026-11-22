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

public class EcjYieldInLoopTest extends AbstractJdTest {

    /** the yields of a rule of a switch expression are yields wherever they are, in a loop for instance, and their constants have the type of the switch */
    @Test
    public void testYieldFromALoop() throws Exception {
        String printed;

        try (InputStream is = getClass().getResourceAsStream("/jar/ecj-yield-in-loop-ecj-17.jar")) {
            printed = decompileSuccess(new ZipLoader(is), new NoLineNumberPrinter(), "org/jd/core/v1/stub/EcjYieldInLoop");
        }

        String source = printed.replaceAll("\\s+", " ");

        assertFalse(printed, source.contains("return 8"));
        assertTrue(printed, source.contains("yield 8;"));
        assertTrue(printed, source.contains("yield 0;"));
        assertTrue(printed, CompilerUtil.compile("17", new InMemoryJavaSourceFileObject("org/jd/core/v1/stub/EcjYieldInLoop", printed)));
    }
}


