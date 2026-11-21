/*
 * Copyright (c) 2026 Nicolas Baumann (@nbauma109), with Claude Sonnet 5.5.
 * This project is distributed under the GPLv3 license.
 * This is a copyleft license that gives the user the right to use,
 * copy and modify the code freely for non-commercial purposes.
 */

package org.jd.core.v1;

import org.jd.core.v1.compiler.CompilerUtil;
import org.jd.core.v1.compiler.InMemoryJavaSourceFileObject;
import org.jd.core.v1.loader.ClassPathLoader;
import org.jd.core.v1.printer.NoLineNumberPrinter;
import org.junit.Test;

public class TryWithTheCloseOfAnOrdinaryHandlerTest extends AbstractJdTest {

    /** the close of the stream is no resource closing of a try-with-resources: it is kept, whichever pattern the handler looks like */
    @Test
    public void testTheCloseIsKept() throws Exception {
        String internalClassName = TryWithTheCloseOfAnOrdinaryHandler.class.getName().replace('.', '/');
        String printed = decompileSuccess(new ClassPathLoader(), new NoLineNumberPrinter(), internalClassName);
        String source = printed.replaceAll("\\s+", " ");

        assertTrue(printed, source.contains("stream.close();"));
        // the handler which rethrows the exception it caught is no synthetic handler either
        assertTrue(printed, source.contains("context"));
        assertFalse(printed, source.contains("try (stream)"));
        // the same with a primary exception which the code does not set to null before the try statement
        assertTrue(printed, source.contains("context.addSuppressed("));
        // the resource is opened in the try statement: its initialization is kept
        assertTrue(printed, source.contains("InputStream opened = open();"));
        // the try statements which do not end with a close are no try-with-resources either
        assertTrue(printed, source.contains("this.count = 1;"));
        assertFalse(printed, source.contains("try (stream) { stream.read(); }"));
        assertTrue(printed, CompilerUtil.compile("17", new InMemoryJavaSourceFileObject(internalClassName, printed)));
    }
}
