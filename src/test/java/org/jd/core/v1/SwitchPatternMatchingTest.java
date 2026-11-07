/*
 * Copyright (c) 2026 Nicolas Baumann (@nbauma109), with Claude Sonnet 5.5.
 * This project is distributed under the GPLv3 license.
 * This is a Copyleft license that gives the user the right to use,
 * copy and modify the code freely for non-commercial purposes.
 */
package org.jd.core.v1;

import org.jd.core.v1.compiler.CompilerUtil;
import org.jd.core.v1.compiler.InMemoryJavaSourceFileObject;
import org.jd.core.v1.loader.ZipLoader;
import org.jd.core.v1.printer.PlainTextPrinter;
import org.junit.Test;

import java.io.InputStream;

/** 'switch' with patterns, which javac 21 compiles to 'invokedynamic typeSwitch' */
public class SwitchPatternMatchingTest extends AbstractJdTest {

    @Test
    public void testJAVAC21() throws Exception {
        test("/jar/SwitchPatternMatching-JAVAC-21.jar", "org/jd/core/v1/stub/SwitchPatternMatching", "/txt/SwitchPatternMatching.txt", "21");
    }

    /** ECJ compiles the same switches, without the record patterns whose lowered form is not the one of javac */
    @Test
    public void testECJ21() throws Exception {
        test("/jar/SwitchPatternMatching-ECJ-21.jar", "org/jd/core/v1/stub/SwitchPatternMatchingEcj", "/txt/SwitchPatternMatchingEcj.txt", "21");
    }

    /** The type label of a primitive array is a descriptor ('[I') */
    @Test
    public void testPrimitiveArrayLabel() throws Exception {
        try (InputStream is = getClass().getResourceAsStream("/jar/SwitchPrimitiveArray-JAVAC-21.jar")) {
            String source = decompileSuccess(new ZipLoader(is), new PlainTextPrinter(), "org/jd/core/v1/stub/SwitchPrimitiveArray");

            assertTrue(source.contains("case int[] xs"));
            assertTrue(CompilerUtil.compile("21", new InMemoryJavaSourceFileObject("org/jd/core/v1/stub/SwitchPrimitiveArray", source)));
        }
    }
}
