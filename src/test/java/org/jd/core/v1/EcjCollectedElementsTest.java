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

public class EcjCollectedElementsTest extends AbstractJdTest {

    /** a cast of the result of 'collect(Collectors.toList())' would fix the type of the elements to the one of the stream: the target gives it */
    @Test
    public void testElementsOfASubtypeAreCollected() throws Exception {
        String printed;

        try (InputStream is = getClass().getResourceAsStream("/jar/ecj-collected-elements-ecj-17.jar")) {
            printed = decompileSuccess(new ZipLoader(is), new NoLineNumberPrinter(), "org/jd/core/v1/stub/EcjCollectedElements");
        }

        assertTrue(printed, printed.contains("return (ArrayList)texts.collect("));
        assertTrue(printed, printed.contains("return (List)texts.collect("));
        assertTrue(printed, CompilerUtil.compile("17", new InMemoryJavaSourceFileObject("org/jd/core/v1/stub/EcjCollectedElements", printed)));
    }
}
