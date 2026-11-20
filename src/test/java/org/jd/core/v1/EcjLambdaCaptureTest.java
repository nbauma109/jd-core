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

public class EcjLambdaCaptureTest extends AbstractJdTest {

    /** The Eclipse compiler gives the method of a lambda no variable names: the captured variables are the ones of the call site, 'this' is no parameter */
    @Test
    public void testCapturedVariablesOfALambdaWhichUsesThis() throws Exception {
        String printed;

        try (InputStream is = getClass().getResourceAsStream("/jar/ecj-lambda-capture-ecj-17.jar")) {
            printed = decompileSuccess(new ZipLoader(is), new PlainTextPrinter(), "org/jd/core/v1/stub/EcjLambdaCapture");
        }

        String source = printed.replaceAll("/\\*[^*]*\\*/", "").replaceAll("\\s+", " ");

        assertFalse(printed, source.contains("paramEcjLambdaCapture"));
        assertFalse(printed, source.contains("paramString"));
        assertTrue(printed, source.contains("if (outer.flag) { outer.inbox.append(toPush).append(this.flag); }"));
        assertTrue(printed, source.contains("new StringBuilder(copy ? toPush : toPush.trim())"));
        assertTrue(printed, CompilerUtil.compile("17", new InMemoryJavaSourceFileObject("org/jd/core/v1/stub/EcjLambdaCapture", printed)));
    }
}
