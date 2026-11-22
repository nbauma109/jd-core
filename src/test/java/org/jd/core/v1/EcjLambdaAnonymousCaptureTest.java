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

public class EcjLambdaAnonymousCaptureTest extends AbstractJdTest {

    /** a variable which only the initializers of an anonymous class read is a parameter of its constructor, not a field: it is the variable, and no argument */
    @Test
    public void testAnonymousClassInALambda() throws Exception {
        String printed;

        try (InputStream is = getClass().getResourceAsStream("/jar/ecj-lambda-anonymous-capture-ecj-17.jar")) {
            printed = decompileSuccess(new ZipLoader(is), new NoLineNumberPrinter(), "org/jd/core/v1/stub/EcjLambdaAnonymousCapture");
        }

        String source = printed.replaceAll("\\s+", " ");

        assertTrue(printed, source.contains("new Iterator<String>() {"));
        assertTrue(printed, source.contains("this.original = numbers.iterator();"));
        assertTrue(printed, source.contains("return new Counter(start) {"));
        assertTrue(printed, source.contains("step + label.length();"));
        assertTrue(printed, CompilerUtil.compile("17", new InMemoryJavaSourceFileObject("org/jd/core/v1/stub/EcjLambdaAnonymousCapture", printed)));
    }
}


