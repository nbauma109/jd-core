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
import org.jd.core.v1.printer.PlainTextPrinter;
import org.junit.Test;

import java.io.InputStream;

public class EcjGenericArrayTest extends AbstractJdTest {

    /** the Eclipse compiler writes no local variable type table: the type of the parameters is the one of the signature of the method */
    @Test
    public void testArrayOfATypeVariable() throws Exception {
        String printed;

        try (InputStream is = getClass().getResourceAsStream("/jar/ecj-generic-array-ecj-17.jar")) {
            printed = decompileSuccess(new ZipLoader(is), new PlainTextPrinter(), "org/jd/core/v1/stub/EcjGenericArray");
        }

        assertTrue(printed, printed.contains("filterValid(long first, T[] all, double second, Function<Integer, T[]> ctor)"));
        assertFalse(printed, printed.contains("return (T[])all"));
        assertTrue(printed, CompilerUtil.compile("17", new InMemoryJavaSourceFileObject("org/jd/core/v1/stub/EcjGenericArray", printed)));
    }
}
