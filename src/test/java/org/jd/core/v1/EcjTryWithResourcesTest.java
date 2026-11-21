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

    @Test
    public void testEmptyBodyOfTheResource() throws Exception {
        String printed;

        try (InputStream is = getClass().getResourceAsStream("/jar/ecj-try-with-resources-ecj-17.jar")) {
            printed = decompileSuccess(new ZipLoader(is), new PlainTextPrinter(), "org/jd/core/v1/stub/EcjTryWithResources");
        }

        String source = printed.replaceAll("/\\*[^*]*\\*/", "").replaceAll("\\s+", " ");

        // the exception temporaries and the handler of the Eclipse compiler are no statements of the source
        assertFalse(printed, source.contains("exception1"));
        assertTrue(printed, source.contains("closeAtOnce(InputStream stream) throws IOException { try (stream) {} }"));
        // the close which no null check guards is no statement of the body either
        assertTrue(printed, source.contains("createAndCloseUnguarded() throws IOException { try (InputStream created = new ByteArrayInputStream(new byte[1])) {} }"));
        assertTrue(printed, source.contains("createAndClose() throws IOException { try (InputStream created = new ByteArrayInputStream(new byte[0])) {} }"));
        assertTrue(printed, CompilerUtil.compile("17", new InMemoryJavaSourceFileObject("org/jd/core/v1/stub/EcjTryWithResources", printed)));
    }

    /** an assignment of the code which precedes the try-with-resources is not the one of the resource */
    @Test
    public void testAssignmentBeforeTheTry() throws Exception {
        String printed;

        try (InputStream is = getClass().getResourceAsStream("/jar/ecj-try-with-resources-ecj-17.jar")) {
            printed = decompileSuccess(new ZipLoader(is), new PlainTextPrinter(), "org/jd/core/v1/stub/EcjTryWithResources");
        }

        String source = printed.replaceAll("/\\*[^*]*\\*/", "").replaceAll("\\s+", " ");

        assertTrue(printed, source.contains("try (InputStream inputStream = source) { success = (inputStream.read() > 0); }"));
        assertFalse(printed, source.contains("addSuppressed"));
    }
}
