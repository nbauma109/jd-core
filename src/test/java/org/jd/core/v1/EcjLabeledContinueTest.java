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

public class EcjLabeledContinueTest extends AbstractJdTest {

    /** a jump to the end of the outer loop is the 'continue' of the outer loop, not the 'break' of the last loop of its body */
    @Test
    public void testContinueOfTheOuterLoopFromANestedLoop() throws Exception {
        String printed;

        try (InputStream is = getClass().getResourceAsStream("/jar/ecj-labeled-continue-ecj-17.jar")) {
            printed = decompileSuccess(new ZipLoader(is), new PlainTextPrinter(), "org/jd/core/v1/stub/EcjLabeledContinue");
        }

        String source = printed.replaceAll("/\\*[^*]*\\*/", "").replaceAll("\\s+", " ");

        assertFalse(printed, source.contains("break label"));
        assertTrue(printed, source.contains("continue label"));
        assertTrue(printed, CompilerUtil.compile("17", new InMemoryJavaSourceFileObject("org/jd/core/v1/stub/EcjLabeledContinue", printed)));
    }
}
