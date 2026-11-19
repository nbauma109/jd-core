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

/** The try-with-resources which the Eclipse compiler (ECJ) generates: see the stub EcjTryWithResources */
public class EcjTryWithResourcesTest extends AbstractJdTest {

    @Test
    public void testVariableAssignedInTheTryAndUsedAfterIt() throws Exception {
        String printed;

        try (InputStream is = getClass().getResourceAsStream("/jar/ecj-try-with-resources-ecj-17.jar")) {
            printed = decompileSuccess(new ZipLoader(is), new PlainTextPrinter(), "org/jd/core/v1/stub/EcjTryWithResources");
        }

        String source = printed.replaceAll("/\\*[^*]*\\*/", "").replaceAll("\\s+", " ");

        // ECJ keeps the exception which the resource may throw in a slot which the code before the try and the handlers see as two variables: its
        // null assignment is no statement of the source
        assertFalse(printed, source.contains("null = null"));
        assertTrue(printed, source.contains("byte[] bytes = null; try (InputStream stream = EcjTryWithResources.class.getResourceAsStream(filename)) {"));
        assertTrue(printed, CompilerUtil.compile("17", new InMemoryJavaSourceFileObject("org/jd/core/v1/stub/EcjTryWithResources", printed)));
    }
}
