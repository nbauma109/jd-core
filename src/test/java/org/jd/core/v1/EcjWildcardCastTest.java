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

public class EcjWildcardCastTest extends AbstractJdTest {

    /** a raw argument would make the call unchecked, whose result is erased: the cast has the type of the parameter */
    @Test
    public void testCastOfAnObjectToTheWildcardTypeOfTheParameter() throws Exception {
        String printed;

        try (InputStream is = getClass().getResourceAsStream("/jar/ecj-wildcard-cast-ecj-17.jar")) {
            printed = decompileSuccess(new ZipLoader(is), new NoLineNumberPrinter(), "org/jd/core/v1/stub/EcjWildcardCast");
        }

        assertTrue(printed, printed.contains("visitor.visitList((List<? extends CharSequence>)this.value, parameter)"));
        assertTrue(printed, CompilerUtil.compile("17", new InMemoryJavaSourceFileObject("org/jd/core/v1/stub/EcjWildcardCast", printed)));
    }
}
