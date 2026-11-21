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

public class EcjPatternHoistedTest extends AbstractJdTest {

    /** the binding of a pattern does not have the name of a variable of the same type which another branch declares: they are hoisted together */
    @Test
    public void testPatternWithTheNameOfAHoistedVariable() throws Exception {
        String printed;

        try (InputStream is = getClass().getResourceAsStream("/jar/ecj-pattern-hoisted-ecj-17.jar")) {
            printed = decompileSuccess(new ZipLoader(is), new PlainTextPrinter(), "org/jd/core/v1/stub/EcjPatternHoisted");
        }

        assertTrue(printed, printed.contains("instanceof Holder holder2"));
        assertTrue(printed, CompilerUtil.compile("17", new InMemoryJavaSourceFileObject("org/jd/core/v1/stub/EcjPatternHoisted", printed)));
    }

    /** a variable which an anonymous class captures is a field of the class: the binding which it uses is not renamed */
    @Test
    public void testNoRenameWhenAnAnonymousClassCapturesTheBinding() throws Exception {
        String printed;

        try (InputStream is = getClass().getResourceAsStream("/jar/ecj-pattern-hoisted-class-body-ecj-17.jar")) {
            printed = decompileSuccess(new ZipLoader(is), new PlainTextPrinter(), "org/jd/core/v1/stub/EcjPatternHoistedClassBody");
        }

        assertFalse(printed, printed.contains("holder1"));
    }
}
