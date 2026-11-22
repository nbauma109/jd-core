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

public class EcjForDeclarationTest extends AbstractJdTest {

    /** the null which the Eclipse compiler assigns to the exception of a try-with-resources, in the slot of the variable of a loop, is no assignment of that variable */
    @Test
    public void testForVariableThenTryWithResources() throws Exception {
        String printed;

        try (InputStream is = getClass().getResourceAsStream("/jar/ecj-for-declaration-ecj-17.jar")) {
            printed = decompileSuccess(new ZipLoader(is), new NoLineNumberPrinter(), "org/jd/core/v1/stub/EcjForDeclaration");
        }

        String source = printed.replaceAll("\\s+", " ");

        assertTrue(printed, source.contains("for (Enumeration<? extends ZipEntry> e = "));
        assertFalse(printed, source.contains("e = null"));
        assertTrue(printed, CompilerUtil.compile("17", new InMemoryJavaSourceFileObject("org/jd/core/v1/stub/EcjForDeclaration", printed)));
    }
}


