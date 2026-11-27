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

public class EcjMultiLineTernaryTest extends AbstractJdTest {

    /** the Eclipse compiler goes back to the line of the creation, inside the expression, to call its constructor */
    @Test
    public void testTernaryOfCreationsOnSeveralLines() throws Exception {
        String printed;

        try (InputStream is = getClass().getResourceAsStream("/jar/ecj-multi-line-ternary-ecj-17.jar")) {
            printed = decompileSuccess(new ZipLoader(is), new NoLineNumberPrinter(), "org/jd/core/v1/stub/EcjMultiLineTernary");
        }

        String source = printed.replaceAll("\\s+", " ");

        assertTrue(printed, source.contains("this.stack[this.pointer] = (operator != 30) ? new"));
        assertFalse(printed, source.contains("labelMerge"));
        assertFalse(printed, source.contains("throw null"));
        assertTrue(printed, CompilerUtil.compile("17", new InMemoryJavaSourceFileObject("org/jd/core/v1/stub/EcjMultiLineTernary", printed)));
    }
}
