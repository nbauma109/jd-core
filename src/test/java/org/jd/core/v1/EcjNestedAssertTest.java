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

public class EcjNestedAssertTest extends AbstractJdTest {

    /** the flag of the assertions of the body of a constant of an enum is the one of the enum: it is an assert, not a test of the flag */
    @Test
    public void testAssertOfAnEnumConstantAndOfAnInnerClass() throws Exception {
        String printed;

        try (InputStream is = getClass().getResourceAsStream("/jar/ecj-nested-assert-ecj-17.jar")) {
            printed = decompileSuccess(new ZipLoader(is), new NoLineNumberPrinter(), "org/jd/core/v1/stub/EcjNestedAssert");
        }

        assertFalse(printed, printed.contains("$assertionsDisabled"));
        assertTrue(printed, printed.contains("assert name != null : \"name match needs a name\";"));
        assertTrue(printed, CompilerUtil.compile("17", new InMemoryJavaSourceFileObject("org/jd/core/v1/stub/EcjNestedAssert", printed)));
    }
}


